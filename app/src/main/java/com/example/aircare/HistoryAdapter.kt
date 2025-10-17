package com.example.aircare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aircare.databinding.ItemHistoryBinding

class HistoryAdapter(private val items: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvHistoryDate.text = item.date
            tvHistoryLocation.text = item.location
            tvHistoryAqiValue.text = item.aqiValue
            viewStatusColor.setBackgroundResource(item.statusColor)
        }
    }

    override fun getItemCount(): Int = items.size
}