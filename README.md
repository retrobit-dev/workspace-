# Ruang Kelas — launcher workspace pixel-art

Launcher Android (Kotlin + Jetpack Compose) untuk **HUAWEI MatePad Mini 8,8"**:
satu layar berisi gambar workspace isometrik ukuran penuh. Tanpa sidebar, tanpa tab,
tanpa header, tanpa panel. Setiap objek di gambar bisa diketuk untuk membuka aplikasi
bawaannya — aplikasi itu dibuat menyusul, satu per satu.

**Status: v0.7.0 — launcher sudah jadi dan diuji di emulator tablet.**

| | |
|---|---|
| APK siap pasang | [`dist/RuangKelas-0.7.0-debug.apk`](dist/RuangKelas-0.7.0-debug.apk) |
| Build otomatis | [`.github/workflows/android.yml`](.github/workflows/android.yml) |
| Bukti jalan | [`docs/index.html`](docs/index.html) · [`docs/screens/`](docs/screens) |
| Prototipe HTML lama | [`preview-ruang-kelas.html`](preview-ruang-kelas.html) (acuan desain) |

![Launcher Ruang Kelas](docs/preview-launcher.png)

## Cara memasang

1. Unduh `dist/RuangKelas-0.7.0-debug.apk`, buka di tablet, izinkan "instal dari sumber ini".
2. Buka **Ruang Kelas** — launcher langsung tampil penuh, landscape, tanpa system bar.

## Objek yang bisa diketuk

Koordinat ketuk disimpan di `Data.kt` (`TILES`, satuan piksel gambar 1312 × 820):

| Objek | Aplikasi |
|---|---|
| Laptop | Jurnal |
| Tumpukan buku | Nilai |
| Papan tulis | Absensi |
| Papan pin | Buku Catatan |
| Jam digital | Jadwal — layarnya menampilkan jam nyata |
| Kalender meja | Kalender |
| Laci meja, rak buku | Binder Materi |
| Tempat pensil | Kalkulator |
| Jendela | Kelas |
| Kursi | Profil |

Ketukan saat ini memunculkan label "<NAMA> · SEGERA HADIR" selama 2 detik
(`Root` → `LauncherScreen(onOpen)`); begitu satu aplikasi dibuat, arahkan saja
`onOpen` ke layarnya.

## Struktur

```
art/workspace-iso-raw.png          sumber gambar isometrik 1312 × 816
tools/make_scene.py                memotong ke 16:10 → res/drawable-nodpi/scene.png
android/app/src/main/java/dev/retrobit/ruangkelas/
├── App.kt                         MainActivity (fullscreen) + Root
├── Launcher.kt                    gambar penuh layar + 11 area ketuk + jam LED + scanline
├── Data.kt                        model, data contoh, simpan/muat JSON, TILES
├── Theme.kt                       token warna + font pixel
└── Ui.kt                          komponen dasar untuk aplikasi yang akan datang
```

## Build

```bash
cd android && gradle :app:assembleDebug
```

GitHub Actions menjalankan hal yang sama, menyimpan APK ke `dist/`, dan memperbarui
release `v0.7.0-android`. Workflow kedua (`android-preview.yml`, dipicu
`.ci/preview.trigger`) memasang APK di emulator tablet 2560 × 1600, memotret launcher,
mengetuk beberapa objek, lalu menyimpan screenshot ke `docs/screens/`.
