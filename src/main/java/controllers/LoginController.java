package controllers;

import app.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import models.Utilisateur;
import services.ServiceUtilisateur;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // 1. Check for empty fields
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // 2. Validate Email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Format d'email invalide.");
            return;
        }

        // Database authentication
        Utilisateur user = serviceUtilisateur.login(email, password);
        
        if (user != null) {
            try {
                if ("ROLE_ADMIN".equals(user.getRole())) {
                    App.setRoot("AdminDashboard");
                } else {
                    App.setRoot("Dashboard");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showError("Email ou mot de passe incorrect.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    void goToSignUp(ActionEvent event) {
        try {
            App.setRoot("SignUp");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
