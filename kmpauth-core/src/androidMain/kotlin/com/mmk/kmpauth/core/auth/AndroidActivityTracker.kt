package com.mmk.kmpauth.core.auth

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the foreground Activity via lifecycle callbacks (registered by
 * `KMPAuthContextInitializer` at process start). Lets non-`compose.ui`
 * code — kmpauth-core deliberately depends on compose.runtime only — reach
 * the Activity some SDK flows need, e.g. Firebase phone verification's
 * reCAPTCHA fallback UI.
 */
internal object AndroidActivityTracker : Application.ActivityLifecycleCallbacks {

    private var current: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = current?.get()

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
