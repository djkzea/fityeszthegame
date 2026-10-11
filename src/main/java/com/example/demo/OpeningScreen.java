package com.example.demo;

import java.io.InputStream;
import java.net.URL;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * Title screen: a splash with the wordmark, then a full-bleed poster - engraved
 * plate behind, cast anchored into the bottom right corner, wordmark and menu
 * down the left.
 *
 * The background covers the window at any size, the cast is sized as a fraction
 * of it, and the left column is drawn at fixed design sizes and scaled as one
 * block, so the type keeps its proportions instead of each piece drifting.
 */
public class OpeningScreen {

    private static final String ART_DIR  = "/com/example/demo/fityesz_art/opening/";
    private static final String FONT_DIR = "/com/example/demo/fonts/";

    /** the window height the left column is drawn for; everything scales off this */
    private static final double DESIGN_H = 800;

    private static final double COL_PAD = 72;    // page margin, in design units
    private static final double COL_W = 352;     // column content width

    private static final String PAGE  = "#F4EFE6";
    private static final String INK   = "#161616";
    private static final String RED   = "#C20222";
    private static final String GREEN = "#206B3A";
    private static final String GRAY  = "#9A9488";
    private static final String RULE  = "#DFD7C8";

    // the wordmark inside the 1920x1080 sheet, and the cast inside its 1200x630 sheet
    private static final Rectangle2D LOGO_CROP = new Rectangle2D(276, 323, 1170, 413);
    private static final Rectangle2D CAST_CROP = new Rectangle2D(358, 180, 789, 335);

    private final StackPane root = new StackPane();
    private final StackPane splash = new StackPane();

    private final Runnable onNewGame;
    private final Runnable onExit;

    private boolean splashDone = false;
    private boolean starting = false;

    private final String headFont;
    private final String labelFont;

    private final List<Button> nav = new ArrayList<>();
    private int navIndex = 0;
    private Button selected;

    private Node plate;
    private Node cast;
    private Node column;
    private Node bar;

    /** window height against the design height, so the column scales with the window */
    private final DoubleBinding scale = Bindings.createDoubleBinding(
            () -> clamp(root.getHeight() / DESIGN_H, 0.75, 1.6),
            root.heightProperty());

    public OpeningScreen(Runnable onNewGame, Runnable onExit) {

        this.onNewGame = onNewGame;
        this.onExit = onExit;

        loadFont("BebasNeue-Regular.ttf");
        loadFont("Archivo-Bold.ttf");

        headFont  = family("Bebas Neue", "Impact");
        labelFont = family("Archivo", "Arial");

        root.setStyle("-fx-background-color: " + PAGE + ";");

        plate = buildPlate();
        cast = buildCast();
        column = buildColumn();
        bar = buildBar();

        StackPane.setAlignment(cast, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(bar, Pos.TOP_CENTER);
        StackPane.setAlignment(column, Pos.CENTER_LEFT);

        buildSplash();

        root.getChildren().addAll(plate, cast, bar, column, splash);

        // the poster starts hidden, the splash plays over it
        plate.setOpacity(0);
        cast.setOpacity(0);
        bar.setOpacity(0);
        column.setOpacity(0);

        splash.setOnMouseClicked(e -> skipSplash());

        root.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);

        root.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                // so tabbing moves the highlight too
                scene.focusOwnerProperty().addListener((o, lost, got) -> {
                    if (got instanceof Button b && nav.contains(b)) {
                        select(b);
                    }
                });
                javafx.application.Platform.runLater(this::playSplash);
            }
        });
    }

    public StackPane getRoot() {
        return root;
    }

    // ==========================================
    // POSTER
    // ==========================================

    /** The engraved plate, covering the window at any aspect ratio. */
    private Region buildPlate() {

        Region r = new Region();
        URL url = OpeningScreen.class.getResource(ART_DIR + "opening_plate.jpg");

        if (url != null) {
            r.setStyle("-fx-background-image: url('" + url.toExternalForm() + "');"
                    + "-fx-background-size: cover;"
                    + "-fx-background-position: center;"
                    + "-fx-background-repeat: no-repeat;");
        }

        r.setMouseTransparent(true);
        return r;
    }

    /** The cast, cropped to the figures themselves and bled off the bottom right corner. */
    private ImageView buildCast() {

        ImageView v = new ImageView();
        Image img = loadImage("opening_people.png");

        if (img != null) {
            v.setImage(img);
            v.setViewport(CAST_CROP);
        }

        v.setPreserveRatio(true);
        v.setSmooth(true);
        v.setMouseTransparent(true);

        // fits inside this box, so it is limited by whichever of the two runs out first.
        // anchored BOTTOM_RIGHT with no offset, so the figures stand on the bottom edge
        // and reach the right one - translateX is left free for the entrance drift.
        v.fitWidthProperty().bind(root.widthProperty().multiply(0.68));
        v.fitHeightProperty().bind(root.heightProperty().multiply(0.62));

        return v;
    }

    /** The tricolour, full width along the top edge. */
    private Node buildBar() {

        HBox bar = new HBox();

        bar.getChildren().addAll(
                barSegment(RED),
                barSegment("#FFFFFF"),
                barSegment(GREEN));

        bar.prefHeightProperty().bind(scale.multiply(10));
        bar.maxHeightProperty().bind(bar.prefHeightProperty());
        bar.setMouseTransparent(true);

        return bar;
    }

    private Region barSegment(String colour) {

        Region r = new Region();
        r.setStyle("-fx-background-color: " + colour + ";");
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ==========================================
    // LEFT COLUMN
    // ==========================================

    private Node buildColumn() {

        VBox col = new VBox();
        col.setPadding(new Insets(0, 0, 0, COL_PAD));
        col.setPrefWidth(COL_W + COL_PAD);
        col.setMaxWidth(COL_W + COL_PAD);
        col.setFillWidth(true);

        ImageView logo = logoView(290);

        Label tagline = new Label(spaced(Lang.magyar()
                ? "POLITIKAI KALANDJÁTÉK"
                : "A POLITICAL ADVENTURE"));
        tagline.setFont(Font.font(labelFont, 11));
        tagline.setStyle("-fx-text-fill: " + GRAY + ";");

        VBox menu = new VBox();

        Button newGame  = menuButton(Lang.t("ui.newGame"),  true,  this::startNewGame);
        Button loadGame = menuButton(Lang.t("ui.loadGame"), false, null);
        Button exit     = menuButton(Lang.t("ui.exit"),     true,  onExit);

        menu.getChildren().addAll(
                menuRow(newGame, null),
                menuRow(loadGame, Lang.magyar() ? "HAMAROSAN" : "SOON"),
                menuRow(exit, null));

        nav.add(newGame);
        nav.add(exit);

        Label hint = new Label(spaced(Lang.magyar()
                ? "↑↓ VÁLASSZ · ENTER INDÍT · ESC KILÉP"
                : "↑↓ SELECT · ENTER START · ESC QUIT"));
        hint.setFont(Font.font(labelFont, 9));
        hint.setStyle("-fx-text-fill: " + GRAY + ";");

        VBox.setMargin(tagline, new Insets(16, 0, 0, 3));
        VBox.setMargin(menu, new Insets(38, 0, 0, 0));
        VBox.setMargin(hint, new Insets(24, 0, 0, 3));

        col.getChildren().addAll(logo, tagline, menu, hint);

        // Scaled as one block so the type keeps its proportions in any window.
        // The pivot is pinned to the left edge (and the vertical middle) because
        // scaling about the centre would walk the column off the page margin as
        // the window grows.
        Group g = new Group(col);

        Scale s = new Scale();
        s.xProperty().bind(scale);
        s.yProperty().bind(scale);
        s.setPivotX(0);
        s.pivotYProperty().bind(col.heightProperty().divide(2));
        g.getTransforms().add(s);

        return g;
    }

    /** One menu line: the button fills the row, an optional note sits at the far right. */
    private Node menuRow(Button button, String note) {

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(COL_W);
        row.setStyle("-fx-border-color: transparent transparent " + RULE + " transparent;"
                + "-fx-border-width: 0 0 1 0;");

        HBox.setHgrow(button, Priority.ALWAYS);
        row.getChildren().add(button);

        if (note != null) {
            Label n = new Label(note);
            n.setFont(Font.font(labelFont, 9));
            n.setStyle("-fx-text-fill: " + GRAY + ";");
            n.setPadding(new Insets(0, 2, 0, 0));
            row.getChildren().add(n);
        }

        return row;
    }

    private Button menuButton(String text, boolean enabled, Runnable action) {

        Button b = new Button(text.toUpperCase());

        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(52);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font(headFont, 34));
        b.setFocusTraversable(enabled);

        // the rule is always 4px wide, just transparent when idle, so nothing shifts
        String base = "-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-padding: 0 10 0 16;"
                + "-fx-border-width: 0 0 0 4;"
                + "-fx-cursor: " + (enabled ? "hand" : "default") + ";";

        String idle   = base + "-fx-border-color: transparent;"
                + "-fx-text-fill: " + (enabled ? INK : GRAY) + ";";

        String active = base + "-fx-border-color: " + RED + ";"
                + "-fx-text-fill: " + RED + ";";

        b.setStyle(idle);

        if (!enabled) {
            b.setOpacity(0.8);
            return b;
        }

        // kept on the node so select() can swap between them
        b.getProperties().put("idle", idle);
        b.getProperties().put("active", active);

        b.setOnMouseEntered(e -> select(b));
        b.setOnAction(e -> action.run());

        return b;
    }

    /**
     * Marks one entry as the selected one. Deliberately not driven by Node.focused:
     * that goes false whenever the window loses focus, which would drop the
     * highlight every time the player alt-tabs away.
     */
    private void select(Button b) {

        if (b == null || b == selected) {
            return;
        }

        if (selected != null) {
            selected.setStyle((String) selected.getProperties().get("idle"));
            slide(selected, 0);
        }

        selected = b;
        b.setStyle((String) b.getProperties().get("active"));
        slide(b, 6);

        navIndex = Math.max(0, nav.indexOf(b));
        b.requestFocus();
    }

    private void slide(Node node, double to) {

        TranslateTransition t = new TranslateTransition(Duration.millis(140), node);
        t.setToX(to);
        t.play();
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
                if (selected != null) {
                    selected.fire();
                }
                e.consume();
            }
            case ESCAPE -> {
                onExit.run();
                e.consume();
            }
            default -> { }
        }
    }

    private void move(int step) {

        if (nav.isEmpty()) {
            return;
        }

        select(nav.get(Math.floorMod(navIndex + step, nav.size())));
    }

    // ==========================================
    // SPLASH
    // ==========================================

    private void buildSplash() {

        splash.setStyle("-fx-background-color: " + PAGE + ";");
        splash.setAlignment(Pos.CENTER);

        ImageView logo = logoView(460);

        if (logo.getImage() == null) {
            return;
        }

        logo.setOpacity(0);

        splash.getChildren().add(logo);
        splash.getProperties().put("logo", logo);
    }

    private void playSplash() {

        Node logo = (Node) splash.getProperties().get("logo");

        if (logo == null) {
            revealPoster();
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
        seq.setOnFinished(e -> revealPoster());
        splash.getProperties().put("anim", seq);
        seq.play();
    }

    private void skipSplash() {

        Object anim = splash.getProperties().get("anim");

        if (anim instanceof SequentialTransition seq && !splashDone) {
            seq.stop();
            revealPoster();
        }
    }

    private void revealPoster() {

        if (splashDone) {
            return;
        }
        splashDone = true;

        FadeTransition splashOut = new FadeTransition(Duration.seconds(0.6), splash);
        splashOut.setFromValue(splash.getOpacity());
        splashOut.setToValue(0);
        splashOut.setOnFinished(e -> splash.setVisible(false));

        // the plate settles first, then the cast comes in from the right and the column from the left
        ParallelTransition all = new ParallelTransition(
                splashOut,
                fadeIn(plate, 1.4, 0),
                fadeIn(bar, 0.8, 0.1),
                fadeIn(cast, 1.1, 0.15),
                fadeIn(column, 1.0, 0.35),
                drift(cast, 60, 1.3, 0.15),
                drift(column, -40, 1.1, 0.35));

        all.setOnFinished(e -> {
            if (!nav.isEmpty()) {
                select(nav.get(0));
            }
        });

        all.play();
    }

    private FadeTransition fadeIn(Node node, double seconds, double delay) {

        FadeTransition f = new FadeTransition(Duration.seconds(seconds), node);
        f.setDelay(Duration.seconds(delay));
        f.setFromValue(0);
        f.setToValue(1);
        return f;
    }

    /** Slides a node in from a horizontal offset. */
    private TranslateTransition drift(Node node, double fromOffset, double seconds, double delay) {

        TranslateTransition t = new TranslateTransition(Duration.seconds(seconds), node);
        t.setDelay(Duration.seconds(delay));
        t.setFromX(fromOffset);
        t.setToX(0);
        return t;
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

    /** JavaFX has no letter-spacing, so hair spaces stand in for it on the small caps labels. */
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
