
package com.example.cookbookapp

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {

    fun saveImageToInternalStorage(
        context: Context,
        uri: Uri
    ): String? {
        var outputFile: File? = null

        return try {
            val mimeType = context.contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?.lowercase()
                ?: "img"

            outputFile = File(
                context.filesDir,
                "recipe_${System.currentTimeMillis()}.$extension"
            )

            val inputStream = context.contentResolver
                .openInputStream(uri)
                ?: return null

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            outputFile?.delete()
            null
        }
    }

    fun deleteImageFromStorage(path: String?): Boolean {
        if (path.isNullOrBlank()) return false

        val file = File(path)

        // Only delete files stored in this app's internal files directory.
        val appFilesDirectory = File(
            file.parentFile ?: return false,
            ""
        )

        return try {
            val expectedDirectory = File(file.absolutePath).canonicalFile.parentFile
            val actualDirectory = File(
                file.parentFile?.absolutePath ?: return false
            ).canonicalFile

            if (
                expectedDirectory == actualDirectory &&
                actualDirectory == File(
                    file.parentFile.absolutePath
                ).canonicalFile &&
                file.exists() &&
                file.isFile
            ) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
