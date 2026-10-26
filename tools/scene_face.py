"""Siapkan gambar scene untuk launcher.

Gambar asli (`art/workspace-iso-v2.png`) masih memuat jarum jam yang tergambar
diam. Karena jam di aplikasi harus hidup, jarum itu dihapus di sini: semua piksel
gelap di dalam muka jam dicat krem, sementara tanda jam (garis tipis di tepi)
tetap dipertahankan. Hasilnya muka jam bersih yang siap ditimpa jarum asli.

Dipakai oleh `tools/make_scene.py` (aset Android) dan `tools/make_preview.py`
(preview browser) supaya keduanya memakai gambar yang sama.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art/workspace-iso-v2.png"

CLOCK_BOX = (752, 100, 108, 136)   # area pencarian muka jam (x, y, w, h)
INK = (14, 13, 26)                 # sama dengan warna Ink di Theme.kt
CAL_BOX = (856, 380, 90, 92)       # area pencarian kertas kalender


def _is_cream(p):
    r, g, b = p
    return r > 185 and g > 160 and b > 100 and b < 200 and (r - b) > 45


def cream_mask(im, box):
    """Baris demi baris: rentang x yang tertutup kertas krem."""
    x0, y0, w, h = box
    px = im.load()
    spans = {}
    for y in range(y0, y0 + h):
        xs = [x for x in range(x0, x0 + w) if _is_cream(px[x, y])]
        if xs:
            spans[y] = (min(xs), max(xs))
    return spans


def face_bbox(spans):
    ys = sorted(spans)
    x0 = min(s[0] for s in spans.values())
    x1 = max(s[1] for s in spans.values())
    return x0, ys[0], x1 - x0 + 1, ys[-1] - ys[0] + 1


def _avg_cream(im, spans):
    px = im.load()
    tot = [0, 0, 0]
    n = 0
    for y, (a, b) in spans.items():
        for x in range(a, b + 1):
            p = px[x, y]
            if _is_cream(p) and sum(p) > 560:
                tot[0] += p[0]; tot[1] += p[1]; tot[2] += p[2]; n += 1
    return tuple(c // n for c in tot)


def load_scene():
    """Gambar scene bersih (jarum jam dihapus)."""
    im = Image.open(ART).convert("RGB")
    spans = cream_mask(im, CLOCK_BOX)
    cream = _avg_cream(im, spans)
    px = im.load()
    for y, (a, b) in spans.items():
        for x in range(a, b + 1):
            px[x, y] = cream  # muka jam dikosongkan; jarum digambar aplikasi
    draw_ticks(im, face_bbox(spans))
    return im, cream


def draw_ticks(im, bbox):
    """Tanda jam 12 buah, digambar di piksel asli supaya tetap tajam di semua skala."""
    x, y, w, h = bbox
    cx, cy = x + w / 2, y + h / 2
    rx, ry = w / 2, h / 2
    d = ImageDraw.Draw(im)
    for i in range(12):
        a = math.radians(i * 30 - 90)
        major = i % 3 == 0
        outer = 0.84 if major else 0.80
        inner = 0.60 if major else 0.69
        p1 = (cx + math.cos(a) * rx * outer, cy + math.sin(a) * ry * outer)
        p2 = (cx + math.cos(a) * rx * inner, cy + math.sin(a) * ry * inner)
        d.line([p1, p2], fill=INK, width=2 if major else 1)


def clock_face(im):
    return face_bbox(cream_mask(im, CLOCK_BOX))


def calendar_face(im):
    return face_bbox(cream_mask(im, CAL_BOX))
