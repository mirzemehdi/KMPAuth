package com.mmk.kmpauth.core

/**
 * A UiContainerScope provides a scope for the children of UiButtons Container.
 */
public interface UiContainerScope {

    /**
     * Child view's click function should call this #onClick method.
     * This way UiContainer will be notified about view(or button) click and will do necessary action.
     */
    public fun onClick()
}