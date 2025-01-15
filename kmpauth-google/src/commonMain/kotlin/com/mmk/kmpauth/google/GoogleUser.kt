package com.mmk.kmpauth.google

/**
 * GoogleUser class holds most necessary fields
 */
public data class GoogleUser(
    val idToken: String,
    val accessToken:String?=null,
    val displayName: String = "",
    val profilePicUrl: String? = null,
)