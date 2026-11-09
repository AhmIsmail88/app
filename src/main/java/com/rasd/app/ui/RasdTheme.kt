package com.rasd.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * هوية «رصد» البصرية — مستخرجة من أيقونة التطبيق:
 * تدرّج تركوازي (0B6F7A) → أزرق (0D5BA8)، وخلفية باردة فاتحة.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0A5FAF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E7F8),
    onPrimaryContainer = Color(0xFF05294E),
    secondary = Color(0xFF0B6F7A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDEAEE),
    onSecondaryContainer = Color(0xFF04333A),
    tertiary = Color(0xFF026DCE),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E6FB),
    onTertiaryContainer = Color(0xFF042B52),
    background = Color(0xFFF2F5F9),
    onBackground = Color(0xFF16202A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16202A),
    surfaceVariant = Color(0xFFE5EBF1),
    onSurfaceVariant = Color(0xFF4A5560),
    surfaceContainerHighest = Color(0xFFDDE5ED),
    outline = Color(0xFFC6D1DC),
    outlineVariant = Color(0xFFDFE7EE),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF5F1410)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBEEC),
    onPrimary = Color(0xFF042A50),
    primaryContainer = Color(0xFF0B4A87),
    onPrimaryContainer = Color(0xFFD6E7F8),
    secondary = Color(0xFF7ECBD6),
    onSecondary = Color(0xFF00353C),
    secondaryContainer = Color(0xFF0B4F58),
    onSecondaryContainer = Color(0xFFCDEAEE),
    tertiary = Color(0xFF9CC7F5),
    onTertiary = Color(0xFF042B52),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFE9EFF5),
    surface = Color(0xFF161C22),
    onSurface = Color(0xFFE9EFF5),
    surfaceVariant = Color(0xFF26303A),
    onSurfaceVariant = Color(0xFFC2CCD6),
    outline = Color(0xFF4A5560),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** مقياس طباعي أوسع قليلاً — مريح للعربية والإنجليزية. */
private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 35.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 26.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 18.sp)
)

/** ألوان دلالية للحضور والسلوك — تبقى ثابتة لأنها ذات معنى. */
object RasdColors {
    val present = Color(0xFF15803D)
    val absent = Color(0xFFB91C1C)
    val late = Color(0xFFB45309)
    val neutral = Color(0xFF57534E)
    val positive = Color(0xFF15803D)
    val negative = Color(0xFFB91C1C)
}

@Composable
fun RasdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
