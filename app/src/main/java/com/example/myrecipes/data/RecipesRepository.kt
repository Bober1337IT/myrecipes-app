package com.example.myrecipes.data

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException

class RecipesRepository(private val context: Context) {

    // Funkcja zwracająca listę nazw plików z folderu "RecipesFile" w zasobach wewnętrznych
    fun getRecipesFileNames(): List<String> {
        val fileNames = mutableListOf<String>()
        try {
            val folder = File(context.filesDir, "RecipesFile")
            if (folder.exists() && folder.isDirectory) {
                val files = folder.listFiles()
                files?.mapTo(fileNames) { it.name.removeSuffix(".txt") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fileNames
    }

    // Funkcja do wczytywania zawartości pliku
    fun getRecipeContent(fileName: String): String {
        val file = File(context.filesDir, "RecipesFile/$fileName.txt")
        return file.readText()
    }

    fun createNewRecipeFile(fileName: String): Boolean {
        return try {
            val folder = File(context.filesDir, "RecipesFile")
            if (!folder.exists()) folder.mkdirs()

            val file = File(folder, "$fileName.txt")
            file.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteRecipeFile(fileName: String): Boolean {
        return try {
            val file = File(context.filesDir, "RecipesFile/$fileName.txt")
            file.exists() && file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveChanges(fileName: String, recipeSections: List<RecipeSection>): Boolean {
        return try {
            val file = File(context.filesDir, "RecipesFile/$fileName.txt")
            val content = buildString {
                recipeSections.forEach { section ->
                    append(": ${section.title}\n")
                    section.ingredients.forEach { (ingredient, amount) ->
                        append("- $ingredient | $amount\n")
                    }
                    section.tips?.let { append("> $it\n") }
                    append("\n")
                }
            }
            file.writeText(content)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun exportRecipeFile(fileName: String): Boolean {
        return try {
            val sourceFile = File(context.filesDir, "RecipesFile/$fileName.txt")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, "$fileName.txt")

            if (sourceFile.exists()) {
                sourceFile.copyTo(destFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importRecipeFile(fileName: String): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val sourceFile = File(downloadsDir, "$fileName")
            val destFolder = File(context.filesDir, "RecipesFile")
            if (!destFolder.exists()) destFolder.mkdirs()
            val destFile = File(destFolder, "$fileName")

            if (sourceFile.exists()) {
                sourceFile.copyTo(destFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}