// Cek preview tanpa browser: hotspot & semua lapisan hidup harus memakai
// koordinat yang sama dengan Data.kt (sumber kebenaran), aturan era rebuild
// 0.16.1 (canvas 1280×800):
//   · 11 zona ketuk (satu Binder — laci meja; rak buku = Catatan)
//   · tidak ada jam analog/digital & bendera; Jadwal lewat papan tulis
//   · kalender DIHAPUS total (objek, hotspot, fitur); bingkai dinding kosong
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


const BOARD = rect("BOARD_QUAD");
const LEAF = rect("LEAF_RECT");
const WINDOW_QUAD = rect("WINDOW_QUAD");

let errors = 0;
const fail = (msg) => { errors++; console.log("  ✗ " + msg); };
const close = (a, b, eps = 0.02) => Math.abs(parseFloat(a) - parseFloat(b)) < eps;

const rawHtml = readFileSync(HTML, "utf8");
const dom = new JSDOM(rawHtml, { runScripts: "dangerously", pretendToBeVisual: true });
const { document } = dom.window;

const hotspots = [...document.querySelectorAll(".hotspot")];
if (hotspots.length !== tiles.length) fail(`jumlah hotspot ${hotspots.length} != ${tiles.length} tile`);
if (tiles.length !== 11) fail(`TILES berisi ${tiles.length} objek, harusnya 11`);

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

// ---- kalender dihapus total (0.16.1) ----
if (document.getElementById("cal") || document.getElementById("cal-page"))
  fail("kalender masih ada (sudah dihapus total)");

// ---- batang gorden cukup digambar art; bar SVG dilarang (dikeluhkan user 0.16.6) ----
if (document.getElementById("rail"))
  fail("bar rel svg masih ada di dekat jendela (hapus - art sudah menggambar batangnya)");
if (document.getElementById("rail-ticks") || document.getElementById("batt"))
  fail("indikator baterai masih ada di rail (sudah dihapus)");
if (/BATTERY_TEXT_QUAD|RAIL_LINE/.test(data))
  fail("BATTERY_TEXT_QUAD / RAIL_LINE masih ada di Data.kt");

// ---- angin: gorden & daun bergoyang ----
if (!document.querySelector("#fx .sway-scene, #fx image")) fail("lapisan angin tidak ada");
if (/CURTAIN_RECTS\b/.test(data)) fail("konstanta lama CURTAIN_RECTS tidak boleh kembali");
if (/TAIL_QUAD|TAIL_SWAY|PAPER_RECTS/.test(data))
  fail("konstanta animasi tirai/kertas kembali — tirai diam total (keputusan user 0.16.14)");
if (document.getElementById("tail-base") || document.getElementById("tail-sway"))
  fail("lapisan ekor tirai masih ada — tirai harus diam total");
const rotCount = (document.querySelectorAll("#fx animateTransform[type=rotate]")).length;
if (rotCount !== 0) fail(`masih ada animasi rotate (${rotCount}) — tirai/daun digerakkan JS`);
if (LEAF.length !== 4) fail("LEAF_RECT harus 4 angka");
// angin nyata: pita tirai + daun JS-driven, kecepatan dari wind_speed Open-Meteo
for (const id of ["cb-d1", "cb-d2", "cb-d3", "cb-n1", "cb-n2", "cb-n3", "leaf-g"])
  if (!document.getElementById(id)) fail(`lapisan angin ${id} tidak ada`);
if (!/wind_speed_10m/.test(rawHtml) || !/windTick/.test(rawHtml))
  fail("mesin angin nyata tidak ada (wind_speed_10m / windTick)");
// bayangan matahari nyata: grup + 8 elips + fungsi matahari + awan
const bayang = document.getElementById("bayangan");
if (!bayang || bayang.children.length !== 8) fail("lapisan bayangan matahari tidak ada / jumlah elips salah");
if (!/function matahari/.test(rawHtml) || !/bayangTick/.test(rawHtml))
  fail("mesin bayangan matahari tidak ada (matahari/bayangTick)");
if (!/cloud_cover/.test(rawHtml)) fail("tutupan awan (cloud_cover) tidak diambil dari Open-Meteo");

// ---- bingkai dinding kosong (ikon pintasan dihapus 0.16.1) ----
if (document.getElementById("icons")) fail("#icons masih ada (ikon pintasan sudah dihapus)");

// ---- scene siang & malam ----
const scene = document.getElementById("scene");
if (!/scene_(day|night)\.png/.test(scene.getAttribute("src") || "")) fail("scene tidak memakai scene_day/night");

// ---- waktu & kalender hidup ----
const now = new Date();
if (!document.getElementById("tint").style.background) fail("selaput suasana belum diset");

// ---- label wajib ----
const labels = hotspots.map((e) => e.dataset.label);
["Jurnal", "Nilai", "Kalkulator", "Alat Tulis", "Binder", "Jadwal", "Profil", "File", "Catatan", "Absensi", "Cuaca"].forEach((k) => {
  if (!labels.includes(k)) fail(`hotspot "${k}" hilang`);
});
if (labels.filter((l) => l === "Binder").length !== 1) fail("hotspot Binder (laci meja) harus tepat satu");
if (WINDOW_QUAD.length !== 8) fail("WINDOW_QUAD harus 8 angka");
if (document.querySelector("#fx #flag")) fail("#flag masih ada di svg");
if (/image-rendering\s*:\s*pixelated/.test(rawHtml))
  fail("CSS pixelated masih ada — art 0.16.9 smooth tanpa pixelate");

// ---- langit hidup (0.16.18 UNDERLAY): svg sebelum scene + scene bolong kaca ----
const skyUnder = document.getElementById("sky-under");
const sceneImg = document.getElementById("scene");
if (!skyUnder) fail("underlay langit hilang (#sky-under)");
else {
  if (!(skyUnder.compareDocumentPosition(sceneImg) & dom.window.Node.DOCUMENT_POSITION_FOLLOWING))
    fail("#sky-under harus berada SEBELUM #scene (underlay, bukan overlay)");
  if (!document.getElementById("skygrad")) fail("gradien langit hilang");
  const orb = document.getElementById("sky-orb-pos");
  if (!orb) fail("orb matahari/bulan hilang");
  else if (!/^translate\(/.test(orb.getAttribute("transform") || "")) fail("posisi orb tidak diset JS");
  if (!document.getElementById("sg0").getAttribute("stop-color")) fail("warna gradien langit belum diset");
  const cloudT = document.querySelectorAll("#sky-clouds animateTransform[type=translate]").length;
  if (cloudT < 3) fail(`awan bergerak kurang (${cloudT} < 3)`);
  const rainT = document.querySelectorAll("#sky-rain animateTransform[type=translate]").length;
  if (rainT < 100) fail(`rintikan kurang (${rainT} < 100)`);
  ["sky-flash", "sky-snow", "sky-fog", "sky-roomray", "sky-stars", "sky-dim", "sky-sun", "sky-moon"].forEach((id) => {
    if (!document.getElementById(id)) fail(`#${id} hilang`);
  });
  if (!/api\.open-meteo\.com/.test(rawHtml)) fail("langit tidak menarik kode cuaca Open-Meteo");
}
const holesM = rawHtml.match(/sky-holes:\s*(\d+)/);
if (!holesM || +holesM[1] < 15000)
  fail(`bolongan kaca scene kurang (${holesM ? holesM[1] : "tidak ada"} < 15000 px) — scene harus transparan di kaca`);

console.log(errors === 0
  ? `PREVIEW OK · ${hotspots.length} hotspot · tanpa kalender · tanpa baterai/bar rel · tirai+daun angin nyata · langit→Cuaca · langit hidup (orb+cuaca+matahari) · bayangan matahari nyata · art smooth · siang lembut`
  : `PREVIEW GAGAL · ${errors} masalah`);
process.exit(errors === 0 ? 0 : 1);
