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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    // ── Nouveaux labels sidebar / topbar ─────────────────────────────
    @FXML private Label adminInitialeLabel;
    @FXML private Label medecinCountLabel;
    @FXML private Label patientCountLabel;
    @FXML private Label dateTimeLabel;
    @FXML private Label userCountLabel;
    @FXML private Label logCountLabel;

    // ── Navigation sidebar ───────────────────────────────────────────
    @FXML private TabPane adminTabPane;
    @FXML private Button  btnNavDashboard;
    @FXML private Button  btnNavUsers;
    @FXML private Button  btnNavReclamations;
    @FXML private Button  btnNavLogs;

    // ── Filtre rôle ──────────────────────────────────────────────────
    @FXML private Button btnRoleTous;
    @FXML private Button btnRoleAdmin;
    @FXML private Button btnRoleMedecin;
    @FXML private Button btnRolePatient;
    private String currentRoleFilter = null;

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

    private static final String[] AVATAR_COLORS = {
        "#5B35A5","#2BBCB0","#F97316","#0EA5E9","#10B981","#8B5CF6","#EC4899","#FB7185"
    };

    // ── Initialisation ────────────────────────────────────────────────

    @FXML
    public void initialize() {
        Utilisateur admin = UserSession.getInstance().getCurrentUser();
        if (admin != null) {
            String fullName = admin.getNom() + " " + admin.getPrenom();
            adminNameLabel.setText(fullName);
            if (adminInitialeLabel != null)
                adminInitialeLabel.setText(String.valueOf(admin.getNom().charAt(0)).toUpperCase());
        }

        // Date/heure actuelle dans la topbar
        if (dateTimeLabel != null) {
            String now = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",
                java.util.Locale.FRENCH));
            String cap = now.substring(0, 1).toUpperCase() + now.substring(1);
            dateTimeLabel.setText("📅 " + cap);
        }

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
        switchTab(0, btnNavDashboard);
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
        applyRoleFilter();
        renderLogs();
        totalUsersLabel.setText(String.valueOf(allUsers.size()));
        if (medecinCountLabel != null)
            medecinCountLabel.setText(String.valueOf(
                allUsers.stream().filter(u -> "MEDECIN".equals(u.getRole())).count()));
        if (patientCountLabel != null)
            patientCountLabel.setText(String.valueOf(
                allUsers.stream().filter(u -> "PATIENT".equals(u.getRole())).count()));
    }

    // ── Navigation sidebar ────────────────────────────────────────────

    @FXML void handleNavDashboard(ActionEvent e)   { switchTab(0, btnNavDashboard); }
    @FXML void handleNavUsers(ActionEvent e)        { switchTab(0, btnNavUsers); }
    @FXML void handleNavReclamations(ActionEvent e) { switchTab(1, btnNavReclamations); }
    @FXML void handleNavLogs(ActionEvent e)         { switchTab(2, btnNavLogs); }

    private void switchTab(int index, Button activeBtn) {
        if (adminTabPane != null) adminTabPane.getSelectionModel().select(index);
        if (btnNavDashboard == null) return;
        String active = "sidebar-nav-btn-doctor-active";
        String normal = "sidebar-nav-btn";
        btnNavDashboard.getStyleClass().setAll(activeBtn == btnNavDashboard ? active : normal);
        btnNavUsers.getStyleClass().setAll(activeBtn == btnNavUsers ? active : normal);
        btnNavReclamations.getStyleClass().setAll(activeBtn == btnNavReclamations ? active : normal);
        btnNavLogs.getStyleClass().setAll(activeBtn == btnNavLogs ? active : normal);
    }

    // ── Filtre rôle ───────────────────────────────────────────────────

    @FXML void filterRoleAll(ActionEvent e)     { currentRoleFilter = null;      updateRoleButtons(); applyRoleFilter(); }
    @FXML void filterRoleAdmin(ActionEvent e)   { currentRoleFilter = "ADMIN";   updateRoleButtons(); applyRoleFilter(); }
    @FXML void filterRoleMedecin(ActionEvent e) { currentRoleFilter = "MEDECIN"; updateRoleButtons(); applyRoleFilter(); }
    @FXML void filterRolePatient(ActionEvent e) { currentRoleFilter = "PATIENT"; updateRoleButtons(); applyRoleFilter(); }

    private void applyRoleFilter() {
        String q = searchField != null ? searchField.getText() : "";
        List<Utilisateur> filtered = allUsers.stream()
            .filter(u -> currentRoleFilter == null || currentRoleFilter.equals(u.getRole()))
            .filter(u -> q == null || q.isEmpty() ||
                (u.getNom() + " " + u.getPrenom() + " " + u.getEmail()).toLowerCase().contains(q.toLowerCase()))
            .collect(Collectors.toList());
        renderUserCards(filtered);
    }

    private void updateRoleButtons() {
        if (btnRoleTous == null) return;
        String activeStyle   = "-fx-background-color: #5B35A5; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String adminStyle    = "ADMIN".equals(currentRoleFilter)   ? activeStyle : "-fx-background-color: #EDE8FF; -fx-text-fill: #5B35A5; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String medecinStyle  = "MEDECIN".equals(currentRoleFilter) ? activeStyle : "-fx-background-color: #EBF8FF; -fx-text-fill: #2B6CB0; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String patientStyle  = "PATIENT".equals(currentRoleFilter) ? activeStyle : "-fx-background-color: #FFF8E7; -fx-text-fill: #D97706; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String tousStyle     = currentRoleFilter == null           ? activeStyle : "-fx-background-color: #EDE8FF; -fx-text-fill: #5B35A5; -fx-padding: 4 12; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        btnRoleTous.setStyle(tousStyle);
        btnRoleAdmin.setStyle(adminStyle);
        btnRoleMedecin.setStyle(medecinStyle);
        btnRolePatient.setStyle(patientStyle);
    }

    // ── Rendu des cartes utilisateur ──────────────────────────────────

    private void renderUserCards(List<Utilisateur> users) {
        userFlowPane.getChildren().clear();
        if (userCountLabel != null)
            userCountLabel.setText(users.size() + " utilisateur" + (users.size() > 1 ? "s" : ""));
        if (users.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(30));
            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size: 36px;");
            Label msg = new Label("Aucun utilisateur trouvé");
            msg.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-font-weight: bold;");
            emptyBox.getChildren().addAll(icon, msg);
            userFlowPane.getChildren().add(emptyBox);
            return;
        }
        for (Utilisateur user : users) {
            userFlowPane.getChildren().add(createUserCard(user));
        }
    }

    private VBox createUserCard(Utilisateur user) {
        VBox card = new VBox(10);
        card.setPrefWidth(210);
        card.setPadding(new Insets(0, 0, 14, 0));
        card.setAlignment(Pos.TOP_CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 14;" +
            "-fx-border-color: #EDE9F8; -fx-border-radius: 14; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.08), 8, 0, 0, 2);"
        );

        // Bandeau coloré en haut selon le rôle
        String stripeColor = "ADMIN".equals(user.getRole()) ? "#5B35A5"
                           : "MEDECIN".equals(user.getRole()) ? "#2BBCB0" : "#F6AD55";
        HBox stripe = new HBox();
        stripe.setPrefHeight(5);
        stripe.setStyle("-fx-background-color: " + stripeColor + "; -fx-background-radius: 14 14 0 0;");
        card.getChildren().add(stripe);

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(10, 14, 0, 14));
        card.getChildren().add(content);

        // Avatar avec initiales
        String name = (user.getPrenom() + " " + user.getNom()).trim();
        String initiale = name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();
        String avatarColor = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];

        StackPane avatarPane = new StackPane();
        Circle bg = new Circle(28);
        bg.setStyle("-fx-fill: " + avatarColor + "30;");
        Circle fg = new Circle(22);
        fg.setStyle("-fx-fill: " + avatarColor + ";");
        Label initialeLabel = new Label(initiale);
        initialeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        avatarPane.getChildren().addAll(bg, fg, initialeLabel);
        content.getChildren().add(avatarPane);

        Label nameLabel = new Label(user.getPrenom() + " " + user.getNom());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2D1B69;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(190);
        nameLabel.setAlignment(Pos.CENTER);

        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        emailLabel.setWrapText(true);
        emailLabel.setMaxWidth(190);
        emailLabel.setAlignment(Pos.CENTER);

        String roleText = "ADMIN".equals(user.getRole()) ? "🔐 Admin"
                        : "MEDECIN".equals(user.getRole()) ? "🩺 Médecin" : "👤 Patient";
        String roleBg   = "ADMIN".equals(user.getRole()) ? "#EDE8FF"
                        : "MEDECIN".equals(user.getRole()) ? "#E6FFFA" : "#FFF8E7";
        String roleColor = "ADMIN".equals(user.getRole()) ? "#5B35A5"
                         : "MEDECIN".equals(user.getRole()) ? "#0D9488" : "#D97706";
        Label roleBadge = new Label(roleText);
        roleBadge.setStyle(
            "-fx-background-color: " + roleBg + "; -fx-text-fill: " + roleColor + ";" +
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 10;" +
            "-fx-background-radius: 20;"
        );

        content.getChildren().addAll(nameLabel, emailLabel, roleBadge);

        // Hover
        String baseStyle =
            "-fx-background-color: white; -fx-background-radius: 14;" +
            "-fx-border-color: #EDE9F8; -fx-border-radius: 14; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.08), 8, 0, 0, 2);";
        String hoverStyle =
            "-fx-background-color: white; -fx-background-radius: 14;" +
            "-fx-border-color: #5B35A5; -fx-border-radius: 14; -fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.20), 12, 0, 0, 4);";
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));

        card.setOnMouseClicked(e -> {
            userFlowPane.getChildren().forEach(n -> {
                if (n instanceof VBox c) c.setStyle(baseStyle);
            });
            card.setStyle(hoverStyle);
            selectUser(user);
        });
        card.setOnMouseExited(e -> {
            if (selectedUser == null || selectedUser.getId() != user.getId())
                card.setStyle(baseStyle);
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
        applyRoleFilter();
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (logCountLabel != null)
            logCountLabel.setText(logs.size() + " entrée" + (logs.size() > 1 ? "s" : ""));

        if (logs.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(30));
            Label icon = new Label("📋");
            icon.setStyle("-fx-font-size: 36px;");
            Label msg = new Label("Aucune connexion enregistrée");
            msg.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
            empty.getChildren().addAll(icon, msg);
            logContainer.getChildren().add(empty);
            return;
        }

        for (LogConnexion log : logs) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;" +
                "-fx-border-color: #EDE9F8; -fx-border-radius: 10; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.04), 4, 0, 0, 1);"
            );

            // Accent stripe gauche
            VBox stripe = new VBox();
            stripe.setMinWidth(4); stripe.setPrefWidth(4);
            stripe.setStyle("-fx-background-color: #5B35A5; -fx-background-radius: 10 0 0 10;");

            HBox inner = new HBox(14);
            inner.setAlignment(Pos.CENTER_LEFT);
            inner.setPadding(new Insets(10, 16, 10, 14));
            HBox.setHgrow(inner, Priority.ALWAYS);

            // Avatar initiale email
            String email = log.getUserEmail();
            String init = email.isEmpty() ? "?" : String.valueOf(email.charAt(0)).toUpperCase();
            Label avatar = new Label(init);
            avatar.setMinSize(32, 32); avatar.setMaxSize(32, 32);
            avatar.setAlignment(Pos.CENTER);
            avatar.setStyle(
                "-fx-background-color: #EDE8FF; -fx-text-fill: #5B35A5;" +
                "-fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 20;"
            );

            VBox meta = new VBox(2);
            HBox.setHgrow(meta, Priority.ALWAYS);
            Label emailL = new Label(email);
            emailL.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2D1B69;");

            LocalDateTime dt = log.getDateConnexion();
            String relativeTime = formatRelativeDateTime(dt);
            Label dateL = new Label("🔑 Connexion · " + relativeTime + "  (" + dt.format(fmt) + ")");
            dateL.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");

            meta.getChildren().addAll(emailL, dateL);
            inner.getChildren().addAll(avatar, meta);
            row.getChildren().addAll(stripe, inner);
            logContainer.getChildren().add(row);
        }
    }

    private String formatRelativeDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        long minutes = ChronoUnit.MINUTES.between(dt, LocalDateTime.now());
        if (minutes < 1)   return "À l'instant";
        if (minutes < 60)  return "Il y a " + minutes + " min";
        long hours = minutes / 60;
        if (hours < 24)    return "Il y a " + hours + " h";
        long days = hours / 24;
        if (days == 1)     return "Hier";
        if (days < 7)      return "Il y a " + days + " j";
        return "Il y a " + (days / 7) + " sem.";
    }

    // ── Filtrage utilisateurs ─────────────────────────────────────────

    private void filterUsers(String query) {
        applyRoleFilter();
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
        applyRoleFilter();
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
