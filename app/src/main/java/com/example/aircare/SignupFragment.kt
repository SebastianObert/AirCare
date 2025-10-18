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
        // Firebase Auth mensyaratkan password minimal 6 karakter
        if (password.length < 6) {
            binding.etPassword.error = "Password minimal 6 karakter"
            binding.etPassword.requestFocus()
            return
        }

        // Tampilkan ProgressBar
        binding.progressBar.visibility = View.VISIBLE

        // Proses pembuatan akun baru menggunakan Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                // Sembunyikan ProgressBar setelah proses selesai
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // Jika pendaftaran berhasil, langsung arahkan ke MainActivity
                    Toast.makeText(context, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    // Jika gagal, tampilkan pesan error dari Firebase
                    // Contoh error: email sudah terdaftar, format email salah, dll.
                    Toast.makeText(
                        context,
                        "Pendaftaran Gagal: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun goToMainActivity() {
        // Fungsi ini sama persis dengan yang ada di LoginFragment
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