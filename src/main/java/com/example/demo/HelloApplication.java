package com.example.demo;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {

        GameController controller = new GameController(stage);

        OpeningScreen opening = new OpeningScreen(

                // NEW GAME: swap the opening page for the game
                () -> {
                    Scene scene = stage.getScene();
                    var gameRoot = controller.getUI().getRoot();

                    gameRoot.setOpacity(0);
                    scene.setRoot(gameRoot);

                    // Enter / Space = continue, 1-9 = choices
                    // (installed only now, so keys can't trigger the hidden game on the opening page)
                    controller.getUI().installKeys(scene);

                    FadeTransition fade = new FadeTransition(Duration.seconds(0.6), gameRoot);
                    fade.setFromValue(0);
                    fade.setToValue(1);
                    fade.play();
                },

                // EXIT: close the game
                Platform::exit
        );

        Scene scene = new Scene(opening.getRoot(), 1200, 800);

        stage.setTitle("The Fityesz Chronicle");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}