package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
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

        stage.show();
    }


    public static void main(String[] args) {

        launch();
    }
}