package com.example.aircare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aircare.databinding.ItemForecastDayBinding

data class Forecast(
    val day: String,
    val icon: Int, // Placeholder for drawable resource
    val tempMax: String,
    val tempMin: String
)

class ForecastAdapter(private val forecastList: List<Forecast>) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemForecastDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(forecastList[position])
    }

    override fun getItemCount() = forecastList.size

    inner class ForecastViewHolder(private val binding: ItemForecastDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(forecast: Forecast) {
            binding.tvDay.text = forecast.day
            binding.ivWeatherIcon.setImageResource(forecast.icon)
            binding.tvTempMax.text = forecast.tempMax
            binding.tvTempMin.text = forecast.tempMin
        }
    }
}