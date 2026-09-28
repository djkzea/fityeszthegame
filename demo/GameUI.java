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
import javafx.scene.shape.Rectangle;
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

    // =========================================================
    // SCENE
    // =========================================================

    private final StackPane imageArea;
    private final ImageView backgroundView;
    private final ImageView characterView;

    // =========================================================
    // BOSS
    // =========================================================

    private final VBox bossArea;
    private final Label bossNameLabel;
    private final Label playerHpLabel;
    private final Label bossHpLabel;

    private final ProgressBar playerHpBar;
    private final ProgressBar bossHpBar;

    // =========================================================
    // EGYÉB
    // =========================================================

    private final Button restartButton;
    private final Label footerLabel;

    // =========================================================
    // SZÍNEK
    // =========================================================

    private static final String PAGE_BACKGROUND = "#F1EDE3";
    private static final String CREAM = "#F7F3E9";
    private static final String WHITE = "#FFFDF8";

    private static final String BLACK = "#111111";
    private static final String DARK = "#191919";

    private static final String RED = "#D20A2E";
    private static final String RED_DARK = "#A80725";

    private static final String GREEN = "#2F6B45";

    private static final String BORDER = "#171717";
    private static final String LIGHT_BORDER = "#C8C1B5";

    private static final String GRAY = "#6D6D6D";

    // =========================================================
    // BETŰTÍPUSOK
    // =========================================================

    private final Font normalFont =
            Font.font("Arial", FontWeight.NORMAL, 16);

    private final Font smallFont =
            Font.font("Arial", FontWeight.NORMAL, 11);

    private final Font boldFont =
            Font.font("Arial", FontWeight.BOLD, 12);

    private final Font titleFont =
            Font.font("Arial", FontWeight.BOLD, 22);

    private final Font chapterFont =
            Font.font("Arial", FontWeight.BOLD, 44);

    private final Font dialogueFont =
            Font.font("Arial", FontWeight.NORMAL, 18);

    // =========================================================
    // KONSTRUKTOR
    // =========================================================

    public GameUI() {

        // =====================================================
        // ROOT
        // =====================================================

        root = new BorderPane();

        root.setStyle(
                "-fx-background-color: " + PAGE_BACKGROUND + ";"
        );

        // =====================================================
        // FELSŐ RÉSZ
        // =====================================================

        topBar = new VBox();

        topBar.setSpacing(0);

        topBar.setStyle(
                "-fx-background-color: " + CREAM + ";"
        );

        // =====================================================
        // PIROS / KRÉM / ZÖLD CSÍK
        // =====================================================

        HBox colorLine = new HBox();

        Region redLine = new Region();
        Region creamLine = new Region();
        Region greenLine = new Region();

        redLine.setPrefHeight(5);
        creamLine.setPrefHeight(5);
        greenLine.setPrefHeight(5);

        redLine.setStyle(
                "-fx-background-color: " + RED + ";"
        );

        creamLine.setStyle(
                "-fx-background-color: " + CREAM + ";"
        );

        greenLine.setStyle(
                "-fx-background-color: " + GREEN + ";"
        );

        HBox.setHgrow(
                redLine,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                creamLine,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                greenLine,
                Priority.ALWAYS
        );

        colorLine.getChildren().addAll(
                redLine,
                creamLine,
                greenLine
        );

        // =====================================================
        // STATUS BAR
        // =====================================================

        HBox statusBar = new HBox();

        statusBar.setAlignment(
                Pos.CENTER_LEFT
        );

        statusBar.setSpacing(22);

        statusBar.setPadding(
                new Insets(13, 20, 13, 20)
        );

        statusBar.setStyle(
                "-fx-background-color: " + CREAM + ";" +
                        "-fx-border-color: " + LIGHT_BORDER + ";" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // =====================================================
        // JÁTÉK CÍME
        // =====================================================

        gameTitle = new Label(
                Lang.t("ui.title")
        );

        gameTitle.setFont(titleFont);

        gameTitle.setTextFill(
                Color.web(BLACK)
        );

        gameTitle.setMinWidth(
                Region.USE_PREF_SIZE
        );

        // =====================================================
        // PLAYER
        // =====================================================

        playerNameLabel = createStatLabel(
                Lang.t("ui.player")
        );

        // =====================================================
        // XP
        // =====================================================

        VBox xpBox = new VBox();

        xpBox.setSpacing(3);

        xpLabel = createStatLabel(
                Lang.t("ui.xp") + " 0"
        );

        xpBar = new ProgressBar(0);

        xpBar.setPrefWidth(90);
        xpBar.setPrefHeight(7);

        xpBar.setMaxWidth(90);

        xpBar.setStyle(
                "-fx-accent: " + RED + ";"
        );

        xpBox.getChildren().addAll(
                xpLabel,
                xpBar
        );

        // =====================================================
        // LEBUKÁS
        // =====================================================

        VBox exposureBox = new VBox();

        exposureBox.setSpacing(3);

        exposureLabel = createStatLabel(
                Lang.t("ui.exposure") + " 0/100"
        );

        exposureBar = new ProgressBar(0);

        exposureBar.setPrefWidth(90);
        exposureBar.setPrefHeight(7);

        exposureBar.setMaxWidth(90);

        exposureBar.setStyle(
                "-fx-accent: " + RED + ";"
        );

        exposureBox.getChildren().addAll(
                exposureLabel,
                exposureBar
        );

        // =====================================================
        // LEVEL
        // =====================================================

        levelLabel = createStatLabel(
                Lang.t("ui.level") + " 1"
        );

        // =====================================================
        // ITEMS
        // =====================================================

        itemsLabel = createStatLabel(
                Lang.t("ui.items") + " 0"
        );

        // =====================================================
        // RESTART
        // =====================================================

        restartButton = new Button(
                Lang.t("ui.restart")
        );

        restartButton.setFont(
                Font.font("Arial", FontWeight.BOLD, 11)
        );

        restartButton.setPadding(
                new Insets(7, 12, 7, 12)
        );

        styleRestartButton(
                restartButton
        );

        // =====================================================
        // STATUS BAR ELEMEI
        // =====================================================

        statusBar.getChildren().addAll(
                gameTitle,
                playerNameLabel,
                xpBox,
                exposureBox,
                levelLabel,
                itemsLabel,
                restartButton
        );

        topBar.getChildren().addAll(
                colorLine,
                statusBar
        );

        root.setTop(topBar);

        // =====================================================
        // JÁTÉK TARTALOM
        // =====================================================

        gameContent = new VBox();

        gameContent.setAlignment(
                Pos.TOP_CENTER
        );

        gameContent.setSpacing(0);

        gameContent.setFillWidth(true);

        gameContent.setStyle(
                "-fx-background-color: " + DARK + ";"
        );

        // =====================================================
        // CHAPTER LABEL
        // =====================================================

        chapterLabel = new Label();

        chapterLabel.setFont(
                Font.font(
                        "Arial",
                        FontWeight.BOLD,
                        12
                )
        );

        chapterLabel.setTextFill(
                Color.web(WHITE)
        );

        chapterLabel.setAlignment(
                Pos.CENTER_LEFT
        );

        chapterLabel.setMaxWidth(
                Double.MAX_VALUE
        );

        chapterLabel.setPadding(
                new Insets(12, 20, 11, 20)
        );

        chapterLabel.setStyle(
                "-fx-background-color: " + DARK + ";" +
                        "-fx-border-color: " + RED + ";" +
                        "-fx-border-width: 0 0 3 0;"
        );

        // =====================================================
        // KÉPTERÜLET
        // =====================================================

        imageArea = new StackPane();

        imageArea.setPrefHeight(500);
        imageArea.setMinHeight(500);
        imageArea.setMaxHeight(500);

        imageArea.setMaxWidth(
                Double.MAX_VALUE
        );

        imageArea.setStyle(
                "-fx-background-color: " + BLACK + ";"
        );

        // =====================================================
        // HÁTTÉRKÉP
        // =====================================================

        backgroundView = new ImageView();

        backgroundView.setPreserveRatio(true);
        backgroundView.setSmooth(true);

        backgroundView.setFitWidth(1000);
        backgroundView.setFitHeight(500);

        // =====================================================
        // KARAKTER
        // =====================================================

        characterView = new ImageView();

        characterView.setPreserveRatio(true);
        characterView.setSmooth(true);

        characterView.setFitHeight(500);

        characterView.setTranslateY(45);

        Rectangle characterClip =
                new Rectangle(
                        350,
                        470
                );

        characterView.setClip(
                characterClip
        );

        StackPane.setAlignment(
                characterView,
                Pos.BOTTOM_CENTER
        );

        imageArea.getChildren().addAll(
                backgroundView,
                characterView
        );

        imageArea.setVisible(false);
        imageArea.setManaged(false);

        // =====================================================
        // DIALOGUE TERÜLET
        // =====================================================

        dialogueArea = new VBox();

        dialogueArea.setAlignment(
                Pos.TOP_LEFT
        );

        dialogueArea.setSpacing(0);

        dialogueArea.setMaxWidth(900);

        dialogueArea.setPadding(
                new Insets(18, 20, 10, 20)
        );

        dialogueArea.setStyle(
                "-fx-background-color: " + DARK + ";"
        );

        // =====================================================
        // SPEAKER
        // =====================================================

        speakerLabel = new Label();

        speakerLabel.setFont(
                Font.font(
                        "Arial",
                        FontWeight.BOLD,
                        13
                )
        );

        speakerLabel.setTextFill(
                Color.WHITE
        );

        speakerLabel.setPadding(
                new Insets(8, 14, 8, 14)
        );

        speakerLabel.setStyle(
                "-fx-background-color: " + RED + ";"
        );

        speakerLabel.setMaxWidth(
                Region.USE_PREF_SIZE
        );

        // =====================================================
        // DIALOGUE SZÖVEG
        // =====================================================

        dialogueLabel = new Label();

        dialogueLabel.setFont(
                dialogueFont
        );

        dialogueLabel.setTextFill(
                Color.web(BLACK)
        );

        dialogueLabel.setWrapText(true);

        dialogueLabel.setMaxWidth(900);

        dialogueLabel.setMinHeight(105);

        dialogueLabel.setPadding(
                new Insets(22, 25, 22, 25)
        );

        dialogueLabel.setStyle(
                "-fx-background-color: " + CREAM + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;"
        );

        dialogueArea.getChildren().addAll(
                speakerLabel,
                dialogueLabel
        );

        // =====================================================
        // CHOICES
        // =====================================================

        choicesBox = new VBox();

        choicesBox.setAlignment(
                Pos.CENTER
        );

        choicesBox.setSpacing(8);

        choicesBox.setMaxWidth(900);

        choicesBox.setPadding(
                new Insets(8, 20, 22, 20)
        );

        choicesBox.setStyle(
                "-fx-background-color: " + DARK + ";"
        );

        // =====================================================
        // BOSS AREA
        // =====================================================

        bossArea = new VBox();

        bossArea.setSpacing(8);

        bossArea.setMaxWidth(900);

        bossArea.setPadding(
                new Insets(18, 20, 18, 20)
        );

        bossArea.setStyle(
                "-fx-background-color: " + CREAM + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;"
        );

        bossNameLabel = new Label();

        bossNameLabel.setFont(
                Font.font(
                        "Arial",
                        FontWeight.BOLD,
                        17
                )
        );

        bossNameLabel.setTextFill(
                Color.web(RED)
        );

        playerHpLabel = new Label();

        playerHpLabel.setFont(
                boldFont
        );

        playerHpLabel.setTextFill(
                Color.web(BLACK)
        );

        bossHpLabel = new Label();

        bossHpLabel.setFont(
                boldFont
        );

        bossHpLabel.setTextFill(
                Color.web(BLACK)
        );

        playerHpBar = new ProgressBar(1);

        playerHpBar.setMaxWidth(
                Double.MAX_VALUE
        );

        playerHpBar.setPrefHeight(10);

        playerHpBar.setStyle(
                "-fx-accent: " + GREEN + ";"
        );

        bossHpBar = new ProgressBar(1);

        bossHpBar.setMaxWidth(
                Double.MAX_VALUE
        );

        bossHpBar.setPrefHeight(10);

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

        // =====================================================
        // MINDEN A GAME CONTENT-BE
        // =====================================================

        gameContent.getChildren().addAll(
                chapterLabel,
                imageArea,
                dialogueArea,
                bossArea,
                choicesBox
        );

        // =====================================================
        // SCROLLPANE
        // =====================================================

        ScrollPane scrollPane =
                new ScrollPane();

        scrollPane.setContent(
                gameContent
        );

        scrollPane.setFitToWidth(true);

        scrollPane.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        scrollPane.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        scrollPane.setStyle(
                "-fx-background-color: " + DARK + ";" +
                        "-fx-border-color: transparent;"
        );

        root.setCenter(
                scrollPane
        );

        // =====================================================
        // FOOTER
        // =====================================================

        bottomArea = new VBox();

        bottomArea.setAlignment(
                Pos.CENTER
        );

        bottomArea.setPadding(
                new Insets(10, 20, 12, 20)
        );

        bottomArea.setStyle(
                "-fx-background-color: " + CREAM + ";" +
                        "-fx-border-color: " + LIGHT_BORDER + ";" +
                        "-fx-border-width: 1 0 0 0;"
        );

        footerLabel = new Label(
                Lang.t("ui.footer")
        );

        footerLabel.setFont(
                smallFont
        );

        footerLabel.setTextFill(
                Color.web(GRAY)
        );

        bottomArea.getChildren().add(
                footerLabel
        );

        root.setBottom(
                bottomArea
        );

        // =====================================================
        // ALAPÉRTELMEZETT ÉRTÉKEK
        // =====================================================

        speakerLabel.setText(
                Lang.t("ui.title")
        );

        dialogueLabel.setText("");

        chapterLabel.setText("");

        playerNameLabel.setText(
                Lang.t("ui.player")
        );

        xpLabel.setText(
                Lang.t("ui.xp") + " 0"
        );

        exposureLabel.setText(
                Lang.t("ui.exposure") + " 0/100"
        );

        levelLabel.setText(
                Lang.t("ui.level") + " 1"
        );

        itemsLabel.setText(
                Lang.t("ui.items") + " 0"
        );
    }

    // =========================================================
    // STAT LABEL
    // =========================================================

    private Label createStatLabel(
            String text
    ) {

        Label label =
                new Label(text);

        label.setFont(
                boldFont
        );

        label.setTextFill(
                Color.web(BLACK)
        );

        return label;
    }

    // =========================================================
    // RESTART BUTTON
    // =========================================================

    private void styleRestartButton(
            Button button
    ) {

        button.setStyle(
                "-fx-background-color: " + CREAM + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: " + BLACK + ";" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(
                event -> {

                    button.setStyle(
                            "-fx-background-color: " + BLACK + ";" +
                                    "-fx-border-color: " + BLACK + ";" +
                                    "-fx-border-width: 1;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-cursor: hand;"
                    );
                }
        );

        button.setOnMouseExited(
                event -> {

                    button.setStyle(
                            "-fx-background-color: " + CREAM + ";" +
                                    "-fx-border-color: " + BORDER + ";" +
                                    "-fx-border-width: 1;" +
                                    "-fx-text-fill: " + BLACK + ";" +
                                    "-fx-cursor: hand;"
                    );
                }
        );
    }

    // =========================================================
    // DIALOGUE MEGJELENÍTÉSE
    // =========================================================

    public void showDialogue(
            String speaker,
            String text
    ) {

        speakerLabel.setText(
                speaker == null
                        ? ""
                        : speaker
        );

        dialogueLabel.setText(
                text == null
                        ? ""
                        : text
        );

        boolean visible =
                speaker != null &&
                        !speaker.isBlank();

        speakerLabel.setVisible(
                visible
        );

        speakerLabel.setManaged(
                visible
        );
    }

    // =========================================================
    // STATISZTIKÁK FRISSÍTÉSE
    // =========================================================

    public void updateStats(
            GameState state
    ) {

        // PLAYER

        if (state.getName() == null ||
                state.getName().isBlank()) {

            playerNameLabel.setText(
                    Lang.t("ui.player")
            );

        } else {

            playerNameLabel.setText(
                    Lang.t("ui.player")
                            + ": "
                            + state.getName()
            );
        }

        // XP

        xpLabel.setText(
                Lang.t("ui.xp")
                        + " "
                        + state.getXp()
        );

        // EXPOSURE

        exposureLabel.setText(
                Lang.t("ui.exposure")
                        + " "
                        + state.getExposure()
                        + "/100"
        );

        // LEVEL

        levelLabel.setText(
                Lang.t("ui.level")
                        + " "
                        + state.getLevel()
        );

        // ITEMS

        int itemCount = 0;

        if (state.hasFirstEnvelope()) {
            itemCount++;
        }

        if (state.hasSmallEnvelope()) {
            itemCount++;
        }

        if (state.hasLakatosFile()) {
            itemCount++;
        }

        itemsLabel.setText(
                Lang.t("ui.items")
                        + " "
                        + itemCount
        );

        // XP BAR

        xpBar.setProgress(
                Math.min(
                        state.getXp(),
                        100
                ) / 100.0
        );

        // EXPOSURE BAR

        exposureBar.setProgress(
                state.getExposure()
                        / 100.0
        );
    }

    // =========================================================
    // CHOICES TÖRLÉSE
    // =========================================================

    public void clearChoices() {

        choicesBox.getChildren().clear();
    }

    // =========================================================
    // CHOICE HOZZÁADÁSA
    // =========================================================

    public void addChoice(
            int number,
            String text,
            Runnable action
    ) {

        Button button =
                new Button();

        button.setText(
                number +
                        "    " +
                        text
        );

        button.setFont(
                normalFont
        );

        button.setAlignment(
                Pos.CENTER_LEFT
        );

        button.setMaxWidth(
                Double.MAX_VALUE
        );

        button.setMinHeight(54);

        button.setPrefHeight(54);

        button.setPadding(
                new Insets(
                        10,
                        16,
                        10,
                        16
                )
        );

        styleChoiceButton(
                button
        );

        button.setOnAction(
                event -> {

                    if (action != null) {
                        action.run();
                    }
                }
        );

        choicesBox.getChildren().add(
                button
        );
    }

    // =========================================================
    // CHOICE BUTTON STYLE
    // =========================================================

    private void styleChoiceButton(
            Button button
    ) {

        button.setStyle(
                "-fx-background-color: " + CREAM + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: " + BLACK + ";" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(
                event -> {

                    button.setStyle(
                            "-fx-background-color: " + BLACK + ";" +
                                    "-fx-border-color: " + BLACK + ";" +
                                    "-fx-border-width: 1;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-cursor: hand;"
                    );
                }
        );

        button.setOnMouseExited(
                event -> {

                    button.setStyle(
                            "-fx-background-color: " + CREAM + ";" +
                                    "-fx-border-color: " + BORDER + ";" +
                                    "-fx-border-width: 1;" +
                                    "-fx-text-fill: " + BLACK + ";" +
                                    "-fx-cursor: hand;"
                    );
                }
        );
    }

    // =========================================================
    // HÁTTÉRKÉP MEGJELENÍTÉSE
    // =========================================================

    public void showImage(
            String path
    ) {

        Image image =
                loadImage(path);

        if (image != null) {

            backgroundView.setImage(
                    image
            );

            imageArea.setVisible(
                    true
            );

            imageArea.setManaged(
                    true
            );

        } else {

            backgroundView.setImage(
                    null
            );

            imageArea.setVisible(
                    false
            );

            imageArea.setManaged(
                    false
            );
        }
    }

    // =========================================================
    // KÉP BETÖLTÉSE
    // =========================================================

    private Image loadImage(
            String path
    ) {

        Image image = null;

        // =====================================================
        // CLASS PATH
        // =====================================================

        try {

            InputStream stream =
                    getClass()
                            .getResourceAsStream(path);

            if (stream != null) {

                image =
                        new Image(stream);
            }

        } catch (Exception ignored) {
        }

        // =====================================================
        // CLASS LOADER
        // =====================================================

        if (image == null) {

            try {

                String cleanPath =
                        path.startsWith("/")
                                ? path.substring(1)
                                : path;

                InputStream stream =
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(
                                        cleanPath
                                );

                if (stream != null) {

                    image =
                            new Image(stream);
                }

            } catch (Exception ignored) {
            }
        }

        // =====================================================
        // FILE
        // =====================================================

        if (image == null) {

            try {

                File file =
                        new File(path);

                if (file.exists()) {

                    image =
                            new Image(
                                    file.toURI()
                                            .toString()
                            );
                }

            } catch (Exception ignored) {
            }
        }

        return image;
    }

    // =========================================================
    // KARAKTER MEGJELENÍTÉSE
    // =========================================================

    public void showCharacter(
            String path
    ) {

        Image image =
                loadImage(path);

        if (image != null) {

            characterView.setImage(
                    image
            );

            characterView.setVisible(
                    true
            );

            characterView.setManaged(
                    true
            );

        } else {

            characterView.setImage(
                    null
            );

            characterView.setVisible(
                    false
            );

            characterView.setManaged(
                    false
            );
        }
    }

    // =========================================================
    // KARAKTER ELREJTÉSE
    // =========================================================

    public void hideCharacter() {

        characterView.setImage(
                null
        );

        characterView.setVisible(
                false
        );

        characterView.setManaged(
                false
        );
    }

    // =========================================================
    // HÁTTÉRKÉP ELREJTÉSE
    // =========================================================

    public void hideImage() {

        backgroundView.setImage(
                null
        );

        imageArea.setVisible(
                false
        );

        imageArea.setManaged(
                false
        );
    }

    // =========================================================
    // BOSS FIGHT
    // =========================================================

    public void showBossFight(
            String bossName,
            int playerHp,
            int bossHp
    ) {

        bossArea.setVisible(
                true
        );

        bossArea.setManaged(
                true
        );

        updateBossHp(
                bossName,
                playerHp,
                bossHp
        );
    }

    // =========================================================
    // BOSS HP FRISSÍTÉSE
    // =========================================================

    public void updateBossHp(
            String bossName,
            int playerHp,
            int bossHp
    ) {

        bossNameLabel.setText(
                bossName == null
                        ? "ELLENFÉL"
                        : bossName
        );

        playerHp =
                Math.max(
                        0,
                        Math.min(
                                100,
                                playerHp
                        )
                );

        bossHp =
                Math.max(
                        0,
                        Math.min(
                                80,
                                bossHp
                        )
                );

        playerHpLabel.setText(
                "JÁTÉKOS ÉLETERŐ: "
                        + playerHp
                        + " / 100"
        );

        bossHpLabel.setText(
                "ELLENFÉL ÉLETERŐ: "
                        + bossHp
                        + " / 80"
        );

        playerHpBar.setProgress(
                playerHp / 100.0
        );

        bossHpBar.setProgress(
                bossHp / 80.0
        );
    }

    // =========================================================
    // BOSS ELREJTÉSE
    // =========================================================

    public void hideBossFight() {

        bossArea.setVisible(
                false
        );

        bossArea.setManaged(
                false
        );
    }

    // =========================================================
    // NYELV FRISSÍTÉSE
    // =========================================================

    public void updateLanguage() {

        gameTitle.setText(
                Lang.t("ui.title")
        );

        restartButton.setText(
                Lang.t("ui.restart")
        );

        footerLabel.setText(
                Lang.t("ui.footer")
        );
    }

    // =========================================================
    // ROOT LEKÉRÉSE
    // =========================================================

    public BorderPane getRoot() {

        return root;
    }

    // =========================================================
    // ABLAK MEGJELENÍTÉSE
    // =========================================================

    public void show(
            Stage stage
    ) {

        Scene scene =
                new Scene(
                        root,
                        1100,
                        750
                );

        stage.setTitle(
                "Fityesz Krónika"
        );

        stage.setScene(
                scene
        );

        stage.show();
    }
}