package com.mmk.kmpauth.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handle returned by the `rememberXxxSignInState` composables. Wire
 * [launch] to any clickable and observe [isInProgress] for loading UI.
 *
 * ```
 * val googleSignIn = rememberGoogleSignInState(onResult = { user -> ... })
 *
 * Button(onClick = { googleSignIn.launch() }, enabled = !googleSignIn.isInProgress) {
 *     Text("Sign in with Google")
 * }
 * ```
 */
@Stable
public interface SignInState {

    /** True while a sign-in flow started by [launch] is still running. */
    public val isInProgress: Boolean

    /**
     * Starts the sign-in flow. Calls while a flow is already in progress
     * are ignored, so a fast double-tap cannot start two flows.
     */
    public fun launch()
}

/**
 * Shared [SignInState] implementation used by the provider modules: runs
 * [block] in [scope], guarding against concurrent launches and driving
 * [isInProgress]. [block] reads its parameters through
 * `rememberUpdatedState`-backed properties, so values current at launch
 * time are used even when the caller recomposes with new arguments.
 */
@KMPAuthInternalApi
public class LaunchingSignInState(
    private val scope: CoroutineScope,
    private val block: suspend () -> Unit,
) : SignInState {

    override var isInProgress: Boolean by mutableStateOf(false)
        private set

    override fun launch() {
        if (isInProgress) return
        scope.launch {
            isInProgress = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Flows report failures through their onResult callback; an
                // exception escaping here would otherwise crash the app via
                // the composition's coroutine scope.
                currentLogger.log("Sign-in flow failed with uncaught exception: $e")
            } finally {
                isInProgress = false
            }
        }
    }
}
