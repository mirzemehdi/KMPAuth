package com.mmk.kmpauth.uihelper.apple

public sealed interface AppleButtonMode {
    public data object Black : AppleButtonMode
    public data object White : AppleButtonMode
    public data object WhiteWithOutline : AppleButtonMode
}