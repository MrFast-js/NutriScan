package com.example.nutritionalbarcodescanner

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nutritionalbarcodescanner.menuPages.*
import org.json.JSONObject

class MainActivity : AppCompatActivity(), ScanFragment.FragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Auto Load Home
        if (savedInstanceState == null) {
            setFrag(HomeFragment())
        }

        setupFragmentButtonUse()

        val scanButton: LinearLayout = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            setFrag(ScanFragment())
        }
    }

    fun setupFragmentButtonUse() {
        val homeButton: LinearLayout = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {
            setFrag(HomeFragment())
        }

        val exploreButton: LinearLayout = findViewById(R.id.exploreButton)
        exploreButton.setOnClickListener {
            setFrag(ExploreFragment())
        }

        val friendsButton: LinearLayout = findViewById(R.id.friendsButton)
        friendsButton.setOnClickListener {
            setFrag(FriendsFragment())
        }

        val profileButton: LinearLayout = findViewById(R.id.profileButton)
        profileButton.setOnClickListener {
            setFrag(ProfileFragment())
        }
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        setFrag(HomeFragment())
    }

    private var previousFragment: Fragment? = null

    fun setFrag(frag: Fragment) {
        if (frag.javaClass.name == previousFragment?.javaClass?.name) return
        val transaction = supportFragmentManager.beginTransaction()

        // Determine the appropriate animation based on the transition direction
        if (previousFragment != null) {
            val currentIndex = getFragmentIndex(frag)
            val previousIndex = getFragmentIndex(previousFragment!!)
            val animations = if (currentIndex > previousIndex) {
                // Going forward in the fragment array
                Pair(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                // Going backward in the fragment array
                Pair(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            transaction.setCustomAnimations(animations.first, animations.second)
        }

        transaction.replace(R.id.fragment_container, frag)
        transaction.commit()

        // Update the previous fragment
        previousFragment = frag
    }

    private fun getFragmentIndex(fragment: Fragment): Int {
        // Define the order of fragment class types
        val order = listOf(
            HomeFragment::class.java,
            ExploreFragment::class.java,
            ProductInfoFragment::class.java,
            FriendsFragment::class.java,
            ProfileFragment::class.java
        )

        // Find the index of the fragment class type in the order list
        for ((index, fragmentClass) in order.withIndex()) {
            if (fragmentClass.isInstance(fragment)) {
                return index
            }
        }

        // Return -1 if the fragment class type is not found in the order list
        return -1
    }


    companion object {
        var productInfo: JSONObject? = JSONObject()
    }

    override fun openProductInfoFragment(productinfo: JSONObject) {
        val frag = ProductInfoFragment()
        val bundle = Bundle()
        bundle.putString("product_info", productinfo.toString())
        frag.arguments = bundle
        setFrag(frag)
    }
}
