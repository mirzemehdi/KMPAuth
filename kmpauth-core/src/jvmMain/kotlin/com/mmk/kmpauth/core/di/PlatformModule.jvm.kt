package com.mmk.kmpauth.core.di

import com.mmk.kmpauth.core.KMPAuthInternalApi
import org.koin.core.module.Module
import org.koin.dsl.module

@KMPAuthInternalApi
public actual fun isAndroidPlatform(): Boolean = false
internal actual val platformModule: Module = module { }

