package com.oncoreminder.controllers;

import com.oncoreminder.models.Ordonnance;
import com.oncoreminder.models.RendezVous;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceOrdonnance;
import com.oncoreminder.services.ServiceRendezVous;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class PatientRendezVousController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy 'à' HH:mm", java.util.Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ORD_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private PatientSidebarController patientSidebarController;

    // Formulaire
    @FXML private Label     formTitleLabel;
    @FXML private ComboBox<String>      specialiteComboBox;
    @FXML private ComboBox<Utilisateur> medecinComboBox;
    @FXML private Label     medecinCountLabel;
    @FXML private DatePicker datePicker;
    @FXML private TextField  timeField;
    @FXML private TextField  lieuField;
    @FXML private TextArea   notesArea;
    @FXML private Label      formMessageLabel;
    @FXML private Button     submitBtn;
    @FXML private Button     cancelEditBtn;

    // Stats
    @FXML private Label totalLabel;
    @FXML private Label attenteLabel;
    @FXML private Label accepteLabel;
    @FXML private Label statusLabel;
    @FXML private Label ordStatusLabel;

    // Card containers
    @FXML private VBox  rdvCardsBox;
    @FXML private Label rdvEmptyLabel;
    @FXML private VBox  ordCardsBox;
    @FXML private Label ordEmptyLabel;

    private final ServiceRendezVous  serviceRdv  = new ServiceRendezVous();
    private final ServiceOrdonnance  serviceOrd  = new ServiceOrdonnance();
    private final ServiceUtilisateur serviceUser = new ServiceUtilisateur();
    private Utilisateur currentPatient;
    private RendezVous  rdvEnEdition = null;

    @FXML
    public void initialize() {
        currentPatient = UserSession.getInstance().getCurrentUser();
        if (patientSidebarController != null)
            patientSidebarController.setActivePage("rendezvous");

        setupSpecialiteComboBox();
        setupMedecinComboBox();
        datePicker.setValue(LocalDate.now().plusDays(1));
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);
        load();
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private void setupSpecialiteComboBox() {
        List<String> specialites = serviceUser.getAllSpecialites();
        specialiteComboBox.getItems().setAll(specialites);

        specialiteComboBox.valueProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                medecinComboBox.getItems().clear();
                medecinComboBox.setValue(null);
                medecinCountLabel.setText("");
                return;
            }
            List<Utilisateur> medecins = serviceUser.getMedecinsBySpecialite(selected);
            medecinComboBox.getItems().setAll(medecins);
            medecinComboBox.setValue(null);
            medecinCountLabel.setText(medecins.size() + " médecin(s) disponible(s)");
        });
    }

    private void setupMedecinComboBox() {
        medecinComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? "Choisir un médecin..." : "Dr. " + u.getPrenom() + " " + u.getNom());
            }
        });
        medecinComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(""); return; }
                String spec = u.getSpecialite() != null ? " — " + u.getSpecialite() : "";
                setText("Dr. " + u.getPrenom() + " " + u.getNom() + spec);
            }
        });
    }

    // ── Load ───────────────────────────────────────────────────────────────

    private void load() {
        if (currentPatient == null) return;
        List<RendezVous>  rdvList = serviceRdv.getByPatient(currentPatient.getId());
        List<Ordonnance>  ordList = serviceOrd.getByPatient(currentPatient.getId());

        long attente = rdvList.stream().filter(r -> r.getStatut() == RendezVous.Statut.EN_ATTENTE).count();
        long accepte = rdvList.stream().filter(r -> r.getStatut() == RendezVous.Statut.ACCEPTE).count();
        totalLabel.setText(String.valueOf(rdvList.size()));
        attenteLabel.setText(String.valueOf(attente));
        accepteLabel.setText(String.valueOf(accepte));
        statusLabel.setText(rdvList.size() + " rendez-vous");
        ordStatusLabel.setText(ordList.size() + " ordonnance(s)");

        buildRdvCards(rdvList);
        buildOrdCards(ordList);
    }

    // ── Card builders ──────────────────────────────────────────────────────

    private void buildRdvCards(List<RendezVous> list) {
        rdvCardsBox.getChildren().clear();
        if (list.isEmpty()) {
            rdvCardsBox.getChildren().add(rdvEmptyLabel);
            return;
        }
        for (RendezVous rv : list) {
            rdvCardsBox.getChildren().add(buildRdvCard(rv));
        }
    }

    private HBox buildRdvCard(RendezVous rv) {
        // Status stripe (left colored bar)
        String stripeColor = switch (rv.getStatut()) {
            case EN_ATTENTE -> "#D97706";
            case ACCEPTE    -> "#059669";
            case REFUSE     -> "#DC2626";
        };
        Region stripe = new Region();
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);
        stripe.setStyle("-fx-background-color:" + stripeColor + ";-fx-background-radius:8 0 0 8;");

        // Card content
        VBox content = new VBox(6);
        content.setStyle("-fx-padding:12 14 12 14;");
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: doctor + status badge
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        String docName = rv.getMedecinNom() != null && !rv.getMedecinNom().isBlank()
                ? "Dr. " + rv.getMedecinNom() : "Médecin inconnu";
        Label lblDoc = new Label("🩺 " + docName);
        lblDoc.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label(rv.getStatutLabel());
        badge.setStyle(
            "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:white;" +
            "-fx-background-color:" + stripeColor + ";" +
            "-fx-background-radius:20;-fx-padding:3 10;"
        );
        row1.getChildren().addAll(lblDoc, spacer, badge);

        // Row 2: date
        String dateStr = rv.getDateRdv() != null ? rv.getDateRdv().format(DATE_FMT) : "—";
        Label lblDate = new Label("📅 " + capitalize(dateStr));
        lblDate.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");

        // Row 3: lieu (if present)
        VBox rows = new VBox(4, row1, lblDate);

        if (rv.getLieu() != null && !rv.getLieu().isBlank()) {
            Label lblLieu = new Label("📍 " + rv.getLieu());
            lblLieu.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");
            rows.getChildren().add(lblLieu);
        }
        if (rv.getNotes() != null && !rv.getNotes().isBlank()) {
            Label lblNotes = new Label("📝 " + rv.getNotes());
            lblNotes.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;-fx-font-style:italic;");
            lblNotes.setWrapText(true);
            rows.getChildren().add(lblNotes);
        }
        content.getChildren().add(rows);

        // Action buttons (only EN_ATTENTE)
        if (rv.getStatut() == RendezVous.Statut.EN_ATTENTE) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            Button btnEdit = new Button("✏️ Modifier");
            btnEdit.setStyle("-fx-background-color:#5B35A5;-fx-text-fill:white;-fx-background-radius:7;-fx-font-size:11px;-fx-padding:5 12;-fx-cursor:hand;");
            Button btnCancel = new Button("✕ Annuler");
            btnCancel.setStyle("-fx-background-color:transparent;-fx-text-fill:#DC2626;-fx-border-color:#DC2626;-fx-border-width:1.5;-fx-background-radius:7;-fx-font-size:11px;-fx-padding:5 12;-fx-cursor:hand;");

            btnEdit.setOnAction(e -> enterEditMode(rv));
            btnCancel.setOnAction(e -> {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Annuler ce rendez-vous ?", ButtonType.YES, ButtonType.NO);
                a.setHeaderText("Confirmation");
                if (a.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                    if (serviceRdv.supprimer(rv.getId())) { load(); formMessageLabel.setText("Rendez-vous annulé."); }
                }
            });
            actions.getChildren().addAll(btnEdit, btnCancel);
            content.getChildren().add(actions);
        }

        HBox card = new HBox(stripe, content);
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:10;" +
            "-fx-border-color:#E8EDF5;" +
            "-fx-border-radius:10;" +
            "-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);"
        );
        return card;
    }

    private void buildOrdCards(List<Ordonnance> list) {
        ordCardsBox.getChildren().clear();
        if (list.isEmpty()) {
            ordCardsBox.getChildren().add(ordEmptyLabel);
            return;
        }
        for (Ordonnance ord : list) {
            ordCardsBox.getChildren().add(buildOrdCard(ord));
        }
    }

    private HBox buildOrdCard(Ordonnance ord) {
        Region stripe = new Region();
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);
        stripe.setStyle("-fx-background-color:#4A2D8F;-fx-background-radius:8 0 0 8;");

        VBox content = new VBox(7);
        content.setStyle("-fx-padding:12 14 12 14;");
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: title + date
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("💊 Ordonnance #" + ord.getId());
        lblTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#4A2D8F;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        String dateStr = ord.getDateEmission() != null ? ord.getDateEmission().format(ORD_DATE) : "—";
        Label lblDate = new Label("📅 " + dateStr);
        lblDate.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;-fx-background-color:#F0EDF8;-fx-background-radius:12;-fx-padding:3 8;");
        row1.getChildren().addAll(lblTitle, spacer, lblDate);

        // Meds
        Label lblMedsTitle = new Label("Médicaments");
        lblMedsTitle.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;-fx-text-transform:uppercase;");
        Label lblMeds = new Label(safe(ord.getMedicaments()));
        lblMeds.setStyle("-fx-font-size:12px;-fx-text-fill:#1E293B;");
        lblMeds.setWrapText(true);

        // Posologie
        Label lblPosoTitle = new Label("Posologie");
        lblPosoTitle.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
        Label lblPoso = new Label(safe(ord.getPosologie()));
        lblPoso.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;-fx-font-style:italic;");
        lblPoso.setWrapText(true);

        // Durée + bouton PDF
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label lblDuree = new Label("⏱ Durée : " + ord.getDureeJours() + " jour(s)");
        lblDuree.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;-fx-background-color:#F0EDF8;-fx-background-radius:12;-fx-padding:3 8;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnPdf = new Button("📄 Télécharger PDF");
        btnPdf.setStyle("-fx-background-color:#4A2D8F;-fx-text-fill:white;-fx-background-radius:8;-fx-font-size:11px;-fx-padding:5 14;-fx-cursor:hand;");
        btnPdf.setOnAction(e -> {
            RendezVous rdv = serviceRdv.getById(ord.getRendezVousId());
            OrdonnanceController.exportOrdonnancePdf(ord, rdv, null);
        });
        footer.getChildren().addAll(lblDuree, sp, btnPdf);

        content.getChildren().addAll(row1, lblMedsTitle, lblMeds, lblPosoTitle, lblPoso, footer);

        HBox card = new HBox(stripe, content);
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:10;" +
            "-fx-border-color:#DDD6F5;" +
            "-fx-border-radius:10;" +
            "-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(74,45,143,0.07),8,0,0,2);"
        );
        return card;
    }

    // ── Actions formulaire ─────────────────────────────────────────────────

    @FXML
    private void onSubmit() {
        if (currentPatient == null) { formMessageLabel.setText("Erreur : utilisateur non connecté."); return; }
        if (specialiteComboBox.getValue() == null) { formMessageLabel.setText("Choisissez une spécialité."); return; }
        if (medecinComboBox.getValue() == null) { formMessageLabel.setText("Choisissez un médecin."); return; }
        if (datePicker.getValue() == null || timeField.getText().isBlank()) {
            formMessageLabel.setText("La date et l'heure sont obligatoires."); return;
        }
        LocalTime time;
        try { time = LocalTime.parse(timeField.getText().trim(), TIME_FMT); }
        catch (DateTimeParseException e) { formMessageLabel.setText("Format heure invalide (HH:mm)."); return; }

        LocalDateTime dateTime = LocalDateTime.of(datePicker.getValue(), time);
        if (dateTime.isBefore(LocalDateTime.now())) {
            formMessageLabel.setText("La date doit être dans le futur."); return;
        }

        String lieu  = lieuField.getText().isBlank()  ? null : lieuField.getText().trim();
        String notes = notesArea.getText().isBlank()   ? null : notesArea.getText().trim();
        int medecinId = medecinComboBox.getValue().getId();

        if (rdvEnEdition != null) {
            rdvEnEdition.setMedecinId(medecinId);
            rdvEnEdition.setDateRdv(dateTime);
            rdvEnEdition.setLieu(lieu);
            rdvEnEdition.setNotes(notes);
            if (serviceRdv.modifier(rdvEnEdition)) {
                load(); exitEditMode(); formMessageLabel.setText("✅ Rendez-vous modifié.");
            } else {
                formMessageLabel.setText("Échec de la modification.");
            }
        } else {
            RendezVous rv = new RendezVous(currentPatient.getId(), medecinId, dateTime, lieu, notes);
            if (serviceRdv.ajouter(rv)) {
                load(); clearForm(); formMessageLabel.setText("✅ Demande envoyée ! En attente de confirmation.");
            } else {
                formMessageLabel.setText("Échec de l'envoi. Réessayez.");
            }
        }
    }

    @FXML private void onCancelEdit() { exitEditMode(); formMessageLabel.setText(""); }
    @FXML private void onRefresh()    { load(); formMessageLabel.setText(""); }

    // ── Edit mode ──────────────────────────────────────────────────────────

    private void enterEditMode(RendezVous rv) {
        rdvEnEdition = rv;
        formTitleLabel.setText("✏️ Modifier le rendez-vous");
        submitBtn.setText("💾 Modifier");
        cancelEditBtn.setVisible(true);
        cancelEditBtn.setManaged(true);

        Integer medecinId = rv.getMedecinId();
        if (medecinId != null) {
            // Find the doctor from all medecins to get their specialite
            Utilisateur medecin = serviceUser.getById(medecinId);
            if (medecin != null && medecin.getSpecialite() != null) {
                specialiteComboBox.setValue(medecin.getSpecialite());
                // After specialite is set the listener populates medecinComboBox, then select
                medecinComboBox.getItems().stream()
                        .filter(m -> m.getId() == medecinId)
                        .findFirst()
                        .ifPresent(medecinComboBox::setValue);
            }
        }
        datePicker.setValue(rv.getDateRdv().toLocalDate());
        timeField.setText(rv.getDateRdv().format(TIME_FMT));
        lieuField.setText(safe(rv.getLieu()));
        notesArea.setText(safe(rv.getNotes()));
        formMessageLabel.setText("Modifiez les champs puis cliquez sur Modifier.");
    }

    private void exitEditMode() {
        rdvEnEdition = null;
        formTitleLabel.setText("📅 Demander un rendez-vous");
        submitBtn.setText("📅 Envoyer la demande");
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);
        clearForm();
    }

    private void clearForm() {
        specialiteComboBox.setValue(null);
        medecinComboBox.getItems().clear();
        medecinComboBox.setValue(null);
        medecinCountLabel.setText("");
        datePicker.setValue(LocalDate.now().plusDays(1));
        timeField.clear(); lieuField.clear(); notesArea.clear();
    }

    private String safe(String v) { return v == null ? "" : v; }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
