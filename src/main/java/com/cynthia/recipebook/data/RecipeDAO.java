package com.cynthia.recipebook.data;

import com.cynthia.recipebook.model.Recipe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * RecipeDAO — Data Access Object.
 *
 * <p>This is the <b>only</b> class that runs SQL. The screens (controllers) call
 * these methods and never touch JDBC directly, which keeps the code organized
 * (separation of concerns). Every method uses try-with-resources and re-throws a
 * friendly {@link RecipeException} so the UI can show an alert instead of
 * crashing.</p>
 *
 * <p>It also performs the input validation the project requires: a recipe can not
 * be saved with a blank name, and two recipes can not share the same name.</p>
 */
public class RecipeDAO {

    // ---------------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------------

    /**
     * Adds a new recipe after validating the name is present and unique.
     *
     * @return the generated id
     * @throws RecipeException if the name is blank, already exists, or a DB error occurs
     */
    public long addRecipe(Recipe r) throws RecipeException {
        validate(r);
        if (nameExists(r.getName(), -1)) {
            throw new RecipeException("A recipe named \"" + r.getName().trim()
                    + "\" already exists. Please choose a different name.");
        }
        String sql = "INSERT INTO " + RecipeDbHelper.TABLE + " (" +
                "name, category, description, ingredients, instructions, rating, " +
                "time_minutes, difficulty, calories, protein, fat, carbs, badge, favorite, image_path) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = RecipeDbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getLong(1));
                }
            }
            return r.getId();
        } catch (SQLException e) {
            throw new RecipeException("Database error while adding the recipe.", e);
        }
    }

    // ---------------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------------

    public List<Recipe> getAllRecipes() throws RecipeException {
        return query("SELECT * FROM " + RecipeDbHelper.TABLE
                + " ORDER BY name COLLATE NOCASE ASC", null);
    }

    public List<Recipe> getRecipesByCategory(String category) throws RecipeException {
        return query("SELECT * FROM " + RecipeDbHelper.TABLE
                + " WHERE category = ? ORDER BY name COLLATE NOCASE ASC",
                new Object[]{category});
    }

    public List<Recipe> getFavorites() throws RecipeException {
        return query("SELECT * FROM " + RecipeDbHelper.TABLE
                + " WHERE favorite = 1 ORDER BY name COLLATE NOCASE ASC", null);
    }

    public List<Recipe> search(String text) throws RecipeException {
        return query("SELECT * FROM " + RecipeDbHelper.TABLE
                + " WHERE name LIKE ? ORDER BY name COLLATE NOCASE ASC",
                new Object[]{"%" + text + "%"});
    }

    public Recipe getById(long id) throws RecipeException {
        List<Recipe> list = query("SELECT * FROM " + RecipeDbHelper.TABLE
                + " WHERE id = ?", new Object[]{id});
        return list.isEmpty() ? null : list.get(0);
    }

    public int count() throws RecipeException {
        try (Connection conn = RecipeDbHelper.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + RecipeDbHelper.TABLE)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RecipeException("Database error while counting recipes.", e);
        }
    }

    // ---------------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------------

    /**
     * Updates an existing recipe (identified by its id).
     *
     * @throws RecipeException if nothing is selected, validation fails, or a DB error occurs
     */
    public void updateRecipe(Recipe r) throws RecipeException {
        if (r == null || r.getId() <= 0) {
            throw new RecipeException("Please select a recipe before updating.");
        }
        validate(r);
        if (nameExists(r.getName(), r.getId())) {
            throw new RecipeException("Another recipe named \"" + r.getName().trim()
                    + "\" already exists. Please choose a different name.");
        }
        String sql = "UPDATE " + RecipeDbHelper.TABLE + " SET " +
                "name=?, category=?, description=?, ingredients=?, instructions=?, rating=?, " +
                "time_minutes=?, difficulty=?, calories=?, protein=?, fat=?, carbs=?, badge=?, " +
                "favorite=?, image_path=? WHERE id=?";
        try (Connection conn = RecipeDbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, r);
            ps.setLong(16, r.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RecipeException("That recipe no longer exists and could not be updated.");
            }
        } catch (SQLException e) {
            throw new RecipeException("Database error while updating the recipe.", e);
        }
    }

    /** Flips the favorite flag for one recipe. */
    public void setFavorite(long id, boolean favorite) throws RecipeException {
        String sql = "UPDATE " + RecipeDbHelper.TABLE + " SET favorite=? WHERE id=?";
        try (Connection conn = RecipeDbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, favorite ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RecipeException("Database error while updating the favorite.", e);
        }
    }

    // ---------------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------------

    /**
     * Deletes a recipe by id.
     *
     * @throws RecipeException if nothing is selected or a DB error occurs
     */
    public void deleteRecipe(long id) throws RecipeException {
        if (id <= 0) {
            throw new RecipeException("Please select a recipe before deleting.");
        }
        String sql = "DELETE FROM " + RecipeDbHelper.TABLE + " WHERE id=?";
        try (Connection conn = RecipeDbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RecipeException("That recipe was already removed.");
            }
        } catch (SQLException e) {
            throw new RecipeException("Database error while deleting the recipe.", e);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Runs a SELECT and maps every row to a Recipe object. */
    private List<Recipe> query(String sql, Object[] args) throws RecipeException {
        List<Recipe> list = new ArrayList<>();
        try (Connection conn = RecipeDbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RecipeException("Database error while loading recipes.", e);
        }
        return list;
    }

    /** Validation shared by add and update. */
    private void validate(Recipe r) throws RecipeException {
        if (r == null) {
            throw new RecipeException("No recipe data was provided.");
        }
        if (r.getName() == null || r.getName().trim().isEmpty()) {
            throw new RecipeException("The recipe name can not be blank.");
        }
        if (r.getCategory() == null || r.getCategory().trim().isEmpty()) {
            throw new RecipeException("Please choose a category.");
        }
    }

    /** Returns true if a recipe with this name exists (ignoring the given id). */
    private boolean nameExists(String name, long ignoreId) throws RecipeException {
        String sql = "SELECT id FROM " + RecipeDbHelper.TABLE
                + " WHERE LOWER(name) = LOWER(?) AND id != ?";
        try (Connection conn = RecipeDbHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setLong(2, ignoreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RecipeException("Database error while checking the recipe name.", e);
        }
    }

    /** Binds a Recipe's 14 fields onto positions 1..14 of a prepared statement. */
    private void bind(PreparedStatement ps, Recipe r) throws SQLException {
        ps.setString(1, r.getName().trim());
        ps.setString(2, r.getCategory());
        ps.setString(3, r.getDescription());
        ps.setString(4, r.getIngredients());
        ps.setString(5, r.getInstructions());
        ps.setDouble(6, r.getRating());
        ps.setInt(7, r.getTimeMinutes());
        ps.setString(8, r.getDifficulty());
        ps.setInt(9, r.getCalories());
        ps.setInt(10, r.getProtein());
        ps.setInt(11, r.getFat());
        ps.setInt(12, r.getCarbs());
        ps.setString(13, r.getBadge());
        ps.setInt(14, r.isFavorite() ? 1 : 0);
        ps.setString(15, r.getImagePath());
    }

    /** Maps the current result-set row into a Recipe object. */
    private Recipe fromResultSet(ResultSet rs) throws SQLException {
        return new Recipe(
                rs.getLong(RecipeDbHelper.COL_ID),
                rs.getString(RecipeDbHelper.COL_NAME),
                rs.getString(RecipeDbHelper.COL_CATEGORY),
                rs.getString(RecipeDbHelper.COL_DESCRIPTION),
                rs.getString(RecipeDbHelper.COL_INGREDIENTS),
                rs.getString(RecipeDbHelper.COL_INSTRUCTIONS),
                rs.getDouble(RecipeDbHelper.COL_RATING),
                rs.getInt(RecipeDbHelper.COL_TIME),
                rs.getString(RecipeDbHelper.COL_DIFFICULTY),
                rs.getInt(RecipeDbHelper.COL_CALORIES),
                rs.getInt(RecipeDbHelper.COL_PROTEIN),
                rs.getInt(RecipeDbHelper.COL_FAT),
                rs.getInt(RecipeDbHelper.COL_CARBS),
                rs.getString(RecipeDbHelper.COL_BADGE),
                rs.getInt(RecipeDbHelper.COL_FAVORITE) == 1,
                rs.getString(RecipeDbHelper.COL_IMAGE)
        );
    }
}
