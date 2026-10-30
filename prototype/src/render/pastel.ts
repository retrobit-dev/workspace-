// Alat warna untuk gaya soft-pastel (docs/04-KONTRAK-RENDERER.md)

export function hexKeRgb(hex: string): [number, number, number] {
  const h = hex.replace("#", "");
  return [
    parseInt(h.slice(0, 2), 16),
    parseInt(h.slice(2, 4), 16),
    parseInt(h.slice(4, 6), 16),
  ];
}

export function rgbKeHex(r: number, g: number, b: number): string {
  const c = (v: number) => Math.round(Math.min(255, Math.max(0, v))).toString(16).padStart(2, "0");
  return `#${c(r)}${c(g)}${c(b)}`;
}

/** f > 1 → lebih terang; f < 1 → lebih gelap. */
export function rawang(hex: string, f: number): string {
  const [r, g, b] = hexKeRgb(hex);
  if (f >= 1) {
    return rgbKeHex(r + (255 - r) * (f - 1), g + (255 - g) * (f - 1), b + (255 - b) * (f - 1));
  }
  return rgbKeHex(r * f, g * f, b * f);
}

/** Campur dua warna (t = 0..1 ke arah b). */
export function campur(a: string, b: string, t: number): string {
  const [r1, g1, b1] = hexKeRgb(a);
  const [r2, g2, b2] = hexKeRgb(b);
  return rgbKeHex(r1 + (r2 - r1) * t, g1 + (g2 - g1) * t, b1 + (b2 - b1) * t);
}
