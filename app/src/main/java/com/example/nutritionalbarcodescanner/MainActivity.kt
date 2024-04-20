package com.example.nutritionalbarcodescanner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.nutritionalbarcodescanner.menuPages.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val productInfoReturnListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val returnedData = result.data?.getStringExtra("product_info")

                val frag = ProductInfoFragment()
                val bundle = Bundle()
                bundle.putString("product_info", returnedData)
                println("BUNDLING DATA: $returnedData")
                frag.arguments = bundle

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .commit()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Auto Load Home
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        setupFragmentButtonUse()

        val scanButton: LinearLayout = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            productInfoReturnListener.launch(intent)
        }
    }

    fun setupFragmentButtonUse() {
        val homeButton: LinearLayout = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        val exploreButton: LinearLayout = findViewById(R.id.exploreButton)
        exploreButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExploreFragment())
                .commit()
        }

//        val scanButton: LinearLayout = findViewById(R.id.scanButton)
//        homeButton.setOnClickListener {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, HomeFragment())
//                .commit()
//        }

        val friendsButton: LinearLayout = findViewById(R.id.friendsButton)
        friendsButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FriendsFragment())
                .commit()
        }

        val profileButton: LinearLayout = findViewById(R.id.profileButton)
        profileButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
        }
    }

    companion object {
        var productInfo: JSONObject? = JSONObject()


    }
}
