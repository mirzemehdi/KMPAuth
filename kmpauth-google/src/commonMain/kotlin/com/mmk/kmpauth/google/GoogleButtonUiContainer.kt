package com.mmk.kmpauth.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

public interface GoogleButtonUiContainerScope {
    public fun onClick()
}

@Composable
public fun GoogleButtonUiContainer(
    modifier: Modifier = Modifier,
    onGoogleSignInResult: (GoogleUser?) -> Unit,
    content: @Composable GoogleButtonUiContainerScope.() -> Unit,
) {
    val googleAuthProvider = GoogleAuthProvider.get()
    val googleAuthUiProvider = googleAuthProvider.getUiProvider()
    val coroutineScope = rememberCoroutineScope()
    val uiContainerScope = remember {
        object : GoogleButtonUiContainerScope {
            override fun onClick() {
                println("GoogleUiButtonContainer is clicked")
                coroutineScope.launch {
                    val googleUser = googleAuthUiProvider.signIn()
                    onGoogleSignInResult(googleUser)
                }
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }

}