package com.cynthia.recipebook;

import com.cynthia.recipebook.data.RecipeDAO;
import com.cynthia.recipebook.data.RecipeDbHelper;
import com.cynthia.recipebook.data.RecipeException;
import com.cynthia.recipebook.model.Recipe;
import com.cynthia.recipebook.ui.RecipeEmoji;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MainApp — the JavaFX application and main controller.
 *
 * <p>It builds three views inside one window and swaps them in the center:
 * the browse view (cards + tabs + search), the detail view, and the add/edit
 * form. All database work goes through {@link RecipeDAO} and is wrapped in
 * try/catch so any problem shows an alert instead of crashing the app.</p>
 */
public class MainApp extends Application {

    private static final String ALL = "All";
    private static final String[] BADGE_DISPLAY = {"None", "Classic", "Popular", "New"};

    private final RecipeDAO dao = new RecipeDAO();

    private BorderPane root;
    private Node browseView;
    private FlowPane cardsPane;
    private TextField searchField;
    private Label countLabel;
    private final List<Label> tabLabels = new ArrayList<>();
    private Label favTab;

    private String currentCategory = ALL;
    private boolean showingFavorites = false;

    @Override
    public void start(Stage stage) {
        // Create the database and seed sample data (guarded).
        try {
            new RecipeDbHelper().initialize();
        } catch (RecipeException e) {
            showError(e.getMessage());
        }

        root = new BorderPane();
        browseView = buildBrowseView();

        Scene scene = new Scene(root, 1140, 760);
        scene.getStylesheets().add(
                getClass().getResource("/com/cynthia/recipebook/styles.css").toExternalForm());

        stage.setTitle("Cynthia's Recipe Book");
        stage.setScene(scene);
        stage.setMinWidth(940);
        stage.setMinHeight(660);
        stage.show();

        showBrowse();
    }

    // =====================================================================
    // BROWSE VIEW
    // =====================================================================

    private Node buildBrowseView() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(22, 26, 10, 26));

        // Row 1: title + New Recipe button
        Label title = new Label("Cynthia's Recipe Book");
        title.getStyleClass().add("app-title");
        Region s1 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        Button newBtn = new Button("+  New Recipe");
        newBtn.getStyleClass().add("primary-button");
        newBtn.setOnAction(e -> showForm(null));
        HBox row1 = new HBox(12, title, s1, newBtn);
        row1.setAlignment(Pos.CENTER_LEFT);

        // Row 2: category tabs + favorites tab
        HBox tabBar = new HBox(18);
        tabBar.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(tabBar, new Insets(16, 0, 6, 0));
        tabLabels.clear();
        List<String> cats = new ArrayList<>();
        cats.add(ALL);
        for (String c : Recipe.CATEGORIES) cats.add(c);
        for (String cat : cats) {
            VBox tab = new VBox(4);
            tab.setAlignment(Pos.CENTER);
            tab.getChildren().add(categoryIcon(cat));
            Label tabText = new Label(cat);
            tabText.getStyleClass().add("tab-button");
            tab.getChildren().add(tabText);
            tabLabels.add(tabText);
            tab.setOnMouseClicked(e -> {
                currentCategory = cat;
                showingFavorites = false;
                styleTabs();
                loadCards();
            });
            hoverMagnify(tab);
            tabBar.getChildren().add(tab);
        }
        Region s2 = new Region();
        HBox.setHgrow(s2, Priority.ALWAYS);
        favTab = new Label("♥ Favorites");
        favTab.getStyleClass().add("tab-button");
        favTab.setOnMouseClicked(e -> {
            showingFavorites = true;
            styleTabs();
            loadCards();
        });
        tabBar.getChildren().addAll(s2, favTab);

        // Row 3: count + search
        countLabel = new Label("Recipes");
        countLabel.getStyleClass().add("count-title");
        Region s3 = new Region();
        HBox.setHgrow(s3, Priority.ALWAYS);
        searchField = new TextField();
        searchField.setPromptText("Search recipes");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, old, val) -> loadCards());
        HBox row3 = new HBox(12, countLabel, s3, searchField);
        row3.setAlignment(Pos.CENTER_LEFT);

        container.getChildren().addAll(row1, tabBar, row3);

        // Cards area
        cardsPane = new FlowPane();
        cardsPane.setHgap(18);
        cardsPane.setVgap(18);
        cardsPane.setPadding(new Insets(6, 26, 26, 26));

        ScrollPane scroll = new ScrollPane(cardsPane);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        BorderPane view = new BorderPane();
        view.setTop(container);
        view.setCenter(scroll);
        styleTabs();
        return view;
    }

    private void styleTabs() {
        for (Label tab : tabLabels) {
            tab.getStyleClass().remove("tab-active");
            if (!showingFavorites && tab.getText().equals(currentCategory)) {
                tab.getStyleClass().add("tab-active");
            }
        }
        if (favTab != null) {
            favTab.getStyleClass().remove("tab-active");
            if (showingFavorites) favTab.getStyleClass().add("tab-active");
        }
    }

    private void showBrowse() {
        root.setCenter(browseView);
        loadCards();
    }

    private void loadCards() {
        try {
            List<Recipe> recipes;
            if (showingFavorites) {
                recipes = dao.getFavorites();
                countLabel.setText("Favorites");
            } else {
                String q = searchField.getText() == null ? "" : searchField.getText().trim();
                if (!q.isEmpty()) {
                    recipes = filterByCategory(dao.search(q));
                } else if (ALL.equals(currentCategory)) {
                    recipes = dao.getAllRecipes();
                } else {
                    recipes = dao.getRecipesByCategory(currentCategory);
                }
                String label = ALL.equals(currentCategory) ? "All recipes" : currentCategory;
                countLabel.setText(recipes.size() + "  " + label);
            }

            cardsPane.getChildren().clear();
            if (recipes.isEmpty()) {
                Label empty = new Label(showingFavorites
                        ? "No favorites yet. Click the heart on a recipe to save it here."
                        : "No recipes found. Click \"New Recipe\" to add one!");
                empty.getStyleClass().add("subtitle");
                cardsPane.getChildren().add(empty);
            } else {
                for (Recipe r : recipes) {
                    cardsPane.getChildren().add(buildCard(r));
                }
            }
        } catch (RecipeException e) {
            showError(e.getMessage());
        }
    }

    private List<Recipe> filterByCategory(List<Recipe> list) {
        if (ALL.equals(currentCategory)) return list;
        List<Recipe> out = new ArrayList<>();
        for (Recipe r : list) {
            if (currentCategory.equals(r.getCategory())) out.add(r);
        }
        return out;
    }

    /** Icon shown above a category tab — the bundled photo, or an emoji fallback. */
    private Node categoryIcon(String category) {
        String base = "/com/cynthia/recipebook/images/cat_" + category.toLowerCase();
        for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
            URL u = getClass().getResource(base + ext);
            if (u != null) {
                // Load the image at full resolution and let the ImageView scale it
                // down — this stays crisp on high-DPI (Retina) displays.
                ImageView iv = new ImageView(new Image(u.toExternalForm()));
                iv.setFitHeight(64);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                return iv;
            }
        }
        Label emoji = new Label(categoryEmoji(category));
        emoji.setStyle("-fx-font-size: 42px;");
        return emoji;
    }

    private String categoryEmoji(String category) {
        switch (category) {
            case "Breakfast": return "🍳";
            case "Lunch":     return "🍜";
            case "Drinks":    return "🥤";
            case "Desserts":  return "🧁";
            default:          return "🍽";
        }
    }

    /** Gently scales a node up on hover and back down on exit. */
    private void hoverMagnify(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setToX(1.22);
            st.setToY(1.22);
            st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    /** Applies the colored badge style based on the badge value. */
    private void styleBadge(Label badge, String value) {
        if ("Classic".equals(value)) {
            badge.getStyleClass().add("badge-classic");
        } else if ("Popular".equals(value)) {
            badge.getStyleClass().add("badge-popular");
        } else if ("New".equals(value)) {
            badge.getStyleClass().add("badge-new");
        }
    }

    private Node buildCard(Recipe recipe) {
        VBox card = new VBox(8);
        card.getStyleClass().add("recipe-card");
        card.setPrefWidth(250);
        card.setMaxWidth(250);

        // Image panel (real photo, or an emoji fallback), with badge + favorite heart
        StackPane panel = imagePanel(recipe, 140, false);

        if (recipe.getBadge() != null && !recipe.getBadge().trim().isEmpty()) {
            Label badge = new Label(recipe.getBadge());
            badge.getStyleClass().add("badge");
            styleBadge(badge, recipe.getBadge());
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(8));
            panel.getChildren().add(badge);
        }

        StackPane fav = heartButton(recipe.isFavorite());
        StackPane.setAlignment(fav, Pos.TOP_LEFT);
        StackPane.setMargin(fav, new Insets(8));
        fav.setOnMouseClicked(e -> {
            e.consume();
            toggleCardFavorite(recipe, fav);
        });
        panel.getChildren().add(fav);

        Label name = new Label(recipe.getName());
        name.getStyleClass().add("card-name");
        name.setWrapText(true);

        Label desc = new Label(recipe.getDescription());
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);

        HBox chips = new HBox(6,
                chip("★ " + recipe.getRatingLabel(), "chip-rating"),
                chip("🕒 " + recipe.getTimeLabel(), "chip-time"),
                chip("📊 " + recipe.getDifficulty(), "chip-difficulty"));

        card.getChildren().addAll(panel, name, desc, chips);
        card.setOnMouseClicked(e -> showDetail(recipe));
        return card;
    }

    private Label chip(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("chip", styleClass);
        // Keep the chip at its full width so the text never truncates to "...".
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private void toggleCardFavorite(Recipe recipe, StackPane heart) {
        try {
            boolean nowFavorite = !recipe.isFavorite();
            dao.setFavorite(recipe.getId(), nowFavorite);
            recipe.setFavorite(nowFavorite);
            setHeartVisual(heart, nowFavorite);
            pop(heart);
            // In the Favorites tab, un-favoriting removes the card — reload after
            // the little pop animation has had a moment to play.
            if (showingFavorites) {
                PauseTransition pause = new PauseTransition(Duration.millis(280));
                pause.setOnFinished(ev -> loadCards());
                pause.play();
            }
        } catch (RecipeException e) {
            showError(e.getMessage());
        }
    }

    /** Builds the round favorite button with a drawn heart (outline or red-filled). */
    private StackPane heartButton(boolean favorite) {
        SVGPath path = new SVGPath();
        path.setContent("M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 "
                + "7.5 3c1.74 0 3.41 0.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 "
                + "22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");
        path.setScaleX(0.78);
        path.setScaleY(0.78);
        StackPane circle = new StackPane(path);
        circle.getStyleClass().add("fav-button");
        circle.setMinSize(38, 38);
        circle.setPrefSize(38, 38);
        circle.setMaxSize(38, 38);
        setHeartVisual(circle, favorite);
        return circle;
    }

    /** Outline heart when off; solid red heart on a rose circle when favorited. */
    private void setHeartVisual(StackPane circle, boolean favorite) {
        circle.getStyleClass().remove("fav-active");
        SVGPath path = (SVGPath) circle.getChildren().get(0);
        if (favorite) {
            circle.getStyleClass().add("fav-active");
            path.setFill(Color.web("#C0392B"));
            path.setStroke(Color.TRANSPARENT);
        } else {
            path.setFill(Color.TRANSPARENT);
            path.setStroke(Color.web("#8A7057"));
            path.setStrokeWidth(2.0);
        }
    }

    /** A quick "pop" bounce used on the favorite hearts. */
    private void pop(javafx.scene.Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(130), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.4);
        st.setToY(1.4);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    // =====================================================================
    // DETAIL VIEW
    // =====================================================================

    private void showDetail(Recipe recipe) {
        VBox content = new VBox(14);
        content.setPadding(new Insets(24, 30, 30, 30));
        content.setMaxWidth(760);

        // Top bar
        Button back = iconButton("←");
        back.setOnAction(e -> showBrowse());
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        StackPane fav = heartButton(recipe.isFavorite());
        fav.setOnMouseClicked(e -> {
            try {
                boolean now = !recipe.isFavorite();
                dao.setFavorite(recipe.getId(), now);
                recipe.setFavorite(now);
                setHeartVisual(fav, now);
                pop(fav);
            } catch (RecipeException ex) {
                showError(ex.getMessage());
            }
        });
        HBox top = new HBox(10, back, sp, fav);
        top.setAlignment(Pos.CENTER_LEFT);

        // Image (real photo, or an emoji fallback)
        StackPane panel = imagePanel(recipe, 240, true);
        if (recipe.getBadge() != null && !recipe.getBadge().trim().isEmpty()) {
            Label badge = new Label(recipe.getBadge());
            badge.getStyleClass().add("badge");
            styleBadge(badge, recipe.getBadge());
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(12));
            panel.getChildren().add(badge);
        }

        String titleText = recipe.getName();
        if (recipe.getCalories() > 0) titleText += ", " + recipe.getCalories() + " Kcal";
        Label title = new Label(titleText);
        title.getStyleClass().add("detail-title");
        title.setWrapText(true);

        Label desc = new Label(recipe.getDescription());
        desc.getStyleClass().add("subtitle");
        desc.setWrapText(true);

        HBox chips = new HBox(8,
                chip("★ " + recipe.getRatingLabel(), "chip-rating"),
                chip("🕒 " + recipe.getTimeLabel(), "chip-time"),
                chip("📊 " + recipe.getDifficulty(), "chip-difficulty"));

        HBox nutrition = new HBox(10,
                nutritionCell("protein", recipe.getProtein() + " g"),
                nutritionCell("fat", recipe.getFat() + " g"),
                nutritionCell("carbs", recipe.getCarbs() + " g"));

        Label ingHeading = new Label("Ingredients:");
        ingHeading.getStyleClass().add("section-heading");
        VBox ingredients = new VBox(4);
        for (String line : splitLines(recipe.getIngredients())) {
            Label l = new Label("•  " + line);
            l.getStyleClass().add("body-text");
            l.setWrapText(true);
            ingredients.getChildren().add(l);
        }
        if (ingredients.getChildren().isEmpty()) {
            ingredients.getChildren().add(bodyText("No ingredients listed."));
        }

        Label insHeading = new Label("Instructions:");
        insHeading.getStyleClass().add("section-heading");
        VBox instructions = new VBox(8);
        int step = 1;
        for (String line : splitLines(recipe.getInstructions())) {
            instructions.getChildren().add(stepRow(step++, line));
        }
        if (instructions.getChildren().isEmpty()) {
            instructions.getChildren().add(bodyText("No instructions listed."));
        }

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("primary-button");
        editBtn.setOnAction(e -> showForm(recipe));
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("outline-button");
        deleteBtn.setOnAction(e -> confirmDelete(recipe));
        HBox actions = new HBox(12, editBtn, deleteBtn);
        actions.setPadding(new Insets(10, 0, 0, 0));

        content.getChildren().addAll(top, panel, title, desc, chips, nutrition,
                ingHeading, ingredients, insHeading, instructions, actions);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        root.setCenter(scroll);
    }

    private VBox nutritionCell(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("nutrition-label");
        Label v = new Label(value);
        v.getStyleClass().add("nutrition-value");
        VBox cell = new VBox(2, l, v);
        cell.getStyleClass().add("nutrition-cell");
        HBox.setHgrow(cell, Priority.ALWAYS);
        cell.setMaxWidth(Double.MAX_VALUE);
        return cell;
    }

    private HBox stepRow(int number, String text) {
        Label num = new Label(String.valueOf(number));
        num.getStyleClass().add("step-number");
        Label body = new Label(text);
        body.getStyleClass().add("body-text");
        body.setWrapText(true);
        HBox.setHgrow(body, Priority.ALWAYS);
        HBox row = new HBox(12, num, body);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void confirmDelete(Recipe recipe) {
        Alert alert = new Alert(AlertType.CONFIRMATION,
                "Delete \"" + recipe.getName() + "\"? This can not be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Delete recipe");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                dao.deleteRecipe(recipe.getId());
                showBrowse();
            } catch (RecipeException e) {
                showError(e.getMessage());
            }
        }
    }

    // =====================================================================
    // ADD / EDIT FORM
    // =====================================================================

    private void showForm(Recipe existing) {
        boolean editMode = existing != null;

        VBox content = new VBox(14);
        content.setPadding(new Insets(24, 30, 30, 30));
        content.setMaxWidth(720);

        Button back = iconButton("←");
        back.setOnAction(e -> {
            if (editMode) showDetail(existing); else showBrowse();
        });
        Label heading = new Label(editMode ? "Edit Recipe" : "New Recipe");
        heading.getStyleClass().add("app-title");
        HBox top = new HBox(14, back, heading);
        top.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = textField("e.g. Tomato Soup");
        ComboBox<String> categoryCombo = combo(Recipe.CATEGORIES);
        TextField descField = textField("Short description");
        TextArea ingredientsArea = textArea("One ingredient per line");
        TextArea instructionsArea = textArea("One step per line");
        TextField ratingField = textField("0 - 5");
        TextField timeField = textField("minutes");
        ComboBox<String> difficultyCombo = combo(Recipe.DIFFICULTIES);
        ComboBox<String> badgeCombo = combo(BADGE_DISPLAY);
        TextField caloriesField = textField("kcal");
        TextField proteinField = textField("g");
        TextField fatField = textField("g");
        TextField carbsField = textField("g");

        // --- Photo picker ---
        final String[] imagePathHolder = { editMode ? existing.getImagePath() : "" };
        StackPane photoPreview = new StackPane();
        photoPreview.setMinSize(220, 130);
        photoPreview.setPrefSize(220, 130);
        photoPreview.setMaxSize(220, 130);
        Button choosePhotoBtn = new Button("Choose photo…");
        choosePhotoBtn.getStyleClass().add("outline-button");
        Button useEmojiBtn = new Button("Use emoji");
        useEmojiBtn.getStyleClass().add("outline-button");
        Runnable refreshPreview = () -> renderImagePreview(photoPreview,
                imagePathHolder[0], nameField.getText(), categoryCombo.getValue());
        choosePhotoBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose a photo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File file = fc.showOpenDialog(
                    photoPreview.getScene() == null ? null : photoPreview.getScene().getWindow());
            if (file != null) {
                imagePathHolder[0] = importImage(file);
                refreshPreview.run();
            }
        });
        useEmojiBtn.setOnAction(e -> {
            imagePathHolder[0] = "";
            refreshPreview.run();
        });
        Label photoLabel = new Label("Photo");
        photoLabel.getStyleClass().add("form-label");
        VBox photoButtons = new VBox(8, choosePhotoBtn, useEmojiBtn);
        photoButtons.setAlignment(Pos.CENTER_LEFT);
        HBox photoRow = new HBox(14, photoPreview, photoButtons);
        photoRow.setAlignment(Pos.CENTER_LEFT);
        VBox photoBox = new VBox(6, photoLabel, photoRow);

        // Pre-fill when editing
        if (editMode) {
            nameField.setText(existing.getName());
            categoryCombo.setValue(existing.getCategory());
            descField.setText(existing.getDescription());
            ingredientsArea.setText(existing.getIngredients());
            instructionsArea.setText(existing.getInstructions());
            ratingField.setText(String.valueOf(existing.getRating()));
            timeField.setText(String.valueOf(existing.getTimeMinutes()));
            difficultyCombo.setValue(existing.getDifficulty());
            badgeCombo.getSelectionModel().select(indexOf(Recipe.BADGES, existing.getBadge()));
            caloriesField.setText(String.valueOf(existing.getCalories()));
            proteinField.setText(String.valueOf(existing.getProtein()));
            fatField.setText(String.valueOf(existing.getFat()));
            carbsField.setText(String.valueOf(existing.getCarbs()));
        } else {
            categoryCombo.getSelectionModel().selectFirst();
            difficultyCombo.getSelectionModel().selectFirst();
            badgeCombo.getSelectionModel().selectFirst();
        }
        refreshPreview.run();

        HBox ratingTime = new HBox(12,
                labeled("Rating (0-5)", ratingField, true),
                labeled("Time (minutes)", timeField, true));
        HBox macros = new HBox(12,
                labeled("Protein (g)", proteinField, true),
                labeled("Fat (g)", fatField, true),
                labeled("Carbs (g)", carbsField, true));

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("primary-button");
        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("outline-button");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("outline-button");
        HBox buttons = new HBox(12, saveBtn, clearBtn, cancelBtn);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        clearBtn.setOnAction(e -> {
            nameField.clear();
            descField.clear();
            ingredientsArea.clear();
            instructionsArea.clear();
            ratingField.clear();
            timeField.clear();
            caloriesField.clear();
            proteinField.clear();
            fatField.clear();
            carbsField.clear();
            categoryCombo.getSelectionModel().selectFirst();
            difficultyCombo.getSelectionModel().selectFirst();
            badgeCombo.getSelectionModel().selectFirst();
            nameField.requestFocus();
        });
        cancelBtn.setOnAction(e -> {
            if (editMode) showDetail(existing); else showBrowse();
        });

        saveBtn.setOnAction(e -> {
            Recipe r = editMode ? existing : new Recipe();
            r.setName(nameField.getText() == null ? "" : nameField.getText());
            r.setCategory(categoryCombo.getValue());
            r.setDescription(text(descField));
            r.setIngredients(text(ingredientsArea));
            r.setInstructions(text(instructionsArea));
            r.setRating(clampRating(parseDouble(ratingField.getText())));
            r.setTimeMinutes(parseInt(timeField.getText()));
            r.setDifficulty(difficultyCombo.getValue());
            r.setCalories(parseInt(caloriesField.getText()));
            r.setProtein(parseInt(proteinField.getText()));
            r.setFat(parseInt(fatField.getText()));
            r.setCarbs(parseInt(carbsField.getText()));
            r.setBadge(Recipe.BADGES[badgeCombo.getSelectionModel().getSelectedIndex()]);
            r.setImagePath(imagePathHolder[0]);
            try {
                if (editMode) {
                    dao.updateRecipe(r);
                    info("Recipe updated.");
                } else {
                    dao.addRecipe(r);
                    info("Recipe added.");
                }
                showBrowse();
            } catch (RecipeException ex) {
                showError(ex.getMessage());
            }
        });

        content.getChildren().addAll(top,
                labeled("Recipe name", nameField, false),
                labeled("Category", categoryCombo, false),
                labeled("Short description", descField, false),
                photoBox,
                labeled("Ingredients (one per line)", ingredientsArea, false),
                labeled("Instructions (one step per line)", instructionsArea, false),
                ratingTime,
                labeled("Difficulty", difficultyCombo, false),
                labeled("Badge", badgeCombo, false),
                labeled("Calories", caloriesField, false),
                macros,
                buttons);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        root.setCenter(scroll);
    }

    // =====================================================================
    // Small UI helpers
    // =====================================================================

    private VBox labeled(String label, Node field, boolean grow) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        VBox box = new VBox(4, l, field);
        if (grow) {
            HBox.setHgrow(box, Priority.ALWAYS);
            box.setMaxWidth(Double.MAX_VALUE);
        }
        if (field instanceof Region) {
            ((Region) field).setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private TextField textField(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        t.getStyleClass().add("text-input");
        t.setMaxWidth(Double.MAX_VALUE);
        return t;
    }

    private TextArea textArea(String prompt) {
        TextArea t = new TextArea();
        t.setPromptText(prompt);
        t.getStyleClass().add("text-input");
        t.setPrefRowCount(4);
        t.setWrapText(true);
        return t;
    }

    private ComboBox<String> combo(String[] items) {
        ComboBox<String> c = new ComboBox<>();
        c.getItems().addAll(items);
        c.getStyleClass().add("combo");
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    private Button iconButton(String glyph) {
        Button b = new Button(glyph);
        b.getStyleClass().add("icon-button");
        return b;
    }

    private Label bodyText(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("body-text");
        l.setWrapText(true);
        return l;
    }

    private List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\\r?\\n")) {
            if (!line.trim().isEmpty()) out.add(line.trim());
        }
        return out;
    }

    private String text(TextField f) {
        return f.getText() == null ? "" : f.getText().trim();
    }

    private String text(TextArea f) {
        return f.getText() == null ? "" : f.getText().trim();
    }

    private int indexOf(String[] arr, String value) {
        if (value != null) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].equals(value)) return i;
            }
        }
        return 0;
    }

    private int parseInt(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return 0;
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDouble(String s) {
        try {
            if (s == null || s.trim().isEmpty()) return 0.0;
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double clampRating(double rating) {
        if (rating < 0) return 0;
        if (rating > 5) return 5;
        return rating;
    }

    // ===== Image helpers =====

    /** Builds the image area: a real photo if the recipe has one, else an emoji. */
    private StackPane imagePanel(Recipe r, double height, boolean detail) {
        StackPane panel = new StackPane();
        panel.setPrefHeight(height);
        panel.setMinHeight(height);
        String url = imageUrl(r);
        if (url != null) {
            Region image = new Region();
            image.setStyle("-fx-background-image: url('" + url + "');"
                    + "-fx-background-size: contain;"
                    + "-fx-background-position: center center;"
                    + "-fx-background-repeat: no-repeat;"
                    + "-fx-background-color: #FFFDF9;");
            image.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            double arc = detail ? 44 : 32;
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(image.widthProperty());
            clip.heightProperty().bind(image.heightProperty());
            clip.setArcWidth(arc);
            clip.setArcHeight(arc);
            image.setClip(clip);
            panel.getChildren().add(image);
        } else {
            panel.getStyleClass().add(detail ? "emoji-panel-detail" : "emoji-panel");
            Label emoji = new Label(RecipeEmoji.forRecipe(r));
            emoji.getStyleClass().add(detail ? "emoji-glyph-detail" : "emoji-glyph");
            panel.getChildren().add(emoji);
        }
        return panel;
    }

    private String imageUrl(Recipe r) {
        return r == null ? null : urlForImagePath(r.getImagePath());
    }

    /** Turns a stored image value into a URL JavaFX can display, or null. */
    private String urlForImagePath(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        if (path.startsWith("res:")) {
            URL u = getClass().getResource("/com/cynthia/recipebook/" + path.substring(4));
            return u == null ? null : u.toExternalForm();
        }
        File f = new File(path);
        return f.exists() ? f.toURI().toString() : null;
    }

    /**
     * Copies a photo the user picked into a local "recipe_images" folder so the
     * app keeps working even if the original file is later moved. Returns the new
     * path, or the original path if copying fails.
     */
    private String importImage(File src) {
        try {
            File dir = new File("recipe_images");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File dest = new File(dir, System.currentTimeMillis() + "_" + src.getName());
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getAbsolutePath();
        } catch (IOException e) {
            return src.getAbsolutePath();
        }
    }

    /** Renders the small photo preview in the form (image or emoji fallback). */
    private void renderImagePreview(StackPane preview, String path, String name, String category) {
        preview.getChildren().clear();
        preview.getStyleClass().remove("emoji-panel");
        String url = urlForImagePath(path);
        if (url != null) {
            Region image = new Region();
            image.setStyle("-fx-background-image: url('" + url + "');"
                    + "-fx-background-size: contain;"
                    + "-fx-background-position: center center;"
                    + "-fx-background-repeat: no-repeat;"
                    + "-fx-background-color: #FFFDF9;");
            image.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(image.widthProperty());
            clip.heightProperty().bind(image.heightProperty());
            clip.setArcWidth(28);
            clip.setArcHeight(28);
            image.setClip(clip);
            preview.getChildren().add(image);
        } else {
            preview.getStyleClass().add("emoji-panel");
            Recipe tmp = new Recipe();
            tmp.setName(name == null ? "" : name);
            tmp.setCategory(category == null ? "Lunch" : category);
            Label emoji = new Label(RecipeEmoji.forRecipe(tmp));
            emoji.getStyleClass().add("emoji-glyph");
            preview.getChildren().add(emoji);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR,
                message == null ? "Unexpected error." : message, ButtonType.OK);
        alert.setHeaderText("Something went wrong");
        alert.showAndWait();
    }

    private void info(String message) {
        Alert alert = new Alert(AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
