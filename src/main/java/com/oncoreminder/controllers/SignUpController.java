package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * SignUpController — Inscription d'un nouvel utilisateur.
 *
 * Améliorations v2 :
 *  ✔ Champ "Confirmer le mot de passe"
 *  ✔ Validation en temps réel (mots de passe identiques)
 *  ✔ Toggle afficher/masquer mot de passe
 *  ✔ Barre de force du mot de passe
 *  ✔ Indicateurs de règles de sécurité (panel gauche)
 *  ✔ Désactivation bouton si les mots de passe ne correspondent pas
 */
public class SignUpController {

    @FXML private TextField        nomField;
    @FXML private TextField        prenomField;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;
    @FXML private TextField        passwordVisible;
    @FXML private PasswordField    confirmField;
    @FXML private TextField        confirmVisible;
    @FXML private CheckBox         showPwdCheck;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ProgressBar      strengthBar;
    @FXML private Button           signUpButton;

    // Labels d'erreur
    @FXML private Label errorNom;
    @FXML private Label errorPrenom;
    @FXML private Label errorEmail;
    @FXML private Label errorPassword;
    @FXML private Label errorConfirm;

    // Indicateurs de règles (panel gauche)
    @FXML private Label ruleLength;
    @FXML private Label ruleUpper;
    @FXML private Label ruleDigit;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("PATIENT", "MEDECIN"));
        roleCombo.setValue("PATIENT");

        // Synchroniser PasswordField ↔ TextField (show/hide)
        passwordField.textProperty().bindBidirectional(passwordVisible.textProperty());
        confirmField.textProperty().bindBidirectional(confirmVisible.textProperty());

        // Listeners nettoyage erreurs + validation temps réel
        nomField.textProperty().addListener((obs, o, n)    -> clearErr(nomField, errorNom));
        prenomField.textProperty().addListener((obs, o, n) -> clearErr(prenomField, errorPrenom));
        emailField.textProperty().addListener((obs, o, n)  -> clearErr(emailField, errorEmail));

        passwordField.textProperty().addListener((obs, o, n) -> {
            clearErr(passwordField, errorPassword);
            updateStrength(n);
            updateRules(n);
            validateConfirm();
        });

        confirmField.textProperty().addListener((obs, o, n) -> {
            clearErr(confirmField, errorConfirm);
            validateConfirm();
        });
    }

    // ── Toggle afficher/masquer mot de passe ──────────────────────────

    @FXML
    void toggleShowPassword(ActionEvent event) {
        boolean show = showPwdCheck.isSelected();

        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        passwordVisible.setVisible(show);
        passwordVisible.setManaged(show);

        confirmField.setVisible(!show);
        confirmField.setManaged(!show);
        confirmVisible.setVisible(show);
        confirmVisible.setManaged(show);
    }

    // ── Validation temps réel mots de passe ──────────────────────────

    /**
     * Vérifie que les deux mots de passe sont identiques et active/désactive le bouton.
     */
    private void validateConfirm() {
        String pwd     = passwordField.getText();
        String confirm = confirmField.getText();

        if (confirm.isEmpty()) {
            errorConfirm.setVisible(false);
            errorConfirm.setManaged(false);
            confirmField.getStyleClass().remove("field-input-error");
        } else if (!pwd.equals(confirm)) {
            errorConfirm.setText("❌ Les mots de passe ne correspondent pas");
            errorConfirm.setVisible(true);
            errorConfirm.setManaged(true);
            confirmField.getStyleClass().remove("field-input-error");
            confirmField.getStyleClass().add("field-input-error");
            signUpButton.setDisable(true);
        } else {
            errorConfirm.setText("✅ Les mots de passe correspondent");
            errorConfirm.setStyle("-fx-text-fill: #2BBCB0; -fx-font-size: 11px;");
            errorConfirm.setVisible(true);
            errorConfirm.setManaged(true);
            confirmField.getStyleClass().remove("field-input-error");
            signUpButton.setDisable(false);
        }
    }

    // ── Barre de force du mot de passe ───────────────────────────────

    private void updateStrength(String pwd) {
        int score = 0;
        if (pwd.length() >= 8)                score++;
        if (pwd.matches(".*[A-Z].*"))         score++;
        if (pwd.matches(".*[a-z].*"))         score++;
        if (pwd.matches(".*[0-9].*"))         score++;
        if (pwd.matches(".*[^A-Za-z0-9].*")) score++;

        strengthBar.setProgress(score / 5.0);
        String color = score <= 1 ? "#FC8181" : score == 2 ? "#F6AD55"
                     : score == 3 ? "#F6E05E" : score == 4 ? "#68D391" : "#2BBCB0";
        strengthBar.setStyle("-fx-accent: " + color + ";");
    }

    // ── Indicateurs de règles (labels panel gauche) ──────────────────

    private void updateRules(String pwd) {
        setRule(ruleLength, pwd.length() >= 8,          "8 caractères minimum");
        setRule(ruleUpper,  pwd.matches(".*[A-Z].*"),   "1 majuscule");
        setRule(ruleDigit,  pwd.matches(".*[0-9].*"),   "1 chiffre");
    }

    private void setRule(Label lbl, boolean ok, String text) {
        if (lbl == null) return;
        if (ok) {
            lbl.setText("✓ " + text);
            lbl.setStyle("-fx-text-fill: #2BBCB0; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            lbl.setText("● " + text);
            lbl.setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 11px;");
        }
    }

    // ── Inscription ───────────────────────────────────────────────────

    @FXML
    void handleSignUp(ActionEvent event) {
        clearAll();
        String nom      = nomField.getText().trim();
        String prenom   = prenomField.getText().trim();
        String email    = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();
        String role     = roleCombo.getValue();
        boolean ok = true;

        // ── Validation champs obligatoires ────────────────────────────
        if (nom.isEmpty()) {
            fieldErr(nomField, errorNom, "Le nom est obligatoire.");
            ok = false;
        }
        if (prenom.isEmpty()) {
            fieldErr(prenomField, errorPrenom, "Le prénom est obligatoire.");
            ok = false;
        }
        if (email.isEmpty()) {
            fieldErr(emailField, errorEmail, "L'email est obligatoire.");
            ok = false;
        } else if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            fieldErr(emailField, errorEmail, "Format d'email invalide.");
            ok = false;
        } else if (serviceUtilisateur.emailExists(email)) {
            fieldErr(emailField, errorEmail, "Cet email est déjà utilisé.");
            ok = false;
        }

        // ── Validation mot de passe ────────────────────────────────────
        if (password.isEmpty()) {
            fieldErr(passwordField, errorPassword, "Le mot de passe est obligatoire.");
            ok = false;
        } else if (password.length() < 8) {
            fieldErr(passwordField, errorPassword, "Minimum 8 caractères requis.");
            ok = false;
        } else if (!password.matches(".*[A-Z].*")) {
            fieldErr(passwordField, errorPassword, "Au moins 1 majuscule requise.");
            ok = false;
        } else if (!password.matches(".*[0-9].*")) {
            fieldErr(passwordField, errorPassword, "Au moins 1 chiffre requis.");
            ok = false;
        }

        // ── Validation confirmation ────────────────────────────────────
        if (ok && !password.equals(confirm)) {
            fieldErr(confirmField, errorConfirm, "Les mots de passe ne correspondent pas.");
            ok = false;
        }

        if (!ok) return;

        // ── Enregistrement ────────────────────────────────────────────
        serviceUtilisateur.add(new Utilisateur(nom, prenom, email, password, role));

        // Alerte succès
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Compte créé");
        alert.setHeaderText("Inscription réussie !");
        alert.setContentText("Votre compte a été créé.\nVous pouvez maintenant vous connecter.");
        alert.showAndWait();
        App.navigate("Login");
    }

    @FXML
    void goToLogin(ActionEvent event) { App.navigate("Login"); }

    // ── Helpers erreurs ───────────────────────────────────────────────

    private void fieldErr(Control field, Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px;");
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
        clearErr(confirmField,  errorConfirm);
    }
}
