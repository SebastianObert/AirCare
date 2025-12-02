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
            // Panggil Repository untuk mengambil data REAL dari API
            // Ini akan memakan waktu sedikit karena melakukan banyak request network
            val realDataList = repository.getRealAirQualityData()

            if (realDataList.isNotEmpty()) {
                val weightedData = realDataList.map { dataPoint ->
                    // Masukkan Lat, Lon, dan Intensitas (PM2.5 atau AQI) ke WeightedLatLng
                    WeightedLatLng(
                        LatLng(dataPoint.lat, dataPoint.lon),
                        dataPoint.aqi.toDouble()
                    )
                }
                airQualityDataPoints.postValue(weightedData)
            }
        }
    }
}