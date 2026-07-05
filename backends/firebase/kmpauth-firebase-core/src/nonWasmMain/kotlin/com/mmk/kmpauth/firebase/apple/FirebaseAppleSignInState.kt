package com.mmk.kmpauth.firebase.apple

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import dev.gitlive.firebase.auth.FirebaseUser

/**
 * Apple Sign-In with Firebase as a Compose state holder.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * //Apple Sign-In with Custom Button and authentication with Firebase
 * val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
 *
 * Button(onClick = { appleSignIn.launch() }) { Text("Apple Sign-In (Custom Design)") }
 * ```
 *
 * @param requestScopes list of request scopes type of [AppleSignInRequestScope].
 * @param linkAccount boolean value to link account with existing account. Default value is false
 * @param onResult receives the signed-in [FirebaseUser] or the failure.
 */
@Composable
public expect fun rememberFirebaseAppleSignInState(
    requestScopes: List<AppleSignInRequestScope> = listOf(
        AppleSignInRequestScope.FullName,
        AppleSignInRequestScope.Email
    ),
    linkAccount: Boolean = false,
    onResult: (Result<FirebaseUser?>) -> Unit,
): SignInState
