package com.mmk.kmpauth.uihelper.apple

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.di.isAndroidPlatform
import com.mmk.kmpauth.uihelper.SignInButtonIconAlignment
import com.mmk.kmpauth.uihelper.theme.Fonts
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.Res
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.ic_apple_logo_black
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.ic_apple_logo_white
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource



/**
 * AppleSignInButton [Composable] with icon only.
 * This follows Apple's design guidelines and can be easily customized to fit into your project.
 *
 * @param mode [AppleButtonMode]
 */
@Composable
public fun AppleSignInButtonIconOnly(
    modifier: Modifier = Modifier.size(44.dp),
    mode: AppleButtonMode = AppleButtonMode.Black,
    shape: Shape = ButtonDefaults.shape,
    onClick: () -> Unit,
) {
    val buttonColor = getButtonColor(mode)
    val borderStroke = getBorderStroke(mode)

    Button(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        shape = shape,
        colors = buttonColor,
        border = borderStroke,
    ) {
        // The official logo asset is a square whose side matches the button
        // height; its internal margins put the glyph at the size Apple's
        // button spec requires.
        BoxWithConstraints {
            AppleIcon(modifier = Modifier.size(maxHeight.orDefaultButtonHeight()), mode = mode)
        }
    }
}



/**
 * AppleSignInButton [Composable] with text that you can use in your #KMP project.
 * This follows Apple's design guidelines and can be easily customized to fit into your project.
 *
 * @param mode [AppleButtonMode]
 * @param text Button's text. As per guideline this text should be "Sign in with Apple",
 * "Sign up with Apple", or "Continue with Apple".
 * @param iconAlignment [SignInButtonIconAlignment.Center] (default) centers logo and
 * title as one group; [SignInButtonIconAlignment.Start] pins the logo at the leading
 * edge with the title centered on the button axis, so stacked sign-in buttons stay aligned.
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun AppleSignInButton(
    modifier: Modifier = Modifier.height(44.dp),
    mode: AppleButtonMode = AppleButtonMode.Black,
    text: String = "Sign in with Apple",
    fontFamily: FontFamily = Fonts.robotoFontFamily,
    shape: Shape = ButtonDefaults.shape,
    iconAlignment: SignInButtonIconAlignment = SignInButtonIconAlignment.Center,
    onClick: () -> Unit,
) {


    val buttonColor = getButtonColor(mode)
    val borderStroke = getBorderStroke(mode)
    Button(
        // Apple's button spec: minimum size 140x30.
        modifier = modifier.defaultMinSize(minWidth = 140.dp, minHeight = 30.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        shape = shape,
        colors = buttonColor,
        border = borderStroke,
    ) {
        BoxWithConstraints {
            // Apple's button spec: logo square side = button height (glyph
            // margins are built into the official asset), title = 43% of
            // the button height.
            val buttonHeight = maxHeight.orDefaultButtonHeight()
            Box(modifier = Modifier.fillMaxSize()) {
                when (iconAlignment) {
                    SignInButtonIconAlignment.Start -> {
                        // Logo centered at horizontalPadding + 12dp, the
                        // shared axis all providers' Start-aligned icons sit
                        // on; the logo square is buttonHeight wide, so its
                        // center sits at padding + buttonHeight / 2.
                        val iconCenter = (if (isAndroidPlatform()) 12.dp else 16.dp) + 12.dp
                        AppleIcon(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = (iconCenter - buttonHeight / 2).coerceAtLeast(0.dp))
                                .size(buttonHeight),
                            mode = mode,
                        )
                        AppleText(
                            text = text,
                            buttonHeight = buttonHeight,
                            fontFamily = fontFamily,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    // The logo square carries an invisible leading margin
                    // (glyph centered in a height-sized square) while the
                    // text edge is flush, so the group's geometric center
                    // sits right of its optical center by half that margin -
                    // the offset compensates.
                    SignInButtonIconAlignment.Center -> {
                        val glyphLeadingMargin = buttonHeight * ((1f - APPLE_GLYPH_WIDTH_FRACTION) / 2)
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = -glyphLeadingMargin / 2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppleIcon(modifier = Modifier.size(buttonHeight), mode = mode)
                            AppleText(text = text, buttonHeight = buttonHeight, fontFamily = fontFamily)
                        }
                    }
                }
            }
        }
    }
}

/** Apple's default button height, used when the incoming constraints are unbounded. */
private fun Dp.orDefaultButtonHeight(): Dp = if (this == Dp.Infinity) 44.dp else this

/** Width of the visible Apple glyph as a fraction of the square logo asset. */
private const val APPLE_GLYPH_WIDTH_FRACTION: Float = 15f / 44f

@Composable
private fun AppleText(
    text: String,
    buttonHeight: Dp,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        fontSize = (buttonHeight.value * 0.43).sp,
        maxLines = 1,
        fontFamily = fontFamily,
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun AppleIcon(modifier: Modifier = Modifier, mode: AppleButtonMode) {
    val source = when (mode) {
        AppleButtonMode.Black -> Res.drawable.ic_apple_logo_white
        AppleButtonMode.White -> Res.drawable.ic_apple_logo_black
        AppleButtonMode.WhiteWithOutline -> Res.drawable.ic_apple_logo_black
    }
    Image(
        modifier = modifier,
        painter = painterResource(source),
        contentDescription = "appleIcon"
    )
}


private fun getBorderStroke(mode: AppleButtonMode): BorderStroke? {
    val borderStroke = when (mode) {
        AppleButtonMode.WhiteWithOutline -> BorderStroke(
            width = 1.dp,
            color = Color.Black,
        )

        else -> null
    }
    return borderStroke
}

@Composable
private fun getButtonColor(mode: AppleButtonMode): ButtonColors {
    val containerColor = when (mode) {
        AppleButtonMode.Black -> Color.Black
        else -> Color.White
    }

    val contentColor = when (mode) {
        AppleButtonMode.Black -> Color.White
        else -> Color.Black
    }

    return ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
}