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


def adb_resize(w: int, h: int) -> str:
    return f"adb shell wm size {w}x{h}"


def screen_size():
    out = sh("adb shell wm size")  # Physical size: 2560x1600
    m = re.search(r"(\d+)x(\d+)", out)
    return (int(m.group(1)), int(m.group(2))) if m else (2560, 1600)


def scene_rect(sw: int, sh: int, scene_w=1312, scene_h=816):
    """Letterbox: gambar 16:10 di tengah layar, sisanya hitam."""
    scale = min(sw / scene_w, sh / scene_h)
    bw, bh = scene_w * scale, scene_h * scale
    return (sw - bw) / 2, (sh - bh) / 2, scale


def tap_scene(px: int, py: int, sw: int, sh: int, wait: float = 0.25) -> None:
    ox, oy, scale = scene_rect(sw, sh)
    tap(int(ox + px * scale), int(oy + py * scale), wait=wait)


def main() -> None:
    w, h = screen_size()
    print("ukuran layar", w, h, flush=True)

    # 1. launcher bersih: hanya gambar workspace
    launch()
    shot("launcher")
    crashes()

    # 2. ketuk objek di scene. Koordinat memakai piksel gambar 1312 x 816
    #    (sama dengan TILES di Data.kt), lalu dipetakan ke piksel layar.
    hotspots = [
        ("launcher-ketuk-jurnal", 615, 350),     # laptop
        ("launcher-ketuk-kalkulator", 752, 441), # kalkulator, di kanan laptop
        ("launcher-ketuk-kalender", 898, 426),   # kalender meja (tanggal hidup)
        ("launcher-ketuk-binder", 655, 565),     # laci
        ("launcher-ketuk-profil", 367, 480),     # kursi
    ]
    for name, px, py in hotspots:
        ox, oy, scale = scene_rect(w, h)
        print("ketuk", name, "layar", int(ox + px * scale), int(oy + py * scale), flush=True)
        tap_scene(px, py, w, h)
        shot(name)
        time.sleep(2.4)

    # 3. layar dengan rasio lain: gambar harus di-letterbox, ketukan tetap kena
    sh(adb_resize(w, 1200))
    time.sleep(2)
    launch()
    shot("launcher-letterbox")
    tap_scene(615, 350, w, 1200)   # laptop
    shot("launcher-letterbox-ketuk")
    time.sleep(2)
    sh("adb shell wm size reset")
    sh("adb shell wm density reset")
    time.sleep(1)

    # 4. bukti tidak ada rail/header: teks di layar hanya label sementara
    labels = [n["text"] for n in nodes() if n["text"]]
    print("teks di layar:", labels, flush=True)

    crashes()


if __name__ == "__main__":
    main()
