package dev.retrobit.ruangkelas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Daftar Aplikasi — dibuka dari laptop launcher yang sudah menyala.
 * Aplikasi aktif: Cuaca (layar sendiri) & File (file manager pabrik).
 * Sisanya masih "SEGERA": layarnya dibuka satu per satu sesuai pesanan.
 */
@Composable
fun DrawerScreen(state: AppState) {
    val ctx = LocalContext.current

    fun buka(tile: TileInfo) {
        when {
            tile.target == Scr.CUACA -> state.screen = Scr.CUACA
            tile.label == "File" -> openFileManager(ctx)
            else -> Unit // segera
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton("< RUANGAN", onClick = { state.screen = Scr.HOME })
                Spacer(Modifier.width(14.dp))
                Text("APLIKASI", style = MaterialTheme.typography.displayLarge, color = Amber)
            }

            Spacer(Modifier.height(18.dp))

            val rows = TILES.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowTiles ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowTiles.forEach { tile ->
                            val aktif = tile.target == Scr.CUACA || tile.label == "File"
                            val tint = if (aktif) Amber else Muted2
                            Column(
                                Modifier
                                    .weight(1f)
                                    .background(Panel)
                                    .border(2.dp, if (aktif) Panel3 else Panel2)
                                    .clickable(enabled = aktif) { buka(tile) }
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PixelGlyph(tile.glyph, tint, 26.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    tile.label.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (aktif) Cream else Muted2
                                )
                                if (!aktif) {
                                    Spacer(Modifier.height(2.dp))
                                    Text("SEGERA", style = MaterialTheme.typography.labelSmall, color = Muted2)
                                }
                            }
                        }
                        repeat(4 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

