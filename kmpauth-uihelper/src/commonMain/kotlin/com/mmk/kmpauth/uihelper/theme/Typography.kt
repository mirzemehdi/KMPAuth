package com.mmk.kmpauth.uihelper.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mmk.kmpauth.uihelper.font

internal object Fonts {
    val robotoFontFamily
        @Composable get() = FontFamily(
            font(
                "roboto_medium",
                FontWeight.Medium,
                FontStyle.Normal
            )
        )
}