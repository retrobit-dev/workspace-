# Gaya Visual "Ruang Kelas" — Acuan Rebuild Dari Awal

> Repo direset dari commit `9c3f59e` (rilis 0.16.43). SEMUA file lama masih
> bisa dipulihkan dari git history: `git checkout 9c3f59e -- <path>` atau
> `git reset --hard 9c3f59e`. Rilis & APK juga tetap ada di GitHub Releases
> (`v0.16.43-android`).

## Identitas gaya
- **Isometrik 3/4 top-down**, satu ruangan utuh (diorama), landscape 16:10.
- **Smooth soft-pastel** — ilustrasi kartun halus, gradien lembut, tepi
  anti-alias bersih, **BUKAN pixel art** (pixelate/blur DILARANG).
- Kanvas **1280×800** = koordinat tap 1:1 dgn piksel gambar.
- Kompas iso: kiri-atas=Barat, kanan-atas=Utara, kanan-bawah=Timur,
  kiri-bawah=Selatan; N=(0.894,−0.447), E=(0.894,0.447).

## Palet (dari art 0.16.43)
| Elemen | Siang | Malam |
|---|---|---|
| Dinding | `#626E88` | `#383E5A` |
| Kayu meja/furniture | `#D89874` | `#AF6949` |
| Lantai ungu (ubin diamond iso) | `#AC90C3` | `#655C8B` |
| Tirai | `#EFE6BF` | `#263867` |
| Kursi | `#403E4A` | `#303145` |
| Veil malam | — | `#3A4670` @ 14% |

## Aturan wajib (ringkas dari docs/aturan-art.md versi lama)
1. Tanpa pixelate & tanpa blur; zoom aset selalu smooth (web tanpa
   `image-rendering:pixelated`; Android `FilterQuality.High`).
2. Hapus/geser objek = **aset mandiri + compose** (`art/objects/`), objek
   lain tak boleh tersentuh; perubahan latar = **full-scene regen** dengan
   prompt yang menyebut eksplisit semua objek yang tetap ada.
3. **Patch lokal / cross-fade DILARANG** (terbukti gagal: band & gradasi).
4. Area ketuk = **bentuk objek** (poligon, `tap_polygons.json`), bukan bbox.
5. Siang: lampu meja OFF. Malam: tirai menutup SEMUA jendela, bulan wajib,
   bintang hanya saat langit cerah, veil `#3A4670` 14%.
6. Waktu & lokasi perangkat real sebagai referensi (fallback Jember
   −8.1728, 113.6995); bayangan dari arah & intensitas matahari real-time.
7. Klik jendela/langit → layar Cuaca (Open-Meteo → cache → contoh berlabel).
8. Native Android wajib (HTML hanya preview); landscape + dark only;
   satu aplikasi tampil pada satu waktu; tanpa sidebar/app bar.

## Prompt regen gaya (full-scene, EN)
```
Isometric 3/4 top-down view of a cozy study room corner, single-room diorama,
clean smooth vector illustration, soft pastel palette: muted blue-gray walls,
warm orange-brown wooden furniture, soft purple floor with isometric diamond
tiles, cream paper accents. [.. sebutkan SETIAP objek yang harus ada ..].
Soft ambient occlusion under furniture, subtle smooth gradients, flat cartoon
shading, crisp clean edges, no outlines, high detail. Landscape 16:10.
```
Negative: `pixel art, pixelated, blurry, photorealistic, 3D render, text,
watermark, people, harsh shadows, neon, noise`

## Referensi visual
- `style/ref-day.png` — komposisi penuh versi siang (1280×800)
- `style/ref-night.png` — komposisi penuh versi malam (1280×800)
