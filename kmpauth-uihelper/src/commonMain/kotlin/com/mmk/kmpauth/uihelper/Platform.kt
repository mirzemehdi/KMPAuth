package com.mmk.kmpauth.uihelper

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
internal expect fun font(res: String, weight: FontWeight, style: FontStyle): Font