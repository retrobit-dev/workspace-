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
    d.ellipse([8, -12, 44, 22], fill=255)   # ujung lid: ghost berkilau cyan
    m = np.asarray(im) > 0
    bg = np.asarray(Image.open(cfg["bg"]).convert("RGB")).astype(int)
    sc = np.asarray(Image.open(cfg["ref"]).convert("RGB")).astype(int)
    diff = np.abs(bg[CROP[1]:CROP[3], CROP[0]:CROP[2]] - sc[CROP[1]:CROP[3], CROP[0]:CROP[2]]).sum(2) > 12
    diff[:, 171:] = False          # zona tirai: ghost tak sampai sini
    diff[:, :40] = False           # sisi kiri laptop: tetap tampil (sisi tertutup)
    m |= diff
    return m


def design(xs, ys, deg):
    xs = np.asarray(xs) / 100.0
    ys = np.asarray(ys) / 100.0
    cols = [np.ones_like(xs), xs, ys]
    if deg >= 2:
        cols += [xs * xs, xs * ys, ys * ys]
    return np.stack(cols, 1)


def bg_rapi(mode: str, cfg: dict) -> None:
    """Buang ghost laptop dari background: mask = laptop_mask, isi = fit
    polinomial (dinding deg2 & meja deg2) + garis tepi meja digambar ulang."""
    bg = np.asarray(Image.open(cfg["bg"]).convert("RGB")).astype(float)
    H, W = bg.shape[:2]
    mask_full = np.zeros((H, W), bool)
    mask_full[CROP[1]:CROP[3], CROP[0]:CROP[2]] = laptop_mask(cfg)
    mask_full = cv2.dilate(mask_full.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0

    # dinding: sampel netral/wall bersih di atas tepi
    wx, wy, wc = [], [], []
    for y in range(282, 318):
        for x in range(745, 928):
            if not mask_full[y, x]:
                px = bg[y, x]
                if abs(int(px[0]) - int(px[2])) <= 20 and 55 <= px.mean() <= 215:
                    wx.append(x)
                    wy.append(y)
                    wc.append(px)
    wall_c = np.linalg.lstsq(design(wx, wy, 2), np.asarray(wc), rcond=None)[0]

    # meja: sampel meja-ish bersih
    dxs, dys, dc = [], [], []
    for y in range(330, 452):
        for x in range(745, 976):
            if not mask_full[y, x]:
                px = bg[y, x]
                if px[0] > px[2] + 12 and px[0] > 110:
                    dxs.append(x)
                    dys.append(y)
                    dc.append(px)
    desk_c = np.linalg.lstsq(design(dxs, dys, 2), np.asarray(dc), rcond=None)[0]

    xx, yy = np.meshgrid(np.arange(W), np.arange(H))
    wall_img = np.clip((design(xx.ravel(), yy.ravel(), 2) @ wall_c), 0, 255).reshape(H, W, 3)
    desk_img = np.clip((design(xx.ravel(), yy.ravel(), 2) @ desk_c), 0, 255).reshape(H, W, 3)

    fill = bg.copy()
    ys, xs = np.where(mask_full)
    eys = np.array([edge_y(x) for x in xs])
    # zona permukaan: kanan puncak iso (x>=793) meja mulai 6px di atas garis
    # (tepi meja tampak tebal); sisanya dinding
    desk_zone = (xs >= 806) & (ys >= eys - 6)
    wall_zone = ~desk_zone
    # campur fit dgn sampel per-kolom (menjaga variasi horizontal: glow lampu)
    col_wall = np.zeros((W, 3))
    for x in range(W):
        rows = [bg[y, x] for y in range(296, 318)
                if not mask_full[y, x] and x <= 916
                and abs(int(bg[y, x][0]) - int(bg[y, x][2])) <= 12
                and 55 <= bg[y, x].mean() <= 215]
        col_wall[x] = np.mean(rows, axis=0) if rows else np.nan
    last = np.nan
    for x in range(W):
        if np.isnan(col_wall[x]).any():
            col_wall[x] = last if not np.isnan(last).any() else wall_img[6, x]
        else:
            last = col_wall[x]
    # meja per-kolom dari strip meja-depan (bebas kursi); rambat kanan->kiri
    col_desk = np.full((W, 3), np.nan)
    y_lo, y_hi = (441, 449) if mode == "day" else (439, 447)
    for x in range(W):
        rows = [bg[y, x] for y in range(y_lo, y_hi) if not mask_full[y, x]
                and bg[y, x][0] > bg[y, x][2] + 12 and bg[y, x][0] > 110]
        if rows:
            col_desk[x] = np.mean(rows, axis=0)
    last = np.nan
    for x in range(W - 1, -1, -1):
        if np.isnan(col_desk[x]).any():
            col_desk[x] = last if not np.isnan(last).any() else desk_img[y_hi - 1, x]
        else:
            last = col_desk[x]

    wall_mix = np.clip(0.2 * wall_img[ys, xs] + 0.8 * col_wall[xs], 0, 255)
    # fit meja hanya dipercaya dalam rentang sampelnya (y>=326 & x>=815);
    # sisanya meja dari strip depan per kolom
    fit_ok = (ys >= 326) & (xs >= 815)
    desk_use = np.where(fit_ok[..., None], desk_img[ys, xs], col_desk[xs])
    fill[ys[wall_zone], xs[wall_zone]] = np.clip(wall_mix[wall_zone], 0, 255)
    fill[ys[desk_zone], xs[desk_zone]] = np.clip(desk_use[desk_zone], 0, 255)

    line = np.abs(ys - eys) < 2.5
    fill[ys[line], xs[line]] = desk_use[line] * 0.74

    out = bg.copy()
    out[mask_full] = fill[mask_full]
    Image.fromarray(out.astype(np.uint8)).save(cfg["rapi"])
    print(f"{mode}: rapi {int(mask_full.sum())} px (wall {len(wx)}, desk {len(dxs)} sampel)")


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
