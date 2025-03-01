package com.example.myrecipes

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myrecipes.data.RecipeSection
import com.example.myrecipes.data.RecipesRepository
import com.example.myrecipes.data.parseRecipe
import com.example.myrecipes.ui.theme.MyRecipesTheme
import kotlinx.coroutines.delay

// Główna klasa aktywności aplikacji
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Włącza tryb pełnoekranowy (edge-to-edge)
        setContent {
            MyRecipesTheme { // Ustawia motyw aplikacji
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> // Tworzy podstawowy układ z paddingiem
                    val navController = rememberNavController() // Kontroler nawigacji
                    val repository = RecipesRepository(this) // Repozytorium do zarządzania plikami przepisów
                    val fileNames = remember { // Lista nazw plików przepisów, reaktywna dzięki mutableStateListOf
                        mutableStateListOf(*repository.getRecipesFileNames().toTypedArray())
                    }

                    // Nawigacja między ekranami
                    NavHost(
                        navController = navController,
                        startDestination = "main" // Początkowy ekran to "main"
                    ) {
                        composable("main") { // Ekran główny
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                fileNames = fileNames,
                                repository = repository,
                                onNavigateToDetails = { fileName ->
                                    navController.navigate("details/$fileName") // Przejście do szczegółów przepisu
                                },
                                onAddNewRecipe = { newFileName ->
                                    val isCreated = repository.createNewRecipeFile(newFileName)
                                    if (isCreated) fileNames.add(newFileName) // Dodaje nowy przepis do listy
                                },
                                onDeleteRecipe = { fileName ->
                                    val isDeleted = repository.deleteRecipeFile(fileName)
                                    if (isDeleted) fileNames.remove(fileName) // Usuwa przepis z listy
                                }
                            )
                        }
                        composable("details/{fileName}") { backStackEntry -> // Ekran szczegółów przepisu
                            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
                            DetailsScreen(
                                modifier = Modifier.padding(innerPadding),
                                fileName = fileName,
                                repository = repository,
                                onBack = { navController.popBackStack() } // Powrót do poprzedniego ekranu
                            )
                        }
                    }
                }
            }
        }
    }

    // Ekran główny wyświetlający listę przepisów
    @Composable
    fun MainScreen(
        modifier: Modifier = Modifier,
        fileNames: SnapshotStateList<String>, // Lista nazw plików, reaktywna i modyfikowalna
        repository: RecipesRepository,
        onNavigateToDetails: (String) -> Unit,
        onAddNewRecipe: (String) -> Unit,
        onDeleteRecipe: (String) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) } // Stan rozwinięcia menu opcji
        var showAddDialog by remember { mutableStateOf(false) } // Pokazuje dialog dodawania przepisu
        var showDeleteDialog by remember { mutableStateOf(false) } // Pokazuje dialog usuwania przepisu
        var deleteFileMode by remember { mutableStateOf(false) } // Tryb usuwania przepisów
        var exportFileMode by remember { mutableStateOf(false) } // Tryb eksportu przepisów
        var newFileName by remember { mutableStateOf("") } // Nazwa nowego przepisu w dialogu
        var fileToDelete by remember { mutableStateOf<String?>(null) } // Plik do usunięcia
        val context = LocalContext.current // Kontekst aplikacji

        // Launcher do wyboru pliku z pamięci urządzenia
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                val importedFileName = repository.importRecipeFile(it) // Importuje wybrany plik
                importedFileName?.let { name ->
                    if (!fileNames.contains(name)) {
                        fileNames.add(name) // Dodaje nowy plik do listy
                        Toast.makeText(context, "$name imported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "$name already exists!", Toast.LENGTH_SHORT).show()
                    }
                } ?: Toast.makeText(context, "Failed to import file!", Toast.LENGTH_SHORT).show()
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp)) // Odstęp górny
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp) // Logo aplikacji
                )
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp) // Nazwa aplikacji
                )
                Box {
                    IconButton(onClick = { expanded = !expanded }) { // Przycisk menu opcji
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
                                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "Add", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                showAddDialog = true // Pokazuje dialog dodawania
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
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Text(text = if (!deleteFileMode) "Delete" else "Cancel", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                deleteFileMode = !deleteFileMode // Przełącza tryb usuwania
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
                                    Icon(Icons.Default.Send, contentDescription = "Export", modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Text(text = if (!exportFileMode) "Export" else "Cancel", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                exportFileMode = !exportFileMode // Przełącza tryb eksportu
                                if (deleteFileMode) deleteFileMode = false
                                expanded = false
                            }
                        )
                        // Opcja "Import"
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = "Import", modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "Import", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                filePickerLauncher.launch(arrayOf("text/plain")) // Uruchamia wybór pliku TXT
                                expanded = false
                                deleteFileMode = false
                                exportFileMode = false
                            }
                        )
                    }
                }
            }
            LazyColumn { // Lista przepisów w formie przewijanej
                items(fileNames) { fileName ->
                    Button(
                        onClick = {
                            when {
                                deleteFileMode -> {
                                    fileToDelete = fileName
                                    showDeleteDialog = true // Pokazuje dialog potwierdzenia usunięcia
                                }
                                exportFileMode -> {
                                    val isExported = repository.exportRecipeFile(fileName)
                                    if (isExported) {
                                        Toast.makeText(context, "$fileName exported to Downloads!", Toast.LENGTH_SHORT).show()
                                        exportFileMode = false
                                    } else {
                                        Toast.makeText(context, "Failed to export $fileName!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> onNavigateToDetails(fileName) // Przejście do szczegółów przepisu
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                deleteFileMode -> Color.Red // Czerwony w trybie usuwania
                                exportFileMode -> MaterialTheme.colorScheme.tertiary // Inny kolor w trybie eksportu
                                else -> MaterialTheme.colorScheme.primary // Standardowy kolor
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
                            style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, lineHeight = 28.sp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) } // Odstęp na dole listy
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
                            Toast.makeText(context, "Please write name of a dish!", Toast.LENGTH_SHORT).show()
                        } else if (fileNames.contains(newFileName.trim())) {
                            Toast.makeText(context, "Recipe with this name already exists!", Toast.LENGTH_SHORT).show()
                        } else {
                            onAddNewRecipe(newFileName.trim()) // Dodaje nowy przepis
                            showAddDialog = false
                            newFileName = ""
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    Button(onClick = { showAddDialog = false }) { Text("Cancel") }
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
                            onDeleteRecipe(fileName) // Usuwa przepis
                        }
                        showDeleteDialog = false
                        deleteFileMode = false
                        Toast.makeText(context, "$fileToDelete has been deleted!", Toast.LENGTH_SHORT).show()
                        fileToDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    Button(onClick = {
                        showDeleteDialog = false
                        deleteFileMode = false
                        fileToDelete = null
                    }) { Text("Cancel") }
                }
            )
        }
    }

    // Ekran szczegółów przepisu – tryb podglądu i edycji
    @Composable
    fun DetailsScreen(
        modifier: Modifier = Modifier,
        fileName: String,
        repository: RecipesRepository,
        onBack: () -> Unit
    ) {
        val recipeContent = remember { repository.getRecipeContent(fileName) } // Wczytuje zawartość przepisu
        val recipeSections = remember { mutableStateListOf(*parseRecipe(recipeContent).toTypedArray()) } // Parsuje sekcje przepisu
        val context = LocalContext.current

        var notSavedDialog by remember { mutableStateOf(false) } // Pokazuje dialog niezapisanych zmian
        var isEditing by remember { mutableStateOf(false) } // Tryb edycji
        var newSectionName by remember { mutableStateOf("") } // Nazwa nowej sekcji
        var showAddSectionDialog by remember { mutableStateOf(false) } // Pokazuje dialog dodawania sekcji
        var pendingDeleteSection by remember { mutableStateOf<RecipeSection?>(null) } // Sekcja do usunięcia
        var pendingDeleteIngredient by remember { mutableStateOf<Pair<RecipeSection, Int>?>(null) } // Składnik do usunięcia

        // Resetuje stan oczekiwania na usunięcie po 3 sekundach
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

        // Obsługuje przycisk "wstecz" w trybie edycji
        BackHandler(enabled = isEditing) {
            notSavedDialog = true
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (!isEditing) onBack() else notSavedDialog = true // Powrót lub pokazanie dialogu
                    },
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        if (!isEditing) {
                            isEditing = true // Włącza tryb edycji
                        } else {
                            repository.saveChanges(fileName, recipeSections) // Zapisuje zmiany
                            Toast.makeText(context, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
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

            Text(
                text = fileName.replace(".txt", ""),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp) // Wyświetla nazwę przepisu
            )

            LazyColumn { // Lista sekcji przepisu
                items(items = recipeSections, key = { section -> System.identityHashCode(section) }) { section ->
                    val sectionIndex = recipeSections.indexOf(section)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(0.75f).padding(1.dp)
                        ) {
                            Text(
                                text = "${section.title}:",
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
                                            val newIngredients = section.ingredients.toMutableList().apply { add("" to "") }
                                            recipeSections[sectionIndex] = section.copy(ingredients = newIngredients) // Dodaje nowy składnik
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add ingredient")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (pendingDeleteSection == section) {
                                                recipeSections.remove(section) // Usuwa sekcję
                                                pendingDeleteSection = null
                                            } else {
                                                pendingDeleteSection = section // Oznacza sekcję do usunięcia
                                            }
                                        },
                                        modifier = Modifier.size(48.dp).padding(end = 4.dp)
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

                    // Wyświetlanie i edycja składników
                    section.ingredients.forEachIndexed { index, ingredientPair ->
                        var ingredientName by remember { mutableStateOf(ingredientPair.first) }
                        var ingredientAmount by remember { mutableStateOf(ingredientPair.second ?: "") }
                        val isChecked = remember { mutableStateOf(false) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(1.dp)
                                .clickable { isChecked.value = !isChecked.value }
                                .background(if (isChecked.value && !isEditing) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
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
                                            section.ingredients[index] = ingredientName to ingredientAmount // Aktualizuje nazwę składnika
                                        },
                                        label = { Text("Ingredient") },
                                        modifier = Modifier.weight(0.60f)
                                    )
                                    OutlinedTextField(
                                        value = ingredientAmount,
                                        onValueChange = { newAmount ->
                                            ingredientAmount = newAmount
                                            section.ingredients[index] = ingredientName to ingredientAmount // Aktualizuje ilość
                                        },
                                        label = { Text("Amount") },
                                        modifier = Modifier.weight(0.35f)
                                    )
                                    Spacer(modifier = Modifier.weight(0.05f))
                                    IconButton(
                                        onClick = {
                                            val current = section to index
                                            if (pendingDeleteIngredient == current) {
                                                val updatedIngredients = section.ingredients.toMutableList().apply { removeAt(index) }
                                                recipeSections[recipeSections.indexOf(section)] = section.copy(ingredients = updatedIngredients) // Usuwa składnik
                                                pendingDeleteIngredient = null
                                            } else {
                                                pendingDeleteIngredient = current // Oznacza składnik do usunięcia
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
                                    modifier = Modifier.padding(end = 8.dp) // Checkbox w trybie podglądu
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = ingredientName, modifier = Modifier.weight(1f))
                                    if (ingredientAmount.isNotEmpty()) {
                                        Text(
                                            text = ingredientAmount,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sekcja "Tips"
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
                                    section.tips = newTips // Aktualizuje wskazówki
                                },
                                label = { Text("Edit tips") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
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
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        ) { Text("Add Section") } // Przycisk dodawania nowej sekcji
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) } // Odstęp na dole
            }

            // Dialog niezapisanych zmian
            if (notSavedDialog) {
                AlertDialog(
                    onDismissRequest = { notSavedDialog = false },
                    title = { Text("You haven't saved the changes!") },
                    confirmButton = {
                        Button(onClick = {
                            repository.saveChanges(fileName, recipeSections) // Zapisuje zmiany
                            Toast.makeText(context, "Changes saved successfully!", Toast.LENGTH_SHORT).show()
                            isEditing = false
                            notSavedDialog = false
                            onBack()
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                notSavedDialog = false
                                isEditing = false
                                onBack() // Wychodzi bez zapisu
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                        ) { Text("Don't save") }
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
                                recipeSections.add(RecipeSection(newSectionName, mutableListOf(), null)) // Dodaje nową sekcję
                                showAddSectionDialog = false
                                newSectionName = ""
                            } else {
                                Toast.makeText(context, "Section name cannot be empty!", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showAddSectionDialog = false
                            newSectionName = ""
                        }) { Text("Cancel") }
                    }
                )
            }
        }
    }

    // Podgląd ekranu głównego w Android Studio
    @SuppressLint("UnrememberedMutableState")
    @Preview(showBackground = true)
    @Composable
    fun PreviewMainScreen() {
        MyRecipesTheme {
            val exampleFileNames = mutableStateListOf("recipe1.txt", "recipe2.txt", "recipe3.txt")
            val repository = RecipesRepository(this)

            val onNavigateToDetails: (String) -> Unit = { fileName ->
                println("Navigating to details of $fileName") // Symulacja nawigacji w podglądzie
            }
            val onAddNewRecipe: (String) -> Unit = { newFileName ->
                println("Adding new recipe: $newFileName") // Symulacja dodawania przepisu
            }
            val onDeleteRecipe: (String) -> Unit = { fileName ->
                println("Deleting recipe: $fileName") // Symulacja usuwania przepisu
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
