package com.mmk.kmpauth.firebase.backend

import kotlinx.coroutines.await
import kotlin.js.Promise

@JsFun(
    """
    (url, body) => fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: body
    }).then(r => r.text())
"""
)
private external fun fetchPostText(url: String, body: String): Promise<JsString>

/** [FirebaseRestTransport] over the browser's fetch API. */
internal class FetchFirebaseRestTransport : FirebaseRestTransport {
    override suspend fun post(url: String, jsonBody: String): String =
        fetchPostText(url, jsonBody).await<JsString>().toString()
}
