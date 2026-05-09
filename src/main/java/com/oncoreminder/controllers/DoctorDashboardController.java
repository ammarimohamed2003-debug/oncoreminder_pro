package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

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
        int medecinId = UserSession.getInstance().getCurrentUser().getId();
        List<Utilisateur> patients = serviceUtilisateur.getPatientsByMedecin(medecinId);

        patientFlowPane.getChildren().clear();
        if (patients.isEmpty()) {
            Label empty = new Label("Aucun patient assigné.\nCliquez sur « + Ajouter » pour en ajouter.");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 13px; -fx-padding: 20; -fx-text-alignment: center;");
            empty.setWrapText(true);
            patientFlowPane.getChildren().add(empty);
        } else {
            for (Utilisateur patient : patients) {
                patientFlowPane.getChildren().add(createPatientCard(patient));
            }
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

        Button removeBtn = new Button("Retirer");
        removeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #FC8181; -fx-cursor: hand;" +
            "-fx-font-size: 11px; -fx-padding: 2 0; -fx-border-width: 0;"
        );
        removeBtn.setOnAction(e -> {
            serviceUtilisateur.unassignMedecin(patient.getId());
            loadPatients();
        });

        card.getChildren().add(removeBtn);

        card.setOnMouseClicked(e -> {
            if (e.getTarget() == removeBtn || removeBtn.isPressed()) return;
            showPatientDetails(patient);
            patientFlowPane.getChildren().forEach(n -> n.setStyle(""));
            card.setStyle("-fx-border-color: #2BBCB0; -fx-border-width: 2px; -fx-border-radius: 12px;");
        });

        return card;
    }

    @FXML
    void handleAddPatient(ActionEvent event) {
        int medecinId = UserSession.getInstance().getCurrentUser().getId();
        List<Utilisateur> unassigned = serviceUtilisateur.getUnassignedPatients();

        Stage dialog = new Stage();
        dialog.setTitle("Ajouter un patient");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #F7F4FE;");

        Label title = new Label("Patients disponibles");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3A1D7A;");
        root.getChildren().add(title);

        VBox listBox = new VBox(8);

        if (unassigned.isEmpty()) {
            Label empty = new Label("Aucun patient disponible.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
            listBox.getChildren().add(empty);
        } else {
            for (Utilisateur patient : unassigned) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10;" +
                    "-fx-border-color: #EDE9F8; -fx-border-radius: 10; -fx-border-width: 1;"
                );

                Label icon = new Label("👤");
                icon.setStyle("-fx-font-size: 18px;");
                Label name = new Label(patient.getPrenom() + " " + patient.getNom());
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: #2D1B69; -fx-font-size: 13px;");
                name.setPrefWidth(180);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button addBtn = new Button("Ajouter →");
                addBtn.setStyle(
                    "-fx-background-color: #2BBCB0; -fx-text-fill: white; -fx-background-radius: 8;" +
                    "-fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 6 14; -fx-border-width: 0;"
                );
                addBtn.setOnAction(e -> {
                    serviceUtilisateur.assignMedecin(patient.getId(), medecinId);
                    listBox.getChildren().remove(row);
                    loadPatients();
                    if (listBox.getChildren().isEmpty()) dialog.close();
                });

                row.getChildren().addAll(icon, name, spacer, addBtn);
                listBox.getChildren().add(row);
            }
        }

        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setPrefHeight(340);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        root.getChildren().add(sp);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle(
            "-fx-background-color: #EDE9F8; -fx-text-fill: #5B35A5; -fx-background-radius: 8;" +
            "-fx-cursor: hand; -fx-padding: 8 20; -fx-border-width: 0;"
        );
        closeBtn.setOnAction(e -> dialog.close());
        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().add(footer);

        dialog.setScene(new Scene(root, 460, 480));
        dialog.showAndWait();
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
        App.navigate("ArticleList");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}
