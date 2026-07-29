package com.mmk.kmpauth.core.auth

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.KMPAuthInternalApi

@OptIn(KMPAuthInternalApi::class)
@Composable
internal actual fun phoneAuthPlatformUiContext(): Any? =
    AndroidActivityTracker.currentActivity
