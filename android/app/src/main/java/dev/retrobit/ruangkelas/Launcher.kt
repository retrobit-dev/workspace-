package dev.retrobit.ruangkelas

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.graphics.drawscope.withTransform
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
        LiveFaces(box = box, isDay = isDay)

        TILES.forEach { tile ->
            LauncherHotspot(
                tile = tile,
                box = box,
                onTap = {
                    when {
                        tile.label == "File" ->
                            caption = if (openFileManager(ctx)) "MEMBUKA FILE MANAGER"
                            else "FILE MANAGER TIDAK DITEMUKAN"
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
        filterQuality = FilterQuality.High  // 0.16.9: art smooth — render halus, bukan piksel blok
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
        // tirai kanan: terpegang di ring rel, bagian bawah mengayun bebas —
        // digerakkan dengan ROTASI di sekitar rel + geseran tipis
        val cur = CURTAIN_R_RECT
        clipRect(
            left = box.x(cur[0]).toPx(),
            top = box.y(cur[1]).toPx(),
            right = box.x(cur[0] + cur[2]).toPx(),
            bottom = box.y(cur[1] + cur[3]).toPx()
        ) {
            withTransform({
                rotate(
                    degrees = sin(phase.value * 2.0f) * 1.5f,
                    pivot = Offset(
                        box.x(cur[0] + cur[2] / 2).toPx(),
                        box.y(cur[1]).toPx()
                    )
                )
                translate(sin(phase.value * 2.0f) * base * 0.8f, 0f)
            }) {
                drawScene(box, scene)
            }
        }
        // daun tanaman bergoyang (pot tidak ikut dianimasikan)
        val leaf = LEAF_RECT
        val dx = sin(phase.value * 1.7f) * base * 1.6f
        clipRect(
            left = box.x(leaf[0]).toPx(),
            top = box.y(leaf[1]).toPx(),
            right = box.x(leaf[0] + leaf[2]).toPx(),
            bottom = box.y(leaf[1] + leaf[3]).toPx()
        ) {
            drawScene(box, scene, dx = dx)
        }
        // kertas di papan pin: bergoyang lembut, pivot tepat di pin masing-masing
        PAPER_RECTS.forEachIndexed { i, pap ->
            clipRect(
                left = box.x(pap[2]).toPx(),
                top = box.y(pap[3]).toPx(),
                right = box.x(pap[2] + pap[4]).toPx(),
                bottom = box.y(pap[3] + pap[5]).toPx()
            ) {
                withTransform({
                    rotate(
                        degrees = sin(phase.value * 1.3f + i * 0.9f) * 1.6f,
                        pivot = Offset(box.x(pap[0]).toPx(), box.y(pap[1]).toPx())
                    )
                }) {
                    drawScene(box, scene)
                }
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
 *  - judul "JADWAL MENGAJAR" di papan tulis.
 */
@Composable
private fun LiveFaces(box: SceneBox, isDay: Boolean) {
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

/** Warna & kepekatan selaput suasana untuk satu jam. */
private fun ambienceOf(hour: Int, isDay: Boolean): Pair<Color, Float> = when (hour) {
    in 5..8 -> Color(0xFFFFD9A0) to 0.10f      // pagi hangat
    in 9..15 -> Color(0xFF3A4670) to 0.14f     // siang lembut — meredam silau (0.16.10)
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
