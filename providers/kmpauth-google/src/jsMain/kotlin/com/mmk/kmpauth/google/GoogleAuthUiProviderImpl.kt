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
            showConsoleError(
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
                    callback = { credentialResponse: dynamic ->
                        resumeOnce(credentialResponse.credential as? String)
                    },
                )
                promptGsiId { moment: dynamic ->
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
            callback = { tokenResponse: dynamic ->
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
            errorCallback = { error: dynamic ->
                val type = (error?.type as? String) ?: "unknown"
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

    private suspend fun CancellableContinuation<Result<GoogleUser>>.handleTokenResponse(
        tokenResponse: dynamic,
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
    errorCallback: (dynamic) -> Unit,
    scope: String,
    prompt: String
): dynamic {
    val obj = js("({})")
    js("Object.assign")(
        obj, json(
            "client_id" to clientId,
            "callback" to callback,
            "error_callback" to errorCallback,
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

// --- Sign in with Google (google.accounts.id) interop -----------------------

private fun initializeGsiId(
    clientId: String,
    autoSelect: Boolean,
    callback: (dynamic) -> Unit,
) {
    val config = js("({})")
    js("Object.assign")(
        config, json(
            "client_id" to clientId,
            "auto_select" to autoSelect,
            "callback" to callback,
            "use_fedcm_for_prompt" to true,
        )
    )
    js("google.accounts.id.initialize(config)")
}

private fun promptGsiId(momentCallback: (dynamic) -> Unit) {
    js("google.accounts.id.prompt(momentCallback)")
}

/** True when the prompt will not produce a credential (skipped/dismissed/not shown). */
private fun isMomentTerminal(moment: dynamic): Boolean = try {
    when {
        moment.isSkippedMoment != null && moment.isSkippedMoment() == true -> true
        moment.isDismissedMoment != null && moment.isDismissedMoment() == true &&
            moment.getDismissedReason != null &&
            moment.getDismissedReason() != "credential_returned" -> true
        moment.isNotDisplayed != null && moment.isNotDisplayed() == true -> true
        else -> false
    }
} catch (e: Throwable) {
    true
}

/** Reads a claim from the ID token's JWT payload without verification. */
private fun jwtClaim(jwt: String, name: String): String? = try {
    val payloadJson = window.atob(
        jwt.split(".")[1].replace("-", "+").replace("_", "/")
    )
    val payload = JSON.parse<dynamic>(payloadJson)
    val value = payload[name]
    if (value == null) null else value.toString()
} catch (e: Throwable) {
    null
}
