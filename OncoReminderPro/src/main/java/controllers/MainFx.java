package controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFx extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionEvent.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 650);
            scene.getStylesheets().add(getClass().getResource("/theme.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("OncoReminder Pro - Event & Reservation");
            primaryStage.show();
        } catch (IOException e) {
            System.out.println("Erreur chargement FXML : " + e.getMessage());
        }
    }
}
