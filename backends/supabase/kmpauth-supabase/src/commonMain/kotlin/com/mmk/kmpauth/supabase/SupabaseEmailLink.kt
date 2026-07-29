package com.mmk.kmpauth.supabase

import io.github.jan.supabase.auth.OtpType

/**
 * Parsed shape of a link Supabase produces during email flows. Supabase
 * redirects (or lets the email template link) in three formats, each
 * completed differently:
 *
 * - `?token_hash=...&type=...` (recommended custom email template, or the
 *   `token=` parameter of the default `/auth/v1/verify` confirmation URL) —
 *   completed with `verifyEmailOtp(tokenHash)`.
 * - `?code=...` (PKCE flow redirect) — completed with
 *   `exchangeCodeForSession`, which needs the code verifier stored by the
 *   `sendSignInLinkToEmail` call of the same client.
 * - `#access_token=...&refresh_token=...` (implicit flow redirect) — the
 *   session is imported directly.
 */
internal sealed interface SupabaseEmailLink {

    data class TokenHash(val tokenHash: String, val otpType: OtpType.Email) : SupabaseEmailLink

    data class PkceCode(val code: String) : SupabaseEmailLink

    data class SessionTokens(val accessToken: String, val refreshToken: String) : SupabaseEmailLink

    companion object {

        /** Parses [link], or returns null when it matches none of the formats. */
        fun parse(link: String): SupabaseEmailLink? {
            val params = parameters(link)

            // token_hash from a custom template; the default verify URL
            // carries the same hash in its `token` parameter. Require the
            // `type` parameter alongside bare `token` to avoid matching
            // unrelated links.
            val tokenHash = params["token_hash"]
                ?: params["token"]?.takeIf { params.containsKey("type") }
            if (tokenHash != null) return TokenHash(tokenHash, otpTypeOf(params["type"]))

            params["code"]?.let { return PkceCode(it) }

            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"]
            if (accessToken != null && refreshToken != null) {
                return SessionTokens(accessToken, refreshToken)
            }
            return null
        }

        /** Key-value parameters from both the query and the fragment part. */
        private fun parameters(link: String): Map<String, String> = buildMap {
            val query = link.substringAfter('?', "").substringBefore('#')
            val fragment = link.substringAfter('#', "")
            (query.split('&') + fragment.split('&'))
                .filter { it.contains('=') }
                .forEach { pair ->
                    val key = pair.substringBefore('=')
                    val value = pair.substringAfter('=')
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        put(urlDecode(key), urlDecode(value))
                    }
                }
        }

        private fun urlDecode(value: String): String {
            if (!value.contains('%') && !value.contains('+')) return value
            val bytes = ArrayList<Byte>(value.length)
            var i = 0
            while (i < value.length) {
                val c = value[i]
                val hex = if (c == '%') {
                    value.drop(i + 1).take(2).takeIf { it.length == 2 }?.toIntOrNull(16)
                } else null
                when {
                    c == '+' -> {
                        bytes.add(' '.code.toByte()); i++
                    }

                    hex != null -> {
                        bytes.add(hex.toByte()); i += 3
                    }

                    else -> {
                        bytes.add(c.code.toByte()); i++
                    }
                }
            }
            return bytes.toByteArray().decodeToString()
        }

        /**
         * `EMAIL` covers both `magiclink` and `signup` in
         * `verifyEmailOtp`; the remaining types must be passed as-is.
         */
        private fun otpTypeOf(type: String?): OtpType.Email = when (type) {
            "recovery" -> OtpType.Email.RECOVERY
            "invite" -> OtpType.Email.INVITE
            "email_change" -> OtpType.Email.EMAIL_CHANGE
            else -> OtpType.Email.EMAIL
        }
    }
}
