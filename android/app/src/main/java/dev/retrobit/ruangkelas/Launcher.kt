package dev.retrobit.ruangkelas

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Geometri layar launcher.
 *
 * Gambar selalu mempertahankan rasio aslinya (16:10). Bila layar lebih lebar/pendek,
 * sisanya dibiarkan hitam (letterbox) sehingga art tidak pernah melar dan koordinat
 * ketuk tetap presisi. Semua posisi dihitung dari [unit] = ukuran 1 piksel gambar.
 */
private data class SceneBox(
    val unit: Dp,
    val width: Dp,
    val height: Dp,
    val originX: Dp,
    val originY: Dp
) {
    fun x(px: Int): Dp = originX + unit * px
    fun y(px: Int): Dp = originY + unit * px
    fun w(px: Int): Dp = unit * px
    fun h(px: Int): Dp = unit * px
}

@Composable
private fun rememberSceneBox(mw: Dp, mh: Dp): SceneBox {
    val unit: Dp = minOf(mw.value / SCENE_W, mh.value / SCENE_H).dp
    val width = unit * SCENE_W
    val height = unit * SCENE_H
    return SceneBox(
        unit = unit,
        width = width,
        height = height,
        originX = (mw - width) / 2,
        originY = (mh - height) / 2
    )
}

/**
 * Launcher: hanya gambar workspace isometrik, landscape penuh.
 * Tidak ada rail, header, maupun panel — setiap objek pada gambar bisa diketuk.
 *
 * [onOpen] dipanggil dengan objek yang diketuk. Selama aplikasi bawaannya belum ada,
 * yang muncul hanya label sebentar; nanti tinggal diarahkan ke layar masing-masing.
 */
@Composable
fun LauncherScreen(
    state: AppState,
    onOpen: (TileInfo) -> Unit = {}
) {
    var caption by remember { mutableStateOf("") }
    var captionTick by remember { mutableStateOf(0) }

    LaunchedEffect(captionTick) {
        if (caption.isNotEmpty()) {
            delay(2200)
            caption = ""
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val box = rememberSceneBox(maxWidth, maxHeight)
        val ctx = LocalContext.current
        val scene = remember {
            BitmapFactory.decodeResource(ctx.resources, R.drawable.scene).asImageBitmap()
        }

        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                image = scene,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(scene.width, scene.height),
                dstOffset = IntOffset(
                    box.originX.toPx().roundToInt(),
                    box.originY.toPx().roundToInt()
                ),
                dstSize = IntSize(
                    box.width.toPx().roundToInt(),
                    box.height.toPx().roundToInt()
                ),
                filterQuality = FilterQuality.None
            )
        }

        // jam analog hidup di dinding: jarum jam & menit mengikuti waktu nyata
        LiveWallClock(
            box = box,
            onTap = {
                caption = "Jadwal"
                captionTick++
                onOpen(TILES.first { it.label == "Jadwal" })
            }
        )

        // kalender meja menampilkan hari, tanggal, dan bulan hari ini
        LiveCalendar(
            box = box,
            onTap = {
                caption = "Kalender"
                captionTick++
                onOpen(TILES.first { it.label == "Kalender" })
            }
        )

        TILES.forEach { tile ->
            LauncherHotspot(
                tile = tile,
                box = box,
                onTap = {
                    caption = tile.label
                    captionTick++
                    onOpen(tile)
                }
            )
        }

        if (state.scanlines) Scanlines()

        if (caption.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
                    .background(Night)
                    .border(2.dp, Amber)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    "${caption.uppercase()} · SEGERA HADIR",
                    style = MaterialTheme.typography.labelMedium,
                    color = Cream2
                )
            }
        }
    }
}

@Composable
private fun LauncherHotspot(
    tile: TileInfo,
    box: SceneBox,
    onTap: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Box(
        Modifier
            .offset(x = box.x(tile.x), y = box.y(tile.y))
            .size(width = box.w(tile.w), height = box.h(tile.h))
            .background(if (pressed) Color(0x33F2AB3F) else Color.Transparent)
            .border(
                width = if (pressed) 3.dp else 0.dp,
                color = if (pressed) Amber else Color.Transparent
            )
            .clickable(interactionSource = source, indication = null) { onTap() }
    )
}

@Composable
private fun rememberNowMs(): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    return now
}

/**
 * Jam analog di dinding.
 * Kertas jam di gambar ditutup oval krem, lalu 12 tanda jam dan dua jarum digambar
 * ulang mengikuti waktu perangkat — jadi selalu menunjukkan jam sekarang.
 */
@Composable
private fun LiveWallClock(box: SceneBox, onTap: () -> Unit) {
    val nowMs = rememberNowMs()
    val time = remember(nowMs) { LocalTime.now() }
    val x = CLOCK_FACE[0]
    val y = CLOCK_FACE[1]
    val w = CLOCK_FACE[2]
    val h = CLOCK_FACE[3]

    Box(
        Modifier
            .offset(x = box.x(x), y = box.y(y))
            .size(width = box.w(w), height = box.h(h))
            .clickable { onTap() }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // muka jam (krem + 12 tanda) sudah ada di gambar; di sini hanya jarumnya
            val rx = size.width / 2f
            val ry = size.height / 2f
            val cx = rx
            val cy = ry

            fun hand(angleDeg: Double, length: Float, width: Float) {
                val a = Math.toRadians(angleDeg - 90.0)
                drawLine(
                    color = Ink,
                    start = Offset(cx, cy),
                    end = Offset(cx + (cos(a) * rx * length).toFloat(), cy + (sin(a) * ry * length).toFloat()),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }

            val minute = time.minute + time.second / 60f
            val hour = (time.hour % 12) + minute / 60f
            hand(hour * 30.0, 0.48f, size.width * 0.10f)
            hand(minute * 6.0, 0.72f, size.width * 0.070f)

            drawCircle(color = Ink, radius = size.width * 0.055f, center = Offset(cx, cy))
        }
    }
}

private const val MONTHS_SHORT = "JAN,FEB,MAR,APR,MEI,JUN,JUL,AGU,SEP,OKT,NOV,DES"
private const val DAYS_SHORT = "SEN,SEL,RAB,KAM,JUM,SAB,MIN"

/**
 * Kalender meja: kertasnya menampilkan bulan, tanggal, dan nama hari hari ini.
 */
@Composable
private fun LiveCalendar(box: SceneBox, onTap: () -> Unit) {
    val nowMs = rememberNowMs()
    val today = remember(nowMs) { LocalDate.now() }
    val month = MONTHS_SHORT.split(",")[today.monthValue - 1]
    val weekday = DAYS_SHORT[today.dayOfWeek.value - 1]
    val x = CALENDAR_FACE[0]
    val y = CALENDAR_FACE[1]
    val w = CALENDAR_FACE[2]
    val h = CALENDAR_FACE[3]

    Box(
        Modifier
            .offset(x = box.x(x), y = box.y(y))
            .size(width = box.w(w), height = box.h(h))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                month,
                style = MaterialTheme.typography.labelMedium,
                color = Ink,
                fontSize = (box.unit.value * 7f).coerceIn(5f, 13f).sp,
                maxLines = 1
            )
            Text(
                "${today.dayOfMonth}",
                style = MaterialTheme.typography.displayLarge,
                color = Ink,
                fontSize = (box.unit.value * 20f).coerceIn(12f, 40f).sp,
                maxLines = 1
            )
            Text(
                weekday,
                style = MaterialTheme.typography.labelMedium,
                color = Ink,
                fontSize = (box.unit.value * 7f).coerceIn(5f, 13f).sp,
                maxLines = 1
            )
        }
    }
}

/** Garis scanline CRT tipis di atas gambar. */
@Composable
fun Scanlines() {
    Canvas(Modifier.fillMaxSize()) {
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color(0x14000000),
                topLeft = Offset(0f, y),
                size = Size(size.width, 1f)
            )
            y += 3f
        }
    }
}
