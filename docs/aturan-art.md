# Aturan ART — penghapusan objek & ketajaman tampilan

Aturan ini WAJIB diikuti setiap kali ada objek di gambar workspace yang
dihapus/digeser, dan setiap kali gambar dasar disentuh. (Ditetapkan user
pada era 0.16.2; direvisi 0.16.5 setelah 0.16.3–0.16.5 membuktikan mana
metode yang benar-benar bersih.)

## A. Menghapus objek dari gambar

1. **JANGAN patch lokal / cross-fade.** Metode strip-referensi kiri–kanan
   (dicoba di era 0.16.2) terbukti GAGAL: menyisakan band dan gradasi
   bekas patch, tampilan tidak tajam, dan ditolak user. Metode ini tidak
   dipakai lagi dalam kondisi apa pun.
2. **Metode resmi: full-scene regen via generate_image.** Ambil gambar
   dasar terakhir yang benar dari git
   (`git show <commit>:art/rebuild-*.png`) sebagai sumber, lalu regen
   SELURUH scene dengan prompt yang menyebut eksplisit setiap objek yang
   harus tetap ada (papan tulis, pin board, rak, meja, kursi, tanaman,
   lampu, laptop, buku, kalkulator, rail, tirai — plus status jendela/
   tirai yang diminta) dan objek yang dihapus tidak disebut sama sekali.
3. **Pixelate dengan resep FINAL v2** (0.16.8, anti-blur; skrip
   `/tmp/sharp_recipe.py` pola: resize → 1280×800 LANCZOS → Color 1.08 →
   Contrast 1.04 → UnsharpMask(2, 80, 2) → quantize(150, MEDIANCUT,
   Dither.NONE) pada 1280×800 PENUH (tanpa turun ke 640×400 yang
   menghaluskan detail; blok 2×2 era lama dihapus karena terlihat blur).
4. **Verifikasi ketat sebelum lanjut:**
   - Crop/zoom area yang berubah (jendela, rail, zona bekas objek) dan
     bandingkan sebelum–sesudah;
   - Cek drift seluruh scene: semua objek utuh, posisi masih cocok dengan
     hotspot/quads di `Data.kt`, grid 32 px sejajar;
   - Sim day & night dicek visual keduanya.
   Deviasi yang ditemukan (objek bergeser/berubah) **diungkapkan ke user**
   saat melapor.
5. **Hapus juga semua jejak non-gambar:** TileInfo/hotspot di `Data.kt`,
   fitur runtime di `Launcher.kt`, renderer di `tools/make_preview.py`,
   `tools/sim_overlay.py`, aturan di `tools/check_preview.mjs`, dan shot
   terkait di `docs/screens/`. Checker harus lulus sebelum commit.

## B. Menjaga tampilan tajam (anti-blur)

6. **Gambar dasar selalu hasil resep pixelate di A.3.** Gradasi halus
   dari gambar AI membuat tampilan tampak blur saat diskalakan; jangan
   pernah memakai gambar AI mentah sebagai aset.
7. **Semua lapisan render wajib nearest-neighbor:**
   - Preview web: `#scene` dan `#fx image` → `image-rendering:pixelated`.
   - APK Compose: `filterQuality = FilterQuality.None` di drawImage.
   - Docs/sim: resize hanya dengan `Image.NEAREST` (zoom) — LANCZOS hanya
     untuk tahap awal resep pixelate & thumbnail pratinjau, bukan aset.
8. **Ukuran kanvas kelipatan grid piksel** (kini 1280×800, grid 32) supaya
   blok piksel tidak pecah saat ditata.

## C. Rutinitas setelah mengubah art

9. `python3 tools/make_scene.py && python3 tools/make_preview.py`
   → `node tools/check_preview.mjs` → `python3 tools/sim_overlay.py`
   → regen `docs/screens/` via `python3 tools/regen_docs.py`
   → commit → push → build CI → rilis → trigger preview
   (`.ci/preview.trigger`) → cek :4173 menyaji versi terbaru
   (bandingkan md5 `web/scene_*.png` vs yang disaji server).
