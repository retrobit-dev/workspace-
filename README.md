# Ruang Kelas — launcher workspace pixel-art

Launcher Android (Kotlin + Jetpack Compose) untuk **HUAWEI MatePad Mini 8,8"**:
satu layar berisi gambar workspace isometrik ukuran penuh. Tanpa sidebar, tanpa tab,
tanpa header, tanpa panel. Setiap objek di gambar bisa diketuk untuk membuka aplikasi
bawaannya — aplikasi itu dibuat menyusul, satu per satu.

**Status: v0.11.0 — latar ruangan (dinding + lantai) disamakan dengan acuan pengguna:
navy gelap dan lantai batu-tuli yang kalem, tanpa kotak bekas masking. Objek tetap hangat
di bawah lampu. Jam dinding hidup, kalender mencetak tanggal hari ini.**

| | |
|---|---|
| APK siap pasang | [`dist/RuangKelas-0.11.0-debug.apk`](dist/RuangKelas-0.11.0-debug.apk) |
| Build otomatis | [`.github/workflows/android.yml`](.github/workflows/android.yml) |
| Bukti jalan | [`docs/index.html`](docs/index.html) · [`docs/screens/`](docs/screens) |
| Prototipe HTML lama | [`preview-ruang-kelas.html`](preview-ruang-kelas.html) (acuan desain) |

![Launcher Ruang Kelas](docs/preview-launcher.png)

## Cara memasang

1. Unduh `dist/RuangKelas-0.11.0-debug.apk`, buka di tablet, izinkan "instal dari sumber ini".
2. Buka **Ruang Kelas** — launcher langsung tampil penuh, landscape, tanpa system bar.

## Mode landscape

- Layar dikunci **landscape** di manifest (`sensorLandscape`) dan ditegaskan lagi di
  `MainActivity.requestedOrientation`, jadi tidak bisa berputar ke portrait.
- Gambar selalu mempertahankan rasio aslinya **16:10**. Kalau layar lebih lebar/pendek,
  sisa ruang dibiarkan hitam (letterbox) — art tidak pernah melar dan koordinat ketuk
  tetap presisi. Semua posisi dihitung dari satu satuan `unit` (ukuran 1 piksel gambar),
  dipakai bersama oleh gambar, 12 area ketuk, dan muka jam/kalender.
- Di browser, jendela portrait menampilkan ajakan "putar ke landscape"; kalau didukung,
  browser juga diminta mengunci orientasi saat layar disentuh.

## Objek yang bisa diketuk

Koordinat ketuk disimpan di `Data.kt` (`TILES`, satuan piksel gambar 1312 × 816):

| Objek | Aplikasi |
|---|---|
| Laptop | Jurnal |
| Tumpukan buku | Nilai |
| Papan tulis | Absensi |
| Papan pin (di atas rak buku) | Buku Catatan |
| Jam dinding analog | Jadwal — jarum jam, menit, dan detik mengikuti waktu nyata |
| Kalender meja | Kalender — mencetak hari, tanggal, dan bulan hari ini |
| Kalkulator (kanan laptop) | Kalkulator |
| Tempat pensil (kiri kalender) | Alat Tulis |
| Laci meja, rak buku | Binder Materi |
| Jendela | Kelas |
| Kursi | Profil |

Jam dinding dan kalender bukan gambar mati. Gambar aslinya dibersihkan dulu oleh
`tools/scene_face.py` (jarum jam dihapus, tanda jam digambar ulang di piksel asli),
lalu aplikasi menimpa muka jam dengan tiga jarum asli (jam, menit, detik merah)
dari `LocalTime.now()` dan
menulis `LocalDate.now()` pada kertas kalender — posisinya diambil dari `CLOCK_FACE`
dan `CALENDAR_FACE`.

Ketukan saat ini memunculkan label "<NAMA> · SEGERA HADIR" selama 2 detik
(`Root` → `LauncherScreen(onOpen)`); begitu satu aplikasi dibuat, arahkan saja
`onOpen` ke layarnya.

## Preview di browser

Launcher yang sama juga bisa dicoba di browser (tanpa memasang APK):

```bash
npm start          # http://localhost:4173  → menyajikan web/
npm run preview:build   # bangun ulang web/ dari art + TILES
```

`web/index.html` dibuat oleh `tools/make_preview.py` yang membaca `TILES`,
`CLOCK_FACE`, `CALENDAR_FACE`, dan `SCENE_W/H` langsung dari `Data.kt` — jadi area
ketuk di browser identik dengan di APK. Gambar penuh layar, tanpa sidebar/tab/header,
objek menyala saat ditekan, jam analog (SVG) berputar mengikuti waktu, kalender
menampilkan tanggal hari ini, dan label "SEGERA HADIR" muncul saat objek diketuk.

Cek cepat tanpa browser: `node tools/check_preview.mjs` (jsdom) memastikan
setiap hotspot berada tepat di atas objeknya dan muka jam/kalender terisi.

## Struktur

```
art/workspace-iso-v3.png          gambar isometrik terkini 1312 × 816 (ruangan lapang)
art/workspace-iso-v2.png          gambar sebelumnya (arsip)
art/workspace-iso-v1.png          gambar pertama (arsip)
tools/scene_face.py                grade warna latar, bersihkan muka jam, gambar tanda jam
tools/make_scene.py                → res/drawable-nodpi/scene.png (tanpa padding)
tools/make_preview.py              bangun web/index.html dari Data.kt (preview browser)
tools/check_preview.mjs            uji area ketuk preview (jsdom)
web/                               preview statis: index.html + scene.png
android/app/src/main/java/dev/retrobit/ruangkelas/
├── App.kt                         MainActivity (fullscreen) + Root
├── Launcher.kt                    gambar penuh layar + 12 area ketuk + jam analog + kalender hidup
├── Data.kt                        model, data contoh, simpan/muat JSON, TILES
├── Theme.kt                       token warna + font pixel
└── Ui.kt                          komponen dasar untuk aplikasi yang akan datang
```

## Build

```bash
cd android && gradle :app:assembleDebug
```

GitHub Actions menjalankan hal yang sama, menyimpan APK ke `dist/`, dan memperbarui
release `v0.11.0-android`. Workflow kedua (`android-preview.yml`, dipicu
`.ci/preview.trigger`) memasang APK di emulator tablet 2560 × 1600, memotret launcher,
mengetuk beberapa objek, lalu menyimpan screenshot ke `docs/screens/`.
