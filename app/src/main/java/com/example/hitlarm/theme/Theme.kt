package com.example.hitlarm.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = ElectricPurple,
    tertiary = NeonGreen,
    background = Obsidian,
    surface = DarkGrey,
    onPrimary = Obsidian,
    onSecondary = Color.White,
    onTertiary = Obsidian,
    onBackground = LightGrey,
    onSurface = Color.White,
    error = NeonPink
)

@Composable
fun HitLarmTheme(
  darkTheme: Boolean = true, // Force dark mode for cyber-neon aesthetic
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve neon branding
  content: @Composable () -> Unit,
) {
  val colorScheme = CyberColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
