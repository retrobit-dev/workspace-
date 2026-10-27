"""Regenerasi dokumen docs/screens dari komposit sim_overlay.

Shot aplikasi asli (emulator) tidak selalu tersedia; komposit sim memakai
scene + overlay yang sama dengan render aplikasi (sumber koordinat Data.kt),
jadi dokumennya representatif. File UI sistem (file manager) tidak disentuh.

  python3 tools/regen_docs.py
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
import sim_overlay  # noqa: E402

SCREENS = ROOT / "docs" / "screens"
FONT = ROOT / "android/app/src/main/res/font/silkscreen.ttf"
FONT_BOLD = ROOT / "android/app/src/main/res/font/silkscreen_bold.ttf"

INK = (14, 13, 26)
NIGHT = (21, 20, 42)
AMBER = (242, 171, 63)
CREAM2 = (227, 205, 166)
MUTED = (125, 117, 160)

TILES = {  # scene coords dari Data.kt (kanvas 1280×800)
    "jurnal": (766, 296, 178, 172),
    "jadwal": (340, 168, 216, 286),
    "kalkulator": (886, 412, 96, 64),
    "binder": (836, 494, 128, 116),
    "profil": (694, 30, 74, 140),
}


def base(mode: str, battery: int = 100) -> Image.Image:
    """Komposit scene + overlay (tanpa toast/highlight)."""
    return sim_overlay.build(mode, 11 if mode == "day" else 21, battery=battery).convert("RGB")


def scale2(im: Image.Image, w: int = 2624, h: int = 1632) -> Image.Image:
    return im.resize((w, h), Image.NEAREST)


def toast(im: Image.Image, text: str) -> None:
    """Kapsul caption di bawah-tengah: bg Night, bingkai amber, teks silkscreen."""
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(str(FONT), 16)
    pad_x, pad_y, border = 54, 15, 4
    bbox = d.textbbox((0, 0), text, font=f)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    bottom = im.height - 52
    x0 = (im.width - tw) // 2 - pad_x
    y1 = bottom
    y0 = y1 - (th + 2 * pad_y + 2 * border)
    x1 = x0 + tw + 2 * pad_x + 2 * border
    d.rectangle([x0, y0, x1, y1], fill=NIGHT, outline=AMBER, width=border)
    d.text(((im.width - tw) // 2, y0 + pad_y + border - bbox[1]), text, font=f, fill=CREAM2)


def highlight(im: Image.Image, key: str) -> None:
    x, y, w, h = TILES[key]
    d = ImageDraw.Draw(im)
    d.rectangle([x * 2 - 5, y * 2 - 5, (x + w) * 2 + 4, (y + h) * 2 + 4], outline=AMBER, width=5)


def save(im: Image.Image, name: str) -> None:
    out = SCREENS / name
    im.save(out, optimize=True)
    print(f"{name:44s} {im.size}")


def launcher_shots() -> None:
    night = scale2(base("night", 100))
    save(night, "launcher.png")
    save(scale2(base("night", 55)), "launcher-kembali-dari-sampah.png")

    day = scale2(base("day", 100))

    im = day.copy(); highlight(im, "jurnal"); toast(im, "JURNAL"); save(im, "launcher-ketuk-jurnal.png")
    im = night.copy(); highlight(im, "jadwal"); toast(im, "JADWAL"); save(im, "launcher-ketuk-jadwal.png")
    im = night.copy(); highlight(im, "kalkulator"); toast(im, "KALKULATOR"); save(im, "launcher-ketuk-kalkulator.png")
    im = night.copy(); highlight(im, "binder"); toast(im, "BINDER"); save(im, "launcher-ketuk-binder.png")
    im = night.copy(); highlight(im, "profil"); toast(im, "PROFIL"); save(im, "launcher-ketuk-profil.png")
    # letterbox: perangkat 2.133:1 — scene penuh lebar, atas/bawah terpotong
    lb = scale2(base("night", 100)).crop((0, 200, 2624, 1432))
    save(lb, "launcher-letterbox.png")
    lb2 = lb.copy(); toast(lb2, "JURNAL"); save(lb2, "launcher-letterbox-ketuk.png")


def battery_panel() -> None:
    """Tiga keadaan rel gorden (siang) berdampingan dengan labelnya."""
    W, H = 1006, 2192
    panel_w, band_h = 778, 539
    im = Image.new("RGB", (W, H), (19, 17, 34))
    d = ImageDraw.Draw(im)
    rows = [
        (100, "100%", "REL MINT (SIANG) ·", "SEMUA TANDA 10% ·", "PENUH", AMBER),
        (55, "55%", "KUNING: TANDAI", "MULAI HABIS", "", AMBER),
        (12, "12%", "MERAH: SEGERA", "ISI DAYA", "", (237, 106, 91)),
    ]
    y = 297
    for battery, pct, l1, l2, l3, color in rows:
        shot = base("day", battery).crop((880, 0, 1282, 278)).resize((panel_w, band_h), Image.NEAREST)
        im.paste(shot, (W - panel_w, y))
        d.text((20, y + 40), pct, font=ImageFont.truetype(str(FONT_BOLD), 30), fill=color)
        f = ImageFont.truetype(str(FONT_BOLD), 13)
        for i, line in enumerate((l1, l2, l3)):
            if line:
                d.text((20, y + 100 + i * 22), line, font=f, fill=MUTED)
        y += band_h + 129
    save(im, "baterai-gorden.png")


def main() -> None:
    SCREENS.mkdir(parents=True, exist_ok=True)
    launcher_shots()
    battery_panel()


if __name__ == "__main__":
    main()
