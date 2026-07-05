package com.mmk.kmpauth.google

import androidx.activity.result.ActivityResult


internal class ActivityResultState(
    var isInProgress: Boolean = false,
    var data: ActivityResult? = null
)