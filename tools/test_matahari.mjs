import {JSDOM} from "jsdom";
import {readFileSync} from "fs";
const html = readFileSync("web/index.html", "utf8");
const dom = new JSDOM(html, {runScripts: "dangerously", pretendToBeVisual: true, url: "https://rk.local/"});
const w = dom.window;
const A = (c, m) => { if (!c) { console.error("GAGAL:", m); process.exit(1); } console.log("ok:", m); };
const sun = w.document.getElementById("sky-sun");
const jam = new Date().getHours();
if (jam >= 6 && jam <= 17) {
  A(sun.style.display !== "none", "siang (" + jam + "h): matahari tampil");
  A(sun.style.opacity !== "", "opacity matahari diset: " + sun.style.opacity);
} else {
  A(sun.style.display === "none", "malam (" + jam + "h): matahari memang disembunyikan");
}
w.applySkyCode(3);                       // mendung tebal
A(sun.style.opacity === "0.3", "mendung tebal: matahari samar α0.3 (dulu hilang total)");
w.applySkyCode(1);
A(sun.style.opacity === "1", "cerah berawan ringan: matahari penuh");
console.log("SEMUA LULUS");
process.exit(0);
