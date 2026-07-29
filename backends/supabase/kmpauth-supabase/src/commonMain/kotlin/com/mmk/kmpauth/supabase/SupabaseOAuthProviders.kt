package com.mmk.kmpauth.supabase

import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Azure
import io.github.jan.supabase.auth.providers.Bitbucket
import io.github.jan.supabase.auth.providers.Discord
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Figma
import io.github.jan.supabase.auth.providers.Fly
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Gitlab
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.Keycloak
import io.github.jan.supabase.auth.providers.LinkedInOIDC
import io.github.jan.supabase.auth.providers.Notion
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.providers.SlackOIDC
import io.github.jan.supabase.auth.providers.Spotify
import io.github.jan.supabase.auth.providers.Twitch
import io.github.jan.supabase.auth.providers.Twitter
import io.github.jan.supabase.auth.providers.WorkOS
import io.github.jan.supabase.auth.providers.Zoom

/**
 * Maps a KMPAuth provider id to supabase-kt's [OAuthProvider] singleton.
 * Accepts both the Firebase-style ids the shared auth states use
 * (`github.com`, `microsoft.com`) and GoTrue's own provider names
 * (`github`, `azure`), so the same `AuthCredential.OAuthWebFlow` works
 * against either backend.
 */
internal fun supabaseOAuthProviderOrNull(providerId: String): OAuthProvider? =
    when (providerId.lowercase().removeSuffix(".com")) {
        "github" -> Github
        "microsoft", "azure" -> Azure
        "google" -> Google
        "apple" -> Apple
        "facebook" -> Facebook
        "bitbucket" -> Bitbucket
        "discord" -> Discord
        "figma" -> Figma
        "fly", "fly.io" -> Fly
        "gitlab" -> Gitlab
        "kakao" -> Kakao
        "keycloak" -> Keycloak
        "linkedin", "linkedin_oidc" -> LinkedInOIDC
        "notion" -> Notion
        "slack", "slack_oidc" -> SlackOIDC
        "spotify" -> Spotify
        "twitch" -> Twitch
        "twitter" -> Twitter
        "workos" -> WorkOS
        "zoom" -> Zoom
        else -> null
    }
