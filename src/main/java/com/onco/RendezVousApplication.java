package com.onco;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RendezVousApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                RendezVousApplication.class.getResource("/com/onco/rendez-vous-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1180, 720);

        stage.setTitle("Gestion des rendez-vous");
        stage.setMinWidth(980);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
