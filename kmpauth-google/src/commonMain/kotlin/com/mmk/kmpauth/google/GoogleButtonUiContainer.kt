package com.mmk.kmpauth.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.UiContainerScope
import kotlinx.coroutines.launch


@Composable
public fun GoogleButtonUiContainer(
    modifier: Modifier = Modifier,
    onGoogleSignInResult: (GoogleUser?) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val googleAuthProvider = GoogleAuthProvider.get()
    val googleAuthUiProvider = googleAuthProvider.getUiProvider()
    val coroutineScope = rememberCoroutineScope()
    val updatedOnResultFunc by rememberUpdatedState(onGoogleSignInResult)

    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                println("GoogleUiButtonContainer is clicked")
                coroutineScope.launch {
                    val googleUser = googleAuthUiProvider.signIn()
                    updatedOnResultFunc(googleUser)
                }
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }

}