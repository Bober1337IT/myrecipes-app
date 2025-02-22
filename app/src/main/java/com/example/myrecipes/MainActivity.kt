package com.example.myrecipes

import android.os.Bundle
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myrecipes.data.RecipeSection
import com.example.myrecipes.data.RecipesRepository
import com.example.myrecipes.data.parseRecipe
import com.example.myrecipes.ui.theme.MyRecipesTheme
import kotlinx.coroutines.delay
import java.time.format.TextStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Włącza wyświetlanie edge-to-edge
        setContent {
            MyRecipesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    val repository = RecipesRepository(this)
                    val fileNames = remember {
                        mutableStateListOf(*repository.getRecipesFileNames().toTypedArray())
                    }

                    // Konfiguracja nawigacji
                    NavHost(
                        navController = navController, startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                fileNames = fileNames,
                                repository = repository,
                                onNavigateToDetails = { fileName ->
                                    navController.navigate("details/$fileName")
                                },
                                onAddNewRecipe = { newFileName ->
                                    val isCreated = repository.createNewRecipeFile(newFileName)
                                    if (isCreated) {
                                        fileNames.add(newFileName) // Aktualizuj listę po dodaniu przepisu
                                    }
                                },
                                onDeleteRecipe = { fileName ->
                                    val isDeleted = repository.deleteRecipeFile(fileName)
                                    if (isDeleted) {
                                        fileNames.remove(fileName) // Aktualizuj listę po usunięciu przepisu
                                    }
                                },
                            )
                        }
                        composable("details/{fileName}") { backStackEntry ->
                            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                            DetailsScreen(
                                modifier = Modifier.padding(innerPadding),
                                fileName = fileName,
                                repository = repository,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Główny ekran wyświetlający listę przepisów
    @Composable
    fun MainScreen(
        modifier: Modifier = Modifier,
        fileNames: List<String>,
        repository: RecipesRepository,
        onNavigateToDetails: (String) -> Unit,
        onAddNewRecipe: (String) -> Unit,
        onDeleteRecipe: (String) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        var showAddDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var deleteFileMode by remember { mutableStateOf(false) }
        var exportFileMode by remember { mutableStateOf(false) }
        var newFileName by remember { mutableStateOf("") }
        var fileToDelete by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                Box {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        // Opcja "Add"
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.padding(end = 8.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(text = "Add", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                showAddDialog = true
                                expanded = false
                                deleteFileMode = false
                                exportFileMode = false
                            }
                        )

                        // Opcja "Delete"
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.padding(end = 8.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (!deleteFileMode) "Delete" else "Cancel",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                deleteFileMode = !deleteFileMode
                                if (exportFileMode) exportFileMode = false
                                expanded = false
                            }
                        )

                        // Opcja "Export"
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Export",
                                        modifier = Modifier.padding(end = 8.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (!exportFileMode) "Export" else "Cancel",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                exportFileMode = !exportFileMode
                                if (deleteFileMode) deleteFileMode = false
                                expanded = false
                            }
                        )
                    }
                }
            }
            LazyColumn {
                items(fileNames) { fileName ->
                    Button(
                        onClick = {
                            when {
                                deleteFileMode -> {
                                    fileToDelete = fileName
                                    showDeleteDialog = true
                                }
                                exportFileMode -> {
                                    val isExported = repository.exportRecipeFile(fileName)
                                    if (isExported) {
                                        Toast.makeText(
                                            context,
                                            "$fileName exported to Downloads!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        exportFileMode = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to export $fileName!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                else -> onNavigateToDetails(fileName)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                deleteFileMode -> Color.Red
                                exportFileMode -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = if (deleteFileMode || exportFileMode) Color.White else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Text(
                            text = fileName,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 24.sp,
                                lineHeight = 28.sp
                            )
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
        // Dialog dodawania nowego przepisu
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add new recipe") },
                text = {
                    TextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Recipe") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFileName.trim().isEmpty()) {
                            Toast.makeText(
                                context, "Please write name of a dish!", Toast.LENGTH_SHORT
                            ).show()
                        } else if (fileNames.contains(newFileName.trim())) {
                            Toast.makeText(
                                context, "Recipe with this name already exists!", Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onAddNewRecipe(newFileName.trim()) // Utwórz nowy plik przepisu
                            showAddDialog = false
                            newFileName = ""
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Dialog potwierdzający usunięcie przepisu
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    fileToDelete = null
                },
                title = { Text("Confirm deletion") },
                text = { Text("Are you sure you want to delete this recipe?") },
                confirmButton = {
                    Button(onClick = {
                        fileToDelete?.let { fileName ->
                            onDeleteRecipe(fileName) // Usuń przepis
                        }
                        showDeleteDialog = false
                        deleteFileMode = false
                        Toast.makeText(
                            context, "$fileToDelete has been deleted!", Toast.LENGTH_SHORT
                        ).show()
                        fileToDelete = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDeleteDialog = false
                        deleteFileMode = false
                        fileToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Ekran szczegółów przepisu – tryb podglądu/edycji
    @Composable
    fun DetailsScreen(
        modifier: Modifier = Modifier,
        fileName: String,
        repository: RecipesRepository,
        onBack: () -> Unit
    ) {
        val recipeContent = remember { repository.getRecipeContent(fileName) }
        val recipeSections =
            remember { mutableStateListOf(*parseRecipe(recipeContent).toTypedArray()) }
        val context = LocalContext.current

        // Stan edycji
        var notSavedDialog by remember { mutableStateOf(false) }
        var isEditing by remember { mutableStateOf(false) }

        // Stan dodawania nowej sekcji
        var newSectionName by remember { mutableStateOf("") }
        var showAddSectionDialog by remember { mutableStateOf(false) }

        // Stan dla usuwania sekcji
        var pendingDeleteSection by remember { mutableStateOf<RecipeSection?>(null) }

        // Stan dla usuwania składnika
        var pendingDeleteIngredient by remember { mutableStateOf<Pair<RecipeSection, Int>?>(null) }

        // Efekty resetujące
        LaunchedEffect(pendingDeleteSection) {
            if (pendingDeleteSection != null) {
                delay(3000)
                pendingDeleteSection = null
            }
        }

        LaunchedEffect(pendingDeleteIngredient) {
            if (pendingDeleteIngredient != null) {
                delay(3000)
                pendingDeleteIngredient = null
            }
        }

        // Obsługa przycisku "wstecz" podczas edycji
        BackHandler(enabled = isEditing) {
            notSavedDialog = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Pasek nawigacji (wstecz i tryb edycji)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (!isEditing) {
                            onBack()
                        } else {
                            notSavedDialog = true
                        }
                    },
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        // Przełączanie trybu edycji
                        if (!isEditing) {
                            isEditing = true
                        } else {
                            repository.saveChanges(fileName, recipeSections)
                            Toast.makeText(
                                context, "Changes saved successfully!", Toast.LENGTH_SHORT
                            ).show()
                            isEditing = false
                        }
                    },
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit"
                    )
                }
            }

            // Wyświetlanie nazwy przepisu (bez rozszerzenia .txt)
            Text(
                text = fileName.replace(".txt", ""),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Wyświetlanie sekcji przepisu
            LazyColumn {
                items(
                    items = recipeSections,
                    key = { section -> System.identityHashCode(section) } // Unikalny klucz
                ) { section ->
                    val sectionIndex = recipeSections.indexOf(section)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .weight(0.75f)
                                .padding(1.dp)
                        ) {
                            Text(
                                text = section.title + ":",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(0.25f)
                        ) {
                            Row {
                                if (isEditing) {
                                    IconButton(
                                        onClick = {
                                            val newIngredients =
                                                section.ingredients.toMutableList().apply {
                                                    add("" to "")
                                                }
                                            recipeSections[sectionIndex] =
                                                section.copy(ingredients = newIngredients)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add ingredient"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (pendingDeleteSection == section) {
                                                recipeSections.remove(section)
                                                pendingDeleteSection = null
                                            } else {
                                                pendingDeleteSection = section
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete section",
                                            tint = if (pendingDeleteSection == section) Color.Red else LocalContentColor.current
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Wyświetlanie składników sekcji
                    section.ingredients.forEachIndexed { index, ingredientPair ->
                        var ingredientName by remember { mutableStateOf(ingredientPair.first) }
                        var ingredientAmount by remember {
                            mutableStateOf(
                                ingredientPair.second ?: ""
                            )
                        }
                        val isChecked = remember { mutableStateOf(false) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(1.dp)
                                .clickable { isChecked.value = !isChecked.value }
                                .background(
                                    if (isChecked.value && !isEditing)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                        ) {
                            if (isEditing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = ingredientName,
                                        onValueChange = { newName ->
                                            ingredientName = newName
                                            section.ingredients[index] =
                                                ingredientName to ingredientAmount
                                        },
                                        label = { Text("Ingredient") },
                                        modifier = Modifier.weight(0.60f)
                                    )
                                    OutlinedTextField(
                                        value = ingredientAmount,
                                        onValueChange = { newAmount ->
                                            ingredientAmount = newAmount
                                            section.ingredients[index] =
                                                ingredientName to ingredientAmount
                                        },
                                        label = { Text("Amount") },
                                        modifier = Modifier.weight(0.35f)
                                    )

                                    Spacer(modifier = Modifier.weight(0.05f))

                                    IconButton(
                                        onClick = {
                                            val current = section to index
                                            if (pendingDeleteIngredient == current) {
                                                val updatedIngredients =
                                                    section.ingredients.toMutableList().apply {
                                                        removeAt(index)
                                                    }
                                                recipeSections[recipeSections.indexOf(section)] =
                                                    section.copy(ingredients = updatedIngredients)
                                                pendingDeleteIngredient = null
                                            } else {
                                                pendingDeleteIngredient = current
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete ingredient",
                                            tint = if (pendingDeleteIngredient == (section to index)) Color.Red else LocalContentColor.current
                                        )
                                    }
                                }
                            } else {
                                Checkbox(
                                    checked = isChecked.value,
                                    onCheckedChange = { isChecked.value = it },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                // Tryb podglądu bez zmian
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = ingredientName, modifier = Modifier.weight(1f)
                                    )
                                    if (ingredientAmount.isNotEmpty()) {
                                        Text(
                                            text = ingredientAmount,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Wyświetlanie sekcji "Tips"
                    if (isEditing) {
                        var currentTips by remember { mutableStateOf(section.tips ?: "") }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Tips:",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = currentTips,
                                onValueChange = { newTips ->
                                    currentTips = newTips
                                    section.tips = newTips
                                },
                                label = { Text("Edit tips") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                    } else {
                        section.tips?.takeIf { it.isNotEmpty() }?.let { tips ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Tips:",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                                Text(
                                    text = tips,
                                    fontSize = 18.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
                item {
                    if (isEditing) {
                        Button(
                            onClick = { showAddSectionDialog = true },
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Add Section")
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }

            // Dialog przy próbie powrotu z niezapisanymi zmianami
            if (notSavedDialog) {
                AlertDialog(
                    onDismissRequest = { notSavedDialog = false },
                    title = { Text("You haven't saved the changes!") },
                    confirmButton = {
                        Button(onClick = {
                            repository.saveChanges(fileName, recipeSections)
                            Toast.makeText(
                                context, "Changes saved successfully!", Toast.LENGTH_SHORT
                            ).show()
                            isEditing = false
                            notSavedDialog = false
                            onBack()
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                notSavedDialog = false
                                isEditing = false
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Don't save")
                        }
                    }
                )
            }

            // Dialog dodawania nowej sekcji
            if (showAddSectionDialog) {
                AlertDialog(
                    onDismissRequest = { showAddSectionDialog = false },
                    title = { Text("Add New Section") },
                    text = {
                        TextField(
                            value = newSectionName,
                            onValueChange = { newSectionName = it },
                            label = { Text("Section Name") }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newSectionName.isNotEmpty()) {
                                // Dodaj nową sekcję do listy
                                recipeSections.add(
                                    RecipeSection(
                                        newSectionName, mutableListOf(), null
                                    )
                                )
                                showAddSectionDialog = false
                                newSectionName = "" // Reset nazwy sekcji
                            } else {
                                Toast.makeText(
                                    context, "Section name cannot be empty!", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showAddSectionDialog = false
                            newSectionName = "" // Reset po anulowaniu
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    // Podgląd ekranu głównego
    @Preview(showBackground = true)
    @Composable
    fun PreviewMainScreen() {
        MyRecipesTheme {
            val exampleFileNames = listOf("recipe1.txt", "recipe2.txt", "recipe3.txt")
            val repository = RecipesRepository(this)

            // Pusta funkcja nawigacyjna
            val onNavigateToDetails: (String) -> Unit = { fileName ->
                println("Navigating to details of $fileName")
            }

            // Pusta funkcja dodawania przepisu
            val onAddNewRecipe: (String) -> Unit = { newFileName ->
                println("Adding new recipe: $newFileName")
            }

            // Pusta funkcja usuwania przepisu
            val onDeleteRecipe: (String) -> Unit = { fileName ->
                println("Deleting recipe: $fileName")
            }

            MainScreen(
                modifier = Modifier.fillMaxSize(),
                fileNames = exampleFileNames,
                repository = repository,
                onNavigateToDetails = onNavigateToDetails,
                onAddNewRecipe = onAddNewRecipe,
                onDeleteRecipe = onDeleteRecipe
            )
        }
    }
}
