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

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

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

        auth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {
            handleSignup()
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }
    }

    private fun handleSignup() {
        // Ambil data dari EditText
        // PERBAIKAN: Ambil Nama juga jika ada inputnya
        val nameInput = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validasi input
        if (nameInput.isEmpty()) {
            binding.etName.error = "Nama Lengkap harus diisi"
            binding.etName.requestFocus()
            return
        }
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
                // Jangan sembunyikan loading dulu, tunggu save database selesai

                if (task.isSuccessful) {
                    // Pendaftaran Auth berhasil, sekarang simpan data detail ke Database
                    val user = task.result.user
                    if (user != null) {
                        // Kirim nama yang diinput user ke fungsi save
                        saveUserToDatabase(user, nameInput)
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        context,
                        "Pendaftaran Gagal: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // FUNGSI UNTUK MENYIMPAN DATA PENGGUNA
    private fun saveUserToDatabase(firebaseUser: FirebaseUser, nameInput: String) {

        // Buat objek User yang akan disimpan
        // Menggunakan Named Arguments agar aman & tidak tertukar urutannya
        val newUser = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            name = nameInput, // Pakai nama inputan user
            memberSince = System.currentTimeMillis() // INI KUNCINYA (Timestamp saat ini)
        )

        FirebaseDatabase.getInstance().getReference("users")
            .child(firebaseUser.uid)
            .setValue(newUser)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show()
                goToMainActivity()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
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