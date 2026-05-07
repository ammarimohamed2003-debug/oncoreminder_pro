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
import javafx.scene.layout.*;

public class ArticleFormController {

    @FXML private Label            formTitleLabel;
    @FXML private TextField        titreField;
    @FXML private TextField        organeField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextField        tagsField;
    @FXML private TextArea         contenuArea;
    @FXML private VBox             previewContainer;
    @FXML private Label            errorLabel;

    private final ServiceArticle serviceArticle = new ServiceArticle();
    private static Article articleToEdit;

    public static void setArticleToEdit(Article article) { articleToEdit = article; }

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("BROUILLON", "PUBLIE");
        statutCombo.setValue("BROUILLON");

        contenuArea.textProperty().addListener((obs, o, n) ->
                MarkdownRenderer.render(n, previewContainer));

        if (articleToEdit != null) {
            formTitleLabel.setText("Modifier l'Article");
            titreField.setText(articleToEdit.getTitre());
            organeField.setText(nvl(articleToEdit.getOrgane()));
            statutCombo.setValue(articleToEdit.getStatut());
            tagsField.setText(nvl(articleToEdit.getTags()));
            contenuArea.setText(nvl(articleToEdit.getContenu()));
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
    // Sauvegarde
    // ════════════════════════════════════════════

    @FXML void handleSave(ActionEvent event) {
        String titre   = titreField.getText().trim();
        String contenu = contenuArea.getText().trim();
        String organe  = organeField.getText().trim();
        String statut  = statutCombo.getValue();
        String tags    = tagsField.getText().trim();

        if (titre.isEmpty() || contenu.isEmpty()) {
            showError("Le titre et le contenu sont obligatoires.");
            return;
        }
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

    @FXML void handleCancel(ActionEvent event) {
        articleToEdit = null;
        App.navigate("ArticleList");
    }

    private void showError(String msg) {
        errorLabel.setText(msg); errorLabel.setVisible(true); errorLabel.setManaged(true);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
