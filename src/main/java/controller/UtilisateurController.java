package controller;

import entity.Role;
import entity.Utilisateur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import service.UtilisateurService;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Controller class for the Utilisateur management interface.
 */
public class UtilisateurController implements Initializable {

    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ComboBox<Role> comboRole;
    @FXML
    private TableView<Utilisateur> tableUtilisateurs;
    @FXML
    private TableColumn<Utilisateur, Integer> colId;
    @FXML
    private TableColumn<Utilisateur, String> colUsername;
    @FXML
    private TableColumn<Utilisateur, String> colEmail;
    @FXML
    private TableColumn<Utilisateur, String> colRole;
    @FXML
    private TableColumn<Utilisateur, String> colDate;

    private UtilisateurService service = new UtilisateurService();
    private ObservableList<Utilisateur> userList = FXCollections.observableArrayList();
    private Utilisateur selectedUser = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadUsers();
        loadRoles();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_creation"));
    }

    private void loadUsers() {
        userList.setAll(service.getAllUtilisateurs());
        tableUtilisateurs.setItems(userList);
    }

    private void loadRoles() {
        comboRole.setItems(FXCollections.observableArrayList(service.getAllRoles()));
    }

    @FXML
    private void handleAdd(ActionEvent event) {
        if (validateInput()) {
            Utilisateur u = new Utilisateur();
            u.setUsername(txtUsername.getText());
            u.setEmail(txtEmail.getText());
            u.setPassword(txtPassword.getText());
            u.setRole_id(comboRole.getValue().getId());
            
            service.addUtilisateur(u);
            loadUsers();
            handleClear(null);
            showAlert("Succès", "Utilisateur ajouté avec succès !");
        }
    }

    @FXML
    private void handleUpdate(ActionEvent event) {
        if (selectedUser == null) {
            showAlert("Erreur", "Veuillez sélectionner un utilisateur dans le tableau.");
            return;
        }
        if (validateInput()) {
            selectedUser.setUsername(txtUsername.getText());
            selectedUser.setEmail(txtEmail.getText());
            // If password field is empty, we might not want to update it. 
            // For this implementation, we assume if it's provided, it's the new password.
            if (!txtPassword.getText().isEmpty()) {
                selectedUser.setPassword(txtPassword.getText());
            }
            selectedUser.setRole_id(comboRole.getValue().getId());

            service.updateUtilisateur(selectedUser);
            loadUsers();
            handleClear(null);
            showAlert("Succès", "Utilisateur modifié avec succès !");
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (selectedUser == null) {
            showAlert("Erreur", "Veuillez sélectionner un utilisateur.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cet utilisateur ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        
        if (confirm.getResult() == ButtonType.YES) {
            service.deleteUtilisateur(selectedUser.getId());
            loadUsers();
            handleClear(null);
            showAlert("Succès", "Utilisateur supprimé !");
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        txtUsername.clear();
        txtEmail.clear();
        txtPassword.clear();
        comboRole.getSelectionModel().clearSelection();
        selectedUser = null;
    }

    @FXML
    private void handleTableSelection(MouseEvent event) {
        selectedUser = tableUtilisateurs.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            txtUsername.setText(selectedUser.getUsername());
            txtEmail.setText(selectedUser.getEmail());
            txtPassword.setText(""); // Don't show hashed password
            
            // Match role in combo
            for (Role r : comboRole.getItems()) {
                if (r.getId() == selectedUser.getRole_id()) {
                    comboRole.setValue(r);
                    break;
                }
            }
        }
    }

    private boolean validateInput() {
        if (txtUsername.getText().isEmpty() || txtEmail.getText().isEmpty() || (selectedUser == null && txtPassword.getText().isEmpty()) || comboRole.getValue() == null) {
            showAlert("Erreur", "Tous les champs sont obligatoires.");
            return false;
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!Pattern.compile(emailRegex).matcher(txtEmail.getText()).matches()) {
            showAlert("Erreur", "Format d'email invalide.");
            return false;
        }
        
        return true;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
