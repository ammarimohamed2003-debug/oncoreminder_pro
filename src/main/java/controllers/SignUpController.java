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

public class SignUpController {

    @FXML
    private TextField prenomField;

    @FXML
    private TextField nomField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    void handleSignUp(ActionEvent event) {
        String prenom = prenomField.getText().trim();
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // 1. Check for empty fields
        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // 2. Validate Names (Letters only)
        if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$") || !nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) {
            showError("Le nom et le prénom ne doivent contenir que des lettres.");
            return;
        }

        // 3. Validate Email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Format d'email invalide (ex: nom@domaine.com).");
            return;
        }

        // 4. Validate Password length
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        // 5. Check if email already exists
        if (serviceUtilisateur.emailExists(email)) {
            showError("Cet email est déjà utilisé.");
            return;
        }

        // Database registration
        Utilisateur newUser = new Utilisateur(nom, prenom, email, password, "ROLE_USER");
        serviceUtilisateur.add(newUser);

        errorLabel.setText("Compte créé avec succès ! Redirection...");
        errorLabel.setStyle("-fx-text-fill: #38a169;");
        errorLabel.setVisible(true);

        // Optional: redirection after short delay or direct
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #e53e3e;");
        errorLabel.setVisible(true);
    }

    @FXML
    void goToLogin(ActionEvent event) {
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
