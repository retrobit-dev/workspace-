"""Bangun preview browser untuk launcher.

Sumber kebenaran tetap Data.kt (TILES, CLOCK_SCREEN, SCENE_W/H) supaya preview di
browser punya area ketuk yang sama persis dengan APK Android.

  python3 tools/make_preview.py   ->  web/index.html + web/scene.png
"""
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "android/app/src/main/java/dev/retrobit/ruangkelas/Data.kt"
ART = ROOT / "art/workspace-iso-raw.png"
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

    clock = [int(v) for v in re.search(
        r"val CLOCK_SCREEN = intArrayOf\(([^)]+)\)", src).group(1).split(",")
    ]
    return scene_w, scene_h, tiles, clock


def make_scene(scene_w, scene_h):
    im = Image.open(ART).convert("RGB")
    w, h = im.size
    canvas = Image.new("RGB", (scene_w, scene_h))
    ox, oy = (scene_w - w) // 2, (scene_h - h) // 2
    canvas.paste(im, (ox, oy))
    if oy > 0:
        canvas.paste(im.crop((0, 0, w, 1)).resize((scene_w, oy)), (0, 0))
        canvas.paste(im.crop((0, h - 1, w, h)).resize((scene_w, scene_h - oy - h)), (0, oy + h))
    WEB.mkdir(exist_ok=True)
    canvas.save(WEB / "scene.png", optimize=True)
    return canvas.size


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

  #clock{position:absolute;display:flex;align-items:center;justify-content:center;
    background:#0C1712;color:var(--screen);
    font-size:clamp(9px,1.05vw,16px);letter-spacing:1px;cursor:pointer}

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
  <div id="clock" aria-label="Jam digital"></div>
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
const CLOCK = __CLOCK__;
const TILES = __TILES__;

const stage = document.getElementById("stage");
const clockEl = document.getElementById("clock");
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

place(clockEl, CLOCK[0], CLOCK[1], CLOCK[2], CLOCK[3]);
clockEl.addEventListener("click", (e) => { e.stopPropagation(); open("Jadwal"); });

function tick() {
  const now = new Date();
  clockEl.textContent =
    String(now.getHours()).padStart(2, "0") + ":" + String(now.getMinutes()).padStart(2, "0");
}
tick();
setInterval(tick, 1000);

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
    scene_w, scene_h, tiles, clock = parse_data()
    size = make_scene(scene_w, scene_h)

    html = (
        HTML.replace("__RATIO__", f"{scene_w} / {scene_h}")
        .replace("__SCENE_W__", str(scene_w))
        .replace("__SCENE_H__", str(scene_h))
        .replace("__CLOCK__", str(clock).replace(" ", ""))
        .replace("__TILES__", "[\n" + "".join(
            f'  {{label: "{t["label"]}", x: {t["x"]}, y: {t["y"]}, w: {t["w"]}, h: {t["h"]}}},\n'
            for t in tiles
        ) + "]")
    )
    (WEB / "index.html").write_text(html)
    print(f"scene {size} · {len(tiles)} objek · {WEB/'index.html'}")


if __name__ == "__main__":
    main()
