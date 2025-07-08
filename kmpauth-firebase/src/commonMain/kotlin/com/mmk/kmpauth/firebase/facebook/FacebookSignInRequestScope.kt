package com.mmk.kmpauth.firebase.facebook

/***
 * Apple Sign in Request Scope that can be requested from user when first time user signup.
 * You can request from user [FacebookSignInRequestScope.FullName] and [FacebookSignInRequestScope.Email]
 */
public sealed interface FacebookSignInRequestScope {
    /**
     * Request scope for user's fullname
     */
    public data object FullName : FacebookSignInRequestScope

    /**
     * Request scope for user's email
     */
    public data object Email : FacebookSignInRequestScope
}