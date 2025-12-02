package com.example.aircare

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class AirQualityRepository {

    private val apiKey = "3edfd82f93e2e50f7497a083e88ece56"

    // Daftar lokasi yang ingin kita pantau (Hanya Lat & Lon, AQI akan diambil dari API)
    private val monitoringLocations = listOf(
        // === GADING SERPONG & SEKITARNYA ===
        LatLng(-6.2413, 106.6263),  // Summarecon Mall
        LatLng(-6.2583, 106.6193),  // UMN
        LatLng(-6.2383, 106.6290),  // Boulevard
        LatLng(-6.2287, 106.6200),  // Sektor 1A
        LatLng(-6.2497, 106.6394),  // Pasar Modern
        LatLng(-6.2510, 106.6000),  // Scientia Square Park

        // === TANGERANG RAYA ===
        LatLng(-6.1781, 106.6381),  // Pusat Kota Tangerang
        LatLng(-6.3000, 106.6527),  // BSD City
        LatLng(-6.1238, 106.6534),  // Bandara Soekarno-Hatta

        // === JAKARTA ===
        LatLng(-6.1754, 106.8272), LatLng(-6.1214, 106.7944),
        LatLng(-6.3025, 106.8950), LatLng(-6.3606, 106.8270),
        LatLng(-6.2253, 106.8096), LatLng(-6.2445, 106.8227),
        LatLng(-6.1900, 106.8494), LatLng(-6.2629, 106.7828),
        LatLng(-6.2088, 106.8456), LatLng(-6.1352, 106.8166),
        LatLng(-6.1623, 106.9002), LatLng(-6.1251, 106.8663),

        // === BODEBEK & JAWA BARAT ===
        LatLng(-6.5950, 106.8061), LatLng(-6.6425, 106.8214),
        LatLng(-6.4025, 106.7942), LatLng(-6.3620, 106.8373),
        LatLng(-6.2383, 106.9756), LatLng(-6.3418, 107.0441),
        LatLng(-6.2618, 107.1523), LatLng(-6.9175, 107.6191),
        LatLng(-6.8329, 107.6042), LatLng(-6.7161, 107.7282),

        // === JAWA TENGAH & TIMUR ===
        LatLng(-6.9667, 110.4167), LatLng(-7.7956, 110.3695),
        LatLng(-7.2575, 112.7521), LatLng(-7.9839, 112.6212)
    )

    suspend fun getRealAirQualityData(): List<AirQualityDataPoint> = withContext(Dispatchers.IO) {
        // Kita mulai request untuk SEMUA lokasi secara bersamaan
        val deferredResults = monitoringLocations.map { location ->
            async {
                try {
                    val response = ApiClient.instance.getAirPollution(
                        location.latitude,
                        location.longitude,
                        apiKey
                    )

                    if (response.list.isNotEmpty()) {
                        val data = response.list[0]


                        val intensityValue = data.components.pm2_5.toInt()


                        AirQualityDataPoint(
                            lat = location.latitude,
                            lon = location.longitude,
                            aqi = intensityValue
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null // Jika gagal fetch satu titik, abaikan saja titik itu
                }
            }
        }

        // Tunggu semua request selesai dan filter yang null (gagal)
        return@withContext deferredResults.awaitAll().filterNotNull()
    }
}