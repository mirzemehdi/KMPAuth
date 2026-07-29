@file:OptIn(KMPAuthInternalApi::class)

package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.core.runCatchingCancellable
import kotlin.concurrent.Volatile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Signed-in user as reported by the Firebase Auth REST API. */
internal data class FirebaseRestUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val providerId: String?,
    val idToken: String,
    val refreshToken: String?,
    val isAnonymous: Boolean,
)

@KMPAuthInternalApi
internal class FirebaseRestKMPAuthUser(internal val user: FirebaseRestUser) : KMPAuthUser {
    override val uid: String get() = user.uid
    override val email: String? get() = user.email
    override val displayName: String? get() = user.displayName
    override val photoUrl: String? get() = user.photoUrl
    override val providerId: String? get() = user.providerId
    override val raw: Any get() = user
}

/**
 * Minimal HTTP POST abstraction so the engine is unit-testable and
 * platform-portable (JDK HttpClient on Desktop, fetch on wasm).
 */
internal fun interface FirebaseRestTransport {
    /** Posts [jsonBody] to [url]; returns the response body (any status). */
    suspend fun post(url: String, jsonBody: String): String
}

/** Firebase session handed back by a browser sign-in page. */
internal data class WebFlowResult(
    val idToken: String,
    val refreshToken: String?,
)

/** Parameters of one web-flow sign-in attempt. */
internal data class WebFlowRequest(
    val providerId: String,
    val scopes: List<String>,
    val customParameters: Map<String, String>,
)

/**
 * [AuthProviderBackend] engine over the Firebase Auth REST API (Identity
 * Toolkit). Used on Desktop, where GitLive's firebase-java-sdk does not
 * implement auth (#204), and on wasm, where the Firebase SDK has no
 * target at all.
 *
 * The session (current user, tokens) is held in memory for the process
 * lifetime; there is no persistence yet. [webFlowRunner] is the platform's
 * browser OAuth flow — Desktop's loopback page, or null where no flow
 * exists (wasm), in which case `OAuthWebFlow` credentials fail with a
 * reason.
 */
internal class FirebaseRestAuthEngine(
    private val transport: FirebaseRestTransport,
    private val apiKeyProvider: () -> String,
    private val webFlowRunner: (suspend (WebFlowRequest) -> WebFlowResult)? = null,
) : AuthProviderBackend {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var session: FirebaseRestUser? = null

    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        val user = when (credential) {
            is AuthCredential.EmailPassword ->
                if (linkWithCurrentUser && session != null) {
                    // Linking email/password to the current (e.g.
                    // anonymous) user keeps its uid.
                    signedInUser(
                        call(
                            "accounts:signUp",
                            buildJsonObject {
                                put("idToken", requireSession().idToken)
                                put("email", credential.email)
                                put("password", credential.password)
                                put("returnSecureToken", true)
                            }
                        )
                    )
                } else {
                    signedInUser(
                        call(
                            "accounts:signInWithPassword",
                            buildJsonObject {
                                put("email", credential.email)
                                put("password", credential.password)
                                put("returnSecureToken", true)
                            }
                        )
                    )
                }

            is AuthCredential.IdToken -> signedInUser(
                call("accounts:signInWithIdp", credential.toSignInWithIdpBody(linkWithCurrentUser))
            )

            is AuthCredential.OAuthWebFlow -> {
                val runner = webFlowRunner ?: throw UnsupportedOperationException(
                    "Firebase web-flow sign-in (provider " +
                        "'${credential.providerId}') is not available on this " +
                        "platform."
                )
                if (linkWithCurrentUser) throw UnsupportedOperationException(
                    "Linking a web-flow provider to the current user is not " +
                        "supported with the Firebase REST engine yet."
                )
                val flowResult = runner(
                    WebFlowRequest(
                        providerId = credential.providerId,
                        scopes = credential.scopes,
                        customParameters = credential.customParameters,
                    )
                )
                adoptSession(flowResult, credential.providerId)
            }
        }
        session = user
        FirebaseRestKMPAuthUser(user)
    }

    /**
     * Adopts a Firebase session produced by the browser page (the JS SDK
     * already completed sign-in there) by looking up the user profile for
     * its ID token.
     */
    private suspend fun adoptSession(flowResult: WebFlowResult, providerId: String): FirebaseRestUser {
        val info = call(
            "accounts:lookup",
            buildJsonObject { put("idToken", flowResult.idToken) },
        ).get("users")?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw IllegalStateException("Firebase Null user")
        return FirebaseRestUser(
            uid = info["localId"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Firebase Null user"),
            email = info["email"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            displayName = info["displayName"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            photoUrl = info["photoUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            providerId = providerId,
            idToken = flowResult.idToken,
            refreshToken = flowResult.refreshToken,
            isAnonymous = false,
        )
    }

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        runCatchingCancellable {
            requireSession()
            val user = when (credential) {
                is AuthCredential.EmailPassword -> signedInUser(
                    call(
                        "accounts:signInWithPassword",
                        buildJsonObject {
                            put("email", credential.email)
                            put("password", credential.password)
                            put("returnSecureToken", true)
                        }
                    )
                )

                is AuthCredential.IdToken -> signedInUser(
                    call("accounts:signInWithIdp", credential.toSignInWithIdpBody(link = false))
                )

                is AuthCredential.OAuthWebFlow -> throw UnsupportedOperationException(
                    "FirebaseAuthBackend cannot reauthenticate with this credential " +
                        "(provider '${credential.providerId}')."
                )
            }
            if (user.uid != requireSession().uid) {
                throw IllegalStateException(
                    "Reauthentication credential belongs to a different user"
                )
            }
            session = user
        }

    override suspend fun signUp(email: String, password: String): Result<KMPAuthUser> =
        runCatchingCancellable {
            val user = signedInUser(
                call(
                    "accounts:signUp",
                    buildJsonObject {
                        put("email", email)
                        put("password", password)
                        put("returnSecureToken", true)
                    }
                )
            )
            session = user
            FirebaseRestKMPAuthUser(user)
        }

    override suspend fun signInAnonymously(): Result<KMPAuthUser> = runCatchingCancellable {
        // An already-anonymous session is resumed instead of minting a new
        // throwaway account, matching the native SDKs.
        session?.takeIf { it.isAnonymous }?.let {
            return@runCatchingCancellable FirebaseRestKMPAuthUser(it)
        }
        val user = signedInUser(
            call("accounts:signUp", buildJsonObject { put("returnSecureToken", true) }),
            anonymous = true,
        )
        session = user
        FirebaseRestKMPAuthUser(user)
    }

    override suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = Result.failure(
        UnsupportedOperationException(
            "Firebase phone sign-in is not available with the REST engine " +
                "(Desktop/wasm): the Identity Toolkit REST flow requires a " +
                "reCAPTCHA token. The Supabase backend serves phone OTP on " +
                "every target."
        )
    )

    override suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings?,
    ): Result<Unit> = runCatchingCancellable {
        call(
            "accounts:sendOobCode",
            buildJsonObject {
                put("requestType", "PASSWORD_RESET")
                put("email", email)
                actionCodeSettings?.applyTo(this)
            }
        )
        Unit
    }

    override suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = runCatchingCancellable {
        call(
            "accounts:sendOobCode",
            buildJsonObject {
                put("requestType", "EMAIL_SIGNIN")
                put("email", email)
                actionCodeSettings.applyTo(this)
            }
        )
        Unit
    }

    override fun isSignInWithEmailLink(link: String): Boolean {
        val query = link.queryStringOrNull() ?: return false
        return query.contains("oobCode=") && query.contains("mode=signIn")
    }

    override suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        val oobCode = link.queryStringOrNull()
            ?.split("&")
            ?.firstOrNull { it.startsWith("oobCode=") }
            ?.substringAfter("=")
            ?: throw IllegalArgumentException("Link is not a Firebase email sign-in link")
        val user = signedInUser(
            call(
                "accounts:signInWithEmailLink",
                buildJsonObject {
                    put("email", email)
                    put("oobCode", oobCode)
                    if (linkAccount) session?.let { put("idToken", it.idToken) }
                }
            )
        )
        session = user
        FirebaseRestKMPAuthUser(user)
    }

    override suspend fun signOut() {
        session = null
    }

    override fun currentUser(): KMPAuthUser? = session?.let { FirebaseRestKMPAuthUser(it) }

    private fun requireSession(): FirebaseRestUser =
        session ?: throw IllegalStateException("No signed-in user to reauthenticate")

    private fun AuthCredential.IdToken.toSignInWithIdpBody(link: Boolean): JsonObject {
        val postBody = buildString {
            when (providerId) {
                AuthProviderIds.FACEBOOK ->
                    if (rawNonce != null) {
                        // Facebook Limited Login: OIDC JWT + nonce.
                        append("id_token=").append(idToken.urlEncoded())
                        append("&nonce=").append(rawNonce!!.urlEncoded())
                    } else {
                        append("access_token=").append((accessToken ?: idToken).urlEncoded())
                    }

                AuthProviderIds.APPLE -> {
                    append("id_token=").append(idToken.urlEncoded())
                    rawNonce?.let { append("&nonce=").append(it.urlEncoded()) }
                }

                else -> append("id_token=").append(idToken.urlEncoded())
            }
            append("&providerId=").append(providerId.urlEncoded())
        }
        return buildJsonObject {
            put("postBody", postBody)
            put("requestUri", "http://localhost")
            put("returnSecureToken", true)
            put("returnIdpCredential", true)
            if (link) session?.let { put("idToken", it.idToken) }
        }
    }

    private fun EmailActionCodeSettings.applyTo(builder: kotlinx.serialization.json.JsonObjectBuilder) {
        with(builder) {
            put("continueUrl", url)
            put("canHandleCodeInApp", canHandleCodeInApp)
            iOSBundleId?.let { put("iOSBundleId", it) }
            androidPackageName?.let {
                put("androidPackageName", it)
                put("androidInstallApp", androidInstallIfNotAvailable)
                androidMinimumVersion?.let { v -> put("androidMinimumVersion", v) }
            }
            linkDomain?.let { put("linkDomain", it) }
        }
    }

    /** Calls an Identity Toolkit endpoint and returns the parsed response. */
    @OptIn(KMPAuthInternalApi::class)
    private suspend fun call(endpoint: String, body: JsonObject): JsonObject {
        val url = "https://identitytoolkit.googleapis.com/v1/$endpoint?key=${apiKeyProvider()}"
        val responseText = transport.post(url, body.toString())
        val response = json.parseToJsonElement(responseText).jsonObject
        response["error"]?.jsonObject?.let { error ->
            val message = error["message"]?.jsonPrimitive?.content ?: "UNKNOWN_ERROR"
            currentLogger.log("Firebase REST auth error: $message")
            throw IllegalStateException("Firebase auth failed: $message")
        }
        return response
    }

    private suspend fun signedInUser(response: JsonObject, anonymous: Boolean = false): FirebaseRestUser {
        val idToken = response["idToken"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Firebase Null user")
        val providerId = response["providerId"]?.jsonPrimitive?.content
        var user = FirebaseRestUser(
            uid = response["localId"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Firebase Null user"),
            email = response["email"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            displayName = response["displayName"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            photoUrl = response["photoUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            providerId = providerId ?: "firebase",
            idToken = idToken,
            refreshToken = response["refreshToken"]?.jsonPrimitive?.content,
            isAnonymous = anonymous,
        )
        if (user.displayName == null && !anonymous) {
            // Password/oob responses omit profile fields; enrich from lookup.
            runCatching {
                val info = call("accounts:lookup", buildJsonObject { put("idToken", idToken) })
                    .get("users")?.jsonArray?.firstOrNull()?.jsonObject
                if (info != null) {
                    user = user.copy(
                        displayName = info["displayName"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                        photoUrl = info["photoUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                        email = user.email
                            ?: info["email"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
                    )
                }
            }
        }
        return user
    }
}

/** The query part of a URL, or null when there is none. */
private fun String.queryStringOrNull(): String? =
    substringAfter('?', "").substringBefore('#').takeIf { it.isNotEmpty() }

/** application/x-www-form-urlencoded percent-encoding (UTF-8). */
internal fun String.urlEncoded(): String = buildString {
    for (byte in this@urlEncoded.encodeToByteArray()) {
        val c = byte.toInt().toChar()
        when {
            c.isLetterOrDigit() && c.code < 128 -> append(c)
            c == '-' || c == '_' || c == '.' || c == '*' -> append(c)
            c == ' ' -> append('+')
            else -> {
                append('%')
                val hex = (byte.toInt() and 0xFF).toString(16).uppercase()
                if (hex.length == 1) append('0')
                append(hex)
            }
        }
    }
}
