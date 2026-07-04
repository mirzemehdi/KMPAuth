package com.mmk.kmpauth.core

import com.mmk.kmpauth.core.logger.KMPAuthLogger
import com.mmk.kmpauth.core.logger.currentLogger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization tests locking the 2.x logging contract:
 * [KMPAuth.setLogger] replaces the library-wide logger, and the logger
 * receives messages verbatim (including null).
 */
@OptIn(KMPAuthInternalApi::class)
class LoggerContractTest {

    private lateinit var loggerBefore: KMPAuthLogger

    @BeforeTest
    fun saveLogger() {
        loggerBefore = currentLogger
    }

    @AfterTest
    fun restoreLogger() {
        currentLogger = loggerBefore
    }

    @Test
    fun setLoggerRoutesMessagesToProvidedLogger() {
        val received = mutableListOf<String?>()
        KMPAuth.setLogger { message -> received.add(message) }

        currentLogger.log("hello")
        currentLogger.log(null)

        assertEquals(listOf("hello", null), received)
    }

    @Test
    fun setLoggerReplacesPreviousLogger() {
        val first = mutableListOf<String?>()
        val second = mutableListOf<String?>()
        KMPAuth.setLogger { first.add(it) }
        KMPAuth.setLogger { second.add(it) }

        currentLogger.log("only-second")

        assertEquals(emptyList(), first)
        assertEquals(listOf<String?>("only-second"), second)
    }
}
