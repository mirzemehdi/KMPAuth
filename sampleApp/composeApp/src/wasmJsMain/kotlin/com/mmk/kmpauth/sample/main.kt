package com.mmk.kmpauth.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.CanvasBasedWindow
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import com.mmk.kmpauth.uihelper.apple.AppleSignInButtonIconOnly
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.mmk.kmpauth.uihelper.google.GoogleSignInButtonIconOnly

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppInitializer.onApplicationStart()
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {


        println("Web app is started")
//        App() //TODO when all implementations are finished remove below and uncomment this
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GoogleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp), fontSize = 19.sp) {  }
            AppleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp)) { }

            GoogleSignInButtonIconOnly(onClick = {  })
            AppleSignInButtonIconOnly(onClick = { })
        }
    }
}