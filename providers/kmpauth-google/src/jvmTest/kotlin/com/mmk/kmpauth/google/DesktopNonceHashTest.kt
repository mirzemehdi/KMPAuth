package com.mmk.kmpauth.google

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the nonce contract behind the Desktop Google -> Supabase fix (#235):
 * KMPAuth sends the SHA-256 hex of the raw nonce to Google (which echoes it
 * into the ID token's `nonce` claim) and forwards the RAW nonce to the
 * backend; Supabase's id_token grant recomputes the same hash from that raw
 * value and compares it with the claim. Both sides must agree byte-for-byte.
 */
class DesktopNonceHashTest {

    @Test
    fun sha256HexMatchesKnownVector() {
        // Ground truth: python hashlib.sha256("kmpauth-test-nonce").hexdigest()
        assertEquals(
            "7111f45f1e423d4d698f6befcf56dd1fb6bca44ef42f13970545da9ce146e88f",
            sha256Hex("kmpauth-test-nonce"),
        )
    }

    @Test
    fun sha256HexIsLowercase64Chars() {
        val hex = sha256Hex("another-raw-nonce")
        assertEquals(64, hex.length)
        assertEquals(hex.lowercase(), hex)
    }

    @Test
    fun sha256HexZeroPadsLeadingLowByte() {
        // "pad5" hashes to a digest whose first byte is 0x06 - the leading
        // zero nibble must survive.
        assertEquals(
            "06630832316f2ab221957b8053772ca61255ea562afe4287097626f6ad91311b",
            sha256Hex("pad5"),
        )
    }
}
