package com.mmk.kmpauth.core.di

import org.koin.dsl.module


public actual fun isAndroidPlatform(): Boolean = false

internal actual val platformModule = module {

}