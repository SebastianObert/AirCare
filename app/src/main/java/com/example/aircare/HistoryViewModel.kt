package com.example.aircare

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HistoryViewModel : ViewModel() {

    fun saveNoteForHistoryItem(item: HistoryItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("history")

        val noteUpdates = mapOf<String, Any?>(
            "note" to item.note,
            "category" to item.category
        )

        database.child(userId).child(item.id ?: return).updateChildren(noteUpdates)
    }
}