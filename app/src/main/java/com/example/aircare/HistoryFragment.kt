package com.example.aircare

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aircare.databinding.DialogAddNoteBinding
import com.example.aircare.databinding.FragmentHistoryBinding
import com.example.aircare.util.NotificationHelper
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryAdapter
    private val list = mutableListOf<HistoryItem>()
    private val viewModel: HistoryViewModel by viewModels()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database: DatabaseReference? =
        userId?.let { FirebaseDatabase.getInstance().getReference("history").child(it) }

    private var valueEventListener: ValueEventListener? = null

    // SharedPreferences constants
    private val PREFS_NAME = "AirCarePrefs"
    private val NOTIFICATIONS_ENABLED = "notifications_enabled"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter(list, { item -> deleteItem(item) }) { item ->
            showAddNoteDialog(item)
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(context)
        binding.rvHistory.adapter = adapter

        setupBarChart(binding.barChart)
        listenToDatabase()
    }

    private fun showAddNoteDialog(historyItem: HistoryItem) {
        val dialogBinding = DialogAddNoteBinding.inflate(LayoutInflater.from(context))

        dialogBinding.etNote.setText(historyItem.note)
        when (historyItem.category) {
            "Alergi" -> dialogBinding.chipGroupCategory.check(R.id.chipAlergi)
            "Penyakit" -> dialogBinding.chipGroupCategory.check(R.id.chipPenyakit)
            "Lainnya" -> dialogBinding.chipGroupCategory.check(R.id.chipLainnya)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle("Catatan Pribadi")
            .setPositiveButton("Simpan") { _, _ ->
                val noteText = dialogBinding.etNote.text.toString()
                val selectedChipId = dialogBinding.chipGroupCategory.checkedChipId
                val categoryText = when (selectedChipId) {
                    R.id.chipAlergi -> "Alergi"
                    R.id.chipPenyakit -> "Penyakit"
                    else -> "Lainnya"
                }

                historyItem.note = noteText
                historyItem.category = categoryText
                viewModel.saveNoteForHistoryItem(historyItem)
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
    }

    private fun setupBarChart(barChart: BarChart) {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 35f))
        entries.add(BarEntry(1f, 15f))
        entries.add(BarEntry(2f, 25f))
        entries.add(BarEntry(3f, 20f))
        entries.add(BarEntry(4f, 10f))

        val labels = arrayOf("PM2.5", "CO", "O3", "NO2", "SO2")
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

        val barData = BarData(dataSet)
        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setFitBars(true)
        barChart.legend.isEnabled = false

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
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
                        cardChart.visibility = View.GONE
                    }
                } else {
                    _binding?.apply {
                        tvEmptyHistory.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        cardChart.visibility = View.VISIBLE
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

                val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_ENABLED, true)

                if (notificationsEnabled) {
                    NotificationHelper.showDeleteSuccessNotification(requireContext())
                }
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