package com.mmk.kmpauth.uihelper

/**
 * How a UiHelper sign-in button lays out its provider icon and title.
 */
public enum class SignInButtonIconAlignment {

    /**
     * Icon pinned at the leading edge, title centered on the button axis.
     * Icon and title positions are independent of each other, so a stacked
     * column of full-width sign-in buttons keeps every provider icon and
     * every title aligned.
     */
    Start,

    /**
     * Icon and title centered together as one group, like a plain button
     * with a leading icon. This is the default. The icon's position depends
     * on the title width, so icons of stacked buttons with different titles
     * won't line up.
     */
    Center,
}
