package com.mmk.kmpauth.google

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.browser.document
import kotlinx.coroutines.*
import org.w3c.dom.HTMLScriptElement
import kotlin.coroutines.resume
import kotlin.js.Promise
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
                callback = { tokenResponse: JsAny ->
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

    private suspend fun CancellableContinuation<GoogleUser?>.handleTokenResponse(tokenResponse: JsAny) {

        val error = getTokenResponseError(tokenResponse)
        if (error != null) {
            showConsoleError("Error during Google sign-in: $error")
            resume(null)
            return
        }

        val idToken = getTokenResponseIdToken(tokenResponse) ?: ""
        val accessToken = getTokenResponseAccessToken(tokenResponse) ?: ""

        val googleUserInfoPromise: Promise<JsAny> = fetchGoogleUserInfoPromise(accessToken).unsafeCast()
        try {
            val userInfo = googleUserInfoPromise.await<JsAny>()

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

}


private fun fetchGoogleUserInfoPromise(accessToken: String): JsAny =
    js(
        """
       (() => {
          const headers = { Authorization: "Bearer " + accessToken };
          const fetchOptions = { headers };
          return fetch("https://www.googleapis.com/oauth2/v3/userinfo", fetchOptions).then(res => res.json());
       })()
    """
    )


@JsFun(
    """
    (config, clientId, scope, prompt, callback) => {
        config.client_id = clientId;
        config.scope = scope;
        config.prompt = prompt;
        config.callback = callback;
    }
"""
)
private external fun setTokenClientConfigPropsImpl(
    config: JsAny,
    clientId: String,
    scope: String,
    prompt: String,
    callback: (JsAny) -> Unit
)


private fun createTokenClientConfig(
    clientId: String,
    callback: (JsAny) -> Unit,
    scope: String,
    prompt: String
): JsAny {
    val obj: JsAny = createEmptyObject()
    setTokenClientConfigPropsImpl(obj, clientId, scope, prompt, callback)
    return obj
}


private fun createEmptyObject(): JsAny = js("({})")

@OptIn(KMPAuthInternalApi::class)
private fun showConsoleError(message: String): Unit {
    currentLogger.log(message)
    // js("console.error(message)") //TODO Show in console?
}

private fun initTokenClient(tokenClientConfig: JsAny): JsAny =
    js("google.accounts.oauth2.initTokenClient(tokenClientConfig)")

private fun requestAccessToken(tokenClient: JsAny): Unit = js("tokenClient.requestAccessToken()")

private fun getUserInfoEmail(userInfo: JsAny): String? = js("userInfo.email")
private fun getUserInfoName(userInfo: JsAny): String? = js("userInfo.name")
private fun getUserInfoPicture(userInfo: JsAny): String? = js("userInfo.picture")

private fun getTokenResponseError(tokenResponse: JsAny): String? = js("tokenResponse.error")

// Extract id_token from tokenResponse
private fun getTokenResponseIdToken(tokenResponse: JsAny): String? = js("tokenResponse.id_token")

// Extract access_token from tokenResponse
private fun getTokenResponseAccessToken(tokenResponse: JsAny): String? = js("tokenResponse.access_token")






