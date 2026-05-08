package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SignUpController {

    @FXML private TextField     nomField;
    @FXML private TextField     prenomField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label         errorNom;
    @FXML private Label         errorPrenom;
    @FXML private Label         errorEmail;
    @FXML private Label         errorPassword;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("PATIENT", "MEDECIN"));
        roleCombo.setValue("PATIENT");
        nomField.textProperty().addListener((obs, o, n)      -> clearErr(nomField,      errorNom));
        prenomField.textProperty().addListener((obs, o, n)   -> clearErr(prenomField,   errorPrenom));
        emailField.textProperty().addListener((obs, o, n)    -> clearErr(emailField,    errorEmail));
        passwordField.textProperty().addListener((obs, o, n) -> clearErr(passwordField, errorPassword));
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        clearAll();
        String nom      = nomField.getText().trim();
        String prenom   = prenomField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String role     = roleCombo.getValue();
        boolean ok = true;

        if (nom.isEmpty())    { fieldErr(nomField,      errorNom,      "Le nom est obligatoire.");     ok = false; }
        if (prenom.isEmpty()) { fieldErr(prenomField,   errorPrenom,   "Le prénom est obligatoire.");  ok = false; }
        if (email.isEmpty())  { fieldErr(emailField,    errorEmail,    "L'email est obligatoire.");    ok = false; }
        else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$"))
                              { fieldErr(emailField,    errorEmail,    "Format d'email invalide.");    ok = false; }
        else if (serviceUtilisateur.emailExists(email))
                              { fieldErr(emailField,    errorEmail,    "Cet email est déjà utilisé."); ok = false; }
        if (password.isEmpty())      { fieldErr(passwordField, errorPassword, "Le mot de passe est obligatoire."); ok = false; }
        else if (password.length() < 6)
                                     { fieldErr(passwordField, errorPassword, "Minimum 6 caractères."); ok = false; }
        if (!ok) return;

        serviceUtilisateur.add(new Utilisateur(nom, prenom, email, password, role));
        App.navigate("Login");
    }

    @FXML
    void goToLogin(ActionEvent event) { App.navigate("Login"); }

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
    }

    private void clearAll() {
        clearErr(nomField,      errorNom);
        clearErr(prenomField,   errorPrenom);
        clearErr(emailField,    errorEmail);
        clearErr(passwordField, errorPassword);
    }
}
