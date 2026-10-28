#!/usr/bin/env python3
"""Poligon area ketuk per objek — bentuk area ketuk = bentuk objek.

Sumber bentuk:
  - Aset mandiri (art/objects/*.png): kontur alpha mask, origin = bbox manifest.
    KOMPUTER dikurangi mask BUKU (buku = aset ketuk sendiri).
  - Bentuk menempel latar: poligon TERUKUR MANUAL (dari scan piksel art) —
    papan tulis = muka papan saja (kaki/nampan tidak ikut), papan pin = gabus
    saja (tidak menelan rak), rak & laci = panel depan.
  - Cuaca: dua poligon kaca jendela (SKY_HOLES_DAY[0]=kiri, [1]=kanan) — satu
    langit hidup (0.16.37).

Output: art/objects/tap_polygons.json  {id: {bbox, poly, poly2, inside}}
  poly   = [[x,y],...] koordinat kanvas 1280x800
  poly2  = poligon kedua bila objek punya dua area (Cuaca), lainnya null
  inside = titik di dalam poligon utama (pole of inaccessibility) utk uji
"""
import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OBJ = ROOT / "art" / "objects"
sys.path.insert(0, str(ROOT / "tools"))
from make_preview import SKY_HOLES_DAY  # noqa: E402

EPS = 2.4  # toleransi penyederhanaan poligon (px)

# id-tap -> (id-aset-manifest | None, sumber)
SPEC = {
    "jurnal":     ("KOMPUTER",      "kartu-buku"),
    "nilai":      ("BUKU",          "aset"),
    "alat-tulis": ("GELAS_PENSIL",  "kartu"),
    "kalkulator": ("KALKULATOR",    "aset"),
    "binder":     (None,            "laci"),
    "absensi":    (None,            "pin"),
    "catatan":    (None,            "rak"),
    "jadwal":     (None,            "papan"),
    "profil":     ("BINGKAI_KANAN", "aset"),
    "cuaca":      (None,            "langit"),
    "file":       ("TEMPAT_SAMPAH", "aset"),
}

# Poligon terukur manual dari scan piksel web/scene_day.png (lihat sesi 0.16.34).
# papan tulis: bingkai luar saja — TL(319,241) TR(578,107) BR(582,337) BL(322,472);
# kaki & nampan pensil TIDAK ikut (keputusan user: area = papan saja).
PAPAN = [(319, 241), (578, 107), (582, 337), (322, 472)]
# papan pin: gabus + bingkai kayu (atas y217-264, bawah y401-437)
PIN = [(66, 264), (233, 217), (233, 401), (67, 437)]
# rak buku: panel depan (kontur scan)
RAK = [(214, 435), (284, 435), (284, 626), (151, 695), (149, 699), (118, 699), (118, 487), (212, 439)]
# laci meja (kontur scan; pegangan di dalam)
LACI = [(848, 493), (944, 493), (944, 604), (884, 604), (839, 568), (838, 520), (853, 519), (869, 507)]
CUACA_BBOX = (1040, 80, 240, 360)

# Kartu aset KOMPUTER & GELAS_PENSIL memuat piksel objek lain (alas lampu, buku,
# kalkulator, tepi laptop) — bentuk objek sendiri dipoligonkan manual dr grid kartu
# (koordinat kartu; ditambah origin manifest saat keluar).
JURNAL_POLY = [(47, 52), (95, 0), (152, 12), (157, 62), (152, 95), (145, 101),
               (88, 122), (10, 88), (42, 60)]
ALAT_POLY = [(60, 18), (63, 0), (69, 3), (73, 9), (78, 0), (85, 0), (86, 16),
             (86, 50), (84, 82), (58, 86), (56, 50), (57, 22)]



def simplify(mask):
    cnts, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not cnts:
        raise SystemExit("tidak ada kontur")
    c = max(cnts, key=cv2.contourArea)
    poly = cv2.approxPolyDP(c, EPS, True).reshape(-1, 2)
    m = np.zeros_like(mask)
    cv2.drawContours(m, [c], -1, 255, -1)
    dist = cv2.distanceTransform(m, cv2.DIST_L2, 5)
    _, _, _, p = cv2.minMaxLoc(dist)
    return [(int(x), int(y)) for x, y in poly], (int(p[0]), int(p[1]))


def aset_mask(asset_id):
    im = np.asarray(Image.open(OBJ / f"{asset_id}.png").convert("RGBA"))
    return (im[:, :, 3] > 100).astype(np.uint8) * 255


def manifest_bbox(asset_id):
    man = json.loads((OBJ / "manifest.json").read_text())
    for o in man["objek"]:
        if o["id"] == asset_id:
            b = o["bbox"]
            return b["x"], b["y"], b["w"], b["h"]
    raise SystemExit(f"{asset_id} tidak ada di manifest")


def poly_aset(asset_id, mode):
    if mode == "kartu":  # poligon manual (kartu aset berisi piksel objek lain)
        m = np.zeros((800, 1280), np.uint8)
        x0, y0, _, _ = manifest_bbox(asset_id)
        src = JURNAL_POLY if asset_id == "KOMPUTER" else ALAT_POLY
        pts = np.array([(px + x0, py + y0) for px, py in src], np.int32)
        cv2.fillPoly(m, [pts], 255)
        poly, inside = simplify(m)
        return [[int(px), int(py)] for px, py in poly], (inside[0], inside[1])
    if mode == "kartu-buku":  # poligon manual laptop - area buku (aset Nilai)
        kx, ky, _, _ = manifest_bbox("KOMPUTER")
        bx, by, _, _ = manifest_bbox("BUKU")
        m = np.zeros((800, 1280), np.uint8)
        pts = np.array([(px + kx, py + ky) for px, py in JURNAL_POLY], np.int32)
        cv2.fillPoly(m, [pts], 255)
        buku = aset_mask("BUKU")
        sub = np.zeros_like(m)
        sub[by:by + buku.shape[0], bx:bx + buku.shape[1]] = buku
        m = cv2.bitwise_and(m, cv2.bitwise_not(sub))
        poly, inside = simplify(m)
        return [[int(px), int(py)] for px, py in poly], (inside[0], inside[1])
    mask = aset_mask(asset_id)
    poly, inside = simplify(mask)
    if mode == "aset-buku":  # laptop: buang area buku (aset Nilai sendiri)
        bx, by, _, _ = manifest_bbox("BUKU")
        kx, ky, _, _ = manifest_bbox("KOMPUTER")
        buku = aset_mask("BUKU")
        H, W = mask.shape
        # irisan rect buku ke kotak komputer (offset boleh negatif)
        x0, y0 = max(0, bx - kx), max(0, by - ky)
        x1, y1 = min(W, bx + buku.shape[1] - kx), min(H, by + buku.shape[0] - ky)
        if x1 > x0 and y1 > y0:
            sub = np.zeros_like(mask)
            sub[y0:y1, x0:x1] = buku[y0 - (by - ky):y1 - (by - ky), x0 - (bx - kx):x1 - (bx - kx)]
            mask = cv2.bitwise_and(mask, cv2.bitwise_not(sub))
    x, y, _, _ = manifest_bbox(asset_id)
    return [[int(px + x), int(py + y)] for px, py in poly], (inside[0] + x, inside[1] + y)


def inside_of(poly):
    m = np.zeros((800, 1280), np.uint8)
    cv2.fillPoly(m, [np.array(poly, np.int32)], 255)
    dist = cv2.distanceTransform(m, cv2.DIST_L2, 5)
    _, _, _, p = cv2.minMaxLoc(dist)
    return [int(p[0]), int(p[1])]


def main():
    out = {}
    for tid, (asset_id, mode) in SPEC.items():
        if mode in ("aset", "aset-buku", "kartu", "kartu-buku"):
            poly, inside = poly_aset(asset_id, mode)
            bbox = list(manifest_bbox(asset_id))
            poly2 = None
        elif mode == "papan":
            poly, poly2, bbox = PAPAN, None, [72, 128, 530, 330]
            inside = inside_of(poly)
        elif mode == "pin":
            poly, poly2, bbox = PIN, None, [56, 220, 228, 220]
            inside = inside_of(poly)
        elif mode == "rak":
            poly, poly2, bbox = RAK, None, [118, 435, 167, 265]
            inside = inside_of(poly)
        elif mode == "laci":
            poly, poly2, bbox = LACI, None, [797, 493, 148, 112]
            inside = inside_of(poly)
        elif mode == "langit":
            poly = [list(SKY_HOLES_DAY[0][i:i + 2]) for i in range(0, 8, 2)]
            poly2 = [list(SKY_HOLES_DAY[1][i:i + 2]) for i in range(0, 8, 2)]
            bbox = list(CUACA_BBOX)
            inside = inside_of(poly)
        out[tid] = {"bbox": bbox, "poly": poly, "poly2": poly2, "inside": list(inside)}
        n = len(poly) + (len(poly2) if poly2 else 0)
        print(f"{tid:11s} {mode:10s} titik={n:2d} inside={inside}")
    (OBJ / "tap_polygons.json").write_text(json.dumps(out, indent=1))
    print("-> art/objects/tap_polygons.json")


if __name__ == "__main__":
    main()
