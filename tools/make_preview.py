"""Bangun preview browser untuk launcher.

Sumber kebenaran tetap Data.kt (TILES, BOARD_QUAD,
RAIL_LINE, BATTERY_TEXT_QUAD, CURTAIN_RECTS, LEAF_RECT, SCENE_W/H) supaya
area ketuk di browser sama persis dengan APK.

  python3 tools/make_preview.py   ->  web/index.html + web/scene_day.png + web/scene_night.png

Siang/malam mengikuti jam browser: pukul 06–17 memakai scene_day.png, selebihnya
scene_night.png. Semua tulisan digambar di SVG dengan matriks bidang, jadi ikut
kemiringan benda seperti di APK.
"""
import hashlib
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import scene_face  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "android/app/src/main/java/dev/retrobit/ruangkelas/Data.kt"
WEB = ROOT / "web"


def _ints(text):
    return [int(v) for v in text.replace("\n", " ").split(",")]


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
        return _ints(re.search(rf"val {name} = intArrayOf\(([^)]*)\)", src).group(1))

    def list_of_quads(name):
        block = src[src.index(f"val {name} = listOf("):]
        block = block[: block.index("\n)\n")]
        return [_ints(m) for m in re.findall(r"intArrayOf\(([-\d,\s]+)\)", block)]

    return dict(
        w=scene_w,
        h=scene_h,
        tiles=tiles,
        board=rect("BOARD_QUAD"),
        rail=rect("RAIL_LINE"),
        curtains=list_of_quads("CURTAIN_RECTS"),
        leaf=rect("LEAF_RECT"),
    )


# ------------------------------------------------------------------ gambar scene

def make_scenes():
    WEB.mkdir(exist_ok=True)
    sizes = {}
    for mode in ("night", "day"):
        im = scene_face.load_scene(mode)
        im.save(WEB / f"scene_{mode}.png", optimize=True)
        sizes[mode] = im.size
    return sizes


# ------------------------------------------------------------------------- SVG

def plane_matrix(quad):
    """Matriks 2D yang memetakan kotak satuan ke bidang (kiri-atas, kanan-atas, kiri-bawah)."""
    ax, ay, bx, by, cx, cy = quad[:6]
    return ax, ay, bx - ax, by - ay, cx - ax, cy - ay


def matrix_attr(quad):
    ax, ay, ux, uy, vx, vy = plane_matrix(quad)
    return f"matrix({ux:.3f},{uy:.3f},{vx:.3f},{vy:.3f},{ax:.3f},{ay:.3f})"


def quad_polygon(quad):
    ax, ay, bx, by, cx, cy = quad[:6]
    return f"{ax},{ay} {bx},{by} {bx + cx - ax},{by + cy - ay} {cx},{cy}"


def sway_group(rects, scene_name, amp, dur):
    """Kelompok gambar yang ikut bergoyang: scene di-klip ke kotak lalu digeser."""
    groups = []
    for i, (x, y, w, h) in enumerate(rects):
        delay = i * 0.9
        groups.append(f"""
    <g clip-path="url(#c{i})">
      <image class="sway-scene" href="{scene_name}" x="0" y="0" width="__W__" height="__H__">
        <animateTransform attributeName="transform" type="translate"
          values="{-amp},0; {amp},0; {-amp},0" dur="{dur}s"
          begin="{delay}s" repeatCount="indefinite" calcMode="spline"
          keySplines="0.45 0 0.55 1;0.45 0 0.55 1"/>
      </image>
    </g>
    <clipPath id="c{i}"><rect x="{x}" y="{y}" width="{w}" height="{h}"/></clipPath>""")
    return "\n".join(groups)


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
    --ink:#0E0D1A; --night:#15142A; --cream:#F8E9C8; --cream2:#E3CDA6;
    --amber:#F2AB3F; --screen:#D7F7CD;
    --ratio: __RATIO__;
  }
  *{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
  html,body{margin:0;height:100%;overflow:hidden;background:var(--ink)}
  body{touch-action:manipulation;user-select:none;
    font-family:ui-monospace,"SF Mono",Menlo,Consolas,monospace}

  /* letterbox: gambar selalu 1280:800, sisa layar hitam — sama seperti APK */
  #stage{
    position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);
    aspect-ratio:var(--ratio);
    width:min(100vw, calc(100vh * var(--ratio)));
    max-height:100vh;
  }
  /* ukuran stage dipastikan lewat JS (fit()) — CSS di atas hanya fallback */
  #scene{position:absolute;inset:0;width:100%;height:100%;display:block;
    image-rendering:pixelated;pointer-events:none}
  #fx image{image-rendering:pixelated}
  #fx{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}
  #fx text{font-family:"Silkscreen","Courier New",monospace;font-weight:700;dominant-baseline:auto}

  .hotspot{position:absolute;border:0;padding:0;margin:0;background:transparent;
    cursor:pointer;outline:none;border-radius:0}
  .hotspot.is-pressed{background:rgba(242,171,63,.20);box-shadow:0 0 0 3px var(--amber) inset}

  #tint{position:absolute;inset:0;pointer-events:none}
  #scan{position:absolute;inset:0;pointer-events:none;
    background:repeating-linear-gradient(180deg,rgba(0,0,0,.10) 0 1px,transparent 1px 3px)}

  #toast{position:absolute;left:50%;bottom:26px;transform:translate(-50%,8px);
    padding:8px 14px;background:var(--night);border:2px solid var(--amber);
    color:var(--cream2);font-size:12px;letter-spacing:1px;white-space:nowrap;
    opacity:0;transition:opacity .18s ease,transform .18s ease;pointer-events:none}
  #toast.show{opacity:1;transform:translate(-50%,0)}

  #rotate{position:fixed;inset:0;display:none;flex-direction:column;
    align-items:center;justify-content:center;gap:18px;
    background:var(--ink);color:var(--cream2);
    font-size:12px;letter-spacing:2px;text-align:center;padding:24px;z-index:10}
  #rotate .icon{width:56px;height:56px;border:3px solid var(--amber);border-radius:8px;position:relative}
  #rotate .icon::after{content:"";position:absolute;inset:8px 16px;border:3px solid var(--amber);border-radius:4px}
  #rotate small{color:#7D75A0;letter-spacing:1px;font-size:11px;line-height:1.7}
  /* di preview, layar potret cukup dilayerbox — sama seperti APK di layar lain rasio */
</style>
</head>
<body>
<div id="stage">
  <img id="scene" src="scene_night.png" alt="Workspace guru" draggable="false">
  <svg id="fx" viewBox="0 0 __SCENE_W__ __SCENE_H__" preserveAspectRatio="none">
    <!-- angin: gorden & daun -->
    __SWAY__

    <!-- rel gorden: batang logam polos -->
    <line id="rail" x1="__RAIL_X1__" y1="__RAIL_Y1__" x2="__RAIL_X2__" y2="__RAIL_Y2__"
          stroke="#A9AEc4" stroke-width="4" stroke-linecap="round"/>

    <!-- lima bingkai aplikasi -->
    

    <!-- judul papan tulis -->
    <g id="board" transform="__BOARD_MATRIX__">
      <text id="board-title" x="__BOARD_W__" y="__BOARD_TY__" text-anchor="middle"
            fill="#181430" font-size="__BOARD_FS__" letter-spacing="1">JADWAL MENGAJAR</text>
      <line x1="__BOARD_LX1__" y1="__BOARD_LY__" x2="__BOARD_LX2__" y2="__BOARD_LY__"
            stroke="rgba(24,20,48,.35)" stroke-width="1.6"/>
    </g>

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
const TILES = __TILES__;
const RAIL = __RAIL__;

const stage = document.getElementById("stage");
const sceneEl = document.getElementById("scene");
const toast = document.getElementById("toast");
const tintEl = document.getElementById("tint");
let toastTimer = 0;
function pct(v, total) { return (v / total * 100).toFixed(4) + "%"; }

function place(el, x, y, w, h) {
  el.style.left = pct(x, SCENE.w);
  el.style.top = pct(y, SCENE.h);
  el.style.width = pct(w, SCENE.w);
  el.style.height = pct(h, SCENE.h);
}

function showToast(name) {
  toast.textContent = name.toUpperCase();
  toast.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 2000);
}

function open(name) {
  showToast("MEMBUKA " + name);
}

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


function ambience(h) {
  if (h >= 5 && h <= 8) return "rgba(255,217,160,.10)";
  if (h >= 9 && h <= 15) return "rgba(255,246,226,.16)";
  if (h >= 16 && h <= 18) return "rgba(255,154,92,.12)";
  if (h >= 19 && h <= 22) return "rgba(43,63,143,.14)";
  return "rgba(6,10,34,.34)";
}

let lastDay = null;

function tick() {
  const now = new Date();
  const day = now.getHours() >= 6 && now.getHours() <= 17;

  if (day !== lastDay) {
    const file = day ? "scene_day.png" : "scene_night.png";
    sceneEl.src = file;
    document.querySelectorAll(".sway-scene").forEach((img) => img.setAttribute("href", file));
    lastDay = day;
  }

  tintEl.style.background = ambience(now.getHours());
}

tick();
setInterval(tick, 1000);

// pastikan stage selalu 1280:800 muat penuh di viewport apa pun
// (CSS min()/calc() gagal di sebagian webview — ukuran dihitung JS di sini)
function fit() {
  var st = document.getElementById("stage");
  var s = Math.min(window.innerWidth / __W__, window.innerHeight / __H__);
  st.style.width = Math.floor(__W__ * s) + "px";
  st.style.height = Math.floor(__H__ * s) + "px";
}
window.addEventListener("resize", fit);
window.addEventListener("orientationchange", fit);
fit();

// minta browser mengunci landscape saat layar disentuh
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


def build_html(data, scene_sizes):
    w, h = data["w"], data["h"]
    board = data["board"]


    board_w = ((board[2] - board[0]) ** 2 + (board[3] - board[1]) ** 2) ** 0.5
    board_h = ((board[4] - board[0]) ** 2 + (board[5] - board[1]) ** 2) ** 0.5

    sway_scene = "scene_night.png"
    sway = sway_group(data["curtains"] + [data["leaf"]], sway_scene, 2.2, 5.2)

    html = (
        HTML.replace("__RATIO__", f"{w} / {h}")
        .replace("__W__", str(w)).replace("__H__", str(h))
        .replace("__SCENE_W__", str(w))
        .replace("__SCENE_H__", str(h))
        .replace("__TILES__", "[\n" + "".join(
            f'  {{label: "{t["label"]}", x: {t["x"]}, y: {t["y"]}, w: {t["w"]}, h: {t["h"]}}},\n'
            for t in data["tiles"]) + "]")
        .replace("__RAIL__", str(data["rail"]).replace(" ", ""))
        .replace("__SWAY__", sway.replace("__W__", str(w)).replace("__H__", str(h)))
        # rel
        .replace("__RAIL_X1__", str(data["rail"][0]))
        .replace("__RAIL_Y1__", str(data["rail"][1]))
        .replace("__RAIL_X2__", str(data["rail"][2]))
        .replace("__RAIL_Y2__", str(data["rail"][3]))
        # papan tulis
        .replace("__BOARD_MATRIX__", matrix_attr(board))
        .replace("__BOARD_W__", f"{board_w / 2:.2f}")
        .replace("__BOARD_TY__", f"{board_h * 0.16:.2f}")
        .replace("__BOARD_FS__", f"{min(board_h * 0.115, board_w * 0.80 / 9.3):.2f}")
        .replace("__BOARD_LX1__", f"{board_w * 0.18:.2f}")
        .replace("__BOARD_LX2__", f"{board_w * 0.82:.2f}")
        .replace("__BOARD_LY__", f"{board_h * 0.215:.2f}")













    )
    (WEB / "index.html").write_text(html)
    return html


def main() -> None:
    data = parse_data()
    sizes = make_scenes()
    build_html(data, sizes)
    print(f"scene {sizes} · {len(data['tiles'])} objek · {WEB/'index.html'}")


if __name__ == "__main__":
    main()
