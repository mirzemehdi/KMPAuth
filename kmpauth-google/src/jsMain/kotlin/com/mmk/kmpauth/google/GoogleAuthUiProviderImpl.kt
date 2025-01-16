package com.mmk.kmpauth.google

internal class GoogleAuthUiProviderImpl : GoogleAuthUiProvider {
    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        TODO("Not yet implemented")
    }

}