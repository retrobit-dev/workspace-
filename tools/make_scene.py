"""Siapkan gambar workspace isometrik untuk launcher.

- potong/pad ke rasio 16:10 (2560 x 1600 tablet)
- simpan ke android/app/src/main/res/drawable-nodpi/scene.png
- cetak ukuran supaya konstanta SCENE_W/SCENE_H di Data.kt bisa dicek
"""
from PIL import Image

SRC = "art/workspace-iso-raw.png"
OUT = "android/app/src/main/res/drawable-nodpi/scene.png"
TARGET_W, TARGET_H = 1312, 820  # 16:10


def main() -> None:
    im = Image.open(SRC).convert("RGB")
    w, h = im.size

    canvas = Image.new("RGB", (TARGET_W, TARGET_H))
    # tempel di tengah, pinggir diisi warna tepi supaya tidak ada garis aneh
    ox, oy = (TARGET_W - w) // 2, (TARGET_H - h) // 2
    canvas.paste(im, (ox, oy))

    if oy > 0:
        top = im.crop((0, 0, w, 1)).resize((TARGET_W, oy))
        bottom = im.crop((0, h - 1, w, h)).resize((TARGET_W, TARGET_H - oy - h))
        canvas.paste(top, (0, 0))
        canvas.paste(bottom, (0, oy + h))

    canvas.save(OUT, optimize=True)
    print("scene", canvas.size, "->", OUT)


if __name__ == "__main__":
    main()
