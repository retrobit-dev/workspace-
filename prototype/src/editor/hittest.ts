// Hit-testing: ketukan mengikuti urutan gambar TERBALIK (objek depan menang).
import type { Objek } from "../scene/tipe";
import { poligonBalok, poligonLantai, poligonDinding, titikDalam, type PoligonBalok } from "../render/geo";
import type { Titik } from "../iso/proyeksi";

export interface Tampak {
  objek: Objek;
  poly: PoligonBalok | Titik[];
  jenis: "balok" | "lantai" | "dinding";
}

/** Daftar objek + poligon, urut gambar (sama dgn renderer). */
export function daftarTampak(scene: { objects: Objek[] }): Tampak[] {
  const banding = (a: Objek, b: Objek) => {
    const ka = a.e + a.w, kb = b.e + b.w;
    return ka !== kb ? ka - kb : a.urutan - b.urutan;
  };
  const l = (lay: Objek["layer"]) => scene.objects.filter((o) => o.layer === lay).sort(banding);
  const out: Tampak[] = [];
  for (const o of [...l("underlay"), ...l("main"), ...l("overlay")]) {
    if (o.bentuk === "lantai") out.push({ objek: o, poly: poligonLantai(o), jenis: "lantai" });
    else if (o.bentuk === "dinding") out.push({ objek: o, poly: poligonDinding(o), jenis: "dinding" });
    else out.push({ objek: o, poly: poligonBalok(o), jenis: "balok" });
  }
  return out;
}

/** Objek terdepan yang memuat titik kanvas, atau null. */
export function kena(scene: { objects: Objek[] }, p: Titik): Objek | null {
  const tampak = daftarTampak(scene);
  for (let i = tampak.length - 1; i >= 0; i--) {
    const t = tampak[i];
    if (t.jenis === "balok") {
      const P = t.poly as PoligonBalok;
      if (titikDalam(p, P.atas) || titikDalam(p, P.timur) || titikDalam(p, P.barat)) return t.objek;
    } else if (titikDalam(p, t.poly as Titik[])) {
      return t.objek;
    }
  }
  return null;
}
