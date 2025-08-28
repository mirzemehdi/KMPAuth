package com.mmk.kmpauth.core.logger

import com.mmk.kmpauth.core.KMPAuthInternalApi

/**
 * Global logger interface for the KMPAuth library.
 */
public fun interface KMPAuthLogger {
    /**
     * Log a message.
     *
     * @param message The message to log
     */
    public fun log(message: String?)
}

/**
 * Default empty logger implementation that doesn't log anything.
 */
internal object EmptyLogger : KMPAuthLogger {
    override fun log(message: String?) {
        // Empty
    }
}

/**
 * The current logger used by the library.
 */
@KMPAuthInternalApi
public var currentLogger: KMPAuthLogger = EmptyLogger