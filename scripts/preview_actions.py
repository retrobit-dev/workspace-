"""Uji launcher di emulator: buka aplikasi, ketuk objek di scene, tangkap reaksinya.

Dijalankan di GitHub Actions setelah emulator boot dan APK terpasang.
"""
import re
import subprocess
import time

PACKAGE = "dev.retrobit.ruangkelas"
ACTIVITY = f"{PACKAGE}/.MainActivity"
NODE = re.compile(r"<node[^>]*>")


def sh(cmd: str) -> str:
    return subprocess.run(cmd, shell=True, capture_output=True, text=True).stdout


def nodes():
    sh("adb shell uiautomator dump /sdcard/ui.xml")
    sh("adb pull /sdcard/ui.xml /tmp/ui.xml")
    try:
        xml = open("/tmp/ui.xml").read()
    except FileNotFoundError:
        return []
    out = []
    for n in NODE.findall(xml):
        t = re.search(r'text="([^"]*)"', n)
        c = re.search(r'class="([^"]*)"', n)
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            out.append(
                {
                    "text": (t.group(1) if t else "").strip(),
                    "class": (c.group(1) if c else ""),
                    "c": ((x1 + x2) // 2, (y1 + y2) // 2),
                }
            )
    return out


def tap(x: int, y: int, wait: float = 0.7) -> None:
    sh(f"adb shell input tap {x} {y}")
    time.sleep(wait)


def shot(name: str) -> None:
    sh(f"adb shell screencap -p /sdcard/{name}.png")
    print(sh(f"adb pull /sdcard/{name}.png docs/screens/{name}.png").strip(), flush=True)


def launch() -> None:
    sh(f"adb shell am force-stop {PACKAGE}")
    sh("adb logcat -c")
    sh(f"adb shell am start -W -n {ACTIVITY}")
    time.sleep(4)


def crashes() -> None:
    out = sh("adb logcat -d -b crash")
    if out.strip():
        print("--- CRASH LOG ---", flush=True)
        print(out[:6000], flush=True)
    else:
        print("tidak ada crash", flush=True)


def screen_size():
    out = sh("adb shell wm size")  # Physical size: 2560x1600
    m = re.search(r"(\d+)x(\d+)", out)
    return (int(m.group(1)), int(m.group(2))) if m else (2560, 1600)


def main() -> None:
    w, h = screen_size()
    print("ukuran layar", w, h, flush=True)

    # 1. launcher bersih: hanya gambar workspace
    launch()
    shot("launcher")
    crashes()

    # 2. ketuk beberapa objek di scene (koordinat grid 51 x 32 -> piksel layar)
    hotspots = [
        ("launcher-ketuk-jurnal", 12, 15, 10, 6),
        ("launcher-ketuk-binder", 27, 24, 10, 8),
        ("launcher-ketuk-kalender", 31, 15, 6, 6),
        ("launcher-ketuk-profil", 43, 14, 8, 10),
    ]
    for name, gx, gy, gw, gh in hotspots:
        cx = int(w * (gx + gw / 2) / 51)
        cy = int(h * (gy + gh / 2) / 32)
        print("ketuk", name, cx, cy, flush=True)
        tap(cx, cy, wait=0.15)
        shot(name)
        time.sleep(2.5)

    # 3. bukti tidak ada rail/header: seluruh lebar layar berisi gambar
    labels = [n["text"] for n in nodes() if n["text"]]
    print("teks di layar:", labels, flush=True)

    crashes()


if __name__ == "__main__":
    main()
