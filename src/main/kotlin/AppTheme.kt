package theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Brand Colours ────────────────────────────────────────────────────────────
val Blue400    = Color(0xFF42A5F5)
val Blue600    = Color(0xFF2196F3)
val Blue800    = Color(0xFF1565C0)
val Violet400  = Color(0xFFCE93D8)
val Violet700  = Color(0xFF9C27B0)
val Violet900  = Color(0xFF6A1B9A)
val GradeA     = Color(0xFF4CAF50)
val GradeB     = Color(0xFF8BC34A)
val GradeC     = Color(0xFFFF9800)
val GradeD     = Color(0xFFFF5722)
val GradeF     = Color(0xFFF44336)
val Surface0   = Color(0xFFF8F9FA)
val Surface1   = Color(0xFFFFFFFF)
val Dark0      = Color(0xFF0D0D0D)
val Dark1      = Color(0xFF1A1A2E)
val Dark2      = Color(0xFF16213E)
val Dark3      = Color(0xFF1E1E2E)

// ─── Colour Schemes ───────────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary          = Blue600,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFDDE8FF),
    secondary        = Violet700,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFF3E5F5),
    background       = Surface0,
    surface          = Surface1,
    onBackground     = Color(0xFF1C1B1F),
    onSurface        = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFFEEF2FF),
    outline          = Color(0xFFBDBDBD),
    error            = Color(0xFFB00020)
)

private val DarkColors = darkColorScheme(
    primary          = Blue400,
    onPrimary        = Color(0xFF003258),
    primaryContainer = Blue800,
    secondary        = Violet400,
    onSecondary      = Color(0xFF380E5C),
    secondaryContainer = Violet900,
    background       = Dark0,
    surface          = Dark3,
    onBackground     = Color(0xFFE6E1E5),
    onSurface        = Color(0xFFE6E1E5),
    surfaceVariant   = Dark2,
    outline          = Color(0xFF938F99),
    error            = Color(0xFFCF6679)
)

// ─── Typography ───────────────────────────────────────────────────────────────
val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,   fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    headlineMedium= TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 14.sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,  fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,  fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,  fontSize = 12.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 14.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 12.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,  fontSize = 11.sp)
)

// ─── Theme Composable ─────────────────────────────────────────────────────────
@Composable
fun GradeCalculatorTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = AppTypography,
        content     = content
    )
}

/** Map a letter grade to its brand colour. */
fun gradeColor(grade: String): Color = when (grade.uppercase().take(1)) {
    "A" -> GradeA
    "B" -> GradeB
    "C" -> GradeC
    "D" -> GradeD
    else -> GradeF
}

/** Map a status string to its brand colour. */
fun statusColor(status: String): Color = when {
    status.contains("Excellent", true) -> GradeA
    status.contains("Very Good", true) -> GradeB
    status.contains("Good", true)      -> GradeC
    status.contains("Pass", true)      -> GradeD
    else                               -> GradeF
}
