package com.personalwallet.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Personal Wallet is strictly 100% Dark Mode (Obsidian Sage)
private val ObsidianSageColorScheme = darkColorScheme(
    primary = SagePrimary,
    onPrimary = ObsidianTextHigh,
    primaryContainer = SagePrimaryHover,
    onPrimaryContainer = ObsidianTextHigh,
    
    secondary = AmberPending,
    onSecondary = ObsidianCanvasBase,
    
    error = CrimsonAlert,
    onError = ObsidianTextHigh,
    
    background = ObsidianCanvasBase,
    onBackground = ObsidianTextHigh,
    
    surface = ObsidianSurface1,
    onSurface = ObsidianTextHigh,
    
    surfaceVariant = ObsidianSurface2,
    onSurfaceVariant = ObsidianTextMuted,
    
    outline = ObsidianStructural
)

@Composable
fun PersonalWalletTheme(
    // We force dark theme based on the Obsidian Sage requirements
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = ObsidianSageColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
