package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorEmail;
    @FXML private Label         errorPassword;
    @FXML private Label         errorLabel;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        emailField.textProperty().addListener((obs, o, n) -> clearErr(emailField, errorEmail));
        passwordField.textProperty().addListener((obs, o, n) -> clearErr(passwordField, errorPassword));
    }

    @FXML
    void handleLogin(ActionEvent event) {
        clearAll();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        boolean ok = true;

        if (email.isEmpty()) {
            fieldErr(emailField, errorEmail, "L'email est obligatoire.");
            ok = false;
        }
        if (password.isEmpty()) {
            fieldErr(passwordField, errorPassword, "Le mot de passe est obligatoire.");
            ok = false;
        }
        if (!ok) return;

        Utilisateur user = serviceUtilisateur.login(email, password);
        if (user != null) {
            UserSession.getInstance().login(user);
            switch (user.getRole()) {
                case "ADMIN":   App.navigate("AdminDashboard");   break;
                case "MEDECIN": App.navigate("DoctorDashboard");  break;
                case "PATIENT": App.navigate("PatientDashboard"); break;
                default: showGlobalError("Rôle inconnu.");
            }
        } else {
            showGlobalError("Email ou mot de passe incorrect.");
        }
    }

    @FXML
    void goToSignUp(ActionEvent event) { App.navigate("SignUp"); }

    /** Naviguer vers la page Mot de passe oublié */
    @FXML
    void goToForgotPassword(ActionEvent event) { App.navigate("ForgotPassword"); }

    // ── Helpers ──────────────────────────────────────────────────────

    private void fieldErr(Control field, Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
        field.getStyleClass().add("field-input-error");
    }

    private void clearErr(Control field, Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
        field.getStyleClass().remove("field-input-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearAll() {
        clearErr(emailField,    errorEmail);
        clearErr(passwordField, errorPassword);
    }

    private void showGlobalError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
