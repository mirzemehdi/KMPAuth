package com.mmk.kmpauth.core.di

import com.mmk.kmpauth.core.KMPAuthInternalApi
import org.koin.core.Koin
import org.koin.core.component.KoinComponent

@KMPAuthInternalApi
public abstract class KMPKoinComponent : KoinComponent {
    override fun getKoin(): Koin {
        requireNotNull(LibDependencyInitializer.koinApp) {
            "Make sure you invoked #initialize method"
        }
        return LibDependencyInitializer.koinApp?.koin!!
    }
}