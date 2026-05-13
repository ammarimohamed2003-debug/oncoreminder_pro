package com.onco.controller;

import com.onco.RendezVousApplication;
import com.onco.dao.RendezVousDAO;
import com.onco.model.RendezVous;
import com.onco.utils.MyDataBase;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RendezVousController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private TextField selectedIdField;

    @FXML
    private TextField treatmentIdField;

    @FXML
    private DatePicker appointmentDatePicker;

    @FXML
    private TextField timeField;

    @FXML
    private TextField locationField;

    @FXML
    private TextArea notesArea;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private TableView<RendezVous> appointmentTable;

    @FXML
    private TableColumn<RendezVous, Number> idColumn;

    @FXML
    private TableColumn<RendezVous, Number> treatmentColumn;

    @FXML
    private TableColumn<RendezVous, String> dateColumn;

    @FXML
    private TableColumn<RendezVous, String> timeColumn;

    @FXML
    private TableColumn<RendezVous, String> locationColumn;

    @FXML
    private TableColumn<RendezVous, String> notesColumn;

    @FXML
    private Label todayCountLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Label confirmedCountLabel;

    @FXML
    private Label footerStatusLabel;

    @FXML
    private Label formMessageLabel;

    private final RendezVousDAO rendezVousDAO = new RendezVousDAO();
    private final ObservableList<RendezVous> masterAppointments = FXCollections.observableArrayList();
    private final ObservableList<RendezVous> filteredAppointments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        bindUiEvents();
        clearForm();
        loadAppointmentsFromDatabase();
    }

    private void setupFilters() {
        filterComboBox.setItems(FXCollections.observableArrayList(
                "Tous",
                "Aujourd'hui",
                "Cette semaine",
                "A venir",
                "Passes"
        ));
        filterComboBox.setValue("Tous");
    }

    private void setupTable() {
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        treatmentColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getTraitementId()));
        dateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatDate(data.getValue())));
        timeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatTime(data.getValue())));
        locationColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getLieu())));
        notesColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getNotes())));

        appointmentTable.setItems(filteredAppointments);
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                fillForm(selected);
            }
        });
    }

    private void bindUiEvents() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        filterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadAppointmentsFromDatabase() {
        if (!hasDatabaseConnection()) {
            filteredAppointments.clear();
            masterAppointments.clear();
            updateStats();
            formMessageLabel.setText("Connexion a la base impossible.");
            footerStatusLabel.setText("Verifiez MySQL et les identifiants dans MyDataBase.");
            return;
        }

        List<RendezVous> rendezVousList = rendezVousDAO.getAll();
        masterAppointments.setAll(rendezVousList);
        applyFilters();
        footerStatusLabel.setText(masterAppointments.size() + " rendez-vous charges depuis la base.");
    }

    private void prefillValidTraitementId() {
        if (!hasDatabaseConnection()) {
            return;
        }

        Integer firstTraitementId = rendezVousDAO.getFirstTraitementId();
        if (firstTraitementId != null) {
            if (treatmentIdField.getText().isBlank()) {
                treatmentIdField.setText(String.valueOf(firstTraitementId));
            }
            formMessageLabel.setText("Traitement valide detecte : " + firstTraitementId);
        } else {
            formMessageLabel.setText("Aucun traitement trouve. Creez d'abord un traitement avant d'ajouter un rendez-vous.");
            footerStatusLabel.setText("Insertion impossible tant que la table traitement est vide.");
        }
    }

    private boolean hasDatabaseConnection() {
        return MyDataBase.getInstance().getCnx() != null;
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedFilter = filterComboBox.getValue() == null ? "Tous" : filterComboBox.getValue();

        filteredAppointments.setAll(
                masterAppointments.stream()
                        .filter(rv -> matchesPeriod(rv, selectedFilter))
                        .filter(rv -> matchesSearch(rv, searchText))
                        .toList()
        );

        updateStats();
    }

    private boolean matchesPeriod(RendezVous rendezVous, String selectedFilter) {
        LocalDate rendezVousDate = rendezVous.getDateRdv().toLocalDate();
        LocalDate today = LocalDate.now();

        return switch (selectedFilter) {
            case "Aujourd'hui" -> rendezVousDate.equals(today);
            case "Cette semaine" -> !rendezVousDate.isBefore(today) && !rendezVousDate.isAfter(today.plusDays(6));
            case "A venir" -> !rendezVousDate.isBefore(today);
            case "Passes" -> rendezVousDate.isBefore(today);
            default -> true;
        };
    }

    private boolean matchesSearch(RendezVous rendezVous, String searchText) {
        if (searchText.isEmpty()) {
            return true;
        }

        return String.valueOf(rendezVous.getId()).contains(searchText)
                || String.valueOf(rendezVous.getTraitementId()).contains(searchText)
                || formatDate(rendezVous).toLowerCase(Locale.ROOT).contains(searchText)
                || formatTime(rendezVous).toLowerCase(Locale.ROOT).contains(searchText)
                || safeText(rendezVous.getLieu()).toLowerCase(Locale.ROOT).contains(searchText)
                || safeText(rendezVous.getNotes()).toLowerCase(Locale.ROOT).contains(searchText);
    }

    private void updateStats() {
        LocalDate today = LocalDate.now();
        long todayCount = masterAppointments.stream()
                .filter(rv -> rv.getDateRdv().toLocalDate().equals(today))
                .count();
        long weekCount = masterAppointments.stream()
                .filter(rv -> {
                    LocalDate date = rv.getDateRdv().toLocalDate();
                    return !date.isBefore(today) && !date.isAfter(today.plusDays(6));
                })
                .count();
        long totalCount = masterAppointments.size();

        todayCountLabel.setText(String.format("%02d", todayCount));
        pendingCountLabel.setText(String.format("%02d", weekCount));
        confirmedCountLabel.setText(String.format("%02d", totalCount));
    }

    @FXML
    private void onNewAppointment() {
        clearForm();
        formMessageLabel.setText("Nouveau rendez-vous pret a etre saisi.");
        footerStatusLabel.setText("Mode creation active.");
    }

    @FXML
    private void onRefresh() {
        loadAppointmentsFromDatabase();
        formMessageLabel.setText("Liste actualisee depuis la base.");
    }

    @FXML
    private void onSwitchToOrdonnances() {
        try {
            System.out.println("Tentative de chargement de : /com/onco/ordonnance-view.fxml");
            FXMLLoader loader = new FXMLLoader(RendezVousApplication.class.getResource("/com/onco/ordonnance-view.fxml"));
            Scene scene = new Scene(loader.load(), 1180, 720);
            Stage stage = (Stage) appointmentTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gestion des Ordonnances");
            System.out.println("Vue Ordonnances chargee avec succes.");
        } catch (Exception e) {
            e.printStackTrace();
            footerStatusLabel.setText("Erreur chargement Ordonnances : " + e.getMessage());
            System.out.println("❌ Erreur lors du switch vers Ordonnances : " + e.getMessage());
        }
    }

    @FXML
    private void onExport() {
        if (filteredAppointments.isEmpty()) {
            formMessageLabel.setText("Aucun rendez-vous a exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les rendez-vous");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("rendez-vous-export.csv");

        Window window = appointmentTable.getScene() == null ? null : appointmentTable.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(window);
        if (selectedFile == null) {
            footerStatusLabel.setText("Export annule.");
            return;
        }

        StringBuilder csv = new StringBuilder("id;traitement_id;date;heure;lieu;notes\n");
        for (RendezVous rendezVous : filteredAppointments) {
            csv.append(rendezVous.getId()).append(';')
                    .append(rendezVous.getTraitementId()).append(';')
                    .append(formatDate(rendezVous)).append(';')
                    .append(formatTime(rendezVous)).append(';')
                    .append(csvValue(rendezVous.getLieu())).append(';')
                    .append(csvValue(rendezVous.getNotes())).append('\n');
        }

        try {
            Files.writeString(selectedFile.toPath(), csv.toString(), StandardCharsets.UTF_8);
            footerStatusLabel.setText("Export CSV reussi : " + selectedFile.getName());
        } catch (IOException e) {
            footerStatusLabel.setText("Erreur export : " + e.getMessage());
        }
    }

    @FXML
    private void onSaveAppointment() {
        if (!hasDatabaseConnection() || !isFormValid()) {
            return;
        }

        RendezVous rendezVous = buildRendezVousFromForm();
        if (rendezVousDAO.ajouter(rendezVous)) {
            loadAppointmentsFromDatabase();
            selectAppointmentById(rendezVous.getId());
            formMessageLabel.setText("Rendez-vous ajoute avec succes.");
            footerStatusLabel.setText("Insertion en base reussie.");
        } else {
            footerStatusLabel.setText("Echec de l'ajout du rendez-vous.");
        }
    }

    @FXML
    private void onUpdateAppointment() {
        RendezVous selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formMessageLabel.setText("Selectionnez un rendez-vous a modifier.");
            return;
        }

        if (!hasDatabaseConnection() || !isFormValid()) {
            return;
        }

        RendezVous rendezVous = buildRendezVousFromForm();
        rendezVous.setId(selected.getId());

        if (rendezVousDAO.modifier(rendezVous)) {
            loadAppointmentsFromDatabase();
            selectAppointmentById(rendezVous.getId());
            formMessageLabel.setText("Rendez-vous modifie avec succes.");
            footerStatusLabel.setText("Mise a jour en base reussie.");
        } else {
            footerStatusLabel.setText("Echec de la modification du rendez-vous.");
        }
    }

    @FXML
    private void onDeleteAppointment() {
        RendezVous selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formMessageLabel.setText("Selectionnez un rendez-vous a supprimer.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer le rendez-vous #" + selected.getId() + " ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirmation.setHeaderText("Confirmation de suppression");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            footerStatusLabel.setText("Suppression annulee.");
            return;
        }

        if (rendezVousDAO.supprimer(selected.getId())) {
            clearForm();
            loadAppointmentsFromDatabase();
            formMessageLabel.setText("Rendez-vous supprime avec succes.");
            footerStatusLabel.setText("Suppression en base reussie.");
        } else {
            footerStatusLabel.setText("Echec de la suppression du rendez-vous.");
        }
    }

    private boolean isFormValid() {
        if (treatmentIdField.getText().isBlank()
                || appointmentDatePicker.getValue() == null
                || timeField.getText().isBlank()) {
            formMessageLabel.setText("Traitement ID, date et heure sont obligatoires.");
            return false;
        }

        try {
            Integer.parseInt(treatmentIdField.getText().trim());
        } catch (NumberFormatException e) {
            formMessageLabel.setText("Traitement ID doit etre un nombre entier.");
            return false;
        }

        try {
            LocalTime.parse(timeField.getText().trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            formMessageLabel.setText("Heure invalide. Utilisez le format HH:mm.");
            return false;
        }

        int traitementId = Integer.parseInt(treatmentIdField.getText().trim());
        if (!rendezVousDAO.traitementExists(traitementId)) {
            Integer firstTraitementId = rendezVousDAO.getFirstTraitementId();
            if (firstTraitementId != null) {
                treatmentIdField.setText(String.valueOf(firstTraitementId));
                formMessageLabel.setText("Traitement ID introuvable. Utilisez un ID existant, par exemple : " + firstTraitementId);
                footerStatusLabel.setText("Le rendez-vous n'a pas ete enregistre car le traitement_id n'existe pas.");
            } else {
                formMessageLabel.setText("Aucun traitement disponible. Creez d'abord un traitement dans la base.");
                footerStatusLabel.setText("Table traitement vide : insertion bloquee.");
            }
            return false;
        }

        return true;
    }

    private RendezVous buildRendezVousFromForm() {
        int traitementId = Integer.parseInt(treatmentIdField.getText().trim());
        LocalDate date = appointmentDatePicker.getValue();
        LocalTime time = LocalTime.parse(timeField.getText().trim(), TIME_FORMATTER);
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        return new RendezVous(
                traitementId,
                dateTime,
                emptyToNull(locationField.getText()),
                emptyToNull(notesArea.getText())
        );
    }

    private void fillForm(RendezVous rendezVous) {
        selectedIdField.setText(String.valueOf(rendezVous.getId()));
        treatmentIdField.setText(String.valueOf(rendezVous.getTraitementId()));
        appointmentDatePicker.setValue(rendezVous.getDateRdv().toLocalDate());
        timeField.setText(rendezVous.getDateRdv().toLocalTime().format(TIME_FORMATTER));
        locationField.setText(safeText(rendezVous.getLieu()));
        notesArea.setText(safeText(rendezVous.getNotes()));
        formMessageLabel.setText("Rendez-vous charge depuis la base.");
        footerStatusLabel.setText("Edition du rendez-vous #" + rendezVous.getId());
    }

    private void clearForm() {
        selectedIdField.clear();
        treatmentIdField.clear();
        appointmentDatePicker.setValue(LocalDate.now());
        timeField.clear();
        locationField.clear();
        notesArea.clear();
        appointmentTable.getSelectionModel().clearSelection();
        prefillValidTraitementId();
    }

    private void selectAppointmentById(int id) {
        for (RendezVous rendezVous : filteredAppointments) {
            if (rendezVous.getId() == id) {
                appointmentTable.getSelectionModel().select(rendezVous);
                appointmentTable.scrollTo(rendezVous);
                return;
            }
        }
    }

    private String formatDate(RendezVous rendezVous) {
        return rendezVous.getDateRdv().format(DATE_FORMATTER);
    }

    private String formatTime(RendezVous rendezVous) {
        return rendezVous.getDateRdv().toLocalTime().format(TIME_FORMATTER);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String csvValue(String value) {
        return safeText(value).replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}
