package com.example.aircare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.aircare.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Buat dummy data
        val dummyHistoryList = createDummyData()

        // Setup RecyclerView dengan Adapter
        val historyAdapter = HistoryAdapter(dummyHistoryList)
        binding.rvHistory.adapter = historyAdapter
    }

    private fun createDummyData(): List<HistoryItem> {
        return listOf(
            HistoryItem(
                date = "16 Okt 2025, 08:30",
                location = "Jakarta Pusat",
                aqiValue = "125",
                aqiStatus = "Sedang",
                statusColor = R.drawable.status_bg_orange
            ),
            HistoryItem(
                date = "15 Okt 2025, 14:00",
                location = "Bandung",
                aqiValue = "75",
                aqiStatus = "Cukup",
                statusColor = R.drawable.status_bg_yellow
            ),
            HistoryItem(
                date = "14 Okt 2025, 09:15",
                location = "Surabaya",
                aqiValue = "45",
                aqiStatus = "Baik",
                statusColor = R.drawable.status_bg_green
            ),
            HistoryItem(
                date = "13 Okt 2025, 17:45",
                location = "Medan",
                aqiValue = "180",
                aqiStatus = "Buruk",
                statusColor = R.drawable.status_bg_red
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}