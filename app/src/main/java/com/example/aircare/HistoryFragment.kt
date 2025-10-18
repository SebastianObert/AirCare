package com.example.aircare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aircare.databinding.FragmentHistoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    // Referensi ke node 'history' di Firebase Realtime Database
    private val database = FirebaseDatabase.getInstance().getReference("history")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.rvHistory.layoutManager = LinearLayoutManager(context)

        // ValueEventListener akan terus "mendengarkan" perubahan data di Firebase.
        // Setiap kali ada data baru, data dihapus, atau data diubah, kode di dalam onDataChange akan berjalan.
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyList = mutableListOf<HistoryItem>()

                // Looping melalui semua "anak" (entri) di dalam node "history"
                for (data in snapshot.children) {
                    val item = data.getValue(HistoryItem::class.java)
                    if (item != null) {
                        historyList.add(item)
                    }
                }

                // Cek apakah daftar riwayat kosong atau tidak
                if (historyList.isEmpty()) {
                    // Jika kosong, sembunyikan RecyclerView dan tampilkan teks
                    binding.rvHistory.visibility = View.GONE
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                } else {
                    // Jika ada data, tampilkan RecyclerView dan sembunyikan teks
                    binding.rvHistory.visibility = View.VISIBLE
                    binding.tvEmptyHistory.visibility = View.GONE

                    // Buat adapter baru dengan data dari Firebase
                    // Kita urutkan berdasarkan timestamp (waktu) agar yang terbaru muncul di atas
                    val historyAdapter = HistoryAdapter(historyList.sortedByDescending { it.timestamp })
                    binding.rvHistory.adapter = historyAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Tampilkan pesan error jika gagal mengambil data dari Firebase
                binding.rvHistory.visibility = View.GONE
                binding.tvEmptyHistory.visibility = View.VISIBLE
                binding.tvEmptyHistory.text = "Gagal memuat data riwayat."
            }
        })
    }

    // Fungsi createDummyData() tidak lagi dibutuhkan, bisa dihapus.
    // private fun createDummyData(): List<HistoryItem> { ... }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}