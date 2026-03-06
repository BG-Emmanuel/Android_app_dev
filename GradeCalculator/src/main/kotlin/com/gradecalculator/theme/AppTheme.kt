package com.gradecalculator.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// App Color Palette
// ─────────────────────────────────────────────────────────────────────────────

object AppColors {
    val Black       = Color(0xFF000000)
    val White       = Color(0xFFFFFFFF)
    val Blue        = Color(0xFF2196F3)
    val BlueLight   = Color(0xFF64B5F6)
    val Violet      = Color(0xFF9C27B0)
    val VioletLight = Color(0xFFCE93D8)

    val GradeA = Color(0xFF4CAF50)
    val GradeB = Color(0xFF8BC34A)
    val GradeC = Color(0xFFFFC107)
    val GradeD = Color(0xFFFF9800)
    val GradeF = Color(0xFFF44336)

    val SurfaceDark   = Color(0xFF1E1E1E)
    val BackgroundDark = Color(0xFF121212)
    val SurfaceLight  = Color(0xFFF5F5F5)

    fun gradeColor(grade: String): Color = when (grade.uppercase()) {
        "A" -> GradeA
        "B" -> GradeB
        "C" -> GradeC
        "D" -> GradeD
        else -> GradeF
    }

    fun fromHex(hex: String): Color {
        val cleaned = hex.removePrefix("#")
        return Color(cleaned.toLong(16) or 0xFF000000L)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme Sealed Class
// ─────────────────────────────────────────────────────────────────────────────

sealed class AppTheme {
    object Light : AppTheme() {
        val background = AppColors.White
        val surface    = AppColors.SurfaceLight
        val primary    = AppColors.Blue
        val accent     = AppColors.Violet
        val text       = AppColors.Black
    }

    object Dark : AppTheme() {
        val background = AppColors.BackgroundDark
        val surface    = AppColors.SurfaceDark
        val primary    = AppColors.BlueLight
        val accent     = AppColors.VioletLight
        val text       = AppColors.White
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Material3 Color Schemes
// ─────────────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary          = AppColors.Blue,
    onPrimary        = AppColors.White,
    secondary        = AppColors.Violet,
    onSecondary      = AppColors.White,
    background       = AppColors.White,
    onBackground     = AppColors.Black,
    surface          = AppColors.SurfaceLight,
    onSurface        = AppColors.Black,
    error            = Color(0xFFB00020),
    onError          = AppColors.White
)

private val DarkColorScheme = darkColorScheme(
    primary          = AppColors.BlueLight,
    onPrimary        = AppColors.Black,
    secondary        = AppColors.VioletLight,
    onSecondary      = AppColors.Black,
    background       = AppColors.BackgroundDark,
    onBackground     = AppColors.White,
    surface          = AppColors.SurfaceDark,
    onSurface        = AppColors.White,
    error            = Color(0xFFCF6679),
    onError          = AppColors.Black
)

@Composable
fun GradeCalculatorTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography(),
        content     = content
    )
}
