package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthProviderBackend

// GitLive's firebase-java-sdk does not implement auth on the JVM (#204);
// Desktop talks to the Firebase Auth REST API instead. Web flows run on
// the loopback page driving the Firebase JS SDK (DesktopWebAuthFlow).
internal actual fun createFirebaseAuthEngine(): AuthProviderBackend =
    FirebaseRestAuthEngine(
        transport = JdkFirebaseRestTransport(),
        apiKeyProvider = { firebaseOptionsOrFail().apiKey },
        webFlowRunner = DesktopWebAuthFlow(config = {
            val options = firebaseOptionsOrFail()
            val projectId = options.projectId
                ?: throw IllegalStateException(
                    "FirebaseBackendOptions.projectId is required for web-flow sign-in on Desktop."
                )
            DesktopWebAuthFlow.WebFlowPageConfig(
                apiKey = options.apiKey,
                authDomain = options.authDomain ?: "$projectId.firebaseapp.com",
                projectId = projectId,
                applicationId = options.applicationId,
            )
        })::signIn,
    )
