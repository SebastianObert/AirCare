package com.example.aircare

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data class for Daily Forecast UI
data class DailyForecast(
    val day: String,
    val iconUrl: String,
    val tempMax: String,
    val tempMin: String,
    val description: String,
    val humidity: String,
    val windSpeed: String,
    val precipitation: String,
    val pressure: String,
    val uvIndex: String
)

class MainViewModel : ViewModel() {

    private val apiKey = BuildConfig.WEATHER_API_KEY
    var isInitialLocationFetched = false

    // --- MEMORY VARIABLES FOR SAVING (CRITICAL) ---
    // We store the raw coordinates separately to ensure accurate saving format
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    private fun getDatabaseReference(): DatabaseReference? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        return if (userId != null) {
            FirebaseDatabase.getInstance().getReference("history").child(userId)
        } else {
            null
        }
    }

    // LiveData for UI
    private val _location = MutableLiveData("Mencari lokasi...")
    val location: LiveData<String> = _location

    private val _lastUpdated = MutableLiveData("")
    val lastUpdated: LiveData<String> = _lastUpdated

    private val _aqiValue = MutableLiveData("--")
    val aqiValue: LiveData<String> = _aqiValue

    private val _aqiStatus = MutableLiveData("Menunggu Lokasi")
    val aqiStatus: LiveData<String> = _aqiStatus

    private val _aqiStatusBackground = MutableLiveData(R.drawable.status_bg_yellow)
    val aqiStatusBackground: LiveData<Int> = _aqiStatusBackground

    private val _aqiIndicatorPosition = MutableLiveData(0.0f)
    val aqiIndicatorPosition: LiveData<Float> = _aqiIndicatorPosition

    private val _recommendationIcon = MutableLiveData<Int>()
    val recommendationIcon: LiveData<Int> = _recommendationIcon

    private val _recommendationText = MutableLiveData("Dapatkan lokasi Anda untuk melihat rekomendasi.")
    val recommendationText: LiveData<String> = _recommendationText

    private val _pm25Value = MutableLiveData("-- µg/m³")
    val pm25Value: LiveData<String> = _pm25Value
    private val _coValue = MutableLiveData("-- µg/m³")
    val coValue: LiveData<String> = _coValue
    private val _o3Value = MutableLiveData("-- µg/m³")
    val o3Value: LiveData<String> = _o3Value
    private val _no2Value = MutableLiveData("-- µg/m³")
    val no2Value: LiveData<String> = _no2Value
    private val _so2Value = MutableLiveData("-- µg/m³")
    val so2Value: LiveData<String> = _so2Value

    private val _weatherIconUrl = MutableLiveData<String>()
    val weatherIconUrl: LiveData<String> = _weatherIconUrl

    private val _temperature = MutableLiveData("--°C")
    val temperature: LiveData<String> = _temperature

    private val _weatherDescription = MutableLiveData("Memuat cuaca...")
    val weatherDescription: LiveData<String> = _weatherDescription

    private val _forecastData = MutableLiveData<List<DailyForecast>>()
    val forecastData: LiveData<List<DailyForecast>> = _forecastData

    private val _saveStatus = MutableLiveData<Event<String>>()
    val saveStatus: LiveData<Event<String>> = _saveStatus

    private val _isDataReadyToSave = MutableLiveData(false)
    val isDataReadyToSave: LiveData<Boolean> = _isDataReadyToSave

    // Internal variables for last fetched data
    private var lastFetchedAqiValue: String? = null
    private var lastFetchedAqiStatus: String? = null
    private var lastFetchedStatusColor: Int? = null
    private var lastFetchedWeatherTemp: String? = null
    private var lastFetchedWeatherCondition: String? = null


    // --- MAIN LOGIC ---
    fun updateLocationAndFetchData(latitude: Double, longitude: Double, locationName: String) {
        // 1. STORE COORDINATES IN MEMORY
        this.currentLatitude = latitude
        this.currentLongitude = longitude

        isInitialLocationFetched = true
        _location.value = locationName
        _aqiStatus.value = "Mengambil data..."

        _forecastData.value = emptyList()

        _isDataReadyToSave.value = false
        lastFetchedAqiValue = null

        viewModelScope.launch {
            try {
                // API CALL 1: AIR QUALITY
                val airQualityResponse = ApiClient.instance.getAirPollution(latitude, longitude, apiKey)
                if (airQualityResponse.list.isNotEmpty()) {
                    processApiResponse(airQualityResponse.list[0])
                } else {
                    showError("Data AQI tidak tersedia")
                }

                // API CALL 2: CURRENT WEATHER
                val weatherResponse = ApiClient.instance.getCurrentWeather(latitude, longitude, apiKey = apiKey)
                processWeatherResponse(weatherResponse)

                // API CALL 3: FORECAST
                val forecastResponse = ApiClient.instance.getForecast(latitude, longitude, apiKey = apiKey)
                processForecastResponse(forecastResponse)

            } catch (e: Exception) {
                Log.e("AirCareAPI", "Gagal memanggil API: ${e.message}", e)
                showError("Gagal memuat data")
            }
        }
    }

    private fun processApiResponse(airData: AirData) {
        val aqi = airData.main.aqi
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        _lastUpdated.value = "Diperbarui: ${sdf.format(Date())}"

        lastFetchedAqiValue = convertAqiValueToString(aqi)
        lastFetchedAqiStatus = convertAqiToStatus(aqi)
        lastFetchedStatusColor = convertAqiToDrawable(aqi)

        _aqiValue.value = lastFetchedAqiValue
        _aqiStatus.value = lastFetchedAqiStatus
        _aqiStatusBackground.value = lastFetchedStatusColor ?: R.drawable.status_bg_yellow
        _aqiIndicatorPosition.value = convertAqiToIndicatorPosition(aqi)
        _recommendationIcon.value = getRecommendationIcon(aqi)
        _recommendationText.value = getHealthRecommendation(aqi)

        val components = airData.components
        _pm25Value.value = "${components.pm2_5} µg/m³"
        _coValue.value = "${components.co} µg/m³"
        _o3Value.value = "${components.o3} µg/m³"
        _no2Value.value = "${components.no2} µg/m³"
        _so2Value.value = "${components.so2} µg/m³"

        _isDataReadyToSave.value = true
    }

    private fun processWeatherResponse(weatherData: WeatherResponse) {
        if (weatherData.weather.isNotEmpty()) {
            val weatherInfo = weatherData.weather[0]
            val processedDescription = weatherInfo.description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            _weatherDescription.value = processedDescription
            _weatherIconUrl.value = "https://openweathermap.org/img/wn/${weatherInfo.icon}@2x.png"
            lastFetchedWeatherCondition = processedDescription
        }
        val formattedTemp = String.format(Locale.getDefault(), "%.1f°C", weatherData.main.temp)
        _temperature.value = formattedTemp
        lastFetchedWeatherTemp = formattedTemp
    }

    private fun processForecastResponse(response: ForecastResponse) {
        val groupedByDay = response.list.groupBy { it.dtTxt.substringBefore(" ") }
        val dailyForecasts = groupedByDay.map { (dateStr, items) ->
            val minTemp = items.minOf { it.main.temp }
            val maxTemp = items.maxOf { it.main.temp }
            val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val dayName = if (date != null) dayFormat.format(date) else "N/A"
            val representativeItem = items.find { it.dtTxt.contains("12:00:00") } ?: items.first()
            val icon = representativeItem.weather.firstOrNull()?.icon ?: ""
            val iconUrl = if (icon.isNotEmpty()) "https://openweathermap.org/img/wn/$icon@2x.png" else ""
            val description = representativeItem.weather.firstOrNull()?.description?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "N/A"
            val humidity = representativeItem.main.humidity?.toString() ?: "N/A"
            val windSpeed = representativeItem.wind.speed.toString()
            val precipitation = (representativeItem.pop * 100).toInt().toString()
            val pressure = representativeItem.main.pressure?.toString() ?: "N/A"
            val uvIndex = "N/A" // Placeholder

            DailyForecast(
                day = dayName,
                iconUrl = iconUrl,
                tempMax = String.format(Locale.getDefault(), "%.0f°", maxTemp),
                tempMin = String.format(Locale.getDefault(), "%.0f°", minTemp),
                description = description,
                humidity = "$humidity%",
                windSpeed = "$windSpeed m/s",
                precipitation = "$precipitation%",
                pressure = "$pressure hPa",
                uvIndex = uvIndex
            )
        }.take(5)
        _forecastData.value = dailyForecasts
    }

    private fun showError(message: String) {
        _isDataReadyToSave.value = false
        _aqiStatus.value = message
        _aqiValue.value = "--"
        _lastUpdated.value = ""
        _pm25Value.value = "-- µg/m³"
        _coValue.value = "-- µg/m³"
        _o3Value.value = "-- µg/m³"
        _no2Value.value = "-- µg/m³"
        _so2Value.value = "-- µg/m³"
        _temperature.value = "--°C"
        _weatherDescription.value = "Gagal memuat"
        _forecastData.value = emptyList()
    }

    // Helper functions for AQI conversion...
    private fun convertAqiToIndicatorPosition(aqi: Int): Float = when(aqi) { 1->0.1f; 2->0.3f; 3->0.5f; 4->0.7f; 5->0.9f; else->0.5f }
    private fun getHealthRecommendation(aqi: Int): String = when(aqi) {
        1 -> "Kualitas udara sangat baik. Waktu yang tepat untuk aktivitas di luar ruangan."
        2 -> "Kualitas udara cukup baik. Nikmati harimu!"
        3 -> "Kurangi aktivitas berat di luar ruangan jika Anda memiliki sensitivitas pernapasan."
        4 -> "Udara tidak sehat. Batasi waktu di luar dan pertimbangkan menggunakan masker."
        5 -> "Sangat tidak sehat. Hindari semua aktivitas di luar ruangan jika memungkinkan."
        else -> "Data tidak tersedia."
    }
    private fun getRecommendationIcon(aqi: Int): Int = when(aqi) { 1,2->R.drawable.ic_recommend_good; 3->R.drawable.ic_recommend_moderate; 4,5->R.drawable.ic_recommend_bad; else->R.drawable.ic_recommend_moderate }
    private fun convertAqiValueToString(aqi: Int): String = when(aqi) { 1->"25"; 2->"75"; 3->"125"; 4->"175"; 5->"250"; else->"--" }
    private fun convertAqiToStatus(aqi: Int): String = when(aqi) { 1->"Baik"; 2->"Cukup"; 3->"Sedang"; 4->"Buruk"; 5->"Sangat Buruk"; else->"Tidak Diketahui" }
    private fun convertAqiToDrawable(aqi: Int): Int = when(aqi) { 1->R.drawable.status_bg_green; 2->R.drawable.status_bg_yellow; 3->R.drawable.status_bg_orange; 4->R.drawable.status_bg_red; 5->R.drawable.status_bg_maroon; else->R.drawable.status_bg_yellow }

    // --- SAVE BUTTON LOGIC ---
    fun onSaveButtonClicked() {
        val database = getDatabaseReference()
        if (database == null) {
            _saveStatus.value = Event("Gagal: Anda harus login untuk menyimpan riwayat.")
            return
        }

        if (lastFetchedAqiValue == null || lastFetchedAqiValue == "--") {
            _saveStatus.value = Event("Gagal: Data kualitas udara belum ada.")
            return
        }

        val historyId = database.push().key ?: return

        // CRITICAL FIX: Use the raw coordinates formatted exactly how HistoryAdapter expects them
        // Your adapter uses getCityName() which expects "Lat: x, Lon: y"
        val locationStringForDb = "Lat: $currentLatitude, Lon: $currentLongitude"

        val historyItem = HistoryItem(
            id = historyId,
            location = locationStringForDb, // Save exact coordinates
            aqiValue = lastFetchedAqiValue,
            aqiStatus = lastFetchedAqiStatus,
            timestamp = System.currentTimeMillis(),
            statusColor = lastFetchedStatusColor ?: R.drawable.status_bg_yellow,
            weatherTemp = lastFetchedWeatherTemp,
            weatherCondition = lastFetchedWeatherCondition
        )

        database.child(historyId).setValue(historyItem)
            .addOnSuccessListener {
                _saveStatus.value = Event("Data berhasil disimpan ke riwayat!")
            }
            .addOnFailureListener { exception ->
                _saveStatus.value = Event("Gagal menyimpan data: ${exception.message}")
            }
    }

    open class Event<out T>(private val content: T) {
        var hasBeenHandled = false
            private set
        fun getContentIfNotHandled(): T? {
            return if (hasBeenHandled) null else { hasBeenHandled = true; content }
        }
    }
}