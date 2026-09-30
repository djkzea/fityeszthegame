package com.example.demo;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Game screen, styled after the website (fityeszthegame.com).
 *
 * The public methods used by GameController are unchanged:
 * showDialogue, addChoice, clearChoices, showImage, showCharacter,
 * hideCharacter, hideImage, showBossFight, updateBossHp, hideBossFight,
 * updateStats, updateLanguage, pressEnterButton, getRoot, getChoicesBox, show.
 *
 * New optional methods:
 * setChapter, setLocation, setProgress, setChoicePrompt,
 * showItemAcquired, setOnRestart, installKeys.
 */
public class GameUI {

    // =========================================================
    // COLORS
    // =========================================================

    private static final String PAPER = "#FAF7F0";
    private static final String PAGE = "#F1EDE3";
    private static final String INK = "#161616";
    private static final String RED = "#D20A2E";
    private static final String GREEN = "#2F6B45";
    private static final String GRAY = "#6D6D6D";
    private static final String TRACK = "#E4DDCF";

    // =========================================================
    // FONTS
    // Put these files in src/main/resources/com/example/demo/fonts/
    // If a file is missing the UI falls back to a system font.
    // =========================================================

    private static final String FONT_DIR = "/com/example/demo/fonts/";

    private static final String HEAD;   // condensed headlines
    private static final String LABEL;  // small uppercase labels
    private static final String BODY;   // dialogue text

    static {
        loadFont("BebasNeue-Regular.ttf");
        loadFont("Archivo-Bold.ttf");
        loadFont("PublicSans-Regular.ttf");
        loadFont("PublicSans-Italic.ttf");

        HEAD = pickFamily("Bebas Neue", "Impact");
        LABEL = pickFamily("Archivo", "Arial");
        BODY = pickFamily("Public Sans", "Segoe UI");
    }

    private static void loadFont(String file) {
        try (InputStream in = GameUI.class.getResourceAsStream(FONT_DIR + file)) {
            if (in != null) {
                Font.loadFont(in, 12);
            }
        } catch (Exception ignored) {
        }
    }

    private static String pickFamily(String wanted, String fallback) {
        return Font.getFamilies().contains(wanted) ? wanted : fallback;
    }

    private static Font head(double size) {
        return Font.font(HEAD, size);
    }

    private static Font label(double size) {
        return Font.font(LABEL, FontWeight.BOLD, size);
    }

    private static Font body(double size) {
        return Font.font(BODY, FontWeight.NORMAL, size);
    }

    private static Font bodyItalic(double size) {
        return Font.font(BODY, FontWeight.NORMAL, FontPosture.ITALIC, size);
    }

    // =========================================================
    // LAYOUT NODES
    // =========================================================

    private final BorderPane root;

    // header
    private final Label chapterTitle;
    private final Label playerLink;
    private final Label xpName, xpValue;
    private final Label exposureName, exposureValue;
    private final Label itemsName, itemsValue;
    private final Bar exposureBar;
    private final Button restartButton;
    private final Region progressFill;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    // scene
    private final StackPane stagePane;
    private final ImageView backgroundView;
    private final Label ghostLabel;
    private final ImageView characterView;
    private final HBox locationStrip;
    private final Label locationLabel;
    private final VBox characterTag;
    private final Label characterNameLabel;
    private final Label characterRoleLabel;

    // dialogue
    private final VBox overlay;
    private final VBox dialogueWrap;
    private final Label speakerTab;
    private final VBox dialogueBox;
    private final Text shownText = new Text();
    private final Text pendingText = new Text();
    private final Label hintLabel;

    // choices
    private final VBox choicesPanel;
    private final Label choicePromptLabel;
    private final Label chooseCountLabel;
    private final VBox choicesBox;

    // title card
    private final StackPane titleCard;
    private final Label titleKicker;
    private final Label titleMain;
    private final Label titleQuote;
    private final Label titleHint;

    // item popup
    private final VBox itemPopup;
    private final Label itemKicker;
    private final Label itemName;

    // boss
    private final VBox bossArea;
    private final Label bossNameLabel;
    private final Label playerHpLabel;
    private final Label bossHpLabel;
    private final Bar playerHpBar;
    private final Bar bossHpBar;
    private final VBox abilitiesBox;
    private final Label abilitiesHeader;

    // =========================================================
    // STATE
    // =========================================================

    private final Map<String, Image> imageCache = new HashMap<>();

    private Timeline typing;
    private boolean typingActive = false;
    private String fullText = "";

    private boolean dialogueShown = false;
    private boolean enterMode = false;
    private boolean panelWasVisible = false;

    private String currentBg = null;
    private String currentCharacter = null;
    private String chapterName = null;
    private int lastXp = 0;

    private ParallelTransition characterAnim;
    private SequentialTransition itemAnim;

    // =========================================================
    // SMALL BAR CONTROL (replaces ProgressBar, fully styleable)
    // =========================================================

    private static final class Bar extends StackPane {

        private final Region fill = new Region();
        private final DoubleProperty value = new SimpleDoubleProperty(0);

        Bar(double width, double height, String fillColor, boolean bordered) {

            setAlignment(Pos.CENTER_LEFT);

            setMinHeight(height);
            setPrefHeight(height);
            setMaxHeight(height);

            if (width > 0) {
                setMinWidth(width);
                setPrefWidth(width);
                setMaxWidth(width);
            }

            double inset = bordered ? 2 : 0;

            setStyle(
                    "-fx-background-color: " + TRACK + ";" +
                            (bordered
                                    ? "-fx-border-color: " + INK + "; -fx-border-width: 1;"
                                    : "")
            );

            fill.setStyle("-fx-background-color: " + fillColor + ";");
            fill.setMinWidth(0);
            fill.setPrefHeight(height - inset);
            fill.setMaxHeight(height - inset);
            fill.prefWidthProperty().bind(
                    widthProperty().subtract(inset).multiply(value)
            );
            fill.maxWidthProperty().bind(fill.prefWidthProperty());

            getChildren().add(fill);
        }

        void set(double v, boolean animate) {

            double target = Math.max(0, Math.min(1, v));

            if (!animate) {
                value.set(target);
                return;
            }

            new Timeline(
                    new KeyFrame(
                            Duration.millis(420),
                            new KeyValue(value, target)
                    )
            ).play();
        }
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GameUI() {

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + PAGE + ";");

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        chapterTitle = new Label(Lang.t("ui.title").toUpperCase());
        chapterTitle.setFont(head(30));
        chapterTitle.setTextFill(Color.web(INK));

        playerLink = new Label("");
        playerLink.setFont(body(12));
        playerLink.setUnderline(true);
        playerLink.setTextFill(Color.web(INK));
        playerLink.setCursor(javafx.scene.Cursor.HAND);
        setShown(playerLink, false);

        VBox titleBox = new VBox(0, chapterTitle, playerLink);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        xpName = statName(Lang.t("ui.xp"));
        xpValue = statValue("0");
        HBox xpBox = statBox(xpName, xpValue);

        exposureName = statName(Lang.t("ui.exposure"));
        exposureBar = new Bar(100, 10, RED, true);
        exposureValue = statSmall("0/100");
        HBox exposureBox = new HBox(9, exposureName, exposureBar, exposureValue);
        exposureBox.setAlignment(Pos.CENTER);

        itemsName = statName(Lang.t("ui.items"));
        itemsValue = statValue("0");
        HBox itemsBox = statBox(itemsName, itemsValue);

        restartButton = new Button(Lang.t("ui.restart"));
        restartButton.setFont(label(11));
        restartButton.setFocusTraversable(false);
        styleOutlineButton(restartButton);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox headerRow = new HBox(28, titleBox, headerSpacer, xpBox, exposureBox, itemsBox, restartButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(14, 24, 12, 16));

        // thin progress line under the header
        StackPane progressLine = new StackPane();
        progressLine.setAlignment(Pos.CENTER_LEFT);
        progressLine.setMinHeight(5);
        progressLine.setPrefHeight(5);
        progressLine.setMaxHeight(5);
        progressLine.setStyle("-fx-background-color: #D9D2C3;");

        progressFill = new Region();
        progressFill.setStyle("-fx-background-color: " + RED + ";");
        progressFill.setMinWidth(0);
        progressFill.prefWidthProperty().bind(progressLine.widthProperty().multiply(progress));
        progressFill.maxWidthProperty().bind(progressFill.prefWidthProperty());
        progressLine.getChildren().add(progressFill);

        VBox header = new VBox(headerRow, progressLine);
        header.setStyle("-fx-background-color: " + PAPER + ";");
        root.setTop(header);

        // -----------------------------------------------------
        // SCENE (stack of layers)
        // -----------------------------------------------------

        stagePane = new StackPane();
        stagePane.setMinSize(0, 0);
        stagePane.setStyle("-fx-background-color: #161616;");

        Rectangle stageClip = new Rectangle();
        stageClip.widthProperty().bind(stagePane.widthProperty());
        stageClip.heightProperty().bind(stagePane.heightProperty());
        stagePane.setClip(stageClip);

        // ghost chapter name on dark scenes
        ghostLabel = new Label("");
        ghostLabel.setFont(head(300));
        ghostLabel.setTextFill(Color.web(RED, 0.16));
        ghostLabel.setMinWidth(Region.USE_PREF_SIZE);
        ghostLabel.setLayoutX(-8);
        ghostLabel.setLayoutY(40);

        Pane ghostPane = new Pane(ghostLabel);
        ghostPane.setMouseTransparent(true);

        // background image, "cover" fit
        backgroundView = new ImageView();
        backgroundView.setManaged(false);
        backgroundView.setSmooth(true);
        backgroundView.setMouseTransparent(true);
        stagePane.widthProperty().addListener((o, a, b) -> updateCover());
        stagePane.heightProperty().addListener((o, a, b) -> updateCover());

        // dotted texture
        Region dots = new Region();
        dots.setBackground(dotBackground(0.10));
        dots.setMouseTransparent(true);

        // dark fade behind the text boxes
        Region bottomFade = new Region();
        bottomFade.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0d0a08", 0)),
                        new Stop(1, Color.web("#0d0a08", 0.88))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        bottomFade.maxHeightProperty().bind(stagePane.heightProperty().multiply(0.5));
        bottomFade.prefHeightProperty().bind(stagePane.heightProperty().multiply(0.5));
        bottomFade.setMouseTransparent(true);
        StackPane.setAlignment(bottomFade, Pos.BOTTOM_CENTER);

        // character (right side, behind the dialogue box)
        characterView = new ImageView();
        characterView.setPreserveRatio(true);
        characterView.setSmooth(true);
        characterView.setMouseTransparent(true);
        characterView.fitHeightProperty().bind(stagePane.heightProperty().multiply(0.86));
        StackPane.setAlignment(characterView, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(characterView, new Insets(0, 60, 0, 0));
        setShown(characterView, false);

        // location strip (top-left)
        Circle dot = new Circle(4, Color.web(RED));
        locationLabel = new Label("");
        locationLabel.setFont(label(12));
        locationLabel.setTextFill(Color.web("#FFF8EC"));
        locationLabel.setEffect(new DropShadow(4, 0, 1, Color.rgb(0, 0, 0, 0.9)));
        locationStrip = new HBox(9, dot, locationLabel);
        locationStrip.setAlignment(Pos.CENTER_LEFT);
        locationStrip.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        locationStrip.setMouseTransparent(true);
        StackPane.setAlignment(locationStrip, Pos.TOP_LEFT);
        StackPane.setMargin(locationStrip, new Insets(20, 0, 0, 20));
        setShown(locationStrip, false);

        // character name tag (top-right)
        characterNameLabel = new Label();
        characterNameLabel.setFont(head(30));
        characterNameLabel.setTextFill(Color.web("#FFF8EC"));

        characterRoleLabel = new Label();
        characterRoleLabel.setFont(label(11));
        characterRoleLabel.setTextFill(Color.web("#D9D2C3"));

        characterTag = new VBox(0, characterNameLabel, characterRoleLabel);
        characterTag.setPadding(new Insets(9, 16, 10, 16));
        characterTag.setStyle(
                "-fx-background-color: " + INK + ";" +
                        "-fx-border-color: #FFF8EC;" +
                        "-fx-border-width: 2;"
        );
        characterTag.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        characterTag.setMouseTransparent(true);
        StackPane.setAlignment(characterTag, Pos.TOP_RIGHT);
        StackPane.setMargin(characterTag, new Insets(58, 46, 0, 0));
        setShown(characterTag, false);

        // -----------------------------------------------------
        // DIALOGUE BOX (speaker tab + cream box)
        // -----------------------------------------------------

        speakerTab = new Label("");
        speakerTab.setFont(head(24));
        speakerTab.setTextFill(Color.web("#FFF8EC"));
        speakerTab.setPadding(new Insets(6, 16, 3, 16));
        speakerTab.setStyle("-fx-background-color: " + RED + ";");
        speakerTab.setMaxWidth(Region.USE_PREF_SIZE);

        shownText.setFont(body(18));
        shownText.setFill(Color.web(INK));
        pendingText.setFont(body(18));
        pendingText.setFill(Color.TRANSPARENT);

        TextFlow textFlow = new TextFlow(shownText, pendingText);
        textFlow.setLineSpacing(5);
        textFlow.setMinHeight(58);
        VBox.setVgrow(textFlow, Priority.ALWAYS);

        hintLabel = new Label("");
        hintLabel.setFont(label(10.5));
        hintLabel.setTextFill(Color.web(GRAY));
        hintLabel.setMaxWidth(Double.MAX_VALUE);
        hintLabel.setAlignment(Pos.CENTER_RIGHT);
        setShown(hintLabel, false);

        dialogueBox = new VBox(10, textFlow, hintLabel);
        dialogueBox.setPadding(new Insets(24, 30, 14, 30));
        dialogueBox.setMinHeight(127);
        dialogueBox.setStyle("-fx-background-color: " + PAPER + ";");
        dialogueBox.setEffect(new DropShadow(24, 0, 6, Color.rgb(0, 0, 0, 0.35)));

        dialogueWrap = new VBox(0, speakerTab, dialogueBox);
        dialogueWrap.setAlignment(Pos.TOP_LEFT);
        dialogueWrap.setCursor(javafx.scene.Cursor.HAND);
        dialogueWrap.setOnMouseClicked(e -> advance());
        setShown(dialogueWrap, false);

        // -----------------------------------------------------
        // CHOICES PANEL
        // -----------------------------------------------------

        choicePromptLabel = new Label("");
        choicePromptLabel.setFont(head(34));
        choicePromptLabel.setTextFill(Color.web(INK));

        chooseCountLabel = new Label("");
        chooseCountLabel.setFont(label(11));
        chooseCountLabel.setTextFill(Color.web(GRAY));

        Region choiceSpacer = new Region();
        HBox.setHgrow(choiceSpacer, Priority.ALWAYS);

        HBox choiceHeader = new HBox(choicePromptLabel, choiceSpacer, chooseCountLabel);
        choiceHeader.setAlignment(Pos.CENTER_LEFT);

        choicesBox = new VBox(10);

        choicesPanel = new VBox(12, choiceHeader, choicesBox);
        choicesPanel.setPadding(new Insets(20, 26, 24, 26));
        choicesPanel.setStyle("-fx-background-color: " + PAPER + ";");
        choicesPanel.setEffect(new DropShadow(24, 0, 6, Color.rgb(0, 0, 0, 0.35)));
        setShown(choicesPanel, false);

        // -----------------------------------------------------
        // BOSS AREA
        // -----------------------------------------------------

        bossNameLabel = new Label();
        bossNameLabel.setFont(head(28));
        bossNameLabel.setTextFill(Color.web(RED));

        playerHpLabel = new Label();
        playerHpLabel.setFont(label(11));
        playerHpLabel.setTextFill(Color.web(INK));

        bossHpLabel = new Label();
        bossHpLabel.setFont(label(11));
        bossHpLabel.setTextFill(Color.web(INK));

        playerHpBar = new Bar(0, 12, GREEN, true);
        playerHpBar.setMaxWidth(Double.MAX_VALUE);
        bossHpBar = new Bar(0, 12, RED, true);
        bossHpBar.setMaxWidth(Double.MAX_VALUE);

        abilitiesHeader = new Label("");
        abilitiesHeader.setFont(label(10.5));
        abilitiesHeader.setTextFill(Color.web(GRAY));

        abilitiesBox = new VBox(6, abilitiesHeader);
        abilitiesBox.setPadding(new Insets(12, 0, 0, 0));
        setShown(abilitiesBox, false);

        bossArea = new VBox(6, bossNameLabel, playerHpLabel, playerHpBar, bossHpLabel, bossHpBar, abilitiesBox);
        bossArea.setPadding(new Insets(16, 22, 18, 22));
        bossArea.setMaxWidth(640);
        bossArea.setStyle(
                "-fx-background-color: " + PAPER + ";" +
                        "-fx-border-color: " + INK + ";" +
                        "-fx-border-width: 1;"
        );
        setShown(bossArea, false);

        // -----------------------------------------------------
        // BOTTOM OVERLAY
        // -----------------------------------------------------

        overlay = new VBox(12, bossArea, dialogueWrap, choicesPanel);
        overlay.setAlignment(Pos.BOTTOM_LEFT);
        overlay.setPadding(new Insets(0, 34, 26, 34));
        overlay.setMaxWidth(Double.MAX_VALUE);
        overlay.setPickOnBounds(false);
        StackPane.setAlignment(overlay, Pos.BOTTOM_CENTER);

        // -----------------------------------------------------
        // ITEM POPUP
        // -----------------------------------------------------

        itemKicker = new Label(Lang.t("ui.item"));
        itemKicker.setFont(label(12));
        itemKicker.setTextFill(Color.web(RED));

        itemName = new Label("");
        itemName.setFont(head(58));
        itemName.setTextFill(Color.web(INK));

        itemPopup = new VBox(0, itemKicker, itemName);
        itemPopup.setAlignment(Pos.CENTER);
        itemPopup.setPadding(new Insets(14, 48, 12, 48));
        itemPopup.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + RED + ";" +
                        "-fx-border-width: 2;"
        );
        itemPopup.setEffect(new DropShadow(30, 0, 10, Color.rgb(0, 0, 0, 0.5)));
        itemPopup.setRotate(-3);
        itemPopup.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        itemPopup.setMouseTransparent(true);
        StackPane.setAlignment(itemPopup, Pos.CENTER);
        setShown(itemPopup, false);

        // -----------------------------------------------------
        // TITLE CARD (full red chapter card)
        // -----------------------------------------------------

        titleKicker = new Label("");
        titleKicker.setFont(label(15));
        titleKicker.setTextFill(Color.web("#FFF8EC"));

        titleMain = new Label("");
        titleMain.setFont(head(112));
        titleMain.setTextFill(Color.web("#FFF8EC"));

        titleQuote = new Label("");
        titleQuote.setFont(bodyItalic(23));
        titleQuote.setTextFill(Color.web("#FFF8EC"));
        titleQuote.setWrapText(true);
        titleQuote.setMaxWidth(760);
        titleQuote.setAlignment(Pos.CENTER);
        titleQuote.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        titleHint = new Label("");
        titleHint.setFont(label(11));
        titleHint.setTextFill(Color.web("#FFF8EC"));

        VBox titleContent = new VBox(6, titleKicker, titleMain, titleQuote, titleHint);
        titleContent.setAlignment(Pos.CENTER);
        VBox.setMargin(titleHint, new Insets(18, 0, 0, 0));

        Region titleDots = new Region();
        titleDots.setBackground(dotBackground(0.12));
        titleDots.setMouseTransparent(true);

        titleCard = new StackPane(titleDots, titleContent);
        titleCard.setStyle("-fx-background-color: " + RED + ";");
        titleCard.setCursor(javafx.scene.Cursor.HAND);
        titleCard.setOnMouseClicked(e -> advance());
        setShown(titleCard, false);

        // -----------------------------------------------------
        // ASSEMBLE
        // -----------------------------------------------------

        stagePane.getChildren().addAll(
                ghostPane,
                backgroundView,
                dots,
                bottomFade,
                characterView,
                locationStrip,
                characterTag,
                overlay,
                itemPopup,
                titleCard
        );

        root.setCenter(stagePane);

        updateGhost();
    }

    // =========================================================
    // HELPERS - construction
    // =========================================================

    private Label statName(String text) {
        Label l = new Label(text.toUpperCase());
        l.setFont(label(11.5));
        l.setTextFill(Color.web(INK));
        return l;
    }

    private Label statValue(String text) {
        Label l = new Label(text);
        l.setFont(label(20));
        l.setTextFill(Color.web(INK));
        return l;
    }

    private Label statSmall(String text) {
        Label l = new Label(text);
        l.setFont(label(12));
        l.setTextFill(Color.web(INK));
        return l;
    }

    private HBox statBox(Label name, Label value) {
        HBox box = new HBox(8, name, value);
        box.setAlignment(Pos.BASELINE_LEFT);
        return box;
    }

    private static Background dotBackground(double alpha) {
        WritableImage img = new WritableImage(6, 6);
        img.getPixelWriter().setColor(2, 2, Color.color(1, 1, 1, alpha));
        return new Background(new BackgroundImage(
                img,
                BackgroundRepeat.REPEAT,
                BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT
        ));
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void styleOutlineButton(Button button) {

        Runnable normal = () -> {
            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-background-radius: 0;" +
                            "-fx-border-color: " + INK + ";" +
                            "-fx-border-width: 1.5;" +
                            "-fx-text-fill: " + INK + ";" +
                            "-fx-padding: 8 15 8 15;" +
                            "-fx-cursor: hand;"
            );
        };

        normal.run();

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + INK + ";" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: " + INK + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 8 15 8 15;" +
                        "-fx-cursor: hand;"
        ));

        button.setOnMouseExited(e -> normal.run());
    }

    // =========================================================
    // DIALOGUE
    // =========================================================

    public void showDialogue(String speaker, String text) {

        String sp = speaker == null ? "" : speaker.trim();
        String tx = text == null ? "" : text;

        // "CHAPTER" speaker = full red chapter card
        if (!sp.isEmpty() && sp.equalsIgnoreCase(Lang.t("ui.chapter"))) {
            showTitleCard(tx);
            return;
        }

        hideTitleCard();

        dialogueShown = true;

        boolean narration = sp.isEmpty()
                || sp.equalsIgnoreCase(Lang.t("npc.you"))
                || sp.equalsIgnoreCase(Lang.t("ui.you"));

        speakerTab.setText(sp.toUpperCase());
        setShown(speakerTab, !narration);

        Font f = narration ? bodyItalic(18) : body(18);
        shownText.setFont(f);
        pendingText.setFont(f);

        // asymptotic progress: every line moves the line a little forward
        setProgress(progress.get() + (1 - progress.get()) * 0.07);

        startTyping(tx);
        refreshLayout();
    }

    private void startTyping(String text) {

        stopTyping();

        fullText = text;

        if (text.isEmpty()) {
            shownText.setText("");
            pendingText.setText("");
            typingActive = false;
            return;
        }

        shownText.setText("");
        pendingText.setText(text);

        final int[] index = {0};

        typing = new Timeline(new KeyFrame(Duration.millis(12), e -> {
            index[0] = Math.min(text.length(), index[0] + 1);
            shownText.setText(text.substring(0, index[0]));
            pendingText.setText(text.substring(index[0]));
        }));

        typing.setCycleCount(text.length());
        typing.setOnFinished(e -> finishTyping());

        typingActive = true;
        typing.play();
    }

    private void stopTyping() {
        if (typing != null) {
            typing.stop();
            typing = null;
        }
        typingActive = false;
    }

    private void finishTyping() {

        if (typing != null) {
            typing.stop();
            typing = null;
        }

        shownText.setText(fullText);
        pendingText.setText("");
        typingActive = false;

        refreshLayout();
    }

    // =========================================================
    // TITLE CARD
    // =========================================================

    private void showTitleCard(String text) {

        stopTyping();

        String[] parts = text.split("\n\n");

        String title = parts.length > 0 ? parts[0] : "";
        String quote = parts.length > 1 ? parts[1] : "";
        String place = parts.length > 2 ? parts[2] : "";

        titleKicker.setText(spaced(Lang.t("ui.chapter").toUpperCase()));
        titleMain.setText(title.toUpperCase());
        titleMain.setFont(head(title.length() > 16 ? 84 : 112));
        titleQuote.setText(quote);
        titleHint.setText(Lang.t("ui.enter"));

        chapterName = title.toUpperCase();
        chapterTitle.setText(Lang.t("ui.chapter").toUpperCase() + " · " + chapterName);

        setLocation(place);
        setProgress(0);

        dialogueShown = false;

        if (!titleCard.isVisible()) {
            setShown(titleCard, true);
            titleCard.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(380), titleCard);
            fade.setToValue(1);
            fade.play();
        }

        updateGhost();
        refreshLayout();
    }

    private void hideTitleCard() {
        if (titleCard.isVisible()) {
            setShown(titleCard, false);
            updateGhost();
        }
    }

    /** Thin-space letterspacing for small uppercase kickers. */
    private static String spaced(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i));
            if (i < s.length() - 1) {
                sb.append('\u2009');
            }
        }
        return sb.toString();
    }

    // =========================================================
    // LAYOUT REFRESH (what is visible right now)
    // =========================================================

    private void refreshLayout() {

        boolean card = titleCard.isVisible();

        setShown(dialogueWrap, dialogueShown && !card);

        setShown(hintLabel, enterMode && !card);

        boolean hasChoices = false;
        int count = 0;
        for (Node n : choicesBox.getChildren()) {
            if (n.isVisible()) {
                hasChoices = true;
                count++;
            }
        }

        boolean panel = !card && !enterMode && hasChoices && !typingActive;

        setShown(choicePromptLabel, !choicePromptLabel.getText().isEmpty());
        chooseCountLabel.setText(
                (Lang.t("ui.choose").toUpperCase()) + " " + (count > 1 ? "1–" + count : "1")
        );

        setShown(choicesPanel, panel);

        if (panel && !panelWasVisible) {
            choicesPanel.setOpacity(0);
            choicesPanel.setTranslateY(14);

            FadeTransition fade = new FadeTransition(Duration.millis(240), choicesPanel);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(240), choicesPanel);
            slide.setToY(0);

            new ParallelTransition(fade, slide).play();
        }

        panelWasVisible = panel;
    }

    // =========================================================
    // CHOICES
    // =========================================================

    public void clearChoices() {

        choicesBox.getChildren().clear();
        enterMode = false;
        panelWasVisible = false;

        choicePromptLabel.setText("");

        refreshLayout();
    }

    /** Optional heading above the choices, e.g. Lang.t("ch1.q1"). Cleared by clearChoices(). */
    public void setChoicePrompt(String prompt) {
        choicePromptLabel.setText(prompt == null ? "" : prompt.toUpperCase());
        refreshLayout();
    }

    public void addChoice(int number, String text, Runnable action) {

        Button button = new Button();
        button.setFocusTraversable(false);
        button.setOnAction(e -> action.run());

        // The "Press Enter to continue" choice is shown as a hint inside the dialogue box
        if (text.equals(Lang.t("ui.enter"))) {

            button.setId("enterButton");
            setShown(button, false);

            enterMode = true;
            hintLabel.setText(text);
            titleHint.setText(text);

            choicesBox.getChildren().add(button);
            refreshLayout();
            return;
        }

        Label numberBox = new Label(String.valueOf(number));
        numberBox.setFont(label(13));
        numberBox.setTextFill(Color.WHITE);
        numberBox.setAlignment(Pos.CENTER);
        numberBox.setMinSize(30, 30);
        numberBox.setPrefSize(30, 30);
        numberBox.setMaxSize(30, 30);

        button.setText(text);
        button.setGraphic(numberBox);
        button.setGraphicTextGap(16);
        button.setFont(body(17));
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(52);

        styleChoice(button, numberBox, false);

        button.setOnMouseEntered(e -> styleChoice(button, numberBox, true));
        button.setOnMouseExited(e -> styleChoice(button, numberBox, false));

        choicesBox.getChildren().add(button);
        refreshLayout();
    }

    private void styleChoice(Button button, Label numberBox, boolean hover) {

        button.setStyle(
                "-fx-background-color: " + (hover ? INK : "#F1EDE3") + ";" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: " + INK + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: " + (hover ? "white" : INK) + ";" +
                        "-fx-padding: 10 18 10 11;" +
                        "-fx-cursor: hand;"
        );

        numberBox.setStyle(
                "-fx-background-color: " + (hover ? RED : INK) + ";"
        );
    }

    // =========================================================
    // KEYBOARD / CLICK ADVANCE
    // =========================================================

    private void advance() {
        pressEnterButton();
    }

    /** Enter: finishes the typewriter first, then continues. */
    public void pressEnterButton() {

        if (typingActive) {
            finishTyping();
            return;
        }

        for (Node node : choicesBox.getChildren()) {
            if (node instanceof Button button && "enterButton".equals(button.getId())) {
                button.fire();
                return;
            }
        }
    }

    /** Keys 1-9: picks the n-th visible choice. Returns true if a choice was fired. */
    public boolean pressChoice(int n) {

        if (!choicesPanel.isVisible()) {
            return false;
        }

        int index = 0;

        for (Node node : choicesBox.getChildren()) {
            if (node instanceof Button button && button.isVisible()) {
                index++;
                if (index == n) {
                    button.fire();
                    return true;
                }
            }
        }

        return false;
    }

    /** Installs Enter / Space / 1-9 handling on a scene. */
    public void installKeys(Scene scene) {

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            KeyCode code = event.getCode();

            if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                pressEnterButton();
                event.consume();
                return;
            }

            int digit = -1;

            if (code.isDigitKey()) {
                digit = Integer.parseInt(code.getName());
            } else if (code.isKeypadKey()) {
                try {
                    digit = Integer.parseInt(code.getName().replace("Numpad ", ""));
                } catch (NumberFormatException ignored) {
                }
            }

            if (digit >= 1 && pressChoice(digit)) {
                event.consume();
            }
        });
    }

    // =========================================================
    // STATS / HEADER
    // =========================================================

    public void updateStats(GameState state) {

        if (state.getName() == null || state.getName().isBlank()) {
            setShown(playerLink, false);
        } else {
            playerLink.setText(
                    state.getName() + " — " + (Lang.magyar() ? "Nem te vagy?" : "Not you?")
            );
            setShown(playerLink, true);
        }

        xpValue.setText(String.valueOf(state.getXp()));

        if (state.getXp() > lastXp) {
            pop(xpValue);
        }
        lastXp = state.getXp();

        exposureValue.setText(state.getExposure() + "/100");
        exposureBar.set(state.getExposure() / 100.0, true);

        int items = 0;
        if (state.hasFirstEnvelope()) items++;
        if (state.hasSmallEnvelope()) items++;
        if (state.hasLakatosFile()) items++;
        if (state.hasOffshoreCode()) items++;
        if (state.hasPeteriDossier()) items++;
        if (state.hasParliamentKey()) items++;

        if (!String.valueOf(items).equals(itemsValue.getText())) {
            itemsValue.setText(String.valueOf(items));
            pop(itemsValue);
        }
    }

    private void pop(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(1.3);
        st.setToY(1.3);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    /** Header title, e.g. "CHAPTER 1 · THE RECRUITMENT". */
    public void setChapter(String text) {
        chapterTitle.setText(text.toUpperCase());

        int dot = text.indexOf('·');
        chapterName = (dot >= 0 ? text.substring(dot + 1) : text).trim().toUpperCase();

        updateGhost();
    }

    /** 0.0 - 1.0, animated. */
    public void setProgress(double value) {
        double target = Math.max(0, Math.min(1, value));
        new Timeline(
                new KeyFrame(Duration.millis(350), new KeyValue(progress, target))
        ).play();
    }

    /** Red-dot location line, top-left of the scene. Empty text hides it. */
    public void setLocation(String text) {
        boolean has = text != null && !text.isBlank();
        locationLabel.setText(has ? text.toUpperCase() : "");
        setShown(locationStrip, has);
    }

    public void setOnRestart(Runnable action) {
        restartButton.setOnAction(e -> action.run());
        playerLink.setOnMouseClicked((MouseEvent e) -> action.run());
    }

    // =========================================================
    // IMAGES
    // =========================================================

    public void showImage(String path) {

        Image image = loadImage(path);

        if (image == null) {
            backgroundView.setImage(null);
            currentBg = null;
        } else if (!path.equals(currentBg)) {
            backgroundView.setImage(image);
            currentBg = path;
            updateCover();

            backgroundView.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(500), backgroundView);
            fade.setToValue(1);
            fade.play();
        }

        updateGhost();
    }

    public void hideImage() {
        backgroundView.setImage(null);
        currentBg = null;
        updateGhost();
    }

    private void updateGhost() {
        boolean dark = backgroundView.getImage() == null && !titleCard.isVisible();
        ghostLabel.setVisible(dark);

        String text = chapterName != null ? chapterName : Lang.t("ui.title").toUpperCase();
        ghostLabel.setText(text);
    }

    /** "Cover" fit: fills the scene and crops the overflow instead of stretching. */
    private void updateCover() {

        Image img = backgroundView.getImage();
        double w = stagePane.getWidth();
        double h = stagePane.getHeight();

        if (img == null || w <= 0 || h <= 0) {
            return;
        }

        double imageRatio = img.getWidth() / img.getHeight();
        double paneRatio = w / h;

        double vw;
        double vh;

        if (paneRatio > imageRatio) {
            vw = img.getWidth();
            vh = vw / paneRatio;
        } else {
            vh = img.getHeight();
            vw = vh * paneRatio;
        }

        backgroundView.setViewport(new Rectangle2D(
                (img.getWidth() - vw) / 2,
                (img.getHeight() - vh) / 2,
                vw,
                vh
        ));

        backgroundView.setFitWidth(w);
        backgroundView.setFitHeight(h);
        backgroundView.relocate(0, 0);
    }

    private Image loadImage(String path) {

        if (path == null) {
            return null;
        }

        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }

        Image image = null;

        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream != null) {
                image = new Image(stream);
            }
        } catch (Exception ignored) {
        }

        if (image == null) {
            try {
                String clean = path.startsWith("/") ? path.substring(1) : path;
                try (InputStream stream = getClass().getClassLoader().getResourceAsStream(clean)) {
                    if (stream != null) {
                        image = new Image(stream);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (image == null) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    image = new Image(file.toURI().toString());
                }
            } catch (Exception ignored) {
            }
        }

        if (image != null) {
            imageCache.put(path, image);
        }

        return image;
    }

    // =========================================================
    // CHARACTER
    // =========================================================

    public void showCharacter(String path) {

        Image image = loadImage(path);

        if (image == null) {
            hideCharacter();
            return;
        }

        boolean wasVisible = characterView.isVisible() && currentCharacter != null;
        boolean changed = !path.equals(currentCharacter);

        characterView.setImage(image);
        setShown(characterView, true);
        currentCharacter = path;

        if (changed) {

            if (characterAnim != null) {
                characterAnim.stop();
            }

            FadeTransition fade = new FadeTransition(Duration.millis(wasVisible ? 200 : 420), characterView);
            fade.setFromValue(wasVisible ? 0.35 : 0);
            fade.setToValue(1);

            characterAnim = new ParallelTransition(fade);

            if (!wasVisible) {
                characterView.setTranslateX(40);
                TranslateTransition slide = new TranslateTransition(Duration.millis(420), characterView);
                slide.setToX(0);
                characterAnim.getChildren().add(slide);
            } else {
                characterView.setTranslateX(0);
            }

            characterAnim.play();
        }

        setShown(characterTag, false);
    }

    public void showCharacter(String path, String name, String role) {

        boolean tagWasVisible = characterTag.isVisible();

        showCharacter(path);

        characterNameLabel.setText(name.toUpperCase());
        characterRoleLabel.setText(role.toUpperCase());

        setShown(characterTag, true);

        if (!tagWasVisible) {
            characterTag.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(350), characterTag);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(150));
            fade.play();
        }
    }

    public void hideCharacter() {
        characterView.setImage(null);
        currentCharacter = null;
        setShown(characterView, false);
        setShown(characterTag, false);
    }

    // =========================================================
    // ITEM POPUP
    // =========================================================

    public void showItemAcquired(String name) {

        if (itemAnim != null) {
            itemAnim.stop();
        }

        itemKicker.setText(Lang.t("ui.item"));
        itemName.setText(name.toUpperCase());

        setShown(itemPopup, true);
        itemPopup.setOpacity(0);
        itemPopup.setScaleX(0.7);
        itemPopup.setScaleY(0.7);

        ScaleTransition scale = new ScaleTransition(Duration.millis(260), itemPopup);
        scale.setToX(1);
        scale.setToY(1);

        FadeTransition in = new FadeTransition(Duration.millis(200), itemPopup);
        in.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.millis(1700));

        FadeTransition out = new FadeTransition(Duration.millis(400), itemPopup);
        out.setToValue(0);

        itemAnim = new SequentialTransition(new ParallelTransition(scale, in), hold, out);
        itemAnim.setOnFinished(e -> setShown(itemPopup, false));
        itemAnim.play();
    }

    // =========================================================
    // BOSS FIGHT
    // =========================================================

    public void showBossFight(String bossName, int playerHp, int bossHp) {
        setShown(bossArea, true);
        updateBossHp(bossName, playerHp, bossHp);
    }

    public void updateBossHp(String bossName, int playerHp, int bossHp) {

        boolean hu = Lang.magyar();

        bossNameLabel.setText(
                bossName == null ? (hu ? "ELLENFÉL" : "OPPONENT") : bossName.toUpperCase()
        );

        playerHp = Math.max(0, Math.min(100, playerHp));
        bossHp = Math.max(0, Math.min(80, bossHp));

        playerHpLabel.setText((hu ? "JÁTÉKOS ÉLETERŐ: " : "PLAYER HEALTH: ") + playerHp + " / 100");
        bossHpLabel.setText((hu ? "ELLENFÉL ÉLETERŐ: " : "OPPONENT HEALTH: ") + bossHp + " / 80");

        playerHpBar.set(playerHp / 100.0, true);
        bossHpBar.set(bossHp / 80.0, true);
    }

    public void hideBossFight() {
        setShown(bossArea, false);
        clearBossAbilities();
    }

    /** Removes all ability rows. Call before adding the abilities of a new fight. */
    public void clearBossAbilities() {
        abilitiesBox.getChildren().setAll(abilitiesHeader);
        setShown(abilitiesBox, false);
    }

    /**
     * Adds one special-ability row to the boss box. The box is shown together with
     * the boss HP bars, so the ability stays visible during the whole fight.
     */
    public void addBossAbility(String name, String description) {

        Region stripe = new Region();
        stripe.setMinWidth(4);
        stripe.setPrefWidth(4);
        stripe.setMaxWidth(4);
        stripe.setStyle("-fx-background-color: " + RED + ";");

        Label nameLabel = new Label(name.toUpperCase());
        nameLabel.setFont(head(22));
        nameLabel.setTextFill(Color.web(RED));
        nameLabel.setMinWidth(Region.USE_PREF_SIZE);

        Label descLabel = new Label(description);
        descLabel.setFont(body(14));
        descLabel.setTextFill(Color.web(INK));
        descLabel.setWrapText(true);
        HBox.setHgrow(descLabel, Priority.ALWAYS);

        HBox row = new HBox(10, stripe, nameLabel, descLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        abilitiesBox.getChildren().add(row);

        int count = abilitiesBox.getChildren().size() - 1;

        abilitiesHeader.setText(
                (count > 1 ? Lang.t("fight.abilities") : Lang.t("fight.ability")).toUpperCase()
        );

        if (!abilitiesBox.isVisible()) {
            setShown(abilitiesBox, true);
            abilitiesBox.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(350), abilitiesBox);
            fade.setToValue(1);
            fade.play();
        }
    }

    // =========================================================
    // LANGUAGE / ACCESSORS
    // =========================================================

    public void updateLanguage() {

        if (chapterName == null) {
            chapterTitle.setText(Lang.t("ui.title").toUpperCase());
        }

        restartButton.setText(Lang.t("ui.restart"));
        xpName.setText(Lang.t("ui.xp").toUpperCase());
        exposureName.setText(Lang.t("ui.exposure").toUpperCase());
        itemsName.setText(Lang.t("ui.items").toUpperCase());
        itemKicker.setText(Lang.t("ui.item"));

        updateGhost();
    }

    public BorderPane getRoot() {
        return root;
    }

    public VBox getChoicesBox() {
        return choicesBox;
    }

    public void show(Stage stage) {

        Scene scene = new Scene(root, 1200, 800);

        installKeys(scene);

        stage.setTitle(Lang.t("ui.title"));
        stage.setScene(scene);
        stage.show();
    }
}