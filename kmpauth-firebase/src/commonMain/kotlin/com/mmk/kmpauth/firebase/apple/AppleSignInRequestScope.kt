package com.mmk.kmpauth.firebase.apple

public sealed interface AppleSignInRequestScope {
    public data object FullName : AppleSignInRequestScope
    public data object Email : AppleSignInRequestScope
}