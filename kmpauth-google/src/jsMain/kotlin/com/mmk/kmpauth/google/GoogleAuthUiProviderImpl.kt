package com.mmk.kmpauth.google

internal class GoogleAuthUiProviderImpl : GoogleAuthUiProvider {
    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        TODO("Not yet implemented")
    }

}