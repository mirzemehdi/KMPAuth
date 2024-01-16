package com.mmk.kmpauth.uihelper.google

public sealed interface GoogleButtonMode {
    public data object Light : GoogleButtonMode
    public data object Dark : GoogleButtonMode
    public data object Neutral : GoogleButtonMode
}