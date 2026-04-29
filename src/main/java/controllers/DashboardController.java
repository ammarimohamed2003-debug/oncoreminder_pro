package controllers;

import app.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Label formeLabel;

    @FXML
    private Label stressLabel;

    @FXML
    private Label objectifsLabel;

    @FXML
    public void initialize() {
        // Here you would load data from the database
        // For now, we mock the stats to match the health and wellness theme
        formeLabel.setText("8/10");
        stressLabel.setText("Niveau de stress: 3/10");
        objectifsLabel.setText("5");
    }

    @FXML
    void logout(ActionEvent event) {
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
