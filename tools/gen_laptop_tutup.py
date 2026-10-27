"""Aset laptop TERTUTUP + glow menyala dari aset KOMPUTER (day/night).

State "tertutup" = overlay di atas scene: seluruh jejak laptop terbuka
(termasuk sliver tepi atas lid yang di background, y~308-321) ditutup dengan
"meja-rapi" lalu digambar tutup tertutup + mouse. Aset memakai crop
diperluas (768,304)-(950,448) dan alpha feather supaya tepi patch menyatu
dengan scene (bayangan objek lain tak tersentuh).

Meja-rapi diisi terstruktur: dinding di atas tepi belakang meja (sudut iso
puncak ~(793,289); kiri -0.5x+686, kanan 0.533x-136), meja di bawahnya
(profil warna dari baris-baris meja depan yang bersih, digelapkan halus ke
arah belakang).

  python3 tools/gen_laptop_tutup.py
"""
from __future__ import annotations

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = __file__.rsplit("/tools/", 1)[0] if "/tools/" in __file__ else "."
S = 4                       # supersample
CROP = (768, 304, 950, 448)  # crop diperluas (mencakup sliver atas di background)
oy = 304  # origin y crop diperluas

MODES = {
    "day": dict(
        src=f"{ROOT}/art/objects/KOMPUTER.png",
        bg=f"{ROOT}/art/objects/background_day.png",
        rapi=f"{ROOT}/art/objects/background_day_rapi.png",
        ref=f"{ROOT}/art/rebuild-day.png",
        hinge=[(14, 104), (155, 58)],     # garis engsel dalam koordinat crop baru (+17)
        deck=[(20, 97), (151, 61), (146, 113), (93, 134)],
        mouse=(56, 28), mbody=(139, 135, 146), mtop=(176, 172, 183), mline=(96, 93, 104),
        base=(233, 230, 228), back=(203, 199, 203), right=(196, 192, 197),
        rim=(246, 244, 242), outline=(150, 146, 156), seam=(168, 164, 172),
        glow=(214, 231, 255), core_a=105, halo_a=55, pool_a=40,
        core_c=(115, 69), pool_c=(97, 133),
    ),
    "night": dict(
        src=f"{ROOT}/art/objects/KOMPUTER-night.png",
        bg=f"{ROOT}/art/objects/background_night.png",
        rapi=f"{ROOT}/art/objects/background_night_rapi.png",
        ref=f"{ROOT}/art/rebuild-base.png",
        hinge=[(14, 102), (162, 58)],
        deck=[(20, 95), (158, 61), (150, 113), (96, 134)],
        mouse=(56, 28), mbody=(97, 94, 106), mtop=(130, 126, 138), mline=(66, 63, 74),
        base=(216, 205, 209), back=(185, 175, 183), right=(175, 166, 176),
        rim=(233, 224, 227), outline=(122, 113, 126), seam=(140, 131, 142),
        glow=(214, 231, 255), core_a=200, halo_a=110, pool_a=85,
        core_c=(112, 64), pool_c=(97, 133),
    ),
}


def bg_rapi(mode: str, cfg: dict) -> None:
    """Hapus laptop (terbuka + ghost) dari background.
    Mask: bbox aset bawah penuh + sliver atas ghost lid + interior lid polos.
    Isi memakai FIT POLINOMIAL 2D per kanal dari piksel bersih di sekitar —
    gradasi cahaya lampu (radial) & perspektif meja otomatis mengalir, tidak
    ada bidang flat."""
    import cv2
    sc = np.asarray(Image.open(cfg["ref"]).convert("RGB")).astype(float)
    bg = np.asarray(Image.open(cfg["bg"]).convert("RGB")).astype(float)
    H, W = bg.shape[:2]

    diff = np.abs(bg - sc).sum(axis=2)
    mask = np.zeros((H, W), bool)
    mask[300:470, 740:980] = diff[300:470, 740:980] > 12
    mask[321:446, 768:950] = True                    # bbox aset bawah (selalu tertutup aset)
    mask[304:321, 782:938] = True                    # sliver atas ghost lid (stop sebelum tirai)
    lmask = Image.new("L", (W, H), 0)
    # interior lid antara tepi meja+3 dan garis engsel (x 796..940)
    hy = cfg["hinge"]
    pts = []
    for x in (796, 940):
        ey = int((-0.50 * x + 686.0) if x < 793 else (0.533 * x - 136.0)) + 3
        yl = hy[0][1] + (x - hy[0][0]) * (hy[1][1] - hy[0][1]) / (hy[1][0] - hy[0][0]) + 304
        pts.append((x, ey)); pts.append((x, int(yl)))
    ImageDraw.Draw(lmask).polygon([(pts[0][0], pts[0][1]), (pts[2][0], pts[2][1]),
                                   (pts[3][0], pts[3][1]), (pts[1][0], pts[1][1])], fill=255)
    mask |= np.asarray(lmask) > 0
    mask = cv2.dilate(mask.astype(np.uint8), np.ones((5, 5), np.uint8)) > 0

    def edge_y(x: float) -> float:
        return (-0.50 * x + 686.0) if x < 793 else (0.533 * x - 136.0)

    def is_desk(px) -> bool:
        return px[0] > px[2] + 12 and px[0] > 110

    # ---- fit polinomial ----
    def design(xs, ys, deg):
        xs = np.asarray(xs) / 100.0
        ys = np.asarray(ys) / 100.0
        cols = [np.ones_like(xs), xs, ys]
        if deg >= 2:
            cols += [xs * xs, xs * ys, ys * ys]
        return np.stack(cols, 1)

    # dinding: sampel warna netral/wall-ish bersih di atas tepi (deg 2 —
    # menangkap gradasi hangat lampu -> dingin ke kanan)
    wx, wy, wc = [], [], []
    for y in range(282, 318):
        for x in range(745, 976):
            px = bg[y, x]
            if not mask[y, x] and abs(int(px[0]) - int(px[2])) <= 20 and 55 <= px.mean() <= 215:
                wx.append(x); wy.append(y); wc.append(px)
    Wm = design(wx, wy, 2)
    wall_c = np.linalg.lstsq(Wm, np.asarray(wc), rcond=None)[0]

    # meja: sampel "meja-ish" bersih di kawasan laptop (deg 2 — gradasi lampu)
    dxs, dys, dc = [], [], []
    for y in range(330, 452):
        for x in range(745, 976):
            if not mask[y, x] and is_desk(bg[y, x]):
                dxs.append(x); dys.append(y); dc.append(bg[y, x])
    Dm = design(dxs, dys, 2)
    desk_c = np.linalg.lstsq(Dm, np.asarray(dc), rcond=None)[0]

    xx, yy = np.meshgrid(np.arange(W), np.arange(H))
    def wall_at(xs, ys):
        return design(xs, ys, 2) @ wall_c
    def desk_at(xs, ys):
        return design(xs, ys, 2) @ desk_c

    fill = bg.copy()
    ys, xs = np.where(mask)
    for x in range(740, 980):
        col = ys[(xs == x)]
        if not len(col):
            continue
        ey = edge_y(x)
        yi = int(round(ey))
        wallc = wall_at(np.full(len(col), x), col.astype(float))
        deskc = desk_at(np.full(len(col), x), col.astype(float))
        up = col < yi - 2
        dn = col > yi + 2
        mid = ~(up | dn)
        fill[col[up], x] = wallc[up]
        fill[col[dn], x] = deskc[dn]
        fill[col[mid], x] = (wallc[mid] + deskc[mid]) * 0.5
        if BYE := [yi]:
            sel = (xs == x) & (ys == yi)
            if sel.any():
                fill[yi, x] = desk_at(np.array([x], float), np.array([yi], float))[0] * 0.78

    out = bg.copy()
    out[mask] = cv2.blur(fill, (5, 5))[mask] * 0.35 + fill[mask] * 0.65
    Image.fromarray(out.astype(np.uint8)).save(cfg["rapi"])
    print(f"{mode}: poly-fit isi {int(mask.sum())} px (dinding {len(wx)}, meja {len(dxs)} sampel)")


def inset_edge(p, q, cpt, w):
    dx, dy = q[0] - p[0], q[1] - p[1]
    L = (dx * dx + dy * dy) ** 0.5 or 1.0
    nx, ny = -dy / L, dx / L
    if nx * (cpt[0] - p[0]) + ny * (cpt[1] - p[1]) < 0:
        nx, ny = -nx, -ny
    return [p[0], p[1], q[0], q[1], q[0] + nx * w, q[1] + ny * w, p[0] + nx * w, p[1] + ny * w]


def gen(mode: str, cfg: dict) -> None:
    rapi = Image.open(cfg["rapi"]).convert("RGBA").crop(CROP)
    W, H = rapi.size
    # alpha feather: rounded-rect jejak laptop (lokal), blur -> tepi menyatu
    am = Image.new("L", (W * S, H * S), 0)
    ImageDraw.Draw(am).rounded_rectangle(
        [4 * S, 2 * S, (W - 4) * S, (H - 2) * S], radius=8 * S, fill=255)
    alpha = am.resize((W, H), Image.LANCZOS).filter(ImageFilter.GaussianBlur(1.2))
    base = rapi.copy()
    base.putalpha(alpha)

    ov = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    # mouse (digambar ulang; milik scene asli di bawah tutup tertutup)
    mx, my = cfg["mouse"][0] * S, cfg["mouse"][1] * S
    d.ellipse([mx - 9 * S, my - 5 * S, mx + 9 * S, my + 7 * S], fill=cfg["mline"] + (255,))
    d.ellipse([mx - 8 * S, my - 5 * S, mx + 8 * S, my + 6 * S], fill=cfg["mbody"] + (255,))
    d.ellipse([mx - 5 * S, my - 4 * S, mx + 4 * S, my + 2 * S], fill=cfg["mtop"] + (255,))
    # tutup tertutup
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
    base.save(f"{ROOT}/art/objects/KOMPUTER_TERTUTUP{'-night' if mode == 'night' else ''}.png")

    # --- KOSONG: meja-rapi + mouse (tanpa laptop) — dasar animasi ---
    kosong = rapi.copy()
    mo = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    dm = ImageDraw.Draw(mo)
    mx, my = cfg["mouse"][0] * S, cfg["mouse"][1] * S
    dm.ellipse([mx - 9 * S, my - 5 * S, mx + 9 * S, my + 7 * S], fill=cfg["mline"] + (255,))
    dm.ellipse([mx - 8 * S, my - 5 * S, mx + 8 * S, my + 6 * S], fill=cfg["mbody"] + (255,))
    dm.ellipse([mx - 5 * S, my - 4 * S, mx + 4 * S, my + 2 * S], fill=cfg["mtop"] + (255,))
    mo = mo.resize((W, H), Image.LANCZOS)
    kosong.alpha_composite(mo)
    kosong.putalpha(alpha)
    kosong.save(f"{ROOT}/art/objects/KOMPUTER_KOSONG{'-night' if mode == 'night' else ''}.png")

    # --- TERBUKA: meja-rapi + piksel laptop terbuka (mask diff + fill holes) ---
    from scipy.ndimage import binary_fill_holes
    src = Image.open(cfg["src"]).convert("RGBA")
    ghost = Image.open(cfg["bg"]).convert("RGBA").crop((CROP[0], CROP[1] + 17, CROP[2], CROP[1] + 17 + 127))
    sa = np.asarray(src).astype(int)
    ga = np.asarray(ghost).astype(int)
    dm2 = np.abs(sa[:, :, :3] - ga[:, :, :3]).sum(axis=2) > 12
    dm2 = binary_fill_holes(dm2)
    from PIL import ImageFilter as _IF
    mk = Image.fromarray((dm2 * 255).astype(np.uint8)).filter(_IF.MaxFilter(3)).filter(_IF.GaussianBlur(1.1))
    # base = scene asli: saat progress 1, overlay identik dgn scene (handoff mulus)
    terbuka = Image.open(cfg["ref"]).convert("RGBA").crop(CROP)
    terbuka.paste(src, (0, 17), mk)
    terbuka.putalpha(alpha)
    terbuka.save(f"{ROOT}/art/objects/KOMPUTER_TERBUKA{'-night' if mode == 'night' else ''}.png")

    # glow menyala (overlay, alpha dibuat progres di app/web)
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
