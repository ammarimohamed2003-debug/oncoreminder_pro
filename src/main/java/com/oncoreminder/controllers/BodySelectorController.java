package com.oncoreminder.controllers;

import com.oncoreminder.models.Cancer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BodySelectorController implements Initializable {

    @FXML private TextField    tfSearch, tfNom, tfOrgane;
    @FXML private ListView<String> listCancers;
    @FXML private Pane         bodyPane;
    @FXML private Label        lblTitle, lblSelectedZone;
    @FXML private ComboBox<String> cbClassification, cbStade;
    @FXML private TextArea     taDescription, taSymptomes;
    @FXML private Button       btnEnregistrer;
    @FXML private VBox         rightFormPanel;
    @FXML private VBox         organeInfoPanel;
    @FXML private ListView<String> listCancerTypes;
    @FXML private Label        suggestedTagsLabel;
    @FXML private HBox         confirmBar;
    @FXML private Label        selectedOrganesLabel;

    private Consumer<Cancer>  onSaveCallback;
    private Consumer<String>  onOrganeCallback;
    private Consumer<String>  onTagsCallback;
    private boolean           organeMode  = false;
    private boolean           isEditMode  = false;
    private int               editId      = 0;
    private boolean           autoFilling = false;
    private String            highlightedZone = null;

    // Multi-sélection (organeMode)
    private final Set<String> selectedOrganes     = new LinkedHashSet<>();
    private final Set<String> selectedZoneIds     = new LinkedHashSet<>();
    private final Set<String> selectedCancerTypes = new LinkedHashSet<>();

    private final Map<String, List<Shape>> zoneShapes = new LinkedHashMap<>();
    private final Map<String, String[]>    zoneColors = new LinkedHashMap<>();

    // Labels affichés pour chaque zone (mode sélection organe)
    private static final Map<String, String> ZONE_LABELS = new LinkedHashMap<>();
    static {
        ZONE_LABELS.put("tete",      "Tête");
        ZONE_LABELS.put("cerveau",   "Cerveau");
        ZONE_LABELS.put("gorge",     "Gorge / Cou");
        ZONE_LABELS.put("thyroide",  "Thyroïde");
        ZONE_LABELS.put("sein",      "Sein");
        ZONE_LABELS.put("poumon",    "Poumons");
        ZONE_LABELS.put("coeur",     "Cœur");
        ZONE_LABELS.put("estomac",   "Estomac");
        ZONE_LABELS.put("foie",      "Foie");
        ZONE_LABELS.put("pancreas",  "Pancréas");
        ZONE_LABELS.put("rein",      "Reins");
        ZONE_LABELS.put("colon",     "Côlon / Intestin");
        ZONE_LABELS.put("pelvis",    "Pelvis");
        ZONE_LABELS.put("os",        "Os");
        ZONE_LABELS.put("muscles",   "Muscles");
        ZONE_LABELS.put("peau",      "Peau");
        ZONE_LABELS.put("ganglions", "Ganglions");
        ZONE_LABELS.put("moelle",    "Moelle osseuse");
    }

    // ═══════════════════════════════════════════════════════════════
    //  DONNÉES CANCERS
    // ═══════════════════════════════════════════════════════════════
    private static final List<CancerEntry> ALL_CANCERS = new ArrayList<>();
    static {
        add("Cancer du sein",                    "Carcinome",       "Sein",                       "sein",      "Tumeur maligne du tissu mammaire, cancer le plus fréquent chez la femme.", "Boule dans le sein, modification de la peau, écoulement du mamelon, douleur.");
        add("Cancer du poumon",                  "Carcinome",       "Poumon",                     "poumon",    "Tumeur broncho-pulmonaire souvent liée au tabagisme.", "Toux persistante, essoufflement, douleur thoracique, crachats sanglants.");
        add("Cancer du côlon",                   "Carcinome",       "Côlon",                      "colon",     "Tumeur maligne du côlon, généralement précédée de polypes.", "Changement du transit, sang dans les selles, douleurs abdominales.");
        add("Cancer colorectal",                 "Carcinome",       "Côlon / Rectum",             "colon",     "Cancer touchant le côlon ou le rectum.", "Sang dans les selles, douleurs abdominales, modification du transit.");
        add("Cancer de la prostate",             "Carcinome",       "Prostate",                   "pelvis",    "Tumeur de la glande prostatique, le plus fréquent chez l'homme après 50 ans.", "Difficultés à uriner, mictions fréquentes, douleurs pelviennes.");
        add("Cancer de l'ovaire",                "Carcinome",       "Ovaire",                     "pelvis",    "Tumeur maligne des ovaires, souvent détectée tardivement.", "Ballonnements, douleurs pelviennes, troubles urinaires.");
        add("Cancer du col de l'utérus",         "Carcinome",       "Col de l'utérus",            "pelvis",    "Cancer développé au niveau du col utérin, causé principalement par le HPV.", "Saignements anormaux, pertes vaginales, douleurs pelviennes.");
        add("Cancer de l'utérus",                "Carcinome",       "Utérus",                     "pelvis",    "Tumeur maligne de l'endomètre.", "Saignements vaginaux anormaux, douleurs pelviennes.");
        add("Cancer de l'estomac",               "Carcinome",       "Estomac",                    "estomac",   "Adénocarcinome gastrique souvent lié à Helicobacter pylori.", "Douleurs épigastriques, nausées, perte d'appétit, amaigrissement.");
        add("Cancer du foie",                    "Carcinome",       "Foie",                       "foie",      "Carcinome hépatocellulaire associé à cirrhose ou hépatite.", "Douleur droite, jaunisse, perte de poids, abdomen enflé.");
        add("Cancer du pancréas",                "Carcinome",       "Pancréas",                   "pancreas",  "Adénocarcinome pancréatique de mauvais pronostic.", "Jaunisse, douleurs abdominales irradiant dans le dos, perte de poids.");
        add("Cancer de la thyroïde",             "Carcinome",       "Thyroïde",                   "thyroide",  "Tumeur maligne de la glande thyroïde.", "Nodule dans le cou, modification de la voix, difficultés à avaler.");
        add("Cancer du rein",                    "Carcinome",       "Rein",                       "rein",      "Carcinome à cellules rénales claires.", "Sang dans les urines, douleur dans le flanc, masse abdominale.");
        add("Cancer de la vessie",               "Carcinome",       "Vessie",                     "pelvis",    "Tumeur urothéliale de la paroi vésicale.", "Sang dans les urines, mictions douloureuses.");
        add("Cancer de l'œsophage",              "Carcinome",       "Œsophage",                   "gorge",     "Tumeur maligne de l'œsophage.", "Difficultés à avaler, perte de poids, douleurs thoraciques.");
        add("Cancer du larynx",                  "Carcinome",       "Larynx",                     "gorge",     "Tumeur maligne du larynx.", "Enrouement persistant, douleurs à la gorge, difficultés à avaler.");
        add("Cancer de la bouche",               "Carcinome",       "Cavité buccale",             "tete",      "Tumeur maligne de la cavité buccale.", "Plaie qui ne guérit pas, taches, douleurs buccales.");
        add("Mélanome cutané",                   "Mélanome",        "Peau",                       "peau",      "Cancer malin des mélanocytes, lié à l'exposition aux UV.", "Grain de beauté asymétrique, bords irréguliers, couleur hétérogène.");
        add("Lymphome de Hodgkin",               "Lymphome",        "Ganglions lymphatiques",     "ganglions", "Cancer du système lymphatique avec cellules de Reed-Sternberg.", "Ganglions indolores, fièvre, sueurs nocturnes, perte de poids.");
        add("Lymphome non hodgkinien",           "Lymphome",        "Ganglions lymphatiques",     "ganglions", "Groupe hétérogène de cancers lymphatiques.", "Ganglions enflés, fièvre, sueurs nocturnes, fatigue.");
        add("Leucémie aiguë lymphoblastique",    "Leucémie",        "Moelle osseuse",             "moelle",    "Cancer hématologique agressif touchant les lymphoblastes.", "Fatigue intense, pâleur, fièvre, infections, saignements.");
        add("Leucémie aiguë myéloblastique",     "Leucémie",        "Moelle osseuse",             "moelle",    "Cancer hématologique agressif touchant les cellules myéloïdes.", "Fatigue, pâleur, fièvre, saignements, ecchymoses.");
        add("Leucémie chronique lymphocytaire",  "Leucémie",        "Sang, moelle",               "moelle",    "Cancer d'évolution lente touchant les lymphocytes B.", "Souvent asymptomatique, ganglions, fatigue.");
        add("Leucémie chronique myéloïde",       "Leucémie",        "Moelle osseuse",             "moelle",    "Cancer lié au chromosome Philadelphie.", "Fatigue, splénomégalie, douleurs abdominales.");
        add("Myélome multiple",                  "Myélome",         "Moelle osseuse",             "moelle",    "Cancer des plasmocytes.", "Douleurs osseuses, fractures, fatigue, infections répétées.");
        add("Glioblastome",                      "Gliome",          "Cerveau",                    "cerveau",   "Forme la plus agressive des gliomes, grade IV.", "Céphalées intenses, crises d'épilepsie, déficits moteurs.");
        add("Astrocytome",                       "Gliome",          "Cerveau",                    "cerveau",   "Tumeur dérivant des astrocytes.", "Céphalées, crises d'épilepsie, troubles visuels.");
        add("Méningiome",                        "Tumeur méningée", "Méninges",                   "cerveau",   "Tumeur des méninges souvent bénigne.", "Céphalées, crises d'épilepsie, troubles visuels.");
        add("Médulloblastome",                   "Tumeur embryon.", "Cervelet",                   "cerveau",   "Tumeur maligne cérébrale pédiatrique la plus fréquente.", "Céphalées matinales, vomissements, troubles de l'équilibre.");
        add("Ostéosarcome",                      "Sarcome",         "Os",                         "os",        "Tumeur maligne osseuse la plus fréquente chez les adolescents.", "Douleur osseuse, gonflement, fracture pathologique.");
        add("Sarcome d'Ewing",                   "Sarcome",         "Os",                         "os",        "Tumeur osseuse agressive.", "Douleur osseuse intense, gonflement, fièvre, fatigue.");
        add("Sarcome de Kaposi",                 "Sarcome",         "Peau / Organes",             "peau",      "Tumeur vasculaire maligne associée à HHV-8.", "Lésions cutanées violacées, œdèmes.");
        add("Rhabdomyosarcome",                  "Sarcome",         "Muscles",                    "muscles",   "Tumeur maligne des muscles striés.", "Masse indolore, gonflement.");
        add("Tumeur de Wilms",                   "Tumeur pédiat.",  "Rein",                       "rein",      "Tumeur rénale maligne de l'enfant.", "Masse abdominale, hématurie, HTA, fièvre.");
        add("Mésothéliome",                      "Tumeur rare",     "Plèvre, péritoine",          "poumon",    "Tumeur de la séreuse liée à l'amiante.", "Dyspnée, douleur thoracique, épanchement pleural.");
        add("Phéochromocytome",                  "Tumeur endocrine","Glande surrénale",           "rein",      "Tumeur sécrétant des catécholamines.", "Hypertension paroxystique, céphalées, palpitations, sueurs.");
        add("Tumeur neuroendocrine",             "Tumeur endocrine","Système neuroendocrine",     "pancreas",  "Tumeur des cellules neuroendocrines.", "Douleurs abdominales, diarrhée, flush.");
        add("Cancer de la peau (non mélanome)",  "Carcinome",       "Peau",                       "peau",      "Carcinome basocellulaire ou épidermoïde cutané.", "Lésion cutanée qui ne guérit pas, croûte, ulcération.");
        add("Cancer du testicule",               "Carcinome",       "Testicule",                  "pelvis",    "Tumeur germinale testiculaire.", "Masse ou gonflement testiculaire indolore.");
        add("Neuroblastome",                     "Tumeur pédiat.",  "Système sympathique",        "cerveau",   "Tumeur sympathique de l'enfant.", "Masse abdominale, exophtalmie, ecchymoses.");
    }

    private static void add(String n, String c, String o, String z, String d, String s) {
        ALL_CANCERS.add(new CancerEntry(n, c, o, z, d, s));
    }

    // ═══════════════════════════════════════════════════════════════
    //  MODE SÉLECTION ORGANE (appelé depuis ArticleFormController)
    // ═══════════════════════════════════════════════════════════════
    public void setOrganeMode(Consumer<String> organeCallback, Consumer<String> tagsCallback) {
        this.organeMode       = true;
        this.onOrganeCallback = organeCallback;
        this.onTagsCallback   = tagsCallback;
        if (lblTitle != null) lblTitle.setText("🫁 Sélectionner un ou plusieurs organes");
        if (rightFormPanel != null) { rightFormPanel.setVisible(false);  rightFormPanel.setManaged(false); }
        if (organeInfoPanel != null) { organeInfoPanel.setVisible(true); organeInfoPanel.setManaged(true); }
        if (confirmBar != null)     { confirmBar.setVisible(true);       confirmBar.setManaged(true); }

        // Liste organes — multi-toggle
        listCancers.getItems().setAll(new ArrayList<>(ZONE_LABELS.values()));
        listCancers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listCancers.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                if (selectedOrganes.contains(item)) {
                    setText("✓  " + item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2BBCB0; -fx-padding: 6 10;");
                } else {
                    setText("     " + item);
                    setStyle("-fx-text-fill: #3A2060; -fx-padding: 6 10;");
                }
            }
        });

        // Liste types de cancer — multi-toggle optionnel
        listCancerTypes.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listCancerTypes.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                if (selectedCancerTypes.contains(item)) {
                    setText("✓  " + item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2BBCB0; -fx-font-size: 11px; -fx-padding: 5 8;");
                } else {
                    setText("     " + item);
                    setStyle("-fx-text-fill: #3A2060; -fx-font-size: 11px; -fx-padding: 5 8;");
                }
            }
        });
        listCancerTypes.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            if (selectedCancerTypes.contains(val)) selectedCancerTypes.remove(val);
            else                                   selectedCancerTypes.add(val);
            listCancerTypes.refresh();
            updateSuggestedTags();
            Platform.runLater(() -> listCancerTypes.getSelectionModel().clearSelection());
        });

        updateSuggestedTags();
    }

    // Backward-compat overload (tags callback optional)
    public void setOrganeMode(Consumer<String> organeCallback) {
        setOrganeMode(organeCallback, null);
    }

    // ═══════════════════════════════════════════════════════════════
    //  MODE CANCER (ajout / modification)
    // ═══════════════════════════════════════════════════════════════
    public void setOnSave(Consumer<Cancer> cb) { this.onSaveCallback = cb; }

    public void setAddMode() {
        isEditMode = false; editId = 0;
        if (lblTitle != null) lblTitle.setText("➕ Ajouter un cancer");
        resetForm();
    }

    public void setEditMode(Cancer c) {
        isEditMode = true; editId = c.getId();
        if (lblTitle != null) lblTitle.setText("✏️ Modifier — " + c.getNom());
        autoFilling = true;
        tfNom.setText(c.getNom());
        cbClassification.setValue(c.getClassification());
        cbStade.setValue(c.getStade());
        tfOrgane.setText(c.getOrgane());
        taDescription.setText(c.getDescription());
        taSymptomes.setText(c.getSymptomes());
        autoFilling = false;
        updateValidateBtn();
        highlightZoneForOrgane(c.getOrgane());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbClassification.setItems(FXCollections.observableArrayList(
                "Carcinome", "Sarcome", "Lymphome", "Leucémie", "Mélanome",
                "Myélome", "Gliome", "Tumeur endocrine", "Tumeur gynécologique",
                "Tumeur pédiatrique", "Tumeur rare", "Autre"));
        cbStade.setItems(FXCollections.observableArrayList("0","I","II","III","IV"));

        drawHumanBody3D();

        ALL_CANCERS.sort((a, b) -> a.nom.compareToIgnoreCase(b.nom));
        refreshList(ALL_CANCERS);

        listCancers.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            if (organeMode) {
                toggleOrgane(val);
                Platform.runLater(() -> listCancers.getSelectionModel().clearSelection());
                return;
            }
            ALL_CANCERS.stream().filter(c -> c.nom.equals(val)).findFirst()
                    .ifPresent(this::applySelection);
        });

        tfNom.textProperty().addListener((o, ov, nv) -> updateValidateBtn());
        cbClassification.valueProperty().addListener((o, ov, nv) -> updateValidateBtn());
        cbStade.valueProperty().addListener((o, ov, nv) -> updateValidateBtn());
        tfOrgane.textProperty().addListener((o, ov, nv) -> updateValidateBtn());
    }

    @FXML private void onSearch() {
        String q = tfSearch.getText().trim().toLowerCase();
        if (organeMode) {
            List<String> filtered = q.isEmpty()
                    ? new ArrayList<>(ZONE_LABELS.values())
                    : ZONE_LABELS.values().stream()
                        .filter(o -> o.toLowerCase().contains(q))
                        .collect(Collectors.toList());
            listCancers.getItems().setAll(filtered);
            return;
        }
        refreshList(q.isEmpty() ? ALL_CANCERS :
                ALL_CANCERS.stream().filter(c ->
                        c.nom.toLowerCase().contains(q) ||
                        c.cls.toLowerCase().contains(q) ||
                        c.organe.toLowerCase().contains(q) ||
                        c.desc.toLowerCase().contains(q)
                ).collect(Collectors.toList()));
    }

    private void refreshList(List<CancerEntry> entries) {
        listCancers.setItems(FXCollections.observableArrayList(
                entries.stream().map(c -> c.nom).collect(Collectors.toList())));
        listCancers.setTooltip(new Tooltip(entries.size() + " élément(s)"));
    }

    private void applySelection(CancerEntry e) {
        autoFilling = true;
        tfNom.setText(e.nom);
        cbClassification.setValue(e.cls);
        tfOrgane.setText(e.organe);
        taDescription.setText(e.desc);
        taSymptomes.setText(e.symp);
        String teal = "-fx-border-color: #2BBCB0; -fx-border-width: 2px; -fx-border-radius: 6px;";
        cbClassification.setStyle(teal); tfOrgane.setStyle(teal);
        taDescription.setStyle(teal); taSymptomes.setStyle(teal);
        if (lblSelectedZone != null) lblSelectedZone.setText("📍 " + e.organe);
        autoFilling = false;
        highlightZone(e.zone);
        updateValidateBtn();
    }

    private void applySelectionByZone(String zoneId) {
        if (organeMode) {
            String organe = ZONE_LABELS.getOrDefault(zoneId, zoneId);
            toggleOrgane(organe);
            return;
        }
        ALL_CANCERS.stream().filter(c -> c.zone.equals(zoneId)).findFirst().ifPresent(entry -> {
            applySelection(entry);
            int idx = listCancers.getItems().indexOf(entry.nom);
            if (idx >= 0) {
                listCancers.getSelectionModel().select(idx);
                listCancers.scrollTo(idx);
            }
        });
    }

    // ─── Multi-sélection organe ─────────────────────────────────────

    private void toggleOrgane(String organeName) {
        if (selectedOrganes.contains(organeName)) {
            selectedOrganes.remove(organeName);
            String zoneId = reverseZoneLookup(organeName);
            if (zoneId != null) selectedZoneIds.remove(zoneId);
        } else {
            selectedOrganes.add(organeName);
            String zoneId = reverseZoneLookup(organeName);
            if (zoneId != null) selectedZoneIds.add(zoneId);
        }
        listCancers.refresh();
        highlightSelectedZones();
        updateConfirmBar();
        updateOrganeInfoPanel();
    }

    private String reverseZoneLookup(String organeName) {
        return ZONE_LABELS.entrySet().stream()
            .filter(e -> e.getValue().equals(organeName))
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    private void highlightSelectedZones() {
        DropShadow glow = new DropShadow(20, Color.web("#2BBCB0", 0.9));
        glow.setSpread(0.4);
        boolean hasSelection = !selectedZoneIds.isEmpty();
        zoneShapes.forEach((id, shapes) -> {
            if (selectedZoneIds.contains(id)) {
                shapes.forEach(s -> { s.setOpacity(1.0); s.setEffect(glow); s.toFront(); });
            } else {
                shapes.forEach(s -> { s.setOpacity(hasSelection ? 0.38 : 0.75); s.setEffect(null); });
            }
        });
    }

    private void updateConfirmBar() {
        if (selectedOrganesLabel == null) return;
        if (selectedOrganes.isEmpty()) {
            selectedOrganesLabel.setText("Aucun organe sélectionné");
        } else {
            selectedOrganesLabel.setText(
                "Sélectionnés (" + selectedOrganes.size() + ") : " +
                String.join("  •  ", selectedOrganes));
        }
    }

    @FXML private void onConfirmer() {
        if (onOrganeCallback != null && !selectedOrganes.isEmpty())
            onOrganeCallback.accept(String.join(", ", selectedOrganes));
        if (onTagsCallback != null) {
            String tags = suggestedTagsLabel != null ? suggestedTagsLabel.getText() : "";
            if (!tags.isEmpty() && !tags.equals("—"))
                onTagsCallback.accept(tags);
        }
        closeStage();
    }

    @FXML private void onClearOrganes() {
        selectedOrganes.clear();
        selectedZoneIds.clear();
        selectedCancerTypes.clear();
        listCancers.refresh();
        if (listCancerTypes != null) {
            listCancerTypes.getItems().clear();
            listCancerTypes.refresh();
        }
        highlightSelectedZones();
        updateConfirmBar();
        updateSuggestedTags();
    }

    private void updateOrganeInfoPanel() {
        if (!organeMode || listCancerTypes == null) return;
        // Cancers liés aux zones sélectionnées
        List<String> cancers = ALL_CANCERS.stream()
            .filter(c -> selectedZoneIds.contains(c.zone))
            .map(c -> c.nom)
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.toList());
        listCancerTypes.setItems(FXCollections.observableArrayList(cancers));
        // Retirer les sélections qui ne sont plus pertinentes
        selectedCancerTypes.retainAll(new HashSet<>(cancers));
        listCancerTypes.refresh();
        updateSuggestedTags();
    }

    private void updateSuggestedTags() {
        if (!organeMode || suggestedTagsLabel == null) return;
        Set<String> tags = new LinkedHashSet<>();
        // Tags depuis les organes
        for (String organe : selectedOrganes) {
            tags.add(organe.toLowerCase()
                .replace(" / ", " ")
                .replace("œ", "oe")
                .replace("ô", "o")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("â", "a")
                .replace("î", "i")
                .replace("û", "u")
                .replace("ç", "c"));
        }
        if (!selectedOrganes.isEmpty()) tags.add("oncologie");
        // Tags depuis les types de cancer sélectionnés
        for (String cancerName : selectedCancerTypes) {
            ALL_CANCERS.stream().filter(c -> c.nom.equals(cancerName)).findFirst().ifPresent(c -> {
                tags.add(c.cls.toLowerCase());
                // Extraire le mot-clé principal du nom du cancer
                String keyword = cancerName.toLowerCase()
                    .replaceAll("^cancer d[eu]s? (la |l')?", "")
                    .replaceAll("^cancer ", "")
                    .replaceAll("^(lymphome|leucémie|mélanome|sarcome|myélome|gliome) ", "")
                    .replaceAll("é", "e").replaceAll("è", "e").replaceAll("ô", "o")
                    .replaceAll("î", "i").replaceAll("â", "a").replaceAll("û", "u")
                    .replaceAll("ç", "c").replaceAll("œ", "oe").trim();
                if (!keyword.isEmpty()) tags.add(keyword);
            });
        }
        String result = String.join(", ", tags);
        suggestedTagsLabel.setText(result.isEmpty() ? "—" : result);
    }

    private void highlightZoneForOrgane(String organe) {
        ALL_CANCERS.stream().filter(c -> c.organe.equalsIgnoreCase(organe))
                .findFirst().ifPresent(e -> highlightZone(e.zone));
    }

    @FXML private void onEnregistrer() {
        if (onSaveCallback == null) return;
        Cancer c = new Cancer();
        c.setId(editId);
        c.setNom(tfNom.getText().trim());
        c.setClassification(cbClassification.getValue());
        c.setStade(cbStade.getValue());
        c.setOrgane(tfOrgane.getText().trim());
        c.setDescription(taDescription.getText().trim());
        c.setSymptomes(taSymptomes.getText().trim());
        onSaveCallback.accept(c);
        closeStage();
    }

    @FXML private void onAnnuler() { closeStage(); }

    private void closeStage() {
        Stage stage = (Stage) bodyPane.getScene().getWindow();
        stage.close();
    }

    private void updateValidateBtn() {
        if (organeMode || btnEnregistrer == null) return;
        btnEnregistrer.setDisable(
                tfNom.getText().trim().isEmpty() ||
                cbClassification.getValue() == null ||
                cbStade.getValue() == null ||
                tfOrgane.getText().trim().isEmpty()
        );
    }

    private void resetForm() {
        autoFilling = true;
        tfNom.clear(); cbClassification.setValue(null); cbStade.setValue(null);
        tfOrgane.clear(); taDescription.clear(); taSymptomes.clear();
        cbClassification.setStyle(""); tfOrgane.setStyle("");
        taDescription.setStyle(""); taSymptomes.setStyle("");
        if (lblSelectedZone != null) lblSelectedZone.setText("");
        autoFilling = false;
        updateValidateBtn();
        highlightedZone = null;
        zoneShapes.forEach((id, shapes) -> shapes.forEach(s -> {
            s.setOpacity(0.75); s.setEffect(null);
        }));
    }

    private void highlightZone(String zoneId) {
        highlightedZone = zoneId;
        DropShadow glow = new DropShadow(20, Color.web("#2BBCB0", 0.9));
        glow.setSpread(0.4);
        zoneShapes.forEach((id, shapes) -> {
            if (id.equals(zoneId)) {
                shapes.forEach(s -> { s.setOpacity(1.0); s.setEffect(glow); s.toFront(); });
            } else {
                shapes.forEach(s -> { s.setOpacity(0.45); s.setEffect(null); });
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  DESSIN DU CORPS HUMAIN 3D
    // ═══════════════════════════════════════════════════════════════
    private void drawHumanBody3D() {
        bodyPane.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #E8F4F8, #D0E8F0);");
        double cx = 160;
        zoneColors.put("tete",      new String[]{"#E8B89D", "#C49473", "#FDBCA0"});
        zoneColors.put("gorge",     new String[]{"#E0AE92", "#BC8A6A", "#F4B094"});
        zoneColors.put("cerveau",   new String[]{"#C8DFF0", "#90B8D8", "#A0CCE8"});
        zoneColors.put("thyroide",  new String[]{"#A0E8D0", "#68C8B0", "#88E0C8"});
        zoneColors.put("poumon",    new String[]{"#B0E8D8", "#78D0C0", "#98E0D0"});
        zoneColors.put("sein",      new String[]{"#F0C8D8", "#D890A8", "#E8B8C8"});
        zoneColors.put("coeur",     new String[]{"#F0A8A8", "#D86060", "#E89090"});
        zoneColors.put("estomac",   new String[]{"#FAD085", "#E0B050", "#F8E0A0"});
        zoneColors.put("foie",      new String[]{"#F4B060", "#D88830", "#F8C878"});
        zoneColors.put("pancreas",  new String[]{"#FAD8A0", "#E0B870", "#F8E8B0"});
        zoneColors.put("rein",      new String[]{"#C8B0E8", "#A088D0", "#D0C0F0"});
        zoneColors.put("colon",     new String[]{"#C8E8A0", "#98C860", "#D8F0B0"});
        zoneColors.put("pelvis",    new String[]{"#D8D0F0", "#B8A8E0", "#E8E0F8"});
        zoneColors.put("os",        new String[]{"#E8E0D0", "#D0C8B8", "#F0E8E0"});
        zoneColors.put("muscles",   new String[]{"#F0D0B8", "#D8A080", "#F8E0C8"});
        zoneColors.put("peau",      new String[]{"#F5CCB8", "#E0A088", "#FDE0D0"});
        zoneColors.put("ganglions", new String[]{"#E0C8F0", "#B898D8", "#F0E0F8"});
        zoneColors.put("moelle",    new String[]{"#F8C8C8", "#E89090", "#FCE0E0"});

        drawSilhouette3D(cx);
        drawHead3D(cx); drawNeck3D(cx); drawBrain3D(cx); drawThyroid3D(cx);
        drawLungs3D(cx); drawBreasts3D(cx); drawHeart3D(cx); drawStomach3D(cx);
        drawLiver3D(cx); drawPancreas3D(cx); drawKidneys3D(cx); drawColon3D(cx);
        drawPelvis3D(cx); drawLymphNodes3D(cx); drawSpine3D(cx);
        drawBones3D(cx); drawArms3D(cx); drawSkin3D(cx);
    }

    private void drawSilhouette3D(double cx) {
        Path shadow = new Path();
        shadow.getElements().addAll(new MoveTo(cx-48,115),new CubicCurveTo(cx-70,125,cx-78,145,cx-75,170),new CubicCurveTo(cx-72,205,cx-65,240,cx-62,275),new CubicCurveTo(cx-60,305,cx-58,330,cx-60,365),new LineTo(cx+60,365),new CubicCurveTo(cx+58,330,cx+60,305,cx+62,275),new CubicCurveTo(cx+65,240,cx+72,205,cx+75,170),new CubicCurveTo(cx+78,145,cx+70,125,cx+48,115),new ClosePath());
        shadow.setFill(Color.web("#000000",0.15)); shadow.setTranslateX(4); shadow.setTranslateY(6); shadow.setMouseTransparent(true); bodyPane.getChildren().add(shadow);
        Path body = new Path();
        body.getElements().addAll(new MoveTo(cx-45,113),new CubicCurveTo(cx-65,118,cx-72,135,cx-68,160),new CubicCurveTo(cx-64,195,cx-58,230,cx-55,265),new CubicCurveTo(cx-53,295,cx-51,320,cx-53,355),new LineTo(cx-33,355),new LineTo(cx-31,510),new LineTo(cx+31,510),new LineTo(cx+33,355),new LineTo(cx+53,355),new CubicCurveTo(cx+51,320,cx+53,295,cx+55,265),new CubicCurveTo(cx+58,230,cx+64,195,cx+68,160),new CubicCurveTo(cx+72,135,cx+65,118,cx+45,113),new ClosePath());
        LinearGradient g = new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#F5E6D8")),new Stop(0.5,Color.web("#FDEFE5")),new Stop(1,Color.web("#F0DCC8")));
        body.setFill(g); body.setStroke(Color.web("#D4A882",0.7)); body.setStrokeWidth(1.5); body.setEffect(new InnerShadow(5,Color.web("#C4A482",0.3))); body.setMouseTransparent(true); bodyPane.getChildren().add(body);
    }

    private void drawHead3D(double cx) {
        Ellipse hs = new Ellipse(cx+3,56,37,46); hs.setFill(Color.web("#000000",0.12)); hs.setMouseTransparent(true); bodyPane.getChildren().add(hs);
        Ellipse head = new Ellipse(cx,52,34,42);
        head.setFill(new RadialGradient(0,0,0.4,0.4,0.6,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#FDBCA0")),new Stop(0.5,Color.web("#E8B89D")),new Stop(1,Color.web("#C49473"))));
        head.setStroke(Color.web("#B88464",0.8)); head.setStrokeWidth(1.2);
        addZone3D("tete",List.of(head),"Tête",cx-12,18);
        drawFaceFeatures(cx);
    }

    private void drawFaceFeatures(double cx) {
        Ellipse le=new Ellipse(cx-12,42,4,3); le.setFill(Color.web("#4A3528"));
        Ellipse re=new Ellipse(cx+12,42,4,3); re.setFill(Color.web("#4A3528"));
        Circle lr=new Circle(cx-13,41,1,Color.web("#FFFFFF",0.6));
        Circle rr=new Circle(cx+11,41,1,Color.web("#FFFFFF",0.6));
        Path nose=new Path(); nose.getElements().addAll(new MoveTo(cx,45),new CubicCurveTo(cx-2,50,cx-1,54,cx,56),new CubicCurveTo(cx+1,54,cx+2,50,cx,45)); nose.setFill(Color.web("#C49473",0.5));
        Path mouth=new Path(); mouth.getElements().addAll(new MoveTo(cx-8,62),new CubicCurveTo(cx-5,64,cx+5,64,cx+8,62)); mouth.setStroke(Color.web("#B07050",0.7)); mouth.setStrokeWidth(1.5); mouth.setFill(null);
        for (javafx.scene.Node n : new javafx.scene.Node[]{le,re,lr,rr,nose,mouth}) { n.setMouseTransparent(true); bodyPane.getChildren().add(n); }
    }

    private void drawNeck3D(double cx) {
        Rectangle neck=new Rectangle(cx-13,91,26,22); neck.setArcWidth(8); neck.setArcHeight(8);
        neck.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#E0AE92")),new Stop(0.5,Color.web("#F4B094")),new Stop(1,Color.web("#BC8A6A"))));
        neck.setStroke(Color.web("#B88464",0.8)); neck.setStrokeWidth(1);
        addZone3D("gorge",List.of(neck),"Gorge",cx+14,105);
    }

    private void drawBrain3D(double cx) {
        Ellipse brain=new Ellipse(cx,48,26,30);
        brain.setFill(new RadialGradient(0,0,0.4,0.4,0.6,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#A0CCE8")),new Stop(0.6,Color.web("#C8DFF0")),new Stop(1,Color.web("#90B8D8"))));
        brain.setStroke(Color.web("#7898B8",0.8)); brain.setStrokeWidth(1);
        addZone3D("cerveau",List.of(brain),"Cerveau",cx-10,22);
    }

    private void drawThyroid3D(double cx) {
        Ellipse th=new Ellipse(cx,108,18,10);
        th.setFill(new RadialGradient(0,0,0.5,0.5,0.7,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#88E0C8")),new Stop(0.5,Color.web("#A0E8D0")),new Stop(1,Color.web("#68C8B0"))));
        th.setStroke(Color.web("#58A898",0.8));
        addZone3D("thyroide",List.of(th),"Thyroïde",cx-8,106);
    }

    private void drawLungs3D(double cx) {
        Path l=new Path(); l.getElements().addAll(new MoveTo(cx-28,135),new CubicCurveTo(cx-48,138,cx-52,165,cx-45,185),new CubicCurveTo(cx-38,200,cx-22,198,cx-18,185),new CubicCurveTo(cx-14,170,cx-12,142,cx-28,135));
        Path r=new Path(); r.getElements().addAll(new MoveTo(cx+28,135),new CubicCurveTo(cx+48,138,cx+52,165,cx+45,185),new CubicCurveTo(cx+38,200,cx+22,198,cx+18,185),new CubicCurveTo(cx+14,170,cx+12,142,cx+28,135));
        RadialGradient g=new RadialGradient(0,0,0.4,0.4,0.6,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#98E0D0")),new Stop(0.5,Color.web("#B0E8D8")),new Stop(1,Color.web("#78D0C0")));
        l.setFill(g); r.setFill(g); l.setStroke(Color.web("#68B8A8",0.8)); r.setStroke(Color.web("#68B8A8",0.8));
        addZone3D("poumon",List.of(l,r),"Poumons",cx+30,140);
    }

    private void drawBreasts3D(double cx) {
        Ellipse lb=new Ellipse(cx-22,178,18,15); Ellipse rb=new Ellipse(cx+22,178,18,15);
        RadialGradient g=new RadialGradient(0,0,0.4,0.4,0.7,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#E8B8C8")),new Stop(0.5,Color.web("#F0C8D8")),new Stop(1,Color.web("#D890A8")));
        lb.setFill(g); rb.setFill(g); lb.setStroke(Color.web("#C88098",0.8)); rb.setStroke(Color.web("#C88098",0.8));
        addZone3D("sein",List.of(lb,rb),"Sein",cx+28,170);
    }

    private void drawHeart3D(double cx) {
        Path h=new Path(); h.getElements().addAll(new MoveTo(cx-8,155),new CubicCurveTo(cx-22,148,cx-26,162,cx-14,170),new CubicCurveTo(cx-6,176,cx,168,cx,164),new CubicCurveTo(cx,168,cx+6,176,cx+14,170),new CubicCurveTo(cx+26,162,cx+22,148,cx+8,155),new ClosePath());
        h.setFill(new RadialGradient(0,0,0.4,0.4,0.6,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#E89090")),new Stop(0.5,Color.web("#F0A8A8")),new Stop(1,Color.web("#D86060"))));
        h.setStroke(Color.web("#C85050",0.9)); h.setStrokeWidth(1.2);
        Ellipse hr=new Ellipse(cx-4,154,6,4); hr.setFill(Color.web("#FFFFFF",0.3)); hr.setMouseTransparent(true); bodyPane.getChildren().add(hr);
        addZone3D("coeur",List.of(h),"Cœur",cx-32,148);
    }

    private void drawStomach3D(double cx) {
        Ellipse s=new Ellipse(cx-10,218,20,16);
        s.setFill(new RadialGradient(0,0,0.4,0.4,0.7,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#F8E0A0")),new Stop(0.5,Color.web("#FAD085")),new Stop(1,Color.web("#E0B050"))));
        s.setStroke(Color.web("#D0A040",0.8));
        addZone3D("estomac",List.of(s),"Estomac",cx-42,215);
    }

    private void drawLiver3D(double cx) {
        Path lv=new Path(); lv.getElements().addAll(new MoveTo(cx+8,204),new CubicCurveTo(cx+30,200,cx+38,212,cx+32,224),new CubicCurveTo(cx+24,234,cx+6,232,cx+2,224),new CubicCurveTo(cx-2,216,cx+2,206,cx+8,204));
        lv.setFill(new RadialGradient(0,0,0.4,0.4,0.6,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#F8C878")),new Stop(0.5,Color.web("#F4B060")),new Stop(1,Color.web("#D88830"))));
        lv.setStroke(Color.web("#C87828",0.8));
        addZone3D("foie",List.of(lv),"Foie",cx+24,205);
    }

    private void drawPancreas3D(double cx) {
        Ellipse p=new Ellipse(cx,240,28,9);
        p.setFill(new RadialGradient(0,0,0.5,0.5,0.7,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#F8E8B0")),new Stop(0.5,Color.web("#FAD8A0")),new Stop(1,Color.web("#E0B870"))));
        p.setStroke(Color.web("#D0A860",0.8));
        addZone3D("pancreas",List.of(p),"Pancréas",cx-10,236);
    }

    private void drawKidneys3D(double cx) {
        Path lk=new Path(); lk.getElements().addAll(new MoveTo(cx-40,250),new CubicCurveTo(cx-52,248,cx-54,264,cx-42,266),new CubicCurveTo(cx-34,268,cx-28,264,cx-32,254),new CubicCurveTo(cx-34,250,cx-36,250,cx-40,250));
        Path rk=new Path(); rk.getElements().addAll(new MoveTo(cx+40,250),new CubicCurveTo(cx+52,248,cx+54,264,cx+42,266),new CubicCurveTo(cx+34,268,cx+28,264,cx+32,254),new CubicCurveTo(cx+34,250,cx+36,250,cx+40,250));
        RadialGradient g=new RadialGradient(0,0,0.5,0.5,0.6,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#D0C0F0")),new Stop(0.5,Color.web("#C8B0E8")),new Stop(1,Color.web("#A088D0")));
        lk.setFill(g); rk.setFill(g); lk.setStroke(Color.web("#9078C0",0.8)); rk.setStroke(Color.web("#9078C0",0.8));
        addZone3D("rein",List.of(lk,rk),"Reins",cx+42,248);
    }

    private void drawColon3D(double cx) {
        Path c=new Path(); c.getElements().addAll(new MoveTo(cx-38,272),new LineTo(cx+38,272),new CubicCurveTo(cx+54,274,cx+54,308,cx+38,310),new LineTo(cx-38,310),new CubicCurveTo(cx-54,308,cx-54,274,cx-38,272));
        c.setFill(new RadialGradient(0,0,0.5,0.5,0.7,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#D8F0B0")),new Stop(0.5,Color.web("#C8E8A0")),new Stop(1,Color.web("#98C860"))));
        c.setStroke(Color.web("#88B850",0.8));
        addZone3D("colon",List.of(c),"Côlon",cx-50,285);
    }

    private void drawPelvis3D(double cx) {
        Ellipse p=new Ellipse(cx,330,38,22);
        p.setFill(new RadialGradient(0,0,0.5,0.5,0.7,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#E8E0F8")),new Stop(0.5,Color.web("#D8D0F0")),new Stop(1,Color.web("#B8A8E0"))));
        p.setStroke(Color.web("#A898D0",0.8));
        addZone3D("pelvis",List.of(p),"Pelvis",cx-14,328);
    }

    private void drawLymphNodes3D(double cx) {
        List<Shape> lymph=new ArrayList<>();
        double[][] pos={{cx-50,128,8},{cx+50,128,8},{cx-48,200,7},{cx+48,200,7},{cx-45,310,7},{cx+45,310,7}};
        for (double[] p : pos) {
            Circle n=new Circle(p[0],p[1],p[2]);
            n.setFill(new RadialGradient(0,0,0.4,0.4,0.6,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#F0E0F8")),new Stop(0.5,Color.web("#E0C8F0")),new Stop(1,Color.web("#B898D8"))));
            n.setStroke(Color.web("#A888C8",0.8)); lymph.add(n);
        }
        addZone3D("ganglions",lymph,"Ganglions",cx+52,122);
    }

    private void drawSpine3D(double cx) {
        Rectangle s=new Rectangle(cx-5,120,10,200); s.setArcWidth(6); s.setArcHeight(6);
        s.setFill(new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#E89090")),new Stop(0.5,Color.web("#F8C8C8")),new Stop(1,Color.web("#E89090"))));
        s.setStroke(Color.web("#D87878",0.8));
        addZone3D("moelle",List.of(s),"Moelle",cx+6,190);
    }

    private void drawBones3D(double cx) {
        Path lf=new Path(); lf.getElements().addAll(new MoveTo(cx-18,358),new CubicCurveTo(cx-28,370,cx-28,385,cx-18,395),new LineTo(cx-10,395),new CubicCurveTo(cx-14,385,cx-14,370,cx-10,358),new ClosePath());
        Path rf=new Path(); rf.getElements().addAll(new MoveTo(cx+18,358),new CubicCurveTo(cx+28,370,cx+28,385,cx+18,395),new LineTo(cx+10,395),new CubicCurveTo(cx+14,385,cx+14,370,cx+10,358),new ClosePath());
        LinearGradient g=new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#D0C8B8")),new Stop(0.5,Color.web("#E8E0D0")),new Stop(1,Color.web("#D0C8B8")));
        lf.setFill(g); rf.setFill(g); lf.setStroke(Color.web("#C0B8A8",0.8)); rf.setStroke(Color.web("#C0B8A8",0.8));
        addZone3D("os",List.of(lf,rf),"Os",cx+38,380);
    }

    private void drawArms3D(double cx) {
        Path la=new Path(); la.getElements().addAll(new MoveTo(cx-58,125),new CubicCurveTo(cx-78,135,cx-82,185,cx-74,265),new LineTo(cx-62,265),new CubicCurveTo(cx-66,185,cx-64,135,cx-58,125));
        Path ra=new Path(); ra.getElements().addAll(new MoveTo(cx+58,125),new CubicCurveTo(cx+78,135,cx+82,185,cx+74,265),new LineTo(cx+62,265),new CubicCurveTo(cx+66,185,cx+64,135,cx+58,125));
        LinearGradient g=new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#D8A080")),new Stop(0.5,Color.web("#F0D0B8")),new Stop(1,Color.web("#D8A080")));
        la.setFill(g); ra.setFill(g); la.setStroke(Color.web("#C89070",0.8)); ra.setStroke(Color.web("#C89070",0.8));
        addZone3D("muscles",List.of(la,ra),"Muscles",cx+68,200);
    }

    private void drawSkin3D(double cx) {
        Rectangle ls=new Rectangle(cx-92,115,16,170); ls.setArcWidth(12); ls.setArcHeight(12);
        Rectangle rs=new Rectangle(cx+76,115,16,170); rs.setArcWidth(12); rs.setArcHeight(12);
        LinearGradient g=new LinearGradient(0,0,1,0,true,CycleMethod.NO_CYCLE,new Stop(0,Color.web("#E0A088")),new Stop(0.5,Color.web("#F5CCB8")),new Stop(1,Color.web("#E0A088")));
        ls.setFill(g); rs.setFill(g); ls.setStroke(Color.web("#D09070",0.8)); rs.setStroke(Color.web("#D09070",0.8));
        addZone3D("peau",List.of(ls,rs),"Peau",cx-90,190);
    }

    private void addZone3D(String zoneId, List<Shape> shapes, String label, double lx, double ly) {
        String[] colors = zoneColors.getOrDefault(zoneId, new String[]{"#CCCCCC","#888888","#EEEEEE"});
        for (Shape s : shapes) {
            s.setCursor(Cursor.HAND);
            s.setOpacity(0.75);
            InnerShadow inner = new InnerShadow(3, Color.web(colors[1], 0.3));
            s.setEffect(inner);
            s.setOnMouseEntered(e -> {
                if (!zoneId.equals(highlightedZone)) {
                    s.setOpacity(1.0);
                    DropShadow h = new DropShadow(12, Color.web(colors[1], 0.7));
                    h.setSpread(0.3); s.setEffect(h);
                }
            });
            s.setOnMouseExited(e -> {
                if (!zoneId.equals(highlightedZone)) {
                    s.setOpacity(0.75); s.setEffect(inner);
                }
            });
            s.setOnMouseClicked(e -> applySelectionByZone(zoneId));
            bodyPane.getChildren().add(s);
        }
        Text txt = new Text(lx, ly, label);
        txt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        txt.setFill(Color.web(colors[1]).darker());
        txt.setMouseTransparent(true);
        txt.setEffect(new DropShadow(2, Color.web("#FFFFFF", 0.5)));
        bodyPane.getChildren().add(txt);
        zoneShapes.put(zoneId, shapes);
    }

    public static class CancerEntry {
        public final String nom, cls, organe, zone, desc, symp;
        public CancerEntry(String n, String c, String o, String z, String d, String s) {
            nom=n; cls=c; organe=o; zone=z; desc=d; symp=s;
        }
    }
}
