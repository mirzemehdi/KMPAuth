package com.mmk.kmpauth.firebase.backend

import java.io.File

/**
 * Desktop session store: one JSON file per key under `~/.kmpauth/`,
 * like the Firebase CLI's `~/.config/configstore`. Contains the refresh
 * token in plain text — same trust model as the CLI and the browser's
 * localStorage.
 */
internal actual fun defaultFirebaseSessionStorage(): FirebaseSessionStorage? =
    object : FirebaseSessionStorage {
        private fun fileFor(key: String): File {
            val dir = File(System.getProperty("user.home"), ".kmpauth")
            return File(dir, key.map { if (it.isLetterOrDigit() || it == '-') it else '_' }.joinToString("") + ".json")
        }

        override fun load(key: String): String? =
            fileFor(key).takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() }

        override fun save(key: String, value: String?) {
            val file = fileFor(key)
            if (value == null) {
                file.delete()
            } else {
                file.parentFile?.mkdirs()
                file.writeText(value)
            }
        }
    }
