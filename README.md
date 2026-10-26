# Ruang Kelas — aplikasi Android pixel-art untuk guru

Aplikasi **native Android** (Kotlin + Jetpack Compose) berisi alat administrasi mengajar
guru, dibungkus tampilan workspace pixel art gelap bergaya game retro.
Target: **HUAWEI MatePad Mini 8,8"**, **landscape**, mode gelap saja.

**Status: v0.6.0 — APK debug sudah dibangun oleh GitHub Actions dan diuji jalan di emulator tablet.**

| | |
|---|---|
| APK siap pasang | [`dist/RuangKelas-debug.apk`](dist/RuangKelas-debug.apk) (≈ 8,8 MB, debug-signed) |
| Build otomatis | [`.github/workflows/android.yml`](.github/workflows/android.yml) → job `assembleDebug` |
| Bukti jalan | [`docs/screens/`](docs/screens) — 12 layar + 3 aksi, diambil dari emulator tablet 2560 × 1600 |
| Prototipe HTML | [`preview-ruang-kelas.html`](preview-ruang-kelas.html) (acuan desain, bukan deliverable) |

## Cara memasang di tablet

1. Unduh `dist/RuangKelas-debug.apk` (atau artifact `RuangKelas-debug-apk` / release
   [`v0.6.0-android`](https://github.com/retrobit-dev/workspace-/releases/tag/v0.6.0-android)).
2. Buka file APK di tablet → izinkan "instal dari sumber ini" saat diminta.
3. Buka **Ruang Kelas**. Aplikasi otomatis landscape penuh (system bar disembunyikan).

Debug build ditandatangani dengan debug key, jadi bisa dipasang langsung tanpa Play Store.
Data tersimpan lokal di `filesDir/ruangkelas.json` (tanpa internet, tanpa akun).

## Yang sudah jalan di perangkat

Semua ini diverifikasi lewat emulator, bukan asumsi:

- 12 layar (Beranda, Jurnal, Catatan, Absensi, Nilai, Jadwal, Kalender, Kalkulator,
  Binder, Kelas, Profil, Design Kit) bisa dibuka tanpa crash.
- Dialog **Catat Pertemuan** terbuka, isi materi bisa diketik, dan tombol **Simpan** bekerja.
- **Kalkulator** menghitung ekspresi campuran (`78+88×2`), termasuk operator menggantung
  dan pembagian nol tanpa crash.
- **Absensi** mengubah status H/S/I/A per siswa dan langsung memperbarui hitungan.
- Jam digital berjalan real-time di app bar, panel konteks, dan display di scene.
- Scanline CRT, toggle art 32/64 px, dan reset data tersedia di Pengaturan.

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

## Tampilan real (seperti aplikasi terpasang)

Preview sekarang tampil **seperti aplikasi sungguhan**, bukan mockup:
tanpa bezel tablet, tanpa sidebar kontrol, tanpa baris tab — layar penuh 100% viewport,
lengkap dengan app bar sendiri (logo, nama layar aktif, jam, tanggal, sinyal, baterai).

- Layout mengikuti ukuran jendela: rail kiri 88 dp · konten · panel kanan 312 dp.
- Beranda berupa **dashboard dua kolom**: scene di kiri (dengan 11 hotspot), tool &
  agenda di kanan.
- Kontrol desain tidak hilang, hanya dipindah ke dalam aplikasi: tombol ⚙ di
  kanan app bar membuka **Pengaturan Tampilan** (art 32/64 px, inspeksi area tap,
  scanline, animasi, layar penuh, reset data).
- Design Kit kini menjadi layar biasa di dalam aplikasi (dari Profil atau tautan di Beranda).
- Navigasi hanya lewat UI aplikasi: rail 11 tujuan, scene, kartu, dan tombol aksi.

## Preview satu file (paling praktis)

`preview-ruang-kelas.html` adalah **satu file HTML mandiri** — CSS, JS, 6 font,
2 scene, dan 11 tile sudah tertanam sebagai data URI. Bisa dikirim lewat WhatsApp/
Google Drive, lalu dibuka langsung di browser tablet tanpa internet dan tanpa server.

```bash
node build.js     # atau: npm run build
```

Membuka file itu, seluruh elemen bisa diketuk: 11 objek di scene, rail 11 tujuan,
kartu tool, kartu catatan, item jurnal (dialog detail + hapus), baris jadwal
(dialog + "Catat Jurnal" yang otomatis mengisi form), nama siswa (dialog siswa),
agenda di panel kanan (lompat ke tanggal), lencana, dan semua tombol aksi.
Setiap aksi punya umpan balik toast, dan data tersimpan di `localStorage`.

> Di server preview, file yang sama juga dilayani di `/preview-ruang-kelas.html`.
> Ubah `prototype/` lalu jalankan `node build.js` lagi untuk memperbarui versi mandiri.

## Struktur

```
preview-ruang-kelas.html       # hasil build: satu file, semua aset tertanam
build.js                       # penggabung prototype/ → preview satu file
prototype/
├── index.html                 # 11 layar, rail, panel konteks, Design Kit
├── styles.css                 # token desain + komponen dasar
├── tablet.css                 # tata letak landscape (rail + konten + panel)
├── real.css                   # mode aplikasi penuh (tanpa bezel/sidebar/tab)
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

- **Jam digital** di atas meja → **Jadwal** — pada grid 32 px digit "88:88" hilang, jadi
  jamnya diganti **display LED hidup** (teks Silkscreen + glow) yang menimpa badan jam di scene,
  selalu menampilkan waktu nyata dan tetap bisa diketuk
- **Kalender meja** tepat di sebelah kanan jam → **Kalender**
- **Laci meja** di bawah meja → **Binder materi**
- Laptop → Jurnal · papan tulis → Absensi · papan pin → Catatan · buku → Nilai
- Pen cup / tempat pensil → Kalkulator · jendela → Kelas · kursi → Profil · rak → Binder

Koordinat tap (satuan grid 51 × 32, `x · y · w × h`):

| Objek | Tujuan | x | y | w | h |
|---|---|---|---|---|---|
| laptop | Jurnal | 12 | 15 | 10 | 6 |
| buku | Nilai | 5 | 19 | 6 | 4 |
| papan tulis | Absensi | 25 | 6 | 15 | 9 |
| papan pin | Catatan | 40 | 6 | 8 | 9 |
| jam digital (LED hidup) | Jadwal | 23 | 15 | 8 | 6 |
| kalender meja | Kalender | 31 | 15 | 6 | 6 |
| laci | Binder | 27 | 24 | 10 | 8 |
| pen cup | Kalkulator | 20 | 15 | 3 | 6 |
| jendela | Kelas | 1 | 1 | 8 | 20 |
| kursi | Profil | 43 | 14 | 8 | 10 |
| rak binder | Binder | 10 | 6 | 11 | 4 |

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

## Build sendiri

Lokal (butuh JDK 17 + Android SDK 35):

```bash
cd android
gradle :app:assembleDebug      # hasil: app/build/outputs/apk/debug/app-debug.apk
```

Lewat GitHub (cara yang dipakai proyek ini):

- **Build APK Ruang Kelas** — dipicu setiap perubahan di `android/`; menjalankan
  `gradle :app:assembleDebug`, menyimpan APK ke `dist/`, mengunggah artifact, dan
  memperbarui release `v0.6.0-android`. Kalau kompilasi gagal, ringkasan error
  ditempel otomatis sebagai komentar commit.
- **Preview APK di Emulator** — dipicu lewat `.ci/preview.trigger`; membuat AVD tablet
  2560 × 1600, memasang APK, memotret 12 layar, menjalankan uji interaktif
  (`scripts/preview_actions.py`), lalu menyimpan screenshot ke `docs/screens/`.

## Isi repo Android

```
android/
├── app/build.gradle.kts            # compileSdk 35 · minSdk 26 · Compose BOM 2024.10.01
└── app/src/main/
    ├── AndroidManifest.xml         # sensorLandscape, satu activity
    ├── java/dev/retrobit/ruangkelas/
    │   ├── App.kt                  # MainActivity, root layout, rail, app bar, panel konteks, scene
    │   ├── Data.kt                 # model, data 96 siswa, seed, simpan/muat JSON, aksi
    │   ├── ScreensA.kt             # Jurnal, Catatan, Absensi, Nilai
    │   ├── ScreensB.kt             # Jadwal, Kalender, Kalkulator, Binder, Kelas, Profil, Design Kit
    │   ├── Theme.kt                # token warna + 3 font pixel
    │   └── Ui.kt                   # glif 8×8, panel, tombol, chip, field, dialog, jam
    └── res/                        # font OFL, scene 51×32 & 102×64, ikon adaptif
```
