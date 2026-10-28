
package com.example.cookbookapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class RecipeDetailActivity : AppCompatActivity() {

    private var recipeId: Int = -1
    private lateinit var dao: RecipeDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        recipeId = intent.getIntExtra("recipe_id", -1)

        if (recipeId == -1) {
            Toast.makeText(
                this,
                "Recipe not found",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        dao = CookbookDatabase
            .getDatabase(applicationContext)
            .recipeDao()

        findViewById<Button>(R.id.btnDetailBack)
            .setOnClickListener {
                finish()
            }

        // Delete recipe button
        val btnDeleteRecipe =
            findViewById<Button>(R.id.btnDeleteRecipe)

        btnDeleteRecipe.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage(
                    "Are you sure you want to delete this recipe? " +
                            "This action cannot be undone."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    deleteRecipe()
                }
                .show()
        }

        // Update recipe button
        val btnUpdateRecipe =
            findViewById<Button>(R.id.btnUpdateRecipe)

        btnUpdateRecipe.setOnClickListener {
            val updateIntent = android.content.Intent(
                this,
                UpdateRecipeActivity::class.java
            )

            updateIntent.putExtra("recipe_id", recipeId)
            startActivity(updateIntent)
        }
    }

    // Refresh recipe details whenever this screen resumes
    override fun onResume() {
        super.onResume()

        if (recipeId != -1 && ::dao.isInitialized) {
            loadRecipeDetails()
        }
    }

    // Load the latest recipe data from Room
    private fun loadRecipeDetails() {
        lifecycleScope.launch {
            try {
                val recipe = dao.getRecipeById(recipeId)

                if (recipe == null) {
                    Toast.makeText(
                        this@RecipeDetailActivity,
                        "Recipe no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@launch
                }

                findViewById<TextView>(R.id.tvDetailName).text =
                    recipe.name

                findViewById<TextView>(R.id.tvDetailPrepTime).text =
                    "${recipe.prepTime} minutes"

                findViewById<TextView>(R.id.tvDetailIngredients).text =
                    recipe.ingredients

                findViewById<TextView>(R.id.tvDetailInstructions).text =
                    recipe.instructions

                val imageView =
                    findViewById<ImageView>(R.id.ivDetailImage)

                val path = recipe.imagePath

                if (!path.isNullOrBlank() && File(path).isFile) {
                    imageView.setImageURI(
                        android.net.Uri.fromFile(File(path))
                    )
                } else {
                    imageView.setImageResource(
                        android.R.drawable.ic_menu_gallery
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RecipeDetailActivity,
                    "Failed to load recipe",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Delete recipe from the database
    private fun deleteRecipe() {
        lifecycleScope.launch {
            try {
                val recipe = dao.getRecipeById(recipeId)

                if (recipe != null) {
                    dao.delete(recipe)

                    ImageStorageHelper.deleteImageFromStorage(
                        recipe.imagePath
                    )

                    Toast.makeText(
                        this@RecipeDetailActivity,
                        "Recipe deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                } else {
                    Toast.makeText(
                        this@RecipeDetailActivity,
                        "Recipe no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RecipeDetailActivity,
                    "Failed to delete recipe",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
