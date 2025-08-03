package com.mmk.kmpauth.uihelper.facebook

/**
 * Facebook Sign-In Button mode.
 *
 * Defines the visual style of the Facebook Sign-In button according to Facebook's branding guidelines.
 */
public sealed interface FacebookButtonMode {

    /**
     * The primary Facebook blue button.
     *
     * Use this on light or neutral backgrounds that provide enough contrast for the Facebook blue.
     */
    public data object Blue : FacebookButtonMode

    /**
     * The white Facebook button with blue logo/text.
     *
     * Use this on dark or saturated backgrounds that require a lighter button for better contrast.
     */
    public data object White : FacebookButtonMode
}
