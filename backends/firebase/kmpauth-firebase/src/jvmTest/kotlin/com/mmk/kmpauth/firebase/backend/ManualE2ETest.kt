package com.mmk.kmpauth.firebase.backend

// Manual end-to-end harness against the sample app's real Firebase project.
// @Ignore-d because it does live network calls and waits for a human to
// drive the printed E2E_URL in a browser - remove @Ignore to run it:
// ./gradlew :backends:firebase:kmpauth-firebase:jvmTest --tests "*ManualE2ETest*"

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.firebase.FirebaseBackendOptions
import com.mmk.kmpauth.firebase.firebase
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test

class ManualE2ETest {

    @Test
    @Ignore
    fun liveDesktopE2E(): Unit = runBlocking {
        // Through the real public path: KMPAuth.initialize { firebase(...) }.
        KMPAuth.initialize {
            firebase(
                FirebaseBackendOptions(
                    apiKey = "AIzaSyAU4EB8PdtZ0faNDJaLvn6r6aXgcOKGpxQ",
                    projectId = "kmpauthapp",
                    applicationId = "1:180951249266:android:1a83eb8eea4835070e2deb",
                )
            )
        }
        val engine = FirebaseRestAuthEngine()

        val anon = engine.signInAnonymously()
        println("E2E_ANON=${anon.map { "${it.uid} anonymous-ok" }}")
        engine.signOut()

        val email = engine.signIn(
            AuthCredential.EmailPassword("nonexistent-e2e@example.com", "wrong-password-123")
        )
        println("E2E_EMAIL=${email.exceptionOrNull()?.message ?: email}")

        val flow = DesktopWebAuthFlow(
            config = {
                DesktopWebAuthFlow.WebFlowPageConfig(
                    apiKey = "AIzaSyAU4EB8PdtZ0faNDJaLvn6r6aXgcOKGpxQ",
                    authDomain = "kmpauthapp.firebaseapp.com",
                    projectId = "kmpauthapp",
                    applicationId = "1:180951249266:android:1a83eb8eea4835070e2deb",
                )
            },
            openBrowser = { url -> println("E2E_URL=$url") },
        )
        val webResult = runCatching {
            flow.signIn(WebFlowRequest("google.com", listOf("email"), emptyMap()))
        }
        println("E2E_WEBFLOW=$webResult")
    }
}
