package com.example.aircare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aircare.databinding.FragmentHistoryBinding
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

    // SIMPAN LISTENER AGAR BISA DI-REMOVE
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

        listenToDatabase()
    }

    private fun listenToDatabase() {
        if (database == null) {
            _binding?.let {
                it.tvEmptyHistory.text = "Anda belum login."
                it.tvEmptyHistory.visibility = View.VISIBLE
                it.rvHistory.visibility = View.GONE
            }
            return
        }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (_binding == null || !isAdded) return  // FRAGMENT SUDAH HANCUR

                val tempList = mutableListOf<HistoryItem>()
                for (data in snapshot.children) {
                    data.getValue(HistoryItem::class.java)?.let { tempList.add(it) }
                }

                if (tempList.isEmpty()) {
                    _binding?.apply {
                        tvEmptyHistory.visibility = View.VISIBLE
                        rvHistory.visibility = View.GONE
                    }
                } else {
                    _binding?.apply {
                        tvEmptyHistory.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        adapter.updateList(tempList.sortedByDescending { it.timestamp })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null || !isAdded) return  // CEGAH NPE
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

        // REMOVE LISTENER UNTUK MENCEGAH CALLBACK SETELAH VIEW HILANG
        if (database != null && valueEventListener != null) {
            database.removeEventListener(valueEventListener!!)
        }

        valueEventListener = null
        _binding = null
    }
}
