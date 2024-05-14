package com.mmk.kmpauth.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mmk.kmpauth.firebase.apple.AppleButtonUiContainer
import com.mmk.kmpauth.firebase.google.GoogleButtonUiContainerFirebase
import com.mmk.kmpauth.uihelper.apple.AppleSignInButton
import com.mmk.kmpauth.uihelper.apple.AppleSignInButtonIconOnly
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
import com.mmk.kmpauth.uihelper.google.GoogleSignInButtonIconOnly

fun main() = application {
    AppInitializer.onApplicationStart()
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMPAuth Desktop",
    ) {
        println("Desktop app is started")

        //Google Sign-In Button and authentication with Firebase
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GoogleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp), fontSize = 19.sp) {  }
            AppleSignInButton(modifier = Modifier.fillMaxWidth().height(44.dp)) { }

            GoogleSignInButtonIconOnly(onClick = {  })
            AppleSignInButtonIconOnly(onClick = { })
        }

    }
}