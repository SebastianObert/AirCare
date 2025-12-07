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

// Data class to hold processed daily forecast information for the UI
data class DailyForecast(
    val day: String,
    val iconUrl: String,
    val tempMax: String,
    val tempMin: String,
    val description: String,
    val humidity: String,
    val windSpeed: String,
    val precipitation: String
)

class MainViewModel : ViewModel() {

    private val apiKey = BuildConfig.WEATHER_API_KEY

    var isInitialLocationFetched = false

    private fun getDatabaseReference(): DatabaseReference? {
        // Dapatkan ID unik pengguna dari Firebase Auth
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        return if (userId != null) {
            // Buat 'tabel' history, lalu buat sub-tabel berdasarkan userId
            FirebaseDatabase.getInstance().getReference("history").child(userId)
        } else {
            null
        }
    }

    // LiveData untuk Status UI (kualitas udara)
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

    // LiveData untuk Status UI (Cuaca)
    private val _weatherIconUrl = MutableLiveData<String>()
    val weatherIconUrl: LiveData<String> = _weatherIconUrl

    private val _temperature = MutableLiveData("--°C")
    val temperature: LiveData<String> = _temperature

    private val _weatherDescription = MutableLiveData("Memuat cuaca...")
    val weatherDescription: LiveData<String> = _weatherDescription
    
    // --- LiveData untuk 5-Day Forecast ---
    private val _forecastData = MutableLiveData<List<DailyForecast>>()
    val forecastData: LiveData<List<DailyForecast>> = _forecastData

    // LiveData untuk memberi feedback saat menyimpan (menggunakan kelas Event)
    private val _saveStatus = MutableLiveData<Event<String>>()
    val saveStatus: LiveData<Event<String>> = _saveStatus

    // === MENGONTROL TOMBOL SIMPAN ===
    private val _isDataReadyToSave = MutableLiveData(false)
    val isDataReadyToSave: LiveData<Boolean> = _isDataReadyToSave

    // Variabel Internal untuk Menyimpan Data Terakhir
    private var lastFetchedLocation: String? = null
    private var lastFetchedAqiValue: String? = null
    private var lastFetchedAqiStatus: String? = null
    private var lastFetchedStatusColor: Int? = null
    private var lastFetchedWeatherTemp: String? = null
    private var lastFetchedWeatherCondition: String? = null


    // Logika Utama
    fun updateLocationAndFetchData(latitude: Double, longitude: Double, locationName: String) {
        isInitialLocationFetched = true
        _location.value = locationName
        _aqiStatus.value = "Mengambil data..."
        
        // Reset forecast data while loading new data
        _forecastData.value = emptyList()

        lastFetchedLocation = String.format("Lat: %.2f, Lon: %.2f", latitude, longitude)

        _isDataReadyToSave.value = false
        lastFetchedAqiValue = null

        viewModelScope.launch {
            try {
                // PANGGILAN API PERTAMA: KUALITAS UDARA
                val airQualityResponse = ApiClient.instance.getAirPollution(latitude, longitude, apiKey)
                if (airQualityResponse.list.isNotEmpty()) {
                    processApiResponse(airQualityResponse.list[0])
                } else {
                    showError("Data AQI tidak tersedia")
                }

                // PANGGILAN API KEDUA: CUACA SAAT INI
                val weatherResponse = ApiClient.instance.getCurrentWeather(latitude, longitude, apiKey = apiKey)
                processWeatherResponse(weatherResponse)
                
                // PANGGILAN API KETIGA: 5-DAY/3-HOUR FORECAST
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

        // Simpan data terakhir untuk digunakan oleh fungsi simpan
        lastFetchedAqiValue = convertAqiValueToString(aqi)
        lastFetchedAqiStatus = convertAqiToStatus(aqi)
        lastFetchedStatusColor = convertAqiToDrawable(aqi)

        // Update LiveData untuk UI
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
            lastFetchedWeatherCondition = processedDescription // Store for saving
        }
        val formattedTemp = String.format(Locale.getDefault(), "%.1f°C", weatherData.main.temp)
        _temperature.value = formattedTemp
        lastFetchedWeatherTemp = formattedTemp // Store for saving
    }

    private fun processForecastResponse(response: ForecastResponse) {
        // 1. Group all forecast items by their date (e.g., "2023-10-27")
        val groupedByDay = response.list.groupBy { it.dtTxt.substringBefore(" ") }

        // 2. Process each day's data into a simplified DailyForecast object
        val dailyForecasts = groupedByDay.map { (dateStr, items) ->
            // Find the minimum and maximum temperature for the day
            val minTemp = items.minOf { it.main.temp }
            val maxTemp = items.maxOf { it.main.temp }

            // Get the day name (e.g., "Jumat") from the date string
            val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val dayName = if (date != null) dayFormat.format(date) else "N/A"

            // A simple way to choose a representative item: use the one from midday (12:00 or 15:00)
            // or fall back to the first item for that day.
            val representativeItem = items.find { it.dtTxt.contains("12:00:00") } ?: items.first()
            val icon = representativeItem.weather.firstOrNull()?.icon ?: ""
            val iconUrl = if (icon.isNotEmpty()) "https://openweathermap.org/img/wn/$icon@2x.png" else ""
            
            // Get additional data from the representative item
            val description = representativeItem.weather.firstOrNull()?.description?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: "N/A"
            val humidity = representativeItem.main.humidity?.toString() ?: "N/A"
            val windSpeed = representativeItem.wind.speed.toString()
            val precipitation = (representativeItem.pop * 100).toInt().toString()

            DailyForecast(
                day = dayName,
                iconUrl = iconUrl,
                tempMax = String.format(Locale.getDefault(), "%.0f°", maxTemp),
                tempMin = String.format(Locale.getDefault(), "%.0f°", minTemp),
                description = description,
                humidity = "$humidity%",
                windSpeed = "$windSpeed m/s",
                precipitation = "$precipitation%"
            )
        }.take(5) // Ensure we only display 5 days of forecast

        // 3. Update the LiveData to notify the UI
        _forecastData.value = dailyForecasts
    }


    private fun showError(message: String) {
        _isDataReadyToSave.value = false
        _aqiStatus.value = message
        _aqiValue.value = "--"
        _lastUpdated.value = ""
        _recommendationText.value = "Tidak dapat memuat rekomendasi."
        // Reset nilai polutan
        _pm25Value.value = "-- µg/m³"
        _coValue.value = "-- µg/m³"
        _o3Value.value = "-- µg/m³"
        _no2Value.value = "-- µg/m³"
        _so2Value.value = "-- µg/m³"
        // Reset nilai cuaca
        _temperature.value = "--°C"
        _weatherDescription.value = "Gagal memuat"
        _weatherIconUrl.value = ""
        // Reset forecast
        _forecastData.value = emptyList()
        lastFetchedWeatherTemp = null
        lastFetchedWeatherCondition = null
    }

    private fun convertAqiToIndicatorPosition(aqi: Int): Float {
        return when (aqi) {
            1 -> 0.1f   // 10% dari kiri
            2 -> 0.3f   // 30%
            3 -> 0.5f   // 50% (tengah)
            4 -> 0.7f   // 70%
            5 -> 0.9f   // 90%
            else -> 0.5f
        }
    }

    private fun getHealthRecommendation(aqi: Int): String {
        return when (aqi) {
            1 -> "Kualitas udara sangat baik. Waktu yang tepat untuk aktivitas di luar ruangan."
            2 -> "Kualitas udara cukup baik. Nikmati harimu!"
            3 -> "Kurangi aktivitas berat di luar ruangan jika Anda memiliki sensitivitas pernapasan."
            4 -> "Udara tidak sehat. Batasi waktu di luar dan pertimbangkan menggunakan masker."
            5 -> "Sangat tidak sehat. Hindari semua aktivitas di luar ruangan jika memungkinkan."
            else -> "Data tidak tersedia."
        }
    }

    private fun getRecommendationIcon(aqi: Int): Int {
        return when (aqi) {
            1, 2 -> R.drawable.ic_recommend_good
            3 -> R.drawable.ic_recommend_moderate
            4, 5 -> R.drawable.ic_recommend_bad
            else -> R.drawable.ic_recommend_moderate
        }
    }

    private fun convertAqiValueToString(aqi: Int): String {
        return when (aqi) {
            1 -> "25"  // Baik
            2 -> "75"  // Cukup
            3 -> "125" // Sedang
            4 -> "175" // Buruk
            5 -> "250" // Sangat Buruk
            else -> "--"
        }
    }

    private fun convertAqiToStatus(aqi: Int): String {
        return when (aqi) {
            1 -> "Baik"
            2 -> "Cukup"
            3 -> "Sedang"
            4 -> "Buruk"
            5 -> "Sangat Buruk"
            else -> "Tidak Diketahui"
        }
    }

    private fun convertAqiToDrawable(aqi: Int): Int {
        return when (aqi) {
            1 -> R.drawable.status_bg_green
            2 -> R.drawable.status_bg_yellow
            3 -> R.drawable.status_bg_orange
            4 -> R.drawable.status_bg_red
            5 -> R.drawable.status_bg_maroon
            else -> R.drawable.status_bg_yellow
        }
    }

    fun onSaveButtonClicked() {
        // Dapatkan referensi database yang spesifik untuk user ini
        val database = getDatabaseReference()
        if (database == null) {
            _saveStatus.value = Event("Gagal: Anda harus login untuk menyimpan riwayat.")
            return
        }

        if (lastFetchedAqiValue == null || lastFetchedAqiValue == "--") {
            _saveStatus.value = Event("Gagal: Data kualitas udara belum ada.")
            return
        }

        val historyId = database.push().key
        if (historyId == null) {
            _saveStatus.value = Event("Gagal membuat ID di database.")
            return
        }

        val historyItem = HistoryItem(
            id = historyId,
            location = lastFetchedLocation,
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
            return if (hasBeenHandled) {
                null
            } else {
                hasBeenHandled = true
                content
            }
        }
    }
}
