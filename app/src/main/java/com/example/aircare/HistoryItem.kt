package com.example.aircare

data class HistoryItem(
    val id: String? = null,
    val location: String? = null,
    val aqiValue: String? = null,
    val aqiStatus: String? = null,
    val timestamp: Long? = null,
    val statusColor: Int = R.drawable.status_bg_yellow 
)