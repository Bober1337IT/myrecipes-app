package com.example.myrecipes.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.IOException
import java.io.InputStream

// Klasa zarządzająca plikami przepisów w pamięci wewnętrznej aplikacji
class RecipesRepository(private val context: Context) {

    fun getRecipesFileNames(): List<String> {
        val fileNames = mutableListOf<String>()
        try {
            val folder = File(context.filesDir, "RecipesFile") // Ścieżka do folderu RecipesFile
            if (folder.exists() && folder.isDirectory) {
                val files = folder.listFiles()
                files?.mapTo(fileNames) { it.name.removeSuffix(".txt") } // Usuwa .txt z nazw
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fileNames
    }

    // Wczytuje zawartość pliku
    fun getRecipeContent(fileName: String): String {
        val file = File(context.filesDir, "RecipesFile/$fileName.txt")
        return file.readText()
    }

    fun createNewRecipeFile(fileName: String): Boolean {
        return try {
            val folder = File(context.filesDir, "RecipesFile")
            if (!folder.exists()) folder.mkdirs() // Tworzy folder, jeśli nie istnieje
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
            file.exists() && file.delete() // Usuwa plik, jeśli istnieje
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveChanges(fileName: String, recipeSections: List<RecipeSection>): Boolean {
        return try {
            val file = File(context.filesDir, "RecipesFile/$fileName.txt")
            val content = buildString { // Tworzy sformatowaną zawartość pliku
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

    // Eksportuje plik do folderu Downloads
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

    fun importRecipeFile(uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.use { it.readText() } ?: return null // Odczytuje zawartość
            val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex).removeSuffix(".txt")
            } ?: "imported_recipe_${System.currentTimeMillis()}" // Generuje nazwę, jeśli brak

            val folder = File(context.filesDir, "RecipesFile")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "$fileName.txt")
            file.writeText(content)
            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyRecipesFromAssets() {
        try {
            val folder = File(context.filesDir, "RecipesFile")
            if (!folder.exists()) folder.mkdirs() // Tworzy folder, jeśli nie istnieje

            // Sprawdza, czy folder jest pusty
            if (folder.listFiles()?.isEmpty() != false) {
                val assetManager = context.assets
                val files = assetManager.list("") ?: return // Pobiera listę plików z assets

                // Kopiowanie każdego pliku z assets do pamięci wewnętrznej
                for (fileName in files.filter { it.endsWith(".txt") }) {
                    val inputStream = assetManager.open(fileName)
                    val destFile = File(folder, fileName)
                    inputStream.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output) // Kopiuje zawartość
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}