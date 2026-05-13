package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.utils.OtpStore;
import com.oncoreminder.utils.OtpStore.OtpResult;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

/**
 * VerifyOtpController — Gestion de l'étape 2 : vérification du code OTP.
 *
 * Stocke l'email en attente via un champ statique (passé depuis ForgotPasswordController).
 * Valide le code saisi, gère les cas : valide, expiré, invalide.
 */
public class VerifyOtpController {

    @FXML private TextField otpField;
    @FXML private Label     errorOtp;
    @FXML private Label     errorLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Button    verifyButton;

    /** Email en attente de vérification — partagé entre les étapes */
    private static String pendingEmail;

    /** Setter appelé par ForgotPasswordController avant la navigation */
    public static void setPendingEmail(String email) {
        pendingEmail = email;
    }

    @FXML
    public void initialize() {
        // Afficher l'email masqué dans le sous-titre
        if (pendingEmail != null && !pendingEmail.isEmpty()) {
            subtitleLabel.setText("Code envoyé à : " + maskEmail(pendingEmail));
        }

        // Filtrage : accepter uniquement des chiffres, max 6
        otpField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Supprimer tout ce qui n'est pas un chiffre
            String filtered = newVal.replaceAll("[^0-9]", "");
            // Limiter à 6 chiffres
            if (filtered.length() > 6) filtered = filtered.substring(0, 6);
            if (!filtered.equals(newVal)) {
                otpField.setText(filtered);
            }
            clearError();
            // Vérification automatique quand 6 chiffres sont entrés
            if (filtered.length() == 6) {
                verifyButton.setDefaultButton(true);
            }
        });

        // Animation FadeTransition d'entrée
        FadeTransition ft = new FadeTransition(Duration.millis(400), otpField);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    /**
     * Gestion du clic sur "Vérifier le code".
     */
    @FXML
    void handleVerify(ActionEvent event) {
        clearError();
        String code = otpField.getText().trim();

        // ── Validation de la saisie ───────────────────────────────────
        if (code.isEmpty()) {
            showFieldError("Veuillez entrer le code OTP");
            return;
        }
        if (!code.matches("\\d{6}")) {
            showFieldError("Le code doit contenir exactement 6 chiffres");
            return;
        }

        // ── Vérification OTP ──────────────────────────────────────────
        if (pendingEmail == null || pendingEmail.isEmpty()) {
            showGlobalError("Session expirée. Recommencez depuis le début.");
            return;
        }

        OtpResult result = OtpStore.getInstance().verify(pendingEmail, code);

        switch (result) {
            case VALID:
                // ✅ Code correct → aller à ResetPassword
                ResetPasswordController.setPendingEmail(pendingEmail);
                App.navigate("ResetPassword");
                break;

            case EXPIRED:
                showGlobalError("⏱ Code expiré. Renvoyez un nouveau code.");
                otpField.clear();
                break;

            case INVALID:
                showGlobalError("❌ Code invalide. Vérifiez et réessayez.");
                // Secouer le champ (effet d'erreur)
                shakeField();
                break;
        }
    }

    /** Renvoie un nouveau code OTP */
    @FXML
    void handleResend(ActionEvent event) {
        if (pendingEmail == null || pendingEmail.isEmpty()) {
            App.navigate("ForgotPassword");
            return;
        }
        // Revenir à ForgotPassword pour regénérer un OTP
        App.navigate("ForgotPassword");
    }

    @FXML
    void goBack(ActionEvent event) {
        App.navigate("ForgotPassword");
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Masque partiellement l'email pour l'affichage : ex: a***@gmail.com */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return email;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    private void showFieldError(String msg) {
        errorOtp.setText(msg);
        errorOtp.setVisible(true);
        errorOtp.setManaged(true);
        otpField.getStyleClass().add("field-input-error");
    }

    private void showGlobalError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void clearError() {
        errorOtp.setVisible(false);
        errorOtp.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        otpField.getStyleClass().remove("field-input-error");
    }

    /** Animation de secousse du champ OTP en cas d'erreur */
    private void shakeField() {
        javafx.animation.TranslateTransition shake =
            new javafx.animation.TranslateTransition(Duration.millis(60), otpField);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.play();
    }
}
