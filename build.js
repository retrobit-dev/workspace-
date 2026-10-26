#!/usr/bin/env node
/**
 * build.js — menggabungkan prototipe menjadi SATU file HTML mandiri.
 *
 *   node build.js
 *   → preview-ruang-kelas.html
 *
 * Semua CSS, JS, font, dan gambar ditanam sebagai data URI sehingga file bisa
 * dibuka langsung dari berkas (offline, tanpa server) di tablet/PC.
 */

const fs = require("fs");
const path = require("path");

const ROOT = __dirname;
const SRC = path.join(ROOT, "prototype");
const OUT = path.join(ROOT, "preview-ruang-kelas.html");
const OUT_COPY = path.join(SRC, "preview-ruang-kelas.html"); // agar ikut tampil di server preview

const read = (rel) => fs.readFileSync(path.join(SRC, rel));

const MIME = {
  ".png": "image/png",
  ".woff2": "font/woff2",
  ".jpg": "image/jpeg",
  ".svg": "image/svg+xml"
};

function dataURI(file) {
  const abs = path.join(SRC, file);
  const ext = path.extname(abs);
  const mime = MIME[ext] || "application/octet-stream";
  return `data:${mime};base64,${fs.readFileSync(abs).toString("base64")}`;
}

/* ---------------- CSS: inline + embed font ---------------- */
function inlineCSS(rel) {
  let css = read(rel).toString("utf8");
  css = css.replace(/url\((["']?)(assets\/[^"')]+)\1\)/g, (m, q, file) => {
    const abs = path.join(SRC, file);
    if (!fs.existsSync(abs)) return m;
    return `url(${dataURI(file)})`;
  });
  return `<style data-source="${rel}">\n${css}\n</style>`;
}

/* ---------------- JS: inline + embed assets ---------------- */
function inlineJS() {
  let js = read("app.js").toString("utf8");

  const tiles = {};
  const tileDir = path.join(SRC, "assets/tiles32");
  for (const name of fs.readdirSync(tileDir).sort()) {
    if (!name.endsWith(".png")) continue;
    tiles[name.replace(/\.png$/, "")] = dataURI(`assets/tiles32/${name}`);
  }

  js = js.replace('src32: "assets/scene-32.png"', `src32: ${JSON.stringify(dataURI("assets/scene-32.png"))}`);
  js = js.replace('src64: "assets/scene-64.png"', `src64: ${JSON.stringify(dataURI("assets/scene-64.png"))}`);
  js = js.replace('src="assets/tiles32/${t.id}.png"', 'src="${ASSETS.tiles[t.id]}"');

  const assetsBlock = `/* aset tertanam (hasil build) */\nconst ASSETS = { tiles: ${JSON.stringify(tiles)} };\n`;
  return `<script data-source="app.js">\n${assetsBlock}\n${js}\n</script>`;
}

/* ---------------- HTML ---------------- */
let html = read("index.html").toString("utf8");

html = html.replace('<link rel="stylesheet" href="styles.css">', () => inlineCSS("styles.css"));
html = html.replace('<link rel="stylesheet" href="tablet.css">', () => inlineCSS("tablet.css"));
html = html.replace('<link rel="stylesheet" href="real.css">', () => inlineCSS("real.css"));
/* pakai fungsi sebagai pengganti agar "$$" di JS tidak ditafsirkan sebagai escape */
html = html.replace('<script src="app.js"></script>', () => inlineJS());

// gambar scene di markup
html = html.replace(/src="assets\/(scene-[0-9]+\.png)"/g, (m, file) => `src="${dataURI(`assets/${file}`)}"`);

// judul + penanda versi mandiri
html = html.replace(
  "<title>Ruang Kelas — Pixel Workspace (Tablet Landscape)</title>",
  "<title>Ruang Kelas — Preview Interaktif (Tablet Landscape)</title>"
);
html = html.replace(
  '<meta name="description" content="Prototipe UI/UX aplikasi guru tablet landscape bergaya pixel art 32px: jurnal, catatan, absensi, nilai, jadwal, kalender, kalkulator, binder.">',
  '<meta name="description" content="Preview interaktif satu file: aplikasi guru tablet landscape bergaya pixel art 32px. Semua elemen bisa diklik.">'
);

fs.writeFileSync(OUT, html);
fs.writeFileSync(OUT_COPY, html);

const kb = (fs.statSync(OUT).size / 1024).toFixed(0);
const tilesCount = fs.readdirSync(path.join(SRC, "assets/tiles32")).filter((f) => f.endsWith(".png")).length;
console.log(`✓ ${path.basename(OUT)} — ${kb} KB, ${tilesCount} tile + 2 scene + 6 font tertanam`);
console.log(`  salinan: prototype/${path.basename(OUT_COPY)} (dilayani di /preview-ruang-kelas.html)`);
