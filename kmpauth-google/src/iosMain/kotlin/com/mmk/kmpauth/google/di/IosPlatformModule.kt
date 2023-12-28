package com.mmk.kmpauth.google.di

import com.mmk.kmpauth.google.GoogleAuthProvider
import com.mmk.kmpauth.google.GoogleAuthProviderImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module


internal actual val googleAuthPlatformModule: Module = module {
    factoryOf(::GoogleAuthProviderImpl) bind GoogleAuthProvider::class
}