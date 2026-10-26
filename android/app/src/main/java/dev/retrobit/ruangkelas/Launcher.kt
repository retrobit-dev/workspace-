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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Geometri layar launcher.
 *
 * Gambar selalu mempertahankan rasio aslinya. Bila layar lebih lebar/pendek,
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
 * Launcher: gambar workspace isometrik, landscape penuh, tanpa rail/header.
 *
 * Siang/malam mengikuti jam perangkat: pukul 06–17 memakai gambar langit siang,
 * selebihnya langit malam, dan selaput warna di atasnya ikut menyesuaikan.
 */
@Composable
fun LauncherScreen(
    state: AppState,
    onOpen: (TileInfo) -> Unit = {}
) {
    val battery = rememberBatteryLevel()
    val nowMs = rememberNowMs()
    val hour = remember(nowMs) { LocalTime.now().hour }
    val isDay = hour in 6..17

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
        val scene = remember(isDay) {
            val res = if (isDay) R.drawable.scene_day else R.drawable.scene_night
            BitmapFactory.decodeResource(ctx.resources, res).asImageBitmap()
        }

        // gambar dasar
        Canvas(Modifier.fillMaxSize()) { drawScene(box, scene) }

        // angin: gorden & daun bergoyang halus
        WindSway(box = box, scene = scene)

        // muka hidup: ikon bingkai, judul papan tulis
        LiveFaces(box = box, battery = battery, isDay = isDay)
        BatteryLabel(box = box, battery = battery)

        TILES.forEach { tile ->
            LauncherHotspot(
                tile = tile,
                box = box,
                onTap = {
                    val pkg = APP_PACKAGES[tile.label]
                    when {
                        pkg != null -> {
                            val ok = launchApp(ctx, pkg)
                            caption = if (ok) "MEMBUKA ${tile.label.uppercase()}"
                            else "${tile.label.uppercase()} · TIDAK TERPASANG"
                        }
                        tile.label == "File" ->
                            caption = if (openFileManager(ctx)) "MEMBUKA FILE MANAGER"
                            else "FILE MANAGER TIDAK DITEMUKAN"
                        tile.label == "Kelas" -> caption = "KELAS · BATERAI $battery%"
                        else -> caption = tile.label
                    }
                    captionTick++
                    onOpen(tile)
                }
            )
        }

        if (state.scanlines) Scanlines()

        Ambience(hour = hour)

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
                    caption.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Cream2
                )
            }
        }
    }
}

/** Gambar scene penuh layar, tanpa distorsi. */
private fun DrawScope.drawScene(box: SceneBox, scene: ImageBitmap, dx: Float = 0f) {
    drawImage(
        image = scene,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(scene.width, scene.height),
        dstOffset = IntOffset(
            (box.originX.toPx() + dx).roundToInt(),
            box.originY.toPx().roundToInt()
        ),
        dstSize = IntSize(box.width.toPx().roundToInt(), box.height.toPx().roundToInt()),
        filterQuality = FilterQuality.None
    )
}

/**
 * Hembusan angin: bagian gorden dan daun digambar ulang dengan geseran halus
 * mengikuti gelombang sinus, sehingga ruangan terasa "hidup" walau gambarnya diam.
 */
@Composable
private fun WindSway(box: SceneBox, scene: ImageBitmap) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            phase.animateTo(
                targetValue = (2f * Math.PI).toFloat(),
                animationSpec = tween(durationMillis = 5200, easing = LinearEasing)
            )
            phase.snapTo(0f)
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val base = box.unit.toPx()
        CURTAIN_RECTS.forEachIndexed { i, rect ->
            val amount = sin(phase.value + i * 1.1f) * base * 1.6f
            val left = box.x(rect[0]).toPx()
            val top = box.y(rect[1]).toPx()
            val w = box.w(rect[2]).toPx()
            val h = box.h(rect[3]).toPx()
            clipRect(left = left, top = top, right = left + w, bottom = top + h) {
                drawScene(box, scene, dx = amount)
            }
        }
        // daun tanaman bergoyang sedikit lebih cepat
        val leaf = LEAF_RECT
        val dx = sin(phase.value * 1.7f) * base * 1.2f
        val left = box.x(leaf[0]).toPx()
        val top = box.y(leaf[1]).toPx()
        clipRect(
            left = left,
            top = top,
            right = left + box.w(leaf[2]).toPx(),
            bottom = top + box.h(leaf[3]).toPx()
        ) {
            drawScene(box, scene, dx = dx)
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
 * Tempat sampah = pintasan ke file manager.
 * Coba aplikasi file manager bawaan pabrik dulu; kalau tidak ada, buka
 * pemilih dokumen Android (DocumentsUI) yang juga menampilkan isi penyimpanan.
 */
private fun openFileManager(ctx: Context): Boolean {
    val candidates = listOf(
        "com.android.documentsui",
        "com.google.android.documentsui",
        "com.mi.android.globalFileexplorer",
        "com.coloros.filemanager",
        "com.huawei.hidisk",
        "com.sec.android.app.myfiles"
    )
    for (pkg in candidates) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: continue
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { ctx.startActivity(intent) }.isSuccess) return true
    }
    val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching { ctx.startActivity(picker) }.isSuccess
}

/** Buka aplikasi lain yang terpasang di HP. */
private fun launchApp(ctx: Context, pkg: String): Boolean {
    val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { ctx.startActivity(intent) }.isSuccess
}

/**
 * Persentase baterai perangkat, dipakai rel gorden dan angka di dinding.
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

/** Jalankan [block] dengan kanvas sudah "dilipat" ke bidang [plane] (0,0 = kiri-atas). */
private inline fun DrawScope.withPlane(plane: Plane, block: (android.graphics.Canvas) -> Unit) {
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
        block(native)
        native.restoreToCount(save)
    }
}

private val FaceInk = Color(0xFF181430)

/**
 * Semua muka hidup di atas gambar:
 *  - ikon aplikasi di lima bingkai,
 *  - judul "JADWAL MENGAJAR" di papan tulis,
 *  - rel gorden sebagai penunjuk baterai + persentasenya di dinding.
 */
@Composable
private fun LiveFaces(box: SceneBox, battery: Int, isDay: Boolean) {
    val ctx = LocalContext.current
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    val typeface = remember {
        runCatching { ctx.resources.getFont(R.font.silkscreen_bold) }.getOrNull()
    }

    Canvas(Modifier.fillMaxSize()) {
        paint.typeface = typeface

        // ---------------- judul di papan tulis ----------------
        val board = planeOf(box, BOARD_QUAD, this)
        paint.color = FaceInk.toArgb()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = minOf(board.height * 0.115f, board.width * 0.80f / 9.3f)
        val title = "JADWAL MENGAJAR"
        val tw = paint.measureText(title)
        if (tw > board.width * 0.80f) paint.textSize *= board.width * 0.80f / tw
        withPlane(board) { native ->
            native.drawText(title, board.width / 2f, board.height * 0.16f, paint)
            // garis tipis di bawah judul
            val linePaint = Paint().apply {
                color = 0x55181430
                strokeWidth = 1.6f
            }
            native.drawLine(
                board.width * 0.18f, board.height * 0.215f,
                board.width * 0.82f, board.height * 0.215f, linePaint
            )
        }

        // ---------------- ikon aplikasi di lima bingkai ----------------
        APP_ICONS.forEachIndexed { i, icon ->
            val q = FRAME_QUADS.getOrNull(i) ?: return@forEachIndexed
            val plane = planeOf(box, q, this)
            // latar kertas ikut kemiringan bingkai (bukan kotak lurus)
            val u = plane.tr - plane.tl
            val v = plane.bl - plane.tl
            val bg = Path().apply {
                moveTo(plane.tl.x, plane.tl.y)
                lineTo(plane.tl.x + u.x, plane.tl.y + u.y)
                lineTo(plane.tl.x + u.x + v.x, plane.tl.y + u.y + v.y)
                lineTo(plane.tl.x + v.x, plane.tl.y + v.y)
                close()
            }
            drawPath(bg, icon.bg.toPixelColor())
            icon.shapes.forEach { part -> drawIconShape(plane, part) }
        }

        // ---------------- rel gorden = penunjuk baterai ----------------
        drawBatteryRail(box, battery, isDay)
    }
}

/** Persegi panjang tegak lurus -> quad bidang dinding (miring mengikuti dinding). */
private fun wallQuad(rect: IntArray): IntArray {
    val (x, y, w, h) = rect
    val slope = 0.42f            // kemiringan dinding pada gambar ini
    val dx = (w * slope).toInt()
    return intArrayOf(x, y, x + w, y + dx, x, y + h)
}

/** "#RRGGBB" -> Color. */
private fun String.toPixelColor(): Color =
    Color(0xFF000000L or (removePrefix("#").toLongOrNull(16) ?: 0xFFFFFFL))

/** Satu potongan ikon, digambar mengikuti vektor bidang bingkai (jadi ikut miring). */
private fun DrawScope.drawIconShape(plane: Plane, part: IconShape) {
    val u = plane.tr - plane.tl
    val v = plane.bl - plane.tl
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
 * Rel gorden sekaligus indikator baterai:
 * warnanya mengikuti suasana (siang/malam) dan memerah saat baterai menipis.
 * Persentasenya ditulis di dinding tepat di atas rel, mengikuti arah dinding.
 */
private fun DrawScope.drawBatteryRail(box: SceneBox, battery: Int, isDay: Boolean) {
    val x1 = box.x(RAIL_LINE[0]).toPx()
    val y1 = box.y(RAIL_LINE[1]).toPx()
    val x2 = box.x(RAIL_LINE[2]).toPx()
    val y2 = box.y(RAIL_LINE[3]).toPx()

    val mood = when {
        battery <= 15 -> Color(0xFFD9483B)
        battery <= 40 -> Color(0xFFE0A03A)
        isDay -> Color(0xFF9FD8B0)
        else -> Color(0xFF77B8D9)
    }

    drawLine(color = Color(0x88000000), start = Offset(x1, y1 + 3f), end = Offset(x2, y2 + 3f),
        strokeWidth = max(5f, box.unit.toPx() * 6f), cap = StrokeCap.Round)
    drawLine(color = mood, start = Offset(x1, y1), end = Offset(x2, y2),
        strokeWidth = max(3f, box.unit.toPx() * 3.4f), cap = StrokeCap.Round)

    // tanda tiap 10% di sepanjang rel
    for (i in 1..9) {
        val t = i / 10f
        val px = x1 + (x2 - x1) * t
        val py = y1 + (y2 - y1) * t
        val filled = battery >= i * 10
        drawLine(
            color = if (filled) Color(0xFF0E0D1A) else Color(0x66FFFFFF),
            start = Offset(px, py - box.unit.toPx() * 4f),
            end = Offset(px, py + box.unit.toPx() * 4f),
            strokeWidth = max(1f, box.unit.toPx() * 1.2f)
        )
    }
}

/**
 * Angka persentase baterai di dinding di atas rel, ditulis mengikuti bidang dinding.
 */
@Composable
private fun BatteryLabel(box: SceneBox, battery: Int) {
    val ctx = LocalContext.current
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    }
    val typeface = remember {
        runCatching { ctx.resources.getFont(R.font.silkscreen_bold) }.getOrNull()
    }
    Canvas(Modifier.fillMaxSize()) {
        paint.typeface = typeface
        val plane = planeOf(box, BATTERY_TEXT_QUAD, this)
        withPlane(plane) { native ->
            paint.color = (if (battery <= 15) Color(0xFFEF6A5B) else Cream2).toArgb()
            paint.textSize = plane.height * 0.58f
            val text = "$battery%"
            val measured = paint.measureText(text)
            if (measured > plane.width * 0.94f) paint.textSize *= plane.width * 0.94f / measured
            native.drawText(text, plane.width / 2f, plane.height * 0.86f, paint)
        }
    }
}

/** Warna & kepekatan selaput suasana untuk satu jam. */
private fun ambienceOf(hour: Int, isDay: Boolean): Pair<Color, Float> = when (hour) {
    in 5..8 -> Color(0xFFFFD9A0) to 0.10f      // pagi hangat
    in 9..15 -> Color(0xFFFFF6E2) to 0.16f     // siang terang
    in 16..18 -> Color(0xFFFF9A5C) to 0.12f    // sore jingga
    in 19..22 -> Color(0xFF2B3F8F) to 0.14f    // malam biru
    else -> Color(0xFF060A22) to 0.34f         // larut
}

/** Selaput tipis di atas scene: siang lebih terang, malam kebiruan. */
@Composable
private fun Ambience(hour: Int) {
    val (tint, alpha) = ambienceOf(hour, hour in 6..17)
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
