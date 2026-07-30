package com.mmk.kmpauth.core.auth

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.mmk.kmpauth.core.KMPAuthInternalApi
import java.lang.ref.WeakReference

/**
 * Tracks the foreground Activity via lifecycle callbacks (registered by
 * `KMPAuthContextInitializer` at process start). Lets non-`compose.ui`
 * code — kmpauth-core deliberately depends on compose.runtime only — reach
 * the Activity some SDK flows need, e.g. Firebase phone verification's
 * reCAPTCHA fallback UI.
 */
@KMPAuthInternalApi
public object AndroidActivityTracker : Application.ActivityLifecycleCallbacks {

    private var current: WeakReference<Activity>? = null

    public val currentActivity: Activity?
        get() = current?.get()

    override fun onActivityResumed(activity: Activity): Unit {
        current = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity): Unit {
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?): Unit = Unit
    override fun onActivityStarted(activity: Activity): Unit = Unit
    override fun onActivityPaused(activity: Activity): Unit = Unit
    override fun onActivityStopped(activity: Activity): Unit = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle): Unit = Unit
}
