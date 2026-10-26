package dev.retrobit.ruangkelas

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            RuangKelasTheme {
                val state = remember { AppState(applicationContext) }
                Root(state)
            }
        }
    }
}

/**
 * Akar aplikasi: hanya launcher.
 * Nanti, ketika aplikasi bawaan dibuat satu per satu, di sinilah layar tujuan dipilih.
 */
@Composable
fun Root(state: AppState) {
    LauncherScreen(state)
}

/* ---------------- komponen yang dipakai layar aplikasi ---------------- */

@Composable
fun NoteCard(
    note: Note,
    onPin: () -> Unit,
    onClick: () -> Unit,
    accent: Color = NOTE_COLOR[note.cat.lowercase()] ?: Amber
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .border(2.dp, Ink)
            .clickable { onClick() }
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fmtDate(note.date), style = MaterialTheme.typography.labelSmall, color = Muted2)
                Spacer(Modifier.width(6.dp))
                Tag(note.cat, if (note.cat == "TO-DO") Sky else if (note.cat == "REFLEKSI") Teal else Amber)
                Spacer(Modifier.weight(1f))
                Box(Modifier.clickable { onPin() }) {
                    PixelGlyph("star", if (note.pinned) Amber else Muted2, 14.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(note.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(note.body, style = MaterialTheme.typography.bodyMedium, color = Cream2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Cream2, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .width(46.dp)
                .height(20.dp)
                .background(if (checked) Color(0x40F2AB3F) else Panel2)
                .border(2.dp, Ink)
        ) {
            Box(
                Modifier
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(3.dp)
                    .width(16.dp)
                    .fillMaxHeight()
                    .background(if (checked) Amber else Muted2)
            )
        }
    }
}
