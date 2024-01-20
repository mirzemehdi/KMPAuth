package com.mmk.kmpauth.core

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity


@KMPAuthInternalApi
public fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}