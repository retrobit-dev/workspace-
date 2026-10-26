# Ruang Kelas — Pixel Workspace

Aplikasi guru (native Android, rencana Kotlin + Jetpack Compose) dengan tampilan
**workspace bergaya 64-bit pixel art**: ruang kerja guru yang bisa diketuk, kartu
bergaya game, dan tipografi piksel.

Tahap saat ini: **UI/UX prototipe v0.2** — interaktif, bisa dibuka di browser, dan
sekaligus menjadi acuan token desain untuk implementasi Android.

## Menjalankan prototipe

```bash
npm start          # python3 -m http.server 4173 → http://localhost:4173
```

Atau buka `prototype/index.html` langsung (font & gambar sudah lokal, tanpa internet).

## Struktur

```
prototype/
├── index.html                    # 11 layar + Design Kit
├── styles.css                    # design token + komponen + mode terang
├── app.js                        # data contoh, render, interaksi, penyimpanan lokal
└── assets/
    ├── workspace-scene.png        # scene senja  1408×768
    ├── workspace-scene-day.png    # scene pagi   1376×768 (cermin, lihat catatan)
    ├── tiles/                     # 9 objek × 2 suasana = 18 aset PNG
    └── fonts/                     # Pixelify Sans, Silkscreen, Press Start 2P (OFL)
```

## 8 tool + 3 layar pendukung

| Layar | Isi | Interaksi |
|---|---|---|
| **Beranda** | scene 9 objek (hotspot presisi), bar XP, 3 statistik, 8 kartu tool, toolbar 4 tool | Ketuk objek: laptop→Jurnal · papan tulis→Absensi · papan pin→Catatan · buku→Nilai · rak→Binder · jendela→Kalender · jam→Jadwal · kursi→Profil · laci→Kelas |
| **Jurnal** | catatan pertemuan (tanggal, kelas, jam, materi, refleksi, kehadiran), filter kelas | Bottom sheet tambah catatan |
| **Buku Catatan** | catatan tempel berwarna: ide, refleksi, to-do | Cari, pin/sematkan, filter kategori, tambah/edit/hapus (dialog) |
| **Absensi** | ringkasan H/S/I/A, daftar siswa | Tap status per siswa, simpan per kelas |
| **Nilai** | Tugas/UTS/UAS, NA otomatis 40/30/30, rata-rata/tertinggi/< KKM | Input langsung, warna NA mengikuti KKM 75, tombol ke Kalkulator |
| **Jadwal** | per hari Senin–Jumat, penanda "sekarang" | Pindah hari, ekspor `.ics` (placeholder) |
| **Kalender** | grid bulan (Senin–Minggu), penanda agenda, agenda per hari | Ganti bulan, pilih tanggal, tambah/hapus agenda (ujian/rapat/kegiatan/libur/admin) |
| **Kalkulator** | 3 mode: **Hitung** (retro keypad, keyboard fisik), **Nilai** (bobot & NA + predikat), **Konversi** (100 → 4 + predikat) | Hitung ekspresi sendiri, ubah bobot, konversi instan |
| **Binder Materi** | 7 dokumen per kategori (Modul Ajar, LKPD, Bank Soal, Administrasi) | Filter kategori, checklist kelengkapan dokumen, tambah dokumen |
| **Kelas** | 4 kelas, kehadiran %, rata-rata NA | Detail kelas → lompat ke Absensi/Nilai/Jurnal |
| **Profil** | identitas guru, XP, 8 lencana (3 terbuka), pengaturan | Mode terang, inspeksi tile, ekspor JSON, reset data |
| **Design Kit** | palet, tipografi, komponen, **atlas 9 tile**, tabel koordinat tile, alur navigasi | Salin kode warna ke clipboard |

## Scene: hotspot presisi per objek

Scene dipecah menjadi **9 tile objek** yang koordinatnya diukur langsung pada gambar
(dalam piksel asli, lalu dikonversi ke persen agar responsif). Aktifkan
**Inspeksi tile** di panel kiri untuk melihat batas tap-area.

| Objek | Tujuan | Malam (x, y, w, h) | Pagi |
|---|---|---|---|
| laptop | Jurnal | 590, 328, 168, 174 | 408, 328, 186, 172 |
| papan tulis | Absensi | 516, 180, 390, 254 | 598, 82, 448, 316 |
| papan pin | Catatan | 934, 178, 314, 240 | 1066, 90, 312, 268 |
| buku | Nilai | 398, 418, 204, 112 | 272, 430, 156, 84 |
| rak | Binder | 428, 6, 560, 148 | 238, 44, 340, 292 |
| jendela | Kalender | 38, 20, 336, 312 | 0, 8, 232, 506 |
| jam | Jadwal | 836, 406, 90, 82 | 796, 428, 90, 84 |
| kursi | Profil | 980, 436, 268, 268 | 1110, 450, 266, 300 |
| laci | Kelas | 884, 520, 200, 190 | 748, 498, 152, 148 |

Aset per objek ada di `assets/tiles/` (mis. `night-laptop.png`) sehingga di Compose
tiap objek bisa dianimasikan sendiri (jam berdetak, layar berkedip, sticky note bergoyang).

> **Catatan aset:** ilustrasi "pagi" dibuat oleh model gambar terpisah, jadi susunannya
> tidak identik dengan "senja" — makanya tiap suasana punya tabel koordinat sendiri.
> Untuk produksi, scene pagi sebaiknya versi recolour dari scene yang sama (atau pakai
> trik Compose `scaleX(-1f)` pada scene senja) agar tata letak 100% konsisten.

## Design token

Palet gelap senja dengan aksen amber (mode terang memakai palet kertas krem):

| Token | Hex | Pemakaian |
|---|---|---|
| Ink | `#0E0D1A` | outline & bayangan keras |
| Night / Navy | `#15142A` / `#1D1C36` | latar layar & kartu |
| Panel / Panel 2 | `#272647` / `#33325A` | kartu, tombol sekunder |
| Cream | `#F8E9C8` | teks utama |
| Amber | `#F2AB3F` | aksi utama, progres XP |
| Coral | `#EF6A5B` | alfa, peringatan, tombol hapus |
| Teal / Mint | `#4FA49A` / `#9FD8B0` | hadir, simpan, predikat tinggi |
| Sky | `#77B8D9` | sakit, informasi |
| Plum | `#A58ECB` | kategori lain, rapat |

Aturan bentuk: sudut **0 px**, outline **2–3 px** gelap, bayangan **solid 3–4 px
tanpa blur**, grid 8 dp, padding layar 14 dp. Font: **Pixelify Sans** (UI),
**Silkscreen** (label/data), **Press Start 2P** (judul/logo).

## Langkah berikutnya (implementasi Android)

1. Skeleton proyek Compose (`minSdk 26`), tema warna dari tabel token di atas.
2. `HomeScreen`: `BoxWithConstraints` + `Image` scene, 9 hotspot memakai koordinat
   persen dari tabel di atas (identik dengan prototipe ini).
3. Room: entitas `Kelas`, `Siswa`, `Jurnal`, `Catatan`, `Nilai`, `Absensi`,
   `Agenda`, `Dokumen` (binder).
4. Fitur ekspor Excel/CSV + backup JSON, lalu APK rilis.
