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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PatientRendezVousController {

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("EEEE dd MMM yyyy 'à' HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT   = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ORD_DATE   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private PatientSidebarController patientSidebarController;

    // Formulaire
    @FXML private Label     formTitleLabel;
    @FXML private ComboBox<String>      specialiteComboBox;
    @FXML private ComboBox<Utilisateur> medecinComboBox;
    @FXML private Label     medecinCountLabel;
    @FXML private HBox      doctorPreviewBox;
    @FXML private Label     doctorAvatarLabel;
    @FXML private Label     doctorNameLabel;
    @FXML private Label     doctorSpecLabel;
    @FXML private DatePicker datePicker;
    @FXML private TextField  timeField;
    @FXML private Label      dateHintLabel;
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
    @FXML private Label histStatusLabel;
    @FXML private Label ordStatusLabel;

    // Card containers
    @FXML private VBox  rdvCardsBox;
    @FXML private Label rdvEmptyLabel;
    @FXML private VBox  histCardsBox;
    @FXML private Label histEmptyLabel;
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
        setupDateHint();
        datePicker.setValue(LocalDate.now().plusDays(1));
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);
        load();
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private void setupSpecialiteComboBox() {
        specialiteComboBox.getItems().setAll(serviceUser.getAllSpecialites());
        specialiteComboBox.valueProperty().addListener((obs, old, selected) -> {
            if (selected == null) {
                medecinComboBox.getItems().clear();
                medecinComboBox.setValue(null);
                medecinCountLabel.setText("");
                hideDoctorPreview();
                return;
            }
            List<Utilisateur> medecins = serviceUser.getMedecinsBySpecialite(selected);
            medecinComboBox.getItems().setAll(medecins);
            medecinComboBox.setValue(null);
            hideDoctorPreview();
            int n = medecins.size();
            medecinCountLabel.setText(n == 0 ? "Aucun médecin disponible" : n + " médecin(s)");
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
                setText("Dr. " + u.getPrenom() + " " + u.getNom());
            }
        });
        medecinComboBox.valueProperty().addListener((obs, old, selected) -> {
            if (selected == null) { hideDoctorPreview(); return; }
            showDoctorPreview(selected);
        });
    }

    private void setupDateHint() {
        datePicker.valueProperty().addListener((obs, old, date) -> updateDateHint(date));
        updateDateHint(datePicker.getValue());
    }

    private void updateDateHint(LocalDate date) {
        if (date == null) { dateHintLabel.setText(""); return; }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (days < 0) {
            dateHintLabel.setText("⚠ Date dans le passé");
            dateHintLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#DC2626;-fx-font-weight:bold;");
        } else if (days == 0) {
            dateHintLabel.setText("📌 Aujourd'hui");
            dateHintLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#D97706;-fx-font-weight:bold;");
        } else if (days == 1) {
            dateHintLabel.setText("✅ Demain");
            dateHintLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#059669;-fx-font-weight:bold;");
        } else {
            dateHintLabel.setText("📅 Dans " + days + " jours");
            dateHintLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#059669;-fx-font-weight:bold;");
        }
    }

    // ── Doctor preview ─────────────────────────────────────────────────────

    private void showDoctorPreview(Utilisateur u) {
        String initiale = (u.getPrenom() != null && !u.getPrenom().isEmpty())
                ? String.valueOf(u.getPrenom().charAt(0)).toUpperCase() : "?";
        doctorAvatarLabel.setText(initiale);
        doctorNameLabel.setText("Dr. " + u.getPrenom() + " " + u.getNom());
        doctorSpecLabel.setText(u.getSpecialite() != null ? u.getSpecialite() : "");
        doctorPreviewBox.setVisible(true);
        doctorPreviewBox.setManaged(true);
    }

    private void hideDoctorPreview() {
        doctorPreviewBox.setVisible(false);
        doctorPreviewBox.setManaged(false);
    }

    // ── Load ───────────────────────────────────────────────────────────────

    private void load() {
        if (currentPatient == null) return;
        List<RendezVous> rdvList = serviceRdv.getByPatient(currentPatient.getId());
        List<Ordonnance> ordList = serviceOrd.getByPatient(currentPatient.getId());

        long attente = rdvList.stream().filter(r -> r.getStatut() == RendezVous.Statut.EN_ATTENTE).count();
        long accepte = rdvList.stream().filter(r -> r.getStatut() == RendezVous.Statut.ACCEPTE).count();
        totalLabel.setText(String.valueOf(rdvList.size()));
        attenteLabel.setText(String.valueOf(attente));
        accepteLabel.setText(String.valueOf(accepte));
        ordStatusLabel.setText(ordList.isEmpty() ? "" : ordList.size() + " ordonnance(s)");

        // Split upcoming vs past
        LocalDateTime now = LocalDateTime.now();
        List<RendezVous> upcoming = rdvList.stream()
                .filter(r -> r.getDateRdv() != null && !r.getDateRdv().isBefore(now))
                .sorted(Comparator.comparing(RendezVous::getDateRdv))
                .collect(Collectors.toList());
        List<RendezVous> past = rdvList.stream()
                .filter(r -> r.getDateRdv() == null || r.getDateRdv().isBefore(now))
                .sorted(Comparator.comparing(RendezVous::getDateRdv, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        statusLabel.setText(upcoming.isEmpty() ? "" : upcoming.size() + " rendez-vous");
        histStatusLabel.setText(past.isEmpty() ? "" : past.size() + " passé(s)");

        buildRdvCards(rdvCardsBox, rdvEmptyLabel, upcoming, false);
        buildRdvCards(histCardsBox, histEmptyLabel, past, true);
        buildOrdCards(ordList);
    }

    // ── Card builders ──────────────────────────────────────────────────────

    private void buildRdvCards(VBox box, Label emptyLabel, List<RendezVous> list, boolean isPast) {
        box.getChildren().clear();
        if (list.isEmpty()) { box.getChildren().add(emptyLabel); return; }
        for (RendezVous rv : list) box.getChildren().add(buildRdvCard(rv, isPast));
    }

    private HBox buildRdvCard(RendezVous rv, boolean isPast) {
        String stripeColor = switch (rv.getStatut()) {
            case EN_ATTENTE -> "#D97706";
            case ACCEPTE    -> "#059669";
            case REFUSE     -> "#94A3B8";
        };

        Region stripe = new Region();
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);
        stripe.setStyle("-fx-background-color:" + stripeColor + ";-fx-background-radius:10 0 0 10;");

        VBox content = new VBox(6);
        content.setStyle("-fx-padding:12 14 12 14;");
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: doctor name + status badge
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        String docName = rv.getMedecinNom() != null && !rv.getMedecinNom().isBlank()
                ? "Dr. " + rv.getMedecinNom() : "Médecin non précisé";
        Label lblDoc = new Label("🩺 " + docName);
        lblDoc.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + (isPast && rv.getStatut() == RendezVous.Statut.REFUSE ? "#94A3B8" : "#1E293B") + ";");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        // Countdown badge
        if (rv.getDateRdv() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), rv.getDateRdv().toLocalDate());
            String countdownText;
            String countdownStyle;
            if (!isPast) {
                if (days == 0) {
                    countdownText = "Aujourd'hui";
                    countdownStyle = "-fx-background-color:#FEF3C7;-fx-text-fill:#D97706;-fx-font-weight:bold;";
                } else if (days == 1) {
                    countdownText = "Demain";
                    countdownStyle = "-fx-background-color:#D1FAE5;-fx-text-fill:#059669;-fx-font-weight:bold;";
                } else {
                    countdownText = "Dans " + days + " j";
                    countdownStyle = "-fx-background-color:#EEF2FF;-fx-text-fill:#4A2D8F;-fx-font-weight:bold;";
                }
            } else {
                long ago = Math.abs(days);
                countdownText = ago == 0 ? "Aujourd'hui" : "Il y a " + ago + " j";
                countdownStyle = "-fx-background-color:#F1F5F9;-fx-text-fill:#94A3B8;";
            }
            Label countdown = new Label(countdownText);
            countdown.setStyle("-fx-font-size:10px;-fx-background-radius:10;-fx-padding:3 8;" + countdownStyle);
            row1.getChildren().addAll(lblDoc, sp1, countdown);
        } else {
            row1.getChildren().addAll(lblDoc, sp1);
        }

        // Row 2: status badge + date
        HBox row2 = new HBox(8);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(rv.getStatutLabel());
        badge.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:white;-fx-background-color:" + stripeColor + ";-fx-background-radius:10;-fx-padding:2 8;");
        String dateStr = rv.getDateRdv() != null ? capitalize(rv.getDateRdv().format(DATE_FMT)) : "—";
        Label lblDate = new Label("📅 " + dateStr);
        lblDate.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
        row2.getChildren().addAll(badge, lblDate);

        VBox rows = new VBox(5, row1, row2);

        if (rv.getLieu() != null && !rv.getLieu().isBlank()) {
            Label lblLieu = new Label("📍 " + rv.getLieu());
            lblLieu.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
            rows.getChildren().add(lblLieu);
        }
        if (rv.getNotes() != null && !rv.getNotes().isBlank()) {
            Label lblNotes = new Label("💬 " + rv.getNotes());
            lblNotes.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;-fx-font-style:italic;");
            lblNotes.setWrapText(true);
            rows.getChildren().add(lblNotes);
        }
        content.getChildren().add(rows);

        // Action buttons (EN_ATTENTE only)
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
                if (a.showAndWait().filter(b -> b == ButtonType.YES).isPresent())
                    if (serviceRdv.supprimer(rv.getId())) { load(); formMessageLabel.setText("Rendez-vous annulé."); }
            });
            actions.getChildren().addAll(btnEdit, btnCancel);
            content.getChildren().add(actions);
        }

        String cardBg = (isPast && rv.getStatut() == RendezVous.Statut.REFUSE) ? "#F8FAFC" : "white";
        HBox card = new HBox(stripe, content);
        card.setStyle(
            "-fx-background-color:" + cardBg + ";" +
            "-fx-background-radius:10;" +
            "-fx-border-color:#E2E8F0;" +
            "-fx-border-radius:10;" +
            "-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);"
        );
        if (isPast) card.setOpacity(rv.getStatut() == RendezVous.Statut.REFUSE ? 0.65 : 0.85);
        return card;
    }

    private void buildOrdCards(List<Ordonnance> list) {
        ordCardsBox.getChildren().clear();
        if (list.isEmpty()) { ordCardsBox.getChildren().add(ordEmptyLabel); return; }
        // Most recent first
        list.stream()
            .sorted(Comparator.comparing(Ordonnance::getDateEmission, Comparator.nullsLast(Comparator.reverseOrder())))
            .forEach(ord -> ordCardsBox.getChildren().add(buildOrdCard(ord)));
    }

    private HBox buildOrdCard(Ordonnance ord) {
        Region stripe = new Region();
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);
        stripe.setStyle("-fx-background-color:#4A2D8F;-fx-background-radius:10 0 0 10;");

        VBox content = new VBox(8);
        content.setStyle("-fx-padding:12 14 12 14;");
        HBox.setHgrow(content, Priority.ALWAYS);

        // Row 1: title + date badge
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Ordonnance #" + ord.getId());
        lblTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#4A2D8F;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        String dateStr = ord.getDateEmission() != null ? ord.getDateEmission().format(ORD_DATE) : "—";
        Label lblDate = new Label("📅 " + dateStr);
        lblDate.setStyle("-fx-font-size:10px;-fx-text-fill:#6B7280;-fx-background-color:#EDE9F8;-fx-background-radius:10;-fx-padding:3 8;");
        row1.getChildren().addAll(lblTitle, sp, lblDate);

        // Médicaments en chips
        VBox medsBox = new VBox(5);
        Label medsTitle = new Label("MÉDICAMENTS");
        medsTitle.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#94A3B8;-fx-letter-spacing:1;");
        FlowPane chips = buildMedChips(safe(ord.getMedicaments()));
        medsBox.getChildren().addAll(medsTitle, chips);

        // Posologie
        VBox posoBox = new VBox(3);
        Label posoTitle = new Label("POSOLOGIE");
        posoTitle.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#94A3B8;");
        Label lblPoso = new Label(safe(ord.getPosologie()));
        lblPoso.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");
        lblPoso.setWrapText(true);
        posoBox.getChildren().addAll(posoTitle, lblPoso);

        // Footer: durée + expiration + PDF
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label lblDuree = new Label("⏱ " + ord.getDureeJours() + " jours");
        lblDuree.setStyle("-fx-font-size:11px;-fx-text-fill:#4A2D8F;-fx-background-color:#EDE9F8;-fx-background-radius:10;-fx-padding:3 8;");

        // Expiration calculation
        if (ord.getDateEmission() != null) {
            LocalDate expiry = ord.getDateEmission().plusDays(ord.getDureeJours());
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
            Label lblExp;
            if (daysLeft < 0) {
                lblExp = new Label("Expirée");
                lblExp.setStyle("-fx-font-size:10px;-fx-text-fill:#DC2626;-fx-background-color:#FEE2E2;-fx-background-radius:10;-fx-padding:3 8;");
            } else if (daysLeft <= 3) {
                lblExp = new Label("Expire dans " + daysLeft + " j");
                lblExp.setStyle("-fx-font-size:10px;-fx-text-fill:#D97706;-fx-background-color:#FEF3C7;-fx-background-radius:10;-fx-padding:3 8;");
            } else {
                lblExp = new Label("Valide jusqu'au " + expiry.format(DATE_SHORT));
                lblExp.setStyle("-fx-font-size:10px;-fx-text-fill:#059669;-fx-background-color:#D1FAE5;-fx-background-radius:10;-fx-padding:3 8;");
            }
            footer.getChildren().addAll(lblDuree, lblExp);
        } else {
            footer.getChildren().add(lblDuree);
        }

        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Button btnPdf = new Button("📄 PDF");
        btnPdf.setStyle("-fx-background-color:#4A2D8F;-fx-text-fill:white;-fx-background-radius:8;-fx-font-size:11px;-fx-padding:5 14;-fx-cursor:hand;");
        btnPdf.setOnAction(e -> {
            RendezVous rdv = serviceRdv.getById(ord.getRendezVousId());
            OrdonnanceController.exportOrdonnancePdf(ord, rdv, null);
        });
        footer.getChildren().addAll(sp2, btnPdf);

        content.getChildren().addAll(row1, medsBox, posoBox, footer);

        HBox card = new HBox(stripe, content);
        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:10;" +
            "-fx-border-color:#DDD6F5;" +
            "-fx-border-radius:10;" +
            "-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(74,45,143,0.07),6,0,0,2);"
        );
        return card;
    }

    private FlowPane buildMedChips(String meds) {
        FlowPane flow = new FlowPane();
        flow.setHgap(6);
        flow.setVgap(5);
        if (meds.isBlank()) {
            Label lbl = new Label("—");
            lbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
            flow.getChildren().add(lbl);
            return flow;
        }
        String[] parts = meds.split("[,\n]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            Label chip = new Label(trimmed);
            chip.setStyle(
                "-fx-font-size:11px;-fx-text-fill:#4A2D8F;" +
                "-fx-background-color:#EDE9F8;" +
                "-fx-background-radius:12;" +
                "-fx-padding:3 10;"
            );
            flow.getChildren().add(chip);
        }
        return flow;
    }

    // ── Actions formulaire ─────────────────────────────────────────────────

    @FXML
    private void onSubmit() {
        if (currentPatient == null) { showError("Erreur : utilisateur non connecté."); return; }
        if (specialiteComboBox.getValue() == null) { showError("Choisissez une spécialité."); return; }
        if (medecinComboBox.getValue() == null) { showError("Choisissez un médecin."); return; }
        if (datePicker.getValue() == null || timeField.getText().isBlank()) {
            showError("La date et l'heure sont obligatoires."); return;
        }
        LocalTime time;
        try { time = LocalTime.parse(timeField.getText().trim(), TIME_FMT); }
        catch (DateTimeParseException e) { showError("Format heure invalide (ex: 10:30)."); return; }

        LocalDateTime dateTime = LocalDateTime.of(datePicker.getValue(), time);
        if (dateTime.isBefore(LocalDateTime.now())) { showError("La date doit être dans le futur."); return; }

        String lieu  = lieuField.getText().isBlank()  ? null : lieuField.getText().trim();
        String notes = notesArea.getText().isBlank()   ? null : notesArea.getText().trim();
        int medecinId = medecinComboBox.getValue().getId();

        if (rdvEnEdition != null) {
            rdvEnEdition.setMedecinId(medecinId);
            rdvEnEdition.setDateRdv(dateTime);
            rdvEnEdition.setLieu(lieu);
            rdvEnEdition.setNotes(notes);
            if (serviceRdv.modifier(rdvEnEdition)) {
                load(); exitEditMode(); showSuccess("✅ Rendez-vous modifié avec succès.");
            } else {
                showError("Échec de la modification.");
            }
        } else {
            RendezVous rv = new RendezVous(currentPatient.getId(), medecinId, dateTime, lieu, notes);
            if (serviceRdv.ajouter(rv)) {
                load(); clearForm(); showSuccess("✅ Demande envoyée ! En attente de confirmation du médecin.");
            } else {
                showError("Échec de l'envoi. Réessayez.");
            }
        }
    }

    @FXML private void onCancelEdit() { exitEditMode(); formMessageLabel.setText(""); }
    @FXML private void onRefresh()    { load(); formMessageLabel.setText(""); }

    // ── Edit mode ──────────────────────────────────────────────────────────

    private void enterEditMode(RendezVous rv) {
        rdvEnEdition = rv;
        formTitleLabel.setText("Modifier le rendez-vous");
        submitBtn.setText("💾 Enregistrer les modifications");
        cancelEditBtn.setVisible(true);
        cancelEditBtn.setManaged(true);

        Integer medecinId = rv.getMedecinId();
        if (medecinId != null) {
            Utilisateur medecin = serviceUser.getById(medecinId);
            if (medecin != null && medecin.getSpecialite() != null) {
                specialiteComboBox.setValue(medecin.getSpecialite());
                medecinComboBox.getItems().stream()
                        .filter(m -> m.getId() == medecinId)
                        .findFirst().ifPresent(medecinComboBox::setValue);
            }
        }
        datePicker.setValue(rv.getDateRdv().toLocalDate());
        timeField.setText(rv.getDateRdv().format(TIME_FMT));
        lieuField.setText(safe(rv.getLieu()));
        notesArea.setText(safe(rv.getNotes()));
        showInfo("Modifiez les champs puis cliquez sur Enregistrer.");
    }

    private void exitEditMode() {
        rdvEnEdition = null;
        formTitleLabel.setText("Nouveau rendez-vous");
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
        hideDoctorPreview();
        datePicker.setValue(LocalDate.now().plusDays(1));
        timeField.clear(); lieuField.clear(); notesArea.clear();
        formMessageLabel.setText("");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void showSuccess(String msg) {
        formMessageLabel.setText(msg);
        formMessageLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#059669;-fx-font-weight:bold;");
    }

    private void showError(String msg) {
        formMessageLabel.setText(msg);
        formMessageLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#DC2626;-fx-font-weight:bold;");
    }

    private void showInfo(String msg) {
        formMessageLabel.setText(msg);
        formMessageLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#4A2D8F;-fx-font-weight:bold;");
    }

    private String safe(String v) { return v == null ? "" : v; }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
