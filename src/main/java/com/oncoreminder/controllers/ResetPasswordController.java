package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.services.ServiceUtilisateur;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

/**
 * ResetPasswordController — Gestion de l'étape 3 : définition du nouveau mot de passe.
 *
 * Règles de validation :
 *   ✔ Minimum 8 caractères
 *   ✔ Au moins 1 majuscule
 *   ✔ Au moins 1 minuscule
 *   ✔ Au moins 1 chiffre
 *   ✔ Confirmation identique
 *
 * Sécurité : mot de passe haché avec BCrypt avant insertion en BDD.
 */
public class ResetPasswordController {

    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private PasswordField confirmField;
    @FXML private TextField     confirmVisible;
    @FXML private CheckBox      showPasswordCheck;
    @FXML private Label         errorPassword;
    @FXML private Label         errorConfirm;
    @FXML private Label         errorLabel;
    @FXML private ProgressBar   strengthBar;
    @FXML private Label         strengthLabel;
    @FXML private Label         ruleLength;
    @FXML private Label         ruleUpper;
    @FXML private Label         ruleLower;
    @FXML private Label         ruleDigit;
    @FXML private Button        confirmButton;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    /** Email de l'utilisateur dont on réinitialise le mot de passe */
    private static String pendingEmail;

    public static void setPendingEmail(String email) {
        pendingEmail = email;
    }

    @FXML
    public void initialize() {
        // Synchroniser PasswordField ↔ TextField (pour show/hide)
        passwordField.textProperty().bindBidirectional(passwordVisible.textProperty());
        confirmField.textProperty().bindBidirectional(confirmVisible.textProperty());

        // Mettre à jour la barre de force à chaque saisie
        passwordField.textProperty().addListener((obs, o, n) -> {
            updateStrengthBar(n);
            updateRuleIndicators(n);
            clearError(errorPassword, passwordField);
            clearError(errorPassword, passwordVisible);
        });
        confirmField.textProperty().addListener((obs, o, n) -> {
            clearError(errorConfirm, confirmField);
        });

        // Animation d'entrée
        FadeTransition ft = new FadeTransition(Duration.millis(400), confirmButton);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    /**
     * Toggle afficher/masquer le mot de passe via la checkbox.
     */
    @FXML
    void toggleShowPassword(ActionEvent event) {
        boolean show = showPasswordCheck.isSelected();

        // Nouveau mot de passe
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        passwordVisible.setVisible(show);
        passwordVisible.setManaged(show);

        // Confirmation
        confirmField.setVisible(!show);
        confirmField.setManaged(!show);
        confirmVisible.setVisible(show);
        confirmVisible.setManaged(show);
    }

    /**
     * Gestion du clic "Confirmer le nouveau mot de passe".
     */
    @FXML
    void handleResetPassword(ActionEvent event) {
        clearAllErrors();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();
        boolean valid   = true;

        // ── Règle 1 : obligatoire ────────────────────────────────────
        if (password.isEmpty()) {
            showFieldError(errorPassword, passwordField, "Le mot de passe est obligatoire.");
            valid = false;
        }

        if (valid) {
            // ── Règle 2 : longueur minimum ───────────────────────────
            if (password.length() < 8) {
                showFieldError(errorPassword, passwordField, "Minimum 8 caractères requis.");
                valid = false;
            }
            // ── Règle 3 : majuscule ──────────────────────────────────
            else if (!password.matches(".*[A-Z].*")) {
                showFieldError(errorPassword, passwordField, "Au moins 1 lettre majuscule requise.");
                valid = false;
            }
            // ── Règle 4 : minuscule ──────────────────────────────────
            else if (!password.matches(".*[a-z].*")) {
                showFieldError(errorPassword, passwordField, "Au moins 1 lettre minuscule requise.");
                valid = false;
            }
            // ── Règle 5 : chiffre ────────────────────────────────────
            else if (!password.matches(".*[0-9].*")) {
                showFieldError(errorPassword, passwordField, "Au moins 1 chiffre requis.");
                valid = false;
            }
        }

        // ── Règle 6 : confirmation identique ────────────────────────
        if (valid && !password.equals(confirm)) {
            showFieldError(errorConfirm, confirmField, "Les mots de passe ne correspondent pas.");
            valid = false;
        }

        if (!valid) return;

        // ── Vérification de la session ───────────────────────────────
        if (pendingEmail == null || pendingEmail.isEmpty()) {
            showGlobalError("Session expirée. Recommencez depuis le début.");
            return;
        }

        // ── Hachage BCrypt + mise à jour BDD ─────────────────────────
        confirmButton.setDisable(true);
        confirmButton.setText("Mise à jour en cours...");

        Thread thread = new Thread(() -> {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            boolean success = serviceUtilisateur.updatePassword(pendingEmail, hashedPassword);

            javafx.application.Platform.runLater(() -> {
                confirmButton.setDisable(false);
                confirmButton.setText("Confirmer le nouveau mot de passe");

                if (success) {
                    // ✅ Succès — afficher un alert et retourner au Login
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Succès");
                    alert.setHeaderText("Mot de passe réinitialisé !");
                    alert.setContentText(
                        "Votre mot de passe a été mis à jour avec succès.\n" +
                        "Vous pouvez maintenant vous connecter."
                    );
                    alert.getDialogPane().getStylesheets().add(
                        getClass().getResource("/css/style.css").toExternalForm()
                    );
                    alert.showAndWait();
                    pendingEmail = null; // Nettoyer la session
                    App.navigate("Login");
                } else {
                    showGlobalError("Erreur lors de la mise à jour. Réessayez.");
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    void goBack(ActionEvent event) {
        App.navigate("VerifyOtp");
    }

    // ── Barre de force ────────────────────────────────────────────────

    /** Met à jour la ProgressBar de force selon la complexité du mot de passe */
    private void updateStrengthBar(String pwd) {
        int score = 0;
        if (pwd.length() >= 8)            score++;
        if (pwd.matches(".*[A-Z].*"))     score++;
        if (pwd.matches(".*[a-z].*"))     score++;
        if (pwd.matches(".*[0-9].*"))     score++;
        if (pwd.matches(".*[^A-Za-z0-9].*")) score++; // caractère spécial (bonus)

        double progress = score / 5.0;
        strengthBar.setProgress(progress);

        String color, label;
        if (score <= 1) {
            color = "#FC8181"; label = "Très faible";
        } else if (score == 2) {
            color = "#F6AD55"; label = "Faible";
        } else if (score == 3) {
            color = "#F6E05E"; label = "Moyen";
        } else if (score == 4) {
            color = "#68D391"; label = "Fort";
        } else {
            color = "#2BBCB0"; label = "Très fort";
        }

        strengthBar.setStyle("-fx-accent: " + color + "; -fx-background-radius: 3; -fx-background-color: #EEF4FB;");
        strengthLabel.setText(label);
        strengthLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    /** Met à jour les indicateurs visuels des règles (vert/gris) */
    private void updateRuleIndicators(String pwd) {
        setRule(ruleLength, pwd.length() >= 8);
        setRule(ruleUpper,  pwd.matches(".*[A-Z].*"));
        setRule(ruleLower,  pwd.matches(".*[a-z].*"));
        setRule(ruleDigit,  pwd.matches(".*[0-9].*"));
    }

    private void setRule(Label label, boolean satisfied) {
        if (satisfied) {
            label.setStyle("-fx-text-fill: #2BBCB0; -fx-font-size: 12px; -fx-font-weight: bold;");
            String txt = label.getText();
            if (!txt.startsWith("✓")) {
                label.setText("✓ " + txt.replaceFirst("^[●✓] ?", ""));
            }
        } else {
            label.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 12px;");
            String txt = label.getText();
            if (!txt.startsWith("●")) {
                label.setText("● " + txt.replaceFirst("^[●✓] ?", ""));
            }
        }
    }

    // ── Helpers erreurs ───────────────────────────────────────────────

    private void showFieldError(Label lbl, Control field, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
        field.getStyleClass().add("field-input-error");
    }

    private void showGlobalError(String msg) {
        errorLabel.setText("❌ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError(Label lbl, Control field) {
        lbl.setVisible(false);
        lbl.setManaged(false);
        field.getStyleClass().remove("field-input-error");
    }

    private void clearAllErrors() {
        clearError(errorPassword, passwordField);
        clearError(errorPassword, passwordVisible);
        clearError(errorConfirm,  confirmField);
        clearError(errorConfirm,  confirmVisible);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
