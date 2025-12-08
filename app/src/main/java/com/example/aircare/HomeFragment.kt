package com.example.aircare

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.*

class HomeFragment : Fragment() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    // PANGGILAN HOMESCREEN DIPERBAIKI DI SINI
                    HomeScreen(
                        viewModel = mainViewModel,

                        onChangeLocationClick = {
                            findNavController().navigate(R.id.action_homeFragment_to_searchLocationFragment)
                        },

                        onSaveClick = {
                            mainViewModel.onSaveButtonClicked()
                        },

                        // --- PARAMETER YANG HILANG (PENYEBAB ERROR) ---
                        onRefreshLocationClick = {
                            Toast.makeText(context, "Mencari lokasi terkini...", Toast.LENGTH_SHORT).show()
                            checkLocationPermission()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupObservers()

        val args = arguments
        if (args != null && args.getBoolean("isFromProfile", false)) {
            val lat = args.getDouble("targetLat")
            val lon = args.getDouble("targetLon")
            val name = args.getString("targetName") ?: "Lokasi Tersimpan"

            mainViewModel.updateLocationAndFetchData(lat, lon, name)
            arguments?.clear()

        } else {
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("location_data")
                ?.observe(viewLifecycleOwner) { result ->
                    if (result.getBoolean("useCurrentLocation")) {
                        checkLocationPermission()
                    } else {
                        val lat = result.getDouble("latitude")
                        val lon = result.getDouble("longitude")
                        val name = result.getString("locationName") ?: "Lokasi Terpilih"
                        mainViewModel.updateLocationAndFetchData(lat, lon, name)
                    }
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>("location_data")
                }

            if (!mainViewModel.isInitialLocationFetched) {
                checkLocationPermission()
            }
        }
    }

    private fun setupObservers() {
        mainViewModel.saveStatus.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude
                        val addressText = getAddressFromLocation(lat, lon)
                        mainViewModel.updateLocationAndFetchData(lat, lon, addressText)
                    } else {
                        Toast.makeText(context, "Gagal dapat lokasi. Pastikan GPS aktif.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error lokasi: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Error izin: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAddressFromLocation(lat: Double, lon: Double): String {
        if (context == null) return "Lokasi tidak diketahui"
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare
                val district = address.subLocality
                val city = address.locality
                when {
                    !street.isNullOrEmpty() -> street
                    !district.isNullOrEmpty() -> district
                    !city.isNullOrEmpty() -> city
                    else -> "Lokasi Terkini"
                }
            } else {
                "Lokasi Terkini"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Lokasi Terkini"
        }
    }
}