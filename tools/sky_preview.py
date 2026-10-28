"""Generator SVG langit dinamis untuk preview web (cermin SkyAnim di Launcher.kt).

UNDERLAY: SVG ini dirender SEBELUM <img id=scene>; scene PNG punya bolongan
kaca transparan (SKY_HOLES) sehingga langit terlihat melaluinya dan semua art
(rel, bingkai, tirai, palang) otomatis berada DI DEPAN animasi.

Geometri & palet sama dengan APK: dua panel langit jendela kanan, busur
matahari/bulan, gradien langit per menit, awan/hujan/salju/kabut/kilat dari
kode cuaca WMO (Open-Meteo, cache localStorage). Semua gerak memakai
animateTransform translate / animate opacity — TANPA animateTransform rotate
(checker melarang rotasi SMIL di scene).
"""
from __future__ import annotations

import math

# (kiri-atas, kanan-atas, kanan-bawah, kiri-bawah) — piksel gambar 1280x800
SKY_PANELS = [
    (1010, 119, 1082, 148, 1082, 337, 1010, 299),  # kaca kiri (tertutup)
    (1100, 146, 1192, 193, 1190, 402, 1100, 355),  # bukaan kanan (terbuka)
]
SKY_ARC = (1108, 336, 1185, 372)  # busur orb (panel kanan); puncak = base-146
SKY_STOPS = [  # (menit, warna-atas, warna-bawah) — gradien sesuai waktu nyata
    (0, "#0A0F2E", "#18224E"), (300, "#2E3A6E", "#F2955F"), (420, "#7FB2E8", "#CFE6F7"),
    (720, "#4D8FE0", "#A8CDF0"), (990, "#6D9BD8", "#F7C873"), (1080, "#3A3F7A", "#E86A5A"),
    (1170, "#232D5B", "#3A4C74"), (1440, "#1B2448", "#303F66"),
]

JEMBER = dict(lat=-8.1728, lon=113.6995)  # kota default CuacaRepo (idx 0)


def _h(i: int, salt: float) -> float:
    """acak deterministik 0..1 — pola sama dengan hash01() di Launcher.kt."""
    v = math.sin(i * 127.1 + salt * 311.7) * 43758.5453
    return abs(v - math.floor(v))


def clip_path() -> str:
    parts = []
    for q in SKY_PANELS:
        parts.append(f"M{q[0]},{q[1]} L{q[2]},{q[3]} L{q[4]},{q[5]} L{q[6]},{q[7]} Z")
    return " ".join(parts)


def group() -> str:
    clouds: list[str] = []
    for i in range(5):  # awan: lahir tersembunyi di kiri, loop ke kanan, wrap tersembunyi
        cy = 130 + _h(i, 13) * 220
        sc = 0.75 + _h(i, 31) * 0.8
        dur = round(198 / (3.2 + _h(i, 21) * 2.6), 2)
        clouds.append(f'''<g transform="translate({-40},0)">
      <g transform="scale({sc:.2f})">
        <circle cx="-14" cy="4" r="16" class="sky-cloud-fill"/>
        <circle cx="0" cy="0" r="21" class="sky-cloud-fill"/>
        <circle cx="15" cy="5" r="15" class="sky-cloud-fill"/>
        <rect x="-26" y="2" width="52" height="12" rx="6" class="sky-cloud-fill"/>
      </g>
      <animateTransform attributeName="transform" type="translate"
        from="{-40},{cy:.0f}" to="158,{cy:.0f}" dur="{dur}s" begin="{-_h(i, 5) * dur:.2f}s" repeatCount="indefinite"/>
    </g>''')

    rains: list[str] = []
    for i in range(150):  # 0-45 gerimis, 46-95 hujan, 96-149 deras
        rx = 1100 + _h(i, 41) * 98
        dur = 3.6 if i < 46 else (2.5 if i < 96 else 2.0)
        rains.append(f'''<line x1="{rx:.1f}" y1="450" x2="{rx - 2.2:.1f}" y2="460">
      <animateTransform attributeName="transform" type="translate"
        from="0,-490" to="0,-90" dur="{dur}s" begin="{-_h(i, 47) * dur:.2f}s" repeatCount="indefinite"/>
    </line>''')

    snows: list[str] = []
    for i in range(70):
        dur = round(14 + _h(i, 63) * 8, 1)
        snows.append(f'''<circle cx="{1100 + _h(i, 69) * 98:.1f}" cy="450" r="{0.9 + _h(i, 67):.2f}">
      <animateTransform attributeName="transform" type="translate"
        from="0,-490" to="0,-80" dur="{dur}s" begin="{-_h(i, 65) * dur:.2f}s" repeatCount="indefinite"/>
    </circle>''')

    stars: list[str] = []
    for i in range(16):
        dur = round(1.6 + _h(i, 3) * 1.8, 2)
        stars.append(f'''<circle cx="{1010 + _h(i, 0) * 190:.1f}" cy="{130 + _h(i, 7) * 270:.1f}" r="{0.7 + _h(i, 11):.2f}">
      <animate attributeName="opacity" values="0.2;0.95;0.2" dur="{dur}s" begin="{-_h(i, 5) * dur:.2f}s" repeatCount="indefinite"/>
    </circle>''')

    fogs: list[str] = []
    for b in range(3):
        fogs.append(f'''<rect x="1066" y="{160 + b * 80}" width="108" height="18" opacity="0.14">
      <animateTransform attributeName="transform" type="translate"
        values="-30 0;30 0;-30 0" dur="{14 + b * 4}s" repeatCount="indefinite"/>
    </rect>''')

    rays = "\n        ".join(
        f'<line x1="0" y1="-16" x2="0" y2="-23" transform="rotate({k * 45})"/>'
        for k in range(8)
    )
    return f'''
    <defs>
      <linearGradient id="skygrad" gradientUnits="userSpaceOnUse" x1="1150" y1="115" x2="1150" y2="410">
        <stop id="sg0" offset="0" stop-color="#4D8FE0"/>
        <stop id="sg1" offset="1" stop-color="#A8CDF0"/>
      </linearGradient>
    </defs>
    <g id="sky-anim" data-weather="1">
      <rect x="968" y="110" width="252" height="305" fill="url(#skygrad)"/>
      <g id="sky-stars" style="display:none">{''.join(stars)}</g>
      <g id="sky-orb"><g id="sky-orb-pos" transform="translate(1146,208)">
        <g id="sky-sun">
          <circle r="26" fill="#FFD9A0" opacity="0.28">
            <animate attributeName="opacity" values="0.16;0.34;0.16" dur="2.8s" repeatCount="indefinite"/>
          </circle>
          <g id="sky-rays" stroke="#FFE9A8" stroke-width="1.7" stroke-linecap="round" opacity="0.85">
        {rays}
            <animate attributeName="opacity" values="0.45;0.9;0.45" dur="2.6s" repeatCount="indefinite"/>
          </g>
          <circle r="15" fill="#FFE9A8"/><circle r="10.5" fill="#FFF3C4"/>
        </g>
        <g id="sky-moon" style="display:none">
          <circle r="13" fill="#EDE9DA" opacity="0.95"/>
          <circle cx="-4" cy="2" r="3.2" fill="#C9C4B4" opacity="0.8"/>
          <circle cx="4.5" cy="-3.5" r="2.1" fill="#C9C4B4" opacity="0.8"/>
          <circle cx="1" cy="5" r="1.6" fill="#C9C4B4" opacity="0.8"/>
        </g>
      </g></g>
      <g id="sky-clouds" class="sky-cloud-host">{''.join(clouds)}</g>
      <rect id="sky-dim" x="968" y="110" width="252" height="305" fill="#3A4150" opacity="0"/>
      <g id="sky-rain" style="display:none">{''.join(rains)}</g>
      <g id="sky-snow" style="display:none">{''.join(snows)}</g>
      <g id="sky-fog" style="display:none">{''.join(fogs)}</g>
      <rect id="sky-flash" x="968" y="110" width="252" height="305" fill="#F4F6FF" opacity="0">
        <animate attributeName="opacity" values="0;0;0.85;0;0.5;0;0;0"
          keyTimes="0;0.42;0.433;0.44;0.452;0.47;0.9;1" dur="7s" repeatCount="indefinite"/>
      </rect>
      <g transform="rotate(-24 1150 275)">
        <rect x="1112" y="140" width="12" height="270" fill="#FFFFFF" opacity="0.10"/>
        <rect x="1150" y="140" width="5" height="270" fill="#FFFFFF" opacity="0.08"/>
      </g>
    </g>
'''


# JS pengendali langit: gradien + posisi orb per detik; cuaca dari Open-Meteo
JS = r"""
const SKY_STOPS = __SKY_STOPS__;
const SKY = {x0:1108, x1:1185, y0:336, y1:372, amp:146};
let SKY_WEATHER = {langit:1, hujan:0, salju:false, kabut:false, petir:false, code:1, angin:8, awan:80};

function skyStored(){try{return localStorage.getItem("rk-sky-code");}catch(e){return null;}}
function skyStore(c){try{localStorage.setItem("rk-sky-code",String(c));}catch(e){}}
function windStored(){try{return localStorage.getItem("rk-sky-wind");}catch(e){return null;}}
function windStore(v){try{localStorage.setItem("rk-sky-wind",String(v));}catch(e){}}
function awanStored(){try{return localStorage.getItem("rk-sky-awan");}catch(e){return null;}}
function awanStore(v){try{localStorage.setItem("rk-sky-awan",String(v));}catch(e){}}
function skyHex2rgb(h){return [parseInt(h.slice(1,3),16),parseInt(h.slice(3,5),16),parseInt(h.slice(5,7),16)];}
function skyMix(a,b,t){
  const A=skyHex2rgb(a),B=skyHex2rgb(b);
  return "rgb("+Math.round(A[0]+(B[0]-A[0])*t)+","+Math.round(A[1]+(B[1]-A[1])*t)+","+Math.round(A[2]+(B[2]-A[2])*t)+")";
}
function skyColors(m){
  m=((m%1440)+1440)%1440;
  for(let i=0;i<SKY_STOPS.length-1;i++){
    const a=SKY_STOPS[i],b=SKY_STOPS[i+1];
    if(m>=a[0]&&m<=b[0]){
      const t=(m-a[0])/Math.max(1,b[0]-a[0]);
      return [skyMix(a[1],b[1],t),skyMix(a[2],b[2],t)];
    }
  }
  return [SKY_STOPS[0][1],SKY_STOPS[0][2]];
}
function skyVisuals(code){
  if(code===0) return {langit:0,hujan:0};
  if(code===1) return {langit:1,hujan:0};
  if(code===2) return {langit:2,hujan:0};
  if(code===3) return {langit:3,hujan:0};
  if(code===45||code===48) return {langit:3,hujan:0,kabut:true};
  if(code>=51&&code<=57) return {langit:2,hujan:1};
  if(code===65||code===82) return {langit:2,hujan:3};
  if((code>=61&&code<=67)||(code>=80&&code<=82)) return {langit:2,hujan:2};
  if((code>=71&&code<=77)||code===85||code===86) return {langit:2,hujan:0,salju:true};
  if(code===95||code===96||code===99) return {langit:3,hujan:3,petir:true};
  return {langit:2,hujan:0};
}
function setDisp(id,on){const el=document.getElementById(id);if(el)el.style.display=on?"":"none";}
function applySkyVis(isDay){
  const w=SKY_WEATHER;
  const clouds=document.getElementById("sky-clouds");
  if(clouds){
    const n=w.langit===0?0:(w.langit===1?1:(w.langit===2?3:5));
    [...clouds.children].forEach((c,i)=>c.style.display=i<n?"":"none");
    const cc=w.langit<=1?["#FFFFFF",0.92]:(w.langit===2?["#E8ECF2",0.9]:["#B9BFCB",0.85]);
    clouds.querySelectorAll(".sky-cloud-fill").forEach((el)=>{
      el.setAttribute("fill",cc[0]);el.setAttribute("fill-opacity",cc[1]);
    });
  }
  document.getElementById("sky-dim").setAttribute("opacity",
    ((w.langit===2?0.18:(w.langit===3?0.34:0))+(w.hujan>0?0.08:0)).toFixed(2));
  const rain=document.getElementById("sky-rain");
  setDisp("sky-rain",w.hujan>0);
  if(w.hujan>0){
    const max=w.hujan===1?46:(w.hujan===2?96:150);
    [...rain.children].forEach((l,i)=>l.style.display=i<max?"":"none");
  }
  setDisp("sky-snow",!!w.salju);
  setDisp("sky-fog",!!w.kabut);
  setDisp("sky-flash",!!w.petir);
  setDisp("sky-roomray",isDay&&w.langit===0);
  const rays=document.getElementById("sky-rays");
  if(rays) rays.setAttribute("opacity",w.langit===0?(w.code===0?0.85:0.6):0.25);
  setDisp("sky-stars",!isDay&&w.langit<=1);
}
let SKY_LAST_DAY=null;
function skyTick(){
  const now=new Date();
  const m=now.getHours()*60+now.getMinutes();
  const isDay=now.getHours()>=6&&now.getHours()<=17;
  const [ct,cb]=skyColors(m);
  document.getElementById("sg0").setAttribute("stop-color",ct);
  document.getElementById("sg1").setAttribute("stop-color",cb);
  let f;
  if(isDay) f=Math.min(1,Math.max(0,(m-360)/660));
  else f=(((m-1080+1440)%1440))/720;
  const ox=SKY.x0+(SKY.x1-SKY.x0)*f;
  const oy=(SKY.y0+(SKY.y1-SKY.y0)*f)-Math.sin(Math.PI*f)*SKY.amp;
  document.getElementById("sky-orb-pos").setAttribute("transform","translate("+ox.toFixed(1)+","+oy.toFixed(1)+")");
  setDisp("sky-sun",isDay);
  // matahari selalu tampak saat siang; mendung tebal hanya meredupkan (α0.3)
  const sunEl=document.getElementById("sky-sun");
  if(sunEl) sunEl.style.opacity=isDay?(SKY_WEATHER.langit<=1?1:(SKY_WEATHER.langit===2?0.8:0.3)):"";
  // bulan selalu tampak di malam hari; mendung tebal hanya membuatnya samar
  setDisp("sky-moon",!isDay);
  const moonEl=document.getElementById("sky-moon");
  if(moonEl) moonEl.style.opacity=!isDay?(SKY_WEATHER.langit<=1?1:(SKY_WEATHER.langit===2?0.8:0.3)):"";
  if(isDay!==SKY_LAST_DAY){applySkyVis(isDay);SKY_LAST_DAY=isDay;}
}
function applySkyCode(code){
  if(typeof code!=="number")return;
  const anginLama=SKY_WEATHER.angin, awanLama=SKY_WEATHER.awan;
  SKY_WEATHER=Object.assign({code:code,angin:anginLama,awan:awanLama},skyVisuals(code));
  document.getElementById("sky-anim").dataset.weather=String(code);
  applySkyVis(new Date().getHours()>=6&&new Date().getHours()<=17);
}
async function skyLoadWeather(){
  try{
    const opt={signal:(typeof AbortSignal!=="undefined"&&AbortSignal.timeout)?AbortSignal.timeout(6000):undefined};
    const r=await fetch("__SKY_URL__",opt);
    const j=await r.json();
    const c=j&&j.current&&j.current.weather_code;
    const wn=j&&j.current&&j.current.wind_speed_10m;
    if(typeof wn==="number"&&wn>=0){SKY_WEATHER.angin=wn;windStore(wn);}
    const cw=j&&j.current&&j.current.cloud_cover;
    if(typeof cw==="number"&&cw>=0){SKY_WEATHER.awan=cw;awanStore(cw);}
    if(typeof c==="number"){skyStore(c);applySkyCode(c);}
  }catch(e){/* offline: pertahankan kode tersimpan */}
}
(function(){
  const w0=parseFloat(windStored());if(!isNaN(w0)&&w0>=0)SKY_WEATHER.angin=w0;
  const a0=parseFloat(awanStored());if(!isNaN(a0)&&a0>=0)SKY_WEATHER.awan=a0;
  applySkyCode(parseInt(skyStored()||"1",10));
  skyTick();
  setInterval(skyTick,1000);
  skyLoadWeather();
  setInterval(skyLoadWeather,15*60*1000);
})();
"""
