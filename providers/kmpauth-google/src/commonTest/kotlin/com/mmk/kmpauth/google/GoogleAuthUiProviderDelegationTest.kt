package com.mmk.kmpauth.google

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization tests locking the overload-delegation contract of
 * [GoogleAuthUiProvider]. The no-arg and two-arg [GoogleAuthUiProvider.signIn]
 * overloads are default interface methods that must forward to the full
 * overload with these exact defaults — user code and the Firebase containers
 * rely on them.
 */
class GoogleAuthUiProviderDelegationTest {

    private class RecordingProvider : GoogleAuthUiProvider {
        var filterByAuthorizedAccounts: Boolean? = null
        var isAutoSelectEnabled: Boolean? = null
        var scopes: List<String>? = null

        override suspend fun signIn(
            filterByAuthorizedAccounts: Boolean,
            isAutoSelectEnabled: Boolean,
            scopes: List<String>,
        ): GoogleUser? {
            this.filterByAuthorizedAccounts = filterByAuthorizedAccounts
            this.isAutoSelectEnabled = isAutoSelectEnabled
            this.scopes = scopes
            return null
        }
    }

    @Test
    fun noArgSignInUsesDocumentedDefaults() = runTest {
        val provider = RecordingProvider()

        provider.signIn()

        assertEquals(false, provider.filterByAuthorizedAccounts)
        assertEquals(true, provider.isAutoSelectEnabled)
        assertEquals(listOf("email", "profile"), provider.scopes)
    }

    @Test
    fun twoArgSignInForwardsArgsAndDefaultScopes() = runTest {
        val provider = RecordingProvider()

        provider.signIn(filterByAuthorizedAccounts = true, isAutoSelectEnabled = false)

        assertEquals(true, provider.filterByAuthorizedAccounts)
        assertEquals(false, provider.isAutoSelectEnabled)
        assertEquals(listOf("email", "profile"), provider.scopes)
    }

    @Test
    fun twoArgSignInDefaultsAutoSelectToTrue() = runTest {
        val provider = RecordingProvider()

        provider.signIn(filterByAuthorizedAccounts = true)

        assertEquals(true, provider.filterByAuthorizedAccounts)
        assertEquals(true, provider.isAutoSelectEnabled)
        assertEquals(listOf("email", "profile"), provider.scopes)
    }

    @Test
    fun fullOverloadPassesCustomScopesThrough() = runTest {
        val provider = RecordingProvider()
        val customScopes = listOf("email", "profile", "https://www.googleapis.com/auth/drive.readonly")

        provider.signIn(
            filterByAuthorizedAccounts = false,
            isAutoSelectEnabled = true,
            scopes = customScopes,
        )

        assertEquals(customScopes, provider.scopes)
    }
}
