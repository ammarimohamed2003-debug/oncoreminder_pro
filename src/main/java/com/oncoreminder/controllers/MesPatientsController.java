package com.oncoreminder.controllers;

import com.oncoreminder.models.RendezVous;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceRendezVous;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MesPatientsController {

    @FXML private DoctorSidebarController doctorSidebarController;
    @FXML private TextField  searchField;
    @FXML private Label      totalPatientsLabel;
    @FXML private Label      totalRdvsLabel;
    @FXML private Label      listCountLabel;
    @FXML private VBox       patientListContainer;
    @FXML private VBox       emptyState;
    @FXML private ScrollPane detailScroll;
    @FXML private VBox       detailContainer;

    private final ServiceRendezVous  serviceRdv  = new ServiceRendezVous();
    private final ServiceUtilisateur serviceUser = new ServiceUtilisateur();

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String[] AVATAR_COLORS = {
        "#5B35A5","#2BBCB0","#FC8181","#F6AD55","#68D391","#63B3ED","#E879F9","#FB7185"
    };

    private Map<Integer, List<RendezVous>> rdvParPatient = new LinkedHashMap<>();
    private Map<Integer, Utilisateur>      patientsMap   = new LinkedHashMap<>();
    private int selectedPatientId = -1;
    private VBox dossierSectionNode;

    @FXML
    public void initialize() {
        if (doctorSidebarController != null)
            doctorSidebarController.setActivePage("patients");
        searchField.textProperty().addListener((obs, old, val) ->
                renderPatientList(val == null ? "" : val.trim().toLowerCase(Locale.ROOT)));
        loadData();
    }

    // ─── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        Utilisateur doctor = UserSession.getInstance().getCurrentUser();
        if (doctor == null) return;

        List<RendezVous> rdvs = serviceRdv.getAcceptesForDoctor(doctor.getId());

        rdvParPatient = rdvs.stream()
                .collect(Collectors.groupingBy(RendezVous::getPatientId, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted((a, b) -> {
                    var la = a.getValue().stream().map(RendezVous::getDateRdv).max(Comparator.naturalOrder()).orElse(null);
                    var lb = b.getValue().stream().map(RendezVous::getDateRdv).max(Comparator.naturalOrder()).orElse(null);
                    if (la == null && lb == null) return 0;
                    if (la == null) return 1;
                    if (lb == null) return -1;
                    return lb.compareTo(la);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        patientsMap = new LinkedHashMap<>();
        for (int pid : rdvParPatient.keySet()) {
            Utilisateur p = serviceUser.getById(pid);
            if (p != null) patientsMap.put(pid, p);
        }

        totalPatientsLabel.setText(patientsMap.size() + " patient(s)");
        totalRdvsLabel.setText(rdvs.size() + " RDV accepté(s)");
        renderPatientList("");
    }

    // ─── Patient list ──────────────────────────────────────────────────────────

    private void renderPatientList(String search) {
        patientListContainer.getChildren().clear();

        List<Utilisateur> visible = patientsMap.values().stream()
                .filter(p -> {
                    if (search.isEmpty()) return true;
                    String full  = (p.getPrenom() + " " + p.getNom()).toLowerCase(Locale.ROOT);
                    String email = p.getEmail() != null ? p.getEmail().toLowerCase(Locale.ROOT) : "";
                    return full.contains(search) || email.contains(search);
                })
                .collect(Collectors.toList());

        listCountLabel.setText(String.valueOf(visible.size()));

        if (visible.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40, 10, 40, 10));
            Label ico = new Label(search.isEmpty() ? "🏥" : "🔍");
            ico.setStyle("-fx-font-size: 28px;");
            Label lbl = new Label(search.isEmpty() ? "Aucun patient" : "Aucun résultat");
            lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
            empty.getChildren().addAll(ico, lbl);
            patientListContainer.getChildren().add(empty);
            return;
        }

        for (Utilisateur p : visible)
            patientListContainer.getChildren().add(buildPatientCard(p));
    }

    private VBox buildPatientCard(Utilisateur p) {
        String name  = p.getPrenom() + " " + p.getNom();
        String color = avatarColor(name);
        int rdvCount = rdvParPatient.getOrDefault(p.getId(), List.of()).size();
        var lastRdv  = rdvParPatient.getOrDefault(p.getId(), List.of()).stream()
                .max(Comparator.comparing(RendezVous::getDateRdv)).orElse(null);
        boolean active = p.getId() == selectedPatientId;

        // Left accent bar
        Rectangle bar = new Rectangle(4, 72);
        bar.setFill(Color.web(color));
        bar.setArcWidth(4);
        bar.setArcHeight(4);

        // Avatar
        Circle bg = new Circle(22, Color.web(color, 0.15));
        Circle fg = new Circle(22, Color.web(color));
        Label  ic = new Label(initiales(p));
        ic.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        StackPane avatar = new StackPane(fg, ic);
        avatar.setMinSize(44, 44);
        avatar.setMaxSize(44, 44);

        // Info
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        String sub = lastRdv != null
                ? "Dernier RDV : " + lastRdv.getDateRdv().format(DATE_SHORT)
                : "Aucun RDV enregistré";
        Label subLabel = new Label(sub);
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        // Chips row
        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);

        Label rdvBadge = chip(rdvCount + " RDV", "#EEF2FF", "#4F46E5");
        chips.getChildren().add(rdvBadge);

        if (p.getGroupeSanguin() != null && !p.getGroupeSanguin().isBlank()) {
            chips.getChildren().add(chip(p.getGroupeSanguin(), "#FFF1F2", "#E11D48"));
        }

        VBox info = new VBox(3, nameLabel, subLabel, chips);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox inner = new HBox(12, avatar, info);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(12, 14, 12, 14));
        HBox.setHgrow(inner, Priority.ALWAYS);

        HBox card = new HBox(0, bar, inner);
        card.setAlignment(Pos.CENTER_LEFT);

        applyCardStyle(card, active, false);

        card.setOnMouseEntered(e -> { if (p.getId() != selectedPatientId) applyCardStyle(card, false, true); });
        card.setOnMouseExited(e  -> { if (p.getId() != selectedPatientId) applyCardStyle(card, false, false); });
        card.setOnMouseClicked(e -> selectPatient(p));

        VBox wrapper = new VBox(card);
        wrapper.setStyle("-fx-padding: 3 6;");
        return wrapper;
    }

    private void applyCardStyle(HBox card, boolean selected, boolean hovered) {
        String bg = selected ? "#F5F3FF"
                : hovered   ? "#F8FAFC"
                             : "white";
        String shadow = (selected || hovered)
                ? "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 8, 0, 0, 2);"
                : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);";
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                "-fx-cursor: hand; " + shadow);
    }

    private void selectPatient(Utilisateur p) {
        selectedPatientId = p.getId();
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        renderPatientList(search);
        showDetail(p);
    }

    // ─── Detail panel ──────────────────────────────────────────────────────────

    private void showDetail(Utilisateur p) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        detailScroll.setVisible(true);
        detailScroll.setManaged(true);

        detailContainer.getChildren().clear();
        detailContainer.setSpacing(0);
        detailContainer.setPadding(new Insets(0));

        detailContainer.getChildren().add(buildDetailHeader(p));

        VBox body = new VBox(22);
        body.setPadding(new Insets(24));

        dossierSectionNode = buildDossierView(p);
        body.getChildren().add(dossierSectionNode);
        body.getChildren().add(buildRdvHistory(p));

        detailContainer.getChildren().add(body);
    }

    // ─── Detail header (banner) ────────────────────────────────────────────────

    private VBox buildDetailHeader(Utilisateur p) {
        String name  = p.getPrenom() + " " + p.getNom();
        String color = avatarColor(name);
        int rdvCount = rdvParPatient.getOrDefault(p.getId(), List.of()).size();
        var lastRdv  = rdvParPatient.getOrDefault(p.getId(), List.of()).stream()
                .max(Comparator.comparing(RendezVous::getDateRdv)).orElse(null);

        // Banner
        VBox banner = new VBox();
        banner.setMinHeight(80);
        banner.setStyle("-fx-background-color: linear-gradient(to right, " + color + ", " + lighten(color) + ");");

        // Delete button top-right inside banner
        Button btnDelete = new Button("🗑  Supprimer");
        btnDelete.setStyle("-fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: white; " +
                "-fx-border-color: rgba(255,255,255,0.40); -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;");
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(
                "-fx-background-color: rgba(220,38,38,0.85); -fx-text-fill: white; " +
                "-fx-border-color: transparent; -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle(
                "-fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: white; " +
                "-fx-border-color: rgba(255,255,255,0.40); -fx-border-width: 1; " +
                "-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;"));
        btnDelete.setOnAction(e -> confirmDeletePatient(p));

        HBox bannerTop = new HBox(btnDelete);
        bannerTop.setAlignment(Pos.TOP_RIGHT);
        bannerTop.setPadding(new Insets(10, 14, 0, 14));
        banner.getChildren().add(bannerTop);
        VBox.setVgrow(banner, Priority.NEVER);

        // Avatar overlapping the banner
        Circle avatarCircle = new Circle(36, Color.web(color));
        Circle avatarBorder = new Circle(38, Color.WHITE);
        Label  avatarInit   = new Label(initiales(p));
        avatarInit.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 24px;");
        StackPane avatar = new StackPane(avatarBorder, avatarCircle, avatarInit);
        avatar.setMinSize(76, 76);
        avatar.setMaxSize(76, 76);

        // Name and info
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label emailLabel = new Label(p.getEmail() != null ? p.getEmail() : "");
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        // Stats chips row
        HBox statsRow = new HBox(8);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setPadding(new Insets(6, 0, 0, 0));
        statsRow.getChildren().add(chip("🗓 " + rdvCount + " rendez-vous", "#EEF2FF", "#4F46E5"));
        if (lastRdv != null)
            statsRow.getChildren().add(chip("📅 " + lastRdv.getDateRdv().format(DATE_SHORT), "#F0FDF4", "#059669"));
        if (p.getGroupeSanguin() != null && !p.getGroupeSanguin().isBlank())
            statsRow.getChildren().add(chip("🩸 " + p.getGroupeSanguin(), "#FFF1F2", "#E11D48"));

        VBox nameBlock = new VBox(4, nameLabel, emailLabel, statsRow);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);

        HBox profileRow = new HBox(16, avatar, nameBlock);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        profileRow.setPadding(new Insets(0, 20, 16, 20));

        // Outer wrapper: banner on top, profile row below
        VBox headerBox = new VBox(0, banner, profileRow);
        headerBox.setStyle("-fx-background-color: white; " +
                "-fx-border-color: transparent transparent #F1F5F9 transparent; " +
                "-fx-border-width: 0 0 1 0;");

        // Shift avatar up to overlap banner
        profileRow.setTranslateY(-20);
        headerBox.setPadding(new Insets(0, 0, -20, 0));

        return headerBox;
    }

    private void confirmDeletePatient(Utilisateur p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le patient");
        alert.setHeaderText("Supprimer " + p.getPrenom() + " " + p.getNom() + " ?");
        alert.setContentText("Cette action est irréversible. Tous les rendez-vous associés seront également supprimés.");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = serviceUser.deletePatient(p.getId());
                if (ok) {
                    patientsMap.remove(p.getId());
                    rdvParPatient.remove(p.getId());
                    selectedPatientId = -1;
                    totalPatientsLabel.setText(patientsMap.size() + " patient(s)");
                    totalRdvsLabel.setText(rdvParPatient.values().stream().mapToInt(List::size).sum() + " RDV accepté(s)");
                    renderPatientList(searchField.getText() == null ? ""
                            : searchField.getText().trim().toLowerCase(Locale.ROOT));
                    detailScroll.setVisible(false);
                    detailScroll.setManaged(false);
                    emptyState.setVisible(true);
                    emptyState.setManaged(true);
                }
            }
        });
    }

    // ─── Dossier view ──────────────────────────────────────────────────────────

    private VBox buildDossierView(Utilisateur p) {
        HBox sectionTitle = sectionHeader("📋  Dossier Médical");

        Button btnEdit = new Button("✏️  Modifier");
        btnEdit.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 6 16; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(
                "-fx-background-color: #4338CA; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 6 16; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-cursor: hand;"));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle(
                "-fx-background-color: #4F46E5; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 6 16; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-cursor: hand;"));
        btnEdit.setOnAction(e -> switchToDossierEdit(p));
        HBox.setHgrow(sectionTitle, Priority.ALWAYS);

        HBox titleRow = new HBox(sectionTitle, btnEdit);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Medical info grid — 2 columns of info cards
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, new ColumnConstraints() {{ setPercentWidth(50); }});

        int col2 = 0, row2 = 0;

        // Blood group — special red card
        if (p.getGroupeSanguin() != null && !p.getGroupeSanguin().isBlank()) {
            grid.add(infoCard("🩸", "Groupe sanguin", p.getGroupeSanguin(), "#FFF1F2", "#E11D48"), col2, row2);
            col2++;
        }
        if (p.getPoids() != null) {
            if (col2 > 1) { col2 = 0; row2++; }
            grid.add(infoCard("⚖️", "Poids", p.getPoids() + " kg", "#EFF6FF", "#2563EB"), col2, row2);
            col2++;
        }
        if (p.getTaille() != null) {
            if (col2 > 1) { col2 = 0; row2++; }
            grid.add(infoCard("📏", "Taille", p.getTaille() + " cm", "#F0FDF4", "#059669"), col2, row2);
            col2++;
        }

        // Full-width text fields
        if (col2 > 0) row2++;
        col2 = 0;

        VBox textCards = new VBox(10);
        if (notBlank(p.getAllergies()))
            textCards.getChildren().add(textCard("⚠️  Allergies", p.getAllergies(), "#FFFBEB", "#D97706"));
        if (notBlank(p.getAntecedents()))
            textCards.getChildren().add(textCard("📜  Antécédents", p.getAntecedents(), "#FFF7FF", "#7C3AED"));
        if (notBlank(p.getTraitements()))
            textCards.getChildren().add(textCard("💊  Traitements en cours", p.getTraitements(), "#F0FDF4", "#059669"));
        if (notBlank(p.getNotes()))
            textCards.getChildren().add(textCard("📝  Notes médicales", p.getNotes(), "#F8FAFC", "#475569"));

        if (textCards.getChildren().isEmpty() && grid.getChildren().isEmpty()) {
            Label empty = new Label("Aucune information médicale renseignée.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-padding: 10 0;");
            textCards.getChildren().add(empty);
        }

        VBox section = new VBox(14, titleRow, grid, textCards);
        return section;
    }

    private VBox infoCard(String icon, String label, String value, String bg, String accent) {
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 18px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + accent + "; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        VBox card = new VBox(2, ico, lbl, val);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                "-fx-border-color: " + accent + "33; -fx-border-radius: 10; -fx-border-width: 1;");
        return card;
    }

    private HBox textCard(String label, String value, String bg, String accent) {
        VBox stripe = new VBox();
        stripe.setMinWidth(4);
        stripe.setMaxWidth(4);
        stripe.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 4 0 0 4;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
        val.setWrapText(true);

        VBox content = new VBox(4, lbl, val);
        content.setPadding(new Insets(10, 14, 10, 12));
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox card = new HBox(0, stripe, content);
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8; " +
                "-fx-border-color: " + accent + "33; -fx-border-radius: 8; -fx-border-width: 1;");
        return card;
    }

    // ─── Dossier edit ──────────────────────────────────────────────────────────

    private void switchToDossierEdit(Utilisateur p) {
        VBox editNode = buildDossierEdit(p);
        replaceInBody(dossierSectionNode, editNode);
        dossierSectionNode = editNode;
    }

    private void switchToDossierView(Utilisateur p) {
        Utilisateur refreshed = serviceUser.getById(p.getId());
        if (refreshed != null) patientsMap.put(p.getId(), refreshed);
        Utilisateur display = refreshed != null ? refreshed : p;
        VBox viewNode = buildDossierView(display);
        replaceInBody(dossierSectionNode, viewNode);
        dossierSectionNode = viewNode;
    }

    private void replaceInBody(VBox oldNode, VBox newNode) {
        if (detailContainer.getChildren().size() < 2) return;
        VBox body = (VBox) detailContainer.getChildren().get(1);
        int idx = body.getChildren().indexOf(oldNode);
        if (idx >= 0) body.getChildren().set(idx, newNode);
    }

    private VBox buildDossierEdit(Utilisateur p) {
        HBox titleRow = sectionHeader("✏️  Modifier le Dossier Médical");

        ComboBox<String> cbGroupe = new ComboBox<>(FXCollections.observableArrayList(
                "", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        cbGroupe.setValue(p.getGroupeSanguin() != null ? p.getGroupeSanguin() : "");
        cbGroupe.setPrefWidth(140);
        cbGroupe.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #C4B5FD; -fx-border-width: 1;");

        TextField tfPoids  = editField(p.getPoids()  != null ? String.valueOf(p.getPoids())  : "", "kg");
        TextField tfTaille = editField(p.getTaille() != null ? String.valueOf(p.getTaille()) : "", "cm");
        TextArea  taAllergies   = editArea(p.getAllergies(),   3);
        TextArea  taAntecedents = editArea(p.getAntecedents(), 3);
        TextArea  taTraitements = editArea(p.getTraitements(), 3);
        TextArea  taNotes       = editArea(p.getNotes(),       3);

        // Top row: groupe + poids + taille
        HBox topRow = new HBox(12,
                editGroup("🩸 Groupe sanguin", cbGroupe),
                editGroup("⚖️ Poids (kg)",     tfPoids),
                editGroup("📏 Taille (cm)",     tfTaille));
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox fields = new VBox(12,
                topRow,
                editGroup("⚠️ Allergies",         taAllergies),
                editGroup("📜 Antécédents",        taAntecedents),
                editGroup("💊 Traitements en cours", taTraitements),
                editGroup("📝 Notes médicales",    taNotes));

        VBox formCard = new VBox(16, fields);
        formCard.setPadding(new Insets(16));
        formCard.setStyle("-fx-background-color: #F5F3FF; -fx-background-radius: 12; " +
                "-fx-border-color: #C4B5FD; -fx-border-radius: 12; -fx-border-width: 1;");

        // Feedback
        Label feedback = new Label();
        feedback.setWrapText(true);

        Button btnSave = new Button("💾  Enregistrer");
        btnSave.setStyle("-fx-background-color: #059669; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 20; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
        Button btnCancel = new Button("✖  Annuler");
        btnCancel.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; " +
                "-fx-background-radius: 8; -fx-padding: 8 20; -fx-font-size: 13px; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            String gs = cbGroupe.getValue();
            p.setGroupeSanguin(gs == null || gs.isBlank() ? null : gs);
            p.setPoids(parseDouble(tfPoids.getText()));
            p.setTaille(parseDouble(tfTaille.getText()));
            p.setAllergies(blank(taAllergies.getText()));
            p.setAntecedents(blank(taAntecedents.getText()));
            p.setTraitements(blank(taTraitements.getText()));
            p.setNotes(blank(taNotes.getText()));

            if (serviceUser.updateDossierComplet(p)) {
                patientsMap.put(p.getId(), p);
                switchToDossierView(p);
            } else {
                feedback.setText("❌ Erreur lors de l'enregistrement.");
                feedback.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
            }
        });
        btnCancel.setOnAction(e -> switchToDossierView(p));

        HBox buttons = new HBox(10, btnSave, btnCancel, feedback);
        buttons.setAlignment(Pos.CENTER_LEFT);

        return new VBox(14, titleRow, formCard, buttons);
    }

    private VBox editGroup(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        VBox group = new VBox(4, lbl, field);
        HBox.setHgrow(group, Priority.ALWAYS);
        VBox.setVgrow(field, Priority.NEVER);
        return group;
    }

    private TextField editField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setPrefWidth(120);
        tf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #C4B5FD; -fx-border-width: 1; -fx-padding: 6 10; -fx-font-size: 12px;");
        return tf;
    }

    private TextArea editArea(String value, int rows) {
        TextArea ta = new TextArea(value != null ? value : "");
        ta.setPrefRowCount(rows);
        ta.setWrapText(true);
        ta.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #C4B5FD; -fx-border-width: 1; -fx-font-size: 12px;");
        return ta;
    }

    // ─── RDV Timeline ──────────────────────────────────────────────────────────

    private VBox buildRdvHistory(Utilisateur p) {
        List<RendezVous> rdvs = new ArrayList<>(rdvParPatient.getOrDefault(p.getId(), List.of()));
        rdvs.sort(Comparator.comparing(RendezVous::getDateRdv).reversed());

        HBox titleRow = sectionHeader("🗓  Historique des rendez-vous (" + rdvs.size() + ")");

        VBox section = new VBox(12, titleRow);

        if (rdvs.isEmpty()) {
            HBox empty = new HBox();
            empty.setAlignment(Pos.CENTER_LEFT);
            empty.setPadding(new Insets(12, 0, 0, 0));
            Label lbl = new Label("Aucun rendez-vous accepté pour ce patient.");
            lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            empty.getChildren().add(lbl);
            section.getChildren().add(empty);
            return section;
        }

        for (int i = 0; i < rdvs.size(); i++)
            section.getChildren().add(buildTimelineRow(rdvs.get(i), i == rdvs.size() - 1));

        return section;
    }

    private HBox buildTimelineRow(RendezVous rv, boolean last) {
        // Timeline dot + line
        Circle dot = new Circle(7, Color.web("#2BBCB0"));
        Circle dotBorder = new Circle(10, Color.web("#2BBCB0", 0.20));
        StackPane dotStack = new StackPane(dotBorder, dot);
        dotStack.setMinSize(20, 20);
        dotStack.setMaxSize(20, 20);

        Rectangle line = new Rectangle(2, last ? 0 : 30);
        line.setFill(Color.web("#E2E8F0"));
        VBox timeline = new VBox(0, dotStack, line);
        timeline.setAlignment(Pos.TOP_CENTER);
        timeline.setMinWidth(24);

        // Date badge
        String dateStr = rv.getDateRdv() != null ? rv.getDateRdv().format(DATE_SHORT) : "—";
        String timeStr = rv.getDateRdv() != null ? rv.getDateRdv().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        Label timeLbl = new Label("🕐 " + timeStr);
        timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        String lieuTxt = rv.getLieu() != null && !rv.getLieu().isBlank() ? rv.getLieu() : "Lieu non précisé";
        Label lieuLbl = new Label("📍 " + lieuTxt);
        lieuLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        VBox cardContent = new VBox(3, dateLbl, timeLbl, lieuLbl);
        if (rv.getNotes() != null && !rv.getNotes().isBlank()) {
            Label notes = new Label("💬 " + rv.getNotes());
            notes.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
            notes.setWrapText(true);
            cardContent.getChildren().add(notes);
        }
        cardContent.setPadding(new Insets(10, 14, 10, 14));
        HBox.setHgrow(cardContent, Priority.ALWAYS);

        Label badge = chip("✅ Accepté", "#F0FDF4", "#059669");

        VBox badgeBox = new VBox(badge);
        badgeBox.setAlignment(Pos.CENTER);
        badgeBox.setPadding(new Insets(0, 12, 0, 0));

        HBox card = new HBox(0, cardContent, badgeBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox row = new HBox(12, timeline, card);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    // ─── Shared helpers ────────────────────────────────────────────────────────

    private HBox sectionHeader(String text) {
        Rectangle bar = new Rectangle(4, 18);
        bar.setFill(Color.web("#2BBCB0"));
        bar.setArcWidth(4);
        bar.setArcHeight(4);
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        HBox hb = new HBox(8, bar, lbl);
        hb.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hb, Priority.ALWAYS);
        return hb;
    }

    private Label chip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }

    private String lighten(String hex) {
        try {
            Color c = Color.web(hex);
            double f = 0.35;
            int r = (int) Math.min(255, c.getRed()   * 255 * (1 - f) + 255 * f);
            int g = (int) Math.min(255, c.getGreen() * 255 * (1 - f) + 255 * f);
            int b = (int) Math.min(255, c.getBlue()  * 255 * (1 - f) + 255 * f);
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (Exception e) { return hex; }
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private String  blank(String s)    { return (s == null || s.isBlank()) ? null : s.trim(); }

    private String initiales(Utilisateur p) {
        String s = "";
        if (p.getPrenom() != null && !p.getPrenom().isEmpty()) s += p.getPrenom().charAt(0);
        if (p.getNom()    != null && !p.getNom().isEmpty())    s += p.getNom().charAt(0);
        return s.toUpperCase();
    }

    private String avatarColor(String name) {
        return AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
    }

    private Double parseDouble(String text) {
        if (text == null || text.isBlank()) return null;
        try { return Double.parseDouble(text.trim().replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }
}
