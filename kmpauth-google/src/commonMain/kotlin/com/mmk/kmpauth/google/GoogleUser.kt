package com.mmk.kmpauth.google

public data class GoogleUser(
    val idToken: String,
    val displayName: String = "",
    val profilePicUrl: String? = null,
)