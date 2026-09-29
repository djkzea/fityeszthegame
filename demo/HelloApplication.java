package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {

        GameController controller =
                new GameController(stage);


        Scene scene = new Scene(
                controller.getUI().getRoot(),
                1200,
                800
        );


        stage.setTitle(
                "The Fityesz Chronicle"
        );


        stage.setScene(scene);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {

            if (event.getCode() == KeyCode.ENTER) {
                controller.getUI().pressEnterButton();
                event.consume();
            }
        });

        stage.show();
    }


    public static void main(String[] args) {

        launch();
    }
}