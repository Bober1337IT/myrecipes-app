# MyRecipes

**MyRecipes** to aplikacja do zarządzania przepisami kulinarnymi, stworzona w Kotlinie z użyciem **Jetpack Compose** i **Material3**. Umożliwia tworzenie, przeglądanie, edycję oraz usuwanie przepisów zapisanych w formacie `.txt`.

---

## Funkcje

- **Lista przepisów**
  - Przegląd dostępnych przepisów zapisanych jako pliki `.txt`.

- **Dodawanie przepisu**
  - Tworzenie nowego przepisu po podaniu jego nazwy.
  - Automatyczne utworzenie pliku i aktualizacja listy.

- **Usuwanie przepisu**
  - Tryb usuwania z potwierdzeniem przed skasowaniem pliku.

- **Szczegóły przepisu**
  - Widok z podziałem na sekcje: składniki i wskazówki (tips).

- **Tryb edycji**
  - Dodawanie, edytowanie i usuwanie składników w poszczególnych sekcjach.
  - Edycja istniejących sekcji lub dodanie nowych.
  - Ostrzeżenie przy próbie wyjścia z niezapisanymi zmianami.

- **Nawigacja**
  - Płynna nawigacja między ekranami z użyciem Android Navigation Compose.

---

## Technologie

- **Język:** Kotlin  
- **UI:** Jetpack Compose  
- **Design:** Material3  
- **Nawigacja:** Android Navigation Compose  
- **Obsługa plików:** Własna implementacja – `RecipesRepository`

---

## Struktura projektu

### MainActivity
- Inicjalizuje interfejs użytkownika (edge-to-edge).
- Konfiguruje nawigację między ekranami.

### MainScreen
- Prezentuje listę przepisów.
- Dialog do dodawania nowych przepisów.
- Tryb usuwania z potwierdzeniem.
- Opcje importu i eksportu plików `.txt`.

### DetailsScreen
- Edycja i podgląd szczegółów przepisu.
- Zarządzanie sekcjami, składnikami i wskazówkami.
- Obsługa stanu edycji i zapisu.

### RecipesRepository
- Logika operacji na plikach:
  - Tworzenie
  - Zapisywanie
  - Usuwanie
  - Pobieranie zawartości

### parseRecipe
- Parsowanie pliku przepisu do wewnętrznej struktury danych używanej w interfejsie.

---
