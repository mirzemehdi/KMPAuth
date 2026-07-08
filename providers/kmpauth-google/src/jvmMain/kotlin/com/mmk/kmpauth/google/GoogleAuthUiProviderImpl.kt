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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.html.body
import kotlinx.html.script
import kotlinx.html.unsafe
import java.awt.Desktop
import java.net.BindException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64


internal class GoogleAuthUiProviderImpl(private val credentials: GoogleAuthCredentials) :
    GoogleAuthUiProvider {

    private val authUrl = "https://accounts.google.com/o/oauth2/v2/auth"

    @OptIn(KMPAuthInternalApi::class)
    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        val redirectTarget = resolveRedirectTarget() ?: return null
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
                    "&redirect_uri=${redirectTarget.redirectUri}" +
                    "&response_type=$encodedResponseType" +
                    "&scope=$encodedScope" +
                    "&nonce=$nonce" +
                    "&state=$state"
        }


        val (idToken, accessToken) = startHttpServerAndGetToken(
            googleAuthUrl = googleAuthUrl,
            redirectTarget = redirectTarget,
            state = state,
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
    @OptIn(KMPAuthInternalApi::class)
    private suspend fun startHttpServerAndGetToken(
        googleAuthUrl: String,
        redirectTarget: RedirectTarget,
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
                    window.location.href = '${redirectTarget.tokenPath}?' +
                        (idToken ? 'id_token=' + idToken : '') +
                        (idToken && accessToken ? '&' : '') +
                        (accessToken ? 'access_token=' + accessToken : '');
                } else {
                    console.error('State does not match! Possible CSRF attack.');
                    window.location.href = '${redirectTarget.tokenPath}?id_token=null';
                }
            }
        """.trimIndent()
        val server = try {
            embeddedServer(Netty, port = redirectTarget.port) {
                routing {
                    get(redirectTarget.callbackPath) {
                        call.respondHtml {
                            body { script { unsafe { +jsCode } } }
                        }
                    }
                    get(redirectTarget.tokenPath) {
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
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            val bindFailure = e is BindException || e.cause is BindException
            if (bindFailure) {
                currentLogger.log(
                    "GoogleAuthUiProvider: could not bind port ${redirectTarget.port} for the OAuth " +
                        "redirect (${redirectTarget.redirectUri}) - it is already in use. Free the " +
                        "port, or set a different (Google-console-registered) " +
                        "GoogleAuthCredentials.redirectUri."
                )
            } else {
                currentLogger.log(
                    "GoogleAuthUiProvider: failed to start the redirect server on port ${redirectTarget.port}: $e"
                )
            }
            return Pair(null, null)
        }

        // Open the browser only after the callback server is listening, so the
        // OAuth redirect can never arrive before the endpoint is ready.
        openUrlInBrowser(googleAuthUrl)

        val idTokenAndAccessTokenPair = tokenPairDeferred.await()
        server.stop(1000, 1000)
        return idTokenAndAccessTokenPair
    }

    /**
     * Resolves [GoogleAuthCredentials.redirectUri] (or the default) into the
     * pieces the desktop flow needs: the exact string sent to Google as
     * `redirect_uri`, the port the local callback server binds, the path it
     * serves, and the internal path the in-browser script posts tokens back to.
     * Returns null (logging why) when the URI is not a usable http loopback URL.
     */
    @OptIn(KMPAuthInternalApi::class)
    private fun resolveRedirectTarget(): RedirectTarget? {
        val raw = credentials.redirectUri
        val parsed = try {
            URI(raw)
        } catch (e: Exception) {
            currentLogger.log("GoogleAuthUiProvider: redirectUri '$raw' is not a valid URI: $e")
            return null
        }
        if (parsed.scheme?.lowercase() != "http") {
            currentLogger.log(
                "GoogleAuthUiProvider: redirectUri must use the http scheme " +
                    "(e.g. http://localhost:8080/callback), got '$raw'."
            )
            return null
        }
        val host = parsed.host
        if (host == null || host.lowercase() !in LOOPBACK_HOSTS) {
            currentLogger.log(
                "GoogleAuthUiProvider: redirectUri host must be a loopback address " +
                    "(localhost or 127.0.0.1), got '$raw'."
            )
            return null
        }
        val port = parsed.port
        if (port <= 0) {
            currentLogger.log(
                "GoogleAuthUiProvider: redirectUri must include an explicit port " +
                    "(e.g. http://localhost:8080/callback), got '$raw'."
            )
            return null
        }
        // Google redirects to the exact path we register, so serve that path
        // verbatim; browsers normalize a missing path to "/".
        val callbackPath = parsed.path.ifEmpty { "/" }
        val tokenPath = callbackPath.trimEnd('/') + "/token"
        return RedirectTarget(
            redirectUri = raw,
            port = port,
            callbackPath = callbackPath,
            tokenPath = tokenPath,
        )
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

    /** Parsed view of the desktop OAuth loopback redirect. */
    private data class RedirectTarget(
        val redirectUri: String,
        val port: Int,
        val callbackPath: String,
        val tokenPath: String,
    )

    private companion object {
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "[::1]", "::1")
    }

}
