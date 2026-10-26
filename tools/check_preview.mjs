// Cek preview tanpa browser: tiap hotspot harus berada tepat di atas objeknya
// (koordinat di Data.kt) dan jam/kalender hidup harus terisi.
//   node tools/check_preview.mjs
import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const SRC = new URL("../android/app/src/main/java/dev/retrobit/ruangkelas/Data.kt", import.meta.url);
const HTML = new URL("../web/index.html", import.meta.url);
const data = readFileSync(SRC, "utf8");

const grab = (name) => {
  const m = data.match(new RegExp(`TileInfo\\("([^"]+)",\\s*Scr\\.\\w+,\\s*"[^"]*",\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\)`));
  return m;
};
const tiles = [...data.matchAll(/TileInfo\("([^"]+)",\s*Scr\.\w+,\s*"[^"]*",\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)/g)]
  .map((m) => ({ label: m[1], x: +m[2], y: +m[3], w: +m[4], h: +m[5] }));
const rect = (name) => {
  const m = data.match(new RegExp(`val ${name} = intArrayOf\\(([^)]+)\\)`));
  return m[1].split(",").map((v) => +v.trim());
};
const sceneOf = (name) => +data.match(new RegExp(`const val ${name} = (\\d+)`))[1];

const SCENE = { w: sceneOf("SCENE_W"), h: sceneOf("SCENE_H") };
const CLOCK = rect("CLOCK_FACE");
const CAL = rect("CALENDAR_FACE");

let errors = 0;
const fail = (msg) => { errors++; console.log("  ✗ " + msg); };

const dom = new JSDOM(readFileSync(HTML, "utf8"), { runScripts: "dangerously", pretendToBeVisual: true });
const { document } = dom.window;

const hotspots = [...document.querySelectorAll(".hotspot")];
if (hotspots.length !== tiles.length) fail(`jumlah hotspot ${hotspots.length} != ${tiles.length} tile`);
if (tiles.length !== 12) fail(`TILES berisi ${tiles.length} objek, harusnya 12`);

const close = (a, b) => Math.abs(parseFloat(a) - parseFloat(b)) < 0.01;

tiles.forEach((tile, i) => {
  const el = hotspots[i];
  if (!el) return;
  if (el.dataset.label !== tile.label) fail(`urutan: ${el.dataset.label} != ${tile.label}`);
  const want = {
    left: (tile.x / SCENE.w) * 100, top: (tile.y / SCENE.h) * 100,
    width: (tile.w / SCENE.w) * 100, height: (tile.h / SCENE.h) * 100,
  };
  const got = {
    left: el.style.left, top: el.style.top, width: el.style.width, height: el.style.height,
  };
  for (const k of Object.keys(want)) {
    if (!close(got[k], want[k])) fail(`${tile.label}.${k}: ${got[k]} != ${want[k].toFixed(4)}%`);
  }
});

const checkRect = (el, name, r) => {
  if (!el) return fail(`${name} tidak ada`);
  const got = { left: el.style.left, top: el.style.top, width: el.style.width, height: el.style.height };
  const want = {
    left: (r[0] / SCENE.w) * 100, top: (r[1] / SCENE.h) * 100,
    width: (r[2] / SCENE.w) * 100, height: (r[3] / SCENE.h) * 100,
  };
  for (const k of Object.keys(want)) {
    if (!close(got[k], want[k])) fail(`${name}.${k}: ${got[k]} != ${want[k].toFixed(4)}%`);
  }
};

checkRect(document.getElementById("clock"), "clock", CLOCK);
checkRect(document.getElementById("cal"), "cal", CAL);

if (!document.getElementById("hh").getAttribute("transform")) fail("jarum jam belum diset");
if (!document.getElementById("mh").getAttribute("transform")) fail("jarum menit belum diset");
if (!document.getElementById("sh").getAttribute("transform")) fail("jarum detik belum diset");
for (const id of ["cal-m", "cal-d", "cal-w"]) {
  const t = document.getElementById(id).textContent.trim();
  if (!t) fail(`${id} kosong`);
}
const day = +document.getElementById("cal-d").textContent;
const today = new Date().getDate();
if (day !== today) fail(`tanggal kalender ${day} != hari ini ${today}`);

const labels = [...document.querySelectorAll(".hotspot")].map((e) => e.dataset.label);
const known = ["Kalkulator", "Alat Tulis", "Kalender", "Jadwal"];
known.forEach((k) => { if (!labels.includes(k)) fail(`hotspot "${k}" hilang`); });

console.log(errors === 0
  ? `PREVIEW OK · ${hotspots.length} hotspot · jam analog · kalender ${day}/${today}`
  : `PREVIEW GAGAL · ${errors} masalah`);
process.exit(errors === 0 ? 0 : 1);
