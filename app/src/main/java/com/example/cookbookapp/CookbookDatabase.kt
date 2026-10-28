package com.example.cookbookapp

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. THE ENTITY (Your Single Database Table)
// ==========================================
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "recipe_name") val name: String,
    @ColumnInfo(name = "prep_time") val prepTime: Int, // in minutes
    val ingredients: String,
    val instructions: String,
    @ColumnInfo(name = "image_path") val imagePath: String? // Local file path string
)

// ==========================================
// 2. THE DAO (Your CRUD Operations)
// ==========================================
@Dao
interface RecipeDao {
    @Insert
    suspend fun insert(recipe: Recipe) // CREATE

    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAllRecipes(): Flow<List<Recipe>> // READ (Streams live updates to UI)


    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: Int): Recipe?

    @Update
    suspend fun update(recipe: Recipe) // UPDATE

    @Delete
    suspend fun delete(recipe: Recipe) // DELETE
}

// ==========================================
// 3. THE DATABASE ACCESS POINT
// ==========================================
@Database(entities = [Recipe::class], version = 1, exportSchema = false)
abstract class CookbookDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: CookbookDatabase? = null

        fun getDatabase(context: Context): CookbookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CookbookDatabase::class.java,
                    "cookbook_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
