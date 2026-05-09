package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

/**
 * PatientDashboardController — Espace Patient.
 *
 * Onglets :
 *  1. Dossier Médical (groupe sanguin, poids, taille, antécédents, allergies, traitements)
 *  2. Mon Profil (nom, prénom, email, téléphone, sexe, date naissance, adresse)
 *  3. Sécurité (changement de mot de passe sécurisé avec vérification BCrypt)
 */
public class PatientDashboardController {

    // ── Sidebar ───────────────────────────────────────────────────────
    @FXML private Label patientNameLabel;
    @FXML private Label medecinSidebarLabel;

    // ── Onglet 1 : Dossier médical ────────────────────────────────────
    @FXML private Label bloodGroupLabelDisplay;
    @FXML private Label poidsLabelDisplay;
    @FXML private Label tailleLabelDisplay;

    @FXML private ComboBox<String> groupeSanguinCombo;
    @FXML private TextField poidsField;
    @FXML private TextField tailleField;
    @FXML private TextArea  antecedentsArea;
    @FXML private TextArea  allergiesArea;
    @FXML private TextArea  traitementsArea;

    @FXML private Label errorGroupeSanguin;
    @FXML private Label errorPoids;
    @FXML private Label errorTaille;
    @FXML private Label medFeedbackLabel;
    @FXML private Button saveMedBtn;

    // ── Onglet 2 : Mon Profil ─────────────────────────────────────────
    @FXML private TextField        profileNomField;
    @FXML private TextField        profilePrenomField;
    @FXML private TextField        profileEmailField;
    @FXML private TextField        profileTelField;
    @FXML private ComboBox<String> profileSexeCombo;
    @FXML private TextField        profileDateNaissField;
    @FXML private TextField        profileAdresseField;

    @FXML private Label profileErrorNom;
    @FXML private Label profileErrorPrenom;
    @FXML private Label profileErrorEmail;
    @FXML private Label profileFeedbackLabel;
    @FXML private Button saveProfileBtn;

    // ── Onglet 3 : Sécurité ───────────────────────────────────────────
    @FXML private PasswordField oldPwdField;
    @FXML private TextField     oldPwdVisible;
    @FXML private PasswordField newPwdField;
    @FXML private TextField     newPwdVisible;
    @FXML private PasswordField confirmPwdField;
    @FXML private TextField     confirmPwdVisible;
    @FXML private CheckBox      showPwdCheck;

    @FXML private Label errorOldPwd;
    @FXML private Label errorNewPwd;
    @FXML private Label errorConfirmPwd;
    @FXML private Label pwdFeedbackLabel;
    @FXML private Button savePwdBtn;

    // ── Services ──────────────────────────────────────────────────────
    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();
    private Utilisateur currentUser;

    private static final String[] GROUPES_SANGUINS = {"A+","A-","B+","B-","AB+","AB-","O+","O-"};
    private static final String[] SEXES = {"Masculin", "Féminin", "Autre"};
    private static final String   EMAIL_REGEX =
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    // ── Initialisation ────────────────────────────────────────────────

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance().getCurrentUser();

        // ComboBoxes
        groupeSanguinCombo.setItems(FXCollections.observableArrayList(GROUPES_SANGUINS));
        profileSexeCombo.setItems(FXCollections.observableArrayList(SEXES));

        // Sync show/hide mots de passe
        oldPwdField.textProperty().bindBidirectional(oldPwdVisible.textProperty());
        newPwdField.textProperty().bindBidirectional(newPwdVisible.textProperty());
        confirmPwdField.textProperty().bindBidirectional(confirmPwdVisible.textProperty());

        // Listeners nettoyage erreurs
        newPwdField.textProperty().addListener((obs, o, n) -> {
            clearErr(newPwdField, errorNewPwd);
            if (!confirmPwdField.getText().isEmpty()) liveCheckConfirm();
        });
        confirmPwdField.textProperty().addListener((obs, o, n) -> liveCheckConfirm());

        if (currentUser != null) {
            loadAll();
            if (currentUser.getMedecinId() != null) {
                Utilisateur dr = serviceUtilisateur.getById(currentUser.getMedecinId());
                if (dr != null && medecinSidebarLabel != null) {
                    medecinSidebarLabel.setText("🩺 Dr. " + dr.getPrenom() + " " + dr.getNom());
                    medecinSidebarLabel.setVisible(true);
                    medecinSidebarLabel.setManaged(true);
                }
            }
        }
    }

    // ── Chargement données ────────────────────────────────────────────

    private void loadAll() {
        patientNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());

        // Stats principaux
        bloodGroupLabelDisplay.setText(orDash(currentUser.getGroupeSanguin()));
        poidsLabelDisplay.setText(currentUser.getPoids() != null ? String.valueOf(currentUser.getPoids()) : "--");
        tailleLabelDisplay.setText(currentUser.getTaille() != null ? String.valueOf(currentUser.getTaille()) : "--");

        // Onglet dossier médical
        groupeSanguinCombo.setValue(currentUser.getGroupeSanguin());
        poidsField.setText(currentUser.getPoids() != null ? String.valueOf(currentUser.getPoids()) : "");
        tailleField.setText(currentUser.getTaille() != null ? String.valueOf(currentUser.getTaille()) : "");
        antecedentsArea.setText(orEmpty(currentUser.getAntecedents()));
        allergiesArea.setText(orEmpty(currentUser.getAllergies()));
        traitementsArea.setText(orEmpty(currentUser.getTraitements()));

        // Onglet profil
        profileNomField.setText(orEmpty(currentUser.getNom()));
        profilePrenomField.setText(orEmpty(currentUser.getPrenom()));
        profileEmailField.setText(orEmpty(currentUser.getEmail()));
        // Téléphone, sexe, date, adresse : stockés dans notes comme JSON simple ou ignorés
        // (seront persistés quand on ajoutera les colonnes BDD)
    }

    // ── Onglet 1 : Dossier Médical ────────────────────────────────────

    @FXML
    void handleSaveMedicalRecord(ActionEvent event) {
        clearMedErrors();
        boolean ok = true;

        String gs        = groupeSanguinCombo.getValue();
        String poidsStr  = poidsField.getText().trim();
        String tailleStr = tailleField.getText().trim();

        // Poids
        if (!poidsStr.isEmpty()) {
            try {
                double p = Double.parseDouble(poidsStr);
                if (p <= 0 || p > 500) { showFieldErr(poidsField, errorPoids, "Valeur entre 0 et 500 kg."); ok = false; }
            } catch (NumberFormatException e) { showFieldErr(poidsField, errorPoids, "Nombre invalide."); ok = false; }
        }
        // Taille
        if (!tailleStr.isEmpty()) {
            try {
                double t = Double.parseDouble(tailleStr);
                if (t <= 0 || t > 300) { showFieldErr(tailleField, errorTaille, "Valeur entre 0 et 300 cm."); ok = false; }
            } catch (NumberFormatException e) { showFieldErr(tailleField, errorTaille, "Nombre invalide."); ok = false; }
        }
        if (!ok) return;

        // Mise à jour objet
        currentUser.setGroupeSanguin(gs);
        currentUser.setAntecedents(antecedentsArea.getText().trim());
        currentUser.setAllergies(allergiesArea.getText().trim());
        currentUser.setTraitements(traitementsArea.getText().trim());
        currentUser.setPoids(poidsStr.isEmpty() ? null : Double.parseDouble(poidsStr));
        currentUser.setTaille(tailleStr.isEmpty() ? null : Double.parseDouble(tailleStr));

        // Sauvegarde async
        saveMedBtn.setDisable(true);
        saveMedBtn.setText("Enregistrement...");
        Thread t = new Thread(() -> {
            boolean success = serviceUtilisateur.updateDossierComplet(currentUser);
            Platform.runLater(() -> {
                saveMedBtn.setDisable(false);
                saveMedBtn.setText("💾  Enregistrer le dossier");
                if (success) {
                    UserSession.getInstance().setCurrentUser(currentUser);
                    loadAll();
                    showFeedback(medFeedbackLabel, "✅ Dossier médical mis à jour avec succès !", true);
                } else {
                    showFeedback(medFeedbackLabel, "❌ Erreur lors de la mise à jour.", false);
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Onglet 2 : Mon Profil ─────────────────────────────────────────

    @FXML
    void handleSaveProfile(ActionEvent event) {
        clearProfileErrors();
        boolean ok = true;

        String nom    = profileNomField.getText().trim();
        String prenom = profilePrenomField.getText().trim();
        String email  = profileEmailField.getText().trim().toLowerCase();

        if (nom.isEmpty())    { showFieldErr(profileNomField,    profileErrorNom,    "Le nom est obligatoire.");    ok = false; }
        if (prenom.isEmpty()) { showFieldErr(profilePrenomField, profileErrorPrenom, "Le prénom est obligatoire."); ok = false; }
        if (email.isEmpty() || !email.matches(EMAIL_REGEX))
                              { showFieldErr(profileEmailField,  profileErrorEmail,  "Email invalide.");             ok = false; }
        else if (!email.equals(currentUser.getEmail()) && serviceUtilisateur.emailExists(email))
                              { showFieldErr(profileEmailField,  profileErrorEmail,  "Cet email est déjà utilisé."); ok = false; }
        if (!ok) return;

        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);

        saveProfileBtn.setDisable(true);
        saveProfileBtn.setText("Enregistrement...");
        Thread t = new Thread(() -> {
            serviceUtilisateur.update(currentUser);
            Platform.runLater(() -> {
                saveProfileBtn.setDisable(false);
                saveProfileBtn.setText("💾  Enregistrer le profil");
                UserSession.getInstance().setCurrentUser(currentUser);
                patientNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
                showFeedback(profileFeedbackLabel, "✅ Profil mis à jour avec succès !", true);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    void handleCancelProfile(ActionEvent event) {
        loadAll();
        clearProfileErrors();
    }

    // ── Onglet 3 : Sécurité ───────────────────────────────────────────

    @FXML
    void toggleShowPassword(ActionEvent event) {
        boolean show = showPwdCheck.isSelected();
        toggleField(oldPwdField,     oldPwdVisible,     show);
        toggleField(newPwdField,     newPwdVisible,     show);
        toggleField(confirmPwdField, confirmPwdVisible, show);
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        clearPwdErrors();
        boolean ok = true;

        String oldPwd  = oldPwdField.getText();
        String newPwd  = newPwdField.getText();
        String confirm = confirmPwdField.getText();

        if (oldPwd.isEmpty())  { showFieldErr(oldPwdField,     errorOldPwd,     "L'ancien mot de passe est obligatoire."); ok = false; }
        if (newPwd.isEmpty())  { showFieldErr(newPwdField,     errorNewPwd,     "Le nouveau mot de passe est obligatoire."); ok = false; }
        else if (newPwd.length() < 8)
                               { showFieldErr(newPwdField,     errorNewPwd,     "Minimum 8 caractères requis."); ok = false; }
        if (ok && !newPwd.equals(confirm))
                               { showFieldErr(confirmPwdField, errorConfirmPwd, "Les mots de passe ne correspondent pas."); ok = false; }
        if (!ok) return;

        // Vérification ancien mot de passe (BCrypt)
        if (!serviceUtilisateur.verifyPassword(currentUser.getEmail(), oldPwd)) {
            showFieldErr(oldPwdField, errorOldPwd, "❌ Mot de passe actuel incorrect.");
            return;
        }

        savePwdBtn.setDisable(true);
        savePwdBtn.setText("Modification...");
        Thread t = new Thread(() -> {
            String hashed = BCrypt.hashpw(newPwd, BCrypt.gensalt());
            boolean success = serviceUtilisateur.updatePassword(currentUser.getEmail(), hashed);
            Platform.runLater(() -> {
                savePwdBtn.setDisable(false);
                savePwdBtn.setText("🔐  Changer le mot de passe");
                if (success) {
                    oldPwdField.clear(); newPwdField.clear(); confirmPwdField.clear();
                    showFeedback(pwdFeedbackLabel, "✅ Mot de passe modifié avec succès !", true);
                } else {
                    showFeedback(pwdFeedbackLabel, "❌ Erreur lors du changement de mot de passe.", false);
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    void handleCancelPassword(ActionEvent event) {
        oldPwdField.clear(); newPwdField.clear(); confirmPwdField.clear();
        clearPwdErrors();
    }

    // ── Navigation ────────────────────────────────────────────────────

    @FXML void handleArticles(ActionEvent event) { App.navigate("PatientArticleList"); }
    @FXML void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void liveCheckConfirm() {
        String n = newPwdField.getText();
        String c = confirmPwdField.getText();
        if (c.isEmpty()) { errorConfirmPwd.setVisible(false); errorConfirmPwd.setManaged(false); return; }
        if (!n.equals(c)) {
            showFieldErr(confirmPwdField, errorConfirmPwd, "❌ Les mots de passe ne correspondent pas");
        } else {
            errorConfirmPwd.setText("✅ Identiques");
            errorConfirmPwd.setStyle("-fx-text-fill: #2BBCB0; -fx-font-size: 11px;");
            errorConfirmPwd.setVisible(true); errorConfirmPwd.setManaged(true);
            confirmPwdField.getStyleClass().remove("field-input-error");
        }
    }

    private void showFieldErr(javafx.scene.control.Control field, Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px;");
        lbl.setVisible(true); lbl.setManaged(true);
        field.getStyleClass().remove("field-input-error");
        field.getStyleClass().add("field-input-error");
    }

    private void clearErr(javafx.scene.control.Control field, Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false);
        field.getStyleClass().remove("field-input-error");
    }

    private void clearMedErrors() {
        clearErr(poidsField, errorPoids);
        clearErr(tailleField, errorTaille);
    }

    private void clearProfileErrors() {
        clearErr(profileNomField,    profileErrorNom);
        clearErr(profilePrenomField, profileErrorPrenom);
        clearErr(profileEmailField,  profileErrorEmail);
        hideFeedback(profileFeedbackLabel);
    }

    private void clearPwdErrors() {
        clearErr(oldPwdField,     errorOldPwd);
        clearErr(newPwdField,     errorNewPwd);
        clearErr(confirmPwdField, errorConfirmPwd);
        hideFeedback(pwdFeedbackLabel);
    }

    private void showFeedback(Label lbl, String msg, boolean success) {
        lbl.setText(msg);
        lbl.setStyle(success
            ? "-fx-text-fill: #2BBCB0; -fx-font-size: 12px; -fx-font-weight: bold;" +
              "-fx-padding: 8 12 8 12; -fx-background-color: #f0fffe;" +
              "-fx-background-radius: 6; -fx-border-color: #2BBCB0; -fx-border-radius: 6; -fx-border-width: 1;"
            : "-fx-text-fill: #E53E3E; -fx-font-size: 12px; -fx-font-weight: bold;" +
              "-fx-padding: 8 12 8 12; -fx-background-color: #fff5f5;" +
              "-fx-background-radius: 6; -fx-border-color: #FC8181; -fx-border-radius: 6; -fx-border-width: 1;"
        );
        lbl.setVisible(true); lbl.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), lbl);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        if (success) {
            PauseTransition hide = new PauseTransition(Duration.seconds(4));
            hide.setOnFinished(e -> hideFeedback(lbl));
            hide.play();
        }
    }

    private void hideFeedback(Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false);
    }

    private void toggleField(PasswordField pf, TextField tf, boolean show) {
        pf.setVisible(!show); pf.setManaged(!show);
        tf.setVisible(show);  tf.setManaged(show);
    }

    private String orEmpty(String s) { return s != null ? s : ""; }
    private String orDash(String s)  { return (s != null && !s.isEmpty()) ? s : "--"; }
}
