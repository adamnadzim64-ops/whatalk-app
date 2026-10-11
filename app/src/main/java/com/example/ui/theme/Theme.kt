package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = WhaTalkEmerald,
  onPrimary = Color.White,
  primaryContainer = WhaTalkOutgoingBubbleDark,
  onPrimaryContainer = Color(0xFFE9EDEF),
  secondary = WhaTalkLightGreen,
  onSecondary = Color(0xFF0B141A),
  secondaryContainer = Color(0xFF1F2C34),
  onSecondaryContainer = Color(0xFFE9EDEF),
  tertiary = WhaTalkReadBlue,
  background = WhaTalkDarkSurface,
  onBackground = Color(0xFFE9EDEF),
  surface = WhaTalkDarkHeader,
  onSurface = Color(0xFFE9EDEF),
  surfaceVariant = Color(0xFF233138),
  onSurfaceVariant = Color(0xFF8696A0),
)

private val LightColorScheme = lightColorScheme(
  primary = WhaTalkTealDark,
  onPrimary = Color.White,
  primaryContainer = WhaTalkOutgoingBubbleLight,
  onPrimaryContainer = Color(0xFF111B21),
  secondary = WhaTalkEmerald,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE7F8F3),
  onSecondaryContainer = WhaTalkTealDark,
  tertiary = WhaTalkReadBlue,
  background = Color(0xFFFFFFFF),
  onBackground = Color(0xFF111B21),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF111B21),
  surfaceVariant = Color(0xFFF0F2F5),
  onSurfaceVariant = WhaTalkMutedGray,
)

@Composable
fun WhaTalkTheme(
  themeMode: String = "SYSTEM",
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val useDark = when (themeMode) {
    "DARK" -> true
    "LIGHT" -> false
    else -> systemDark
  }
  val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  WhaTalkTheme(
    themeMode = if (darkTheme) "DARK" else "LIGHT",
    content = content,
  )
}
