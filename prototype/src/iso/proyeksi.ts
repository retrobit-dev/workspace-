// Konvensi ruang & proyeksi isometrik (docs/01-KONVENSI-RUANG.md)
// Kanvas internal 568×355 (satuan kanvas), PIKSEL=2, DENS=0.8875.

export const KANVAS_W = 568;
export const KANVAS_H = 355;
export const PIKSEL = 2; // raster per satuan kanvas → 1136×710
export const DENS = 0.8875; // faktor tampilan CSS pada zoom ×1

export const RASTER_W = KANVAS_W * PIKSEL; // 1136
export const RASTER_H = KANVAS_H * PIKSEL; // 710

// Titik sudut belakang ruangan (asal e,w,z) di kanvas:
export const ASAL_X = KANVAS_W / 2; // 284
export const ASAL_Y = 56;

// Skala iso per satuan (satuan kanvas):
export const UNIT_X = 24; // setengah-lebar ubin
export const UNIT_Y = 12; // setengah-tinggi ubin
export const UNIT_Z = 24; // tinggi per satuan z

export interface Titik { x: number; y: number }
export interface Bidang { e: number; w: number }

/** Proyeksikan (e,w,z) → titik kanvas (satuan kanvas, sebelum kamera). */
export function proyeksikan(e: number, w: number, z: number): Titik {
  return {
    x: ASAL_X + (e - w) * UNIT_X,
    y: ASAL_Y + (e + w) * UNIT_Y - z * UNIT_Z,
  };
}

/** Invers pada bidang z tetap: titik kanvas → (e,w). */
export function inversBidang(x: number, y: number, z = 0): Bidang {
  const dx = x - ASAL_X;
  const dy = y - ASAL_Y + z * UNIT_Z;
  const s = dy / UNIT_Y; // e + w
  const d = dx / UNIT_X; // e - w
  return { e: (s + d) / 2, w: (s - d) / 2 };
}

/** Bulatkan ke 3 desimal (aturan schema). */
export function des3(v: number): number {
  return Math.round(v * 1000) / 1000;
}

/** Jepit ke [min, max] dgn 3 desimal. */
export function jepit3(v: number, min: number, max: number): number {
  return des3(Math.min(max, Math.max(min, v)));
}
