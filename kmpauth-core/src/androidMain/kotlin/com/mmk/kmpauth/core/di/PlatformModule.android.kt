package com.mmk.kmpauth.core.di

import android.content.Context
import androidx.startup.Initializer
import org.koin.dsl.module

internal lateinit var applicationContext: Context
    private set

internal class ContextInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}


internal actual fun isAndroidPlatform(): Boolean = true
internal actual val platformModule = module {
    single { applicationContext }
}


