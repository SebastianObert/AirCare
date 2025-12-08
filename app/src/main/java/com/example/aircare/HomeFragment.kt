package com.example.aircare

import android.Manifest
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
import java.util.*

class HomeFragment : Fragment() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        } else {
            // Kita bisa handle error state di ViewModel jika mau ditampilkan di UI Compose
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Return ComposeView
        return ComposeView(requireContext()).apply {
            // Strategi agar Compose view didestroy saat Fragment view didestroy
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                // Panggil Composable Screen kita
                // Pastikan tema Material diset (biasanya di MainActivity, tapi aman dipanggil di sini juga)
                MaterialTheme {
                    HomeScreen(
                        viewModel = mainViewModel,
                        onChangeLocationClick = {
                            findNavController().navigate(R.id.action_homeFragment_to_searchLocationFragment)
                        },
                        onSaveClick = {
                            mainViewModel.onSaveButtonClicked()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupObservers() // Untuk Toast/Feedback non-UI

        // Handle result dari SearchLocationFragment (Logika sama persis)
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("location_data")
            ?.observe(viewLifecycleOwner) { result ->
                if (result.getBoolean("useCurrentLocation")) {
                    checkLocationPermission()
                } else {
                    val latitude = result.getDouble("latitude")
                    val longitude = result.getDouble("longitude")
                    val locationName = result.getString("locationName")

                    if (locationName != null) {
                        mainViewModel.updateLocationAndFetchData(latitude, longitude, locationName)
                    }
                }
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>("location_data")
            }

        if (!mainViewModel.isInitialLocationFetched) {
            checkLocationPermission()
        }
    }

    private fun setupObservers() {
        // Kita hanya perlu observe hal-hal yang bersifat "One-shot event" seperti Toast
        // Data UI sudah di-observe langsung di dalam Composable (HomeScreen.kt)

        mainViewModel.saveStatus.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Logika Lokasi tetap sama (tidak ada perubahan)
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
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val addressText = getAddressFromLocation(latitude, longitude)
                    mainViewModel.updateLocationAndFetchData(latitude, longitude, addressText)
                } else {
                   // Handle null location
                }
            }
        } catch (e: SecurityException) {
             // Handle error
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
}
