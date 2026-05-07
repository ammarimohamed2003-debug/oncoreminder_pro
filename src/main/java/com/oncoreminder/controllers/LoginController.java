package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
            switch (user.getRole()) {
                case "ADMIN":   App.navigate("AdminDashboard"); break;
                case "MEDECIN": App.navigate("DoctorDashboard"); break;
                case "PATIENT": App.navigate("PatientDashboard"); break;
                default: showError("Rôle inconnu.");
            }
        } else {
            showError("Email ou mot de passe incorrect.");
        }
    }

    @FXML
    void goToSignUp(ActionEvent event) {
        App.navigate("SignUp");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
