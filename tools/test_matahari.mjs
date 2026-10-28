// Uji matahari (0.16.30): selalu tampil saat siang; mendung hanya meredupkan.
// Bebas jam: jalur siang diverifikasi statis (kode), jalur malam via runtime.
import { JSDOM } from "jsdom";
import { readFileSync } from "fs";

const html = readFileSync(new URL("../web/index.html", import.meta.url));
const A = (c, m) => {
  if (!c) { console.error("GAGAL:", m); process.exit(1); }
  console.log("ok:", m);
};

// jalur siang (statis): matahari dikendalikan siang/malam saja + opasitas berjenjang
A(/setDisp\("sky-sun",isDay\)/.test(html), "matahari dikendalikan murni oleh siang/malam (bukan mendung)");
A(/sunEl\.style\.opacity=isDay\?\(SKY_WEATHER\.langit<=1\?1:\(SKY_WEATHER\.langit===2\?0\.8:0\.3\)\):""/.test(html),
  "mendung sedang -> α0.8, mendung tebal -> α0.3, cerah -> 1 (cermin aturan bulan 0.16.22)");

// jalur malam (runtime): bulan tetap samar saat mendung tebal — kode yang sama
const dom = new JSDOM(html, { runScripts: "dangerously", pretendToBeVisual: true, url: "https://rk.local/" });
const w = dom.window;
const moon = w.document.getElementById("sky-moon");
const sun = w.document.getElementById("sky-sun");
A(!!moon && !!sun, "elemen matahari & bulan ada");
w.applySkyCode(3);
// opasitas bulan dipasang oleh skyTick (interval 1 dtk) — tunggu satu tick
await new Promise((r) => setTimeout(r, 1300));
if (new Date().getHours() >= 6 && new Date().getHours() <= 17) {
  A(sun.style.opacity === "0.3", "siang + mendung tebal: matahari α0.3 (dulu hilang total)");
} else {
  A(moon.style.opacity === "0.3", "malam + mendung tebal: bulan α0.3 (jalur opasitas yang sama aktif)");
  A(sun.style.display === "none", "malam: matahari memang disembunyikan");
}
w.applySkyCode(1);
await new Promise((r) => setTimeout(r, 1300));
if (new Date().getHours() >= 6 && new Date().getHours() <= 17) {
  A(sun.style.opacity === "1", "siang cerah: matahari penuh");
} else {
  A(moon.style.opacity === "1", "malam berawan ringan: bulan penuh");
}
console.log("SEMUA LULUS");
process.exit(0);
