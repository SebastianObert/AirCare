package com.example.aircare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.aircare.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

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
        mainViewModel.location.observe(viewLifecycleOwner) { binding.tvLocation.text = it }
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
    }

    private fun setupActions() {
        binding.btnSave.setOnClickListener {
            mainViewModel.onSaveButtonClicked()
        }
    }

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
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        mainViewModel.updateLocationAndFetchData(location.latitude, location.longitude)
                    } else {
                        binding.tvLocation.text = "Gagal mendapatkan lokasi. Aktifkan GPS."
                    }
                }
        } catch (e: SecurityException) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}