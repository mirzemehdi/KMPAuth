package com.mmk.kmpauth.core.di

import org.koin.core.module.Module

public expect fun isAndroidPlatform(): Boolean
internal expect val platformModule: Module