package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Article;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.utils.ArticleGeneratorService;
import com.oncoreminder.utils.MarkdownRenderer;
import com.oncoreminder.utils.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ArticleFormController {

    @FXML private Label            formTitleLabel;
    @FXML private TextField        titreField;
    @FXML private TextField        organeField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField        tagsField;
    @FXML private TextArea         contenuArea;
    @FXML private VBox             previewContainer;
    @FXML private Label            errorTitre;
    @FXML private Label            errorContenu;
    @FXML private Button           generateBtn;
    @FXML private ProgressIndicator generateSpinner;
    @FXML private Label            generateStatusLabel;

    private final ServiceArticle serviceArticle = new ServiceArticle();
    private static Article articleToEdit;
    private String lastSelectedCancerType = null;

    public static void setLastCancerType(String type) {
        // Called optionally from BodySelectorController if a cancer type was chosen
    }

    public static void setArticleToEdit(Article article) { articleToEdit = article; }

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("BROUILLON", "PUBLIE");
        statutCombo.setValue("BROUILLON");

        contenuArea.textProperty().addListener((obs, o, n) -> {
            MarkdownRenderer.render(n, previewContainer);
            clearErr(contenuArea, errorContenu);
        });
        titreField.textProperty().addListener((obs, o, n) -> clearErr(titreField, errorTitre));
        organeField.textProperty().addListener((obs, o, n) ->
            generateBtn.setDisable(n == null || n.trim().isEmpty()));

        if (articleToEdit != null) {
            formTitleLabel.setText("Modifier l'Article");
            titreField.setText(articleToEdit.getTitre());
            organeField.setText(nvl(articleToEdit.getOrgane()));
            statutCombo.setValue(articleToEdit.getStatut());
            tagsField.setText(nvl(articleToEdit.getTags()));
            contenuArea.setText(nvl(articleToEdit.getContenu()));
            generateBtn.setDisable(articleToEdit.getOrgane() == null || articleToEdit.getOrgane().isEmpty());
        } else {
            formTitleLabel.setText("Nouvel Article");
        }
    }

    // ════════════════════════════════════════════
    // Formatage éditeur
    // ════════════════════════════════════════════

    @FXML void fmtBold(ActionEvent e)      { wrapSelection("**", "**"); }
    @FXML void fmtItalic(ActionEvent e)    { wrapSelection("*", "*"); }
    @FXML void fmtUnderline(ActionEvent e) { wrapSelection("__", "__"); }
    @FXML void fmtStrike(ActionEvent e)    { wrapSelection("~~", "~~"); }
    @FXML void fmtCode(ActionEvent e)      { wrapSelection("`", "`"); }
    @FXML void fmtHighlight(ActionEvent e) { wrapSelectionTag("hl"); }
    @FXML void fmtSup(ActionEvent e)       { wrapSelectionTag("sup"); }
    @FXML void fmtSub(ActionEvent e)       { wrapSelectionTag("sub"); }
    @FXML void fmtH1(ActionEvent e)        { insertLinePrefix("# "); }
    @FXML void fmtH2(ActionEvent e)        { insertLinePrefix("## "); }
    @FXML void fmtList(ActionEvent e)      { insertLinePrefix("- "); }
    @FXML void fmtNumList(ActionEvent e)   { insertLinePrefix("1. "); }
    @FXML void fmtQuote(ActionEvent e)     { insertLinePrefix("> "); }
    @FXML void fmtCenter(ActionEvent e)    { wrapLine("[center]", "[/center]"); }
    @FXML void fmtSmall(ActionEvent e)     { wrapSelectionTag("small"); }
    @FXML void fmtNormal(ActionEvent e)    { wrapSelectionTag("normal"); }
    @FXML void fmtBig(ActionEvent e)       { wrapSelectionTag("big"); }
    @FXML void fmtXl(ActionEvent e)        { wrapSelectionTag("xl"); }
    @FXML void fmtRed(ActionEvent e)       { wrapSelectionTag("red"); }
    @FXML void fmtOrange(ActionEvent e)    { wrapSelectionTag("orange"); }
    @FXML void fmtGreen(ActionEvent e)     { wrapSelectionTag("green"); }
    @FXML void fmtBlue(ActionEvent e)      { wrapSelectionTag("blue"); }
    @FXML void fmtPurple(ActionEvent e)    { wrapSelectionTag("purple"); }
    @FXML void fmtTeal(ActionEvent e)      { wrapSelectionTag("teal"); }
    @FXML void fmtGray(ActionEvent e)      { wrapSelectionTag("gray"); }
    @FXML void fmtBlack(ActionEvent e)     { wrapSelectionTag("black"); }

    @FXML void fmtHr(ActionEvent e) {
        int pos = contenuArea.getCaretPosition();
        String t = contenuArea.getText();
        contenuArea.setText(t.substring(0, pos) + "\n---\n" + t.substring(pos));
        contenuArea.positionCaret(pos + 5);
        contenuArea.requestFocus();
    }

    @FXML void fmtClear(ActionEvent e) {
        IndexRange sel = contenuArea.getSelection();
        if (sel.getLength() == 0) return;
        String clean = contenuArea.getSelectedText()
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[/?[a-z]+]", "");
        contenuArea.replaceSelection(clean);
        contenuArea.requestFocus();
    }

    private void wrapSelectionTag(String tag) { wrapSelection("[" + tag + "]", "[/" + tag + "]"); }

    private void wrapSelection(String before, String after) {
        IndexRange sel = contenuArea.getSelection();
        if (sel.getLength() == 0) {
            int pos = contenuArea.getCaretPosition();
            contenuArea.insertText(pos, before + "texte" + after);
            contenuArea.selectRange(pos + before.length(), pos + before.length() + 5);
        } else {
            contenuArea.replaceSelection(before + contenuArea.getSelectedText() + after);
        }
        contenuArea.requestFocus();
    }

    private void insertLinePrefix(String prefix) {
        int pos = contenuArea.getCaretPosition();
        String t = contenuArea.getText();
        int lineStart = t.lastIndexOf('\n', pos - 1) + 1;
        contenuArea.insertText(lineStart, prefix);
        contenuArea.positionCaret(pos + prefix.length());
        contenuArea.requestFocus();
    }

    private void wrapLine(String before, String after) {
        int pos = contenuArea.getCaretPosition();
        String t = contenuArea.getText();
        int ls = t.lastIndexOf('\n', pos - 1) + 1;
        int le = t.indexOf('\n', pos); if (le == -1) le = t.length();
        String line = t.substring(ls, le);
        contenuArea.replaceText(ls, le, before + line + after);
        contenuArea.requestFocus();
    }

    // ════════════════════════════════════════════
    // Génération automatique (Wikipedia — sans clé)
    // ════════════════════════════════════════════

    @FXML void handleGenerate(ActionEvent event) {
        String organe = organeField.getText().trim();
        if (organe.isEmpty()) return;

        setGenerating(true, "Recherche sur Wikipedia…", "#8B7EC0");

        Task<ArticleGeneratorService.GeneratedArticle> task = new Task<>() {
            @Override
            protected ArticleGeneratorService.GeneratedArticle call() throws Exception {
                return new ArticleGeneratorService().generate(organe, lastSelectedCancerType);
            }
        };

        task.setOnSucceeded(e -> {
            var art = task.getValue();
            titreField.setText(art.titre());
            tagsField.setText(art.tags());
            contenuArea.setText(art.contenu());
            contenuArea.setEditable(true);
            contenuArea.requestFocus();
            contenuArea.positionCaret(0);
            setGenerating(false, "✓ Brouillon généré — modifiez librement !", "#38A169");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Erreur inconnue";
            setGenerating(false, "✗ " + msg, "#E53E3E");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void setGenerating(boolean loading, String statusMsg, String color) {
        Platform.runLater(() -> {
            generateBtn.setDisable(loading);
            generateSpinner.setVisible(loading);
            generateSpinner.setManaged(loading);
            generateStatusLabel.setText(statusMsg);
            generateStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
            generateStatusLabel.setVisible(true);
            generateStatusLabel.setManaged(true);
        });
    }

    // ════════════════════════════════════════════
    // Sauvegarde
    // ════════════════════════════════════════════

    @FXML void handleSave(ActionEvent event) {
        String titre   = titreField.getText().trim();
        String contenu = contenuArea.getText().trim();
        String organe  = organeField.getText().trim();
        String statut  = statutCombo.getValue();
        String tags    = tagsField.getText().trim();

        boolean ok = true;
        if (titre.isEmpty())   { fieldErr(titreField,   errorTitre,   "Le titre est obligatoire.");   ok = false; }
        if (contenu.isEmpty()) { fieldErr(contenuArea,  errorContenu, "Le contenu est obligatoire."); ok = false; }
        if (!ok) return;
        Utilisateur user = UserSession.getInstance().getCurrentUser();

        if (articleToEdit != null) {
            articleToEdit.setTitre(titre);
            articleToEdit.setContenu(contenu);
            articleToEdit.setOrgane(organe.isEmpty() ? null : organe);
            articleToEdit.setStatut(statut);
            articleToEdit.setTags(tags.isEmpty() ? null : tags);
            serviceArticle.update(articleToEdit);
        } else {
            Article a = new Article(titre, contenu, statut, organe.isEmpty() ? null : organe, user.getId());
            a.setTags(tags.isEmpty() ? null : tags);
            serviceArticle.add(a);
        }
        articleToEdit = null;
        App.navigate("ArticleList");
    }

    @FXML void handleSelectOrgane(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/views/BodySelector.fxml"));
            Parent root = loader.load();
            BodySelectorController ctrl = loader.getController();
            ctrl.setOrganeMode(
                organe -> organeField.setText(organe),
                tags   -> { if (tags != null && !tags.isEmpty()) tagsField.setText(tags); }
            );
            Stage stage = new Stage();
            stage.setTitle("Sélectionner un organe");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 820, 640);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            showError("Impossible d'ouvrir le sélecteur d'organe : " + ex.getMessage());
        }
    }

    @FXML void handleCancel(ActionEvent event) {
        articleToEdit = null;
        App.navigate("ArticleList");
    }

    private void fieldErr(javafx.scene.control.Control field, Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
        field.getStyleClass().add("field-input-error");
    }

    private void clearErr(javafx.scene.control.Control field, Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false);
        field.getStyleClass().remove("field-input-error");
    }

    private void showError(String msg) {
        errorTitre.setText(msg); errorTitre.setVisible(true); errorTitre.setManaged(true);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
