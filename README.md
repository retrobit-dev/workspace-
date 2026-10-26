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

## Langkah berikutnya (Android)

1. Skeleton Compose (`minSdk 26`), `android:screenOrientation="sensorLandscape"`, tema dari token di atas.
2. `HomeScreen`: `BoxWithConstraints` + `Image(painterResource(R.drawable.scene32), filterQuality = FilterQuality.None)` dan 11 hotspot dari tabel koordinat.
3. Room: `Kelas`, `Siswa`, `Jurnal`, `Catatan`, `Nilai`, `Absensi`, `Agenda`, `Dokumen`.
4. Widget/komponen jam digital memakai font Silkscreen + `rememberInfiniteTransition` untuk titik berkedip.
5. Ekspor CSV/JSON untuk administrasi, lalu APK rilis.
