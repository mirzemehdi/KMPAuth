package com.mmk.kmpauth.google

/**
 * Provider class for Google Authentication UI part. a.k.a [signIn]
 */
public interface GoogleAuthUiProvider {

    /**
     * Opens Sign In with Google UI, and returns [GoogleUser]
     * if sign-in was successful, otherwise, null
     * @return returns GoogleUser or null(if sign-in was not successful)
     */
    public suspend fun signIn(): GoogleUser?
}