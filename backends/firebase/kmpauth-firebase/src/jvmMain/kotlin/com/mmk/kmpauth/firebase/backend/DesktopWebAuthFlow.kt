@file:OptIn(KMPAuthInternalApi::class)

package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.minutes

/**
 * Runs a Firebase OAuth web flow on Desktop: serves a local page on a
 * loopback port, opens it in the system browser, and lets the official
 * Firebase **JS SDK** on that page run `signInWithPopup` against the
 * project's hosted auth handler (`https://<authDomain>/__/auth/handler`) —
 * the same https redirect target the Android SDK uses, so it works with
 * every provider configured in the Firebase console, including Apple
 * (which forbids direct localhost redirects). `localhost` is in Firebase's
 * authorized domains by default.
 *
 * The page posts the resulting Firebase ID/refresh tokens back to the
 * loopback server; nothing but this single flow is served, and the server
 * is torn down as soon as a result (or the timeout) arrives.
 */
internal class DesktopWebAuthFlow(
    private val config: () -> WebFlowPageConfig,
    private val openBrowser: (String) -> Unit = ::openInSystemBrowser,
) {

    internal data class WebFlowPageConfig(
        val apiKey: String,
        val authDomain: String,
        val projectId: String,
        val applicationId: String,
    )

    suspend fun signIn(request: WebFlowRequest): WebFlowResult =
        withContext(Dispatchers.IO) {
            val resultDeferred = CompletableDeferred<Result<WebFlowResult>>()
            val page = buildSignInPageHtml(config(), request)
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/") { exchange ->
                    exchange.respond("text/html; charset=utf-8", page)
                }
                createContext("/complete") { exchange ->
                    val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                    exchange.respond("text/plain; charset=utf-8", "OK")
                    resultDeferred.complete(parseCompletion(body))
                }
                start()
            }
            try {
                openBrowser("http://localhost:${server.address.port}/")
                withTimeout(5.minutes) { resultDeferred.await() }.getOrThrow()
            } finally {
                server.stop(1)
            }
        }

    private fun parseCompletion(body: String): Result<WebFlowResult> = runCatching {
        val json = Json.parseToJsonElement(body).jsonObject
        json["error"]?.jsonPrimitive?.content?.let { error ->
            currentLogger.log("Desktop web-flow sign-in failed: $error")
            throw IllegalStateException("Web sign-in failed: $error")
        }
        WebFlowResult(
            idToken = json["idToken"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Web sign-in returned no token"),
            refreshToken = json["refreshToken"]?.jsonPrimitive?.content,
        )
    }

    private fun HttpExchange.respond(contentType: String, body: String) {
        use {
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            responseHeaders.add("Content-Type", contentType)
            sendResponseHeaders(200, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
        }
    }

    internal companion object {

        /** Pinned Firebase JS SDK version served from Firebase's own CDN. */
        private const val FIREBASE_JS_VERSION = "10.12.2"

        /**
         * The served page. All dynamic values are injected as one
         * JSON-encoded config object, so nothing user-controlled is ever
         * interpolated into markup or script.
         */
        fun buildSignInPageHtml(
            config: WebFlowPageConfig,
            request: WebFlowRequest,
        ): String {
            val pageConfigJson = buildJsonObject {
                putJsonObject("firebase") {
                    put("apiKey", config.apiKey)
                    put("authDomain", config.authDomain)
                    put("projectId", config.projectId)
                    put("appId", config.applicationId)
                }
                put("providerId", request.providerId)
                putJsonArray("scopes") { request.scopes.forEach { add(JsonPrimitive(it)) } }
                putJsonObject("customParameters") {
                    request.customParameters.forEach { (k, v) -> put(k, v) }
                }
            }
            // JSON strings may legally contain "</script>", which would
            // terminate the embedding script block; < keeps the JSON
            // equivalent but markup-inert.
            val pageConfig = pageConfigJson.toString().replace("<", "\\u003c")
            // language=html
            return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Sign in</title>
                <style>
                  body { font-family: system-ui, sans-serif; display: flex; flex-direction: column;
                         align-items: center; justify-content: center; height: 100vh; margin: 0; gap: 16px; }
                  button { font-size: 16px; padding: 12px 24px; border-radius: 8px; border: 1px solid #ccc;
                           background: #111; color: #fff; cursor: pointer; }
                  #status { color: #555; }
                </style>
                </head>
                <body>
                <button id="signin">Continue</button>
                <div id="status"></div>
                <script id="kmpauth-config" type="application/json">$pageConfig</script>
                <script type="module">
                  import { initializeApp } from "https://www.gstatic.com/firebasejs/$FIREBASE_JS_VERSION/firebase-app.js";
                  import { getAuth, signInWithPopup, signInWithRedirect, getRedirectResult, OAuthProvider } from "https://www.gstatic.com/firebasejs/$FIREBASE_JS_VERSION/firebase-auth.js";

                  const cfg = JSON.parse(document.getElementById("kmpauth-config").textContent);
                  const auth = getAuth(initializeApp(cfg.firebase));
                  const provider = new OAuthProvider(cfg.providerId);
                  cfg.scopes.forEach((s) => provider.addScope(s));
                  if (Object.keys(cfg.customParameters).length) provider.setCustomParameters(cfg.customParameters);

                  const button = document.getElementById("signin");
                  const status = document.getElementById("status");
                  button.textContent = "Continue with " + cfg.providerId;

                  const complete = (payload) =>
                    fetch("/complete", { method: "POST", headers: { "Content-Type": "application/json" },
                                         body: JSON.stringify(payload) });

                  const finish = async (credential) => {
                    const idToken = await credential.user.getIdToken();
                    await complete({ idToken, refreshToken: credential.user.refreshToken });
                    status.textContent = "Signed in. You can close this window and return to the app.";
                    button.remove();
                  };

                  // Resume a redirect-fallback round trip, if one is pending.
                  getRedirectResult(auth)
                    .then((credential) => { if (credential) return finish(credential); })
                    .catch(async (e) => { await complete({ error: (e && e.code) || String(e) }); });

                  button.addEventListener("click", async () => {
                    button.disabled = true;
                    status.textContent = "Waiting for the sign-in window...";
                    try {
                      await finish(await signInWithPopup(auth, provider));
                    } catch (e) {
                      // Popup blockers are common; fall back to a full-page
                      // redirect (resumed by getRedirectResult above).
                      if (e && e.code === "auth/popup-blocked") {
                        status.textContent = "Continuing in this window...";
                        await signInWithRedirect(auth, provider);
                        return;
                      }
                      await complete({ error: (e && e.code) || String(e) });
                      status.textContent = "Sign-in failed: " + ((e && e.code) || e);
                      button.disabled = false;
                    }
                  });
                </script>
                </body>
                </html>
            """.trimIndent()
        }
    }
}

private fun openInSystemBrowser(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    } else {
        @OptIn(KMPAuthInternalApi::class)
        currentLogger.log("DesktopWebAuthFlow: Desktop browsing is not supported on this platform.")
    }
}
