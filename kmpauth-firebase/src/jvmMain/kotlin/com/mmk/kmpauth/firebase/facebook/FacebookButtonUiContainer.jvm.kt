package com.mmk.kmpauth.firebase.facebook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.UiContainerScope
import com.mmk.kmpauth.core.logger.currentLogger
import dev.gitlive.firebase.auth.FirebaseUser

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun FacebookButtonUiContainer(
    modifier: Modifier,
    requestScopes: List<FacebookSignInRequestScope>,
    onResult: (Result<FirebaseUser?>) -> Unit,
    linkAccount: Boolean,
    content: @Composable (UiContainerScope.() -> Unit)
) {
    currentLogger.log("Facebook Login is not supported on JVM")
}