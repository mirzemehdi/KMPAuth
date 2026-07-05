package com.mmk.kmpauth.firebase.apple

/***
 * Apple Sign in Request Scope that can be requested from user when first time user signup.
 * You can request from user [AppleSignInRequestScope.FullName] and [AppleSignInRequestScope.Email]
 */
public sealed interface AppleSignInRequestScope {
    /**
     * Request scope for user's fullname
     */
    public data object FullName : AppleSignInRequestScope

    /**
     * Request scope for user's email
     */
    public data object Email : AppleSignInRequestScope
}