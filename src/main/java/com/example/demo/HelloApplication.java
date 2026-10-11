package com.example.demo;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HelloApplication extends Application {

    private static final String PAGE = "#F4EFE6";
    private static final String INK  = "#161616";

    /**
     * Screens live inside this, with the curtain always on top, so a transition can
     * hold its cover across the swap underneath it.
     */
    private final StackPane shell = new StackPane();
    private final Region curtain = new Region();

    @Override
    public void start(Stage stage) {

        GameController controller = new GameController(stage);

        curtain.setStyle("-fx-background-color: " + INK + ";");
        curtain.setOpacity(0);
        curtain.setMouseTransparent(true);

        OpeningScreen opening = new OpeningScreen(
                () -> enterGame(controller),
                () -> dip(() -> Platform.exit(), 420));

        shell.getChildren().addAll(opening.getRoot(), curtain);

        Scene scene = new Scene(shell, 1200, 800);

        // the title page is cream and the game is dark; without this the gap between
        // them is the scene's default white, which reads as a flash
        scene.setFill(Color.web(PAGE));

        stage.setTitle("The Fityesz Chronicle");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    /** Title page pushes in slightly as the curtain falls, then the game lifts out of it. */
    private void enterGame(GameController controller) {

        Node title = shell.getChildren().get(0);

        ScaleTransition push = new ScaleTransition(Duration.millis(620), title);
        push.setToX(1.04);
        push.setToY(1.04);
        push.setInterpolator(Interpolator.EASE_IN);
        push.play();

        dip(() -> {

            shell.getChildren().set(0, controller.getUI().getRoot());

            // started under the curtain, so the first line is already typing as it lifts
            controller.begin();
            controller.getUI().installKeys(shell.getScene());

        }, 520);
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

            FadeTransition lift = new FadeTransition(Duration.millis(640), curtain);
            lift.setDelay(Duration.millis(140));
            lift.setFromValue(1);
            lift.setToValue(0);
            lift.play();
        });

        fall.play();
    }

    public static void main(String[] args) {
        launch();
    }
}
