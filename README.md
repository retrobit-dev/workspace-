# Ruang Kelas — Pixel Workspace (Tablet Landscape)

Aplikasi guru native Android (rencana Kotlin + Jetpack Compose) untuk **HUAWEI MatePad Mini 8,8"**
dalam **mode landscape**, dengan tampilan workspace pixel art **gelap** bergaya game retro.

Tahap saat ini: **UI/UX prototipe v0.3** — interaktif di browser, sekaligus acuan token untuk Android.

## Target perangkat

| | |
|---|---|
| Perangkat | HUAWEI MatePad Mini 8,8" |
| Layar | 2560 × 1600 (16:10), landscape lock |
| UI acuan | 1280 × 800 DP (density ≈ 2,0) |
| Rail navigasi | 84 DP · panel konteks 300 DP |
| Art scene | **51 × 32 piksel** · 32 warna · nearest-neighbour |

## Menjalankan prototipe

```bash
npm start          # python3 -m http.server 4173 → http://localhost:4173
```

Atau buka `prototype/index.html` langsung (font & gambar lokal, tanpa internet).
Toggle **Fokus tablet** di panel kiri menyembunyikan panel kontrol agar tampil penuh seperti di perangkat.

## Struktur

```
prototype/
├── index.html                 # 11 layar, rail, panel konteks, Design Kit
├── styles.css                 # token desain + komponen dasar
├── tablet.css                 # tata letak landscape (rail + konten + panel)
├── app.js                     # data contoh, render, interaksi, jam digital, localStorage
└── assets/
    ├── scene-32.png           # scene utama 51 × 32 (art 32 px)
    ├── scene-64.png           # versi 102 × 64 (opsi detail)
    ├── scene-landscape-raw.png# sumber 1312 × 816 untuk membuat ulang scene
    ├── scene-32-map.png       # peta area tap semua objek
    ├── tiles32/               # 11 objek dipotong (untuk animasi terpisah di Compose)
    └── fonts/                 # Pixelify Sans, Silkscreen, Press Start 2P (OFL)
```

## Scene: jam digital, kalender meja, dan laci

Scene malam digambar ulang khusus landscape, lalu diturunkan ke grid 32 piksel:

- **Jam digital** di atas meja (kotak hijau) → **Jadwal**
- **Kalender meja** tepat di sebelah kanan jam → **Kalender**
- **Laci meja** di bawah meja → **Binder materi**
- Laptop → Jurnal · papan tulis → Absensi · papan pin → Catatan · buku → Nilai
- Nampan di bawah meja → Kalkulator · jendela → Kelas · kursi → Profil · rak → Binder

Koordinat tap (satuan grid 51 × 32, `x · y · w × h`):

| Objek | Tujuan | x | y | w | h |
|---|---|---|---|---|---|
| laptop | Jurnal | 14 | 15 | 8 | 5 |
| buku | Nilai | 6 | 18 | 7 | 4 |
| papan tulis | Absensi | 25 | 6 | 16 | 10 |
| papan pin | Catatan | 41 | 6 | 10 | 9 |
| jam digital | Jadwal | 24 | 15 | 7 | 6 |
| kalender meja | Kalender | 31 | 15 | 6 | 7 |
| laci | Binder | 27 | 23 | 11 | 9 |
| nampan | Kalkulator | 16 | 23 | 8 | 4 |
| jendela | Kelas | 0 | 1 | 9 | 23 |
| kursi | Profil | 41 | 15 | 9 | 9 |
| rak | Binder | 12 | 5 | 12 | 9 |

Aktifkan **Inspeksi tile** untuk melihat batas tap di scene. Di Compose, koordinat ini
dipakai apa adanya dengan `BoxWithConstraints` (persen dari lebar/tinggi gambar).

## Jam digital

- Jam besar 24 jam di status bar kiri atas + tanggal panjang (`SENIN · 26 OKT 2026`).
- Blok jam digital hijau di panel kanan: `09:12:44` dengan efek glow + titik berkedip.
- Tag di scene ikut menampilkan jam aktif.
- Font **Silkscreen**, angka tabular, `letter-spacing` lebar.

## Layar

Rail kiri berisi 11 tujuan: Beranda · Jurnal · Catatan · Absensi · Nilai · Jadwal ·
Kalender · Kalkulator · Binder · Kelas · Profil. Panel kanan selalu menampilkan jam,
agenda hari ini, kelas cepat, dan progres XP.

| Layar | Isi |
|---|---|
| **Beranda** | scene landscape + 11 hotspot, 4 statistik, 8 kartu tool, agenda berikutnya, 3 catatan terakhir |
| **Jurnal** | grid catatan pertemuan, filter kelas, sheet tambah catatan |
| **Buku Catatan** | catatan tempel berwarna, cari, pin, filter, tambah/edit/hapus |
| **Absensi** | ringkasan H/S/I/A, grid 2 kolom siswa, tap status |
| **Nilai** | tabel Tugas/UTS/UAS, NA otomatis 40/30/30, ringkasan rata-rata/tertinggi/< KKM |
| **Jadwal** | per hari, penanda "sekarang" |
| **Kalender** | grid bulan + agenda per hari (ujian/rapat/kegiatan/libur/admin) |
| **Kalkulator** | 3 mode: Hitung (keypad + keyboard), Nilai (bobot 40/30/30 + predikat), Konversi (100 → 4) |
| **Binder** | 7 dokumen per kategori, checklist kelengkapan, tombol unggah |
| **Kelas** | 4 kelas, kehadiran %, rata-rata NA, detail + lompat ke layar lain |
| **Profil** | identitas guru, XP, 8 lencana, pengaturan (inspeksi, ekspor JSON, reset) |

## Design token

| Token | Hex | Pemakaian |
|---|---|---|
| Ink | `#0E0D1A` | outline & bayangan keras |
| Night / Navy | `#15142A` / `#1D1C36` | latar layar & kartu |
| Panel / Panel 2 | `#272647` / `#33325A` | kartu, tombol sekunder |
| Cream | `#F8E9C8` | teks utama |
| Amber | `#F2AB3F` | aksi utama, XP, jam |
| Coral | `#EF6A5B` | alfa, hapus |
| Teal / Mint | `#4FA49A` / `#9FD8B0` | hadir, simpan, display jam digital |
| Sky | `#77B8D9` | informasi, sakit |
| Plum | `#A58ECB` | rapat, kategori lain |

Aturan bentuk: sudut **0 px**, outline **2–3 px** gelap, bayangan **solid 3–4 px tanpa
blur**, grid 8 DP, padding layar 16 DP, tipografi **Pixelify Sans** (UI),
**Silkscreen** (label & jam), **Press Start 2P** (judul).

## Langkah berikutnya (Android)

1. Skeleton Compose (`minSdk 26`), `android:screenOrientation="sensorLandscape"`, tema dari token di atas.
2. `HomeScreen`: `BoxWithConstraints` + `Image(painterResource(R.drawable.scene32), filterQuality = FilterQuality.None)` dan 11 hotspot dari tabel koordinat.
3. Room: `Kelas`, `Siswa`, `Jurnal`, `Catatan`, `Nilai`, `Absensi`, `Agenda`, `Dokumen`.
4. Widget/komponen jam digital memakai font Silkscreen + `rememberInfiniteTransition` untuk titik berkedip.
5. Ekspor CSV/JSON untuk administrasi, lalu APK rilis.
