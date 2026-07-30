package com.mmk.kmpauth.google

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.browser.document
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeoutOrNull
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
        scopes: List<String>,
        requestAccessToken: Boolean
    ): Result<GoogleUser> {
        val scriptLoaded = waitForGoogleAuthScriptToLoad()
        if (!scriptLoaded) {
            return Result.failure(
                IllegalStateException("Google Sign-In script failed to load")
            )
        }

        // GIS splits the two tokens across two flows: google.accounts.id
        // (Sign in with Google / One Tap) issues the ID token, while the
        // OAuth token client issues the access token only (#146). Get the
        // ID token first - it is what Firebase and app backends verify.
        val needsAccessToken = requestAccessToken ||
            scopes.toSet() != GoogleAuthUiProvider.BASIC_AUTH_SCOPE.toSet()
        val idToken = requestIdTokenViaOneTap(isAutoSelectEnabled)

        if (idToken != null && !needsAccessToken) {
            return Result.success(googleUserFromIdToken(idToken))
        }
        if (idToken == null) {
            currentLoggerLog(
                "GoogleAuthUiProvider: One Tap did not return an ID token " +
                    "(prompt suppressed or dismissed); falling back to the OAuth token flow."
            )
        }
        return requestViaTokenClient(filterByAuthorizedAccounts, scopes, idToken)
    }

    /**
     * Runs the Sign in with Google (One Tap) prompt and returns the ID
     * token, or null when the prompt is suppressed, skipped or dismissed.
     */
    private suspend fun requestIdTokenViaOneTap(isAutoSelectEnabled: Boolean): String? =
        withTimeoutOrNull(5.minutes) {
            suspendCancellableCoroutine { continuation ->
                var resumed = false
                fun resumeOnce(value: String?) {
                    if (!resumed && continuation.isActive) {
                        resumed = true
                        continuation.resume(value)
                    }
                }
                initializeGsiId(
                    clientId = credentials.serverId,
                    autoSelect = isAutoSelectEnabled,
                    callback = { credentialResponse ->
                        resumeOnce(getCredentialResponseIdToken(credentialResponse))
                    },
                )
                promptGsiId { moment ->
                    // Covers FedCM and pre-FedCM notification shapes.
                    if (isMomentTerminal(moment)) resumeOnce(null)
                }
            }
        }

    /** OAuth token-client flow: access token (plus a previously obtained ID token, if any). */
    private suspend fun requestViaTokenClient(
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>,
        presetIdToken: String?,
    ): Result<GoogleUser> = suspendCancellableCoroutine { continuation ->
        var resumed = false
        val tokenClientConfig = createTokenClientConfig(
            clientId = credentials.serverId,
            scope = scopes.joinToString(" "),
            prompt = if (filterByAuthorizedAccounts) "none" else "select_account",
            callback = { tokenResponse: JsAny ->
                if (!resumed) {
                    resumed = true
                    CoroutineScope(continuation.context).launch {
                        continuation.handleTokenResponse(tokenResponse, presetIdToken)
                    }
                }
            },
            // Without this, a blocked or user-closed popup never invokes any
            // callback and the flow hangs with isInProgress stuck (GIS only
            // reports those through error_callback).
            errorCallback = { error: JsAny ->
                val type = getErrorType(error) ?: "unknown"
                showConsoleError("GoogleAuthUiProvider: token flow failed: $type")
                if (!resumed && continuation.isActive) {
                    resumed = true
                    continuation.resume(
                        Result.failure(
                            IllegalStateException(
                                "Google sign-in could not open its popup ($type). " +
                                    "Allow popups for this site, and make sure this " +
                                    "origin is listed in the OAuth client's " +
                                    "Authorized JavaScript origins."
                            )
                        )
                    )
                }
            },
        )

        val tokenClient = initTokenClient(tokenClientConfig)
        requestAccessToken(tokenClient)
    }

    private fun googleUserFromIdToken(idToken: String): GoogleUser = GoogleUser(
        idToken = idToken,
        accessToken = null,
        email = jwtClaim(idToken, "email"),
        displayName = jwtClaim(idToken, "name") ?: "",
        profilePicUrl = jwtClaim(idToken, "picture"),
    )

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

    private suspend fun CancellableContinuation<Result<GoogleUser>>.handleTokenResponse(
        tokenResponse: JsAny,
        presetIdToken: String?,
    ) {
        val error = getTokenResponseError(tokenResponse)
        if (error != null) {
            showConsoleError("Error during Google sign-in: $error")
            resume(Result.failure(IllegalStateException("Google sign-in failed: $error")))
            return
        }

        val idToken = presetIdToken ?: getTokenResponseIdToken(tokenResponse) ?: ""
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

            resume(Result.success(googleUser))

        } catch (err: Throwable) {
            showConsoleError("Error fetching user info: $err")

            val googleUser = GoogleUser(
                idToken = idToken,
                accessToken = accessToken
            )
            resume(Result.success(googleUser))
        }
    }

    @OptIn(KMPAuthInternalApi::class)
    private fun currentLoggerLog(message: String) {
        currentLogger.log(message)
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
    (config, clientId, scope, prompt, callback, errorCallback) => {
        config.client_id = clientId;
        config.scope = scope;
        config.prompt = prompt;
        config.callback = callback;
        config.error_callback = errorCallback;
    }
"""
)
private external fun setTokenClientConfigPropsImpl(
    config: JsAny,
    clientId: String,
    scope: String,
    prompt: String,
    callback: (JsAny) -> Unit,
    errorCallback: (JsAny) -> Unit,
)


private fun createTokenClientConfig(
    clientId: String,
    callback: (JsAny) -> Unit,
    errorCallback: (JsAny) -> Unit,
    scope: String,
    prompt: String
): JsAny {
    val obj: JsAny = createEmptyObject()
    setTokenClientConfigPropsImpl(obj, clientId, scope, prompt, callback, errorCallback)
    return obj
}

private fun getErrorType(error: JsAny): String? = js("error && error.type ? error.type : null")


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

// --- Sign in with Google (google.accounts.id) interop -----------------------

@JsFun(
    """
    (clientId, autoSelect, callback) => {
        google.accounts.id.initialize({
            client_id: clientId,
            auto_select: autoSelect,
            callback: callback,
            use_fedcm_for_prompt: true
        });
    }
"""
)
private external fun initializeGsiId(
    clientId: String,
    autoSelect: Boolean,
    callback: (JsAny) -> Unit,
)

private fun promptGsiId(momentCallback: (JsAny) -> Unit): Unit =
    js("google.accounts.id.prompt(momentCallback)")

/** True when the prompt will not produce a credential (skipped/dismissed/not shown). */
private fun isMomentTerminal(moment: JsAny): Boolean =
    js(
        """
        (() => {
           try {
             if (moment.isSkippedMoment && moment.isSkippedMoment()) return true;
             if (moment.isDismissedMoment && moment.isDismissedMoment()
                 && moment.getDismissedReason && moment.getDismissedReason() !== 'credential_returned') return true;
             if (moment.isNotDisplayed && moment.isNotDisplayed()) return true;
           } catch (e) { return true; }
           return false;
        })()
    """
    )

private fun getCredentialResponseIdToken(credentialResponse: JsAny): String? =
    js("credentialResponse.credential")

/** Reads a claim from the ID token's JWT payload without verification. */
private fun jwtClaim(jwt: String, name: String): String? =
    js(
        """
        (() => {
           try {
             const payload = JSON.parse(atob(jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
             const value = payload[name];
             return value == null ? null : String(value);
           } catch (e) { return null; }
        })()
    """
    )
