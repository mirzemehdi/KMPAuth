package com.mmk.kmpauth.uihelper.google

/**
 * Google Sign-In Button mode
 */
public sealed interface GoogleButtonMode {

    /**
     * Light mode
     */
    public data object Light : GoogleButtonMode

    /**
     * Dark mode
     */
    public data object Dark : GoogleButtonMode

    /**
     * Neutral mode
     */
    public data object Neutral : GoogleButtonMode
}