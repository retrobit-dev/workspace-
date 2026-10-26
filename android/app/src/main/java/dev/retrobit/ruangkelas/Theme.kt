package dev.retrobit.ruangkelas

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ---------------- palet ---------------- */
val Ink = Color(0xFF0E0D1A)
val Night = Color(0xFF15142A)
val Navy = Color(0xFF1D1C36)
val Panel = Color(0xFF272647)
val Panel2 = Color(0xFF33325A)
val Panel3 = Color(0xFF403F6C)
val Cream = Color(0xFFF8E9C8)
val Cream2 = Color(0xFFE3CDA6)
val Muted = Color(0xFFA79EC4)
val Muted2 = Color(0xFF7D75A0)
val Amber = Color(0xFFF2AB3F)
val AmberDeep = Color(0xFFC2791D)
val Coral = Color(0xFFEF6A5B)
val Teal = Color(0xFF4FA49A)
val Mint = Color(0xFF9FD8B0)
val Sky = Color(0xFF77B8D9)
val Plum = Color(0xFFA58ECB)
val Lime = Color(0xFFC8D96B)
val ScreenGreen = Color(0xFFD7F7CD)

/* ---------------- font pixel ---------------- */
val UiFont = FontFamily(Font(R.font.pixelify))
val LabelFont = FontFamily(Font(R.font.silkscreen))
val LabelFontBold = FontFamily(Font(R.font.silkscreen_bold))
val TitleFont = FontFamily(Font(R.font.press_start_2p))

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = TitleFont, fontSize = 12.sp, lineHeight = 20.sp, color = Cream),
    headlineLarge = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Cream),
    headlineMedium = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Cream),
    titleLarge = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Cream),
    titleMedium = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Cream),
    bodyLarge = TextStyle(fontFamily = UiFont, fontSize = 15.sp, lineHeight = 21.sp, color = Cream2),
    bodyMedium = TextStyle(fontFamily = UiFont, fontSize = 13.sp, lineHeight = 19.sp, color = Cream2),
    bodySmall = TextStyle(fontFamily = LabelFont, fontSize = 9.sp, lineHeight = 15.sp, color = Muted2),
    labelLarge = TextStyle(fontFamily = LabelFont, fontSize = 10.sp, color = Cream2),
    labelMedium = TextStyle(fontFamily = LabelFont, fontSize = 8.sp, color = Muted2),
    labelSmall = TextStyle(fontFamily = LabelFont, fontSize = 7.sp, color = Muted2)
)

@Composable
fun RuangKelasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Amber,
            onPrimary = Ink,
            secondary = Teal,
            onSecondary = Ink,
            background = Night,
            onBackground = Cream,
            surface = Navy,
            onSurface = Cream,
            error = Coral,
            outline = Ink
        ),
        typography = AppTypography,
        content = content
    )
}
