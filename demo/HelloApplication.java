package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {

        GameController controller = new GameController(stage);

        Scene scene = new Scene(
                controller.getUI().getRoot(),
                1200,
                800
        );

        // Enter / Space = continue (finishes the typewriter first), 1-9 = choices
        controller.getUI().installKeys(scene);

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