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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
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

    // the site's design tokens, read off fityeszthegame.com
    private static final String PAGE     = "#F4EFE6";   // --paper
    private static final String INK      = "#161616";   // --ink
    private static final String RED      = "#C8102E";   // --accent
    private static final String RED_TEXT = "#B10E28";   // --accentText, the logo subtitle
    private static final String GREEN    = "#1F6B3A";   // --second
    private static final String GRAY     = "#4A4640";   // --inkSoft
    private static final String RULE     = "#CFC6B6";   // --line

    // the cast inside its 1200x630 sheet
    private static final Rectangle2D CAST_CROP = new Rectangle2D(358, 180, 789, 335);

    // Lockup geometry, measured off the original artwork and expressed as fractions
    // of the lockup width, so it holds at any size.
    private static final double SUB_X    = 0.4427;             // subtitle left edge
    private static final double SUB_BASE = 0.3504;             // subtitle baseline
    private static final double SUB_W    = 0.4803;             // subtitle width
    private static final double SUB_H    = 0.1162;             // subtitle glyph height
    private static final double SUB_MAX  = 0.5560;             // subtitle may run to the mark's right edge

    private final StackPane root = new StackPane();
    private final StackPane splash = new StackPane();

    private final java.util.function.IntConsumer onNewGame;
    private final Runnable onExit;
    private final Runnable onSignIn;
    private final java.util.function.Consumer<Run> onContinue;
    private final Account account;
    private final SaveStore saves;

    /** What the menu column is showing: the menu itself, or a slot to start in / load from. */
    private enum Mode { MENU, NEW, LOAD }

    private Mode mode = Mode.MENU;

    /** The slot whose overwrite is waiting for a second press. */
    private int confirming = 0;

    private java.util.Map<Integer, SaveData> runs = java.util.Map.of();

    private boolean splashDone = false;
    private boolean starting = false;

    private final String headFont;
    private final String labelFont;

    private final List<Button> nav = new ArrayList<>();
    private int navIndex = 0;
    private Button selected;

    private Node plate;
    private Node cast;

    /** The card of names after the wordmark, and whether it is up on its own. */
    private Node crew;
    private boolean crewOpen = false;
    private Node column;
    private Node bar;

    /** window height against the design height, so the column scales with the window */
    private final DoubleBinding scale = Bindings.createDoubleBinding(
            () -> clamp(root.getHeight() / DESIGN_H, 0.75, 1.6),
            root.heightProperty());

    /** A run and the slot it lives in. */
    public record Run(int slot, SaveData save) { }

    public OpeningScreen(java.util.function.IntConsumer onNewGame, Runnable onExit, Runnable onSignIn,
                         java.util.function.Consumer<Run> onContinue,
                         Account account, SaveStore saves) {

        this.onNewGame = onNewGame;
        this.onExit = onExit;
        this.onSignIn = onSignIn;
        this.onContinue = onContinue;
        this.account = account;
        this.saves = saves;

        loadRuns();

        loadFont("Antonio-Bold.ttf");
        loadFont("PublicSans-Regular.ttf");

        headFont  = family("Antonio", "Arial Narrow");
        labelFont = family("Public Sans", "Arial");

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

    /**
     * Shows the poster straight away, with no splash. Used when coming back out of
     * the game - the wordmark has been seen already and replaying it would make
     * leaving a game feel like relaunching the app.
     */
    public void showPosterNow() {

        splashDone = true;
        Sound.get().play(Sound.Track.MENU);
        splash.setVisible(false);
        splash.setOpacity(0);

        plate.setOpacity(1);
        bar.setOpacity(1);
        cast.setOpacity(1);
        column.setOpacity(1);

        if (!nav.isEmpty()) {
            select(nav.get(0));
        }
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

        Pane logo = lockup(290);

        Label tagline = new Label(spaced(Lang.magyar()
                ? "POLITIKAI KALANDJÁTÉK"
                : "A POLITICAL ADVENTURE"));
        tagline.setFont(Font.font(labelFont, 11));
        tagline.setStyle("-fx-text-fill: " + GRAY + ";");

        VBox menu = new VBox();

        if (mode == Mode.MENU) {
            buildMenu(menu);
        } else {
            buildSlots(menu);
        }

        Label hint = new Label(spaced(Lang.magyar()
                ? "↑↓ VÁLASSZ · ENTER INDÍT · ESC KILÉP"
                : "↑↓ SELECT · ENTER START · ESC QUIT"));
        hint.setFont(Font.font(labelFont, 9));
        hint.setStyle("-fx-text-fill: " + GRAY + ";");

        Node languages = languageToggle();

        VBox.setMargin(tagline, new Insets(16, 0, 0, 3));
        VBox.setMargin(menu, new Insets(38, 0, 0, 0));
        VBox.setMargin(hint, new Insets(24, 0, 0, 3));
        VBox.setMargin(languages, new Insets(14, 0, 0, 3));

        col.getChildren().addAll(logo, tagline, menu, hint, languages);

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
        return menuRow(button, note, false);
    }

    /** The note at the far right; a warning wears the accent instead of grey. */
    private Node menuRow(Button button, String note, boolean warning) {

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
            n.setStyle("-fx-text-fill: " + (warning ? RED : GRAY) + ";");
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

    /** Language picker. Without one the opening page could only ever be Hungarian. */
    private Node languageToggle() {

        HBox box = new HBox(7);
        box.setAlignment(Pos.CENTER_LEFT);

        Label slash = new Label("/");
        slash.setFont(Font.font(labelFont, 9));
        slash.setStyle("-fx-text-fill: " + GRAY + ";");

        box.getChildren().addAll(langLabel("MAGYAR", Lang.HU), slash, langLabel("ENGLISH", Lang.EN));

        return box;
    }

    private Label langLabel(String text, String code) {

        boolean active = Lang.kod().equals(code);

        Label l = new Label(spaced(text));
        l.setFont(Font.font(labelFont, 9));
        l.setStyle("-fx-text-fill: " + (active ? RED : GRAY) + ";"
                + "-fx-cursor: " + (active ? "default" : "hand") + ";");

        if (!active) {
            l.setOnMouseClicked(e -> switchLanguage(code));
        }

        return l;
    }

    /** Swaps the language and rebuilds the column, so the wordmark and the menu follow it. */
    private void switchLanguage(String code) {

        if (Lang.kod().equals(code)) {
            return;
        }

        Lang.set(code);

        rebuild();
    }

    /** Builds the column again - after a language change, or a sign-in that changed the menu. */
    private void rebuild() {

        nav.clear();
        selected = null;

        Node old = column;
        int at = root.getChildren().indexOf(old);

        column = buildColumn();
        column.setOpacity(old.getOpacity());
        StackPane.setAlignment(column, Pos.CENTER_LEFT);

        root.getChildren().set(at, column);

        if (!nav.isEmpty()) {
            select(nav.get(0));
        }
    }

    /** The title menu itself. */
    private void buildMenu(VBox menu) {

        // A guest session has no name on it, and offering to sign out of one
        // would be offering to leave an account the player never joined.
        String who = account == null
                ? null
                : account.session().filter(Account.Session::named)
                        .map(Account.Session::displayName).orElse(null);

        Button signIn = menuButton(
                who == null ? Lang.t("ui.logIn") : who,
                true,
                who == null
                        ? onSignIn
                        : () -> {
                            account.signOut();
                            runs = java.util.Map.of();
                            loadRuns();
                            rebuild();
                        });

        Button newGame = menuButton(Lang.t("ui.newGame"), true, () -> {
            // with a run already saved, where the new one goes is the player's call
            if (runs.isEmpty()) {
                startNewGame(1);
            } else {
                mode = Mode.NEW;
                confirming = 0;
                rebuild();
            }
        });

        Button loadGame = menuButton(Lang.t("ui.loadGame"), !runs.isEmpty(),
                runs.isEmpty() ? null : () -> {
                    mode = Mode.LOAD;
                    confirming = 0;
                    rebuild();
                });

        Button crewButton = menuButton(Lang.t("ui.crew"), true, this::showCrew);

        Button exit = menuButton(Lang.t("ui.exit"), true, onExit);

        menu.getChildren().addAll(
                menuRow(signIn, who == null ? null : Lang.t("ui.signOut").toUpperCase()),
                menuRow(newGame, null),
                menuRow(loadGame, runs.isEmpty()
                        ? Lang.t("ui.noSave").toUpperCase()
                        : (runs.size() + " " + Lang.t("ui.saves")).toUpperCase()),
                menuRow(crewButton, "fityeszthegame.com".toUpperCase()),
                menuRow(exit, null));

        nav.add(signIn);
        nav.add(newGame);

        if (!runs.isEmpty()) {
            nav.add(loadGame);
        }

        nav.add(crewButton);
        nav.add(exit);
    }

    /**
     * The three slots.
     *
     * Starting a new run never writes over another one by accident: an occupied
     * slot asks first, and the second press is the answer. Keeping runs apart is
     * also what makes two of them comparable afterwards.
     */
    private void buildSlots(VBox menu) {

        Label heading = new Label(spaced(
                (mode == Mode.NEW ? Lang.t("ui.chooseSlot") : Lang.t("ui.whichRun")).toUpperCase()));
        heading.setFont(Font.font(labelFont, 10));
        heading.setStyle("-fx-text-fill: " + RED + ";");
        VBox.setMargin(heading, new Insets(0, 0, 12, 3));

        menu.getChildren().add(heading);

        for (int i = 1; i <= SaveStore.SLOTS; i++) {

            final int slot = i;
            SaveData run = runs.get(slot);

            boolean usable = mode == Mode.NEW || run != null;

            String name = run == null
                    ? Lang.t("ui.slotEmpty")
                    : (run.playerName() == null || run.playerName().isBlank()
                            ? Lang.t("ui.guest") : run.playerName());

            String note = confirming == slot
                    ? Lang.t("ui.overwriteAsk").toUpperCase()
                    : (run == null ? null : runNote(run));

            Button row = menuButton(slot + " · " + name, usable, usable ? () -> choose(slot) : null);

            menu.getChildren().add(menuRow(row, note, confirming == slot));

            if (usable) {
                nav.add(row);
            }
        }

        Button back = menuButton(Lang.t("ui.back"), true, () -> {
            mode = Mode.MENU;
            confirming = 0;
            rebuild();
        });

        menu.getChildren().add(menuRow(back, null));
        nav.add(back);
    }

    /** A slot was picked: load it, start in it, or ask before writing over it. */
    private void choose(int slot) {

        SaveData run = runs.get(slot);

        if (mode == Mode.LOAD) {
            onContinue.accept(new Run(slot, run));
            return;
        }

        if (run != null && confirming != slot) {
            confirming = slot;      // the second press is the answer
            rebuild();
            return;
        }

        startNewGame(slot);
    }

    /** "2. FEJEZET · 10.17 21:13" */
    private String runNote(SaveData run) {

        String chapter = run.chapter() == 0
                ? Lang.t("ui.prologue")
                : (Lang.magyar()
                        ? run.chapter() + ". " + Lang.t("ui.chapter")
                        : Lang.t("ui.chapter") + " " + run.chapter());

        String when = "";

        try {
            when = " · " + java.time.LocalDateTime.ofInstant(
                            java.time.Instant.parse(run.updatedAt()), java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("MM.dd HH:mm"));
        } catch (RuntimeException ignored) {
            // a save with no readable time still shows its chapter
        }

        return (chapter + when).toUpperCase();
    }

    /**
     * The runs, this machine's first and the account's folded in behind.
     *
     * The local files are read on the spot; the account costs a request, so it
     * happens off the JavaFX thread and the column is rebuilt when it lands.
     */
    private void loadRuns() {

        if (saves == null) {
            return;
        }

        runs = saves.runs(null);

        Account.Session signedIn = account == null ? null : account.session().orElse(null);

        if (signedIn == null) {
            return;
        }

        Thread worker = new Thread(() -> {

            // Both directions, every time the title page is built: a run played
            // offline, or before signing in, reaches the account here rather
            // than waiting for its next chapter to end.
            saves.reconcile(signedIn.userId(), signedIn.accessToken());

            java.util.Map<Integer, SaveData> merged = saves.runs(signedIn.accessToken());

            javafx.application.Platform.runLater(() -> {
                runs = merged;

                if (mode == Mode.MENU) {
                    rebuild();
                }
            });
        }, "fityesz-runs");

        worker.setDaemon(true);
        worker.start();
    }

    // ==========================================
    // KEYS
    // ==========================================

    private void onKey(KeyEvent e) {

        // any key gets past the splash, and past the card if it was called up
        if (!splashDone || crewOpen) {
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

        Pane logo = lockup(460);
        logo.setOpacity(0);

        crew = buildCrew();
        crew.setOpacity(0);

        splash.getChildren().addAll(logo, crew);
        splash.getProperties().put("logo", logo);
    }

    /**
     * Who made it, the way a film says so: after the wordmark, before the
     * poster, held long enough to read and gone on any key.
     *
     * It is a card of four bands - frames from the game, the two who made it,
     * the cast they wrote, and the address of the site. It is also the one
     * place in the game that asks for anything, and it asks once.
     */
    private Node buildCrew() {

        VBox box = new VBox(0, filmstrip(), makers(), cast(), support());
        box.setAlignment(Pos.CENTER);
        box.setMouseTransparent(true);

        // taller than the wordmark it replaces, so it rides the window like the menu
        DoubleBinding fit = Bindings.createDoubleBinding(
                () -> clamp(root.getHeight() / 940.0, 0.52, 1.08), root.heightProperty());

        box.scaleXProperty().bind(fit);
        box.scaleYProperty().bind(fit);

        return box;
    }

    /** Six frames out of the seven chapters, graded down so the names stay on top. */
    private Node filmstrip() {

        String[] frames = {
                "bg/ch1_cafe.png", "bg/ch2_hall.png", "bg/ch4_office.png",
                "bg/ch5_corridor.png", "bg/ch6_lodge.png", "bg/ch7-2_chamber.png"
        };

        HBox strip = new HBox(7);
        strip.setAlignment(Pos.CENTER);

        for (String file : frames) {

            Image img = art(file, 460);

            if (img == null) {
                continue;
            }

            double w = 152;
            double h = 86;

            ImageView view = new ImageView(img);
            view.setPreserveRatio(false);
            view.setSmooth(true);
            view.setFitWidth(w);
            view.setFitHeight(h);
            view.setViewport(centreCrop(img, w / h, 0.42));

            ColorAdjust grade = new ColorAdjust();
            grade.setSaturation(-0.32);
            grade.setBrightness(-0.04);
            view.setEffect(grade);

            StackPane frame = new StackPane(view);
            frame.setMaxSize(w, h);
            frame.setStyle("-fx-border-color: " + INK + "; -fx-border-width: 1;");

            strip.getChildren().add(frame);
        }

        Label caption = new Label(spaced(Lang.t("crew.frames").toUpperCase()));
        caption.setFont(Font.font(labelFont, 8.5));
        caption.setStyle("-fx-text-fill: " + GRAY + ";");

        VBox band = new VBox(0, strip, caption);
        band.setAlignment(Pos.CENTER);
        VBox.setMargin(caption, new Insets(9, 0, 0, 0));
        VBox.setMargin(band, new Insets(0, 0, 30, 0));

        return band;
    }

    /** The two of them, with a hairline between, under the red mark. */
    private Node makers() {

        Region rule = new Region();
        rule.setPrefSize(54, 3);
        rule.setMinSize(54, 3);
        rule.setMaxSize(54, 3);
        rule.setStyle("-fx-background-color: " + RED + ";");

        Label made = new Label(spaced(Lang.t("crew.made").toUpperCase()));
        made.setFont(Font.font(labelFont, 11));
        made.setStyle("-fx-text-fill: " + GRAY + ";");

        Region hair = new Region();
        hair.setPrefSize(1, 196);
        hair.setMinSize(1, 196);
        hair.setMaxSize(1, 196);
        hair.setStyle("-fx-background-color: #D6CEC0;");

        HBox pair = new HBox(46,
                person("Frezzard2.png", Lang.t("crew.name1"), Lang.t("crew.role1")),
                hair,
                person("djkzea.png", Lang.t("crew.name2"), Lang.t("crew.role2")));
        pair.setAlignment(Pos.BOTTOM_CENTER);

        VBox band = new VBox(0, rule, made, pair);
        band.setAlignment(Pos.CENTER);
        VBox.setMargin(rule, new Insets(0, 0, 16, 0));
        VBox.setMargin(pair, new Insets(22, 0, 0, 0));

        return band;
    }

    /** One maker: the portrait, the name, what they did. */
    private Node person(String file, String name, String role) {

        VBox box = new VBox(0);
        box.setAlignment(Pos.CENTER);

        Image img = art("crew/" + file, 320);

        if (img != null) {
            ImageView view = new ImageView(img);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.setFitHeight(204);
            box.getChildren().add(view);
        }

        Label who = new Label(name.toUpperCase());
        who.setFont(Font.font(headFont, 26));
        who.setStyle("-fx-text-fill: " + INK + ";");
        VBox.setMargin(who, new Insets(14, 0, 0, 0));

        Label what = new Label(spaced(role.toUpperCase()));
        what.setFont(Font.font(labelFont, 9));
        what.setStyle("-fx-text-fill: " + RED_TEXT + ";");
        VBox.setMargin(what, new Insets(5, 0, 0, 0));

        box.getChildren().addAll(who, what);
        return box;
    }

    /** The people the game is about, as a film lists its players. */
    private Node cast() {

        String[][] players = {
                {"lipoti.png", "npc.lipoti"},
                {"lakatoservin.png", "npc.lakatos"},
                {"drpeterikatalin.png", "npc.peteri"},
                {"kapzsimre.png", "npc.kapzs"},
                {"molnar.png", "npc.molnar"},
        };

        HBox row = new HBox(30);
        row.setAlignment(Pos.TOP_CENTER);

        for (String[] player : players) {

            Node head = medallion("characters/" + player[0], 76);

            if (head == null) {
                continue;
            }

            Label name = new Label(spaced(Lang.t(player[1]).toUpperCase()));
            name.setFont(Font.font(labelFont, 8));
            name.setStyle("-fx-text-fill: " + GRAY + ";");

            VBox one = new VBox(0, head, name);
            one.setAlignment(Pos.TOP_CENTER);
            VBox.setMargin(name, new Insets(8, 0, 0, 0));

            row.getChildren().add(one);
        }

        Label heading = new Label(spaced(Lang.t("crew.cast").toUpperCase()));
        heading.setFont(Font.font(labelFont, 9));
        heading.setStyle("-fx-text-fill: " + RED_TEXT + ";");

        VBox band = new VBox(0, heading, row);
        band.setAlignment(Pos.CENTER);
        VBox.setMargin(band, new Insets(34, 0, 0, 0));
        VBox.setMargin(row, new Insets(14, 0, 0, 0));

        return band;
    }

    /** The address, which is the only thing the card asks of anyone. */
    private Node support() {

        Region rule = new Region();
        rule.setPrefSize(180, 1);
        rule.setMinSize(180, 1);
        rule.setMaxSize(180, 1);
        rule.setStyle("-fx-background-color: #D6CEC0;");

        Label support = new Label(spaced(Lang.t("crew.support").toUpperCase()));
        support.setFont(Font.font(labelFont, 9));
        support.setStyle("-fx-text-fill: " + GRAY + ";");

        Label site = new Label("fityeszthegame.com");
        site.setFont(Font.font(headFont, 26));
        site.setStyle("-fx-text-fill: " + RED + ";");

        VBox band = new VBox(0, rule, support, site);
        band.setAlignment(Pos.CENTER);
        VBox.setMargin(rule, new Insets(0, 0, 16, 0));
        VBox.setMargin(site, new Insets(4, 0, 0, 0));
        VBox.setMargin(band, new Insets(32, 0, 0, 0));

        return band;
    }

    /** A head in a circle, cropped the way the log crops its portraits. */
    private Node medallion(String file, double size) {

        Image img = art(file, 300);

        if (img == null) {
            return null;
        }

        double side = img.getWidth() * 0.52;
        double x = Math.max(0, img.getWidth() * 0.44 - side / 2);
        double y = Math.max(0, img.getHeight() * 0.15 - side / 2);

        side = Math.min(side, Math.min(img.getWidth() - x, img.getHeight() - y));

        ImageView view = new ImageView(img);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setViewport(new Rectangle2D(x, y, side, side));
        view.setClip(new Circle(size / 2, size / 2, size / 2));

        Circle ring = new Circle(size / 2);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web(INK));
        ring.setStrokeWidth(1);

        return new StackPane(view, ring);
    }

    /** The part of an image that fills a frame of this shape. */
    private static Rectangle2D centreCrop(Image img, double aspect, double bias) {

        double cw;
        double ch;

        if (img.getWidth() / img.getHeight() > aspect) {
            ch = img.getHeight();
            cw = ch * aspect;
        } else {
            cw = img.getWidth();
            ch = cw / aspect;
        }

        return new Rectangle2D((img.getWidth() - cw) / 2, (img.getHeight() - ch) * bias, cw, ch);
    }

    /** Any of the game's own art, decoded no larger than it is drawn. */
    private Image art(String file, double width) {

        try (InputStream in = OpeningScreen.class
                .getResourceAsStream("/com/example/demo/fityesz_art/" + file)) {

            return in == null ? null : new Image(in, width, 0, true, true);

        } catch (Exception missing) {
            return null;
        }
    }

    private void playSplash() {

        if (splashDone) {
            return;
        }

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

        // then the names, the way a film puts them after the studio's mark
        FadeTransition crewIn = new FadeTransition(Duration.seconds(0.8), crew);
        crewIn.setFromValue(0);
        crewIn.setToValue(1);

        PauseTransition crewHold = new PauseTransition(Duration.seconds(3.0));

        FadeTransition crewOut = new FadeTransition(Duration.seconds(0.7), crew);
        crewOut.setFromValue(1);
        crewOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(in, hold, out, crewIn, crewHold, crewOut);
        seq.setOnFinished(e -> revealPoster());
        splash.getProperties().put("anim", seq);
        seq.play();
    }

    /** The card again, from the menu - the support line has to stay reachable. */
    private void showCrew() {

        crewOpen = true;

        Object logo = splash.getProperties().get("logo");

        if (logo instanceof Node node) {
            node.setOpacity(0);
        }

        crew.setOpacity(1);
        splash.setOpacity(0);
        splash.setVisible(true);

        FadeTransition up = new FadeTransition(Duration.seconds(0.35), splash);
        up.setFromValue(0);
        up.setToValue(1);
        up.play();
    }

    private void hideCrew() {

        crewOpen = false;

        FadeTransition down = new FadeTransition(Duration.seconds(0.35), splash);
        down.setFromValue(splash.getOpacity());
        down.setToValue(0);
        down.setOnFinished(e -> splash.setVisible(false));
        down.play();
    }

    private void skipSplash() {

        if (crewOpen) {
            hideCrew();
            return;
        }

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

        // the wordmark's sting has had its moment; the menu has its own cue
        Sound.get().play(Sound.Track.MENU);

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

    private void startNewGame(int slot) {

        if (starting) {
            return;
        }
        starting = true;

        // the shell drops its curtain over this page; fading it out here as well
        // would just wash the page to nothing first
        onNewGame.accept(slot);
    }

    // ==========================================
    // RESOURCES
    // ==========================================

    /**
     * The full lockup: the F + FITYESZ bitmap with the subtitle set in type under it.
     *
     * The Hungarian and English logos differ by exactly one word, and the English
     * one only ever existed baked into a 240px composite - too small to scale up.
     * Setting that word in Antonio instead gives both languages crisply at any
     * size from one asset. It is auto-sized to the width the original KRÓNIKA had,
     * so a longer word like CHRONICLE sets smaller rather than overrunning the mark.
     */
    private Pane lockup(double width) {

        Pane p = new Pane();

        double height = width * SUB_BASE + width * 0.012;
        p.setPrefSize(width, height);
        p.setMinSize(width, height);
        p.setMaxSize(width, height);
        p.setMouseTransparent(true);

        ImageView mark = new ImageView();
        Image img = loadImage("opening_wordmark.png");

        if (img != null) {
            mark.setImage(img);
        }

        mark.setPreserveRatio(true);
        mark.setSmooth(true);
        mark.setFitWidth(width);

        p.getChildren().add(mark);
        addSubtitle(p, width);

        return p;
    }

    /**
     * Sets the subtitle to the proportions the original artwork had: the glyphs are
     * sized by its height and the leftover width is spread between the letters as
     * tracking, rather than stretched into the glyphs. A longer word (CHRONICLE)
     * simply sets smaller instead of overrunning the mark.
     */
    private void addSubtitle(Pane p, double width) {

        String word = Lang.magyar() ? "KRÓNIKA" : "CHRONICLE";

        double minW = width * SUB_W;        // the width the original KRÓNIKA had
        double maxW = width * SUB_MAX;      // flush with the right edge of FITYESZ
        double targetH = width * SUB_H;

        // size from the height first
        Text probe = new Text(word);
        probe.setBoundsType(TextBoundsType.VISUAL);
        probe.setFont(Font.font(headFont, 100));

        double inkH = probe.getLayoutBounds().getHeight();
        double size = inkH > 0 ? 100 * targetH / inkH : 40;

        double[] advance = new double[word.length()];
        double natural = measure(word, size, advance);

        // only shrink once a longer word has used up the room out to the right edge
        if (natural > maxW && natural > 0) {
            size *= maxW / natural;
            natural = measure(word, size, advance);
        }

        double allowed = Math.max(minW, Math.min(maxW, natural));
        double track = word.length() > 1 ? (allowed - natural) / (word.length() - 1) : 0;
        Font font = Font.font(headFont, size);

        double x = width * SUB_X;

        for (int i = 0; i < word.length(); i++) {

            Text t = new Text(String.valueOf(word.charAt(i)));
            t.setFont(font);
            t.setFill(Color.web(RED_TEXT));
            t.setX(x);
            t.setY(width * SUB_BASE);

            p.getChildren().add(t);
            x += advance[i] + track;
        }
    }

    /** Fills in each glyph's advance at this size and returns their total. */
    private double measure(String word, double size, double[] advance) {

        Font font = Font.font(headFont, size);
        double total = 0;

        for (int i = 0; i < word.length(); i++) {
            Text t = new Text(String.valueOf(word.charAt(i)));
            t.setFont(font);
            advance[i] = t.getLayoutBounds().getWidth();
            total += advance[i];
        }

        return total;
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
