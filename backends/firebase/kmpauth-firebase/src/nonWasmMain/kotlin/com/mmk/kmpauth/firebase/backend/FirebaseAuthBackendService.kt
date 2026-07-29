package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthProviderBackend

/**
 * `ServiceLoader` entry point so [FirebaseAuthBackend] is discovered
 * automatically on JVM and Android when `kmpauth-firebase` is on the
 * classpath — no explicit `KMPAuth.registerBackendProvider` call needed.
 * `ServiceLoader` requires an instantiable class, hence this delegate over
 * the object.
 */
@KMPAuthInternalApi
public class FirebaseAuthBackendService : AuthProviderBackend by FirebaseAuthBackend
