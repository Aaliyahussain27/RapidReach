package com.example.rapidreach.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val RapidReachColorScheme = lightColorScheme(
    primary          = RapidRed,
    onPrimary        = RapidWhite,
    primaryContainer = RapidRedLight,
    background       = RapidWhite,
    surface          = RapidWhite,
    onBackground     = RapidDarkText,
    onSurface        = RapidDarkText,
    error            = RapidError,
    onError          = RapidWhite,
    secondary        = RapidGray,
    onSecondary      = RapidWhite,
    outline          = RapidDivider
)

@Composable
fun RapidReachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Disabled to use custom color scheme
    content: @Composable () -> Unit
) {
    val colorScheme = RapidReachColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}