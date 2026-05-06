package com.oncoreminder.controllers;

import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class SignUpController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label errorLabel;
    @FXML private Button signUpButton;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("PATIENT", "MEDECIN"));
        roleCombo.setValue("PATIENT");
        errorLabel.setVisible(false);
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleCombo.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            showError("Tous les champs sont obligatoires.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Format d'email invalide.");
            return;
        }

        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        if (serviceUtilisateur.emailExists(email)) {
            showError("Cet email est déjà utilisé.");
            return;
        }

        Utilisateur user = new Utilisateur(nom, prenom, email, password, role);
        serviceUtilisateur.add(user);

        goToLogin(null);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    void goToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) signUpButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
