package com.oncoreminder.controllers;

import com.oncoreminder.models.Reclamation;
import com.oncoreminder.models.Reponse;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.*;
import com.oncoreminder.utils.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminReclamationController {

    // ── Liste ─────────────────────────────────────────────────────────
    @FXML private Label reclamCountLabel;
    @FXML private VBox  adminReclamListContainer;
    @FXML private TextField searchField;

    // ── Stats ─────────────────────────────────────────────────────────
    @FXML private Label statTotalCount;
    @FXML private Label statEnCoursCount;
    @FXML private Label statTraiteeCount;
    @FXML private Label statFermeeCount;

    // ── Panneau détail ─────────────────────────────────────────────────
    @FXML private VBox       adminEmptyPane;
    @FXML private ScrollPane adminDetailPane;
    @FXML private Label      adminDetailSujet;
    @FXML private Label      adminDetailPatient;
    @FXML private Label      adminDetailDate;
    @FXML private Label      adminDetailStatut;
    @FXML private Label      adminDetailMessage;
    @FXML private VBox       adminReponsesContainer;

    // ── IA & urgence ───────────────────────────────────────────────────
    @FXML private Label urgenceLabel;
    @FXML private Label resumeLabel;
    @FXML private Label iaLoadingLabel;

    // ── Note patient ──────────────────────────────────────────────────
    @FXML private HBox  adminNoteBox;
    @FXML private HBox  adminStarsDisplay;
    @FXML private Label adminNoteText;

    // ── Formulaire réponse ─────────────────────────────────────────────
    @FXML private TextArea         reponseArea;
    @FXML private Label            errorReponse;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Label            adminFeedbackLabel;

    // ── Services ──────────────────────────────────────────────────────
    private final ServiceReclamation   serviceReclamation = new ServiceReclamation();
    private final ServiceReponse       serviceReponse     = new ServiceReponse();
    private final ServiceUtilisateur   serviceUtil        = new ServiceUtilisateur();
    private final ReclamationAIService aiService          = new ReclamationAIService();

    private List<Reclamation> allReclamations;
    private Reclamation       selectedReclamation;
    private String            currentFilter = null;
    private String            currentSearch = "";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Init ──────────────────────────────────────────────────────────

    @FXML
    void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList("EN_COURS", "TRAITEE", "FERMEE"));
        searchField.textProperty().addListener((obs, old, val) -> {
            currentSearch = val == null ? "" : val.trim().toLowerCase();
            applyFilters();
        });
        chargerTout();
        showDetail(false);
    }

    // ── Chargement ────────────────────────────────────────────────────

    private void chargerTout() {
        allReclamations = serviceReclamation.getAll();
        updateStats();
        applyFilters();
    }

    private void applyFilters() {
        List<Reclamation> filtered = allReclamations.stream()
            .filter(r -> currentFilter == null || r.getStatut().equals(currentFilter))
            .filter(r -> {
                if (currentSearch.isEmpty()) return true;
                String sujet   = r.getSujet()   != null ? r.getSujet().toLowerCase()   : "";
                String patient = r.getUtilisateurNomComplet() != null
                    ? r.getUtilisateurNomComplet().toLowerCase() : "";
                return sujet.contains(currentSearch) || patient.contains(currentSearch);
            })
            .collect(Collectors.toList());
        renderList(filtered);
    }

    private void updateStats() {
        long total    = allReclamations.size();
        long enCours  = allReclamations.stream().filter(r -> "EN_COURS".equals(r.getStatut())).count();
        long traitee  = allReclamations.stream().filter(r -> "TRAITEE".equals(r.getStatut())).count();
        long fermee   = allReclamations.stream().filter(r -> "FERMEE".equals(r.getStatut())).count();

        statTotalCount.setText(String.valueOf(total));
        statEnCoursCount.setText(String.valueOf(enCours));
        statTraiteeCount.setText(String.valueOf(traitee));
        statFermeeCount.setText(String.valueOf(fermee));
    }

    private void renderList(List<Reclamation> list) {
        adminReclamListContainer.getChildren().clear();
        reclamCountLabel.setText(list.size() + " réclamation(s)");
        for (Reclamation r : list)
            adminReclamListContainer.getChildren().add(buildAdminCard(r));
    }

    private VBox buildAdminCard(Reclamation r) {
        VBox card = new VBox(6);
        card.setStyle(baseCardStyle());
        card.setPadding(new Insets(11, 13, 11, 13));

        Label sujet = new Label(r.getSujet());
        sujet.setWrapText(true);
        sujet.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2D1B69;");

        Label patient = new Label("👤 " + (r.getUtilisateurNomComplet() != null
            ? r.getUtilisateurNomComplet() : "Patient"));
        patient.setStyle("-fx-text-fill: #5B35A5; -fx-font-size: 11px;");

        int nbRep = serviceReponse.countByReclamation(r.getId());
        Label repCount = new Label(nbRep == 0 ? "Sans réponse" : nbRep + " réponse(s)");
        repCount.setStyle(nbRep == 0
            ? "-fx-text-fill: #E53E3E; -fx-font-size: 10px;"
            : "-fx-text-fill: #276749; -fx-font-size: 10px;");

        HBox meta = new HBox(8);
        Label date = new Label(r.getDateReclamation() != null ? r.getDateReclamation().format(FMT) : "");
        date.setStyle("-fx-text-fill: #718096; -fx-font-size: 10px;");
        meta.getChildren().addAll(buildStatutBadge(r.getStatut()), date, repCount);

        card.getChildren().addAll(sujet, patient, meta);
        card.setOnMouseClicked(e -> selectReclamation(r, card));
        card.setOnMouseEntered(e -> {
            if (selectedReclamation == null || selectedReclamation.getId() != r.getId())
                card.setStyle(hoverCardStyle());
        });
        card.setOnMouseExited(e -> {
            if (selectedReclamation == null || selectedReclamation.getId() != r.getId())
                card.setStyle(baseCardStyle());
        });
        return card;
    }

    private void selectReclamation(Reclamation r, VBox clickedCard) {
        adminReclamListContainer.getChildren().forEach(n ->
            ((VBox) n).setStyle(baseCardStyle()));
        clickedCard.setStyle(selectedCardStyle());
        selectedReclamation = r;
        afficherDetail(r);
        showDetail(true);
        analyseIA(r);
    }

    // ── Affichage détail ──────────────────────────────────────────────

    private void afficherDetail(Reclamation r) {
        adminDetailSujet.setText(r.getSujet());
        adminDetailPatient.setText("👤 " + (r.getUtilisateurNomComplet() != null
            ? r.getUtilisateurNomComplet() : "Patient"));
        adminDetailDate.setText(r.getDateReclamation() != null ? r.getDateReclamation().format(FMT) : "");
        adminDetailMessage.setText(r.getMessage());

        Label badge = buildStatutBadge(r.getStatut());
        adminDetailStatut.setText(badge.getText());
        adminDetailStatut.setStyle(badge.getStyle());

        statutCombo.setValue(r.getStatut());
        reponseArea.clear();
        hide(adminFeedbackLabel);
        hide(errorReponse);
        hide(urgenceLabel);
        hide(resumeLabel);
        hide(iaLoadingLabel);

        // Afficher la note du patient
        afficherNotePatient(r);

        adminReponsesContainer.getChildren().clear();
        List<Reponse> reponses = serviceReponse.getByReclamation(r.getId());
        if (reponses.isEmpty()) {
            Label noRep = new Label("Aucune réponse envoyée pour le moment.");
            noRep.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 12px; -fx-font-style: italic;");
            adminReponsesContainer.getChildren().add(noRep);
        } else {
            for (Reponse rep : reponses)
                adminReponsesContainer.getChildren().add(buildReponseCard(rep));
        }
    }

    private void afficherNotePatient(Reclamation r) {
        if (r.getNote() == null) {
            adminNoteBox.setVisible(false); adminNoteBox.setManaged(false);
            return;
        }
        adminStarsDisplay.getChildren().clear();
        int note = r.getNote();
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= note ? "★" : "☆");
            star.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (i <= note ? "#F6AD55;" : "#CBD5E0;"));
            adminStarsDisplay.getChildren().add(star);
        }
        adminNoteText.setText("(" + note + "/5)");
        adminNoteBox.setVisible(true); adminNoteBox.setManaged(true);
    }

    private VBox buildReponseCard(Reponse rep) {
        VBox card = new VBox(6);
        card.setStyle(
            "-fx-background-color: #EBF8FF; -fx-background-radius: 10;" +
            "-fx-border-color: #90CDF4; -fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-padding: 10 14;"
        );
        HBox hdr = new HBox(8);
        Label admin = new Label("🩺 " + (rep.getAdminNomComplet() != null ? rep.getAdminNomComplet() : "Admin"));
        admin.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B6CB0; -fx-font-size: 12px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label date = new Label(rep.getDateReponse() != null ? rep.getDateReponse().format(FMT) : "");
        date.setStyle("-fx-text-fill: #718096; -fx-font-size: 10px;");
        hdr.getChildren().addAll(admin, sp, date);

        Label msg = new Label(rep.getMessage());
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: #2D3748; -fx-font-size: 13px;");
        card.getChildren().addAll(hdr, msg);
        return card;
    }

    // ── IA ────────────────────────────────────────────────────────────

    private void analyseIA(Reclamation r) {
        iaLoadingLabel.setVisible(true); iaLoadingLabel.setManaged(true);
        hide(urgenceLabel); hide(resumeLabel);

        Thread t = new Thread(() -> {
            try {
                String urgence = aiService.analyzeUrgency(r.getSujet(), r.getMessage());
                String resume  = aiService.summarize(r.getSujet(), r.getMessage());
                Platform.runLater(() -> {
                    hide(iaLoadingLabel);
                    showUrgence(urgence);
                    resumeLabel.setText("📝 " + resume);
                    resumeLabel.setVisible(true); resumeLabel.setManaged(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> hide(iaLoadingLabel));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void showUrgence(String niveau) {
        urgenceLabel.setText(niveau);
        String style = switch (niveau) {
            case "ÉLEVÉ" -> "-fx-background-color: #FED7D7; -fx-text-fill: #C53030;";
            case "MOYEN" -> "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;";
            default      -> "-fx-background-color: #C6F6D5; -fx-text-fill: #276749;";
        };
        urgenceLabel.setStyle(style + "-fx-padding: 2 10; -fx-background-radius: 8;" +
            "-fx-font-size: 11px; -fx-font-weight: bold;");
        urgenceLabel.setVisible(true); urgenceLabel.setManaged(true);
    }

    @FXML void handleSuggestIA(ActionEvent e) {
        if (selectedReclamation == null) return;

        iaLoadingLabel.setText("⏳ Génération en cours...");
        iaLoadingLabel.setVisible(true); iaLoadingLabel.setManaged(true);
        reponseArea.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                String suggestion = aiService.suggestResponse(
                    selectedReclamation.getSujet(), selectedReclamation.getMessage());
                Platform.runLater(() -> {
                    hide(iaLoadingLabel);
                    iaLoadingLabel.setText("⏳ IA en cours...");
                    reponseArea.setDisable(false);
                    reponseArea.setText(suggestion);
                    reponseArea.positionCaret(0);
                    showFeedback(adminFeedbackLabel, "💡 Réponse suggérée par l'IA — modifiez-la avant d'envoyer.", true);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    hide(iaLoadingLabel);
                    iaLoadingLabel.setText("⏳ IA en cours...");
                    reponseArea.setDisable(false);
                    showFeedback(adminFeedbackLabel, "❌ Erreur IA : " + ex.getMessage(), false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Envoi réponse ─────────────────────────────────────────────────

    @FXML void handleEnvoyerReponse(ActionEvent e) {
        hide(errorReponse);
        String msg = reponseArea.getText().trim();
        if (msg.isEmpty()) {
            showErr(reponseArea, errorReponse, "La réponse ne peut pas être vide.");
            return;
        }
        if (selectedReclamation == null) return;

        if (!serviceReclamation.isRepondable(selectedReclamation.getId())) {
            showFeedback(adminFeedbackLabel, "⚠️ Cette réclamation est fermée et ne peut plus recevoir de réponse.", false);
            return;
        }

        Utilisateur admin = UserSession.getInstance().getCurrentUser();
        serviceReponse.add(new Reponse(selectedReclamation.getId(), admin.getId(), msg));

        String newStatut = statutCombo.getValue();
        if (newStatut != null && !newStatut.equals(selectedReclamation.getStatut())) {
            serviceReclamation.updateStatut(selectedReclamation.getId(), newStatut);
            selectedReclamation.setStatut(newStatut);
        } else if ("EN_COURS".equals(selectedReclamation.getStatut())) {
            serviceReclamation.updateStatut(selectedReclamation.getId(), "TRAITEE");
            selectedReclamation.setStatut("TRAITEE");
            statutCombo.setValue("TRAITEE");
        }

        chargerTout();
        afficherDetail(selectedReclamation);
        showFeedback(adminFeedbackLabel, "✅ Réponse envoyée avec succès !", true);
    }

    // ── Suppression ───────────────────────────────────────────────────

    @FXML void handleDelete(ActionEvent e) {
        if (selectedReclamation == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer la réclamation ?");
        confirm.setContentText("« " + selectedReclamation.getSujet() + " »\n\nCette action est irréversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceReponse.deleteByReclamation(selectedReclamation.getId());
            serviceReclamation.delete(selectedReclamation.getId());
            selectedReclamation = null;
            showDetail(false);
            chargerTout();
        }
    }

    // ── Filtres ───────────────────────────────────────────────────────

    @FXML void handleFiltreAll(ActionEvent e)     { currentFilter = null;       chargerTout(); }
    @FXML void handleFiltreEnCours(ActionEvent e) { currentFilter = "EN_COURS"; chargerTout(); }
    @FXML void handleFiltreTraitee(ActionEvent e) { currentFilter = "TRAITEE";  chargerTout(); }
    @FXML void handleFiltreFermee(ActionEvent e)  { currentFilter = "FERMEE";   chargerTout(); }

    // ── Helpers ───────────────────────────────────────────────────────

    private void showDetail(boolean show) {
        adminDetailPane.setVisible(show);  adminDetailPane.setManaged(show);
        adminEmptyPane.setVisible(!show);  adminEmptyPane.setManaged(!show);
    }

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

    private void showErr(Control field, Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #E53E3E; -fx-font-size: 11px;");
        lbl.setVisible(true); lbl.setManaged(true);
        field.getStyleClass().remove("field-input-error");
        field.getStyleClass().add("field-input-error");
    }

    private void hide(Label l) { l.setVisible(false); l.setManaged(false); }

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
            PauseTransition p = new PauseTransition(Duration.seconds(5));
            p.setOnFinished(ev -> hide(lbl));
            p.play();
        }
    }

    private String baseCardStyle() {
        return "-fx-background-color: white; -fx-background-radius: 10;" +
               "-fx-border-color: #E9E4F7; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private String hoverCardStyle() {
        return "-fx-background-color: #F4F0FF; -fx-background-radius: 10;" +
               "-fx-border-color: #5B35A5; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private String selectedCardStyle() {
        return "-fx-background-color: #EDE8FF; -fx-background-radius: 10;" +
               "-fx-border-color: #5B35A5; -fx-border-radius: 10; -fx-border-width: 2; -fx-cursor: hand;";
    }
}
