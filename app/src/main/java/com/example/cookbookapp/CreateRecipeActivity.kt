
package com.example.cookbookapp

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CreateRecipeActivity : AppCompatActivity() {

    private lateinit var etRecipeName: EditText
    private lateinit var etPrepTime: EditText
    private lateinit var etIngredients: EditText
    private lateinit var etInstructions: EditText
    private lateinit var ivCreateRecipeImage: ImageView
    private lateinit var recipeDao: RecipeDao

    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivCreateRecipeImage.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_recipe)

        etRecipeName = findViewById(R.id.etRecipeName)
        etPrepTime = findViewById(R.id.etPrepTime)
        etIngredients = findViewById(R.id.etIngredients)
        etInstructions = findViewById(R.id.etInstructions)
        ivCreateRecipeImage = findViewById(R.id.ivCreateRecipeImage)

        val btnSaveRecipe = findViewById<Button>(R.id.btnSaveRecipe)
        val btnBack = findViewById<Button>(R.id.btnCreateBack)

        recipeDao = CookbookDatabase
            .getDatabase(applicationContext)
            .recipeDao()

        ivCreateRecipeImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnSaveRecipe.setOnClickListener {
            saveRecipe()
        }
    }

    private fun saveRecipe() {
        val name = etRecipeName.text.toString().trim()
        val prepTimeText = etPrepTime.text.toString().trim()
        val ingredients = etIngredients.text.toString().trim()
        val instructions = etInstructions.text.toString().trim()

        if (
            name.isEmpty() ||
            prepTimeText.isEmpty() ||
            ingredients.isEmpty() ||
            instructions.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Please fill in all required fields",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val prepTime = prepTimeText.toIntOrNull()

        if (prepTime == null || prepTime <= 0) {
            etPrepTime.error = "Enter a valid time greater than zero"
            return
        }

        lifecycleScope.launch {
            var savedImagePath: String? = null

            try {
                // Save the selected image locally, if one was chosen.
                val imageUri = selectedImageUri

                if (imageUri != null) {
                    savedImagePath =
                        ImageStorageHelper.saveImageToInternalStorage(
                            this@CreateRecipeActivity,
                            imageUri
                        )

                    if (savedImagePath == null) {
                        Toast.makeText(
                            this@CreateRecipeActivity,
                            "Could not save the selected image. Please try another image.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                }

                val recipe = Recipe(
                    name = name,
                    prepTime = prepTime,
                    ingredients = ingredients,
                    instructions = instructions,
                    imagePath = savedImagePath
                )

                recipeDao.insert(recipe)

                Toast.makeText(
                    this@CreateRecipeActivity,
                    "Recipe saved successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {
                // Remove an image file if saving the database record fails.
                ImageStorageHelper.deleteImageFromStorage(savedImagePath)

                Toast.makeText(
                    this@CreateRecipeActivity,
                    "Failed to save recipe",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
