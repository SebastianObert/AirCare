package com.example.aircare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.aircare.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var items: List<HistoryItem>,
    private val onDeleteClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        holder.binding.tvHistoryDate.text = sdf.format(Date(item.timestamp ?: 0))
        holder.binding.tvHistoryLocation.text = item.location ?: "-"
        holder.binding.tvHistoryAqiValue.text = item.aqiValue ?: "-"
        holder.binding.viewStatusColor.setBackgroundResource(item.statusColor)

        // tombol hapus
        holder.binding.ivDelete.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Hapus Riwayat")
                .setMessage("Apakah kamu yakin ingin menghapus data ini?")
                .setPositiveButton("Ya") { _, _ ->
                    onDeleteClick(item)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    fun updateList(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
