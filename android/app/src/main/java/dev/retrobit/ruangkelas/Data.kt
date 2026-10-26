package dev.retrobit.ruangkelas

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------------- model ---------------- */

enum class Scr(val label: String) {
    HOME("Beranda"),
    JURNAL("Jurnal"),
    CATATAN("Buku Catatan"),
    ABSENSI("Absensi"),
    NILAI("Nilai"),
    JADWAL("Jadwal"),
    KALENDER("Kalender"),
    KALKULATOR("Kalkulator"),
    BINDER("Binder Materi"),
    KELAS("Kelas"),
    PROFIL("Profil"),
    KIT("Design Kit")
}

data class Journal(
    val id: Long,
    val date: String,
    val kelas: String,
    val jam: String,
    val time: String,
    val materi: String,
    val note: String,
    val hadir: Int,
    val total: Int
)

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val cat: String,
    val pinned: Boolean,
    val date: String
)

data class Agenda(
    val id: Long,
    val date: String,
    val title: String,
    val time: String,
    val jenis: String
)

data class DocItem(
    val nama: String,
    val jenis: String,
    val kelas: String,
    val format: String,
    val size: String,
    val updated: String,
    val checks: List<Boolean>
)

data class Grade(
    val nama: String,
    val tugas: Int,
    val uts: Int,
    val uas: Int
) {
    val na: Int get() = Math.round(tugas * 0.4f + uts * 0.3f + uas * 0.3f)
}

data class Lesson(val time: String, val end: String, val kelas: String, val room: String, val materi: String)

val STUDENTS_BY_CLASS: Map<String, List<String>> = mapOf(
    "7A" to listOf(
        "Aditya Pratama", "Bunga Lestari", "Citra Maharani", "Dimas Saputra",
        "Eka Wulandari", "Fajar Nugroho", "Gita Puspita", "Hendra Wijaya",
        "Intan Permata", "Joko Susilo", "Kirana Dewi", "Lukman Hakim",
        "Maya Anggraini", "Naufal Rizki", "Oktavia Sari", "Putra Mahendra",
        "Qori Amalia", "Raka Pradana", "Siti Aminah", "Taufik Hidayat",
        "Ulfa Rahmawati", "Vino Saputra", "Wulan Sari", "Yoga Pratama"
    ),
    "7B" to listOf(
        "Ahmad Fauzi", "Bella Kartika", "Candra Wijaya", "Dewi Lestari",
        "Eko Prasetyo", "Fitri Handayani", "Gilang Ramadhan", "Hana Safira",
        "Ilham Maulana", "Jihan Aulia", "Kurnia Sandi", "Laras Ayu",
        "Miftah Farid", "Nabila Zahra", "Oscar Simbolon", "Prasetya Aji",
        "Qonita Rahma", "Rizal Fahmi", "Salsabila Putri", "Teguh Santoso",
        "Umi Kalsum", "Vania Larasati", "Wisnu Adi", "Yuni Astuti"
    ),
    "8A" to listOf(
        "Agus Setiawan", "Bunga Citra", "Cahyo Nugroho", "Dinda Ayu",
        "Eri Kurniawan", "Farah Nabila", "Galih Pratama", "Hesti Wulandari",
        "Irfan Hakim", "Jasmine Aulia", "Kevin Sanjaya", "Lina Marlina",
        "Miko Ardiansyah", "Nadia Safitri", "Oka Mahendra", "Putri Amelia",
        "Rafi Ahmad", "Sinta Dewi", "Tio Bagaskara", "Uswatun Hasanah",
        "Vito Ramadhan", "Winda Lestari", "Yusuf Maulana", "Zulfa Hanifah"
    ),
    "8B" to listOf(
        "Arif Budiman", "Bela Safitri", "Cakra Wijaya", "Dewa Ayu",
        "Erna Wati", "Fikri Ramadhan", "Gita Savitri", "Hafiz Alfarizi",
        "Indah Permata", "Joni Iskandar", "Kartika Sari", "Luthfi Hakim",
        "Mila Kurnia", "Nanda Pratama", "Oki Setiawan", "Paramita Dewi",
        "Qomaruddin", "Rina Anggraini", "Satria Wibowo", "Tiara Puspita",
        "Ujang Solihin", "Vina Melati", "Wira Sanjaya", "Zahra Amelia"
    )
)

fun studentsOf(kelas: String): List<String> =
    STUDENTS_BY_CLASS[kelas] ?: STUDENTS_BY_CLASS.getValue("7A")

val STUDENTS: List<String> = studentsOf("7A")

val CLASS_CODES = listOf("7A", "7B", "8A", "8B")

val CLASS_INFO = mapOf(
    "7A" to ("VII A" to 24), "7B" to ("VII B" to 24),
    "8A" to ("VIII A" to 24), "8B" to ("VIII B" to 24)
)

const val KKM = 75

val STATUS_LABEL = mapOf("H" to "HADIR", "S" to "SAKIT", "I" to "IZIN", "A" to "ALFA")
val STATUS_COLOR = mapOf("H" to Mint, "S" to Sky, "I" to Amber, "A" to Coral)

val SCHEDULE: Map<String, List<Lesson>> = mapOf(
    "Senin" to listOf(
        Lesson("07:00", "08:20", "7A", "Ruang 12", "Klasifikasi makhluk hidup"),
        Lesson("09:15", "10:35", "8A", "Lab Sains", "Sistem pencernaan"),
        Lesson("11:00", "12:20", "7B", "Ruang 9", "Ciri makhluk hidup")
    ),
    "Selasa" to listOf(
        Lesson("07:45", "09:05", "8B", "Lab Sains", "Zat aditif"),
        Lesson("10:00", "11:20", "8A", "Ruang 14", "Listrik statis")
    ),
    "Rabu" to listOf(
        Lesson("07:00", "08:20", "7A", "Ruang 12", "Kunci determinasi"),
        Lesson("08:20", "09:40", "7B", "Ruang 9", "Kunci determinasi"),
        Lesson("10:35", "11:55", "8A", "Lab Sains", "Uji amilum")
    ),
    "Kamis" to listOf(
        Lesson("07:45", "09:05", "8B", "Ruang 11", "Sistem pernapasan")
    ),
    "Jumat" to listOf(
        Lesson("07:00", "08:20", "7A", "Ruang 12", "Proyek herbarium"),
        Lesson("08:20", "09:40", "8B", "Ruang 14", "Praktikum rangkaian")
    )
)

val EVENT_KIND = mapOf(
    "ujian" to (Coral to "UJIAN"),
    "rapat" to (Plum to "RAPAT"),
    "kegiatan" to (Amber to "KEGIATAN"),
    "libur" to (Mint to "LIBUR"),
    "admin" to (Sky to "ADMIN")
)

val DOC_TAB = mapOf(
    "Modul Ajar" to Amber, "LKPD" to Mint, "Bank Soal" to Sky,
    "Administrasi" to Plum, "Lainnya" to Teal
)

val NOTE_COLOR = mapOf(
    "amber" to Amber, "mint" to Mint, "sky" to Sky, "coral" to Coral, "plum" to Plum
)

val PALETTE = listOf(
    "Ink" to Ink, "Night" to Night, "Panel" to Panel, "Panel 2" to Panel2,
    "Cream" to Cream, "Muted" to Muted, "Amber" to Amber, "Coral" to Coral,
    "Teal" to Teal, "Mint" to Mint, "Sky" to Sky, "Plum" to Plum
)

/**
 * Satu objek yang bisa diketuk di gambar workspace.
 * Koordinat memakai piksel gambar (SCENE_W × SCENE_H), bukan grid.
 */
data class TileInfo(
    val label: String,
    val target: Scr,
    val glyph: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int
)

/**
 * Objek di gambar isometrik (koordinat = piksel gambar 1312 × 816).
 *
 * Selain 12 objek utama, lima bingkai foto di atas rak buku jadi pintasan
 * aplikasi, dan foto user di kiri jam dinding membuka profil & pengaturan.
 */
val TILES = listOf(
    // --- meja kerja ---
    TileInfo("Jurnal", Scr.JURNAL, "book", 756, 314, 196, 126),          // laptop
    TileInfo("Nilai", Scr.NILAI, "chart", 686, 306, 112, 92),            // tumpukan buku
    TileInfo("Kalkulator", Scr.KALKULATOR, "calc", 905, 436, 74, 52),    // kalkulator kanan laptop
    TileInfo("Alat Tulis", Scr.KIT, "pencil", 980, 372, 50, 66),         // tempat pensil
    TileInfo("Kalender", Scr.KALENDER, "calendar", 1022, 392, 80, 90),   // kalender meja tebal
    TileInfo("Binder", Scr.BINDER, "folder", 688, 536, 158, 158),        // laci meja

    // --- dinding ---
    TileInfo("Absensi", Scr.ABSENSI, "check", 334, 124, 252, 330),       // papan tulis
    TileInfo("Catatan", Scr.CATATAN, "pencil", 70, 265, 168, 190),       // papan pin di atas rak
    TileInfo("Jadwal", Scr.JADWAL, "clock", 792, 34, 130, 136),          // jam digital dinding
    TileInfo("Profil", Scr.PROFIL, "face", 712, 36, 76, 124),            // foto user

    // --- sisi kiri & kanan ---
    TileInfo("Binder", Scr.BINDER, "folder", 60, 425, 255, 310),         // rak buku penuh
    TileInfo("Kelas", Scr.KELAS, "grid", 1024, 95, 230, 300),            // jendela terbuka
    TileInfo("File", Scr.HOME, "trash", 12, 646, 92, 122),               // tempat sampah

    // --- 5 bingkai aplikasi di atas papan pin ---
    TileInfo("WhatsApp", Scr.KIT, "chat", 70, 218, 48, 82),
    TileInfo("Vivaldi", Scr.KIT, "globe", 144, 179, 48, 82),
    TileInfo("Spotify", Scr.KIT, "music", 218, 141, 48, 82),
    TileInfo("YouTube", Scr.KIT, "play", 292, 102, 48, 82),
    TileInfo("Sinaga Jateng", Scr.KIT, "shield", 366, 63, 48, 83)
)

/**
 * Jam digital: sejajar dengan foto user, tingginya sama dengan bingkai foto.
 * Bendera Indonesia menempel tepat di kanan jam, tinggi sama, lebar menyesuaikan.
 */
val CLOCK_RECT = intArrayOf(796, 38, 120, 124)

/** Papan tulis: judul "JADWAL MENGAJAR" di bagian atas, rata tengah. */
val BOARD_QUAD = intArrayOf(344, 250, 574, 132, 344, 442)

/**
 * Kertas kalender meja: bidang miring supaya tulisan ikut arah muka kalender.
 * Urutan: kiri-atas, kanan-atas, kiri-bawah, kanan-bawah (piksel gambar).
 */
val CALENDAR_QUAD = intArrayOf(1043, 405, 1086, 425, 1030, 451, 1073, 470)

/** Kertas di dalam lima bingkai (kiri-atas, kanan-atas, kiri-bawah). */
val FRAME_QUADS = listOf(
    intArrayOf(77, 241, 107, 226, 77, 293),
    intArrayOf(151, 202, 181, 187, 151, 254),
    intArrayOf(225, 164, 255, 149, 225, 216),
    intArrayOf(299, 124, 329, 110, 299, 177),
    intArrayOf(374, 86, 404, 72, 373, 139)
)

/** Kaca jendela (kiri-atas, kanan-atas, kiri-bawah, kanan-bawah). */
val WINDOW_QUAD = intArrayOf(1062, 112, 1200, 178, 1062, 332, 1200, 398)

/** Rel gorden: dari titik kiri ke kanan, dipakai juga sebagai penunjuk baterai. */
val RAIL_LINE = intArrayOf(1006, 57, 1288, 198)

/** Persentase baterai di dinding, tepat di atas rel, mengikuti arah dinding. */
val BATTERY_TEXT_QUAD = intArrayOf(1014, 0, 1122, 26, 1014, 44)

/** Area gorden kiri & kanan yang ikut bergoyang saat ada hembusan angin. */
val CURTAIN_RECTS = listOf(
    intArrayOf(995, 78, 40, 308),
    intArrayOf(1216, 224, 34, 246)
)

/** Daun tanaman yang ikut bergoyang. */
val LEAF_RECT = intArrayOf(1088, 470, 170, 200)

/** Nama aplikasi di lima bingkai + paket Android-nya (untuk tautan pintasan). */
val APP_PACKAGES = mapOf(
    "WhatsApp" to "com.whatsapp",
    "Vivaldi" to "com.vivaldi.browser",
    "Spotify" to "com.spotify.music",
    "YouTube" to "com.google.android.youtube",
    "Sinaga Jateng" to "id.jateng.sinaga"
)

/* ---------------- ikon aplikasi di lima bingkai ---------------- */

/**
 * Satu potongan ikon: warna ("#RRGGBB") + titik-titik poligon dalam koordinat
 * ternormalisasi 0..1 pada kertas bingkai (0,0 = kiri-atas kertas).
 * Semua digambar mengikuti kemiringan bingkai, jadi ikon tidak pernah "datar".
 */
data class IconShape(val color: String, val points: FloatArray)

data class AppIcon(val name: String, val bg: String, val shapes: List<IconShape>)

private fun shape(color: String, points: String): IconShape =
    IconShape(color, points.trim().split(" ").flatMap { pair ->
        pair.split(",").map { it.toFloat() }
    }.toFloatArray())

/**
 * Lima bingkai di atas papan pin = pintasan aplikasi.
 * Bentuknya sengaja tebal & sederhana supaya tetap terbaca di bingkai kecil.
 */
val APP_ICONS = listOf(
    AppIcon("WhatsApp", "#1FBF5B", listOf(
        shape("#FFFFFF", "0.20,0.18 0.80,0.18 0.80,0.62 0.46,0.62 0.30,0.80 0.31,0.62 0.20,0.62")
    )),
    AppIcon("Vivaldi", "#E03A3A", listOf(
        shape("#FFFFFF", "0.22,0.22 0.40,0.22 0.50,0.62 0.60,0.22 0.78,0.22 0.58,0.80 0.42,0.80")
    )),
    AppIcon("Spotify", "#1DB954", listOf(
        shape("#FFFFFF", "0.20,0.30 0.80,0.34 0.80,0.44 0.20,0.40"),
        shape("#FFFFFF", "0.22,0.48 0.78,0.52 0.78,0.60 0.22,0.56"),
        shape("#FFFFFF", "0.24,0.64 0.76,0.68 0.76,0.74 0.24,0.70")
    )),
    AppIcon("YouTube", "#D63B32", listOf(
        shape("#FFFFFF", "0.24,0.22 0.24,0.78 0.80,0.50")
    )),
    AppIcon("Sinaga Jateng", "#1E2C63", listOf(
        shape("#FFFFFF", "0.50,0.16 0.80,0.28 0.80,0.54 0.50,0.82 0.20,0.54 0.20,0.28"),
        shape("#1E2C63", "0.50,0.30 0.66,0.38 0.66,0.52 0.50,0.66 0.34,0.52 0.34,0.38")
    ))
)
const val SCENE_W = 1312
const val SCENE_H = 816

/* ---------------- state aplikasi ---------------- */

class AppState(private val ctx: Context) {

    var screen by mutableStateOf(Scr.HOME)
    var journalFilter by mutableStateOf("SEMUA")
    var noteFilter by mutableStateOf("SEMUA")
    var noteQuery by mutableStateOf("")
    var absClass by mutableStateOf("7A")
    var gradeClass by mutableStateOf("7A")
    var day by mutableStateOf("Senin")
    var binderFilter by mutableStateOf("SEMUA")
    var month by mutableStateOf(YearMonth.now())
    var selectedDate by mutableStateOf(LocalDate.now().toString())
    var calcMode by mutableStateOf("HITUNG")
    var calcExpr by mutableStateOf("")
    var art64 by mutableStateOf(false)
    var scanlines by mutableStateOf(true)

    val journal = mutableStateListOf<Journal>()
    val notes = mutableStateListOf<Note>()
    val agenda = mutableStateListOf<Agenda>()
    val docs = mutableStateListOf<DocItem>()
    val attendance = mutableStateMapOf<String, List<String>>()
    val grades = mutableStateMapOf<String, List<Grade>>()

    private val file = File(ctx.filesDir, "ruangkelas.json")

    private var ready = false

    /**
     * Data contoh baru disiapkan saat aplikasi bawaan pertama kali dibuka.
     * Launcher tidak perlu memuat 96 siswa hanya untuk menampilkan gambar.
     */
    fun ensureLoaded() {
        if (ready) return
        ready = true
        seed()
        load()
    }

    /* ---------- data contoh ---------- */
    private fun seed() {
        val today = LocalDate.now().toString()
        journal.clear()
        journal.addAll(
            listOf(
                Journal(4, today, "8A", "3–4", "09:15", "Sistem pencernaan manusia", "Diskusi kelompok lancar, 2 siswa perlu pendampingan.", 22, 24),
                Journal(3, today, "7A", "1–2", "07:00", "Klasifikasi makhluk hidup", "Kuis pembuka, rata-rata 78. Lanjut kunci determinasi.", 23, 24),
                Journal(2, minusDays(3), "8B", "5–6", "10:35", "Zat aditif & adiktif", "Praktikum uji pewarna makanan. Alat aman, hasil tercatat.", 22, 24),
                Journal(1, minusDays(5), "7B", "2–3", "08:20", "Ciri-ciri makhluk hidup", "Observasi taman sekolah, siswa antusias.", 23, 24)
            )
        )

        notes.clear()
        notes.addAll(
            listOf(
                Note(1, "Ide ice breaking: kartu makhluk hidup", "Bagi kartu gambar hewan/tumbuhan, siswa kelompokkan berdasarkan ciri. 10 menit di awal kelas, cocok untuk 7A yang ramai setelah olahraga.", "IDE", true, today),
                Note(2, "Refleksi kelas 8A", "Materi pencernaan terlalu padat kalau hanya ceramah. Minggu depan pakai model organ dari kardus + video 3 menit, lalu LKPD kelompok.", "REFLEKSI", false, today),
                Note(3, "To-do administrasi Oktober", "Rekap nilai UTS 8B, cetak daftar hadir bulanan, unggah modul ajar bab 4, minta tanda tangan waka kurikulum.", "TO-DO", true, minusDays(2)),
                Note(4, "Siswa perlu perhatian", "Fajar (8B) sering izin saat praktikum — cek kondisi, mungkin butuh tugas pengganti. Kirana (7A) sangat aktif, beri peran ketua kelompok.", "REFLEKSI", false, minusDays(3)),
                Note(5, "Ide kuis: salah atau benar", "Pakai kartu hijau/merah, 15 pernyataan tentang klasifikasi. Cepat, hemat kertas, bisa jadi nilai harian.", "IDE", false, minusDays(5))
            )
        )

        agenda.clear()
        agenda.addAll(
            listOf(
                Agenda(1, today, "Mengajar 8A — Sistem pencernaan", "09:15", "kegiatan"),
                Agenda(2, plusDays(1), "Rapat wali kelas", "13:00", "rapat"),
                Agenda(3, plusDays(3), "UTS IPA kelas 8", "07:30", "ujian"),
                Agenda(4, plusDays(4), "Batas unggah nilai formatif", "23:59", "admin"),
                Agenda(5, plusDays(7), "Libur panjang akhir semester", "—", "libur")
            )
        )

        docs.clear()
        docs.addAll(
            listOf(
                DocItem("Modul Ajar — Klasifikasi Makhluk Hidup", "Modul Ajar", "7A", "DOCX", "248 KB", "24 OKT", listOf(true, true, true, false, false)),
                DocItem("LKPD 3 — Kunci Determinasi", "LKPD", "7A", "PDF", "1.2 MB", "23 OKT", listOf(true, true, false)),
                DocItem("Bank Soal UTS IPA 8", "Bank Soal", "8A", "DOCX", "312 KB", "22 OKT", listOf(true, true, true, false)),
                DocItem("Modul Ajar — Sistem Pencernaan", "Modul Ajar", "8A", "PDF", "1.8 MB", "20 OKT", listOf(true, true, false)),
                DocItem("Daftar Hadir Bulanan Oktober", "Administrasi", "SEMUA", "XLSX", "96 KB", "19 OKT", listOf(true, false)),
                DocItem("LKPD 1 — Ciri Makhluk Hidup", "LKPD", "7B", "PDF", "840 KB", "17 OKT", listOf(true, false)),
                DocItem("Rekap Nilai Formatif Ganjil", "Administrasi", "SEMUA", "XLSX", "124 KB", "15 OKT", listOf(true, true, true))
            )
        )

        attendance.clear()
        CLASS_CODES.forEach { code ->
            val seed = code[0].code + code[1].code
            attendance[code] = studentsOf(code).mapIndexed { i, _ ->
                when {
                    (i * 3 + seed) % 17 == 0 -> "S"
                    (i * 5 + seed) % 23 == 0 -> "I"
                    (i * 7 + seed) % 31 == 0 -> "A"
                    else -> "H"
                }
            }
        }

        grades.clear()
        CLASS_CODES.forEach { code ->
            grades[code] = studentsOf(code).mapIndexed { i, nama ->
                val base = 68 + ((i * 7 + code[0].code) % 26)
                Grade(
                    nama,
                    (base + 4).coerceAtMost(100),
                    (base - 2 + (i % 3)).coerceAtMost(100),
                    (base + 2 - (i % 4)).coerceAtMost(100)
                )
            }
        }
    }

    fun reset() {
        file.delete()
        art64 = false
        scanlines = true
        seed()
        screen = Scr.HOME
    }

    /* ---------- simpan / muat ---------- */
    private fun minusDays(n: Long) = LocalDate.now().minusDays(n).toString()
    private fun plusDays(n: Long) = LocalDate.now().plusDays(n).toString()

    fun save() {
        try {
            val root = JSONObject()

            val jArr = JSONArray()
            journal.forEach { j ->
                jArr.put(JSONObject().apply {
                    put("id", j.id); put("date", j.date); put("kelas", j.kelas); put("jam", j.jam)
                    put("time", j.time); put("materi", j.materi); put("note", j.note)
                    put("hadir", j.hadir); put("total", j.total)
                })
            }
            root.put("journal", jArr)

            val nArr = JSONArray()
            notes.forEach { n ->
                nArr.put(JSONObject().apply {
                    put("id", n.id); put("title", n.title); put("body", n.body);
                    put("cat", n.cat); put("pinned", n.pinned); put("date", n.date)
                })
            }
            root.put("notes", nArr)

            val aArr = JSONArray()
            agenda.forEach { a ->
                aArr.put(JSONObject().apply {
                    put("id", a.id); put("date", a.date); put("title", a.title);
                    put("time", a.time); put("jenis", a.jenis)
                })
            }
            root.put("agenda", aArr)

            val dArr = JSONArray()
            docs.forEach { d ->
                val checks = JSONArray()
                d.checks.forEach { checks.put(it) }
                dArr.put(JSONObject().apply {
                    put("nama", d.nama); put("jenis", d.jenis); put("kelas", d.kelas)
                    put("format", d.format); put("size", d.size); put("updated", d.updated)
                    put("checks", checks)
                })
            }
            root.put("docs", dArr)

            val attObj = JSONObject()
            attendance.forEach { (k, v) -> attObj.put(k, JSONArray(v)) }
            root.put("attendance", attObj)

            val gradeObj = JSONObject()
            grades.forEach { (k, list) ->
                val arr = JSONArray()
                list.forEach { g ->
                    arr.put(JSONObject().apply {
                        put("nama", g.nama); put("tugas", g.tugas); put("uts", g.uts); put("uas", g.uas)
                    })
                }
                gradeObj.put(k, arr)
            }
            root.put("grades", gradeObj)

            root.put("ar64", art64)
            root.put("scanlines", scanlines)
            root.put("screen", screen.name)

            file.writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())

            root.optJSONArray("journal")?.let { arr ->
                journal.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    journal.add(
                        Journal(
                            o.optLong("id"), o.optString("date"), o.optString("kelas"), o.optString("jam"),
                            o.optString("time"), o.optString("materi"), o.optString("note"),
                            o.optInt("hadir"), o.optInt("total")
                        )
                    )
                }
            }

            root.optJSONArray("notes")?.let { arr ->
                notes.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    notes.add(
                        Note(
                            o.optLong("id"), o.optString("title"), o.optString("body"),
                            o.optString("cat"), o.optBoolean("pinned"), o.optString("date")
                        )
                    )
                }
            }

            root.optJSONArray("agenda")?.let { arr ->
                agenda.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    agenda.add(
                        Agenda(
                            o.optLong("id"), o.optString("date"), o.optString("title"),
                            o.optString("time"), o.optString("jenis")
                        )
                    )
                }
            }

            root.optJSONArray("docs")?.let { arr ->
                docs.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val checks = mutableListOf<Boolean>()
                    o.optJSONArray("checks")?.let { c ->
                        for (j in 0 until c.length()) checks.add(c.optBoolean(j))
                    }
                    docs.add(
                        DocItem(
                            o.optString("nama"), o.optString("jenis"), o.optString("kelas"),
                            o.optString("format"), o.optString("size"), o.optString("updated"), checks
                        )
                    )
                }
            }

            root.optJSONObject("attendance")?.let { obj ->
                obj.keys().forEach { key ->
                    val arr = obj.optJSONArray(key) ?: return@forEach
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.optString(i, "H"))
                    attendance[key] = list
                }
            }

            root.optJSONObject("grades")?.let { obj ->
                obj.keys().forEach { key ->
                    val arr = obj.optJSONArray(key) ?: return@forEach
                    val list = mutableListOf<Grade>()
                    for (i in 0 until arr.length()) {
                        val g = arr.getJSONObject(i)
                        list.add(Grade(g.optString("nama"), g.optInt("tugas"), g.optInt("uts"), g.optInt("uas")))
                    }
                    grades[key] = list
                }
            }

            art64 = root.optBoolean("ar64", false)
            scanlines = root.optBoolean("scanlines", true)
        } catch (_: Exception) {
        }
    }

    /* ---------- aksi ---------- */

    fun addJournal(kelas: String, jam: String, materi: String, note: String, hadir: Int, total: Int) {
        val now = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        journal.add(
            0,
            Journal(
                System.currentTimeMillis(), LocalDate.now().toString(), kelas, jam, now,
                materi, note.ifBlank { "Tanpa catatan tambahan." }, hadir, total
            )
        )
        save()
    }

    fun deleteJournal(id: Long) {
        journal.removeAll { it.id == id }
        save()
    }

    fun saveNote(note: Note?) {
        if (note == null) return
        val idx = notes.indexOfFirst { it.id == note.id }
        if (idx >= 0) notes[idx] = note else notes.add(0, note)
        save()
    }

    fun addNote(title: String, body: String, cat: String) {
        notes.add(0, Note(System.currentTimeMillis(), title, body, cat, false, LocalDate.now().toString()))
        save()
    }

    fun deleteNote(id: Long) {
        notes.removeAll { it.id == id }
        save()
    }

    fun togglePin(id: Long) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(pinned = !notes[idx].pinned)
        save()
    }

    fun addAgenda(date: String, title: String, time: String, jenis: String) {
        agenda.add(Agenda(System.currentTimeMillis(), date, title, time, jenis))
        save()
    }

    fun deleteAgenda(id: Long) {
        agenda.removeAll { it.id == id }
        save()
    }

    fun setAttendance(kelas: String, index: Int, status: String) {
        val list = attendance[kelas]?.toMutableList() ?: return
        if (index !in list.indices) return
        list[index] = status
        attendance[kelas] = list
        save()
    }

    fun setGrade(kelas: String, index: Int, tugas: Int, uts: Int, uas: Int) {
        val list = grades[kelas]?.toMutableList() ?: return
        if (index !in list.indices) return
        list[index] = list[index].copy(tugas = tugas, uts = uts, uas = uas)
        grades[kelas] = list
        save()
    }

    fun toggleDocCheck(docIndex: Int, checkIndex: Int) {
        val doc = docs.getOrNull(docIndex) ?: return
        val checks = doc.checks.toMutableList()
        if (checkIndex !in checks.indices) return
        checks[checkIndex] = !checks[checkIndex]
        docs[docIndex] = doc.copy(checks = checks)
        save()
    }

    fun addDoc(nama: String, jenis: String, kelas: String) {
        docs.add(0, DocItem(nama, jenis, kelas, "PDF", "—", "BARU", listOf(false, false)))
        save()
    }

    fun eventsOn(date: String) = agenda.filter { it.date == date }

    fun todayAgenda() = eventsOn(LocalDate.now().toString())

    fun filteredNotes(): List<Note> {
        val q = noteQuery.trim().lowercase()
        return notes
            .filter { n ->
                val byFilter = when (noteFilter) {
                    "DIPIN" -> n.pinned
                    "IDE" -> n.cat == "IDE"
                    "REFLEKSI" -> n.cat == "REFLEKSI"
                    "TO-DO" -> n.cat == "TO-DO"
                    else -> true
                }
                val byQuery = q.isEmpty() || (n.title + " " + n.body).lowercase().contains(q)
                byFilter && byQuery
            }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.date })
    }

    fun filteredJournal(): List<Journal> =
        journal.filter { journalFilter == "SEMUA" || it.kelas == journalFilter }
            .sortedByDescending { it.date }

    fun docProgress(doc: DocItem): Int =
        if (doc.checks.isEmpty()) 0 else (doc.checks.count { it } * 100) / doc.checks.size
}

/* ---------------- util tanggal ---------------- */

val ID_LOCALE = Locale("in", "ID")

fun fmtDate(iso: String, pattern: String = "d MMM"): String {
    return try {
        LocalDate.parse(iso).format(DateTimeFormatter.ofPattern(pattern, ID_LOCALE)).uppercase(ID_LOCALE)
    } catch (_: Exception) {
        iso
    }
}

fun fmtDayNum(iso: String): String = fmtDate(iso, "dd")
fun fmtDayMon(iso: String): String = fmtDate(iso, "MMM")
fun fmtLongDay(iso: String): String =
    fmtDate(iso, "EEEE, d MMMM yyyy")

fun todayIso(): String = LocalDate.now().toString()

fun monthLabel(ym: YearMonth): String =
    ym.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", ID_LOCALE)).uppercase(ID_LOCALE)
