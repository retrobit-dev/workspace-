package dev.retrobit.ruangkelas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle.uppercase(), style = MaterialTheme.typography.labelMedium, color = Amber)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions()
        }
    }
}

fun noteAccent(note: Note): Color = when (note.cat) {
    "IDE" -> Amber
    "REFLEKSI" -> Teal
    "TO-DO" -> Sky
    else -> Amber
}

/* ================= JURNAL ================= */

@Composable
fun JournalScreen(state: AppState) {
    var showNew by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Journal?>(null) }
    val filters = listOf("SEMUA") + CLASS_CODES

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Jurnal", "Catatan mengajar") {
            PixelButton("Catat Pertemuan", { showNew = true })
        }

        Box(Modifier.padding(horizontal = 16.dp)) {
            ChipRow(filters, state.journalFilter) { state.journalFilter = it }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.filteredJournal(), key = { it.id }) { j ->
                JournalCard(j) { detail = j }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showNew) NewJournalDialog(state) { showNew = false }
    detail?.let { j ->
        JournalDetailDialog(j, state) { detail = null }
    }
}

@Composable
private fun JournalCard(j: Journal, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .border(2.dp, Ink)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .width(46.dp)
                .height(48.dp)
                .background(Amber),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fmtDayNum(j.date), style = MaterialTheme.typography.labelLarge, color = Ink, fontSize = 15.sp)
                Text(fmtDayMon(j.date), style = MaterialTheme.typography.labelSmall, color = Ink, fontSize = 8.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(j.kelas, Teal)
                Spacer(Modifier.width(6.dp))
                Text("JAM ${j.jam} · ${j.time}", style = MaterialTheme.typography.labelSmall, color = Muted2)
            }
            Spacer(Modifier.height(3.dp))
            Text(j.materi, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(j.note, style = MaterialTheme.typography.bodyMedium, color = Cream2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                "${j.hadir}/${j.total} HADIR",
                style = MaterialTheme.typography.labelSmall,
                color = Mint
            )
        }
    }
}

@Composable
fun NewJournalDialog(state: AppState, onClose: () -> Unit) {
    var kelas by remember { mutableStateOf("8A") }
    var jam by remember { mutableStateOf("3–4") }
    var materi by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var hadir by remember { mutableStateOf("22") }
    var total by remember { mutableStateOf("24") }
    var error by remember { mutableStateOf("") }

    PixelDialog("Catat Pertemuan", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pilih kelas", style = MaterialTheme.typography.labelMedium, color = Amber)
            ChipRow(CLASS_CODES, kelas) { kelas = it }

            PixelField("Materi", materi, { materi = it })
            PixelField("Catatan / refleksi", note, { note = it }, singleLine = false)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { PixelField("Jam ke", jam, { jam = it }) }
                Box(Modifier.weight(1f)) { PixelField("Hadir", hadir, { hadir = it.filter(Char::isDigit) }) }
                Box(Modifier.weight(1f)) { PixelField("Dari", total, { total = it.filter(Char::isDigit) }) }
            }

            if (error.isNotEmpty()) {
                Text(error, style = MaterialTheme.typography.labelSmall, color = Coral)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Batal", onClose, Modifier.weight(1f))
                PixelButton("Simpan", {
                    if (materi.isBlank()) {
                        error = "MATERI MASIH KOSONG"
                    } else {
                        val h = hadir.toIntOrNull() ?: 0
                        val t = (total.toIntOrNull() ?: h).coerceAtLeast(h)
                        state.addJournal(kelas, jam.ifBlank { "—" }, materi.trim(), note.trim(), h, t)
                        onClose()
                    }
                }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun JournalDetailDialog(j: Journal, state: AppState, onClose: () -> Unit) {
    val pct = if (j.total == 0) 0 else (j.hadir * 100) / j.total

    PixelDialog("Detail Jurnal ${j.kelas}", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelPanel(padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$pct%", style = MaterialTheme.typography.labelLarge, fontSize = 26.sp, color = Mint)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(j.materi, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text(
                            "${fmtDate(j.date, "d MMM yyyy")} · JAM ${j.jam} · ${j.time}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2
                        )
                        Text("${j.hadir} DARI ${j.total} SISWA HADIR", style = MaterialTheme.typography.labelSmall, color = Muted2)
                    }
                }
            }
            Text(j.note, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Absensi", {
                    state.absClass = j.kelas
                    state.screen = Scr.ABSENSI
                    onClose()
                }, Modifier.weight(1f))
                PixelButton("Hapus", {
                    state.deleteJournal(j.id)
                    onClose()
                }, Modifier.weight(1f), background = Coral)
            }
        }
    }
}

/* ================= CATATAN ================= */

@Composable
fun NotesScreen(state: AppState) {
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Note?>(null) }
    val filters = listOf("SEMUA", "DIPIN", "IDE", "REFLEKSI", "TO-DO")

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Buku Catatan", "${state.notes.size} catatan · ${state.notes.count { it.pinned }} dipin") {
            PixelButton("Tulis Catatan", { showNew = true })
        }

        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.width(260.dp)) {
                PixelField(
                    "Cari catatan",
                    state.noteQuery,
                    { state.noteQuery = it },
                    placeholder = "judul atau isi catatan"
                )
            }
            ChipRow(filters, state.noteFilter) { state.noteFilter = it }
        }

        Spacer(Modifier.height(12.dp))

        val rows = state.filteredNotes()
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("TIDAK ADA CATATAN YANG COCOK", style = MaterialTheme.typography.labelMedium, color = Muted2)
            }
        } else {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onPin = { state.togglePin(note.id) },
                        onClick = { editing = note },
                        accent = noteAccent(note)
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showNew) NoteEditorDialog(state, null) { showNew = false }
    editing?.let { note -> NoteEditorDialog(state, note) { editing = null } }
}

@Composable
fun NoteEditorDialog(state: AppState, existing: Note?, onClose: () -> Unit) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var body by remember { mutableStateOf(existing?.body ?: "") }
    var cat by remember { mutableStateOf(existing?.cat ?: "IDE") }
    var error by remember { mutableStateOf("") }

    PixelDialog(if (existing == null) "Catatan Baru" else "Edit Catatan", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChipRow(listOf("IDE", "REFLEKSI", "TO-DO"), cat) { cat = it }
            PixelField("Judul", title, { title = it })
            PixelField("Isi catatan", body, { body = it }, singleLine = false)

            if (error.isNotEmpty()) Text(error, style = MaterialTheme.typography.labelSmall, color = Coral)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing != null) {
                    PixelButton("Hapus", {
                        state.deleteNote(existing.id)
                        onClose()
                    }, Modifier.weight(1f), background = Coral)
                } else {
                    GhostButton("Batal", onClose, Modifier.weight(1f))
                }
                PixelButton("Simpan", {
                    if (title.isBlank()) {
                        error = "JUDUL MASIH KOSONG"
                    } else if (existing == null) {
                        state.addNote(title.trim(), body.trim(), cat)
                        onClose()
                    } else {
                        state.saveNote(existing.copy(title = title.trim(), body = body.trim(), cat = cat))
                        onClose()
                    }
                }, Modifier.weight(1f))
            }
        }
    }
}

/* ================= ABSENSI ================= */

@Composable
fun AttendanceScreen(state: AppState, wide: Boolean) {
    val names = studentsOf(state.absClass)
    val statuses = state.attendance[state.absClass] ?: names.map { "H" }
    val counts = statuses.groupingBy { it }.eachCount()
    val perColumn = if (wide) (names.size + 1) / 2 else names.size
    val columns = names.withIndex().chunked(perColumn)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Absensi", "${fmtDate(todayIso(), "EEEE, d MMM yyyy")} · kelas ${state.absClass}") {
            ChipRow(CLASS_CODES, state.absClass) { state.absClass = it }
            PixelButton("Simpan", { state.save() }, background = Teal, contentColor = Color(0xFF06211D))
        }

        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("H", "S", "I", "A").forEach { key ->
                PixelPanel(padding = 8.dp, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${counts[key] ?: 0}",
                            style = MaterialTheme.typography.labelLarge,
                            color = STATUS_COLOR[key] ?: Cream,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(STATUS_LABEL[key] ?: key, style = MaterialTheme.typography.labelSmall, color = Muted2)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            columns.forEach { chunk ->
                Column(Modifier.weight(1f).verticalScrollable()) {
                    chunk.forEach { entry ->
                        val index = entry.index
                        val status = statuses.getOrNull(index) ?: "H"
                        StudentRow(index, entry.value, status) { newStatus ->
                            state.setAttendance(state.absClass, index, newStatus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentRow(index: Int, nama: String, status: String, onStatus: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            color = Muted2,
            modifier = Modifier.width(20.dp)
        )
        Text(
            nama,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("H", "S", "I", "A").forEach { key ->
                Box(
                    Modifier
                        .size(26.dp)
                        .background(if (status == key) STATUS_COLOR[key] ?: Mint else Panel)
                        .border(2.dp, Ink)
                        .clickable { onStatus(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        key,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status == key) Ink else Muted
                    )
                }
            }
        }
    }
}

/* ================= NILAI ================= */

@Composable
fun GradesScreen(state: AppState, wide: Boolean) {
    val rows = state.grades[state.gradeClass] ?: emptyList()
    var editing by remember { mutableStateOf<Int?>(null) }
    val avg = if (rows.isEmpty()) 0 else rows.sumOf { it.na } / rows.size
    val low = rows.count { it.na < KKM }
    val max = rows.maxOfOrNull { it.na } ?: 0
    val perColumn = if (wide) (rows.size + 1) / 2 else rows.size.coerceAtLeast(1)
    val columns = rows.withIndex().chunked(perColumn)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Nilai", "Bobot · tugas 40% · uts 30% · uas 30%") {
            ChipRow(CLASS_CODES, state.gradeClass) { state.gradeClass = it }
            PixelButton("Simpan", { state.save() })
        }

        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PixelPanel(padding = 8.dp, modifier = Modifier.weight(1f)) {
                Column {
                    Text("RATA-RATA", style = MaterialTheme.typography.labelSmall, color = Muted2)
                    Text("$avg", style = MaterialTheme.typography.labelLarge, fontSize = 17.sp, color = Cream)
                }
            }
            PixelPanel(padding = 8.dp, modifier = Modifier.weight(1f)) {
                Column {
                    Text("TERTINGGI", style = MaterialTheme.typography.labelSmall, color = Muted2)
                    Text("$max", style = MaterialTheme.typography.labelLarge, fontSize = 17.sp, color = Mint)
                }
            }
            PixelPanel(padding = 8.dp, modifier = Modifier.weight(1f)) {
                Column {
                    Text("DI BAWAH KKM $KKM", style = MaterialTheme.typography.labelSmall, color = Muted2)
                    Text("$low", style = MaterialTheme.typography.labelLarge, fontSize = 17.sp, color = Coral)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            columns.forEach { chunk ->
                Column(Modifier.weight(1f).verticalScrollable()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NAMA", style = MaterialTheme.typography.labelSmall, color = Muted2, modifier = Modifier.weight(1f))
                        listOf("TGS", "UTS", "UAS", "NA").forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted2,
                                modifier = Modifier.width(if (it == "NA") 40.dp else 34.dp)
                            )
                        }
                    }
                    chunk.forEach { entry ->
                        val g = entry.value
                        val index = entry.index
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x1AFFFFFF))
                                .clickable { editing = index }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                g.nama,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${g.tugas}", style = MaterialTheme.typography.labelLarge, color = Cream2, modifier = Modifier.width(34.dp))
                            Text("${g.uts}", style = MaterialTheme.typography.labelLarge, color = Cream2, modifier = Modifier.width(34.dp))
                            Text("${g.uas}", style = MaterialTheme.typography.labelLarge, color = Cream2, modifier = Modifier.width(34.dp))
                            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "${g.na}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (g.na < KKM) Coral else if (g.na < KKM + 8) Amber else Mint
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { index ->
        val g = rows.getOrNull(index)
        if (g != null) {
            EditGradeDialog(g, state, state.gradeClass, index) { editing = null }
        }
    }
}

@Composable
private fun EditGradeDialog(g: Grade, state: AppState, kelas: String, index: Int, onClose: () -> Unit) {
    var tugas by remember { mutableStateOf(g.tugas.toString()) }
    var uts by remember { mutableStateOf(g.uts.toString()) }
    var uas by remember { mutableStateOf(g.uas.toString()) }

    val na = run {
        val t = tugas.toIntOrNull() ?: 0
        val u = uts.toIntOrNull() ?: 0
        val a = uas.toIntOrNull() ?: 0
        Math.round(t * 0.4f + u * 0.3f + a * 0.3f)
    }

    PixelDialog("Nilai ${g.nama}", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    PixelField("Tugas", tugas, { tugas = it.filter(Char::isDigit).take(3) })
                }
                Box(Modifier.weight(1f)) {
                    PixelField("UTS", uts, { uts = it.filter(Char::isDigit).take(3) })
                }
                Box(Modifier.weight(1f)) {
                    PixelField("UAS", uas, { uas = it.filter(Char::isDigit).take(3) })
                }
            }
            PixelPanel(padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$na", style = MaterialTheme.typography.labelLarge, fontSize = 24.sp, color = if (na < KKM) Coral else Mint)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("NILAI AKHIR", style = MaterialTheme.typography.labelSmall, color = Muted2)
                        Text(
                            if (na >= KKM) "TUNTAS · DI ATAS KKM $KKM" else "PERLU REMEDIAL · DI BAWAH KKM $KKM",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (na >= KKM) Mint else Amber
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Batal", onClose, Modifier.weight(1f))
                PixelButton("Simpan", {
                    state.setGrade(
                        kelas, index,
                        (tugas.toIntOrNull() ?: 0).coerceIn(0, 100),
                        (uts.toIntOrNull() ?: 0).coerceIn(0, 100),
                        (uas.toIntOrNull() ?: 0).coerceIn(0, 100)
                    )
                    onClose()
                }, Modifier.weight(1f))
            }
        }
    }
}

/* ---------------- util scroll ---------------- */

@Composable
fun Modifier.verticalScrollable(): Modifier = this.verticalScroll(rememberScrollState())
