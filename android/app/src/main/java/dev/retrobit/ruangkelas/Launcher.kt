package dev.retrobit.ruangkelas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
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
    val minute = remember(nowMs) { LocalTime.now().let { it.hour * 60 + it.minute } }

    // kode cuaca untuk animasi langit: cache dulu, lalu refresh online tiap 15 menit
    val appCtx = LocalContext.current
    var skyCode by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            val idx = appCtx.getSharedPreferences("cuaca", Context.MODE_PRIVATE).getInt("kota", 0)
            skyCode = CuacaRepo.kodeKini(appCtx, idx) ?: 1
            delay(15 * 60 * 1000L)
        }
    }

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
        // jam bersama utk seluruh animasi langit
        val skyClock = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            skyClock.animateTo(2_000_000f, tween(2_000_000_000, easing = LinearEasing))
        }
        // scene dengan kaca jendela dibolongi transparan — langit underlay mengintip
        val scene = remember(isDay) {
            val res = if (isDay) R.drawable.scene_day else R.drawable.scene_night
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val bmp = BitmapFactory.decodeResource(ctx.resources, res, opts)
                .copy(Bitmap.Config.ARGB_8888, true)
            val clear = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            // garis tepi CLEAR tipis = transisi halus tanpa MaskFilter
            val edge = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2.4f
            }
            val c = android.graphics.Canvas(bmp)
            val sc = bmp.width.toFloat() / SCENE_W
            val hole = android.graphics.Path()
            for (q in SKY_HOLES) {
                hole.moveTo(q[0] * sc, q[1] * sc)
                for (k in 2 until q.size step 2) hole.lineTo(q[k] * sc, q[k + 1] * sc)
                hole.close()
            }
            c.drawPath(hole, clear)
            c.drawPath(hole, edge)
            bmp.asImageBitmap()
        }

        // langit hidup DI BALIK scene (underlay): terlihat lewat bolongan kaca jendela
        SkyAnim(box = box, code = skyCode, isDay = isDay, minute = minute, clock = skyClock)

        // gambar dasar (kaca jendela transparan — art berada DI DEPAN langit animasi)
        Canvas(Modifier.fillMaxSize()) { drawScene(box, scene) }

        // angin: ekor tirai, daun, kertas pin bergoyang halus
        WindSway(box = box, scene = scene)

        // berkas cahaya matahari ke ruangan (overlay, hanya saat siang cerah)
        SkyBeam(box = box, code = skyCode, isDay = isDay, clock = skyClock)

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
        // hanya daun tanaman yang bergoyang — tirai diam total (keputusan user 0.16.14)
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

/* ==================== LANGIT HIDUP (0.16.16) ====================
   Kedua kaca jendela digambar ulang terus-menerus di atas art:
   - gradien langit mengikuti WAKTU NYATA perangkat (fajar→siang→senja→malam);
   - matahari/bulan bergerak sepanjang busur mengikuti jam;
   - sinar matahari saat cerah/terik, awan bergerak saat berawan,
     langit menggelap saat mendung, rintikan saat gerimis/hujan,
     salju, kabut, dan kilat saat badai — semua dari kode cuaca
     Open-Meteo (CuacaRepo, cache offline). Vektor halus, bukan piksel. */

// Bolongan kaca jendela di scene (piksel gambar): langit underlay terlihat dari
// sini; semua art (rel, bingkai, tirai, palang, gagang) tetap DI DEPAN animasi.
// GEOMETRI SIANG & MALAM BERBEDA (malam: semua sash tertutup) — masing-masing
// diukur dari art-nya sendiri.
private val SKY_HOLES_DAY = listOf(
    floatArrayOf(1010f, 119f, 1082f, 148f, 1082f, 338f, 1010f, 300f), // kaca kiri
    floatArrayOf(1100f, 146f, 1193f, 193f, 1190f, 404f, 1100f, 357f), // bukaan kanan
    // celah sempit antara tirai kiri & bingkai (langit asli terlihat di art)
    floatArrayOf(975f, 206f, 977f, 197f, 981f, 181f, 987f, 186f, 987f, 280f, 986f, 287f, 982f, 296f, 978f, 284f, 975f, 252f),
    // segitiga kaca di kanan palang vertikal, sampai tirai kanan
    floatArrayOf(1191f, 272f, 1194f, 289f, 1196f, 301f, 1198f, 313f, 1200f, 320f, 1202f, 346f,
        1203f, 366f, 1200f, 375f, 1198f, 385f, 1196f, 398f, 1194f, 407f, 1191f, 407f),
)

private val SKY_HOLES_NIGHT = listOf(
    // kaca kiri (sash tertutup; gagang tengah = art, tetap opaque)
    floatArrayOf(1019f, 122f, 1083f, 153f, 1082f, 366f, 1018f, 334f),
    // kaca kanan (area bulan art ikut digantikan orb animasi)
    floatArrayOf(1105f, 166f, 1171f, 197f, 1168f, 408f, 1105f, 376f),
)

// titik gradien langit per menit dalam hari (atas, bawah) — diinterpolasi mulus
private val SKY_STOPS = listOf(
    intArrayOf(0, 0x0A0F2E, 0x18224E),
    intArrayOf(300, 0x2E3A6E, 0xF2955F),   // 05:00 fajar
    intArrayOf(420, 0x7FB2E8, 0xCFE6F7),   // 07:00 pagi cerah
    intArrayOf(720, 0x4D8FE0, 0xA8CDF0),   // 12:00 siang
    intArrayOf(990, 0x6D9BD8, 0xF7C873),   // 16:30 sore keemasan
    intArrayOf(1080, 0x3A3F7A, 0xE86A5A),  // 18:00 senja
    intArrayOf(1170, 0x232D5B, 0x3A4C74),  // 19:30 malam (senada art malam)
    intArrayOf(1440, 0x1B2448, 0x303F66),
)

private fun lerpColor(c0: Int, c1: Int, t: Float): Color {
    fun ch(v: Int, s: Int) = ((v shr s) and 0xFF).toFloat()
    val r = (ch(c0, 16) + (ch(c1, 16) - ch(c0, 16)) * t) / 255f
    val g = (ch(c0, 8) + (ch(c1, 8) - ch(c0, 8)) * t) / 255f
    val b = (ch(c0, 0) + (ch(c1, 0) - ch(c0, 0)) * t) / 255f
    return Color(r, g, b, 1f)
}

private fun skyColors(minute: Int): Pair<Color, Color> {
    val m = minute.coerceIn(0, 1439)
    var i = 0
    while (i < SKY_STOPS.size - 2 && m > SKY_STOPS[i + 1][0]) i++
    val a = SKY_STOPS[i]
    val b = SKY_STOPS[i + 1]
    val t = (m - a[0]).toFloat() / (b[0] - a[0]).toFloat()
    return lerpColor(a[1], b[1], t) to lerpColor(a[2], b[2], t)
}

/** Ringkasan efek langit dari kode cuaca WMO. */
private data class SkyVisuals(
    val langit: Int,          // 0 cerah, 1 cerah berawan sedikit, 2 berawan, 3 mendung
    val hujan: Int,           // 0 tidak, 1 gerimis, 2 hujan, 3 deras
    val salju: Boolean = false,
    val kabut: Boolean = false,
    val petir: Boolean = false,
)

private fun skyVisualsOf(code: Int): SkyVisuals = when (code) {
    0 -> SkyVisuals(0, 0)
    1 -> SkyVisuals(1, 0)
    2 -> SkyVisuals(2, 0)
    3 -> SkyVisuals(3, 0)
    45, 48 -> SkyVisuals(3, 0, kabut = true)
    in 51..57 -> SkyVisuals(2, 1)                       // gerimis
    61, 80 -> SkyVisuals(2, 2)                          // hujan
    63, 81 -> SkyVisuals(2, 2)
    65, 82 -> SkyVisuals(2, 3)                          // deras
    66, 67 -> SkyVisuals(2, 2)
    in 71..77, 85, 86 -> SkyVisuals(2, 0, salju = true) // salju
    95, 96, 99 -> SkyVisuals(3, 3, petir = true)        // badai
    else -> SkyVisuals(2, 0)
}

/** acak deterministik 0..1 (stabil antar frame, tanpa kelas Random). */
private fun hash01(i: Int, salt: Float = 0f): Float =
    (sin(i * 127.1f + salt * 311.7f) * 43758.5453f).let { it - it.toInt() }.let { if (it < 0) it + 1f else it }

/**
 * UNDERLAY langit: digambar SEBELUM scene. Scene memilik bolongan kaca transparan
 * (SKY_HOLES), jadi art — rel, bingkai, tirai, palang — otomatis berada di depan
 * animasi. Yang digambar di luar bolongan pun tertutup scene, tak perlu klip presisi.
 */
@Composable
private fun SkyAnim(
    box: SceneBox,
    code: Int,
    isDay: Boolean,
    minute: Int,
    clock: Animatable<Float, AnimationVector1D>,
) {
    val v = remember(code) { skyVisualsOf(code) }

    Canvas(Modifier.fillMaxSize()) {
        val base = box.unit.toPx()
        fun sx(px: Float) = box.originX.toPx() + px * base
        fun sy(px: Float) = box.originY.toPx() + px * base
        val regionTopLeft = Offset(sx(1006f), sy(115f))
        val regionSize = Size(box.w(198).toPx(), box.h(295).toPx())

        // 1) dasar gradien sesuai waktu nyata (area lega — sisanya tertutup scene)
        val (cTop, cBot) = skyColors(minute)
        drawRect(
            brush = Brush.verticalGradient(listOf(cTop, cBot), startY = sy(110f), endY = sy(415f)),
            topLeft = Offset(sx(985f), sy(105f)), size = Size(box.w(235).toPx(), box.h(310).toPx())
        )

        // 2) bintang berkedip saat malam yang tidak mendung
        if (!isDay && v.langit <= 1) {
            for (i in 0 until 16) {
                val sxx = sx(1010f + hash01(i) * 190f)
                val syy = sy(130f + hash01(i, 7f) * 270f)
                val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(clock.value * (2f + hash01(i, 3f) * 3f) + i))
                drawCircle(
                    Color(0xFFFFF6DC), radius = base * (0.7f + hash01(i, 11f)),
                    center = Offset(sxx, syy), alpha = tw * 0.9f
                )
            }
        }

        // 3) matahari / bulan menuruni busur panel kanan sesuai jam
        val f = if (isDay) ((minute - 360).coerceAtLeast(0)).toFloat() / 660f
        else (((minute - 1080 + 1440) % 1440)).toFloat() / 720f
        val fx = f.coerceIn(0f, 1f)
        val orbX = sx(1108f + 77f * fx)
        val orbY = sy(336f + 36f * fx - sin(fx * 3.14159265f) * 146f)

        if (isDay && v.langit <= 2) {
            val hot = code == 0
            val rayA = if (v.langit == 0) (if (hot) 0.85f else 0.6f) else 0.25f
            for (k in 0 until 8) {
                val ang = (clock.value * 12f + k * 45f) * (3.14159265f / 180f)
                val c = cos(ang)
                val sSin = sin(ang)
                drawLine(
                    Color(0xFFFFE9A8),
                    Offset(orbX + c * 16f * base, orbY + sSin * 16f * base),
                    Offset(orbX + c * 23f * base, orbY + sSin * 23f * base),
                    strokeWidth = base * 1.7f, alpha = rayA, cap = StrokeCap.Round
                )
            }
            val pulse = 0.55f + 0.45f * sin(clock.value * 1.4f)
            drawCircle(Color(0xFFFFD9A0), radius = base * (if (hot) 30f else 24f), center = Offset(orbX, orbY), alpha = 0.28f * pulse)
            drawCircle(Color(0xFFFFE9A8), radius = base * 15f, center = Offset(orbX, orbY))
            drawCircle(Color(0xFFFFF3C4), radius = base * 10.5f, center = Offset(orbX, orbY))
        }
        if (!isDay && v.langit <= 2) {
            drawCircle(Color(0xFFEDE9DA), radius = base * 13f, center = Offset(orbX, orbY), alpha = 0.95f)
            drawCircle(Color(0xFFC9C4B4), radius = base * 3.2f, center = Offset(orbX - 4f * base, orbY + 2f * base), alpha = 0.8f)
            drawCircle(Color(0xFFC9C4B4), radius = base * 2.1f, center = Offset(orbX + 4.5f * base, orbY - 3.5f * base), alpha = 0.8f)
            drawCircle(Color(0xFFC9C4B4), radius = base * 1.6f, center = Offset(orbX + 1f * base, orbY + 5f * base), alpha = 0.8f)
        }

        // 4) awan bergerak (jumlah & warna sesuai kondisi langit)
        val cloudN = when (v.langit) { 0 -> 0; 1 -> 1; 2 -> 3; else -> 5 }
        if (cloudN > 0) {
            val cloudColor = when (v.langit) {
                0, 1 -> Color(0xFFFFFFFF)
                2 -> Color(0xFFE8ECF2)
                else -> Color(0xFFB9BFCB)
            }
            val cloudAlpha = when (v.langit) { 0, 1 -> 0.92f; 2 -> 0.9f; else -> 0.85f }
            for (i in 0 until cloudN) {
                val sp = 3.2f + hash01(i, 21f) * 2.6f
                val cx = 1066f + ((clock.value * sp + hash01(i, 5f) * 198f) % 198f)
                val cy = 130f + hash01(i, 13f) * 220f
                val sc = (0.75f + hash01(i, 31f) * 0.8f) * base
                val c = Offset(sx(cx), sy(cy))
                drawCircle(cloudColor, 16f * sc, Offset(c.x - 14f * sc, c.y + 4f * sc), cloudAlpha)
                drawCircle(cloudColor, 21f * sc, c, cloudAlpha)
                drawCircle(cloudColor, 15f * sc, Offset(c.x + 15f * sc, c.y + 5f * sc), cloudAlpha)
                drawRect(cloudColor, Offset(c.x - 26f * sc, c.y + 2f * sc), Size(52f * sc, 12f * sc), cloudAlpha)
            }
        }

        // 5) selaput gelap: mendung/hujan menggelapkan langit
        val dim = (when (v.langit) { 2 -> 0.18f; 3 -> 0.34f; else -> 0f }) +
            (if (v.hujan > 0) 0.08f else 0f)
        if (dim > 0f) drawRect(Color(0xFF3A4150), regionTopLeft, regionSize, dim)

        // 6) rintikan hujan / gerimis
        if (v.hujan > 0) {
            val n = if (v.hujan == 1) 46 else if (v.hujan == 2) 96 else 150
            val sp = if (v.hujan == 1) 90f else 130f + v.hujan * 45f
            val len = if (v.hujan == 1) 6f else 10f
            for (i in 0 until n) {
                val rx = 1100f + hash01(i, 41f) * 98f
                val ry = 150f + ((clock.value * sp * (0.75f + hash01(i, 43f) * 0.5f) + hash01(i, 47f) * 300f) % 300f)
                drawLine(
                    Color(0xFFCFE4F7), Offset(sx(rx), sy(ry)), Offset(sx(rx - 2.2f), sy(ry + len)),
                    strokeWidth = base * 0.9f, alpha = 0.6f
                )
            }
        }

        // 7) salju turun perlahan dengan goyangan
        if (v.salju) {
            for (i in 0 until 70) {
                val sway = sin(clock.value * (0.8f + hash01(i, 61f)) + i) * 6f
                val syy = 150f + ((clock.value * (18f + hash01(i, 63f) * 14f) + hash01(i, 65f) * 300f) % 300f)
                drawCircle(
                    Color(0xFFFFFFFF), radius = base * (0.9f + hash01(i, 67f)),
                    center = Offset(sx(1100f + hash01(i, 69f) * 98f + sway), sy(syy)), alpha = 0.85f
                )
            }
        }

        // 8) kabut menyelimuti
        if (v.kabut) {
            for (b in 0 until 3) {
                val fo = (clock.value * (5f + b * 2f) + b * 53f) % 160f - 30f
                drawRect(
                    Color(0xFFD8DEE8),
                    topLeft = Offset(sx(1096f + fo), sy(160f + b * 80f)),
                    size = Size(box.w(108).toPx(), base * 18f), alpha = 0.14f
                )
            }
        }

        // 9) kilat badai: dua kedipan singkat tiap ~7 detik
        if (v.petir) {
            val ph = clock.value % 7f
            val flash = if (ph in 3.0f..3.14f) 0.85f else if (ph in 3.3f..3.42f) 0.5f else 0f
            if (flash > 0f) drawRect(Color(0xFFF4F6FF), regionTopLeft, regionSize, flash)
        }

        // 10) kilau kaca: pita diagonal tipis agar tetap terasa kaca
        rotate(-24f, pivot = Offset(sx(1150f), sy(275f))) {
            drawRect(Color(0xFFFFFFFF), Offset(sx(1112f), sy(140f)), Size(base * 12f, base * 270f), 0.10f)
            drawRect(Color(0xFFFFFFFF), Offset(sx(1150f), sy(140f)), Size(base * 5f, base * 270f), 0.08f)
        }
    }
}

/** OVERLAY berkas cahaya ke ruangan (di atas scene) — hanya siang cerah. */
@Composable
private fun SkyBeam(
    box: SceneBox,
    code: Int,
    isDay: Boolean,
    clock: Animatable<Float, AnimationVector1D>,
) {
    if (!isDay || skyVisualsOf(code).langit != 0) return
    Canvas(Modifier.fillMaxSize()) {
        val base = box.unit.toPx()
        fun sx(px: Float) = box.originX.toPx() + px * base
        fun sy(px: Float) = box.originY.toPx() + px * base
        val p = 0.5f + 0.5f * sin(clock.value * 0.9f)
        drawPath(
            Path().apply {
                moveTo(sx(1150f), sy(400f))
                lineTo(sx(1196f), sy(410f))
                lineTo(sx(900f), sy(645f))
                lineTo(sx(740f), sy(565f))
                close()
            },
            color = Color(0xFFFFF2CC), alpha = 0.05f + 0.04f * p
        )
    }
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
