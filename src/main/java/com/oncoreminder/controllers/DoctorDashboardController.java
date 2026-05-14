package com.oncoreminder.controllers;

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
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

public class DoctorDashboardController {

    public static int     pendingTab            = -1;
    public static boolean showEventsOnLoad      = false;
    public static boolean showReservationsOnLoad = false;

    // ── Sidebar (partagée via fx:include) ─────────────────────────────
    @FXML private DoctorSidebarController doctorSidebarController;

    // ── Contenu principal ─────────────────────────────────────────────
    @FXML private TabPane  mainTabPane;
    @FXML private VBox     mainContent;
    @FXML private VBox     eventsPane;
    @FXML private VBox     reservationsPane;

    // ── Sous-contrôleurs (fx:include) ─────────────────────────────────
    @FXML private EventController       eventTabContentController;
    @FXML private ReservationController reservationContentController;

    // ── Onglet 1 : Mon Profil ────────────────────────────────────────
    @FXML private TextField        docNomField;
    @FXML private TextField        docPrenomField;
    @FXML private TextField        docEmailField;
    @FXML private TextField        docTelField;
    @FXML private ComboBox<String> docSexeCombo;
    @FXML private TextField        docDateNaissField;
    @FXML private ComboBox<String> docSpecialiteCombo;
    @FXML private TextField        docMatriculeField;
    @FXML private TextField        docHopitalField;
    @FXML private TextField        docAdresseField;
    @FXML private Label  docErrorNom;
    @FXML private Label  docErrorPrenom;
    @FXML private Label  docErrorEmail;
    @FXML private Label  docProfileFeedback;
    @FXML private Button saveDocProfileBtn;

    // ── Onglet 2 : Sécurité ──────────────────────────────────────────
    @FXML private PasswordField docOldPwdField;
    @FXML private TextField     docOldPwdVisible;
    @FXML private PasswordField docNewPwdField;
    @FXML private TextField     docNewPwdVisible;
    @FXML private PasswordField docConfirmPwdField;
    @FXML private TextField     docConfirmPwdVisible;
    @FXML private CheckBox      docShowPwdCheck;
    @FXML private Label  docErrorOldPwd;
    @FXML private Label  docErrorNewPwd;
    @FXML private Label  docErrorConfirmPwd;
    @FXML private Label  docPwdFeedback;
    @FXML private Button saveDocPwdBtn;

    // ── Services ─────────────────────────────────────────────────────
    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();
    private Utilisateur currentDoctor;

    private static final String[] SEXES = {"Masculin", "Féminin", "Autre"};
    private static final String[] SPECIALITES = {
        "Oncologie","Cardiologie","Dermatologie","Gynécologie","Neurologie",
        "Pédiatrie","Psychiatrie","Radiologie","Rhumatologie","Chirurgie générale","Médecine générale"
    };
    private static final String EMAIL_REGEX =
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    @FXML
    public void initialize() {
        currentDoctor = UserSession.getInstance().getCurrentUser();
        if (pendingTab >= 0) { int t = pendingTab; pendingTab = -1; Platform.runLater(() -> mainTabPane.getSelectionModel().select(t)); }
        if (showEventsOnLoad)       { showEventsOnLoad       = false; Platform.runLater(() -> showPane(1)); }
        if (showReservationsOnLoad) { showReservationsOnLoad = false; Platform.runLater(() -> showPane(2)); }

        Platform.runLater(() -> {
            if (eventTabContentController != null)
                eventTabContentController.setOnOuvrirReservations(() -> showPane(2));
            if (reservationContentController != null)
                reservationContentController.setOnRetourEvents(() -> showPane(1));
        });

        if (doctorSidebarController != null) {
            doctorSidebarController.setMonEspaceCallback(() -> showPane(0));
            doctorSidebarController.setEvenementsCallback(() -> showPane(1));
            doctorSidebarController.setActivePane(0);
        }

        docSexeCombo.setItems(FXCollections.observableArrayList(SEXES));
        docSpecialiteCombo.setItems(FXCollections.observableArrayList(SPECIALITES));

        docOldPwdField.textProperty().bindBidirectional(docOldPwdVisible.textProperty());
        docNewPwdField.textProperty().bindBidirectional(docNewPwdVisible.textProperty());
        docConfirmPwdField.textProperty().bindBidirectional(docConfirmPwdVisible.textProperty());

        docNewPwdField.textProperty().addListener((obs, o, n) -> {
            clearErr(docNewPwdField, docErrorNewPwd);
            if (!docConfirmPwdField.getText().isEmpty()) liveCheckDocConfirm();
        });
        docConfirmPwdField.textProperty().addListener((obs, o, n) -> liveCheckDocConfirm());

        loadDocProfile();
    }

    // ════════════════════════════════════════════════════════════════
    // ONGLET 1 — MON PROFIL MÉDECIN
    // ════════════════════════════════════════════════════════════════

    private void loadDocProfile() {
        if (currentDoctor == null) return;
        docNomField.setText(orE(currentDoctor.getNom()));
        docPrenomField.setText(orE(currentDoctor.getPrenom()));
        docEmailField.setText(orE(currentDoctor.getEmail()));
    }

    @FXML void handleSaveDocProfile(ActionEvent e) {
        String nom = docNomField.getText().trim(), prenom = docPrenomField.getText().trim(),
               email = docEmailField.getText().trim().toLowerCase();
        boolean ok = true;
        if (nom.isEmpty())    { showFe(docNomField, docErrorNom, "Obligatoire."); ok = false; }
        if (prenom.isEmpty()) { showFe(docPrenomField, docErrorPrenom, "Obligatoire."); ok = false; }
        if (!email.matches(EMAIL_REGEX)) { showFe(docEmailField, docErrorEmail, "Email invalide."); ok = false; }
        else if (!email.equals(currentDoctor.getEmail()) && serviceUtilisateur.emailExists(email))
                              { showFe(docEmailField, docErrorEmail, "Email déjà utilisé."); ok = false; }
        if (!ok) return;

        currentDoctor.setNom(nom); currentDoctor.setPrenom(prenom); currentDoctor.setEmail(email);
        saveDocProfileBtn.setDisable(true); saveDocProfileBtn.setText("Enregistrement...");
        Thread profileThread = new Thread(() -> {
            serviceUtilisateur.update(currentDoctor);
            Platform.runLater(() -> {
                saveDocProfileBtn.setDisable(false); saveDocProfileBtn.setText("💾  Enregistrer le profil");
                UserSession.getInstance().setCurrentUser(currentDoctor);
                if (doctorSidebarController != null)
                    doctorSidebarController.initialize();
                showFb(docProfileFeedback, "✅ Profil mis à jour avec succès !", true);
            });
        });
        profileThread.setDaemon(true);
        profileThread.start();
    }

    @FXML void handleCancelDocProfile(ActionEvent e) { loadDocProfile(); hideFb(docProfileFeedback); }

    // ════════════════════════════════════════════════════════════════
    // ONGLET 2 — SÉCURITÉ
    // ════════════════════════════════════════════════════════════════

    @FXML void toggleDocShowPassword(ActionEvent e) {
        boolean s = docShowPwdCheck.isSelected();
        toggle(docOldPwdField, docOldPwdVisible, s);
        toggle(docNewPwdField, docNewPwdVisible, s);
        toggle(docConfirmPwdField, docConfirmPwdVisible, s);
    }

    @FXML void handleChangeDocPassword(ActionEvent e) {
        String old = docOldPwdField.getText(), np = docNewPwdField.getText(), conf = docConfirmPwdField.getText();
        boolean ok = true;
        if (old.isEmpty())       { showFe(docOldPwdField, docErrorOldPwd, "Obligatoire."); ok = false; }
        if (np.isEmpty())        { showFe(docNewPwdField, docErrorNewPwd, "Obligatoire."); ok = false; }
        else if (np.length() < 8){ showFe(docNewPwdField, docErrorNewPwd, "Minimum 8 caractères."); ok = false; }
        if (ok && !np.equals(conf)) { showFe(docConfirmPwdField, docErrorConfirmPwd, "Mots de passe différents."); ok = false; }
        if (!ok) return;

        if (!serviceUtilisateur.verifyPassword(currentDoctor.getEmail(), old)) {
            showFe(docOldPwdField, docErrorOldPwd, "❌ Mot de passe actuel incorrect."); return;
        }
        saveDocPwdBtn.setDisable(true); saveDocPwdBtn.setText("Modification...");
        final String newPwdHashed = BCrypt.hashpw(np, BCrypt.gensalt());
        Thread pwdThread = new Thread(() -> {
            boolean success = serviceUtilisateur.updatePassword(currentDoctor.getEmail(), newPwdHashed);
            Platform.runLater(() -> {
                saveDocPwdBtn.setDisable(false); saveDocPwdBtn.setText("🔐  Changer le mot de passe");
                if (success) { docOldPwdField.clear(); docNewPwdField.clear(); docConfirmPwdField.clear();
                    showFb(docPwdFeedback, "✅ Mot de passe modifié !", true);
                } else { showFb(docPwdFeedback, "❌ Erreur lors du changement.", false); }
            });
        });
        pwdThread.setDaemon(true);
        pwdThread.start();
    }

    @FXML void handleCancelDocPassword(ActionEvent e) {
        docOldPwdField.clear(); docNewPwdField.clear(); docConfirmPwdField.clear(); hideFb(docPwdFeedback);
    }

    private void liveCheckDocConfirm() {
        String n = docNewPwdField.getText(), c = docConfirmPwdField.getText();
        if (c.isEmpty()) { docErrorConfirmPwd.setVisible(false); docErrorConfirmPwd.setManaged(false); return; }
        if (!n.equals(c)) showFe(docConfirmPwdField, docErrorConfirmPwd, "❌ Différents");
        else { docErrorConfirmPwd.setText("✅ Identiques"); docErrorConfirmPwd.setStyle("-fx-text-fill:#2BBCB0;-fx-font-size:11px;");
               docErrorConfirmPwd.setVisible(true); docErrorConfirmPwd.setManaged(true);
               docConfirmPwdField.getStyleClass().remove("field-input-error"); }
    }

    // ── Navigation ────────────────────────────────────────────────────

    private void showPane(int which) {
        mainContent.setVisible(which == 0);      mainContent.setManaged(which == 0);
        eventsPane.setVisible(which == 1);       eventsPane.setManaged(which == 1);
        reservationsPane.setVisible(which == 2); reservationsPane.setManaged(which == 2);
        if (doctorSidebarController != null) doctorSidebarController.setActivePane(which);
    }

    // ── Helpers génériques ────────────────────────────────────────────
    private void showFb(Label l, String msg, boolean ok) {
        l.setText(msg);
        l.setStyle(ok
            ? "-fx-text-fill:#2BBCB0;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 12 8 12;-fx-background-color:#f0fffe;-fx-background-radius:6;-fx-border-color:#2BBCB0;-fx-border-radius:6;-fx-border-width:1;"
            : "-fx-text-fill:#E53E3E;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 12 8 12;-fx-background-color:#fff5f5;-fx-background-radius:6;-fx-border-color:#FC8181;-fx-border-radius:6;-fx-border-width:1;"
        );
        l.setVisible(true); l.setManaged(true);
        fade(l);
        if (ok) {
            PauseTransition hidePause = new PauseTransition(Duration.seconds(4));
            hidePause.setOnFinished(ev -> hideFb(l));
            hidePause.play();
        }
    }
    private void hideFb(Label l) { l.setVisible(false); l.setManaged(false); }
    private void showFe(Control f, Label l, String msg) {
        l.setText(msg); l.setStyle("-fx-text-fill:#E53E3E;-fx-font-size:11px;"); l.setVisible(true); l.setManaged(true);
        f.getStyleClass().remove("field-input-error"); f.getStyleClass().add("field-input-error");
    }
    private void clearErr(Control f, Label l) {
        l.setVisible(false); l.setManaged(false); f.getStyleClass().remove("field-input-error");
    }
    private void toggle(PasswordField pf, TextField tf, boolean show) {
        pf.setVisible(!show); pf.setManaged(!show); tf.setVisible(show); tf.setManaged(show);
    }
    private void fade(javafx.scene.Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(250), n);
        ft.setFromValue(0.5); ft.setToValue(1.0); ft.play();
    }
    private String orE(String s) { return s != null ? s : ""; }
}