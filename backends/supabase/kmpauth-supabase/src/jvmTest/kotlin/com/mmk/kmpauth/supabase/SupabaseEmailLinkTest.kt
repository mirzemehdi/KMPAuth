package com.mmk.kmpauth.supabase

import io.github.jan.supabase.auth.OtpType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Locks the recognized Supabase email-link shapes and their OTP types. */
class SupabaseEmailLinkTest {

    @Test
    fun parsesTokenHashLinkFromCustomEmailTemplate() {
        val parsed = SupabaseEmailLink.parse(
            "https://example.com/auth/confirm?token_hash=pkce_abc123&type=magiclink"
        )
        val tokenHash = assertIs<SupabaseEmailLink.TokenHash>(parsed)
        assertEquals("pkce_abc123", tokenHash.tokenHash)
        assertEquals(OtpType.Email.EMAIL, tokenHash.otpType)
    }

    @Test
    fun parsesDefaultVerifyUrlTokenParameter() {
        val parsed = SupabaseEmailLink.parse(
            "https://xyz.supabase.co/auth/v1/verify?token=hash-token&type=signup&redirect_to=https%3A%2F%2Fexample.com"
        )
        val tokenHash = assertIs<SupabaseEmailLink.TokenHash>(parsed)
        assertEquals("hash-token", tokenHash.tokenHash)
        assertEquals(OtpType.Email.EMAIL, tokenHash.otpType)
    }

    @Test
    fun mapsRecoveryTypeToRecoveryOtp() {
        val parsed = SupabaseEmailLink.parse(
            "https://example.com/confirm?token_hash=abc&type=recovery"
        )
        assertEquals(OtpType.Email.RECOVERY, assertIs<SupabaseEmailLink.TokenHash>(parsed).otpType)
    }

    @Test
    fun parsesPkceCodeRedirect() {
        val parsed = SupabaseEmailLink.parse("https://example.com/finish?code=uuid-code")
        assertEquals("uuid-code", assertIs<SupabaseEmailLink.PkceCode>(parsed).code)
    }

    @Test
    fun parsesImplicitFlowFragmentTokens() {
        val parsed = SupabaseEmailLink.parse(
            "https://example.com/finish#access_token=at&refresh_token=rt&expires_in=3600&token_type=bearer"
        )
        val tokens = assertIs<SupabaseEmailLink.SessionTokens>(parsed)
        assertEquals("at", tokens.accessToken)
        assertEquals("rt", tokens.refreshToken)
    }

    @Test
    fun bareTokenWithoutTypeIsNotAnEmailLink() {
        assertNull(SupabaseEmailLink.parse("https://example.com/page?token=csrf-token"))
    }

    @Test
    fun ordinaryLinksAreNotEmailLinks() {
        assertNull(SupabaseEmailLink.parse("https://example.com/"))
        assertNull(SupabaseEmailLink.parse("https://example.com/docs?query=auth#section"))
    }

    @Test
    fun urlEncodedValuesAreDecoded() {
        val parsed = SupabaseEmailLink.parse(
            "https://example.com/confirm?token_hash=a%2Bb%20c&type=magiclink"
        )
        assertEquals("a+b c", assertIs<SupabaseEmailLink.TokenHash>(parsed).tokenHash)
    }
}
