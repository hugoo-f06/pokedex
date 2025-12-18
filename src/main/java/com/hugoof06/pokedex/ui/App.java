package com.hugoof06.pokedex.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        var root = new MainView().getRoot();
        var scene = new Scene(root, 1000, 650);

        stage.setTitle("Pokedex");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
