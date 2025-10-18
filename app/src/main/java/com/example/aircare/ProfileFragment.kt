package com.example.aircare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aircare.databinding.FragmentProfileBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Referensi ke Firebase
    private lateinit var auth: FirebaseAuth

    // Klien untuk mendapatkan lokasi
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variabel untuk Adapter dan List Lokasi
    private lateinit var savedLocationAdapter: SavedLocationAdapter
    private val savedLocationsList = mutableListOf<SavedLocation>()

    // Launcher untuk meminta izin lokasi
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                saveCurrentLocationAsHome()
            } else {
                Toast.makeText(context, "Izin lokasi ditolak.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Memanggil semua fungsi setup
        setupRecyclerView()
        loadUserData()
        loadSavedLocations()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        // Inisialisasi adapter dengan list kosong. Lambda akan menangani klik hapus.
        savedLocationAdapter = SavedLocationAdapter(savedLocationsList) { location ->
            deleteLocation(location)
        }
        binding.rvSavedLocations.layoutManager = LinearLayoutManager(context)
        binding.rvSavedLocations.adapter = savedLocationAdapter
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToAuthActivity()
            return
        }

        binding.tvEmail.text = currentUser.email

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    binding.tvName.text = it.name
                    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    binding.tvJoined.text = "Bergabung sejak ${sdf.format(Date(it.memberSince ?: 0))}"
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

        // addValueEventListener akan terus memantau perubahan data secara real-time
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                savedLocationsList.clear() // Kosongkan list sebelum diisi data baru
                for (data in snapshot.children) {
                    val location = data.getValue(SavedLocation::class.java)
                    if (location != null) {
                        savedLocationsList.add(location)
                    }
                }
                // Beri tahu adapter bahwa data telah berubah agar UI di-refresh
                savedLocationAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Gagal memuat lokasi tersimpan.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSaveHomeLocation.setOnClickListener {
            saveCurrentLocationAsHome()
        }
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToAuthActivity()
        }
    }

    private fun saveCurrentLocationAsHome() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "Anda harus login untuk menyimpan lokasi.", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("saved_locations").child(userId)
                    val locationId = dbRef.push().key ?: return@addOnSuccessListener

                    val homeLocation = SavedLocation(
                        id = locationId,
                        name = "Rumah",
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    dbRef.child(locationId).setValue(homeLocation).addOnSuccessListener {
                        Toast.makeText(context, "Lokasi 'Rumah' berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Gagal mendapatkan lokasi saat ini. Pastikan GPS Anda aktif.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Terjadi error keamanan saat mengakses lokasi.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteLocation(location: SavedLocation) {
        val userId = auth.currentUser?.uid ?: return
        val locationId = location.id
        if (locationId == null) {
            Toast.makeText(context, "ID Lokasi tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseDatabase.getInstance().getReference("saved_locations")
            .child(userId)
            .child(locationId)
            .removeValue() // Perintah untuk menghapus data dari Firebase
            .addOnSuccessListener {
                Toast.makeText(context, "'${location.name}' berhasil dihapus.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal menghapus lokasi.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToAuthActivity() {
        if (activity == null) return
        val intent = Intent(requireActivity(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}