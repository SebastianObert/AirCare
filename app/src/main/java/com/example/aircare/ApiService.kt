package com.example.aircare

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// Data Classes for Air Pollution
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

// Data Classes for Current Weather
data class WeatherResponse(
    val weather: List<WeatherInfo>,
    val main: MainWeather
)
data class WeatherInfo(
    val description: String,
    val icon: String
)
data class MainWeather(
    val temp: Double,
    val humidity: Int?,
    @SerializedName("temp_min") val tempMin: Double?,
    @SerializedName("temp_max") val tempMax: Double?,
    val pressure: Int?
)

// Data Classes for Geocoding
data class GeocodingResponse(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String
)

// Data Classes for 5-day/3-hour Forecast
data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val main: MainWeather,
    val weather: List<WeatherInfo>,
    val wind: Wind,
    val pop: Double, // Probability of precipitation
    @SerializedName("dt_txt") val dtTxt: String // Date and time of forecast
)

data class Wind(
    val speed: Double,
    val deg: Int
)

interface ApiService {
    @GET("data/2.5/air_pollution")
    suspend fun getAirPollution(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String
    ): AirQualityResponse

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric", // 'metric' untuk Celcius
        @Query("appid") apiKey: String
    ): WeatherResponse

    @GET("geo/1.0/direct")
    suspend fun geocode(
        @Query("q") locationName: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<GeocodingResponse>

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String
    ): ForecastResponse
}
