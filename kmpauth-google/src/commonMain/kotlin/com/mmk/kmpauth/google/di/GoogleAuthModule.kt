package com.mmk.kmpauth.google.di

import com.mmk.kmpauth.google.GoogleAuthCredentials
import org.koin.core.module.Module
import org.koin.dsl.module


internal fun googleAuthModule(credentials: GoogleAuthCredentials) = module {
    factory { credentials }
    includes(googleAuthPlatformModule)
}

internal expect val googleAuthPlatformModule: Module

