package com.example.aircare

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aircare.databinding.DialogAddNoteBinding
import com.example.aircare.databinding.FragmentHistoryBinding
import com.example.aircare.util.NotificationHelper
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryAdapter
    private val list = mutableListOf<HistoryItem>()
    private val viewModel: HistoryViewModel by viewModels()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database: DatabaseReference?
        get() = userId?.let { FirebaseDatabase.getInstance().getReference("history").child(it) }

    private var valueEventListener: ValueEventListener? = null
    private var currentHeaderColor: Int = Color.parseColor("#4CAF50")

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

        adapter = HistoryAdapter(list, { deleteItem(it) }) { showAddNoteDialog(it) }
        binding.rvHistory.layoutManager = LinearLayoutManager(context)
        binding.rvHistory.adapter = adapter

        setupLineChart(binding.lineChart)

        val marker = CustomMarkerView(requireContext(), R.layout.marker_view)
        binding.lineChart.marker = marker

        listenToDatabase()
    }

    private fun getAqiColor(aqiStatus: String?): Int {
        return when (aqiStatus) {
            "Baik" -> Color.parseColor("#4CAF50")      // Green
            "Sedang" -> Color.parseColor("#4CAF50")    // Green
            "Tidak Sehat" -> Color.parseColor("#FF9800") // Orange
            "Sangat Tidak Sehat" -> Color.parseColor("#F44336") // Red
            "Berbahaya" -> Color.parseColor("#8E24AA")  // Purple
            else -> Color.parseColor("#4CAF50") // Default color green
        }
    }

    private fun updateHeaderColor(newColor: Int) {
        if (binding.viewHeaderBackground.background !is GradientDrawable) return

        val background = binding.viewHeaderBackground.background.mutate() as GradientDrawable

        val colorAnimation = ValueAnimator.ofArgb(currentHeaderColor, newColor)
        colorAnimation.duration = 500 // 0.5 seconds
        colorAnimation.addUpdateListener { animator ->
            background.setColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
        currentHeaderColor = newColor
    }

    private fun showAddNoteDialog(historyItem: HistoryItem) {
        val dialogBinding = DialogAddNoteBinding.inflate(LayoutInflater.from(context))

        dialogBinding.etNote.setText(historyItem.note)
        when (historyItem.category) {
            "Alergi" -> dialogBinding.chipGroupCategory.check(R.id.chipAlergi)
            "Penyakit" -> dialogBinding.chipGroupCategory.check(R.id.chipPenyakit)
            "Lainnya" -> dialogBinding.chipGroupCategory.check(R.id.chipLainnya)
        }

        MaterialAlertDialogBuilder(requireContext())
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
            .show()
    }

    private fun setupLineChart(lineChart: LineChart) {
        lineChart.description.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.legend.isEnabled = false
        lineChart.isHighlightPerTapEnabled = true

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f

        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisRight.isEnabled = false
        lineChart.animateX(1000)
    }

    private fun populateLineChart(historyItems: List<HistoryItem>, chartColor: Int) {
        if (historyItems.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            binding.lineChart.visibility = View.INVISIBLE
            return
        }

        val sortedList = historyItems.sortedBy { it.timestamp }
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        sortedList.forEachIndexed { index, item ->
            val tempString = item.weatherTemp
            val time = item.timestamp
            if (tempString != null && time != null) {
                val numericTempString = Regex("[-+]?[0-9]+([.,]?[0-9]+)?").find(tempString)?.value
                if (numericTempString != null) {
                    val parsableString = numericTempString.replace(',', '.')
                    val tempFloat = parsableString.toFloatOrNull()
                    if (tempFloat != null) {
                        entries.add(Entry(index.toFloat(), tempFloat))
                        labels.add(dateFormat.format(Date(time)))
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            binding.lineChart.visibility = View.INVISIBLE
            return
        }

        binding.lineChart.visibility = View.VISIBLE
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        val dataSet = LineDataSet(entries, "Suhu")
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2.5f
        dataSet.setDrawCircleHole(false)
        dataSet.circleRadius = 5f

        // Dynamic coloring
        dataSet.color = chartColor
        dataSet.setCircleColor(chartColor)

        // Dynamic gradient
        val startColor = Color.argb(150, Color.red(chartColor), Color.green(chartColor), Color.blue(chartColor))
        val gradient = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(startColor, Color.TRANSPARENT))
        dataSet.fillDrawable = gradient
        dataSet.setDrawFilled(true)

        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate()
    }

    private fun listenToDatabase() {
        if (database == null) {
            binding.tvEmptyHistory.text = "Anda belum login."
            binding.tvEmptyHistory.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
            binding.cardChart.visibility = View.GONE
            return
        }

        binding.chartProgressBar.visibility = View.VISIBLE
        binding.lineChart.visibility = View.INVISIBLE

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null || !isAdded) return

                binding.chartProgressBar.visibility = View.GONE
                val tempList = mutableListOf<HistoryItem>()
                snapshot.children.mapNotNullTo(tempList) { it.getValue(HistoryItem::class.java) }

                if (tempList.isEmpty()) {
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                    binding.cardChart.visibility = View.GONE
                } else {
                    binding.tvEmptyHistory.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                    binding.cardChart.visibility = View.VISIBLE
                    binding.lineChart.visibility = View.VISIBLE

                    val sortedList = tempList.sortedByDescending { it.timestamp }
                    adapter.updateList(sortedList)

                    // Update colors based on the latest data
                    sortedList.firstOrNull()?.let {
                        Log.d("HistoryFragment", "Latest AQI Status from DB: ${it.aqiStatus}")
                        val aqiColor = getAqiColor(it.aqiStatus)
                        updateHeaderColor(aqiColor)
                        populateLineChart(tempList, aqiColor)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null || !isAdded) return
                binding.chartProgressBar.visibility = View.GONE
                binding.cardChart.visibility = View.GONE
                binding.tvEmptyHistory.text = "Gagal memuat data: ${error.message}"
                binding.tvEmptyHistory.visibility = View.VISIBLE
            }
        }
        database?.addValueEventListener(valueEventListener!!)
    }

    private fun deleteItem(item: HistoryItem) {
        database?.child(item.id ?: return)?.removeValue()?.addOnSuccessListener {
            Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
            val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (sharedPreferences.getBoolean(NOTIFICATIONS_ENABLED, true)) {
                NotificationHelper.showDeleteSuccessNotification(requireContext())
            }
        }?.addOnFailureListener {
            Toast.makeText(context, "Gagal menghapus data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        valueEventListener?.let { database?.removeEventListener(it) }
        valueEventListener = null
        _binding = null
    }
}
