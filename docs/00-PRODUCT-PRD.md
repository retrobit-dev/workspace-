# 00 — PRODUCT PRD (Isometric Launcher)

Custom launcher Android untuk guru matematika SMA, berbentuk **ruang belajar
dalam rumah** bergaya game simulasi — bukan launcher dgn dock/wallpaper/top bar.

## Perangkat target
- HUAWEI MatePad Mini, HarmonyOS 4.x, **landscape**, 2560×1600 (16:10).
- **Prototipe interaktif: Vite + React + TypeScript + Canvas.**
- Android/Kotlin + APK via GitHub Actions = **tahap lanjutan** (jangan
  diklaim selesai sebelum diimplementasikan).

## Prinsip
- Pengguna menyusun ruangan & objeknya sendiri lewat **editor sandbox**.
- Objek kelak dipetakan ke aplikasi/informasi: materi matematika, jadwal,
  komputer, kalkulator, absensi, jam, kalender, cuaca, baterai.
- Gaya visual: ilustrasi kartun-vektor halus, palet pastel (kayu honey
  oranye-cokelat, dinding biru-abu, lantai lavender), gradien lembut,
  ambient occlusion, suasana malam biru nyaman; tanpa outline/dithering/
  noise/low-poly. Latar hitam OLED di luar ruangan dipertahankan.
- Ruang kerja menyimpan acuan gaya di `style/` (`gaya.md`, `ref-day.png`,
  `ref-night.png`) dari versi sebelumnya (0.16.43 @ git `9c3f59e`).

## Milestone
| M | Isi | Status |
|---|---|---|
| M1 | Fondasi repo, docs kontrak, scaffold prototipe, renderer ruangan kosong + balok, LIHAT/EDIT dasar, scene.json simpan/muat/impor/ekspor, undo/redo, kamera geser+pinch | **dikerjakan** |
| M2 | Grup/subgrup, katalog kaya, animasi objek, kunci/LATAR penuh | belum |
| M3 | Meja (laci geser + pintu ayun), lampu berdiri (pendar malam) | belum |
| M4 | Jendela → pendekatan kamera (scene 3D sementara) + cuaca per jam perangkat | belum |
| M5 | Android/Kotlin + APK via CI | belum |

Setiap milestone diakhiri: build hijau + preview + laporan jujur
(yang diuji / yang belum diuji / pekerjaan lanjutan).
