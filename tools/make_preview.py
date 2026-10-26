"""Bangun preview browser untuk launcher.

Sumber kebenaran tetap Data.kt (TILES, CLOCK_FACE, CALENDAR_FACE, SCENE_W/H) supaya preview di
browser punya area ketuk yang sama persis dengan APK Android.

  python3 tools/make_preview.py   ->  web/index.html + web/scene.png
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import scene_face  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "android/app/src/main/java/dev/retrobit/ruangkelas/Data.kt"
ART = ROOT / "art/workspace-iso-v2.png"
WEB = ROOT / "web"


def parse_data():
    src = DATA.read_text()

    scene_w = int(re.search(r"const val SCENE_W = (\d+)", src).group(1))
    scene_h = int(re.search(r"const val SCENE_H = (\d+)", src).group(1))

    tiles = []
    for label, x, y, w, h in re.findall(
        r'TileInfo\("([^"]+)",\s*Scr\.\w+,\s*"[^"]*",\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)',
        src,
    ):
        tiles.append({"label": label, "x": int(x), "y": int(y), "w": int(w), "h": int(h)})

    def rect(name):
        return [int(v) for v in re.search(
            rf"val {name} = intArrayOf\(([^)]+)\)", src).group(1).split(",")
        ]

    return scene_w, scene_h, tiles, rect("CLOCK_FACE"), rect("CALENDAR_FACE")


def make_scene(scene_w, scene_h):
    """Art sudah berukuran persis SCENE_W × SCENE_H — tanpa padding, tanpa skala."""
    im, _ = scene_face.load_scene()
    if im.size != (scene_w, scene_h):
        raise SystemExit(f"art {im.size} != scene {(scene_w, scene_h)}")
    WEB.mkdir(exist_ok=True)
    im.save(WEB / "scene.png", optimize=True)
    return im.size


HTML = """<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#15142A">
<title>Ruang Kelas</title>
<style>
  :root{
    --ink:#0E0D1A; --night:#15142A;
    --cream2:#E3CDA6; --amber:#F2AB3F; --screen:#D7F7CD;
    --ratio: __RATIO__;
  }
  *{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
  html,body{margin:0;height:100%;overflow:hidden;background:var(--ink)}
  body{touch-action:manipulation;user-select:none;
    font-family:ui-monospace,"SF Mono",Menlo,Consolas,monospace}

  /* letterbox: gambar selalu 16:10, sisa layar hitam — sama seperti APK */
  #stage{
    position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);
    aspect-ratio:var(--ratio);
    width:min(100vw, calc(100vh * var(--ratio)));
    max-height:100vh;
  }
  #scene{position:absolute;inset:0;width:100%;height:100%;display:block;
    image-rendering:pixelated;pointer-events:none}

  .hotspot{position:absolute;border:0;padding:0;margin:0;background:transparent;
    cursor:pointer;outline:none;border-radius:0}
  .hotspot.is-pressed{
    background:rgba(242,171,63,.20);
    box-shadow:0 0 0 3px var(--amber) inset;
  }

  /* jam analog hidup di dinding: SVG di dalam kotak muka jam */
  #clock{position:absolute;cursor:pointer}
  #clock svg{display:block;width:100%;height:100%;overflow:visible}

  /* kalender meja: bulan / tanggal / hari, mengikuti tanggal hari ini */
  #cal{position:absolute;display:flex;flex-direction:column;align-items:center;
    justify-content:center;gap:0;color:#0E0D1A;cursor:pointer;
    font-weight:700;line-height:1;letter-spacing:.5px;overflow:hidden}
  #cal .m,#cal .w{opacity:.85}
  #cal .d{font-weight:700;letter-spacing:0}

  #scan{position:absolute;inset:0;pointer-events:none;
    background:repeating-linear-gradient(180deg,rgba(0,0,0,.10) 0 1px,transparent 1px 3px)}

  #toast{position:absolute;left:50%;bottom:26px;transform:translate(-50%,8px);
    padding:8px 14px;background:var(--night);border:2px solid var(--amber);
    color:var(--cream2);font-size:12px;letter-spacing:1px;white-space:nowrap;
    opacity:0;transition:opacity .18s ease,transform .18s ease;pointer-events:none}
  #toast.show{opacity:1;transform:translate(-50%,0)}

  /* hanya muncul kalau jendela masih tegak */
  #rotate{
    position:fixed;inset:0;display:none;flex-direction:column;
    align-items:center;justify-content:center;gap:18px;
    background:var(--ink);color:var(--cream2);
    font-size:12px;letter-spacing:2px;text-align:center;padding:24px;z-index:10}
  #rotate .icon{
    width:56px;height:56px;border:3px solid var(--amber);
    border-radius:8px;position:relative}
  #rotate .icon::after{
    content:"";position:absolute;inset:8px 16px;
    border:3px solid var(--amber);border-radius:4px}
  #rotate small{color:#7D75A0;letter-spacing:1px;font-size:11px;line-height:1.7}
  @media (orientation:portrait){
    #rotate{display:flex}
  }
</style>
</head>
<body>
<div id="stage">
  <img id="scene" src="scene.png" alt="Workspace guru malam hari" draggable="false">
  <div id="clock" role="img" aria-label="Jam analog">
    <svg viewBox="0 0 100 100" preserveAspectRatio="none">
      <g stroke="#0E0D1A" stroke-linecap="round">
        <line id="hh" x1="50" y1="50" x2="50" y2="26" stroke-width="8"></line>
        <line id="mh" x1="50" y1="50" x2="50" y2="14" stroke-width="5.5"></line>
      </g>
      <circle cx="50" cy="50" r="4.5" fill="#0E0D1A"></circle>
    </svg>
  </div>
  <div id="cal" role="img" aria-label="Kalender hari ini">
    <span class="m" id="cal-m">OKT</span>
    <span class="d" id="cal-d">26</span>
    <span class="w" id="cal-w">SEN</span>
  </div>
  <div id="scan"></div>
  <div id="toast" role="status" aria-live="polite"></div>
</div>

<div id="rotate">
  <div class="icon"></div>
  <div>PUTAR PERANGKAT KE LANDSCAPE</div>
  <small>Launcher ini hanya berjalan mendatar,<br>sama seperti di tablet.</small>
</div>

<script>
const SCENE = {w: __SCENE_W__, h: __SCENE_H__};
const CLOCK_FACE = __CLOCK__;
const CALENDAR_FACE = __CALENDAR__;
const TILES = __TILES__;

const stage = document.getElementById("stage");
const clockEl = document.getElementById("clock");
const calEl = document.getElementById("cal");
const toast = document.getElementById("toast");
let toastTimer = 0;

function pct(v, total) { return (v / total * 100).toFixed(4) + "%"; }

function place(el, x, y, w, h) {
  el.style.left = pct(x, SCENE.w);
  el.style.top = pct(y, SCENE.h);
  el.style.width = pct(w, SCENE.w);
  el.style.height = pct(h, SCENE.h);
}

function showToast(name) {
  toast.textContent = name.toUpperCase() + " \u00b7 SEGERA HADIR";
  toast.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 2000);
}

function open(name) { showToast(name); }

TILES.forEach((tile) => {
  const el = document.createElement("button");
  el.className = "hotspot";
  el.type = "button";
  el.dataset.label = tile.label;
  el.setAttribute("aria-label", tile.label);
  place(el, tile.x, tile.y, tile.w, tile.h);
  el.addEventListener("pointerdown", () => el.classList.add("is-pressed"));
  ["pointerup", "pointercancel", "pointerleave"].forEach((ev) =>
    el.addEventListener(ev, () => el.classList.remove("is-pressed")));
  el.addEventListener("click", (e) => { e.stopPropagation(); open(tile.label); });
  stage.appendChild(el);
});

place(clockEl, CLOCK_FACE[0], CLOCK_FACE[1], CLOCK_FACE[2], CLOCK_FACE[3]);
place(calEl, CALENDAR_FACE[0], CALENDAR_FACE[1], CALENDAR_FACE[2], CALENDAR_FACE[3]);
clockEl.addEventListener("click", (e) => { e.stopPropagation(); open("Jadwal"); });
calEl.addEventListener("click", (e) => { e.stopPropagation(); open("Kalender"); });

// muka jam (krem + 12 tanda) sudah tergambar di scene.png;
// di sini hanya jarum jam & menit yang diputar sesuai waktu perangkat
const BULAN = ["JAN", "FEB", "MAR", "APR", "MEI", "JUN",
               "JUL", "AGU", "SEP", "OKT", "NOV", "DES"];
const HARI = ["SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN"];

// jarum jam & menit mengikuti waktu perangkat; kalender mengikuti tanggal hari ini
function tick() {
  const now = new Date();
  const minute = now.getMinutes() + now.getSeconds() / 60;
  const hour = (now.getHours() % 12) + minute / 60;
  document.getElementById("hh")
    .setAttribute("transform", "rotate(" + (hour * 30) + " 50 50)");
  document.getElementById("mh")
    .setAttribute("transform", "rotate(" + (minute * 6) + " 50 50)");
  document.getElementById("cal-m").textContent = BULAN[now.getMonth()];
  document.getElementById("cal-d").textContent = now.getDate();
  document.getElementById("cal-w").textContent = HARI[(now.getDay() + 6) % 7];
}
tick();
setInterval(tick, 1000);

// ukuran teks kalender mengikuti `unit` panggung, sama seperti di APK
function fitClock() {
  const u = (stage.clientWidth || SCENE.w) / SCENE.w;
  const px = (v) => Math.max(4, u * v).toFixed(2) + "px";
  calEl.style.fontSize = px(7);
  const d = calEl.querySelector(".d");
  d.style.fontSize = px(20);
}
fitClock();
window.addEventListener("resize", fitClock);
if (window.ResizeObserver) new ResizeObserver(fitClock).observe(stage);

// minta browser mengunci landscape (butuh fullscreen; kalau ditolak, abaikan saja)
function lockLandscape() {
  try {
    if (screen.orientation && screen.orientation.lock) {
      screen.orientation.lock("landscape").catch(() => {});
    }
  } catch (_) {}
}
document.addEventListener("pointerdown", lockLandscape, { once: true });
</script>
</body>
</html>
"""


def main() -> None:
    scene_w, scene_h, tiles, clock, calendar = parse_data()
    size = make_scene(scene_w, scene_h)

    html = (
        HTML.replace("__RATIO__", f"{scene_w} / {scene_h}")
        .replace("__SCENE_W__", str(scene_w))
        .replace("__SCENE_H__", str(scene_h))
        .replace("__CLOCK__", str(clock).replace(" ", ""))
        .replace("__CALENDAR__", str(calendar).replace(" ", ""))
        .replace("__TILES__", "[\n" + "".join(
            f'  {{label: "{t["label"]}", x: {t["x"]}, y: {t["y"]}, w: {t["w"]}, h: {t["h"]}}},\n'
            for t in tiles
        ) + "]")
    )
    (WEB / "index.html").write_text(html)
    print(f"scene {size} · {len(tiles)} objek · {WEB/'index.html'}")


if __name__ == "__main__":
    main()
