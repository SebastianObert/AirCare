package com.example.aircare

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aircare.databinding.FragmentSearchLocationBinding
import com.example.aircare.databinding.ItemLocationResultBinding
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException


class SearchLocationFragment : Fragment() {

    private var _binding: FragmentSearchLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var geocoder: Geocoder
    private lateinit var locationsAdapter: LocationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geocoder = Geocoder(requireContext())

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupActions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        locationsAdapter = LocationsAdapter { address ->
            val bundle = Bundle().apply {
                putDouble("latitude", address.latitude)
                putDouble("longitude", address.longitude)
                putString("locationName", address.getAddressLine(0))
            }
            findNavController().previousBackStackEntry?.savedStateHandle?.set("location_data", bundle)
            findNavController().popBackStack()
        }
        binding.rvLocations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = locationsAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchLocation(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    binding.tvStatus.visibility = View.VISIBLE
                    locationsAdapter.submitList(emptyList())
                } else {
                    binding.tvStatus.visibility = View.GONE
                }
                return true
            }
        })
    }

    private fun setupActions() {
        binding.btnCurrentLocation.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("useCurrentLocation", true)
            }
            findNavController().previousBackStackEntry?.savedStateHandle?.set("location_data", bundle)
            findNavController().popBackStack()
        }
    }

    private fun searchLocation(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.GONE

        try {
            val addresses = geocoder.getFromLocationName(query, 5)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    locationsAdapter.submitList(addresses)
                } else {
                    binding.tvStatus.text = "Lokasi tidak ditemukan"
                    binding.tvStatus.visibility = View.VISIBLE
                }
            }
        } catch (e: IOException) {
            binding.tvStatus.text = "Gagal mencari lokasi. Periksa koneksi internet."
            binding.tvStatus.visibility = View.VISIBLE
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class LocationsAdapter(private val onClick: (Address) -> Unit) :
    RecyclerView.Adapter<LocationsAdapter.LocationViewHolder>() {

    private var addresses: List<Address> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount(): Int = addresses.size

    fun submitList(newAddresses: List<Address>) {
        addresses = newAddresses
        notifyDataSetChanged()
    }

    class LocationViewHolder(private val binding: ItemLocationResultBinding, val onClick: (Address) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        private var currentAddress: Address? = null

        init {
            itemView.setOnClickListener {
                currentAddress?.let { onClick(it) }
            }
        }

        fun bind(address: Address) {
            currentAddress = address
            binding.tvLocationName.text = address.getAddressLine(0)
        }
    }
}