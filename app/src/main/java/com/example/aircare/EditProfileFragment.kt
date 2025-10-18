package com.example.aircare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aircare.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        loadCurrentUserName()
        setupClickListener()
    }

    private fun loadCurrentUserName() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.child("name").get().addOnSuccessListener { dataSnapshot ->
            val currentName = dataSnapshot.getValue(String::class.java)
            binding.etName.setText(currentName)
        }
    }

    private fun setupClickListener() {
        binding.btnSaveChanges.setOnClickListener {
            val newName = binding.etName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateUserNameInDatabase(newName)
        }
    }

    private fun updateUserNameInDatabase(newName: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.child("name").setValue(newName)
            .addOnSuccessListener {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Nama berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener {
                if (isAdded && context != null) {
                    Toast.makeText(context, "Gagal memperbarui nama.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}