import {JSDOM} from "jsdom";
import {readFileSync} from "fs";
const html = readFileSync("web/index.html", "utf8");
const dom = new JSDOM(html, {runScripts: "dangerously", pretendToBeVisual: true, url: "https://rk.local/"});
const w = dom.window;
const A = (c, m) => { if (!c) { console.error("GAGAL:", m); process.exit(1); } console.log("ok:", m); };
const g = w.document.getElementById("bayangan");
A(g && g.children.length === 8, "grup bayangan + 8 elips ada");
A(typeof w.matahari === "function", "fungsi matahari tersedia");
const R = -6.7089, O = 111.3419;
// 10:30 WIB = 03:30 UTC — matahari tinggi
const [az1, el1] = w.matahari(R, O, Date.UTC(2026, 9, 28, 3, 30));
A(el1 > 40 && el1 < 85, "10:30 WIB: elevasi tinggi (" + el1.toFixed(1) + "°)");
// 07:00 WIB = 00:00 UTC — pagi, elevasi rendah-sedang
const [az2, el2] = w.matahari(R, O, Date.UTC(2026, 9, 28, 0, 0));
A(el2 > 5 && el2 < 40, "07:00 WIB: elevasi pagi (" + el2.toFixed(1) + "°), az " + az2.toFixed(0) + "°");
A(az2 < 180, "pagi: matahari di timur (az " + az2.toFixed(0) + "° < 180)");
// 15:30 WIB = 08:30 UTC — sore
const [az3, el3] = w.matahari(R, O, Date.UTC(2026, 9, 28, 8, 30));
A(el3 > 5 && el3 < 50, "15:30 WIB: elevasi sore (" + el3.toFixed(1) + "°)");
A(az3 > 240, "sore: matahari di barat (az " + az3.toFixed(0) + "° > 240)");
// malam
const [, el4] = w.matahari(R, O, Date.UTC(2026, 9, 28, 17, 0));
A(el4 < 0, "00:00 WIB: matahari di bawah horizon (" + el4.toFixed(1) + "°)");
// bayangan tersembunyi saat malam perangkat
await new Promise(r => setTimeout(r, 300));
A(g.style.display === "none", "malam perangkat: bayangan disembunyikan");
console.log("SEMUA LULUS");
process.exit(0);
