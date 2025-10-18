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
            binding.tvEmptyHistory.text = "Anda belum login."
            binding.tvEmptyHistory.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
            return
        }

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<HistoryItem>()
                for (data in snapshot.children) {
                    val item = data.getValue(HistoryItem::class.java)
                    if (item != null) tempList.add(item)
                }

                if (tempList.isEmpty()) {
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyHistory.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                    adapter.updateList(tempList.sortedByDescending { it.timestamp })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvEmptyHistory.text = "Gagal memuat data: ${error.message}"
                binding.tvEmptyHistory.visibility = View.VISIBLE
            }
        })
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
        _binding = null
    }
}
