package dev.retrobit.ruangkelas

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp

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
                val startScreen = intent?.getStringExtra("screen")
                val state = remember {
                    AppState(applicationContext).also { st ->
                        if (!startScreen.isNullOrBlank()) {
                            runCatching { st.screen = Scr.valueOf(startScreen.uppercase()) }
                        }
                    }
                }
                Root(state)
            }
        }
    }
}

@Composable
fun Root(state: AppState) {
    var showSettings by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Night)) {
        val wide = maxWidth >= 1000.dp

        Row(Modifier.fillMaxSize()) {
            Rail(state)

            Column(Modifier.weight(1f).fillMaxHeight()) {
                AppBar(state) { showSettings = true }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    ScreenHost(state, wide)
                }
            }

            if (wide) ContextPanel(state)
        }

        if (state.scanlines) Scanlines()
    }

    if (showSettings) SettingsDialog(state) { showSettings = false }
}

/* ---------------- app bar ---------------- */

@Composable
private fun AppBar(state: AppState, onSettings: () -> Unit) {
    val clock = rememberClock()
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF191833))
            .border(2.dp, Ink)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).background(Amber).border(2.dp, Ink),
            contentAlignment = Alignment.Center
        ) { PixelGlyph("book", Ink, 17.dp) }

        Spacer(Modifier.width(10.dp))
        Text(
            "RUANG KELAS",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 10.sp,
            color = Cream,
            maxLines = 1
        )
        Spacer(Modifier.width(12.dp))
        VerticalDividerLine()
        Spacer(Modifier.width(12.dp))
        Text(
            state.screen.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Amber,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.weight(1f))

        Box(
            Modifier.background(Amber).padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text("LV 7 · GURU SIGAP", style = MaterialTheme.typography.labelSmall, color = Ink)
        }
        Spacer(Modifier.width(12.dp))

        Text(clock, style = MaterialTheme.typography.labelLarge, fontSize = 22.sp, color = ScreenGreen)
        Spacer(Modifier.width(10.dp))
        Text(
            todayLongLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            maxLines = 1
        )

        Spacer(Modifier.width(12.dp))
        IconBox("gear", onSettings, tint = Cream2)
    }
}

/* ---------------- rail ---------------- */

private data class RailItem(val screen: Scr, val glyph: String, val short: String)

private val RAIL = listOf(
    RailItem(Scr.HOME, "home", "Beranda"),
    RailItem(Scr.JURNAL, "book", "Jurnal"),
    RailItem(Scr.CATATAN, "pencil", "Catatan"),
    RailItem(Scr.ABSENSI, "check", "Absensi"),
    RailItem(Scr.NILAI, "chart", "Nilai"),
    RailItem(Scr.JADWAL, "clock", "Jadwal"),
    RailItem(Scr.KALENDER, "calendar", "Kalender"),
    RailItem(Scr.KALKULATOR, "calc", "Kalk."),
    RailItem(Scr.BINDER, "folder", "Binder"),
    RailItem(Scr.KELAS, "grid", "Kelas"),
    RailItem(Scr.PROFIL, "face", "Profil")
)

@Composable
private fun Rail(state: AppState) {
    Column(
        Modifier
            .width(86.dp)
            .fillMaxHeight()
            .background(Color(0xFF111022))
            .border(2.dp, Ink)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        RAIL.forEach { item ->
            val active = state.screen == item.screen
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(if (active) Color(0x1FF2AB3F) else Color.Transparent)
                    .border(if (active) 2.dp else 0.dp, if (active) Amber else Color.Transparent)
                    .clickable { state.screen = item.screen }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelGlyph(item.glyph, if (active) Amber else Muted2, 20.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.short.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) Amber else Muted2,
                    maxLines = 1,
                    fontSize = 7.sp
                )
            }
        }
    }
}

/* ---------------- panel konteks ---------------- */

@Composable
private fun ContextPanel(state: AppState) {
    val clock = rememberClockSeconds()
    val events = state.todayAgenda()

    Column(
        Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Color(0xFF111022))
            .border(2.dp, Ink)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF101A16))
                .border(2.dp, Color(0xFF2C4A3B))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("JAM DIGITAL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D8F79))
            Text(clock, style = MaterialTheme.typography.labelLarge, fontSize = 38.sp, color = ScreenGreen)
            Text(todayLongLabel(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8DBFA6))
            Spacer(Modifier.height(6.dp))
            Text("WAKTU LOKAL PERANGKAT · 24 JAM", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4C7663))
        }

        PixelPanel(padding = 10.dp) {
            Column {
                Text("AGENDA HARI INI", style = MaterialTheme.typography.labelMedium, color = Amber)
                Spacer(Modifier.height(8.dp))
                if (events.isEmpty()) {
                    Text("TIDAK ADA AGENDA HARI INI.", style = MaterialTheme.typography.labelSmall, color = Muted2)
                } else {
                    events.forEach { ev ->
                        val kind = EVENT_KIND[ev.jenis] ?: (Amber to "KEGIATAN")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    state.selectedDate = ev.date
                                    state.month = java.time.YearMonth.parse(ev.date.substring(0, 7))
                                    state.screen = Scr.KALENDER
                                }
                                .padding(vertical = 5.dp)
                        ) {
                            Text(ev.time, style = MaterialTheme.typography.labelSmall, color = Amber, modifier = Modifier.width(38.dp))
                            Column {
                                Text(ev.title, style = MaterialTheme.typography.titleMedium, fontSize = 12.sp, maxLines = 2)
                                Text(kind.second, style = MaterialTheme.typography.labelSmall, color = kind.first)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                GhostButton("Buka Kalender", { state.screen = Scr.KALENDER }, Modifier.fillMaxWidth())
            }
        }

        PixelPanel(padding = 10.dp) {
            Column {
                Text("KELAS CEPAT", style = MaterialTheme.typography.labelMedium, color = Amber)
                Spacer(Modifier.height(8.dp))
                CLASS_CODES.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { code ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .background(Panel)
                                    .border(2.dp, Ink)
                                    .clickable {
                                        state.absClass = code
                                        state.screen = Scr.ABSENSI
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(code, style = MaterialTheme.typography.labelLarge, color = Cream)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        PixelPanel(padding = 10.dp) {
            Column {
                Text("PROGRES", style = MaterialTheme.typography.labelMedium, color = Amber)
                Spacer(Modifier.height(8.dp))
                val xp = (600 + state.journal.size * 35 + state.notes.size * 12).coerceAtMost(1000)
                Text("LEVEL 7 · $xp / 1000 XP", style = MaterialTheme.typography.labelSmall, color = Cream2)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(Ink).border(1.dp, Ink)) {
                    Box(
                        Modifier
                            .fillMaxWidth(xp / 1000f)
                            .fillMaxHeight()
                            .background(Amber)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("SELESAIKAN 1 JURNAL = +25 XP", style = MaterialTheme.typography.labelSmall, color = Muted2)
            }
        }

        PixelPanel(padding = 10.dp) {
            Column {
                Text("BINDER TERBARU", style = MaterialTheme.typography.labelMedium, color = Amber)
                Spacer(Modifier.height(8.dp))
                state.docs.take(3).forEach { doc ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { state.screen = Scr.BINDER }
                            .padding(vertical = 5.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                doc.nama,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Cream2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(doc.jenis.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted2)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${state.docProgress(doc)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber
                        )
                    }
                }
            }
        }
    }
}

/* ---------------- scanline ---------------- */

@Composable
private fun Scanlines() {
    Canvas(Modifier.fillMaxSize()) {
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color(0x1A000000),
                topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, 1f)
            )
            y += 3f
        }
    }
}

/* ---------------- pengaturan ---------------- */

@Composable
private fun SettingsDialog(state: AppState, onClose: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }

    PixelDialog("Pengaturan Tampilan", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Preferensi disimpan otomatis di perangkat.", style = MaterialTheme.typography.bodyMedium, color = Muted)

            ToggleRow("Detail art 64 px", state.art64) {
                state.art64 = !state.art64
                state.save()
            }
            ToggleRow("Scanline CRT", state.scanlines) {
                state.scanlines = !state.scanlines
                state.save()
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Tutup", onClose, Modifier.weight(1f))
                PixelButton("Design Kit", {
                    state.screen = Scr.KIT
                    onClose()
                }, Modifier.weight(1f))
            }

            if (!confirmReset) {
                GhostButton("Reset Data Contoh", { confirmReset = true }, Modifier.fillMaxWidth())
            } else {
                PixelPanel(background = Color(0xFF2A1B22), padding = 10.dp) {
                    Column {
                        Text(
                            "Semua catatan, jurnal, agenda, dan nilai akan kembali ke data contoh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Cream2
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GhostButton("Batal", { confirmReset = false }, Modifier.weight(1f))
                            PixelButton("Reset", {
                                state.reset()
                                confirmReset = false
                                onClose()
                            }, Modifier.weight(1f), background = Coral)
                        }
                    }
                }
            }
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

/* ---------------- host layar ---------------- */

@Composable
private fun ScreenHost(state: AppState, wide: Boolean) {
    when (state.screen) {
        Scr.HOME -> HomeScreen(state)
        Scr.JURNAL -> JournalScreen(state)
        Scr.CATATAN -> NotesScreen(state)
        Scr.ABSENSI -> AttendanceScreen(state, wide)
        Scr.NILAI -> GradesScreen(state, wide)
        Scr.JADWAL -> ScheduleScreen(state)
        Scr.KALENDER -> CalendarScreen(state)
        Scr.KALKULATOR -> CalculatorScreen(state)
        Scr.BINDER -> BinderScreen(state)
        Scr.KELAS -> ClassesScreen(state)
        Scr.PROFIL -> ProfileScreen(state)
        Scr.KIT -> DesignKitScreen()
    }
}

/* ---------------- beranda ---------------- */

@Composable
private fun HomeScreen(state: AppState) {
    val clock = rememberClock()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(greeting(), style = MaterialTheme.typography.labelMedium, color = Amber)
                Text("Bu Ratna", style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.background(Panel).border(2.dp, Ink).padding(horizontal = 8.dp, vertical = 5.dp)) {
                Text(
                    java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("EEE · d MMM", ID_LOCALE))
                        .uppercase(ID_LOCALE),
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream2
                )
            }
            Spacer(Modifier.width(8.dp))
            IconBox("calendar", { state.screen = Scr.KALENDER })
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1.05f)) {
                SceneCard(state, clock)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        StatBox("3", "Kelas hari ini", Amber) { state.screen = Scr.JADWAL }
                    }
                    Box(Modifier.weight(1f)) {
                        StatBox("96", "Total siswa", Mint) { state.screen = Scr.KELAS }
                    }
                    Box(Modifier.weight(1f)) {
                        StatBox(state.notes.size.toString(), "Catatan aktif", Sky) { state.screen = Scr.CATATAN }
                    }
                    Box(Modifier.weight(1f)) {
                        StatBox(state.docs.size.toString(), "Dokumen binder", Cream2) { state.screen = Scr.BINDER }
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                SectionHead("Tools mengajar", "Design Kit") { state.screen = Scr.KIT }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tools = listOf(
                        Triple("book", "Jurnal" to "Catat pertemuan", Scr.JURNAL) ,
                        Triple("pencil", "Catatan" to "Ide & refleksi", Scr.CATATAN),
                        Triple("check", "Absensi" to "Kehadiran", Scr.ABSENSI),
                        Triple("chart", "Nilai" to "Tugas & ujian", Scr.NILAI),
                        Triple("clock", "Jadwal" to "Mingguan", Scr.JADWAL),
                        Triple("calendar", "Kalender" to "Agenda & ujian", Scr.KALENDER),
                        Triple("calc", "Kalkulator" to "Hitung nilai", Scr.KALKULATOR),
                        Triple("folder", "Binder" to "Materi pelajaran", Scr.BINDER)
                    )
                    tools.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (glyph, info, target) ->
                                Box(Modifier.weight(1f)) {
                                    ToolCard(glyph, info.first, info.second, Amber) { state.screen = target }
                                }
                            }
                        }
                    }
                }

                SectionHead("Berikutnya", "Jadwal") { state.screen = Scr.JADWAL }
                PixelPanel(padding = 10.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("09:15", style = MaterialTheme.typography.labelLarge, color = Amber)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.width(2.dp).height(30.dp).background(Panel3))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("IPA · Kelas 8A", style = MaterialTheme.typography.titleMedium)
                            Text("LAB SAINS · 09:15–10:35", style = MaterialTheme.typography.labelSmall, color = Muted2)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.background(Coral).padding(horizontal = 5.dp, vertical = 3.dp)) {
                            Text("LANJUT", style = MaterialTheme.typography.labelSmall, color = Ink)
                        }
                    }
                }

                SectionHead("Catatan terakhir", "Semua") { state.screen = Scr.CATATAN }
                state.notes.sortedByDescending { it.date }.take(2).forEach { note ->
                    NoteCard(note, onPin = { state.togglePin(note.id) }, onClick = { state.screen = Scr.CATATAN })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SceneCard(state: AppState, clock: String) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(SCENE_W.toFloat() / SCENE_H.toFloat())
            .border(3.dp, Ink)
            .background(Ink)
    ) {
        val mw = maxWidth
        val mh = maxHeight
        val ctx = LocalContext.current
        val bmp = remember(state.art64) {
            BitmapFactory.decodeResource(
                ctx.resources,
                if (state.art64) R.drawable.scene64 else R.drawable.scene
            ).asImageBitmap()
        }
        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                image = bmp,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bmp.width, bmp.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.None
            )
        }

        val dot = 34.dp
        TILES.forEach { tile ->
            val cx = (tile.x + tile.w / 2f) / SCENE_W.toFloat()
            val cy = (tile.y + tile.h / 2f) / SCENE_H.toFloat()
            Box(
                Modifier
                    .offset(x = mw * cx - dot / 2, y = mh * cy - dot / 2)
                    .size(dot)
                    .background(Color(0xE6F8E9C8))
                    .border(2.dp, Ink)
                    .clickable { state.screen = tile.target },
                contentAlignment = Alignment.Center
            ) {
                PixelGlyph(tile.glyph, Ink, 13.dp)
            }
        }

        // jam LED hidup di atas jam digital pada scene
        val jam = TILES.first { it.label == "Jadwal" }
        val jx = (jam.x + jam.w / 2f) / SCENE_W.toFloat()
        val jy = (jam.y + jam.h / 2f) / SCENE_H.toFloat()
        Box(
            Modifier
                .offset(x = mw * jx - mw * 0.062f, y = mh * jy - 10.dp)
                .width(mw * 0.124f)
                .height(20.dp)
                .background(Color(0xFF0B1410))
                .border(1.dp, Color(0xFF3E6B57))
                .clickable { state.screen = Scr.JADWAL },
            contentAlignment = Alignment.Center
        ) {
            Text(clock, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = ScreenGreen)
        }

        Box(
            Modifier
                .padding(8.dp)
                .background(Amber)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text("RUANG KERJA · $clock", style = MaterialTheme.typography.labelSmall, color = Ink)
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            val xp = (600 + state.journal.size * 35 + state.notes.size * 12).coerceAtMost(1000)
            Text(
                "LEVEL 7 · GURU SIGAP   $xp / 1000 XP",
                style = MaterialTheme.typography.labelSmall,
                color = Cream
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(mw * 0.42f).height(7.dp).background(Color(0xCC0A0A14)).border(1.dp, Ink)) {
                Box(Modifier.fillMaxWidth(xp / 1000f).fillMaxHeight().background(Amber))
            }
        }
    }
}

/* ---------------- kartu catatan (dipakai beberapa layar) ---------------- */

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
