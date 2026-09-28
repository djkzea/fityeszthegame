package com.example.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;

public class GameUI {

    // =========================================================
    // FŐ ELEMEK
    // =========================================================

    private final BorderPane root;

    private final VBox topBar;
    private final VBox gameContent;
    private final VBox dialogueArea;
    private final VBox choicesBox;
    private final VBox bottomArea;

    private final Label gameTitle;

    private final Label playerNameLabel;
    private final Label xpLabel;
    private final Label exposureLabel;
    private final Label levelLabel;
    private final Label itemsLabel;

    private final ProgressBar xpBar;
    private final ProgressBar exposureBar;

    private final Label chapterLabel;

    private final Label speakerLabel;
    private final Label dialogueLabel;

    private final StackPane imageArea;
    private final ImageView imageView;

    private final Button restartButton;

    private final Label footerLabel;

    // Boss UI
    private final VBox bossArea;

    private final Label bossNameLabel;
    private final Label playerHpLabel;
    private final Label bossHpLabel;

    private final ProgressBar playerHpBar;
    private final ProgressBar bossHpBar;

    // =========================================================
    // SZÍNEK
    // =========================================================

    private final String BACKGROUND = "#F5F1E8";
    private final String WHITE = "#FFFDF8";
    private final String DARK = "#202020";
    private final String LIGHT_BORDER = "#D6D0C4";
    private final String RED = "#B3261E";
    private final String RED_DARK = "#8F1D18";
    private final String GRAY = "#777777";
    private final String LIGHT_GRAY = "#ECE8DF";

    // =========================================================
    // BETŰTÍPUSOK
    // =========================================================

    private final Font normalFont =
            Font.font("Monospaced", FontWeight.NORMAL, 15);

    private final Font smallFont =
            Font.font("Monospaced", FontWeight.NORMAL, 12);

    private final Font boldFont =
            Font.font("Monospaced", FontWeight.BOLD, 14);

    private final Font titleFont =
            Font.font("Monospaced", FontWeight.BOLD, 22);

    private final Font chapterFont =
            Font.font("Monospaced", FontWeight.BOLD, 20);

    // =========================================================
    // KONSTRUKTOR
    // =========================================================

    public GameUI() {

        // -----------------------------------------------------
        // ROOT
        // -----------------------------------------------------

        root = new BorderPane();

        root.setStyle(
                "-fx-background-color: " + BACKGROUND + ";"
        );

        // -----------------------------------------------------
        // TOP BAR
        // -----------------------------------------------------

        topBar = new VBox();
        topBar.setPadding(new Insets(18, 30, 12, 30));
        topBar.setSpacing(12);

        topBar.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + LIGHT_BORDER + ";" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // -----------------------------------------------------
        // CÍM
        // -----------------------------------------------------

        gameTitle = new Label("FITYESZ KRÓNIKA");

        gameTitle.setFont(titleFont);
        gameTitle.setTextFill(Color.web(DARK));

        // -----------------------------------------------------
        // STATS
        // -----------------------------------------------------

        HBox statsRow = new HBox();
        statsRow.setSpacing(35);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // PLAYER NAME

        playerNameLabel = createStatLabel("Játékos");

        // XP

        VBox xpBox = new VBox();
        xpBox.setSpacing(4);

        xpLabel = createStatLabel("XP 0");

        xpBar = new ProgressBar(0);
        xpBar.setPrefWidth(150);
        xpBar.setMaxWidth(150);

        xpBar.setStyle(
                "-fx-accent: " + RED + ";"
        );

        xpBox.getChildren().addAll(
                xpLabel,
                xpBar
        );

        // EXPOSURE

        VBox exposureBox = new VBox();
        exposureBox.setSpacing(4);

        exposureLabel = createStatLabel("LEBUKÁS 0/100");

        exposureBar = new ProgressBar(0);
        exposureBar.setPrefWidth(150);
        exposureBar.setMaxWidth(150);

        exposureBar.setStyle(
                "-fx-accent: " + RED + ";"
        );

        exposureBox.getChildren().addAll(
                exposureLabel,
                exposureBar
        );

        // LEVEL

        levelLabel = createStatLabel("SZINT 1");

        // ITEMS

        itemsLabel = createStatLabel("TÁRGYAK 0");

        // RESTART

        restartButton = new Button("ÚJRakezdés");

        restartButton.setFont(smallFont);

        restartButton.setPadding(
                new Insets(8, 15, 8, 15)
        );

        restartButton.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + DARK + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: " + DARK + ";" +
                        "-fx-cursor: hand;"
        );

        restartButton.setOnMouseEntered(event ->
                restartButton.setStyle(
                        "-fx-background-color: " + DARK + ";" +
                                "-fx-border-color: " + DARK + ";" +
                                "-fx-border-width: 1;" +
                                "-fx-text-fill: white;" +
                                "-fx-cursor: hand;"
                )
        );

        restartButton.setOnMouseExited(event ->
                restartButton.setStyle(
                        "-fx-background-color: " + WHITE + ";" +
                                "-fx-border-color: " + DARK + ";" +
                                "-fx-border-width: 1;" +
                                "-fx-text-fill: " + DARK + ";" +
                                "-fx-cursor: hand;"
                )
        );

        HBox.setHgrow(playerNameLabel, Priority.ALWAYS);

        statsRow.getChildren().addAll(
                playerNameLabel,
                xpBox,
                exposureBox,
                levelLabel,
                itemsLabel,
                restartButton
        );

        topBar.getChildren().addAll(
                gameTitle,
                statsRow
        );

        root.setTop(topBar);

        // -----------------------------------------------------
        // GAME CONTENT
        // -----------------------------------------------------

        gameContent = new VBox();
        gameContent.setSpacing(18);
        gameContent.setPadding(
                new Insets(30, 40, 20, 40)
        );

        gameContent.setAlignment(Pos.TOP_CENTER);

        // -----------------------------------------------------
        // CHAPTER
        // -----------------------------------------------------

        chapterLabel = new Label();

        chapterLabel.setFont(chapterFont);
        chapterLabel.setTextFill(Color.web(DARK));

        chapterLabel.setWrapText(true);

        // -----------------------------------------------------
        // IMAGE AREA
        // -----------------------------------------------------

        imageArea = new StackPane();

        imageArea.setMinHeight(0);
        imageArea.setPrefHeight(300);
        imageArea.setMaxHeight(350);

        imageArea.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-border-color: " + LIGHT_BORDER + ";" +
                        "-fx-border-width: 1;"
        );

        imageView = new ImageView();

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        imageView.setFitWidth(700);
        imageView.setFitHeight(330);

        imageArea.getChildren().add(imageView);

        imageArea.setVisible(false);
        imageArea.setManaged(false);

        // -----------------------------------------------------
        // DIALOGUE
        // -----------------------------------------------------

        dialogueArea = new VBox();
        dialogueArea.setSpacing(0);

        dialogueArea.setMaxWidth(850);

        // SPEAKER

        speakerLabel = new Label();

        speakerLabel.setFont(
                Font.font("Monospaced", FontWeight.BOLD, 14)
        );

        speakerLabel.setTextFill(Color.WHITE);

        speakerLabel.setPadding(
                new Insets(7, 14, 7, 14)
        );

        speakerLabel.setStyle(
                "-fx-background-color: " + RED + ";"
        );

        // DIALOGUE TEXT

        dialogueLabel = new Label();

        dialogueLabel.setFont(normalFont);
        dialogueLabel.setTextFill(Color.web(DARK));

        dialogueLabel.setWrapText(true);

        dialogueLabel.setMaxWidth(850);

        dialogueLabel.setPadding(
                new Insets(20)
        );

        dialogueLabel.setMinHeight(100);

        dialogueLabel.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + LIGHT_BORDER + ";" +
                        "-fx-border-width: 1;"
        );

        dialogueArea.getChildren().addAll(
                speakerLabel,
                dialogueLabel
        );

        // -----------------------------------------------------
        // CHOICES
        // -----------------------------------------------------

        choicesBox = new VBox();

        choicesBox.setSpacing(10);

        choicesBox.setMaxWidth(850);

        choicesBox.setAlignment(Pos.CENTER);

        // -----------------------------------------------------
        // BOSS AREA
        // -----------------------------------------------------

        bossArea = new VBox();

        bossArea.setSpacing(12);
        bossArea.setPadding(new Insets(15));

        bossArea.setMaxWidth(850);

        bossArea.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + LIGHT_BORDER + ";" +
                        "-fx-border-width: 1;"
        );

        bossNameLabel = new Label();

        bossNameLabel.setFont(boldFont);
        bossNameLabel.setTextFill(Color.web(RED));

        playerHpLabel = new Label();
        playerHpLabel.setFont(smallFont);

        bossHpLabel = new Label();
        bossHpLabel.setFont(smallFont);

        playerHpBar = new ProgressBar(1);
        playerHpBar.setPrefHeight(12);
        playerHpBar.setMaxWidth(Double.MAX_VALUE);

        playerHpBar.setStyle(
                "-fx-accent: #4C8A4C;"
        );

        bossHpBar = new ProgressBar(1);
        bossHpBar.setPrefHeight(12);
        bossHpBar.setMaxWidth(Double.MAX_VALUE);

        bossHpBar.setStyle(
                "-fx-accent: " + RED + ";"
        );

        bossArea.getChildren().addAll(
                bossNameLabel,
                playerHpLabel,
                playerHpBar,
                bossHpLabel,
                bossHpBar
        );

        bossArea.setVisible(false);
        bossArea.setManaged(false);

        // -----------------------------------------------------
        // GAME CONTENT
        // -----------------------------------------------------

        gameContent.getChildren().addAll(
                chapterLabel,
                imageArea,
                dialogueArea,
                bossArea,
                choicesBox
        );

        // -----------------------------------------------------
        // SCROLL
        // -----------------------------------------------------

        ScrollPane scrollPane = new ScrollPane();

        scrollPane.setContent(gameContent);

        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.setStyle(
                "-fx-background-color: " + BACKGROUND + ";" +
                        "-fx-border-color: transparent;"
        );

        root.setCenter(scrollPane);

        // -----------------------------------------------------
        // FOOTER
        // -----------------------------------------------------

        bottomArea = new VBox();

        bottomArea.setAlignment(Pos.CENTER);
        bottomArea.setPadding(
                new Insets(12, 20, 15, 20)
        );

        bottomArea.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + LIGHT_BORDER + ";" +
                        "-fx-border-width: 1 0 0 0;"
        );

        footerLabel = new Label(
                "Fityesz Krónika · Szöveges kalandjáték"
        );

        footerLabel.setFont(smallFont);
        footerLabel.setTextFill(Color.web(GRAY));

        bottomArea.getChildren().add(footerLabel);

        root.setBottom(bottomArea);

        // -----------------------------------------------------
        // DEFAULT VALUES
        // -----------------------------------------------------

        speakerLabel.setText("FITYESZ KRÓNIKA");
        dialogueLabel.setText("");

        updateStats(null);
    }

    // =========================================================
    // STAT LABEL
    // =========================================================

    private Label createStatLabel(String text) {

        Label label = new Label(text);

        label.setFont(boldFont);
        label.setTextFill(Color.web(DARK));

        return label;
    }

    // =========================================================
    // SHOW DIALOGUE
    // =========================================================

    public void showDialogue(
            String speaker,
            String text
    ) {

        speakerLabel.setText(
                speaker == null ? "" : speaker
        );

        dialogueLabel.setText(
                text == null ? "" : text
        );

        speakerLabel.setVisible(
                speaker != null && !speaker.isBlank()
        );

        speakerLabel.setManaged(
                speaker != null && !speaker.isBlank()
        );
    }

    // =========================================================
    // UPDATE STATS
    // =========================================================

    public void updateStats(GameState state) {

        if (state == null) {
            playerNameLabel.setText("JÁTÉKOS");
            xpLabel.setText("XP 0");
            exposureLabel.setText("LEBUKÁS 0/100");
            levelLabel.setText("SZINT 1");
            itemsLabel.setText("TÁRGYAK 0");

            xpBar.setProgress(0);
            exposureBar.setProgress(0);

            return;
        }

        // NAME

        String name = state.getName();

        if (name == null || name.isBlank()) {
            playerNameLabel.setText("JÁTÉKOS");
        } else {
            playerNameLabel.setText(
                    name.toUpperCase()
            );
        }

        // XP

        int xp = state.getXp();

        xpLabel.setText(
                "XP " + xp
        );

        /*
         * Az első szint 50 XP-nél lép szintet.
         * Ezért 50-nél telik meg a sáv.
         */

        double xpProgress =
                Math.min(xp / 50.0, 1.0);

        xpBar.setProgress(xpProgress);

        // EXPOSURE

        int exposure = state.getExposure();

        exposureLabel.setText(
                "LEBUKÁS " + exposure + "/100"
        );

        exposureBar.setProgress(
                exposure / 100.0
        );

        // LEVEL

        levelLabel.setText(
                "SZINT " + state.getLevel()
        );

        // ITEMS

        int items = 0;

        if (state.hasFirstEnvelope()) {
            items++;
        }

        if (state.hasSmallEnvelope()) {
            items++;
        }

        if (state.hasLakatosFile()) {
            items++;
        }

        itemsLabel.setText(
                "TÁRGYAK " + items
        );
    }

    // =========================================================
    // CLEAR CHOICES
    // =========================================================

    public void clearChoices() {

        choicesBox.getChildren().clear();
    }

    // =========================================================
    // ADD CHOICE
    // =========================================================

    public void addChoice(
            int number,
            String text,
            Runnable action
    ) {

        Button button = new Button();

        button.setText(
                number + ".  " + text
        );

        button.setFont(normalFont);

        button.setAlignment(Pos.CENTER_LEFT);

        button.setMaxWidth(Double.MAX_VALUE);

        button.setMinHeight(50);

        button.setPadding(
                new Insets(12, 18, 12, 18)
        );

        button.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + DARK + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: " + DARK + ";" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(event -> {

            button.setStyle(
                    "-fx-background-color: " + DARK + ";" +
                            "-fx-border-color: " + DARK + ";" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );
        });

        button.setOnMouseExited(event -> {

            button.setStyle(
                    "-fx-background-color: " + WHITE + ";" +
                            "-fx-border-color: " + DARK + ";" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: " + DARK + ";" +
                            "-fx-cursor: hand;"
            );
        });

        button.setOnAction(event -> {

            if (action != null) {
                action.run();
            }
        });

        choicesBox.getChildren().add(button);
    }

    // =========================================================
    // IMAGE
    // =========================================================

    public void showImage(String path) {

        Image image = null;

        // -----------------------------------------------------
        // 1. CLASS PATH
        // -----------------------------------------------------

        try {

            InputStream stream =
                    getClass()
                            .getResourceAsStream(path);

            if (stream != null) {

                image = new Image(stream);
            }

        } catch (Exception ignored) {
        }

        // -----------------------------------------------------
        // 2. CLASS PATH WITHOUT /
        // -----------------------------------------------------

        if (image == null) {

            try {

                InputStream stream =
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(
                                        path.startsWith("/")
                                                ? path.substring(1)
                                                : path
                                );

                if (stream != null) {

                    image = new Image(stream);
                }

            } catch (Exception ignored) {
            }
        }

        // -----------------------------------------------------
        // 3. NORMAL FILE PATH
        // -----------------------------------------------------

        if (image == null) {

            try {

                File file = new File(path);

                if (file.exists()) {

                    image = new Image(
                            file.toURI().toString()
                    );
                }

            } catch (Exception ignored) {
            }
        }

        // -----------------------------------------------------
        // IMAGE FOUND
        // -----------------------------------------------------

        if (image != null) {

            imageView.setImage(image);

            imageArea.setVisible(true);
            imageArea.setManaged(true);

        } else {

            imageView.setImage(null);

            imageArea.setVisible(false);
            imageArea.setManaged(false);
        }
    }

    // =========================================================
    // HIDE IMAGE
    // =========================================================

    public void hideImage() {

        imageView.setImage(null);

        imageArea.setVisible(false);
        imageArea.setManaged(false);
    }

    // =========================================================
    // BOSS FIGHT
    // =========================================================

    public void showBossFight(
            String bossName,
            int playerHp,
            int bossHp
    ) {

        bossArea.setVisible(true);
        bossArea.setManaged(true);

        updateBossHp(
                bossName,
                playerHp,
                bossHp
        );

    }

    // =========================================================
    // UPDATE BOSS HP
    // =========================================================

    public void updateBossHp(
            String bossName,
            int playerHp,
            int bossHp
    ) {

        // BOSS NAME

        bossNameLabel.setText(
                bossName == null
                        ? "ELLENFÉL"
                        : bossName
        );

        // PLAYER HP

        playerHp = Math.max(
                0,
                Math.min(100, playerHp)
        );

        playerHpLabel.setText(
                "JÁTÉKOS ÉLETERŐ: "
                        + playerHp
                        + " / 100"
        );

        playerHpBar.setProgress(
                playerHp / 100.0
        );

        // BOSS HP

        bossHp = Math.max(
                0,
                Math.min(80, bossHp)
        );

        bossHpLabel.setText(
                "ELLENFÉL ÉLETERŐ: "
                        + bossHp
                        + " / 80"
        );

        bossHpBar.setProgress(
                bossHp / 80.0
        );
    }

    // =========================================================
    // HIDE BOSS
    // =========================================================

    public void hideBossFight() {

        bossArea.setVisible(false);
        bossArea.setManaged(false);
    }

    // =========================================================
    // GET ROOT
    // =========================================================

    public BorderPane getRoot() {

        return root;
    }

    // =========================================================
    // SHOW WINDOW
    // =========================================================

    public void show(Stage stage) {

        Scene scene = new Scene(
                root,
                1100,
                750
        );

        stage.setTitle(
                "Fityesz Krónika"
        );

        stage.setScene(scene);

        stage.show();
    }
}