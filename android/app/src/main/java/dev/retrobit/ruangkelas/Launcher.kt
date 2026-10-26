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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Launcher: hanya gambar workspace isometrik ukuran penuh (51 × 32 piksel).
 * Tidak ada rail, app bar, maupun panel — setiap objek di gambar bisa diketuk.
 *
 * Objek yang diketuk akan memanggil [onOpen]. Saat ini setiap aplikasi bawaan
 * launcher belum ada, jadi [onOpen] hanya memunculkan label sebentar.
 * Nanti, satu per satu aplikasi ditambahkan dan [onOpen] tinggal diarahkan ke layarnya.
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

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        val mw = maxWidth
        val mh = maxHeight
        val ctx = LocalContext.current
        val scene = remember {
            BitmapFactory.decodeResource(ctx.resources, R.drawable.scene).asImageBitmap()
        }

        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                image = scene,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(scene.width, scene.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.None
            )
        }

        // jam LED di badan jam digital pada gambar, selalu menunjukkan waktu nyata
        val clockTile = TILES.first { it.label == "Jadwal" }
        LauncherClock(
            tile = clockTile,
            mw = mw,
            mh = mh,
            onTap = {
                caption = clockTile.label
                captionTick++
                onOpen(clockTile)
            }
        )

        TILES.forEach { tile ->
            LauncherHotspot(
                tile = tile,
                mw = mw,
                mh = mh,
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
                    .padding(bottom = 28.dp)
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
    mw: androidx.compose.ui.unit.Dp,
    mh: androidx.compose.ui.unit.Dp,
    onTap: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()

    Box(
        Modifier
            .offset(
                x = mw * (tile.x.toFloat() / SCENE_W),
                y = mh * (tile.y.toFloat() / SCENE_H)
            )
            .size(
                width = mw * (tile.w.toFloat() / SCENE_W),
                height = mh * (tile.h.toFloat() / SCENE_H)
            )
            .background(if (pressed) Color(0x38F2AB3F) else Color.Transparent)
            .border(
                width = if (pressed) 3.dp else 0.dp,
                color = if (pressed) Amber else Color.Transparent
            )
            .clickable(interactionSource = source, indication = null) { onTap() }
    )
}

@Composable
private fun LauncherClock(
    tile: TileInfo,
    mw: androidx.compose.ui.unit.Dp,
    mh: androidx.compose.ui.unit.Dp,
    onTap: () -> Unit
) {
    val clock = rememberClock()

    Box(
        Modifier
            .offset(
                x = mw * (tile.x.toFloat() / SCENE_W),
                y = mh * (tile.y.toFloat() / SCENE_H) + mh * 0.012f
            )
            .size(
                width = mw * (tile.w.toFloat() / SCENE_W),
                height = mh * (tile.h.toFloat() / SCENE_H) * 0.72f
            )
            .background(Color(0xFF0B1410))
            .border(1.dp, Color(0xFF3E6B57))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            clock,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            color = ScreenGreen
        )
    }
}

/** Garis scanline CRT tipis di atas gambar. */
@Composable
fun Scanlines() {
    Canvas(Modifier.fillMaxSize()) {
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color(0x18000000),
                topLeft = Offset(0f, y),
                size = Size(size.width, 1f)
            )
            y += 3f
        }
    }
}
