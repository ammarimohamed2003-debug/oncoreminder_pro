package com.oncoreminder.controllers;

import com.oncoreminder.models.LogConnexion;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.collections.FXCollections;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboardController {

    @FXML private FlowPane userFlowPane;
    @FXML private VBox logContainer;
    @FXML private TextField searchField;
    
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleCombo;

    @FXML private Label adminNameLabel;
    @FXML private Label totalUsersLabel;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();
    private List<Utilisateur> allUsers;
    private Utilisateur selectedUser;

    @FXML
    public void initialize() {
        if (UserSession.getInstance().getCurrentUser() != null) {
            adminNameLabel.setText(UserSession.getInstance().getCurrentUser().getNom());
        }

        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "MEDECIN", "PATIENT"));
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterUsers(newVal);
        });

        loadData();
    }

    private void loadData() {
        allUsers = serviceUtilisateur.getAll();
        renderUserCards(allUsers);
        renderLogs();
        totalUsersLabel.setText(String.valueOf(allUsers.size()));
    }

    private void renderUserCards(List<Utilisateur> users) {
        userFlowPane.getChildren().clear();
        for (Utilisateur user : users) {
            VBox card = createUserCard(user);
            userFlowPane.getChildren().add(card);
        }
    }

    private VBox createUserCard(Utilisateur user) {
        VBox card = new VBox(10);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(220);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);

        Label iconLabel = new Label(user.getRole().equals("ADMIN") ? "🔐" : (user.getRole().equals("MEDECIN") ? "👨‍⚕️" : "👤"));
        iconLabel.setStyle("-fx-font-size: 30px;");

        Label nameLabel = new Label(user.getPrenom() + " " + user.getNom());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");

        Label roleBadge = new Label(user.getRole());
        roleBadge.getStyleClass().add(user.getRole().equals("ADMIN") ? "stat-badge-success" : "stat-badge-info");

        card.getChildren().addAll(iconLabel, nameLabel, emailLabel, roleBadge);

        card.setOnMouseClicked(e -> {
            selectUser(user);
            // Highlight selected card (CSS effect)
            userFlowPane.getChildren().forEach(n -> n.setStyle(""));
            card.setStyle("-fx-border-color: #5B35A5; -fx-border-width: 2px; -fx-border-radius: 12px;");
        });

        return card;
    }

    private void selectUser(Utilisateur user) {
        this.selectedUser = user;
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        roleCombo.setValue(user.getRole());
    }

    private void renderLogs() {
        logContainer.getChildren().clear();
        List<LogConnexion> logs = serviceUtilisateur.getAllLogs();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (LogConnexion log : logs) {
            HBox logRow = new HBox(20);
            logRow.getStyleClass().add("form-panel");
            logRow.setPadding(new Insets(10, 20, 10, 20));
            logRow.setAlignment(Pos.CENTER_LEFT);

            Label email = new Label(log.getUserEmail());
            email.setPrefWidth(250);
            email.setStyle("-fx-font-weight: bold;");

            Label date = new Label("s'est connecté le " + log.getDateConnexion().format(formatter));
            
            logRow.getChildren().addAll(email, date);
            logContainer.getChildren().add(logRow);
        }
    }

    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            renderUserCards(allUsers);
        } else {
            List<Utilisateur> filtered = allUsers.stream()
                .filter(u -> (u.getNom() + " " + u.getPrenom() + " " + u.getEmail()).toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
            renderUserCards(filtered);
        }
    }

    @FXML
    void handleAddUser(ActionEvent event) {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String role = roleCombo.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || role == null) {
            showAlert("Erreur", "Tous les champs sont obligatoires.", Alert.AlertType.ERROR);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Erreur", "Format d'email invalide.", Alert.AlertType.ERROR);
            return;
        }

        if (serviceUtilisateur.emailExists(email)) {
            showAlert("Erreur", "Cet email est déjà utilisé.", Alert.AlertType.ERROR);
            return;
        }

        Utilisateur user = new Utilisateur(nom, prenom, email, "123456", role);
        serviceUtilisateur.add(user);
        loadData();
        clearFields();
        showAlert("Succès", "Utilisateur ajouté avec succès.", Alert.AlertType.INFORMATION);
    }

    @FXML
    void handleUpdateUser(ActionEvent event) {
        if (selectedUser != null) {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String role = roleCombo.getValue();

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || role == null) {
                showAlert("Erreur", "Tous les champs sont obligatoires.", Alert.AlertType.ERROR);
                return;
            }

            selectedUser.setNom(nom);
            selectedUser.setPrenom(prenom);
            selectedUser.setEmail(email);
            selectedUser.setRole(role);
            serviceUtilisateur.update(selectedUser);
            loadData();
            showAlert("Succès", "Utilisateur mis à jour.", Alert.AlertType.INFORMATION);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void handleDeleteUser(ActionEvent event) {
        if (selectedUser != null) {
            serviceUtilisateur.delete(selectedUser.getId());
            loadData();
            clearFields();
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) userFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        roleCombo.setValue(null);
        selectedUser = null;
    }
}
