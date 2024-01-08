package com.mmk.kmpauth.firebase.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider

@Composable
public expect fun SignInWithAppleButton(modifier: Modifier = Modifier, onResult: (Result<FirebaseUser?>) -> Unit)