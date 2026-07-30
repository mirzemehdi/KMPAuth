package com.mmk.kmpauth.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppInitializer.onApplicationStart()
    ComposeViewport(document.body!!) {
        App()
    }
}
