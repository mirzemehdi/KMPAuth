package com.mmk.kmpauth.google

import com.auth0.jwt.JWT
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.body
import kotlinx.html.script
import kotlinx.html.unsafe
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

internal class GoogleAuthUiProviderImpl(private val credentials: GoogleAuthCredentials) :
    GoogleAuthUiProvider {

    private val authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
    private val preferredPort: Int = 8080

    @OptIn(KMPAuthInternalApi::class)
    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        // Parse custom redirectUri or use default with preferred port
        val (redirectUri, port) = if (credentials.redirectUri != null) {
            // Use custom redirectUri and extract port from it
            val customUri = credentials.redirectUri
            val portFromUri = try {
                URI(customUri).port.takeIf { it > 0 } ?: 8080
            } catch (e: Exception) {
                currentLogger.log("GoogleAuthUiProvider: Failed to parse redirectUri, using default port 8080")
                8080
            }
            customUri to portFromUri
        } else {
            // Use default behavior: try preferred port, fallback to nearby ports
            val foundPort = findAvailablePort(preferredPort)
            "http://localhost:$foundPort/callback" to foundPort
        }

        val responseType = "id_token token"
        val scopeString = scopes.joinToString(" ")
        val state: String
        var nonce: String?
        val googleAuthUrl = withContext(Dispatchers.IO) {
            val encodedResponseType =
                URLEncoder.encode(responseType, StandardCharsets.UTF_8.toString())
            state = URLEncoder.encode(generateRandomString(), StandardCharsets.UTF_8.toString())
            val encodedScope = URLEncoder.encode(scopeString, StandardCharsets.UTF_8.toString())
            nonce = URLEncoder.encode(generateRandomString(), StandardCharsets.UTF_8.toString())
            "$authUrl?" +
                    "client_id=${credentials.serverId}" +
                    "&redirect_uri=$redirectUri" +
                    "&response_type=$encodedResponseType" +
                    "&scope=$encodedScope" +
                    "&nonce=$nonce" +
                    "&state=$state"
        }

        // Start server BEFORE opening browser
        val (idToken, accessToken) = startHttpServerAndGetToken(
            state = state,
            googleAuthUrl = googleAuthUrl,
            port = port
        )
        if (idToken == null && accessToken == null) {
            currentLogger.log("GoogleAuthUiProvider: token is null")
            return null
        }


        val jwt = idToken?.let { JWT().decodeJwt(it) }
        val email = jwt?.getClaim("email")?.asString()
        val name = jwt?.getClaim("name")?.asString() // User's name
        val picture = jwt?.getClaim("picture")?.asString()
        val receivedNonce = jwt?.getClaim("nonce")?.asString()
        if (receivedNonce != nonce) {
            currentLogger.log("GoogleAuthUiProvider: Invalid nonce state: A login callback was received, but no login request was sent.")
            return null
        }

        return GoogleUser(
            idToken = idToken ?: "",
            accessToken = accessToken,
            email = email,
            displayName = name ?: "",
            profilePicUrl = picture
        )
    }

    //Pair, first one is idToken, second one is accessToken
    private suspend fun startHttpServerAndGetToken(
        redirectUriPath: String = "/callback",
        state: String,
        googleAuthUrl: String,
        port: Int
    ): Pair<String?, String?> {
        val tokenPairDeferred = CompletableDeferred<Pair<String?, String?>>()

        val jsCode = """
            var fragment = window.location.hash;
            if (fragment) {
                var params = new URLSearchParams(fragment.substring(1)); 
                var idToken = params.get('id_token');
                var accessToken = params.get('access_token');
                var receivedState = params.get('state');
                var expectedState = '${state}'; 
                if (receivedState === expectedState) {
                    window.location.href = '$redirectUriPath/token?' + 
                        (idToken ? 'id_token=' + idToken : '') + 
                        (idToken && accessToken ? '&' : '') + 
                        (accessToken ? 'access_token=' + accessToken : '');
                } else {
                    console.error('State does not match! Possible CSRF attack.');
                    window.location.href = '$redirectUriPath/token?id_token=null';
                }
            }                 
        """.trimIndent()

        val server = embeddedServer(Netty, port = port) {
            routing {
                get(redirectUriPath) {
                    call.respondHtml {
                        body { script { unsafe { +jsCode } } }
                    }
                }
                get("$redirectUriPath/token") {
                    val idToken = call.request.queryParameters["id_token"]
                    val accessToken = call.request.queryParameters["access_token"]
                    if (idToken.isNullOrEmpty().not() || accessToken.isNullOrEmpty().not()) {
                        call.respondText(
                            "Authorization is complete. You can close this window, and return to the application",
                            contentType = ContentType.Text.Plain
                        )
                        tokenPairDeferred.complete(Pair(idToken, accessToken))
                    } else {
                        call.respondText(
                            "Authorization failed",
                            contentType = ContentType.Text.Plain
                        )
                        tokenPairDeferred.complete(Pair(null, null))
                    }
                }
            }
        }.start(wait = false)

        // Open browser AFTER server is started
        openUrlInBrowser(googleAuthUrl)

        val idTokenAndAccessTokenPair = tokenPairDeferred.await()
        server.stop(1000, 1000)
        return idTokenAndAccessTokenPair
    }

    @OptIn(KMPAuthInternalApi::class)
    private fun openUrlInBrowser(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            currentLogger.log("GoogleAuthUiProvider: Desktop is not supported on this platform.")
        }
    }

    private fun generateRandomString(length: Int = 32): String {
        val secureRandom = SecureRandom()
        val stateBytes = ByteArray(length)
        secureRandom.nextBytes(stateBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
    }


    @OptIn(KMPAuthInternalApi::class)
    private fun findAvailablePort(preferredPort: Int = 8080): Int {
        // First, try the preferred port
        try {
            ServerSocket(preferredPort).use {
                return preferredPort
            }
        } catch (_: Exception) {
            // Port is occupied, try nearby ports (8080-8089)
            for (port in (preferredPort + 1)..(preferredPort + 9)) {
                try {
                    ServerSocket(port).use {
                        currentLogger.log("GoogleAuthUiProvider: Preferred port $preferredPort is occupied, using port $port instead")
                        return port
                    }
                } catch (_: Exception) {
                    // Try next port
                }
            }
        }
        // If all preferred ports are occupied, find any available port
        val port = ServerSocket(0).use { socket -> socket.localPort }
        currentLogger.log("GoogleAuthUiProvider: All preferred ports (8080-8089) are occupied, using random port $port. Make sure this is configured in Google Console.")
        return port
    }

}