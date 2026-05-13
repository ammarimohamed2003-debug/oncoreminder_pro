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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.stream.Collectors;

public class DoctorDashboardController {

    public static int     pendingTab            = -1;
    public static boolean showEventsOnLoad      = false;
    public static boolean showReservationsOnLoad = false;

    // ── Sidebar ───────────────────────────────────────────────────────
    @FXML private TabPane  mainTabPane;
    @FXML private VBox     mainContent;
    @FXML private VBox     eventsPane;
    @FXML private VBox     reservationsPane;
    @FXML private Button   btnMonEspace;
    @FXML private Button   btnEvenements;
    @FXML private Label    doctorSidebarLabel;

    // ── Sous-contrôleurs (fx:include) ─────────────────────────────────
    @FXML private EventController       eventTabContentController;
    @FXML private ReservationController reservationContentController;

    // ── Top bar ──────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     patientCountLabel;

    // ── Onglet 1 : Patients ──────────────────────────────────────────
    @FXML private FlowPane patientFlowPane;
    @FXML private VBox   detailPanel;
    @FXML private Label  selectedPatientLabel;
    @FXML private Label  patientEmailLabel;
    @FXML private Label  patientIconLabel;
    @FXML private Label  bloodGroupLabel;
    @FXML private Label  weightLabel;
    @FXML private Label  heightLabel;
    @FXML private TextArea antecedentsArea;
    @FXML private TextArea allergiesArea;
    @FXML private VBox   readPanel;
    @FXML private VBox   actionButtons;
    @FXML private VBox   emptyPanel;
    @FXML private VBox      editPanel;
    @FXML private TextField editNomField;
    @FXML private TextField editPrenomField;
    @FXML private TextField editEmailField;
    @FXML private ComboBox<String> editGroupeSanguinCombo;
    @FXML private TextField editPoidsField;
    @FXML private TextField editTailleField;
    @FXML private TextArea  editAntecedentsArea;
    @FXML private TextArea  editAllergiesArea;
    @FXML private TextArea  editTraitementsArea;
    @FXML private TextArea  editNotesArea;
    @FXML private Label     editFeedbackLabel;
    @FXML private Label     editErrorEmail;
    @FXML private Label     editErrorMesures;
    @FXML private Button    saveButton;

    // ── Onglet 2 : Mon Profil ────────────────────────────────────────
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

    // ── Onglet 3 : Sécurité ──────────────────────────────────────────
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
    private Utilisateur selectedPatient;
    private List<Utilisateur> allPatients;
    private Utilisateur currentDoctor;

    private static final String[] GROUPES_SANGUINS = {"A+","A-","B+","B-","AB+","AB-","O+","O-"};
    private static final String[] SEXES = {"Masculin", "Féminin", "Autre"};
    private static final String[] SPECIALITES = {
        "Oncologie","Cardiologie","Dermatologie","Gynécologie","Neurologie",
        "Pédiatrie","Psychiatrie","Radiologie","Rhumatologie","Chirurgie générale","Médecine générale"
    };
    private static final String EMAIL_REGEX =
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    // ── Initialisation ────────────────────────────────────────────────

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

        if (currentDoctor != null) {
            doctorSidebarLabel.setText("Dr. " + currentDoctor.getNom() + " " + currentDoctor.getPrenom());
        }

        // ComboBoxes
        editGroupeSanguinCombo.setItems(FXCollections.observableArrayList(GROUPES_SANGUINS));
        docSexeCombo.setItems(FXCollections.observableArrayList(SEXES));
        docSpecialiteCombo.setItems(FXCollections.observableArrayList(SPECIALITES));

        // Sync show/hide mots de passe
        docOldPwdField.textProperty().bindBidirectional(docOldPwdVisible.textProperty());
        docNewPwdField.textProperty().bindBidirectional(docNewPwdVisible.textProperty());
        docConfirmPwdField.textProperty().bindBidirectional(docConfirmPwdVisible.textProperty());

        // Live check confirmation
        docNewPwdField.textProperty().addListener((obs, o, n) -> {
            clearErr(docNewPwdField, docErrorNewPwd);
            if (!docConfirmPwdField.getText().isEmpty()) liveCheckDocConfirm();
        });
        docConfirmPwdField.textProperty().addListener((obs, o, n) -> liveCheckDocConfirm());

        // Recherche
        searchField.textProperty().addListener((obs, o, n) -> filterPatients(n.trim()));

        // Préremplir profil médecin
        loadDocProfile();

        // Charger patients
        loadPatients();
        showEmpty();
    }

    // ════════════════════════════════════════════════════════════════
    // ONGLET 1 — PATIENTS
    // ════════════════════════════════════════════════════════════════

    private void loadPatients() {
        int medecinId = UserSession.getInstance().getCurrentUser().getId();
        allPatients = serviceUtilisateur.getPatientsByMedecin(medecinId);
        renderPatients(allPatients);
    }

    private void filterPatients(String q) {
        if (q == null || q.isEmpty()) { renderPatients(allPatients); return; }
        String ql = q.toLowerCase();
        renderPatients(allPatients.stream()
            .filter(p -> (p.getNom()+" "+p.getPrenom()+" "+p.getEmail()).toLowerCase().contains(ql))
            .collect(Collectors.toList()));
    }

    private void renderPatients(List<Utilisateur> list) {
        patientFlowPane.getChildren().clear();
        patientCountLabel.setText("(" + list.size() + ")");
        if (list.isEmpty()) {
            Label e = new Label("Aucun patient assigné.\nCliquez sur « + Ajouter » pour en ajouter.");
            e.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 13px; -fx-font-style: italic;");
            e.setWrapText(true);
            patientFlowPane.getChildren().add(e);
            return;
        }
        list.forEach(p -> patientFlowPane.getChildren().add(createPatientCard(p)));
    }

    private VBox createPatientCard(Utilisateur patient) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(165);
        card.setPadding(new Insets(14));
        card.setAlignment(Pos.CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);

        StackPane avatar = new StackPane();
        Circle bg = new Circle(24);
        bg.setFill(javafx.scene.paint.Color.web("rgba(43,188,176,0.18)"));
        Label initials = new Label(
            String.valueOf(patient.getPrenom().isEmpty() ? '?' : patient.getPrenom().charAt(0)) +
            String.valueOf(patient.getNom().isEmpty()    ? '?' : patient.getNom().charAt(0))
        );
        initials.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2BBCB0;");
        avatar.getChildren().addAll(bg, initials);

        Label nameL = new Label(patient.getPrenom() + " " + patient.getNom());
        nameL.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2D3748;");
        nameL.setWrapText(true); nameL.setMaxWidth(150);

        Label bgL = new Label(patient.getGroupeSanguin() != null ? patient.getGroupeSanguin() : "N/A");
        bgL.getStyleClass().add("stat-badge-info");

        card.getChildren().addAll(avatar, nameL, bgL);

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
            if (e.getTarget() == removeBtn) return;
            patientFlowPane.getChildren().forEach(n -> n.setStyle(""));
            card.setStyle("-fx-border-color: #2BBCB0; -fx-border-width: 2; -fx-border-radius: 12;");
            showPatientDetails(patient);
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
        this.selectedPatient = patient;
        selectedPatientLabel.setText(patient.getPrenom() + " " + patient.getNom());
        patientEmailLabel.setText(patient.getEmail());
        bloodGroupLabel.setText(val(patient.getGroupeSanguin()));
        weightLabel.setText(patient.getPoids() != null ? patient.getPoids() + " kg" : "--");
        heightLabel.setText(patient.getTaille() != null ? patient.getTaille() + " cm" : "--");
        antecedentsArea.setText(nvl(patient.getAntecedents(), "Aucun antécédent."));
        allergiesArea.setText(nvl(patient.getAllergies(), "Aucune allergie."));
        showReadMode();
        fade(detailPanel);
    }

    @FXML void handleEdit(ActionEvent e) {
        if (selectedPatient == null) return;
        editNomField.setText(orE(selectedPatient.getNom()));
        editPrenomField.setText(orE(selectedPatient.getPrenom()));
        editEmailField.setText(orE(selectedPatient.getEmail()));
        editGroupeSanguinCombo.setValue(selectedPatient.getGroupeSanguin());
        editPoidsField.setText(selectedPatient.getPoids() != null ? String.valueOf(selectedPatient.getPoids()) : "");
        editTailleField.setText(selectedPatient.getTaille() != null ? String.valueOf(selectedPatient.getTaille()) : "");
        editAntecedentsArea.setText(orE(selectedPatient.getAntecedents()));
        editAllergiesArea.setText(orE(selectedPatient.getAllergies()));
        editTraitementsArea.setText(orE(selectedPatient.getTraitements()));
        editNotesArea.setText(orE(selectedPatient.getNotes()));
        hideFb(editFeedbackLabel);
        showEditMode();
        fade(editPanel);
    }

    @FXML void handleCancelEdit(ActionEvent e) { showReadMode(); }

    @FXML void handleSave(ActionEvent e) {
        if (selectedPatient == null) return;
        hideFb(editFeedbackLabel);

        // Validation email
        String email = editEmailField.getText().trim().toLowerCase();
        if (!email.matches(EMAIL_REGEX)) {
            editErrorEmail.setText("Email invalide."); editErrorEmail.setVisible(true); editErrorEmail.setManaged(true);
            return;
        }
        // Poids / Taille
        String ps = editPoidsField.getText().trim(), ts = editTailleField.getText().trim();
        if (!ps.isEmpty()) { try { Double.parseDouble(ps); } catch (NumberFormatException ex) {
            editErrorMesures.setText("Poids invalide."); editErrorMesures.setVisible(true); editErrorMesures.setManaged(true); return; } }
        if (!ts.isEmpty()) { try { Double.parseDouble(ts); } catch (NumberFormatException ex) {
            editErrorMesures.setText("Taille invalide."); editErrorMesures.setVisible(true); editErrorMesures.setManaged(true); return; } }

        selectedPatient.setNom(editNomField.getText().trim());
        selectedPatient.setPrenom(editPrenomField.getText().trim());
        selectedPatient.setEmail(email);
        selectedPatient.setGroupeSanguin(editGroupeSanguinCombo.getValue());
        selectedPatient.setPoids(ps.isEmpty() ? null : Double.parseDouble(ps));
        selectedPatient.setTaille(ts.isEmpty() ? null : Double.parseDouble(ts));
        selectedPatient.setAntecedents(editAntecedentsArea.getText().trim());
        selectedPatient.setAllergies(editAllergiesArea.getText().trim());
        selectedPatient.setTraitements(editTraitementsArea.getText().trim());
        selectedPatient.setNotes(editNotesArea.getText().trim());

        saveButton.setDisable(true); saveButton.setText("Enregistrement...");
        Thread saveThread = new Thread(() -> {
            boolean ok = serviceUtilisateur.updateDossierComplet(selectedPatient);
            Platform.runLater(() -> {
                saveButton.setDisable(false); saveButton.setText("💾  Enregistrer");
                if (ok) { loadPatients(); showPatientDetails(selectedPatient);
                    showFb(editFeedbackLabel, "✅ Dossier mis à jour !", true);
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(ev -> showReadMode());
                    pause.play();
                } else { showFb(editFeedbackLabel, "❌ Erreur lors de la mise à jour.", false); }
            });
        });
        saveThread.setDaemon(true);
        saveThread.start();
    }

    @FXML void handleDelete(ActionEvent e) {
        if (selectedPatient == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Supprimer le patient");
        a.setHeaderText("Êtes-vous sûr ?");
        a.setContentText("Patient : " + selectedPatient.getPrenom() + " " + selectedPatient.getNom() + "\n⚠️ Action irréversible.");
        a.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) {
            serviceUtilisateur.deletePatient(selectedPatient.getId());
            selectedPatient = null; loadPatients(); showEmpty();
        }});
    }

    @FXML void handleRefresh(ActionEvent e) { searchField.clear(); loadPatients(); }

    // ════════════════════════════════════════════════════════════════
    // ONGLET 2 — MON PROFIL MÉDECIN
    // ════════════════════════════════════════════════════════════════

    private void loadDocProfile() {
        if (currentDoctor == null) return;
        docNomField.setText(orE(currentDoctor.getNom()));
        docPrenomField.setText(orE(currentDoctor.getPrenom()));
        docEmailField.setText(orE(currentDoctor.getEmail()));
    }

    @FXML void handleSaveDocProfile(ActionEvent e) {
        // Validation
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
                doctorSidebarLabel.setText("Dr. " + currentDoctor.getNom() + " " + currentDoctor.getPrenom());
                showFb(docProfileFeedback, "✅ Profil mis à jour avec succès !", true);
            });
        });
        profileThread.setDaemon(true);
        profileThread.start();
    }

    @FXML void handleCancelDocProfile(ActionEvent e) { loadDocProfile(); hideFb(docProfileFeedback); }

    // ════════════════════════════════════════════════════════════════
    // ONGLET 3 — SÉCURITÉ
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
    @FXML void handleArticles(ActionEvent e)   { App.navigate("ArticleList"); }
    @FXML void handleMonEspace(ActionEvent e)  { showPane(0); }
    @FXML void handleRendezVous(ActionEvent e) { showPane(1); }
    @FXML void handleLogout(ActionEvent e)     { UserSession.getInstance().logout(); App.navigate("Login"); }

    private static final String BTN_ACTIVE = "sidebar-nav-btn-doctor-active";
    private static final String BTN_NORMAL = "sidebar-nav-btn";

    private void showPane(int which) {
        mainContent.setVisible(which == 0);      mainContent.setManaged(which == 0);
        eventsPane.setVisible(which == 1);       eventsPane.setManaged(which == 1);
        reservationsPane.setVisible(which == 2); reservationsPane.setManaged(which == 2);
        btnMonEspace.getStyleClass().setAll(which == 0 ? BTN_ACTIVE : BTN_NORMAL);
        // Événements reste actif aussi quand réservations est affiché (sous-section)
        btnEvenements.getStyleClass().setAll(which >= 1 ? BTN_ACTIVE : BTN_NORMAL);
    }

    // ── Modes panneau patient ─────────────────────────────────────────
    private void showEmpty() {
        emptyPanel.setVisible(true);  emptyPanel.setManaged(true);
        readPanel.setVisible(false);  readPanel.setManaged(false);
        editPanel.setVisible(false);  editPanel.setManaged(false);
        actionButtons.setVisible(false); actionButtons.setManaged(false);
        selectedPatientLabel.setText("Sélectionnez un patient"); patientEmailLabel.setText("");
        bloodGroupLabel.setText("--"); weightLabel.setText("--"); heightLabel.setText("--");
    }
    private void showReadMode() {
        emptyPanel.setVisible(false); emptyPanel.setManaged(false);
        readPanel.setVisible(true);   readPanel.setManaged(true);
        editPanel.setVisible(false);  editPanel.setManaged(false);
        actionButtons.setVisible(true); actionButtons.setManaged(true);
    }
    private void showEditMode() {
        emptyPanel.setVisible(false); emptyPanel.setManaged(false);
        readPanel.setVisible(false);  readPanel.setManaged(false);
        editPanel.setVisible(true);   editPanel.setManaged(true);
        actionButtons.setVisible(false); actionButtons.setManaged(false);
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
            hidePause.setOnFinished(e -> hideFb(l));
            hidePause.play();
        }
    }
    private void hideFb(Label l) { l.setVisible(false); l.setManaged(false); }
    private void showFe(javafx.scene.control.Control f, Label l, String msg) {
        l.setText(msg); l.setStyle("-fx-text-fill:#E53E3E;-fx-font-size:11px;"); l.setVisible(true); l.setManaged(true);
        f.getStyleClass().remove("field-input-error"); f.getStyleClass().add("field-input-error");
    }
    private void clearErr(javafx.scene.control.Control f, Label l) {
        l.setVisible(false); l.setManaged(false); f.getStyleClass().remove("field-input-error");
    }
    private void toggle(PasswordField pf, TextField tf, boolean show) {
        pf.setVisible(!show); pf.setManaged(!show); tf.setVisible(show); tf.setManaged(show);
    }
    private void fade(javafx.scene.Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(250), n);
        ft.setFromValue(0.5); ft.setToValue(1.0); ft.play();
    }
    private String val(String s) { return (s != null && !s.isEmpty()) ? s : "--"; }
    private String orE(String s) { return s != null ? s : ""; }
    private String nvl(String s, String def) { return (s != null && !s.isEmpty()) ? s : def; }
}
