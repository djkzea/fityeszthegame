package com.example.demo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * Splash (the wordmark fades in and out) followed by the opening page:
 * tricolour rule, wordmark and menu on the left, cast on the right.
 *
 * Everything is laid out in a fixed 1200 x 630 design space and scaled as a
 * whole to whatever the window is, so the composition never drifts apart.
 */
public class OpeningScreen {

    private static final String ART_DIR  = "/com/example/demo/fityesz_art/opening/";
    private static final String FONT_DIR = "/com/example/demo/fonts/";

    // the design space; every coordinate below is in these units
    private static final double ART_W = 1200;
    private static final double ART_H = 630;

    private static final String PAGE  = "#F4EFE6";
    private static final String INK   = "#161616";
    private static final String RED   = "#C20222";
    private static final String GREEN = "#206B3A";
    private static final String GRAY  = "#9A9488";

    // opening_fityesz.png is a 1920 x 1080 sheet - this is the wordmark inside it
    private static final Rectangle2D LOGO_CROP = new Rectangle2D(276, 323, 1170, 413);

    private static final double BAR_Y = 115;
    private static final double BAR_H = 10;

    private static final double COL_X = 72;     // left column
    private static final double LOGO_Y = 236;
    private static final double LOGO_W = 300;
    private static final double MENU_Y = 400;

    private final StackPane root = new StackPane();
    private final Pane art = new Pane();
    private final StackPane splash = new StackPane();

    private final Runnable onNewGame;
    private final Runnable onExit;

    private boolean splashDone = false;
    private boolean starting = false;

    private final String headFont;
    private final String labelFont;

    /** the menu entries arrow keys move between */
    private final List<Button> nav = new ArrayList<>();
    private int navIndex = 0;

    public OpeningScreen(Runnable onNewGame, Runnable onExit) {

        this.onNewGame = onNewGame;
        this.onExit = onExit;

        loadFont("BebasNeue-Regular.ttf");
        loadFont("Archivo-Bold.ttf");

        headFont  = family("Bebas Neue", "Impact");
        labelFont = family("Archivo", "Arial");

        root.setStyle("-fx-background-color: " + PAGE + ";");

        buildArt();
        buildSplash();

        root.getChildren().addAll(art, splash);

        // keep the whole 1200x630 composition fitted and centred in any window
        DoubleBinding scale = Bindings.createDoubleBinding(
                () -> Math.min(root.getWidth() / ART_W, root.getHeight() / ART_H),
                root.widthProperty(), root.heightProperty());

        art.scaleXProperty().bind(scale);
        art.scaleYProperty().bind(scale);

        // the opening page starts hidden, the splash plays first
        art.setOpacity(0);
        art.setMouseTransparent(true);

        splash.setOnMouseClicked(e -> skipSplash());

        root.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);

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

        // the page itself is flat colour (set on root), so only the cast is a bitmap
        ImageView people = layer("opening_people.png");

        ImageView logo = logoView(LOGO_W);
        logo.setLayoutX(COL_X);
        logo.setLayoutY(LOGO_Y);

        // kept short on purpose: the cast artwork starts at x=358 in the design space
        Label tagline = new Label(spaced(Lang.magyar()
                ? "POLITIKAI KALANDJÁTÉK"
                : "A POLITICAL ADVENTURE"));
        tagline.setFont(Font.font(labelFont, 11));
        tagline.setStyle("-fx-text-fill: " + GRAY + ";");
        tagline.setLayoutX(COL_X + 3);
        tagline.setLayoutY(LOGO_Y + logoHeight(LOGO_W) + 30);

        VBox menu = new VBox(4);
        menu.setLayoutX(COL_X);
        menu.setLayoutY(MENU_Y);
        menu.setPrefWidth(260);

        Button newGame  = menuButton(Lang.t("ui.newGame"),  true,  this::startNewGame);
        Button loadGame = menuButton(Lang.t("ui.loadGame"), false, null);
        Button exit     = menuButton(Lang.t("ui.exit"),     true,  onExit);

        Label soon = new Label(Lang.magyar() ? "HAMAROSAN" : "COMING SOON");
        soon.setFont(Font.font(labelFont, 11));
        soon.setStyle("-fx-text-fill: " + GRAY + ";");
        soon.setPadding(new Insets(0, 0, 6, 22));
        soon.setTranslateY(-4);

        menu.getChildren().addAll(newGame, loadGame, soon, exit);

        nav.add(newGame);
        nav.add(exit);

        art.getChildren().addAll(people, tricolourBar(), logo, tagline, menu);

        // remembered so the intro animation can stagger them
        art.getProperties().put("people", people);
        art.getProperties().put("menu", menu);
    }

    /** The Hungarian tricolour, drawn rather than scaled from a bitmap so it stays crisp and exact. */
    private Node tricolourBar() {

        Group bar = new Group();
        double third = ART_W / 3.0;

        bar.getChildren().addAll(
                barSegment(0, third, RED),
                barSegment(third, third, "#FFFFFF"),
                barSegment(third * 2, third, GREEN));

        bar.setMouseTransparent(true);
        return bar;
    }

    private Rectangle barSegment(double x, double width, String colour) {

        Rectangle r = new Rectangle(width, BAR_H, Color.web(colour));
        r.setX(x);
        r.setY(BAR_Y);
        return r;
    }

    private Button menuButton(String text, boolean enabled, Runnable action) {

        Button b = new Button(text.toUpperCase());

        b.setPrefWidth(260);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font(headFont, 34));
        b.setFocusTraversable(enabled);

        // the 4px rule is always there, just transparent when idle, so nothing shifts on hover
        String base = "-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-padding: 2 12 2 18;"
                + "-fx-border-width: 0 0 0 4;"
                + "-fx-cursor: " + (enabled ? "hand" : "default") + ";";

        String idle   = base + "-fx-border-color: transparent;"
                + "-fx-text-fill: " + (enabled ? INK : GRAY) + ";";

        String active = base + "-fx-border-color: " + RED + ";"
                + "-fx-text-fill: " + RED + ";";

        b.setStyle(idle);

        if (!enabled) {
            b.setOpacity(0.85);
            return b;
        }

        b.setOnMouseEntered(e -> focus(b));
        b.setOnAction(e -> action.run());

        b.focusedProperty().addListener((o, was, is) -> {
            b.setStyle(is ? active : idle);
            slide(b, is ? 6 : 0);
            if (is) {
                navIndex = Math.max(0, nav.indexOf(b));
            }
        });

        return b;
    }

    private void slide(Node node, double to) {

        TranslateTransition t = new TranslateTransition(Duration.millis(140), node);
        t.setToX(to);
        t.play();
    }

    private void focus(Button b) {

        if (!b.isFocused()) {
            b.requestFocus();
        }
    }

    // ==========================================
    // KEYS
    // ==========================================

    private void onKey(KeyEvent e) {

        // any key gets past the splash
        if (!splashDone) {
            skipSplash();
            e.consume();
            return;
        }

        switch (e.getCode()) {
            case UP, LEFT -> {
                move(-1);
                e.consume();
            }
            case DOWN, RIGHT -> {
                move(1);
                e.consume();
            }
            case ENTER, SPACE -> {
                if (!nav.isEmpty()) {
                    nav.get(navIndex).fire();
                }
                e.consume();
            }
            default -> { }
        }
    }

    private void move(int step) {

        if (nav.isEmpty()) {
            return;
        }

        navIndex = Math.floorMod(navIndex + step, nav.size());
        nav.get(navIndex).requestFocus();
    }

    // ==========================================
    // SPLASH
    // ==========================================

    private void buildSplash() {

        splash.setStyle("-fx-background-color: " + PAGE + ";");

        ImageView logo = logoView(460);

        if (logo.getImage() == null) {
            return;
        }

        logo.setOpacity(0);

        splash.getChildren().add(logo);
        splash.setAlignment(Pos.CENTER);
        splash.getProperties().put("logo", logo);
    }

    private void playSplash() {

        Node logo = (Node) splash.getProperties().get("logo");

        if (logo == null) {
            revealOpeningPage();
            return;
        }

        FadeTransition in = new FadeTransition(Duration.seconds(1.2), logo);
        in.setFromValue(0);
        in.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(1.1));

        FadeTransition out = new FadeTransition(Duration.seconds(1.0), logo);
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

        FadeTransition splashOut = new FadeTransition(Duration.seconds(0.6), splash);
        splashOut.setFromValue(splash.getOpacity());
        splashOut.setToValue(0);
        splashOut.setOnFinished(e -> splash.setVisible(false));

        FadeTransition artIn = new FadeTransition(Duration.seconds(1.2), art);
        artIn.setFromValue(0);
        artIn.setToValue(1);

        // the cast drifts in from the right, the menu from the left
        Node people = (Node) art.getProperties().get("people");
        Node menu = (Node) art.getProperties().get("menu");

        people.setTranslateX(40);
        TranslateTransition peopleSlide = new TranslateTransition(Duration.seconds(1.4), people);
        peopleSlide.setToX(0);

        menu.setTranslateX(-30);
        TranslateTransition menuSlide = new TranslateTransition(Duration.seconds(1.0), menu);
        menuSlide.setToX(0);

        ParallelTransition all = new ParallelTransition(splashOut, artIn, peopleSlide, menuSlide);

        all.setOnFinished(e -> {
            art.setMouseTransparent(false);
            if (!nav.isEmpty()) {
                nav.get(0).requestFocus();
            }
        });

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

    /** The wordmark, cropped out of its sheet and scaled to the requested width. */
    private ImageView logoView(double fitWidth) {

        ImageView view = new ImageView();
        Image img = loadImage("opening_fityesz.png");

        if (img != null) {
            view.setImage(img);
            view.setViewport(LOGO_CROP);
        }

        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setFitWidth(fitWidth);
        view.setMouseTransparent(true);

        return view;
    }

    private static double logoHeight(double fitWidth) {
        return fitWidth * LOGO_CROP.getHeight() / LOGO_CROP.getWidth();
    }

    /** JavaFX has no letter-spacing, so hair spaces stand in for it on the small caps label. */
    private static String spaced(String text) {

        StringBuilder out = new StringBuilder(text.length() * 2);

        for (int i = 0; i < text.length(); i++) {
            out.append(text.charAt(i));
            if (i < text.length() - 1) {
                out.append(' ');
            }
        }

        return out.toString();
    }

    /**
     * A full-width layer. Only the width is pinned - the height follows the image's
     * own ratio, so an asset that isn't exactly 1200x630 lands square on the left
     * edge instead of being quietly shrunk away from it.
     */
    private ImageView layer(String file) {

        Image img = loadImage(file);
        ImageView view = new ImageView();

        if (img != null) {
            view.setImage(img);
        }

        view.setPreserveRatio(true);
        view.setFitWidth(ART_W);
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

    private static String family(String wanted, String fallback) {
        return Font.getFamilies().contains(wanted) ? wanted : fallback;
    }
}
