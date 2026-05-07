package com.oncoreminder.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Stage primaryStage;
    private static double W;
    private static double H;

    @Override
    public void start(Stage stage) throws Exception {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        W = bounds.getWidth();
        H = bounds.getHeight();

        primaryStage = stage;
        primaryStage.setTitle("OncoReminder Pro");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setMaximized(true);

        navigate("Login");
        primaryStage.show();
    }

    public static void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(App.class.getResource("/views/" + fxml + ".fxml"));
            Scene scene = new Scene(root, W, H);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setRoot(String fxml) throws IOException {
        navigate(fxml);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
