// Geometri poligon balok & dinding di ruang kanvas.
// DIPAKAI BERSAMA renderer & hit-testing agar konsisten (kontrak renderer).
import type { Objek } from "../scene/tipe";
import { proyeksikan, type Titik } from "../iso/proyeksi";

export interface PoligonBalok {
  atas: Titik[]; // 4 titik searah jarum jam dari sudut (e,w)
  timur: Titik[]; // muka kanan (x = e+se)
  barat: Titik[]; // muka kiri (y = w+sw)
}

/** Poligon balok di ruang kanvas (3 muka terlihat kamera 3/4). */
export function poligonBalok(o: Objek): PoligonBalok {
  const { e, w, z, se, sw, sz } = o;
  const A = proyeksikan(e, w, z + sz);          // atas-depan-kiri
  const B = proyeksikan(e + se, w, z + sz);     // atas-kanan-belakang sisi timur
  const C = proyeksikan(e + se, w + sw, z + sz);// atas-depan (sudut terdepan)
  const D = proyeksikan(e, w + sw, z + sz);     // atas-kiri
  const C0 = proyeksikan(e + se, w + sw, z);    // bawah sudut terdepan
  const B0 = proyeksikan(e + se, w, z);
  const D0 = proyeksikan(e, w + sw, z);
  return {
    atas: [A, B, C, D],
    timur: [B, C, C0, B0],
    barat: [D, C, C0, D0],
  };
}

/** Poligon lantai (diamond) memakai ukuran objek lantai. */
export function poligonLantai(o: Objek): Titik[] {
  return [
    proyeksikan(o.e, o.w, 0),
    proyeksikan(o.e + o.se, o.w, 0),
    proyeksikan(o.e + o.se, o.w + o.sw, 0),
    proyeksikan(o.e, o.w + o.sw, 0),
  ];
}

/** Poligon dinding vertikal pada sisi yang dipegang objek. */
export function poligonDinding(o: Objek): Titik[] {
  const h = o.sz;
  if (o.side === "dt") {
    // dinding timur: sepanjang sumbu e di w ≈ 0
    const tebal = Math.max(o.sw, 0.12);
    const e0 = o.e, e1 = o.e + o.se;
    return [
      proyeksikan(e0, 0, h),
      proyeksikan(e1, 0, h),
      proyeksikan(e1, 0, 0),
      proyeksikan(e0, 0, 0),
    ];
  }
  // db (default): sepanjang sumbu w di e ≈ 0
  const tebal = Math.max(o.se, 0.12);
  const w0 = o.w, w1 = o.w + o.sw;
  return [
    proyeksikan(0, w0, h),
    proyeksikan(0, w1, h),
    proyeksikan(0, w1, 0),
    proyeksikan(0, w0, 0),
  ];
}

/** Uji titik dalam poligon (ray casting). */
export function titikDalam(p: Titik, poly: Titik[]): boolean {
  let dalam = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const xi = poly[i].x, yi = poly[i].y, xj = poly[j].x, yj = poly[j].y;
    if (yi > p.y !== yj > p.y && p.x < ((xj - xi) * (p.y - yi)) / (yj - yi) + xi) {
      dalam = !dalam;
    }
  }
  return dalam;
}
