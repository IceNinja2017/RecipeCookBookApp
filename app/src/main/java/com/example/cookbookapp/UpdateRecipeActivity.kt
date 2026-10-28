
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
import java.io.File

class UpdateRecipeActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPrepTime: EditText
    private lateinit var etIngredients: EditText
    private lateinit var etInstructions: EditText
    private lateinit var ivUpdateRecipeImage: ImageView
    private lateinit var recipeDao: RecipeDao

    private var recipeId: Int = -1
    private var originalImagePath: String? = null
    private var selectedImageUri: Uri? = null
    private var isSaving = false

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivUpdateRecipeImage.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_recipe)

        etName = findViewById(R.id.etUpdateRecipeName)
        etPrepTime = findViewById(R.id.etUpdatePrepTime)
        etIngredients = findViewById(R.id.etUpdateIngredients)
        etInstructions = findViewById(R.id.etUpdateInstructions)
        ivUpdateRecipeImage = findViewById(R.id.ivUpdateRecipeImage)

        val btnBack = findViewById<Button>(R.id.btnUpdateBack)
        val btnSave = findViewById<Button>(R.id.btnSaveChanges)

        recipeDao = CookbookDatabase
            .getDatabase(applicationContext)
            .recipeDao()

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

        ivUpdateRecipeImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveChanges()
        }

        loadRecipe()
    }

    private fun loadRecipe() {
        lifecycleScope.launch {
            try {
                val recipe = recipeDao.getRecipeById(recipeId)

                if (recipe == null) {
                    Toast.makeText(
                        this@UpdateRecipeActivity,
                        "Recipe no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                etName.setText(recipe.name)
                etPrepTime.setText(recipe.prepTime.toString())
                etIngredients.setText(recipe.ingredients)
                etInstructions.setText(recipe.instructions)

                originalImagePath = recipe.imagePath

                val path = recipe.imagePath
                if (!path.isNullOrBlank() && File(path).isFile) {
                    ivUpdateRecipeImage.setImageURI(Uri.fromFile(File(path)))
                } else {
                    ivUpdateRecipeImage.setImageResource(
                        android.R.drawable.ic_menu_gallery
                    )
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@UpdateRecipeActivity,
                    "Failed to load recipe",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun saveChanges() {
        if (isSaving) return

        val name = etName.text.toString().trim()
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

        isSaving = true

        lifecycleScope.launch {
            var newImagePath: String? = null

            try {
                val existingRecipe = recipeDao.getRecipeById(recipeId)

                if (existingRecipe == null) {
                    Toast.makeText(
                        this@UpdateRecipeActivity,
                        "Recipe no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                // Copy a newly selected image into app storage, if provided.
                val imageUri = selectedImageUri

                val finalImagePath = if (imageUri != null) {
                    newImagePath =
                        ImageStorageHelper.saveImageToInternalStorage(
                            this@UpdateRecipeActivity,
                            imageUri
                        )

                    if (newImagePath == null) {
                        Toast.makeText(
                            this@UpdateRecipeActivity,
                            "Could not save the selected image",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    newImagePath
                } else {
                    existingRecipe.imagePath
                }

                val updatedRecipe = existingRecipe.copy(
                    name = name,
                    prepTime = prepTime,
                    ingredients = ingredients,
                    instructions = instructions,
                    imagePath = finalImagePath
                )

                recipeDao.update(updatedRecipe)

                // Delete the previous image only after the update succeeds.
                if (
                    imageUri != null &&
                    existingRecipe.imagePath != finalImagePath
                ) {
                    ImageStorageHelper.deleteImageFromStorage(
                        existingRecipe.imagePath
                    )
                }

                originalImagePath = finalImagePath
                selectedImageUri = null

                Toast.makeText(
                    this@UpdateRecipeActivity,
                    "Recipe updated successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {
                // Keep the original image; remove only the new unused copy.
                ImageStorageHelper.deleteImageFromStorage(newImagePath)

                Toast.makeText(
                    this@UpdateRecipeActivity,
                    "Failed to update recipe",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isSaving = false
            }
        }
    }
}
