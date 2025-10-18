package com.example.aircare.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.content.Intent
import com.example.aircare.databinding.FragmentSettingsBinding
import com.example.aircare.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.btnPrivacyPolicy.setOnClickListener {
            Toast.makeText(requireContext(), "Menampilkan Kebijakan Privasi", Toast.LENGTH_SHORT).show()
        }

        binding.btnAbout.setOnClickListener {
            Toast.makeText(requireContext(), "AirCare v1.0 © 2025", Toast.LENGTH_SHORT).show()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "aktif" else "nonaktif"
            Toast.makeText(requireContext(), "Notifikasi $status", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
