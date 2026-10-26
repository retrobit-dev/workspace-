/* ==========================================================================
   RUANG KELAS — pixel workspace prototype
   ========================================================================== */

/* ------------------------------ icons ---------------------------------- */

const ICONS = {
  logo: `
    <rect x="1" y="2" width="14" height="2"/><rect x="1" y="2" width="2" height="9"/>
    <rect x="13" y="2" width="2" height="9"/><rect x="1" y="9" width="14" height="2"/>
    <rect x="6" y="12" width="4" height="2"/><rect x="3" y="14" width="10" height="1"/>
    <rect x="4" y="5" width="2" height="2"/><rect x="8" y="5" width="4" height="2"/>`,
  home: `
    <rect x="6" y="1" width="4" height="1"/><rect x="4" y="2" width="8" height="1"/>
    <rect x="2" y="4" width="12" height="1"/><rect x="1" y="5" width="14" height="2"/>
    <rect x="2" y="7" width="12" height="3"/><rect x="2" y="10" width="4" height="5"/>
    <rect x="10" y="10" width="4" height="5"/><rect x="2" y="15" width="12" height="1"/>`,
  book: `
    <rect x="2" y="2" width="12" height="13"/><rect x="5" y="2" width="9" height="13"/>
    <rect x="2" y="2" width="3" height="13"/><rect x="1" y="4" width="1" height="9"/>
    <rect x="7" y="5" width="5" height="1"/><rect x="7" y="8" width="4" height="1"/>`,
  check: `
    <rect x="1" y="8" width="2" height="2"/><rect x="3" y="10" width="2" height="2"/>
    <rect x="5" y="8" width="2" height="2"/><rect x="7" y="6" width="2" height="2"/>
    <rect x="9" y="4" width="2" height="2"/><rect x="11" y="2" width="2" height="2"/>
    <rect x="13" y="1" width="2" height="2"/>`,
  chart: `
    <rect x="1" y="1" width="1" height="14"/><rect x="1" y="14" width="14" height="1"/>
    <rect x="3" y="9" width="3" height="5"/><rect x="7" y="6" width="3" height="8"/>
    <rect x="11" y="3" width="3" height="11"/>`,
  calendar: `
    <rect x="4" y="0" width="2" height="4"/><rect x="10" y="0" width="2" height="4"/>
    <rect x="1" y="3" width="14" height="2"/><rect x="1" y="6" width="2" height="9"/>
    <rect x="13" y="6" width="2" height="9"/><rect x="1" y="14" width="14" height="1"/>
    <rect x="5" y="7" width="2" height="2"/><rect x="9" y="7" width="2" height="2"/>
    <rect x="5" y="11" width="2" height="2"/><rect x="9" y="11" width="2" height="2"/>`,
  stack: `
    <rect x="2" y="2" width="12" height="2"/><rect x="4" y="6" width="8" height="2"/>
    <rect x="6" y="10" width="4" height="2"/><rect x="7" y="13" width="2" height="1"/>`,
  folder: `
    <rect x="1" y="3" width="6" height="2"/><rect x="1" y="5" width="14" height="9"/>
    <rect x="1" y="14" width="14" height="1"/><rect x="3" y="7" width="2" height="4"/>`,
  plus: `<rect x="7" y="2" width="2" height="12"/><rect x="2" y="7" width="12" height="2"/>`,
  bell: `
    <rect x="7" y="1" width="2" height="1"/><rect x="5" y="2" width="6" height="2"/>
    <rect x="3" y="4" width="10" height="7"/><rect x="2" y="11" width="12" height="2"/>
    <rect x="7" y="13" width="2" height="2"/>`,
  scan: `
    <rect x="1" y="1" width="5" height="2"/><rect x="1" y="1" width="2" height="5"/>
    <rect x="10" y="1" width="5" height="2"/><rect x="13" y="1" width="2" height="5"/>
    <rect x="1" y="13" width="5" height="2"/><rect x="1" y="10" width="2" height="5"/>
    <rect x="10" y="13" width="5" height="2"/><rect x="13" y="10" width="2" height="5"/>`,
  download: `
    <rect x="7" y="1" width="2" height="9"/><rect x="5" y="5" width="2" height="2"/>
    <rect x="9" y="5" width="2" height="2"/><rect x="6" y="7" width="4" height="2"/>
    <rect x="2" y="12" width="12" height="2"/>`,
  close: `
    <rect x="7" y="1" width="2" height="14" transform="rotate(45 8 8)"/>
    <rect x="7" y="1" width="2" height="14" transform="rotate(-45 8 8)"/>`,
  face: `
    <rect x="4" y="1" width="8" height="2"/><rect x="3" y="3" width="10" height="5"/>
    <rect x="5" y="8" width="6" height="1"/><rect x="2" y="10" width="12" height="5"/>`,
  clock: `
    <rect x="5" y="0" width="6" height="2"/><rect x="3" y="2" width="10" height="2"/>
    <rect x="1" y="4" width="2" height="8"/><rect x="13" y="4" width="2" height="8"/>
    <rect x="3" y="12" width="10" height="2"/><rect x="5" y="14" width="6" height="2"/>
    <rect x="7" y="4" width="2" height="5"/><rect x="9" y="8" width="3" height="2"/>`,
  star: `
    <rect x="7" y="1" width="2" height="4"/><rect x="1" y="5" width="14" height="2"/>
    <rect x="3" y="7" width="10" height="2"/><rect x="4" y="9" width="3" height="3"/>
    <rect x="9" y="9" width="3" height="3"/>`
};

function hydrateIcons(root = document) {
  root.querySelectorAll("[data-icon]").forEach((el) => {
    if (el.dataset.iconReady) return;
    const name = el.dataset.icon;
    const shapes = ICONS[name];
    if (!shapes) return;
    const size = el.dataset.size || 16;
    el.innerHTML = `<svg viewBox="0 0 16 16" width="${size}" height="${size}" fill="currentColor" aria-hidden="true" shape-rendering="crispEdges">${shapes}</svg>`;
    el.dataset.iconReady = "1";
  });
}

/* ------------------------------ palette -------------------------------- */

const PALETTE = [
  { name: "Ink", hex: "#0E0D1A" },
  { name: "Night", hex: "#15142A" },
  { name: "Panel", hex: "#272647" },
  { name: "Panel 2", hex: "#33325A" },
  { name: "Cream", hex: "#F8E9C8" },
  { name: "Muted", hex: "#A79EC4" },
  { name: "Amber", hex: "#F2AB3F" },
  { name: "Coral", hex: "#EF6A5B" },
  { name: "Teal", hex: "#4FA49A" },
  { name: "Mint", hex: "#9FD8B0" },
  { name: "Sky", hex: "#77B8D9" },
  { name: "Plum", hex: "#A58ECB" }
];

/* ------------------------------- data ---------------------------------- */

const STUDENTS = [
  "Aditya Pratama", "Bunga Lestari", "Citra Maharani", "Dimas Saputra",
  "Eka Wulandari", "Fajar Nugroho", "Gita Puspita", "Hendra Wijaya",
  "Intan Permata", "Joko Susilo", "Kirana Dewi", "Lukman Hakim"
];

const CLASSES = {
  "7A": { subject: "IPA", students: STUDENTS },
  "7B": { subject: "IPA", students: STUDENTS },
  "8A": { subject: "IPA", students: STUDENTS },
  "8B": { subject: "IPA", students: STUDENTS }
};

const JURNAL_SEED = [
  {
    id: 4, date: 26, day: "OKT", kelas: "8A", time: "09:15", jam: "3–4",
    materi: "Sistem pencernaan manusia", note: "Diskusi kelompok lancar, 2 siswa perlu pendampingan.",
    hadir: 30, total: 32
  },
  {
    id: 3, date: 26, day: "OKT", kelas: "7A", time: "07:00", jam: "1–2",
    materi: "Klasifikasi makhluk hidup", note: "Kuis pembuka, rata-rata 78. Lanjut kunci determinasi.",
    hadir: 31, total: 32
  },
  {
    id: 2, date: 23, day: "OKT", kelas: "8B", time: "10:35", jam: "5–6",
    materi: "Zat aditif & adiktif", note: "Praktikum uji pewarna makanan. Alat aman, hasil tercatat.",
    hadir: 29, total: 31
  },
  {
    id: 1, date: 21, day: "OKT", kelas: "7B", time: "08:20", jam: "2–3",
    materi: "Ciri-ciri makhluk hidup", note: "Observasi taman sekolah, siswa antusias.",
    hadir: 30, total: 31
  }
];

const JADWAL = {
  Senin: [
    { time: "07:00", end: "08:20", kelas: "7A", subject: "IPA", room: "Ruang 12", materi: "Klasifikasi makhluk hidup" },
    { time: "09:15", end: "10:35", kelas: "8A", subject: "IPA", room: "Lab Sains", materi: "Sistem pencernaan", now: true },
    { time: "11:00", end: "12:20", kelas: "7B", subject: "IPA", room: "Ruang 9", materi: "Ciri makhluk hidup" }
  ],
  Selasa: [
    { time: "07:45", end: "09:05", kelas: "8B", subject: "IPA", room: "Lab Sains", materi: "Zat aditif" },
    { time: "10:00", end: "11:20", kelas: "9A", subject: "IPA", room: "Ruang 14", materi: "Listrik statis" }
  ],
  Rabu: [
    { time: "07:00", end: "08:20", kelas: "7A", subject: "IPA", room: "Ruang 12", materi: "Kunci determinasi" },
    { time: "08:20", end: "09:40", kelas: "7B", subject: "IPA", room: "Ruang 9", materi: "Kunci determinasi" },
    { time: "10:35", end: "11:55", kelas: "8A", subject: "IPA", room: "Lab Sains", materi: "Uji amilum" }
  ],
  Kamis: [
    { time: "07:45", end: "09:05", kelas: "8B", subject: "IPA", room: "Ruang 11", materi: "Sistem pernapasan" }
  ],
  Jumat: [
    { time: "07:00", end: "08:20", kelas: "7A", subject: "IPA", room: "Ruang 12", materi: "Proyek herbarium" },
    { time: "08:20", end: "09:40", kelas: "9A", subject: "IPA", room: "Ruang 14", materi: "Praktikum rangkaian" }
  ]
};

const STATUS_LABEL = { H: "Hadir", S: "Sakit", I: "Izin", A: "Alfa" };
const STATUS_CLASS = { H: "on-h", S: "on-s", I: "on-i", A: "on-a" };
const KKM = 75;

/* ------------------------------- state --------------------------------- */

const LS_KEY = "ruangkelas.prototype.v1";

const state = {
  screen: "home",
  journalFilter: "all",
  absClass: "7A",
  scoreClass: "7A",
  day: "Senin",
  jurnal: [...JURNAL_SEED],
  absensi: {},
  nilai: {}
};

Object.keys(CLASSES).forEach((k) => {
  state.absensi[k] = STUDENTS.map(() => "H");
  state.nilai[k] = STUDENTS.map((nama, i) => {
    const base = 68 + ((i * 7 + k.charCodeAt(0)) % 26);
    return {
      nama,
      tugas: Math.min(100, base + 4),
      uts: Math.min(100, base - 2 + (i % 3)),
      uas: Math.min(100, base + 2 - (i % 4))
    };
  });
});

function restore() {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) return;
    const saved = JSON.parse(raw);
    if (Array.isArray(saved.jurnal)) state.jurnal = saved.jurnal;
    if (saved.absensi) state.absensi = { ...state.absensi, ...saved.absensi };
    if (saved.nilai) state.nilai = { ...state.nilai, ...saved.nilai };
  } catch (e) { /* prototype: ignore corrupt storage */ }
}

function persist() {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify({
      jurnal: state.jurnal, absensi: state.absensi, nilai: state.nilai
    }));
  } catch (e) { /* ignore quota errors */ }
}

/* ------------------------------ helpers -------------------------------- */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

let toastTimer;
function toast(msg) {
  const el = $("#toast");
  if (!el) return;
  el.textContent = msg;
  el.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("show"), 2400);
}

function na(n) {
  return Math.round(n.tugas * 0.4 + n.uts * 0.3 + n.uas * 0.3);
}

function naClass(value) {
  if (value < KKM) return "bad";
  if (value < KKM + 8) return "warn";
  return "";
}

/* ------------------------------ screens -------------------------------- */

function showScreen(name) {
  state.screen = name;
  const isKit = name === "kit";

  $$(".screen").forEach((el) => {
    el.classList.toggle("is-active", el.dataset.panel === name);
  });

  $$(".tab").forEach((el) => {
    el.setAttribute("aria-selected", String(el.dataset.screen === name));
  });

  $$(".nav-btn").forEach((el) => {
    if (el.dataset.goto === name) el.setAttribute("aria-current", "page");
    else el.removeAttribute("aria-current");
  });

  $("#phoneWrap").style.display = isKit ? "none" : "";
  $("#stageBody").classList.toggle("kit-mode", isKit);
  $$(".kit").forEach((el) => el.classList.toggle("is-active", isKit));

  const scroll = $(`.screen[data-panel="${name}"] .screen-scroll`);
  if (scroll) scroll.scrollTop = 0;
}

/* ------------------------------- jurnal -------------------------------- */

function renderJurnal() {
  const list = $("#journalList");
  const rows = state.jurnal.filter((j) => state.journalFilter === "all" || j.kelas === state.journalFilter);

  if (!rows.length) {
    list.innerHTML = `<p class="empty">BELUM ADA CATATAN<br>UNTUK KELAS INI.<br><br>TAP TOMBOL DI BAWAH.</p>`;
    return;
  }

  list.innerHTML = rows.map((j, i) => {
    const tone = i % 4 === 1 ? "b" : i % 4 === 2 ? "c" : i % 4 === 3 ? "d" : "";
    return `
      <article class="journal-item">
        <div class="date-badge"><b>${j.date}</b><span>${j.day}</span></div>
        <div class="journal-body">
          <div class="line1">
            <span class="kelas-tag ${tone}">${j.kelas}</span>
            <span class="time-tag">JAM ${j.jam} · ${j.time}</span>
          </div>
          <h4>${j.materi}</h4>
          <p>${j.note}</p>
          <span class="attend-pill">${j.hadir}/${j.total} HADIR</span>
        </div>
      </article>`;
  }).join("");
}

/* ------------------------------- absensi ------------------------------- */

function renderAbsensi() {
  const statuses = state.absensi[state.absClass];
  const counts = { H: 0, S: 0, I: 0, A: 0 };
  statuses.forEach((s) => { counts[s] = (counts[s] || 0) + 1; });

  $("#absSummary").innerHTML = `
    <div class="h"><b>${counts.H}</b><span>HADIR</span></div>
    <div class="s"><b>${counts.S}</b><span>SAKIT</span></div>
    <div class="i"><b>${counts.I}</b><span>IZIN</span></div>
    <div class="a"><b>${counts.A}</b><span>ALFA</span></div>`;

  $("#studentList").innerHTML = STUDENTS.map((nama, i) => `
    <div class="student-row">
      <span class="num">${String(i + 1).padStart(2, "0")}</span>
      <span class="nm">${nama}</span>
      <span class="status-group" data-index="${i}">
        ${Object.keys(STATUS_LABEL).map((k) => `
          <button class="st ${statuses[i] === k ? STATUS_CLASS[k] : ""}"
                  data-status="${k}" aria-label="${STATUS_LABEL[k]} untuk ${nama}"
                  aria-pressed="${statuses[i] === k}">${k}</button>`).join("")}
      </span>
    </div>`).join("");
}

/* -------------------------------- nilai -------------------------------- */

function renderNilai() {
  const rows = state.nilai[state.scoreClass];

  $("#scoreList").innerHTML = rows.map((n, i) => {
    const value = na(n);
    return `
      <div class="nilai-row" data-index="${i}">
        <span class="nm">${n.nama}</span>
        <input type="number" min="0" max="100" value="${n.tugas}" data-field="tugas" aria-label="Nilai tugas ${n.nama}">
        <input type="number" min="0" max="100" value="${n.uts}" data-field="uts" aria-label="Nilai UTS ${n.nama}">
        <input type="number" min="0" max="100" value="${n.uas}" data-field="uas" aria-label="Nilai UAS ${n.nama}">
        <span class="na ${naClass(value)}">${value}</span>
      </div>`;
  }).join("");

  updateSummary();
}

function updateSummary() {
  const values = state.nilai[state.scoreClass].map(na);
  const avg = values.reduce((a, b) => a + b, 0) / values.length;
  $("#avgScore").textContent = String(Math.round(avg)).padStart(2, "0");
  $("#maxScore").textContent = Math.max(...values);
  $("#lowScore").textContent = values.filter((v) => v < KKM).length;
}

/* -------------------------------- jadwal ------------------------------- */

function renderJadwal() {
  const lessons = JADWAL[state.day] || [];
  $("#lessonList").innerHTML = lessons.length
    ? lessons.map((l) => `
        <div class="lesson ${l.now ? "is-now" : ""}">
          <div class="clock">${l.time}<span>${l.end}</span></div>
          <div class="body">
            <h4>${l.subject} · ${l.kelas}</h4>
            <div class="meta"><span>${l.room}</span><span>${l.materi}</span></div>
          </div>
          ${l.now ? `<span class="badge-now">SEKARANG</span>` : ""}
        </div>`)
    : `<p class="empty">TIDAK ADA JADWAL<br>DI HARI INI.<br><br>WAKTU UNTUK MENYIAPKAN<br>PERANGKAT AJAR.</p>`;
}

/* -------------------------------- sheet -------------------------------- */

function openJournal() {
  $("#journalSheet").classList.add("open");
  setTimeout(() => $("#fMateri")?.focus(), 60);
}

function closeSheet() {
  $("#journalSheet").classList.remove("open");
}

/* ------------------------------- wiring -------------------------------- */

function initSwatches() {
  $("#swatches").innerHTML = PALETTE.map((p) => `
    <button class="swatch" style="background:${p.hex}" data-hex="${p.hex}" title="${p.name} ${p.hex}"
      aria-label="Salin warna ${p.name} ${p.hex}"></button>`).join("");

  $("#kitPal").innerHTML = PALETTE.map((p) => `
    <button data-hex="${p.hex}" title="Salin ${p.hex}">
      <i style="background:${p.hex}"></i>
      <b>${p.name.toUpperCase()}</b><span>${p.hex}</span>
    </button>`).join("");
}

function copyHex(hex) {
  const done = () => toast(`WARNA ${hex} DISALIN KE CLIPBOARD`);
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(hex).then(done).catch(() => toast(`WARNA ${hex}`));
  } else {
    toast(`WARNA ${hex}`);
  }
}

function initToggles() {
  $$("[data-toggle]").forEach((row) => {
    const key = row.dataset.toggle;
    const apply = (on) => {
      row.setAttribute("aria-checked", String(on));
      if (key === "scanlines") document.body.classList.toggle("scanlines", on);
      if (key === "motion") document.body.classList.toggle("no-motion", !on);
    };
    apply(row.getAttribute("aria-checked") === "true");
    const flip = () => apply(row.getAttribute("aria-checked") !== "true");
    row.addEventListener("click", flip);
    row.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); flip(); }
    });
  });
}

function initEvents() {
  document.addEventListener("click", (e) => {
    const goto = e.target.closest("[data-goto]");
    if (goto) { showScreen(goto.dataset.goto); return; }

    const soon = e.target.closest("[data-soon]");
    if (soon) { toast(`SEGERA HADIR: ${soon.dataset.soon.toUpperCase()}`); return; }

    const swatch = e.target.closest("[data-hex]");
    if (swatch) { copyHex(swatch.dataset.hex); return; }

    if (e.target.closest("[data-close-sheet]")) { closeSheet(); return; }
    if (e.target.id === "journalSheet") { closeSheet(); return; }

    const chip = e.target.closest(".chip");
    if (chip && !chip.dataset.kelas) return;
  });

  $$(".tab").forEach((tab) => {
    tab.addEventListener("click", () => showScreen(tab.dataset.screen));
  });

  $("#journalChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.journalFilter = chip.dataset.kelas;
    $$(".chip", $("#journalChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderJurnal();
  });

  $("#absClassChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.absClass = chip.dataset.kelas;
    $$(".chip", $("#absClassChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderAbsensi();
  });

  $("#scoreClassChips").addEventListener("click", (e) => {
    const chip = e.target.closest(".chip");
    if (!chip) return;
    state.scoreClass = chip.dataset.kelas;
    $$(".chip", $("#scoreClassChips")).forEach((c) => c.setAttribute("aria-pressed", String(c === chip)));
    renderNilai();
  });

  $("#studentList").addEventListener("click", (e) => {
    const btn = e.target.closest(".st");
    if (!btn) return;
    const group = btn.closest(".status-group");
    const index = Number(group.dataset.index);
    state.absensi[state.absClass][index] = btn.dataset.status;
    renderAbsensi();
    hydrateIcons();
  });

  $("#scoreList").addEventListener("input", (e) => {
    const input = e.target.closest("input");
    if (!input) return;
    const row = input.closest(".nilai-row");
    const index = Number(row.dataset.index);
    const raw = parseInt(input.value, 10);
    const value = Number.isNaN(raw) ? 0 : Math.max(0, Math.min(100, raw));
    state.nilai[state.scoreClass][index][input.dataset.field] = value;

    const naEl = row.querySelector(".na");
    const total = na(state.nilai[state.scoreClass][index]);
    naEl.textContent = total;
    naEl.className = `na ${naClass(total)}`;
    updateSummary();
  });

  $("#dayTabs").addEventListener("click", (e) => {
    const btn = e.target.closest("button");
    if (!btn) return;
    state.day = btn.dataset.day;
    $$("button", $("#dayTabs")).forEach((b) => b.setAttribute("aria-pressed", String(b === btn)));
    renderJadwal();
  });

  ["#openJournal", "#openJournal2"].forEach((sel) => {
    const el = $(sel);
    if (el) el.addEventListener("click", openJournal);
  });

  $("#saveJournal").addEventListener("click", () => {
    const materi = $("#fMateri").value.trim();
    if (!materi) {
      toast("ISI MATERI DULU YA. MATERI MASIH KOSONG.");
      $("#fMateri").focus();
      return;
    }
    const hadir = Math.max(0, parseInt($("#fHadir").value, 10) || 0);
    const total = Math.max(hadir, parseInt($("#fTotal").value, 10) || hadir);
    const now = new Date();

    state.jurnal.unshift({
      id: Date.now(),
      date: now.getDate(),
      day: now.toLocaleDateString("id-ID", { month: "short" }).toUpperCase().slice(0, 3),
      kelas: $("#fKelas").value,
      time: now.toLocaleTimeString("id-ID", { hour: "2-digit", minute: "2-digit" }).replace(".", ":"),
      jam: $("#fJam").value || "—",
      materi,
      note: $("#fCatatan").value.trim() || "Tanpa catatan tambahan.",
      hadir, total
    });

    state.journalFilter = "all";
    $$(".chip", $("#journalChips")).forEach((c) => c.setAttribute("aria-pressed", String(c.dataset.kelas === "all")));
    renderJurnal();
    persist();
    closeSheet();
    $("#fMateri").value = "";
    $("#fCatatan").value = "";
    toast("CATATAN TERSIMPAN. +25 XP DITAMBAHKAN.");
  });

  $("#saveAbsensi").addEventListener("click", () => {
    persist();
    const statuses = state.absensi[state.absClass];
    const count = (k) => statuses.filter((s) => s === k).length;
    toast(`ABSENSI ${state.absClass} TERSIMPAN · H ${count("H")} · S ${count("S")} · I ${count("I")} · A ${count("A")}`);
  });

  $("#saveNilai").addEventListener("click", () => {
    persist();
    toast(`NILAI ${state.scoreClass} TERSIMPAN · NA OTOMATIS DIHITUNG`);
  });

  $("#exportNilai").addEventListener("click", () => {
    toast("EKSPOR CSV SIAP · SIMPAN KE FOLDER UNDUHAN");
  });

  $("#exportJadwal").addEventListener("click", () => {
    toast("JADWAL DIEKSPOR · FORMAT .ICS UNTUK KALENDER HP");
  });

  $$(".nav-btn").forEach((btn) => btn.addEventListener("click", () => showScreen(btn.dataset.goto)));

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeSheet();
  });
}

/* -------------------------------- boot --------------------------------- */

function boot() {
  restore();
  hydrateIcons();
  initSwatches();
  initToggles();
  initEvents();
  renderJurnal();
  renderAbsensi();
  renderNilai();
  renderJadwal();
  hydrateIcons();
  showScreen("home");
}

document.addEventListener("DOMContentLoaded", boot);
