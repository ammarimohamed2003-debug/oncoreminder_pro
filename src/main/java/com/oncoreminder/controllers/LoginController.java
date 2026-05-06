package com.oncoreminder.controllers;

import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        Utilisateur user = serviceUtilisateur.login(email, password);

        if (user != null) {
            UserSession.getInstance().login(user);
            redirectToDashboard(user.getRole());
        } else {
            showError("Email ou mot de passe incorrect.");
        }
    }

    private void redirectToDashboard(String role) {
        String fxmlFile = "";
        switch (role) {
            case "ADMIN":
                fxmlFile = "/views/AdminDashboard.fxml";
                break;
            case "MEDECIN":
                fxmlFile = "/views/DoctorDashboard.fxml";
                break;
            case "PATIENT":
                fxmlFile = "/views/PatientDashboard.fxml";
                break;
            default:
                showError("Rôle inconnu.");
                return;
        }

        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur de chargement du tableau de bord.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    void goToSignUp(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/SignUp.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
