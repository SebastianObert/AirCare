package com.example.aircare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Firebase & Location Config
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userRef: DatabaseReference
    private lateinit var locationsRef: DatabaseReference

    // Adapter & Data
    private lateinit var savedLocationAdapter: SavedLocationAdapter
    private val savedLocationsList = mutableListOf<SavedLocation>()

    // --- Listeners (Disimpan sebagai properti agar bersih) ---
    private val userValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (_binding == null) return
            val user = snapshot.getValue(User::class.java)
            if (user != null) {
                binding.tvName.text = user.name
                val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                // Menggunakan format string resource atau text biasa
                binding.tvJoined.text = "Member sejak ${sdf.format(Date(user.memberSince ?: 0))}"
            }
        }
        override fun onCancelled(error: DatabaseError) {
            context?.let { Toast.makeText(it, "Gagal memuat data profil", Toast.LENGTH_SHORT).show() }
        }
    }

    private val locationsValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (_binding == null) return
            savedLocationsList.clear()
            snapshot.children.mapNotNullTo(savedLocationsList) { it.getValue(SavedLocation::class.java) }
            savedLocationAdapter.notifyDataSetChanged()
        }
        override fun onCancelled(error: DatabaseError) {
            context?.let { Toast.makeText(it, "Gagal memuat lokasi", Toast.LENGTH_SHORT).show() }
        }
    }

    // --- Launchers untuk Kamera & Izin ---
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (_binding != null) {
                Glide.with(this).load(it).circleCrop().into(binding.ivProfilePicture)
                saveImageUriToPrefs(it)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            if (_binding != null) {
                Glide.with(this).load(it).circleCrop().into(binding.ivProfilePicture)
                saveBitmapToInternalStorage(it)
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
    }

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) saveCurrentLocationAsHome()
        else Toast.makeText(context, "Izin lokasi diperlukan untuk menyimpan rumah", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Firebase & Auth
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
        // Setup RecyclerView
        savedLocationAdapter = SavedLocationAdapter(savedLocationsList) { location -> deleteLocation(location) }
        binding.rvSavedLocations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = savedLocationAdapter
        }

        // Setup Buttons Click
        binding.ivProfilePicture.setOnClickListener { showPhotoSourceDialog() }
        binding.btnEditProfile.setOnClickListener {
            // Pastikan ID action di nav_graph sesuai
            try {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigasi belum diatur", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnSaveHomeLocation.setOnClickListener { saveCurrentLocationAsHome() }

        // Set email statis dari Auth
        binding.tvEmail.text = auth.currentUser?.email

        loadProfilePicture()
    }

    private fun loadData() {
        userRef.addValueEventListener(userValueEventListener)
        locationsRef.addValueEventListener(locationsValueEventListener)
    }

    // --- Logika Fungsional ---

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Kamera", "Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Ganti Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun saveCurrentLocationAsHome() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val userId = auth.currentUser?.uid ?: return
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val dbRef = FirebaseDatabase.getInstance().getReference("saved_locations").child(userId)
                val locationId = dbRef.push().key ?: return@addOnSuccessListener
                // Menggunakan timestamp sebagai ID unik sederhana jika diperlukan nama custom nanti
                val homeLocation = SavedLocation(locationId, "Rumah (${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())})", location.latitude, location.longitude)

                dbRef.child(locationId).setValue(homeLocation).addOnSuccessListener {
                    if (_binding != null) Toast.makeText(context, "Lokasi Rumah berhasil disimpan!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Lokasi tidak ditemukan. Coba buka Google Maps dulu.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteLocation(location: SavedLocation) {
        val userId = auth.currentUser?.uid ?: return
        val locationId = location.id ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Lokasi")
            .setMessage("Hapus '${location.name}' dari daftar?")
            .setPositiveButton("Hapus") { _, _ ->
                FirebaseDatabase.getInstance().getReference("saved_locations")
                    .child(userId).child(locationId)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Lokasi dihapus", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- Utilitas Gambar ---

    private fun saveImageUriToPrefs(uri: Uri?) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("profile_image_path_${auth.currentUser?.uid}", uri?.toString())
            apply()
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap) {
        val file = File(context?.filesDir, "profile_picture_${auth.currentUser?.uid}.jpg")
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            saveImageUriToPrefs(Uri.fromFile(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadProfilePicture() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val path = sharedPref?.getString("profile_image_path_${auth.currentUser?.uid}", null)
        if (path != null && _binding != null) {
            Glide.with(this).load(Uri.parse(path)).circleCrop().into(binding.ivProfilePicture)
        }
    }

    private fun goToAuthActivity() {
        val intent = Intent(requireActivity(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userRef.removeEventListener(userValueEventListener)
        locationsRef.removeEventListener(locationsValueEventListener)
        _binding = null
    }
}