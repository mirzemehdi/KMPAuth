package com.mmk.kmpauth.google

import com.auth0.jwt.JWT
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
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

internal class GoogleAuthUiProviderImpl(private val credentials: GoogleAuthCredentials) :
    GoogleAuthUiProvider {

    private val authUrl = "https://accounts.google.com/o/oauth2/v2/auth"

    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        val responseType = "id_token token"
        val scopeString = scopes.joinToString(" ")
        val redirectUri = "http://localhost:8080/callback"
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


        openUrlInBrowser(googleAuthUrl)

        val (idToken, accessToken) = startHttpServerAndGetToken(state = state)
        if (idToken == null && accessToken == null) {
            println("GoogleAuthUiProvider: token is null")
            return null
        }


        val jwt = idToken?.let { JWT().decodeJwt(it) }
        val email = jwt?.getClaim("email")?.asString()
        val name = jwt?.getClaim("name")?.asString() // User's name
        val picture = jwt?.getClaim("picture")?.asString()
        val receivedNonce = jwt?.getClaim("nonce")?.asString()
        if (receivedNonce != nonce) {
            println("GoogleAuthUiProvider: Invalid nonce state: A login callback was received, but no login request was sent.")
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
        state: String
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

        val server = embeddedServer(Netty, port = 8080) {
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

        val idTokenAndAccessTokenPair = tokenPairDeferred.await()
        server.stop(1000, 1000)
        return idTokenAndAccessTokenPair
    }

    private fun openUrlInBrowser(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            println("GoogleAuthUiProvider: Desktop is not supported on this platform.")
        }
    }

    private fun generateRandomString(length: Int = 32): String {
        val secureRandom = SecureRandom()
        val stateBytes = ByteArray(length)
        secureRandom.nextBytes(stateBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
    }


}