package ua.edu.cunl.lyceummobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0C254E)
val DeepNavy = Color(0xFF06142E)
val Blue = Color(0xFF1C63C2)
val Gold = Color(0xFFF5B738)
val LightBackground = Color(0xFFF3F7FC)
val Danger = Color(0xFFAD1726)
val Success = Color(0xFF0E704C)

private val LyceumColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FB),
    onPrimaryContainer = Navy,
    secondary = Gold,
    onSecondary = Navy,
    background = LightBackground,
    onBackground = DeepNavy,
    surface = Color.White,
    onSurface = DeepNavy,
    error = Danger,
    onError = Color.White
)

@Composable
fun LyceumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LyceumColors,
        content = content
    )
}
