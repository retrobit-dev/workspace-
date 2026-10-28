"""Sumber gambar scene untuk launcher (era rebuild 0.16.0).

Sejak tampilan dibangun ulang dari awal, suasana siang/malam TIDAK lagi
diolah dari satu gambar dasar — masing-masing sudah digambar langsung:

  - ``art/rebuild-base.png`` : malam — lampu meja menyala, langit berbintang.
  - ``art/rebuild-day.png``  : siang — lampu mati, matahari & awan.

Dua-duanya 1280×800 (kelipatan grid 32). Fungsi ``load_scene`` tinggal
memilih file yang tepat. Sejak 0.16.33 malam diberi grade deterministik:
interpolasi seam kolom x1001-1013 lalu gamma 0.72 pada strip bingkai kiri
(STRIP_BINGKAI) — lihat _regrade_strip_night.
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART_NIGHT = ROOT / "art" / "rebuild-base.png"
ART_DAY = ROOT / "art" / "rebuild-day.png"


# strip sisi bingkai kaca kiri (antara tirai & kaca): arsiran art asli
# terlalu gelap utk malam sehingga terbaca "objek hitam" di sisi tirai
# (laporan user 0.16.33). Regrade: naikkan midtone, pertahankan garis
# gelap pemisah — hanya di bbox ini, hanya mode night, deterministik.
STRIP_BINGKAI = (986, 105, 1022, 368)


def _regrade_strip_night(im: Image.Image) -> Image.Image:
    x0, y0, x1, y1 = STRIP_BINGKAI
    a = np.asarray(im.crop((x0, y0, x1, y1))).astype(float)
    # 1) hilangkan seam gelap internal (x_full 1001..1013): di art siang garis
    #    ini tidak ada -> bukan struktur; isi interpolasi horizontal antara
    #    wajah bingkai kiri (996..1000) dan kanan (1014..1018) per baris
    l0, l1 = 996 - x0, 1000 - x0
    r0, r1 = 1014 - x0, 1018 - x0
    s0, s1 = 1001 - x0, 1013 - x0
    for yy in range(a.shape[0]):
        L = a[yy, l0:l1 + 1].mean(axis=0)
        R = a[yy, r0:r1 + 1].mean(axis=0)
        for xx in range(s0, s1 + 1):
            t = (xx - s0) / float(s1 - s0)
            a[yy, xx] = L * (1.0 - t) + R * t
    # 2) gamma 0.72: midtone naik menyamai wajah bingkai lain
    a = 255.0 * np.power(a / 255.0, 0.72)
    im.paste(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)), (x0, y0))
    return im


def load_scene(mode: str = "night") -> Image.Image:
    """Kembalikan gambar scene siap pakai untuk ``night`` atau ``day``."""
    src = ART_DAY if mode == "day" else ART_NIGHT
    im = Image.open(src).convert("RGB")
    if mode == "night":
        im = _regrade_strip_night(im)
    return im
