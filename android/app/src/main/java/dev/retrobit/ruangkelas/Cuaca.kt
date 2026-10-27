package dev.retrobit.ruangkelas

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import org.json.JSONObject

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/* ============================================================
   Cuaca — aplikasi cuaca bawaan Ruang Kelas.
   Data: Open-Meteo (gratis, tanpa kunci API). Bila perangkat
   tidak terhubung internet, dipakai cache terakhir; bila cache
   juga kosong, dipakai data contoh agar layar tetap hidup.
   ============================================================ */

data class Kota(val nama: String, val lat: Double, val lon: Double)

data class CuacaNow(
    val temp: Int, val feels: Int, val hum: Int,
    val wind: Int, val code: Int, val rainProb: Int
)

data class CuacaHour(val label: String, val temp: Int, val code: Int, val rainProb: Int)

data class CuacaDay(
    val nama: String, val code: Int,
    val min: Int, val max: Int,
    val sunrise: String, val sunset: String
)

data class CuacaData(
    val kota: String,
    val now: CuacaNow,
    val hours: List<CuacaHour>,
    val days: List<CuacaDay>,
    val at: String,
    val contoh: Boolean = false
)

object CuacaRepo {

    val kota = listOf(
        Kota("JEMBER", -8.1728, 113.6995),
        Kota("BONDOWOSO", -7.9136, 113.8208),
        Kota("BANYUWANGI", -8.2192, 114.3692),
        Kota("PROBOLINGGO", -7.9983, 113.7052),
        Kota("SURABAYA", -7.2575, 112.7521),
        Kota("SEMARANG", -6.9667, 110.4167),
        Kota("YOGYAKARTA", -7.7956, 110.3695),
        Kota("JAKARTA", -6.2088, 106.8456),
        Kota("DENPASAR", -8.6500, 115.2167)
    )

    const val SUMBER = "OPEN-METEO"

    /** Ambil prakiraan terbaru dari Open-Meteo. Melempar pengecualian bila gagal. */
    fun ambil(k: Kota): CuacaData {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${k.lat}&longitude=${k.lon}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m," +
            "weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
            "&timezone=auto&forecast_days=6"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout = 6000
        try {
            val body = conn.inputStream.bufferedReader().readText()
            return parse(body, k.nama)
        } finally {
            conn.disconnect()
        }
    }

    fun parse(body: String, namaKota: String): CuacaData {
        val root = JSONObject(body)

        val cur = root.getJSONObject("current")
        val now = CuacaNow(
            temp = cur.getDouble("temperature_2m").toInt(),
            feels = cur.getDouble("apparent_temperature").toInt(),
            hum = cur.getInt("relative_humidity_2m"),
            wind = cur.getDouble("wind_speed_10m").toInt(),
            code = cur.getInt("weather_code"),
            rainProb = 0
        )

        val hourly = root.getJSONObject("hourly")
        val hTime = hourly.getJSONArray("time")
        val hTemp = hourly.getJSONArray("temperature_2m")
        val hCode = hourly.getJSONArray("weather_code")
        val hProb = hourly.getJSONArray("precipitation_probability")
        val mulai = LocalDateTime.now().apply {
            LocalDateTime.parse(hTime.getString(0)) // validasi format sekaligus
        }
        val jamIni = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        var idx = 0
        for (i in 0 until hTime.length()) {
            if (LocalDateTime.parse(hTime.getString(i)) >= jamIni) { idx = i; break }
        }
        val hours = ArrayList<CuacaHour>()
        for (i in idx until minOf(idx + 8, hTime.length())) {
            hours.add(
                CuacaHour(
                    label = hTime.getString(i).substring(11, 16),
                    temp = hTemp.getDouble(i).toInt(),
                    code = hCode.getInt(i),
                    rainProb = hProb.optInt(i, 0)
                )
            )
        }

        val daily = root.getJSONObject("daily")
        val dTime = daily.getJSONArray("time")
        val dCode = daily.getJSONArray("weather_code")
        val dMin = daily.getJSONArray("temperature_2m_min")
        val dMax = daily.getJSONArray("temperature_2m_max")
        val dUp = daily.getJSONArray("sunrise")
        val dDown = daily.getJSONArray("sunset")
        val namaHari = arrayOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")
        val days = ArrayList<CuacaDay>()
        for (i in 0 until minOf(5, dTime.length())) {
            val d = LocalDate.parse(dTime.getString(i))
            days.add(
                CuacaDay(
                    nama = namaHari[d.dayOfWeek.value - 1],
                    code = dCode.getInt(i),
                    min = dMin.getDouble(i).toInt(),
                    max = dMax.getDouble(i).toInt(),
                    sunrise = dUp.getString(i).substring(11, 16),
                    sunset = dDown.getString(i).substring(11, 16)
                )
            )
        }

        return CuacaData(
            kota = namaKota,
            now = now,
            hours = hours,
            days = days,
            at = LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
            contoh = false
        )
    }

    /* ---------- cache offline ---------- */

    private fun cacheFile(ctx: android.content.Context) =
        File(ctx.filesDir, "cuaca-cache.json")

    fun simpanCache(ctx: android.content.Context, d: CuacaData) {
        try {
            val root = JSONObject()
            root.put("kota", d.kota)
            root.put("at", d.at)
            root.put("now", JSONObject().apply {
                put("temp", d.now.temp); put("feels", d.now.feels)
                put("hum", d.now.hum); put("wind", d.now.wind); put("code", d.now.code)
            })
            val hArr = org.json.JSONArray()
            d.hours.forEach { h ->
                hArr.put(JSONObject().apply {
                    put("label", h.label); put("temp", h.temp)
                    put("code", h.code); put("prob", h.rainProb)
                })
            }
            root.put("hours", hArr)
            val dArr = org.json.JSONArray()
            d.days.forEach { x ->
                dArr.put(JSONObject().apply {
                    put("nama", x.nama); put("code", x.code); put("min", x.min)
                    put("max", x.max); put("up", x.sunrise); put("down", x.sunset)
                })
            }
            root.put("days", dArr)
            cacheFile(ctx).writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    fun muatCache(ctx: android.content.Context): CuacaData? {
        return try {
            val root = JSONObject(cacheFile(ctx).readText())
            val nowJ = root.getJSONObject("now")
            val hours = ArrayList<CuacaHour>()
            val hArr = root.getJSONArray("hours")
            for (i in 0 until hArr.length()) {
                val o = hArr.getJSONObject(i)
                hours.add(
                    CuacaHour(o.getString("label"), o.getInt("temp"),
                        o.getInt("code"), o.optInt("prob", 0))
                )
            }
            val days = ArrayList<CuacaDay>()
            val dArr = root.getJSONArray("days")
            for (i in 0 until dArr.length()) {
                val o = dArr.getJSONObject(i)
                days.add(
                    CuacaDay(o.getString("nama"), o.getInt("code"), o.getInt("min"),
                        o.getInt("max"), o.getString("up"), o.getString("down"))
                )
            }
            val now = CuacaNow(
                temp = nowJ.getInt("temp"), feels = nowJ.getInt("feels"),
                hum = nowJ.getInt("hum"), wind = nowJ.getInt("wind"),
                code = nowJ.getInt("code"), rainProb = 0
            )
            CuacaData(root.getString("kota"), now, hours, days, root.getString("at"), false)
        } catch (_: Exception) {
            null
        }
    }

    /** Data contoh: tampil saat offline dan cache masih kosong. */
    fun contoh(namaKota: String): CuacaData = CuacaData(
        kota = namaKota,
        now = CuacaNow(temp = 27, feels = 29, hum = 78, wind = 12, code = 2, rainProb = 15),
        hours = listOf(
            CuacaHour("06:00", 26, 0, 5), CuacaHour("07:00", 27, 2, 5),
            CuacaHour("08:00", 28, 2, 10), CuacaHour("09:00", 29, 3, 10),
            CuacaHour("10:00", 30, 3, 15), CuacaHour("11:00", 29, 61, 55),
            CuacaHour("12:00", 28, 61, 60), CuacaHour("13:00", 27, 95, 70)
        ),
        days = listOf(
            CuacaDay("SENIN", 0, 24, 31, "05:12", "17:45"),
            CuacaDay("SELASA", 2, 24, 32, "05:12", "17:45"),
            CuacaDay("RABU", 61, 23, 30, "05:13", "17:45"),
            CuacaDay("KAMIS", 95, 22, 29, "05:13", "17:44"),
            CuacaDay("JUMAT", 3, 23, 30, "05:14", "17:44")
        ),
        at = "-",
        contoh = true
    )
}

/* ---------- pemetaan kode WMO ---------- */

fun namaKondisi(code: Int): String = when (code) {
    0 -> "CERAH"
    1 -> "CERAH SEDANG"
    2 -> "CERAH BERAWAN"
    3 -> "BERAWAN"
    45, 48 -> "BERKABUT"
    in 51..57 -> "GERIMIS"
    61, 63 -> "HUJAN"
    65 -> "HUJAN LEBAT"
    66, 67 -> "HUJAN BEKU"
    in 71..77 -> "SALJU"
    80, 81 -> "HUJAN LOKAL"
    82 -> "HUJAN SANGAT LEBAT"
    85, 86 -> "BADAI SALJU"
    95 -> "BADAI"
    96, 99 -> "BADAI + ESA"
    else -> "-"
}

/* ---------- sprite pixel ---------- */

private val SPR_PAL = mapOf(
    'o' to Amber,
    'O' to Color(0xFFFFD678),
    'c' to Color(0xFF9A98B2),
    'C' to Color(0xFFBEBCD0),
    'd' to Color(0xFF787496),
    'D' to Color(0xFF9692B4),
    's' to Sky
)

private val SPR_SUN = listOf(
    "....o..o....",
    ".....oo.....",
    "..o.OOOO.o..",
    "...OooooO...",
    "..OooOOooO..",
    "ooOoOOOOoOoo",
    "..OooOOooO..",
    "...OooooO...",
    "..o.OOOO.o..",
    ".....oo.....",
    "....o..o...."
)

private val SPR_CLOUD = listOf(
    "....CCCC......",
    "..CCccccCC....",
    ".CcccccccccC..",
    "CcccccccccccC.",
    ".CCCCCCCCCCCC."
)

private val SPR_RAIN = SPR_CLOUD + listOf(
    "....s..s..s...",
    "..s..s..s.....",
    "....s..s..s..."
)

private val SPR_STORM = listOf(
    "....DDDD......",
    "..DDddddDD....",
    ".DdddddddddD..",
    "DdddddddddddD.",
    ".DDDDDDDDDDDD.",
    "......yy......",
    ".....yy.......",
    "....yy........"
)

private val SPR_PARTLY = listOf(
    "......o..o........",
    ".......oo.........",
    "..o...OOOO...o....",
    "......OooO........",
    "oo...OoOOoO...oo..",
    "....CCCCCCCCC.....",
    "..CCcccccccccCC...",
    ".CccccccccccccC...",
    "CccccccccccccccC..",
    ".CCCCCCCCCCCCCCC.."
)

fun spriteKode(code: Int): List<String> = when (code) {
    0, 1 -> SPR_SUN
    2 -> SPR_PARTLY
    3, 45, 48, in 71..77, 85, 86 -> SPR_CLOUD
    in 51..67, in 80..82 -> SPR_RAIN
    95, 96, 99 -> SPR_STORM
    else -> SPR_CLOUD
}

@Composable
fun PixelSprite(rows: List<String>, unit: Dp, modifier: Modifier = Modifier) {
    val cols = rows.maxOf { it.length }
    Canvas(modifier.size(unit * cols, unit * rows.size)) {
        val u = unit.toPx()
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                val c = SPR_PAL[ch] ?: return@forEachIndexed
                drawRect(color = c, topLeft = Offset(x * u, y * u), size = Size(u, u))
            }
        }
    }
}

/* ---------- UI ---------- */

@Composable
fun CuacaScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("cuaca", android.content.Context.MODE_PRIVATE) }

    var cityIdx by remember { mutableStateOf(prefs.getInt("kota", 0)) }
    var data by remember { mutableStateOf<CuacaData?>(null) }
    var online by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var cityOpen by remember { mutableStateOf(false) }

    LaunchedEffect(cityIdx, refreshKey) {
        loading = true
        val hasil = withContext(Dispatchers.IO) {
            try {
                CuacaRepo.ambil(CuacaRepo.kota[cityIdx])
                    .also { CuacaRepo.simpanCache(ctx, it) }
            } catch (_: Exception) {
                null
            }
        }
        if (hasil != null) {
            data = hasil
            online = true
        } else {
            data = CuacaRepo.muatCache(ctx)
            online = false
        }
        loading = false
    }

    val d = data

    Box(Modifier.fillMaxSize().background(Night)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {

            /* ----- baris atas ----- */
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton("< RUANGAN", onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Text("CUACA", style = MaterialTheme.typography.displayLarge, color = Amber)
                Spacer(Modifier.weight(1f))
                // chip status: ketuk untuk memperbarui
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { refreshKey++ }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(
                                when {
                                    loading -> Muted2
                                    online -> Mint
                                    else -> Coral
                                }
                            )
                            .border(2.dp, Ink)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            loading -> "MEMUAT"
                            online -> "ONLINE"
                            else -> "OFFLINE"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
                Spacer(Modifier.width(12.dp))
                GhostButton("KOTA: ${(d?.kota ?: CuacaRepo.kota[cityIdx].nama)} \u25be", onClick = {
                    cityOpen = true
                })
            }

            Spacer(Modifier.height(12.dp))

            /* ----- isi ----- */
            Row(Modifier.weight(1f)) {

                /* panel kiri: saat ini */
                PixelPanel(
                    Modifier.width(330.dp).fillMaxHeight(),
                    padding = 16.dp
                ) {
                    if (d == null) {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                if (loading) "MEMUAT DATA CUACA\u2026" else "TIDAK ADA DATA",
                                style = MaterialTheme.typography.labelLarge,
                                color = Muted
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Text("SAAT INI", style = MaterialTheme.typography.bodySmall, color = Muted2)
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PixelSprite(spriteKode(d.now.code), unit = 8.dp)
                                Spacer(Modifier.width(18.dp))
                                Column {
                                    Text(
                                        "${d.now.temp}\u00b0C",
                                        style = TextStyle(
                                            fontFamily = UiFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 54.sp,
                                            lineHeight = 58.sp
                                        ),
                                        color = Cream
                                    )
                                    Text(
                                        namaKondisi(d.now.code),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Amber
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            listOf(
                                "TERASA" to "${d.now.feels}\u00b0",
                                "LEMBAP" to "${d.now.hum}%",
                                "ANGIN" to "${d.now.wind} km/j",
                                "PELUANG HUJAN" to "${peluangMaks(d.hours)}%"
                            ).forEach { (k, v) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                    Text(k, style = MaterialTheme.typography.bodySmall, color = Muted2)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        v,
                                        style = TextStyle(
                                            fontFamily = UiFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp
                                        ),
                                        color = Cream2
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFF101020))
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Row {
                                Text(
                                    "TERBIT  ${d.days.firstOrNull()?.sunrise ?: "-"}",
                                    style = MaterialTheme.typography.labelLarge, color = Amber
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "TERBENAM  ${d.days.firstOrNull()?.sunset ?: "-"}",
                                    style = MaterialTheme.typography.labelLarge, color = Plum
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (d.contoh) "DATA CONTOH \u00b7 BELUM PERNAH ONLINE"
                                else "SUMBER: ${CuacaRepo.SUMBER} \u00b7 DIPERBARUI ${d.at}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted2
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                /* kolom kanan */
                Column(Modifier.weight(1f)) {

                    /* panel per jam */
                    PixelPanel(Modifier.fillMaxWidth().weight(1f), padding = 12.dp) {
                        Column(Modifier.fillMaxSize()) {
                            Text("8 JAM KE DEPAN", style = MaterialTheme.typography.bodySmall, color = Muted2)
                            Spacer(Modifier.height(8.dp))
                            if (d != null) {
                                Row(Modifier.weight(1f)) {
                                    d.hours.take(8).forEachIndexed { i, h ->
                                        if (i > 0) Box(
                                            Modifier.width(1.dp).fillMaxHeight()
                                                .background(Color(0xFF101020))
                                        )
                                        Column(
                                            Modifier.weight(1f).fillMaxHeight(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                h.label, style = MaterialTheme.typography.bodySmall,
                                                color = Muted
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            PixelSprite(spriteKode(h.code), unit = 4.dp)
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "${h.temp}\u00b0",
                                                style = TextStyle(
                                                    fontFamily = UiFont,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 17.sp
                                                ),
                                                color = Cream2
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(1.dp)
                                        .background(Color(0xFF101020))
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(rangkumJam(d.hours), style = MaterialTheme.typography.bodySmall, color = Teal)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    /* panel 5 hari */
                    PixelPanel(Modifier.fillMaxWidth().weight(1.15f), padding = 12.dp) {
                        Column(Modifier.fillMaxSize()) {
                            Text("PRAKIRAAN 5 HARI", style = MaterialTheme.typography.bodySmall, color = Muted2)
                            Spacer(Modifier.height(6.dp))
                            if (d != null) {
                                d.days.forEachIndexed { i, day ->
                                    Row(
                                        Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            day.nama,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Cream2,
                                            modifier = Modifier.width(86.dp)
                                        )
                                        PixelSprite(spriteKode(day.code), unit = 3.dp)
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            "${day.min}\u00b0",
                                            style = TextStyle(fontFamily = UiFont, fontSize = 16.sp),
                                            color = Muted,
                                            modifier = Modifier.width(44.dp)
                                        )
                                        BarSuhu(
                                            lo = day.min,
                                            hi = day.max,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "${day.max}\u00b0",
                                            style = TextStyle(
                                                fontFamily = UiFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            ),
                                            color = Cream2
                                        )
                                    }
                                    if (i < d.days.lastIndex) {
                                        Box(
                                            Modifier.fillMaxWidth().height(1.dp)
                                                .background(Color(0xFF101020))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /* ----- pemilih kota ----- */
        if (cityOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x990E0D1A))
                    .clickable { cityOpen = false },
                contentAlignment = Alignment.TopEnd
            ) {
                PixelPanel(
                    Modifier
                        .padding(top = 16.dp, end = 16.dp)
                        .width(220.dp)
                        .clickable { },
                    padding = 8.dp
                ) {
                    Column {
                        Text(
                            "PILIH KOTA",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted2,
                            modifier = Modifier.padding(6.dp)
                        )
                        CuacaRepo.kota.forEachIndexed { i, k ->
                            val dipilih = i == cityIdx
                            Text(
                                k.nama,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (dipilih) Amber else Cream2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        cityIdx = i
                                        prefs.edit().putInt("kota", i).apply()
                                        cityOpen = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Peluang hujan tertinggi pada 8 jam ke depan. */
private fun peluangMaks(hours: List<CuacaHour>): Int = hours.take(8).maxOfOrNull { it.rainProb } ?: 0

/** Bar rentang suhu min-maks untuk baris 5 hari. */
@Composable
private fun BarSuhu(lo: Int, hi: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .height(16.dp)
            .background(Navy)
            .border(2.dp, Ink)
    ) {
        val f0 = ((lo - 22f) / 10f).coerceIn(0f, 1f)
        val f1 = ((hi - 22f) / 10f).coerceIn(0f, 1f)
        Box(
            Modifier
                .offset(x = maxWidth * f0)
                .width(maxWidth * (f1 - f0))
                .fillMaxHeight()
                .background(AmberDeep)
        )
    }
}

/** Satu baris ringkasan untuk panel per jam. */
private fun rangkumJam(hours: List<CuacaHour>): String {
    val ambil = hours.take(8)
    if (ambil.isEmpty()) return ""
    val hujan = ambil.filter { it.rainProb >= 50 }
    val teringgi = ambil.maxByOrNull { it.temp }!!
    val p1 = if (hujan.isEmpty()) "TANPA HUJAN SIGNIFIKAN"
    else "PELUANG HUJAN SEKITAR ${hujan.first().label}"
    return "$p1 \u00b7 TERINGGI ${teringgi.temp}\u00b0 PUKUL ${teringgi.label}"
}
