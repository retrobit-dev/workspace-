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
        # 3) tempel buku-1 kembali DI TEMPAT SAMA
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

    # poligon "nilai" = mask buku (posisi sama)
    m = info["day"][0]
    cnts, _ = cv2.findContours(m, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    c = max(cnts, key=cv2.contourArea)
    poly = cv2.approxPolyDP(c, 2.0, True).reshape(-1, 2)
    dist = cv2.distanceTransform(m, cv2.DIST_L2, 5)
    _, _, _, p = cv2.minMaxLoc(dist)
    data = json.loads((ROOT / "art/objects/tap_polygons.json").read_text())
    ys, xs = np.where(m > 0)
    data["nilai"] = {"bbox": [int(xs.min()), int(ys.min()), int(xs.max() - xs.min()), int(ys.max() - ys.min())],
                     "poly": [[int(px), int(py)] for px, py in poly],
                     "poly2": None, "inside": [int(p[0]), int(p[1])]}
    (ROOT / "art/objects/tap_polygons.json").write_text(json.dumps(data, indent=1))
    print("nilai poly:", data["nilai"]["poly"], "inside:", data["nilai"]["inside"])


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
        # mikro: hantu outline tua di kiri-bawah (x656-702, y342-378) —
        # isi kayu meja per kolom (sampel y380-386), lalu shadow kontak lembut
        for x in range(656, 702):
            samp = np.median(arr[380:387, x], axis=0)
            bc = np.where(bookm[:, x])[0]
            ytop = (bc.max() - 1) if len(bc) else 342
            for y in range(max(342, ytop + 1), 378):
                t = (378 - y) / 36.0
                v = samp * (1 + 0.10 * t)
                if y >= 370:   # lebur ke art asli di tepi bawah (hindari jahitan)
                    w = (y - 370) / 8.0
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
            for y in range(ytop, 378):
                if mode == "night" and x > 766 and y > 345 and arr[y, x].mean() < 132:
                    continue   # alas/kaki lampu night (gelap): jangan disentuh
                t = (378 - y) / 48.0
                v = samp * (1 + 0.06 * t)
                if y >= 370:
                    w = (y - 370) / 8.0
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
        z[288:392, 758:800] = True
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
        Image.fromarray(np.clip(final, 0, 255).astype(np.uint8)).save(pth)
        print("->", pth)


if __name__ == "__main__":
    if "--clean2" in sys.argv:
        clean2()
    elif "--restore" in sys.argv:
        for m, p in ARTS.items():
            bak = Path(f"/tmp/backup_{'day' if m == 'day' else 'night'}.png")
            if bak.exists():
                Image.open(bak).save(p)
                print("restore", p)
    elif "--dry" in sys.argv:
        main(dry=True)
    else:
        main()
