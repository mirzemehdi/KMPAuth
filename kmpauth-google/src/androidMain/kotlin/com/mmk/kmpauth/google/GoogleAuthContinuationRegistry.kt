package com.mmk.kmpauth.google

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resumeWithException

internal object GoogleAuthContinuationRegistry {
    private var continuation: CancellableContinuation<AuthorizationResult>? = null

    fun store(c: CancellableContinuation<AuthorizationResult>) { continuation = c }

    fun deliver(ctx: Context, res: ActivityResult) {
        val cont = continuation ?: return
        continuation = null

        try {
            if (res.resultCode == Activity.RESULT_OK && res.data != null) {
                val authResult = Identity
                    .getAuthorizationClient(ctx)
                    .getAuthorizationResultFromIntent(res.data!!)
                cont.resume(authResult) { cause, _, _ -> }
            } else {
                cont.resumeWithException(
                    kotlin.coroutines.cancellation.CancellationException("User cancelled authorization")
                )
            }
        } catch (t: Throwable) {
            cont.resumeWithException(t)
        }
    }
}