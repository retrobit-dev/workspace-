// Cek preview tanpa browser: hotspot harus tepat di atas objeknya (koordinat Data.kt),
// jam analog + layar jam digital + kalender hidup harus terisi dan menempel bidangnya.
//   node tools/check_preview.mjs
import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const SRC = new URL("../android/app/src/main/java/dev/retrobit/ruangkelas/Data.kt", import.meta.url);
const HTML = new URL("../web/index.html", import.meta.url);
const data = readFileSync(SRC, "utf8");

const tiles = [...data.matchAll(/TileInfo\("([^"]+)",\s*Scr\.\w+,\s*"[^"]*",\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)/g)]
  .map((m) => ({ label: m[1], x: +m[2], y: +m[3], w: +m[4], h: +m[5] }));
const rect = (name) => {
  const m = data.match(new RegExp(`val ${name} = intArrayOf\\(([^)]+)\\)`));
  return m[1].split(",").map((v) => +v.trim());
};
const sceneOf = (name) => +data.match(new RegExp(`const val ${name} = (\\d+)`))[1];

const SCENE = { w: sceneOf("SCENE_W"), h: sceneOf("SCENE_H") };
const CLOCK = rect("CLOCK_FACE");
const CAL_Q = rect("CALENDAR_QUAD");
const LED_Q = rect("LED_QUAD");
const WINDOW_Q = rect("WINDOW_QUAD");
const FRAME_Q = [...(data.slice(data.indexOf("val FRAME_QUADS")).matchAll(/intArrayOf\((\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)/g))]
  .map((m) => [+m[1], +m[2], +m[3], +m[4], +m[5], +m[6]]);

let errors = 0;
const fail = (msg) => { errors++; console.log("  ✗ " + msg); };
const close = (a, b, eps = 0.02) => Math.abs(parseFloat(a) - parseFloat(b)) < eps;

const dom = new JSDOM(readFileSync(HTML, "utf8"), { runScripts: "dangerously", pretendToBeVisual: true });
const { document } = dom.window;

const hotspots = [...document.querySelectorAll(".hotspot")];
if (hotspots.length !== tiles.length) fail(`jumlah hotspot ${hotspots.length} != ${tiles.length} tile`);
if (tiles.length !== 17) fail(`TILES berisi ${tiles.length} objek, harusnya 17`);

tiles.forEach((tile, i) => {
  const el = hotspots[i];
  if (!el) return;
  if (el.dataset.label !== tile.label) fail(`urutan: ${el.dataset.label} != ${tile.label}`);
  const want = {
    left: (tile.x / SCENE.w) * 100, top: (tile.y / SCENE.h) * 100,
    width: (tile.w / SCENE.w) * 100, height: (tile.h / SCENE.h) * 100,
  };
  const got = { left: el.style.left, top: el.style.top, width: el.style.width, height: el.style.height };
  for (const k of Object.keys(want)) {
    if (!close(got[k], want[k])) fail(`${tile.label}.${k}: ${got[k]} != ${want[k].toFixed(4)}%`);
  }
});

// jam analog: kotak lurus sesuai CLOCK_FACE
const clockEl = document.getElementById("clock");
const wantClock = {
  left: (CLOCK[0] / SCENE.w) * 100, top: (CLOCK[1] / SCENE.h) * 100,
  width: (CLOCK[2] / SCENE.w) * 100, height: (CLOCK[3] / SCENE.h) * 100,
};
for (const k of Object.keys(wantClock)) {
  if (!close(clockEl.style[k], wantClock[k])) fail(`clock.${k}: ${clockEl.style[k]} != ${wantClock[k].toFixed(4)}%`);
}

// bidang miring (kalender & layar LED): ukuran + matriks harus cocok dengan quad
function checkPlane(el, name, q) {
  if (!el) return fail(`#${name} tidak ada`);
  const [ax, ay, bx, by, cx, cy] = q;
  const W = Math.hypot(bx - ax, by - ay);
  const H = Math.hypot(cx - ax, cy - ay);
  const want = {
    left: ax, top: ay, width: W, height: H,
    a: (bx - ax) / W, b: (by - ay) / W, c: (cx - ax) / H, d: (cy - ay) / H,
  };
  const m = (el.style.transform.match(/matrix\(([^)]+)\)/) || [, ""])[1].split(",").map(Number);
  if (!close(el.style.width, want.width, 0.05)) fail(`${name}.width: ${el.style.width} != ${want.width.toFixed(2)}px`);
  if (!close(el.style.height, want.height, 0.05)) fail(`${name}.height: ${el.style.height} != ${want.height.toFixed(2)}px`);
  if (!close(el.style.left, want.left, 0.05)) fail(`${name}.left: ${el.style.left} != ${want.left}px`);
  if (!close(el.style.top, want.top, 0.05)) fail(`${name}.top: ${el.style.top} != ${want.top}px`);
  ["a", "b", "c", "d"].forEach((k, i) => {
    if (!close(m[i], want[k], 0.002)) fail(`${name}.matrix.${k}: ${m[i]} != ${want[k].toFixed(4)}`);
  });
}
checkPlane(document.getElementById("cal"), "cal", CAL_Q);

checkPlane(document.getElementById("led"), "led", LED_Q);

// ikon bingkai: lima latar + banyak sel
const icons = document.querySelectorAll("#fx polygon");
if (icons.length < 13) fail(`poligon ikon hanya ${icons.length} (harus 5 latar + 8 bentuk)`);
if (FRAME_Q.length < 5) fail(`bingkai hanya ${FRAME_Q.length}`);

// gorden baterai ada di lapisan vektor
if (!document.getElementById("curtain")) fail("poligon gorden baterai tidak ada");

if (!document.getElementById("hh").getAttribute("transform")) fail("jarum jam belum diset");
if (!document.getElementById("mh").getAttribute("transform")) fail("jarum menit belum diset");
if (!document.getElementById("sh").getAttribute("transform")) fail("jarum detik belum diset");

const led = document.getElementById("led-t").textContent.trim();
if (!/^\d{2}:\d{2}:\d{2}$/.test(led)) fail(`jam digital "${led}" bukan HH:MM:SS`);
const now = new Date();
const hhmm = String(now.getHours()).padStart(2, "0") + ":" + String(now.getMinutes()).padStart(2, "0");
if (!led.startsWith(hhmm)) fail(`jam digital ${led} != waktu sekarang ${hhmm}`);

for (const id of ["cal-m", "cal-d", "cal-w"]) {
  if (!document.getElementById(id).textContent.trim()) fail(`${id} kosong`);
}
const day = +document.getElementById("cal-d").textContent;
if (day !== now.getDate()) fail(`tanggal kalender ${day} != hari ini ${now.getDate()}`);
if (!document.getElementById("tint").style.background) fail("selaput suasana belum diset");
if (!document.getElementById("led-t").textContent.trim()) fail("teks jam digital kosong");

const labels = [...hotspots].map((e) => e.dataset.label);
["Kalkulator", "Alat Tulis", "Kalender", "Jadwal", "Profil"].forEach((k) => {
  if (!labels.includes(k)) fail(`hotspot "${k}" hilang`);
});
["WhatsApp", "Vivaldi", "YouTube", "Spotify", "Sinaga Jateng"].forEach((k) => {
  if (!labels.includes(k)) fail(`bingkai aplikasi "${k}" hilang`);
});
if (labels.filter((l) => l === "Binder").length < 2) fail("hotspot rak & laci harus dua");
if (FRAME_Q.length !== 5) fail(`bingkai harus 5, ada ${FRAME_Q.length}`);
if (WINDOW_Q.length !== 8) fail("WINDOW_QUAD harus 8 angka");

console.log(errors === 0
  ? `PREVIEW OK · ${hotspots.length} hotspot · jam ${led} · kalender ${day}/${now.getDate()} · bidang miring cocok`
  : `PREVIEW GAGAL · ${errors} masalah`);
process.exit(errors === 0 ? 0 : 1);
