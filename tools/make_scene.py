"""Siapkan gambar workspace isometrik untuk launcher Android.

Gambar dipakai apa adanya (1312 × 816) supaya koordinat di Data.kt sama persis
dengan piksel gambar — tanpa padding, tanpa penskalaan. Muka jam dibersihkan dan
tanda jamnya digambar ulang oleh `tools/scene_face.py` (jarumnya digambar aplikasi).

  python3 tools/make_scene.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import scene_face  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "android/app/src/main/res/drawable-nodpi/scene.png"


def main() -> None:
    im, cream = scene_face.load_scene()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    im.save(OUT, optimize=True)
    print("scene", im.size, "->", OUT.relative_to(ROOT))
    print("CLOCK_FACE   ", scene_face.clock_face(im), "#%02X%02X%02X" % cream)
    print("CALENDAR_FACE", scene_face.calendar_face(im))


if __name__ == "__main__":
    main()
