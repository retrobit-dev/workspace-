// Isometric Launcher — Prototipe 0.1.0 (M1)
// LIHAT/EDIT, ruangan kosong + balok, kamera geser+pinch, scene.json.
import { useEffect, useMemo, useRef, useState } from "react";
import type { Objek, SceneJson } from "./scene/tipe";
import { sceneKosong } from "./scene/sceneKosong";
import { eksporScene, imporScene, muatLokal, simpanLokal } from "./scene/validasi";
import { gambarScene, terapkanKamera } from "./render/renderer";
import { poligonBalok } from "./render/geo";
import { kena } from "./editor/hittest";
import {
  ASAL_X, ASAL_Y, des3, inversBidang, jepit3, KANVAS_H, KANVAS_W, PIKSEL, RASTER_H, RASTER_W,
  type Titik,
} from "./iso/proyeksi";

interface Kamera { x: number; y: number; zoom: number }

const KATALOG = [
  { label: "Meja", se: 1.6, sw: 0.9, sz: 0.75, warna: "#C98A5B" },
  { label: "Kursi", se: 0.5, sw: 0.5, sz: 0.9, warna: "#3F4652" },
  { label: "Lampu berdiri", se: 0.35, sw: 0.35, sz: 1.5, warna: "#2F3A46" },
  { label: "Rak", se: 1.2, sw: 0.35, sz: 1.1, warna: "#B47A4E" },
  { label: "Tanaman", se: 0.45, sw: 0.45, sz: 0.7, warna: "#5F7D4F" },
  { label: "Balok kosong", se: 0.6, sw: 0.6, sz: 0.6, warna: "#8C93A8" },
];

export default function App() {
  const kanvasRef = useRef<HTMLCanvasElement | null>(null);
  const [scene, setScene] = useState<SceneJson>(() => muatLokal() ?? sceneKosong());
  const [mode, setMode] = useState<"lihat" | "edit">("lihat");
  const [pilihId, setPilihId] = useState<string | null>(null);
  const [bukaLatar, setBukaLatar] = useState(false);
  const [kamera, setKamera] = useState<Kamera>({ x: 0, y: 0, zoom: 1 });
  const [info, setInfo] = useState("");
  const [tambahBuka, setTambahBuka] = useState(false);
  const sejarah = useRef<{ tumpuk: string[]; idx: number }>({ tumpuk: [JSON.stringify(muatLokal() ?? sceneKosong())], idx: 0 });

  // ---------- util scene ----------
  const commit = (berikut: SceneJson) => {
    const h = sejarah.current;
    h.tumpuk = h.tumpuk.slice(0, h.idx + 1);
    h.tumpuk.push(JSON.stringify(berikut));
    if (h.tumpuk.length > 60) h.tumpuk.shift();
    h.idx = h.tumpuk.length - 1;
    setScene(berikut);
  };
  const commitScene = (mutasi: (s: SceneJson) => SceneJson) => commit(mutasi(scene));
  const undo = () => {
    const h = sejarah.current;
    if (h.idx > 0) { h.idx--; setScene(JSON.parse(h.tumpuk[h.idx])); }
  };
  const redo = () => {
    const h = sejarah.current;
    if (h.idx < h.tumpuk.length - 1) { h.idx++; setScene(JSON.parse(h.tumpuk[h.idx])); }
  };

  const pilih = useMemo(() => scene.objects.find((o) => o.id === pilihId) ?? null, [scene, pilihId]);
  const bolehEdit = (o: Objek) => !(o.latar && !bukaLatar) && !o.kunci;

  const ubahObjek = (id: string, ubah: (o: Objek) => Objek) => {
    const o = scene.objects.find((x) => x.id === id);
    if (!o || !bolehEdit(o)) return;
    commitScene((s) => ({ ...s, objects: s.objects.map((x) => (x.id === id ? ubah(x) : x)) }));
  };

  const tambahObjek = (k: (typeof KATALOG)[number]) => {
    const id = `obj-${Date.now().toString(36)}-${Math.floor(Math.random() * 1e4)}`;
    const baru: Objek = {
      id, label: k.label,
      e: des3(scene.room.e / 2 - k.se / 2), w: des3(scene.room.w / 2 - k.sw / 2), z: 0,
      se: k.se, sw: k.sw, sz: k.sz,
      bentuk: "balok", warna: k.warna,
      layer: "main", urutan: scene.objects.length,
      role: "umum", side: null, rot: 0, opasitas: 1,
      grup: null, subgrup: null, kunci: false, latar: false, animasi: null, app: null,
    };
    commitScene((s) => ({ ...s, objects: [...s.objects, baru] }));
    setPilihId(id);
    setTambahBuka(false);
  };

  const hapusPilih = () => {
    if (!pilih || !bolehEdit(pilih)) return;
    commitScene((s) => ({ ...s, objects: s.objects.filter((o) => o.id !== pilih.id) }));
    setPilihId(null);
  };

  // ---------- render ----------
  useEffect(() => {
    const c = kanvasRef.current;
    if (!c) return;
    const ctx = c.getContext("2d");
    if (!ctx) return;
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.fillStyle = "#000";
    ctx.fillRect(0, 0, RASTER_W, RASTER_H);
    terapkanKamera(ctx, kamera);
    gambarScene(ctx, scene);
    if (pilih && pilih.bentuk === "balok") {
      const P = poligonBalok(pilih);
      ctx.setLineDash([4, 3]);
      ctx.strokeStyle = "rgba(255,236,170,0.95)";
      ctx.lineWidth = 1.4 / kamera.zoom;
      ctx.beginPath();
      ctx.moveTo(P.atas[0].x, P.atas[0].y);
      for (let i = 1; i < 4; i++) ctx.lineTo(P.atas[i].x, P.atas[i].y);
      ctx.closePath();
      ctx.stroke();
      ctx.setLineDash([]);
    }
  }, [scene, kamera, pilih]);

  // ---------- pointer / kamera ----------
  // raster = piksel kanvas 1136×710; dunia = satuan kanvas 568×355.
  const rasterPointer = useRef(new Map<number, Titik>());
  interface Gestur {
    jenis: "" | "pan" | "pinch" | "objek";
    mulaiRaster: Titik; cam0: Kamera; zoom0: number;
    jarak0: number; mid0: Titik; duniaMid0: Titik;
    objId: string | null; objAwal: { e: number; w: number } | null;
    duniaMulai: Titik; pindah: boolean;
  }
  const gestur = useRef<Gestur | null>(null);

  const keRaster = (e: { clientX: number; clientY: number }): Titik => {
    const c = kanvasRef.current!;
    const r = c.getBoundingClientRect();
    return {
      x: ((e.clientX - r.left) / r.width) * RASTER_W,
      y: ((e.clientY - r.top) / r.height) * RASTER_H,
    };
  };
  /** raster piksel → dunia (satuan kanvas) memakai kamera tertentu. */
  const keDuniaDgn = (r: Titik, cam: Kamera): Titik => ({
    x: ASAL_X + (r.x / PIKSEL - KANVAS_W / 2) / cam.zoom - cam.x,
    y: ASAL_Y + (r.y / PIKSEL - KANVAS_H / 2) / cam.zoom - cam.y,
  });
  const keDunia = (r: Titik): Titik => keDuniaDgn(r, kamera);

  const jarakDua = (m: Map<number, Titik>): number => {
    const a = [...m.values()];
    return Math.hypot(a[0].x - a[1].x, a[0].y - a[1].y);
  };
  const tengahDua = (m: Map<number, Titik>): Titik => {
    const a = [...m.values()];
    return { x: (a[0].x + a[1].x) / 2, y: (a[0].y + a[1].y) / 2 };
  };

  const padaTurun = (e: React.PointerEvent) => {
    (e.target as Element).setPointerCapture?.(e.pointerId);
    const r = keRaster(e);
    rasterPointer.current.set(e.pointerId, r);
    if (rasterPointer.current.size === 2) {
      const mid = tengahDua(rasterPointer.current);
      gestur.current = {
        jenis: "pinch", mulaiRaster: r, cam0: kamera, zoom0: kamera.zoom,
        jarak0: jarakDua(rasterPointer.current), mid0: mid,
        duniaMid0: keDuniaDgn(mid, kamera),
        objId: null, objAwal: null, duniaMulai: r, pindah: true,
      };
    } else if (rasterPointer.current.size === 1) {
      const dunia = keDunia(r);
      const kenaO = kena(scene, dunia);
      const bisa = mode === "edit" && kenaO && bolehEdit(kenaO);
      gestur.current = {
        jenis: bisa ? "objek" : "pan",
        mulaiRaster: r, cam0: kamera, zoom0: kamera.zoom,
        jarak0: 0, mid0: r, duniaMid0: dunia,
        objId: bisa ? kenaO!.id : null,
        objAwal: bisa ? { e: kenaO!.e, w: kenaO!.w } : null,
        duniaMulai: dunia, pindah: false,
      };
    }
  };

  const padaGerak = (e: React.PointerEvent) => {
    const g = gestur.current;
    if (!g) return;
    const r = keRaster(e);
    rasterPointer.current.set(e.pointerId, r);
    if (g.jenis === "pinch" && rasterPointer.current.size >= 2) {
      const jarak = jarakDua(rasterPointer.current);
      const mid = tengahDua(rasterPointer.current);
      const zoom = jepit3(g.cam0.zoom * (jarak / Math.max(g.jarak0, 1)), 0.4, 5);
      // titik dunia di bawah cubit awal tetap di bawah cubit kini:
      const camX = ASAL_X + (mid.x / PIKSEL - KANVAS_W / 2) / zoom - g.duniaMid0.x;
      const camY = ASAL_Y + (mid.y / PIKSEL - KANVAS_H / 2) / zoom - g.duniaMid0.y;
      setKamera({ zoom, x: camX, y: camY });
    } else if (g.jenis === "pan") {
      const dx = r.x - g.mulaiRaster.x, dy = r.y - g.mulaiRaster.y;
      if (Math.abs(dx) + Math.abs(dy) > 5) g.pindah = true;
      setKamera({
        zoom: g.zoom0,
        x: g.cam0.x - dx / (PIKSEL * g.zoom0),
        y: g.cam0.y - dy / (PIKSEL * g.zoom0),
      });
    } else if (g.jenis === "objek" && g.objId && g.objAwal) {
      if (Math.abs(r.x - g.mulaiRaster.x) + Math.abs(r.y - g.mulaiRaster.y) > 4) g.pindah = true;
      const dunia = keDunia(r); // kamera tak berubah saat geser objek
      const awal = inversBidang(g.duniaMulai.x, g.duniaMulai.y, 0);
      const kini = inversBidang(dunia.x, dunia.y, 0);
      const ne = jepit3(g.objAwal.e + (kini.e - awal.e), -2, scene.room.e);
      const nw = jepit3(g.objAwal.w + (kini.w - awal.w), -2, scene.room.w);
      setScene((s) => ({
        ...s,
        objects: s.objects.map((x) => (x.id === g.objId ? { ...x, e: ne, w: nw } : x)),
      }));
    }
  };

  const padaNaik = (e: React.PointerEvent) => {
    const g = gestur.current;
    rasterPointer.current.delete(e.pointerId);
    if (g && g.jenis === "objek") {
      if (!g.pindah) {
        setPilihId(g.objId);
        setInfo("");
      } else {
        // komit hasil geser ke sejarah
        setScene((s) => {
          const h = sejarah.current;
          h.tumpuk = h.tumpuk.slice(0, h.idx + 1);
          h.tumpuk.push(JSON.stringify(s));
          if (h.tumpuk.length > 60) h.tumpuk.shift();
          h.idx = h.tumpuk.length - 1;
          return s;
        });
      }
    } else if (g && g.jenis === "pan" && !g.pindah) {
      const dunia = keDunia(keRaster(e));
      const kenaO = kena(scene, dunia);
      if (kenaO) {
        if (mode === "edit" && !(kenaO.latar && !bukaLatar)) setPilihId(kenaO.id);
        else setInfo(mode === "lihat" && kenaO.app ? `${kenaO.label} → ${kenaO.app}` : "");
      } else {
        setPilihId(null);
        setInfo("");
      }
    }
    if (rasterPointer.current.size === 0) gestur.current = null;
  };

  const padaRoda = (e: React.WheelEvent) => {
    if (e.ctrlKey || e.metaKey) {
      setKamera((k) => ({ ...k, zoom: jepit3(k.zoom * Math.exp(-e.deltaY * 0.002), 0.4, 5) }));
    } else {
      setKamera((k) => ({
        ...k,
        x: k.x + e.deltaX / (PIKSEL * k.zoom),
        y: k.y + e.deltaY / (PIKSEL * k.zoom),
      }));
    }
  };

  // ---------- UI ----------
  const tombol: React.CSSProperties = {
    background: "#1d1f26", color: "#e8e4d8", border: "2px solid #5a4a35",
    borderRadius: 8, padding: "6px 12px", font: "600 13px ui-monospace,monospace",
    letterSpacing: "0.08em", cursor: "pointer",
  };
  const nUtama = scene.objects.filter((o) => !o.latar).length;
  const nLatar = scene.objects.length - nUtama;

  return (
    <div style={{ position: "fixed", inset: 0, background: "#000", display: "flex", flexDirection: "column" }}>
      <div style={{ display: "flex", gap: 8, padding: "8px 10px", flexWrap: "wrap", alignItems: "center" }}>
        <button style={tombol} onClick={() => { setMode(mode === "lihat" ? "edit" : "lihat"); setPilihId(null); }}>
          MODE: {mode === "lihat" ? "LIHAT" : "EDIT"}
        </button>
        <button style={tombol} onClick={undo}>↶ UNDO</button>
        <button style={tombol} onClick={redo}>↷ REDO</button>
        <div style={{ ...tombol, cursor: "default" }}>OBJEK {nUtama} (+{nLatar} LATAR)</div>
        {mode === "edit" && (
          <div style={{ position: "relative" }}>
            <button style={tombol} onClick={() => setTambahBuka(!tambahBuka)}>+ TAMBAH</button>
            {tambahBuka && (
              <div style={{ position: "absolute", top: "110%", left: 0, zIndex: 30, background: "#15161c", border: "2px solid #5a4a35", borderRadius: 10, padding: 6, display: "flex", flexDirection: "column", gap: 4 }}>
                {KATALOG.map((k) => (
                  <button key={k.label} style={{ ...tombol, textAlign: "left" }} onClick={() => tambahObjek(k)}>
                    {k.label} <span style={{ opacity: 0.6 }}>{k.se}×{k.sw}×{k.sz}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
        <button style={tombol} onClick={() => {
          simpanLokal(scene);
          commitScene((s) => ({ ...s, objects: s.objects.map((o) => (o.latar ? o : { ...o, kunci: true })) }));
          setInfo("Tersimpan & objek dikunci sementara.");
        }}>💾 SIMPAN & KUNCI</button>
        <button style={tombol} onClick={() => {
          const s = muatLokal();
          if (s) { commit(s); setInfo("Dimuat dari penyimpanan."); } else setInfo("Tidak ada simpanan.");
        }}>MUAT</button>
        <button style={tombol} onClick={() => eksporScene(scene)}>EKSPOR</button>
        <label style={{ ...tombol, display: "inline-block" }}>
          IMPOR
          <input type="file" accept="application/json" style={{ display: "none" }} onChange={async (ev) => {
            const f = ev.target.files?.[0];
            if (!f) return;
            try {
              const { imporScene: imp } = await import("./scene/validasi");
              commit(await imp(f));
              setInfo("Scene diimpor & divalidasi.");
            } catch (err) { setInfo(`Impor gagal: ${(err as Error).message}`); }
            ev.target.value = "";
          }} />
        </label>
        <button style={{ ...tombol, borderColor: bukaLatar ? "#7ba05b" : "#5a4a35" }} onClick={() => setBukaLatar(!bukaLatar)}>
          {bukaLatar ? "LATAR: BUKA" : "LATAR: KUNCI"}
        </button>
        {info && <div style={{ color: "#f0d9a0", font: "12px ui-monospace,monospace" }}>{info}</div>}
      </div>

      <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", minHeight: 0 }}>
        <canvas
          ref={kanvasRef}
          width={RASTER_W}
          height={RASTER_H}
          style={{
            background: "#000",
            width: "min(100vw, calc(100vh * 1.6))",
            aspectRatio: `${RASTER_W} / ${RASTER_H}`,
            touchAction: "none",
            cursor: mode === "edit" ? "crosshair" : "grab",
          }}
          onPointerDown={padaTurun}
          onPointerMove={padaGerak}
          onPointerUp={padaNaik}
          onPointerCancel={padaNaik}
          onWheel={padaRoda}
        />
      </div>

      {pilih && (
        <PanelObjek
          objek={pilih}
          bolehEdit={bolehEdit(pilih)}
          ubah={(ubah) => ubahObjek(pilih.id, ubah)}
          hapus={hapusPilih}
          tutup={() => setPilihId(null)}
        />
      )}
    </div>
  );
}

// ---------- Panel objek ----------
interface FieldAngka { nama: "e" | "w" | "z" | "se" | "sw" | "sz"; nilai: number; min: number; max: number; langkah?: number; bolehEdit: boolean; ubah: (v: number) => void }

function FieldAngka({ nama, nilai, min, max, langkah = 0.1, bolehEdit, ubah }: FieldAngka) {
  const kecil: React.CSSProperties = {
    width: 26, height: 26, background: "#1d1f26", color: "#e8e4d8",
    border: "1px solid #5a4a35", borderRadius: 6, cursor: bolehEdit ? "pointer" : "not-allowed",
  };
  const masukan: React.CSSProperties = {
    width: 66, background: "#101116", color: "#f0ead8", border: "1px solid #44403a",
    borderRadius: 6, font: "12px ui-monospace,monospace", textAlign: "center", padding: "3px 2px",
  };
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 2, alignItems: "center" }}>
      <div style={{ color: "#b9b2a0", font: "10px ui-monospace,monospace" }}>{nama}</div>
      <div style={{ display: "flex", gap: 2, alignItems: "center" }}>
        <button style={kecil} disabled={!bolehEdit} onClick={() => ubah(jepit3(nilai - langkah, min, max))}>−</button>
        <input style={masukan} disabled={!bolehEdit} defaultValue={nilai.toFixed(3)}
          key={`${nama}-${nilai}`}
          onChange={(ev) => {
            const v = parseFloat(ev.target.value.replace(",", "."));
            if (Number.isFinite(v)) ubah(jepit3(v, min, max));
          }} />
        <button style={kecil} disabled={!bolehEdit} onClick={() => ubah(jepit3(nilai + langkah, min, max))}>+</button>
      </div>
    </div>
  );
}

function PanelObjek(props: {
  objek: Objek; bolehEdit: boolean;
  ubah: (ubah: (o: Objek) => Objek) => void;
  hapus: () => void; tutup: () => void;
}) {
  const { objek: o, bolehEdit } = props;
  const kecil: React.CSSProperties = {
    width: 26, height: 26, background: "#1d1f26", color: "#e8e4d8",
    border: "1px solid #5a4a35", borderRadius: 6, cursor: bolehEdit ? "pointer" : "not-allowed",
  };
  const masukan: React.CSSProperties = {
    width: 90, background: "#101116", color: "#f0ead8", border: "1px solid #44403a",
    borderRadius: 6, font: "12px ui-monospace,monospace", padding: "3px 4px",
  };
  return (
    <div style={{ display: "flex", gap: 14, alignItems: "center", padding: "8px 12px", background: "#101116", borderTop: "2px solid #5a4a35", flexWrap: "wrap", zIndex: 20 }}>
      <b style={{ color: "#f0ead8", font: "600 13px ui-monospace,monospace", minWidth: 150 }}>
        {o.label}{o.kunci ? " 🔒" : ""}{o.latar ? " · LATAR" : ""}
      </b>
      <FieldAngka nama="e" nilai={o.e} min={-2} max={40} bolehEdit={bolehEdit} ubah={(v) => props.ubah((x) => ({ ...x, e: v }))} />
      <FieldAngka nama="w" nilai={o.w} min={-2} max={40} bolehEdit={bolehEdit} ubah={(v) => props.ubah((x) => ({ ...x, w: v }))} />
      <FieldAngka nama="z" nilai={o.z} min={0} max={12} bolehEdit={bolehEdit} ubah={(v) => props.ubah((x) => ({ ...x, z: v }))} />
      <FieldAngka nama="se" nilai={o.se} min={0.001} max={20} bolehEdit={bolehEdit} ubah={(v) => props.ubah((x) => ({ ...x, se: v }))} />
      <FieldAngka nama="sw" nilai={o.sw} min={0.001} max={20} bolehEdit={bolehEdit} ubah={(v) => props.ubah((x) => ({ ...x, sw: v }))} />
      <FieldAngka nama="sz" nilai={o.sz} min={0.001} max={12} bolehEdit={bolehEdit} ubah={(v) => props.ubah((x) => ({ ...x, sz: v }))} />
      <div style={{ display: "flex", flexDirection: "column", gap: 2, alignItems: "center" }}>
        <div style={{ color: "#b9b2a0", font: "10px ui-monospace,monospace" }}>warna</div>
        <input type="color" value={o.warna} disabled={!bolehEdit} style={{ width: 42, height: 26, background: "none", border: "none" }}
          onChange={(ev) => props.ubah((x) => ({ ...x, warna: ev.target.value }))} />
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 2, alignItems: "center" }}>
        <div style={{ color: "#b9b2a0", font: "10px ui-monospace,monospace" }}>layer</div>
        <select value={o.layer} disabled={!bolehEdit} style={masukan}
          onChange={(ev) => props.ubah((x) => ({ ...x, layer: ev.target.value as Objek["layer"] }))}>
          <option value="underlay">underlay</option>
          <option value="main">main</option>
          <option value="overlay">overlay</option>
        </select>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 2, alignItems: "center" }}>
        <div style={{ color: "#b9b2a0", font: "10px ui-monospace,monospace" }}>urutan</div>
        <div style={{ display: "flex", gap: 2 }}>
          <button style={kecil} disabled={!bolehEdit} onClick={() => props.ubah((x) => ({ ...x, urutan: x.urutan - 1 }))}>−</button>
          <div style={{ ...masukan, width: 30, textAlign: "center" }}>{o.urutan}</div>
          <button style={kecil} disabled={!bolehEdit} onClick={() => props.ubah((x) => ({ ...x, urutan: x.urutan + 1 }))}>+</button>
        </div>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 2, alignItems: "center" }}>
        <div style={{ color: "#b9b2a0", font: "10px ui-monospace,monospace" }}>opasitas</div>
        <input type="range" min={0.05} max={1} step={0.05} value={o.opasitas ?? 1} disabled={!bolehEdit}
          onChange={(ev) => props.ubah((x) => ({ ...x, opasitas: parseFloat(ev.target.value) }))} />
      </div>
      <button style={kecil} title="kunci" onClick={() => props.ubah((x) => ({ ...x, kunci: !x.kunci }))}>{o.kunci ? "🔓" : "🔒"}</button>
      <button style={{ ...kecil, width: "auto", padding: "0 10px", color: "#ff9d9d", borderColor: "#7a3b3b" }} disabled={!bolehEdit} onClick={props.hapus}>HAPUS</button>
      <button style={kecil} onClick={props.tutup}>✕</button>
    </div>
  );
}
