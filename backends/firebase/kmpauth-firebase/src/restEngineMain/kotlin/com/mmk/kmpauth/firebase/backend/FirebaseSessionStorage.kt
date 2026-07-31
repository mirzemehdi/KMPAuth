package com.mmk.kmpauth.firebase.backend

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Persists the REST engine's session across process restarts, mirroring
 * what the native Firebase SDKs do with the keychain / SharedPreferences.
 * Keyed by API key so two Firebase projects don't clobber each other.
 */
internal interface FirebaseSessionStorage {
    fun load(key: String): String?

    /** null [value] clears the stored session. */
    fun save(key: String, value: String?)
}

/**
 * Platform default: a file under `~/.kmpauth/` on Desktop (JVM),
 * `localStorage` on wasm. Null disables persistence.
 */
internal expect fun defaultFirebaseSessionStorage(): FirebaseSessionStorage?

/**
 * Session JSON codec. Only sessions with a refresh token are worth
 * persisting — a bare ID token expires within the hour and cannot be
 * renewed, which would restore a dead session.
 */
internal fun FirebaseRestUser.toSessionJson(): String? {
    if (refreshToken == null) return null
    return buildJsonObject {
        put("uid", uid)
        email?.let { put("email", it) }
        displayName?.let { put("displayName", it) }
        photoUrl?.let { put("photoUrl", it) }
        providerId?.let { put("providerId", it) }
        put("idToken", idToken)
        put("refreshToken", refreshToken)
        put("isAnonymous", isAnonymous)
        putJsonArray("providerIds") { providerIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
    }.toString()
}

internal fun sessionFromJson(raw: String): FirebaseRestUser? = runCatching {
    val obj = Json.parseToJsonElement(raw).jsonObject
    FirebaseRestUser(
        uid = obj["uid"]!!.jsonPrimitive.content,
        email = obj["email"]?.jsonPrimitive?.content,
        displayName = obj["displayName"]?.jsonPrimitive?.content,
        photoUrl = obj["photoUrl"]?.jsonPrimitive?.content,
        providerId = obj["providerId"]?.jsonPrimitive?.content,
        idToken = obj["idToken"]!!.jsonPrimitive.content,
        refreshToken = obj["refreshToken"]!!.jsonPrimitive.content,
        isAnonymous = obj["isAnonymous"]?.jsonPrimitive?.content == "true",
        providerIds = obj["providerIds"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
        // The stored ID token has almost certainly expired; refresh before
        // first use.
        stale = true,
    )
}.getOrNull()
