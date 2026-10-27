# Aturan ART — penghapusan objek & ketajaman tampilan

Aturan ini WAJIB diikuti setiap kali ada objek di gambar workspace yang
dihapus/digeser, dan setiap kali gambar dasar disentuh. (Ditetapkan user
pada era 0.16.2 setelah perbaikan bekas kalender.)

## A. Menghapus objek dari gambar

1. **Patch dari art asli, bukan dari versi yang sudah terdegradasi.**
   Selalu ambil gambar dasar terakhir yang benar dari git
   (`git show <commit>:art/rebuild-*.png`) — jangan menimpa file yang sudah
   pernah diedit berulang kali.
2. **Rekonstruksi mengikuti arah isometrik.** Isi area bekas objek dengan
   cross-fade dua kolom/strip referensi kiri–kanan di sekeliling objek,
   dengan **anchor tepi hasil ukur manual** (bukan deteksi otomatis — deteksi
   tepi sering salah kena objek yang mau dihapus).
   Contoh teruji (bekas kalender, kanvas 1280×800):
   kolom ref XL=991, XR=1089; zona Y 352–486; anchor awal-kayu
   malam (425, 442), siang (424, 443).
3. **Verifikasi ketat sebelum lanjut:** diff terhadap art asli harus
   **0 piksel berubah di luar zona patch** (ambang >40 per-sum-RGB).
   Kalau ada perubahan di luar zona → salah sumber/ salah metode, ulangi.
4. **Jangan pakai generate_image untuk hapus objek.** Hasilnya regenerasi
   menyeluruh (terukur 6% piksel bergeser di luar zona) — hanya aman untuk
   menggambar scene BARU dari nol.
5. **Hapus juga semua jejak non-gambar:** TileInfo/hotspot di `Data.kt`,
   fitur runtime di `Launcher.kt`, renderer di `tools/make_preview.py`,
   `tools/sim_overlay.py`, aturan di `tools/check_preview.mjs`, dan shot
   terkait di `docs/screens/`. Checker harus lulus sebelum commit.

## B. Menjaga tampilan tajam (anti-blur)

6. **Gambar dasar harus dipixelasikan** sebelum dipakai: blok piksel 2×2
   (resize 1280×800 → 640×400 lalu NEAREST balik) + kuantisasi palet
   (~200 warna, **tanpa dither**). Gradasi halus dari gambar AI membuat
   tampilan tampak blur saat diskalakan.
7. **Semua lapisan render wajib nearest-neighbor:**
   - Preview web: `#scene` dan `#fx image` → `image-rendering:pixelated`.
   - APK Compose: `filterQuality = FilterQuality.None` di drawImage.
   - Docs/sim: resize hanya dengan `Image.NEAREST` (zoom) — LANCZOS hanya
     untuk thumbnail pratinjau, bukan aset.
8. **Ukuran kanvas kelipatan grid piksel** (kini 1280×800, grid 32) supaya
   blok piksel tidak pecah saat ditata.

## C. Rutinitas setelah mengubah art

9. `python3 tools/make_scene.py && python3 tools/make_preview.py`
   → `node tools/check_preview.mjs` → `python3 tools/sim_overlay.py`
   → regen `docs/screens/` via `python3 tools/regen_docs.py`
   → commit → push → build CI → rilis → trigger preview
   (`.ci/preview.trigger`) → cek :4173 menyaji versi terbaru.
