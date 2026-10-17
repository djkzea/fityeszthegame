package com.example.demo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HelloApplication extends Application {

    private static final String PAGE  = "#F4EFE6";
    private static final String SHEET = "#FBF8F2";
    private static final String INK   = "#161616";
    private static final String RED   = "#C8102E";

    private static final String DISCLAIMER_HU =
            "A játékban szereplő személyek és események kitaláltak. "
            + "Bármilyen hasonlóság valós személyekkel vagy eseményekkel "
            + "csupán a véletlen műve.";

    private static final String DISCLAIMER_EN =
            "The characters and events depicted in this game are fictitious. "
            + "Any similarity to actual persons, living or dead, is purely coincidental.";

    /**
     * Screens live at the bottom, then the pause sheet, then the curtain, then the
     * card - so a transition covers the pause menu too, and the card sits on the ink.
     */
    private final StackPane shell = new StackPane();
    private final StackPane pauseLayer = new StackPane();
    private final Region curtain = new Region();
    private final StackPane card = new StackPane();

    private GameController controller;
    private Scene scene;

    /** set while the disclaimer is on screen; any key or click runs it */
    private Runnable skip;

    private boolean inGame = false;
    private boolean paused = false;
    private boolean keysInstalled = false;

    private final List<Button> pauseNav = new ArrayList<>();

    /** Held so its own label can report the save - the lambda refers to it. */
    private Button pauseSave;
    private int pauseIndex = 0;

    @Override
    public void start(Stage stage) {

        controller = new GameController(stage);

        curtain.setStyle("-fx-background-color: " + INK + ";");
        curtain.setOpacity(0);
        curtain.setMouseTransparent(true);

        card.setAlignment(Pos.CENTER);
        card.setOpacity(0);
        card.setVisible(false);
        card.setMouseTransparent(true);

        pauseLayer.setStyle("-fx-background-color: rgba(22,22,22,0.82);");
        pauseLayer.setAlignment(Pos.CENTER);
        setPauseShown(false);

        shell.getChildren().addAll(title().getRoot(), pauseLayer, curtain, card);

        scene = new Scene(shell, 1200, 800);

        // the title page is cream and the game is dark; without this the gap between
        // them is the scene's default white, which reads as a flash
        scene.setFill(Color.web(PAGE));

        // filters, so these are seen before the page underneath gets them
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (skip != null) {
                skip.run();
                e.consume();
            }
        });

        stage.setTitle("The Fityesz Chronicle");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    /** A fresh title page wired to this shell. */
    private OpeningScreen title() {
        return new OpeningScreen(
                this::enterGame,
                () -> dip(Platform::exit, 420),
                this::showLogin,
                this::enterGame,
                controller.account(),
                controller.saves());
    }

    /** The sign-in screen, and back to a rebuilt title page either way. */
    private void showLogin() {

        Runnable back = () -> dip(() -> shell.getChildren().set(0, title().getRoot()), 420);

        LoginScreen screen = new LoginScreen(controller.account(), back, back);

        dip(() -> shell.getChildren().set(0, screen.getRoot()), 420);
    }

    // ==========================================
    // KEYS
    // ==========================================

    private void onKey(KeyEvent e) {

        if (skip != null) {
            skip.run();
            e.consume();
            return;
        }

        if (!inGame) {
            return;              // the title page handles its own keys
        }

        // an open log is what ESC closes - the pause sheet comes after it
        if (controller.getUI().logVisible()) {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                controller.getUI().hideLog();
                e.consume();
            }
            return;
        }

        if (paused) {
            switch (e.getCode()) {
                case ESCAPE -> resume();
                case UP, LEFT -> movePause(-1);
                case DOWN, RIGHT -> movePause(1);
                case ENTER, SPACE -> pauseNav.get(pauseIndex).fire();
                default -> { }
            }
            e.consume();         // nothing reaches the game while it is paused
            return;
        }

        if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            openPause();
            e.consume();
        }
    }

    // ==========================================
    // PAUSE
    // ==========================================

    private void openPause() {

        if (paused) {
            return;
        }
        paused = true;

        boolean hu = Lang.magyar();

        Label heading = new Label(hu ? "SZÜNET" : "PAUSED");
        heading.setFont(Font.font(headFamily(), 34));
        heading.setTextFill(Color.web(INK));

        pauseNav.clear();
        pauseIndex = 0;

        Button resume = pauseItem(hu ? "Folytatás" : "Resume", this::resume);

        pauseSave = pauseItem(Lang.t("ui.save"), () -> {
            controller.saveNow(true);
            pauseSave.setText(Lang.t("ui.saved").toUpperCase());
        });

        Button log = pauseItem(Lang.t("ui.log"), () -> {
            resume();
            controller.getUI().showLog(controller.chatlog().entries());
        });

        Button menu = pauseItem(hu ? "Főmenü" : "Main menu", this::toTitle);
        Button quit = pauseItem(Lang.t("ui.exit"), () -> dip(Platform::exit, 420));

        VBox items = new VBox(2, resume, pauseSave, log, menu, quit);
        items.setPadding(new Insets(18, 0, 0, 0));

        VBox sheet = new VBox(heading, items);
        sheet.setPadding(new Insets(26, 30, 26, 26));
        sheet.setMaxSize(360, Region.USE_PREF_SIZE);
        sheet.setStyle("-fx-background-color: " + SHEET + ";"
                + "-fx-border-color: " + INK + ";"
                + "-fx-border-width: 1;");

        pauseLayer.getChildren().setAll(sheet);
        setPauseShown(true);

        selectPause(0);

        FadeTransition in = new FadeTransition(Duration.millis(180), pauseLayer);
        in.setFromValue(0);
        in.setToValue(1);
        in.play();
    }

    private Button pauseItem(String text, Runnable action) {

        Button b = new Button(text.toUpperCase());

        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font(headFamily(), 26));
        b.setFocusTraversable(false);
        b.setOnAction(e -> action.run());
        b.setOnMouseEntered(e -> selectPause(pauseNav.indexOf(b)));

        pauseNav.add(b);
        return b;
    }

    private void selectPause(int index) {

        if (pauseNav.isEmpty()) {
            return;
        }

        pauseIndex = Math.floorMod(index, pauseNav.size());

        for (int i = 0; i < pauseNav.size(); i++) {

            boolean on = i == pauseIndex;

            pauseNav.get(i).setStyle("-fx-background-color: transparent;"
                    + "-fx-background-radius: 0;"
                    + "-fx-padding: 4 10 4 14;"
                    + "-fx-border-width: 0 0 0 4;"
                    + "-fx-cursor: hand;"
                    + "-fx-border-color: " + (on ? RED : "transparent") + ";"
                    + "-fx-text-fill: " + (on ? RED : INK) + ";");
        }
    }

    private void movePause(int step) {
        selectPause(pauseIndex + step);
    }

    private void resume() {

        if (!paused) {
            return;
        }

        FadeTransition out = new FadeTransition(Duration.millis(160), pauseLayer);
        out.setFromValue(pauseLayer.getOpacity());
        out.setToValue(0);
        out.setOnFinished(e -> {
            setPauseShown(false);
            paused = false;
        });
        out.play();
    }

    /** Back to the title page, behind the curtain. */
    private void toTitle() {

        paused = false;
        setPauseShown(false);
        pauseLayer.setOpacity(1);

        dip(() -> {
            inGame = false;

            OpeningScreen again = title();
            again.showPosterNow();
            shell.getChildren().set(0, again.getRoot());
        }, 460);
    }

    private void setPauseShown(boolean shown) {
        pauseLayer.setVisible(shown);
        pauseLayer.setManaged(shown);
        pauseLayer.setMouseTransparent(!shown);
    }

    // ==========================================
    // TITLE -> GAME
    // ==========================================

    /**
     * Title page pushes in as the curtain falls, the disclaimer plays on the ink,
     * then the game lifts out of it.
     */
    private void enterGame() {
        enterGame(null);
    }

    /** The curtain, the card, and then either a fresh run or a saved one. */
    private void enterGame(SaveData save) {

        Node title = shell.getChildren().get(0);

        ScaleTransition push = new ScaleTransition(Duration.millis(620), title);
        push.setToX(1.04);
        push.setToY(1.04);
        push.setInterpolator(Interpolator.EASE_IN);
        push.play();

        curtain.setMouseTransparent(false);

        FadeTransition fall = new FadeTransition(Duration.millis(520), curtain);
        fall.setFromValue(0);
        fall.setToValue(1);

        fall.setOnFinished(e -> showDisclaimer(() -> {

            shell.getChildren().set(0, controller.getUI().getRoot());

            // started as the curtain lifts, so the first line is already typing
            if (save == null) {
                controller.begin();
            } else {
                controller.resume(save);
            }

            // once only: the filter stays on the scene for the life of the app
            if (!keysInstalled) {
                controller.getUI().installKeys(scene);
                keysInstalled = true;
            }

            inGame = true;
            curtain.setMouseTransparent(true);
            lift(640, 140);
        }));

        fall.play();
    }

    /** The film-style card, held on the ink before the story starts. */
    private void showDisclaimer(Runnable done) {

        Label text = new Label(Lang.magyar() ? DISCLAIMER_HU : DISCLAIMER_EN);

        text.setWrapText(true);
        text.setMaxWidth(640);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setAlignment(Pos.CENTER);
        text.setLineSpacing(9);
        text.setFont(Font.font(bodyFamily(), 15.5));
        text.setTextFill(Color.web(PAGE));

        card.getChildren().setAll(text);
        card.setOpacity(0);
        card.setVisible(true);

        FadeTransition in = new FadeTransition(Duration.millis(800), card);
        in.setFromValue(0);
        in.setToValue(1);

        FadeTransition out = new FadeTransition(Duration.millis(700), card);
        out.setFromValue(1);
        out.setToValue(0);

        SequentialTransition play = new SequentialTransition(
                in, new PauseTransition(Duration.millis(2600)), out);

        // Both endings go through the same teardown. Stopping a SequentialTransition
        // does not reliably leave an opacity set straight afterwards - the stopped
        // fade writes its last value back on the next pulse - so the card is hidden
        // outright rather than trusted to be transparent.
        Runnable finish = () -> {
            skip = null;
            play.stop();
            card.getChildren().clear();
            card.setOpacity(0);
            card.setVisible(false);
            done.run();
        };

        play.setOnFinished(e -> finish.run());

        // any key or click cuts it short, the way a title card should
        skip = finish;

        play.play();
    }

    // ==========================================
    // CURTAIN
    // ==========================================

    /**
     * Fades to ink, runs the swap behind it, then lifts. The curtain takes the clicks
     * while it is down so the screen underneath cannot be triggered twice.
     */
    private void dip(Runnable swap, double fallMillis) {

        curtain.setMouseTransparent(false);

        FadeTransition fall = new FadeTransition(Duration.millis(fallMillis), curtain);
        fall.setFromValue(curtain.getOpacity());
        fall.setToValue(1);

        fall.setOnFinished(e -> {
            swap.run();
            curtain.setMouseTransparent(true);
            lift(640, 140);
        });

        fall.play();
    }

    private void lift(double millis, double delayMillis) {

        FadeTransition lift = new FadeTransition(Duration.millis(millis), curtain);
        lift.setDelay(Duration.millis(delayMillis));
        lift.setFromValue(1);
        lift.setToValue(0);
        lift.play();
    }

    // ==========================================
    // FONTS
    // ==========================================

    private static String headFamily() {
        load("Antonio-Bold.ttf");
        return Font.getFamilies().contains("Antonio") ? "Antonio" : "Arial Narrow";
    }

    private static String bodyFamily() {
        load("PublicSans-Regular.ttf");
        return Font.getFamilies().contains("Public Sans") ? "Public Sans" : "Segoe UI";
    }

    private static void load(String file) {
        try (InputStream in = HelloApplication.class
                .getResourceAsStream("/com/example/demo/fonts/" + file)) {
            if (in != null) {
                Font.loadFont(in, 12);
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
