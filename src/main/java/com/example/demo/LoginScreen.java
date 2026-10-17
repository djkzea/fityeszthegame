package com.example.demo;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Signing in to the website's accounts.
 *
 * Built out of the parts the rest of the game is built from: the title page's
 * tricolour bar across the top, and a sheet with the red header band the
 * chapter summary card wears. Two modes in one screen - sign in with a
 * username or an address, or register with both.
 *
 * Every refusal reads the same, because the Edge Function behind this answers
 * the same to everything that fails: a caller who does not know the password
 * must not learn whether the username exists.
 */
public final class LoginScreen {

    private static final String PAGE  = "#F4EFE6";   // --paper
    private static final String SHEET = "#FBF8F2";   // --sheet
    private static final String INK   = "#161616";   // --ink
    private static final String RED   = "#C8102E";   // --accent
    private static final String RED_D = "#B10E28";   // --accentText
    private static final String GREEN = "#1F6B3A";   // --second
    private static final String GRAY  = "#4A4640";   // --inkSoft
    private static final String RULE  = "#CFC6B6";   // --line

    private final StackPane root = new StackPane();

    private final Account account;
    private final Runnable onDone;

    private final TextField identifier = new TextField();
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();

    private final Label identifierLabel = caption(Lang.t("ui.usernameOrEmail"));
    private final Label heading = new Label();
    private final Label note = new Label(" ");
    private final Button submit = primary("");
    private final Button mode = quiet("");

    private final VBox emailRow;

    private boolean registering = false;
    private boolean working = false;

    public LoginScreen(Account account, Runnable onDone, Runnable onBack) {

        this.account = account;
        this.onDone = onDone;

        root.setStyle("-fx-background-color: " + PAGE + ";");
        root.setAlignment(Pos.CENTER);

        // ----- the sheet's red band, as the summary card wears it -----

        Label kicker = new Label(spaced(Lang.t("ui.account").toUpperCase()));
        kicker.setFont(Font.font(label(), 11));
        kicker.setTextFill(Color.web(SHEET));
        kicker.setPadding(new Insets(7, 16, 7, 16));

        StackPane band = new StackPane(kicker);
        band.setAlignment(Pos.CENTER_LEFT);
        band.setStyle("-fx-background-color: " + RED + ";");

        // ----- the heading and its rule -----

        heading.setFont(Font.font(head(), 40));
        heading.setTextFill(Color.web(INK));

        Region underline = new Region();
        underline.setMinSize(54, 3);
        underline.setPrefSize(54, 3);
        underline.setMaxSize(54, 3);
        underline.setStyle("-fx-background-color: " + RED + ";");

        Label why = new Label(Lang.t("ui.accountWhy"));
        why.setFont(Font.font(label(), 12));
        why.setTextFill(Color.web(GRAY));
        why.setWrapText(true);
        why.setMaxWidth(360);

        note.setFont(Font.font(label(), 12));
        note.setTextFill(Color.web(RED));
        note.setWrapText(true);
        note.setMaxWidth(360);
        note.setMinHeight(18);

        emailRow = field(caption(Lang.t("ui.email")), email);

        Label hint = new Label(spaced(Lang.t("ui.escBack")));
        hint.setFont(Font.font(label(), 9));
        hint.setTextFill(Color.web(GRAY));

        submit.setOnAction(e -> go());
        mode.setOnAction(e -> toggle());

        VBox body = new VBox(0,
                heading, underline, why,
                emailRow,
                field(identifierLabel, identifier),
                field(caption(Lang.t("ui.password")), password),
                note, submit, mode, hint);

        VBox.setMargin(underline, new Insets(10, 0, 14, 0));
        VBox.setMargin(why, new Insets(0, 0, 22, 0));
        VBox.setMargin(note, new Insets(6, 0, 10, 0));
        VBox.setMargin(mode, new Insets(10, 0, 0, 0));
        VBox.setMargin(hint, new Insets(18, 0, 0, 2));

        body.setPadding(new Insets(26, 30, 26, 30));

        VBox sheet = new VBox(band, body);
        sheet.setMaxSize(430, Region.USE_PREF_SIZE);
        sheet.setStyle("-fx-background-color: " + SHEET + ";"
                + "-fx-border-color: " + INK + ";"
                + "-fx-border-width: 2;");

        // ----- the tricolour, straight off the title page -----

        HBox bar = new HBox(segment(RED), segment("#FFFFFF"), segment(GREEN));
        bar.setMinHeight(10);
        bar.setPrefHeight(10);
        bar.setMaxHeight(10);
        bar.setMouseTransparent(true);
        StackPane.setAlignment(bar, Pos.TOP_CENTER);

        Label wordmark = new Label(spaced(Lang.t("ui.title").toUpperCase()));
        wordmark.setFont(Font.font(label(), 10));
        wordmark.setTextFill(Color.web(GRAY));

        VBox column = new VBox(14, wordmark, sheet);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMaxSize(430, Region.USE_PREF_SIZE);

        root.getChildren().addAll(column, bar);

        showMode();

        identifier.setOnAction(e -> go());
        password.setOnAction(e -> go());
        email.setOnAction(e -> go());

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onBack.run();
                e.consume();
            }
        });

        // the sheet rises in, the way the summary card does
        column.setOpacity(0);
        column.setTranslateY(12);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(320), column);
        fade.setToValue(1);

        TranslateTransition rise = new TranslateTransition(javafx.util.Duration.millis(320), column);
        rise.setToY(0);

        new ParallelTransition(fade, rise).play();

        Platform.runLater(identifier::requestFocus);
    }

    public StackPane getRoot() {
        return root;
    }

    /** Registration needs an address as well; signing in does not. */
    private void toggle() {
        registering = !registering;
        note.setText(" ");
        showMode();
    }

    private void showMode() {

        emailRow.setVisible(registering);
        emailRow.setManaged(registering);

        heading.setText((registering ? Lang.t("ui.signUp") : Lang.t("ui.logIn")).toUpperCase());

        identifierLabel.setText(
                spaced((registering ? Lang.t("ui.username") : Lang.t("ui.usernameOrEmail")).toUpperCase()));

        submit.setText((registering ? Lang.t("ui.signUp") : Lang.t("ui.logIn")).toUpperCase());
        mode.setText(registering ? Lang.t("ui.haveAccount") : Lang.t("ui.noAccount"));
    }

    private void go() {

        if (working) {
            return;
        }

        if (account == null) {
            fail(Lang.t("ui.authUnavailable"));
            return;
        }

        String id = identifier.getText() == null ? "" : identifier.getText().trim();
        String pass = password.getText() == null ? "" : password.getText();
        String mail = email.getText() == null ? "" : email.getText().trim();

        if (id.isEmpty() || pass.isEmpty() || (registering && mail.isEmpty())) {
            fail(Lang.t("ui.fillFields"));
            return;
        }

        working = true;
        submit.setDisable(true);
        note.setTextFill(Color.web(GRAY));
        note.setText("…");

        boolean signUp = registering;

        // Off the JavaFX thread: a slow server must not freeze the screen.
        Thread worker = new Thread(() -> {

            String message = null;
            boolean in = false;

            try {
                if (signUp) {
                    in = account.signUp(mail, id, pass).isPresent();
                    message = in ? null : Lang.t("ui.checkEmail");
                } else {
                    account.signIn(id, pass);
                    in = true;
                }

            } catch (Account.AuthException e) {
                message = Lang.t(e.messageKey());
            }

            final String shown = message;
            final boolean done = in;

            Platform.runLater(() -> {
                working = false;
                submit.setDisable(false);
                note.setTextFill(Color.web(done ? GREEN : RED));
                note.setText(shown == null ? " " : shown);

                if (done) {
                    onDone.run();
                }
            });
        }, "fityesz-auth");

        worker.setDaemon(true);
        worker.start();
    }

    private void fail(String message) {
        note.setTextFill(Color.web(RED));
        note.setText(message);
    }

    // =========================================================
    // PARTS
    // =========================================================

    /** A caption over a line: small caps, letter-spaced, the way the game labels things. */
    private Label caption(String text) {

        Label l = new Label(spaced(text.toUpperCase()));
        l.setFont(Font.font(label(), 9));
        l.setTextFill(Color.web(GRAY));
        return l;
    }

    /** A field is a caption and a rule the text sits on; the rule turns red on focus. */
    private VBox field(Label caption, TextField input) {

        input.setFont(Font.font(label(), 15));
        input.setPrefHeight(34);

        String line = "-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-text-fill: " + INK + ";"
                + "-fx-padding: 0;"
                + "-fx-border-width: 0 0 1 0;"
                + "-fx-border-color: transparent transparent ";

        input.setStyle(line + RULE + " transparent;");
        input.focusedProperty().addListener((o, was, has) ->
                input.setStyle(line + (has ? RED : RULE) + " transparent;"));

        VBox box = new VBox(3, caption, input);
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    /** The one button that carries the screen: filled, the accent's own red. */
    private Button primary(String text) {

        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(46);
        b.setFont(Font.font(head(), 22));
        b.setFocusTraversable(false);

        String base = "-fx-background-radius: 0;"
                + "-fx-text-fill: " + SHEET + ";"
                + "-fx-cursor: hand;"
                + "-fx-background-color: ";

        b.setStyle(base + RED + ";");
        b.setOnMouseEntered(e -> b.setStyle(base + RED_D + ";"));
        b.setOnMouseExited(e -> b.setStyle(base + RED + ";"));

        return b;
    }

    /** The other way in: a line of text, not a button pretending to be one. */
    private Button quiet(String text) {

        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER);
        b.setFont(Font.font(label(), 12));
        b.setFocusTraversable(false);

        String base = "-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-padding: 6 0 6 0;"
                + "-fx-cursor: hand;"
                + "-fx-underline: true;"
                + "-fx-text-fill: ";

        b.setStyle(base + GRAY + ";");
        b.setOnMouseEntered(e -> b.setStyle(base + RED + ";"));
        b.setOnMouseExited(e -> b.setStyle(base + GRAY + ";"));

        return b;
    }

    private static Region segment(String colour) {

        Region r = new Region();
        r.setStyle("-fx-background-color: " + colour + ";");
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Letter-spacing, as the title page does it - JavaFX has no tracking of its own. */
    private static String spaced(String text) {

        StringBuilder out = new StringBuilder(text.length() * 2);

        for (int i = 0; i < text.length(); i++) {
            out.append(text.charAt(i));
            if (i < text.length() - 1) {
                out.append(' ');
            }
        }

        return out.toString();
    }

    private static String head() {
        return Font.getFamilies().contains("Antonio") ? "Antonio" : "Arial Narrow";
    }

    private static String label() {
        return Font.getFamilies().contains("Public Sans") ? "Public Sans" : "Arial";
    }
}
