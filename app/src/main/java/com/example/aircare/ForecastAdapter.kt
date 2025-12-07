package com.example.aircare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aircare.databinding.ItemForecastDayBinding

class ForecastAdapter(private var forecastList: List<DailyForecast>) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    fun updateData(newForecastList: List<DailyForecast>) {
        forecastList = newForecastList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemForecastDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(forecastList[position])
    }

    override fun getItemCount() = forecastList.size

    inner class ForecastViewHolder(private val binding: ItemForecastDayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(forecast: DailyForecast) {
            binding.tvDay.text = forecast.day
            binding.tvTempMax.text = forecast.tempMax
            binding.tvTempMin.text = forecast.tempMin
            
            if (forecast.iconUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(forecast.iconUrl)
                    .into(binding.ivWeatherIcon)
            }
        }
    }
}