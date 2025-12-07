package com.example.aircare.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.aircare.AuthActivity
import com.example.aircare.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
    }

    private fun setupActions() {
        // --- PERUBAHAN ADA DI SINI ---
        // Sekarang tombol ini akan memanggil fungsi showPrivacyDialog()
        binding.btnPrivacyPolicy.setOnClickListener {
            showPrivacyDialog()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "aktif" else "nonaktif"
            Toast.makeText(requireContext(), "Notifikasi $status", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }
    private fun showPrivacyDialog() {
        val isiKebijakan = """
            1. PENGUMPULAN DATA
            Kami mengumpulkan data lokasi dan email Anda hanya untuk keperluan fungsionalitas aplikasi AirCare.
            
            2. PENGGUNAAN DATA
            Data digunakan untuk memberikan informasi kualitas udara yang akurat sesuai lokasi Anda.
            
            3. KEAMANAN
            Kami berkomitmen menjaga keamanan data pribadi Anda dan tidak membagikannya ke pihak ketiga tanpa izin.
            
            4. HAK PENGGUNA
            Anda berhak menghapus akun dan data Anda kapan saja melalui menu pengaturan.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Kebijakan Privasi")
            .setMessage(isiKebijakan)
            .setPositiveButton("Saya Mengerti", null) // Tombol tutup
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Tentang AirCare")
            .setMessage("AirCare v1.0\n\nAplikasi pemantau kualitas udara untuk kesehatan Anda.\n\n© 2025 AirCare Team")
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari akun?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}