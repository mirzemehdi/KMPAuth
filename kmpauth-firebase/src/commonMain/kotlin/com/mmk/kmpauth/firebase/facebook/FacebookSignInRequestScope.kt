package com.mmk.kmpauth.firebase.facebook

/***
 * Facebook Sign in Request Scope that can be requested from user when first time user signup.
 * You can request from user [FacebookSignInRequestScope.PublicProfile] and [FacebookSignInRequestScope.Email]
 */
public sealed interface FacebookSignInRequestScope {
    /**
     * Request scope for user's public profile information
     */
    public data object PublicProfile : FacebookSignInRequestScope

    /**
     * Request scope for user's email
     */
    public data object Email : FacebookSignInRequestScope
}