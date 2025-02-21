MyRecipes

MyRecipes to aplikacja do zarządzania przepisami kulinarnymi stworzona przy użyciu Jetpack Compose oraz Material3. Aplikacja umożliwia tworzenie, przeglądanie, edycję oraz usuwanie przepisów zapisanych jako pliki tekstowe.

Funkcje:

Lista Przepisów:

Przeglądaj listę dostępnych przepisów zapisanych w plikach tekstowych.

Dodawanie Przepisu:

Dodaj nowy przepis, podając jego nazwę. Aplikacja tworzy nowy plik przepisu i aktualizuje listę.

Usuwanie Przepisu:

W trybie usuwania możesz łatwo usunąć niechciane przepisy. System poprosi o potwierdzenie przed usunięciem.

Szczegóły Przepisu:

Otwórz przepis, aby zobaczyć jego szczegóły, w tym sekcje, składniki oraz dodatkowe wskazówki (tips).

Tryb Edycji:

W trybie edycji możesz:

Dodawać, modyfikować lub usuwać składniki w poszczególnych sekcjach.

Edytować istniejące sekcje oraz dodawać nowe.

Modyfikować wskazówki do przepisu.

System ostrzega o niezapisanych zmianach przy próbie wyjścia z trybu edycji.

Nawigacja:

Aplikacja korzysta z komponentu nawigacji Compose, umożliwiając płynne przejścia między ekranem głównym a ekranem szczegółów przepisu.

Technologie

Język: Kotlin

Framework UI: Jetpack Compose

Design: Material3

Nawigacja: Android Navigation Component (Compose)

Obsługa plików: Własna implementacja RecipesRepository do zarządzania przepisami

Struktura Projektu

MainActivity:

Główna aktywność, która:

Inicjalizuje interfejs użytkownika i włącza tryb edge-to-edge.

Konfiguruje nawigację między ekranami głównym (MainScreen) i szczegółów przepisu (DetailsScreen).

MainScreen:

Ekran główny prezentujący listę przepisów oraz umożliwiający:

Dodawanie nowego przepisu przez wyskakujące okno dialogowe.

Włączanie trybu usuwania (delete mode) oraz potwierdzanie usunięcia przepisu.

DetailsScreen:

Ekran szczegółów wybranego przepisu, w którym można:

Przeglądać i edytować zawartość przepisu, w tym sekcje i składniki.

Dodawać nowe sekcje i składniki.

Edytować wskazówki (tips) dla każdej sekcji.

Zarządzać stanem edycji oraz zapisywać zmiany przy powrocie.

RecipesRepository:

Klasa obsługująca operacje na plikach przepisu, w tym:

Pobieranie zawartości przepisu.

Tworzenie nowych plików przepisu.

Usuwanie istniejących przepisów.

Zapisywanie zmian w edytowanych przepisach.

parseRecipe:

Funkcja do parsowania zawartości pliku przepisu na strukturę danych, która jest wykorzystywana do wyświetlania poszczególnych sekcji i składników.
