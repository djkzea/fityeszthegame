package com.example.demo;

import java.io.InputStream;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HelloApplication extends Application {

    private static final String PAGE = "#F4EFE6";
    private static final String INK  = "#161616";

    private static final String DISCLAIMER_HU =
            "A filmben szereplő személyek és események kitaláltak. "
            + "Bármilyen hasonlóság valós személyekkel vagy eseményekkel "
            + "csupán a véletlen műve.";

    private static final String DISCLAIMER_EN =
            "The characters and events depicted in this film are fictitious. "
            + "Any similarity to actual persons, living or dead, is purely coincidental.";

    /**
     * Screens live inside this, with the curtain above them and the card above that,
     * so a transition can hold its cover across the swap underneath it.
     */
    private final StackPane shell = new StackPane();
    private final Region curtain = new Region();
    private final StackPane card = new StackPane();

    /** set while the card is on screen; any key or click runs it */
    private Runnable skip;

    @Override
    public void start(Stage stage) {

        GameController controller = new GameController(stage);

        curtain.setStyle("-fx-background-color: " + INK + ";");
        curtain.setOpacity(0);
        curtain.setMouseTransparent(true);

        card.setAlignment(Pos.CENTER);
        card.setOpacity(0);
        card.setMouseTransparent(true);

        OpeningScreen opening = new OpeningScreen(
                () -> enterGame(controller),
                () -> dip(Platform::exit, 420));

        shell.getChildren().addAll(opening.getRoot(), curtain, card);

        Scene scene = new Scene(shell, 1200, 800);

        // the title page is cream and the game is dark; without this the gap between
        // them is the scene's default white, which reads as a flash
        scene.setFill(Color.web(PAGE));

        // filters, so this gets the key before the page underneath does
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (skip != null) {
                skip.run();
                e.consume();
            }
        });
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

    /**
     * Title page pushes in as the curtain falls, the disclaimer plays on the ink,
     * then the game lifts out of it.
     */
    private void enterGame(GameController controller) {

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
            controller.begin();
            controller.getUI().installKeys(shell.getScene());

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

        FadeTransition in = new FadeTransition(Duration.millis(800), card);
        in.setFromValue(0);
        in.setToValue(1);

        FadeTransition out = new FadeTransition(Duration.millis(700), card);
        out.setFromValue(1);
        out.setToValue(0);

        SequentialTransition play = new SequentialTransition(
                in, new PauseTransition(Duration.millis(2600)), out);

        play.setOnFinished(e -> {
            skip = null;
            done.run();
        });

        // any key or click cuts it short, the way a title card should
        skip = () -> {
            skip = null;
            play.stop();
            card.setOpacity(0);
            done.run();
        };

        play.play();
    }

    /**
     * Fades to ink, runs the swap behind it, then lifts. The curtain takes the clicks
     * while it is down so the screen underneath cannot be triggered twice.
     */
    private void dip(Runnable swap, double fallMillis) {

        curtain.setMouseTransparent(false);

        FadeTransition fall = new FadeTransition(Duration.millis(fallMillis), curtain);
        fall.setFromValue(0);
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

    /** Public Sans if the game's fonts loaded, otherwise whatever the system offers. */
    private static String bodyFamily() {

        try (InputStream in = HelloApplication.class
                .getResourceAsStream("/com/example/demo/fonts/PublicSans-Regular.ttf")) {
            if (in != null) {
                Font.loadFont(in, 12);
            }
        } catch (Exception ignored) {
        }

        return Font.getFamilies().contains("Public Sans") ? "Public Sans" : "Segoe UI";
    }

    public static void main(String[] args) {
        launch();
    }
}
