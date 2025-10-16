package com.example.aircare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val apiKey = "YOUR_API_KEY"

    private val _location = MutableLiveData("Mencari lokasi...")
    val location: LiveData<String> = _location

    private val _aqiValue = MutableLiveData("--")
    val aqiValue: LiveData<String> = _aqiValue

    private val _aqiStatus = MutableLiveData("Menunggu Lokasi")
    val aqiStatus: LiveData<String> = _aqiStatus

    fun updateLocationAndFetchData(latitude: Double, longitude: Double) {
        val locationString = String.format("Lat: %.2f, Lon: %.2f", latitude, longitude)
        _location.value = locationString
        _aqiStatus.value = "Mengambil data..."

        viewModelScope.launch {
            try {
                val response = ApiClient.instance.getAirPollution(latitude, longitude, apiKey)
                if (response.list.isNotEmpty()) {
                    val airData = response.list[0]
                    _aqiValue.value = convertAqiValueToString(airData.main.aqi)
                    _aqiStatus.value = convertAqiToStatus(airData.main.aqi)
                } else {
                    _aqiStatus.value = "Data tidak tersedia"
                }
            } catch (e: Exception) {
                _aqiStatus.value = "Gagal memuat data"
            }
        }
    }

    private fun convertAqiValueToString(aqi: Int): String {
        return when (aqi) {
            1 -> "25"
            2 -> "75"
            3 -> "125"
            4 -> "175"
            5 -> "250"
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

    fun onSaveButtonClicked() {
        _location.value = "Fitur simpan belum diimplementasikan"
    }
}