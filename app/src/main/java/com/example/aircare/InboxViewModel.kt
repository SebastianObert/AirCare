package com.example.aircare

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aircare.data.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class InboxViewModel : ViewModel() {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var databaseReference: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    init {
        fetchNotifications()
    }

    private fun fetchNotifications() {
        _isLoading.value = true
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("notifications").child(currentUser.uid)

            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notificationList = mutableListOf<Notification>()
                    for (childSnapshot in snapshot.children) {
                        val notification = childSnapshot.getValue(Notification::class.java)
                        if (notification != null) {
                            notificationList.add(notification)
                        }
                    }
                    _notifications.value = notificationList.sortedByDescending { it.timestamp }
                    _isLoading.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    _isLoading.value = false
                    // Handle error
                }
            }
            databaseReference?.addValueEventListener(valueEventListener!!)
        } else {
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        valueEventListener?.let {
            databaseReference?.removeEventListener(it)
        }
    }
}
