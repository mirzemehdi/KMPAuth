package com.mmk.kmpauth.uihelper.facebook

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.di.isAndroidPlatform
import com.mmk.kmpauth.uihelper.theme.Fonts
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.Res
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.ic_facebook_logo_background_white
import io.github.mirzemehdi.kmpauth_uihelper.generated.resources.ic_facebook_logo_blue
import org.jetbrains.compose.resources.painterResource


/**
 * FacebookSignInButton [Composable] with icon only.
 * This follows Facebook's design guidelines and can be easily customized to fit into your project.
 *
 * @param mode [FacebookButtonMode]
 */
@Composable
public fun FacebookSignInButtonIconOnly(
    modifier: Modifier = Modifier.size(44.dp),
    mode: FacebookButtonMode = FacebookButtonMode.Blue,
    shape: Shape = ButtonDefaults.shape,
    onClick: () -> Unit,
) {
    val iconTintColor = when (mode) {
        FacebookButtonMode.Blue -> facebookBlueColor
        FacebookButtonMode.White -> Color.Transparent
    }
    Button(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
        shape = shape,
        colors = getButtonColor(mode).copy(containerColor = iconTintColor)
    ) {
        FacebookIcon(modifier = Modifier.fillMaxSize(), mode = mode)
    }
}


/**
 * FacebookSignInButton [Composable] with text that you can use in your #KMP project.
 * This follows Facebook's design guidelines and can be easily customized to fit into your project.
 *
 * @param mode [FacebookButtonMode]
 * @param text Button's text. As per guideline this text should be "Sign in with Facebook",
 * "Sign up with Facebook", or "Continue with Facebook".
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun FacebookSignInButton(
    modifier: Modifier = Modifier.height(44.dp),
    mode: FacebookButtonMode = FacebookButtonMode.Blue,
    text: String = "Sign in with Facebook",
    fontFamily: FontFamily = Fonts.robotoFontFamily,
    shape: Shape = ButtonDefaults.shape,
    fontSize: TextUnit = 14.sp,
    onClick: () -> Unit,
) {

    val buttonColor = getButtonColor(mode)
    val horizontalPadding = if (isAndroidPlatform()) 12.dp else 16.dp
    val iconTextPadding = if (isAndroidPlatform()) 10.dp else 12.dp

    Button(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        onClick = onClick,
        shape = shape,
        colors = buttonColor,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            //With Text Button We always request blue background icon
            FacebookIcon(modifier = Modifier.size(24.dp), mode = FacebookButtonMode.Blue)
            Spacer(modifier = Modifier.width(iconTextPadding))
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                maxLines = 1,
                fontSize = fontSize,
                fontFamily = fontFamily,
            )
        }
    }
}

@Composable
private fun FacebookIcon(modifier: Modifier = Modifier, mode: FacebookButtonMode) {
    val source = when (mode) {
        FacebookButtonMode.Blue -> Res.drawable.ic_facebook_logo_blue
        FacebookButtonMode.White -> Res.drawable.ic_facebook_logo_background_white
    }

    Image(
        modifier = modifier,
        painter = painterResource(source),
        contentDescription = "facebookIcon"
    )


}


@Composable
private fun getButtonColor(mode: FacebookButtonMode): ButtonColors {
    val containerColor = when (mode) {
        FacebookButtonMode.Blue -> facebookBlueColor
        FacebookButtonMode.White -> Color.White
    }

    val contentColor = when (mode) {
        FacebookButtonMode.Blue -> Color.White
        FacebookButtonMode.White -> Color.Black
    }

    return ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
}

internal val facebookBlueColor = Color(0xFF0965fe) //Facebook Blue