// Tipe scene.json (docs/03-SCENE-SCHEMA.md)

export type Layer = "underlay" | "main" | "overlay";
export type Bentuk = "lantai" | "dinding" | "balok";
export type Sisi = "db" | "dt" | null;

export interface Animasi {
  jenis: "naik-turun" | "ayun" | "denyut" | "kedip" | "putar";
  amplitudo: number; // satuan sesuai jenis (fraksi / derajat)
  periode: number; // detik
  fasa?: number;
}

export interface Objek {
  id: string;
  label: string;
  e: number; w: number; z: number; // posisi
  se: number; sw: number; sz: number; // ukuran
  bentuk: Bentuk;
  warna: string;
  layer: Layer;
  urutan: number;
  role?: string; // "ruang" | "umum" | lainnya
  side?: Sisi;
  rot?: number; // derajat (disimpan; M1 belum memutar visual)
  opasitas?: number; // 0..1 (default 1)
  grup?: string | null;
  subgrup?: string | null;
  kunci?: boolean;
  latar?: boolean; // terlindungi sampai BUKA
  animasi?: Animasi | null;
  app?: string | null;
}

export interface Ruang { e: number; w: number; z: number }

export interface Meta {
  dibuat: string;
  diubah: string;
  catatan?: string;
  migrasi?: string[];
}

export interface SceneJson {
  version: 1;
  room: Ruang;
  objects: Objek[];
  meta: Meta;
}
