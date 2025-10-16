package com.example.aircare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        checkLocationPermission()
        setupObservers()
        setupActions()
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
                        binding.tvLocation.text = "Gagal mendapatkan lokasi."
                    }
                }
        } catch (e: SecurityException) {
        }
    }

    private fun setupObservers() {
        mainViewModel.location.observe(viewLifecycleOwner) { binding.tvLocation.text = it }
        mainViewModel.aqiValue.observe(viewLifecycleOwner) { binding.tvAqiValue.text = it }
        mainViewModel.aqiStatus.observe(viewLifecycleOwner) { binding.tvAqiStatus.text = it }
    }

    private fun setupActions() {
        binding.btnSave.setOnClickListener {
            mainViewModel.onSaveButtonClicked()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}