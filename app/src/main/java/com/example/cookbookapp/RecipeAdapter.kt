
package com.example.cookbookapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class RecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private var recipes = listOf<Recipe>()

    fun submitList(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }

    class RecipeViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val recipeItem: View =
            view.findViewById(R.id.recipeItem)

        val thumbnail: ImageView =
            view.findViewById(R.id.ivRecipeThumbnail)

        val name: TextView =
            view.findViewById(R.id.tvRecipeName)

        val prepTime: TextView =
            view.findViewById(R.id.tvPrepTime)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)

        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RecipeViewHolder,
        position: Int
    ) {
        val recipe = recipes[position]

        holder.name.text = recipe.name
        holder.prepTime.text = "${recipe.prepTime} minutes"

        // Show a placeholder first.
        holder.thumbnail.setImageResource(
            android.R.drawable.ic_menu_gallery
        )

        // Load the saved image if the file exists.
        val imagePath = recipe.imagePath

        if (!imagePath.isNullOrBlank()) {
            val imageFile = File(imagePath)

            if (imageFile.exists() && imageFile.isFile) {
                try {
                    holder.thumbnail.setImageURI(
                        Uri.fromFile(imageFile)
                    )
                } catch (e: Exception) {
                    holder.thumbnail.setImageResource(
                        android.R.drawable.ic_menu_gallery
                    )
                }
            }
        }

        holder.itemView.isClickable = true

        holder.recipeItem.setOnClickListener {
            onRecipeClick(recipe)
        }
    }

    override fun getItemCount(): Int = recipes.size
}
