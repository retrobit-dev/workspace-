"""Simulasi overlay Compose dengan PIL — untuk memeriksa tata letak tanpa emulator.

Meniru persis matematika Launcher.kt:
  · planeOf / withPlane  -> affine poly-to-poly (PIL Image.transform)
  · wallQuad(rect)       -> quad dinding dengan kemiringan 0,42
  · kalender, papan tulis, ikon bingkai, rel baterai, persentase di dinding

  python3 tools/sim_overlay.py  ->  /tmp/sim_day.png, /tmp/sim_night.png
"""
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "android/app/src/main/res"
ANDROID = ROOT / "android/app/src/main/java/dev/retrobit/ruangkelas"
FONT = str(RES / "font/silkscreen_bold.ttf")

INK = (14, 13, 26, 255)
CREAM2 = (227, 205, 166, 255)
SCREEN = (215, 247, 205, 255)
BATT_LOW = (239, 106, 91, 255)


def parse():
    src = (ANDROID / "Data.kt").read_text()

    def rect(name):
        m = re.search(rf"val {name} = intArrayOf\(([^)]*)\)", src)
        return [int(v) for v in m.group(1).split(",")]

    def quads(name):
        start = src.index(f"val {name} = listOf(")
        block = src[start:src.index("\n)", start)]
        return [[int(v) for v in m.split(",")] for m in re.findall(r"intArrayOf\(([-\d,\s]+)\)", block)]

    return dict(
        board=rect("BOARD_QUAD"),
        curtains=quads("CURTAIN_RECTS"),
        leaf=rect("LEAF_RECT"),
    )


def wall_quad(rect):
    x, y, w, h = rect
    dx = round(w * 0.42)
    return (x, y, x + w, y + dx, x, y + h)


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def affine_from_quad(quad, w, h):
    """PIL AFFINE: (x_dst, y_dst) -> (a*x + b*y + c, d*x + e*y + f).

    Kotak lapisan w x h direntangkan tepat ke jajar genjang (tl, tr, bl), sama
    seperti Matrix.setPolyToPoly di Launcher.kt.
    """
    ax, ay, bx, by, cx, cy = quad[:6]
    ux, uy = (bx - ax) / max(1, w), (by - ay) / max(1, w)
    vx, vy = (cx - ax) / max(1, h), (cy - ay) / max(1, h)
    det = ux * vy - uy * vx
    ia = vy / det
    ib = -vx / det
    ic = (vx * ay - vy * ax) / det
    id_ = -uy / det
    ie = ux / det
    if_ = (uy * ax - ux * ay) / det
    return (ia, ib, ic, id_, ie, if_)


def plane_layer(size, quad, painter):
    """Buat lapisan datar selebar/tinggi bidang, lalu dipetakan ke quad."""
    ax, ay, bx, by, cx, cy = quad
    w = max(1, int(round(math.dist((ax, ay), (bx, by)))))
    h = max(1, int(round(math.dist((ax, ay), (cx, cy)))))
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    painter(layer, w, h)
    out = layer.transform((1600, 1000), Image.AFFINE, affine_from_quad(quad, w, h), resample=Image.BICUBIC)
    return out, (ax, ay)


def text(draw, xy, s, font, fill, anchor="ms"):
    """anchor "ms" = tengah horizontal, dasar huruf di y — sama seperti drawText."""
    draw.text(xy, s, font=font, fill=fill, anchor=anchor)


def paint_board(layer, w, h):
    d = ImageDraw.Draw(layer)
    title = "JADWAL MENGAJAR"
    fs = max(6, int(min(h * 0.115, w * 0.80 / 9.3)))
    f = ImageFont.truetype(FONT, fs)
    while d.textlength(title, font=f) > w * 0.80 and fs > 4:
        fs -= 1
        f = ImageFont.truetype(FONT, fs)
    text(d, (w / 2, h * 0.16), title, f, (24, 20, 48, 255))
    d.line([w * 0.18, h * 0.215, w * 0.82, h * 0.215], fill=(24, 20, 48, 90), width=2)


def quad6(q):
    """Data.kt kadang menyimpan 4 titik; bidang butuh kiri-atas, kanan-atas, kiri-bawah."""
    return tuple(q[:6])


def draw_plane(img, quad, layer_maker):
    """Tempel lapisan datar ke quad memakai affine output->input."""
    ax, ay, bx, by, cx, cy = quad6(quad)
    w = max(1, int(round(math.dist((ax, ay), (bx, by)))))
    h = max(1, int(round(math.dist((ax, ay), (cx, cy)))))
    layer = layer_maker(w, h)
    warped = layer.transform(img.size, Image.AFFINE, affine_from_quad(quad, w, h), resample=Image.BICUBIC)
    img.alpha_composite(warped)


def build(mode, hour, battery=76, second=32):
    data = parse()
    src = Image.open(RES / f"drawable-nodpi/scene_{mode}.png").convert("RGBA")




    draw_plane(src, data["board"], lambda w, h: _layer(w, h, lambda l: paint_board(l, w, h)))

    d = ImageDraw.Draw(src)
    # selaput suasana, sama dengan ambienceOf() di Launcher.kt
    if hour in range(5, 9):
        tint = (255, 217, 160)
        alpha = 0.10
    elif hour in range(9, 16):
        tint = (255, 246, 226)
        alpha = 0.16
    elif hour in range(16, 19):
        tint = (255, 154, 92)
        alpha = 0.12
    elif hour in range(19, 23):
        tint = (43, 63, 143)
        alpha = 0.14
    else:
        tint = (6, 10, 34)
        alpha = 0.34
    veil = Image.new("RGBA", src.size, tint + (int(alpha * 255),))
    src = Image.alpha_composite(src, veil)
    return src


def _layer(w, h, painter):
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    painter(layer)
    return layer


if __name__ == "__main__":
    for mode in ("day", "night"):
        im = build(mode, 11 if mode == "day" else 21)
        out = f"/tmp/sim_{mode}.png"
        im.convert("RGB").save(out)
        print(out)
