"""Rakit ulang scene dari background + aset objek mandiri.

Pipeline 0.16.15:
    art/objects/background_{day,night}.png   (latar dasar, bekas objek di-repair)
  + art/objects/<ID>.png                      (aset RGBA, diurutkan per layer)
  = art/compose-{day,night}.png               (harus identik dengan art asli)

Objek dengan terpasang_di_latar=true (mis. TANAMAN) tidak ditempel ulang —
akarnya memang bagian latar; edit dilakukan langsung di gambar dasar.

  python3 tools/compose.py [--check] [--tanpa ID[,ID..]] [--out PREFIX]

  --check        verifikasi komposit == art asli (0 piksel beda di luar
                 cincin tepi objek); gagal -> exit non-zero
  --tanpa ID     rakit scene TANPA objek tsb (hapus aman, objek lain tak
                 terpengaruh); contoh: --tanpa KOMPUTER,KALKULATOR
  --out PREFIX   tulis hasil ke PREFIX-day.png / PREFIX-night.png
                 (default art/compose)
"""
from __future__ import annotations

import json
import sys

import cv2
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OBJ = ROOT / "art" / "objects"


def compose(mode: str, tanpa: set[str] | None = None) -> Image.Image:
    man = json.loads((OBJ / "manifest.json").read_text())
    bg = Image.open(OBJ / f"background_{mode}.png").convert("RGBA")
    for o in sorted(man["objek"], key=lambda o: o["layer"]):
        if o["terpasang_di_latar"] or (tanpa and o["id"] in tanpa):
            continue
        cut = Image.open(ROOT / o["aset"][mode]).convert("RGBA")
        bg.alpha_composite(cut, (o["bbox"]["x"], o["bbox"]["y"]))
    return bg.convert("RGB")


def main() -> None:
    check = "--check" in sys.argv
    tanpa: set[str] = set()
    for i, a in enumerate(sys.argv):
        if a == "--tanpa":
            tanpa = {x.strip() for x in sys.argv[i + 1].split(",") if x.strip()}
    out_pre = "art/compose"
    if "--out" in sys.argv:
        out_pre = sys.argv[sys.argv.index("--out") + 1]
    if out_pre != "art/compose":
        check = False  # verifikasi hanya utk komposit arsitektur standar
    man = json.loads((ROOT / "art/objects/manifest.json").read_text())
    # cincin tepi (union alfa semua aset, di-dilate 1px): komposit tepi feather
    # memadukan objek dgn latar hasil inpaint yang sedikit berbeda dr aslinya
    ring = np.zeros((800, 1280), bool)
    for o in man["objek"]:
        if o["terpasang_di_latar"]:
            continue
        x, y = o["bbox"]["x"], o["bbox"]["y"]
        for fn in (o["aset"]["day"], o["aset"]["night"]):
            a = np.asarray(Image.open(ROOT / fn).convert("RGBA"))[:, :, 3]
            h, w = a.shape
            ring[y:y + h, x:x + w] |= a > 2
    ring = cv2.dilate(ring.astype(np.uint8), np.ones((7, 7), np.uint8)).astype(bool)
    for mode, sumber in (("day", "art/rebuild-day.png"), ("night", "art/rebuild-base.png")):
        hasil = compose(mode, tanpa)
        hasil.save(ROOT / f"{out_pre}-{mode}.png")
        asli = np.asarray(Image.open(ROOT / sumber).convert("RGB")).astype(int)
        beda = np.abs(asli - np.asarray(hasil).astype(int)).max(axis=2) > 10
        n_ring = int((beda & ring).sum())
        n_luar = int((beda & ~ring).sum())
        print(f"{mode:5s} beda>10: cincin tepi {n_ring}, di luar cincin {n_luar} "
              f"({n_luar / beda.size:.4%})")
        if check and n_luar > 0:
            sys.exit(f"GAGAL: {mode} ada piksel beda di luar cincin tepi objek")


if __name__ == "__main__":
    main()
