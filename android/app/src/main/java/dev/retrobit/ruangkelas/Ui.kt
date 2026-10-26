package dev.retrobit.ruangkelas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/* ---------------- glyph pixel 8×8 ---------------- */

private val GLYPHS: Map<String, List<String>> = mapOf(
    "home" to listOf(
        "...##...", "..####..", ".######.", "########", ".#....#.", ".#.##.#.", ".#.##.#.", "........"
    ),
    "book" to listOf(
        "..####..", ".#....#.", ".#.##.#.", ".#....#.", ".#.##.#.", ".#....#.", "..####..", "........"
    ),
    "pencil" to listOf(
        "......##", ".....##.", "....##..", "...##...", "..##....", ".##.....", "##......", "........"
    ),
    "check" to listOf(
        "........", "......##", ".....##.", "##..##..", "####....", ".###....", "..#.....", "........"
    ),
    "chart" to listOf(
        "........", ".#......", ".#...#..", ".#.#.#..", ".#.#.#.#", ".#.#.#.#", "#######.", "........"
    ),
    "clock" to listOf(
        "..####..", ".#....#.", "#..#...#", "#..#...#", "#..###.#", "#......#", ".#....#.", "..####.."
    ),
    "calendar" to listOf(
        ".##..##.", "########", "#......#", "#.####.#", "#......#", "#.####.#", "#......#", "########"
    ),
    "calc" to listOf(
        "########", "#......#", "#.####.#", "#......#", "#.#.#.##", "#......#", "#.#.#.##", "########"
    ),
    "folder" to listOf(
        "###.....", "#..#....", "########", "#......#", "#......#", "#......#", "########", "........"
    ),
    "grid" to listOf(
        ".##..##.", ".##..##.", "........", "........", ".##..##.", ".##..##.", "........", "........"
    ),
    "face" to listOf(
        "..####..", ".#....#.", "#.#..#.#", "#......#", "#.#..#.#", "#..##..#", ".#....#.", "..####.."
    ),
    "gear" to listOf(
        "..####..", ".#....#.", "#..##..#", ".#....#.", "#..##..#", ".#....#.", "..####..", "........"
    ),
    "star" to listOf(
        "...##...", "...##...", "########", ".######.", "..####..", ".##..##.", "##....##", "........"
    )
)

@Composable
fun PixelGlyph(kind: String, tint: Color, size: Dp = 20.dp) {
    val rows = GLYPHS[kind] ?: GLYPHS.getValue("grid")
    Canvas(Modifier.size(size)) {
        val cell = this.size.minDimension / 8f
        rows.forEachIndexed { y, line ->
            line.forEachIndexed { x, ch ->
                if (ch == '#') {
                    drawRect(color = tint, topLeft = Offset(x * cell, y * cell), size = Size(cell, cell))
                }
            }
        }
    }
}

/* ---------------- panel & tombol ---------------- */

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    background: Color = Navy,
    borderColor: Color = Ink,
    padding: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .background(background)
            .border(2.dp, borderColor)
            .padding(padding)
    ) { content() }
}

@Composable
fun PixelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Amber,
    contentColor: Color = Ink,
    enabled: Boolean = true
) {
    Box(
        modifier
            .background(if (enabled) background else Panel)
            .border(2.dp, Ink)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (enabled) contentColor else Muted2)
    }
}

@Composable
fun GhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PixelButton(label, onClick, modifier, background = Panel, contentColor = Cream)
}

@Composable
fun IconBox(kind: String, onClick: () -> Unit, tint: Color = Cream, background: Color = Panel) {
    Box(
        Modifier
            .size(34.dp)
            .background(background)
            .border(2.dp, Ink)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { PixelGlyph(kind, tint, 16.dp) }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) Cream else Panel)
            .border(2.dp, Ink)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Ink else Cream2
        )
    }
}

@Composable
fun ChipRow(labels: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { label ->
            Chip(label, selected == label) { onSelect(label) }
        }
    }
}

@Composable
fun SectionHead(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Cream2)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Amber,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
fun StatBox(value: String, label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .background(Navy)
            .border(2.dp, Ink)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(value, style = MaterialTheme.typography.labelLarge, color = color, fontSize = 16.sp)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted2)
        }
    }
}

@Composable
fun ToolCard(kind: String, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .border(2.dp, Ink)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(tint),
            contentAlignment = Alignment.Center
        ) { PixelGlyph(kind, Ink, 16.dp) }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Cream, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Muted2, maxLines = 1)
        }
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Box(
        Modifier
            .background(color)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Ink)
    }
}

/* ---------------- input ---------------- */

@Composable
fun PixelField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Amber)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Amber,
                unfocusedBorderColor = Ink,
                focusedContainerColor = Panel,
                unfocusedContainerColor = Panel,
                focusedTextColor = Cream,
                unfocusedTextColor = Cream,
                cursorColor = Amber
            )
        )
    }
}

/* ---------------- dialog ---------------- */

@Composable
fun PixelDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .background(Night)
                .border(3.dp, Ink)
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Panel2)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Cream2,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .size(22.dp)
                            .border(2.dp, Ink)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) { PixelGlyph("check", Coral, 12.dp) }
                }
                Box(Modifier.padding(14.dp)) {
                    Column { content() }
                }
            }
        }
    }
}

/* ---------------- jam ---------------- */

@Composable
fun rememberClock(): String {
    var clock by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            delay(1000)
        }
    }
    return clock
}

@Composable
fun rememberClockSeconds(): String {
    var clock by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            delay(1000)
        }
    }
    return clock
}

fun todayLongLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMM yyyy", ID_LOCALE)).uppercase(ID_LOCALE)

fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 11 -> "SELAMAT PAGI,"
        h < 15 -> "SELAMAT SIANG,"
        h < 18 -> "SELAMAT SORE,"
        else -> "SELAMAT MALAM,"
    }
}

@Composable
fun VerticalDividerLine() {
    Box(Modifier.width(2.dp).height(22.dp).background(Color(0x24FFFFFF)))
}

@Composable
fun RowScope.DividerSpacer() {
    Box(Modifier.width(2.dp).height(22.dp).background(Color(0x24FFFFFF)))
}
