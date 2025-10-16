package com.example.aircare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aircare.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // 1. Deklarasikan variabel binding
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Inisialisasi binding
        binding = ActivityMainBinding.inflate(layoutInflater)

        // 3. Set content view menggunakan root dari binding
        setContentView(binding.root)

        binding.tvLocation.text = "Tangerang, Indonesia"
        binding.tvAqiValue.text = "75"
        binding.tvAqiStatus.text = "Sedang"

        binding.btnSave.setOnClickListener {
            binding.tvLocation.text = "Data Tersimpan!"
        }
    }
}