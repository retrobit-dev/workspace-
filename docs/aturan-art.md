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

## B. Gaya visual (0.16.9: smooth soft-pastel)

6. **Gambar dasar TANPA pixelate** (permintaan user 0.16.9): art halus
   gaya soft-pastel persis referensi user — palet lembut, shading halus,
   detail jelas. Jangan kembalikan blok piksel 2×2 / kuantisasi blok.
7. **Semua lapisan render halus (smooth):**
   - Preview web: TANPA `image-rendering:pixelated` (checker men-fail
     bila CSS itu muncul lagi).
   - APK Compose: `filterQuality = FilterQuality.High` di drawImage.
   - Docs/sim: zoom `Image.NEAREST` hanya untuk crop verifikasi, bukan aset.
8. **Kanvas tetap 1280×800** — koordinat Data.kt sama persis dengan piksel
   gambar.

## C. Rutinitas setelah mengubah art

9. `python3 tools/make_scene.py && python3 tools/make_preview.py`
   → `node tools/check_preview.mjs` → `python3 tools/sim_overlay.py`
   → regen `docs/screens/` via `python3 tools/regen_docs.py`
   → commit → push → build CI → rilis → trigger preview
   (`.ci/preview.trigger`) → cek :4173 menyaji versi terbaru
   (bandingkan md5 `web/scene_*.png` vs yang disaji server).
