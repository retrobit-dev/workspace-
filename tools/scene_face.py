"""Siapkan gambar scene untuk launcher.

Gambar asli (`art/workspace-iso-v2.png`) masih memuat jarum jam yang tergambar
diam. Karena jam di aplikasi harus hidup, jarum itu dihapus di sini: semua piksel
gelap di dalam muka jam dicat krem, sementara tanda jam (garis tipis di tepi)
tetap dipertahankan. Hasilnya muka jam bersih yang siap ditimpa jarum asli.

Dipakai oleh `tools/make_scene.py` (aset Android) dan `tools/make_preview.py`
(preview browser) supaya keduanya memakai gambar yang sama.
"""
import colorsys
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art/workspace-iso-v3.png"

CLOCK_BOX = (880, 45, 120, 145)    # area pencarian muka jam (x, y, w, h)
INK = (14, 13, 26)                 # sama dengan warna Ink di Theme.kt
CAL_BOX = (958, 335, 120, 110)     # area pencarian kertas kalender


def _is_cream(p):
    """Kertas/krem di scene: terang, kekuningan, bukan jingga meja maupun langit."""
    r, g, b = p
    return r > 200 and g > 190 and 140 < b < 215 and 40 < (r - b) < 75


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


GRADE_GAMMA = 1.45   # malam yang lebih pekat, mendekati acuan pengguna
GRADE_SAT = 0.85     # warna sedikit diredam, tidak norak


def grade(im):
    """Redupkan & damai-kan warna supaya suasana malam terasa seperti acuan."""
    lut = [min(255, int(round(255 * ((i / 255) ** GRADE_GAMMA)))) for i in range(256)]
    return ImageEnhance.Color(im.point(lut * 3)).enhance(GRADE_SAT)


BG_LUMA_FLOOR = 45    # hanya permukaan latar yang cukup terang ikut diturunkan
BG_BASE = 22.0        # lantai/dinding paling gelap setelah diturunkan
BG_SLOPE = 0.37       # seberapa banyak kecerahan latar dipadatkan
BG_SAT = 0.72         # latar dibuat kalem, tidak ungu menyala
BG_DARK_LUMA = 25     # di bawah ini latar digeser sedikit ke atas
BG_DARK_GAIN = 1.25
BG_DARK_ADD = 7.0


def restyle_background(im):
    """Samakan latar (dinding + lantai) dengan acuan: biru-tuli gelap, kalem.

    Hanya piksel kebiruan yang terang (lantai, panel dinding) yang dipadatkan;
    objek hangat (kayu, lampu, kertas) dan bayangan gelap tidak diusik, jadi tidak
    ada kotak-kotak bekas masking.
    """
    px = im.load()
    w, h = im.size
    cache = {}
    for y in range(h):
        for x in range(w):
            p = px[x, y]
            c = cache.get(p)
            if c is None:
                r, g, b = p
                luma = 0.299 * r + 0.587 * g + 0.114 * b
                hh, s, _v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                if b > r and luma < BG_DARK_LUMA:
                    # dinding/bayangan yang nyaris hitam dinaikkan sedikit supaya
                    # panel dinding masih terbaca seperti di acuan
                    k = (luma * BG_DARK_GAIN + BG_DARK_ADD) / max(luma, 1.0)
                    c = (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)))
                elif b > r + 20 and s >= 0.30 and luma >= BG_LUMA_FLOOR:
                    nl = BG_BASE + BG_SLOPE * luma
                    k = nl / luma
                    nr, ng, nb = r * k, g * k, b * k
                    hh2, _, _ = colorsys.rgb_to_hsv(nr / 255, ng / 255, nb / 255)
                    rr, gg, bb = colorsys.hsv_to_rgb(hh2, s * BG_SAT, max(nr, ng, nb) / 255)
                    c = (int(rr * 255), int(gg * 255), int(bb * 255))
                else:
                    c = p
                cache[p] = c
            px[x, y] = c
    return im


def load_scene():
    """Gambar scene siap pakai: warna digrade, muka jam dikosongkan."""
    raw = Image.open(ART).convert("RGB")
    spans = cream_mask(raw, CLOCK_BOX)          # deteksi di gambar asli
    im = restyle_background(grade(raw))
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


def clock_face(_im=None):
    """Kotak muka jam pada gambar asli (piksel scene)."""
    if _im is not None and not isinstance(_im, Image.Image):
        _im = None
    return face_bbox(cream_mask(Image.open(ART).convert("RGB"), CLOCK_BOX))


def calendar_face(_im=None):
    """Kotak kertas kalender pada gambar asli (piksel scene)."""
    return face_bbox(cream_mask(Image.open(ART).convert("RGB"), CAL_BOX))
