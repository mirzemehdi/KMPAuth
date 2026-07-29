package com.mmk.kmpauth.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but rethrows [CancellationException] instead of
 * capturing it, so coroutine cancellation keeps propagating. Catching a
 * cancellation into a failed [Result] would keep a cancelled flow running
 * and report a bogus failure to the caller.
 */
@KMPAuthInternalApi
public inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
