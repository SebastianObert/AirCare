package com.example.aircare.data

import com.google.firebase.database.PropertyName

data class Notification(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("message") @set:PropertyName("message") var message: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = 0
)
