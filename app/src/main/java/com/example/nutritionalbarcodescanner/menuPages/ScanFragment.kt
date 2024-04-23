package com.example.nutritionalbarcodescanner.menuPages

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.nutritionalbarcodescanner.R
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {
    interface FragmentInteractionListener {
        fun openProductInfoFragment(productId: JSONObject) {

        }
    }

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var previewView: PreviewView
    private lateinit var listener: FragmentInteractionListener
    private lateinit var viewReference: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (requireActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requireActivity().requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                1001
            )
        }
        viewReference = inflater.inflate(R.layout.fragment_scan, container, false)
        previewView = viewReference.findViewById(R.id.previewView)
        listener = activity as FragmentInteractionListener
        return viewReference
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(listener, requireActivity()))
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // BarcodeAnalyzer class for analyzing images and detecting barcodes
    private class BarcodeAnalyzer(
        private val listener: FragmentInteractionListener,
        context: FragmentActivity
    ) : ImageAnalysis.Analyzer, Fragment() {
        var lastScannedBarcode = ""

        private val scanner: BarcodeScanner = BarcodeScanning.getClient()

        @RequiresApi(Build.VERSION_CODES.S)
        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val firstCode = barcodes[0]
                            if (lastScannedBarcode != firstCode.rawValue.toString()) {
                                println("SCANNED BARCODE: " + firstCode.rawValue)
                                lastScannedBarcode = firstCode.rawValue.toString()

                                lifecycleScope.launch {
                                    try {
                                        val productJson = fetchProductInfo(lastScannedBarcode)

                                        if (productJson != null) {
                                            listener.openProductInfoFragment(JSONObject(productJson))
                                        }
                                    } catch (_: Exception) {

                                    }
                                }
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { exception ->
                        // Handle failure
                        exception.printStackTrace()
                        // Continue analyzing frames
                        imageProxy.close()
                    }
                    .addOnCompleteListener {

                    }
            }
        }

        val sharedPreferences = context.getSharedPreferences("ProductIdFetchCache", AppCompatActivity.MODE_PRIVATE)
        suspend fun fetchProductInfo(productId: String): String? {
            if (sharedPreferences.contains(productId)) {
                println("USING CACHE")
                return sharedPreferences.getString(productId, "")
            }

            return withContext(Dispatchers.IO) {
                try {
                    // Coroutine with a timeout of 10 seconds
                    withTimeout(10_000) {
                        // Your existing fetchProductInfo function implementation
                        val client = OkHttpClient()
                        val request = Request.Builder()
                            .url("https://world.openfoodfacts.org/api/v0/product/$productId.json")
                            .build()

                        println("SENDING REQUEST TO ${request.url}")

                        val response = client.newCall(request).execute()
                        val body = response.body?.string()

                        if (response.isSuccessful) {
                            val jsonObject = JSONObject(body)

                            if (jsonObject.getInt("status") == 1) {
                                // Cache the product info
                                val editor = sharedPreferences.edit()
                                val bigJson = jsonObject.getJSONObject("product")
                                val filteredJson = JSONObject()
                                filteredJson.put("_id",bigJson.getString("_id"))
                                filteredJson.put("_keywords",bigJson.getJSONArray("_keywords"))
                                filteredJson.put("image_url",bigJson.getString("image_url"))
                                filteredJson.put("nova_groups_tags",bigJson.getJSONArray("nova_groups_tags"))
                                filteredJson.put("nutriments",bigJson.getJSONObject("nutriments"))
                                filteredJson.put("categories_hierarchy",bigJson.getJSONArray("categories_hierarchy"))
                                filteredJson.put("nutriscore_score",bigJson.getInt("nutriscore_score"))
                                filteredJson.put("allergens_tags",bigJson.getJSONArray("allergens_tags"))
                                filteredJson.put("product_name",bigJson.getString("product_name"))
                                filteredJson.put("ingredients",bigJson.getJSONArray("ingredients"))
                                filteredJson.put("additives_n",bigJson.getDouble("additives_n"))
                                filteredJson.put("scanned_at",System.currentTimeMillis())
                                editor.putString(productId, filteredJson.toString())
                                editor.apply()

                                // Parse and return product info
                                jsonObject.getJSONObject("product").toString()
                            } else {
                                throw IOException("Could not find food product")
                            }
                        } else {
                            throw IOException("Error processing product information")
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    throw IOException("Timeout: Unable to fetch product information within 10 seconds")
                }
            }
        }
    }
}
