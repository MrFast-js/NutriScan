package com.example.nutritionalbarcodescanner.menuPages

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.nutritionalbarcodescanner.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class ExploreFragment : Fragment() {
    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val MAP_BASE_URL = "https://maps.google.com/maps?q="
    private var latitude: Double? = null
    private var longitude: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize WebView
        webView = view.findViewById(R.id.webView)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Set up WebViewClient to prevent opening links in external apps
        webView.webViewClient = MyWebViewClient()

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (requireActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permReqLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
        getLocation()
    }
    private val permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }
            if (granted) {
                getLocation()
            }
        }

    private fun getLocation() {
        // Check if permissions are granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Get last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    // Use location.latitude and location.longitude to get latitude and longitude
                    latitude = location.latitude
                    longitude = location.longitude
                    // Load the Google Maps URL with location
                    loadMapUrl()
                } else {
                    // Location is null, handle accordingly
                    showErrorToast("Unable to retrieve location")
                }
            }
    }

    private fun loadMapUrl() {
        // Construct the URL with location parameters
        val mapUrl = "$MAP_BASE_URL$latitude,$longitude&amp;t=&amp;z=13&amp;ie=UTF8&amp;iwloc=&amp;output=embe"
        // Load the Google Maps URL
        webView.loadUrl(mapUrl)
    }

    // WebViewClient to handle URL loading
    private class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            // Return false to allow loading the URL in the WebView
            return false
        }
    }

    private fun showErrorToast(message: String) {
        // Show toast message for errors
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}