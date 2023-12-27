package com.mmk.kmpauth.core.di

import org.koin.dsl.module


internal actual fun isAndroidPlatform(): Boolean = false

internal actual val platformModule = module {

}