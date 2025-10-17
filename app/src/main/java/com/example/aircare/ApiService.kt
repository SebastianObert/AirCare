package com.example.aircare

import retrofit2.http.GET
import retrofit2.http.Query

data class AirQualityResponse(val list: List<AirData>)
data class AirData(val main: MainAqi, val components: Components)
data class MainAqi(val aqi: Int)
data class Components(
    val co: Double,
    val no2: Double,
    val o3: Double,
    val so2: Double,
    val pm2_5: Double
)

interface ApiService {
    @GET("data/2.5/air_pollution")
    suspend fun getAirPollution(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String
    ): AirQualityResponse
}