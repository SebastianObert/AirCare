package com.example.aircare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aircare.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class SignupFragment : Fragment() {

    // Setup ViewBinding
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    // Deklarasi instance Firebase Authentication
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Menambahkan aksi klik pada tombol Daftar
        binding.btnSignup.setOnClickListener {
            handleSignup()
        }

        // Menambahkan aksi klik pada teks untuk kembali ke halaman Login
        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }
    }

    private fun handleSignup() {
        // Ambil data dari EditText
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validasi input
        if (email.isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password tidak boleh kosong"
            binding.etPassword.requestFocus()
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password minimal 6 karakter"
            binding.etPassword.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // Pendaftaran berhasil, panggil fungsi untuk menyimpan info user ke Realtime Database
                    saveUserToDatabase(task.result.user)

                    Toast.makeText(context, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    Toast.makeText(
                        context,
                        "Pendaftaran Gagal: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // FUNGSI UNTUK MENYIMPAN DATA PENGGUNA
    private fun saveUserToDatabase(firebaseUser: FirebaseUser?) {
        // Pastikan firebaseUser tidak null
        val user = firebaseUser ?: return

        // Ambil bagian nama dari email sebagai nama default
        val nameFromEmail = user.email?.split('@')?.get(0)?.replaceFirstChar { it.titlecase() } ?: "User"

        // Buat objek User yang akan disimpan
        val newUser = User(
            uid = user.uid,
            email = user.email,
            name = nameFromEmail,
            memberSince = System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().getReference("users")
            .child(user.uid)
            .setValue(newUser)
            .addOnFailureListener {
                Toast.makeText(context, "Gagal menyimpan info profil.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToMainActivity() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
