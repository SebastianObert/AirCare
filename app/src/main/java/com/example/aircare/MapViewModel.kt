package com.example.aircare

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlinx.coroutines.launch

class MapViewModel(private val repository: AirQualityRepository) : ViewModel() {

    // LiveData untuk menyimpan titik data kualitas udara
    val airQualityDataPoints = MutableLiveData<List<WeightedLatLng>>()

    fun fetchAirQualityDataForMap() {
        viewModelScope.launch {
            // Panggil Repository untuk mendapatkan data dari API
            val result = repository.getAirQualityDataForBounds(/* kirim batas peta saat ini */)

            if (result.isSuccess) {
                val apiResponse = result.getOrNull()
                // Konversi respons API menjadi List<WeightedLatLng>
                val weightedData = apiResponse?.map {
                    // "Intensity" atau "bobot" di sini adalah nilai AQI.
                    // Semakin tinggi AQI, semakin "panas" titiknya di heatmap.
                    WeightedLatLng(LatLng(it.lat, it.lon), it.aqi.toDouble())
                }
                airQualityDataPoints.postValue(weightedData ?: emptyList())
            }
        }
    }
}