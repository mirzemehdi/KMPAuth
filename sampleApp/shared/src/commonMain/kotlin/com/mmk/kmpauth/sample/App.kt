package com.mmk.kmpauth.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.mmk.kmpauth.core.auth.EmailAuthMode
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.rememberAnonymousAuthState
import com.mmk.kmpauth.core.auth.rememberEmailAuthState
import com.mmk.kmpauth.facebook.rememberFacebookAuthState
import com.mmk.kmpauth.facebook.rememberFacebookSignInState
import com.mmk.kmpauth.firebase.apple.rememberAppleAuthState
import com.mmk.kmpauth.firebase.github.rememberGithubAuthState
import com.mmk.kmpauth.firebase.microsoft.rememberMicrosoftAuthState
import com.mmk.kmpauth.firebase.phone.rememberPhoneAuthState
import com.mmk.kmpauth.google.rememberGoogleAuthState
import com.mmk.kmpauth.google.rememberGoogleSignInState
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
                    onFailure = { error -> "$source failed: ${error.message}" },
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
    val scope = rememberCoroutineScope()
    Section(title = "Status") {
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Firebase user: ${KMPAuthBackend.currentUser()?.uid ?: "-"} · " +
                "Supabase user: ${AppInitializer.supabaseBackend.currentUser()?.uid ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    KMPAuthBackend.signOut()
                    onStatus("Signed out of Firebase")
                }
            }) { Text("Sign out (Firebase)") }
            OutlinedButton(onClick = {
                scope.launch {
                    AppInitializer.supabaseBackend.signOut()
                    onStatus("Signed out of Supabase")
                }
            }) { Text("Sign out (Supabase)") }
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
        Button(onClick = { googleSignIn.launch() }) { Text("Google credential") }

        // Apple platforms only; no-op state elsewhere.
        val appleSignIn = rememberAppleSignInState(onResult = { result ->
            onStatus(result.fold(
                onSuccess = { "Apple credential: ${it.email ?: it.userId} (idToken received)" },
                onFailure = { "Apple credential failed: ${it.message}" },
            ))
        })
        Button(onClick = { appleSignIn.launch() }) { Text("Apple credential (iOS)") }

        val facebookSignIn = rememberFacebookSignInState(onResult = { result ->
            onStatus(result.fold(
                onSuccess = { "Facebook credential: token received" },
                onFailure = { "Facebook credential failed: ${it.message}" },
            ))
        })
        Button(onClick = { facebookSignIn.launch() }) { Text("Facebook credential") }
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
        Button(onClick = { googleAuth.launch() }) { Text("Google") }

        val appleAuth = rememberAppleAuthState(onResult = report("Firebase/Apple"))
        Button(onClick = { appleAuth.launch() }) { Text("Apple") }

        val githubAuth = rememberGithubAuthState(onResult = report("Firebase/GitHub"))
        Button(onClick = { githubAuth.launch() }) { Text("GitHub") }

        val microsoftAuth = rememberMicrosoftAuthState(onResult = report("Firebase/Microsoft"))
        Button(onClick = { microsoftAuth.launch() }) { Text("Microsoft") }

        val facebookAuth = rememberFacebookAuthState(onResult = report("Firebase/Facebook"))
        Button(onClick = { facebookAuth.launch() }) { Text("Facebook") }

        val anonymousAuth = rememberAnonymousAuthState(onResult = report("Firebase/Guest"))
        Button(onClick = { anonymousAuth.launch() }) { Text("Continue as guest") }

        EmailAuthBlock(
            label = "Firebase",
            backend = KMPAuthBackend,
            report = report,
            onStatus = onStatus,
        )
        PhoneAuthBlock(report = report)
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
    val backend = AppInitializer.supabaseBackend
    Section(title = "Supabase backend") {
        val googleAuth = rememberGoogleAuthState(
            backend = backend,
            onResult = report("Supabase/Google"),
        )
        Button(onClick = { googleAuth.launch() }) { Text("Google") }

        val anonymousAuth = rememberAnonymousAuthState(
            backend = backend,
            onResult = report("Supabase/Guest"),
        )
        Button(onClick = { anonymousAuth.launch() }) { Text("Continue as guest") }

        EmailAuthBlock(
            label = "Supabase",
            backend = backend,
            report = report,
            onStatus = onStatus,
        )
        Text(
            "Web-flow providers (GitHub/Microsoft/Apple) are Firebase-driven; " +
                "the Supabase backend reports them as unsupported.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// ---------------------------------------------------------------------------
// Email + phone blocks (reused per backend)
// ---------------------------------------------------------------------------

@Composable
private fun EmailAuthBlock(
    label: String,
    backend: AuthProviderBackend,
    report: (String) -> (Result<KMPAuthUser>) -> Unit,
    onStatus: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
        backend = backend,
        onResult = report("$label/Email sign-in"),
    )
    val emailSignUp = rememberEmailAuthState(
        email = email,
        password = password,
        mode = EmailAuthMode.SignUp,
        backend = backend,
        onResult = report("$label/Email sign-up"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { emailSignIn.launch() }) { Text("Sign in") }
        Button(onClick = { emailSignUp.launch() }) { Text("Sign up") }
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
}

@Composable
private fun PhoneAuthBlock(report: (String) -> (Result<KMPAuthUser>) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    val phoneAuth = rememberPhoneAuthState(
        phoneNumber = phoneNumber,
        onResult = report("Firebase/Phone"),
    )
    if (!phoneAuth.isCodeSent) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone (+15551234567)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { phoneAuth.launch() },
            enabled = !phoneAuth.isInProgress,
        ) { Text("Phone sign-in") }
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
        GoogleSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp,
        ) { googleAuth.launch() }

        val appleAuth = rememberAppleAuthState(onResult = report("Firebase/Apple"))
        AppleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp)) { appleAuth.launch() }

        val facebookAuth = rememberFacebookAuthState(onResult = report("Firebase/Facebook"))
        FacebookSignInButton(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            fontSize = 19.sp,
        ) { facebookAuth.launch() }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            GoogleSignInButtonIconOnly(onClick = { googleAuth.launch() })
            AppleSignInButtonIconOnly(onClick = { appleAuth.launch() })
            FacebookSignInButtonIconOnly(onClick = { facebookAuth.launch() })
        }
    }
}
