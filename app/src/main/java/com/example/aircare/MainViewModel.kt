package com.example.aircare

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val apiKey = "3edfd82f93e2e50f7497a083e88ece56"

    private val _location = MutableLiveData("Mencari lokasi...")
    val location: LiveData<String> = _location

    private val _aqiValue = MutableLiveData("--")
    val aqiValue: LiveData<String> = _aqiValue

    private val _aqiStatus = MutableLiveData("Menunggu Lokasi")
    val aqiStatus: LiveData<String> = _aqiStatus

    private val _aqiStatusBackground = MutableLiveData<Int>()
    val aqiStatusBackground: LiveData<Int> = _aqiStatusBackground

    private val _pm25Value = MutableLiveData<String>("-- µg/m³")
    val pm25Value: LiveData<String> = _pm25Value

    private val _coValue = MutableLiveData<String>("-- µg/m³")
    val coValue: LiveData<String> = _coValue


    fun updateLocationAndFetchData(latitude: Double, longitude: Double) {
        val locationString = String.format("Lat: %.2f, Lon: %.2f", latitude, longitude)
        _location.value = locationString
        _aqiStatus.value = "Mengambil data..."

        viewModelScope.launch {
            try {
                val response = ApiClient.instance.getAirPollution(latitude, longitude, apiKey)
                if (response.list.isNotEmpty()) {
                    val airData = response.list[0]
                    val aqi = airData.main.aqi

                    _aqiValue.value = convertAqiValueToString(aqi)
                    _aqiStatus.value = convertAqiToStatus(aqi)
                    _aqiStatusBackground.value = convertAqiToDrawable(aqi)

                    val components = airData.components
                    _pm25Value.value = "${components.pm2_5} µg/m³"
                    _coValue.value = "${components.co} µg/m³"

                } else {
                    _aqiStatus.value = "Data tidak tersedia"
                }
            } catch (e: Exception) {
                Log.e("AirCareAPI", "Gagal memanggil API: ${e.message}", e)
                _aqiStatus.value = "Gagal memuat data"
            }
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
        _location.value = "Fitur simpan belum diimplementasikan"
    }
}