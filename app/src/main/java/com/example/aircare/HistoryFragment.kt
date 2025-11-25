package com.example.aircare

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aircare.databinding.FragmentHistoryBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryAdapter
    private val list = mutableListOf<HistoryItem>()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database: DatabaseReference? =
        userId?.let { FirebaseDatabase.getInstance().getReference("history").child(it) }

    private var valueEventListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter(list) { item -> deleteItem(item) }

        binding.rvHistory.layoutManager = LinearLayoutManager(context)
        binding.rvHistory.adapter = adapter

        // Panggil fungsi untuk setup chart
        setupBarChart(binding.barChart)

        listenToDatabase()
    }

    private fun setupBarChart(barChart: BarChart) {
        // 1. Buat data dummy untuk BarChart
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 35f)) // PM2.5
        entries.add(BarEntry(1f, 15f)) // CO
        entries.add(BarEntry(2f, 25f)) // O3
        entries.add(BarEntry(3f, 20f)) // NO2
        entries.add(BarEntry(4f, 10f)) // SO2

        val labels = arrayOf("PM2.5", "CO", "O3", "NO2", "SO2")

        // 2. Buat DataSet dari data
        val dataSet = BarDataSet(entries, "Tingkat Polutan")
        dataSet.colors = listOf(
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF7043"),
            Color.parseColor("#8D6E63"),
            Color.parseColor("#78909C"),
            Color.parseColor("#9CCC65")
        )
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        // 3. Buat BarData dan set ke chart
        val barData = BarData(dataSet)
        barChart.data = barData

        // 4. Konfigurasi tampilan chart
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setFitBars(true)
        barChart.legend.isEnabled = false

        // Konfigurasi Sumbu X (Label Polutan)
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        // Konfigurasi Sumbu Y (Kiri dan Kanan)
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false

        // 5. Animasikan dan refresh chart
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun listenToDatabase() {
        if (database == null) {
            _binding?.let {
                it.tvEmptyHistory.text = "Anda belum login."
                it.tvEmptyHistory.visibility = View.VISIBLE
                it.rvHistory.visibility = View.GONE
                it.cardChart.visibility = View.GONE
            }
            return
        }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (_binding == null || !isAdded) return

                val tempList = mutableListOf<HistoryItem>()
                for (data in snapshot.children) {
                    data.getValue(HistoryItem::class.java)?.let { tempList.add(it) }
                }

                if (tempList.isEmpty()) {
                    _binding?.apply {
                        tvEmptyHistory.visibility = View.VISIBLE
                        rvHistory.visibility = View.GONE
                        cardChart.visibility = View.GONE // Sembunyikan chart jika data kosong
                    }
                } else {
                    _binding?.apply {
                        tvEmptyHistory.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        cardChart.visibility = View.VISIBLE // Tampilkan chart jika ada data
                        adapter.updateList(tempList.sortedByDescending { it.timestamp })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null || !isAdded) return
                _binding?.tvEmptyHistory?.apply {
                    text = "Gagal memuat data: ${error.message}"
                    visibility = View.VISIBLE
                }
            }
        }

        database.addValueEventListener(valueEventListener!!)
    }

    private fun deleteItem(item: HistoryItem) {
        database?.child(item.id ?: return)?.removeValue()
            ?.addOnSuccessListener {
                Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener {
                Toast.makeText(context, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (database != null && valueEventListener != null) {
            database.removeEventListener(valueEventListener!!)
        }
        valueEventListener = null
        _binding = null
    }
}