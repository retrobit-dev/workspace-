# 02 — ARSITEKTUR 2.5D & PENGEMBANGAN 3D

## Sekarang
- Tampilan utama **Canvas 2D** dengan proyeksi isometrik (painter's algorithm
  per layer: underlay → main → overlay; di dalam layer urut `(e+w)`, lalu
  `urutan`).
- Objek = data murni di `scene.json`; renderer hanya membaca, tidak mengubah.

## Arah 3D (disetujui pengguna, BELUM selesai)
- Transisi jendela memakai **scene 3D sementara** (pendekatan kamera, langit,
  informasi cuaca).
- Pengembangan menuju **scene 3D permanen** di mode LIHAT dan EDIT —
  diimplementasikan bertahap hanya sesuai permintaan, **tanpa menghilangkan
  editor**.
- Pergerakan kamera ke jendela = satu gerakan utuh: zoom dgn jendela & sebagian
  dinding terlihat → putar memusat jendela → maju sampai bingkai keluar layar.
- Bebas flicker, tepi hitam, ruang kosong, gerakan patah-patah.
- Langit & elemen cuaca **selalu terpotong bidang kaca**, termasuk saat kamera
  bergerak / perspektif.

## Aturan konsistensi renderer
- Urutan gambar, layer, rotasi, bentuk, opasitas, hit-testing konsisten;
  objek paling depan menerima ketukan pada area bertumpuk.
- Laci & pintu meja: koreksi panel samping + urutan visual laci saat terbuka.
- **Tanpa pantulan objek.** Tanpa outline, dithering, noise, bidang low-poly.
- Jangan klaim "vektor murni" selama renderer masih Canvas raster.
