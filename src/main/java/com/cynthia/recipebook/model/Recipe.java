package com.cynthia.recipebook.model;

import java.util.Locale;

/**
 * Recipe (Model / OOP data class).
 *
 * <p>Holds all the data for a single recipe. This class knows nothing about the
 * database or the screens — it is a plain Java object that the rest of the app
 * passes around. Keeping the data in one class is the Object-Oriented part of the
 * project: {@link com.cynthia.recipebook.data.RecipeDAO} turns database rows into
 * {@code Recipe} objects and back again.</p>
 */
public class Recipe {

    /** Category options shown in the dropdown and the top tab bar. */
    public static final String[] CATEGORIES = {"Breakfast", "Lunch", "Drinks", "Desserts"};

    /** Difficulty options shown in the dropdown. */
    public static final String[] DIFFICULTIES = {"Easy", "Medium", "Hard"};

    /** Badge options ("" means no badge is shown on the card). */
    public static final String[] BADGES = {"", "Classic", "Popular", "New"};

    private long id;
    private String name;
    private String category;
    private String description;
    private String ingredients;
    private String instructions;
    private double rating;
    private int timeMinutes;
    private String difficulty;
    private int calories;
    private int protein;
    private int fat;
    private int carbs;
    private String badge;
    private boolean favorite;
    private String imagePath;

    /** Empty constructor — used when the user is creating a brand new recipe. */
    public Recipe() {
        this.id = -1;
        this.name = "";
        this.category = CATEGORIES[0];
        this.description = "";
        this.ingredients = "";
        this.instructions = "";
        this.rating = 0.0;
        this.timeMinutes = 0;
        this.difficulty = DIFFICULTIES[0];
        this.calories = 0;
        this.protein = 0;
        this.fat = 0;
        this.carbs = 0;
        this.badge = "";
        this.favorite = false;
        this.imagePath = "";
    }

    /** Full constructor — used when reading a row back out of the database. */
    public Recipe(long id, String name, String category, String description,
                  String ingredients, String instructions, double rating,
                  int timeMinutes, String difficulty, int calories, int protein,
                  int fat, int carbs, String badge, boolean favorite, String imagePath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.rating = rating;
        this.timeMinutes = timeMinutes;
        this.difficulty = difficulty;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
        this.badge = badge;
        this.favorite = favorite;
        this.imagePath = imagePath;
    }

    // ---- Getters and setters (encapsulation) ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTimeMinutes() { return timeMinutes; }
    public void setTimeMinutes(int timeMinutes) { this.timeMinutes = timeMinutes; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public int getProtein() { return protein; }
    public void setProtein(int protein) { this.protein = protein; }

    public int getFat() { return fat; }
    public void setFat(int fat) { this.fat = fat; }

    public int getCarbs() { return carbs; }
    public void setCarbs(int carbs) { this.carbs = carbs; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    /** Convenience helper used by the list screen ("35 mins"). */
    public String getTimeLabel() {
        return timeMinutes + " mins";
    }

    /** Convenience helper used by the list screen ("4.9"). */
    public String getRatingLabel() {
        return String.format(Locale.US, "%.1f", rating);
    }

    @Override
    public String toString() {
        return name + " (" + category + ")";
    }
}
