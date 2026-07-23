package com.cynthia.recipebook.data;

/**
 * RecipeException — a single checked exception used for both validation problems
 * (blank name, duplicate name) and database problems.
 *
 * <p>Because it is checked, the compiler forces every screen that calls the DAO
 * to handle it, which is exactly the error handling the project requires. The
 * message is always user-friendly so it can be shown directly in an alert.</p>
 */
public class RecipeException extends Exception {
    public RecipeException(String message) {
        super(message);
    }

    public RecipeException(String message, Throwable cause) {
        super(message, cause);
    }
}
