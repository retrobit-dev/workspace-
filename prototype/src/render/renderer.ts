// Renderer Canvas 2D — membaca scene, tidak mengubah data (docs/04).
import type { Objek, SceneJson } from "../scene/tipe";
import { PIKSEL, proyeksikan, ASAL_X, ASAL_Y, UNIT_X, UNIT_Y, UNIT_Z, KANVAS_W, KANVAS_H } from "../iso/proyeksi";
import { poligonBalok, poligonDinding, poligonLantai } from "./geo";
import { campur, rawang } from "./pastel";

const BAYANG = "rgba(30,24,50,0.28)"; // warna AO (biru-gelap lembut)

function isiPoly(ctx: CanvasRenderingContext2D, poly: { x: number; y: number }[], isi: string | CanvasGradient): void {
  ctx.beginPath();
  ctx.moveTo(poly[0].x, poly[0].y);
  for (let i = 1; i < poly.length; i++) ctx.lineTo(poly[i].x, poly[i].y);
  ctx.closePath();
  ctx.fillStyle = isi;
  ctx.fill();
}

/** AO lembut di bawah balok: diamond bayangan mengembang memudar. */
function gambarAO(ctx: CanvasRenderingContext2D, o: Objek): void {
  const cx = proyeksikan(o.e + o.se / 2, o.w + o.sw / 2, 0);
  const rx = ((o.se + o.sw) / 2) * UNIT_X * 0.72 + 6;
  const ry = ((o.se + o.sw) / 2) * UNIT_Y * 0.72 + 3;
  const g = ctx.createRadialGradient(cx.x, cx.y, 1, cx.x, cx.y, Math.max(rx, ry));
  g.addColorStop(0, BAYANG);
  g.addColorStop(1, "rgba(30,24,50,0)");
  ctx.save();
  ctx.translate(cx.x, cx.y);
  ctx.scale(1, ry / rx);
  ctx.translate(-cx.x, -cx.y);
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.arc(cx.x, cx.y, rx, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

function gambarLantai(ctx: CanvasRenderingContext2D, o: Objek): void {
  const poly = poligonLantai(o);
  const pA = poly[0], pC = poly[2];
  const g = ctx.createLinearGradient(pA.x, pA.y, pC.x, pC.y);
  g.addColorStop(0, rawang(o.warna, 1.1));
  g.addColorStop(1, rawang(o.warna, 0.88));
  isiPoly(ctx, poly, g);

  // nat ubin isometrik tiap 1 satuan — halus, tanpa noise
  ctx.save();
  isiPoly(ctx, poly, "#000"); // clip
  ctx.clip();
  ctx.strokeStyle = "rgba(255,255,255,0.10)";
  ctx.lineWidth = 0.5;
  ctx.beginPath();
  for (let i = 1; i < Math.ceil(Math.max(o.se, o.sw)); i++) {
    let p = proyeksikan(o.e + i, o.w, 0);
    let q = proyeksikan(o.e + i, o.w + o.sw, 0);
    ctx.moveTo(p.x, p.y); ctx.lineTo(q.x, q.y);
    p = proyeksikan(o.e, o.w + i, 0);
    q = proyeksikan(o.e + o.se, o.w + i, 0);
    ctx.moveTo(p.x, p.y); ctx.lineTo(q.x, q.y);
  }
  ctx.stroke();
  // sambungan dinding-lantai: strip AO
  const dB = proyeksikan(o.e, o.w, 0);
  const dT = proyeksikan(o.e + o.se, o.w, 0);
  const gAO = ctx.createLinearGradient(dB.x, dB.y - 8, dB.x, dB.y + 6);
  gAO.addColorStop(0, "rgba(30,24,50,0)");
  gAO.addColorStop(1, "rgba(30,24,50,0.18)");
  ctx.strokeStyle = gAO;
  ctx.lineWidth = 6;
  ctx.beginPath();
  ctx.moveTo(dB.x, dB.y); ctx.lineTo(dT.x, dT.y);
  const kiri0 = proyeksikan(o.e, o.w, 0);
  const kiri1 = proyeksikan(o.e, o.w + o.sw, 0);
  const gAO2 = ctx.createLinearGradient(kiri0.x, kiri0.y - 8, kiri0.x, kiri0.y + 6);
  gAO2.addColorStop(0, "rgba(30,24,50,0)");
  gAO2.addColorStop(1, "rgba(30,24,50,0.18)");
  ctx.strokeStyle = gAO2;
  ctx.beginPath();
  ctx.moveTo(kiri0.x, kiri0.y); ctx.lineTo(kiri1.x, kiri1.y);
  ctx.stroke();
  ctx.restore();
}

function gambarDinding(ctx: CanvasRenderingContext2D, o: Objek): void {
  const poly = poligonDinding(o);
  const atas = poly[0], bawah = poly[3];
  const g = ctx.createLinearGradient(0, atas.y, 0, bawah.y);
  g.addColorStop(0, rawang(o.warna, 1.14));
  g.addColorStop(1, rawang(o.warna, 0.82));
  isiPoly(ctx, poly, g);
  // skirting (plin) gelap tipis di kaki dinding
  const kaki = [poly[3], poly[2]];
  const t = 3.5;
  const dx = kaki[1].x - kaki[0].x, dy = kaki[1].y - kaki[0].y;
  const len = Math.hypot(dx, dy) || 1;
  const nx = (-dy / len) * t, ny = (dx / len) * t;
  isiPoly(ctx, [kaki[0], kaki[1], { x: kaki[1].x + nx, y: kaki[1].y + ny }, { x: kaki[0].x + nx, y: kaki[0].y + ny }], rawang(o.warna, 0.62));
}

function gambarBalok(ctx: CanvasRenderingContext2D, o: Objek): void {
  gambarAO(ctx, o);
  const P = poligonBalok(o);
  // muka timur (kanan) — agak terang
  const gt = ctx.createLinearGradient(P.timur[0].x, P.timur[0].y, P.timur[3].x, P.timur[3].y);
  gt.addColorStop(0, rawang(o.warna, 1.02));
  gt.addColorStop(1, rawang(o.warna, 0.86));
  isiPoly(ctx, P.timur, gt);
  // muka barat (kiri) — bayangan
  const gb = ctx.createLinearGradient(P.barat[0].x, P.barat[0].y, P.barat[3].x, P.barat[3].y);
  gb.addColorStop(0, rawang(o.warna, 0.8));
  gb.addColorStop(1, rawang(o.warna, 0.66));
  isiPoly(ctx, P.barat, gb);
  // atap — paling terang
  const ga = ctx.createLinearGradient(P.atas[0].x, P.atas[0].y, P.atas[2].x, P.atas[2].y);
  ga.addColorStop(0, rawang(o.warna, 1.26));
  ga.addColorStop(1, rawang(o.warna, 1.08));
  isiPoly(ctx, P.atas, ga);
  // garis jatuh halus antara muka (bukan outline): campuran muka
  ctx.strokeStyle = campur(rawang(o.warna, 0.7), rawang(o.warna, 1.15), 0.5);
  ctx.lineWidth = 0.7;
  ctx.beginPath();
  ctx.moveTo(P.atas[1].x, P.atas[1].y);
  ctx.lineTo(P.timur[2].x, P.timur[2].y);
  ctx.moveTo(P.atas[2].x, P.atas[2].y);
  ctx.lineTo(P.barat[2].x, P.barat[2].y);
  ctx.stroke();
}

function bandingGambar(a: Objek, b: Objek): number {
  const ka = a.e + a.w, kb = b.e + b.w;
  if (ka !== kb) return ka - kb;
  return a.urutan - b.urutan;
}

/** Gambar satu frame scene penuh ke ctx (dalam transform kamera aktif). */
export function gambarScene(ctx: CanvasRenderingContext2D, scene: SceneJson): void {
  // luar ruangan: hitam OLED
  ctx.fillStyle = "#000";
  ctx.fillRect(-ASAL_X * 4, -ASAL_Y * 4, KANVAS_W * 8, KANVAS_H * 8);

  const layer = (l: Objek["layer"]) => scene.objects.filter((o) => o.layer === l).sort(bandingGambar);
  for (const o of layer("underlay")) {
    if (o.bentuk === "lantai") gambarLantai(ctx, o);
    else if (o.bentuk === "dinding") gambarDinding(ctx, o);
  }
  for (const o of layer("main")) gambarBalok(ctx, o);
  for (const o of layer("overlay")) gambarBalok(ctx, o);
}

/** Terapkan kamera ke ctx; kembalikan skala piksel per satuan kanvas. */
export function terapkanKamera(
  ctx: CanvasRenderingContext2D,
  cam: { x: number; y: number; zoom: number },
): void {
  ctx.setTransform(PIKSEL, 0, 0, PIKSEL, 0, 0);
  ctx.translate(KANVAS_W / 2 + cam.x * cam.zoom, KANVAS_H / 2 + cam.y * cam.zoom);
  ctx.scale(cam.zoom, cam.zoom);
  ctx.translate(-ASAL_X, -ASAL_Y);
}
