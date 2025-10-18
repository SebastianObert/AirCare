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

    private val requestPermissionLauncher =
        registerForActivityResult(
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
        mainViewModel.lastUpdated.observe(viewLifecycleOwner) { binding.tvLastUpdated.text = it }
        mainViewModel.aqiValue.observe(viewLifecycleOwner) { binding.tvAqiValue.text = it }
        mainViewModel.aqiStatus.observe(viewLifecycleOwner) { binding.tvAqiStatus.text = it }

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

        mainViewModel.pm25Value.observe(viewLifecycleOwner) { binding.pollutantPm25.tvPollutantValue.text = it }
        mainViewModel.coValue.observe(viewLifecycleOwner) { binding.pollutantCo.tvPollutantValue.text = it }
        mainViewModel.o3Value.observe(viewLifecycleOwner) { binding.pollutantO3.tvPollutantValue.text = it }
        mainViewModel.no2Value.observe(viewLifecycleOwner) { binding.pollutantNo2.tvPollutantValue.text = it }
        mainViewModel.so2Value.observe(viewLifecycleOwner) { binding.pollutantSo2.tvPollutantValue.text = it }

        mainViewModel.temperature.observe(viewLifecycleOwner) { binding.tvTemperature.text = it }
        mainViewModel.weatherDescription.observe(viewLifecycleOwner) { binding.tvWeatherDescription.text = it }
        mainViewModel.weatherIconUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).into(binding.ivWeatherIcon)
            }
        }

        mainViewModel.saveStatus.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
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

    // Ambil lokasi terakhir dan ubah jadi nama kota
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                val addressText = getAddressFromLocation(latitude, longitude)
                binding.tvLocation.text = addressText

                // Update data AQI dari ViewModel
                mainViewModel.updateLocationAndFetchData(latitude, longitude)
            } else {
                binding.tvLocation.text = "Lokasi tidak ditemukan"
            }
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: ""
                val adminArea = addresses[0].subAdminArea ?: ""
                val country = addresses[0].countryName ?: ""

                when {
                    city.isNotEmpty() -> "$city, $country"
                    adminArea.isNotEmpty() -> "$adminArea, $country"
                    else -> country
                }
            } else {
                "Lokasi tidak diketahui"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Lokasi tidak diketahui"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
