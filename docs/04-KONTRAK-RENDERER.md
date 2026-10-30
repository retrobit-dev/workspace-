# 04 — KONTRAK RENDERER & TAMPILAN

- Renderer **membaca scene yang sama** tanpa mengubah data tersimpan.
- Pipeline transform: raster kanvas `568×355 × PIKSEL(2) = 1136×710`;
  tampilan CSS = raster × `DENS(0,8875)` ≈ 1008×630, difit ke viewport
  (letterbox), lalu kamera (geser + pinch-zoom) di atasnya.
- Urutan gambar: `underlay` → `main` → `overlay`; di dalam layer urut
  `(e+w)` menaik lalu `urutan`. Objek depan menerima ketukan pada area
  bertumpuk (hit-test poligon, urutan terbalik dari gambar).
- Gaya (kontrak visual): kayu honey oranye-cokelat, dinding biru-abu,
  lantai lavender dgn ubin isometrik, **gradien halus per muka**,
  ambient occlusion di bawah objek & sambungan dinding-lantai, tepi
  bersih anti-alias **tanpa outline**, tanpa dithering/noise/low-poly.
  Palet acuan di `style/gaya.md`; referensi `style/ref-day/night.png`.
- Latar **hitam OLED** di luar ruangan dipertahankan.
- Pantulan objek: **tidak ada**.
- Animasi tampilan tidak mengubah posisi/ukuran dasar di scene.
- Jangan klaim "vektor murni" atau "identik referensi" selama
  implementasi masih Canvas raster / hasil mendekati.

## Status implementasi M1 (jujur)
- ✅ lantai + 2 dinding (dari objek LATAR), balok 3 muka + AO, pilih/geser
  balok, kamera geser + pinch, undo/redo, simpan/muat/impor/ekspor.
- ❌ belum: rotasi, grup, animasi, langit/cuaca, jendela/3D, laci meja,
  pemetaan aplikasi.
