package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.services.EmailService;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.OtpStore;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * ForgotPasswordController — Étape 1 du flux de récupération.
 *
 * Flux :
 *   1. Saisie + validation email
 *   2. Vérification existence en BDD
 *   3. Génération OTP (6 chiffres, 5 min)
 *   4. Envoi email SMTP (ou affichage console en mode DEV)
 *   5. Navigation vers VerifyOtp
 */
public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label     errorEmail;
    @FXML private Label     errorLabel;
    @FXML private Label     successLabel;
    @FXML private Button    sendButton;

    private static final String EMAIL_REGEX =
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        // Nettoyer erreurs à la saisie
        emailField.textProperty().addListener((obs, o, n) -> clearError());
    }

    @FXML
    void handleSendOtp(ActionEvent event) {
        clearError();
        String email = emailField.getText().trim();

        // ── Validation ──────────────────────────────────────────────
        if (email.isEmpty()) {
            showFieldError("Veuillez entrer votre email");
            return;
        }
        if (!email.matches(EMAIL_REGEX)) {
            showFieldError("Adresse email invalide");
            return;
        }

        // ── Désactiver bouton + indication chargement ────────────────
        sendButton.setDisable(true);
        sendButton.setText("Vérification...");

        // ── Thread de vérification ──────────────────────────────────
        Thread thread = new Thread(() -> {
            boolean exists = serviceUtilisateur.emailExists(email);

            Platform.runLater(() -> {
                if (!exists) {
                    sendButton.setDisable(false);
                    sendButton.setText("Envoyer le code OTP");
                    showGlobalError("Aucun compte associé à cet email.");
                    return;
                }

                // OTP généré
                String otp = OtpStore.getInstance().generateAndStore(email);
                sendButton.setText("Envoi en cours...");

                // Envoi email en arrière-plan
                Thread emailThread = new Thread(() -> {
                    boolean sent = false;
                    try {
                        EmailService.sendOtpEmail(email, otp);
                        sent = true;
                    } catch (Exception e) {
                        // Mode DEV — afficher OTP dans la console
                        System.out.println("╔══════════════════════════════════════╗");
                        System.out.println("║  FORGOT PASSWORD — MODE DEV          ║");
                        System.out.println("║  Email  : " + email);
                        System.out.println("║  Code OTP : " + otp + "              ║");
                        System.out.println("╚══════════════════════════════════════╝");
                    }

                    final boolean emailSent = sent;
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        sendButton.setText("Envoyer le code OTP");

                        String msg = emailSent
                            ? "✅ Code envoyé ! Vérifiez votre boîte email."
                            : "✅ Code généré ! (Vérifiez la console IntelliJ)";
                        showSuccess(msg);

                        // Attendre 1.5s puis naviguer
                        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                        pause.setOnFinished(ev -> {
                            VerifyOtpController.setPendingEmail(email);
                            App.navigate("VerifyOtp");
                        });
                        pause.play();
                    });
                });
                emailThread.setDaemon(true);
                emailThread.start();
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    void goBack(ActionEvent event) {
        App.navigate("Login");
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void showFieldError(String msg) {
        errorEmail.setText(msg);
        errorEmail.setVisible(true);
        errorEmail.setManaged(true);
        emailField.getStyleClass().add("field-input-error");
    }

    private void showGlobalError(String msg) {
        errorLabel.setText("❌  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(400), successLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void clearError() {
        errorEmail.setVisible(false);
        errorEmail.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        emailField.getStyleClass().remove("field-input-error");
    }
}
