# Arsip Tampilan Era 0.15.12 ("ruangan klasik")

Snapshot terakhir dari desain workspace pixel-art lama sebelum dibangun ulang
dari awal. Tidak ada file besar yang diduplikat di folder ini — semuanya sudah
tersimpan di git dan di GitHub Releases pada commit yang tercantum di bawah.

## Penunjuk arsip

| Yang diarsipkan | Lokasi |
| --- | --- |
| Kondisi final seluruh repo | commit `f76d2ad` (art, scene, preview, kode hotspot) + commit docs menyusul |
| APK 0.15.12 | release `v0.15.12-android` → `RuangKelas-0.15.12-debug.apk` |
| Gambar dasar (lampu meja + kalender) | `art/workspace-iso-v7c.png` @ `f76d2ad` |
| Preview HTML + scene day/night | `web/index.html`, `web/scene_day.png`, `web/scene_night.png` |
| Scene yang dipakai aplikasi | `android/app/src/main/res/drawable-nodpi/scene_{day,night}.png` |
| Screenshot era ini | `docs/screens/` (refresh dari `tools/regen_docs.py`) |
| Referensi dari user | `referensi-user.jpg` (di folder ini) |
| Rilis-rilis era sebelumnya | tag `v0.15.1-android` … `v0.15.11-android` |

## Cara memulihkan tampilan ini nanti

```bash
git checkout f76d2ad -- art web android/app/src/main/res/drawable-nodpi \
  tools/scene_face.py tools/sim_overlay.py
python3 tools/make_scene.py && python3 tools/make_preview.py
```

(Koordinat hotspot 0.15.12 ada di `Data.kt` @ `f76d2ad`: `TILES` 18 zona,
`CALENDAR_QUAD`, `BOARD_QUAD`, `FRAME_QUADS`.)

## Konteks desain (untuk bangun ulang dari awal)

- Canvas 1312×816, grid piksel 32, gaya pixel-art isometrik malam + mode siang.
- Kriteria user era ini: lampu meja anglepoise menyala (glow + genangan
  cahaya), kalender meja hidup, dinding/lantai gelap original, tanpa standing
  lamp / jam dinding / bendera; landscape-only, dark theme, preview wajib benar
  di portrait via `fit()`.
- Poin yang rawan rusak saat mengubah art (pelajaran dari 0.15.8–0.15.11):
  tepi belakang meja miring (valid x≲940), sudut ruangan x≈918–996, koridor
  antara buku dan laptop (x≈806–856), rect laptop penuh a[276:508, 846:962].
