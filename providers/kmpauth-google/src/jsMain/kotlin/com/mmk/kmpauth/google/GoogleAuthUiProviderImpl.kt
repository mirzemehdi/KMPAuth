package com.mmk.kmpauth.google

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.dom.HTMLScriptElement
import org.w3c.fetch.RequestInit
import kotlin.coroutines.resume
import kotlin.js.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class GoogleAuthUiProviderImpl(private val credentials: GoogleAuthCredentials) : GoogleAuthUiProvider {

    private var googleAuthScriptLoaded = false

    init {
        if (!googleAuthScriptLoaded) loadGoogleAuthScript()
    }

    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        val scriptLoaded = waitForGoogleAuthScriptToLoad()
        if (!scriptLoaded) return null

        return suspendCancellableCoroutine { continuation ->
            val tokenClientConfig = createTokenClientConfig(
                clientId = credentials.serverId,
                scope = scopes.joinToString(" "),
                prompt = if (filterByAuthorizedAccounts) "none" else "select_account",
                callback = { tokenResponse: dynamic ->
                    CoroutineScope(continuation.context).launch {
                        continuation.handleTokenResponse(tokenResponse)
                    }
                }
            )
            val tokenClient = initTokenClient(tokenClientConfig)
            requestAccessToken(tokenClient)
        }
    }


    private fun loadGoogleAuthScript() {
        val script = document.createElement("script") as HTMLScriptElement
        script.src = "https://accounts.google.com/gsi/client"
        script.async = true
        script.defer = true
        script.onload = {
            googleAuthScriptLoaded = true
        }
        document.head?.appendChild(script)
    }

    private suspend fun CancellableContinuation<GoogleUser?>.handleTokenResponse(tokenResponse: dynamic) {

        val error = getTokenResponseError(tokenResponse)
        if (error != null) {
            showConsoleError("Error during Google sign-in: $error")
            resume(null)
            return
        }

        val idToken = getTokenResponseIdToken(tokenResponse) ?: ""
        val accessToken = getTokenResponseAccessToken(tokenResponse) ?: ""

        try {
            val userInfo = fetchGoogleUserInfo(accessToken = accessToken)

            val email = getUserInfoEmail(userInfo)
            val name = getUserInfoName(userInfo) ?: ""
            val picture = getUserInfoPicture(userInfo)

            val googleUser = GoogleUser(
                idToken = idToken,
                accessToken = accessToken,
                email = email,
                displayName = name,
                profilePicUrl = picture
            )

            resume(googleUser)

        } catch (err: Throwable) {
            showConsoleError("Error fetching user info: $err")

            val googleUser = GoogleUser(
                idToken = idToken,
                accessToken = accessToken
            )
            resume(googleUser)
        }
    }


    private suspend fun fetchGoogleUserInfo(accessToken: String): dynamic {
        val headers = js("({})")

        headers["Authorization"] = "Bearer $accessToken"

        val fetchOptions = js("({})")
        fetchOptions["headers"] = headers

        val response = window.fetch(
            "https://www.googleapis.com/oauth2/v3/userinfo",
            fetchOptions.unsafeCast<RequestInit>()
        ).await()

        val userInfo = response.json().await()
        return userInfo.asDynamic()
    }

    private suspend fun waitForGoogleAuthScriptToLoad(timeout: Duration = 5.minutes): Boolean {
        if (googleAuthScriptLoaded) return true

        return withTimeoutOrNull(timeout) {
            while (!googleAuthScriptLoaded) delay(300)
            true
        } ?: run {
            showConsoleError("Google Auth failed to initialize. Timeout reached: $timeout")
            false
        }
    }


}

private fun createTokenClientConfig(
    clientId: String,
    callback: (dynamic) -> Unit,
    scope: String,
    prompt: String
): dynamic {
    val obj = js("({})")
    js("Object.assign")(
        obj, json(
            "client_id" to clientId,
            "callback" to callback,
            "scope" to scope,
            "prompt" to prompt
        )
    )
    return obj
}

@OptIn(KMPAuthInternalApi::class)
private fun showConsoleError(message: String): Unit {
    currentLogger.log(message)
    // js("console.error(message)") //TODO Show in console?
}

private fun initTokenClient(tokenClientConfig: dynamic): dynamic =
    js("google.accounts.oauth2.initTokenClient(tokenClientConfig)")

private fun requestAccessToken(tokenClient: dynamic): Unit = tokenClient.requestAccessToken()

private fun getUserInfoEmail(userInfo: dynamic): String? = userInfo.email
private fun getUserInfoName(userInfo: dynamic): String? = userInfo.name
private fun getUserInfoPicture(userInfo: dynamic): String? = userInfo.picture

private fun getTokenResponseError(tokenResponse: dynamic): String? = tokenResponse.error

// Extract id_token from tokenResponse
private fun getTokenResponseIdToken(tokenResponse: dynamic): String? = tokenResponse.id_token

// Extract access_token from tokenResponse
private fun getTokenResponseAccessToken(tokenResponse: dynamic): String? = tokenResponse.access_token
