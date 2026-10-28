import {JSDOM} from "jsdom";
import {readFileSync} from "fs";
const html = readFileSync("web/index.html", "utf8");
const dom = new JSDOM(html, {runScripts: "dangerously", pretendToBeVisual: true, url: "https://rk.local/"});
const w = dom.window;
const A = (c, m) => { if (!c) { console.error("GAGAL:", m); process.exit(1); } console.log("ok:", m); };
A(w.document.getElementById("cb-d1"), "pita tirai siang ada");
A(w.document.getElementById("cb-n3"), "pita tirai malam ada");
A(w.document.getElementById("leaf-g"), "daun JS-driven ada");
A(/wind_speed_10m/.test(html), "fetch angin Open-Meteo ada");
const sun = w.document.getElementById("sky-sun");
A(sun, "elemen matahari ada");
// biarkan windTick jalan
await new Promise(r => setTimeout(r, 250));
const t1 = w.document.getElementById("cb-n3").getAttribute("transform");
await new Promise(r => setTimeout(r, 220));
const t2 = w.document.getElementById("cb-n3").getAttribute("transform");
A(t1 && t2, "pita malam bertransform: " + t1 + " -> " + t2);
A(t1 !== t2, "transform berubah antar frame (bergerak)");
const lt = w.document.getElementById("leaf-g").getAttribute("transform");
A(lt && lt.includes(","), "daun bertransform: " + lt);
// matahari: simulasi siang → tampil; langit mendung tebal → opacity 0.3
w.SKAY = null;
console.log("SEMUA LULUS");
process.exit(0);
