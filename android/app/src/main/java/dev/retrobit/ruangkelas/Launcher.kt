package dev.retrobit.ruangkelas

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
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

        // muka jam (analog + digital) dan kalender menggambar di atas gambar
        LiveFaces(box = box)

        // suasana mengikuti jam perangkat
        Ambience()

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

/** Bidang miring: tiga sudut pertama menentukan arah tulisan. */
private data class Plane(val tl: Offset, val tr: Offset, val bl: Offset) {
    val width: Float get() = (tr - tl).getDistance()
    val height: Float get() = (bl - tl).getDistance()
}

private fun planeOf(box: SceneBox, quad: IntArray, density: Density): Plane = with(density) {
    Plane(
        tl = Offset(box.x(quad[0]).toPx(), box.y(quad[1]).toPx()),
        tr = Offset(box.x(quad[2]).toPx(), box.y(quad[3]).toPx()),
        bl = Offset(box.x(quad[4]).toPx(), box.y(quad[5]).toPx())
    )
}

/** Tulis satu baris teks seolah menempel di bidang [plane]. */
private fun DrawScope.drawOnPlane(plane: Plane, paint: Paint, text: String, baseline: Float) {
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val matrix = Matrix()
        matrix.setPolyToPoly(
            floatArrayOf(0f, 0f, plane.width, 0f, 0f, plane.height), 0,
            floatArrayOf(plane.tl.x, plane.tl.y, plane.tr.x, plane.tr.y, plane.bl.x, plane.bl.y), 0,
            3
        )
        val save = native.save()
        native.concat(matrix)
        native.drawText(text, plane.width / 2f, baseline, paint)
        native.restoreToCount(save)
    }
}

private val MONTHS_SHORT = listOf("JAN", "FEB", "MAR", "APR", "MEI", "JUN",
    "JUL", "AGU", "SEP", "OKT", "NOV", "DES")
private val DAYS_SHORT = listOf("SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN")
private val FaceInk = Color(0xFF181430)
private val ScreenGreen = Color(0xFFD7F7CD)

/**
 * Tiga muka hidup di atas gambar:
 *  - jarum jam analog mengikuti [LocalTime],
 *  - layar jam digital di bawahnya menampilkan jam:menit:detik,
 *  - kertas kalender mencetak hari, tanggal, bulan hari ini.
 *
 * Tulisan menempel pada bidangnya masing-masing (kalender & layar LED miring),
 * jadi ikut arah muka benda — bukan datar di atas gambar.
 */
@Composable
private fun LiveFaces(box: SceneBox) {
    val nowMs = rememberNowMs()
    val time = remember(nowMs) { LocalTime.now() }
    val today = remember(nowMs) { LocalDate.now() }
    val ctx = LocalContext.current

    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    }
    val typeface = remember {
        runCatching { ctx.resources.getFont(R.font.silkscreen_bold) }.getOrNull()
    }

    Canvas(Modifier.fillMaxSize()) {
        // ---------- jarum jam analog ----------
        val f = CLOCK_FACE
        val left = box.x(f[0]).toPx()
        val top = box.y(f[1]).toPx()
        val w = box.w(f[2]).toPx()
        val h = box.h(f[3]).toPx()
        val cx = left + w / 2f
        val cy = top + h / 2f
        val rx = w / 2f
        val ry = h / 2f

        fun hand(angleDeg: Double, length: Float, width: Float, color: Color = Ink) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(
                color = color,
                start = Offset(cx, cy),
                end = Offset(cx + (cos(a) * rx * length).toFloat(), cy + (sin(a) * ry * length).toFloat()),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }

        val minute = time.minute + time.second / 60f
        val hour = (time.hour % 12) + minute / 60f
        hand(hour * 30.0, 0.48f, w * 0.105f)
        hand(minute * 6.0, 0.70f, w * 0.075f)
        hand(time.second * 6.0, 0.80f, w * 0.030f, Coral)
        drawCircle(color = Ink, radius = w * 0.06f, center = Offset(cx, cy))
        drawCircle(color = Coral, radius = w * 0.025f, center = Offset(cx, cy))

        // ---------- layar jam digital (jam:menit:detik) ----------
        val led = planeOf(box, LED_QUAD, this)
        val digital = "%02d:%02d:%02d".format(time.hour, time.minute, time.second)
        paint.color = ScreenGreen.toArgb()
        paint.typeface = typeface
        paint.textSize = led.height * 0.62f
        val measured = paint.measureText(digital)
        if (measured > led.width * 0.86f) paint.textSize *= led.width * 0.86f / measured
        drawOnPlane(led, paint, digital, led.height * 0.68f)

        // ---------- kalender: bulan, tanggal, hari ----------
        val cal = planeOf(box, CALENDAR_QUAD, this)
        val month = MONTHS_SHORT[today.monthValue - 1]
        val weekday = DAYS_SHORT[today.dayOfWeek.value - 1]
        val day = today.dayOfMonth.toString()
        paint.color = FaceInk.toArgb()

        paint.textSize = cal.height * 0.22f
        drawOnPlane(cal, paint, month, cal.height * 0.30f)

        paint.textSize = cal.height * 0.46f
        val dayWidth = paint.measureText(day)
        if (dayWidth > cal.width * 0.84f) paint.textSize *= cal.width * 0.84f / dayWidth
        drawOnPlane(cal, paint, day, cal.height * 0.74f)

        paint.textSize = cal.height * 0.22f
        drawOnPlane(cal, paint, weekday, cal.height * 0.99f)
    }
}

/** Warna selaput suasana untuk satu jam tertentu. */
private fun ambienceOf(hour: Int): Pair<Color, Float> = when (hour) {
    in 4..5 -> Color(0xFF6E5AA8) to 0.10f      // subuh
    in 6..9 -> Color(0xFFFFC46B) to 0.06f      // pagi
    in 10..14 -> Color(0xFFFFF2D0) to 0.03f    // siang
    in 15..17 -> Color(0xFFFF8A4C) to 0.10f    // sore
    in 18..21 -> Color(0xFF2B3F8F) to 0.12f    // malam
    else -> Color(0xFF060A22) to 0.34f         // larut
}

/** Selaput tipis di atas scene: pagi hangat, malam kebiruan, larut lebih gelap. */
@Composable
private fun Ambience() {
    val nowMs = rememberNowMs()
    val hour = remember(nowMs) { LocalTime.now().hour }
    val (tint, alpha) = ambienceOf(hour)
    Box(Modifier.fillMaxSize().background(tint.copy(alpha = alpha)))
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
