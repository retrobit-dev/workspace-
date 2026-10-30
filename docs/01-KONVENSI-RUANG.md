# 01 — KONVENSI RUANG

- Pandangan **isometrik 3/4 top-down**.
- Koordinat ruang **(e, w, z)**:
  - `e` — dari sudut belakang ke arah dinding timur/kanan (DT).
  - `w` — dari sudut belakang ke arah dinding barat/kiri (DB).
  - `z` — ketinggian dari lantai.
- **Metrik kanvas yang dipertahankan**: kanvas internal **568 × 355**
  (satuan kanvas), `PIKSEL = 2` (faktor raster → 1136×710 piksel kanvas),
  `DENS = 0,8875` (faktor tampilan CSS pada zoom ×1).
- Asal proyeksi (sudut belakang ruangan) di titik `(284, 56)` kanvas;
  kamera awal **×1 di (0, 0)**.
- **Jangan** mengubah koordinat/dimensi tersimpan hanya demi tampilan.
- Dimensi ruangan tidak dikunci permanen; scene lama yang valid tetap
  terbaca (renderer mengikuti data, bukan sebaliknya).
- Kamera: geser bebas; **zoom hanya pinch** (tanpa tombol zoom).
- Jendela berada di dinding kanan/DT; dinding kiri disebut DB.
- Satuan posisi & ukuran: 3 desimal; ukuran minimum 0,001.
