package com.example.aircare

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.aircare.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var items: List<HistoryItem>,
    private val onDeleteClick: (HistoryItem) -> Unit,
    private val onNoteClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    @SuppressLint("SetTextI18n") // Supresi warning penggabungan string pada setText
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        // Gunakan Locale Indonesia untuk format tanggal
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        holder.binding.tvHistoryDate.text = sdf.format(Date(item.timestamp ?: 0))

        // Ubah koordinat menjadi nama lokasi
        val locationText = getCityName(context, item.location)
        holder.binding.tvHistoryLocation.text = locationText

        holder.binding.tvHistoryAqiValue.text = "AQI: ${item.aqiValue} - ${item.aqiStatus}"

        // Menampilkan informasi cuaca jika tersedia
        if (item.weatherTemp != null && item.weatherCondition != null) {
            holder.binding.tvWeatherInfo.visibility = View.VISIBLE
            holder.binding.tvWeatherInfo.text = "Cuaca: ${item.weatherTemp} - ${item.weatherCondition}"
        } else {
            holder.binding.tvWeatherInfo.visibility = View.GONE
        }

        holder.binding.viewStatusColor.setBackgroundResource(item.statusColor)

        if (!item.note.isNullOrEmpty()) {
            holder.binding.tvNote.visibility = View.VISIBLE
            holder.binding.tvNote.text = "[${item.category}] ${item.note}"
        } else {
            holder.binding.tvNote.visibility = View.GONE
        }

        holder.binding.ivAddNote.setOnClickListener {
            onNoteClick(item)
        }

        holder.binding.ivDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Hapus Riwayat")
                .setMessage("Apakah kamu yakin ingin menghapus data ini?")
                .setPositiveButton("Ya") { _, _ -> onDeleteClick(item) }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    /**
     * Fungsi untuk mengubah string koordinat (Lat/Lon) menjadi nama jalan/daerah.
     * Menggunakan Locale ID agar hasil alamat dalam Bahasa Indonesia.
     */
    private fun getCityName(context: Context, location: String?): String {
        if (location == null) return "Lokasi tidak diketahui"

        return try {
            // 1. Bersihkan string dari teks "Lat:", "Lon:", koma, dan spasi berlebih
            // Contoh Input: "Lat: -6.24, Lon: 106.63" -> Menjadi: "-6.24 106.63"
            val cleanLocation = location
                .replace("Lat:", "", ignoreCase = true)
                .replace("Lon:", "", ignoreCase = true)
                .replace(",", "") // Hapus koma
                .replace("Lat", "", ignoreCase = true) // Jaga-jaga format tanpa titik dua
                .replace("Lon", "", ignoreCase = true)
                .trim()

            // 2. Pisahkan Lat dan Lon berdasarkan spasi
            val parts = cleanLocation.split("\\s+".toRegex())

            if (parts.size >= 2) {
                val lat = parts[0].toDouble()
                val lon = parts[1].toDouble()

                val geocoder = Geocoder(context, Locale("id", "ID"))

                // Suppress deprecation agar tidak kuning di API level tinggi,
                // karena kita menggunakan method getFromLocation versi standar (synchronous)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]

                    val street = address.thoroughfare     // Nama Jalan
                    val district = address.subLocality    // Kecamatan/Kelurahan
                    val city = address.locality           // Kota/Kabupaten
                    val adminArea = address.adminArea     // Provinsi

                    // Logika Prioritas Tampilan:
                    return when {
                        !street.isNullOrEmpty() && !city.isNullOrEmpty() -> "$street, $city"
                        !district.isNullOrEmpty() && !city.isNullOrEmpty() -> "$district, $city"
                        !city.isNullOrEmpty() -> "$city, $adminArea"
                        else -> address.getAddressLine(0) ?: location
                    }
                }
            }
            // Jika parsing gagal atau tidak ada hasil, kembalikan teks asli
            location
        } catch (e: Exception) {
            // Jika terjadi error (misal tidak ada internet), kembalikan teks asli
            location
        }
    }

    @SuppressLint("NotifyDataSetChanged") // Supresi warning update list keseluruhan
    fun updateList(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}