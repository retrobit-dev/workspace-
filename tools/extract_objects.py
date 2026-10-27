"""Pisahkan objek scene menjadi aset RGBA mandiri + manifest identitas.

Arsitektur 0.16.15:
  - Setiap objek dipotong dari art (day & night) menjadi PNG RGBA transparan
    di art/objects/ — mandiri, tidak menempel pada objek lain.
  - Background (art/rebuild-*.png) dipulihkan di bekas objek dengan
    inpaint ber-mask sehingga tidak ada "bekos".
  - Identitas tiap objek (bbox, pivot, layer, warna, deskripsi) ditulis ke
    art/objects/manifest.json.
  - Scene final dirakit ulang oleh tools/compose.py:
    background + potongan objek = hasil yang identik piksel-demi-piksel.

  python3 tools/extract_objects.py
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
OBJ = ROOT / "art" / "objects"
OBJ.mkdir(parents=True, exist_ok=True)

DAY = np.asarray(Image.open(ROOT / "art/rebuild-day.png").convert("RGB"))
NIGHT = np.asarray(Image.open(ROOT / "art/rebuild-base.png").convert("RGB"))
H, W = DAY.shape[:2]

# ---------------------------------------------------------------- spesifikasi
# bbox = (x, y, w, h); core = kotak interior ber-alfa penuh (area halus yang
# tak terdeteksi sebagai detail, mis. bidang kaca/bingkai dalam).
# terpasang_di_latar: objek yang akarnya menyatu dengan latar (pot tanaman).
SPECS = [
    # --- dinding kiri: 5 bingkai bertingkat (bbox hasil deteksi blob otomatis) ---
    dict(id="BINGKAI_1", nama="Bingkai dinding 1 (bawah)", bbox=(136, 164, 59, 105), margin=(14, 8, 6, 10), layer=11, terpasang=False, fungsi="dekorasi dinding"),
    dict(id="BINGKAI_2", nama="Bingkai dinding 2", bbox=(208, 126, 58, 105), margin=(14, 8, 4, 10), layer=12, terpasang=False, fungsi="dekorasi dinding"),
    dict(id="BINGKAI_3", nama="Bingkai dinding 3", bbox=(280, 89, 58, 105), margin=(14, 8, 4, 10), layer=13, terpasang=False, fungsi="dekorasi dinding"),
    dict(id="BINGKAI_4", nama="Bingkai dinding 4 (atas)", bbox=(352, 51, 59, 105), margin=(11, 8, 6, 10), layer=14, terpasang=False, fungsi="dekorasi dinding"),
    dict(id="BINGKAI_5", nama="Bingkai dinding 5 (kiri bawah)", bbox=(64, 202, 59, 104), margin=(12, 8, 6, 10), layer=15, terpasang=False, fungsi="dekorasi dinding"),
    dict(id="BINGKAI_KANAN", nama="Bingkai dinding kanan", bbox=(694, 36, 63, 117), margin=(10, 8, 6, 8), layer=16, terpasang=False, fungsi="dekorasi dinding dekat jendela"),
    # --- papan pin: 5 catatan krem ---
    dict(id="CATATAN_1", nama="Catatan papan pin 1 (tengah bawah)", bbox=(122, 363, 34, 60), force=(122, 363, 34, 60), margin=(10, 14, 8, 8), layer=31, terpasang=False, fungsi="catatan di papan pin"),
    dict(id="CATATAN_2", nama="Catatan papan pin 2 (atas)", bbox=(181, 297, 34, 56), force=(181, 297, 34, 56), margin=(12, 14, 6, 8), layer=32, terpasang=False, fungsi="catatan di papan pin"),
    dict(id="CATATAN_3", nama="Catatan papan pin 3 (pin kecil)", bbox=(136, 326, 15, 24), force=(136, 326, 15, 24), margin=(10, 12, 8, 8), layer=33, terpasang=False, fungsi="catatan kecil di papan pin"),
    dict(id="CATATAN_4", nama="Catatan papan pin 4 (kanan)", bbox=(166, 364, 25, 37), force=(166, 364, 25, 37), margin=(8, 12, 8, 8), layer=34, terpasang=False, fungsi="catatan bergaris di papan pin"),
    dict(id="CATATAN_5", nama="Catatan papan pin 5 (kiri)", bbox=(92, 347, 29, 54), force=(92, 347, 29, 54), margin=(10, 12, 8, 8), layer=35, terpasang=False, fungsi="catatan di papan pin"),
    # --- meja kerja ---
    dict(id="KOMPUTER", nama="Laptop di meja", bbox=(776, 335, 174, 113), margin=(8, 14, 0, 0),
         tanpa=[(882, 421, 72, 29)], layer=41, terpasang=False, fungsi="perangkat kerja di meja"),
    dict(id="BUKU", nama="Tumpukan buku di meja", bbox=(735, 400, 78, 80), margin=(16, 6, 0, 8), layer=42, terpasang=False, fungsi="buku rujukan di meja"),
    dict(id="KALKULATOR", nama="Kalkulator di meja", bbox=(886, 425, 76, 50), margin=(8, 8, 8, 8), layer=43, terpasang=False, fungsi="alat hitung di meja"),
    dict(id="GELAS_PENSIL", nama="Gelas pensil", bbox=(917, 381, 61, 68), margin=(16, 10, 8, 10), layer=44, terpasang=False, fungsi="wadah alat tulis di meja"),
    # --- lantai ---
    dict(id="TEMPAT_SAMPAH", nama="Tempat sampah", bbox=(20, 645, 79, 108), force=(32, 658, 55, 82), margin=(16, 10, 8, 12), layer=46, terpasang=False, fungsi="keranjang sampah kamar"),
    dict(id="KERTAS_1", nama="Kertas kusut 1", bbox=(108, 716, 29, 27), force=(108, 716, 29, 27), margin=(12, 12, 8, 8), layer=47, terpasang=False, fungsi="kertas kusut di lantai"),
    dict(id="KERTAS_2", nama="Kertas kusut 2", bbox=(76, 742, 26, 22), force=(76, 742, 26, 22), margin=(10, 10, 8, 8), layer=48, terpasang=False, fungsi="kertas kusut di lantai"),
    # --- sudut jendela: tanaman (matahari di kaca ikut region; akar menyatu latar) ---
    dict(id="TANAMAN", nama="Tanaman pot di sudut", bbox=(1040, 390, 240, 310), layer=50, terpasang=True, fungsi="dekorasi tanaman di bawah jendela"),
]

TAU = 30  # jarak warna minimum dari latar agar dihitung bagian objek


def object_alpha(img: np.ndarray, bbox, force=None) -> np.ndarray:
    """Alfa = jarak warna ke warna LATAR; referensi latar dari 4 pojok bbox
    lalu DIKLASTERKAN — klaster beranggota >=2 dianggap latar sejati,
    pojok yang ternyata mengenai objek (klaster tunggal) dibuang.
    Jika tak ada klaster >=2 (latar memang campur), pakai semua pojok.
    """
    x, y, w, h = bbox
    ex, ey = max(0, x - 12), max(0, y - 12)
    ex1, ey1 = min(W, x + w + 12), min(H, y + h + 12)
    ext = img[ey:ey1, ex:ex1].astype(int)
    eh, ew = ext.shape[:2]
    refs = []
    for cx, cy in ((0, 0), (ew - 6, 0), (0, eh - 6), (ew - 6, eh - 6)):
        patch = ext[cy:cy + 6, cx:cx + 6].reshape(-1, 3)
        refs.append(np.median(patch, axis=0))
    # klasterisasi sederhana: gabung ref yang saling mirip
    grup = []  # list of list index
    for i, r in enumerate(refs):
        for g in grup:
            if max(abs(refs[g[0]] - r)) < 30:
                g.append(i)
                break
        else:
            grup.append([i])
    bgrefs = [refs[g[0]] for g in grup if len(g) >= 2] or refs
    dist = np.min([np.abs(ext - r).max(axis=2) for r in bgrefs], axis=0)
    a = ((dist > TAU) * 255).astype(np.uint8)
    if force:
        fx, fy, fw, fh = force
        a[fy - ey:fy - ey + fh, fx - ex:fx - ex + fw] = 255
    a = cv2.morphologyEx(a, cv2.MORPH_CLOSE, np.ones((5, 5), np.uint8))
    a = cv2.morphologyEx(a, cv2.MORPH_OPEN, np.ones((2, 2), np.uint8))
    return a[y - ey:y - ey + h, x - ex:x - ex + w]


def feather(a: np.ndarray) -> np.ndarray:
    a = cv2.dilate(a, np.ones((5, 5), np.uint8))
    return cv2.GaussianBlur(a, (3, 3), 0.7)


def main() -> None:
    manifest = dict(
        versi="0.16.15",
        kanvas=dict(w=W, h=H, mode=["day", "night"]),
        arsitektur=(
            "Objek = aset RGBA mandiri di art/objects/, digambar berlapis di atas "
            "background oleh tools/compose.py. Background adalah scene dasar yang "
            "bekas objeknya dipulihkan (inpaint) — menghapus/memindah satu objek "
            "tidak memengaruhi objek lain. Elemen yang masih menempel di latar "
            "dapat dipromosikan ke aset mandiri dengan menambah entri SPECS."
        ),
        objek=[],
    )
    bgs = {}
    for name, img in (("day", DAY), ("night", NIGHT)):
        bgs[name] = img.copy()

    for sp in SPECS:
        x, y, w, h = sp["bbox"]
        m1, m2, m3, m4 = sp.get("margin", (6, 6, 6, 6))
        # jendela potong = bbox + margin bayangan (bayangan jatuh ikut aset)
        bx0, by0 = max(0, x - m1), max(0, y - m2)
        bx1, by1 = min(W, x + w + m3), min(H, y + h + m4)
        bw, bh = bx1 - bx0, by1 - by0
        cuts = {}
        masks = {}
        for name, img in (("day", DAY), ("night", NIGHT)):
            a = feather(object_alpha(img, (bx0, by0, bw, bh), sp.get("force")))
            for tx, ty, tw, th in sp.get("tanpa", []):
                a[max(0, ty - by0):ty - by0 + th, max(0, tx - bx0):tx - bx0 + tw] = 0
            rgba = np.dstack([img[by0:by1, bx0:bx1], a])
            cuts[name] = rgba
            masks[name] = a > 10
            # aset per-mode: <ID>.png = siang, <ID>-night.png = malam
            fname = f"{sp['id']}.png" if name == "day" else f"{sp['id']}-night.png"
            Image.fromarray(rgba, "RGBA").save(OBJ / fname)
        # warna dominan (dari mode day, area alfa penuh)
        px = cuts["day"][:, :, :3][cuts["day"][:, :, 3] > 220]
        pal = (
            Image.fromarray(px.reshape(-1, 1, 3)).quantize(colors=3).getpalette()[:9]
            if len(px) else []
        )
        warna = ["#%02X%02X%02X" % tuple(pal[i:i + 3]) for i in (0, 3, 6)] if pal else []
        # inpaint background di bekas objek (kecuali yang berakar di latar)
        if not sp["terpasang"]:
            for name in ("day", "night"):
                b = bgs[name]
                # inpaint rect penuh (diperluas 5px): menjamin tak ada jejak/hantu
                # objek (halo anti-alias dsb.) tersisa di latar
                mx0, my0 = bx0, by0
                mx1, my1 = bx1, by1
                pad = 40  # cincin sumber untuk inpaint
                px0, py0 = max(0, mx0 - pad), max(0, my0 - pad)
                px1, py1 = min(W, mx1 + pad), min(H, my1 + pad)
                sub = b[py0:py1, px0:px1]
                # mask = bentuk objek + halo 2px (bukan kotak penuh) supaya
                # area latar di sekitar objek tidak ikut termakan inpaint
                m = np.zeros(sub.shape[:2], np.uint8)
                am = (cuts[name][:, :, 3] > 2).astype(np.uint8) * 255
                am = cv2.dilate(am, np.ones((5, 5), np.uint8))
                m[my0 - py0:my0 - py0 + bh, mx0 - px0:mx0 - px0 + bw] = am
                b[py0:py1, px0:px1] = cv2.inpaint(sub, m, 7, cv2.INPAINT_NS)
        manifest["objek"].append(dict(
            id=sp["id"], nama=sp["nama"], layer=sp["layer"],
            bbox=dict(x=bx0, y=by0, w=bw, h=bh),
            ukuran_px=dict(panjang=bw, lebar=bh, tinggi="2D — ketebalan mengikuti perspektif gambar"),
            pivot=dict(x=bx0 + bw // 2, y=by0 + bh // 2),
            inti_bbox=dict(x=x, y=y, w=w, h=h),
            margin_bayangan=dict(kiri=m1, atas=m2, kanan=m3, bawah=m4),
            rotasi=0, opacity=1.0,
            sumber_mode=["day", "night"],
            warna_dominan=warna,
            terpasang_di_latar=sp["terpasang"],
            fungsi=sp["fungsi"],
            aset={"day": f"art/objects/{sp['id']}.png", "night": f"art/objects/{sp['id']}-night.png"},
            catatan=("akar (pot & tanah) menyatu dengan latar — pindah objek = pindah ikut area latar"
                     if sp["terpasang"] else
                     "bayangan jatuh ikut aset (margin); bekas objek di-repair pada background (inpaint)"),
        ))
        print(f"{sp['id']:18s} {bw}x{bh} layer={sp['layer']} warna={warna[:2]}")

    (OBJ / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False))
    for name, b in bgs.items():
        Image.fromarray(b).save(OBJ / f"background_{name}.png")
    print(f"manifest: {len(manifest['objek'])} objek -> {OBJ/'manifest.json'}")


if __name__ == "__main__":
    main()
