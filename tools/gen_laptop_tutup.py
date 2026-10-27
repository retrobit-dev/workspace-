"""Aset state laptop launcher: TERTUTUP, KOSONG, TERBUKA, GLOW (day/night).

Laptop terbuka pada art asal dipakai sebagai state "terbuka"/redup; state
"tertutup" digambar dari quad alas aset (aset mandiri, TANPA menyentuh art
lain). Aset memakai crop diperluas CROP=(768,294)-(950,448) dan alpha
=siluet laptop (bukan persegi) supaya patch tak berkotak di scene.

"Meja-rapi" (background_*_rapi.png) = background tanpa laptop: ghost dihapus
dengan mask siluet, diisi fit polinomial 2D per kanal (dinding & meja) agar
gradasi lampu malam & perspektif meja tetap mengalir; garis tepi belakang
meja digambar ulang pada posisinya (terlihat di atas laptop tertutup).

  python3 tools/gen_laptop_tutup.py
"""
from __future__ import annotations

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = __file__.rsplit("/tools/", 1)[0] if "/tools/" in __file__ else "."
S = 4                        # supersample
CROP = (768, 294, 950, 448)  # crop diperluas (dinding bersih utk konteks fill)

MODES = {
    "day": dict(
        src=f"{ROOT}/art/objects/KOMPUTER.png",
        bg=f"{ROOT}/art/objects/background_day.png",
        rapi=f"{ROOT}/art/objects/background_day_rapi.png",
        ref=f"{ROOT}/art/rebuild-day.png",
        hinge=[(14, 114), (155, 68)],
        deck=[(20, 107), (151, 71), (146, 123), (93, 144)],
        mouse=(56, 38), mbody=(139, 135, 146), mtop=(176, 172, 183), mline=(96, 93, 104),
        base=(233, 230, 228), back=(203, 199, 203), right=(196, 192, 197),
        rim=(246, 244, 242), outline=(150, 146, 156), seam=(168, 164, 172),
        glow=(214, 231, 255), core_a=105, halo_a=55, pool_a=40,
        core_c=(115, 79), pool_c=(97, 143),
    ),
    "night": dict(
        src=f"{ROOT}/art/objects/KOMPUTER-night.png",
        bg=f"{ROOT}/art/objects/background_night.png",
        rapi=f"{ROOT}/art/objects/background_night_rapi.png",
        ref=f"{ROOT}/art/rebuild-base.png",
        hinge=[(14, 112), (162, 68)],
        deck=[(20, 105), (158, 71), (150, 123), (96, 144)],
        mouse=(56, 38), mbody=(97, 94, 106), mtop=(130, 126, 138), mline=(66, 63, 74),
        base=(216, 205, 209), back=(185, 175, 183), right=(175, 166, 176),
        rim=(233, 224, 227), outline=(122, 113, 126), seam=(140, 131, 142),
        glow=(214, 231, 255), core_a=200, halo_a=110, pool_a=85,
        core_c=(112, 74), pool_c=(97, 143),
    ),
}


def edge_y(x: float) -> float:
    """Tepi belakang meja: puncak sudut iso (793,290); kiri -0.5x+686,
    kanan 0.500x-104 (fit kolom bersih x816-840, konsisten s.d. x940)."""
    return (-0.50 * x + 686.0) if x < 793 else (0.500 * x - 104.0)


def laptop_mask(cfg) -> np.ndarray:
    """Mask siluet laptop dalam koordinat crop: quad lid (garis engsel s.d.
    atas ghost) ∪ poligon deck ∪ diff (bg != scene). Jadi alpha aset."""
    W, H = CROP[2] - CROP[0], CROP[3] - CROP[1]
    hy = cfg["hinge"]
    # ghost lid: puncak di (25,-2) [full (793,292)], tepi atas turun-ke-kanan ke
    # (172,57) [full (940,351)], bawah = garis engsel. Kanan dihentikan x<=170
    # (full 938) supaya tirai tak tersentuh.
    lid = [(30, 10), (170, 57), (172, hy[1][1] - 2), (hy[0][0], hy[0][1] + 6), (2, hy[0][1] - 92)]
    im = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(im)
    d.polygon(lid, fill=255)
    d.polygon([tuple(pt) for pt in cfg["deck"]], fill=255)
    m = np.asarray(im) > 0
    bg = np.asarray(Image.open(cfg["bg"]).convert("RGB")).astype(int)
    sc = np.asarray(Image.open(cfg["ref"]).convert("RGB")).astype(int)
    diff = np.abs(bg[CROP[1]:CROP[3], CROP[0]:CROP[2]] - sc[CROP[1]:CROP[3], CROP[0]:CROP[2]]).sum(2) > 12
    diff[:, 171:] = False          # zona tirai: ghost tak sampai sini
    m |= diff
    # siluet laptop TERBUKA (alpha aset) utk layar+bingkai yang mengintip;
    # hanya x>=814: kolom kiri memuat lengan lampu - jangan disentuh
    tb = np.asarray(Image.open("art/objects/KOMPUTER_TERBUKA.png"))[:, :, 3]
    m |= (tb > 10) & (np.arange(W)[None, :] >= 814 - CROP[0])
    # mahkota ghost: bingkai+layar di atas garis tutup (full 844..942 x
    # 296..354); warnanya identik dinding -> poligon geometris, bukan warna
    crown = Image.new("L", (W, H), 0)
    ImageDraw.Draw(crown).polygon(
        [(76, 2), (136, 2), (174, 60), (76, 60)], fill=255)
    m |= np.asarray(crown) > 0
    return m


def bg_rapi(mode: str, cfg: dict) -> None:
    """Buang ghost laptop dari background: mask = laptop_mask, isi = DIFUSI
    LAPLACE (tiap piksel mask -> rata-rata 4 tetangga, iteratif sampai halus).
    Batas = semua piksel bersih di sekeliling (buku, meja, dinding, tirai),
    jadi isi selalu meneruskan lingkungannya tanpa bidang/rect artifisial.
    Garis tepi belakang meja digambar ulang tipis pada posisinya."""
    bg = np.asarray(Image.open(cfg["bg"]).convert("RGB")).astype(float)
    H, W = bg.shape[:2]
    mask_full = np.zeros((H, W), bool)
    mask_full[CROP[1]:CROP[3], CROP[0]:CROP[2]] = laptop_mask(cfg)
    mask_full = cv2.dilate(mask_full.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0

    # bbox kerja = luas mask + margin
    ys, xs = np.where(mask_full)
    y0, y1 = max(0, ys.min() - 2), min(H, ys.max() + 3)
    x0, x1 = max(0, xs.min() - 2), min(W, xs.max() + 3)
    img = bg[y0:y1, x0:x1].copy()
    m = mask_full[y0:y1, x0:x1]
    mh, mw = m.shape

    # isi struktural: zona DINDING (di atas tepi belakang meja) diisi dari
    # piksel bersih teratas kolom yang sama; zona MEJA dari terbawah. Dua
    # domain tak tercampur -> dinding tetap dinding, meja tetap meja.
    ys_f = np.arange(mh)[:, None] * np.ones((1, mw))
    ey_row = np.array([edge_y(x + x0) for x in range(mw)])[None, :]
    wall_z = m & (ys_f < ey_row - 0.5)
    desk_z = m & (ys_f >= ey_row - 0.5)
    for x in range(mw):
        cw = np.where(wall_z[:, x])[0]
        if len(cw):
            yt = cw.min() - 1
            img[wall_z[:, x], x] = img[yt, x] if yt >= 0 else img[0, x]
        cd = np.where(desk_z[:, x])[0]
        if len(cd):
            yb = cd.max() + 1
            img[desk_z[:, x], x] = img[yb, x] if yb < mh else img[mh - 1, x]

    # penghalusan Jacobi per zona (domain terpisah, ~800 iter sampai halus)
    for _ in range(800):
        avg = (np.roll(img, 1, 0) + np.roll(img, -1, 0) +
               np.roll(img, 1, 1) + np.roll(img, -1, 1)) * 0.25
        img[wall_z] = avg[wall_z]
    for _ in range(800):
        avg = (np.roll(img, 1, 0) + np.roll(img, -1, 0) +
               np.roll(img, 1, 1) + np.roll(img, -1, 1)) * 0.25
        img[desk_z] = avg[desk_z]

    # haluskan interior (banding & seam melebur; boundary dijaga)
    interior = cv2.erode(m.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0
    blur = cv2.GaussianBlur(img, (0, 0), 2.2)
    img[interior] = blur[interior]

    # garis tepi belakang meja: digambar supersample 4x (anti-alias),
    # hanya di dalam mask; cukup menggelapkan isi yang baru
    lx0, lx1 = max(x0, 768), min(x1, 950)
    line_im = Image.new("L", (W * 4, H * 4), 0)
    ImageDraw.Draw(line_im).line(
        [(lx0 * 4, edge_y(lx0) * 4), (lx1 * 4, edge_y(lx1) * 4)],
        fill=255, width=4)
    line_a = np.asarray(line_im.resize((W, H), Image.LANCZOS)).astype(float) / 255.0
    la = np.zeros((mh, mw))
    la[y0 - y0:y1 - y0, x0 - x0:x1 - x0] = line_a[y0:y1, x0:x1]
    la *= m
    img *= (1.0 - (0.30 * la)[:, :, None])

    out = bg.copy()
    out[y0:y1, x0:x1][m] = img[m]
    Image.fromarray(out.astype(np.uint8)).save(cfg["rapi"])
    print(f"{mode}: rapi laplace {int(mask_full.sum())} px, bbox {x1-x0}x{y1-y0}")


def inset_edge(p, q, cpt, w):
    dx, dy = q[0] - p[0], q[1] - p[1]
    L = (dx * dx + dy * dy) ** 0.5 or 1.0
    nx, ny = -dy / L, dx / L
    if nx * (cpt[0] - p[0]) + ny * (cpt[1] - p[1]) < 0:
        nx, ny = -nx, -ny
    return [p[0], p[1], q[0], q[1], q[0] + nx * w, q[1] + ny * w, p[0] + nx * w, p[1] + ny * w]


def gen(mode: str, cfg: dict) -> None:
    """TERTUTUP: (rapi + mouse + tutup) alpha siluet. KOSONG: rapi + mouse.
    TERBUKA: scene crop + piksel aset (handoff identik dgn scene). GLOW: cahaya."""
    from scipy.ndimage import binary_fill_holes
    W, H = CROP[2] - CROP[0], CROP[3] - CROP[1]
    rapi = Image.open(cfg["rapi"]).convert("RGBA").crop(CROP)

    m = laptop_mask(cfg)
    alpha = (
        Image.fromarray((m * 255).astype(np.uint8))
        .filter(ImageFilter.MaxFilter(5))
        .filter(ImageFilter.GaussianBlur(1.4))
    )

    def with_mouse(img):
        ov = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
        d = ImageDraw.Draw(ov)
        mx, my = cfg["mouse"][0] * S, cfg["mouse"][1] * S
        d.ellipse([mx - 9 * S, my - 5 * S, mx + 9 * S, my + 7 * S], fill=cfg["mline"] + (255,))
        d.ellipse([mx - 8 * S, my - 5 * S, mx + 8 * S, my + 6 * S], fill=cfg["mbody"] + (255,))
        d.ellipse([mx - 5 * S, my - 4 * S, mx + 4 * S, my + 2 * S], fill=cfg["mtop"] + (255,))
        ov = ov.resize((W, H), Image.LANCZOS)
        img.alpha_composite(ov)
        return img

    base = with_mouse(rapi.copy())
    ov = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    L, T, R, B = [(pt[0] * S, pt[1] * S) for pt in cfg["deck"]]
    cpt = ((L[0] + R[0]) / 2, (L[1] + R[1]) / 2)
    quad = [L, T, R, B]
    d.polygon(quad, fill=cfg["base"] + (255,))
    d.polygon(inset_edge(L, T, cpt, 5.5 * S), fill=cfg["back"] + (255,))
    d.polygon(inset_edge(T, R, cpt, 4.5 * S), fill=cfg["right"] + (255,))
    d.polygon(inset_edge(R, B, cpt, 3.2 * S), fill=cfg["rim"] + (255,))
    d.polygon(inset_edge(B, L, cpt, 3.2 * S), fill=cfg["rim"] + (255,))
    v = inset_edge(L, T, cpt, 8 * S)
    d.line([v[6], v[7], v[4], v[5]], fill=cfg["seam"] + (120,), width=int(1.0 * S))
    bx, by = B[0] + (cpt[0] - B[0]) * 0.06, B[1] + (cpt[1] - B[1]) * 0.06
    r = 3.4 * S
    d.ellipse([bx - r, by - r * 0.55, bx + r, by + r * 0.55], fill=cfg["outline"] + (200,))
    d.polygon(quad, outline=cfg["outline"] + (255,), width=int(1.25 * S))
    ov = ov.resize((W, H), Image.LANCZOS)
    base.alpha_composite(ov)
    base.putalpha(alpha)
    base.save(f"{ROOT}/art/objects/KOMPUTER_TERTUTUP{'-night' if mode == 'night' else ''}.png")

    kos = with_mouse(rapi.copy())
    kos.putalpha(alpha)
    kos.save(f"{ROOT}/art/objects/KOMPUTER_KOSONG{'-night' if mode == 'night' else ''}.png")

    src = Image.open(cfg["src"]).convert("RGBA")
    ghost = Image.open(cfg["bg"]).convert("RGBA").crop(
        (CROP[0], CROP[1] + 27, CROP[2], CROP[1] + 27 + 127))
    sa = np.asarray(src).astype(int)
    ga = np.asarray(ghost).astype(int)
    dm2 = np.abs(sa[:, :, :3] - ga[:, :, :3]).sum(axis=2) > 12
    dm2 = binary_fill_holes(dm2)
    mk = (
        Image.fromarray((dm2 * 255).astype(np.uint8))
        .filter(ImageFilter.MaxFilter(3))
        .filter(ImageFilter.GaussianBlur(1.1))
    )
    terbuka = Image.open(cfg["ref"]).convert("RGBA").crop(CROP)
    terbuka.paste(src, (0, 27), mk)
    terbuka.putalpha(alpha)
    terbuka.save(f"{ROOT}/art/objects/KOMPUTER_TERBUKA{'-night' if mode == 'night' else ''}.png")

    g = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    dg = ImageDraw.Draw(g)
    col = cfg["glow"]
    for rx, ry, a in ((75, 60, cfg["halo_a"]), (46, 36, cfg["core_a"])):
        ccx, ccy = cfg["core_c"][0] * S, cfg["core_c"][1] * S
        dg.ellipse([ccx - rx * S, ccy - ry * S, ccx + rx * S, ccy + ry * S], fill=col + (a,))
    px, py = cfg["pool_c"][0] * S, cfg["pool_c"][1] * S
    dg.ellipse([px - 66 * S, py - 20 * S, px + 66 * S, py + 20 * S], fill=col + (cfg["pool_a"],))
    g = g.filter(ImageFilter.GaussianBlur(6 * S)).resize((W, H), Image.LANCZOS)
    g.save(f"{ROOT}/art/objects/KOMPUTER_GLOW{'-night' if mode == 'night' else ''}.png")
    print(mode, "aset ok", (W, H))


if __name__ == "__main__":
    for mode, cfg in MODES.items():
        bg_rapi(mode, cfg)
        gen(mode, cfg)
