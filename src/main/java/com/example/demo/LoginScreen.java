package com.example.demo;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Signing in to the website's accounts.
 *
 * Two modes in one screen: sign in with a username or an address, or register
 * with both. Every refusal reads the same, because the Edge Function behind
 * this answers the same to everything that fails - a caller who does not know
 * the password must not learn whether the username exists.
 */
public final class LoginScreen {

    private static final String PAGE = "#F4EFE6";
    private static final String INK  = "#161616";
    private static final String RED  = "#C8102E";
    private static final String GRAY = "#4A4640";
    private static final String RULE = "#CFC6B6";

    private final StackPane root = new StackPane();

    private final Account account;
    private final Runnable onDone;

    private final TextField identifier = new TextField();
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();

    private final Label identifierLabel = fieldLabel(Lang.t("ui.usernameOrEmail"));
    private final Label note = new Label("");
    private final Button submit = action(Lang.t("ui.logIn"));
    private final Button mode = action(Lang.t("ui.signUp"));

    private final VBox emailRow;

    private boolean registering = false;
    private boolean working = false;

    public LoginScreen(Account account, Runnable onDone, Runnable onBack) {

        this.account = account;
        this.onDone = onDone;

        root.setStyle("-fx-background-color: " + PAGE + ";");
        root.setAlignment(Pos.CENTER);

        Label heading = new Label(Lang.t("ui.logIn").toUpperCase());
        heading.setFont(Font.font(head(), 44));
        heading.setTextFill(Color.web(INK));

        note.setFont(Font.font(body(), 12));
        note.setTextFill(Color.web(RED));
        note.setWrapText(true);
        note.setMaxWidth(380);
        note.setMinHeight(34);

        emailRow = row(fieldLabel(Lang.t("ui.email")), email);

        Button back = action(Lang.t("ui.back"));
        back.setOnAction(e -> onBack.run());

        submit.setOnAction(e -> go());
        mode.setOnAction(e -> toggle());

        VBox form = new VBox(10,
                heading,
                emailRow,
                row(identifierLabel, identifier),
                row(fieldLabel(Lang.t("ui.password")), password),
                note,
                submit, mode, back);

        form.setMaxWidth(420);
        form.setPadding(new Insets(40));

        root.getChildren().add(form);

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

        Platform.runLater(identifier::requestFocus);
    }

    public StackPane getRoot() {
        return root;
    }

    /** Registration needs an address as well; signing in does not. */
    private void toggle() {
        registering = !registering;
        note.setText("");
        showMode();
    }

    private void showMode() {

        emailRow.setVisible(registering);
        emailRow.setManaged(registering);

        identifierLabel.setText(
                (registering ? Lang.t("ui.username") : Lang.t("ui.usernameOrEmail")).toUpperCase());

        submit.setText((registering ? Lang.t("ui.signUp") : Lang.t("ui.logIn")).toUpperCase());
        mode.setText((registering ? Lang.t("ui.logIn") : Lang.t("ui.signUp")).toUpperCase());
    }

    private void go() {

        if (working) {
            return;
        }

        if (account == null) {
            note.setText(Lang.t("ui.authUnavailable"));
            return;
        }

        String id = identifier.getText() == null ? "" : identifier.getText().trim();
        String pass = password.getText() == null ? "" : password.getText();
        String mail = email.getText() == null ? "" : email.getText().trim();

        if (id.isEmpty() || pass.isEmpty() || (registering && mail.isEmpty())) {
            note.setText(Lang.t("ui.fillFields"));
            return;
        }

        working = true;
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
                note.setTextFill(Color.web(done ? GRAY : RED));
                note.setText(shown == null ? "" : shown);

                if (done) {
                    onDone.run();
                }
            });
        }, "fityesz-auth");

        worker.setDaemon(true);
        worker.start();
    }

    private VBox row(Label label, TextField input) {

        input.setFont(Font.font(body(), 15));
        input.setPrefHeight(38);
        input.setStyle("-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-border-color: transparent transparent " + RULE + " transparent;"
                + "-fx-border-width: 0 0 1 0;"
                + "-fx-text-fill: " + INK + ";"
                + "-fx-padding: 0;");

        return new VBox(2, label, input);
    }

    private Label fieldLabel(String text) {

        Label l = new Label(text.toUpperCase());
        l.setFont(Font.font(body(), 10));
        l.setTextFill(Color.web(GRAY));
        return l;
    }

    /** A menu line in the title screen's hand: flat, with a red rule on hover. */
    private Button action(String text) {

        Button b = new Button(text.toUpperCase());
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(44);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font(head(), 26));
        b.setFocusTraversable(false);

        String base = "-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-padding: 0 10 0 16;"
                + "-fx-border-width: 0 0 0 4;"
                + "-fx-cursor: hand;";

        String idle = base + "-fx-border-color: transparent; -fx-text-fill: " + INK + ";";
        String on = base + "-fx-border-color: " + RED + "; -fx-text-fill: " + RED + ";";

        b.setStyle(idle);
        b.setOnMouseEntered(e -> b.setStyle(on));
        b.setOnMouseExited(e -> b.setStyle(idle));

        return b;
    }

    private static String head() {
        return Font.getFamilies().contains("Antonio") ? "Antonio" : "Arial Narrow";
    }

    private static String body() {
        return Font.getFamilies().contains("Public Sans") ? "Public Sans" : "Arial";
    }
}
