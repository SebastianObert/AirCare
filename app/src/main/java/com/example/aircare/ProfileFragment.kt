package com.example.aircare

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.aircare.databinding.FragmentProfileBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Firebase & Services
    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference
    private lateinit var locationsRef: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Adapter & Data
    private lateinit var savedLocationAdapter: SavedLocationAdapter
    private val savedLocationsList = mutableListOf<SavedLocation>()

    // Listener Data Profil
    private val userValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (_binding == null) return
            val user = snapshot.getValue(User::class.java)
            if (user != null) {
                binding.tvName.text = user.name
                val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                binding.tvJoined.text = "Member sejak ${sdf.format(Date(user.memberSince ?: 0))}"
            }
        }
        override fun onCancelled(error: DatabaseError) {
            context?.let { Toast.makeText(it, "Gagal memuat profil", Toast.LENGTH_SHORT).show() }
        }
    }

    // Listener Riwayat Lokasi
    private val locationsValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (_binding == null) return
            savedLocationsList.clear()
            snapshot.children.mapNotNullTo(savedLocationsList) { it.getValue(SavedLocation::class.java) }
            savedLocationAdapter.notifyDataSetChanged()
        }
        override fun onCancelled(error: DatabaseError) { }
    }

    // Permission Launcher untuk tombol "Set Rumah"
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) setHomeLocation() else Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
    }

    // Gallery & Camera Launchers
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { if (_binding != null) { Glide.with(this).load(it).circleCrop().into(binding.ivProfilePicture); saveImageUriToPrefs(it) } }
    }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { if (_binding != null) { Glide.with(this).load(it).circleCrop().into(binding.ivProfilePicture); saveBitmapToInternalStorage(it) } }
    }
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId == null) {
            goToAuthActivity()
            return
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        locationsRef = FirebaseDatabase.getInstance().getReference("saved_locations").child(userId)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupUI()
        loadData()
    }

    private fun setupUI() {
        // Setup RecyclerView (Klik item -> Pindah ke Home)
        savedLocationAdapter = SavedLocationAdapter(
            locations = savedLocationsList,
            onItemClick = { location -> navigateToHomeWithLocation(location) },
            onDeleteClick = { location -> deleteLocation(location) }
        )

        binding.rvSavedLocations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = savedLocationAdapter
        }

        // Tombol Profil
        binding.ivProfilePicture.setOnClickListener { showPhotoSourceDialog() }
        binding.btnEditProfile.setOnClickListener {
            try { findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment) }
            catch (e: Exception) { Toast.makeText(context, "Navigasi Error", Toast.LENGTH_SHORT).show() }
        }

        // Tombol Set Rumah
        binding.btnSetHome.setOnClickListener {
            checkLocationAndSetHome()
        }

        binding.tvEmail.text = auth.currentUser?.email
        loadProfilePicture()
    }

    // --- FITUR SET LOKASI RUMAH ---
    private fun checkLocationAndSetHome() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setHomeLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setHomeLocation() {
        Toast.makeText(context, "Mencari lokasi rumah...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    // 1. Dapatkan Alamat Asli
                    val addressText = getAddressName(location.latitude, location.longitude)

                    // 2. Simpan sebagai "Rumah (Alamat)"
                    saveLocationToFirebase("Rumah ($addressText)", location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "Gagal dapat lokasi. Pastikan GPS nyala.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error lokasi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getAddressName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lon, 1)
            if (!list.isNullOrEmpty()) {
                val addr = list[0]
                addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: "Lokasi Terkini"
            } else "Lokasi Terkini"
        } catch (e: Exception) {
            "Lokasi Terkini"
        }
    }

    private fun saveLocationToFirebase(name: String, lat: Double, lon: Double) {
        val key = locationsRef.push().key ?: return
        val loc = SavedLocation(key, name, lat, lon)
        locationsRef.child(key).setValue(loc)
            .addOnSuccessListener {
                Toast.makeText(context, "Lokasi Rumah berhasil diupdate!", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Navigasi ke Home saat lokasi diklik ---
    private fun navigateToHomeWithLocation(location: SavedLocation) {
        val bundle = Bundle().apply {
            putBoolean("isFromProfile", true)
            putDouble("targetLat", location.lat ?: 0.0)
            putDouble("targetLon", location.lon ?: 0.0)
            putString("targetName", location.name)
        }
        try {
            findNavController().navigate(R.id.homeFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(context, "Navigasi ke Home...", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Utilitas ---
    private fun loadData() {
        userRef.addValueEventListener(userValueEventListener)
        locationsRef.addValueEventListener(locationsValueEventListener)
    }

    private fun deleteLocation(location: SavedLocation) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus")
            .setMessage("Hapus '${location.name}'?")
            .setPositiveButton("Ya") { _, _ -> locationsRef.child(location.id!!).removeValue() }
            .setNegativeButton("Batal", null).show()
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Kamera", "Galeri")
        AlertDialog.Builder(requireContext()).setItems(options) { _, w ->
            if (w == 0) { if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            else galleryLauncher.launch("image/*")
        }.show()
    }

    private fun saveImageUriToPrefs(uri: Uri?) {
        activity?.getPreferences(Context.MODE_PRIVATE)?.edit()?.putString("profile_image_path_${auth.currentUser?.uid}", uri?.toString())?.apply()
    }
    private fun saveBitmapToInternalStorage(bitmap: Bitmap) {
        try {
            val file = File(context?.filesDir, "profile_${auth.currentUser?.uid}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            saveImageUriToPrefs(Uri.fromFile(file))
        } catch (e: Exception) { e.printStackTrace() }
    }
    private fun loadProfilePicture() {
        val path = activity?.getPreferences(Context.MODE_PRIVATE)?.getString("profile_image_path_${auth.currentUser?.uid}", null)
        if (path != null) Glide.with(this).load(Uri.parse(path)).circleCrop().into(binding.ivProfilePicture)
    }
    private fun goToAuthActivity() {
        startActivity(Intent(requireActivity(), AuthActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userRef.removeEventListener(userValueEventListener)
        locationsRef.removeEventListener(locationsValueEventListener)
        _binding = null
    }
}