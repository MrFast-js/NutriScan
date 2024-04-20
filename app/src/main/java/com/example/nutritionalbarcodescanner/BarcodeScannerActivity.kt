package com.example.nutritionalbarcodescanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nutritionalbarcodescanner.MainActivity.Companion.productInfo
import com.example.nutritionalbarcodescanner.menuPages.HomeFragment
import com.example.nutritionalbarcodescanner.menuPages.ProductInfoFragment
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class BarcodeScannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_scanner)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            startBarcodeScanner()
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    val skipScan = false
    private fun startBarcodeScanner() {
        if (skipScan) {
            fetchProductInfo("0013562000043")
            return
        }
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(true)
        integrator.setBeepEnabled(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scan the product barcode to view information.")
        integrator.initiateScan()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScanner()
            } else {
                Toast.makeText(this, "Camera permission required for scanning", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents != null) {
                if (result.contents != null) {
                    fetchProductInfo(result.contents)
                }
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun fetchProductInfo(productId: String) {
        val barcodeScanner: DecoratedBarcodeView = findViewById(R.id.barcode_scanner)
        barcodeScanner.alpha = 0.0F
        val sharedPreferences = getSharedPreferences("ProductIdFetchCache", MODE_PRIVATE)

        if (sharedPreferences.contains(productId)) {
            val previousJson = sharedPreferences.getString(productId, "")
            val json = JSONObject(previousJson)
            if(json.has("product")) {
                val productInfo = json.getJSONObject("product")
                println("USE CACHE FOR " + productInfo.getString("product_name") + " $productId")

                val intent = Intent().apply {
                    putExtra("product_info", productInfo.toString())
                }
                setResult(Activity.RESULT_OK, intent)
                finish()

                return
            }
        }

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v0/product/$productId.json")
            .build()
        println(
            "SENDING REQ TO: \"https://world.openfoodfacts.org/api/v0/product/$productId.json\""
        )


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val intent = Intent(this@BarcodeScannerActivity, BarcodeScannerActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                println(body)

                try {
                    val jsonObject = JSONObject(body)

                    val editor = sharedPreferences.edit()
                    editor.putString(productId, jsonObject.toString())
                    editor.apply()

                    if (jsonObject.getInt("status") == 1) {
                        productInfo = jsonObject.getJSONObject("product")
                        println("SCANNED " + productInfo!!.getString("product_name"))

                        val intent = Intent().apply {
                            putExtra("product_info", productInfo.toString())
                        }
                        setResult(Activity.RESULT_OK, intent)
                        finish()

                    } else {
                        runOnUiThread {
                            Toast.makeText(this@BarcodeScannerActivity, "Could not find food product", Toast.LENGTH_LONG)
                                .show()
                            val intent = Intent(this@BarcodeScannerActivity, BarcodeScannerActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@BarcodeScannerActivity, "Error processing product information", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@BarcodeScannerActivity, BarcodeScannerActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        })
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}