package com.mmk.kmpauth.google

/**
 * Provider class for Google Authentication UI part. a.k.a [signIn]
 */
public interface GoogleAuthUiProvider {

    public companion object {
        internal val BASIC_AUTH_SCOPE = listOf("email", "profile")
    }

    /**
     * Opens Sign In with Google UI and returns the signed-in [GoogleUser].
     * By default all available accounts are listed to choose from
     * @see signIn(filterByAuthorizedAccounts: Boolean)
     * @return a successful [Result] with the [GoogleUser], or a failed [Result]
     * carrying the reason - including when the user dismisses the chooser.
     */
    public suspend fun signIn(): Result<GoogleUser> =
        signIn(
            filterByAuthorizedAccounts = false,
            isAutoSelectEnabled = true,
            scopes = BASIC_AUTH_SCOPE,
            requestAccessToken = false
        )


    /**
     * @param filterByAuthorizedAccounts set to true so users can choose between available accounts to sign in.
     * setting to false list any accounts that have previously been used to sign in to your app. Default value is false.
     * @param isAutoSelectEnabled If `true`, the user will be automatically signed in
     * without showing the account chooser when there is exactly one eligible account.
     * If `false`, the account chooser will be shown even if only one account is available. Default value is true
     */
    public suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean = true
    ): Result<GoogleUser> =
        signIn(
            filterByAuthorizedAccounts = filterByAuthorizedAccounts,
            isAutoSelectEnabled = isAutoSelectEnabled,
            scopes = BASIC_AUTH_SCOPE,
            requestAccessToken = false
        )


    /**
     * @param filterByAuthorizedAccounts set to true so users can choose between available accounts to sign in.
     * setting to false list any accounts that have previously been used to sign in to your app. Default value is false.
     * @param isAutoSelectEnabled If `true`, the user will be automatically signed in
     * without showing the account chooser when there is exactly one eligible account.
     * If `false`, the account chooser will be shown even if only one account is available. Default value is true
     * @param scopes Custom scopes to retrieve more information. Default value listOf("email", "profile")
     * @param requestAccessToken Request an OAuth access token alongside the ID
     * token. **Only affects Android**, where Credential Manager returns an ID
     * token alone and an access token needs a separate authorization request
     * with its own consent prompt; every other platform already returns one.
     * Asking for scopes beyond `email`/`profile` implies this. See
     * [GoogleUser.accessToken].
     * @return a successful [Result] with the [GoogleUser], or a failed [Result]
     * carrying the reason - e.g. the platform's credential exception, or the
     * user cancelling.
     */
    public suspend fun signIn(
        filterByAuthorizedAccounts: Boolean = false,
        isAutoSelectEnabled: Boolean = true,
        scopes: List<String> = BASIC_AUTH_SCOPE,
        requestAccessToken: Boolean = false
    ): Result<GoogleUser>
}