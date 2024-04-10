package com.mmk.kmpauth.uihelper.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.Res
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.roboto_medium
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

internal object Fonts {
    @OptIn(ExperimentalResourceApi::class)
    val robotoFontFamily
        @Composable get() = FontFamily(
            Font(Res.font.roboto_medium, FontWeight.Medium, FontStyle.Normal),
        )
}