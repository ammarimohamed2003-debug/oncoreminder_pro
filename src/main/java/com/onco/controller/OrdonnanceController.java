package com.onco.controller;

import com.onco.RendezVousApplication;
import com.onco.dao.OrdonnanceDAO;
import com.onco.model.Ordonnance;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class OrdonnanceController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private TextField selectedIdField;

    @FXML
    private TextField rendezVousIdField;

    @FXML
    private DatePicker issueDatePicker;

    @FXML
    private TextField durationField;

    @FXML
    private TextArea medicationArea;

    @FXML
    private TextArea dosageArea;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private TableView<Ordonnance> prescriptionTable;

    @FXML
    private TableColumn<Ordonnance, Number> idColumn;

    @FXML
    private TableColumn<Ordonnance, Number> rendezVousColumn;

    @FXML
    private TableColumn<Ordonnance, String> medicamentsColumn;

    @FXML
    private TableColumn<Ordonnance, String> posologieColumn;

    @FXML
    private TableColumn<Ordonnance, Number> durationColumn;

    @FXML
    private TableColumn<Ordonnance, String> emissionDateColumn;

    @FXML
    private Label todayCountLabel;

    @FXML
    private Label weekCountLabel;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label footerStatusLabel;

    @FXML
    private Label formMessageLabel;

    private final OrdonnanceDAO ordonnanceDAO = new OrdonnanceDAO();
    private final ObservableList<Ordonnance> masterOrdonnances = FXCollections.observableArrayList();
    private final ObservableList<Ordonnance> filteredOrdonnances = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupFilters();
        setupTable();
        bindUiEvents();
        clearForm();
        loadOrdonnancesFromDatabase();
    }

    private void setupFilters() {
        filterComboBox.setItems(FXCollections.observableArrayList(
                "Toutes",
                "Aujourd'hui",
                "Cette semaine",
                "Ce mois",
                "Anciennes"
        ));
        filterComboBox.setValue("Toutes");
    }

    private void setupTable() {
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        rendezVousColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getRendezVousId()));
        medicamentsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getMedicaments())));
        posologieColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(safeText(data.getValue().getPosologie())));
        durationColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDureeJours()));
        emissionDateColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatDate(data.getValue().getDateEmission())));

        prescriptionTable.setItems(filteredOrdonnances);
        prescriptionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                fillForm(selected);
            }
        });
    }

    private void bindUiEvents() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        filterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadOrdonnancesFromDatabase() {
        if (!hasDatabaseConnection()) {
            masterOrdonnances.clear();
            filteredOrdonnances.clear();
            updateStats();
            formMessageLabel.setText("Connexion a la base impossible.");
            footerStatusLabel.setText("Verifiez MySQL et les identifiants dans MyDataBase.");
            return;
        }

        List<Ordonnance> ordonnanceList = ordonnanceDAO.getAll();
        masterOrdonnances.setAll(ordonnanceList);
        applyFilters();
        footerStatusLabel.setText(masterOrdonnances.size() + " ordonnances chargees depuis la base.");
    }

    private boolean hasDatabaseConnection() {
        return MyDataBase.getInstance().getCnx() != null;
    }

    private void prefillValidRendezVousId() {
        if (!hasDatabaseConnection()) {
            return;
        }

        Integer firstRendezVousId = ordonnanceDAO.getFirstRendezVousId();
        if (firstRendezVousId != null) {
            if (rendezVousIdField.getText().isBlank()) {
                rendezVousIdField.setText(String.valueOf(firstRendezVousId));
            }
            formMessageLabel.setText("Rendez-vous valide detecte : " + firstRendezVousId);
        } else {
            formMessageLabel.setText("Aucun rendez-vous trouve. Creez d'abord un rendez-vous avant d'ajouter une ordonnance.");
            footerStatusLabel.setText("Insertion impossible tant que la table rendez_vous est vide.");
        }
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedFilter = filterComboBox.getValue() == null ? "Toutes" : filterComboBox.getValue();

        filteredOrdonnances.setAll(
                masterOrdonnances.stream()
                        .filter(ordonnance -> matchesPeriod(ordonnance, selectedFilter))
                        .filter(ordonnance -> matchesSearch(ordonnance, searchText))
                        .toList()
        );

        updateStats();
    }

    private boolean matchesPeriod(Ordonnance ordonnance, String selectedFilter) {
        LocalDate issueDate = ordonnance.getDateEmission();
        LocalDate today = LocalDate.now();

        return switch (selectedFilter) {
            case "Aujourd'hui" -> issueDate.equals(today);
            case "Cette semaine" -> !issueDate.isBefore(today) && !issueDate.isAfter(today.plusDays(6));
            case "Ce mois" -> issueDate.getMonth() == today.getMonth() && issueDate.getYear() == today.getYear();
            case "Anciennes" -> issueDate.isBefore(today);
            default -> true;
        };
    }

    private boolean matchesSearch(Ordonnance ordonnance, String searchText) {
        if (searchText.isEmpty()) {
            return true;
        }

        return String.valueOf(ordonnance.getId()).contains(searchText)
                || String.valueOf(ordonnance.getRendezVousId()).contains(searchText)
                || String.valueOf(ordonnance.getDureeJours()).contains(searchText)
                || formatDate(ordonnance.getDateEmission()).toLowerCase(Locale.ROOT).contains(searchText)
                || safeText(ordonnance.getMedicaments()).toLowerCase(Locale.ROOT).contains(searchText)
                || safeText(ordonnance.getPosologie()).toLowerCase(Locale.ROOT).contains(searchText);
    }

    private void updateStats() {
        LocalDate today = LocalDate.now();
        long todayCount = masterOrdonnances.stream()
                .filter(ordonnance -> ordonnance.getDateEmission().equals(today))
                .count();
        long weekCount = masterOrdonnances.stream()
                .filter(ordonnance -> {
                    LocalDate date = ordonnance.getDateEmission();
                    return !date.isBefore(today) && !date.isAfter(today.plusDays(6));
                })
                .count();
        long totalCount = masterOrdonnances.size();

        todayCountLabel.setText(String.format("%02d", todayCount));
        weekCountLabel.setText(String.format("%02d", weekCount));
        totalCountLabel.setText(String.format("%02d", totalCount));
    }

    @FXML
    private void onNewOrdonnance() {
        clearForm();
        formMessageLabel.setText("Nouvelle ordonnance prete a etre saisie.");
        footerStatusLabel.setText("Mode creation active.");
    }

    @FXML
    private void onRefresh() {
        loadOrdonnancesFromDatabase();
        formMessageLabel.setText("Liste des ordonnances actualisee.");
    }

    @FXML
    private void onSwitchToRendezVous() {
        try {
            System.out.println("Tentative de chargement de : /com/onco/rendez-vous-view.fxml");
            FXMLLoader loader = new FXMLLoader(RendezVousApplication.class.getResource("/com/onco/rendez-vous-view.fxml"));
            Scene scene = new Scene(loader.load(), 1180, 720);
            Stage stage = (Stage) prescriptionTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gestion des Rendez-vous");
            System.out.println("Vue Rendez-vous chargee avec succes.");
        } catch (Exception e) {
            e.printStackTrace();
            footerStatusLabel.setText("Erreur chargement Rendez-vous : " + e.getMessage());
            System.out.println("❌ Erreur lors du switch vers Rendez-vous : " + e.getMessage());
        }
    }

    @FXML
    private void onExport() {
        if (filteredOrdonnances.isEmpty()) {
            formMessageLabel.setText("Aucune ordonnance a exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les ordonnances");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("ordonnances-export.csv");

        Window window = prescriptionTable.getScene() == null ? null : prescriptionTable.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(window);
        if (selectedFile == null) {
            footerStatusLabel.setText("Export annule.");
            return;
        }

        StringBuilder csv = new StringBuilder("id;rendez_vous_id;medicaments;posologie;duree_jours;date_emission\n");
        for (Ordonnance ordonnance : filteredOrdonnances) {
            csv.append(ordonnance.getId()).append(';')
                    .append(ordonnance.getRendezVousId()).append(';')
                    .append(csvValue(ordonnance.getMedicaments())).append(';')
                    .append(csvValue(ordonnance.getPosologie())).append(';')
                    .append(ordonnance.getDureeJours()).append(';')
                    .append(formatDate(ordonnance.getDateEmission())).append('\n');
        }

        try {
            Files.writeString(selectedFile.toPath(), csv.toString(), StandardCharsets.UTF_8);
            footerStatusLabel.setText("Export CSV reussi : " + selectedFile.getName());
        } catch (IOException e) {
            footerStatusLabel.setText("Erreur export : " + e.getMessage());
        }
    }

    @FXML
    private void onSaveOrdonnance() {
        if (!hasDatabaseConnection() || !isFormValid()) {
            return;
        }

        Ordonnance ordonnance = buildOrdonnanceFromForm();
        if (ordonnanceDAO.ajouter(ordonnance)) {
            loadOrdonnancesFromDatabase();
            selectOrdonnanceById(ordonnance.getId());
            formMessageLabel.setText("Ordonnance ajoutee avec succes.");
            footerStatusLabel.setText("Insertion en base reussie.");
        } else {
            footerStatusLabel.setText("Echec de l'ajout de l'ordonnance.");
        }
    }

    @FXML
    private void onUpdateOrdonnance() {
        Ordonnance selected = prescriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formMessageLabel.setText("Selectionnez une ordonnance a modifier.");
            return;
        }

        if (!hasDatabaseConnection() || !isFormValid()) {
            return;
        }

        Ordonnance ordonnance = buildOrdonnanceFromForm();
        ordonnance.setId(selected.getId());

        if (ordonnanceDAO.modifier(ordonnance)) {
            loadOrdonnancesFromDatabase();
            selectOrdonnanceById(ordonnance.getId());
            formMessageLabel.setText("Ordonnance modifiee avec succes.");
            footerStatusLabel.setText("Mise a jour en base reussie.");
        } else {
            footerStatusLabel.setText("Echec de la modification de l'ordonnance.");
        }
    }

    @FXML
    private void onDeleteOrdonnance() {
        Ordonnance selected = prescriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formMessageLabel.setText("Selectionnez une ordonnance a supprimer.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer l'ordonnance #" + selected.getId() + " ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirmation.setHeaderText("Confirmation de suppression");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            footerStatusLabel.setText("Suppression annulee.");
            return;
        }

        if (ordonnanceDAO.supprimer(selected.getId())) {
            clearForm();
            loadOrdonnancesFromDatabase();
            formMessageLabel.setText("Ordonnance supprimee avec succes.");
            footerStatusLabel.setText("Suppression en base reussie.");
        } else {
            footerStatusLabel.setText("Echec de la suppression de l'ordonnance.");
        }
    }

    private boolean isFormValid() {
        if (rendezVousIdField.getText().isBlank()
                || medicationArea.getText().isBlank()
                || dosageArea.getText().isBlank()
                || durationField.getText().isBlank()
                || issueDatePicker.getValue() == null) {
            formMessageLabel.setText("Rendez-vous ID, medicaments, posologie, duree et date sont obligatoires.");
            return false;
        }

        int rendezVousId;
        int dureeJours;

        try {
            rendezVousId = Integer.parseInt(rendezVousIdField.getText().trim());
        } catch (NumberFormatException e) {
            formMessageLabel.setText("Rendez-vous ID doit etre un nombre entier.");
            return false;
        }

        try {
            dureeJours = Integer.parseInt(durationField.getText().trim());
        } catch (NumberFormatException e) {
            formMessageLabel.setText("La duree doit etre un nombre entier de jours.");
            return false;
        }

        if (dureeJours <= 0) {
            formMessageLabel.setText("La duree doit etre superieure a 0.");
            return false;
        }

        if (!ordonnanceDAO.rendezVousExists(rendezVousId)) {
            Integer firstRendezVousId = ordonnanceDAO.getFirstRendezVousId();
            if (firstRendezVousId != null) {
                rendezVousIdField.setText(String.valueOf(firstRendezVousId));
                formMessageLabel.setText("Rendez-vous ID introuvable. Utilisez un ID existant, par exemple : " + firstRendezVousId);
                footerStatusLabel.setText("L'ordonnance n'a pas ete enregistree car le rendez_vous_id n'existe pas.");
            } else {
                formMessageLabel.setText("Aucun rendez-vous disponible. Creez d'abord un rendez-vous dans la base.");
                footerStatusLabel.setText("Table rendez_vous vide : insertion bloquee.");
            }
            return false;
        }

        return true;
    }

    private Ordonnance buildOrdonnanceFromForm() {
        return new Ordonnance(
                Integer.parseInt(rendezVousIdField.getText().trim()),
                medicationArea.getText().trim(),
                dosageArea.getText().trim(),
                Integer.parseInt(durationField.getText().trim()),
                issueDatePicker.getValue()
        );
    }

    private void fillForm(Ordonnance ordonnance) {
        selectedIdField.setText(String.valueOf(ordonnance.getId()));
        rendezVousIdField.setText(String.valueOf(ordonnance.getRendezVousId()));
        medicationArea.setText(safeText(ordonnance.getMedicaments()));
        dosageArea.setText(safeText(ordonnance.getPosologie()));
        durationField.setText(String.valueOf(ordonnance.getDureeJours()));
        issueDatePicker.setValue(ordonnance.getDateEmission());
        formMessageLabel.setText("Ordonnance chargee depuis la base.");
        footerStatusLabel.setText("Edition de l'ordonnance #" + ordonnance.getId());
    }

    private void clearForm() {
        selectedIdField.clear();
        rendezVousIdField.clear();
        medicationArea.clear();
        dosageArea.clear();
        durationField.clear();
        issueDatePicker.setValue(LocalDate.now());
        prescriptionTable.getSelectionModel().clearSelection();
        prefillValidRendezVousId();
    }

    private void selectOrdonnanceById(int id) {
        for (Ordonnance ordonnance : filteredOrdonnances) {
            if (ordonnance.getId() == id) {
                prescriptionTable.getSelectionModel().select(ordonnance);
                prescriptionTable.scrollTo(ordonnance);
                return;
            }
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String csvValue(String value) {
        return safeText(value).replace(";", ",").replace("\n", " ").replace("\r", " ");
    }
}
