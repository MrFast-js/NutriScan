package com.example.nutritionalbarcodescanner.menuPages

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nutritionalbarcodescanner.R
import kotlinx.coroutines.withContext
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.nutritionalbarcodescanner.CircularProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import kotlin.math.max

class ProductInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_product_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val json = JSONObject(requireArguments().getString("product_info"))
        setProductImage(json)

        val productHealthScore = getNutritionScore(json) // @TODO hook up to get user match score
        val progressBar = view.findViewById<CircularProgressBar>(R.id.circularProgressBar)
        progressBar.setProgress(productHealthScore)

        doCountingAnimation(productHealthScore.toInt())

        val productTitle = view.findViewById<TextView>(R.id.textView)
        productTitle.text = json.getString("product_name")

        if (json.has("brands")) {
            val productDescription = view.findViewById<TextView>(R.id.productDescriptionTextView)
            productDescription.text = json.getString("brands")
        }
        displayWarnings(json)
        displayStats(json)
    }

    private fun displayWarnings(product: JSONObject) {
        val brokenDiets = doesProductBreakDiet(product)
        for ((dietName, reasons) in brokenDiets) {
            // Inflate the stat layout
            val statLayout = layoutInflater.inflate(R.layout.warning_layout, null, false)

            // Find views in the stat layout
            val titleTextView = statLayout.findViewById<TextView>(R.id.warningTitleText)
            val descriptionTextView = statLayout.findViewById<TextView>(R.id.warningDescriptionText)

            // Set values for the stat
            titleTextView.text = "Voilates ${dietName} Diet!"
            descriptionTextView.text = reasons.joinToString(", ")

            // Add the stat layout to the parent layout
            view?.findViewById<LinearLayout>(R.id.warningContainer)?.addView(statLayout)
        }
    }

    private fun displayStats(product: JSONObject) {
        val nutrients = product.getJSONObject("nutriments")

        for (nutrient in nutrients.keys()) {
            if (nutrient.contains("_") || nutrient.contains("energy") || nutrient.contains("nova")) continue
            if (!nutrients.has("${nutrient}_value")) continue
            val nutrientValue = nutrients.getDouble("${nutrient}_value")
            val nutrientUnit = nutrients.getString("${nutrient}_unit")

            // Inflate the stat layout
            val statLayout = layoutInflater.inflate(R.layout.stat_layout, null, false)

            // Find views in the stat layout
            val titleTextView = statLayout.findViewById<TextView>(R.id.titleTextView)
            val descriptionTextView = statLayout.findViewById<TextView>(R.id.descriptionTextView)

            // Set values for the stat
            titleTextView.text = nutrient.toTitleCase()
            descriptionTextView.text = "${nutrientValue}${nutrientUnit}"

            // Add the stat layout to the parent layout
            view?.findViewById<LinearLayout>(R.id.parentLayout)?.addView(statLayout)
        }
    }

    private fun String.toTitleCase(): String {
        return this.split("-").joinToString(" ") { a -> a.replaceFirstChar { it.uppercaseChar() } }
    }

    private fun doCountingAnimation(productHealthScore: Int) {
        val scoreText = view?.findViewById<TextView>(R.id.scoreTextView)

        // Create a ValueAnimator that interpolates between 0 and the productHealthScore
        val animator = ValueAnimator.ofInt(0, productHealthScore)
        animator.duration = 1000 // Duration in milliseconds (adjust as needed)

        // Update the scoreTextView with the animated value on each frame
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            if (scoreText != null) {
                scoreText.text = "$animatedValue"
            }
        }

        // Start the animation
        animator.start()
    }

    private fun setProductImage(json: JSONObject) {
        var imageUrl =
            "https://storage.googleapis.com/spoonful_product_images_compressed/${json.getString("_id")}-300x300.webp"

        // Start the coroutine
        CoroutineScope(Dispatchers.Main).launch {
            if (!isImageUrlExists(imageUrl)) {
                imageUrl = json.getString("image_url")
            }

            // Load the image using Glide after checking image URL existence
            view?.findViewById<ImageView>(R.id.imageView).apply {
                this?.let { Glide.with(this@ProductInfoFragment).load(imageUrl).into(it) }
            }
        }
    }

    private suspend fun isImageUrlExists(imageUrl: String): Boolean {
        // Perform OkHttp request here and return true or false based on existence of image
        // This function will suspend until the request completes
        // Example:
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(imageUrl)
                .build()
            try {
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: IOException) {
                false
            }
        }
    }

    /*
    Idea: calculates how well it fits the users specified diet and or oil/health decisions
    takes into account the health score aswell
     */
    fun getUserMatchScore(product: JSONObject): JSONObject {
        val nutrients = product.getJSONObject("nutriments")
        // User opinions
        val userCanHavePeanuts = "none"
        val userCanHaveSeedOils = "indifferent"
        val userCanHaveGluten = "prefer-not"
        var userCanHaveLactose = "indifferent"
        var userWantsLowSodium = "preferred"
        var userWantsLowCarb = "indifferent"

        // Oil opinions
        val userWillEatPalmOil = "indifferent"
        val userWillEatSunflowerOil = "indifferent"
        val userWillEatConolaOil = "indifferent"
        val userWillEatCornOil = "indifferent"

        if (product.has("ingredients")) {
            val tags = product.getJSONArray("ingredients")
            for (i in 0 until tags.length()) {
                val ingredient = tags.getJSONObject(i)
                if (userWillEatPalmOil == "") {

                }
            }
        }


        val success = JSONObject()
        success.put("can_eat", true)
        return success
    }

    fun doesProductBreakDiet(product: JSONObject): MutableMap<String, MutableList<String>> {
        val nutrients = product.getJSONObject("nutriments")
        val brokenDiets = mutableMapOf<String, MutableList<String>>()

        // user diets
        val ketoDiet = true // very low-carb, 20-50g per day, per snack 2-4g
        val paleoDiet =
            true // CAN HAVE lean meats, fish, fruits, vegetables,nuts, and seeds. CAN NOT HAVE processed foods, grains, dairy, legumes
        val vegetarianDiet = true // No meat, poultry, or seafood
        val veganDiet = true // No Animal products at all.
        val mediterraneanDiet =
            true // Emphasizes fruits, vegetables, whole grains, legumes, nuts, seeds, olive oil, and herbs. Limits red meat and processed foods and encourages moderate consumption of fish and poultry.
        val glutenFreeDiet = true

        // Diets
        if (ketoDiet) {
            if (nutrients.getDouble("carbohydrates") > 8) {
                if (product.has("serving_quantity") && product.getDouble("serving_quantity") > 16) { // meal vs snack
                    brokenDiets["Keto"] = mutableListOf("Exceeds recommended carb intake for a meal")
                } else {
                    brokenDiets["Keto"] = mutableListOf("Exceeds recommended carb intake for a snack")
                }
            }
        }
        if (paleoDiet) {
            if (product.has("nova_groups_tags")) {
                val tags = product.getJSONArray("nova_groups_tags")
                for (i in 0 until tags.length()) {
                    val tag = tags[i]
                    if (tag.toString().contains("processed")) {
                        if (!brokenDiets.contains("Paleo")) brokenDiets["Paleo"] =
                            mutableListOf("Paleo doesnt allow processed foods")
                        break
                    }
                }
            }
            if (product.has("categories_hierarchy")) {
                val tags = product.getJSONArray("categories_hierarchy")
                for (i in 0 until tags.length()) {
                    val tag = tags[i]
                    if (tag.toString().contains("legumes")) {
                        if (!brokenDiets.contains("Paleo")) brokenDiets["Paleo"] = mutableListOf()
                        brokenDiets["Paleo"]!!.add("Paleo doesnt typically allow legumes")
                        break
                    }
                }
            }
            if (product.has("allergens_tags")) {
                val tags = product.getJSONArray("allergens_tags")
                for (i in 0 until tags.length()) {
                    val tag = tags[i]
                    if (tag.toString().contains("gluten")) {
                        if (!brokenDiets.contains("Paleo")) brokenDiets["Paleo"] = mutableListOf()
                        brokenDiets["Paleo"]!!.add("Paleo doesnt typically allow grains")
                        break
                    }
                }
                for (i in 0 until tags.length()) {
                    val tag = tags[i]
                    if (tag.toString().contains("milk")) {
                        if (!brokenDiets.contains("Paleo")) brokenDiets["Paleo"] = mutableListOf()
                        brokenDiets["Paleo"]!!.add("Paleo doesnt typically allow dairy")
                        break
                    }
                }
            }
        }
        if (vegetarianDiet) {
            if (product.has("ingredients")) {
                val tags = product.getJSONArray("ingredients")
                for (i in 0 until tags.length()) {
                    val ingredient = tags.getJSONObject(i)
                    if (ingredient.has("vegetarian")) {
                        if (ingredient.getString("vegetarian") != "yes") {
                            if (!brokenDiets.contains("Vegetarian")) brokenDiets["Vegetarian"] =
                                mutableListOf("One or more of the following ingredients is not vegetarian")
                            if (!brokenDiets["Vegetarian"]!!.contains(ingredient.getString("text"))) {
                                brokenDiets["Vegetarian"]!!.add(ingredient.getString("text"))
                            }
                        }
                    }
                }
            }
            val n = product.getString("product_name").toLowerCase()
            val violators = listOf("pork", "beef", "chicken", "turkey", "pig")
            for (violator in violators) {
                if (n.contains(violator)) {
                    if (!brokenDiets.contains("Vegetarian")) brokenDiets["Vegetarian"] =
                        mutableListOf("One or more of the following ingredients is not vegetarian")
                    if (!brokenDiets["Vegetarian"]!!.contains(violator)) {
                        brokenDiets["Vegetarian"]!!.add(violator)
                    }
                }
            }
            val keywords = product.getJSONArray("_keywords")
            for (i in 0 until keywords.length()) {
                if (violators.contains(keywords.getString(i))) {
                    if (!brokenDiets.contains("Vegetarian")) brokenDiets["Vegetarian"] =
                        mutableListOf("One or more of the following ingredients is not vegetarian")
                    if (!brokenDiets["Vegetarian"]!!.contains(keywords.getString(i))) {
                        brokenDiets["Vegetarian"]!!.add(keywords.getString(i))
                    }
                }
            }
        }
        if (veganDiet) {
            if (product.has("ingredients")) {
                val tags = product.getJSONArray("ingredients")
                for (i in 0 until tags.length()) {
                    val ingredient = tags.getJSONObject(i)
                    if (ingredient.has("vegan")) {
                        if (ingredient.getString("vegan") != "yes") {
                            if (!brokenDiets.contains("Vegan")) brokenDiets["Vegan"] =
                                mutableListOf("One or more of the following ingredients is not vegan")
                            if (!brokenDiets["Vegan"]!!.contains(ingredient.getString("text"))) {
                                brokenDiets["Vegan"]!!.add(ingredient.getString("text"))
                            }
                        }
                    }
                }
            }
            val n = product.getString("product_name").toLowerCase()
            val violators = listOf("pork", "beef", "chicken", "turkey", "pig")
            for (violator in violators) {
                if (n.contains(violator)) {
                    if (!brokenDiets.contains("Vegan")) brokenDiets["Vegan"] =
                        mutableListOf("One or more of the following ingredients is not vegan")
                    if (!brokenDiets["Vegan"]!!.contains(violator)) {
                        brokenDiets["Vegan"]!!.add(violator)
                    }
                }
            }
            val keywords = product.getJSONArray("_keywords")
            for (i in 0 until keywords.length()) {
                if (violators.contains(keywords.getString(i))) {
                    if (!brokenDiets.contains("Vegan")) brokenDiets["Vegan"] =
                        mutableListOf("One or more of the following ingredients is not vegan")
                    if (!brokenDiets["Vegan"]!!.contains(keywords.getString(i))) {
                        brokenDiets["Vegan"]!!.add(keywords.getString(i))
                    }
                }
            }
        }
        if (glutenFreeDiet) {
            if (product.has("allergens_tags")) {
                val tags = product.getJSONArray("allergens_tags")
                for (i in 0 until tags.length()) {
                    val tag = tags[i]
                    if (tag.toString().contains("gluten")) {
                        brokenDiets["Gluten Free"] = mutableListOf("This item contains gluten")
                    }
                }
            }
        }

        return brokenDiets
    }

    fun getNutritionScore(product: JSONObject): Float {
        if (product.has("nutriscore_score")) {
            // Food scaling
            val nutriscore = product.getInt("nutriscore_score").coerceIn(-4, 19)
            return 100F - max((nutriscore + 4) * 4, 0)
        }
        val nutritionStats = product.getJSONObject("nutriments")

        // Initialize total score to neutral (50)
        var healthScore = 50.0

        healthScore += checkProtein(nutritionStats.getDouble("proteins_value"))
        healthScore += checkSugar(nutritionStats.getDouble("sugars_value"))
        healthScore += checkSodium(nutritionStats.getDouble("sodium_value"))
        healthScore += checkCarbohydrates(nutritionStats.getDouble("carbohydrates_value"))
        healthScore += checkFats(nutritionStats.getDouble("saturated-fat_value"), nutritionStats.getDouble("fat_value"))

        if (product.has("additives_n")) {
            healthScore += checkAdditives(product.getDouble("additives_n"))
        }

        // Return rounded health score
        return healthScore.toFloat()
    }

    // 5%
    fun checkProtein(protein: Double): Double {
        if (protein <= 1) {
            println("LOW PROTEIN")
            return -5.0
        }
        if (protein <= 4) return 0.0

        return 5.0
    }

    // 15%
    fun checkSugar(sugar: Double): Double {
        if (sugar <= 1) return 15.0
        if (sugar <= 2) return 8.0
        if (sugar <= 4) return 2.0
        if (sugar <= 8) return -6.0

        if (sugar <= 50) {
            println("HIGH SUGAR")
            return -15.0
        }
        println("XTREME HIGH SUGAR")
        return -20.0 //
    }

    // 5%
    fun checkSodium(sodium: Double): Double {
        if (sodium < 200) return 5.0
        if (sodium < 400) return 2.0
        if (sodium < 600) return -1.0

        println("HIGH SODIUM")
        return -5.0
    }

    // 5%
    fun checkCarbohydrates(carbs: Double): Double {
        if (carbs < 20) return 5.0

        println("HIGH CARB")
        return -5.0
    }

    // 20%
    fun checkAdditives(additives: Double): Double {
        if (additives == 0.0) println("NO ADDITIVES")
        return max(-20.0, (20.0 + (-5 * (additives))))
    }

    // 20%
    fun checkFats(satFats: Double, fats: Double): Double {
        if (satFats + fats < 12) return 20 * (1 - (satFats + fats) / 12)
        if (fats / satFats > 2) {
            println("HIGH SAT FAT")
            return -20.0
        }

        println("HIGH FAT")
        return -15.0
    }
}
