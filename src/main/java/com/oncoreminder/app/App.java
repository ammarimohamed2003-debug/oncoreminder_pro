package com.oncoreminder.app;

import com.oncoreminder.utils.ChatbotFab;
import com.oncoreminder.utils.UserSession;
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

    /**
     * Navigation principale — charge un FXML depuis /views/ et l'affiche.
     * Capture TOUTES les exceptions (IOException + RuntimeException) pour éviter les pages blanches.
     */
    public static void navigate(String fxml) {
        try {
            java.net.URL resource = App.class.getResource("/views/" + fxml + ".fxml");
            if (resource == null) {
                System.err.println("[App.navigate] FXML introuvable : /views/" + fxml + ".fxml");
                return;
            }
            Parent root = FXMLLoader.load(resource);
            boolean loggedIn = UserSession.getInstance().getCurrentUser() != null;
            Parent displayRoot = loggedIn ? ChatbotFab.wrapWithChatbot(root) : root;
            Scene scene = new Scene(displayRoot, W, H);
            java.net.URL css = App.class.getResource("/css/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("[App.navigate] IOException chargement " + fxml + " : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[App.navigate] Erreur inattendue chargement " + fxml + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void setRoot(String fxml) throws IOException {
        navigate(fxml);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
