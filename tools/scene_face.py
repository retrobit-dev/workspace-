"""Sumber gambar scene untuk launcher (era rebuild 0.16.0).

Sejak tampilan dibangun ulang dari awal, suasana siang/malam TIDAK lagi
diolah dari satu gambar dasar — masing-masing sudah digambar langsung:

  - ``art/rebuild-base.png`` : malam — lampu meja menyala, langit berbintang.
  - ``art/rebuild-day.png``  : siang — lampu mati, matahari & awan.

Dua-duanya 1280×800 (kelipatan grid 32). Fungsi ``load_scene`` tinggal
memilih file yang tepat; tidak ada grade/pet ulang piksel lagi.
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
    reg = im.crop((x0, y0, x1, y1))
    a = np.asarray(reg).astype(float)
    # gamma 0.72: midtone naik (arsiran 107-140 -> ~139-166, setara wajah
    # bingkai lain), struktur internal (rel, seam gelap) tetap terlihat
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
