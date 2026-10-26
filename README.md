# Ruang Kelas — Pixel Workspace

Aplikasi guru (native Android, rencana Kotlin + Jetpack Compose) dengan tampilan
**workspace bergaya 64-bit pixel art**: ruang kerja guru yang bisa diketuk, kartu
bergaya game, dan tipografi piksel.

Tahap saat ini: **UI/UX prototipe** — interaktif, bisa dibuka di browser, dan
sekaligus menjadi acuan token desain untuk implementasi Android.

## Menjalankan prototipe

```bash
npm start          # python3 -m http.server 4173 → http://localhost:4173
```

Atau buka `prototype/index.html` langsung (font & gambar sudah lokal, tanpa internet).

## Struktur

```
prototype/
├── index.html                  # kerangka layar + panel Design Kit
├── styles.css                  # design token (warna, tipografi, komponen)
├── app.js                      # data contoh, render, interaksi, penyimpanan lokal
└── assets/
    ├── workspace-scene.png     # ilustrasi ruang kerja 64-bit pixel art (1408×768)
    └── fonts/                  # Pixelify Sans, Silkscreen, Press Start 2P (OFL)
```

## Layar yang sudah dirancang

| Layar | Isi | Interaksi |
|---|---|---|
| **Beranda** | Scene ruang kerja + hotspot objek, level/XP guru, 3 statistik, 6 kartu tool | Ketuk laptop → Jurnal, papan tulis → Absensi, papan pin → Nilai, jam → Jadwal |
| **Jurnal** | Daftar catatan pertemuan (tanggal, kelas, jam, materi, refleksi, kehadiran), filter kelas | Bottom sheet "Catat Pertemuan", simpan → entri baru + toast |
| **Absensi** | Ringkasan H/S/I/A, daftar siswa, status per siswa | Ketuk status, simpan per kelas, data tersimpan di `localStorage` |
| **Nilai** | Tabel Tugas/UTS/UAS, NA otomatis (40/30/30), rata-rata, tertinggi, jumlah < KKM | Input angka langsung, warna NA mengikuti KKM 75 |
| **Jadwal** | Per hari Senin–Jumat, penanda "sekarang" | Pindah hari, ekspor `.ics` (placeholder) |
| **Design Kit** | Palet, tipografi, komponen, spesifikasi grid | Salin kode warna ke clipboard |

## Design token

Palet gelap senja dengan aksen amber:

| Token | Hex | Pemakaian |
|---|---|---|
| Ink | `#0E0D1A` | outline & bayangan keras |
| Night | `#15142A` | latar layar |
| Panel | `#272647` | kartu, tombol sekunder |
| Cream | `#F8E9C8` | teks utama |
| Amber | `#F2AB3F` | aksi utama, progres XP |
| Coral | `#EF6A5B` | alfa, peringatan |
| Teal / Mint | `#4FA49A` / `#9FD8B0` | hadir, simpan |
| Sky | `#77B8D9` | sakit, statistik |
| Plum | `#A58ECB` | kategori kelas lain |

Aturan bentuk: sudut **0 px**, outline **2–3 px** gelap, bayangan **solid 3–4 px
tanpa blur**, grid 8 dp, padding layar 14 dp. Font: **Pixelify Sans** (UI),
**Silkscreen** (label/data), **Press Start 2P** (judul/logo).

## Langkah berikutnya (implementasi Android)

1. Skeleton proyek Compose (`minSdk 26`), tema warna dari tabel token di atas.
2. `HomeScreen` dengan `Image` scene + hotspot `Box` yang bisa diketuk.
3. Room: entitas `Kelas`, `Siswa`, `Jurnal`, `Nilai`, `Jadwal`, `Absensi`.
4. Fitur ekspor Excel/CSV untuk administrasi, lalu APK rilis.

Scene ilustrasi dapat diganti dengan aset pixel art lain (mis. tile per objek)
agar hotspot presisi dan mendukung animasi idle.
