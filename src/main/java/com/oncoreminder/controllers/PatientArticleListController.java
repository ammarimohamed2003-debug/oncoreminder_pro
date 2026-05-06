package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Article;
import com.oncoreminder.models.Commentaire;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.services.ServiceCommentaire;
import com.oncoreminder.utils.MarkdownRenderer;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class PatientArticleListController {

    @FXML private Label patientNameLabel;
    @FXML private VBox articleListContainer;
    @FXML private Label placeholderLabel;
    @FXML private VBox detailContent;
    @FXML private Label articleTitreLabel;
    @FXML private Label statutBadge;
    @FXML private Label dateLabel;
    @FXML private Label organeLabel;
    @FXML private VBox contenuContainer;
    @FXML private Label likesLabel;
    @FXML private VBox commentsContainer;
    @FXML private TextArea commentField;

    private final ServiceArticle serviceArticle = new ServiceArticle();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private Article selectedArticle;

    @FXML
    public void initialize() {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            patientNameLabel.setText(user.getPrenom() + " " + user.getNom());
        }
        loadArticles();
    }

    private void loadArticles() {
        List<Article> articles = serviceArticle.getPublished();
        articleListContainer.getChildren().clear();
        if (articles.isEmpty()) {
            Label empty = new Label("Aucun article disponible.");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 13px;");
            articleListContainer.getChildren().add(empty);
            return;
        }
        for (Article article : articles) {
            articleListContainer.getChildren().add(createArticleCard(article));
        }
    }

    private VBox createArticleCard(Article article) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(15));
        card.setCursor(javafx.scene.Cursor.HAND);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label(article.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-text-fill: #4A2D8F;");
        titre.setWrapText(true);
        titre.setMaxWidth(200);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titre, spacer);

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        if (article.getDatePublication() != null) {
            Label date = new Label("📅 " + article.getDatePublication());
            date.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
            footer.getChildren().add(date);
        }
        if (article.getOrgane() != null) {
            Label organe = new Label("🏥 " + article.getOrgane());
            organe.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
            footer.getChildren().add(organe);
        }
        Label likes = new Label("❤ " + article.getLikes());
        likes.setStyle("-fx-text-fill: #FC8181; -fx-font-size: 11px;");
        footer.getChildren().add(likes);

        card.getChildren().addAll(header, footer);
        card.setOnMouseClicked(e -> selectArticle(article));

        if (selectedArticle != null && selectedArticle.getId() == article.getId()) {
            card.setStyle("-fx-border-color: #2BBCB0; -fx-border-width: 2px;");
        }

        return card;
    }

    private void selectArticle(Article article) {
        this.selectedArticle = article;
        placeholderLabel.setVisible(false);
        placeholderLabel.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);

        articleTitreLabel.setText(article.getTitre());
        statutBadge.setText(article.getStatut());
        dateLabel.setText(article.getDatePublication() != null ? "📅 " + article.getDatePublication() : "");
        organeLabel.setText(article.getOrgane() != null ? "🏥 " + article.getOrgane() : "");
        MarkdownRenderer.render(article.getContenu(), contenuContainer);
        likesLabel.setText(article.getLikes() + " likes");

        loadComments();
        loadArticles();
    }

    private void loadComments() {
        commentsContainer.getChildren().clear();
        if (selectedArticle == null) return;
        List<Commentaire> comments = serviceCommentaire.getByArticle(selectedArticle.getId());
        if (comments.isEmpty()) {
            Label empty = new Label("Aucun commentaire pour l'instant.");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 12px;");
            commentsContainer.getChildren().add(empty);
            return;
        }
        for (Commentaire c : comments) {
            commentsContainer.getChildren().add(createCommentCard(c));
        }
    }

    private VBox createCommentCard(Commentaire c) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: #F4F9FF; -fx-background-radius: 8; -fx-border-color: #C8DCF0; -fx-border-radius: 8; -fx-border-width: 1;");
        card.setPadding(new Insets(10));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label auteur = new Label("👤 " + c.getAuteur());
        auteur.setStyle("-fx-font-weight: bold; -fx-text-fill: #5B35A5; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label date = new Label(c.getFormattedDate());
        date.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 11px;");
        header.getChildren().addAll(auteur, spacer, date);

        Label contenu = new Label(c.getContenu());
        contenu.setWrapText(true);
        contenu.setStyle("-fx-text-fill: #2D3748; -fx-font-size: 13px;");

        card.getChildren().addAll(header, contenu);
        return card;
    }

    @FXML
    void handleLike(ActionEvent event) {
        if (selectedArticle == null) return;
        serviceArticle.addLike(selectedArticle.getId());
        selectedArticle.setLikes(selectedArticle.getLikes() + 1);
        likesLabel.setText(selectedArticle.getLikes() + " likes");
    }

    @FXML
    void handleAddComment(ActionEvent event) {
        String text = commentField.getText().trim();
        if (text.isEmpty() || selectedArticle == null) return;
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        String auteur = user.getPrenom() + " " + user.getNom();
        serviceCommentaire.add(new Commentaire(selectedArticle.getId(), text, auteur));
        commentField.clear();
        loadComments();
    }

    @FXML
    void handleDashboard(ActionEvent event) {
        App.navigate("PatientDashboard");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}
