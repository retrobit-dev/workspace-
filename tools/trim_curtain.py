"""Pangkas gorden kiri + rel pada art supaya kembali ke ukuran v6 (lebih kecil).

`art/workspace-iso-v7.png` -> `art/workspace-iso-v7b.png`

V7 : kain x 974..1032 (58 px) + rel mulai x 958 (finial).
V6 : kain 36 px (x 997..1032) + rel mulai x 992 (bola finial baru).

Supaya dinding tidak bergaris, tiap baris diisi SATU warna yang diambil dari
dinding nyata di baris yang sama (x 957..972): gradasi vertikal tetap utuh,
tidak ada blok/blur. Bola finial lama dipindah (bukan digambar ulang) supaya
gaya pikselnya sama.

    python3 tools/trim_curtain.py
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "art/workspace-iso-v7.png"
OUT = ROOT / "art/workspace-iso-v7b.png"

NEW_EDGE = 997                  # tepi kiri kain yang baru
FABRIC_RIGHT = 1032
BAND_TOP, BAND_BOTTOM = 22, 104  # rel + ring + finial kiri yang dibuang
BAND_X1 = 1000                   # ring ke-2 ikut dibuang (jangan sisa separuh)
BALL_BOX = (956, 30, 978, 53)
BALL_CENTER = (967, 41)
NEW_BALL = (993, 50)
OUTLINE = (29, 21, 29)


def wall_row(px, y: int):
    """Satu warna dinding untuk baris y (median, lewati bola rel / objek terang)."""
    xs = range(940, 953) if 26 <= y <= 58 else range(957, 973)
    vals = []
    for x in xs:
        r, g, b = px[x, y]
        if r > 120 or r - b > 45:      # terlalu terang / hangat (layar, meja)
            continue
        vals.append((r, g, b))
    if not vals:
        return None
    vals.sort(key=lambda c: c[0] + c[1] + c[2])
    return vals[len(vals) // 2]


def bright(px, x: int, y: int, limit: float = 112) -> bool:
    r, g, b = px[x, y]
    return (r + g + b) / 3 > limit


def fabric_top(px) -> int:
    """Baris pertama kain pada kolom 1002 (tepi baru)."""
    for y in range(80, 300):
        if bright(px, 1002, y):
            return y
    return 100


def fabric_bottom(px) -> int:
    """Baris terakhir kain pada kolom 1002."""
    for y in range(419, 200, -1):
        if bright(px, 1002, y):
            return y
    return 380


def main() -> None:
    im = Image.open(SRC).convert("RGB")
    px = im.load()
    out = im.copy()
    op = out.load()

    ball = im.crop(BALL_BOX)
    offset = (NEW_BALL[0] - BALL_CENTER[0], NEW_BALL[1] - BALL_CENTER[1])

    # ---- 1. rel + ring kiri + finial dibuang ----
    for y in range(BAND_TOP, BAND_BOTTOM + 1):
        c = wall_row(px, y)
        if c is None:
            continue
        for x in range(936, BAND_X1 + 1):
            op[x, y] = c

    # ---- 2. kain kiri dibuang sampai tepi baru ----
    for x in range(968, NEW_EDGE):
        for y in range(BAND_BOTTOM + 1, 420):
            c = wall_row(px, y)
            if c is None:
                c = op[x - 1, y - 1]
            w = 1.0 if x > 972 else max(0.0, (x - 967) / 5.0)
            r, g, b = op[x, y]
            op[x, y] = (int(r * (1 - w) + c[0] * w),
                        int(g * (1 - w) + c[1] * w),
                        int(b * (1 - w) + c[2] * w))

    # ---- 3. bola finial di ujung rel yang baru ----
    for yy in range(ball.height):
        for xx in range(ball.width):
            if (xx - (BALL_CENTER[0] - BALL_BOX[0])) ** 2 + (yy - (BALL_CENTER[1] - BALL_BOX[1])) ** 2 > 10 ** 2:
                continue
            op[BALL_BOX[0] + offset[0] + xx, BALL_BOX[1] + offset[1] + yy] = ball.getpixel((xx, yy))

    # ---- 4. tepi kiri kain + bayangan di dinding ----
    top = fabric_top(px)
    bottom = fabric_bottom(px) + 2
    for y in range(top + 2, bottom + 1):
        op[NEW_EDGE, y] = OUTLINE
        for x, k in ((NEW_EDGE - 1, 0.52), (NEW_EDGE - 2, 0.72)):
            r, g, b = op[x, y]
            op[x, y] = (int(r * k), int(g * k), int(b * k))

    out.save(OUT, optimize=True)
    print("->", OUT.relative_to(ROOT), f"kain {NEW_EDGE}..{FABRIC_RIGHT}, rel mulai {NEW_BALL[0]}, kain mulai y={top}")


if __name__ == "__main__":
    main()
