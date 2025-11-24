package com.example.aircare

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.aircare.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        } else {
            binding.tvLocation.text = "Izin lokasi ditolak"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupStaticViews()
        setupObservers()
        setupActions()

        checkLocationPermission()
    }

    private fun setupStaticViews() {
        binding.pollutantPm25.tvPollutantName.text = "PM2.5"
        binding.pollutantCo.tvPollutantName.text = "CO"
        binding.pollutantO3.tvPollutantName.text = "Ozon (O₃)"
        binding.pollutantNo2.tvPollutantName.text = "NO₂"
        binding.pollutantSo2.tvPollutantName.text = "SO₂"

        binding.guideGood.apply {
            viewGuideColor.setBackgroundResource(R.drawable.status_bg_green)
            tvGuideLevel.text = "Baik"
            tvGuideRange.text = "0-50"
        }
        binding.guideModerate.apply {
            viewGuideColor.setBackgroundResource(R.drawable.status_bg_yellow)
            tvGuideLevel.text = "Cukup"
            tvGuideRange.text = "51-100"
        }
        binding.guideUnhealthySensitive.apply {
            viewGuideColor.setBackgroundResource(R.drawable.status_bg_orange)
            tvGuideLevel.text = "Sedang"
            tvGuideRange.text = "101-150"
        }
        binding.guideUnhealthy.apply {
            viewGuideColor.setBackgroundResource(R.drawable.status_bg_red)
            tvGuideLevel.text = "Buruk"
            tvGuideRange.text = "151-200"
        }
        binding.guideVeryUnhealthy.apply {
            viewGuideColor.setBackgroundResource(R.drawable.status_bg_maroon)
            tvGuideLevel.text = "Sangat Buruk"
            tvGuideRange.text = "201+"
        }
    }

    private fun setupObservers() {
        // --- Observer untuk data utama
        mainViewModel.location.observe(viewLifecycleOwner) { binding.tvLocation.text = it }
        mainViewModel.lastUpdated.observe(viewLifecycleOwner) { binding.tvLastUpdated.text = it }
        mainViewModel.aqiValue.observe(viewLifecycleOwner) { binding.tvAqiValue.text = it }
        mainViewModel.aqiStatus.observe(viewLifecycleOwner) { binding.tvAqiStatus.text = it }

        // --- Observer untuk UI dinamis ---
        mainViewModel.aqiStatusBackground.observe(viewLifecycleOwner) { drawableId ->
            if (drawableId != null && drawableId != 0) {
                binding.tvAqiStatus.setBackgroundResource(drawableId)
            }
        }
        mainViewModel.recommendationIcon.observe(viewLifecycleOwner) { iconId ->
            if (iconId != null && iconId != 0) {
                binding.ivRecommendationIcon.setImageResource(iconId)
            }
        }
        mainViewModel.recommendationText.observe(viewLifecycleOwner) { text ->
            binding.tvRecommendationText.text = text
        }
        mainViewModel.aqiIndicatorPosition.observe(viewLifecycleOwner) { position ->
            val params = binding.guidelineIndicator.layoutParams as ConstraintLayout.LayoutParams
            params.guidePercent = position
            binding.guidelineIndicator.layoutParams = params
        }

        // --- Observer untuk detail polutan ---
        mainViewModel.pm25Value.observe(viewLifecycleOwner) { binding.pollutantPm25.tvPollutantValue.text = it }
        mainViewModel.coValue.observe(viewLifecycleOwner) { binding.pollutantCo.tvPollutantValue.text = it }
        mainViewModel.o3Value.observe(viewLifecycleOwner) { binding.pollutantO3.tvPollutantValue.text = it }
        mainViewModel.no2Value.observe(viewLifecycleOwner) { binding.pollutantNo2.tvPollutantValue.text = it }
        mainViewModel.so2Value.observe(viewLifecycleOwner) { binding.pollutantSo2.tvPollutantValue.text = it }

        // --- Observer untuk data cuaca ---
        mainViewModel.temperature.observe(viewLifecycleOwner) { binding.tvTemperature.text = it }
        mainViewModel.weatherDescription.observe(viewLifecycleOwner) { binding.tvWeatherDescription.text = it }
        mainViewModel.weatherIconUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).into(binding.ivWeatherIcon)
            }
        }

        // --- Observer untuk feedback simpan data ---
        mainViewModel.saveStatus.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        // Observer ini mengontrol status aktif/nonaktif dari tombol Simpan.
        mainViewModel.isDataReadyToSave.observe(viewLifecycleOwner) { isReady ->
            binding.btnSave.isEnabled = isReady

            // Memberikan feedback visual. Tombol akan terlihat pudar saat tidak bisa diklik.
            binding.btnSave.alpha = if (isReady) 1.0f else 0.5f
        }
    }

    private fun setupActions() {
        binding.btnSave.setOnClickListener {
            mainViewModel.onSaveButtonClicked()
        }
    }

    //  Periksa izin lokasi
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // 1. Dapatkan koordinat
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // 2. Ubah koordinat menjadi nama alamat
                    val addressText = getAddressFromLocation(latitude, longitude)

                    // 3. Kirim SEMUA data (koordinat DAN nama alamat) ke ViewModel
                    mainViewModel.updateLocationAndFetchData(latitude, longitude, addressText)
                } else {
                    binding.tvLocation.text = "Gagal mendapatkan lokasi. Aktifkan GPS."
                }
            }
        } catch (e: SecurityException) {
            binding.tvLocation.text = "Error keamanan saat akses lokasi."
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        if (context == null) return "Lokasi tidak diketahui"

        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality
                val adminArea = address.subAdminArea
                val country = address.countryName

                when {
                    !city.isNullOrEmpty() -> "$city, $country"
                    !adminArea.isNullOrEmpty() -> "$adminArea, $country"
                    else -> country ?: "Lokasi tidak diketahui"
                }
            } else {
                "Lokasi tidak diketahui"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Gagal menerjemahkan lokasi"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
