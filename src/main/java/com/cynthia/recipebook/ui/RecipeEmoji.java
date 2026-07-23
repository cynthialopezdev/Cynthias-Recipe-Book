package com.cynthia.recipebook.ui;

import com.cynthia.recipebook.model.Recipe;

/**
 * Small helper that picks a food emoji for a recipe so every card has a fun,
 * colorful "photo" without shipping any image files. It first looks for a
 * keyword in the recipe name, then falls back to a default for the category.
 */
public final class RecipeEmoji {

    private RecipeEmoji() { }

    public static String forRecipe(Recipe recipe) {
        if (recipe == null) return "🍽";
        String n = recipe.getName() == null ? "" : recipe.getName().toLowerCase();

        if (n.contains("ramen") || n.contains("noodle")) return "🍜";      // 🍜
        if (n.contains("pasta") || n.contains("spaghetti") || n.contains("carbonara")
                || n.contains("bolognese")) return "🍝";                    // 🍝
        if (n.contains("avocado")) return "🥑";                             // 🥑
        if (n.contains("toast") || n.contains("bread")) return "🍞";        // 🍞
        if (n.contains("pancake")) return "🥞";                             // 🥞
        if (n.contains("omelette") || n.contains("omelet") || n.contains("egg")) return "🍳"; // 🍳
        if (n.contains("latte") || n.contains("coffee") || n.contains("espresso")) return "☕"; // ☕
        if (n.contains("smoothie") || n.contains("juice") || n.contains("shake")) return "🥤"; // 🥤
        if (n.contains("lemonade") || n.contains("lemon")) return "🍋";     // 🍋
        if (n.contains("cheesecake") || n.contains("cake")) return "🍰";    // 🍰
        if (n.contains("soup")) return "🍲";                               // 🍲
        if (n.contains("salad")) return "🥗";                              // 🥗
        if (n.contains("pizza")) return "🍕";                              // 🍕
        if (n.contains("burger")) return "🍔";                             // 🍔
        if (n.contains("rice")) return "🍚";                               // 🍚
        if (n.contains("chicken")) return "🍗";                            // 🍗
        if (n.contains("taco")) return "🌮";                               // 🌮
        if (n.contains("cookie")) return "🍪";                             // 🍪

        String c = recipe.getCategory() == null ? "" : recipe.getCategory();
        switch (c) {
            case "Breakfast": return "🍳"; // 🍳
            case "Drinks":    return "🥤"; // 🥤
            case "Desserts":  return "🍰"; // 🍰
            case "Lunch":
            default:          return "🍽"; // 🍽️
        }
    }
}
