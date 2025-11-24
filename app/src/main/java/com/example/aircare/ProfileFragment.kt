package com.example.aircare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Referensi Firebase dan Klien Lokasi
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variabel untuk Adapter dan List Lokasi
    private lateinit var savedLocationAdapter: SavedLocationAdapter
    private val savedLocationsList = mutableListOf<SavedLocation>()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Glide.with(this)
                .load(it)
                .circleCrop() // <-- TAMBAHKAN BARIS INI
                .into(binding.ivProfilePicture)
            saveImageUriToPrefs(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            Glide.with(this)
                .load(it)
                .circleCrop() // <-- TAMBAHKAN BARIS INI
                .into(binding.ivProfilePicture)
            saveBitmapToInternalStorage(it)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(null) else Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }
    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) saveCurrentLocationAsHome() else Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_LONG).show()
    }

    // === AKHIR BAGIAN BARU ===

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()
        loadUserData()
        loadSavedLocations()
        loadProfilePicture() // <-- Memuat gambar profil saat fragment dibuat
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        savedLocationAdapter = SavedLocationAdapter(savedLocationsList) { location -> deleteLocation(location) }
        binding.rvSavedLocations.layoutManager = LinearLayoutManager(context)
        binding.rvSavedLocations.adapter = savedLocationAdapter
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: run { goToAuthActivity(); return }
        binding.tvEmail.text = currentUser.email
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null && context != null) {
                    binding.tvName.text = user.name
                    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    binding.tvJoined.text = "Bergabung sejak ${sdf.format(Date(user.memberSince ?: 0))}"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Gagal memuat data profil.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadSavedLocations() {
        val userId = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("saved_locations").child(userId)
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                savedLocationsList.clear()
                snapshot.children.mapNotNullTo(savedLocationsList) { it.getValue(SavedLocation::class.java) }
                savedLocationAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Gagal memuat lokasi tersimpan.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickListeners() {
        binding.ivProfilePicture.setOnClickListener { showPhotoSourceDialog() } // <-- Listener untuk gambar profil
        binding.btnEditProfile.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment) }
        binding.btnSaveHomeLocation.setOnClickListener { saveCurrentLocationAsHome() }
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToAuthActivity()
        }
    }

    // === FUNGSI-FUNGSI BARU UNTUK GAMBAR PROFIL ===
    private fun showPhotoSourceDialog() {
        val options = arrayOf("Ambil Foto dari Kamera", "Pilih dari Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> cameraLauncher.launch(null)
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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

        // Move the 'if' statement to its own line
        if (path != null) {
            Glide.with(this)
                .load(Uri.parse(path))
                .circleCrop()
                .into(binding.ivProfilePicture)
        }
    }



    private fun saveCurrentLocationAsHome() {
        val userId = auth.currentUser?.uid ?: return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("saved_locations").child(userId)
                    val locationId = dbRef.push().key ?: return@addOnSuccessListener
                    val homeLocation = SavedLocation(locationId, "Rumah", location.latitude, location.longitude)
                    dbRef.child(locationId).setValue(homeLocation).addOnSuccessListener {
                        Toast.makeText(context, "Lokasi 'Rumah' berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Gagal mendapatkan lokasi. Aktifkan GPS.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: SecurityException) {
            // Error ditangani oleh pengecekan izin
        }
    }

    private fun deleteLocation(location: SavedLocation) {
        val userId = auth.currentUser?.uid ?: return
        val locationId = location.id ?: return
        FirebaseDatabase.getInstance().getReference("saved_locations").child(userId).child(locationId)
            .removeValue()
            .addOnSuccessListener { Toast.makeText(context, "'${location.name}' berhasil dihapus.", Toast.LENGTH_SHORT).show() }
    }

    private fun goToAuthActivity() {
        if (!isAdded) return
        val intent = Intent(requireActivity(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}