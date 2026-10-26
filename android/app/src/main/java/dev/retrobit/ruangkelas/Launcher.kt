package dev.retrobit.ruangkelas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.os.BatteryManager
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Path
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
import kotlin.math.max
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
    val battery = rememberBatteryLevel()
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

        // muka jam (analog + digital), kalender, ikon bingkai, dan gorden baterai
        LiveFaces(box = box, level = battery)

        // suasana mengikuti jam perangkat
        Ambience()

        TILES.forEach { tile ->
            LauncherHotspot(
                tile = tile,
                box = box,
                onTap = {
                    caption = if (tile.label == "Kelas") "${tile.label} · BATERAI $battery%" else tile.label
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

/**
 * Persentase baterai perangkat, dipakai gorden jendela sebagai penunjuk.
 * Mendengar ACTION_BATTERY_CHANGED, jadi angkanya ikut berubah saat dicas/dipakai.
 */
@Composable
private fun rememberBatteryLevel(): Int {
    val ctx = LocalContext.current
    var level by remember { mutableStateOf(readBattery(ctx)) }
    DisposableEffect(ctx) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val l = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (l >= 0 && scale > 0) level = l * 100 / scale
            }
        }
        ctx.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }
    return level
}

private fun readBattery(ctx: Context): Int {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
    return if (level in 0..100) level else 100
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
private fun LiveFaces(box: SceneBox, level: Int) {
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
        // digit digambar mengikuti bidang panel; tanpa kotak tambahan supaya
        // tidak ada bagian yang menonjol keluar dari panel kayu.
        val led = planeOf(box, LED_QUAD, this)
        val digital = "%02d:%02d:%02d".format(time.hour, time.minute, time.second)
        paint.color = ScreenGreen.toArgb()
        paint.typeface = typeface
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = led.height * 0.84f
        val measured = paint.measureText(digital)
        if (measured > led.width * 0.84f) paint.textSize *= led.width * 0.84f / measured
        drawOnPlane(led, paint, digital, led.height * 0.78f)

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

        // ---------- ikon aplikasi di lima bingkai ----------
        APP_ICONS.forEachIndexed { i, icon ->
            val q = FRAME_QUADS.getOrNull(i) ?: return@forEachIndexed
            val plane = planeOf(box, q, this)
            drawRect(
                color = icon.bg.toPixelColor(),
                topLeft = plane.tl,
                size = Size(plane.width, plane.height)
            )
            icon.shapes.forEach { part -> drawIconShape(plane, part) }
        }

        // ---------- gorden jendela sebagai penunjuk baterai ----------
        drawBatteryCurtain(box, level)
    }
}

/** "#RRGGBB" -> Color. */
private fun String.toPixelColor(): Color =
    Color(0xFF000000L or (removePrefix("#").toLongOrNull(16) ?: 0xFFFFFFL))

/** Satu potongan ikon, digambar mengikuti vektor bidang bingkai (jadi ikut miring). */
private fun DrawScope.drawIconShape(plane: Plane, part: IconShape) {
    val u = plane.tr - plane.tl           // arah kanan kertas
    val v = plane.bl - plane.tl           // arah bawah kertas
    val pts = part.points
    if (pts.size < 6) return
    val cell = Path()
    for (i in pts.indices step 2) {
        val x = plane.tl.x + u.x * pts[i] + v.x * pts[i + 1]
        val y = plane.tl.y + u.y * pts[i] + v.y * pts[i + 1]
        if (i == 0) cell.moveTo(x, y) else cell.lineTo(x, y)
    }
    cell.close()
    drawPath(cell, part.color.toPixelColor())
}

/**
 * Gorden sebagai penunjuk baterai.
 * Baterai penuh → gorden tergulung, jendela terbuka. Baterai menipis → kain
 * turun menutup kaca. Warnanya ikut berubah saat baterai rendah.
 */
private fun DrawScope.drawBatteryCurtain(box: SceneBox, level: Int) {
    if (level >= 100) return
    val q = WINDOW_QUAD
    val cover = (100 - level) / 100f

    fun pt(i: Int) = Offset(box.x(q[i]).toPx(), box.y(q[i + 1]).toPx())
    val tl = pt(0)
    val tr = pt(2)
    val bl = pt(4)
    val br = pt(6)

    fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    val bottomL = lerp(tl, bl, cover)
    val bottomR = lerp(tr, br, cover)

    // warna mengikuti kain gorden di gambar, memerah saat baterai kritis
    val fabric = when {
        level <= 15 -> Color(0xFF8E4A4E)
        level <= 40 -> Color(0xFF9A6567)
        else -> Color(0xFFA9888A)
    }
    val cloth = Path().apply {
        moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
        lineTo(bottomR.x, bottomR.y); lineTo(bottomL.x, bottomL.y); close()
    }
    drawPath(cloth, fabric.copy(alpha = 0.95f))

    // lipatan kain: pita gelap-terang bergantian supaya terbaca sebagai gorden
    for (i in 0 until 5) {
        if (i % 2 != 0) continue
        val t0 = i / 5f
        val t1 = (i + 1) / 5f
        val band = Path().apply {
            val a = lerp(tl, tr, t0); val b = lerp(tl, tr, t1)
            val c = lerp(bottomL, bottomR, t1); val e = lerp(bottomL, bottomR, t0)
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(e.x, e.y); close()
        }
        drawPath(band, Color(0x1A000000))
    }
    for (i in 1..5) {
        val t = i / 6f
        drawLine(
            color = Color(0x26000000),
            start = lerp(tl, tr, t),
            end = lerp(bottomL, bottomR, t),
            strokeWidth = max(1f, box.unit.toPx() * 0.8f)
        )
    }
    drawLine(
        color = Color(0x66000000),
        start = bottomL,
        end = bottomR,
        strokeWidth = max(1.5f, box.unit.toPx() * 1.4f)
    )
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
