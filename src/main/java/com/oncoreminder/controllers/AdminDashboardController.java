package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.LogConnexion;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminDashboardController — Gestion des utilisateurs par l'admin.
 *
 * Fonctionnalités :
 *  ✔ Ajout utilisateur avec mot de passe BCrypt (obligatoire)
 *  ✔ Modification — mot de passe optionnel (vide = conserver l'ancien)
 *  ✔ Validation : longueur min 8, mots de passe identiques
 *  ✔ Toggle afficher/masquer mot de passe
 *  ✔ Feedback succès/erreur inline (sans Alert bloquante)
 *  ✔ Suppression avec confirmation
 *  ✔ Recherche en temps réel
 *  ✔ Réinitialisation auto après ajout
 */
public class AdminDashboardController {

    // ── Liste + recherche ────────────────────────────────────────────
    @FXML private FlowPane  userFlowPane;
    @FXML private VBox      logContainer;
    @FXML private TextField searchField;
    @FXML private Label     totalUsersLabel;
    @FXML private Label     adminNameLabel;

    // ── Formulaire ───────────────────────────────────────────────────
    @FXML private Label     formModeLabel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleCombo;

    // ── Champs mot de passe ──────────────────────────────────────────
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private PasswordField confirmField;
    @FXML private TextField     confirmVisible;
    @FXML private CheckBox      showPwdCheck;

    // ── Labels contextuels ────────────────────────────────────────────
    @FXML private Label pwdLabel;
    @FXML private Label confirmLabel;
    @FXML private Label passwordHintLabel;

    // ── Labels d'erreur ───────────────────────────────────────────────
    @FXML private Label errorNom;
    @FXML private Label errorPrenom;
    @FXML private Label errorEmail;
    @FXML private Label errorPassword;
    @FXML private Label errorConfirm;
    @FXML private Label feedbackLabel;

    // ── État ─────────────────────────────────────────────────────────
    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();
    private List<Utilisateur> allUsers;
    private Utilisateur selectedUser;

    private static final String EMAIL_REGEX =
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    // ── Initialisation ────────────────────────────────────────────────

    @FXML
    public void initialize() {
        Utilisateur admin = UserSession.getInstance().getCurrentUser();
        if (admin != null) adminNameLabel.setText(admin.getNom() + " " + admin.getPrenom());

        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "MEDECIN", "PATIENT"));

        // Sync PasswordField ↔ TextField (show/hide)
        passwordField.textProperty().bindBidirectional(passwordVisible.textProperty());
        confirmField.textProperty().bindBidirectional(confirmVisible.textProperty());

        // Nettoyage erreurs en temps réel
        passwordField.textProperty().addListener((obs, o, n) -> {
            clearFieldError(passwordField, errorPassword);
            // Validation live confirmation
            if (!confirmField.getText().isEmpty()) validateConfirmLive();
        });
        confirmField.textProperty().addListener((obs, o, n) -> validateConfirmLive());
        nomField.textProperty().addListener((obs, o, n)    -> clearFieldError(nomField,    errorNom));
        prenomField.textProperty().addListener((obs, o, n) -> clearFieldError(prenomField, errorPrenom));
        emailField.textProperty().addListener((obs, o, n)  -> clearFieldError(emailField,  errorEmail));

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, o, n) -> filterUsers(n));

        // Mode initial : ajout
        setAddMode();
        loadData();
    }

    // ── Toggle show/hide mot de passe ─────────────────────────────────

    @FXML
    void toggleShowPassword(ActionEvent event) {
        boolean show = showPwdCheck.isSelected();

        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        passwordVisible.setVisible(show);
        passwordVisible.setManaged(show);

        confirmField.setVisible(!show);
        confirmField.setManaged(!show);
        confirmVisible.setVisible(show);
        confirmVisible.setManaged(show);
    }

    // ── Chargement ───────────────────────────────────────────────────

    private void loadData() {
        allUsers = serviceUtilisateur.getAll();
        renderUserCards(allUsers);
        renderLogs();
        totalUsersLabel.setText(String.valueOf(allUsers.size()));
    }

    // ── Rendu des cartes utilisateur ──────────────────────────────────

    private void renderUserCards(List<Utilisateur> users) {
        userFlowPane.getChildren().clear();
        if (users.isEmpty()) {
            Label empty = new Label("Aucun utilisateur trouvé");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 13px; -fx-font-style: italic;");
            userFlowPane.getChildren().add(empty);
            return;
        }
        for (Utilisateur user : users) {
            userFlowPane.getChildren().add(createUserCard(user));
        }
    }

    private VBox createUserCard(Utilisateur user) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(200);
        card.setPadding(new Insets(14));
        card.setAlignment(Pos.CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);

        // Icône rôle
        String icon = "ADMIN".equals(user.getRole()) ? "🔐"
                    : "MEDECIN".equals(user.getRole()) ? "👨‍⚕️" : "👤";
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");

        Label nameLabel = new Label(user.getPrenom() + " " + user.getNom());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2D3748;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(185);

        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #718096;");
        emailLabel.setWrapText(true);
        emailLabel.setMaxWidth(185);

        Label roleBadge = new Label(user.getRole());
        roleBadge.getStyleClass().add(
            "ADMIN".equals(user.getRole()) ? "stat-badge-success" : "stat-badge-info"
        );

        card.getChildren().addAll(iconLabel, nameLabel, emailLabel, roleBadge);

        card.setOnMouseClicked(e -> {
            // Désélectionner toutes les cartes
            userFlowPane.getChildren().forEach(n -> n.setStyle(""));
            card.setStyle("-fx-border-color: #5B35A5; -fx-border-width: 2; -fx-border-radius: 12;");
            selectUser(user);
        });

        return card;
    }

    /**
     * Sélectionner un utilisateur → préremplir le formulaire en mode Modification.
     * Les champs mot de passe restent vides (ne jamais afficher le mot de passe).
     */
    private void selectUser(Utilisateur user) {
        this.selectedUser = user;

        // Préremplir les champs identité
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        roleCombo.setValue(user.getRole());

        // Champs mot de passe TOUJOURS vides en modification
        passwordField.clear();
        confirmField.clear();

        // Passer en mode modification
        setEditMode();
        clearAllErrors();
        hideFeedback();
    }

    // ── Modes du formulaire ───────────────────────────────────────────

    private void setAddMode() {
        formModeLabel.setText("[Ajout]");
        formModeLabel.setStyle("-fx-text-fill: #2BBCB0; -fx-font-size: 11px; -fx-font-weight: bold;" +
                               "-fx-padding: 2 8 2 8; -fx-background-color: rgba(43,188,176,0.12);" +
                               "-fx-background-radius: 10;");
        pwdLabel.setText("Mot de passe *");
        confirmLabel.setText("Confirmer le mot de passe *");
        passwordHintLabel.setVisible(false);
        passwordHintLabel.setManaged(false);
    }

    private void setEditMode() {
        formModeLabel.setText("[Modification]");
        formModeLabel.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px; -fx-font-weight: bold;" +
                               "-fx-padding: 2 8 2 8; -fx-background-color: rgba(217,119,6,0.12);" +
                               "-fx-background-radius: 10;");
        pwdLabel.setText("Nouveau mot de passe (optionnel)");
        confirmLabel.setText("Confirmer le nouveau mot de passe");
        passwordHintLabel.setVisible(true);
        passwordHintLabel.setManaged(true);
    }

    // ── Validation live confirmation ──────────────────────────────────

    private void validateConfirmLive() {
        String pwd     = passwordField.getText();
        String confirm = confirmField.getText();

        if (confirm.isEmpty() || pwd.isEmpty()) {
            errorConfirm.setVisible(false);
            errorConfirm.setManaged(false);
            return;
        }
        if (!pwd.equals(confirm)) {
            showFieldError(confirmField, errorConfirm, "❌ Les mots de passe ne correspondent pas");
        } else {
            errorConfirm.setText("✅ Les mots de passe correspondent");
            errorConfirm.setStyle("-fx-text-fill: #2BBCB0; -fx-font-size: 11px;");
            errorConfirm.setVisible(true);
            errorConfirm.setManaged(true);
            confirmField.getStyleClass().remove("field-input-error");
        }
    }

    // ── Actions FXML ──────────────────────────────────────────────────

    /**
     * Ajouter un nouvel utilisateur.
     * Mot de passe obligatoire, hashé avec BCrypt avant sauvegarde.
     */
    @FXML
    void handleAddUser(ActionEvent event) {
        clearAllErrors();
        hideFeedback();

        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim().toLowerCase();
        String role    = roleCombo.getValue();
        String pwd     = passwordField.getText();
        String confirm = confirmField.getText();
        boolean ok     = true;

        // ── Validation identité ──────────────────────────────────────
        if (nom.isEmpty())    { showFieldError(nomField,    errorNom,    "Le nom est obligatoire.");    ok = false; }
        if (prenom.isEmpty()) { showFieldError(prenomField, errorPrenom, "Le prénom est obligatoire."); ok = false; }
        if (email.isEmpty())  { showFieldError(emailField,  errorEmail,  "L'email est obligatoire.");   ok = false; }
        else if (!email.matches(EMAIL_REGEX))
                              { showFieldError(emailField,  errorEmail,  "Format d'email invalide.");   ok = false; }
        else if (serviceUtilisateur.emailExists(email))
                              { showFieldError(emailField,  errorEmail,  "Cet email est déjà utilisé."); ok = false; }
        if (role == null)     { showFeedbackError("Veuillez sélectionner un rôle."); ok = false; }

        // ── Validation mot de passe (obligatoire pour l'ajout) ───────
        if (pwd.isEmpty())    { showFieldError(passwordField, errorPassword, "Le mot de passe est obligatoire."); ok = false; }
        else if (pwd.length() < 8)
                              { showFieldError(passwordField, errorPassword, "Minimum 8 caractères requis.");     ok = false; }

        if (ok && !pwd.equals(confirm))
                              { showFieldError(confirmField, errorConfirm, "Les mots de passe ne correspondent pas."); ok = false; }

        if (!ok) return;

        // ── Hachage BCrypt + ajout en BDD ────────────────────────────
        String hashedPwd = BCrypt.hashpw(pwd, BCrypt.gensalt());
        Utilisateur newUser = new Utilisateur(nom, prenom, email, hashedPwd, role);
        serviceUtilisateur.add(newUser);

        loadData();
        clearForm();
        setAddMode();
        showFeedbackSuccess("✅ Utilisateur ajouté avec succès !");
    }

    /**
     * Modifier un utilisateur sélectionné.
     * Mot de passe optionnel : s'il est vide, on conserve l'ancien.
     * S'il est renseigné, il doit être valide + haché BCrypt.
     */
    @FXML
    void handleUpdateUser(ActionEvent event) {
        if (selectedUser == null) {
            showFeedbackError("Sélectionnez d'abord un utilisateur dans la liste.");
            return;
        }
        clearAllErrors();
        hideFeedback();

        String nom     = nomField.getText().trim();
        String prenom  = prenomField.getText().trim();
        String email   = emailField.getText().trim().toLowerCase();
        String role    = roleCombo.getValue();
        String pwd     = passwordField.getText();
        String confirm = confirmField.getText();
        boolean ok     = true;

        // ── Validation identité ──────────────────────────────────────
        if (nom.isEmpty())    { showFieldError(nomField,    errorNom,    "Le nom est obligatoire.");    ok = false; }
        if (prenom.isEmpty()) { showFieldError(prenomField, errorPrenom, "Le prénom est obligatoire."); ok = false; }
        if (email.isEmpty())  { showFieldError(emailField,  errorEmail,  "L'email est obligatoire.");   ok = false; }
        else if (!email.matches(EMAIL_REGEX))
                              { showFieldError(emailField,  errorEmail,  "Format d'email invalide.");   ok = false; }
        else if (!email.equals(selectedUser.getEmail()) && serviceUtilisateur.emailExists(email))
                              { showFieldError(emailField,  errorEmail,  "Cet email est déjà utilisé."); ok = false; }
        if (role == null)     { showFeedbackError("Veuillez sélectionner un rôle."); ok = false; }

        // ── Validation mot de passe (optionnel en modification) ──────
        if (!pwd.isEmpty()) {
            if (pwd.length() < 8)
                { showFieldError(passwordField, errorPassword, "Minimum 8 caractères requis."); ok = false; }
            else if (!pwd.equals(confirm))
                { showFieldError(confirmField,  errorConfirm,  "Les mots de passe ne correspondent pas."); ok = false; }
        }

        if (!ok) return;

        // ── Mise à jour identité ─────────────────────────────────────
        selectedUser.setNom(nom);
        selectedUser.setPrenom(prenom);
        selectedUser.setEmail(email);
        selectedUser.setRole(role);

        // ── Nouveau mot de passe ? → Hachage BCrypt ─────────────────
        if (!pwd.isEmpty()) {
            String hashedPwd = BCrypt.hashpw(pwd, BCrypt.gensalt());
            selectedUser.setPassword(hashedPwd);
            serviceUtilisateur.updateWithPassword(selectedUser);
        } else {
            serviceUtilisateur.update(selectedUser);
        }

        loadData();
        showFeedbackSuccess("✅ Utilisateur modifié avec succès !");
    }

    /** Supprimer l'utilisateur sélectionné avec confirmation */
    @FXML
    void handleDeleteUser(ActionEvent event) {
        if (selectedUser == null) {
            showFeedbackError("Sélectionnez d'abord un utilisateur.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'utilisateur");
        confirm.setHeaderText("Confirmation de suppression");
        confirm.setContentText(
            "Utilisateur : " + selectedUser.getPrenom() + " " + selectedUser.getNom() + "\n" +
            "Email       : " + selectedUser.getEmail() + "\n\n" +
            "⚠️ Cette action est irréversible."
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                serviceUtilisateur.delete(selectedUser.getId());
                loadData();
                clearForm();
                setAddMode();
                showFeedbackSuccess("✅ Utilisateur supprimé.");
            }
        });
    }

    /** Effacer le formulaire et repasser en mode Ajout */
    @FXML
    void handleClearForm(ActionEvent event) {
        clearForm();
        setAddMode();
        hideFeedback();
        clearAllErrors();
        // Désélectionner les cartes
        userFlowPane.getChildren().forEach(n -> n.setStyle(""));
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }

    // ── Rendu des logs ────────────────────────────────────────────────

    private void renderLogs() {
        logContainer.getChildren().clear();
        List<LogConnexion> logs = serviceUtilisateur.getAllLogs();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        if (logs.isEmpty()) {
            Label empty = new Label("Aucune connexion enregistrée.");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 13px; -fx-font-style: italic; -fx-padding: 20;");
            logContainer.getChildren().add(empty);
            return;
        }

        for (LogConnexion log : logs) {
            HBox row = new HBox(20);
            row.getStyleClass().add("form-panel");
            row.setPadding(new Insets(10, 20, 10, 20));
            row.setAlignment(Pos.CENTER_LEFT);

            Label iconL  = new Label("🔑");
            iconL.setStyle("-fx-font-size: 14px;");

            Label emailL = new Label(log.getUserEmail());
            emailL.setPrefWidth(230);
            emailL.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            Label dateL  = new Label("s'est connecté le " + log.getDateConnexion().format(fmt));
            dateL.setStyle("-fx-text-fill: #718096; -fx-font-size: 11px;");

            row.getChildren().addAll(iconL, emailL, dateL);
            logContainer.getChildren().add(row);
        }
    }

    // ── Filtrage utilisateurs ─────────────────────────────────────────

    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            renderUserCards(allUsers);
        } else {
            String q = query.toLowerCase();
            List<Utilisateur> filtered = allUsers.stream()
                .filter(u -> (u.getNom() + " " + u.getPrenom() + " " + u.getEmail()).toLowerCase().contains(q))
                .collect(Collectors.toList());
            renderUserCards(filtered);
        }
    }

    // ── Helpers formulaire ────────────────────────────────────────────

    private void clearForm() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        roleCombo.setValue(null);
        passwordField.clear();
        confirmField.clear();
        showPwdCheck.setSelected(false);

        // Remettre en mode masqué
        passwordField.setVisible(true);   passwordField.setManaged(true);
        passwordVisible.setVisible(false); passwordVisible.setManaged(false);
        confirmField.setVisible(true);    confirmField.setManaged(true);
        confirmVisible.setVisible(false); confirmVisible.setManaged(false);

        selectedUser = null;
        userFlowPane.getChildren().forEach(n -> n.setStyle(""));
    }

    // ── Gestion des erreurs par champ ─────────────────────────────────

    private void showFieldError(Control field, Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px;");
        lbl.setVisible(true);
        lbl.setManaged(true);
        field.getStyleClass().remove("field-input-error");
        field.getStyleClass().add("field-input-error");
    }

    private void clearFieldError(Control field, Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
        field.getStyleClass().remove("field-input-error");
    }

    private void clearAllErrors() {
        clearFieldError(nomField,      errorNom);
        clearFieldError(prenomField,   errorPrenom);
        clearFieldError(emailField,    errorEmail);
        clearFieldError(passwordField, errorPassword);
        clearFieldError(confirmField,  errorConfirm);
    }

    // ── Feedback global (succès / erreur) ─────────────────────────────

    private void showFeedbackSuccess(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(
            "-fx-text-fill: #2BBCB0; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-padding: 8 12 8 12; -fx-background-color: #f0fffe;" +
            "-fx-background-radius: 6; -fx-border-color: #2BBCB0;" +
            "-fx-border-radius: 6; -fx-border-width: 1;"
        );
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(300), feedbackLabel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // Auto-masquer après 3 secondes
        PauseTransition hide = new PauseTransition(Duration.seconds(3));
        hide.setOnFinished(e -> hideFeedback());
        hide.play();
    }

    private void showFeedbackError(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(
            "-fx-text-fill: #E53E3E; -fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-padding: 8 12 8 12; -fx-background-color: #fff5f5;" +
            "-fx-background-radius: 6; -fx-border-color: #FC8181;" +
            "-fx-border-radius: 6; -fx-border-width: 1;"
        );
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private void hideFeedback() {
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
    }
}
