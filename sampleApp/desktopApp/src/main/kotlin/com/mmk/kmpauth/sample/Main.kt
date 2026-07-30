package com.mmk.kmpauth.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    AppInitializer.onApplicationStart()
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMPAuth Desktop",
        state = rememberWindowState(width = 480.dp, height = 900.dp),
    ) {
        // The shared sample screen covers every KMPAuth feature.
        App()
    }
}
