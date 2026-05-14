package com.oncoreminder.controllers;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.oncoreminder.models.Ordonnance;
import com.oncoreminder.models.RendezVous;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceOrdonnance;
import com.oncoreminder.services.ServiceRendezVous;
import com.oncoreminder.utils.UserSession;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;


public class OrdonnanceController {

    private static final DateTimeFormatter DATE_FMT      = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_DISPLAY  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private DoctorSidebarController doctorSidebarController;

    @FXML private Label selectedIdLabel;
    @FXML private ComboBox<RendezVous> rdvComboBox;
    @FXML private DatePicker issueDatePicker;
    @FXML private TextField durationField;
    @FXML private TextArea medicationArea;
    @FXML private TextArea dosageArea;
    @FXML private Label formMessageLabel;

    @FXML private TableView<Ordonnance> prescriptionTable;
    @FXML private TableColumn<Ordonnance, Number> idColumn;
    @FXML private TableColumn<Ordonnance, Number> rendezVousColumn;
    @FXML private TableColumn<Ordonnance, String> medicamentsColumn;
    @FXML private TableColumn<Ordonnance, String> posologieColumn;
    @FXML private TableColumn<Ordonnance, Number> durationColumn;
    @FXML private TableColumn<Ordonnance, String> emissionDateColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;

    @FXML private Label todayCountLabel;
    @FXML private Label weekCountLabel;
    @FXML private Label totalCountLabel;
    @FXML private Label footerStatusLabel;

    private final ServiceOrdonnance serviceOrd = new ServiceOrdonnance();
    private final ServiceRendezVous serviceRdv = new ServiceRendezVous();
    private final ObservableList<Ordonnance> master   = FXCollections.observableArrayList();
    private final ObservableList<Ordonnance> filtered = FXCollections.observableArrayList();
    private Ordonnance selectedOrdonnance;
    private Utilisateur currentDoctor;

    @FXML
    public void initialize() {
        currentDoctor = UserSession.getInstance().getCurrentUser();
        if (doctorSidebarController != null)
            doctorSidebarController.setActivePage("ordonnance");

        setupRdvComboBox();
        setupTable();
        setupFilters();
        load();
    }

    private void setupRdvComboBox() {
        int medecinId = currentDoctor != null ? currentDoctor.getId() : 0;
        List<RendezVous> acceptes = serviceRdv.getAcceptesForDoctor(medecinId);
        rdvComboBox.setItems(FXCollections.observableArrayList(acceptes));
        rdvComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(RendezVous rv, boolean empty) {
                super.updateItem(rv, empty);
                setText(empty || rv == null ? "Sélectionner un rendez-vous accepté" : rv + " — " + rv.getPatientNom());
            }
        });
        rdvComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(RendezVous rv, boolean empty) {
                super.updateItem(rv, empty); setText(empty || rv == null ? "" : rv + " — " + rv.getPatientNom());
            }
        });
        if (acceptes.isEmpty()) formMessageLabel.setText("Aucun rendez-vous accepté disponible.");
    }

    private void setupTable() {
        idColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getId()));
        rendezVousColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getRendezVousId()));
        medicamentsColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(safe(d.getValue().getMedicaments())));
        posologieColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(safe(d.getValue().getPosologie())));
        durationColumn.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getDureeJours()));
        emissionDateColumn.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateEmission() != null ? d.getValue().getDateEmission().format(DATE_FMT) : ""));
        prescriptionTable.setItems(filtered);
        prescriptionTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) { selectedOrdonnance = sel; fillForm(sel); }
        });
    }

    private void setupFilters() {
        filterComboBox.setItems(FXCollections.observableArrayList("Toutes", "Aujourd'hui", "Cette semaine", "Ce mois", "Anciennes"));
        filterComboBox.setValue("Toutes");
        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        filterComboBox.valueProperty().addListener((o, a, b) -> applyFilters());
    }

    private void load() {
        int medecinId = currentDoctor != null ? currentDoctor.getId() : 0;
        master.setAll(serviceOrd.getAllForDoctor(medecinId));
        applyFilters();
        updateStats();
        footerStatusLabel.setText(master.size() + " ordonnances chargées.");
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String period = filterComboBox.getValue() == null ? "Toutes" : filterComboBox.getValue();
        LocalDate today = LocalDate.now();
        filtered.setAll(master.stream()
                .filter(o -> o.getDateEmission() != null)
                .filter(o -> switch (period) {
                    case "Aujourd'hui"   -> o.getDateEmission().equals(today);
                    case "Cette semaine" -> !o.getDateEmission().isBefore(today) && !o.getDateEmission().isAfter(today.plusDays(6));
                    case "Ce mois"       -> o.getDateEmission().getMonth() == today.getMonth() && o.getDateEmission().getYear() == today.getYear();
                    case "Anciennes"     -> o.getDateEmission().isBefore(today);
                    default -> true;
                })
                .filter(o -> search.isEmpty()
                        || String.valueOf(o.getId()).contains(search)
                        || safe(o.getMedicaments()).toLowerCase(Locale.ROOT).contains(search)
                        || safe(o.getPosologie()).toLowerCase(Locale.ROOT).contains(search))
                .toList());
    }

    private void updateStats() {
        LocalDate today = LocalDate.now();
        long t = master.stream().filter(o -> o.getDateEmission() != null && o.getDateEmission().equals(today)).count();
        long w = master.stream().filter(o -> o.getDateEmission() != null && !o.getDateEmission().isBefore(today) && !o.getDateEmission().isAfter(today.plusDays(6))).count();
        todayCountLabel.setText(String.format("%02d", t));
        weekCountLabel.setText(String.format("%02d", w));
        totalCountLabel.setText(String.format("%02d", master.size()));
    }

    @FXML private void onNew()     { clearForm(); formMessageLabel.setText("Nouvelle ordonnance."); setupRdvComboBox(); }
    @FXML private void onRefresh() { load(); setupRdvComboBox(); }

    @FXML
    private void onSave() {
        if (!valid()) return;
        Ordonnance o = fromForm();
        if (serviceOrd.ajouter(o)) { load(); formMessageLabel.setText("Ordonnance créée avec succès."); clearForm(); }
        else footerStatusLabel.setText("Échec de la création.");
    }

    @FXML
    private void onUpdate() {
        if (selectedOrdonnance == null) { formMessageLabel.setText("Sélectionnez une ordonnance à modifier."); return; }
        if (!valid()) return;
        Ordonnance o = fromForm(); o.setId(selectedOrdonnance.getId());
        if (serviceOrd.modifier(o)) { load(); formMessageLabel.setText("Ordonnance modifiée."); clearForm(); }
        else footerStatusLabel.setText("Échec de la modification.");
    }

    @FXML
    private void onDelete() {
        if (selectedOrdonnance == null) { formMessageLabel.setText("Sélectionnez une ordonnance à supprimer."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'ordonnance #" + selectedOrdonnance.getId() + " ?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Confirmation");
        if (a.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
            if (serviceOrd.supprimer(selectedOrdonnance.getId())) { load(); clearForm(); formMessageLabel.setText("Ordonnance supprimée."); }
            else footerStatusLabel.setText("Échec de la suppression.");
        }
    }

    @FXML
    private void onExport() {
        if (selectedOrdonnance == null) { formMessageLabel.setText("Sélectionnez une ordonnance à exporter."); return; }
        RendezVous rdv = serviceRdv.getById(selectedOrdonnance.getRendezVousId());
        exportOrdonnancePdf(selectedOrdonnance, rdv, currentDoctor);
    }

    public static void exportOrdonnancePdf(Ordonnance ord, RendezVous rdv, Utilisateur doctor) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("ordonnance_" + ord.getId() + ".pdf");
        File f = fc.showSaveDialog(null);
        if (f == null) return;

        try {
            Document doc = new Document(PageSize.A4, 60, 60, 80, 80);
            PdfWriter.getInstance(doc, new FileOutputStream(f));
            doc.open();

            Font brandFont   = new Font(Font.HELVETICA, 22, Font.BOLD,  new Color(74, 45, 143));
            Font subFont     = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);
            Font titleFont   = new Font(Font.HELVETICA, 15, Font.BOLD,  new Color(43, 62, 80));
            Font labelFont   = new Font(Font.HELVETICA, 11, Font.BOLD,  new Color(74, 45, 143));
            Font normalFont  = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
            Font smallFont   = new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.GRAY);

            // ── Header ──────────────────────────────────────────────
            Paragraph brand = new Paragraph("OncorReminder PRO", brandFont);
            brand.setAlignment(Element.ALIGN_LEFT);
            doc.add(brand);
            doc.add(new Paragraph("Système de gestion médicale oncologique", subFont));

            // Doctor info (right side via table)
            if (doctor != null) {
                PdfPTable headerTable = new PdfPTable(2);
                headerTable.setWidthPercentage(100);
                headerTable.setSpacingBefore(4);

                PdfPCell leftCell = new PdfPCell();
                leftCell.setBorder(Rectangle.NO_BORDER);
                leftCell.addElement(new Paragraph("Dr. " + doctor.getPrenom() + " " + doctor.getNom(), labelFont));
                leftCell.addElement(new Paragraph("Médecin — OncorReminder", smallFont));
                headerTable.addCell(leftCell);

                String dateStr = ord.getDateEmission() != null ? ord.getDateEmission().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                PdfPCell rightCell = new PdfPCell();
                rightCell.setBorder(Rectangle.NO_BORDER);
                rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                rightCell.addElement(new Paragraph("Date : " + dateStr, normalFont));
                headerTable.addCell(rightCell);

                doc.add(headerTable);
            }

            // Separator
            LineSeparator sep = new LineSeparator(1.5f, 100, new Color(74, 45, 143), Element.ALIGN_CENTER, -2);
            doc.add(new Chunk(sep));
            doc.add(Chunk.NEWLINE);

            // ── Title ────────────────────────────────────────────────
            Paragraph title = new Paragraph("ORDONNANCE MÉDICALE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16);
            doc.add(title);

            // ── Patient info ─────────────────────────────────────────
            PdfPTable patientTable = new PdfPTable(1);
            patientTable.setWidthPercentage(100);
            patientTable.setSpacingAfter(14);
            PdfPCell patientCell = new PdfPCell();
            patientCell.setBackgroundColor(new Color(240, 244, 255));
            patientCell.setPadding(10);
            patientCell.setBorderColor(new Color(180, 160, 220));
            String patientName = (rdv != null && !rdv.getPatientNom().isBlank()) ? rdv.getPatientNom() : "—";
            patientCell.addElement(new Paragraph("Patient : " + patientName, labelFont));
            patientTable.addCell(patientCell);
            doc.add(patientTable);

            // ── Prescriptions ─────────────────────────────────────────
            doc.add(new Paragraph("Médicaments prescrits :", labelFont));
            Paragraph meds = new Paragraph(safe(ord.getMedicaments()), normalFont);
            meds.setIndentationLeft(15);
            meds.setSpacingAfter(12);
            doc.add(meds);

            doc.add(new Paragraph("Posologie :", labelFont));
            Paragraph pos = new Paragraph(safe(ord.getPosologie()), normalFont);
            pos.setIndentationLeft(15);
            pos.setSpacingAfter(12);
            doc.add(pos);

            doc.add(new Paragraph("Durée du traitement : " + ord.getDureeJours() + " jour(s)", labelFont));
            doc.add(Chunk.NEWLINE);

            // ── Signature ────────────────────────────────────────────
            doc.add(new Chunk(new LineSeparator(0.5f, 100, Color.LIGHT_GRAY, Element.ALIGN_CENTER, -2)));
            doc.add(Chunk.NEWLINE);
            Paragraph sig = new Paragraph("Signature et cachet du médecin", smallFont);
            sig.setAlignment(Element.ALIGN_RIGHT);
            doc.add(sig);
            Paragraph sigLine = new Paragraph("_______________________________", normalFont);
            sigLine.setAlignment(Element.ALIGN_RIGHT);
            doc.add(sigLine);

            // ── Footer ───────────────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Document généré par OncorReminder PRO • Non valide sans signature", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            javafx.application.Platform.runLater(() ->
                new Alert(Alert.AlertType.INFORMATION, "PDF exporté : " + f.getName()).showAndWait());
        } catch (Exception e) {
            javafx.application.Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, "Erreur PDF : " + e.getMessage()).showAndWait());
        }
    }

    private boolean valid() {
        if (rdvComboBox.getValue() == null) { formMessageLabel.setText("Sélectionnez un rendez-vous accepté."); return false; }
        if (medicationArea.getText().isBlank()) { formMessageLabel.setText("Les médicaments sont obligatoires."); return false; }
        if (dosageArea.getText().isBlank()) { formMessageLabel.setText("La posologie est obligatoire."); return false; }
        if (durationField.getText().isBlank()) { formMessageLabel.setText("La durée est obligatoire."); return false; }
        if (issueDatePicker.getValue() == null) { formMessageLabel.setText("La date d'émission est obligatoire."); return false; }
        try { int d = Integer.parseInt(durationField.getText().trim()); if (d <= 0) { formMessageLabel.setText("Durée doit être > 0."); return false; } }
        catch (NumberFormatException e) { formMessageLabel.setText("Durée doit être un entier."); return false; }
        return true;
    }

    private Ordonnance fromForm() {
        return new Ordonnance(rdvComboBox.getValue().getId(), medicationArea.getText().trim(),
                dosageArea.getText().trim(), Integer.parseInt(durationField.getText().trim()), issueDatePicker.getValue());
    }

    private void fillForm(Ordonnance o) {
        selectedIdLabel.setText("Ordonnance #" + o.getId());
        medicationArea.setText(safe(o.getMedicaments()));
        dosageArea.setText(safe(o.getPosologie()));
        durationField.setText(String.valueOf(o.getDureeJours()));
        issueDatePicker.setValue(o.getDateEmission());
        int medecinId = currentDoctor != null ? currentDoctor.getId() : 0;
        RendezVous rdv = serviceRdv.getAcceptesForDoctor(medecinId).stream()
                .filter(r -> r.getId() == o.getRendezVousId()).findFirst().orElse(null);
        rdvComboBox.setValue(rdv);
    }

    private void clearForm() {
        selectedOrdonnance = null;
        selectedIdLabel.setText("Nouvelle ordonnance");
        rdvComboBox.setValue(null);
        medicationArea.clear(); dosageArea.clear(); durationField.clear();
        issueDatePicker.setValue(LocalDate.now());
        prescriptionTable.getSelectionModel().clearSelection();
        formMessageLabel.setText("");
    }

    private static String safe(String v) { return v == null ? "" : v; }
}