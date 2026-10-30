# 03 — SCENE SCHEMA (scene.json)

Kontrak data bersama antara preview dan (kelak) aplikasi Android.

## Bentuk
```jsonc
{
  "version": 1,
  "room":  { "e": 9.6, "w": 9.6, "z": 2.6 },        // dimensi ruangan
  "objects": [ /* Objek */ ],
  "meta": { "dibuat": "ISO", "diubah": "ISO", "catatan": "", "migrasi": [] }
}
```

## Objek
| Field | Keterangan |
|---|---|
| `id` | unik, stabil |
| `label` | nama tampilan |
| `e`,`w`,`z` | posisi (≤3 desimal) |
| `se`,`sw`,`sz` | ukuran (≤3 desimal, min 0,001) |
| `bentuk` | `lantai` · `dinding` · `balok` (katalog menambah jenis) |
| `warna` | warna/material dasar (hex) |
| `layer` | `underlay` · `main` · `overlay` |
| `urutan` | urutan gambar di dalam layer |
| `role`,`side`,`rot`,`opasitas` | peran, sisi dinding (`db`/`dt`), rotasi, opasitas 0–1 |
| `grup`,`subgrup` | pengelompokan (M2) |
| `kunci` | kunci objek sementara (dapat dibuka) |
| `latar` | penanda LATAR — terlindungi sampai pengguna memilih BUKA |
| `animasi` | `{jenis: naik-turun·ayun·denyut·kedip·putar, amplitudo, periode, fasa}` |
| `app` | pemetaan aplikasi/informasi (nanti) |

## Aturan
- **SIMPAN & KUNCI** = simpan + kunci objek sementara (bisa dibuka kembali).
- **LATAR** terlindungi sampai pengguna memilih **BUKA**.
- Migrasi: satu kali, aman utk scene lama, tidak menimpa setelan khusus
  pengguna/objek tak terkait; ditandai di `meta.migrasi`.
- Jangan mengganti scene aktif dgn contoh/data lama tanpa persetujuan.
- Scene harus bisa: disimpan (localStorage), dimuat, diimpor, diekspor (file).
- Kanvas 568×355 = acuan; ruangan default 9,6×9,6×2,6 (bukan batas kaku).
