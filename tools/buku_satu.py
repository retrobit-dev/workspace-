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
        bottom[x] = (yb2[-1] if len(yb2) else cut[x])
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


if __name__ == "__main__":
    if "--restore" in sys.argv:
        for m, p in ARTS.items():
            bak = Path(f"/tmp/backup_{'day' if m == 'day' else 'night'}.png")
            if bak.exists():
                Image.open(bak).save(p)
                print("restore", p)
    elif "--dry" in sys.argv:
        main(dry=True)
    else:
        main()
