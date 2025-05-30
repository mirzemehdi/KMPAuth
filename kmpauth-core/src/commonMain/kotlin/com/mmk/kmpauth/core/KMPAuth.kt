package com.mmk.kmpauth.core

import com.mmk.kmpauth.core.logger.KMPAuthLogger
import com.mmk.kmpauth.core.logger.currentLogger

@OptIn(KMPAuthInternalApi::class)
public object KMPAuth {
    public fun setLogger(logger: KMPAuthLogger) {
        currentLogger = logger
    }
}