"""Uji interaktif APK di emulator: buka dialog, isi form, pakai kalkulator, ubah absensi.

Dijalankan di GitHub Actions setelah emulator boot dan APK terpasang.
Semua langkah memakai uiautomator (bukan koordinat tetap) supaya tahan perubahan layout.
"""
import re
import subprocess
import time

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


def tap(x: int, y: int, wait: float = 1.0) -> None:
    sh(f"adb shell input tap {x} {y}")
    time.sleep(wait)


def tap_text(label: str, nth: int = 0) -> bool:
    ns = nodes()
    hits = [n for n in ns if n["text"].lower() == label.lower()]
    if not hits:
        hits = [n for n in ns if label.lower() in n["text"].lower()]
    print(f"tap_text {label!r} hits={len(hits)}", flush=True)
    if len(hits) <= nth:
        print("  NOT FOUND; teks tersedia:", [n["text"] for n in ns if n["text"]][:40], flush=True)
        return False
    tap(*hits[nth]["c"])
    return True


def tap_cls(kind: str, nth: int = 0) -> bool:
    ns = [n for n in nodes() if kind in n["class"]]
    print(f"tap_cls {kind} hits={len(ns)}", flush=True)
    if len(ns) <= nth:
        return False
    tap(*ns[nth]["c"])
    return True


def shot(name: str) -> None:
    sh(f"adb shell screencap -p /sdcard/{name}.png")
    print(sh(f"adb pull /sdcard/{name}.png docs/screens/{name}.png").strip(), flush=True)


def launch(screen: str) -> None:
    sh("adb shell am force-stop dev.retrobit.ruangkelas")
    sh("adb logcat -c")
    sh(f"adb shell am start -W -n dev.retrobit.ruangkelas/.MainActivity --es screen {screen}")
    time.sleep(4)


def crashes() -> None:
    out = sh("adb logcat -d -b crash")
    if out.strip():
        print("--- CRASH LOG ---", flush=True)
        print(out[:6000], flush=True)
    else:
        print("tidak ada crash", flush=True)


def main() -> None:
    # 1. Jurnal: dialog -> isi materi -> simpan
    launch("JURNAL")
    tap_text("CATAT PERTEMUAN")
    shot("aksi-dialog-jurnal")
    tap_cls("EditText", 0)
    sh('adb shell input text "Sistem%sPencernaan%sManusia"')
    time.sleep(1)
    shot("aksi-form-jurnal")
    tap_text("SIMPAN", 0)
    time.sleep(1.5)
    shot("aksi-jurnal-baru")
    crashes()

    # 2. Kalkulator: 78 + 85 x 2
    launch("KALKULATOR")
    for key in ["2", "5", "×", "3", "6", "−", "4", "0"]:
        print("key", key, tap_text(key), flush=True)
    shot("aksi-kalkulator")
    crashes()

    # 3. Absensi: tandai sakit dua siswa
    launch("ABSENSI")
    tap_text("S", 0)
    tap_text("S", 1)
    shot("aksi-absensi")
    crashes()


if __name__ == "__main__":
    main()
