// Scene awal: ruangan kosong + 3 objek LATAR terlindungi
// (lantai, dinding DB, dinding DT) — persis konvensi "OBJEK n (+3 LATAR)".
import type { SceneJson } from "./tipe";

const DIbuat = "ISO-0.1.0";

export function sceneKosong(): SceneJson {
  return {
    version: 1,
    room: { e: 9.6, w: 9.6, z: 2.6 },
    objects: [
      {
        id: "latar-lantai",
        label: "Lantai",
        e: 0, w: 0, z: 0,
        se: 9.6, sw: 9.6, sz: 0.05,
        bentuk: "lantai",
        warna: "#AC90C3",
        layer: "underlay",
        urutan: 0,
        role: "ruang",
        side: null,
        opasitas: 1,
        kunci: false,
        latar: true,
        animasi: null,
        app: null,
      },
      {
        id: "latar-dinding-db",
        label: "Dinding Barat (DB)",
        e: 0, w: 0, z: 0,
        se: 0.12, sw: 9.6, sz: 2.6,
        bentuk: "dinding",
        warna: "#667391",
        layer: "underlay",
        urutan: 1,
        role: "ruang",
        side: "db",
        opasitas: 1,
        kunci: false,
        latar: true,
        animasi: null,
        app: null,
      },
      {
        id: "latar-dinding-dt",
        label: "Dinding Timur (DT)",
        e: 0, w: 0, z: 0,
        se: 9.6, sw: 0.12, sz: 2.6,
        bentuk: "dinding",
        warna: "#5F6B8A",
        layer: "underlay",
        urutan: 2,
        role: "ruang",
        side: "dt",
        opasitas: 1,
        kunci: false,
        latar: true,
        animasi: null,
        app: null,
      },
    ],
    meta: {
      dibuat: DIbuat,
      diubah: DIbuat,
      catatan: "Ruangan kosong — susun lewat editor.",
      migrasi: [],
    },
  };
}
