"""Bangun preview browser untuk launcher.

Sumber kebenaran tetap Data.kt (TILES, CLOCK_FACE, CALENDAR_FACE, SCENE_W/H) supaya preview di
browser punya area ketuk yang sama persis dengan APK Android.

  python3 tools/make_preview.py   ->  web/index.html + web/scene.png
"""
import hashlib
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

    icons = []
    for name, bg, body in re.findall(
        r'AppIcon\("([^"]+)",\s*"([^"]+)",\s*listOf\((.*?)\)\)', src, re.S
    ):
        shapes = [{"color": c, "points": [float(v) for v in pts.replace(",", " ").split()]}
                  for c, pts in re.findall(r'shape\("([^"]+)",\s*"([^"]+)"\)', body)]
        icons.append({"name": name, "bg": bg, "shapes": shapes})

    return (scene_w, scene_h, tiles, rect("CLOCK_FACE"), rect("CALENDAR_QUAD"),
            rect("LED_QUAD"), rect("WINDOW_QUAD"),
            [[int(v) for v in q.split(",")] for q in re.findall(
                r'intArrayOf\(([\d,\s]+)\)', s_after(src, "val FRAME_QUADS"))],
            icons)


def s_after(src, marker):
    """Ambil potongan sumber setelah tanda tertentu (untuk blok FRAME_QUADS)."""
    i = src.index(marker)
    return src[i:src.index(")\n\n", i) + 1]


def icon_svg(frames, icons, scene_w, scene_h):
    """Ikon bingkai sebagai poligon SVG, langsung dalam koordinat scene."""
    out = []
    for frame, icon in zip(frames, icons):
        ax, ay, bx, by, cx, cy = frame
        ux, uy = bx - ax, by - ay
        vx, vy = cx - ax, cy - ay

        def to_scene(u, v):
            return (ax + ux * u + vx * v, ay + uy * u + vy * v)

        corners = [to_scene(0, 0), to_scene(1, 0), to_scene(1, 1), to_scene(0, 1)]
        out.append('<polygon points="%s" fill="%s"/>' % (
            " ".join(f"{x:.2f},{y:.2f}" for x, y in corners), icon["bg"]))
        for part in icon["shapes"]:
            pts = part["points"]
            pairs = [(pts[i], pts[i + 1]) for i in range(0, len(pts) - 1, 2)]
            out.append('<polygon points="%s" fill="%s"/>' % (
                " ".join(f"{x:.2f},{y:.2f}" for x, y in (to_scene(u, v) for u, v in pairs)),
                part["color"]))
    return "\n    ".join(out)


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
<meta http-equiv="Cache-Control" content="no-store, no-cache, must-revalidate">
<meta http-equiv="Pragma" content="no-cache">
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

  /* kertas kalender: tulisan menempel pada bidang miringnya */
  #cal{position:absolute;left:0;top:0;transform-origin:0 0;display:flex;
    flex-direction:column;align-items:center;justify-content:center;
    color:#181430;font-weight:700;line-height:.95;letter-spacing:.5px;
    pointer-events:none;white-space:nowrap}
  #cal .m,#cal .w{opacity:.9}

  /* layar jam digital: digit menempel pada bidang miring panel, tanpa kotak */
  #led{position:absolute;left:0;top:0;transform-origin:0 0;display:flex;
    align-items:center;justify-content:center;background:transparent;
    color:#D7F7CD;font-weight:700;line-height:1;letter-spacing:0;
    pointer-events:none;white-space:nowrap;overflow:hidden}

  /* lapisan vektor: ikon bingkai + gorden baterai, dalam koordinat scene */
  #fx{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}

  /* selaput suasana: pagi hangat, malam kebiruan */
  #tint{position:absolute;inset:0;pointer-events:none}

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
  <img id="scene" src="scene.png?v=__HASH__" alt="Workspace guru malam hari" draggable="false">
  <div id="clock" role="img" aria-label="Jam analog">
    <svg viewBox="0 0 100 100" preserveAspectRatio="none">
      <g stroke="#0E0D1A" stroke-linecap="round">
        <line id="hh" x1="50" y1="50" x2="50" y2="26" stroke-width="8"></line>
        <line id="mh" x1="50" y1="50" x2="50" y2="14" stroke-width="5.5"></line>
        <line id="sh" x1="50" y1="50" x2="50" y2="6" stroke-width="2.2" stroke="#EF6A5B"></line>
      </g>
      <circle cx="50" cy="50" r="4.5" fill="#0E0D1A"></circle>
    </svg>
  </div>
  <div id="cal" role="img" aria-label="Kalender hari ini">
    <span class="m" id="cal-m">OKT</span>
    <span class="d" id="cal-d">26</span>
    <span class="w" id="cal-w">SEN</span>
  </div>
  <div id="led" role="img" aria-label="Jam digital"><span id="led-t">00:00:00</span></div>
  <svg id="fx" viewBox="0 0 __SCENE_W__ __SCENE_H__" preserveAspectRatio="none">
    __ICONS__
    <polygon id="curtain" points="" fill="#8F6A72" fill-opacity=".95"/>
    <g id="curtain-lines" stroke="#000" stroke-opacity=".2" stroke-width="1"></g>
  </svg>
  <div id="tint"></div>
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
const CAL_QUAD = __CALQUAD__;
const LED_QUAD = __LEDQUAD__;
const WINDOW_QUAD = __WINDOWQUAD__;
const TILES = __TILES__;

const stage = document.getElementById("stage");
const clockEl = document.getElementById("clock");
const calEl = document.getElementById("cal");
const ledEl = document.getElementById("led");
const ledText = document.getElementById("led-t");
const curtainEl = document.getElementById("curtain");
const curtainLines = document.getElementById("curtain-lines");
const tintEl = document.getElementById("tint");
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
clockEl.addEventListener("click", (e) => { e.stopPropagation(); open("Jadwal"); });

// tempelkan sebuah elemen pada bidang miring: quad = [kiri-atas, kanan-atas, kiri-bawah]
function placeOnPlane(el, quad, u) {
  const [ax, ay, bx, by, cx, cy] = quad;
  const W = Math.hypot(bx - ax, by - ay);
  const H = Math.hypot(cx - ax, cy - ay);
  el.style.width = (u * W).toFixed(2) + "px";
  el.style.height = (u * H).toFixed(2) + "px";
  el.style.left = (u * ax).toFixed(2) + "px";
  el.style.top = (u * ay).toFixed(2) + "px";
  const a = (bx - ax) / W, b = (by - ay) / W, c = (cx - ax) / H, d = (cy - ay) / H;
  el.style.transform = "matrix(" + [a, b, c, d, 0, 0].join(",") + ")";
  return H;
}

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
  document.getElementById("sh")
    .setAttribute("transform", "rotate(" + (now.getSeconds() * 6) + " 50 50)");
  document.getElementById("cal-m").textContent = BULAN[now.getMonth()];
  document.getElementById("cal-d").textContent = now.getDate();
  document.getElementById("cal-w").textContent = HARI[(now.getDay() + 6) % 7];
  ledText.textContent =
    String(now.getHours()).padStart(2, "0") + ":" +
    String(now.getMinutes()).padStart(2, "0") + ":" +
    String(now.getSeconds()).padStart(2, "0");
  tintEl.style.background = ambience(now.getHours());
  drawCurtain();
}

/* ---- gorden jendela = penunjuk baterai ---- */
let batteryLevel = 100;

function drawCurtain() {
  const [ax, ay, bx, by, cx, cy, dx, dy] = WINDOW_QUAD;
  const cover = Math.max(0, Math.min(1, (100 - batteryLevel) / 100));
  if (cover <= 0.001) {
    curtainEl.setAttribute("points", "");
    curtainLines.innerHTML = "";
    return;
  }
  const blx = ax + (cx - ax) * cover, bly = ay + (cy - ay) * cover;
  const brx = bx + (dx - bx) * cover, bry = by + (dy - by) * cover;
  curtainEl.setAttribute("points",
    [ax, ay, bx, by, brx, bry, blx, bly].map((v) => v.toFixed(1)).join(" "));
  curtainEl.setAttribute("fill", batteryLevel <= 15 ? "#8E4A4E" : batteryLevel <= 40 ? "#9A6567" : "#A9888A");
  let g = "";
  // lipatan kain: pita gelap-terang bergantian
  for (let i = 0; i < 5; i += 2) {
    const t0 = i / 5, t1 = (i + 1) / 5;
    const p1 = [ax + (bx - ax) * t0, ay + (by - ay) * t0];
    const p2 = [ax + (bx - ax) * t1, ay + (by - ay) * t1];
    const p3 = [blx + (brx - blx) * t1, bly + (bry - bly) * t1];
    const p4 = [blx + (brx - blx) * t0, bly + (bry - bly) * t0];
    g += '<polygon points="' + [p1, p2, p3, p4].map((p) => p.map((v) => v.toFixed(1)).join(",")).join(" ") +
         '" fill="#000" fill-opacity=".10"/>';
  }
  for (let i = 1; i <= 5; i++) {
    const t = i / 6;
    const x1 = ax + (bx - ax) * t, y1 = ay + (by - ay) * t;
    const x2 = blx + (brx - blx) * t, y2 = bly + (bry - bly) * t;
    g += '<line x1="' + x1.toFixed(1) + '" y1="' + y1.toFixed(1) +
         '" x2="' + x2.toFixed(1) + '" y2="' + y2.toFixed(1) + '"/>';
  }
  curtainLines.innerHTML = g;
}

if (navigator.getBattery) {
  navigator.getBattery().then((b) => {
    const sync = () => { batteryLevel = Math.round(b.level * 100); drawCurtain(); };
    sync();
    b.addEventListener("levelchange", sync);
  }).catch(() => {});
}

// suasana mengikuti jam: sama seperti ambienceOf() di Launcher.kt
function ambience(h) {
  if (h >= 4 && h <= 5) return "rgba(110,90,168,.10)";
  if (h >= 6 && h <= 9) return "rgba(255,196,107,.06)";
  if (h >= 10 && h <= 14) return "rgba(255,242,208,.03)";
  if (h >= 15 && h <= 17) return "rgba(255,138,76,.10)";
  if (h >= 18 && h <= 21) return "rgba(43,63,143,.12)";
  return "rgba(6,10,34,.34)";
}
tick();
setInterval(tick, 1000);

// ukuran bidang & huruf mengikuti `unit` panggung, sama seperti di APK
function fitFaces() {
  const u = (stage.clientWidth || SCENE.w) / SCENE.w;
  const calH = placeOnPlane(calEl, CAL_QUAD, u);
  const ledH = placeOnPlane(ledEl, LED_QUAD, u);
  const calFont = (f) => (calH * u * f).toFixed(2) + "px";
  calEl.querySelector(".m").style.fontSize = calFont(0.22);
  calEl.querySelector(".d").style.fontSize = calFont(0.46);
  calEl.querySelector(".w").style.fontSize = calFont(0.22);
  const ledW = parseFloat(ledEl.style.width);
  ledEl.style.fontSize = Math.min(ledH * u * 0.86, (ledW * 0.92) / 5.0).toFixed(2) + "px";
}
fitFaces();
window.addEventListener("resize", fitFaces);
if (window.ResizeObserver) new ResizeObserver(fitFaces).observe(stage);

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
    scene_w, scene_h, tiles, clock, cal_quad, led_rect, window_quad, frames, icons = parse_data()
    size = make_scene(scene_w, scene_h)
    scene_hash = hashlib.md5((WEB / "scene.png").read_bytes()).hexdigest()[:10]

    html = (
        HTML.replace("__HASH__", scene_hash)
        .replace("__RATIO__", f"{scene_w} / {scene_h}")
        .replace("__SCENE_W__", str(scene_w))
        .replace("__SCENE_H__", str(scene_h))
        .replace("__CLOCK__", str(clock).replace(" ", ""))
        .replace("__CALQUAD__", str(cal_quad).replace(" ", ""))
        .replace("__LEDQUAD__", str(led_rect).replace(" ", ""))
        .replace("__WINDOWQUAD__", str(window_quad).replace(" ", ""))
        .replace("__ICONS__", icon_svg(frames, icons, scene_w, scene_h))
        .replace("__TILES__", "[\n" + "".join(
            f'  {{label: "{t["label"]}", x: {t["x"]}, y: {t["y"]}, w: {t["w"]}, h: {t["h"]}}},\n'
            for t in tiles
        ) + "]")
    )
    (WEB / "index.html").write_text(html)
    print(f"scene {size} · {len(tiles)} objek · {WEB/'index.html'}")


if __name__ == "__main__":
    main()
