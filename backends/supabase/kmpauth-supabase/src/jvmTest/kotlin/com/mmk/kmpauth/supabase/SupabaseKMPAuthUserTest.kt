package com.mmk.kmpauth.supabase

import com.mmk.kmpauth.core.KMPAuthInternalApi
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Locks the UserInfo -> KMPAuthUser field mapping, including fallbacks. */
@OptIn(KMPAuthInternalApi::class)
class SupabaseKMPAuthUserTest {

    @Test
    fun mapsMetadataFields() {
        val userInfo = UserInfo(
            id = "uid-1",
            aud = "authenticated",
            email = "user@example.com",
            appMetadata = buildJsonObject { put("provider", "google") },
            userMetadata = buildJsonObject {
                put("full_name", "Full Name")
                put("avatar_url", "https://example.com/avatar.png")
            },
        )

        val user = SupabaseKMPAuthUser(userInfo)
        assertEquals("uid-1", user.uid)
        assertEquals("user@example.com", user.email)
        assertEquals("Full Name", user.displayName)
        assertEquals("https://example.com/avatar.png", user.photoUrl)
        assertEquals("google", user.providerId)
        assertSame(userInfo, user.raw)
    }

    @Test
    fun fallsBackToAlternateMetadataKeys() {
        val user = SupabaseKMPAuthUser(
            UserInfo(
                id = "uid-2",
                aud = "authenticated",
                userMetadata = buildJsonObject {
                    put("name", "Alt Name")
                    put("picture", "https://example.com/p.png")
                },
            )
        )

        assertEquals("Alt Name", user.displayName)
        assertEquals("https://example.com/p.png", user.photoUrl)
    }

    @Test
    fun missingMetadataMapsToNulls() {
        val user = SupabaseKMPAuthUser(UserInfo(id = "uid-3", aud = "authenticated"))

        assertNull(user.email)
        assertNull(user.displayName)
        assertNull(user.photoUrl)
        assertNull(user.providerId)
    }
}
