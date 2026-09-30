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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;

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
            Font.font("Arial", FontWeight.NORMAL, 16);

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
                "-fx-background-color: " + PAGE_BACKGROUND + ";"
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

        imageArea.setPrefHeight(600);
        imageArea.setMinHeight(600);
        imageArea.setMaxHeight(Double.MAX_VALUE);

        imageArea.setPrefWidth(Double.MAX_VALUE);
        imageArea.setMinWidth(0);
        imageArea.setMaxWidth(Double.MAX_VALUE);

        imageArea.setStyle(
                "-fx-background-color: " + PAGE_BACKGROUND + ";"
        );

        VBox.setVgrow(
                imageArea,
                Priority.ALWAYS
        );

        // =====================================================
        // HÁTTÉRKÉP
        // =====================================================

        backgroundView = new ImageView();

        backgroundView.setPreserveRatio(false);
        backgroundView.setSmooth(true);

        backgroundView.fitWidthProperty().bind(
                imageArea.widthProperty()
        );

        backgroundView.fitHeightProperty().bind(
                imageArea.heightProperty()
        );

        // =====================================================
        // KARAKTER
        // =====================================================

        characterView = new ImageView();

        characterView.setPreserveRatio(true);
        characterView.setSmooth(true);

        characterView.setFitHeight(790);

        characterView.setTranslateY(45);

        Rectangle characterClip =
                new Rectangle(
                        455,
                        611
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


        imageArea.setVisible(true);
        imageArea.setManaged(true);

        // =====================================================
        // DIALOGUE TERÜLET
        // =====================================================

        dialogueArea = new VBox();

        dialogueArea.setAlignment(
                Pos.TOP_LEFT
        );

        dialogueArea.setSpacing(0);

        dialogueArea.setMaxWidth(Double.MAX_VALUE);
        dialogueArea.setPrefWidth(Double.MAX_VALUE);

        dialogueArea.setPadding(
                new Insets(8, 0, 5, 0) //18, 20, 10, 20
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

        dialogueLabel.setMaxWidth(Double.MAX_VALUE);
        dialogueLabel.setPrefWidth(Double.MAX_VALUE);

        dialogueLabel.setMinHeight(70);
        dialogueLabel.setPadding(new Insets(12,25,12,25));

        dialogueLabel.setStyle(
                "-fx-background-color: " + CREAM + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;"
        );

        dialogueArea.getChildren().addAll(
                speakerLabel,
                dialogueLabel
        );

        dialogueArea.setVisible(true);
        dialogueArea.setManaged(true);

        // =====================================================
        // CHOICES
        // =====================================================

        choicesBox = new VBox();

        choicesBox.setAlignment(
                Pos.CENTER
        );

        choicesBox.setSpacing(8);

        choicesBox.setMaxWidth(Double.MAX_VALUE);
        choicesBox.setPrefWidth(Double.MAX_VALUE);

        choicesBox.setPadding(
                new Insets(5, 20, 10, 20)
        );

        choicesBox.setStyle(
                "-fx-background-color: " + DARK + ";"
        );

        VBox overlay = new VBox();

        overlay.setAlignment(Pos.BOTTOM_CENTER);
        overlay.setSpacing(0);
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setPrefWidth(Double.MAX_VALUE);
        overlay.setPrefWidth(Double.MAX_VALUE);

        overlay.getChildren().addAll(
                dialogueArea,
                choicesBox
        );

        StackPane.setAlignment(
                overlay,
                Pos.BOTTOM_CENTER
        );

        StackPane.setMargin(
                overlay,
                new Insets(0)
        );

        imageArea.getChildren().add(
                overlay
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

        overlay.getChildren().add(0, bossArea);

        // =====================================================
        // MINDEN A GAME CONTENT-BE
        // =====================================================

        gameContent.getChildren().add(
                imageArea
        );

        // =====================================================
        // SCROLLPANE
        // =====================================================

        root.setCenter(gameContent);

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

    public void showDialogue(String speaker, String text) {

        dialogueArea.setVisible(true);
        dialogueArea.setManaged(true);

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

        speakerLabel.setVisible(visible);

        speakerLabel.setManaged(visible);
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
                number + "    " + text
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

        button.setMinHeight(40);

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

        button.setOnAction(e -> action.run());

        if (text.equals(Lang.t("ui.enter"))) {
            button.setId("enterButton");
        }

        choicesBox.getChildren().add(button);
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

            backgroundView.setImage(image);

        } else {

            backgroundView.setImage(null);
        }

        imageArea.setVisible(true);
        imageArea.setManaged(true);
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

        backgroundView.setImage(null);

        imageArea.setVisible(true);
        imageArea.setManaged(true);
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
    }

    // =========================================================
    // ROOT LEKÉRÉSE
    // =========================================================

    public BorderPane getRoot() {

        return root;
    }

    public VBox getChoicesBox() {

        return choicesBox;
    }

    public void pressEnterButton() {

        for (javafx.scene.Node node : choicesBox.getChildren()) {

            if (node instanceof Button button) {

                if ("enterButton".equals(button.getId())) {

                    button.fire();
                    return;
                }
            }
        }
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

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {

            if (event.getCode() == KeyCode.ENTER) {

                if (!choicesBox.getChildren().isEmpty()) {

                    Button firstButton =
                            (Button) choicesBox.getChildren().get(0);

                    firstButton.fire();

                    event.consume();
                }
            }
        });

        stage.setTitle(
                "Fityesz Krónika"
        );

        stage.setScene(
                scene
        );

        stage.show();
    }
}