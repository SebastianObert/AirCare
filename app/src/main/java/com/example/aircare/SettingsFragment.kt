package com.example.aircare.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.aircare.AuthActivity
import com.example.aircare.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Define SharedPreferences constants
    private val PREFS_NAME = "AirCarePrefs"
    private val NOTIFICATIONS_ENABLED = "notifications_enabled"

    // Permission launcher
    private val requestPermissionLauncher = 
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Save the setting.
                saveNotificationSetting(true)
                Toast.makeText(requireContext(), "Notifikasi diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                // Permission is denied. Revert the switch and inform the user.
                binding.switchNotifications.isChecked = false
                Toast.makeText(requireContext(), "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
        setupActions()
    }

    private fun loadSettings() {
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_ENABLED, true) // Default to true
        binding.switchNotifications.isChecked = notificationsEnabled
    }

    private fun setupActions() {
        binding.btnPrivacyPolicy.setOnClickListener {
            showPrivacyDialog()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                askForNotificationPermission()
            } else {
                saveNotificationSetting(false)
                Toast.makeText(requireContext(), "Notifikasi dinonaktifkan", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission has already been granted
                    saveNotificationSetting(true)
                    Toast.makeText(requireContext(), "Notifikasi diaktifkan", Toast.LENGTH_SHORT).show()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain to the user why we need this permission
                    AlertDialog.Builder(requireContext())
                        .setTitle("Izin Notifikasi")
                        .setMessage("Aplikasi ini memerlukan izin notifikasi untuk memberitahu Anda saat ada pembaruan penting.")
                        .setPositiveButton("Izinkan") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Jangan Izinkan") { _, _ ->
                            binding.switchNotifications.isChecked = false
                        }
                        .show()
                }
                else -> {
                    // Directly ask for the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For older Android versions, no runtime permission is needed
            saveNotificationSetting(true)
            Toast.makeText(requireContext(), "Notifikasi diaktifkan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveNotificationSetting(isEnabled: Boolean) {
        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(NOTIFICATIONS_ENABLED, isEnabled)
            apply()
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
