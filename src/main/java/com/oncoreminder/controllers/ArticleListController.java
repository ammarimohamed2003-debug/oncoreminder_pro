package com.oncoreminder.controllers;

import com.oncoreminder.models.Article;
import com.oncoreminder.models.Commentaire;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.services.ServiceCommentaire;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ArticleListController {

    @FXML private Label userNameLabel;
    @FXML private Button newArticleBtn;
    @FXML private HBox filterBar;
    @FXML private VBox articleListContainer;
    @FXML private Label placeholderLabel;
    @FXML private VBox detailContent;
    @FXML private Label articleTitreLabel;
    @FXML private Label statutBadge;
    @FXML private Label dateLabel;
    @FXML private Label organeLabel;
    @FXML private TextArea contenuArea;
    @FXML private Label likesLabel;
    @FXML private HBox medecinActions;
    @FXML private Button publishBtn;
    @FXML private Button archiveBtn;
    @FXML private VBox commentsContainer;
    @FXML private TextField commentField;

    private final ServiceArticle serviceArticle = new ServiceArticle();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private Article selectedArticle;
    private boolean showingAll = true;

    @FXML
    public void initialize() {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;

        boolean isMedecin = "MEDECIN".equals(user.getRole());
        userNameLabel.setText(isMedecin ? "Dr. " + user.getNom() : user.getPrenom() + " " + user.getNom());
        newArticleBtn.setVisible(isMedecin);
        newArticleBtn.setManaged(isMedecin);
        filterBar.setVisible(isMedecin);
        filterBar.setManaged(isMedecin);

        loadArticles();
    }

    private void loadArticles() {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        List<Article> articles;
        if ("MEDECIN".equals(user.getRole())) {
            articles = showingAll ? serviceArticle.getAll() : serviceArticle.getByMedecin(user.getId());
        } else {
            articles = serviceArticle.getPublished();
        }
        renderArticleList(articles);
    }

    private void renderArticleList(List<Article> articles) {
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
        Label badge = new Label(article.getStatut());
        badge.getStyleClass().add(getBadgeStyle(article.getStatut()));
        header.getChildren().addAll(titre, spacer, badge);

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

    private String getBadgeStyle(String statut) {
        if (statut == null) return "stat-badge-info";
        switch (statut) {
            case "PUBLIE":   return "stat-badge-success";
            case "BROUILLON": return "stat-badge-warning";
            default:          return "stat-badge-info";
        }
    }

    private void selectArticle(Article article) {
        this.selectedArticle = article;
        placeholderLabel.setVisible(false);
        placeholderLabel.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);

        articleTitreLabel.setText(article.getTitre());
        statutBadge.setText(article.getStatut());
        statutBadge.getStyleClass().setAll(getBadgeStyle(article.getStatut()));
        dateLabel.setText(article.getDatePublication() != null ? "📅 " + article.getDatePublication() : "");
        organeLabel.setText(article.getOrgane() != null ? "🏥 " + article.getOrgane() : "");
        contenuArea.setText(article.getContenu());
        likesLabel.setText(article.getLikes() + " likes");

        Utilisateur user = UserSession.getInstance().getCurrentUser();
        boolean isMedecin = "MEDECIN".equals(user.getRole());
        medecinActions.setVisible(isMedecin);
        medecinActions.setManaged(isMedecin);

        if (isMedecin) {
            publishBtn.setVisible(!"PUBLIE".equals(article.getStatut()));
            publishBtn.setManaged(!"PUBLIE".equals(article.getStatut()));
            archiveBtn.setVisible(!"ARCHIVE".equals(article.getStatut()));
            archiveBtn.setManaged(!"ARCHIVE".equals(article.getStatut()));
        }

        loadComments();
        renderArticleList(showingAll ? serviceArticle.getAll() : serviceArticle.getByMedecin(user.getId()));
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
    void filterAll(ActionEvent event) {
        showingAll = true;
        loadArticles();
    }

    @FXML
    void filterMine(ActionEvent event) {
        showingAll = false;
        loadArticles();
    }

    @FXML
    void handleNewArticle(ActionEvent event) {
        ArticleFormController.setArticleToEdit(null);
        navigateTo("ArticleForm");
    }

    @FXML
    void handleEdit(ActionEvent event) {
        if (selectedArticle == null) return;
        ArticleFormController.setArticleToEdit(selectedArticle);
        navigateTo("ArticleForm");
    }

    @FXML
    void handleLike(ActionEvent event) {
        if (selectedArticle == null) return;
        serviceArticle.addLike(selectedArticle.getId());
        selectedArticle.setLikes(selectedArticle.getLikes() + 1);
        likesLabel.setText(selectedArticle.getLikes() + " likes");
    }

    @FXML
    void handlePublish(ActionEvent event) {
        if (selectedArticle == null) return;
        serviceArticle.updateStatut(selectedArticle.getId(), "PUBLIE");
        selectedArticle.setStatut("PUBLIE");
        selectArticle(selectedArticle);
    }

    @FXML
    void handleArchive(ActionEvent event) {
        if (selectedArticle == null) return;
        serviceArticle.updateStatut(selectedArticle.getId(), "ARCHIVE");
        selectedArticle.setStatut("ARCHIVE");
        selectArticle(selectedArticle);
    }

    @FXML
    void handleDelete(ActionEvent event) {
        if (selectedArticle == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer l'article \"" + selectedArticle.getTitre() + "\" ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                serviceArticle.delete(selectedArticle.getId());
                selectedArticle = null;
                detailContent.setVisible(false);
                detailContent.setManaged(false);
                placeholderLabel.setVisible(true);
                placeholderLabel.setManaged(true);
                loadArticles();
            }
        });
    }

    @FXML
    void handleAddComment(ActionEvent event) {
        String text = commentField.getText().trim();
        if (text.isEmpty() || selectedArticle == null) return;
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        String auteur = "MEDECIN".equals(user.getRole())
            ? "Dr. " + user.getNom()
            : user.getPrenom() + " " + user.getNom();
        serviceCommentaire.add(new Commentaire(selectedArticle.getId(), text, auteur));
        commentField.clear();
        loadComments();
    }

    @FXML
    void handleBack(ActionEvent event) {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        String view = "MEDECIN".equals(user.getRole()) ? "DoctorDashboard" : "PatientDashboard";
        navigateTo(view);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        navigateTo("Login");
    }

    private void navigateTo(String view) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/" + view + ".fxml"));
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
