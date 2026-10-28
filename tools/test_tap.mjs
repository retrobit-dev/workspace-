// Uji area ketuk poligon (0.16.35): bentuk area ketuk = bentuk objek.
//   node tools/test_tap.mjs
import {JSDOM} from "jsdom";
import {readFileSync} from "fs";
const html = readFileSync("web/index.html", "utf8");
const dom = new JSDOM(html, {runScripts: "dangerously", pretendToBeVisual: true});
const w = dom.window, d = w.document;
const A = (c, m) => { if (!c) { console.error("GAGAL:", m); process.exit(1); } console.log("ok:", m); };

A(typeof w.tileAt === "function", "tileAt global tersedia");
A(w.TILES_RO.length === 11, "11 tile poligon");

// ketuk langsung di level hit-test
A(w.tileAt(875, 382)?.label === "Jurnal", "dalam laptop -> Jurnal");
A(w.tileAt(768, 321)?.label !== "Jurnal", "pojok kartu lama (buku/lampu) BUKAN Jurnal");
A(w.tileAt(170, 330)?.label === "Absensi", "papan pin -> Absensi (tidak ditelan Jadwal)");
A(w.tileAt(478, 275)?.label === "Jadwal", "muka papan -> Jadwal");
A(w.tileAt(390, 500) === null, "kaki papan tulis -> null (bukan area ketuk)");
A(w.tileAt(972, 412)?.label === "Alat Tulis", "gelas pensil -> Alat Tulis");
A(w.tileAt(1040, 230)?.label === "Cuaca", "kaca kiri -> Cuaca (satu langit dgn kanan)");
A(w.tileAt(1150, 300)?.label === "Cuaca", "kaca kanan -> Cuaca");
A(w.tileAt(640, 700) === null, "lantai kosong -> null");

// ketuk DOM: stage pointerdown -> toast sesuai objek
const stage = d.getElementById("stage");
const toast = d.getElementById("toast");
stage.getBoundingClientRect = () => ({left: 0, top: 0, width: 1280, height: 800, right: 1280, bottom: 800});
const tap = (x, y) => stage.dispatchEvent(new w.MouseEvent("pointerdown", {clientX: x, clientY: y, bubbles: true}));
const show = () => (toast.classList.contains("show") ? toast.textContent : "");
tap(170, 330);
A(show().includes("ABSENSI"), "ketuk pin -> toast ABSENSI (" + show() + ")");
await new Promise((r) => setTimeout(r, 2350));
tap(390, 500);
A(show() === "", "ketuk kaki papan -> tanpa toast");
tap(733, 323);   // satu buku di meja (area Nilai 0.16.38)
A(show().includes("NILAI"), "ketuk buku -> toast NILAI (" + show() + ")");
console.log("SEMUA LULUS");
process.exit(0);
