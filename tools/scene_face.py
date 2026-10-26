"""Siapkan gambar scene untuk launcher.

Ada dua versi suasana dari satu gambar sumber:

* ``night`` — malam: warna digrade pekat, langit di luar jendela malam berbintang.
* ``day``   — siang: gambar dicerahkan, kaca jendela dicat langit siang
  (biru muda + matahari + awan), jadi ruangan tidak lagi terlihat malam
  padahal jam perangkat menunjukkan siang.

Selain itu, kertas kalender tebal dicari batasnya untuk keperluan animasi lipat.

Dipakai oleh ``tools/make_scene.py`` (aset Android) dan ``tools/make_preview.py``
(preview browser) supaya keduanya memakai gambar yang sama.
"""
import colorsys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art/workspace-iso-v7c.png"


# Kaca jendela: kiri-atas, kanan-atas, kiri-bawah (piksel gambar asli).
# Dipakai untuk mengecat langit siang dan untuk efek angin.
GLASS_QUAD = (1062, 112, 1200, 178, 1062, 332)

# ------------------------------------------------------------------ warna latar

BG_LUMA_FLOOR = 45    # hanya permukaan latar yang cukup terang ikut diturunkan
BG_BASE = 22.0        # lantai/dinding paling gelap setelah diturunkan
BG_SLOPE = 0.37       # seberapa banyak kecerahan latar dipadatkan
BG_SAT = 0.72         # latar dibuat kalem, tidak ungu menyala
BG_DARK_LUMA = 25     # di bawah ini latar digeser sedikit ke atas
BG_DARK_GAIN = 1.25
BG_DARK_ADD = 7.0

# Gambar dasar tetap versi malam (gradenya sudah enak dilihat). Untuk siang,
# kecerahan tidak dinaikkan lewat gamma (itu memunculkan bercak dithering di
# lantai) tetapi lewat "screen" dengan cahaya siang: bayangan ikut terangkat,
# kontras lokal tetap, dan noise tidak ikut diperbesar.
NIGHT = dict(gamma=1.45, sat=0.85)

# Cahaya siang: eksposur dinaikkan (bukan gamma — gamma memunculkan bercak
# dithering), lalu ruangan digrade dingin-kebiruan karena satu-satunya sumber
# cahaya adalah langit di luar jendela: pendar biru-putih di sekitar jendela dan
# pantulan cahaya jendela di lantai. Standing lamp menyala hanya di malam
# (night_lamp), jadi siang otomatis bebas lampu.
DAY_GAIN = 1.50
DAY_ADD = 0.05
DAY_WB = (0.98, 1.00, 1.05)
DAY_SAT = 1.06
DAY_BLOOM = dict(center=(1140, 230), radius=320, colour=(240, 246, 255), alpha=0.34)
DAY_FLOOR = dict(
    points=[(1010, 452), (1210, 552), (1120, 742), (918, 650)],
    colour=(244, 248, 255),
    alpha=0.17,
    blur=12,
)

# Standing lamp (kiri meja): nyala hanya saat malam — lukisan glow prosedural,
# sehingga siang otomatis tanpa lampu (tidak ada lamp_off lagi).
LAMP_CX, LAMP_CY = 628, 242         # pusat cahaya tudung
LAMP_SHADE_I = 24                   # tinggi interior tudung (y 223..246)
LAMP_BULB = (628, 252)              # pusat bohlam di bawah bibir tudung
HALO_BOX = (598, 100, 778, 462)     # dinding kena halo (kiri: papan tulis)
POOL = (632, 518, 100, 34)          # kolam cahaya lantai (cx, cy, rx, ry)


def grade(im, gamma, sat):
    """Atur terang & saturasi seluruh gambar."""
    lut = [min(255, int(round(255 * ((i / 255) ** gamma)))) for i in range(256)]
    return ImageEnhance.Color(im.point(lut * 3)).enhance(sat)


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


def restyle_background(im):
    """Samakan latar (dinding + lantai) dengan acuan: navy gelap, lantai kalem.

    Hanya piksel kebiruan yang terang (lantai, panel dinding) yang dipadatkan;
    objek hangat (kayu, lampu, kertas) tidak diusik, jadi tidak ada kotak-kotak
    bekas masking.
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
                _hh, s, _v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                if b > r and luma < BG_DARK_LUMA:
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


# --------------------------------------------------------------- jendela siang

SKY_TOP = (122, 178, 226)
SKY_BOTTOM = (186, 219, 240)
SUN = (255, 232, 158)
SUN_CORE = (255, 246, 205)
CLOUD = (250, 252, 255)
CLOUD_SHADE = (214, 230, 245)


def _in_quad(x, y, quad):
    """Uji titik di dalam jajar genjang (kiri-atas, kanan-atas, kiri-bawah)."""
    ax, ay, bx, by, cx, cy = quad
    ux, uy = bx - ax, by - ay
    vx, vy = cx - ax, cy - ay
    det = ux * vy - uy * vx
    if det == 0:
        return False
    s = ((x - ax) * vy - (y - ay) * vx) / det
    t = ((y - ay) * ux - (x - ax) * uy) / det
    return 0.0 <= s <= 1.0 and 0.0 <= t <= 1.0


def _sky_pixel(p):
    """Piksel langit di gambar: biru gelap malam atau bintang terang."""
    r, g, b = p
    dark_sky = b > 30 and b > r + 12 and r < 70
    star = r > 150 and g > 150 and b > 150
    return dark_sky or star


def paint_day_sky(im, mask_from=None):
    """Cat kaca jendela jadi langit siang: biru muda, matahari, dan awan.

    ``mask_from`` = gambar acuan untuk menentukan piksel langit. Dipakai karena
    setelah ruangan diterangkan, langit malam tidak lagi terdeteksi sebagai
    "gelap", sedangkan di acuan masih. Bingkai & kusen tetap tidak dicat.
    """
    quad = GLASS_QUAD
    ax, ay, bx, by, cx, cy = quad
    px = im.load()
    ref = (mask_from or im).load()
    w, h = im.size
    for y in range(max(0, ay - 4), min(h, cy + 8)):
        for x in range(max(0, ax - 4), min(w, bx + 8)):
            if not _in_quad(x, y, quad):
                continue
            if not _sky_pixel(ref[x, y]):
                continue  # bingkai & kusen tidak dicat
            # gradasi langit: makin ke bawah makin terang
            t = max(0.0, min(1.0, (y - ay) / max(1, cy - ay)))
            col = tuple(int(SKY_TOP[i] + (SKY_BOTTOM[i] - SKY_TOP[i]) * t) for i in range(3))
            px[x, y] = col

    d = ImageDraw.Draw(im)
    # matahari kecil dekat sudut kanan-atas kaca
    sx, sy = ax + (bx - ax) * 0.74, ay + (by - ay) * 0.74 + 22
    r = 9
    d.ellipse([sx - r - 4, sy - r - 4, sx + r + 4, sy + r + 4], fill=(255, 244, 205))
    d.ellipse([sx - r, sy - r, sx + r, sy + r], fill=SUN_CORE)
    d.ellipse([sx - r + 3, sy - r + 3, sx + r - 3, sy + r - 3], fill=SUN)

    # dua awan pipih bergaya piksel
    for ox, oy, sw in ((-90, 48, 34), (-40, 96, 24)):
        ccx, ccy = ax + (bx - ax) * 0.45 + ox, ay + oy
        d.rectangle([ccx, ccy, ccx + sw, ccy + 7], fill=CLOUD)
        d.rectangle([ccx + 7, ccy - 6, ccx + sw - 8, ccy], fill=CLOUD)
        d.rectangle([ccx, ccy + 5, ccx + sw, ccy + 9], fill=CLOUD_SHADE)
    return im


# --------------------------------------------------------------------- publik

def _in_box(x, y, box):
    return box[0] <= x < box[2] and box[1] <= y < box[3]


def _soft_in(x, y, box, feather=14):
    """1.0 di dalam kotak, meluruh mulus ke 0.0 di luar (feather piksel)."""
    dx = max(box[0] - x, x - box[2], 0)
    dy = max(box[1] - y, y - box[3], 0)
    if dx == 0 and dy == 0:
        return 1.0
    d = (dx * dx + dy * dy) ** 0.5
    return max(0.0, 1.0 - d / feather)


def night_lamp(im):
    """Nyalakan standing lamp untuk suasana malam.

    Base art sengaja netral (tudung krem, dinding polos) supaya siang tidak
    perlu "mematikan" apa pun. Fungsi ini melukis tiga hal:
    1. interior tudung + bohlam jadi hangat terang,
    2. halo hangat di dinding di sekitar lampu,
    3. kolam cahaya di lantai mengelilingi kaki lampu.
    """
    px = im.load()

    # 1) interior tudung & bohlam
    for i in range(LAMP_SHADE_I):
        y = 223 + i
        half = 7 + (15 + i) * 0.52 - 3
        for x in range(int(round(LAMP_CX - half)) + 1, int(round(LAMP_CX + half))):
            c = (252, 218, 122) if i < 6 else ((244, 178, 70) if i < 15 else (208, 132, 40))
            px[x, y] = c
    bx, by = LAMP_BULB
    for dx in range(-8, 9):
        for dy in range(0, 7):
            if dx * dx / 64.0 + dy * dy / 30.0 <= 1:
                px[bx + dx, by + dy] = (252, 218, 122) if dy < 3 else (244, 178, 70)

    # 2) halo dinding
    floor = lambda x: 402 + 0.42 * (x - 600)
    edge = lambda x: 300 + 0.19 * (x - 745)
    for y in range(HALO_BOX[1], HALO_BOX[3]):
        for x in range(HALO_BOX[0], HALO_BOX[2]):
            if x < 745 and y >= floor(x) - 2:
                continue          # sudah lantai
            if x >= 745 and y >= edge(x) - 1:
                continue          # sudah meja
            if 625 <= x <= 634 and y > 258:
                continue          # tiang
            r, g, b = px[x, y]
            if b <= r:
                continue          # bukan dinding navy (objek lain)
            d = ((x - LAMP_CX) * 1.15) ** 2 + (y - LAMP_CY) ** 2
            d = d ** 0.5
            w = max(0.0, 1.0 - d / 205.0) ** 1.55 * 0.8
            if w <= 0.02:
                continue
            # target hangat absolut: dinding dipetakan ke warna cahaya lampu
            px[x, y] = (int(r + (88 - r) * w), int(g + (66 - g) * w), int(b + (60 - b) * w))

    # 3) kolam cahaya lantai
    cx, cy, rx, ry = POOL
    for y in range(cy - ry - 6, cy + ry + 6):
        for x in range(cx - rx - 6, cx + rx + 6):
            n = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if n > 1.0:
                continue
            r, g, b = px[x, y]
            if r + g + b < 60:
                continue          # outline gelap tetap gelap
            w = (1.0 - n) ** 1.5 * 0.5
            px[x, y] = (int(r + (112 - r) * w), int(g + (88 - g) * w), int(b + (82 - b) * w))
    return im


def add_daylight(im):
    """Terangkan ruangan seperti siang tanpa memunculkan bercak.

    Tiga lapis:
    1. eksposur naik (gain + sedikit penambahan) dengan white balance hangat,
    2. pendar hangat lembut di sekitar jendela — matahari masuk,
    3. pantulan matahari berbentuk jajar genjang di lantai dekat jendela.
    """
    lut = []
    for c in range(3):
        for i in range(256):
            lut.append(max(0, min(255, int(round(((i / 255 * DAY_GAIN) + DAY_ADD) * DAY_WB[c] * 255)))))
    out = ImageEnhance.Color(im.point(lut)).enhance(DAY_SAT)

    # 2. pendar di sekitar jendela
    b = DAY_BLOOM
    grad = Image.radial_gradient("L").resize((b["radius"] * 2, b["radius"] * 2))
    mask = grad.point(lambda v: int(b["alpha"] * 255 * (1 - v / 255) ** 2 * 0.9))
    full = Image.new("L", out.size, 0)
    full.paste(mask, (int(b["center"][0] - b["radius"]), int(b["center"][1] - b["radius"])))
    out = Image.composite(Image.new("RGB", out.size, b["colour"]), out, full)

    # 3. pantulan matahari di lantai
    f = DAY_FLOOR
    glow = Image.new("RGBA", out.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).polygon(f["points"], fill=f["colour"] + (int(f["alpha"] * 255),))
    glow = glow.filter(ImageFilter.GaussianBlur(f["blur"]))
    return Image.alpha_composite(out.convert("RGBA"), glow).convert("RGB")


def load_scene(mode="night"):
    """Gambar scene siap pakai untuk suasana ``night`` atau ``day``.

    Siang memakai gambar dasar yang sama, tapi ruangan diterangkan (screen
    cahaya siang) dan kaca jendela dicat langit siang dengan matahari & awan,
    jadi ruangan tidak lagi terlihat malam saat jam perangkat menunjukkan siang.
    """
    raw = Image.open(ART).convert("RGB")
    im = restyle_background(grade(raw, NIGHT["gamma"], NIGHT["sat"]))
    if mode == "day":
        base = im
        im = add_daylight(im)
        im = paint_day_sky(im, mask_from=base)
    else:
        im = night_lamp(im)
    return im

