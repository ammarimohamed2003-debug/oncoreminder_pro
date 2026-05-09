package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PatientDashboardController {

    @FXML private Label     patientNameLabel;
    @FXML private Label     medecinSidebarLabel;
    @FXML private TextArea  antecedentsArea;
    @FXML private TextArea  allergiesArea;
    @FXML private TextField groupeSanguinField;
    @FXML private TextField poidsField;
    @FXML private TextField tailleField;
    @FXML private Label     successLabel;
    @FXML private Label     bloodGroupLabelDisplay;
    @FXML private Label     poidsLabelDisplay;
    @FXML private Label     tailleLabelDisplay;
    @FXML private Label     errorGroupeSanguin;
    @FXML private Label     errorPoids;
    @FXML private Label     errorTaille;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();
    private Utilisateur currentUser;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            patientNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            loadMedicalData();
            if (currentUser.getMedecinId() != null) {
                Utilisateur dr = serviceUtilisateur.getById(currentUser.getMedecinId());
                if (dr != null && medecinSidebarLabel != null) {
                    medecinSidebarLabel.setText("🩺 Dr. " + dr.getPrenom() + " " + dr.getNom());
                    medecinSidebarLabel.setVisible(true);
                    medecinSidebarLabel.setManaged(true);
                }
            }
        }
        successLabel.setVisible(false);
        groupeSanguinField.textProperty().addListener((obs, o, n) -> clearErr(groupeSanguinField, errorGroupeSanguin));
        poidsField.textProperty().addListener((obs, o, n)         -> clearErr(poidsField,         errorPoids));
        tailleField.textProperty().addListener((obs, o, n)        -> clearErr(tailleField,        errorTaille));
    }

    private void loadMedicalData() {
        antecedentsArea.setText(nvl(currentUser.getAntecedents()));
        allergiesArea.setText(nvl(currentUser.getAllergies()));
        groupeSanguinField.setText(nvl(currentUser.getGroupeSanguin()));
        poidsField.setText(currentUser.getPoids() != null ? String.valueOf(currentUser.getPoids()) : "");
        tailleField.setText(currentUser.getTaille() != null ? String.valueOf(currentUser.getTaille()) : "");
        
        // Update display labels
        bloodGroupLabelDisplay.setText(currentUser.getGroupeSanguin() != null ? currentUser.getGroupeSanguin() : "--");
        poidsLabelDisplay.setText(currentUser.getPoids() != null ? String.valueOf(currentUser.getPoids()) : "0.0");
        tailleLabelDisplay.setText(currentUser.getTaille() != null ? String.valueOf(currentUser.getTaille()) : "0");
    }

    @FXML
    void handleSaveMedicalRecord(ActionEvent event) {
        clearAllErrors();
        try {
            String gs        = groupeSanguinField.getText().trim().toUpperCase();
            String poidsStr  = poidsField.getText().trim();
            String tailleStr = tailleField.getText().trim();
            boolean ok = true;

            if (!gs.isEmpty() && !gs.matches("^(A|B|AB|O)[+-]$")) {
                fieldErr(groupeSanguinField, errorGroupeSanguin, "Format invalide (ex: A+, AB-, O+).");
                ok = false;
            }
            if (!poidsStr.isEmpty()) {
                try {
                    double p = Double.parseDouble(poidsStr);
                    if (p <= 0 || p > 500) { fieldErr(poidsField, errorPoids, "Valeur entre 0 et 500 kg."); ok = false; }
                } catch (NumberFormatException ex) { fieldErr(poidsField, errorPoids, "Nombre invalide."); ok = false; }
            }
            if (!tailleStr.isEmpty()) {
                try {
                    double t = Double.parseDouble(tailleStr);
                    if (t <= 0 || t > 300) { fieldErr(tailleField, errorTaille, "Valeur entre 0 et 300 cm."); ok = false; }
                } catch (NumberFormatException ex) { fieldErr(tailleField, errorTaille, "Nombre invalide."); ok = false; }
            }
            if (!ok) return;

            currentUser.setAntecedents(antecedentsArea.getText().trim());
            currentUser.setAllergies(allergiesArea.getText().trim());
            currentUser.setGroupeSanguin(gs.isEmpty() ? null : gs);

            if (!poidsStr.isEmpty()) {
                currentUser.setPoids(Double.parseDouble(poidsStr));
            } else {
                currentUser.setPoids(null);
            }

            if (!tailleStr.isEmpty()) {
                currentUser.setTaille(Double.parseDouble(tailleStr));
            } else {
                currentUser.setTaille(null);
            }

            serviceUtilisateur.updateMedicalRecord(currentUser);
            loadMedicalData();
            showSuccess("Dossier médical mis à jour avec succès !");
        } catch (NumberFormatException e) {
            // already validated above, should not reach here
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void fieldErr(javafx.scene.control.Control field, Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
        field.getStyleClass().add("field-input-error");
    }

    private void clearErr(javafx.scene.control.Control field, Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false);
        field.getStyleClass().remove("field-input-error");
    }

    private void clearAllErrors() {
        clearErr(groupeSanguinField, errorGroupeSanguin);
        clearErr(poidsField,         errorPoids);
        clearErr(tailleField,        errorTaille);
        successLabel.setVisible(false);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setStyle("-fx-text-fill: #38a169;");
        successLabel.setVisible(true);
    }

    private void showError(String message) {
        successLabel.setText(message);
        successLabel.setStyle("-fx-text-fill: #e53e3e;");
        successLabel.setVisible(true);
    }

    @FXML
    void handleArticles(ActionEvent event) {
        App.navigate("PatientArticleList");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}
