package com.example.aircare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aircare.databinding.ItemSavedLocationBinding

class SavedLocationAdapter(
    private val locations: List<SavedLocation>,
    private val onItemClick: (SavedLocation) -> Unit,
    private val onDeleteClick: (SavedLocation) -> Unit
) : RecyclerView.Adapter<SavedLocationAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSavedLocationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = locations[position]

        holder.binding.tvLocationName.text = location.name
        holder.itemView.setOnClickListener {
            onItemClick(location)
        }
        holder.binding.ivDeleteLocation.setOnClickListener {
            onDeleteClick(location)
        }
    }

    override fun getItemCount() = locations.size
}