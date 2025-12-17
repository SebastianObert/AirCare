package com.example.aircare

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.aircare.databinding.ActivityMainBinding
import androidx.navigation.ui.NavigationUI
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var predictionHelper: PredictionHelper
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        setupCustomBottomNav(navController)

        // Inisialisasi PredictionHelper
        predictionHelper = PredictionHelper(this)

        // Amati perubahan data dari ViewModel
        observeViewModel()
    }

    private fun observeViewModel() {
        // Kita butuh pm2.5, suhu, dan kelembaban untuk prediksi
        viewModel.pm25Value.observe(this) { tryToMakePrediction() }
        viewModel.temperature.observe(this) { tryToMakePrediction() }
        // Karena kelembaban tidak ada di MainViewModel, kita akan amati dari sini juga
        // Jika Anda menambahkan kelembaban ke MainViewModel, hapus observer ini
        viewModel.forecastData.observe(this) { tryToMakePrediction() }
    }

    // UBAH DARI PRIVATE KE PUBLIC agar bisa dipanggil dari Fragment
    fun tryToMakePrediction() {
        // 1. Ambil data mentah dari ViewModel
        val rawPm25 = viewModel.pm25Value.value
        val rawTemp = viewModel.temperature.value
        // Ambil humidity dari forecast pertama, jaga-jaga kalau list masih kosong
        val rawHumidity = viewModel.forecastData.value?.firstOrNull()?.humidity

        // 2. Debugging: Lihat apa isi datanya di Logcat
        Log.d("PREDIKSI", "Raw Data -> PM2.5: $rawPm25, Temp: $rawTemp, Hum: $rawHumidity")

        // 3. Konversi Data (Parsing) dengan penanganan error lebih baik
        // Hapus semua teks non-angka, ganti koma dengan titik jika ada
        val pm25 = rawPm25?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull() ?: 0f
        
        // Suhu kadang formatnya "28°C" atau "28.5°C"
        val temp = rawTemp?.replace("°C", "")?.trim()?.toFloatOrNull() ?: 0f

        // Humidity kadang "80%" -> jadi 80.0
        val humidity = rawHumidity?.replace("%", "")?.trim()?.toFloatOrNull() ?: 0f

        // 4. Cek Validitas Data
        if (pm25 <= 0f) {
            showSnackbar("Data Kualitas Udara (PM2.5) belum dimuat. Tunggu sebentar...")
            return
        }
        if (temp <= 0f) {
            showSnackbar("Data Suhu belum dimuat. Pastikan GPS/Internet aktif.")
            return
        }
        if (humidity <= 0f) {
            showSnackbar("Data Kelembaban belum dimuat. Tunggu update cuaca.")
            return
        }

        // 5. Jika semua data > 0, Lakukan Prediksi
        val prediction = predictionHelper.predict(pm25, temp, humidity)
        
        if (prediction != null) {
            showErrorDialog(prediction) // Tampilkan hasil (bisa pakai Dialog atau Snackbar)
        } else {
            showSnackbar("Kondisi Udara Aman. Tidak ada risiko kesehatan signifikan.")
        }
    }

    // Fungsi helper untuk menampilkan pesan
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
            .setTextColor(resources.getColor(android.R.color.white, null))
            .show()
    }

    // Opsional: Tampilkan hasil prediksi pakai Dialog biar lebih jelas
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Hasil Analisis AI")
            .setMessage(message)
            .setIcon(R.drawable.ic_warning) // Pastikan icon ada, atau hapus baris ini
            .setPositiveButton("Mengerti") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    private fun setupCustomBottomNav(navController: NavController) {
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.setOnItemSelectedListener {
            item ->
            val builder = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(R.anim.fade_in)
                .setExitAnim(R.anim.fade_out)
                .setPopEnterAnim(R.anim.fade_in)
                .setPopExitAnim(R.anim.fade_out)

            val options = builder.build()
            try {
                NavigationUI.onNavDestinationSelected(item, navController)
                navController.navigate(item.itemId, null, options)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        predictionHelper.close() // Jangan lupa menutup interpreter
    }
}