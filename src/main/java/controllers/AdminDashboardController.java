package controllers;

import app.App;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Utilisateur;
import services.ServiceUtilisateur;

import java.io.IOException;
import java.util.List;

public class AdminDashboardController {

    @FXML
    private TableView<Utilisateur> userTable;
    @FXML
    private TableColumn<Utilisateur, Integer> colId;
    @FXML
    private TableColumn<Utilisateur, String> colNom;
    @FXML
    private TableColumn<Utilisateur, String> colPrenom;
    @FXML
    private TableColumn<Utilisateur, String> colEmail;
    @FXML
    private TableColumn<Utilisateur, String> colRole;
    @FXML
    private TextField searchField;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();
    private ObservableList<Utilisateur> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        loadUsers();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterTable(newValue);
        });
    }

    private void loadUsers() {
        List<Utilisateur> users = serviceUtilisateur.getAll();
        userList.setAll(users);
        userTable.setItems(userList);
    }

    @FXML
    void refreshTable(ActionEvent event) {
        loadUsers();
    }

    @FXML
    void handleDelete(ActionEvent event) {
        Utilisateur selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            serviceUtilisateur.delete(selected);
            loadUsers();
        }
    }

    @FXML
    void handleRoleToggle(ActionEvent event) {
        Utilisateur selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String newRole = selected.getRole().equals("ROLE_ADMIN") ? "ROLE_USER" : "ROLE_ADMIN";
            selected.setRole(newRole);
            serviceUtilisateur.update(selected);
            loadUsers();
        }
    }

    private void filterTable(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            userTable.setItems(userList);
            return;
        }

        ObservableList<Utilisateur> filteredData = FXCollections.observableArrayList();
        for (Utilisateur user : userList) {
            if (user.getNom().toLowerCase().contains(keyword.toLowerCase()) || 
                user.getEmail().toLowerCase().contains(keyword.toLowerCase())) {
                filteredData.add(user);
            }
        }
        userTable.setItems(filteredData);
    }

    @FXML
    void logout(ActionEvent event) {
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
