package com.oncoreminder.controllers;

import com.oncoreminder.models.Event;
import com.oncoreminder.models.Reclamation;
import com.oncoreminder.models.Reponse;
import com.oncoreminder.services.ServiceEvent;
import com.oncoreminder.services.ServiceReclamation;
import com.oncoreminder.services.ServiceReponse;
import com.oncoreminder.utils.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReclamationPatientController {

    // ── Liste ─────────────────────────────────────────────────────────
    @FXML private Label countLabel;
    @FXML private VBox  reclamationListContainer;

    // ── Filtres patient ───────────────────────────────────────────────
    @FXML private Button btnFiltreToutes;
    @FXML private Button btnFiltreEnCours;
    @FXML private Button btnFiltreTraitee;
    @FXML private Button btnFiltreFermee;

    // ── Panneau formulaire ─────────────────────────────────────────────
    @FXML private ScrollPane     formPane;
    @FXML private ComboBox<Event> eventCombo;
    @FXML private TextField       sujetField;
    @FXML private TextArea        messageArea;
    @FXML private Label           errorSujet;
    @FXML private Label           errorMessage;
    @FXML private Label           feedbackLabel;

    // ── Panneau détail ─────────────────────────────────────────────────
    @FXML private ScrollPane detailPane;
    @FXML private VBox       emptyPane;
    @FXML private Label      detailSujetLabel;
    @FXML private Label      detailStatutLabel;
    @FXML private Label      detailDateLabel;
    @FXML private Label      detailMessageLabel;
    @FXML private VBox       reponsesContainer;
    @FXML private Button     deleteBtn;

    // ── Évaluation ────────────────────────────────────────────────────
    @FXML private VBox  noteSection;
    @FXML private Label noteTitre;
    @FXML private HBox  starsContainer;
    @FXML private Label noteMessage;

    // ── Services ──────────────────────────────────────────────────────
    private final ServiceReclamation serviceReclamation = new ServiceReclamation();
    private final ServiceReponse     serviceReponse     = new ServiceReponse();
    private final ServiceEvent       serviceEvent       = new ServiceEvent();

    private List<Reclamation> allReclamations;
    private Reclamation       selectedReclamation;
    private String            currentFilter = null;

    private Runnable onFermer;
    public void setOnFermer(Runnable r) { this.onFermer = r; }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Init ──────────────────────────────────────────────────────────

    @FXML
    void initialize() {
        List<Event> events = serviceEvent.getAll();
        eventCombo.setItems(FXCollections.observableArrayList(events));
        eventCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Event e)   { return e == null ? "" : e.getTitre(); }
            @Override public Event fromString(String s) { return null; }
        });
        eventCombo.setOnAction(ev -> {
            Event sel = eventCombo.getValue();
            if (sel != null && sujetField.getText().isBlank())
                sujetField.setText("Réclamation — " + sel.getTitre());
        });

        chargerReclamations();
        showRight(formPane);
    }

    public void preselectEvent(String eventTitre) {
        for (Event e : eventCombo.getItems()) {
            if (e.getTitre().equals(eventTitre)) {
                eventCombo.setValue(e);
                sujetField.setText("Réclamation — " + eventTitre);
                break;
            }
        }
        showRight(formPane);
    }

    // ── Chargement liste ──────────────────────────────────────────────

    private void chargerReclamations() {
        int userId = UserSession.getInstance().getCurrentUser().getId();
        allReclamations = serviceReclamation.getByUtilisateur(userId);
        applyFilter();
    }

    private void applyFilter() {
        List<Reclamation> filtered = currentFilter == null ? allReclamations
            : allReclamations.stream()
                .filter(r -> r.getStatut().equals(currentFilter))
                .collect(Collectors.toList());

        countLabel.setText("(" + filtered.size() + ")");
        reclamationListContainer.getChildren().clear();

        if (allReclamations.isEmpty()) {
            showRight(emptyPane);
        } else if (filtered.isEmpty()) {
            Label noResult = new Label("Aucune réclamation " + labelForFilter() + ".");
            noResult.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px; -fx-font-style: italic;");
            reclamationListContainer.getChildren().add(noResult);
            showRight(formPane);
        } else {
            for (Reclamation r : filtered)
                reclamationListContainer.getChildren().add(buildReclamCard(r));
        }

        updateFilterButtons();
    }

    private String labelForFilter() {
        return switch (currentFilter == null ? "" : currentFilter) {
            case "EN_COURS" -> "en cours";
            case "TRAITEE"  -> "traitée";
            case "FERMEE"   -> "fermée";
            default         -> "";
        };
    }

    private void updateFilterButtons() {
        String activeStyle   = "-fx-background-color: #5B35A5; -fx-text-fill: white; " +
                               "-fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String toutesStyle   = currentFilter == null
            ? activeStyle
            : "-fx-background-color: #EDE8FF; -fx-text-fill: #5B35A5; " +
              "-fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String enCoursStyle  = "EN_COURS".equals(currentFilter)
            ? activeStyle
            : "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; " +
              "-fx-padding: 4 8; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String traiteeStyle  = "TRAITEE".equals(currentFilter)
            ? activeStyle
            : "-fx-background-color: #C6F6D5; -fx-text-fill: #276749; " +
              "-fx-padding: 4 8; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";
        String fermeeStyle   = "FERMEE".equals(currentFilter)
            ? activeStyle
            : "-fx-background-color: #E2E8F0; -fx-text-fill: #4A5568; " +
              "-fx-padding: 4 8; -fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand;";

        btnFiltreToutes.setStyle(toutesStyle);
        btnFiltreEnCours.setStyle(enCoursStyle);
        btnFiltreTraitee.setStyle(traiteeStyle);
        btnFiltreFermee.setStyle(fermeeStyle);
    }

    private VBox buildReclamCard(Reclamation r) {
        VBox card = new VBox(6);
        card.setStyle(baseCardStyle());
        card.setPadding(new Insets(12, 14, 12, 14));

        Label sujet = new Label(r.getSujet());
        sujet.setWrapText(true);
        sujet.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2D1B69;");

        int nbRep = serviceReponse.countByReclamation(r.getId());
        Label repBadge = new Label(nbRep > 0 ? "💬 " + nbRep + " réponse(s)" : "En attente");
        repBadge.setStyle(nbRep > 0
            ? "-fx-text-fill: #276749; -fx-font-size: 10px;"
            : "-fx-text-fill: #D97706; -fx-font-size: 10px;");

        HBox meta = new HBox(8);
        meta.getChildren().addAll(buildStatutBadge(r.getStatut()),
            labelDate(r.getDateReclamation() != null ? r.getDateReclamation().format(FMT) : ""),
            repBadge);

        card.getChildren().addAll(sujet, meta);
        card.setOnMouseClicked(e -> showDetail(r));
        card.setOnMouseEntered(e -> card.setStyle(hoverCardStyle()));
        card.setOnMouseExited(e  -> card.setStyle(baseCardStyle()));
        return card;
    }

    // ── Affichage détail ──────────────────────────────────────────────

    private void showDetail(Reclamation r) {
        selectedReclamation = r;
        detailSujetLabel.setText(r.getSujet());
        detailDateLabel.setText(r.getDateReclamation() != null ? r.getDateReclamation().format(FMT) : "");
        detailMessageLabel.setText(r.getMessage());

        Label badge = buildStatutBadge(r.getStatut());
        detailStatutLabel.setText(badge.getText());
        detailStatutLabel.setStyle(badge.getStyle());

        // Bouton supprimer visible uniquement si aucune réponse et non fermée
        boolean peutSupprimer = !serviceReponse.hasReponse(r.getId()) && !"FERMEE".equals(r.getStatut());
        deleteBtn.setVisible(peutSupprimer);
        deleteBtn.setManaged(peutSupprimer);

        // Évaluation
        afficherEvaluation(r);

        reponsesContainer.getChildren().clear();
        List<Reponse> reponses = serviceReponse.getByReclamation(r.getId());
        if (reponses.isEmpty()) {
            Label noRep = new Label("Aucune réponse pour le moment...");
            noRep.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 12px; -fx-font-style: italic;");
            reponsesContainer.getChildren().add(noRep);
        } else {
            for (Reponse rep : reponses)
                reponsesContainer.getChildren().add(buildReponseCard(rep));
        }
        showRight(detailPane);
    }

    private void afficherEvaluation(Reclamation r) {
        boolean traitee = "TRAITEE".equals(r.getStatut()) || "FERMEE".equals(r.getStatut());
        boolean aReponse = serviceReponse.hasReponse(r.getId());
        noteSection.setVisible(traitee && aReponse);
        noteSection.setManaged(traitee && aReponse);
        if (!traitee || !aReponse) return;

        starsContainer.getChildren().clear();
        noteMessage.setVisible(false); noteMessage.setManaged(false);

        if (r.getNote() != null) {
            // Affichage lecture seule
            noteTitre.setText("⭐  Votre évaluation");
            for (int i = 1; i <= 5; i++) {
                Label star = new Label(i <= r.getNote() ? "★" : "☆");
                star.setStyle("-fx-font-size: 22px; -fx-text-fill: " + (i <= r.getNote() ? "#F6AD55;" : "#CBD5E0;"));
                starsContainer.getChildren().add(star);
            }
            noteMessage.setText(noteLabel(r.getNote()));
            noteMessage.setVisible(true); noteMessage.setManaged(true);
        } else {
            // Étoiles cliquables
            noteTitre.setText("⭐  Évaluez la prise en charge");
            Button[] stars = new Button[5];
            for (int i = 1; i <= 5; i++) {
                final int val = i;
                Button star = new Button("☆");
                star.setStyle("-fx-background-color: transparent; -fx-font-size: 24px; " +
                              "-fx-text-fill: #CBD5E0; -fx-cursor: hand; -fx-padding: 0 2;");
                star.setOnMouseEntered(ev -> highlightStars(stars, val));
                star.setOnMouseExited(ev  -> resetStars(stars));
                star.setOnAction(ev -> soumettrNote(r, val, stars));
                stars[i - 1] = star;
                starsContainer.getChildren().add(star);
            }
        }
    }

    private void highlightStars(Button[] stars, int upTo) {
        for (int i = 0; i < 5; i++)
            stars[i].setStyle("-fx-background-color: transparent; -fx-font-size: 24px; " +
                "-fx-text-fill: " + (i < upTo ? "#F6AD55;" : "#CBD5E0;") + " -fx-cursor: hand; -fx-padding: 0 2;");
    }

    private void resetStars(Button[] stars) {
        for (Button s : stars)
            s.setStyle("-fx-background-color: transparent; -fx-font-size: 24px; " +
                       "-fx-text-fill: #CBD5E0; -fx-cursor: hand; -fx-padding: 0 2;");
    }

    private void soumettrNote(Reclamation r, int note, Button[] stars) {
        serviceReclamation.updateNote(r.getId(), note);
        r.setNote(note);
        // Mise à jour dans la liste locale
        allReclamations.stream().filter(x -> x.getId() == r.getId())
            .findFirst().ifPresent(x -> x.setNote(note));
        // Afficher étoiles fixes
        starsContainer.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= note ? "★" : "☆");
            star.setStyle("-fx-font-size: 22px; -fx-text-fill: " + (i <= note ? "#F6AD55;" : "#CBD5E0;"));
            starsContainer.getChildren().add(star);
        }
        noteTitre.setText("⭐  Votre évaluation");
        noteMessage.setText(noteLabel(note));
        noteMessage.setVisible(true); noteMessage.setManaged(true);
    }

    private String noteLabel(int note) {
        return switch (note) {
            case 1 -> "Très insatisfait(e)";
            case 2 -> "Insatisfait(e)";
            case 3 -> "Correct";
            case 4 -> "Satisfait(e)";
            case 5 -> "Très satisfait(e) ✓";
            default -> "";
        };
    }

    private VBox buildReponseCard(Reponse rep) {
        VBox card = new VBox(6);
        card.setStyle(
            "-fx-background-color: #F0FFF4; -fx-background-radius: 10;" +
            "-fx-border-color: #9AE6B4; -fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-padding: 12 14;"
        );
        HBox hdr = new HBox(8);
        Label admin = new Label("🩺 " + (rep.getAdminNomComplet() != null ? rep.getAdminNomComplet() : "Admin"));
        admin.setStyle("-fx-font-weight: bold; -fx-text-fill: #276749; -fx-font-size: 12px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        hdr.getChildren().addAll(admin, sp,
            labelDate(rep.getDateReponse() != null ? rep.getDateReponse().format(FMT) : ""));

        Label msg = new Label(rep.getMessage());
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: #2D3748; -fx-font-size: 13px;");
        card.getChildren().addAll(hdr, msg);
        return card;
    }

    // ── Actions FXML ──────────────────────────────────────────────────

    @FXML void handleNouvelleReclamation(ActionEvent e) { clearForm(); showRight(formPane); }
    @FXML void handleAnnulerForm(ActionEvent e)         { clearForm(); showRight(formPane); }
    @FXML void handleRetourListe(ActionEvent e)         { showRight(formPane); }
    @FXML void handleFermer(ActionEvent e)              { if (onFermer != null) onFermer.run(); }

    @FXML void handleFiltreToutes(ActionEvent e)  { currentFilter = null;       chargerReclamations(); }
    @FXML void handleFiltreEnCours(ActionEvent e) { currentFilter = "EN_COURS"; chargerReclamations(); }
    @FXML void handleFiltreTraitee(ActionEvent e) { currentFilter = "TRAITEE";  chargerReclamations(); }
    @FXML void handleFiltreFermee(ActionEvent e)  { currentFilter = "FERMEE";   chargerReclamations(); }

    @FXML void handleDelete(ActionEvent e) {
        if (selectedReclamation == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer cette réclamation ?");
        confirm.setContentText("« " + selectedReclamation.getSujet() + " »\n\nCette action est irréversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceReclamation.delete(selectedReclamation.getId());
            selectedReclamation = null;
            chargerReclamations();
            showRight(formPane);
        }
    }

    @FXML void handleSoumettre(ActionEvent e) {
        clearErrors();
        String sujet   = sujetField.getText().trim();
        String message = messageArea.getText().trim();
        boolean ok = true;

        if (sujet.isEmpty())   { showErr(sujetField,  errorSujet,   "Le sujet est obligatoire.");      ok = false; }
        if (message.isEmpty()) { showErr(messageArea,  errorMessage, "La description est obligatoire."); ok = false; }
        if (!ok) return;

        var currentUser = UserSession.getInstance().getCurrentUser();
        serviceReclamation.add(new Reclamation(currentUser.getId(), sujet, message));

        clearForm();
        currentFilter = null;
        chargerReclamations();
        showRight(formPane);
        showFeedback(feedbackLabel, "✅ Réclamation soumise avec succès !", true);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void clearForm() {
        eventCombo.setValue(null);
        sujetField.clear();
        messageArea.clear();
        clearErrors();
    }

    private void clearErrors() {
        hide(errorSujet);   clearFieldErr(sujetField);
        hide(errorMessage); clearFieldErr(messageArea);
    }

    private void showRight(Node which) {
        formPane.setVisible(which == formPane);    formPane.setManaged(which == formPane);
        detailPane.setVisible(which == detailPane); detailPane.setManaged(which == detailPane);
        emptyPane.setVisible(which == emptyPane);  emptyPane.setManaged(which == emptyPane);
    }

    private void showErr(Control field, Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px;");
        lbl.setVisible(true); lbl.setManaged(true);
        field.getStyleClass().remove("field-input-error");
        field.getStyleClass().add("field-input-error");
    }

    private void clearFieldErr(Control f) { f.getStyleClass().remove("field-input-error"); }
    private void hide(Label l)            { l.setVisible(false); l.setManaged(false); }

    private Label buildStatutBadge(String statut) {
        Label lbl = new Label(statut);
        String s = switch (statut) {
            case "EN_COURS" -> "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;";
            case "TRAITEE"  -> "-fx-background-color: #C6F6D5; -fx-text-fill: #276749;";
            case "FERMEE"   -> "-fx-background-color: #E2E8F0; -fx-text-fill: #4A5568;";
            default         -> "-fx-background-color: #EEE;    -fx-text-fill: #555;";
        };
        lbl.setStyle(s + "-fx-padding: 2 8; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        return lbl;
    }

    private Label labelDate(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #718096; -fx-font-size: 10px;");
        return l;
    }

    private String baseCardStyle() {
        return "-fx-background-color: white; -fx-background-radius: 10;" +
               "-fx-border-color: #E9E4F7; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private String hoverCardStyle() {
        return "-fx-background-color: #F4F0FF; -fx-background-radius: 10;" +
               "-fx-border-color: #5B35A5; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private void showFeedback(Label lbl, String msg, boolean success) {
        lbl.setText(msg);
        lbl.setStyle(success
            ? "-fx-text-fill: #2BBCB0; -fx-background-color: #f0fffe; -fx-border-color: #2BBCB0;" +
              "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 12;" +
              "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1;"
            : "-fx-text-fill: #E53E3E; -fx-background-color: #fff5f5; -fx-border-color: #FC8181;" +
              "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 12;" +
              "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 1;"
        );
        lbl.setVisible(true); lbl.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), lbl);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        if (success) {
            PauseTransition p = new PauseTransition(Duration.seconds(4));
            p.setOnFinished(ev -> hide(lbl));
            p.play();
        }
    }
}
