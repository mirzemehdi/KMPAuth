package com.mmk.kmpauth.firebase.backend

@JsFun("(k) => window.localStorage.getItem(k)")
private external fun localStorageGet(key: String): String?

@JsFun("(k, v) => window.localStorage.setItem(k, v)")
private external fun localStorageSet(key: String, value: String)

@JsFun("(k) => window.localStorage.removeItem(k)")
private external fun localStorageRemove(key: String)

/**
 * Wasm session store: `localStorage`, the same place the Firebase JS SDK
 * keeps its sessions — survives page reloads.
 */
internal actual fun defaultFirebaseSessionStorage(): FirebaseSessionStorage? =
    object : FirebaseSessionStorage {
        override fun load(key: String): String? = localStorageGet("kmpauth-$key")

        override fun save(key: String, value: String?) {
            if (value == null) localStorageRemove("kmpauth-$key")
            else localStorageSet("kmpauth-$key", value)
        }
    }
