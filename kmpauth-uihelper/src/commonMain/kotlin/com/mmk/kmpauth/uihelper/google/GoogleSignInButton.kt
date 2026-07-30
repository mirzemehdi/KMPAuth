package com.mmk.kmpauth.uihelper.google

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.di.isAndroidPlatform
import com.mmk.kmpauth.uihelper.SignInButtonIconAlignment
import com.mmk.kmpauth.uihelper.theme.Fonts
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.Res
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.ic_google
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


/**
 * GoogleSignInButton [Composable] icon only.
 * This follows Google's design guidelines and can be easily customized to fit into your project.
 *
 * @param mode [GoogleButtonMode]
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun GoogleSignInButtonIconOnly(
    modifier: Modifier = Modifier.size(44.dp),
    mode: GoogleButtonMode = GoogleButtonMode.Light,
    shape: Shape = ButtonDefaults.shape,
    onClick: () -> Unit,
) {
    val buttonColor = getButtonColor(mode)
    val borderStroke = getBorderStroke(mode)

    Button(
        modifier = modifier.size(if (isAndroidPlatform()) 40.dp else 44.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        shape = shape,
        colors = buttonColor,
        border = borderStroke,
    ) {
        GoogleIcon()
    }
}


/**
 * GoogleSignInButton [Composable] with text that you can use in your #KMP project.
 * This follows Google's design guidelines and can be easily customized to fit into your project.
 *
 * @param mode [GoogleButtonMode]
 * @param text Button's text. As per guideline this text should be "Sign in with Google",
 * "Sign up with Google", or "Continue with Google".
 * @param iconAlignment [SignInButtonIconAlignment.Center] (default) centers icon and
 * title as one group; [SignInButtonIconAlignment.Start] pins the icon at the leading
 * edge with the title centered on the button axis, so stacked sign-in buttons stay aligned.
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun GoogleSignInButton(
    modifier: Modifier = Modifier.height(44.dp),
    mode: GoogleButtonMode = GoogleButtonMode.Light,
    text: String = "Sign in with Google",
    shape: Shape = ButtonDefaults.shape,
    fontSize: TextUnit = 14.sp,
    iconAlignment: SignInButtonIconAlignment = SignInButtonIconAlignment.Center,
    onClick: () -> Unit,
) {


    val buttonColor = getButtonColor(mode)
    val borderStroke = getBorderStroke(mode)

    val horizontalPadding = if (isAndroidPlatform()) 12.dp else 16.dp
    val iconTextPadding = if (isAndroidPlatform()) 10.dp else 12.dp
    Button(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        shape = shape,
        colors = buttonColor,
        border = borderStroke,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (iconAlignment) {
                SignInButtonIconAlignment.Start -> {
                    GoogleIcon(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = horizontalPadding)
                            .size(20.dp),
                    )
                    GoogleText(text = text, fontSize = fontSize, modifier = Modifier.align(Alignment.Center))
                }

                SignInButtonIconAlignment.Center -> Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoogleIcon(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(iconTextPadding))
                    GoogleText(text = text, fontSize = fontSize)
                }
            }
        }
    }
}

@Composable
private fun GoogleText(text: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = 1,
        fontSize = fontSize,
        fontFamily = Fonts.robotoFontFamily,
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun GoogleIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Image(
        modifier = modifier,
        painter = painterResource(Res.drawable.ic_google),
        contentDescription = "googleIcon"
    )
}


private fun getBorderStroke(mode: GoogleButtonMode): BorderStroke? {
    val borderStroke = when (mode) {
        GoogleButtonMode.Light -> BorderStroke(
            width = 1.dp,
            color = Color(0xFF747775),
        )

        GoogleButtonMode.Dark -> BorderStroke(
            width = 1.dp,
            color = Color(0xFF8E918F),
        )

        GoogleButtonMode.Neutral -> null
    }
    return borderStroke
}

@Composable
private fun getButtonColor(mode: GoogleButtonMode): ButtonColors {
    val containerColor = when (mode) {
        GoogleButtonMode.Light -> Color(0xFFFFFFFF)
        GoogleButtonMode.Dark -> Color(0xFF131314)
        GoogleButtonMode.Neutral -> Color(0xFFF2F2F2)
    }

    val contentColor = when (mode) {
        GoogleButtonMode.Dark -> Color(0xFFE3E3E3)
        else -> Color(0xFF1F1F1F)
    }

    return ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
}