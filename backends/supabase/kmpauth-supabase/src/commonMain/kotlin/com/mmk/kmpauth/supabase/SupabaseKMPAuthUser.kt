package com.mmk.kmpauth.supabase

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthUser
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * [KMPAuthUser] view over a Supabase user. The native
 * [io.github.jan.supabase.auth.user.UserInfo] stays reachable through [raw].
 *
 * [displayName] and [photoUrl] are read from the user metadata keys the
 * Supabase social providers populate (`full_name`/`name` and
 * `avatar_url`/`picture`). [providerId] is Supabase's provider name from the
 * app metadata (`"google"`, `"apple"`, `"facebook"`, `"email"`, ...) — note
 * this differs from the Firebase-style domain ids in
 * [com.mmk.kmpauth.core.auth.AuthProviderIds].
 */
@KMPAuthInternalApi
public class SupabaseKMPAuthUser(private val user: UserInfo) : KMPAuthUser {
    override val uid: String get() = user.id
    override val email: String? get() = user.email
    override val displayName: String?
        get() = user.userMetadata.stringOrNull("full_name", "name")
    override val photoUrl: String?
        get() = user.userMetadata.stringOrNull("avatar_url", "picture")
    override val providerId: String?
        get() = user.appMetadata.stringOrNull("provider")
    override val raw: Any get() = user

    private fun JsonObject?.stringOrNull(vararg keys: String): String? {
        if (this == null) return null
        return keys.firstNotNullOfOrNull { key ->
            (get(key) as? JsonPrimitive)?.contentOrNull
        }
    }
}
