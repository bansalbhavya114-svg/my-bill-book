package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Pure White ColorScheme for BillBook
private val WhiteColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = TealSuccess,
    onSecondary = Color.White,
    secondaryContainer = TealSuccessContainer,
    onSecondaryContainer = TealSuccessText,
    tertiary = AmberWarning,
    onTertiary = Color.White,
    tertiaryContainer = AmberWarningContainer,
    onTertiaryContainer = AmberWarningText,
    background = PureWhite,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainerHigh = SurfaceSubtle,
    surfaceContainerHighest = SurfaceBorderLight,
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseErrorContainer,
    onErrorContainer = RoseErrorText,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Forced white theme per user request
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = WhiteColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.White.toArgb()
            window.navigationBarColor = Color.White.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
