package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = SlateIndigo,
    onTertiary = Color.White,
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = OnSlateContainer,
    background = WarmIvoryBackground,
    onBackground = InkText,
    surface = CrispSurface,
    onSurface = InkText,
    surfaceVariant = SoftSandSurface,
    onSurfaceVariant = MutedInk,
    outline = WarmBorder,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldContainer,
    primaryContainer = EmeraldPrimaryDark,
    onPrimaryContainer = EmeraldContainer,
    secondary = AmberStar,
    onSecondary = OnAmberContainer,
    secondaryContainer = OnAmberContainer,
    onSecondaryContainer = AmberContainer,
    tertiary = Color(0xFF7DD3FC),
    onTertiary = OnSlateContainer,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedText,
    outline = Color(0xFF284146),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val SinggahShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SinggahMasjidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SinggahTypography,
        shapes = SinggahShapes,
        content = content
    )
}
