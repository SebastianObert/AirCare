package com.example.aircare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aircare.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.tvLocation.text = "Tangerang, Indonesia"
        binding.tvAqiValue.text = "75"
        binding.tvAqiStatus.text = "Sedang"

        binding.btnSave.setOnClickListener {
            binding.tvLocation.text = "Data Tersimpan!"
        }
    }
}