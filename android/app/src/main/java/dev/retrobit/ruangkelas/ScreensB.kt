package dev.retrobit.ruangkelas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

/* ================= JADWAL ================= */

@Composable
fun ScheduleScreen(state: AppState) {
    val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
    val lessons = SCHEDULE[state.day] ?: emptyList()
    val totalLessons = SCHEDULE.values.sumOf { it.size }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Jadwal", "Semester ganjil · minggu 9 · $totalLessons pertemuan") {
            ChipRow(days.map { it.take(3).uppercase() }, state.day.take(3).uppercase()) { picked ->
                state.day = days.first { it.take(3).uppercase() == picked }
            }
        }

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1.35f).verticalScrollable()) {
                Text(state.day.uppercase(), style = MaterialTheme.typography.labelMedium, color = Amber)
                Spacer(Modifier.height(8.dp))

                if (lessons.isEmpty()) {
                    Text("TIDAK ADA JADWAL DI HARI INI.", style = MaterialTheme.typography.labelMedium, color = Muted2)
                }

                lessons.forEachIndexed { i, lesson ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (i % 2 == 0) Color(0x14F2AB3F) else Navy)
                            .border(2.dp, Ink)
                            .padding(12.dp)
                    ) {
                        Column(Modifier.width(64.dp)) {
                            Text(lesson.time, style = MaterialTheme.typography.labelLarge, color = Amber, fontSize = 14.sp)
                            Text("–${lesson.end}", style = MaterialTheme.typography.labelSmall, color = Muted2)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("IPA · ${lesson.kelas}", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                Tag("JAM ${i + 1}", Teal)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(lesson.materi, style = MaterialTheme.typography.bodyMedium, color = Cream2)
                            Spacer(Modifier.height(4.dp))
                            Text(lesson.room.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted2)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(4.dp))
                PixelPanel(padding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("RINGKASAN ${state.day.uppercase()}", style = MaterialTheme.typography.labelMedium, color = Amber)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${lessons.size} pertemuan · ${lessons.map { it.kelas }.distinct().size} kelas · mulai ${lessons.firstOrNull()?.time ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted2
                            )
                        }
                        GhostButton("Catat Jurnal", { state.screen = Scr.JURNAL })
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelPanel(padding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("PEKAN INI", style = MaterialTheme.typography.labelMedium, color = Amber)
                        Spacer(Modifier.height(8.dp))
                        days.forEach { day ->
                            val list = SCHEDULE[day] ?: emptyList()
                            val active = day == state.day
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (active) Color(0x1FF2AB3F) else Color.Transparent)
                                    .clickable { state.day = day }
                                    .padding(horizontal = 6.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    day.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Amber else Cream2,
                                    modifier = Modifier.width(64.dp)
                                )
                                Text(
                                    "${list.size} PERTEMUAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Muted2,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    list.firstOrNull()?.let { it.time } ?: "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Amber else Muted2
                                )
                            }
                        }
                    }
                }

                PixelPanel(padding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("BEBAN MENGAJAR", style = MaterialTheme.typography.labelMedium, color = Amber)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox("$totalLessons", "Pertemuan", Amber) { }
                            StatBox("5", "Hari", Sky) { }
                            StatBox("4", "Kelas", Mint) { state.screen = Scr.KELAS }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "TOTAL 16 JAM PELAJARAN · LAB SAINS 3 SESI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2
                        )
                    }
                }
            }
        }
    }
}

/* ================= KALENDER ================= */

@Composable
fun CalendarScreen(state: AppState) {
    var showNew by remember { mutableStateOf(false) }
    val ym = state.month
    val first = ym.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val daysInMonth = ym.lengthOfMonth()
    val prevMonth = ym.minusMonths(1)
    val cells = (0 until 42).map { i ->
        val dayIndex = i - offset
        when {
            dayIndex < 0 -> prevMonth.atDay(prevMonth.lengthOfMonth() + dayIndex + 1) to false
            dayIndex >= daysInMonth -> ym.plusMonths(1).atDay(dayIndex - daysInMonth + 1) to false
            else -> ym.atDay(dayIndex + 1) to true
        }
    }
    val monthEvents = state.agenda.count { it.date.startsWith(ym.toString()) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Kalender", "$monthEvents agenda bulan ini · ujian · rapat") {
            GhostButton("<", { state.month = ym.minusMonths(1) })
            Text(monthLabel(ym), style = MaterialTheme.typography.labelLarge, color = Cream)
            GhostButton(">", { state.month = ym.plusMonths(1) })
            PixelButton("Agenda", { showNew = true })
        }

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1.2f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN").forEach { d ->
                        Text(
                            d,
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    for (week in 0 until 6) {
                        Row(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (d in 0 until 7) {
                                val (date, inMonth) = cells[week * 7 + d]
                                val iso = date.toString()
                                val selected = iso == state.selectedDate
                                val today = iso == LocalDate.now().toString()
                                val events = state.eventsOn(iso).size

                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(2.dp)
                                        .background(if (selected) Amber else if (inMonth) Navy else Color(0x6615142A))
                                        .border(2.dp, if (today && !selected) Amber else Ink)
                                        .clickable {
                                            state.selectedDate = iso
                                            state.month = YearMonth.from(date)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${date.dayOfMonth}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontFamily = UiFont,
                                            fontSize = 15.sp,
                                            color = when {
                                                selected -> Ink
                                                !inMonth -> Muted2
                                                today -> Amber
                                                else -> Cream2
                                            }
                                        )
                                        if (events > 0) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "•".repeat(events.coerceAtMost(3)),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.sp,
                                                color = if (selected) Ink else Sky
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AGENDA · ${fmtLongDay(state.selectedDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Cream2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "+ TAMBAH",
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber,
                        modifier = Modifier.clickable { showNew = true }
                    )
                }
                Spacer(Modifier.height(8.dp))

                Column(Modifier.verticalScrollable()) {
                    val events = state.eventsOn(state.selectedDate).sortedBy { it.time }
                    if (events.isEmpty()) {
                        Text("BELUM ADA AGENDA DI TANGGAL INI.", style = MaterialTheme.typography.labelSmall, color = Muted2)
                        Spacer(Modifier.height(8.dp))
                    }
                    events.forEach { ev ->
                        val kind = EVENT_KIND[ev.jenis] ?: (Amber to "KEGIATAN")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Navy)
                                .border(2.dp, Ink)
                                .padding(8.dp)
                        ) {
                            Box(Modifier.width(4.dp).height(34.dp).background(kind.first))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ev.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text("${ev.time} · ${kind.second}", style = MaterialTheme.typography.labelSmall, color = kind.first)
                            }
                            Box(
                                Modifier.size(24.dp).clickable { state.deleteAgenda(ev.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("×", style = MaterialTheme.typography.labelLarge, color = Coral)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    Spacer(Modifier.height(4.dp))
                    PixelPanel(padding = 10.dp, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("AGENDA MENDATANG", style = MaterialTheme.typography.labelMedium, color = Amber)
                            Spacer(Modifier.height(6.dp))
                            val upcoming = state.agenda.filter { it.date > state.selectedDate }.sortedBy { it.date }.take(4)
                            if (upcoming.isEmpty()) {
                                Text("TIDAK ADA AGENDA LAIN.", style = MaterialTheme.typography.labelSmall, color = Muted2)
                            }
                            upcoming.forEach { ev ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Text(
                                        fmtDate(ev.date, "d MMM"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Amber,
                                        modifier = Modifier.width(66.dp)
                                    )
                                    Text(
                                        ev.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Cream2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNew) NewAgendaDialog(state) { showNew = false }
}

@Composable
private fun NewAgendaDialog(state: AppState, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("07:30") }
    var jenis by remember { mutableStateOf("KEGIATAN") }
    var error by remember { mutableStateOf("") }

    PixelDialog("Agenda Baru", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tanggal: ${fmtLongDay(state.selectedDate)}", style = MaterialTheme.typography.labelMedium, color = Amber)
            PixelField("Judul agenda", title, { title = it }, placeholder = "mis. Rapat wali murid")
            PixelField("Jam", time, { time = it })
            ChipRow(listOf("KEGIATAN", "UJIAN", "RAPAT", "LIBUR", "ADMIN"), jenis) { jenis = it }
            if (error.isNotEmpty()) Text(error, style = MaterialTheme.typography.labelSmall, color = Coral)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Batal", onClose, Modifier.weight(1f))
                PixelButton("Simpan", {
                    if (title.isBlank()) {
                        error = "JUDUL MASIH KOSONG"
                    } else {
                        val key = when (jenis) {
                            "UJIAN" -> "ujian"
                            "RAPAT" -> "rapat"
                            "LIBUR" -> "libur"
                            "ADMIN" -> "admin"
                            else -> "kegiatan"
                        }
                        state.addAgenda(state.selectedDate, title.trim(), time.ifBlank { "—" }, key)
                        onClose()
                    }
                }, Modifier.weight(1f))
            }
        }
    }
}

/* ================= KALKULATOR ================= */

fun evalExpr(raw: String): Double? {
    val clean = raw.replace("×", "*").replace("÷", "/")
    val tokens = Regex("""(\d+\.?\d*|\.\d+|[+\-*/])""").findAll(clean).map { it.value }.toList()
    if (tokens.isEmpty()) return null
    tokens.forEachIndexed { i, t ->
        val isNum = t[0].isDigit() || t[0] == '.'
        if (isNum != (i % 2 == 0)) return null
    }
    val nums = mutableListOf<Double>()
    val ops = mutableListOf<String>()
    tokens.forEachIndexed { i, t ->
        if (i % 2 == 0) nums.add(t.toDoubleOrNull() ?: return null) else ops.add(t)
    }
    var i = 0
    while (i < ops.size) {
        if (ops[i] == "*" || ops[i] == "/") {
            val a = nums[i]
            val b = nums[i + 1]
            val r = if (ops[i] == "*") a * b else {
                if (b == 0.0) return null
                a / b
            }
            nums[i] = r
            nums.removeAt(i + 1)
            ops.removeAt(i)
        } else {
            i++
        }
    }
    var acc = nums.firstOrNull() ?: return null
    ops.forEachIndexed { k, op ->
        acc = if (op == "+") acc + nums[k + 1] else acc - nums[k + 1]
    }
    return acc
}

fun fmtNum(v: Double): String {
    val rounded = Math.round(v * 100.0) / 100.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
}

@Composable
fun CalculatorScreen(state: AppState) {
    val modes = listOf("HITUNG", "NILAI", "KONVERSI")

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Kalkulator", "Hitung cepat · nilai · konversi") {
            ChipRow(modes, state.calcMode) { state.calcMode = it }
        }

        when (state.calcMode) {
            "NILAI" -> CalcNilai()
            "KONVERSI" -> CalcKonversi()
            else -> CalcHitung(state)
        }
    }
}

@Composable
private fun CalcHitung(state: AppState) {
    val value = evalExpr(state.calcExpr)

    Row(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1.1f)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101A16))
                    .border(2.dp, Color(0xFF2C4A3B))
                    .padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    state.calcExpr.ifBlank { "KETIK ANGKA" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6F9F86),
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (value == null) "0" else fmtNum(value),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 38.sp,
                    color = ScreenGreen
                )
            }
            Spacer(Modifier.height(10.dp))

            val keys = listOf(
                listOf("AC", "DEL", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "=")
            )
            keys.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        val isOp = key in listOf("÷", "×", "−", "+", "%", "DEL")
                        val isEq = key == "="
                        val isAc = key == "AC"
                        Box(
                            Modifier
                                .weight(if (key == "=") 2f else 1f)
                                .height(56.dp)
                                .background(
                                    when {
                                        isEq -> Amber
                                        isAc -> Coral
                                        isOp -> Panel3
                                        else -> Panel
                                    }
                                )
                                .border(2.dp, Ink)
                                .clickable { state.calcExpr = pressKey(state.calcExpr, key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                key,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isEq || isAc) Ink else Cream
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Column(Modifier.weight(0.9f)) {
            PixelPanel(padding = 12.dp) {
                Column {
                    Text("CONTOH CEPAT", style = MaterialTheme.typography.labelMedium, color = Amber)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "RATA-RATA → (78+85+92)/3",
                        "NILAI AKHIR → 82*0.4+90*0.3+88*0.3",
                        "KEHADIRAN → 28/32*100"
                    ).forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = Cream2)
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "HASIL DAPAT LANGSUNG DIPAKAI UNTUK MENGISI NILAI DI MENU NILAI.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted2
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            PixelPanel(padding = 12.dp) {
                Column {
                    Text("RIWAYAT CEPAT", style = MaterialTheme.typography.labelMedium, color = Amber)
                    Spacer(Modifier.height(8.dp))
                    val samples = listOf("78+85+92", "28/32*100", "90*0.4+88*0.3")
                    samples.forEach { sample ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { state.calcExpr = sample }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sample, style = MaterialTheme.typography.labelSmall, color = Cream2, modifier = Modifier.weight(1f))
                            Text(
                                evalExpr(sample)?.let { fmtNum(it) } ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = Mint
                            )
                        }
                    }
                }
            }
        }
    }
}

fun pressKey(expr: String, key: String): String {
    return when (key) {
        "AC" -> ""
        "DEL" -> if (expr.isNotEmpty()) expr.dropLast(1) else ""
        "=" -> expr
        "÷" -> if (expr.isEmpty()) "" else expr + "/"
        "×" -> if (expr.isEmpty()) "" else expr + "*"
        "−" -> expr + "-"
        "%" -> if (expr.isEmpty()) "" else expr + "/100"
        else -> expr + key
    }
}

@Composable
private fun CalcNilai() {
    var wTugas by remember { mutableStateOf("40") }
    var wUts by remember { mutableStateOf("30") }
    var wUas by remember { mutableStateOf("30") }
    var nTugas by remember { mutableStateOf("82") }
    var nUts by remember { mutableStateOf("90") }
    var nUas by remember { mutableStateOf("88") }

    val wt = wTugas.toIntOrNull() ?: 0
    val wu = wUts.toIntOrNull() ?: 0
    val wa = wUas.toIntOrNull() ?: 0
    val wSum = (wt + wu + wa).coerceAtLeast(1)
    val nt = (nTugas.toIntOrNull() ?: 0).coerceIn(0, 100)
    val nu = (nUts.toIntOrNull() ?: 0).coerceIn(0, 100)
    val na = (nUas.toIntOrNull() ?: 0).coerceIn(0, 100)
    val total = Math.round((nt * wt + nu * wu + na * wa).toFloat() / wSum)

    Row(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("BOBOT PENILAIAN", style = MaterialTheme.typography.labelMedium, color = Amber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { PixelField("Tugas %", wTugas, { wTugas = it.filter(Char::isDigit) }) }
                Box(Modifier.weight(1f)) { PixelField("UTS %", wUts, { wUts = it.filter(Char::isDigit) }) }
                Box(Modifier.weight(1f)) { PixelField("UAS %", wUas, { wUas = it.filter(Char::isDigit) }) }
            }
            Text("NILAI SISWA", style = MaterialTheme.typography.labelMedium, color = Amber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { PixelField("Tugas", nTugas, { nTugas = it.filter(Char::isDigit).take(3) }) }
                Box(Modifier.weight(1f)) { PixelField("UTS", nUts, { nUts = it.filter(Char::isDigit).take(3) }) }
                Box(Modifier.weight(1f)) { PixelField("UAS", nUas, { nUas = it.filter(Char::isDigit).take(3) }) }
            }
        }

        Column(Modifier.weight(1f)) {
            PixelPanel(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$total",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 30.sp,
                        color = when {
                            total < KKM -> Coral
                            total < KKM + 8 -> Amber
                            else -> Mint
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "PREDIKAT " + when {
                                total >= 90 -> "A · SANGAT BAIK"
                                total >= 80 -> "B · BAIK"
                                total >= 70 -> "C · CUKUP"
                                else -> "D · PERLU BIMBINGAN"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Cream
                        )
                        Text(
                            if (total >= KKM) "DI ATAS KKM $KKM · TUNTAS" else "DI BAWAH KKM $KKM · REMEDIAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2
                        )
                        Text(
                            "BOBOT $wt/$wu/$wa · TOTAL ${wt + wu + wa}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PixelPanel(padding = 12.dp) {
                Text(
                    "NA = (TUGAS × %TUGAS) + (UTS × %UTS) + (UAS × %UAS). Ubah bobot sesuai kurikulum sekolah Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Cream2
                )
            }
        }
    }
}

@Composable
private fun CalcKonversi() {
    var input by remember { mutableStateOf("87") }
    val value = (input.toIntOrNull() ?: 0).coerceIn(0, 100)
    val scale4 = value / 25.0
    val predikat = when {
        value >= 90 -> "A" to "SANGAT BAIK"
        value >= 80 -> "B" to "BAIK"
        value >= 70 -> "C" to "CUKUP"
        else -> "D" to "PERLU BIMBINGAN"
    }

    Row(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(220.dp)) {
                PixelField("Nilai skala 100", input, { input = it.filter(Char::isDigit).take(3) })
            }
            PixelPanel(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(predikat.first, style = MaterialTheme.typography.labelLarge, fontSize = 30.sp, color = Amber)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(predikat.second, style = MaterialTheme.typography.labelSmall, color = Cream)
                        Text("SKALA 4: ${"%.2f".format(scale4)}", style = MaterialTheme.typography.labelSmall, color = Mint)
                    }
                }
            }
        }

        Column(Modifier.weight(1f)) {
            PixelPanel(padding = 12.dp) {
                Column {
                    Text("TABEL PREDIKAT", style = MaterialTheme.typography.labelMedium, color = Amber)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        Triple("A", "90–100", "SANGAT BAIK · 4.00"),
                        Triple("B", "80–89", "BAIK · 3.00–3.60"),
                        Triple("C", "70–79", "CUKUP · 2.00–2.80"),
                        Triple("D", "< 70", "PERLU BIMBINGAN · < 2.00")
                    ).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(row.first, style = MaterialTheme.typography.labelLarge, color = Cream, modifier = Modifier.width(20.dp))
                            Text(row.second, style = MaterialTheme.typography.labelSmall, color = Cream2, modifier = Modifier.width(60.dp))
                            Text(row.third, style = MaterialTheme.typography.labelSmall, color = Muted2)
                        }
                    }
                }
            }
        }
    }
}

/* ================= BINDER ================= */

@Composable
fun BinderScreen(state: AppState) {
    var showNew by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Int?>(null) }
    val filters = listOf("SEMUA", "Modul Ajar", "LKPD", "Bank Soal", "Administrasi")
    val rows = state.docs.withIndex().filter {
        state.binderFilter == "SEMUA" || it.value.jenis == state.binderFilter
    }
    val totalChecks = state.docs.sumOf { it.checks.size }
    val doneChecks = state.docs.sumOf { doc -> doc.checks.count { it } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Binder Materi", "IPA · ${state.docs.size} dokumen · $doneChecks/$totalChecks cek") {
            ChipRow(filters, state.binderFilter) { state.binderFilter = it }
            PixelButton("Unggah", { showNew = true })
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Amber)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelGlyph("folder", Ink, 13.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "LACI BINDER TERBUKA · SEMUA DOKUMEN MATERI TERSIMPAN DI SINI",
                style = MaterialTheme.typography.labelSmall,
                color = Ink
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumnVertical {
            rows.forEach { entry ->
                val doc = entry.value
                val idx = entry.index
                DocCard(doc, state.docProgress(doc)) { detail = idx }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showNew) NewDocDialog(state) { showNew = false }
    detail?.let { idx ->
        state.docs.getOrNull(idx)?.let { doc ->
            DocDetailDialog(state, doc, idx) { detail = null }
        }
    }
}

@Composable
private fun LazyColumnVertical(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) { content() }
}

@Composable
private fun DocCard(doc: DocItem, progress: Int, onClick: () -> Unit) {
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
                .size(34.dp, 40.dp)
                .background(Cream2),
            contentAlignment = Alignment.Center
        ) { PixelGlyph("book", Ink, 18.dp) }

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(doc.jenis, DOC_TAB[doc.jenis] ?: Amber)
                Spacer(Modifier.width(6.dp))
                Text("${doc.format} · ${doc.size} · ${doc.kelas}", style = MaterialTheme.typography.labelSmall, color = Muted2)
            }
            Spacer(Modifier.height(3.dp))
            Text(doc.nama, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(7.dp).background(Ink)) {
                Box(Modifier.fillMaxWidth(progress / 100f).fillMaxHeight().background(Amber))
            }
            Spacer(Modifier.height(3.dp))
            Row {
                Text("CHECKLIST ${doc.checks.count { it }}/${doc.checks.size}", style = MaterialTheme.typography.labelSmall, color = Muted2)
                Spacer(Modifier.weight(1f))
                Text("$progress%", style = MaterialTheme.typography.labelSmall, color = Amber)
            }
        }
    }
}

@Composable
private fun DocDetailDialog(state: AppState, doc: DocItem, docIndex: Int, onClose: () -> Unit) {
    PixelDialog("Detail Dokumen", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(doc.nama, style = MaterialTheme.typography.titleLarge)
            Text(
                "${doc.jenis.uppercase()} · ${doc.kelas} · ${doc.format} · ${doc.size} · ${doc.updated}",
                style = MaterialTheme.typography.labelSmall,
                color = Muted2
            )
            Text("CHECKLIST KELENGKAPAN", style = MaterialTheme.typography.labelMedium, color = Amber)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                doc.checks.forEachIndexed { i, checked ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (checked) Color(0x1A9FD8B0) else Panel)
                            .border(2.dp, Ink)
                            .clickable { state.toggleDocCheck(docIndex, i) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(if (checked) Mint else Ink)
                                .border(2.dp, Ink),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) Text("✓", style = MaterialTheme.typography.labelSmall, color = Ink)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Bagian ${i + 1}", style = MaterialTheme.typography.bodyMedium, color = Cream2)
                    }
                }
            }
            GhostButton("Tutup", onClose, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NewDocDialog(state: AppState, onClose: () -> Unit) {
    var nama by remember { mutableStateOf("") }
    var jenis by remember { mutableStateOf("Modul Ajar") }
    var kelas by remember { mutableStateOf("7A") }
    var error by remember { mutableStateOf("") }

    PixelDialog("Unggah Dokumen", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelField("Nama dokumen", nama, { nama = it })
            ChipRow(listOf("Modul Ajar", "LKPD", "Bank Soal", "Administrasi"), jenis) { jenis = it }
            ChipRow(listOf("7A", "7B", "8A", "8B", "SEMUA"), kelas) { kelas = it }
            if (error.isNotEmpty()) Text(error, style = MaterialTheme.typography.labelSmall, color = Coral)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Batal", onClose, Modifier.weight(1f))
                PixelButton("Simpan", {
                    if (nama.isBlank()) error = "NAMA DOKUMEN MASIH KOSONG"
                    else {
                        state.addDoc(nama.trim(), jenis, kelas)
                        onClose()
                    }
                }, Modifier.weight(1f))
            }
        }
    }
}

/* ================= KELAS ================= */

@Composable
fun ClassesScreen(state: AppState) {
    var detail by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Kelas", "4 kelas · 96 siswa · IPA") {
            PixelButton("Absensi Hari Ini", {
                state.screen = Scr.ABSENSI
            }, background = Teal, contentColor = Color(0xFF06211D))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CLASS_CODES.take(2).forEach { code -> Box(Modifier.weight(1f)) { ClassCard(state, code) { detail = code } } }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CLASS_CODES.drop(2).forEach { code -> Box(Modifier.weight(1f)) { ClassCard(state, code) { detail = code } } }
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1.3f)) {
                    PixelPanel(padding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("PERLU PERHATIAN", style = MaterialTheme.typography.labelMedium, color = Coral)
                                Spacer(Modifier.weight(1f))
                                Text("NA DI BAWAH KKM $KKM", style = MaterialTheme.typography.labelSmall, color = Muted2)
                            }
                            Spacer(Modifier.height(8.dp))
                            val low = CLASS_CODES.flatMap { code ->
                                (state.grades[code] ?: emptyList()).mapIndexedNotNull { i, g ->
                                    if (g.na < KKM) Triple(code, i, g) else null
                                }
                            }.sortedBy { it.third.na }

                            if (low.isEmpty()) {
                                Text("SEMUA SISWA TUNTAS.", style = MaterialTheme.typography.labelSmall, color = Mint)
                            }
                            low.take(6).forEach { (code, index, g) ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            state.gradeClass = code
                                            state.screen = Scr.NILAI
                                        }
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Tag(code, Teal)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        g.nama,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Cream2,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text("${g.tugas} · ${g.uts} · ${g.uas}", style = MaterialTheme.typography.labelSmall, color = Muted2)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${g.na}", style = MaterialTheme.typography.labelLarge, color = Coral, fontSize = 13.sp)
                                }
                            }
                            if (low.size > 6) {
                                Spacer(Modifier.height(4.dp))
                                Text("+${low.size - 6} SISWA LAIN", style = MaterialTheme.typography.labelSmall, color = Muted2)
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    PixelPanel(padding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("AKSI CEPAT", style = MaterialTheme.typography.labelMedium, color = Amber)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GhostButton("Absensi", { state.screen = Scr.ABSENSI }, Modifier.weight(1f))
                                GhostButton("Nilai", { state.screen = Scr.NILAI }, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GhostButton("Jurnal", { state.screen = Scr.JURNAL }, Modifier.weight(1f))
                                GhostButton("Jadwal", { state.screen = Scr.JADWAL }, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "96 SISWA · 4 KELAS · KKM $KKM · BOBOT 40/30/30",
                                style = MaterialTheme.typography.labelSmall,
                                color = Muted2
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    detail?.let { code -> ClassDetailDialog(state, code) { detail = null } }
}

@Composable
private fun ClassCard(state: AppState, code: String, onClick: () -> Unit) {
    val info = CLASS_INFO[code] ?: ("Kelas" to 0)
    val rows = state.grades[code] ?: emptyList()
    val avg = if (rows.isEmpty()) 0 else rows.sumOf { it.na } / rows.size
    val attendance = state.attendance[code] ?: emptyList()
    val hadirPct = if (attendance.isEmpty()) 0 else attendance.count { it == "H" } * 100 / attendance.size
    val journalCount = state.journal.count { it.kelas == code }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .border(2.dp, Ink)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(Teal), contentAlignment = Alignment.Center) {
                Text(code, style = MaterialTheme.typography.labelLarge, color = Ink)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("${info.first} · IPA", style = MaterialTheme.typography.titleMedium)
                Text("${info.second} SISWA · $journalCount JURNAL · KKM $KKM", style = MaterialTheme.typography.labelSmall, color = Muted2)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).background(Ink)) {
            Box(Modifier.fillMaxWidth(hadirPct / 100f).fillMaxHeight().background(Mint))
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Text("KEHADIRAN $hadirPct%", style = MaterialTheme.typography.labelSmall, color = Muted2)
            Spacer(Modifier.weight(1f))
            Text("RATA NA $avg", style = MaterialTheme.typography.labelSmall, color = Amber)
        }
    }
}

@Composable
private fun ClassDetailDialog(state: AppState, code: String, onClose: () -> Unit) {
    val rows = state.grades[code] ?: emptyList()
    val avg = if (rows.isEmpty()) 0 else rows.sumOf { it.na } / rows.size
    val attendance = state.attendance[code] ?: emptyList()
    val counts = attendance.groupingBy { it }.eachCount()
    val journal = state.journal.filter { it.kelas == code }

    PixelDialog("Kelas $code", onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelPanel(padding = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$avg", style = MaterialTheme.typography.labelLarge, fontSize = 26.sp, color = if (avg < KKM) Coral else Mint)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("RATA-RATA NA $code", style = MaterialTheme.typography.labelSmall, color = Cream)
                        Text(
                            "H ${counts["H"] ?: 0} · S ${counts["S"] ?: 0} · I ${counts["I"] ?: 0} · A ${counts["A"] ?: 0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2
                        )
                        Text("${journal.size} JURNAL TERCATAT", style = MaterialTheme.typography.labelSmall, color = Muted2)
                    }
                }
            }

            Text("JURNAL TERAKHIR", style = MaterialTheme.typography.labelMedium, color = Amber)
            journal.take(3).forEach { j ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(j.time, style = MaterialTheme.typography.labelSmall, color = Amber, modifier = Modifier.width(46.dp))
                    Text(j.materi, style = MaterialTheme.typography.bodyMedium, color = Cream2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Absensi", {
                    state.absClass = code
                    state.screen = Scr.ABSENSI
                    onClose()
                }, Modifier.weight(1f))
                GhostButton("Nilai", {
                    state.gradeClass = code
                    state.screen = Scr.NILAI
                    onClose()
                }, Modifier.weight(1f))
                PixelButton("Jurnal", {
                    state.journalFilter = code
                    state.screen = Scr.JURNAL
                    onClose()
                }, Modifier.weight(1f))
            }
        }
    }
}

/* ================= PROFIL ================= */

@Composable
fun ProfileScreen(state: AppState) {
    val badges = listOf(
        "100 JURNAL" to true, "30 HARI" to true, "SIGAP" to true, "JUARA" to false,
        "ARSIP" to false, "HITUNG" to false, "PENULIS" to false, "DISIPLIN" to false
    )

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Profil", "Akun guru · lokal") {
            PixelButton("Design Kit", { state.screen = Scr.KIT })
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(Teal)
                            .border(3.dp, Ink),
                        contentAlignment = Alignment.Center
                    ) { PixelGlyph("face", Ink, 40.dp) }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text("Ratna Kusumaningrum", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "NIP 19870412 201001 2 004\nSMP NEGERI 1 SEMARANG\nGURU IPA · 2026/2027",
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { StatBox("128", "Jurnal", Amber) { state.screen = Scr.JURNAL } }
                    Box(Modifier.weight(1f)) { StatBox("740", "XP", Mint) { } }
                    Box(Modifier.weight(1f)) { StatBox("4", "Kelas", Sky) { state.screen = Scr.KELAS } }
                }

                SectionHead("Lencana", "3 / 8")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.take(4).forEach { (label, unlocked) -> BadgeBox(label, unlocked) }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.drop(4).forEach { (label, unlocked) -> BadgeBox(label, unlocked) }
                }
            }

            Column(Modifier.weight(1f)) {
                SectionHead("Pengaturan")
                ToggleRow("Detail art 64 px", state.art64) {
                    state.art64 = !state.art64
                    state.save()
                }
                ToggleRow("Scanline CRT", state.scanlines) {
                    state.scanlines = !state.scanlines
                    state.save()
                }
                ToggleRow("Data tersimpan lokal", true) { }
                Spacer(Modifier.height(8.dp))
                GhostButton("Buka Design Kit", { state.screen = Scr.KIT }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                GhostButton("Reset Data Contoh", { state.reset() }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BadgeBox(label: String, unlocked: Boolean) {
    Column(
        Modifier
            .size(64.dp)
            .background(if (unlocked) Panel else Color(0x33FFFFFF))
            .border(2.dp, Ink)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PixelGlyph("star", if (unlocked) Amber else Muted2, 20.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) Cream2 else Muted2,
            maxLines = 1,
            fontSize = 6.sp
        )
    }
}

/* ================= DESIGN KIT ================= */

@Composable
fun DesignKitScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("DESIGN KIT", style = MaterialTheme.typography.displayLarge, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Token yang sama dipakai aplikasi ini dan siap disalin ke Compose.",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        Spacer(Modifier.height(14.dp))

        SectionHead("Warna inti")
        PALETTE.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, color) ->
                    Column(Modifier.width(88.dp)) {
                        Box(Modifier.fillMaxWidth().height(30.dp).background(color).border(2.dp, Ink))
                        Spacer(Modifier.height(3.dp))
                        Text(name.uppercase(), style = MaterialTheme.typography.labelSmall, color = Cream2)
                        Text(
                            "#" + Integer.toHexString(color.value.toLong().shr(32).toInt()).takeLast(6).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted2
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        SectionHead("Tipografi")
        Text("RUANG KELAS", style = MaterialTheme.typography.displayLarge)
        Text("Press Start 2P · judul & logo", style = MaterialTheme.typography.labelSmall, color = Muted2)
        Spacer(Modifier.height(6.dp))
        Text("Binder Materi", style = MaterialTheme.typography.headlineMedium)
        Text("Pixelify Sans · heading & isi", style = MaterialTheme.typography.labelSmall, color = Muted2)
        Spacer(Modifier.height(6.dp))
        Text("09:12 · NA 87 · PREDIKAT B", style = MaterialTheme.typography.labelLarge, color = Amber)
        Text("Silkscreen · label, angka, jam", style = MaterialTheme.typography.labelSmall, color = Muted2)

        SectionHead("Peta area tap scene (grid 51 × 32)")
        TILES.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = Cream2, modifier = Modifier.width(90.dp))
                Text(t.target.label, style = MaterialTheme.typography.labelSmall, color = Muted2, modifier = Modifier.width(120.dp))
                Text("${t.x} · ${t.y} · ${t.w}×${t.h}", style = MaterialTheme.typography.labelSmall, color = Amber)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Aplikasi: Kotlin + Jetpack Compose · minSdk 26 · landscape · art 51×32 piksel (32 warna) · font OFL.",
            style = MaterialTheme.typography.labelSmall,
            color = Muted2
        )
        Spacer(Modifier.height(20.dp))
    }
}
