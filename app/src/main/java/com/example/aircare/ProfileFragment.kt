package com.example.aircare.ui.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.aircare.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        // Contoh data statis user
        binding.tvName.text = "Jevon Chan Ho Kim"
        binding.tvEmail.text = "jevon.chan@example.com"
        binding.tvRole.text = "Student & Developer"
        binding.tvJoined.text = "Bergabung sejak 2024"

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
