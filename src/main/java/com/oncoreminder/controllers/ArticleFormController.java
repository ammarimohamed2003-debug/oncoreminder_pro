package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Article;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.utils.MarkdownRenderer;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ArticleFormController {

    @FXML private Label formTitleLabel;
    @FXML private TextField titreField;
    @FXML private TextField organeField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextArea contenuArea;
    @FXML private VBox previewContainer;
    @FXML private Label errorLabel;

    private final ServiceArticle serviceArticle = new ServiceArticle();
    private static Article articleToEdit;

    public static void setArticleToEdit(Article article) {
        articleToEdit = article;
    }

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("BROUILLON", "PUBLIE");
        statutCombo.setValue("BROUILLON");

        // Aperçu en direct
        contenuArea.textProperty().addListener((obs, oldVal, newVal) ->
                MarkdownRenderer.render(newVal, previewContainer));

        if (articleToEdit != null) {
            formTitleLabel.setText("Modifier l'Article");
            titreField.setText(articleToEdit.getTitre());
            organeField.setText(articleToEdit.getOrgane() != null ? articleToEdit.getOrgane() : "");
            statutCombo.setValue(articleToEdit.getStatut());
            contenuArea.setText(articleToEdit.getContenu());
        } else {
            formTitleLabel.setText("Nouvel Article");
        }
    }

    // ── Style texte ────────────────────────────────────────────

    @FXML void fmtBold(ActionEvent e)      { wrapSelection("**", "**"); }
    @FXML void fmtItalic(ActionEvent e)    { wrapSelection("*", "*"); }
    @FXML void fmtUnderline(ActionEvent e) { wrapSelection("__", "__"); }
    @FXML void fmtStrike(ActionEvent e)    { wrapSelection("~~", "~~"); }
    @FXML void fmtCode(ActionEvent e)      { wrapSelection("`", "`"); }
    @FXML void fmtHighlight(ActionEvent e) { wrapSelectionTag("hl"); }
    @FXML void fmtSup(ActionEvent e)       { wrapSelectionTag("sup"); }
    @FXML void fmtSub(ActionEvent e)       { wrapSelectionTag("sub"); }

    // ── Structure ──────────────────────────────────────────────

    @FXML void fmtH1(ActionEvent e)     { insertLinePrefix("# "); }
    @FXML void fmtH2(ActionEvent e)     { insertLinePrefix("## "); }
    @FXML void fmtList(ActionEvent e)   { insertLinePrefix("- "); }
    @FXML void fmtNumList(ActionEvent e){ insertLinePrefix("1. "); }
    @FXML void fmtQuote(ActionEvent e)  { insertLinePrefix("> "); }
    @FXML void fmtCenter(ActionEvent e) { wrapLine("[center]", "[/center]"); }

    @FXML
    void fmtHr(ActionEvent e) {
        int pos = contenuArea.getCaretPosition();
        String text = contenuArea.getText();
        String insert = "\n---\n";
        contenuArea.setText(text.substring(0, pos) + insert + text.substring(pos));
        contenuArea.positionCaret(pos + insert.length());
        contenuArea.requestFocus();
    }

    // ── Taille ─────────────────────────────────────────────────

    @FXML void fmtSmall(ActionEvent e)  { wrapSelectionTag("small"); }
    @FXML void fmtNormal(ActionEvent e) { wrapSelectionTag("normal"); }
    @FXML void fmtBig(ActionEvent e)    { wrapSelectionTag("big"); }
    @FXML void fmtXl(ActionEvent e)     { wrapSelectionTag("xl"); }

    // ── Couleurs ───────────────────────────────────────────────

    @FXML void fmtRed(ActionEvent e)    { wrapSelectionTag("red"); }
    @FXML void fmtOrange(ActionEvent e) { wrapSelectionTag("orange"); }
    @FXML void fmtGreen(ActionEvent e)  { wrapSelectionTag("green"); }
    @FXML void fmtBlue(ActionEvent e)   { wrapSelectionTag("blue"); }
    @FXML void fmtPurple(ActionEvent e) { wrapSelectionTag("purple"); }
    @FXML void fmtTeal(ActionEvent e)   { wrapSelectionTag("teal"); }
    @FXML void fmtGray(ActionEvent e)   { wrapSelectionTag("gray"); }
    @FXML void fmtBlack(ActionEvent e)  { wrapSelectionTag("black"); }

    // ── Effacer formatage ──────────────────────────────────────

    @FXML
    void fmtClear(ActionEvent e) {
        IndexRange sel = contenuArea.getSelection();
        if (sel.getLength() == 0) return;
        String selected = contenuArea.getSelectedText();
        // Supprimer tous les marqueurs markdown et tags
        String clean = selected
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[/?[a-z]+]", "");
        contenuArea.replaceSelection(clean);
        contenuArea.requestFocus();
    }

    // ── Helpers ────────────────────────────────────────────────

    private void wrapSelectionTag(String tag) {
        wrapSelection("[" + tag + "]", "[/" + tag + "]");
    }

    private void wrapSelection(String before, String after) {
        IndexRange sel = contenuArea.getSelection();
        if (sel.getLength() == 0) {
            int pos = contenuArea.getCaretPosition();
            String placeholder = "texte";
            contenuArea.insertText(pos, before + placeholder + after);
            contenuArea.selectRange(pos + before.length(), pos + before.length() + placeholder.length());
        } else {
            contenuArea.replaceSelection(before + contenuArea.getSelectedText() + after);
        }
        contenuArea.requestFocus();
    }

    private void insertLinePrefix(String prefix) {
        int pos = contenuArea.getCaretPosition();
        String text = contenuArea.getText();
        int lineStart = text.lastIndexOf('\n', pos - 1) + 1;
        contenuArea.insertText(lineStart, prefix);
        contenuArea.positionCaret(pos + prefix.length());
        contenuArea.requestFocus();
    }

    private void wrapLine(String before, String after) {
        int pos = contenuArea.getCaretPosition();
        String text = contenuArea.getText();
        int lineStart = text.lastIndexOf('\n', pos - 1) + 1;
        int lineEnd   = text.indexOf('\n', pos);
        if (lineEnd == -1) lineEnd = text.length();
        String line = text.substring(lineStart, lineEnd);
        contenuArea.replaceText(lineStart, lineEnd, before + line + after);
        contenuArea.positionCaret(lineStart + before.length() + line.length() + after.length());
        contenuArea.requestFocus();
    }

    // ── Sauvegarde ─────────────────────────────────────────────

    @FXML
    void handleSave(ActionEvent event) {
        String titre  = titreField.getText().trim();
        String contenu = contenuArea.getText().trim();
        String organe  = organeField.getText().trim();
        String statut  = statutCombo.getValue();

        if (titre.isEmpty() || contenu.isEmpty()) {
            errorLabel.setText("Le titre et le contenu sont obligatoires.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        Utilisateur user = UserSession.getInstance().getCurrentUser();

        if (articleToEdit != null) {
            articleToEdit.setTitre(titre);
            articleToEdit.setContenu(contenu);
            articleToEdit.setOrgane(organe.isEmpty() ? null : organe);
            articleToEdit.setStatut(statut);
            serviceArticle.update(articleToEdit);
        } else {
            serviceArticle.add(new Article(titre, contenu, statut,
                    organe.isEmpty() ? null : organe, user.getId()));
        }

        articleToEdit = null;
        App.navigate("ArticleList");
    }

    @FXML
    void handleCancel(ActionEvent event) {
        articleToEdit = null;
        App.navigate("ArticleList");
    }
}
