package com.oncoreminder.controllers;

import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class PatientDashboardController {

    @FXML private Label patientNameLabel;
    @FXML private TextArea antecedentsArea;
    @FXML private TextArea allergiesArea;
    @FXML private TextField groupeSanguinField;
    @FXML private TextField poidsField;
    @FXML private TextField tailleField;
    @FXML private Label successLabel;
    
    // New display labels for modern UI
    @FXML private Label bloodGroupLabelDisplay;
    @FXML private Label poidsLabelDisplay;
    @FXML private Label tailleLabelDisplay;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();
    private Utilisateur currentUser;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            patientNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            loadMedicalData();
        }
        successLabel.setVisible(false);
    }

    private void loadMedicalData() {
        antecedentsArea.setText(currentUser.getAntecedents());
        allergiesArea.setText(currentUser.getAllergies());
        groupeSanguinField.setText(currentUser.getGroupeSanguin());
        poidsField.setText(currentUser.getPoids() != null ? String.valueOf(currentUser.getPoids()) : "");
        tailleField.setText(currentUser.getTaille() != null ? String.valueOf(currentUser.getTaille()) : "");
        
        // Update display labels
        bloodGroupLabelDisplay.setText(currentUser.getGroupeSanguin() != null ? currentUser.getGroupeSanguin() : "--");
        poidsLabelDisplay.setText(currentUser.getPoids() != null ? String.valueOf(currentUser.getPoids()) : "0.0");
        tailleLabelDisplay.setText(currentUser.getTaille() != null ? String.valueOf(currentUser.getTaille()) : "0");
    }

    @FXML
    void handleSaveMedicalRecord(ActionEvent event) {
        try {
            String gs = groupeSanguinField.getText().trim().toUpperCase();
            String poidsStr = poidsField.getText().trim();
            String tailleStr = tailleField.getText().trim();

            if (!gs.isEmpty() && !gs.matches("^(A|B|AB|O)[+-]$")) {
                showError("Groupe sanguin invalide (ex: A+, AB-, O+).");
                return;
            }

            currentUser.setAntecedents(antecedentsArea.getText().trim());
            currentUser.setAllergies(allergiesArea.getText().trim());
            currentUser.setGroupeSanguin(gs);
            
            if (!poidsStr.isEmpty()) {
                double p = Double.parseDouble(poidsStr);
                if (p <= 0 || p > 500) {
                    showError("Poids invalide.");
                    return;
                }
                currentUser.setPoids(p);
            } else {
                currentUser.setPoids(null);
            }

            if (!tailleStr.isEmpty()) {
                double t = Double.parseDouble(tailleStr);
                if (t <= 0 || t > 300) {
                    showError("Taille invalide.");
                    return;
                }
                currentUser.setTaille(t);
            } else {
                currentUser.setTaille(null);
            }

            serviceUtilisateur.updateMedicalRecord(currentUser);
            loadMedicalData(); // Refresh labels
            showSuccess("Dossier médical mis à jour avec succès !");
        } catch (NumberFormatException e) {
            showError("Le poids et la taille doivent être des nombres.");
        }
    }

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
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) patientNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
