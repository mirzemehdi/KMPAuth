package com.mmk.kmpauth.core.di

import org.koin.core.Koin
import org.koin.core.component.KoinComponent

internal abstract class KMPKoinComponent : KoinComponent {
    override fun getKoin(): Koin {
        requireNotNull(LibDependencyInitializer.koinApp){
            "Make sure you invoked #initialize method"
        }
        return LibDependencyInitializer.koinApp?.koin!!
    }
}