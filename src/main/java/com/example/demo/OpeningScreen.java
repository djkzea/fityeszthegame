package com.example.demo;

import java.io.InputStream;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Splash ("FITYESZ CHRONICLE" fades in and out in the middle) followed by the
 * opening page: layered artwork + menu (New Game / Load Game / Exit) on the left.
 */
public class OpeningScreen {

    // Put the four PNGs in src/main/resources/com/example/demo/fityesz_art/opening/
    private static final String ART_DIR  = "/com/example/demo/fityesz_art/opening/";
    private static final String FONT_DIR = "/com/example/demo/fonts/";

    // The artwork layers are all 1200 x 630
    private static final double ART_W = 1200;
    private static final double ART_H = 630;

    private static final String PAGE = "#F4EFE6";
    private static final String INK  = "#161616";
    private static final String RED  = "#D20A2E";
    private static final String GRAY = "#9A9488";

    private final StackPane root = new StackPane();
    private final Pane art = new Pane();
    private final StackPane splash = new StackPane();

    private final Runnable onNewGame;
    private final Runnable onExit;

    private boolean splashDone = false;
    private boolean starting = false;

    private final String headFont;

    public OpeningScreen(Runnable onNewGame, Runnable onExit) {

        this.onNewGame = onNewGame;
        this.onExit = onExit;

        loadFont("BebasNeue-Regular.ttf");
        headFont = Font.getFamilies().contains("Bebas Neue") ? "Bebas Neue" : "Impact";

        root.setStyle("-fx-background-color: " + PAGE + ";");

        buildArt();
        buildSplash();

        root.getChildren().addAll(art, splash);

        // keep the 1200x630 artwork fitted (and centered) in any window size
        DoubleBinding scale = Bindings.createDoubleBinding(
                () -> Math.min(root.getWidth() / ART_W, root.getHeight() / ART_H),
                root.widthProperty(), root.heightProperty());

        art.scaleXProperty().bind(scale);
        art.scaleYProperty().bind(scale);

        // the opening page starts hidden, the splash plays first
        art.setOpacity(0);
        art.setMouseTransparent(true);

        // click to skip the splash
        splash.setOnMouseClicked(e -> skipSplash());

        // play once the screen is on a stage
        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                javafx.application.Platform.runLater(this::playSplash);
            }
        });
    }

    public StackPane getRoot() {
        return root;
    }

    // ==========================================
    // OPENING PAGE
    // ==========================================

    private void buildArt() {

        art.setMinSize(ART_W, ART_H);
        art.setPrefSize(ART_W, ART_H);
        art.setMaxSize(ART_W, ART_H);

        ImageView bg      = layer("opening_bg.png");
        ImageView flag    = layer("opening_flag.png");
        ImageView people  = layer("opening_people.png");
        ImageView fityesz = layer("opening_fityesz.png");

        VBox menu = new VBox(6);
        menu.setLayoutX(72);
        menu.setLayoutY(400);
        menu.setPrefWidth(250);

        Button newGame  = menuButton("NEW GAME", true,  this::startNewGame);
        Button loadGame = menuButton("LOAD GAME", false, null);
        Button exit     = menuButton("EXIT", true, onExit);

        Label soon = new Label("COMING SOON");
        soon.setFont(Font.font(headFont, 13));
        soon.setStyle("-fx-text-fill: " + GRAY + ";");
        soon.setPadding(new Insets(0, 0, 0, 22));
        soon.setTranslateY(-6);

        menu.getChildren().addAll(newGame, loadGame, soon, exit);

        art.getChildren().addAll(bg, flag, people, fityesz, menu);

        // remembered so the intro animation can stagger them
        art.getProperties().put("people", people);
        art.getProperties().put("menu", menu);
    }

    private Button menuButton(String text, boolean enabled, Runnable action) {

        Button b = new Button(text);

        b.setPrefWidth(250);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font(headFont, FontWeight.NORMAL, 34));
        b.setFocusTraversable(enabled);

        String base = "-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-padding: 4 12 4 18;"
                + "-fx-border-width: 0 0 0 4;"
                + "-fx-cursor: " + (enabled ? "hand" : "default") + ";";

        String normal = base + "-fx-border-color: transparent;"
                + "-fx-text-fill: " + (enabled ? INK : GRAY) + ";";

        String hover = base + "-fx-border-color: " + RED + ";"
                + "-fx-background-color: rgba(255,255,255,0.65);"
                + "-fx-text-fill: " + RED + ";";

        b.setStyle(normal);

        if (!enabled) {
            b.setDisable(false);        // keep full gray look instead of JavaFX's faded disabled look
            b.setOpacity(0.85);
            b.setOnAction(e -> { });
            return b;
        }

        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(b.isFocused() ? hover : normal));
        b.focusedProperty().addListener((o, was, is) -> b.setStyle(is ? hover : normal));

        b.setOnAction(e -> action.run());

        b.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                b.fire();
                e.consume();
            }
        });

        return b;
    }

    // ==========================================
    // SPLASH
    // ==========================================

    private void buildSplash() {

        splash.setStyle("-fx-background-color: " + PAGE + ";");

        Image img = loadImage("opening_fityesz.png");

        if (img == null) {
            return;
        }

        // the logo sits on the left of the PNG - crop to just the logo, then center it
        ImageView logo = new ImageView(img);
        logo.setViewport(new Rectangle2D(60, 262, 250, 104));
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setFitWidth(500);

        logo.setOpacity(0);

        splash.getChildren().add(logo);
        splash.getProperties().put("logo", logo);
    }

    private void playSplash() {

        Node logo = (Node) splash.getProperties().get("logo");

        FadeTransition in = new FadeTransition(Duration.seconds(1.4), logo);
        in.setFromValue(0);
        in.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(1.3));

        FadeTransition out = new FadeTransition(Duration.seconds(1.4), logo);
        out.setFromValue(1);
        out.setToValue(0);

        SequentialTransition seq = new SequentialTransition(in, hold, out);
        seq.setOnFinished(e -> revealOpeningPage());
        splash.getProperties().put("anim", seq);
        seq.play();
    }

    private void skipSplash() {

        Object anim = splash.getProperties().get("anim");

        if (anim instanceof SequentialTransition seq && !splashDone) {
            seq.stop();
            revealOpeningPage();
        }
    }

    private void revealOpeningPage() {

        if (splashDone) {
            return;
        }
        splashDone = true;

        // splash fades away, opening page fades in
        FadeTransition splashOut = new FadeTransition(Duration.seconds(0.6), splash);
        splashOut.setFromValue(splash.getOpacity());
        splashOut.setToValue(0);
        splashOut.setOnFinished(e -> splash.setVisible(false));

        FadeTransition artIn = new FadeTransition(Duration.seconds(1.2), art);
        artIn.setFromValue(0);
        artIn.setToValue(1);

        // people drift in slightly from the right, menu from the left
        Node people = (Node) art.getProperties().get("people");
        Node menu = (Node) art.getProperties().get("menu");

        people.setTranslateX(40);
        TranslateTransition peopleSlide = new TranslateTransition(Duration.seconds(1.4), people);
        peopleSlide.setToX(0);

        menu.setTranslateX(-30);
        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(1.0), menu);
        menuSlide.setToX(0);

        ParallelTransition all = new ParallelTransition(splashOut, artIn, peopleSlide, menuSlide);
        all.setOnFinished(e -> art.setMouseTransparent(false));
        all.play();
    }

    // ==========================================
    // NEW GAME
    // ==========================================

    private void startNewGame() {

        if (starting) {
            return;
        }
        starting = true;

        FadeTransition fade = new FadeTransition(Duration.seconds(0.6), root);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> onNewGame.run());
        fade.play();
    }

    // ==========================================
    // RESOURCES
    // ==========================================

    private ImageView layer(String file) {

        Image img = loadImage(file);
        ImageView view = new ImageView();

        if (img != null) {
            view.setImage(img);
        }

        view.setFitWidth(ART_W);
        view.setFitHeight(ART_H);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setMouseTransparent(true);

        return view;
    }

    private Image loadImage(String file) {

        try (InputStream in = OpeningScreen.class.getResourceAsStream(ART_DIR + file)) {

            if (in != null) {
                return new Image(in);
            }
        } catch (Exception ignored) {
        }

        System.err.println("OpeningScreen: missing image " + ART_DIR + file);
        return null;
    }

    private static void loadFont(String file) {

        try (InputStream in = OpeningScreen.class.getResourceAsStream(FONT_DIR + file)) {

            if (in != null) {
                Font.loadFont(in, 12);
            }
        } catch (Exception ignored) {
        }
    }
}