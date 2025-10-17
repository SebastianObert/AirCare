package com.example.aircare

data class HistoryItem(
    val date: String,
    val location: String,
    val aqiValue: String,
    val aqiStatus: String,
    val statusColor: Int
)