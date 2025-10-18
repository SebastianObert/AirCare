package com.example.aircare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aircare.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    // Setup ViewBinding
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Deklarasi instance Firebase Authentication
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout menggunakan ViewBinding
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Menambahkan aksi klik pada tombol Login
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }

        // Menambahkan aksi klik pada teks untuk pindah ke halaman Signup
        binding.tvGoToSignup.setOnClickListener {
            // Menggunakan NavController untuk berpindah fragment sesuai action di nav_graph
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

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

        // Tampilkan ProgressBar untuk memberi feedback ke pengguna
        binding.progressBar.visibility = View.VISIBLE

        // Proses login menggunakan Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // Jika login berhasil, panggil fungsi untuk pindah ke MainActivity
                    Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    // Jika login gagal, tampilkan pesan error dari Firebase
                    Toast.makeText(
                        context,
                        "Login Gagal: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
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