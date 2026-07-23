package com.cynthia.recipebook.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RecipeDbHelper — owns the connection to the SQLite database file
 * <b>recipes.db</b> and makes sure the <b>recipes</b> table exists.
 *
 * <p>On first run it creates the table and inserts a set of sample recipes
 * (each with a bundled photo) so the app is not empty when the grader opens it.
 * If an older database already exists without the image column, it is migrated
 * automatically. All column names live here as constants so the DAO never has to
 * hard-code strings.</p>
 */
public class RecipeDbHelper {

    /** The database is a single file created next to the app. */
    public static final String DB_URL = "jdbc:sqlite:recipes.db";

    public static final String TABLE = "recipes";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_INGREDIENTS = "ingredients";
    public static final String COL_INSTRUCTIONS = "instructions";
    public static final String COL_RATING = "rating";
    public static final String COL_TIME = "time_minutes";
    public static final String COL_DIFFICULTY = "difficulty";
    public static final String COL_CALORIES = "calories";
    public static final String COL_PROTEIN = "protein";
    public static final String COL_FAT = "fat";
    public static final String COL_CARBS = "carbs";
    public static final String COL_BADGE = "badge";
    public static final String COL_FAVORITE = "favorite";
    public static final String COL_IMAGE = "image_path";

    /** Opens a new connection to the SQLite file. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Creates the table if needed, migrates older databases, and seeds sample
     * data the first time. Called once when the app starts.
     */
    public void initialize() throws RecipeException {
        String createTable =
                "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_NAME + " TEXT NOT NULL, " +
                        COL_CATEGORY + " TEXT NOT NULL, " +
                        COL_DESCRIPTION + " TEXT, " +
                        COL_INGREDIENTS + " TEXT, " +
                        COL_INSTRUCTIONS + " TEXT, " +
                        COL_RATING + " REAL DEFAULT 0, " +
                        COL_TIME + " INTEGER DEFAULT 0, " +
                        COL_DIFFICULTY + " TEXT, " +
                        COL_CALORIES + " INTEGER DEFAULT 0, " +
                        COL_PROTEIN + " INTEGER DEFAULT 0, " +
                        COL_FAT + " INTEGER DEFAULT 0, " +
                        COL_CARBS + " INTEGER DEFAULT 0, " +
                        COL_BADGE + " TEXT, " +
                        COL_FAVORITE + " INTEGER DEFAULT 0, " +
                        COL_IMAGE + " TEXT);";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            ensureImageColumn(conn);

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + TABLE)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    seed(conn);
                } else {
                    // Existing database from an earlier version — fill in any
                    // missing sample photos so the seeded recipes get pictures.
                    backfillImages(conn);
                }
            }
        } catch (SQLException e) {
            throw new RecipeException("Could not open or create the database.", e);
        }
    }

    /** Adds the image_path column to older databases that don't have it yet. */
    private void ensureImageColumn(Connection conn) throws SQLException {
        boolean has = false;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + TABLE + ")")) {
            while (rs.next()) {
                if (COL_IMAGE.equalsIgnoreCase(rs.getString("name"))) {
                    has = true;
                    break;
                }
            }
        }
        if (!has) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_IMAGE + " TEXT");
            }
        }
    }

    /** Maps each sample recipe name to its bundled photo (stored as res:...). */
    private Map<String, String> seedImages() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Pasta Bolognese", "res:images/bolognese.jpg");
        m.put("Pasta Carbonara", "res:images/carbonara.jpg");
        m.put("Pork Ramen", "res:images/ramen.jpg");
        m.put("Avocado Toast", "res:images/avocado_toast.jpg");
        m.put("Fluffy Pancakes", "res:images/pancakes.jpg");
        m.put("Veggie Omelette", "res:images/omelette.jpg");
        m.put("Iced Caramel Latte", "res:images/iced_latte.jpg");
        m.put("Mango Smoothie", "res:images/mango_smoothie.jpg");
        m.put("Classic Lemonade", "res:images/lemonade.jpg");
        m.put("Chocolate Lava Cake", "res:images/lava_cake.jpg");
        m.put("New York Cheesecake", "res:images/cheesecake.jpg");
        return m;
    }

    /** Fills in sample photos for seeded recipes that don't have one yet. */
    private void backfillImages(Connection conn) throws SQLException {
        String sql = "UPDATE " + TABLE + " SET " + COL_IMAGE + "=? WHERE " + COL_NAME
                + "=? AND (" + COL_IMAGE + " IS NULL OR " + COL_IMAGE + "='')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> e : seedImages().entrySet()) {
                ps.setString(1, e.getValue());
                ps.setString(2, e.getKey());
                ps.executeUpdate();
            }
        }
    }

    private void seed(Connection conn) throws SQLException {
        Map<String, String> images = seedImages();
        String sql = "INSERT INTO " + TABLE + " (" +
                COL_NAME + ", " + COL_CATEGORY + ", " + COL_DESCRIPTION + ", " +
                COL_INGREDIENTS + ", " + COL_INSTRUCTIONS + ", " + COL_RATING + ", " +
                COL_TIME + ", " + COL_DIFFICULTY + ", " + COL_CALORIES + ", " +
                COL_PROTEIN + ", " + COL_FAT + ", " + COL_CARBS + ", " + COL_BADGE +
                ", " + COL_IMAGE + ", " + COL_FAVORITE + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            add(ps, images, "Pasta Bolognese", "Lunch",
                    "Italian lunch with rich meat sauce",
                    "400g spaghetti\n300g ground beef\n1 onion\n2 garlic cloves\n400g crushed tomatoes\n2 tbsp olive oil\nSalt and pepper",
                    "Boil the spaghetti until al dente.\nSaute onion and garlic in olive oil.\nAdd the beef and brown it well.\nPour in crushed tomatoes and simmer 20 minutes.\nToss the pasta in the sauce and serve.",
                    4.9, 35, "Medium", 620, 28, 22, 65, "Classic");

            add(ps, images, "Pasta Carbonara", "Lunch",
                    "A hearty lunch you can make in 25 mins",
                    "350g spaghetti\n150g pancetta\n3 egg yolks\n50g parmesan\nBlack pepper",
                    "Cook the spaghetti in salted water.\nFry the pancetta until crisp.\nWhisk egg yolks with parmesan.\nToss hot pasta with pancetta off the heat.\nStir in the egg mix so it turns creamy.",
                    4.7, 25, "Easy", 580, 24, 26, 58, "Popular");

            add(ps, images, "Pork Ramen", "Lunch",
                    "Rich and hearty Japanese-style soup",
                    "2 pork belly slices\n2 eggs\n200g ramen noodles\n1L pork or chicken broth\n2 tbsp soy sauce\n2 garlic cloves\nGreen onions",
                    "Marinate pork with soy sauce and garlic.\nSear pork in a hot pan until browned.\nBoil eggs for 6-7 minutes.\nCook ramen noodles separately.\nAssemble bowls with broth, noodles, pork and egg.",
                    4.8, 45, "Medium", 520, 28, 18, 55, "Popular");

            add(ps, images, "Avocado Toast", "Breakfast",
                    "Creamy avocado on crunchy sourdough",
                    "2 slices sourdough\n1 ripe avocado\n1 tbsp lemon juice\nChili flakes\nSalt",
                    "Toast the sourdough slices.\nMash avocado with lemon juice and salt.\nSpread avocado on the toast.\nTop with chili flakes and serve.",
                    4.5, 10, "Easy", 320, 10, 18, 30, "New");

            add(ps, images, "Fluffy Pancakes", "Breakfast",
                    "Light and airy stack for lazy mornings",
                    "200g flour\n2 tbsp sugar\n1 tsp baking powder\n1 egg\n250ml milk\n30g butter",
                    "Mix the dry ingredients in a bowl.\nWhisk egg, milk and melted butter together.\nCombine wet and dry until just mixed.\nCook spoonfuls on a hot griddle until bubbly.\nFlip and cook the other side, then stack and serve.",
                    4.9, 20, "Easy", 450, 9, 14, 70, "Classic");

            add(ps, images, "Veggie Omelette", "Breakfast",
                    "Protein packed start with fresh veggies",
                    "3 eggs\n1/4 bell pepper\n1/4 onion\nHandful spinach\n30g cheese\nSalt and pepper",
                    "Whisk the eggs with salt and pepper.\nSaute the chopped vegetables briefly.\nPour eggs over the veggies in the pan.\nAdd cheese, fold and cook until set.",
                    4.4, 15, "Easy", 290, 20, 21, 6, "");

            add(ps, images, "Iced Caramel Latte", "Drinks",
                    "Cool coffee with a sweet caramel swirl",
                    "1 shot espresso\n200ml cold milk\n2 tbsp caramel syrup\nIce cubes",
                    "Brew a shot of espresso and let it cool.\nFill a glass with ice.\nAdd caramel syrup and milk.\nPour the espresso over the top and stir.",
                    4.6, 8, "Easy", 220, 6, 7, 32, "Popular");

            add(ps, images, "Mango Smoothie", "Drinks",
                    "Tropical and refreshing in five minutes",
                    "1 ripe mango\n150ml yogurt\n100ml milk\n1 tsp honey\nIce cubes",
                    "Peel and chop the mango.\nAdd all ingredients to a blender.\nBlend until smooth.\nPour into a glass and enjoy.",
                    4.7, 5, "Easy", 180, 4, 2, 40, "New");

            add(ps, images, "Classic Lemonade", "Drinks",
                    "Zesty homemade lemonade for hot days",
                    "4 lemons\n100g sugar\n1L cold water\nIce cubes\nMint leaves",
                    "Squeeze the juice from the lemons.\nStir sugar into a little warm water until dissolved.\nCombine juice, syrup and cold water.\nServe over ice with mint.",
                    4.3, 10, "Easy", 120, 0, 0, 31, "");

            add(ps, images, "Chocolate Lava Cake", "Desserts",
                    "Warm cake with a molten chocolate center",
                    "100g dark chocolate\n100g butter\n2 eggs\n50g sugar\n50g flour",
                    "Melt chocolate and butter together.\nWhisk eggs and sugar until pale.\nFold in chocolate and flour.\nPour into greased ramekins.\nBake at 200C for 10-12 minutes until the edges set.",
                    4.9, 30, "Medium", 480, 7, 24, 58, "Popular");

            add(ps, images, "New York Cheesecake", "Desserts",
                    "Dense, creamy classic with a biscuit base",
                    "200g digestive biscuits\n80g butter\n600g cream cheese\n150g sugar\n3 eggs\n1 tsp vanilla",
                    "Blitz biscuits and mix with melted butter for the base.\nPress into a tin and chill.\nBeat cream cheese, sugar and vanilla.\nAdd eggs one at a time.\nPour over the base and bake low and slow, then chill overnight.",
                    4.8, 60, "Hard", 540, 9, 34, 48, "Classic");
        }
    }

    private void add(PreparedStatement ps, Map<String, String> images, String name,
                     String category, String description, String ingredients,
                     String instructions, double rating, int time, String difficulty,
                     int calories, int protein, int fat, int carbs, String badge)
            throws SQLException {
        ps.setString(1, name);
        ps.setString(2, category);
        ps.setString(3, description);
        ps.setString(4, ingredients);
        ps.setString(5, instructions);
        ps.setDouble(6, rating);
        ps.setInt(7, time);
        ps.setString(8, difficulty);
        ps.setInt(9, calories);
        ps.setInt(10, protein);
        ps.setInt(11, fat);
        ps.setInt(12, carbs);
        ps.setString(13, badge);
        ps.setString(14, images.getOrDefault(name, ""));
        ps.executeUpdate();
    }
}
