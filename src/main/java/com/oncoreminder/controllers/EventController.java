package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Event;
import com.oncoreminder.services.ServiceEvent;
import com.oncoreminder.utils.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
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
    @FXML private TextArea  taDescription;
    @FXML private DatePicker dpDate;
    @FXML private ImageView  imagePreview;
    @FXML private Label      imageHint;
    @FXML private Label      formTitle;
    @FXML private Label      lbMessage;
    @FXML private Label      lbTotalEvents;
    @FXML private Label      lbReservationsActives;
    @FXML private Label      lbEventsActifs;

    @FXML private Button btnFiltreTous;
    @FXML private Button btnFiltreAujourdhui;
    @FXML private Button btnFiltreSemaine;
    @FXML private Button btnFiltreComplet;
    @FXML private Button btnFiltreDisponible;

    @FXML private TableView<Event>            tableEvent;
    @FXML private TableColumn<Event, String>  colImage;
    @FXML private TableColumn<Event, String>  colTitre;
    @FXML private TableColumn<Event, String>  colLieu;
    @FXML private TableColumn<Event, String>  colDate;
    @FXML private TableColumn<Event, Integer> colCapacite;
    @FXML private TableColumn<Event, Integer> colPlaces;
    @FXML private TableColumn<Event, Void>    colActions;

    @FXML private ScrollPane formulairePane;

    private Runnable onOuvrirReservations;
    public void setOnOuvrirReservations(Runnable r) { this.onOuvrirReservations = r; }

    private final ServiceEvent serviceEvent = new ServiceEvent();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObservableList<Event> events = FXCollections.observableArrayList();
    private Event eventSelectionne = null;
    private String filtreActif = "TOUS";

    private static final DateTimeFormatter DATE_TABLE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DEFAULT_HF_MODEL = "HuggingFaceH4/zephyr-7b-beta";

    @FXML
    void initialize() {
        colTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLieu()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateEvent().format(DATE_TABLE_FMT)));
        colCapacite.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCapacite()).asObject());
        colPlaces.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getPlacesRestantes()).asObject());
        configurerColonneImage();
        if (colActions != null) configurerActions();

        tableEvent.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> eventSelectionne = sel);
        tfRecherche.textProperty().addListener((obs, old, val) -> appliquerFiltres());

        dpDate.setValue(LocalDate.now());
        tfHeure.setText("10:00");
        chargerEvents();
        preparerCreation();
    }

    @FXML void handleAjouterClick() {
        boolean visible = formulairePane.isVisible();
        formulairePane.setVisible(!visible);
        formulairePane.setManaged(!visible);
        if (!visible) { tableEvent.getSelectionModel().clearSelection(); preparerCreation(); }
    }

    @FXML void ouvrirFormulaireModification(ActionEvent e) {
        if (eventSelectionne == null) { lbMessage.setText("Sélectionnez un événement à modifier."); return; }
        formulairePane.setVisible(true); formulairePane.setManaged(true);
        remplirFormulaire(eventSelectionne);
    }

    @FXML void enregistrerEvent(ActionEvent e) {
        if (eventSelectionne == null) ajouterEvent(e); else modifierEvent(e);
    }

    @FXML void ajouterEvent(ActionEvent e) {
        try {
            Event ev = lireFormulaire();
            int createurId = UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getId() : 1;
            serviceEvent.add(ev, createurId);
            lbMessage.setText("✅ Événement créé.");
            chargerEvents(); preparerCreation();
            formulairePane.setVisible(false); formulairePane.setManaged(false);
        } catch (Exception ex) { lbMessage.setText("❌ Erreur : " + ex.getMessage()); }
    }

    @FXML void modifierEvent(ActionEvent e) {
        if (eventSelectionne == null) { lbMessage.setText("Sélectionnez un événement."); return; }
        try {
            Event v = lireFormulaire();
            eventSelectionne.setTitre(v.getTitre()); eventSelectionne.setLieu(v.getLieu());
            eventSelectionne.setDescription(v.getDescription()); eventSelectionne.setCapacite(v.getCapacite());
            eventSelectionne.setDateEvent(v.getDateEvent()); eventSelectionne.setImagePath(v.getImagePath());
            serviceEvent.update(eventSelectionne);
            lbMessage.setText("✅ Événement modifié.");
            chargerEvents(); preparerCreation();
            formulairePane.setVisible(false); formulairePane.setManaged(false);
        } catch (Exception ex) { lbMessage.setText("❌ Erreur : " + ex.getMessage()); }
    }

    @FXML void supprimerEvent(ActionEvent e) {
        if (eventSelectionne == null) { lbMessage.setText("Sélectionnez un événement."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer « " + eventSelectionne.getTitre() + " » ?", ButtonType.OK, ButtonType.CANCEL);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                serviceEvent.delete(eventSelectionne);
                lbMessage.setText("✅ Événement supprimé.");
                chargerEvents(); preparerCreation();
                formulairePane.setVisible(false); formulairePane.setManaged(false);
            }
        });
    }

    @FXML void filtrerTous(ActionEvent e)       { changerFiltre("TOUS"); }
    @FXML void filtrerAujourdhui(ActionEvent e) { changerFiltre("AUJOURDHUI"); }
    @FXML void filtrerSemaine(ActionEvent e)    { changerFiltre("SEMAINE"); }
    @FXML void filtrerComplet(ActionEvent e)    { changerFiltre("COMPLET"); }
    @FXML void filtrerDisponible(ActionEvent e) { changerFiltre("DISPONIBLE"); }

    @FXML void choisirImage(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.bmp"));
        File file = chooser.showOpenDialog(tfTitre.getScene().getWindow());
        if (file != null) {
            tfImagePath.setText(file.getAbsolutePath());
            imagePreview.setImage(new Image(file.toURI().toString(), true));
            imageHint.setVisible(false);
        }
    }

    @FXML void genererDescription(ActionEvent e) {
        String titre = tfTitre.getText().trim();
        String lieu  = tfLieu.getText().trim();
        if (titre.isEmpty()) { lbMessage.setText("Saisissez un titre avant de générer."); return; }
        String token = System.getenv("HF_API_TOKEN");
        if (token == null || token.isBlank()) {
            taDescription.setText(descriptionLocale(titre, lieu));
            lbMessage.setText("Description locale générée. (Ajoutez HF_API_TOKEN pour IA)");
            return;
        }
        lbMessage.setText("Génération IA en cours...");
        String model  = System.getenv().getOrDefault("HF_MODEL", DEFAULT_HF_MODEL);
        String prompt = "Rédige une description courte en français pour un événement médical oncologique. Titre: " + titre + ". Lieu: " + (lieu.isBlank() ? "non précisé" : lieu) + ".";
        String body   = "{\"inputs\":\"" + jsonEscape(prompt) + "\",\"parameters\":{\"max_new_tokens\":90,\"temperature\":0.7}}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://api-inference.huggingface.co/models/" + model))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenApply(this::extraireTexteHuggingFace)
            .exceptionally(ex -> descriptionLocale(titre, lieu))
            .thenAccept(desc -> Platform.runLater(() -> {
                taDescription.setText(desc); lbMessage.setText("✅ Description générée.");
            }));
    }

    @FXML void ouvrirReservations(ActionEvent e) {
        if (onOuvrirReservations != null) onOuvrirReservations.run();
        else App.navigate("GestionReservation");
    }
    @FXML void handleBack(ActionEvent e)          { App.navigate("DoctorDashboard"); }
    @FXML void handleArticles(ActionEvent e)      { App.navigate("ArticleList"); }
    @FXML void handleLogout(ActionEvent e)        { UserSession.getInstance().logout(); App.navigate("Login"); }

    private void configurerColonneImage() {
        colImage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getImagePath()));
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            { iv.setFitWidth(56); iv.setFitHeight(42); iv.setPreserveRatio(true); }
            @Override protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isBlank()) { setGraphic(null); setText("—"); }
                else {
                    File f = new File(path);
                    if (f.exists()) { iv.setImage(new Image(f.toURI().toString(), 56, 42, true, true, true)); setGraphic(iv); setText(null); }
                    else { setGraphic(null); setText("—"); }
                }
            }
        });
    }

    private void configurerActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button edit    = new Button("✎");
            private final Button delete  = new Button("🗑");
            private final Button reserve = new Button("🎟");
            private final HBox box = new HBox(7, edit, delete, reserve);
            {
                edit.getStyleClass().add("event-icon-purple");
                delete.getStyleClass().add("event-icon-red");
                reserve.getStyleClass().add("event-icon-teal");
                edit.setOnAction(e -> {
                    eventSelectionne = getTableView().getItems().get(getIndex());
                    tableEvent.getSelectionModel().select(eventSelectionne);
                    formulairePane.setVisible(true); formulairePane.setManaged(true);
                    remplirFormulaire(eventSelectionne);
                });
                delete.setOnAction(e -> {
                    eventSelectionne = getTableView().getItems().get(getIndex());
                    tableEvent.getSelectionModel().select(eventSelectionne);
                    supprimerEvent(e);
                });
                reserve.setOnAction(e -> ouvrirReservations(e));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : box);
            }
        });
    }

    private void chargerEvents() {
        events.setAll(serviceEvent.getAll());
        lbTotalEvents.setText(String.valueOf(events.size()));
        lbEventsActifs.setText(String.valueOf(events.stream()
            .filter(ev -> ev.getDateEvent().isAfter(LocalDateTime.now())).count()));
        lbReservationsActives.setText(String.valueOf(serviceEvent.countReservationsActives()));
        appliquerFiltres();
    }

    private void appliquerFiltres() {
        String q = tfRecherche.getText() == null ? "" : tfRecherche.getText().toLowerCase(Locale.ROOT).trim();
        LocalDate today = LocalDate.now();
        List<Event> filtres = events.stream()
            .filter(ev -> q.isEmpty()
                || ev.getTitre().toLowerCase(Locale.ROOT).contains(q)
                || (ev.getLieu() != null && ev.getLieu().toLowerCase(Locale.ROOT).contains(q)))
            .filter(ev -> switch (filtreActif) {
                case "AUJOURDHUI"  -> ev.getDateEvent().toLocalDate().equals(today);
                case "SEMAINE"     -> !ev.getDateEvent().toLocalDate().isBefore(today)
                                      && !ev.getDateEvent().toLocalDate().isAfter(today.plusDays(7));
                case "COMPLET"     -> ev.getPlacesRestantes() <= 0;
                case "DISPONIBLE"  -> ev.getPlacesRestantes() > 0;
                default            -> true;
            })
            .collect(Collectors.toCollection(ArrayList::new));
        tableEvent.setItems(FXCollections.observableArrayList(filtres));
        lbMessage.setText(filtres.size() + " événement(s)");
    }

    private void changerFiltre(String filtre) {
        filtreActif = filtre;
        List.of(btnFiltreTous, btnFiltreAujourdhui, btnFiltreSemaine, btnFiltreComplet, btnFiltreDisponible)
            .forEach(b -> b.getStyleClass().setAll("event-tab"));
        switch (filtre) {
            case "AUJOURDHUI" -> btnFiltreAujourdhui.getStyleClass().setAll("event-tab-active");
            case "SEMAINE"    -> btnFiltreSemaine.getStyleClass().setAll("event-tab-active");
            case "COMPLET"    -> btnFiltreComplet.getStyleClass().setAll("event-tab-active");
            case "DISPONIBLE" -> btnFiltreDisponible.getStyleClass().setAll("event-tab-active");
            default           -> btnFiltreTous.getStyleClass().setAll("event-tab-active");
        }
        appliquerFiltres();
    }

    private Event lireFormulaire() {
        String titre = tfTitre.getText().trim();
        String lieu  = tfLieu.getText().trim();
        if (titre.isEmpty()) throw new IllegalArgumentException("titre obligatoire");
        if (lieu.isEmpty())  throw new IllegalArgumentException("lieu obligatoire");
        if (dpDate.getValue() == null) throw new IllegalArgumentException("date obligatoire");
        LocalTime heure = LocalTime.parse(tfHeure.getText().trim(), TIME_FMT);
        int capacite = Integer.parseInt(tfCapacite.getText().trim());
        Event ev = new Event(titre, taDescription.getText().trim(), LocalDateTime.of(dpDate.getValue(), heure), lieu, capacite);
        ev.setImagePath(tfImagePath.getText().isBlank() ? null : tfImagePath.getText());
        return ev;
    }

    private void remplirFormulaire(Event ev) {
        formTitle.setText("Modifier un Événement");
        tfTitre.setText(ev.getTitre()); tfLieu.setText(ev.getLieu());
        taDescription.setText(ev.getDescription()); tfCapacite.setText(String.valueOf(ev.getCapacite()));
        dpDate.setValue(ev.getDateEvent().toLocalDate());
        tfHeure.setText(ev.getDateEvent().toLocalTime().format(TIME_FMT));
        tfImagePath.setText(ev.getImagePath() == null ? "" : ev.getImagePath());
        chargerImage(ev.getImagePath());
    }

    private void preparerCreation() {
        eventSelectionne = null; formTitle.setText("Ajouter un Événement");
        tfTitre.clear(); tfLieu.clear(); taDescription.clear();
        tfCapacite.clear(); tfImagePath.clear();
        imagePreview.setImage(null); imageHint.setVisible(true);
        dpDate.setValue(LocalDate.now()); tfHeure.setText("10:00");
    }

    private void chargerImage(String path) {
        if (path == null || path.isBlank()) { imagePreview.setImage(null); imageHint.setVisible(true); return; }
        File f = new File(path);
        if (f.exists()) { imagePreview.setImage(new Image(f.toURI().toString(), true)); imageHint.setVisible(false); }
        else { imagePreview.setImage(null); imageHint.setVisible(true); }
    }

    private String descriptionLocale(String titre, String lieu) {
        String endroit = (lieu == null || lieu.isBlank()) ? "un lieu à confirmer" : lieu;
        return "Participez à \"" + titre + "\" à " + endroit + ". Cet événement propose un temps d'information, d'échange et de sensibilisation autour de l'oncologie.";
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

    private String jsonEscape(String v)   { return v.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n"); }
    private String jsonUnescape(String v) { return v.replace("\\n","\n").replace("\\\"","\"").replace("\\\\","\\"); }
}