#!/usr/bin/env python3
"""0.16.38: tumpukan buku di meja -> SATU buku saja (permintaan user).

Tumpukan 3 buku menempel di art dasar (art/rebuild-day.png & rebuild-base.png).
Struktur art (diagnosa overlay /tmp/dry_*.png): buku teratas = cover merah +
pita halaman PUTIH; di bawahnya buku-2/3; dinding biru flat di belakang;
garis batas dinding->meja turun ke kiri (y = a + b*x, b ~ -0.5).

Metode v5 (deterministik):
  - buku-1 dideteksi per kolom: tepi atas = piksel pertama yang bukan dinding &
    bukan lampu; dasar = akhir pita halaman PUTIH pertama + garis gelap;
  - garis batas dinding->meja di-fit dari segmen kiri bersih (x653-681);
  - tumpukan lama diisi ulang: atas garis = warna dinding per baris, bawah =
    warna meja per kolom (disampel di bawah hole), blend 6px di garis, blur 1.1;
  - buku-1 ditempel KEMBALI DI TEMPAT SAMA (dasarnya memang sudah di bidang
    meja — buku-2/3 hanya di bawahnya), + bayangan kontak tipis;
  - poligon ketuk "nilai" diperbarui di art/objects/tap_polygons.json.

  python3 tools/buku_satu.py --dry        # overlay diagnosa, tidak menulis
  python3 tools/buku_satu.py              # eksekusi
  python3 tools/buku_satu.py --restore    # pulihkan dari /tmp backup
"""
import json
import shutil
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ARTS = {"day": ROOT / "art" / "rebuild-day.png",
        "night": ROOT / "art" / "rebuild-base.png"}

X0, X1 = 684, 780          # rentang kolom tumpukan (termasuk pojok kanan)
Y_TOP, Y_BOT = 288, 400
DX, DY = 2, 36             # offset posisi buku tunggal = posisi buku TERBAWAH
                           # tumpukan asli (V-tip halaman asli (726,384) vs
                           # buku tunggal (726,348)) — 0.16.40 permintaan user
WALL_SEG = (650, 682)      # kolom bersih utk sampel dinding per baris
BOUND_SEG = (653, 681)     # kolom bersih utk fit garis dinding->meja


def largest_cc(mask):
    n, lab = cv2.connectedComponents(mask.astype(np.uint8))
    sizes = [(lab == i).sum() for i in range(1, n)]
    return np.where(lab == (1 + int(np.argmax(sizes))), 255, 0).astype(np.uint8)


def rb(arr):
    return arr[:, :, 0].astype(int) - arr[:, :, 2].astype(int)


def deteksi(arr, mode):
    _rb = rb(arr)
    mean = arr.mean(2)
    putih = (np.abs(_rb) <= 30) & (mean >= (168 if mode == "day" else 148))
    wall = np.median(arr[283:292, 700:730].reshape(-1, 3), axis=0)
    dwall = np.abs(arr.astype(int) - wall).sum(2)
    lampu = (mean < 112) & (np.abs(_rb) < 35)   # lampu/abu gelap

    W = arr.shape[1]
    top = np.full(W, -1, int); cut = np.full(W, -1, int); bottom = np.full(W, -1, int)
    for x in range(X0, X1):
        notwall = (dwall[:, x] >= 26) & ~lampu[:, x]
        ys = np.where(notwall[Y_TOP:Y_BOT])[0]
        if len(ys) == 0:
            continue
        top[x] = Y_TOP + ys[0]
        band = putih[top[x] + 4:Y_BOT, x]
        yb = np.where(band)[0]
        if len(yb) == 0:
            continue
        s = e = yb[0]
        # tolak kolom "ekor": jarak top->pita putih pertama >46px berarti kolom
        # itu meja+dinding, bukan badan buku-1 (cover buku ~25-40px)
        if s > 46:
            continue
        while e < len(band) - 1 and band[e + 1:e + 3].any():
            e += 1
        cut[x] = top[x] + 4 + e + 3
        yb2 = np.where(_rb[:, x] > 60)[0]
        yb2 = yb2[(yb2 >= top[x]) & (yb2 < Y_BOT)]
        bottom[x] = (yb2[-1] if len(yb2) else min(cut[x] + 26, Y_BOT - 1))
    m = np.zeros(arr.shape[:2], np.uint8)
    for x in range(X0, X1):
        if 0 < cut[x] < bottom[x]:
            m[max(top[x], 0):cut[x] + 1, x] = 255
    m = largest_cc(m)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    m[373:, :] = 0   # pengaman: buku-1 tidak mungkin sampai y>372
    return m, top, cut, bottom, wall, lampu


def garis_meja(arr):
    xs, ys = [], []
    _rb = rb(arr)
    for x in range(BOUND_SEG[0], BOUND_SEG[1]):
        col = np.where(_rb[:, x] > 60)[0]
        col = col[(col > 340) & (col < 430)]
        if len(col):
            xs.append(x); ys.append(col[0])
    if len(xs) < 10:
        raise SystemExit("fit garis meja gagal")
    b, a = np.polyfit(xs, ys, 1)
    return a, b


def main(dry=False):
    imgs = {m: np.asarray(Image.open(p).convert("RGB")).copy() for m, p in ARTS.items()}
    info = {}
    for mode, arr in imgs.items():
        m, top, cut, bottom, wall, lampu = deteksi(arr, mode)
        a, bl = garis_meja(arr)
        ys, xs = np.where(m > 0)
        print(f"{mode}: mask {int((m>0).sum())}px x{xs.min()}-{xs.max()} y{ys.min()}-{ys.max()} "
              f"y_boundary@684={a+bl*684:.0f} @780={a+bl*780:.0f}")
        info[mode] = (m, top, cut, bottom, wall, a, bl)
        if dry:
            ov = arr.copy()
            ov[m > 0] = (ov[m > 0] * 0.5 + np.array([255, 60, 60]) * 0.5).astype(np.uint8)
            for x in range(X0, X1):
                yb_ = int(a + bl * x)
                if 0 <= yb_ < 800:
                    ov[yb_, x] = (255, 255, 0)
                if 0 < top[x] < 800:
                    ov[top[x], x] = (0, 255, 0)
                if 0 < cut[x] < 800:
                    ov[cut[x], x] = (0, 120, 255)
                if 0 < bottom[x] < 800:
                    ov[bottom[x], x] = (255, 0, 255)
            Image.fromarray(ov[260:440, 620:820]).resize((800, 720), Image.NEAREST).save(f"/tmp/dry_{mode}.png")
    if dry:
        print("dry: overlay /tmp/dry_day.png, /tmp/dry_night.png")
        return

    for mode, arr in imgs.items():
        m, top, cut, bottom, wall, a, bl = info[mode]
        mloc = m[Y_TOP:Y_BOT, X0:X1]
        # 0) cutout buku-1 SEBELUM area lama dihapus
        reg0 = arr[Y_TOP:Y_BOT, X0:X1]
        cutout = cv2.bitwise_and(reg0, reg0, mask=mloc)
        # 1) hole tumpukan lama + bayangan bawahnya
        hole = np.zeros(arr.shape[:2], bool)
        for x in range(X0, X1):
            if 0 < cut[x] < bottom[x]:
                hole[max(top[x], 0):bottom[x] + 7, x] = True
        # 2) isi ulang: dinding per baris (atas garis), meja per kolom (bawah)
        fill = arr.copy()
        wallref = np.median(arr[:, WALL_SEG[0]:WALL_SEG[1]], axis=1)   # (800,3)
        for x in range(X0, X1):
            if not hole[:, x].any():
                continue
            yb_ = a + bl * x
            ysx = np.where(hole[:, x])[0]
            y0h, y1h = int(ysx.min()), min(int(ysx.max()), 392)
            desksamp = np.median(arr[y1h + 4:y1h + 12, x], axis=0)
            rows = np.arange(y0h, y1h + 1)
            cols = np.zeros((len(rows), 3))
            up = rows <= yb_ - 3
            dn = rows >= yb_ + 3
            mid = ~up & ~dn
            cols[up] = wallref[rows[up]]
            cols[dn] = desksamp
            if mid.any():   # blend 6px melintasi garis
                t = (rows[mid] - (yb_ - 3)) / 6.0
                cols[mid] = wallref[rows[mid]] * (1 - t[:, None]) + desksamp * t[:, None]
            fill[rows, x] = cols
        blur = np.asarray(Image.fromarray(fill).filter(ImageFilter.GaussianBlur(1.1)))
        arr[hole] = blur[hole]
        # 3) tempel buku-1 kembali DI TEMPAT SAMA (pemindahan ke posisi
        #    terbawah dilakukan di akhir --clean2, lihat "PINDAH BUKU")
        mm = (mloc[:, :, None] / 255.0)
        arr[Y_TOP:Y_BOT, X0:X1] = np.clip(
            cutout.astype(float) * mm + arr[Y_TOP:Y_BOT, X0:X1].astype(float) * (1 - mm),
            0, 255).astype(np.uint8)
        # 4) bayangan kontak tipis di bawah tepi buku
        for i in range(mloc.shape[1]):
            col = np.where(mloc[:, i] > 0)[0]
            if not len(col):
                continue
            yb2 = Y_TOP + col.max() + 1
            x2 = X0 + i
            seg = arr[yb2:yb2 + 5, x2].astype(float)
            arr[yb2:yb2 + 5, x2] = np.clip(seg * np.linspace(0.86, 1.0, len(seg))[:, None], 0, 255).astype(np.uint8)

    for mode, arr in imgs.items():
        Image.fromarray(arr).save(ARTS[mode])
        print("->", ARTS[mode])

    # poligon "nilai" = mask buku (posisi baru: +DX,+DY)
    m = info["day"][0]
    m2 = np.zeros_like(m)
    m2[Y_TOP + DY:Y_BOT + DY, X0 + DX:X1 + DX] = m[Y_TOP:Y_BOT, X0:X1]
    cnts, _ = cv2.findContours(m2, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    c = max(cnts, key=cv2.contourArea)
    poly = cv2.approxPolyDP(c, 2.0, True).reshape(-1, 2)
    dist = cv2.distanceTransform(m2, cv2.DIST_L2, 5)
    _, _, _, p = cv2.minMaxLoc(dist)
    data = json.loads((ROOT / "art/objects/tap_polygons.json").read_text())
    ys, xs = np.where(m2 > 0)
    data["nilai"] = {"bbox": [int(xs.min()), int(ys.min()), int(xs.max() - xs.min()), int(ys.max() - ys.min())],
                     "poly": [[int(px), int(py)] for px, py in poly],
                     "poly2": None, "inside": [int(p[0]), int(p[1])]}
    (ROOT / "art/objects/tap_polygons.json").write_text(json.dumps(data, indent=1))
    print("nilai poly:", data["nilai"]["poly"], "inside:", data["nilai"]["inside"])




# ==================== REBUILD (0.16.42) ====================
# Zona buku-meja dibangun ulang SEKALI PUTUS dari art asli 3-buku.
#   dinding   x656-792 di atas tepi-belakang: DINDING POLOS = profil kolom
#             asli y273-280 dikali kurva gradien vertikal global (dinding
#             murni x628-664) — berkas cahaya berakhir di buku (fisik ok)
#   permukaan back(x)..lip(x): PLANE-FIT 2D per kanal dr piksel kayu asli
#             kedua sisi (rb>35, bebas kertas/buku)
#   muka depan lip(x)..+28: profil dd-aligned muka kanan x792-799
#   buku      cutout mask deteksi() dari ORIGINAL (buku-1 murni, terbukti
#             di stage-1) dipaste di (+2,+36) + bayangan kontak
LBLIP_X0, LBLIP_Y0, LBLIP_SLP = 648, 372.0, 0.2235
BACK_X0, BACK_Y0, BACK_SLP = 645, 372.0, -0.13


def lip(x):
    return LBLIP_Y0 + LBLIP_SLP * (x - LBLIP_X0)


def back(x):
    return BACK_Y0 + BACK_SLP * (x - BACK_X0)


def rebuild():
    orig = {m: np.asarray(Image.open(ROOT / "art" / f"asli-tumpukan-{m}.png")
                          .convert("RGB")).astype(float) for m in ARTS}
    for mode in ("day", "night"):
        out = orig[mode].copy()
        _rb = rb(out); _m = out.mean(2)
        # ---- 1) DINDING POLOS (x656-792, y273..back)
        profk = {x: np.median(out[273:281, x], axis=0) for x in range(656, 793)}
        kurva = {}
        for y in range(273, 372):                      # gradien vertikal global
            base = np.median(orig[mode][y, 628:660], axis=0)
            top = np.median(orig[mode][273:281, 628:660].reshape(-1, 3), 0)
            kurva[y] = np.clip(base / np.maximum(top, 1e-3), 0.5, 1.6)
        for x in range(656, 786):
            yb = int(back(x))
            for y in range(273, yb):
                v = profk[x] * kurva[y]
                if x < 663:                            # feather kiri (ke dinding asli)
                    t = (x - 656) / 7.0
                    v = v * t + out[y, x] * (1 - t)
                out[y, x] = v
        # ---- 2) PERMUKAAN (x679-785, back..lip): plane-fit 2D per kanal dr
        # kayu murni kedua sisi (rb>35, bebas kertas/buku) — seluruh zona ini
        # bekas tumpukan+bayangannya, tidak ada asli yg perlu dipertahankan.
        sx, sy, sc = [], [], []
        for x in list(range(628, 676, 2)) + list(range(788, 801, 2)):
            for y in range(int(back(x)) + 1, int(lip(x)) - 1):
                if _rb[y, x] > 35 and 100 <= _m[y, x] <= 235:
                    sx.append(x); sy.append(y); sc.append(out[y, x])
        A = np.column_stack([np.ones(len(sx)), sx, sy]).astype(float)
        coef = [np.linalg.lstsq(A, np.array([c[i] for c in sc]), rcond=None)[0] for i in range(3)]
        for y in range(340, 402):
            for x in range(679, 786):
                if y <= back(x) + 1 or y > lip(x) - 1:
                    continue
                v = np.array([c[0] + c[1] * x + c[2] * y for c in coef])
                if x < 684:                      # feather kiri (ke sisi-meja asli)
                    t = (x - 679) / 5.0
                    v = v * t + out[y, x] * (1 - t)
                if x >= 781:                     # feather kanan (ke kayu asli)
                    t = (x - 781) / 4.0
                    v = v * (1 - t) + out[y, x] * t
                out[y, x] = v
        # ---- 3) MUKA DEPAN (y int(lip) .. 406): GRADIEN BARIS-SAMA dari
        # kolom muka asli yang terekspos di original: kiri x650-666 & kanan
        # x772-786 (bebas buku). Kontur lip, shading vertikal, dan transisi
        # kursi/lantau ikut otomatis karena sumbernya per-baris dari asli.
        for y in range(370, 405):
            L = np.median(orig[mode][y, 650:667], axis=0)
            R = np.median(orig[mode][y, 768:781], axis=0)
            for x in range(661, 792):
                if y < int(lip(x)) - 1:
                    continue
                if out[y, x, 2] - out[y, x, 0] > 10:      # lantai ungu asli
                    continue
                mn = out[y, x].mean()
                if mn < 70:                               # kursi asli
                    continue
                bekas = ((abs(out[y, x, 0] - out[y, x, 2]) <= 28 and mn >= (128 if mode == "day" else 110))
                         or (-16 <= out[y, x, 0] - out[y, x, 2] and mn <= 150))
                if not bekas:                             # muka asli: biarkan
                    continue
                t = (x - 661) / 126.0; t = t * t * (3 - 2 * t)
                v = L * (1 - t) + R * t
                if x < 668:                               # feather kiri
                    tt = (x - 661) / 7.0
                    v = v * tt + out[y, x] * (1 - tt)
                if x >= 784:                              # feather kanan (meja/laptop asli)
                    tt = (x - 784) / 5.0
                    v = v * (1 - tt) + out[y, x] * tt
                out[y, x] = v
        # ---- 4) BUKU: cutout mask deteksi dari ORIGINAL, paste (+2,+36)
        m = deteksi(orig[mode].astype(np.uint8), mode)[0]
        mloc = m[Y_TOP:Y_BOT, X0:X1]
        mm = cv2.GaussianBlur(mloc, (3, 3), 0)[:, :, None] / 255.0
        src = orig[mode][Y_TOP:Y_BOT, X0:X1].astype(float)
        dst = out[Y_TOP + DY:Y_BOT + DY, X0 + DX:X1 + DX]
        out[Y_TOP + DY:Y_BOT + DY, X0 + DX:X1 + DX] = src * mm + dst * (1 - mm)
        # bayangan kontak di bawah buku baru
        for i in range(mloc.shape[1]):
            col = np.where(mloc[:, i] > 0)[0]
            if not len(col):
                continue
            yb2 = Y_TOP + DY + col.max() + 1
            x2 = X0 + DX + i
            if yb2 + 5 < int(lip(x2)) and out[yb2:yb2 + 5, x2].mean() > 70:
                seg = out[yb2:yb2 + 5, x2]
                out[yb2:yb2 + 5, x2] = seg * np.linspace(0.86, 1.0, 5)[:, None]
        Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).save(ARTS[mode])
        print(f"rebuild[{mode}] ->", ARTS[mode].name)


def clean2():
    """Tahap 2 (0.16.38): seluruh band di sekitar buku (x652-798, y300-394)
    di bawah/di atas garis batas dinding-meja diisi ulang dgn BIDANG gradien
    2D per kanal (dipaskan dr piksel band itu sendiri). Bekas sisa tumpukan,
    goresan vertikal, dan tepi tua semua tertutup bidang halus; buku, lampu,
    dan garis batas dilindungi."""
    for mode, pth in ARTS.items():
        arr = np.asarray(Image.open(pth).convert("RGB")).astype(float)
        H, W = arr.shape[:2]
        a, bl = garis_meja(arr)
        book = deteksi(arr.astype(np.uint8), mode)[0]
        prot = cv2.dilate(book, np.ones((5, 5), np.uint8)) > 0
        # lampu (abu gelap) dilindungi — ambang ketat supaya serpihan buku lama
        # di dekat kaki lampu TIDAK ikut terlindungi
        _rb = rb(arr); mean = arr.mean(2)
        lampu = (mean < 108) & (np.abs(_rb) < 42)
        prot |= cv2.dilate((lampu * 255).astype(np.uint8), np.ones((3, 3), np.uint8)) > 0

        BX0, BY0, BX1, BY1 = 652, 300, 798, 394
        yy, xx = np.mgrid[0:H, 0:W]
        inband = np.zeros((H, W), bool)
        inband[BY0:BY1, BX0:BX1] = True
        yb = a + bl * xx
        atas = inband & (yy <= yb - 3)          # sisi dinding
        bawah = inband & (yy >= yb + 3)         # sisi meja
        garis = inband & ~atas & ~bawah         # pita garis kayu: jangan disentuh
        fillable = (atas | bawah) & ~prot

        def fit_plane(sel):
            pys, pxs = np.where(sel)
            A = np.stack([np.ones(len(pxs)), pxs, pys], 1).astype(float)
            coef, *_ = np.linalg.lstsq(A, arr[pys, pxs], rcond=None)
            ys_, xs_ = np.mgrid[0:H, 0:W]
            return coef[0][None, None, :] + coef[1] * xs_[:, :, None] + coef[2] * ys_[:, :, None]

        # sampel: dalam band, jauh dari buku/lampu/garis (noise sisa = nol-mean)
        samp_atas = atas & ~prot & (xx < 780)
        samp_bawah = bawah & ~prot & (yy < 388)
        plane_atas = fit_plane(samp_atas)
        plane_bawah = fit_plane(samp_bawah)

        out = arr.copy()
        out[atas & fillable] = plane_atas[atas & fillable]
        out[bawah & fillable] = plane_bawah[bawah & fillable]
        # feather: blur hanya area terisi utk melebur tepi
        mask = np.zeros((H, W), float)
        mask[(atas | bawah) & fillable & (yy > BY0 + 2) & (yy < BY1 - 3)] = 1.0
        mimg = np.asarray(Image.fromarray((mask * 255).astype(np.uint8)).filter(
            ImageFilter.GaussianBlur(1.6))).astype(float) / 255
        sm = np.asarray(Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).filter(
            ImageFilter.GaussianBlur(1.2))).astype(float)
        alpha = (mimg * 255).astype(np.uint8)
        alpha = np.asarray(Image.fromarray(alpha).filter(ImageFilter.GaussianBlur(1.0))).astype(float) / 255
        final = out * (1 - alpha[:, :, None]) + sm * alpha[:, :, None]
        final = np.where(mask[:, :, None] > 0, final, arr)
        # bayangan kontak halus di bawah buku (kembali setelah fill)
        bookm = book > 0
        for x in range(X0, X1):
            col = np.where(bookm[:, x])[0]
            if not len(col):
                continue
            y0c = col.max() + 1
            for k, alpha2 in enumerate((0.14, 0.10, 0.06, 0.03)):
                y2 = y0c + k
                if y2 < H:
                    final[y2, x] *= (1 - alpha2)
        # mikro night: serpihan halaman x776-782, y334-362 (di luar jangkauan
        # semua fill) -> kayu meja per kolom, lebur dua tepi
        if mode == "night":
            for x in range(776, 783):
                samp = np.median(arr[366:372, x], axis=0)
                for y in range(334, 362):
                    v = samp * (1 + 0.05 * ((362 - y) / 28.0))
                    w = 0.0
                    if y >= 356: w = max(w, (y - 356) / 6.0)
                    if x >= 781: w = max(w, (x - 781) / 2.0)
                    v = v * (1 - w) + arr[y, x] * w
                    final[y, x] = v
        # mikro: hantu outline tua di kiri-bawah (x656-702, y342-366) —
        # isi kayu meja per kolom (sampel y380-386), lalu shadow kontak lembut
        for x in range(656, 702):
            samp = np.median(arr[380:387, x], axis=0)
            bc = np.where(bookm[:, x])[0]
            ytop = (bc.max() - 1) if len(bc) else 342
            for y in range(max(342, ytop + 1), 366):
                t = (366 - y) / 24.0
                v = samp * (1 + 0.10 * t)
                if y >= 358:   # lebur ke art asli di tepi bawah (hindari jahitan)
                    w = (y - 358) / 8.0
                    v = v * (1 - w) + arr[y, x] * w
                final[y, x] = v
        for x in range(656, 702):
            bc = np.where(bookm[:, x])[0]
            if not len(bc):
                continue
            y0c = bc.max() + 1
            for k, al in enumerate((0.16, 0.11, 0.07, 0.04)):
                if y0c + k < H:
                    final[y0c + k, x] *= (1 - al)
        if mode == "day":
            pass  # siang: area kanan-atas sudah bersih tanpa patch (patch abu menimpa tepi meja asli)
        # strip halaman lama kanan-atas (x779-787, y295-330): dinding
        # (di atas garis) -> warna dinding per baris, meja di bawah garis
        for x in range(779, 788):
            if mode == "day":
                continue
            yb_ = a + bl * x
            wall_col = np.median(arr[281:285, x], axis=0)   # dinding di kolom yg sama (atas buku)
            samp = np.median(arr[336:344, x], axis=0) if mode == "night" else np.median(arr[338:346, x], axis=0)
            bc = np.where(bookm[:, x])[0]
            ytop = (286 if mode == "day" else max(295, (bc.max() + 1) if len(bc) else 295))
            for y in range(ytop, 314):
                if len(bc) and bc.min() <= y <= bc.max():
                    continue
                if y <= yb_:
                    v = wall_col
                else:
                    v = samp
                w = 0.0
                if y >= 308:
                    w = max(w, (y - 308) / 6.0)
                if ytop > 295:  # strip dikecilkan utk night: lembutkan atas
                    w = max(w, min(1.0, (y - ytop + 3) / 6.0))
                if w > 0:
                    v = v * (1 - w) + arr[y, x] * w
                final[y, x] = v
        # kanan-bawah buku (x718-784, s/d y378): bekas halaman/tumpukan/hantu
        # diisi kayu meja per kolom (sampel bersih y381-387). TANPA skip gelap —
        # cover gelap buku lama justru sasaran; lengan lampu ada di x>=790 (aman).
        xmax = 776 if mode == "night" else 778
        for x in range(718, xmax):
            samp = np.median(arr[381:388, x], axis=0)
            bc = np.where(bookm[:, x])[0]
            ytop = max(336, (bc.max() + 1)) if len(bc) else 336
            for y in range(ytop, 372):
                if mode == "night" and x > 766 and y > 345 and arr[y, x].mean() < 132:
                    continue   # alas/kaki lampu night (gelap): jangan disentuh
                t = (372 - y) / 48.0
                v = samp * (1 + 0.06 * t)
                if y >= 364:
                    w = (y - 364) / 8.0
                    v = v * (1 - w) + arr[y, x] * w
                if x >= xmax - 4:
                    w2 = (x - (xmax - 4)) / 4.0
                    v = v * (1 - w2) + arr[y, x] * w2
                final[y, x] = v
        # SAPU GENERIK: piksel "warna halaman" (netral terang) yang tersisa di
        # zona kanan-buku -> dinding (atas garis) / meja (bawah garis). Buku,
        # lampu gelap, dan kayu meja tak tersentuh.
        a2, bl2 = garis_meja(arr)
        bookf = deteksi(arr.astype(np.uint8), mode)[0]
        _rba = rb(arr); _mean = arr.mean(2)
        thr = 148 if mode == "day" else 138
        hal = (np.abs(_rba) <= 32) & (_mean >= thr)
        hal &= cv2.dilate(bookf, np.ones((3, 3), np.uint8)) == 0
        z = np.zeros((H, W), bool)
        z[288:366, 758:800] = True
        hal &= z
        yy_, xx_ = np.mgrid[0:H, 0:W]
        yb_ = a2 + bl2 * xx_
        atas2 = hal & (yy_ <= yb_)
        bawah2 = hal & (yy_ > yb_)
        wallc = np.median(arr[281:285, :, :], axis=0)   # per kolom (H,3)
        for x in range(758, 800):
            col = np.where(hal[:, x])[0]
            if not len(col):
                continue
            dsk = np.median(arr[376:384, x], axis=0) if x < 776 else np.median(arr[336:344, x], axis=0)
            for y in col:
                final[y, x] = wallc[x] if y <= yb_[0, x] else dsk
        # ==== REKONSTRUKSI BAND KAYU BAWAH (0.16.40) ====
        # Slab flat bekas isi-ulang tumpukan diganti permukaan gradien
        # natural: referensi kiri & kanan dari art asli 3-buku, konteks
        # bawah dari muka-depan yang dibersihkan dulu; buku baru
        # dilindungi + bayangan kontak ikut dasar.
        orig = np.asarray(Image.open(ROOT / "art" / f"asli-tumpukan-{mode}.png").convert("RGB")).astype(float)
        bmask = cv2.dilate(book, np.ones((3, 3), np.uint8)) > 0
        _rb2 = arr[:, :, 0] - arr[:, :, 2]
        _m2 = arr.mean(2)
        # -- band permukaan kayu (y344-391): mulai x687 — pojok kiri sudah
        # ditangani langkah (0); di atas y362 hanya piksel sisa tumpukan.
        x0b, x1b, y0b, y1b = 687, 796, 344, 392
        woodthr = 35 if mode == "day" else 20
        refL = np.median(orig[368:376, 646:664].reshape(-1, 3), 0)   # kayu kiri asli
        refR = np.median(orig[368:376, 780:798].reshape(-1, 3), 0)   # kayu kanan asli
        bawah = np.median(final[392:396, 650:790], axis=0)           # muka depan bersih
        bawah_full = np.tile(bawah[0], (x1b, 1))                     # per kolom penuh
        bawah_full[650:790] = bawah
        bawah_full[:650] = bawah[0]; bawah_full[790:] = bawah[-1]
        bawah = cv2.blur(bawah_full[None, :, :], (13, 1))[0]
        xs = np.arange(x1b)
        tt = np.clip((xs - 630) / (780 - 630), 0, 1); tt = tt * tt * (3 - 2 * tt)
        surf = refL[None, :] * (1 - tt[:, None]) + refR[None, :] * tt[:, None]
        ys = np.arange(362, y1b)
        # blend ke muka depan mengikuti GARIS BIBIR diagonal (bukan y global):
        # bibir = 391 - 0.287*(x-650) (dari (650,391) ke (800,363) di art asli)
        yb = 391.0 - 0.287 * (xs - 650)
        tv = np.clip((ys[:, None] - (yb[None, :] - 6.0)) / 9.0, 0, 1)[:, :, None]
        band = surf[None, :, :] * (1 - tv) + bawah[None, :, :] * tv
        hal_day = (_m2 >= 148) & (np.abs(_rb2) <= 40)      # halaman krem
        hal_nig = (_m2 >= 120) & (np.abs(_rb2) <= 40)
        cov = (_rb2 >= 8) & (_m2 <= 115)                    # cover biru gelap
        stack_like = ((hal_day if mode == "day" else hal_nig) | cov)
        # dinding day (ungu terang, |r-b| kecil) ikut hal_day — remnant hanya
        # dianggap tumpukan bila berada DI BAWAH garis dinding-meja
        _yy2, _xx2 = np.mgrid[0:H, 0:W]
        stack_like &= _yy2 > (a2 + bl2 * _xx2 + 2)
        for y in range(y0b, y1b):
            for x in range(x0b, x1b):
                if bmask[y, x]:
                    continue
                if y < 362:      # zona atas: hanya piksel sisa tumpukan
                    if not stack_like[y, x]:
                        continue
                    w = max(0.0, (6 - (y - 344)) / 6.0) if y < 350 else 0.0
                    v = band[0, x] if w == 0 else band[0, x] * (1 - w) + final[y, x] * w
                else:
                    v = band[y - 362, x]
                    if x >= 792:  # lebur ke kanan (hindari jahitan vertikal)
                        w2 = (x - 792) / 4.0
                        v = v * (1 - w2) + final[y, x] * w2
                final[y, x] = v
        # -- (0b) ZONA MEJA KIRI (x634-687, y330-392): bekas cover buku
        # terbawah + isi-ulang lama. Geometri dari art asli:
        #   dinding-belakang meja : yb(x) = a2 + bl2*x
        #   tepi-kiri meja (iso)  : xedge(y) = 626 + 1.08*(y-362) utk y>=362
        # y di atas yb -> dinding (wallref per baris); kiri xedge -> tone
        # baris-sama dari zona kiri murni (x616-630: bayangan laptop/lantai);
        # selebihnya permukaan kayu = gradien band (surf). Kertas asli di
        # dinding (x656-670, y336-354) dipertahankan; buku baru & lampu aman.
        wallref2 = np.array([np.median(orig[y, 630:652].reshape(-1, 3), 0) for y in range(H)])
        # gradien horizontal dinding (terang ke kanan arah jendela) dari baris
        # dinding bersih asli y306-326; kolom 630-710 -> faktor per kolom
        _wcol = np.median(orig[306:327, 630:711].reshape(21, 81, 3), axis=0)
        _wref = np.median(orig[306:327, 630:652].reshape(-1, 3), 0)
        _wfac = np.clip(_wcol.mean(1) / _wref.mean(), 0.85, 1.15)
        _edge = lambda y: 626 + 1.08 * (y - 362)
        _front = lambda x: 362.0 + 0.5 * (x - 626)   # tepi depan-kiri meja (iso)
        for x in range(634, 677):
            for y in range(330, 392):      # kiri tumpukan: art asli MURNI
                final[y, x] = orig[y, x]
        for x in range(677, 687):
            ybline = a2 + bl2 * x
            for y in range(330, 392):
                if 0 <= y - DY < H and 0 <= x - DX < W and book[y - DY, x - DX]:
                    continue                                   # buku BARU
                if arr[y, x].mean() < 108 and abs(arr[y, x, 0] - arr[y, x, 2]) < 42:
                    continue                                   # lampu
                if y < ybline - 2:
                    if arr[y, x, 0] - arr[y, x, 2] <= 22:
                        continue                               # dinding/laptop asli: aman
                    v = wallref2[y]                            # bekas cover hangat -> dinding
                elif y >= 362 and x < _edge(y):
                    v = np.median(final[y, 616:630], axis=0)   # kiri meja
                else:
                    v = surf[x]                                # kayu
                    if y >= _front(x) and x < 672:
                        v = v * 0.86                           # sisi depan-kiri (lebih gelap)
                    e = _edge(y)
                    if y >= 362 and x < e + 3:
                        v = v * 0.78                           # garis tepi meja
                    if y >= 362 and x < e + 6:
                        wq = (x - e) / 6.0
                        v = v * wq + np.median(final[y, 616:630], axis=0) * (1 - wq)
                if x >= 683:      # lebur ke band di kanan
                    w2 = (x - 683) / 4.0
                    v = v * (1 - w2) + final[y, x] * w2
                final[y, x] = v
        for x in range(x0b, x1b):        # bayangan kontak mengikuti dasar buku
            bcol = np.where(book[:, x])[0]
            if not len(bcol):
                continue
            y0c = bcol.max() + 1
            for k, al in enumerate((0.14, 0.10, 0.06, 0.03)):
                y = y0c + k
                if y0b <= y < y1b and not bmask[y, x]:
                    final[y, x] *= (1.0 - al)
        # ==== PINDAH BUKU ke posisi buku TERBAWAH tumpukan asli (0.16.40) ====
        # Dilakukan TERAKHIR: semua isi-ulang sudah bersih, tinggal angkat
        # buku (mask posisi lama), isi jejaknya dinding/meja seperti mesin
        # stage-1, lalu tempel di (DX,DY) + bayangan kontak baru.
        bookD = cv2.dilate(book, np.ones((7, 7), np.uint8))
        hole2 = np.zeros(final.shape[:2], bool)
        for x in range(676, X1):
            bc = np.where(bookD[:, x])[0]
            if len(bc):
                hole2[bc.min():bc.max() + 7, x] = True   # +7: bayangan kontak lama ikut
        wallref = np.median(final[:, WALL_SEG[0]:WALL_SEG[1]], axis=1)
        fill2 = final.copy()
        for x in range(X0, X1):
            if not hole2[:, x].any():
                continue
            yb_ = a + bl * x
            ysx = np.where(hole2[:, x])[0]
            y0h, y1h = int(ysx.min()), min(int(ysx.max()), 392)
            desksamp = np.median(final[y1h + 4:y1h + 12, x], axis=0)
            rows = np.arange(y0h, y1h + 1)
            cols = np.zeros((len(rows), 3))
            up = rows <= yb_ - 3
            dn = rows >= yb_ + 3
            mid = ~up & ~dn
            cols[up] = wallref[rows[up]]
            cols[dn] = desksamp
            if mid.any():
                t = (rows[mid] - (yb_ - 3)) / 6.0
                cols[mid] = wallref[rows[mid]] * (1 - t[:, None]) + desksamp * t[:, None]
            fill2[rows, x] = cols
        blur2 = np.asarray(Image.fromarray(np.clip(fill2, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.1)))
        bwin = book[Y_TOP:Y_BOT, X0:X1]
        mm = bwin[:, :, None] / 255.0
        src = final[Y_TOP:Y_BOT, X0:X1].astype(float)      # buku SEBELUM lubang diisi
        final[hole2] = blur2[hole2]
        dst = final[Y_TOP + DY:Y_BOT + DY, X0 + DX:X1 + DX]
        final[Y_TOP + DY:Y_BOT + DY, X0 + DX:X1 + DX] = np.clip(
            src * mm + dst.astype(float) * (1 - mm), 0, 255).astype(np.uint8)
        for i in range(bwin.shape[1]):  # bayangan kontak baru
            col = np.where(bwin[:, i] > 0)[0]
            if not len(col):
                continue
            yb2 = Y_TOP + DY + col.max() + 1
            x2 = X0 + DX + i
            seg = final[yb2:yb2 + 5, x2].astype(float)
            final[yb2:yb2 + 5, x2] = np.clip(seg * np.linspace(0.86, 1.0, len(seg))[:, None], 0, 255).astype(np.uint8)
        _bb2 = lambda x: 391.0 - 0.287 * (x - 650)    # bibir depan meja
        # -- (0c-NEW) MUKA DEPAN HOMOGEN (x646-784, ybib-3..+45): seluruh
        # muka depan di bawah buku = profil dd-aligned (dd = y - ybib(x)) —
        # struktur bibir/muka ikut kemiringan. dd 0-14 dari art asli kolom
        # x772-781 (bebas buku & laptop); dd 15-45 dari muka bersih final
        # x748-790. Gelap halus ke kiri; buku baru, lantai ungu aman.
        _p0y = int(_bb2(781))
        prof = []
        for dd in range(46):
            if dd <= 14:
                prof.append(np.median(orig[min(_p0y + dd, 799), 772:782].reshape(-1, 3), 0))
            else:
                prof.append(np.median(final[min(_p0y + dd, 799), 748:791].reshape(-1, 3), 0))
        prof = np.array(prof)
        for x in range(646, 785):
            ybib2 = int(_bb2(x))
            for y in range(max(ybib2 - 3, 362), min(ybib2 + 15, 405)):
                if 0 <= y - DY < H and 0 <= x - DX < W and book[y - DY, x - DX]:
                    continue
                if final[y, x, 2] - final[y, x, 0] > 10:
                    continue                                   # lantai ungu
                dd = int(np.clip(y - ybib2, 0, 45))
                v = prof[dd] * (1.0 - 0.10 * (784 - x) / 138.0)
                if x >= 684:      # lebur ke muka hasil hole-fill (kanan)
                    w3 = min((x - 684) / 60.0, 1.0)
                    v = v * (1 - w3) + final[y, x] * w3
                if x < 652:       # lebur ke tepi kiri
                    vk = (652 - x) / 6.0
                    v = v * (1 - vk) + final[y, x] * vk
                if x >= 780:      # lebur ke muka existing kanan
                    w2 = (x - 780) / 4.0
                    v = v * (1 - w2) + final[y, x] * w2
                final[y, x] = v
        # -- (0f) KOTAK KANAN-BUKU (x783-798, y342-362): strip lama beda tone
        # di kanan buku (kiri-atas alas lampu) -> tone baris-sama dari kayu
        # bersih x784-796; lampu gelap & laptop (y>362) tak disentuh.
        for x in range(783, 799):
            for y in range(342, 363):
                if 0 <= y - DY < H and 0 <= x - DX < W and book[y - DY, x - DX]:
                    continue
                if arr[y, x].mean() < 108:
                    continue
                v = np.median(final[y, 784:797], axis=0)
                if x < 787:       # lebur ke kayu kiri
                    wk = (787 - x) / 4.0
                    v = v * (1 - wk) + final[y, x] * wk
                final[y, x] = v
        # -- (0d) SAPU MINI kiri buku (x676-700, y295-375): kertas tergantung
        # di dinding + sliver pucat kayu, bekas tepi kiri buku lama. Piksel
        # pucat netral saja: di atas garis -> warna dinding kolom, di bawah
        # -> kayu baris-sama (x662-674, hasil rekonstruksi pojok). Buku lama
        # (posisi pre-pindah) dilindungi.
        _thr2 = 148 if mode == "day" else 136
        for x in range(676, 700):
            yb_ = a2 + bl2 * x
            for y in range(295, 375):
                if 0 <= y - DY < H and 0 <= x - DX < W and book[y - DY, x - DX]:
                    continue                                   # buku BARU
                if arr[y, x].mean() < _thr2 or abs(arr[y, x, 0] - arr[y, x, 2]) <= 38:
                    continue   # hanya piksel HANGAT (kertas): dinding tak tersentuh
                v = wallc[x] if y <= yb_ else np.median(final[y, 744:757], axis=0)
                w = 0.0
                if x >= 698: w = max(w, (x - 698) / 2.0)
                if y < 297:  w = max(w, (297 - y) / 2.0)
                if y > 373:  w = max(w, (y - 373) / 2.0)
                if w > 0:
                    v = v * (1 - w) + final[y, x] * w
                final[y, x] = v
        # -- (0e) titik gelap netral kecil (bekas pusat spiral & notch tepi
        # kiri buku lama) di zona x672-690 y322-364: komponen gelap-netral
        # <=14px yang tak menempel buku -> inpaint radius 3.
        zz = np.zeros((H, W), bool)
        zz[300:372, 672:687] = True
        gelp = (arr.mean(2) < _thr2 + 8) & (np.abs(arr[:, :, 0] - arr[:, :, 2]) < 40)
        gelp &= zz
        gelp &= ~(np.roll(np.roll(book, DY, axis=0), DX, axis=1) > 0)   # buku BARU
        nb, lab = cv2.connectedComponents(gelp.astype(np.uint8), connectivity=8)
        for k in range(1, nb):
            comp = lab == k
            if comp.sum() > 220 or (comp & (book > 0)).any():
                continue
            ys2, xs2 = np.where(comp)
            for yy2, xx2 in zip(ys2, xs2):
                final[yy2, xx2] = (wallc[xx2] if yy2 <= a2 + bl2 * xx2
                                   else surf[xx2])
        # -- (0g) SAPU FINAL pojok kiri-buku (x674-688, y338-360): sisa
        # tepi halaman/cover buku terbawah asli. Deterministik: dinding di
        # atas garis, kayu (sumber x744-757) di bawahnya; lebur ke kanan.
        for x in range(674, 693):
            for y in range(332, 378):
                if 0 <= y - DY < H and 0 <= x - DX < W and bookD[y - DY, x - DX]:
                    continue
                if y <= a2 + bl2 * x:
                    v = wallref2[y] * _wfac[x - 630]
                else:
                    v = surf[x]          # gradien kayu (kontinu dgn band)
                    if x <= 690:         # nyambung dgn sisi-kiri meja asli
                        wk = (690 - x) / 13.0
                        v = v * (1 - wk) + np.median(orig[y, 658:673].reshape(-1, 3), 0) * wk
                if x >= 689:
                    w2 = min((x - 689) / 3.0, 1.0)
                    v = v * (1 - w2) + final[y, x] * w2
                final[y, x] = v
        Image.fromarray(np.clip(final, 0, 255).astype(np.uint8)).save(pth)
        print("->", pth)


if __name__ == "__main__":
    if "--rebuild" in sys.argv:
        rebuild()
    elif "--clean2" in sys.argv:
        clean2()
    elif "--restore" in sys.argv:
        for m, p in ARTS.items():
            bak = Path(f"/tmp/backup_{'day' if m == 'day' else 'night'}.png")
            if not bak.exists():
                alt = ROOT / "art" / f"asli-tumpukan-{m}.png"
                if alt.exists():
                    shutil.copy(alt, bak)
                    print("(fallback art/asli-tumpukan)", bak)
            if bak.exists():
                Image.open(bak).save(p)
                print("restore", p)
            else:
                sys.exit(f"backup {m} tidak ada — /tmp & art/asli-tumpukan hilang")
    elif "--dry" in sys.argv:
        main(dry=True)
    else:
        main()
