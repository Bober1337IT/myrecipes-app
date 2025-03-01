package com.example.myrecipes.data

data class RecipeSection(
    val title: String,
    val ingredients: MutableList<Pair<String, String?>>,
    var tips: String? // Tips (może być null)
)

fun parseRecipe(content: String): List<RecipeSection> {
    val sections = mutableListOf<RecipeSection>()
    var currentSection: RecipeSection? = null
    val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

    for (line in lines) {
        when {
            line.startsWith(":") -> {
                // Nowa sekcja
                currentSection?.let { sections.add(it) }
                currentSection = RecipeSection(
                    title = line.removePrefix(":").trim(),
                    ingredients = mutableListOf(),
                    tips = null
                )
            }

            line.startsWith("-") -> {
                // Składnik
                val parts = line.removePrefix("-").trim().split("|").map { it.trim() }
                val ingredient = parts.getOrNull(0) ?: continue
                val amount = parts.getOrNull(1)
                currentSection?.ingredients?.add(ingredient to amount)
            }

            line.startsWith(">") -> {
                // Tips
                val tipLine = line.removePrefix(">").trim()
                currentSection?.tips = currentSection?.tips?.let { "$it\n$tipLine" } ?: tipLine
            }
        }
    }

    currentSection?.let { sections.add(it) }
    return sections
}