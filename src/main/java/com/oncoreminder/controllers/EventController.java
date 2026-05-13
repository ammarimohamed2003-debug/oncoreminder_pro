package com.oncoreminder.controllers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.oncoreminder.app.App;
import com.oncoreminder.models.Event;
import com.oncoreminder.models.Reservation;
import com.oncoreminder.services.ServiceEvent;
import com.oncoreminder.services.ServiceReservation;
import com.oncoreminder.utils.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EventController {

    @FXML private TextField tfTitre;
    @FXML private TextField tfLieu;
    @FXML private TextField tfCapacite;
    @FXML private TextField tfHeure;
    @FXML private TextField tfImagePath;
    @FXML private TextField tfRecherche;
    @FXML private TextArea taDescription;
    @FXML private DatePicker dpDate;
    @FXML private DatePicker dpCalendrier;
    @FXML private ImageView imagePreview;
    @FXML private Label imageHint;
    @FXML private Label formTitle;
    @FXML private Label lbMessage;
    @FXML private Label lbTotalEvents;
    @FXML private Label lbReservationsActives;
    @FXML private Label lbEventsActifs;
    @FXML private Label lbTauxRemplissage;
    @FXML private Label lbMapsInfo;
    @FXML private Label lbMapsCoords;
    @FXML private Label lbFooterTime;
    @FXML private Label toastLabel;
    @FXML private StackPane toastPane;
    @FXML private WebView webMap;
    @FXML private ProgressIndicator aiSpinner;

    @FXML private Button btnFiltreTous;
    @FXML private Button btnFiltreAujourdhui;
    @FXML private Button btnFiltreSemaine;
    @FXML private Button btnFiltreComplet;
    @FXML private Button btnFiltreDisponible;
    @FXML private Button btnDarkMode;

    @FXML private TableView<Event> tableEvent;
    @FXML private TableColumn<Event, String> colImage;
    @FXML private TableColumn<Event, String> colTitre;
    @FXML private TableColumn<Event, String> colLieu;
    @FXML private TableColumn<Event, String> colDate;
    @FXML private TableColumn<Event, Integer> colCapacite;
    @FXML private TableColumn<Event, Integer> colPlaces;
    @FXML private TableColumn<Event, String> colStatut;
    @FXML private TableColumn<Event, Void> colActions;

    @FXML private ScrollPane formulairePane;
    @FXML private ListView<String> listCalendrier;
    @FXML private PieChart chartStatuts;
    @FXML private BarChart<String, Number> chartRemplissage;

    private Runnable onOuvrirReservations;
    public void setOnOuvrirReservations(Runnable r) { this.onOuvrirReservations = r; }

    private final ServiceEvent serviceEvent = new ServiceEvent();
    private final ServiceReservation serviceReservation = new ServiceReservation();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private final PauseTransition mapSearchPause = new PauseTransition(Duration.millis(450));
    private Event eventSelectionne;
    private String filtreActif = "TOUS";
    private boolean lightMode;

    private static final DateTimeFormatter DATE_TABLE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DEFAULT_HF_MODEL = "HuggingFaceH4/zephyr-7b-beta";

    @FXML
    void initialize() {
        configurerColonnes();
        configurerTable();
        configurerListeners();

        dpDate.setValue(LocalDate.now());
        tfHeure.setText("10:00");
        if (dpCalendrier != null) dpCalendrier.setValue(LocalDate.now());
        if (lbFooterTime != null) {
            lbFooterTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        chargerEvents();
        preparerCreation();
        mettreAJourCarte(tfLieu.getText());
    }

    @FXML void handleAjouterClick() {
        boolean visible = formulairePane.isVisible();
        formulairePane.setVisible(!visible);
        formulairePane.setManaged(!visible);
        if (!visible) {
            tableEvent.getSelectionModel().clearSelection();
            preparerCreation();
        }
    }

    @FXML void ouvrirFormulaireModification(ActionEvent e) {
        if (eventSelectionne == null) {
            afficherToast("Selectionnez un evenement a modifier.", false);
            return;
        }
        afficherFormulaireEdition(eventSelectionne);
    }

    @FXML void enregistrerEvent(ActionEvent e) {
        if (eventSelectionne == null) ajouterEvent(e); else modifierEvent(e);
    }

    @FXML void ajouterEvent(ActionEvent e) {
        try {
            Event ev = lireFormulaire();
            int createurId = currentUserId();
            serviceEvent.add(ev, createurId);
            afficherToast("Evenement cree avec succes.", true);
            chargerEvents();
            preparerCreation();
            masquerFormulaire();
        } catch (Exception ex) {
            afficherToast("Erreur creation : " + ex.getMessage(), false);
        }
    }

    @FXML void modifierEvent(ActionEvent e) {
        if (eventSelectionne == null) {
            afficherToast("Selectionnez un evenement.", false);
            return;
        }
        try {
            Event v = lireFormulaire();
            int reservationsExistantes = Math.max(0, eventSelectionne.getCapacite() - eventSelectionne.getPlacesRestantes());
            eventSelectionne.setTitre(v.getTitre());
            eventSelectionne.setLieu(v.getLieu());
            eventSelectionne.setDescription(v.getDescription());
            eventSelectionne.setCapacite(v.getCapacite());
            eventSelectionne.setPlacesRestantes(Math.max(0, v.getCapacite() - reservationsExistantes));
            eventSelectionne.setDateEvent(v.getDateEvent());
            eventSelectionne.setImagePath(v.getImagePath());
            serviceEvent.update(eventSelectionne);
            afficherToast("Evenement modifie avec succes.", true);
            chargerEvents();
            preparerCreation();
            masquerFormulaire();
        } catch (Exception ex) {
            afficherToast("Erreur modification : " + ex.getMessage(), false);
        }
    }

    @FXML void supprimerEvent(ActionEvent e) {
        if (eventSelectionne == null) {
            afficherToast("Selectionnez un evenement.", false);
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + eventSelectionne.getTitre() + "\" ?", ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Confirmation de suppression");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                serviceEvent.delete(eventSelectionne);
                afficherToast("Evenement supprime.", true);
                chargerEvents();
                preparerCreation();
                masquerFormulaire();
            }
        });
    }

    @FXML void filtrerTous(ActionEvent e) { changerFiltre("TOUS"); }
    @FXML void filtrerAujourdhui(ActionEvent e) { changerFiltre("AUJOURDHUI"); }
    @FXML void filtrerSemaine(ActionEvent e) { changerFiltre("SEMAINE"); }
    @FXML void filtrerComplet(ActionEvent e) { changerFiltre("COMPLET"); }
    @FXML void filtrerDisponible(ActionEvent e) { changerFiltre("DISPONIBLE"); }

    @FXML void choisirImage(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = chooser.showOpenDialog(tfTitre.getScene().getWindow());
        if (file != null) {
            tfImagePath.setText(file.getAbsolutePath());
            imagePreview.setImage(new Image(file.toURI().toString(), true));
            imageHint.setVisible(false);
        }
    }

    @FXML void genererDescription(ActionEvent e) {
        String titre = value(tfTitre);
        String lieu = value(tfLieu);
        if (titre.isEmpty()) {
            afficherToast("Saisissez un titre avant de generer.", false);
            return;
        }
        String token = System.getenv("HF_API_TOKEN");
        if (token == null || token.isBlank()) {
            taDescription.setText(descriptionLocale(titre, lieu) + "\n\nSlogan : Ensemble pour mieux comprendre et agir.\nHashtags : #OncoReminder #Sante #EvenementMedical");
            afficherToast("Description locale generee.", true);
            return;
        }
        setAiLoading(true);
        afficherToast("Generation IA en cours...", true);
        String model = System.getenv().getOrDefault("HF_MODEL", DEFAULT_HF_MODEL);
        String prompt = "Redige en francais pour un evenement medical oncologique. Titre: " + titre
                + ". Lieu: " + (lieu.isBlank() ? "non precise" : lieu)
                + ". Retourne exactement trois sections: Description, Slogan, Hashtags.";
        String body = "{\"inputs\":\"" + jsonEscape(prompt) + "\",\"parameters\":{\"max_new_tokens\":160,\"temperature\":0.7}}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api-inference.huggingface.co/models/" + model))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::extraireTexteHuggingFace)
                .exceptionally(ex -> descriptionLocale(titre, lieu))
                .thenAccept(desc -> Platform.runLater(() -> {
                    taDescription.setText(desc);
                    setAiLoading(false);
                    afficherToast("Description generee.", true);
                }));
    }

    @FXML void onLieuChanged() {
        mettreAJourCarte(tfLieu.getText());
    }

    @FXML void ouvrirGoogleMaps(ActionEvent e) {
        ouvrirMaps(value(tfLieu));
    }

    @FXML void exporterPdf(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les evenements");
        chooser.setInitialFileName("evenements-oncoreminder.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File dest = chooser.showSaveDialog(tableEvent.getScene().getWindow());
        if (dest == null) return;
        try {
            exporterEvenementsPdf(dest);
            afficherToast("PDF exporte avec succes.", true);
        } catch (Exception ex) {
            afficherToast("Erreur export PDF : " + ex.getMessage(), false);
        }
    }

    @FXML void toggleDarkMode(ActionEvent e) {
        Scene scene = tableEvent.getScene();
        if (scene == null) return;
        lightMode = !lightMode;
        scene.getRoot().getStyleClass().remove("em-light");
        if (lightMode) scene.getRoot().getStyleClass().add("em-light");
        if (btnDarkMode != null) btnDarkMode.setText(lightMode ? "Mode sombre" : "Mode clair");
    }

    @FXML void ouvrirReservations(ActionEvent e) {
        if (onOuvrirReservations != null) onOuvrirReservations.run();
        else App.navigate("GestionReservation");
    }

    @FXML void handleBack(ActionEvent e) { App.navigate("DoctorDashboard"); }
    @FXML void handleArticles(ActionEvent e) { App.navigate("ArticleList"); }
    @FXML void handleLogout(ActionEvent e) { UserSession.getInstance().logout(); App.navigate("Login"); }

    private void configurerColonnes() {
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLieu()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateEvent().format(DATE_TABLE_FMT)));
        colCapacite.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCapacite()).asObject());
        colPlaces.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getPlacesRestantes()).asObject());
        if (colStatut != null) {
            colStatut.setCellValueFactory(c -> new SimpleStringProperty(statut(c.getValue())));
        }
        configurerColonneImage();
        configurerColonnePlaces();
        if (colStatut != null) configurerColonneStatut();
        if (colActions != null) configurerActions();
    }

    private void configurerTable() {
        tableEvent.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> eventSelectionne = sel);
        tableEvent.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (!row.isEmpty()) {
                    eventSelectionne = row.getItem();
                    if (evt.getClickCount() == 2) afficherDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void configurerListeners() {
        tfRecherche.textProperty().addListener((obs, old, val) -> appliquerFiltres());
        tfLieu.textProperty().addListener((obs, old, val) -> {
            mapSearchPause.stop();
            mapSearchPause.setOnFinished(e -> mettreAJourCarte(val));
            mapSearchPause.playFromStart();
        });
        if (dpCalendrier != null) {
            dpCalendrier.valueProperty().addListener((obs, old, val) -> mettreAJourCalendrier());
        }
    }

    private void configurerColonneImage() {
        colImage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImagePath()));
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(58);
                iv.setFitHeight(42);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
            }
            @Override protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Image img = chargerImageTable(path);
                if (img == null) {
                    setText("Image");
                    setGraphic(null);
                } else {
                    iv.setImage(img);
                    setText(null);
                    setGraphic(iv);
                }
            }
        });
    }

    private void configurerColonnePlaces() {
        colPlaces.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar progress = new ProgressBar();
            private final Label label = new Label();
            private final VBox box = new VBox(4, label, progress);
            {
                progress.setMaxWidth(Double.MAX_VALUE);
                label.setStyle("-fx-text-fill:#2D3748;-fx-font-size:11px;-fx-font-weight:bold;");
            }
            @Override protected void updateItem(Integer places, boolean empty) {
                super.updateItem(places, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Event ev = getTableRow().getItem();
                int capacite = Math.max(1, ev.getCapacite());
                double ratio = Math.max(0, Math.min(1, ev.getPlacesRestantes() / (double) capacite));
                label.setText(ev.getPlacesRestantes() + "/" + ev.getCapacite());
                progress.setProgress(ratio);
                setGraphic(box);
            }
        });
    }

    private void configurerColonneStatut() {
        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(status);
                badge.getStyleClass().setAll(switch (status) {
                    case "Complet" -> "event-badge-red";
                    case "Bientot complet" -> "stat-badge-warning";
                    default -> "stat-badge-success";
                });
                setGraphic(badge);
            }
        });
    }

    private void configurerActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button view = new Button("Voir");
            private final Button edit = new Button("Mod");
            private final Button delete = new Button("Sup");
            private final HBox box = new HBox(6, view, edit, delete);
            {
                view.getStyleClass().add("event-icon-purple");
                edit.getStyleClass().add("event-icon-purple");
                delete.getStyleClass().add("event-icon-red");
                view.setOnAction(e -> executerSurLigne(this, EventController.this::afficherDetails));
                edit.setOnAction(e -> executerSurLigne(this, EventController.this::afficherFormulaireEdition));
                delete.setOnAction(e -> executerSurLigne(this, ev -> supprimerEvent(e)));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void executerSurLigne(TableCell<Event, ?> cell, java.util.function.Consumer<Event> action) {
        Event ev = cell.getTableView().getItems().get(cell.getIndex());
        eventSelectionne = ev;
        tableEvent.getSelectionModel().select(ev);
        action.accept(ev);
    }

    private void chargerEvents() {
        events.setAll(serviceEvent.getAll());
        mettreAJourStats();
        appliquerFiltres();
        mettreAJourCalendrier();
        mettreAJourCharts();
    }

    private void appliquerFiltres() {
        String q = tfRecherche.getText() == null ? "" : tfRecherche.getText().toLowerCase(Locale.ROOT).trim();
        LocalDate today = LocalDate.now();
        List<Event> filtres = events.stream()
                .filter(ev -> q.isEmpty()
                        || safe(ev.getTitre()).toLowerCase(Locale.ROOT).contains(q)
                        || safe(ev.getLieu()).toLowerCase(Locale.ROOT).contains(q))
                .filter(ev -> switch (filtreActif) {
                    case "AUJOURDHUI" -> ev.getDateEvent().toLocalDate().equals(today);
                    case "SEMAINE" -> !ev.getDateEvent().toLocalDate().isBefore(today)
                            && !ev.getDateEvent().toLocalDate().isAfter(today.plusDays(7));
                    case "COMPLET" -> ev.getPlacesRestantes() <= 0;
                    case "DISPONIBLE" -> ev.getPlacesRestantes() > 0;
                    default -> true;
                })
                .collect(Collectors.toCollection(ArrayList::new));
        tableEvent.setItems(FXCollections.observableArrayList(filtres));
        lbMessage.setText(filtres.size() + " evenement(s)");
    }

    private void changerFiltre(String filtre) {
        filtreActif = filtre;
        List.of(btnFiltreTous, btnFiltreAujourdhui, btnFiltreSemaine, btnFiltreComplet, btnFiltreDisponible)
                .forEach(b -> b.getStyleClass().setAll("event-tab"));
        switch (filtre) {
            case "AUJOURDHUI" -> btnFiltreAujourdhui.getStyleClass().setAll("event-tab-active");
            case "SEMAINE" -> btnFiltreSemaine.getStyleClass().setAll("event-tab-active");
            case "COMPLET" -> btnFiltreComplet.getStyleClass().setAll("event-tab-active");
            case "DISPONIBLE" -> btnFiltreDisponible.getStyleClass().setAll("event-tab-active");
            default -> btnFiltreTous.getStyleClass().setAll("event-tab-active");
        }
        appliquerFiltres();
    }

    private void mettreAJourStats() {
        int total = events.size();
        int actifs = (int) events.stream().filter(ev -> ev.getDateEvent().isAfter(LocalDateTime.now())).count();
        int reservations = serviceEvent.countReservationsActives();
        int capaciteTotale = events.stream().mapToInt(Event::getCapacite).sum();
        int placesRestantes = events.stream().mapToInt(Event::getPlacesRestantes).sum();
        int taux = capaciteTotale == 0 ? 0 : Math.round((capaciteTotale - placesRestantes) * 100f / capaciteTotale);
        lbTotalEvents.setText(String.valueOf(total));
        lbEventsActifs.setText(String.valueOf(actifs));
        lbReservationsActives.setText(String.valueOf(reservations));
        if (lbTauxRemplissage != null) lbTauxRemplissage.setText(taux + "%");
    }

    private void mettreAJourCalendrier() {
        if (dpCalendrier == null || listCalendrier == null) return;
        LocalDate date = dpCalendrier.getValue() == null ? LocalDate.now() : dpCalendrier.getValue();
        List<String> items = events.stream()
                .filter(ev -> ev.getDateEvent().toLocalDate().equals(date))
                .map(ev -> ev.getDateEvent().toLocalTime().format(TIME_FMT) + "  " + ev.getTitre() + " - " + ev.getLieu())
                .toList();
        listCalendrier.setItems(FXCollections.observableArrayList(items.isEmpty() ? List.of("Aucun evenement ce jour") : items));
    }

    private void mettreAJourCharts() {
        if (chartStatuts != null) {
            long disponibles = events.stream().filter(ev -> "Disponible".equals(statut(ev))).count();
            long bientot = events.stream().filter(ev -> "Bientot complet".equals(statut(ev))).count();
            long complets = events.stream().filter(ev -> "Complet".equals(statut(ev))).count();
            chartStatuts.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Disponible", disponibles),
                    new PieChart.Data("Bientot complet", bientot),
                    new PieChart.Data("Complet", complets)
            ));
        }
        if (chartRemplissage != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            events.stream().limit(8).forEach(ev -> {
                int capacite = Math.max(1, ev.getCapacite());
                int taux = Math.round((capacite - ev.getPlacesRestantes()) * 100f / capacite);
                series.getData().add(new XYChart.Data<>(ev.getTitre(), taux));
            });
            chartRemplissage.getData().setAll(series);
        }
    }

    private Event lireFormulaire() {
        String titre = value(tfTitre);
        String lieu = value(tfLieu);
        if (titre.isEmpty()) throw new IllegalArgumentException("titre obligatoire");
        if (lieu.isEmpty()) throw new IllegalArgumentException("lieu obligatoire");
        if (dpDate.getValue() == null) throw new IllegalArgumentException("date obligatoire");
        LocalTime heure = LocalTime.parse(value(tfHeure), TIME_FMT);
        int capacite = Integer.parseInt(value(tfCapacite));
        if (capacite <= 0) throw new IllegalArgumentException("capacite invalide");
        Event ev = new Event(titre, value(taDescription), LocalDateTime.of(dpDate.getValue(), heure), lieu, capacite);
        ev.setImagePath(value(tfImagePath).isBlank() ? null : value(tfImagePath));
        return ev;
    }

    private void afficherFormulaireEdition(Event ev) {
        formulairePane.setVisible(true);
        formulairePane.setManaged(true);
        remplirFormulaire(ev);
    }

    private void remplirFormulaire(Event ev) {
        eventSelectionne = ev;
        formTitle.setText("Modifier un Evenement");
        tfTitre.setText(ev.getTitre());
        tfLieu.setText(ev.getLieu());
        taDescription.setText(safe(ev.getDescription()));
        tfCapacite.setText(String.valueOf(ev.getCapacite()));
        dpDate.setValue(ev.getDateEvent().toLocalDate());
        tfHeure.setText(ev.getDateEvent().toLocalTime().format(TIME_FMT));
        tfImagePath.setText(ev.getImagePath() == null ? "" : ev.getImagePath());
        chargerImagePreview(ev.getImagePath());
        mettreAJourCarte(ev.getLieu());
    }

    private void preparerCreation() {
        eventSelectionne = null;
        formTitle.setText("Ajouter un Evenement");
        tfTitre.clear();
        tfLieu.clear();
        taDescription.clear();
        tfCapacite.clear();
        tfImagePath.clear();
        imagePreview.setImage(null);
        imageHint.setVisible(true);
        dpDate.setValue(LocalDate.now());
        tfHeure.setText("10:00");
        mettreAJourCarte("");
    }

    private void masquerFormulaire() {
        formulairePane.setVisible(false);
        formulairePane.setManaged(false);
    }

    private void afficherDetails(Event ev) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(tableEvent.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Details evenement");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        ImageView image = new ImageView(chargerImageTable(ev.getImagePath()));
        image.setFitWidth(640);
        image.setFitHeight(210);
        image.setPreserveRatio(true);
        image.setSmooth(true);

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-text-fill:#3A1D7A; -fx-font-size:20px; -fx-font-weight:bold;");
        Label status = new Label(statut(ev));
        status.getStyleClass().add(statusClass(ev));

        WebView map = new WebView();
        map.setPrefHeight(180);
        chargerMap(map, ev.getLieu());

        VBox meta = new VBox(8,
                ligneDetail("Lieu", ev.getLieu()),
                ligneDetail("Date", ev.getDateEvent().format(DATE_TABLE_FMT)),
                ligneDetail("Capacite", String.valueOf(ev.getCapacite())),
                ligneDetail("Places restantes", String.valueOf(ev.getPlacesRestantes())),
                ligneDetail("Vues", String.valueOf(compteurVues(ev))),
                ligneDetail("Compte a rebours", compteARebours(ev))
        );
        Label desc = new Label(safe(ev.getDescription()).isBlank() ? "Aucune description." : ev.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#2D3748; -fx-font-size:13px;");

        Button reserve = new Button("Reserver");
        reserve.getStyleClass().add("event-btn-turquoise");
        reserve.setMaxWidth(Double.MAX_VALUE);
        reserve.setOnAction(e -> {
            reserverEvent(ev);
            dialog.close();
        });

        VBox card = new VBox(16, image, new HBox(12, title, new Region(), status), desc, meta, map, reserve);
        HBox.setHgrow(card.getChildren().get(1), Priority.ALWAYS);
        card.getStyleClass().add("form-panel");
        card.setPrefWidth(680);
        card.setStyle("-fx-padding:22;");

        dialog.getDialogPane().setContent(card);
        Node close = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (close != null) close.getStyleClass().add("event-tab");

        FadeTransition fade = new FadeTransition(Duration.millis(220), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), card);
        slide.setFromY(24);
        slide.setToY(0);
        dialog.setOnShown(e -> {
            fade.playFromStart();
            slide.playFromStart();
        });
        dialog.showAndWait();
    }

    private HBox ligneDetail(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:#64748B; -fx-font-size:11px; -fx-font-weight:bold;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:#2D3748; -fx-font-size:13px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(12, l, spacer, v);
    }

    private void reserverEvent(Event ev) {
        if (ev.getPlacesRestantes() <= 0) {
            afficherToast("Evenement complet.", false);
            return;
        }
        Reservation reservation = new Reservation(ev.getId(), currentUserId());
        boolean ok = serviceReservation.add(reservation);
        if (!ok) {
            afficherToast("Reservation impossible : complet ou deja reserve.", false);
            chargerEvents();
            return;
        }
        afficherQrReservation(ev, reservation);
        afficherToast("Reservation confirmee.", true);
        chargerEvents();
    }

    private void afficherQrReservation(Event ev, Reservation reservation) {
        String payload = "ONCOREMINDER|event=" + ev.getId() + "|user=" + currentUserId() + "|date=" + LocalDateTime.now();
        ImageView qr = new ImageView(genererQr(payload, 220));
        qr.setFitWidth(220);
        qr.setFitHeight(220);
        Label label = new Label("Reservation : " + ev.getTitre());
        label.setStyle("-fx-text-fill:#3A1D7A; -fx-font-size:16px; -fx-font-weight:bold;");
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(tableEvent.getScene().getWindow());
        dialog.setTitle("QR reservation");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        VBox box = new VBox(14, label, qr, new Label("Code a presenter a l'accueil"));
        box.getStyleClass().add("form-panel");
        box.setStyle("-fx-padding:24;-fx-alignment:center;");
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private WritableImage genererQr(String text, int size) {
        WritableImage image = new WritableImage(size, size);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    image.getPixelWriter().setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException ex) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean dark = ((x / 12) + (y / 12) + text.hashCode()) % 3 == 0;
                    image.getPixelWriter().setColor(x, y, dark ? Color.BLACK : Color.WHITE);
                }
            }
        }
        return image;
    }

    private void mettreAJourCarte(String lieu) {
        String clean = safe(lieu).trim();
        if (clean.isEmpty()) {
            if (lbMapsInfo != null) lbMapsInfo.setText("Saisissez un lieu pour voir la carte");
            if (lbMapsCoords != null) lbMapsCoords.setText("");
            if (webMap != null) chargerLeaflet(webMap, 36.8065, 10.1815, 11, "Tunis");
            return;
        }
        if (lbMapsInfo != null) lbMapsInfo.setText("Recherche OpenStreetMap : " + clean);
        geocoderLieu(clean, webMap, lbMapsInfo, lbMapsCoords);
    }

    private void chargerMap(WebView view, String lieu) {
        geocoderLieu(safe(lieu), view, null, null);
    }

    private void ouvrirMaps(String lieu) {
        if (lieu == null || lieu.isBlank()) {
            afficherToast("Saisissez un lieu.", false);
            return;
        }
        try {
            String encoded = URLEncoder.encode(lieu, StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(URI.create("https://www.openstreetmap.org/search?query=" + encoded));
        } catch (Exception ex) {
            afficherToast("Impossible d'ouvrir OpenStreetMap.", false);
        }
    }

    private void geocoderLieu(String lieu, WebView targetMap, Label infoLabel, Label coordsLabel) {
        if (lieu == null || lieu.isBlank()) {
            if (targetMap != null) chargerLeaflet(targetMap, 36.8065, 10.1815, 11, "Tunis");
            return;
        }
        String encoded = URLEncoder.encode(lieu, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1"))
                .header("User-Agent", "OncoReminder-Pro-JavaFX")
                .GET()
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(json -> Platform.runLater(() -> {
                    double[] coords = extraireCoordsNominatim(json);
                    double lat = coords[0];
                    double lon = coords[1];
                    boolean found = !Double.isNaN(lat) && !Double.isNaN(lon);
                    if (!found) {
                        lat = 36.8065;
                        lon = 10.1815;
                    }
                    if (targetMap != null) chargerLeaflet(targetMap, lat, lon, found ? 15 : 11, lieu);
                    if (infoLabel != null) infoLabel.setText(found ? "Lieu localise : " + lieu : "Lieu non trouve, carte centree sur Tunis");
                    if (coordsLabel != null) coordsLabel.setText(found ? String.format(Locale.US, "Lat %.5f, Lng %.5f", lat, lon) : "");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (targetMap != null) chargerLeaflet(targetMap, 36.8065, 10.1815, 11, lieu);
                        if (infoLabel != null) infoLabel.setText("OpenStreetMap indisponible pour le moment");
                    });
                    return null;
                });
    }

    private double[] extraireCoordsNominatim(String json) {
        Matcher latMatcher = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"").matcher(safe(json));
        Matcher lonMatcher = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"").matcher(safe(json));
        if (latMatcher.find() && lonMatcher.find()) {
            try {
                return new double[]{Double.parseDouble(latMatcher.group(1)), Double.parseDouble(lonMatcher.group(1))};
            } catch (NumberFormatException ignored) {
                return new double[]{Double.NaN, Double.NaN};
            }
        }
        return new double[]{Double.NaN, Double.NaN};
    }

    private void chargerLeaflet(WebView view, double lat, double lon, int zoom, String label) {
        String safeLabel = safe(label).replace("\\", "\\\\").replace("'", "\\'");
        String html = """
                <!doctype html>
                <html>
                <head>
                  <meta charset='utf-8'>
                  <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>
                  <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
                  <style>
                    html, body, #map { height:100%%; margin:0; background:#FAFAFA; }
                    .leaflet-container { font-family: Segoe UI, Arial, sans-serif; border-radius:8px; }
                  </style>
                </head>
                <body>
                  <div id='map'></div>
                  <script>
                    const map = L.map('map', { zoomControl: true }).setView([%s, %s], %d);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      maxZoom: 19,
                      attribution: '&copy; OpenStreetMap'
                    }).addTo(map);
                    L.marker([%s, %s]).addTo(map).bindPopup('%s').openPopup();
                    setTimeout(() => map.invalidateSize(), 250);
                  </script>
                </body>
                </html>
                """.formatted(lat, lon, zoom, lat, lon, safeLabel);
        view.getEngine().loadContent(html);
    }

    private void afficherToast(String message, boolean success) {
        lbMessage.setText(message);
        if (toastPane == null || toastLabel == null) return;
        toastLabel.setText(message);
        toastLabel.setStyle((success
                ? "-fx-background-color:#0D9488;"
                : "-fx-background-color:#EF4444;")
                + "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;"
                + "-fx-background-radius:10; -fx-padding:10 16 10 16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),12,0,0,4);");
        toastPane.setManaged(true);
        toastPane.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(160), toastPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.playFromStart();
        PauseTransition pause = new PauseTransition(Duration.seconds(2.4));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toastPane);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(done -> {
                toastPane.setVisible(false);
                toastPane.setManaged(false);
            });
            fadeOut.play();
        });
        pause.play();
    }

    private void setAiLoading(boolean loading) {
        if (aiSpinner == null) return;
        aiSpinner.setVisible(loading);
        aiSpinner.setManaged(loading);
    }

    private Image chargerImageTable(String path) {
        if (path != null && !path.isBlank()) {
            File f = new File(path);
            if (f.exists()) return new Image(f.toURI().toString(), 640, 240, true, true, true);
        }
        WritableImage placeholder = new WritableImage(640, 240);
        for (int y = 0; y < 240; y++) {
            for (int x = 0; x < 640; x++) {
                double t = (x + y) / 880.0;
                placeholder.getPixelWriter().setColor(
                        x,
                        y,
                        Color.color(0.12 + t * 0.18, 0.08 + t * 0.10, 0.30 + t * 0.34)
                );
            }
        }
        return placeholder;
    }

    private void chargerImagePreview(String path) {
        Image img = null;
        if (path != null && !path.isBlank()) {
            File f = new File(path);
            if (f.exists()) img = new Image(f.toURI().toString(), true);
        }
        imagePreview.setImage(img);
        imageHint.setVisible(img == null);
    }

    private String statut(Event ev) {
        if (ev.getPlacesRestantes() <= 0) return "Complet";
        double ratio = ev.getPlacesRestantes() / (double) Math.max(1, ev.getCapacite());
        if (ratio <= 0.2) return "Bientot complet";
        return "Disponible";
    }

    private String statusClass(Event ev) {
        return switch (statut(ev)) {
            case "Complet" -> "event-badge-red";
            case "Bientot complet" -> "stat-badge-warning";
            default -> "stat-badge-success";
        };
    }

    private String compteARebours(Event ev) {
        LocalDateTime now = LocalDateTime.now();
        if (!ev.getDateEvent().isAfter(now)) return "Evenement passe";
        long hours = java.time.Duration.between(now, ev.getDateEvent()).toHours();
        long days = hours / 24;
        long restHours = hours % 24;
        return days + "j " + restHours + "h";
    }

    private int compteurVues(Event ev) {
        return Math.abs((ev.getId() * 37 + safe(ev.getTitre()).hashCode()) % 900) + 100;
    }

    private void exporterEvenementsPdf(File dest) throws IOException, DocumentException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 42, 42);
        PdfWriter.getInstance(doc, new FileOutputStream(dest));
        doc.open();
        Font title = new Font(Font.HELVETICA, 20, Font.BOLD, new java.awt.Color(91, 53, 165));
        Font head = new Font(Font.HELVETICA, 11, Font.BOLD, new java.awt.Color(79, 70, 229));
        Font body = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(31, 41, 55));
        doc.add(new Paragraph("OncoReminder PRO - Evenements", title));
        doc.add(new Paragraph("Export du " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), body));
        doc.add(Chunk.NEWLINE);
        for (Event ev : tableEvent.getItems()) {
            Paragraph p = new Paragraph(ev.getTitre(), head);
            p.setSpacingBefore(8);
            doc.add(p);
            doc.add(new Paragraph("Lieu: " + ev.getLieu()
                    + " | Date: " + ev.getDateEvent().format(DATE_TABLE_FMT)
                    + " | Capacite: " + ev.getCapacite()
                    + " | Restantes: " + ev.getPlacesRestantes()
                    + " | Statut: " + statut(ev), body));
            if (ev.getDescription() != null && !ev.getDescription().isBlank()) {
                Paragraph desc = new Paragraph(ev.getDescription(), body);
                desc.setAlignment(Element.ALIGN_JUSTIFIED);
                doc.add(desc);
            }
        }
        doc.close();
    }

    private int currentUserId() {
        return UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getId()
                : 1;
    }

    private String descriptionLocale(String titre, String lieu) {
        String endroit = (lieu == null || lieu.isBlank()) ? "un lieu a confirmer" : lieu;
        return "Participez a \"" + titre + "\" a " + endroit
                + ". Cet evenement propose un temps d'information, d'echange et de sensibilisation autour de l'oncologie.";
    }

    private String extraireTexteHuggingFace(String json) {
        Matcher m = Pattern.compile("\"generated_text\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL).matcher(json);
        if (m.find()) {
            String text = jsonUnescape(m.group(1));
            int idx = text.lastIndexOf("Lieu:");
            return idx >= 0 ? text.substring(idx).replaceFirst("Lieu:[^.]*\\.", "").trim() : text.trim();
        }
        return (json != null && !json.isBlank()) ? json : "Description indisponible.";
    }

    private String value(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String value(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String jsonEscape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String jsonUnescape(String v) {
        return v.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
