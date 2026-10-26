// Cek preview tanpa browser: hotspot & semua lapisan hidup harus memakai
// koordinat yang sama dengan Data.kt (sumber kebenaran), plus aturan v7:
//   · tidak ada jam analog, hanya jam digital
//   · (0.15.6) jam digital & bendera dihapus; Jadwal lewat papan tulis
//   · bendera di kanan jam DIHAPUS (0.15.5)
//   · rel gorden = indikator baterai + persen di dinding
//   · siang/malam mengikuti jam, gorden & daun bergoyang
//
//   node tools/check_preview.mjs
import { readFileSync } from "node:fs";
import { JSDOM } from "jsdom";

const SRC = new URL("../android/app/src/main/java/dev/retrobit/ruangkelas/Data.kt", import.meta.url);
const HTML = new URL("../web/index.html", import.meta.url);
const data = readFileSync(SRC, "utf8");

const sceneOf = (name) => +data.match(new RegExp(`const val ${name} = (\\d+)`))[1];
const SCENE = { w: sceneOf("SCENE_W"), h: sceneOf("SCENE_H") };

const rect = (name) => {
  const m = data.match(new RegExp(`val ${name} = intArrayOf\\(([^)]*)\\)`));
  if (!m) throw new Error(`Data.kt tidak punya ${name}`);
  return m[1].split(",").map((v) => +v.trim());
};

const tiles = [...data.matchAll(/TileInfo\("([^"]+)",\s*Scr\.\w+,\s*"[^"]*",\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)/g)]
  .map((m) => ({ label: m[1], x: +m[2], y: +m[3], w: +m[4], h: +m[5] }));

const listOfQuads = (name) => {
  const start = data.indexOf(`val ${name} = listOf(`);
  const block = data.slice(start, data.indexOf("\n)", start));
  return [...block.matchAll(/intArrayOf\(([-\d,\s]+)\)/g)].map((m) => m[1].split(",").map((v) => +v.trim()));
};

const frameQuads = listOfQuads("FRAME_QUADS");
const curtainRects = listOfQuads("CURTAIN_RECTS");
const icons = [...data.matchAll(/AppIcon\("([^"]+)",\s*"(#[0-9A-Fa-f]+)"/g)].map((m) => ({ name: m[1], bg: m[2] }));

const BOARD = rect("BOARD_QUAD");
const RAIL = rect("RAIL_LINE");
const BATT = rect("BATTERY_TEXT_QUAD");
const LEAF = rect("LEAF_RECT");
const WINDOW_QUAD = rect("WINDOW_QUAD");

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

// ---- tidak ada jam analog ----
["hh", "mh", "sh", "analog"].forEach((id) => {
  if (document.getElementById(id)) fail(`#${id} (jam analog) masih ada`);
});
if (/analog/i.test(document.body.innerHTML)) fail("masih menyebut jam analog");

// ---- jam digital dihapus; hotspot Jadwal kini di papan tulis ----
if (document.getElementById("clock")) fail("#clock masih ada (jam digital sudah dihapus)");
const jadwalTile = tiles.find((t) => t.label === "Jadwal");
if (!jadwalTile) fail("hotspot Jadwal hilang");
else {
  const boardC = { x: jadwalTile.x + jadwalTile.w / 2, y: jadwalTile.y + jadwalTile.h / 2 };
  if (boardC.x < 300 || boardC.x > 620 || boardC.y < 200 || boardC.y > 380)
    fail(`hotspot Jadwal tidak di area papan tulis (${boardC.x},${boardC.y})`);
}

// ---- bendera dihapus dari jam digital ----
if (document.getElementById("flag")) fail("#flag masih ada (bendera sudah dihapus)");
if (/CE1126/i.test(document.getElementById("fx").innerHTML)) fail("masih ada warna bendera merah di overlay");

// ---- papan tulis: judul di atas, rata tengah ----
const title = document.getElementById("board-title");
if (!title) fail("judul papan tulis hilang");
else {
  if (!/jadwal mengajar/i.test(title.textContent)) fail(`judul papan "${title.textContent}"`);
  if (title.getAttribute("text-anchor") !== "middle") fail("judul papan tidak rata tengah");
  const boardW = Math.hypot(BOARD[2] - BOARD[0], BOARD[3] - BOARD[1]);
  if (Math.abs(parseFloat(title.getAttribute("x")) * 2 - boardW) > 1.5)
    fail("judul papan tidak di tengah lebar papan");
  if (!document.querySelector("#board line")) fail("garis bawah judul papan hilang");
}

// ---- kalender dihapus (0.15.7) ----
if (document.getElementById("cal-page") || document.getElementById("cal"))
  fail("kalender masih ada (sudah dihapus)");

// ---- rel gorden = baterai ----
if (!document.getElementById("rail")) fail("rel gorden baterai hilang");
const ticks = document.querySelectorAll("#rail-ticks line");
if (ticks.length !== 9) fail(`tanda rel ${ticks.length}, harusnya 9 (tiap 10%)`);
if (!document.getElementById("batt")) fail("persentase baterai di dinding hilang");
const battG = document.getElementById("batt-g").getAttribute("transform") || "";
if (!/matrix/.test(battG)) fail("persentase baterai tidak mengikuti arah dinding");
const battM = battG.match(/matrix\(([^)]+)\)/)[1].split(",").map(Number);
if (!close(battM[4], BATT[0], 0.5) || !close(battM[5], BATT[1], 0.5)) fail("persen baterai tidak di BATTERY_TEXT_QUAD");

// ---- angin: gorden & daun bergoyang ----
if (!document.querySelector("#fx .sway-scene, #fx image")) fail("lapisan angin tidak ada");
if (curtainRects.length !== 2) fail(`CURTAIN_RECTS harus 2, ada ${curtainRects.length}`);
if (LEAF.length !== 4) fail("LEAF_RECT harus 4 angka");

// ---- ikon bingkai ----
const polys = document.querySelectorAll("#fx #icons polygon");
if (polys.length < frameQuads.length + 5) fail(`poligon ikon hanya ${polys.length}`);
if (frameQuads.length !== 5) fail(`bingkai harus 5, ada ${frameQuads.length}`);
if (icons.length !== 5) fail(`ikon aplikasi harus 5, ada ${icons.length}`);
if (icons[2]?.name !== "Spotify" || icons[3]?.name !== "YouTube")
  fail(`urutan bingkai 3-4 = ${icons[2]?.name}/${icons[3]?.name}, harusnya Spotify/YouTube`);

// ---- scene siang & malam ----
const scene = document.getElementById("scene");
if (!/scene_(day|night)\.png/.test(scene.getAttribute("src") || "")) fail("scene tidak memakai scene_day/night");

// ---- jam & tanggal browser masih dipakai untuk siang/malam ----
const now = new Date();
for (const id of ["batt"]) {
  if (!document.getElementById(id).textContent.trim()) fail(`${id} kosong`);
}
if (!document.getElementById("tint").style.background) fail("selaput suasana belum diset");

// ---- label wajib ----
const labels = hotspots.map((e) => e.dataset.label);
["Kalkulator", "Alat Tulis", "Jadwal", "Profil", "File"].forEach((k) => {
  if (!labels.includes(k)) fail(`hotspot "${k}" hilang`);
});
["WhatsApp", "Vivaldi", "Spotify", "YouTube", "Sinaga Jateng"].forEach((k) => {
  if (!labels.includes(k)) fail(`bingkai aplikasi "${k}" hilang`);
});
if (labels.filter((l) => l === "Binder").length < 2) fail("hotspot rak & laci harus dua");
if (WINDOW_QUAD.length !== 8) fail("WINDOW_QUAD harus 8 angka");
if (RAIL.length !== 4) fail("RAIL_LINE harus 4 angka");
if (BATT.length !== 6) fail("BATTERY_TEXT_QUAD harus 6 angka");
if (document.querySelector("#fx #flag")) fail("#flag masih ada di svg");

console.log(errors === 0
  ? `PREVIEW OK · ${hotspots.length} hotspot · ${icons.length} bingkai · rel baterai ${ticks.length} tanda`
  : `PREVIEW GAGAL · ${errors} masalah`);
process.exit(errors === 0 ? 0 : 1);
