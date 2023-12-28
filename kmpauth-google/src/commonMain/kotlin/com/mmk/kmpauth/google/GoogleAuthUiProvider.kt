package com.mmk.kmpauth.google

public interface GoogleAuthUiProvider {

    /**
     * Opens Sign In with Google UI,
     * @return returns GoogleUser
     */
    public suspend fun signIn(): GoogleUser?
}