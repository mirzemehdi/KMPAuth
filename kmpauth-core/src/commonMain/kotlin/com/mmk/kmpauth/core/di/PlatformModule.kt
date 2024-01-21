package com.mmk.kmpauth.core.di

import com.mmk.kmpauth.core.KMPAuthInternalApi
import org.koin.core.module.Module

@KMPAuthInternalApi
public expect fun isAndroidPlatform(): Boolean
internal expect val platformModule: Module