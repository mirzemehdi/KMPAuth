package com.mmk.kmpauth.core.di

import android.content.Context
import androidx.startup.Initializer
import com.mmk.kmpauth.core.KMPAuthInternalApi

/**
 * Application context captured automatically at process start via
 * androidx.startup. Library-internal wiring only — not for consumer use.
 */
@KMPAuthInternalApi
public lateinit var applicationContext: Context
    private set

public class KMPAuthContextInitializer : Initializer<Unit> {
    @OptIn(KMPAuthInternalApi::class)
    override fun create(context: Context) {
        applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}


@KMPAuthInternalApi
public actual fun isAndroidPlatform(): Boolean = true
