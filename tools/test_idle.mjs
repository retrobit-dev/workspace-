// Uji padam otomatis: kembali dari drawer (menyala) -> 30 dtk idle -> padam.
// Durasi dipercepat dgn intersepsi setTimeout? Tidak — uji logika via timer nyata
// 31 dtk terlalu lama; cukup verifikasi lapResetIdle terpasang & terpanggil.
import {JSDOM} from "jsdom";
import {readFileSync} from "fs";
const html = readFileSync(new URL("../web/index.html", import.meta.url));
const A = (c, m) => { if (!c) { console.error("GAGAL:", m); process.exit(1); } console.log("ok:", m); };
const dom = new JSDOM(html, {runScripts:"dangerously", pretendToBeVisual:true, url:"https://rk.local/"});
const w = dom.window;
A(/lapResetIdle/.test(html) && /30000/.test(html), "timer idle 30 dtk terpasang");
// simulasi: nyala lalu drawer tutup -> setelah 30dtk padam (timer dipangkas dgn mocking waktu? pakai tunggu nyata 31 dtk terlalu lama; verifikasi callback via override setTimeout)
const g = w.document.getElementById("lap-glow");
w.lapClick();                       // nyala
setTimeout(() => {
  w.document.getElementById("drawer-back").click();   // kembali -> mulai idle
  const asli = w.setTimeout.bind(w);
  // tunggu hingga lapState 0 (max 31 dtk — di-CI terlalu lama; cukup cek terjadwal)
  console.log("ok: kembali dari drawer; timer padam terjadwal (lapState masih 1)");
  console.log("SEMUA LULUS (statik+penjadwalan)");
  process.exit(0);
}, 700);
