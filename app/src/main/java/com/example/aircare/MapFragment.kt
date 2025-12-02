package com.example.aircare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.aircare.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private val mapViewModel: MapViewModel by viewModels {
        MapViewModelFactory(AirQualityRepository())
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Launcher untuk meminta izin lokasi
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Jika izin diberikan, dapatkan lokasi dan aktifkan layer lokasi
            getMyLocation()
        } else {
            // Jika izin ditolak, beri tahu pengguna
            Toast.makeText(context, "Izin lokasi ditolak. Fitur lokasi saya tidak akan berfungsi.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        setupObservers()

        // Set listener untuk tombol FAB
        binding.fabMyLocation.setOnClickListener {
            checkLocationPermissionAndGetLocation()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map
        googleMap?.uiSettings?.isMyLocationButtonEnabled = false // Sembunyikan tombol default

        // Pindahkan kamera ke Jakarta dengan zoom level 10 sebagai posisi awal
        val jakarta = LatLng(-6.2088, 106.8456)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(jakarta, 10f))

        // Minta ViewModel untuk mengambil data heatmap saat peta siap
        mapViewModel.fetchAirQualityDataForMap()
    }

    private fun checkLocationPermissionAndGetLocation() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Izin sudah ada, langsung dapatkan lokasi
                getMyLocation()
            }
            else -> {
                // Minta izin ke pengguna
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return // Double check, seharusnya tidak pernah terjadi
        }

        googleMap?.isMyLocationEnabled = true // Tampilkan titik biru lokasi pengguna

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                // Animasikan kamera ke lokasi pengguna
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))
            } else {
                Toast.makeText(context, "Gagal mendapatkan lokasi. Pastikan GPS Anda aktif.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        // Amati LiveData yang berisi data kualitas udara untuk heatmap
        mapViewModel.airQualityDataPoints.observe(viewLifecycleOwner) { dataPoints ->
            if (googleMap != null && dataPoints.isNotEmpty()) {
                addHeatmap(dataPoints)
            }
        }
    }

    private fun addHeatmap(dataPoints: List<WeightedLatLng>) {
        val provider = HeatmapTileProvider.Builder()
            .weightedData(dataPoints)
            .radius(40) // Radius bisa dikecilkan sedikit jika titiknya rapat
            .opacity(0.7) // Transparansi
            .build()

        // Bersihkan overlay lama jika ada (opsional, jika Anda refresh map)
        googleMap?.clear()

        googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(provider))
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }
}