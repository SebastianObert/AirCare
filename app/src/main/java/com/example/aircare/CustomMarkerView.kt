package com.example.aircare

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.DecimalFormat

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)
    private val format = DecimalFormat("#.0")

    // This method is called every time the MarkerView is redrawn
    override fun refreshContent(e: Entry, highlight: Highlight) {
        tvContent.text = "${format.format(e.y)}°C"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Position the marker above the selected point
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}