package com.mmk.kmpauth.core.auth

import androidx.compose.runtime.Composable

@Composable
internal actual fun phoneAuthPlatformUiContext(): Any? =
    AndroidActivityTracker.currentActivity
