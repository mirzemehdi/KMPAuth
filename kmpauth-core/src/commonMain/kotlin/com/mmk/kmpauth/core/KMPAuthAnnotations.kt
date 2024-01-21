package com.mmk.kmpauth.core

/**
 * KMPAuthInternal Annotation class that limits access for internal usage
 */
@RequiresOptIn(
    message = "This is internal API for KMPAuth. This shouldn't be used outside of KMPAuth API",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY
)
public annotation class KMPAuthInternalApi