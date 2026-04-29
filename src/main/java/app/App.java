package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("OncoReminder - Gestion Utilisateurs");
        setRoot("Utilisateur");
        primaryStage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        String path = fxml.equals("Utilisateur") ? "/gui/" + fxml + ".fxml" : "/views/" + fxml + ".fxml";
        FXMLLoader loader = new FXMLLoader(App.class.getResource(path));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        String css = App.class.getResource("/styles/styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
