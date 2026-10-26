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


OUT_DAY = ROOT / "android/app/src/main/res/drawable-nodpi/scene_day.png"
OUT_NIGHT = ROOT / "android/app/src/main/res/drawable-nodpi/scene_night.png"


def main() -> None:
    """Tulis dua versi suasana: langit malam dan langit siang."""
    for mode, out in (("night", OUT_NIGHT), ("day", OUT_DAY)):
        im = scene_face.load_scene(mode)
        out.parent.mkdir(parents=True, exist_ok=True)
        im.save(out, optimize=True)
        print(f"{mode:5s} {im.size} -> {out.relative_to(ROOT)}")
    print("CALENDAR_FACE", scene_face.calendar_face())


if __name__ == "__main__":
    main()
