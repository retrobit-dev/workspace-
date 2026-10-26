/* ==========================================================================
   RUANG KELAS — pixel workspace prototype  ·  v0.2
   Layar: Beranda · Jurnal · Catatan · Absensi · Nilai · Jadwal · Kalender
           Kalkulator · Binder · Kelas · Profil · Design Kit
   ========================================================================== */

/* ------------------------------ icons ---------------------------------- */

const ICONS = {
  logo: `<rect x="1" y="2" width="14" height="2"/><rect x="1" y="2" width="2" height="9"/>
    <rect x="13" y="2" width="2" height="9"/><rect x="1" y="9" width="14" height="2"/>
    <rect x="6" y="12" width="4" height="2"/><rect x="3" y="14" width="10" height="1"/>
    <rect x="4" y="5" width="2" height="2"/><rect x="8" y="5" width="4" height="2"/>`,
  home: `<rect x="6" y="1" width="4" height="1"/><rect x="4" y="2" width="8" height="1"/>
    <rect x="2" y="4" width="12" height="1"/><rect x="1" y="5" width="14" height="2"/>
    <rect x="2" y="7" width="12" height="3"/><rect x="2" y="10" width="4" height="5"/>
    <rect x="10" y="10" width="4" height="5"/><rect x="2" y="15" width="12" height="1"/>`,
  book: `<rect x="2" y="2" width="12" height="13"/><rect x="5" y="2" width="9" height="13"/>
    <rect x="2" y="2" width="3" height="13"/><rect x="1" y="4" width="1" height="9"/>
    <rect x="7" y="5" width="5" height="1"/><rect x="7" y="8" width="4" height="1"/>`,
  pencil: `<rect x="2" y="12" width="2" height="2"/><rect x="4" y="10" width="2" height="2"/>
    <rect x="6" y="8" width="2" height="2"/><rect x="8" y="6" width="2" height="2"/>
    <rect x="10" y="4" width="2" height="2"/><rect x="12" y="2" width="2" height="2"/>
    <rect x="1" y="14" width="4" height="1"/><rect x="11" y="1" width="4" height="2"/>`,
  check: `<rect x="1" y="8" width="2" height="2"/><rect x="3" y="10" width="2" height="2"/>
    <rect x="5" y="8" width="2" height="2"/><rect x="7" y="6" width="2" height="2"/>
    <rect x="9" y="4" width="2" height="2"/><rect x="11" y="2" width="2" height="2"/>
    <rect x="13" y="1" width="2" height="2"/>`,
  chart: `<rect x="1" y="1" width="1" height="14"/><rect x="1" y="14" width="14" height="1"/>
    <rect x="3" y="9" width="3" height="5"/><rect x="7" y="6" width="3" height="8"/>
    <rect x="11" y="3" width="3" height="11"/>`,
  calendar: `<rect x="4" y="0" width="2" height="4"/><rect x="10" y="0" width="2" height="4"/>
    <rect x="1" y="3" width="14" height="2"/><rect x="1" y="6" width="2" height="9"/>
    <rect x="13" y="6" width="2" height="9"/><rect x="1" y="14" width="14" height="1"/>
    <rect x="5" y="7" width="2" height="2"/><rect x="9" y="7" width="2" height="2"/>
    <rect x="5" y="11" width="2" height="2"/><rect x="9" y="11" width="2" height="2"/>`,
  grid: `<rect x="1" y="1" width="6" height="6"/><rect x="9" y="1" width="6" height="6"/>
    <rect x="1" y="9" width="6" height="6"/><rect x="9" y="9" width="6" height="6"/>
    <rect x="2" y="2" width="4" height="4" fill="var(--night)"/><rect x="10" y="2" width="4" height="4" fill="var(--night)"/>
    <rect x="2" y="10" width="4" height="4" fill="var(--night)"/><rect x="10" y="10" width="4" height="4" fill="var(--night)"/>`,
  calc: `<rect x="2" y="1" width="12" height="14"/><rect x="4" y="3" width="8" height="3" fill="var(--night)"/>
    <rect x="4" y="8" width="2" height="2" fill="var(--night)"/><rect x="7" y="8" width="2" height="2" fill="var(--night)"/>
    <rect x="10" y="8" width="2" height="2" fill="var(--night)"/><rect x="4" y="11" width="2" height="2" fill="var(--night)"/>
    <rect x="7" y="11" width="2" height="2" fill="var(--night)"/><rect x="10" y="11" width="2" height="2" fill="var(--night)"/>`,
  folder: `<rect x="1" y="3" width="6" height="2"/><rect x="1" y="5" width="14" height="9"/>
    <rect x="1" y="14" width="14" height="1"/><rect x="3" y="7" width="2" height="4"/>`,
  plus: `<rect x="7" y="2" width="2" height="12"/><rect x="2" y="7" width="12" height="2"/>`,
  bell: `<rect x="7" y="1" width="2" height="1"/><rect x="5" y="2" width="6" height="2"/>
    <rect x="3" y="4" width="10" height="7"/><rect x="2" y="11" width="12" height="2"/>
    <rect x="7" y="13" width="2" height="2"/>`,
  scan: `<rect x="1" y="1" width="5" height="2"/><rect x="1" y="1" width="2" height="5"/>
    <rect x="10" y="1" width="5" height="2"/><rect x="13" y="1" width="2" height="5"/>
    <rect x="1" y="13" width="5" height="2"/><rect x="1" y="10" width="2" height="5"/>
    <rect x="10" y="13" width="5" height="2"/><rect x="13" y="10" width="2" height="5"/>`,
  download: `<rect x="7" y="1" width="2" height="9"/><rect x="5" y="5" width="2" height="2"/>
    <rect x="9" y="5" width="2" height="2"/><rect x="6" y="7" width="4" height="2"/>
    <rect x="2" y="12" width="12" height="2"/>`,
  upload: `<rect x="7" y="1" width="2" height="9"/><rect x="5" y="3" width="2" height="2"/>
    <rect x="9" y="3" width="2" height="2"/><rect x="6" y="1" width="4" height="2"/>
    <rect x="2" y="12" width="12" height="2"/>`,
  share: `<rect x="11" y="1" width="4" height="4"/><rect x="1" y="6" width="4" height="4"/>
    <rect x="11" y="11" width="4" height="4"/><rect x="5" y="4" width="2" height="2"/>
    <rect x="7" y="5" width="2" height="2"/><rect x="9" y="6" width="2" height="1"/>
    <rect x="5" y="10" width="2" height="2"/><rect x="7" y="9" width="2" height="2"/>
    <rect x="9" y="8" width="2" height="1"/>`,
  close: `<rect x="7" y="1" width="2" height="14" transform="rotate(45 8 8)"/>
    <rect x="7" y="1" width="2" height="14" transform="rotate(-45 8 8)"/>`,
  face: `<rect x="4" y="1" width="8" height="2"/><rect x="3" y="3" width="10" height="5"/>
    <rect x="5" y="8" width="6" height="1"/><rect x="2" y="10" width="12" height="5"/>`,
  clock: `<rect x="5" y="0" width="6" height="2"/><rect x="3" y="2" width="10" height="2"/>
    <rect x="1" y="4" width="2" height="8"/><rect x="13" y="4" width="2" height="8"/>
    <rect x="3" y="12" width="10" height="2"/><rect x="5" y="14" width="6" height="2"/>
    <rect x="7" y="4" width="2" height="5"/><rect x="9" y="8" width="3" height="2"/>`,
  star: `<rect x="7" y="1" width="2" height="4"/><rect x="1" y="5" width="14" height="2"/>
    <rect x="3" y="7" width="10" height="2"/><rect x="4" y="9" width="3" height="3"/>
    <rect x="9" y="9" width="3" height="3"/>`,
  medal: `<rect x="6" y="0" width="4" height="6"/><rect x="4" y="3" width="8" height="2"/>
    <rect x="3" y="5" width="10" height="5"/><rect x="4" y="10" width="8" height="2"/>
    <rect x="6" y="12" width="4" height="3"/>`,
  search: `<rect x="3" y="2" width="7" height="2"/><rect x="1" y="4" width="2" height="5"/>
    <rect x="10" y="4" width="2" height="5"/><rect x="3" y="9" width="7" height="2"/>
    <rect x="9" y="9" width="2" height="2"/><rect x="11" y="11" width="2" height="2"/>
    <rect x="13" y="13" width="2" height="2"/>`,
  chevL: `<rect x="8" y="2" width="2" height="2"/><rect x="6" y="4" width="2" height="2"/>
    <rect x="4" y="6" width="2" height="4"/><rect x="6" y="10" width="2" height="2"/>
    <rect x="8" y="12" width="2" height="2"/>`,
  chevR: `<rect x="6" y="2" width="2" height="2"/><rect x="8" y="4" width="2" height="2"/>
    <rect x="10" y="6" width="2" height="4"/><rect x="8" y="10" width="2" height="2"/>
    <rect x="6" y="12" width="2" height="2"/>`,
  refresh: `<rect x="5" y="1" width="8" height="2"/><rect x="12" y="3" width="2" height="3"/>
    <rect x="3" y="4" width="2" height="6"/><rect x="5" y="11" width="8" height="2"/>
    <rect x="2" y="6" width="2" height="3"/><rect x="10" y="4" width="3" height="2"/>
    <rect x="3" y="12" width="8" height="2"/><rect x="1" y="4" width="3" height="2"/>`,
  gear: `<rect x="6" y="0" width="4" height="2"/><rect x="6" y="14" width="4" height="2"/>
    <rect x="0" y="6" width="2" height="4"/><rect x="14" y="6" width="2" height="4"/>
    <rect x="2" y="2" width="2" height="2"/><rect x="12" y="2" width="2" height="2"/>
    <rect x="2" y="12" width="2" height="2"/><rect x="12" y="12" width="2" height="2"/>
    <rect x="5" y="5" width="6" height="6"/><rect x="7" y="7" width="2" height="2" fill="var(--night)"/>`,
  sun: `<rect x="7" y="0" width="2" height="3"/><rect x="7" y="13" width="2" height="3"/>
    <rect x="0" y="7" width="3" height="2"/><rect x="13" y="7" width="3" height="2"/>
    <rect x="2" y="2" width="2" height="2"/><rect x="12" y="2" width="2" height="2"/>
    <rect x="2" y="12" width="2" height="2"/><rect x="12" y="12" width="2" height="2"/>
    <rect x="4" y="4" width="8" height="8"/><rect x="6" y="6" width="4" height="4" fill="var(--night)"/>`,
  trash: `<rect x="3" y="2" width="10" height="2"/><rect x="6" y="0" width="4" height="2"/>
    <rect x="4" y="4" width="8" height="11"/><rect x="6" y="6" width="1" height="7" fill="var(--night)"/>
    <rect x="9" y="6" width="1" height="7" fill="var(--night)"/>`,
  doc: `<rect x="3" y="1" width="8" height="2"/><rect x="11" y="3" width="2" height="2"/>
    <rect x="3" y="1" width="2" height="14"/><rect x="5" y="13" width="8" height="2"/>
    <rect x="11" y="5" width="2" height="8"/><rect x="5" y="5" width="5" height="1"/>
    <rect x="5" y="8" width="4" height="1"/>`,
  pin: `<rect x="4" y="1" width="8" height="2"/><rect x="6" y="3" width="4" height="4"/>
    <rect x="4" y="7" width="8" height="2"/><rect x="7" y="9" width="2" height="6"/>`,
  link: `<rect x="1" y="6" width="5" height="4"/><rect x="10" y="6" width="5" height="4"/>
    <rect x="6" y="7" width="4" height="2"/><rect x="3" y="4" width="2" height="2"/>
    <rect x="11" y="10" width="2" height="2"/>`
};

function hydrateIcons(root = document) {
  root.querySelectorAll("[data-icon]").forEach((el) => {
    if (el.dataset.iconReady) return;
    const shapes = ICONS[el.dataset.icon];
    if (!shapes) return;
    const size = el.dataset.size || 16;
    el.innerHTML = `<svg viewBox="0 0 16 16" width="${size}" height="${size}" fill="currentColor" aria-hidden="true" shape-rendering="crispEdges">${shapes}</svg>`;
    el.dataset.iconReady = "1";
  });
}

/* ------------------------------ palette -------------------------------- */

const PALETTE = [
  { name: "Ink", hex: "#0E0D1A" },
  { name: "Night", hex: "#15142A" },
  { name: "Panel", hex: "#272647" },
  { name: "Panel 2", hex: "#33325A" },
  { name: "Cream", hex: "#F8E9C8" },
  { name: "Muted", hex: "#A79EC4" },
  { name: "Amber", hex: "#F2AB3F" },
  { name: "Coral", hex: "#EF6A5B" },
  { name: "Teal", hex: "#4FA49A" },
  { name: "Mint", hex: "#9FD8B0" },
  { name: "Sky", hex: "#77B8D9" },
  { name: "Plum", hex: "#A58ECB" }
];

/* --------------------------- scene tile map ---------------------------- */
/* rect = [x, y, w, h] dalam piksel gambar asli (night 1408×768 / day 1376×768) */

const SCENE = {
  night: { src: "assets/workspace-scene.png", w: 1408, h: 768, tag: "RUANG KERJA · SENJA" },
  day: { src: "assets/workspace-scene-day.png", w: 1376, h: 768, tag: "RUANG KERJA · PAGI" }
};

const TILES = [
  { id: "laptop", label: "Jurnal", target: "jurnal", icon: "book",
    night: [590, 328, 168, 174], day: [408, 328, 186, 172] },
  { id: "papan", label: "Absensi", target: "absensi", icon: "check",
    night: [516, 180, 390, 254], day: [598, 82, 448, 316] },
  { id: "pinboard", label: "Catatan", target: "catatan", icon: "pencil",
    night: [934, 178, 314, 240], day: [1066, 90, 312, 268] },
  { id: "buku", label: "Nilai", target: "nilai", icon: "chart",
    night: [398, 418, 204, 112], day: [272, 430, 156, 84] },
  { id: "rak", label: "Binder", target: "binder", icon: "folder",
    night: [428, 6, 560, 148], day: [238, 44, 340, 292] },
  { id: "jendela", label: "Kalender", target: "kalender", icon: "calendar",
    night: [38, 20, 336, 312], day: [0, 8, 232, 506] },
  { id: "jam", label: "Jadwal", target: "jadwal", icon: "clock",
    night: [836, 406, 90, 82], day: [796, 428, 90, 84] },
  { id: "kursi", label: "Profil", target: "profil", icon: "face",
    night: [980, 436, 268, 268], day: [1110, 450, 266, 300] },
  { id: "laci", label: "Kelas", target: "kelas", icon: "grid",
    night: [884, 520, 200, 190], day: [748, 498, 152, 148] }
];

const INVENTORY = [
  { label: "Kalkulator", target: "kalkulator", icon: "calc" },
  { label: "Kelas", target: "kelas", icon: "grid" },
  { label: "Kalender", target: "kalender", icon: "calendar" },
  { label: "Profil", target: "profil", icon: "face" }
];

/* ------------------------------- data ---------------------------------- */

const STUDENTS = [
  "Aditya Pratama", "Bunga Lestari", "Citra Maharani", "Dimas Saputra",
  "Eka Wulandari", "Fajar Nugroho", "Gita Puspita", "Hendra Wijaya",
  "Intan Permata", "Joko Susilo", "Kirana Dewi", "Lukman Hakim"
];

const CLASSES = {
  "7A": { subject: "IPA", rombel: "VII A", total: 32, students: STUDENTS },
  "7B": { subject: "IPA", rombel: "VII B", total: 31, students: STUDENTS },
  "8A": { subject: "IPA", rombel: "VIII A", total: 32, students: STUDENTS },
  "8B": { subject: "IPA", rombel: "VIII B", total: 31, students: STUDENTS }
};

const JURNAL_SEED = [
  { id: 4, date: "2026-10-26", kelas: "8A", time: "09:15", jam: "3–4", materi: "Sistem pencernaan manusia",
    note: "Diskusi kelompok lancar, 2 siswa perlu pendampingan.", hadir: 30, total: 32 },
  { id: 3, date: "2026-10-26", kelas: "7A", time: "07:00", jam: "1–2", materi: "Klasifikasi makhluk hidup",
    note: "Kuis pembuka, rata-rata 78. Lanjut kunci determinasi.", hadir: 31, total: 32 },
  { id: 2, date: "2026-10-23", kelas: "8B", time: "10:35", jam: "5–6", materi: "Zat aditif & adiktif",
    note: "Praktikum uji pewarna makanan. Alat aman, hasil tercatat.", hadir: 29, total: 31 },
  { id: 1, date: "2026-10-21", kelas: "7B", time: "08:20", jam: "2–3", materi: "Ciri-ciri makhluk hidup",
    note: "Observasi taman sekolah, siswa antusias.", hadir: 30, total: 31 }
];

const NOTE_SEED = [
  { id: 1, title: "Ide ice breaking: kartu makhluk hidup", body: "Bagi kartu gambar hewan/tumbuhan, siswa kelompokkan berdasarkan ciri. 10 menit di awal kelas, cocok untuk 7A yang ramai setelah olahraga.",
    cat: "ide", color: "amber", pinned: true, date: "2026-10-26" },
  { id: 2, title: "Refleksi kelas 8A", body: "Materi pencernaan terlalu padat kalau hanya ceramah. Minggu depan pakai model organ dari kardus + video 3 menit, lalu LKPD kelompok.",
    cat: "refleksi", color: "sky", pinned: false, date: "2026-10-26" },
  { id: 3, title: "To-do administrasi Oktober", body: "1) Rekap nilai UTS 8B. 2) Cetak daftar hadir bulanan. 3) Unggah modul ajar bab 4 ke binder. 4) Minta tanda tangan waka kurikulum.",
    cat: "to-do", color: "mint", pinned: true, date: "2026-10-24" },
  { id: 4, title: "Siswa perlu perhatian", body: "Fajar (8B) sering izin saat praktikum — cek kondisi, mungkin butuh tugas pengganti. Kirana (7A) sangat aktif, beri peran ketua kelompok.",
    cat: "refleksi", color: "coral", pinned: false, date: "2026-10-23" },
  { id: 5, title: "Ide kuis: 'salah atau benar'", body: "Pakai kartu hijau/merah, 15 pernyataan tentang klasifikasi. Cepat, hemat kertas, bisa jadi nilai harian.",
    cat: "ide", color: "plum", pinned: false, date: "2026-10-21" },
  { id: 6, title: "Catatan rapat kurikulum", body: "Asesmen formatif minimal 2x per bab. Nilai akhir = rata-rata formatif 40% + UTS 30% + UAS 30%. Sama dengan bobot di menu Nilai.",
    cat: "to-do", color: "amber", pinned: false, date: "2026-10-19" }
];

const EVENT_SEED = [
  { id: 1, date: "2026-10-26", title: "Mengajar 8A — Sistem pencernaan", jenis: "kegiatan", time: "09:15" },
  { id: 2, date: "2026-10-27", title: "Rapat wali kelas", jenis: "rapat", time: "13:00" },
  { id: 3, date: "2026-10-29", title: "UTS IPA kelas 8", jenis: "ujian", time: "07:30" },
  { id: 4, date: "2026-10-30", title: "Batas unggah nilai formatif", jenis: "admin", time: "23:59" },
  { id: 5, date: "2026-11-02", title: "Libur panjang akhir semester", jenis: "libur", time: "—" },
  { id: 6, date: "2026-11-05", title: "Praktikum uji amilum 8A", jenis: "kegiatan", time: "10:00" },
  { id: 7, date: "2026-11-10", title: "Ujian praktik IPA", jenis: "ujian", time: "08:00" }
];

const DOC_SEED = [
  { id: 1, nama: "Modul Ajar — Klasifikasi Makhluk Hidup", jenis: "Modul Ajar", kelas: "7A", size: "248 KB", format: "DOCX",
    updated: "24 OKT 2026", items: [["Tujuan pembelajaran", true], ["Apersepsi & pemantik", true], ["Kegiatan inti", true], ["Asesmen formatif", false], ["Refleksi guru", false]] },
  { id: 2, nama: "LKPD 3 — Kunci Determinasi", jenis: "LKPD", kelas: "7A", size: "1.2 MB", format: "PDF",
    updated: "23 OKT 2026", items: [["Kunci jawaban", true], ["Rubrik penilaian", true], ["Versi remedial", false]] },
  { id: 3, nama: "Bank Soal UTS IPA 8", jenis: "Bank Soal", kelas: "8A", size: "312 KB", format: "DOCX",
    updated: "22 OKT 2026", items: [["25 soal pilihan ganda", true], ["5 soal uraian", true], ["Kisi-kisi & bobot", true], ["Kartu jawaban", false]] },
  { id: 4, nama: "Modul Ajar — Sistem Pencernaan", jenis: "Modul Ajar", kelas: "8A", size: "1.8 MB", format: "PDF",
    updated: "20 OKT 2026", items: [["Tujuan pembelajaran", true], ["Media & alat", true], ["Langkah praktikum", false]] },
  { id: 5, nama: "Daftar Hadir Bulanan Oktober", jenis: "Administrasi", kelas: "SEMUA", size: "96 KB", format: "XLSX",
    updated: "19 OKT 2026", items: [["Rekap kehadiran", true], ["Kolom tanda tangan", false]] },
  { id: 6, nama: "LKPD 1 — Ciri Makhluk Hidup", jenis: "LKPD", kelas: "7B", size: "840 KB", format: "PDF",
    updated: "17 OKT 2026", items: [["Lembar kerja siswa", true], ["Rubrik penilaian", false]] },
  { id: 7, nama: "Rekap Nilai Formatif Ganjil", jenis: "Administrasi", kelas: "SEMUA", size: "124 KB", format: "XLSX",
    updated: "15 OKT 2026", items: [["Formatif 1–4", true], ["Perhitungan NA", true], ["Catatan remedial", true]] }
];

const JADWAL = {
  Senin: [
    { time: "07:00", end: "08:20", kelas: "7A", room: "Ruang 12", materi: "Klasifikasi makhluk hidup" },
    { time: "09:15", end: "10:35", kelas: "8A", room: "Lab Sains", materi: "Sistem pencernaan", now: true },
    { time: "11:00", end: "12:20", kelas: "7B", room: "Ruang 9", materi: "Ciri makhluk hidup" }
  ],
  Selasa: [
    { time: "07:45", end: "09:05", kelas: "8B", room: "Lab Sains", materi: "Zat aditif" },
    { time: "10:00", end: "11:20", kelas: "8A", room: "Ruang 14", materi: "Listrik statis" }
  ],
  Rabu: [
    { time: "07:00", end: "08:20", kelas: "7A", room: "Ruang 12", materi: "Kunci determinasi" },
    { time: "08:20", end: "09:40", kelas: "7B", room: "Ruang 9", materi: "Kunci determinasi" },
    { time: "10:35", end: "11:55", kelas: "8A", room: "Lab Sains", materi: "Uji amilum" }
  ],
  Kamis: [
    { time: "07:45", end: "09:05", kelas: "8B", room: "Ruang 11", materi: "Sistem pernapasan" }
  ],
  Jumat: [
    { time: "07:00", end: "08:20", kelas: "7A", room: "Ruang 12", materi: "Proyek herbarium" },
    { time: "08:20", end: "09:40", kelas: "8B", room: "Ruang 14", materi: "Praktikum rangkaian" }
  ]
};

const STATUS_LABEL = { H: "Hadir", S: "Sakit", I: "Izin", A: "Alfa" };
const STATUS_CLASS = { H: "on-h", S: "on-s", I: "on-i", A: "on-a" };
const EVENT_KIND = {
  ujian: { label: "UJIAN", cls: "k-ujian" },
  rapat: { label: "RAPAT", cls: "k-rapat" },
  kegiatan: { label: "KEGIATAN", cls: "k-kegiatan" },
  libur: { label: "LIBUR", cls: "k-libur" },
  admin: { label: "ADMIN", cls: "" }
};
const DOC_TAB = {
  "Modul Ajar": "t-modul", "LKPD": "t-lkpd", "Bank Soal": "t-soal",
  "Administrasi": "t-admin", "Lainnya": "t-lain"
};
const NOTE_COLOR = { amber: "", mint: "c-mint", sky: "c-sky", coral: "c-coral", plum: "c-plum" };
const KKM = 75;

/* ------------------------------- state --------------------------------- */

const LS_KEY = "ruangkelas.prototype.v2";

const state = {
  screen: "home",
  journalFilter: "all",
  absClass: "7A",
  scoreClass: "7A",
  day: "Senin",
  noteFilter: "all",
  noteQuery: "",
  binderFilter: "all",
  calMonth: null,      // Date pada hari pertama bulan yang ditampilkan
  calSelected: null,   // 'YYYY-MM-DD'
  calcMode: "hitung",
  calcExpr: "",
  sceneVariant: "night",
  jurnal: JSON.parse(JSON.stringify(JURNAL_SEED)),
  notes: JSON.parse(JSON.stringify(NOTE_SEED)),
  events: JSON.parse(JSON.stringify(EVENT_SEED)),
  docs: JSON.parse(JSON.stringify(DOC_SEED)),
  absensi: {},
  nilai: {}
};

function seedScores(kelas) {
  return STUDENTS.map((nama, i) => {
    const base = 68 + ((i * 7 + kelas.charCodeAt(0)) % 26);
    return {
      nama,
      tugas: Math.min(100, base + 4),
      uts: Math.min(100, base - 2 + (i % 3)),
      uas: Math.min(100, base + 2 - (i % 4))
    };
  });
}

Object.keys(CLASSES).forEach((k) => {
  state.absensi[k] = STUDENTS.map(() => "H");
  state.nilai[k] = seedScores(k);
});

function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

state.calSelected = todayISO();
state.calMonth = new Date(`${state.calSelected}T00:00:00`);
state.calMonth.setDate(1);

function restore() {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) return;
    const saved = JSON.parse(raw);
    ["jurnal", "notes", "events", "docs"].forEach((key) => {
      if (Array.isArray(saved[key])) state[key] = saved[key];
    });
    if (saved.absensi) state.absensi = { ...state.absensi, ...saved.absensi };
    if (saved.nilai) state.nilai = { ...state.nilai, ...saved.nilai };
    if (saved.theme === "light") document.body.classList.add("theme-light");
    if (saved.palette === "warm") document.body.classList.add("palette-warm");
    if (saved.scanlines === false) document.body.classList.remove("scanlines");
    if (saved.motion === false) document.body.classList.add("no-motion");
    if (saved.sceneVariant) state.sceneVariant = saved.sceneVariant;
    hydrateTogglesFromBody();
  } catch (e) { /* abaikan data rusak */ }
}

function persist() {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify({
      jurnal: state.jurnal, notes: state.notes, events: state.events, docs: state.docs,
      absensi: state.absensi, nilai: state.nilai,
      theme: document.body.classList.contains("theme-light") ? "light" : "dark",
      palette: document.body.classList.contains("palette-warm") ? "warm" : "default",
      scanlines: document.body.classList.contains("scanlines"),
      motion: !document.body.classList.contains("no-motion"),
      sceneVariant: state.sceneVariant
    }));
  } catch (e) { /* kuota penuh — abaikan */ }
}

/* ------------------------------ helpers -------------------------------- */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

let toastTimer;
function toast(msg) {
  const el = $("#toast");
  if (!el) return;
  el.textContent = msg;
  el.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("show"), 2500);
}

const fmtID = (iso, opts = { day: "numeric", month: "short" }) =>
  new Date(`${iso}T00:00:00`).toLocaleDateString("id-ID", opts);

const dateParts = (iso) => {
  const d = new Date(`${iso}T00:00:00`);
  return {
    num: String(d.getDate()).padStart(2, "0"),
    mon: d.toLocaleDateString("id-ID", { month: "short" }).toUpperCase().slice(0, 3)
  };
};

const clamp = (n, lo, hi) => Math.max(lo, Math.min(hi, n));
const intOr = (v, fallback = 0) => {
  const n = parseInt(String(v).replace(/[^\d-]/g, ""), 10);
  return Number.isNaN(n) ? fallback : n;
};

function naOf(n) { return Math.round(n.tugas * 0.4 + n.uts * 0.3 + n.uas * 0.3); }

function naClass(value) {
  if (value < KKM) return "bad";
  if (value < KKM + 8) return "warn";
  return "";
}

function predikat(value) {
  if (value >= 90) return { letter: "A", label: "SANGAT BAIK", cls: "" };
  if (value >= 80) return { letter: "B", label: "BAIK", cls: "warn" };
  if (value >= 70) return { letter: "C", label: "CUKUP", cls: "warn" };
  return { letter: "D", label: "PERLU BIMBINGAN", cls: "bad" };
}

/* ------------------------------ screens -------------------------------- */

const NAV_SCREENS = ["home", "jurnal", "catatan", "nilai", "binder"];

function showScreen(name) {
  state.screen = name;
  const isKit = name === "kit";

  $$(".screen").forEach((el) => el.classList.toggle("is-active", el.dataset.panel === name));
  $$(".tab").forEach((el) => el.setAttribute("aria-selected", String(el.dataset.screen === name)));
  $$(".nav-btn").forEach((el) => {
    if (el.dataset.goto === name) el.setAttribute("aria-current", "page");
    else el.removeAttribute("aria-current");
  });

  $("#phoneWrap").style.display = isKit ? "none" : "";
  $("#stageBody").classList.toggle("kit-mode", isKit);
  $$(".kit").forEach((el) => el.classList.toggle("is-active", isKit));

  const scroll = $(`.screen[data-panel="${name}"] .screen-scroll`);
  if (scroll) scroll.scrollTop = 0;
  if (name === "kalender") renderKalender();
  if (name === "home") renderHomeMeta();
}

/* -------------------------------- home --------------------------------- */

function renderTiles() {
  const variant = state.sceneVariant;
  const img = $("#sceneImg");
  img.src = SCENE[variant].src;
  img.alt = `Ruang kerja guru bergaya pixel art — suasana ${variant === "night" ? "senja" : "pagi"}`;
  $("#sceneTag").textContent = SCENE[variant].tag;

  const meta = SCENE[variant];
  $("#scene").style.setProperty("--scene-ratio", (meta.w / meta.h).toFixed(4));
  $("#sceneTiles").innerHTML = TILES.map((t) => {
    const [x, y, w, h] = t[variant];
    const left = (x / meta.w) * 100;
    const top = (y / meta.h) * 100;
    const width = (w / meta.w) * 100;
    const height = (h / meta.h) * 100;
    return `<button class="tile" data-goto="${t.target}" data-label="${t.label}"
              style="left:${left.toFixed(2)}%;top:${top.toFixed(2)}%;width:${width.toFixed(2)}%;height:${height.toFixed(2)}%"
              aria-label="${t.label}">
              <span class="tile-dot"><span class="ico" data-icon="${t.icon}" data-size="11"></span></span>
              <span class="tile-label">${t.label.toUpperCase()}</span>
            </button>`;
  }).join("");

  hydrateIcons($("#sceneTiles"));
}

function renderInventory() {
  const el = $("#inventory");
  if (!el) return;
  el.innerHTML = INVENTORY.map((it) => `
    <button class="inv-slot" data-goto="${it.target}" aria-label="${it.label}">
      <span class="ico" data-icon="${it.icon}" data-size="16"></span>
      <span>${it.label.toUpperCase()}</span>
    </button>`).join("");
  hydrateIcons(el);
}

function renderHomeMeta() {
  const xp = Math.min(1000, 600 + state.jurnal.length * 35 + state.notes.length * 12);
  $("#xpBar").style.width = `${(xp / 1000) * 100}%`;
  $("#xpLabel").textContent = `${xp} / 1000 XP`;
  $("#noteCount").textContent = state.notes.filter((n) => n.cat !== "to-do" || true).length;
  const hour = new Date().getHours();
  const greet = hour < 11 ? "Selamat pagi," : hour < 15 ? "Selamat siang," : hour < 18 ? "Selamat sore," : "Selamat malam,";
  $("#greetLine").textContent = greet;
}

function toggleScene() {
  state.sceneVariant = state.sceneVariant === "night" ? "day" : "night";
  document.body.classList.toggle("theme-light", state.sceneVariant === "day");
  hydrateTogglesFromBody();
  renderTiles();
  persist();
  toast(`SUASANA ${state.sceneVariant === "night" ? "SENJA" : "PAGI"} · MODE ${state.sceneVariant === "night" ? "GELAP" : "TERANG"}`);
}

/* ------------------------------- jurnal -------------------------------- */

function renderJurnal() {
  const list = $("#journalList");
  const rows = state.jurnal
    .filter((j) => state.journalFilter === "all" || j.kelas === state.journalFilter)
    .sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0));

  if (!rows.length) {
    list.innerHTML = `<p class="empty">BELUM ADA CATATAN<br>UNTUK KELAS INI.<br><br>TAP TOMBOL DI BAWAH.</p>`;
    return;
  }

  list.innerHTML = rows.map((j, i) => {
    const tone = i % 4 === 1 ? "b" : i % 4 === 2 ? "c" : i % 4 === 3 ? "d" : "";
    const d = dateParts(j.date);
    return `
      <article class="journal-item">
        <div class="date-badge"><b>${d.num}</b><span>${d.mon}</span></div>
        <div class="journal-body">
          <div class="line1">
            <span class="kelas-tag ${tone}">${j.kelas}</span>
            <span class="time-tag">JAM ${j.jam} · ${j.time}</span>
          </div>
          <h4>${j.materi}</h4>
          <p>${j.note}</p>
          <span class="attend-pill">${j.hadir}/${j.total} HADIR</span>
        </div>
      </article>`;
  }).join("");
}

/* -------------------------------- catatan ------------------------------ */

function renderCatatan() {
  const list = $("#noteList");
  const q = state.noteQuery.trim().toLowerCase();

  const rows = state.notes
    .filter((n) => {
      if (state.noteFilter === "pin" && !n.pinned) return false;
      if (state.noteFilter !== "all" && state.noteFilter !== "pin" && n.cat !== state.noteFilter) return false;
      if (!q) return true;
      return `${n.title} ${n.body}`.toLowerCase().includes(q);
    })
    .sort((a, b) => Number(b.pinned) - Number(a.pinned) || (a.date < b.date ? 1 : -1));

  $("#noteSub").textContent = `${rows.length} CATATAN · ${state.notes.filter((n) => n.pinned).length} DIPIN`;

  if (!rows.length) {
    list.innerHTML = `<p class="empty">TIDAK ADA CATATAN<br>YANG COCOK.<br><br>COBA KATA KUNCI LAIN.</p>`;
    return;
  }

  list.innerHTML = rows.map((n) => `
    <article class="note ${NOTE_COLOR[n.color] || ""}">
      <button class="pin" data-pin="${n.id}" aria-pressed="${!!n.pinned}" aria-label="Sematkan catatan">
        <span class="ico" data-icon="pin" data-size="13"></span>
      </button>
      <div class="note-head">
        <span class="date">${fmtID(n.date).toUpperCase()}</span>
        <span class="tag ${n.cat === "to-do" ? "sky" : n.cat === "refleksi" ? "teal" : "amber"}">${n.cat.toUpperCase()}</span>
      </div>
      <h4>${n.title}</h4>
      <p>${n.body}</p>
      <div class="foot">
        <button class="btn small ghost" data-note-edit="${n.id}"><span class="ico" data-icon="pencil" data-size="11"></span> Edit</button>
        <button class="btn small ghost" data-note-del="${n.id}"><span class="ico" data-icon="trash" data-size="11"></span> Hapus</button>
      </div>
    </article>`).join("");

  hydrateIcons(list);
}

function noteDialog(note) {
  const isNew = !note;
  const data = note || { title: "", body: "", cat: "ide", color: "amber" };
  openDialog(
    isNew ? "CATATAN BARU" : "EDIT CATATAN",
    [
      { type: "row", fields: [
        { name: "cat", label: "Kategori", type: "select", value: data.cat,
          options: [["ide", "IDE"], ["refleksi", "REFLEKSI"], ["to-do", "TO-DO"]] },
        { name: "color", label: "Warna", type: "select", value: data.color,
          options: [["amber", "AMBER"], ["mint", "MINT"], ["sky", "SKY"], ["coral", "CORAL"], ["plum", "PLUM"]] }
      ] },
      { name: "title", label: "Judul", type: "text", value: data.title, placeholder: "Contoh: ide ice breaking" },
      { name: "body", label: "Isi catatan", type: "textarea", value: data.body, placeholder: "Tulis bebas: ide, refleksi, rencana…" }
    ],
    isNew ? "Simpan Catatan" : "Simpan Perubahan",
    (values) => {
      if (!values.title.trim()) { toast("JUDUL CATATAN MASIH KOSONG."); return false; }
      if (isNew) {
        state.notes.unshift({
          id: Date.now(), title: values.title.trim(), body: values.body.trim(),
          cat: values.cat, color: values.color, pinned: false, date: todayISO()
        });
        toast("CATATAN DISIMPAN · +12 XP");
      } else {
        Object.assign(note, {
          title: values.title.trim(), body: values.body.trim(), cat: values.cat, color: values.color
        });
        toast("CATATAN DIPERBARUI");
      }
      renderCatatan(); renderHomeMeta(); persist();
      return true;
    }
  );
}

/* ------------------------------- absensi ------------------------------- */

function renderAbsensi() {
  const statuses = state.absensi[state.absClass];
  const counts = { H: 0, S: 0, I: 0, A: 0 };
  statuses.forEach((s) => { counts[s] = (counts[s] || 0) + 1; });

  $("#absSummary").innerHTML = `
    <div class="h"><b>${counts.H}</b><span>HADIR</span></div>
    <div class="s"><b>${counts.S}</b><span>SAKIT</span></div>
    <div class="i"><b>${counts.I}</b><span>IZIN</span></div>
    <div class="a"><b>${counts.A}</b><span>ALFA</span></div>`;

  $("#studentList").innerHTML = STUDENTS.map((nama, i) => `
    <div class="student-row">
      <span class="num">${String(i + 1).padStart(2, "0")}</span>
      <span class="nm">${nama}</span>
      <span class="status-group" data-index="${i}">
        ${Object.keys(STATUS_LABEL).map((k) => `
          <button class="st ${statuses[i] === k ? STATUS_CLASS[k] : ""}"
                  data-status="${k}" aria-label="${STATUS_LABEL[k]} untuk ${nama}"
                  aria-pressed="${statuses[i] === k}">${k}</button>`).join("")}
      </span>
    </div>`).join("");
}

/* -------------------------------- nilai -------------------------------- */

function renderNilai() {
  const rows = state.nilai[state.scoreClass];

  $("#scoreList").innerHTML = rows.map((n, i) => {
    const value = naOf(n);
    return `
      <div class="nilai-row" data-index="${i}">
        <span class="nm">${n.nama}</span>
        <input type="number" min="0" max="100" value="${n.tugas}" data-field="tugas" aria-label="Nilai tugas ${n.nama}">
        <input type="number" min="0" max="100" value="${n.uts}" data-field="uts" aria-label="Nilai UTS ${n.nama}">
        <input type="number" min="0" max="100" value="${n.uas}" data-field="uas" aria-label="Nilai UAS ${n.nama}">
        <span class="na ${naClass(value)}">${value}</span>
      </div>`;
  }).join("");

  updateSummary();
}

function updateSummary() {
  const values = state.nilai[state.scoreClass].map(naOf);
  const avg = values.reduce((a, b) => a + b, 0) / values.length;
  $("#avgScore").textContent = String(Math.round(avg)).padStart(2, "0");
  $("#maxScore").textContent = Math.max(...values);
  $("#lowScore").textContent = values.filter((v) => v < KKM).length;
}

/* -------------------------------- jadwal ------------------------------- */

function renderJadwal() {
  const lessons = JADWAL[state.day] || [];
  $("#lessonList").innerHTML = lessons.length
    ? lessons.map((l) => `
        <div class="lesson ${l.now ? "is-now" : ""}">
          <div class="clock">${l.time}<span>${l.end}</span></div>
          <div class="body">
            <h4>IPA · ${l.kelas}</h4>
            <div class="meta"><span>${l.room}</span><span>${l.materi}</span></div>
          </div>
          ${l.now ? `<span class="badge-now">SEKARANG</span>` : ""}
        </div>`)
    : `<p class="empty">TIDAK ADA JADWAL<br>DI HARI INI.<br><br>WAKTU UNTUK MENYIAPKAN<br>PERANGKAT AJAR.</p>`;
}

/* ------------------------------- kalender ------------------------------ */

const DOW = ["SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN"];

function isoOf(y, m, d) {
  return `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
}

function eventsOn(iso) {
  return state.events.filter((e) => e.date === iso);
}

function renderKalender() {
  const first = new Date(state.calMonth);
  const year = first.getFullYear();
  const month = first.getMonth();

  $("#calTitle").textContent = first
    .toLocaleDateString("id-ID", { month: "long", year: "numeric" })
    .toUpperCase();

  const startOffset = (new Date(year, month, 1).getDay() + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const daysPrev = new Date(year, month, 0).getDate();
  const today = todayISO();

  let cells = DOW.map((d) => `<span class="cal-dow">${d}</span>`).join("");

  for (let i = 0; i < startOffset; i++) {
    const d = daysPrev - startOffset + i + 1;
    cells += `<button class="cal-day out" data-date="${isoOf(month === 0 ? year - 1 : year, month === 0 ? 11 : month - 1, d)}">${d}</button>`;
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const iso = isoOf(year, month, d);
    const has = eventsOn(iso).length > 0;
    const cls = ["cal-day", iso === today ? "today" : "", iso === state.calSelected ? "sel" : ""].filter(Boolean).join(" ");
    cells += `<button class="${cls}" data-date="${iso}" aria-pressed="${iso === state.calSelected}">
        ${d}${has ? `<i class="dot-mark"></i>` : ""}
      </button>`;
  }

  const total = startOffset + daysInMonth;
  const trailing = (7 - (total % 7)) % 7;
  for (let i = 1; i <= trailing; i++) {
    cells += `<button class="cal-day out" data-date="${isoOf(month === 11 ? year + 1 : year, month === 11 ? 0 : month + 1, i)}">${i}</button>`;
  }

  $("#calGrid").innerHTML = cells;
  renderAgenda();
}

function renderAgenda() {
  const iso = state.calSelected;
  const list = eventsOn(iso);

  $("#agendaTitle").textContent =
    `AGENDA · ${new Date(`${iso}T00:00:00`).toLocaleDateString("id-ID", { weekday: "long", day: "numeric", month: "short" }).toUpperCase()}`;

  $("#agendaList").innerHTML = list.length
    ? list.sort((a, b) => (a.time > b.time ? 1 : -1)).map((e) => {
        const kind = EVENT_KIND[e.jenis] || EVENT_KIND.kegiatan;
        return `<div class="event ${kind.cls}">
            <span class="bar"></span>
            <div class="body">
              <h4>${e.title}</h4>
              <div class="meta"><span>${e.time}</span><span>${kind.label}</span></div>
            </div>
            <button class="icon-btn" data-event-del="${e.id}" aria-label="Hapus agenda" style="width:24px;height:24px">
              <span class="ico" data-icon="trash" data-size="12"></span>
            </button>
          </div>`;
      }).join("")
    : `<p class="empty">TIDAK ADA AGENDA.<br><br>TAP + UNTUK MENAMBAH.</p>`;

  hydrateIcons($("#agendaList"));
}

function eventDialog() {
  openDialog("AGENDA BARU", [
    { name: "title", label: "Judul agenda", type: "text", placeholder: "Contoh: UTS IPA kelas 8" },
    { type: "row", fields: [
      { name: "date", label: "Tanggal", type: "date", value: state.calSelected },
      { name: "time", label: "Jam", type: "text", value: "07:30", placeholder: "07:30" }
    ] },
    { name: "jenis", label: "Jenis", type: "select", value: "kegiatan",
      options: [["kegiatan", "KEGIATAN"], ["ujian", "UJIAN"], ["rapat", "RAPAT"], ["libur", "LIBUR"], ["admin", "ADMIN"]] }
  ], "Simpan Agenda", (v) => {
    if (!v.title.trim()) { toast("JUDUL AGENDA MASIH KOSONG."); return false; }
    state.events.push({ id: Date.now(), title: v.title.trim(), date: v.date, time: v.time || "—", jenis: v.jenis });
    state.calSelected = v.date;
    state.calMonth = new Date(`${v.date}T00:00:00`);
    state.calMonth.setDate(1);
    renderKalender(); persist();
    toast("AGENDA DITAMBAHKAN");
    return true;
  });
}

/* ------------------------------ kalkulator ----------------------------- */

function calcEval(expr) {
  const cleaned = expr.replace(/×/g, "*").replace(/÷/g, "/").replace(/%/g, "/100");
  if (!cleaned || !/[\d]/.test(cleaned)) return 0;
  const tokens = cleaned.match(/(\d+\.?\d*|\.\d+|[+\-*/])/g);
  if (!tokens) return 0;
  // validasi: token harus bergantian angka/operator
  for (let i = 0; i < tokens.length; i++) {
    const isNum = /^[\d.]/.test(tokens[i]);
    if (isNum === (i % 2 === 1)) return NaN;
  }
  const nums = [];
  const ops = [];
  for (let i = 0; i < tokens.length; i++) {
    if (i % 2 === 0) nums.push(parseFloat(tokens[i]));
    else ops.push(tokens[i]);
  }
  for (let i = 0; i < ops.length; i++) {
    while (ops[i] && (ops[i] === "*" || ops[i] === "/")) {
      const a = nums[i], b = nums[i + 1];
      const r = ops[i] === "*" ? a * b : a / b;
      nums.splice(i, 2, r);
      ops.splice(i, 1);
    }
  }
  let acc = nums[0];
  for (let i = 0; i < ops.length; i++) {
    acc = ops[i] === "+" ? acc + nums[i + 1] : acc - nums[i + 1];
  }
  return acc;
}

function calcDisplay(extra) {
  const expr = state.calcExpr;
  const shown = expr.replace(/\*/g, "×").replace(/\//g, "÷");
  $("#calcExpr").innerHTML = shown || "&nbsp;";
  const value = calcEval(expr);
  const out = $("#calcOut");
  if (Number.isNaN(value) || !Number.isFinite(value)) {
    out.textContent = "ERROR";
    return;
  }
  const rounded = Math.round(value * 100) / 100;
  out.textContent = Number.isInteger(rounded) ? String(rounded) : String(rounded);
  if (extra) out.textContent = extra;
}

function calcKey(key) {
  if (key === "AC") { state.calcExpr = ""; calcDisplay(); return; }
  if (key === "del") { state.calcExpr = state.calcExpr.slice(0, -1); calcDisplay(); return; }
  if (key === "=") {
    const value = calcEval(state.calcExpr);
    if (Number.isNaN(value) || !Number.isFinite(value)) { toast("EKSPRESI BELUM VALID."); return; }
    const rounded = Math.round(value * 100) / 100;
    state.calcExpr = String(rounded);
    $("#calcExpr").innerHTML = `= ${rounded}`.replace(/\*/g, "×");
    $("#calcOut").textContent = rounded;
    return;
  }
  const isOp = ["+", "-", "*", "/", "%"].includes(key);
  const last = state.calcExpr.slice(-1);
  if (isOp && (last === "" || ["+", "-", "*", "/"].includes(last))) {
    if (last === "" && key !== "-") return;
    state.calcExpr = state.calcExpr.slice(0, -1) + key;
  } else if (key === "." ) {
    const tail = state.calcExpr.split(/[+\-*/%]/).pop();
    if (tail.includes(".")) return;
    state.calcExpr += tail === "" ? "0." : ".";
  } else {
    state.calcExpr += key;
  }
  calcDisplay();
}

function renderCalcNilai() {
  const wT = intOr($("#wTugas").value, 0);
  const wU = intOr($("#wUts").value, 0);
  const wA = intOr($("#wUas").value, 0);
  const weightSum = wT + wU + wA;
  const nT = clamp(intOr($("#nTugas").value, 0), 0, 100);
  const nU = clamp(intOr($("#nUts").value, 0), 0, 100);
  const nA = clamp(intOr($("#nUas").value, 0), 0, 100);

  const na = Math.round((nT * wT + nU * wU + nA * wA) / (weightSum || 1));
  const p = predikat(na);

  $("#naBig").textContent = na;
  $("#naBig").className = `big ${p.cls}`;
  $("#naPredikat").textContent = `PREDIKAT ${p.letter} · ${p.label}`;
  $("#naStatus").textContent = na >= KKM ? `DI ATAS KKM ${KKM} · TUNTAS` : `DI BAWAH KKM ${KKM} · PERLU REMEDIAL`;
  $("#naWeightInfo").textContent = weightSum === 100
    ? `BOBOT ${wT}/${wU}/${wA} · TOTAL 100%`
    : `BOBOT ${wT}/${wU}/${wA} · TOTAL ${weightSum}% (DINORMALKAN)`;
}

function renderKonversi() {
  const raw = intOr($("#convIn").value, 0);
  const value = clamp(raw, 0, 100);
  const p = predikat(value);
  $("#convOut").value = (value / 25).toFixed(2);
  $("#convBig").textContent = p.letter;
  $("#convBig").className = `big ${p.cls}`;
  $("#convLabel").textContent = p.label;
  $("#convDesc").textContent =
    value >= 90 ? "Menguasai kompetensi, siap pengayaan." :
    value >= 80 ? "Menguasai kompetensi, perlu latihan mandiri." :
    value >= 70 ? "Cukup, perlu pendampingan terarah." :
    "Perlu bimbingan intensif dan remedial.";
}

/* -------------------------------- binder ------------------------------- */

function docProgress(doc) {
  const done = doc.items.filter((i) => i[1]).length;
  return { done, total: doc.items.length, pct: Math.round((done / doc.items.length) * 100) };
}

function renderBinder() {
  const rows = state.docs.filter((d) => state.binderFilter === "all" || d.jenis === state.binderFilter);
  const allDone = state.docs.reduce((acc, d) => acc + docProgress(d).done, 0);
  const allTotal = state.docs.reduce((acc, d) => acc + docProgress(d).total, 0);

  $("#binderSub").textContent = `IPA · ${state.docs.length} DOKUMEN · ${allDone}/${allTotal} CEK`;

  $("#binderList").innerHTML = rows.length ? rows.map((d) => {
    const p = docProgress(d);
    return `<button class="binder-card" data-doc="${d.id}">
        <span class="binder-tab ${DOC_TAB[d.jenis] || ""}">${d.jenis.toUpperCase()}</span>
        <span class="binder-icon"><span class="ico" data-icon="doc" data-size="20"></span></span>
        <span class="binder-body">
          <h4>${d.nama}</h4>
          <span class="meta">
            <span>${d.format} · ${d.size}</span>
            <span>${d.kelas}</span>
            <span>${d.updated}</span>
          </span>
          <span class="mini-bar amber"><span style="width:${p.pct}%"></span></span>
          <span class="mini-meta"><span>CHECKLIST ${p.done}/${p.total}</span><span>${p.pct}%</span></span>
        </span>
      </button>`;
  }).join("") : `<p class="empty">TIDAK ADA DOKUMEN<br>DI KATEGORI INI.</p>`;

  hydrateIcons($("#binderList"));
}

function docDialog(doc) {
  const isNew = !doc;
  const items = isNew ? [] : doc.items.map(([label, done], i) => ({ label, done, i }));
  const checklist = items.length
    ? `<div class="field"><label>Checklist isi dokumen</label>
         <div class="checklist">${items.map((it) => `
           <label><input type="checkbox" data-doc-item="${it.i}" ${it.done ? "checked" : ""}> ${it.label}</label>`).join("")}
         </div></div>`
    : "";

  openDialog(
    isNew ? "UNGGAH DOKUMEN" : "DETAIL DOKUMEN",
    [
      { name: "nama", label: "Nama dokumen", type: "text", value: doc ? doc.nama : "", placeholder: "Contoh: Modul Ajar — Ekosistem" },
      { type: "row", fields: [
        { name: "jenis", label: "Kategori", type: "select", value: doc ? doc.jenis : "Modul Ajar",
          options: [["Modul Ajar", "MODUL AJAR"], ["LKPD", "LKPD"], ["Bank Soal", "BANK SOAL"], ["Administrasi", "ADMINISTRASI"], ["Lainnya", "LAINNYA"]] },
        { name: "kelas", label: "Kelas", type: "select", value: doc ? doc.kelas : "7A",
          options: [["7A", "7A"], ["7B", "7B"], ["8A", "8A"], ["8B", "8B"], ["SEMUA", "SEMUA"]] }
      ] }
    ],
    isNew ? "Simpan Dokumen" : "Simpan Perubahan",
    (v) => {
      if (!v.nama.trim()) { toast("NAMA DOKUMEN MASIH KOSONG."); return false; }

      const checks = $$("[data-doc-item]", $("#dialogBody"));
      if (isNew) {
        state.docs.unshift({
          id: Date.now(), nama: v.nama.trim(), jenis: v.jenis, kelas: v.kelas,
          size: "—", format: "PDF", updated: fmtID(todayISO(), { day: "numeric", month: "short" }).toUpperCase() + " " + new Date().getFullYear(),
          items: [["Periksa kelengkapan", false], ["Siap dipakai di kelas", false]]
        });
        toast("DOKUMEN DITAMBAHKAN KE BINDER");
      } else {
        doc.nama = v.nama.trim();
        doc.jenis = v.jenis;
        doc.kelas = v.kelas;
        checks.forEach((c) => { doc.items[Number(c.dataset.docItem)][1] = c.checked; });
        toast("DOKUMEN DIPERBARUI");
      }
      renderBinder(); persist();
      return true;
    },
    checklist
  );
}

/* -------------------------------- kelas -------------------------------- */

function renderKelas() {
  $("#classList").innerHTML = Object.entries(CLASSES).map(([kode, c], i) => {
    const scores = state.nilai[kode].map(naOf);
    const avg = Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
    const statuses = state.absensi[kode];
    const hadirPct = Math.round((statuses.filter((s) => s === "H").length / statuses.length) * 100);
    const notes = state.jurnal.filter((j) => j.kelas === kode).length;
    const tone = ["", "b", "c", "d"][i % 4];
    return `<button class="class-card" data-class="${kode}">
        <span class="top">
          <span class="class-badge ${tone}">${kode}</span>
          <span style="flex:1;min-width:0">
            <h4>${c.rombel} · ${c.subject}</h4>
            <span class="sub">${c.total} SISWA · ${notes} JURNAL · KKM ${KKM}</span>
          </span>
          <span class="ico" data-icon="chevR"></span>
        </span>
        <span class="mini-bar"><span style="width:${hadirPct}%"></span></span>
        <span class="mini-meta"><span>KEHADIRAN ${hadirPct}%</span><span>RATA NA ${avg}</span></span>
      </button>`;
  }).join("");

  hydrateIcons($("#classList"));
}

function classDialog(kode) {
  const c = CLASSES[kode];
  const scores = state.nilai[kode].map(naOf);
  const avg = Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
  const statuses = state.absensi[kode];
  const counts = { H: 0, S: 0, I: 0, A: 0 };
  statuses.forEach((s) => { counts[s] += 1; });
  const notes = state.jurnal.filter((j) => j.kelas === kode);

  const html = `
    <div class="result-box" style="margin-bottom:10px">
      <span class="big ${avg < KKM ? "bad" : ""}">${avg}</span>
      <span class="info">
        <b>RATA-RATA NA ${kode}</b>
        <span>H ${counts.H} · S ${counts.S} · I ${counts.I} · A ${counts.A}</span>
        <span>${notes.length} JURNAL TERCATAT</span>
      </span>
    </div>
    <div class="file-list">
      ${notes.slice(0, 4).map((j) => `
        <div class="file-row">
          <span class="fi ico" data-icon="book" data-size="12"></span>
          <span class="fn">${j.materi}</span>
          <span class="fs">${j.time}</span>
        </div>`).join("") || `<p class="empty" style="padding:8px 0">BELUM ADA JURNAL KELAS INI.</p>`}
    </div>
    <div class="dialog-actions" style="margin-top:12px">
      <button class="btn ghost" data-goto="absensi">Absensi</button>
      <button class="btn ghost" data-goto="nilai">Nilai</button>
      <button class="btn" data-goto="jurnal">Jurnal</button>
    </div>`;

  openDialog(`KELAS ${kode}`, [], null, null, html);
}

/* ------------------------------ dialog --------------------------------- */

let dialogOnSubmit = null;

function fieldHTML(f) {
  if (f.type === "row") {
    return `<div class="field-row">${f.fields.map(fieldHTML).join("")}</div>`;
  }
  const id = `d_${f.name}`;
  let control;
  if (f.type === "select") {
    control = `<select id="${id}" data-field="${f.name}">
        ${f.options.map(([v, l]) => `<option value="${v}" ${v === f.value ? "selected" : ""}>${l}</option>`).join("")}
      </select>`;
  } else if (f.type === "textarea") {
    control = `<textarea id="${id}" data-field="${f.name}" placeholder="${f.placeholder || ""}">${f.value || ""}</textarea>`;
  } else {
    control = `<input id="${id}" data-field="${f.name}" type="${f.type === "date" ? "date" : "text"}"
        value="${f.value || ""}" placeholder="${f.placeholder || ""}">`;
  }
  return `<div class="field"><label for="${id}">${f.label}</label>${control}</div>`;
}

function openDialog(title, fields, submitLabel, onSubmit, extraHTML) {
  $("#dialogTitle").textContent = title;
  $("#dialogBody").innerHTML =
    (extraHTML || "") +
    fields.map(fieldHTML).join("") +
    (submitLabel ? `<div class="dialog-actions">
        <button class="btn ghost" data-close-dialog>Batal</button>
        <button class="btn" id="dialogSubmit">${submitLabel}</button>
      </div>` : "");

  dialogOnSubmit = onSubmit || null;
  $("#dialog").classList.add("open");
  hydrateIcons($("#dialog"));

  const first = $('#dialog input, #dialog textarea, #dialog select');
  if (first && submitLabel) setTimeout(() => first.focus(), 80);
}

function closeDialog() {
  $("#dialog").classList.remove("open");
  dialogOnSubmit = null;
}

function submitDialog() {
  const values = {};
  $$("[data-field]", $("#dialogBody")).forEach((el) => {
    values[el.dataset.field] = el.type === "checkbox" ? el.checked : el.value.trim ? el.value.trim() : el.value;
  });
  const result = dialogOnSubmit ? dialogOnSubmit(values) : true;
  if (result !== false) closeDialog();
}

/* -------------------------------- sheet -------------------------------- */

function openJournal() { $("#journalSheet").classList.add("open"); setTimeout(() => $("#fMateri")?.focus(), 60); }
function closeSheet() { $("#journalSheet").classList.remove("open"); }

/* ------------------------------ toggles -------------------------------- */

function hydrateTogglesFromBody() {
  const map = {
    theme: document.body.classList.contains("theme-light"),
    palette: document.body.classList.contains("palette-warm"),
    scanlines: document.body.classList.contains("scanlines"),
    motion: !document.body.classList.contains("no-motion"),
    inspect: $("#scene")?.classList.contains("inspect") || false
  };
  $$("[data-toggle]").forEach((row) => {
    const on = map[row.dataset.toggle] ?? false;
    row.setAttribute("aria-checked", String(on));
    const sw = $(".switch", row);
    if (sw) sw.setAttribute("aria-checked", String(on));
  });
  const st = $("#switchTheme"); if (st) st.setAttribute("aria-checked", String(map.theme));
  const si = $("#switchInspect"); if (si) si.setAttribute("aria-checked", String(map.inspect));
}

function applyToggle(key, on) {
  if (key === "theme") {
    document.body.classList.toggle("theme-light", on);
    state.sceneVariant = on ? "day" : "night";
    renderTiles();
    renderAtlas();
    toast(on ? "MODE TERANG AKTIF" : "MODE GELAP AKTIF");
  }
  if (key === "palette") {
    document.body.classList.toggle("palette-warm", on);
    toast(on ? "PALET HANGAT AKTIF" : "PALET SENJA AKTIF");
  }
  if (key === "scanlines") document.body.classList.toggle("scanlines", on);
  if (key === "motion") document.body.classList.toggle("no-motion", !on);
  if (key === "inspect") {
    $("#scene").classList.toggle("inspect", on);
    toast(on ? "INSPEKSI TILE AKTIF · KOTAK = AREA TAP" : "INSPEKSI TILE NONAKTIF");
  }
  hydrateTogglesFromBody();
  persist();
}

/* -------------------------------- kit ---------------------------------- */

function initKit() {
  $("#kitPal").innerHTML = PALETTE.map((p) => `
    <button data-hex="${p.hex}" title="Salin ${p.hex}">
      <i style="background:${p.hex}"></i>
      <b>${p.name.toUpperCase()}</b><span>${p.hex}</span>
    </button>`).join("");

  $("#swatches").innerHTML = PALETTE.slice(0, 8).map((p) => `
    <button class="swatch" style="background:${p.hex}" data-hex="${p.hex}" title="${p.name} ${p.hex}"
      aria-label="Salin warna ${p.name} ${p.hex}"></button>`).join("");

  const rows = TILES.map((t) => {
    const [x, y, w, h] = t.night;
    const px = (v, total) => ((v / total) * 100).toFixed(2);
    return `<tr>
        <td>${t.label}</td>
        <td>${t.id}</td>
        <td>${px(x, 1408)}/${px(y, 768)}<br>${px(w, 1408)}×${px(h, 768)}</td>
      </tr>`;
  }).join("");
  $("#tileTable").innerHTML = `<tr><th>TUJUAN</th><th>ID</th><th>X/Y · W×H (%)</th></tr>${rows}`;

  $("#tileHints").innerHTML = TILES.map((t) => `
    <li><span class="dot"></span><span><b>${t.label}</b> → ${t.target}</span></li>`).join("") +
    `<li><span class="dot" style="background:var(--sky)"></span><span><b>Kalkulator</b> → toolbar</span></li>`;
}

function renderAtlas() {
  const el = $("#atlas");
  if (!el) return;
  const variant = state.sceneVariant;
  el.innerHTML = TILES.map((t) => {
    const rect = t[variant];
    return `<figure>
        <img src="assets/tiles/${variant}-${t.id}.png" alt="Objek ${t.label}" loading="lazy">
        <figcaption>
          <b>${t.label.toUpperCase()}</b><br>
          ${t.id} · ${rect[2]}×${rect[3]} PX
        </figcaption>
      </figure>`;
  }).join("");
}

function copyHex(hex) {
  const done = () => toast(`WARNA ${hex} DISALIN`);
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(hex).then(done).catch(() => toast(`WARNA ${hex}`));
  } else toast(`WARNA ${hex}`);
}

/* ------------------------------- events -------------------------------- */

function initEvents() {
  document.addEventListener("click", (e) => {
    const goto = e.target.closest("[data-goto]");
    if (goto) {
      if ($("#dialog").classList.contains("open")) closeDialog();
      closeSheet();
      showScreen(goto.dataset.goto);
      return;
    }

    const soon = e.target.closest("[data-soon]");
    if (soon) { toast(`SEGERA HADIR: ${soon.dataset.soon.toUpperCase()}`); return; }

    const swatch = e.target.closest("[data-hex]");
    if (swatch) { copyHex(swatch.dataset.hex); return; }

    if (e.target.closest("[data-close-dialog]")) { closeDialog(); return; }
    if (e.target.id === "dialog") { closeDialog(); return; }
    if (e.target.closest("[data-close-sheet]")) { closeSheet(); return; }
    if (e.target.id === "journalSheet") { closeSheet(); return; }
  });

  $$(".tab").forEach((tab) => tab.addEventListener("click", () => showScreen(tab.dataset.screen)));
  $$(".nav-btn").forEach((btn) => btn.addEventListener("click", () => showScreen(btn.dataset.goto)));

  /* toggles */
  $$("[data-toggle]").forEach((row) => {
    const flip = () => applyToggle(row.dataset.toggle, row.getAttribute("aria-checked") !== "true");
    row.addEventListener("click", flip);
    row.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); flip(); }
    });
  });

  /* jurnal */
  $("#journalChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.journalFilter = chip.dataset.kelas;
    $$(".chip", $("#journalChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderJurnal();
  });

  ["#openJournal", "#openJournal2"].forEach((sel) => $(sel)?.addEventListener("click", openJournal));

  $("#saveJournal").addEventListener("click", () => {
    const materi = $("#fMateri").value.trim();
    if (!materi) { toast("ISI MATERI DULU YA."); $("#fMateri").focus(); return; }
    const hadir = Math.max(0, intOr($("#fHadir").value, 0));
    const total = Math.max(hadir, intOr($("#fTotal").value, hadir));
    const now = new Date();

    state.jurnal.unshift({
      id: Date.now(), date: todayISO(), kelas: $("#fKelas").value,
      time: now.toLocaleTimeString("id-ID", { hour: "2-digit", minute: "2-digit" }).replace(".", ":"),
      jam: $("#fJam").value || "—", materi,
      note: $("#fCatatan").value.trim() || "Tanpa catatan tambahan.",
      hadir, total
    });

    state.journalFilter = "all";
    $$(".chip", $("#journalChips")).forEach((c) => c.setAttribute("aria-pressed", String(c.dataset.kelas === "all")));
    renderJurnal(); renderHomeMeta(); persist();
    closeSheet();
    $("#fMateri").value = ""; $("#fCatatan").value = "";
    toast("CATATAN TERSIMPAN · +25 XP");
  });

  /* catatan */
  ["#openNote", "#openNote2"].forEach((sel) => $(sel)?.addEventListener("click", () => noteDialog(null)));

  $("#noteChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.noteFilter = chip.dataset.filter;
    $$(".chip", $("#noteChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderCatatan();
  });

  $("#noteSearch").addEventListener("input", (e) => {
    state.noteQuery = e.target.value;
    renderCatatan();
  });

  $("#noteList").addEventListener("click", (e) => {
    const pin = e.target.closest("[data-pin]");
    if (pin) {
      const note = state.notes.find((n) => n.id === Number(pin.dataset.pin));
      if (note) { note.pinned = !note.pinned; renderCatatan(); persist(); toast(note.pinned ? "CATATAN DISEMATKAN" : "SEMATAN DILEPAS"); }
      return;
    }
    const edit = e.target.closest("[data-note-edit]");
    if (edit) {
      const note = state.notes.find((n) => n.id === Number(edit.dataset.noteEdit));
      if (note) noteDialog(note);
      return;
    }
    const del = e.target.closest("[data-note-del]");
    if (del) {
      const id = Number(del.dataset.noteDel);
      const note = state.notes.find((n) => n.id === id);
      if (!note) return;
      openDialog("HAPUS CATATAN", [], null, null,
        `<p style="font-size:13.5px;line-height:1.6;margin:0 0 6px">Hapus catatan <b>“${note.title}”</b>? Tindakan ini tidak bisa dibatalkan.</p>
         <div class="dialog-actions" style="margin-top:14px">
           <button class="btn ghost" data-close-dialog>Batal</button>
           <button class="btn" id="confirmDelete">Hapus</button>
         </div>`);
      $("#confirmDelete").addEventListener("click", () => {
        state.notes = state.notes.filter((n) => n.id !== id);
        renderCatatan(); renderHomeMeta(); persist(); closeDialog(); toast("CATATAN DIHAPUS");
      });
    }
  });

  /* absensi */
  $("#absClassChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.absClass = chip.dataset.kelas;
    $$(".chip", $("#absClassChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderAbsensi();
  });

  $("#studentList").addEventListener("click", (e) => {
    const btn = e.target.closest(".st");
    if (!btn) return;
    const index = Number(btn.closest(".status-group").dataset.index);
    state.absensi[state.absClass][index] = btn.dataset.status;
    renderAbsensi();
  });

  $("#saveAbsensi").addEventListener("click", () => {
    persist();
    const statuses = state.absensi[state.absClass];
    const count = (k) => statuses.filter((s) => s === k).length;
    toast(`ABSENSI ${state.absClass} TERSIMPAN · H ${count("H")} · S ${count("S")} · I ${count("I")} · A ${count("A")}`);
  });

  /* nilai */
  $("#scoreClassChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.scoreClass = chip.dataset.kelas;
    $$(".chip", $("#scoreClassChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderNilai();
  });

  $("#scoreList").addEventListener("input", (e) => {
    const input = e.target.closest("input");
    if (!input) return;
    const row = input.closest(".nilai-row");
    const index = Number(row.dataset.index);
    state.nilai[state.scoreClass][index][input.dataset.field] = clamp(intOr(input.value, 0), 0, 100);
    const total = naOf(state.nilai[state.scoreClass][index]);
    const naEl = row.querySelector(".na");
    naEl.textContent = total;
    naEl.className = `na ${naClass(total)}`;
    updateSummary();
  });

  $("#saveNilai").addEventListener("click", () => {
    persist();
    toast(`NILAI ${state.scoreClass} TERSIMPAN · NA OTOMATIS DIHITUNG`);
  });

  /* jadwal */
  $("#dayTabs").addEventListener("click", (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    state.day = btn.dataset.day;
    $$("button", $("#dayTabs")).forEach((b) => b.setAttribute("aria-pressed", String(b === btn)));
    renderJadwal();
  });

  /* kalender */
  $("#calPrev").addEventListener("click", () => {
    state.calMonth = new Date(state.calMonth.getFullYear(), state.calMonth.getMonth() - 1, 1);
    renderKalender();
  });
  $("#calNext").addEventListener("click", () => {
    state.calMonth = new Date(state.calMonth.getFullYear(), state.calMonth.getMonth() + 1, 1);
    renderKalender();
  });
  $("#calGrid").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-date]");
    if (!btn) return;
    state.calSelected = btn.dataset.date;
    const d = new Date(`${btn.dataset.date}T00:00:00`);
    if (d.getMonth() !== state.calMonth.getMonth()) {
      state.calMonth = new Date(d.getFullYear(), d.getMonth(), 1);
    }
    renderKalender();
  });
  $("#openEvent").addEventListener("click", eventDialog);
  $("#agendaAdd").addEventListener("click", eventDialog);
  $("#agendaList").addEventListener("click", (e) => {
    const del = e.target.closest("[data-event-del]");
    if (!del) return;
    state.events = state.events.filter((ev) => ev.id !== Number(del.dataset.eventDel));
    renderKalender(); persist(); toast("AGENDA DIHAPUS");
  });

  /* kalkulator */
  $("#calcSeg").addEventListener("click", (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    state.calcMode = btn.dataset.mode;
    $$("button", $("#calcSeg")).forEach((b) => b.setAttribute("aria-pressed", String(b === btn)));
    $("#calcHitung").style.display = state.calcMode === "hitung" ? "" : "none";
    $("#calcNilai").style.display = state.calcMode === "nilai" ? "" : "none";
    $("#calcKonversi").style.display = state.calcMode === "konversi" ? "" : "none";
    if (state.calcMode === "nilai") renderCalcNilai();
    if (state.calcMode === "konversi") renderKonversi();
  });

  $("#calcKeys").addEventListener("click", (e) => {
    const key = e.target.closest("[data-key]");
    if (key) calcKey(key.dataset.key);
  });

  $("#calcReset").addEventListener("click", () => {
    state.calcExpr = "";
    calcDisplay();
    $("#nTugas").value = "82"; $("#nUts").value = "90"; $("#nUas").value = "88";
    $("#wTugas").value = "40"; $("#wUts").value = "30"; $("#wUas").value = "30";
    $("#convIn").value = "87";
    renderCalcNilai(); renderKonversi();
    toast("KALKULATOR DIRESET");
  });

  ["#wTugas", "#wUts", "#wUas", "#nTugas", "#nUts", "#nUas"].forEach((sel) => {
    $(sel).addEventListener("input", renderCalcNilai);
  });
  $("#convIn").addEventListener("input", renderKonversi);

  document.addEventListener("keydown", (e) => {
    if ($("#dialog").classList.contains("open")) {
      if (e.key === "Escape") closeDialog();
      if (e.key === "Enter" && e.target.tagName !== "TEXTAREA") {
        if (dialogOnSubmit) { e.preventDefault(); submitDialog(); }
      }
      return;
    }
    if (e.key === "Escape") closeSheet();

    if (state.screen === "kalkulator" && state.calcMode === "hitung" && !["INPUT", "TEXTAREA", "SELECT"].includes(e.target.tagName)) {
      const map = { Enter: "=", Backspace: "del", Escape: "AC", "*": "*", "/": "/", "+": "+", "-": "-", "%": "%", ".": "." };
      const key = /^\d$/.test(e.key) ? e.key : map[e.key];
      if (key) { e.preventDefault(); calcKey(key); }
    }
  });

  /* binder */
  $("#binderChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.binderFilter = chip.dataset.jenis;
    $$(".chip", $("#binderChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderBinder();
  });
  ["#openDoc", "#openDoc2"].forEach((sel) => $(sel)?.addEventListener("click", () => docDialog(null)));
  $("#binderList").addEventListener("click", (e) => {
    const card = e.target.closest("[data-doc]");
    if (!card) return;
    const doc = state.docs.find((d) => d.id === Number(card.dataset.doc));
    if (doc) docDialog(doc);
  });

  /* kelas */
  $("#classList").addEventListener("click", (e) => {
    const card = e.target.closest("[data-class]");
    if (card) classDialog(card.dataset.class);
  });

  /* profil: pengaturan */
  $$("[data-setting]").forEach((row) => {
    row.addEventListener("click", () => {
      const key = row.dataset.setting;
      if (key === "theme") applyToggle("theme", !document.body.classList.contains("theme-light"));
      if (key === "inspect") applyToggle("inspect", !$("#scene").classList.contains("inspect"));
      if (key === "backup") {
        const data = JSON.stringify({ jurnal: state.jurnal, notes: state.notes, events: state.events, docs: state.docs }, null, 2);
        toast(`BACKUP SIAP · ${(data.length / 1024).toFixed(1)} KB JSON`);
      }
      if (key === "reset") {
        openDialog("RESET DATA", [], null, null,
          `<p style="font-size:13.5px;line-height:1.6;margin:0">Semua catatan, jurnal, agenda, dan nilai yang Anda ubah akan kembali ke data contoh.</p>
           <div class="dialog-actions" style="margin-top:14px">
             <button class="btn ghost" data-close-dialog>Batal</button>
             <button class="btn" id="confirmReset">Reset</button>
           </div>`);
        $("#confirmReset").addEventListener("click", () => {
          localStorage.removeItem(LS_KEY);
          location.reload();
        });
      }
    });
  });

  /* dialog submit */
  $("#dialogBody").addEventListener("click", (e) => {
    if (e.target.closest("#dialogSubmit")) submitDialog();
  });
}

/* -------------------------------- boot --------------------------------- */

function boot() {
  restore();
  hydrateIcons();
  initKit();
  renderTiles();
  renderAtlas();
  renderInventory();
  initEvents();
  renderHomeMeta();
  renderJurnal();
  renderCatatan();
  renderAbsensi();
  renderNilai();
  renderJadwal();
  renderKalender();
  renderBinder();
  renderKelas();
  calcDisplay();
  renderCalcNilai();
  renderKonversi();
  hydrateIcons();
  hydrateTogglesFromBody();
  showScreen("home");
}

document.addEventListener("DOMContentLoaded", boot);
