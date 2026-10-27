"""Sumber gambar scene untuk launcher (era rebuild 0.16.0).

Sejak tampilan dibangun ulang dari awal, suasana siang/malam TIDAK lagi
diolah dari satu gambar dasar — masing-masing sudah digambar langsung:

  - ``art/rebuild-base.png`` : malam — lampu meja menyala, langit berbintang.
  - ``art/rebuild-day.png``  : siang — lampu mati, matahari & awan.

Dua-duanya 1280×800 (kelipatan grid 32). Fungsi ``load_scene`` tinggal
memilih file yang tepat; tidak ada grade/pet ulang piksel lagi.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART_NIGHT = ROOT / "art" / "rebuild-base.png"
ART_DAY = ROOT / "art" / "rebuild-day.png"


def load_scene(mode: str = "night") -> Image.Image:
    """Kembalikan gambar scene siap pakai untuk ``night`` atau ``day``."""
    src = ART_DAY if mode == "day" else ART_NIGHT
    return Image.open(src).convert("RGB")
