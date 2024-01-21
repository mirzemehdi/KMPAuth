package com.mmk.kmpauth.uihelper.apple

/**
 * Apple Sign-In Button mode
 */
public sealed interface AppleButtonMode {
    /**
     * Black mode. According to branding guideline:
     * "Use this style on white or light-color backgrounds that provide sufficient contrast;
     * don’t use it on black or dark backgrounds"
     */
    public data object Black : AppleButtonMode

    /**
     * White mode. According to branding guideline:
     * "Use this style on dark backgrounds that provide sufficient contrast."
     */
    public data object White : AppleButtonMode

    /**
     * White with outline mode. According to branding guideline:
     * "Use this style on white or light-color backgrounds that don’t provide sufficient contrast
     * with the white button fill. Avoid using this style on a dark or saturated background,
     * because the black outline can add visual clutter; instead, use the white style to contrast
     * with a dark background."
     */
    public data object WhiteWithOutline : AppleButtonMode
}