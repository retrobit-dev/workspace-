# Objek Mandiri — arsitektur aset & manifest (0.16.15)

Setiap objek di gambar workspace kini **aset RGBA mandiri** + **identitas di
manifest**. Menghapus/mengedit/memindah satu objek **tidak memengaruhi objek
lain** karena scene dirakit ulang berlapis:

```
art/objects/background_{day,night}.png   latar dasar (bekas objek di-repair inpaint)
+ art/objects/<ID>.png / <ID>-night.png  aset RGBA per objek (bayangan ikut aset)
= art/compose-{day,night}.png            hasil rakitan (terverifikasi identik piksel)
```

`tools/compose.py --check` membandingkan komposit dengan `art/rebuild-day.png`
/ `art/rebuild-base.png`: **0 piksel boleh berbeda di luar cincin tepi objek**
(piksel di dalam cincin = perpaduan tepi feather dengan latar inpaint, sudah
diverifikasi visual identik). Status terakhir: **LULUS day & night**.

## Daftar objek (19)

| ID | Nama | bbox (x,y,w,h) | Layer |
|----|------|----------------|-------|
| BINGKAI_1 | Bingkai dinding 1 (bawah) | 122,156,79,123 | 11 |
| BINGKAI_2 | Bingkai dinding 2 | 194,118,76,123 | 12 |
| BINGKAI_3 | Bingkai dinding 3 | 266,81,76,123 | 13 |
| BINGKAI_4 | Bingkai dinding 4 (atas) | 341,43,76,123 | 14 |
| BINGKAI_5 | Bingkai dinding 5 (kiri bawah) | 52,194,77,122 | 15 |
| BINGKAI_KANAN | Bingkai dinding kanan | 684,28,79,133 | 16 |
| CATATAN_1 | Catatan papan pin 1 (tengah bawah) | 112,349,52,82 | 31 |
| CATATAN_2 | Catatan papan pin 2 (atas) | 169,283,52,78 | 32 |
| CATATAN_3 | Catatan papan pin 3 (pin kecil) | 126,314,33,44 | 33 |
| CATATAN_4 | Catatan papan pin 4 (kanan) | 158,352,41,57 | 34 |
| CATATAN_5 | Catatan papan pin 5 (kiri) | 82,335,47,74 | 35 |
| KOMPUTER | Laptop di meja | 768,321,182,127 | 41 |
| BUKU | Tumpukan buku di meja | 719,394,94,94 | 42 |
| KALKULATOR | Kalkulator di meja | 878,417,92,66 | 43 |
| GELAS_PENSIL | Gelas pensil | 901,371,85,88 | 44 |
| TEMPAT_SAMPAH | Tempat sampah | 4,635,103,130 | 46 |
| KERTAS_1 | Kertas kusut 1 | 96,704,49,47 | 47 |
| KERTAS_2 | Kertas kusut 2 | 66,732,44,40 | 48 |
| TANAMAN | Tanaman pot di sudut | 1034,384,246,322 | 50 |

> `bbox` pada manifest = jendela potong aset (inti + margin bayangan).
> `inti_bbox` = kotak inti objek tanpa margin. TANAMAN ber-flag
> `terpasang_di_latar=true` (akar pot menyatu latar — tidak ditempel ulang).

## Identitas per objek (manifest.json)

`id, nama, layer, bbox, ukuran_px{panjang,lebar,tinggi}, pivot, rotasi,
opacity, sumber_mode, warna_dominan (3 hex), terpasang_di_latar, fungsi,
aset{day,night}, inti_bbox, margin_bayangan, catatan`.

## Cara pakai

```bash
# ekstrak ulang semua aset dari art asli (setelah mengubah SPECS)
python3 tools/extract_objects.py

# rakit scene standar + verifikasi identik piksel (wajib hijau)
python3 tools/compose.py --check

#hapus objek (contoh: laptop + kalkulator) — objek lain tak tersentuh
python3 tools/compose.py --tanpa KOMPUTER,KALKULATOR --out art/scene-tanpa-laptop

# lanjutan: regen scene app dari hasil compose (make_scene.py) lalu pipeline biasa
```

### Resep operasi

- **Hapus** objek X: `compose.py --tanpa X --out art/compose` (tanpa `--check`,
  karena hasil memang sudah bukan komposisi penuh) → regen scene → pipeline.
- **Pindah** objek X: edit `bbox` X di SPECS (`tools/extract_objects.py`),
  jalankan `extract_objects.py` (aset & background ikut ter-update), lalu
  `compose.py --check`. Hanya X yang berubah — objek lain berdekat aset sama.
- **Edit** isi objek X: gambar ulang `art/objects/X.png` (dan `-night.png`)
  langsung, atau ubah art sumber lalu ekstrak ulang.
- **Tambah** objek baru: tambah entri SPECS (bbox hasil ukur) → ekstrak ulang.
- **Ganti urutan tumpuk**: ubah `layer` (makin besar makin depan).

## Metode ekstraksi (terbukti; jangan diubah sembarangan)

- Alfa = jarak warna (Chebyshev) ke **referensi latar dari 4 pojok bbox yang
  DIKLASTERKAN** (klaster ≥2 = latar; pojok yang menangkap objek dibuang);
  TAU=30; close 5×5, open 2×2; feather dilate 5×5 + Gaussian σ0.7;
  `force` = rect ber-alfa penuh; `tanpa` = rect yang dipaksa transparan.
- `margin` (kiri,atas,kanan,bawah) memperluas jendela potong agar **bayangan
  jatuh ikut aset** — penting agar tak ada bayangan hantu tertinggal di latar.
- Background di-repair per objek dengan `cv2.inpaint` NS r7 atas mask bentuk
  objek (alfa>2, dilate 5×5) dengan cincin sumber 40px. Rect penuh TIDAK
  dipakai (menghapus area latar di sekitar objek).

## Keterbatasan yang diketahui

- Bekas objek di background hasil inpaint berupa smear halus (natural tapi
  tidak detail). Saat objek dihapus via `--tanpa`, area bekasnya memperlihatkan
  smear ini. Perbaikan lanjutan (bila diminta): inpaint berbasis patch atau
  regen penuh scene latar.
- TANAMAN (dan elemen berakar latar) tidak bisa dipindah lewat compose; edit
  dilakukan langsung pada gambar dasar.
