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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class DoctorDashboardController {

    @FXML private Label doctorNameLabel;
    @FXML private FlowPane patientFlowPane;

    @FXML private Label selectedPatientLabel;
    @FXML private Label bloodGroupLabel;
    @FXML private Label weightLabel;
    @FXML private Label heightLabel;
    @FXML private TextArea antecedentsArea;
    @FXML private TextArea allergiesArea;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        if (UserSession.getInstance().getCurrentUser() != null) {
            doctorNameLabel.setText("Dr. " + UserSession.getInstance().getCurrentUser().getNom());
        }

        loadPatients();
    }

    private void loadPatients() {
        List<Utilisateur> patients = serviceUtilisateur.getAll().stream()
                .filter(u -> "PATIENT".equals(u.getRole()))
                .collect(Collectors.toList());
        
        patientFlowPane.getChildren().clear();
        for (Utilisateur patient : patients) {
            patientFlowPane.getChildren().add(createPatientCard(patient));
        }
    }

    private VBox createPatientCard(Utilisateur patient) {
        VBox card = new VBox(10);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(180);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);

        Label iconLabel = new Label("👤");
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label nameLabel = new Label(patient.getPrenom() + " " + patient.getNom());
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        Label bgLabel = new Label(patient.getGroupeSanguin() != null ? patient.getGroupeSanguin() : "N/A");
        bgLabel.getStyleClass().add("stat-badge-info");

        card.getChildren().addAll(iconLabel, nameLabel, bgLabel);

        card.setOnMouseClicked(e -> {
            showPatientDetails(patient);
            patientFlowPane.getChildren().forEach(n -> n.setStyle(""));
            card.setStyle("-fx-border-color: #2BBCB0; -fx-border-width: 2px; -fx-border-radius: 12px;");
        });

        return card;
    }

    private void showPatientDetails(Utilisateur patient) {
        selectedPatientLabel.setText(patient.getPrenom() + " " + patient.getNom());
        bloodGroupLabel.setText(patient.getGroupeSanguin() != null ? patient.getGroupeSanguin() : "N/A");
        weightLabel.setText(patient.getPoids() != null ? patient.getPoids() + " kg" : "N/A");
        heightLabel.setText(patient.getTaille() != null ? patient.getTaille() + " cm" : "N/A");
        antecedentsArea.setText(patient.getAntecedents() != null ? patient.getAntecedents() : "Aucun antécédent renseigné.");
        allergiesArea.setText(patient.getAllergies() != null ? patient.getAllergies() : "Aucune allergie renseignée.");
    }

    @FXML
    void handleArticles(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/ArticleList.fxml"));
            Stage stage = (Stage) doctorNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) doctorNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
