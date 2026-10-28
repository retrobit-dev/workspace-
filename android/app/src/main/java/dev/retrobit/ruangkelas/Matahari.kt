/**
 * Posisi matahari untuk bayangan dinamis (waktu & lokasi perangkat).
 *
 * Algoritme ringkas macam NOAA: deklinasi & anomali rata-rata dari jumlah hari
 * sejak J2000, waktu sideris untuk sudut jam — akurasi ~0.5°, cukup untuk arah
 * dan panjang bayangan. Tidak ada ketergantungan library luar.
 */
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object Matahari {

    /** Pair(azimut 0..360° dari utara searah jarum jam, elevasi °; negatif = di bawah horizon). */
    fun posisi(latDeg: Double, lonDeg: Double, tMs: Long): Pair<Double, Double> {
        val r = PI / 180.0
        val d = tMs / 86400000.0 - 10957.5          // hari sejak J2000 (2000-01-01T12:00Z)
        val g = norm360(357.529 + 0.98560028 * d)   // anomali rata-rata
        val q = norm360(280.459 + 0.98564736 * d)   // bujur rata-rata
        val L = norm360(q + 1.915 * sin(r * g) + 0.020 * sin(r * 2 * g))
        val e = 23.439 - 0.00000036 * d             // kemiringan sumbu
        val dec = asin(sin(r * e) * sin(r * L)) / r // deklinasi
        val ra = atan2(cos(r * e) * sin(r * L), cos(r * L)) / r / 15.0
        val lmst = norm24(18.697374558 + 24.06570982441908 * d + lonDeg / 15.0)
        val H = (lmst - ra) * 15.0 * r              // sudut jam
        val phi = r * latDeg
        val sinElev = sin(phi) * sin(r * dec) + cos(phi) * cos(r * dec) * cos(H)
        val elev = asin(sinElev) / r
        val az = atan2(sin(H), cos(H) * sin(phi) - tan(r * dec) * cos(phi)) / r + 180.0
        return Pair(norm360(az), elev)
    }

    private fun norm360(x: Double) = ((x % 360.0) + 360.0) % 360.0
    private fun norm24(x: Double) = ((x % 24.0) + 24.0) % 24.0
}
