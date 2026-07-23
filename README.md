# Cynthia's Recipe Book 🍳

A trendy desktop recipe book app built with **JavaFX** and an **SQLite** database.
Save, browse, edit, and delete your favorite recipes across four categories, mark
favorites, and view full details with ingredients, numbered instructions, and
nutrition info.

The app uses a warm, spiced/autumn color palette (paprika, honey, clay, and cream)
with rounded recipe cards, category tabs, and a clean search bar.

---

## ✨ Features

- **Full CRUD** — Create, Read, Update, and Delete recipes.
- **SQLite database** (`recipes.db`) with one `recipes` table, preloaded with 11 sample recipes on first launch.
- **Category tabs with photo icons** — All, Breakfast, Lunch, Drinks, and Desserts, each with its own image that magnifies on hover.
- **Recipe photos** — every recipe shows a real photo, and you can attach your own image when adding or editing a recipe.
- **Search** — filter recipes by name as you type.
- **Favorites** — click the heart on any card (or in the detail view) to save it; the heart fills red with a pop animation, and a Favorites tab lists them.
- **Colored badges** — Classic, Popular, and New badges highlight recipes.
- **Recipe detail view** — large photo, calories in the title, nutrition table (protein / fat / carbs), ingredient list, and numbered cooking steps.
- **Add / Edit form** with:
  - Category, difficulty, and badge shown as **dropdowns** (so they can't be typed wrong).
  - **Input validation** — a recipe can't be saved with a blank name, and duplicate names are rejected.
- **Exception handling** — every database action is wrapped in `try/catch` and shows a friendly
  alert dialog instead of crashing.
- **Warm, themed interface** — a consistent spiced/autumn palette applied through a JavaFX stylesheet.

Every visible button works: New Recipe, Save, Clear, Cancel, Edit, Delete, the favorite
hearts, the category tabs, and the back buttons.

---

## 🧱 Project structure (Object-Oriented design)

```
src/main/java/com/cynthia/recipebook/
├── MainApp.java                  # JavaFX Application + controller (views + button logic)
├── model/
│   └── Recipe.java               # Data class holding one recipe (OOP model)
├── data/
│   ├── RecipeDbHelper.java       # Opens recipes.db, creates the table, seeds samples
│   ├── RecipeDAO.java            # The ONLY class that runs SQL (CRUD + validation)
│   └── RecipeException.java      # Friendly checked exception for validation/DB errors
└── ui/
    └── RecipeEmoji.java          # Emoji fallback when a recipe has no photo

src/main/resources/com/cynthia/recipebook/
├── styles.css                    # Warm spiced/autumn theme
└── images/                       # Recipe photos + category tab icons
```

The three roles required by the assignment map to:
`Recipe` (data), `RecipeDAO` (database access), and `MainApp` (controller).

---

## 🛠️ Technologies used

- **Java 17**
- **JavaFX 21** (controls + FXML) for the GUI
- **SQLite** via the `sqlite-jdbc` driver (JDBC)
- **Maven** for building and running

---

## ▶️ How to compile and run

### Option A — Maven (recommended, one command)

You need **JDK 17 or newer** and **Maven** installed.

```bash
cd CynthiasRecipeBook-JavaFX
mvn clean javafx:run
```

Maven downloads JavaFX and the SQLite driver automatically the first time, then
launches the app. The database `recipes.db` is created in the project folder on first
run and preloaded with sample recipes, so the app is never empty.

### Option B — IntelliJ IDEA / Eclipse

1. **File → Open** and select the `CynthiasRecipeBook-JavaFX` folder (it's a Maven project).
2. Let the IDE import the Maven dependencies.
3. Run the Maven goal **`javafx:run`** (Maven tool window → Plugins → javafx → javafx:run),
   or add a run configuration for that goal.

> Running `MainApp` directly from the IDE also works but requires JavaFX VM options.
> Using the `javafx:run` Maven goal is the simplest and needs no extra setup.

---

## 👤 Author

**Cynthia Lopez** — Java Programming Final Project

---

## 🔗 Submission links

- **GitHub repository:** _add your public repo link here_
- **Demo video (YouTube, unlisted):** _add your video link here_
