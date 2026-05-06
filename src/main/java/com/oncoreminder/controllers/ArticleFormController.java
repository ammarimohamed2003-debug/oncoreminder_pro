package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Article;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ArticleFormController {

    @FXML private Label formTitleLabel;
    @FXML private TextField titreField;
    @FXML private TextField organeField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextArea contenuArea;
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

    @FXML
    void handleSave(ActionEvent event) {
        String titre = titreField.getText().trim();
        String contenu = contenuArea.getText().trim();
        String organe = organeField.getText().trim();
        String statut = statutCombo.getValue();

        if (titre.isEmpty() || contenu.isEmpty()) {
            errorLabel.setText("Le titre et le contenu sont obligatoires.");
            errorLabel.setVisible(true);
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
            serviceArticle.add(new Article(titre, contenu, statut, organe.isEmpty() ? null : organe, user.getId()));
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
