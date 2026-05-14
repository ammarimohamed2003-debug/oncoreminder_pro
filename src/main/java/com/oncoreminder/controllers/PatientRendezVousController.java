package com.oncoreminder.controllers;

import com.oncoreminder.models.Ordonnance;
import com.oncoreminder.models.RendezVous;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceOrdonnance;
import com.oncoreminder.services.ServiceRendezVous;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class PatientRendezVousController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ORD_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private PatientSidebarController patientSidebarController;

    // ── Formulaire ────────────────────────────────────────────────────
    @FXML private Label formTitleLabel;
    @FXML private ComboBox<Utilisateur> medecinComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField lieuField;
    @FXML private TextArea notesArea;
    @FXML private Label formMessageLabel;
    @FXML private Button submitBtn;
    @FXML private Button cancelEditBtn;

    // ── Table RDV ─────────────────────────────────────────────────────
    @FXML private TableView<RendezVous> table;
    @FXML private TableColumn<RendezVous, String> colMedecin;
    @FXML private TableColumn<RendezVous, String> colDate;
    @FXML private TableColumn<RendezVous, String> colLieu;
    @FXML private TableColumn<RendezVous, String> colNotes;
    @FXML private TableColumn<RendezVous, String> colStatut;
    @FXML private TableColumn<RendezVous, Void>   colActions;

    // ── Stats ─────────────────────────────────────────────────────────
    @FXML private Label totalLabel;
    @FXML private Label attenteLabel;
    @FXML private Label accepteLabel;
    @FXML private Label statusLabel;

    // ── Table Ordonnances ─────────────────────────────────────────────
    @FXML private TableView<Ordonnance> ordTable;
    @FXML private TableColumn<Ordonnance, String> ordColDate;
    @FXML private TableColumn<Ordonnance, String> ordColMeds;
    @FXML private TableColumn<Ordonnance, String> ordColPoso;
    @FXML private TableColumn<Ordonnance, Number> ordColDuree;
    @FXML private TableColumn<Ordonnance, Void>   ordColActions;
    @FXML private Label ordStatusLabel;

    private final ServiceRendezVous serviceRdv   = new ServiceRendezVous();
    private final ServiceOrdonnance serviceOrd   = new ServiceOrdonnance();
    private final ServiceUtilisateur serviceUser = new ServiceUtilisateur();
    private final ObservableList<RendezVous>  rdvItems = FXCollections.observableArrayList();
    private final ObservableList<Ordonnance>  ordItems = FXCollections.observableArrayList();
    private Utilisateur currentPatient;
    private RendezVous rdvEnEdition = null;

    @FXML
    public void initialize() {
        currentPatient = UserSession.getInstance().getCurrentUser();
        if (patientSidebarController != null)
            patientSidebarController.setActivePage("rendezvous");

        setupMedecinComboBox();
        setupRdvTable();
        setupOrdTable();

        datePicker.setValue(LocalDate.now().plusDays(1));
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);

        load();
    }

    // ── Setup ─────────────────────────────────────────────────────────

    private void setupMedecinComboBox() {
        medecinComboBox.setItems(FXCollections.observableArrayList(serviceUser.getAllMedecins()));
        medecinComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? "Choisir un médecin..." : "Dr. " + u.getPrenom() + " " + u.getNom());
            }
        });
        medecinComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? "" : "Dr. " + u.getPrenom() + " " + u.getNom());
            }
        });
    }

    private void setupRdvTable() {
        colMedecin.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getMedecinNom().isBlank() ? "—" : "Dr. " + d.getValue().getMedecinNom()));
        colDate.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateRdv() != null ? d.getValue().getDateRdv().format(DATE_FMT) : ""));
        colLieu.setCellValueFactory(d -> new ReadOnlyStringWrapper(safe(d.getValue().getLieu())));
        colNotes.setCellValueFactory(d -> new ReadOnlyStringWrapper(safe(d.getValue().getNotes())));
        colStatut.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatutLabel()));

        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                RendezVous rv = getTableView().getItems().get(getIndex());
                setStyle(switch (rv.getStatut()) {
                    case EN_ATTENTE -> "-fx-text-fill: #D97706; -fx-font-weight: bold;";
                    case ACCEPTE    -> "-fx-text-fill: #059669; -fx-font-weight: bold;";
                    case REFUSE     -> "-fx-text-fill: #DC2626; -fx-font-weight: bold;";
                });
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏️");
            private final Button btnCancel = new Button("✕");
            private final HBox box = new HBox(4, btnEdit, btnCancel);
            {
                btnEdit.setStyle("-fx-background-color:#5B35A5;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:3 8;-fx-cursor:hand;");
                btnCancel.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:3 8;-fx-cursor:hand;");
                box.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(e -> {
                    RendezVous rv = getTableView().getItems().get(getIndex());
                    if (rv.getStatut() == RendezVous.Statut.EN_ATTENTE) enterEditMode(rv);
                    else formMessageLabel.setText("Seuls les rendez-vous en attente peuvent être modifiés.");
                });
                btnCancel.setOnAction(e -> {
                    RendezVous rv = getTableView().getItems().get(getIndex());
                    if (rv.getStatut() != RendezVous.Statut.EN_ATTENTE) {
                        formMessageLabel.setText("Seuls les rendez-vous en attente peuvent être annulés."); return;
                    }
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Annuler ce rendez-vous ?", ButtonType.YES, ButtonType.NO);
                    a.setHeaderText("Confirmation");
                    if (a.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                        if (serviceRdv.supprimer(rv.getId())) { load(); formMessageLabel.setText("Rendez-vous annulé."); }
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                RendezVous rv = getTableView().getItems().get(getIndex());
                btnEdit.setVisible(rv.getStatut() == RendezVous.Statut.EN_ATTENTE);
                btnCancel.setVisible(rv.getStatut() == RendezVous.Statut.EN_ATTENTE);
                setGraphic(box);
            }
        });

        table.setItems(rdvItems);
    }

    private void setupOrdTable() {
        ordColDate.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateEmission() != null ? d.getValue().getDateEmission().format(ORD_DATE) : ""));
        ordColMeds.setCellValueFactory(d -> new ReadOnlyStringWrapper(safe(d.getValue().getMedicaments())));
        ordColPoso.setCellValueFactory(d -> new ReadOnlyStringWrapper(safe(d.getValue().getPosologie())));
        ordColDuree.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getDureeJours()));

        ordColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPdf = new Button("📄 PDF");
            {
                btnPdf.setStyle("-fx-background-color:#4A2D8F;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:4 10;-fx-cursor:hand;");
                btnPdf.setOnAction(e -> {
                    Ordonnance ord = getTableView().getItems().get(getIndex());
                    RendezVous rdv = serviceRdv.getById(ord.getRendezVousId());
                    OrdonnanceController.exportOrdonnancePdf(ord, rdv, null);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnPdf);
            }
        });

        ordTable.setItems(ordItems);
    }

    // ── Load ──────────────────────────────────────────────────────────

    private void load() {
        if (currentPatient == null) return;
        rdvItems.setAll(serviceRdv.getByPatient(currentPatient.getId()));
        ordItems.setAll(serviceOrd.getByPatient(currentPatient.getId()));

        long attente = rdvItems.stream().filter(rv -> rv.getStatut() == RendezVous.Statut.EN_ATTENTE).count();
        long accepte = rdvItems.stream().filter(rv -> rv.getStatut() == RendezVous.Statut.ACCEPTE).count();
        totalLabel.setText(String.valueOf(rdvItems.size()));
        attenteLabel.setText(String.valueOf(attente));
        accepteLabel.setText(String.valueOf(accepte));
        statusLabel.setText(rdvItems.size() + " rendez-vous");
        ordStatusLabel.setText(ordItems.size() + " ordonnance(s)");
    }

    // ── Actions formulaire ────────────────────────────────────────────

    @FXML
    private void onSubmit() {
        if (currentPatient == null) { formMessageLabel.setText("Erreur : utilisateur non connecté."); return; }
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
            // Edit mode
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
            // New rdv
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

    // ── Edit mode helpers ─────────────────────────────────────────────

    private void enterEditMode(RendezVous rv) {
        rdvEnEdition = rv;
        formTitleLabel.setText("✏️ Modifier le rendez-vous");
        submitBtn.setText("💾 Modifier");
        cancelEditBtn.setVisible(true);
        cancelEditBtn.setManaged(true);

        // Pre-fill form
        Integer medecinId = rv.getMedecinId();
        if (medecinId != null) {
            medecinComboBox.getItems().stream()
                    .filter(m -> m.getId() == medecinId)
                    .findFirst()
                    .ifPresent(medecinComboBox::setValue);
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
        medecinComboBox.setValue(null);
        datePicker.setValue(LocalDate.now().plusDays(1));
        timeField.clear(); lieuField.clear(); notesArea.clear();
    }

    private String safe(String v) { return v == null ? "" : v; }
}
