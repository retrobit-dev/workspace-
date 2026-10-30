// Validasi + migrasi scene.json (docs/03-SCENE-SCHEMA.md)
import type { Bentuk, Layer, Objek, SceneJson } from "./tipe";
import { des3, jepit3 } from "../iso/proyeksi";

const LAYER: Layer[] = ["underlay", "main", "overlay"];
const BENTUK: Bentuk[] = ["lantai", "dinding", "balok"];

export class GalatScene extends Error {}

function angka(v: unknown, fallback: number): number {
  const n = typeof v === "number" ? v : parseFloat(String(v));
  return Number.isFinite(n) ? n : fallback;
}

function warnaHex(v: unknown, fallback: string): string {
  return typeof v === "string" && /^#[0-9a-fA-F]{6}$/.test(v) ? v : fallback;
}

/** Normalkan satu objek; melempar GalatScene bila tidak bisa diselamatkan. */
function normalkanObjek(o: Record<string, unknown>, i: number): Objek {
  const id = typeof o.id === "string" && o.id ? o.id : `obj-${i}-${Date.now().toString(36)}`;
  const bentuk = BENTUK.includes(o.bentuk as Bentuk) ? (o.bentuk as Bentuk) : "balok";
  const layer = LAYER.includes(o.layer as Layer) ? (o.layer as Layer) : "main";
  const sz = Math.max(0.001, angka(o.sz, bentuk === "dinding" ? 2.6 : 0.6));
  return {
    id,
    label: typeof o.label === "string" && o.label ? o.label : id,
    e: des3(angka(o.e, 1)), w: des3(angka(o.w, 1)), z: des3(Math.max(0, angka(o.z, 0))),
    se: Math.max(0.001, des3(angka(o.se, 0.6))),
    sw: Math.max(0.001, des3(angka(o.sw, 0.6))),
    sz: jepit3(sz, 0.001, 50),
    bentuk,
    warna: warnaHex(o.warna, bentuk === "lantai" ? "#AC90C3" : bentuk === "dinding" ? "#667391" : "#8C93A8"),
    layer,
    urutan: Math.round(angka(o.urutan, 0)),
    role: typeof o.role === "string" ? o.role : "umum",
    side: o.side === "db" || o.side === "dt" ? o.side : null,
    rot: des3(angka(o.rot, 0)),
    opasitas: jepit3(angka(o.opasitas, 1), 0.05, 1),
    grup: typeof o.grup === "string" ? o.grup : null,
    subgrup: typeof o.subgrup === "string" ? o.subgrup : null,
    kunci: !!o.kunci,
    latar: !!o.latar,
    animasi: null, // M1: tipe disiapkan, belum dianimasikan (jujur di laporan)
    app: typeof o.app === "string" ? o.app : null,
  };
}

/** Muat + validasi + migrasi scene apa pun → SceneJson v1 yang aman. */
export function muatScene(data: unknown): SceneJson {
  if (typeof data !== "object" || data === null) throw new GalatScene("scene bukan objek");
  const d = data as Record<string, unknown>;
  const migrasi: string[] = [];

  // Migrasi v0 (prototipe lama tanpa version) → v1: terima & tandai.
  const versi = Math.round(angka(d.version, 0));
  if (versi === 0) migrasi.push("v0→v1: version ditambahkan, field dinormalkan");
  if (versi > 1) migrasi.push(`v${versi}→v1: field baru diabaikan (aman)`);

  const roomRaw = (typeof d.room === "object" && d.room !== null ? d.room : {}) as Record<string, unknown>;
  const room = {
    e: jepit3(angka(roomRaw.e, 9.6), 1, 60),
    w: jepit3(angka(roomRaw.w, 9.6), 1, 60),
    z: jepit3(angka(roomRaw.z, 2.6), 0.5, 20),
  };

  if (!Array.isArray(d.objects)) throw new GalatScene("objects bukan array");
  const objects = (d.objects as Record<string, unknown>[]).map(normalkanObjek);
  const seen = new Set<string>();
  for (const o of objects) {
    if (seen.has(o.id)) throw new GalatScene(`id ganda: ${o.id}`);
    seen.add(o.id);
  }

  const metaRaw = (typeof d.meta === "object" && d.meta !== null ? d.meta : {}) as Record<string, unknown>;
  const metaLama = (metaRaw.migrasi ?? []) as string[];
  const meta = {
    dibuat: typeof metaRaw.dibuat === "string" ? metaRaw.dibuat : "ISO",
    diubah: new Date().toISOString(),
    catatan: typeof metaRaw.catatan === "string" ? metaRaw.catatan : "",
    migrasi: [...metaLama, ...migrasi],
  };

  return { version: 1, room, objects, meta };
}

/** Coba parse JSON string → SceneJson. */
export function parseScene(teks: string): SceneJson {
  return muatScene(JSON.parse(teks));
}

const KUNCI_SIMPAN = "iso-scene-aktif";

export function simpanLokal(scene: SceneJson): void {
  localStorage.setItem(KUNCI_SIMPAN, JSON.stringify(scene));
}

export function muatLokal(): SceneJson | null {
  const t = localStorage.getItem(KUNCI_SIMPAN);
  if (!t) return null;
  try {
    return parseScene(t);
  } catch {
    return null;
  }
}

/** Unduh scene sebagai file scene.json. */
export function eksporScene(scene: SceneJson): void {
  const blob = new Blob([JSON.stringify(scene, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "scene.json";
  a.click();
  URL.revokeObjectURL(url);
}

/** Baca file scene.json dari <input type=file>. */
export function imporScene(file: File): Promise<SceneJson> {
  return file.text().then(parseScene);
}
