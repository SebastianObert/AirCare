package com.example.aircare

import com.google.android.gms.maps.model.LatLng

class AirQualityRepository {

    // Data dummy yang lebih bervariasi untuk simulasi heatmap di Pulau Jawa
    private val dummyData = listOf(

        // === GADING SERPONG & SEKITARNYA ===
        AirQualityDataPoint(lat = -6.2413, lon = 106.6263, aqi = 145),  // Summarecon Mall Serpong (Pusat keramaian)
        AirQualityDataPoint(lat = -6.2583, lon = 106.6193, aqi = 110),  // Universitas Multimedia Nusantara (UMN)
        AirQualityDataPoint(lat = -6.2383, lon = 106.6290, aqi = 150),  // Jl. Gading Serpong Boulevard (Lalu lintas padat)
        AirQualityDataPoint(lat = -6.2287, lon = 106.6200, aqi = 95),   // Perumahan Sektor 1A (Lebih hijau)
        AirQualityDataPoint(lat = -6.2497, lon = 106.6394, aqi = 135),  // Pasar Modern Paramount
        AirQualityDataPoint(lat = -6.2510, lon = 106.6000, aqi = 120),  // Scientia Square Park (area rekreasi)

        // === TANGERANG RAYA ===
        AirQualityDataPoint(lat = -6.1781, lon = 106.6381, aqi = 140),  // Pusat Kota Tangerang
        AirQualityDataPoint(lat = -6.3000, lon = 106.6527, aqi = 165),  // BSD City
        AirQualityDataPoint(lat = -6.1238, lon = 106.6534, aqi = 180),  // Bandara Soekarno-Hatta

    ).plus(listOf(
        // Data Jakarta yang sudah ada
        AirQualityDataPoint(lat = -6.1754, lon = 106.8272, aqi = 45), AirQualityDataPoint(lat = -6.1214, lon = 106.7944, aqi = 50), AirQualityDataPoint(lat = -6.3025, lon = 106.8950, aqi = 40), AirQualityDataPoint(lat = -6.3606, lon = 106.8270, aqi = 35),
        AirQualityDataPoint(lat = -6.2253, lon = 106.8096, aqi = 75), AirQualityDataPoint(lat = -6.2445, lon = 106.8227, aqi = 90), AirQualityDataPoint(lat = -6.1900, lon = 106.8494, aqi = 85), AirQualityDataPoint(lat = -6.2629, lon = 106.7828, aqi = 95),
        AirQualityDataPoint(lat = -6.2088, lon = 106.8456, aqi = 155), AirQualityDataPoint(lat = -6.1352, lon = 106.8166, aqi = 170), AirQualityDataPoint(lat = -6.1623, lon = 106.9002, aqi = 185), AirQualityDataPoint(lat = -6.1251, lon = 106.8663, aqi = 190),
        // Data Bodebek yang sudah ada
        AirQualityDataPoint(lat = -6.5950, lon = 106.8061, aqi = 60), AirQualityDataPoint(lat = -6.6425, lon = 106.8214, aqi = 70),
        AirQualityDataPoint(lat = -6.4025, lon = 106.7942, aqi = 110), AirQualityDataPoint(lat = -6.3620, lon = 106.8373, aqi = 125),
        AirQualityDataPoint(lat = -6.2383, lon = 106.9756, aqi = 215), AirQualityDataPoint(lat = -6.3418, lon = 107.0441, aqi = 240), AirQualityDataPoint(lat = -6.2618, lon = 107.1523, aqi = 250),
        // Data Jawa Barat lainnya yang sudah ada
        AirQualityDataPoint(lat = -6.9175, lon = 107.6191, aqi = 175), AirQualityDataPoint(lat = -6.8329, lon = 107.6042, aqi = 40), AirQualityDataPoint(lat = -6.7161, lon = 107.7282, aqi = 195),
        AirQualityDataPoint(lat = -6.7031, lon = 106.9946, aqi = 30),
        AirQualityDataPoint(lat = -6.7063, lon = 108.5570, aqi = 130),
        // Data Jawa Tengah & Yogyakarta yang sudah ada
        AirQualityDataPoint(lat = -6.9667, lon = 110.4167, aqi = 115),
        AirQualityDataPoint(lat = -7.7956, lon = 110.3695, aqi = 80), AirQualityDataPoint(lat = -7.5561, lon = 110.8318, aqi = 75),
        AirQualityDataPoint(lat = -7.2100, lon = 109.9102, aqi = 15),
        // Data Jawa Timur yang sudah ada
        AirQualityDataPoint(lat = -7.2575, lon = 112.7521, aqi = 205), AirQualityDataPoint(lat = -7.2458, lon = 112.7378, aqi = 220), AirQualityDataPoint(lat = -7.1472, lon = 112.6393, aqi = 260),
        AirQualityDataPoint(lat = -7.9839, lon = 112.6212, aqi = 65),
        AirQualityDataPoint(lat = -7.9425, lon = 112.9533, aqi = 10)
    ))

    fun getAirQualityDataForBounds(/* bounds: LatLngBounds */): Result<List<AirQualityDataPoint>> {
        // Mengembalikan data dummy yang sudah diperkaya untuk seluruh Jawa
        return Result.success(dummyData)
    }
}