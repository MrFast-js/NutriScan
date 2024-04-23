package com.example.nutritionalbarcodescanner.menuPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.nutritionalbarcodescanner.MainActivity
import com.example.nutritionalbarcodescanner.R
import org.json.JSONObject

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val recentScannedProducts =
            requireActivity().getSharedPreferences("recentScannedProducts", AppCompatActivity.MODE_PRIVATE)
        val productCache = requireActivity().getSharedPreferences("ProductIdFetchCache", AppCompatActivity.MODE_PRIVATE)

        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        val sorted = recentScannedProducts.all.entries.toList()
        sorted.sortedBy {
            val matches = productCache.all.toMap().filter { it2 -> it2.value.toString().contains(it.key) }

            val product = matches.values.first()
            JSONObject(product as String).getLong("scanned_at")
        }

        for (recentProduct in sorted) {
            val matches = productCache.all.toMap().filter { it.value.toString().contains(recentProduct.key) }

            if (matches.isNotEmpty()) {
                val product = matches.values.first()
                // Inflate the new CardView layout
                val newCardViewLayout = inflater.inflate(R.layout.recent_scan_entry_layout, null, false)

                // Find ImageView in the new CardView layout
                val recentScanProductImage = newCardViewLayout.findViewById<ImageView>(R.id.productImage)

                // Load image into ImageView using Glide
                Glide.with(this@HomeFragment).load(recentProduct.value).into(recentScanProductImage)

                // Find the LinearLayout to add the new CardView
                val recentScansContainer = rootView.findViewById<LinearLayout>(R.id.recentScansContainer)

                newCardViewLayout.setOnClickListener {
                    (requireActivity() as MainActivity).openProductInfoFragment(JSONObject(product as String))
                }

                // Add the new CardView to the LinearLayout
                recentScansContainer.addView(newCardViewLayout)
                break
            }
        }

        return rootView
    }
}
