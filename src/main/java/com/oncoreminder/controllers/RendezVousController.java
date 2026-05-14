package com.oncoreminder.controllers;

import com.oncoreminder.models.RendezVous;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceRendezVous;
import com.oncoreminder.utils.UserSession;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class RendezVousController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private DoctorSidebarController doctorSidebarController;

    @FXML private Label totalLabel;
    @FXML private Label attenteLabel;
    @FXML private Label accepteLabel;
    @FXML private Label refuseLabel;

    @FXML private TableView<RendezVous> table;
    @FXML private TableColumn<RendezVous, Number>  colId;
    @FXML private TableColumn<RendezVous, String>  colPatient;
    @FXML private TableColumn<RendezVous, String>  colDate;
    @FXML private TableColumn<RendezVous, String>  colLieu;
    @FXML private TableColumn<RendezVous, String>  colNotes;
    @FXML private TableColumn<RendezVous, String>  colStatut;
    @FXML private TableColumn<RendezVous, Void>    colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label statusLabel;

    private final ServiceRendezVous service = new ServiceRendezVous();
    private final ObservableList<RendezVous> master   = FXCollections.observableArrayList();
    private final ObservableList<RendezVous> filtered = FXCollections.observableArrayList();
    private Utilisateur currentDoctor;

    @FXML
    public void initialize() {
        currentDoctor = UserSession.getInstance().getCurrentUser();
        if (doctorSidebarController != null)
            doctorSidebarController.setActivePage("rendezvous");

        filterStatut.setItems(FXCollections.observableArrayList("Tous", "En attente", "Acceptés", "Refusés"));
        filterStatut.setValue("Tous");

        setupTable();
        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        filterStatut.valueProperty().addListener((o, a, b) -> applyFilters());

        load();
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getId()));
        colPatient.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPatientNom()));
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
            private final Button btnAccepter = new Button("✅ Accepter");
            private final Button btnRefuser  = new Button("❌ Refuser");
            private final Button btnSuppr    = new Button("🗑");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnAccepter, btnRefuser, btnSuppr);
            {
                btnAccepter.setStyle("-fx-background-color:#059669;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:4 8;-fx-cursor:hand;");
                btnRefuser.setStyle("-fx-background-color:#DC2626;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:4 8;-fx-cursor:hand;");
                btnSuppr.setStyle("-fx-background-color:#6B7280;-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11px;-fx-padding:4 8;-fx-cursor:hand;");
                box.setAlignment(Pos.CENTER);
                btnAccepter.setOnAction(e -> {
                    RendezVous rv = getTableView().getItems().get(getIndex());
                    if (rv.getStatut() == RendezVous.Statut.EN_ATTENTE) {
                        int docId = currentDoctor != null ? currentDoctor.getId() : 0;
                        if (service.accepter(rv.getId(), docId)) { load(); statusLabel.setText("Rendez-vous #" + rv.getId() + " accepté."); }
                    } else { statusLabel.setText("Ce rendez-vous n'est pas en attente."); }
                });
                btnRefuser.setOnAction(e -> {
                    RendezVous rv = getTableView().getItems().get(getIndex());
                    if (rv.getStatut() == RendezVous.Statut.EN_ATTENTE) {
                        if (service.refuser(rv.getId())) { load(); statusLabel.setText("Rendez-vous #" + rv.getId() + " refusé."); }
                    } else { statusLabel.setText("Ce rendez-vous n'est pas en attente."); }
                });
                btnSuppr.setOnAction(e -> {
                    RendezVous rv = getTableView().getItems().get(getIndex());
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce rendez-vous ?", ButtonType.YES, ButtonType.NO);
                    a.setHeaderText("Confirmation");
                    if (a.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                        if (service.supprimer(rv.getId())) { load(); statusLabel.setText("Rendez-vous supprimé."); }
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.setItems(filtered);
    }

    private void load() {
        int medecinId = currentDoctor != null ? currentDoctor.getId() : 0;
        master.setAll(service.getAllForDoctor(medecinId));
        applyFilters();
        updateStats();
        statusLabel.setText(master.size() + " rendez-vous chargés.");
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String statut = filterStatut.getValue();
        filtered.setAll(master.stream()
                .filter(rv -> statut == null || statut.equals("Tous")
                        || (statut.equals("En attente") && rv.getStatut() == RendezVous.Statut.EN_ATTENTE)
                        || (statut.equals("Acceptés")   && rv.getStatut() == RendezVous.Statut.ACCEPTE)
                        || (statut.equals("Refusés")    && rv.getStatut() == RendezVous.Statut.REFUSE))
                .filter(rv -> search.isEmpty()
                        || rv.getPatientNom().toLowerCase(Locale.ROOT).contains(search)
                        || safe(rv.getLieu()).toLowerCase(Locale.ROOT).contains(search)
                        || safe(rv.getNotes()).toLowerCase(Locale.ROOT).contains(search))
                .toList());
    }

    private void updateStats() {
        totalLabel.setText(String.valueOf(master.size()));
        attenteLabel.setText(String.valueOf(master.stream().filter(rv -> rv.getStatut() == RendezVous.Statut.EN_ATTENTE).count()));
        accepteLabel.setText(String.valueOf(master.stream().filter(rv -> rv.getStatut() == RendezVous.Statut.ACCEPTE).count()));
        refuseLabel.setText(String.valueOf(master.stream().filter(rv -> rv.getStatut() == RendezVous.Statut.REFUSE).count()));
    }

    @FXML private void onRefresh() { load(); }

    @FXML
    private void onExportExcel() {
        if (filtered.isEmpty()) { statusLabel.setText("Aucun rendez-vous à exporter."); return; }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (.xls)", "*.xls"));
        String docName = currentDoctor != null ? currentDoctor.getNom() + "_" + currentDoctor.getPrenom() : "medecin";
        fc.setInitialFileName("rendez_vous_" + docName + ".xls");
        Window w = table.getScene() == null ? null : table.getScene().getWindow();
        File f = fc.showSaveDialog(w);
        if (f == null) return;

        try (FileWriter fw = new FileWriter(f, StandardCharsets.UTF_8)) {
            // SpreadsheetML — natively opened by Excel without any extra library
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            fw.write("<?mso-application progid=\"Excel.Sheet\"?>\n");
            fw.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            fw.write("  xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
            fw.write("  <Styles>\n");
            fw.write("    <Style ss:ID=\"header\"><Font ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#4A2D8F\" ss:Pattern=\"Solid\"/></Style>\n");
            fw.write("    <Style ss:ID=\"alt\"><Interior ss:Color=\"#F5F0FF\" ss:Pattern=\"Solid\"/></Style>\n");
            fw.write("  </Styles>\n");
            fw.write("  <Worksheet ss:Name=\"Rendez-vous\">\n");
            fw.write("    <Table>\n");

            // Header row
            String[] headers = {"ID", "Patient", "Date / Heure", "Lieu", "Notes", "Statut"};
            fw.write("      <Row>\n");
            for (String h : headers) fw.write("        <Cell ss:StyleID=\"header\"><Data ss:Type=\"String\">" + esc(h) + "</Data></Cell>\n");
            fw.write("      </Row>\n");

            // Data rows
            int row = 0;
            for (RendezVous rv : filtered) {
                String style = (row++ % 2 == 1) ? " ss:StyleID=\"alt\"" : "";
                fw.write("      <Row>\n");
                fw.write("        <Cell" + style + "><Data ss:Type=\"Number\">" + rv.getId() + "</Data></Cell>\n");
                fw.write("        <Cell" + style + "><Data ss:Type=\"String\">" + esc(rv.getPatientNom()) + "</Data></Cell>\n");
                fw.write("        <Cell" + style + "><Data ss:Type=\"String\">" + esc(rv.getDateRdv() != null ? rv.getDateRdv().format(DATE_FMT) : "") + "</Data></Cell>\n");
                fw.write("        <Cell" + style + "><Data ss:Type=\"String\">" + esc(safe(rv.getLieu())) + "</Data></Cell>\n");
                fw.write("        <Cell" + style + "><Data ss:Type=\"String\">" + esc(safe(rv.getNotes())) + "</Data></Cell>\n");
                fw.write("        <Cell" + style + "><Data ss:Type=\"String\">" + esc(rv.getStatut().name()) + "</Data></Cell>\n");
                fw.write("      </Row>\n");
            }

            fw.write("    </Table>\n");
            fw.write("  </Worksheet>\n");
            fw.write("</Workbook>\n");

            statusLabel.setText("Export Excel réussi : " + f.getName() + " (" + filtered.size() + " lignes)");
        } catch (IOException e) {
            statusLabel.setText("Erreur export : " + e.getMessage());
        }
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String safe(String v) { return v == null ? "" : v; }
}