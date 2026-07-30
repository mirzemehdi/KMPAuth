package com.mmk.kmpauth.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.apple.rememberAppleSignInState
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.EmailAuthMode
import com.mmk.kmpauth.core.auth.LocalKMPAuthBackend
import com.mmk.kmpauth.core.auth.ProvideKMPAuthBackend
import com.mmk.kmpauth.supabase.SUPABASE_BACKEND_ID
import androidx.compose.runtime.CompositionLocalProvider
import com.mmk.kmpauth.core.auth.KMPAuthRecentLoginRequiredException
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.KMPAuthUserCollisionException
import com.mmk.kmpauth.core.auth.rememberAnonymousAuthState
import com.mmk.kmpauth.core.auth.rememberEmailAuthState
import com.mmk.kmpauth.core.auth.rememberOAuthState
import com.mmk.kmpauth.facebook.FacebookLoginTracking
import com.mmk.kmpauth.facebook.rememberFacebookAuthState
import com.mmk.kmpauth.facebook.rememberFacebookSignInState
import com.mmk.kmpauth.apple.rememberAppleAuthState
import com.mmk.kmpauth.core.auth.rememberGithubAuthState
import com.mmk.kmpauth.core.auth.rememberMicrosoftAuthState
import com.mmk.kmpauth.core.auth.rememberPhoneAuthState
import com.mmk.kmpauth.google.rememberGoogleAuthState
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.google.rememberGoogleSignInState
import com.mmk.kmpauth.uihelper.SignInButtonIconAlignment
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import com.mmk.kmpauth.uihelper.apple.AppleSignInButtonIconOnly
import com.mmk.kmpauth.uihelper.facebook.FacebookSignInButton
import com.mmk.kmpauth.uihelper.facebook.FacebookSignInButtonIconOnly
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.mmk.kmpauth.uihelper.google.GoogleSignInButtonIconOnly
import kotlinx.coroutines.launch

/**
 * Exercises every KMPAuth feature, grouped for end-to-end testing:
 * provider-only credentials, the Firebase backend (registered default),
 * the Supabase backend (standalone instance running side by side), and the
 * pre-styled UiHelper buttons.
 */
@Composable
fun App() {
    MaterialTheme {
        var status by remember { mutableStateOf("Not signed in") }
        val report: (source: String) -> (Result<KMPAuthUser>) -> Unit = { source ->
            { result ->
                status = result.fold(
                    onSuccess = { user ->
                        "$source: signed in as ${user.displayName ?: user.email ?: user.uid}"
                    },
                    onFailure = { error ->
                        // The typed core exceptions make the two well-known
                        // conditions detectable without message matching.
                        when (error) {
                            is KMPAuthUserCollisionException ->
                                "$source: this identity already has an account - " +
                                    "sign in WITHOUT the link checkbox to use it. (${error.message})"

                            is KMPAuthRecentLoginRequiredException ->
                                "$source: session too old - press Reauthenticate " +
                                    "(auto-detect) and retry. (${error.message})"

                            else -> "$source failed: ${error.message}"
                        }
                    },
                )
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(status = status, onStatus = { status = it })
            ProviderOnlySection(onStatus = { status = it })
            FirebaseSection(report = report, onStatus = { status = it })
            SupabaseSection(report = report, onStatus = { status = it })
            UiHelperSection(report = report)
        }
    }
}

// ---------------------------------------------------------------------------
// Status + section scaffolding
// ---------------------------------------------------------------------------

@Composable
private fun StatusCard(status: String, onStatus: (String) -> Unit) {
    // Both backends were registered in KMPAuth.initialize { } - the
    // registry hands back either one by id.
    val supabaseBackend = KMPAuth.requireBackendProvider(SUPABASE_BACKEND_ID)

    val scope = rememberCoroutineScope()
    Section(title = "Status") {
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Firebase user: ${KMPAuth.currentUser()?.uid ?: "-"} · " +
                "Supabase user: ${supabaseBackend.currentUser()?.uid ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    KMPAuth.signOut()
                    onStatus("Signed out of Firebase")
                }
            }) { Text("Sign out (Firebase)") }
            OutlinedButton(onClick = {
                scope.launch {
                    supabaseBackend.signOut()
                    onStatus("Signed out of Supabase")
                }
            }) { Text("Sign out (Supabase)") }
        }
        // Deletes the default-backend (Firebase) user. A stale session fails
        // with the typed KMPAuthRecentLoginRequiredException - reauthenticate
        // with a fresh credential, then retry.
        OutlinedButton(onClick = {
            scope.launch {
                KMPAuth.deleteAccount().fold(
                    onSuccess = { onStatus("Account deleted") },
                    onFailure = { error ->
                        onStatus(
                            if (error is KMPAuthRecentLoginRequiredException) {
                                "Delete needs a recent sign-in: press Reauthenticate " +
                                    "(auto-detect) in the Firebase section, then delete again."
                            } else {
                                "Delete account failed: ${error.message}"
                            }
                        )
                    },
                )
            }
        }) { Text("Delete account (Firebase)") }
    }
}

/**
 * Launch button driven by the state's observable [SignInState.isInProgress]:
 * disabled with a spinner while the flow runs, so double-taps are impossible
 * and the user sees something is happening.
 */
@Composable
private fun AuthButton(state: SignInState, label: String) {
    Button(onClick = { state.launch() }, enabled = !state.isInProgress) {
        if (state.isInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Text(label)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Provider-only credentials (no backend session)
// ---------------------------------------------------------------------------

@Composable
private fun ProviderOnlySection(onStatus: (String) -> Unit) {
    Section(title = "Provider only (credential, no backend)") {
        val googleSignIn = rememberGoogleSignInState(onResult = { result ->
            onStatus(result.fold(
                onSuccess = { "Google credential: ${it.displayName} (idToken received)" },
                onFailure = { "Google credential failed: ${it.message}" },
            ))
        })
        AuthButton(googleSignIn, "Google credential")

        // Access token needs a separate consent prompt on Android (#90/#129).
        val googleWithAccessToken = rememberGoogleSignInState(
            requestAccessToken = true,
            onResult = { result ->
                onStatus(result.fold(
                    onSuccess = { "Google credential: accessToken=${it.accessToken?.take(8)}..." },
                    onFailure = { "Google credential failed: ${it.message}" },
                ))
            },
        )
        AuthButton(googleWithAccessToken, "Google + access token")

        // Apple platforms only; no-op state elsewhere.
        val appleSignIn = rememberAppleSignInState(onResult = { result ->
            onStatus(result.fold(
                onSuccess = { "Apple credential: ${it.email ?: it.userId} (idToken received)" },
                onFailure = { "Apple credential failed: ${it.message}" },
            ))
        })
        AuthButton(appleSignIn, "Apple credential (iOS)")

        val facebookSignIn = rememberFacebookSignInState(onResult = { result ->
            onStatus(result.fold(
                onSuccess = { "Facebook credential: token received" },
                onFailure = { "Facebook credential failed: ${it.message}" },
            ))
        })
        AuthButton(facebookSignIn, "Facebook credential")

        // Classic login returns a Graph-API access token instead of the
        // Limited-Login OIDC JWT (counts as tracking on iOS).
        val facebookClassic = rememberFacebookSignInState(
            loginTracking = FacebookLoginTracking.Enabled,
            onResult = { result ->
                onStatus(result.fold(
                    onSuccess = { "Facebook classic: Graph access token received" },
                    onFailure = { "Facebook classic failed: ${it.message}" },
                ))
            },
        )
        AuthButton(facebookClassic, "Facebook classic (Graph token)")
    }
}

// ---------------------------------------------------------------------------
// 2. Firebase backend (registered default)
// ---------------------------------------------------------------------------

@Composable
private fun FirebaseSection(
    report: (String) -> (Result<KMPAuthUser>) -> Unit,
    onStatus: (String) -> Unit,
) {
    Section(title = "Firebase backend") {
        val googleAuth = rememberGoogleAuthState(onResult = report("Firebase/Google"))
        AuthButton(googleAuth, "Google")

        val appleAuth = rememberAppleAuthState(onResult = report("Firebase/Apple"))
        AuthButton(appleAuth, "Apple")

        val githubAuth = rememberGithubAuthState(onResult = report("Firebase/GitHub"))
        AuthButton(githubAuth, "GitHub")

        val microsoftAuth = rememberMicrosoftAuthState(onResult = report("Firebase/Microsoft"))
        AuthButton(microsoftAuth, "Microsoft")

        // Generic state: any provider enabled in the console.
        val yahooAuth = rememberOAuthState(provider = "yahoo.com", onResult = report("Firebase/Yahoo"))
        AuthButton(yahooAuth, "Yahoo (generic OAuth)")

        val facebookAuth = rememberFacebookAuthState(onResult = report("Firebase/Facebook"))
        AuthButton(facebookAuth, "Facebook")

        val anonymousAuth = rememberAnonymousAuthState(onResult = report("Firebase/Guest"))
        AuthButton(anonymousAuth, "Continue as guest")

        // Reauthentication always needs a FRESH credential of a provider the
        // current user has linked - user.providerIds routes to the right
        // flow: Apple/Google re-run their native flow here; email users
        // re-enter their password (the email block below has the fields).
        val backend = LocalKMPAuthBackend.current
        val scope = rememberCoroutineScope()
        val reauthWith: (String) -> (Result<AuthCredential>) -> Unit = { label ->
            { credentialResult ->
                credentialResult.fold(
                    onSuccess = { credential ->
                        scope.launch {
                            backend.reauthenticate(credential).fold(
                                onSuccess = { onStatus("Firebase: reauthenticated via $label") },
                                onFailure = { onStatus("Firebase $label reauth failed: ${it.message}") },
                            )
                        }
                    },
                    onFailure = { onStatus("Firebase $label reauth failed: ${it.message}") },
                )
            }
        }
        val appleReauth = rememberAppleSignInState(onResult = { result ->
            reauthWith("Apple")(result.map { apple ->
                AuthCredential.IdToken(AuthProviderIds.APPLE, apple.idToken, rawNonce = apple.nonce)
            })
        })
        val googleReauth = rememberGoogleSignInState(onResult = { result ->
            reauthWith("Google")(result.map { google ->
                AuthCredential.IdToken(AuthProviderIds.GOOGLE, google.idToken)
            })
        })
        OutlinedButton(onClick = {
            val user = KMPAuth.currentUser()
            when {
                user == null -> onStatus("No signed-in user to reauthenticate")
                AuthProviderIds.APPLE in user.providerIds -> appleReauth.launch()
                AuthProviderIds.GOOGLE in user.providerIds -> googleReauth.launch()
                AuthProviderIds.EMAIL in user.providerIds ->
                    onStatus("Email user: fill the password below and press Reauthenticate")
                else -> onStatus("No reauth flow wired for providers ${user.providerIds}")
            }
        }) { Text("Reauthenticate (auto-detect)") }

        EmailAuthBlock(label = "Firebase", report = report, onStatus = onStatus)
        PhoneAuthBlock(label = "Firebase", report = report)
    }
}

// ---------------------------------------------------------------------------
// 3. Supabase backend (standalone instance, side by side with Firebase)
// ---------------------------------------------------------------------------

@Composable
private fun SupabaseSection(
    report: (String) -> (Result<KMPAuthUser>) -> Unit,
    onStatus: (String) -> Unit,
) {
    // One wrapper scopes every auth state in this section to Supabase; the
    // rest of the app keeps the default backend (Firebase).
    ProvideKMPAuthBackend(SUPABASE_BACKEND_ID) {
    Section(title = "Supabase backend") {
        val googleAuth = rememberGoogleAuthState(onResult = report("Supabase/Google"))
        AuthButton(googleAuth, "Google")

        val anonymousAuth = rememberAnonymousAuthState(onResult = report("Supabase/Guest"))
        AuthButton(anonymousAuth, "Continue as guest")

        EmailAuthBlock(label = "Supabase", report = report, onStatus = onStatus)
        PhoneAuthBlock(label = "Supabase", report = report)

        // Browser OAuth through the backend interface: Desktop works out of
        // the box (supabase-kt catches the redirect on a localhost server);
        // Android/iOS need supabase-kt's deep-link setup.
        val githubAuth = rememberGithubAuthState(onResult = report("Supabase/GitHub"))
        AuthButton(githubAuth, "GitHub (web flow)")

        val microsoftAuth = rememberMicrosoftAuthState(onResult = report("Supabase/Microsoft"))
        AuthButton(microsoftAuth, "Microsoft (web flow)")

        // GoTrue provider names work too ("gitlab", "discord", ...).
        val gitlabAuth = rememberOAuthState(provider = "gitlab", onResult = report("Supabase/GitLab"))
        AuthButton(gitlabAuth, "GitLab (GoTrue name)")

        // Native Apple credential on iOS via the id_token grant; browser
        // flow elsewhere.
        val appleAuth = rememberAppleAuthState(onResult = report("Supabase/Apple"))
        AuthButton(appleAuth, "Apple")
    }
    }
}

// ---------------------------------------------------------------------------
// Email + phone blocks (reused per backend)
// ---------------------------------------------------------------------------

@Composable
private fun EmailAuthBlock(
    label: String,
    report: (String) -> (Result<KMPAuthUser>) -> Unit,
    onStatus: (String) -> Unit,
) {
    // Same ambient backend the auth states use - reset/reauth run against it.
    val backend = LocalKMPAuthBackend.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var linkToCurrentUser by remember { mutableStateOf(false) }
    var magicLink by remember { mutableStateOf("") }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )

    // Field values are read at launch time - states are created once and
    // reused as the user types.
    val emailSignIn = rememberEmailAuthState(
        email = email,
        password = password,
        mode = EmailAuthMode.SignIn,
        linkAccount = linkToCurrentUser,
        onResult = report("$label/Email sign-in"),
    )
    val emailSignUp = rememberEmailAuthState(
        email = email,
        password = password,
        mode = EmailAuthMode.SignUp,
        onResult = report("$label/Email sign-up"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AuthButton(emailSignIn, "Sign in")
        AuthButton(emailSignUp, "Sign up")
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = linkToCurrentUser, onCheckedChange = { linkToCurrentUser = it })
        // Upgrade path: sign in as guest first, then sign in with email and
        // this enabled - the anonymous uid and its data are kept.
        Text("Link to current user (guest upgrade)", style = MaterialTheme.typography.bodySmall)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            scope.launch {
                backend.sendPasswordResetEmail(email).fold(
                    onSuccess = { onStatus("$label: password reset email sent") },
                    onFailure = { onStatus("$label reset failed: ${it.message}") },
                )
            }
        }) { Text("Reset password") }
        OutlinedButton(onClick = {
            scope.launch {
                backend.reauthenticate(AuthCredential.EmailPassword(email, password)).fold(
                    onSuccess = { onStatus("$label: reauthenticated") },
                    onFailure = { onStatus("$label reauth failed: ${it.message}") },
                )
            }
        }) { Text("Reauthenticate") }
    }

    // Passwordless magic link: step 1 sends the link; step 2 pastes the
    // opened link back in (a real app completes it in its deep-link handler).
    OutlinedButton(onClick = {
        scope.launch {
            backend.sendSignInLinkToEmail(
                email = email,
                actionCodeSettings = EmailActionCodeSettings(
                    url = "https://kmpauthapp.firebaseapp.com",
                    canHandleCodeInApp = true,
                ),
            ).fold(
                onSuccess = { onStatus("$label: magic link sent - open it, then paste it below") },
                onFailure = { onStatus("$label magic link failed: ${it.message}") },
            )
        }
    }) { Text("Send magic link") }
    OutlinedTextField(
        value = magicLink,
        onValueChange = { magicLink = it },
        label = { Text("Paste opened magic link") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = {
        scope.launch {
            if (!backend.isSignInWithEmailLink(magicLink)) {
                onStatus("$label: not a sign-in link for this backend")
            } else {
                backend.signInWithEmailLink(email, magicLink).fold(
                    onSuccess = { onStatus("$label: magic link sign-in as ${it.email ?: it.uid}") },
                    onFailure = { onStatus("$label magic link sign-in failed: ${it.message}") },
                )
            }
        }
    }) { Text("Complete link sign-in") }
}

@Composable
private fun PhoneAuthBlock(
    label: String,
    report: (String) -> (Result<KMPAuthUser>) -> Unit,
) {
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    val phoneAuth = rememberPhoneAuthState(
        phoneNumber = phoneNumber,
        onResult = report("$label/Phone"),
    )
    if (!phoneAuth.isCodeSent) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone (+15551234567)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        AuthButton(phoneAuth, "Phone sign-in")
    } else {
        OutlinedTextField(
            value = smsCode,
            onValueChange = { smsCode = it },
            label = { Text("SMS code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { phoneAuth.submitCode(smsCode) }) { Text("Verify") }
            OutlinedButton(onClick = { phoneAuth.cancel() }) { Text("Cancel") }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Pre-styled UiHelper buttons (kmpauth-uihelper)
// ---------------------------------------------------------------------------

@Composable
private fun UiHelperSection(report: (String) -> (Result<KMPAuthUser>) -> Unit) {
    Section(title = "UiHelper buttons") {
        val googleAuth = rememberGoogleAuthState(onResult = report("Firebase/Google"))
        val appleAuth = rememberAppleAuthState(onResult = report("Firebase/Apple"))
        val facebookAuth = rememberFacebookAuthState(onResult = report("Firebase/Facebook"))

        Text("iconAlignment = Center (default)", style = MaterialTheme.typography.bodySmall)
        GoogleSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp,
        ) { googleAuth.launch() }
        AppleSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) { appleAuth.launch() }
        FacebookSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp,
        ) { facebookAuth.launch() }

        Text("iconAlignment = Start (stacked icons align)", style = MaterialTheme.typography.bodySmall)
        GoogleSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp,
            iconAlignment = SignInButtonIconAlignment.Start,
        ) { googleAuth.launch() }
        AppleSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            iconAlignment = SignInButtonIconAlignment.Start,
        ) { appleAuth.launch() }
        FacebookSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp,
            iconAlignment = SignInButtonIconAlignment.Start,
        ) { facebookAuth.launch() }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            GoogleSignInButtonIconOnly(onClick = { googleAuth.launch() })
            AppleSignInButtonIconOnly(onClick = { appleAuth.launch() })
            FacebookSignInButtonIconOnly(onClick = { facebookAuth.launch() })
        }
    }
}
