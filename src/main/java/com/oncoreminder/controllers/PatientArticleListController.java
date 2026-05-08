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
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class PatientArticleListController {

    // Sidebar
    @FXML private Label patientNameLabel;

    // Grid view
    @FXML private VBox      gridView;
    @FXML private TextField searchField;
    @FXML private FlowPane  articleFlowPane;

    // Detail view
    @FXML private VBox   detailView;
    @FXML private Label  articleTitreLabel;
    @FXML private Label  statutBadge;
    @FXML private Label  dateLabel;
    @FXML private Label  organeLabel;
    @FXML private Label  tagsLabel;
    @FXML private Label  viewsLabel;
    @FXML private VBox   contenuContainer;
    @FXML private Button likeBtn;
    @FXML private Label  likesLabel;
    @FXML private VBox   commentsContainer;
    @FXML private TextArea commentField;

    private final ServiceArticle     serviceArticle     = new ServiceArticle();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    private Article       selectedArticle;
    private List<Article> currentArticles;

    @FXML
    public void initialize() {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        if (user != null) patientNameLabel.setText(user.getPrenom() + " " + user.getNom());
        searchField.textProperty().addListener((obs, o, n) -> filterBySearch(n));
        loadArticles();
    }

    // ─── Chargement ──────────────────────────────────────────────────

    private void loadArticles() {
        currentArticles = serviceArticle.getPublished();
        renderGrid(currentArticles);
    }

    private void filterBySearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            renderGrid(currentArticles);
            return;
        }
        List<Article> filtered = serviceArticle.search(keyword.trim());
        filtered.removeIf(a -> !"PUBLIE".equals(a.getStatut()));
        renderGrid(filtered);
    }

    // ─── Grille de cards ─────────────────────────────────────────────

    private void renderGrid(List<Article> articles) {
        articleFlowPane.getChildren().clear();
        if (articles.isEmpty()) {
            Label empty = new Label("Aucun article disponible.");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 14px; -fx-padding: 20;");
            articleFlowPane.getChildren().add(empty);
            return;
        }
        for (Article a : articles) articleFlowPane.getChildren().add(buildCard(a));
    }

    private VBox buildCard(Article article) {
        VBox card = new VBox(10);
        card.setPrefWidth(268);
        card.setMaxWidth(268);
        card.setPadding(new Insets(16));
        card.setCursor(Cursor.HAND);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E0D4F8;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,50,180,0.10), 10, 0, 0, 3);"
        );

        // Organe badge en haut
        if (article.getOrgane() != null && !article.getOrgane().isEmpty()) {
            Label badge = new Label("🏥 " + article.getOrgane());
            badge.setStyle(
                "-fx-background-color: #EEF2FF; -fx-text-fill: #5B35A5;" +
                "-fx-padding: 2 8; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: bold;"
            );
            card.getChildren().add(badge);
        }

        // Titre
        Label titre = new Label(article.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #3A1D7A;");
        titre.setWrapText(true);
        titre.setMaxWidth(236);
        card.getChildren().add(titre);

        // Extrait du contenu (texte brut, 130 chars max)
        if (article.getContenu() != null && !article.getContenu().isEmpty()) {
            String raw = article.getContenu()
                .replaceAll("\\*{1,3}|#{1,6} |__|~~|`|\\[/?[a-z]+]|> |^- |^\\d+\\. ", "")
                .replaceAll("\\s+", " ").trim();
            String excerpt = raw.length() > 130 ? raw.substring(0, 127) + "…" : raw;
            Label excLabel = new Label(excerpt);
            excLabel.setWrapText(true);
            excLabel.setMaxWidth(236);
            excLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
            card.getChildren().add(excLabel);
        }

        // Tags
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            HBox tagsRow = new HBox(5);
            tagsRow.setAlignment(Pos.CENTER_LEFT);
            for (String tag : article.getTags().split(",")) {
                Label t = new Label(tag.trim());
                t.setStyle(
                    "-fx-background-color: #F3F0FF; -fx-text-fill: #7C3AED;" +
                    "-fx-padding: 1 6; -fx-background-radius: 8; -fx-font-size: 10px;"
                );
                tagsRow.getChildren().add(t);
            }
            card.getChildren().add(tagsRow);
        }

        // Séparateur visuel
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E9E3FF;");
        card.getChildren().add(sep);

        // Footer : date + likes + vues
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        if (article.getDatePublication() != null) {
            Label date = new Label("📅 " + article.getDatePublication());
            date.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px;");
            footer.getChildren().add(date);
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        footer.getChildren().add(sp);
        Label likes = new Label("❤ " + article.getLikes());
        likes.setStyle("-fx-text-fill: #F87171; -fx-font-size: 11px; -fx-font-weight: bold;");
        footer.getChildren().add(likes);
        if (article.getViews() > 0) {
            Label views = new Label("👁 " + article.getViews());
            views.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            footer.getChildren().add(views);
        }
        card.getChildren().add(footer);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #F5F0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #7C3AED;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,50,180,0.22), 16, 0, 0, 5);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E0D4F8;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,50,180,0.10), 10, 0, 0, 3);"
        ));

        card.setOnMouseClicked(e -> selectArticle(article));
        return card;
    }

    // ─── Détail ──────────────────────────────────────────────────────

    private void selectArticle(Article article) {
        this.selectedArticle = article;
        int userId = UserSession.getInstance().getCurrentUser().getId();

        if (serviceArticle.incrementViewIfNew(article.getId(), userId))
            article.setViews(article.getViews() + 1);

        articleTitreLabel.setText(article.getTitre());
        statutBadge.setText("Publié");
        dateLabel.setText(article.getDatePublication() != null ? "📅 " + article.getDatePublication() : "");
        organeLabel.setText(article.getOrgane() != null ? "🏥 " + article.getOrgane() : "");
        viewsLabel.setText("👁 " + article.getViews() + " vues");

        String tags = article.getTags();
        tagsLabel.setText(tags != null && !tags.isEmpty() ? "🏷 " + tags : "");
        tagsLabel.setVisible(tags != null && !tags.isEmpty());
        tagsLabel.setManaged(tags != null && !tags.isEmpty());

        MarkdownRenderer.render(article.getContenu(), contenuContainer);
        likesLabel.setText(article.getLikes() + " likes");

        applyLikeBtnState(serviceArticle.hasLiked(article.getId(), userId));
        loadComments();
        showDetail(true);
    }

    private void applyLikeBtnState(boolean liked) {
        if (liked) {
            likeBtn.setText("❤  Aimé ✓");
            likeBtn.getStyleClass().setAll("btn-primary");
            likeBtn.setStyle("-fx-background-color: #FC8181; -fx-padding: 0 20;");
        } else {
            likeBtn.setText("❤  J'aime");
            likeBtn.getStyleClass().setAll("btn-outline");
            likeBtn.setStyle("-fx-padding: 0 20;");
        }
    }

    @FXML void handleBack(ActionEvent event) {
        showDetail(false);
        renderGrid(currentArticles);
    }

    private void showDetail(boolean show) {
        gridView.setVisible(!show);
        gridView.setManaged(!show);
        detailView.setVisible(show);
        detailView.setManaged(show);
    }

    // ─── Commentaires ────────────────────────────────────────────────

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
        for (Commentaire c : comments) commentsContainer.getChildren().add(createCommentCard(c));
    }

    private VBox createCommentCard(Commentaire c) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: #F4F9FF; -fx-background-radius: 8; -fx-border-color: #C8DCF0; -fx-border-radius: 8; -fx-border-width: 1;");
        card.setPadding(new Insets(10));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label auteur = new Label("👤 " + c.getAuteur());
        auteur.setStyle("-fx-font-weight: bold; -fx-text-fill: #5B35A5; -fx-font-size: 12px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label date = new Label(c.getFormattedDate());
        date.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 11px;");
        header.getChildren().addAll(auteur, spacer, date);

        Label contenu = new Label(c.getContenu());
        contenu.setWrapText(true);
        contenu.setStyle("-fx-text-fill: #2D3748; -fx-font-size: 13px;");

        int userId = UserSession.getInstance().getCurrentUser().getId();
        boolean alreadyLiked = serviceCommentaire.hasLiked(c.getId(), userId);

        HBox likeRow = new HBox(6);
        likeRow.setAlignment(Pos.CENTER_LEFT);
        Label likeCount = new Label("👍 " + c.getLikes());
        likeCount.setStyle("-fx-text-fill: #5B35A5; -fx-font-size: 11px;");
        Button cLikeBtn = new Button(alreadyLiked ? "👍 Aimé" : "👍");
        cLikeBtn.setStyle("-fx-background-color: " + (alreadyLiked ? "#EEF2FF" : "transparent") +
            "; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 6; -fx-border-radius: 5;");
        cLikeBtn.setOnAction(e -> {
            boolean nowLiked = serviceCommentaire.toggleLike(c.getId(), userId);
            c.setLikes(nowLiked ? c.getLikes() + 1 : c.getLikes() - 1);
            likeCount.setText("👍 " + c.getLikes());
            cLikeBtn.setText(nowLiked ? "👍 Aimé" : "👍");
            cLikeBtn.setStyle("-fx-background-color: " + (nowLiked ? "#EEF2FF" : "transparent") +
                "; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0 6; -fx-border-radius: 5;");
        });
        likeRow.getChildren().addAll(cLikeBtn, likeCount);

        card.getChildren().addAll(header, contenu, likeRow);
        return card;
    }

    // ─── Actions ─────────────────────────────────────────────────────

    @FXML void handleLike(ActionEvent event) {
        if (selectedArticle == null) return;
        int userId = UserSession.getInstance().getCurrentUser().getId();
        boolean nowLiked = serviceArticle.toggleLike(selectedArticle.getId(), userId);
        selectedArticle.setLikes(nowLiked ? selectedArticle.getLikes() + 1 : selectedArticle.getLikes() - 1);
        likesLabel.setText(selectedArticle.getLikes() + " likes");
        applyLikeBtnState(nowLiked);
    }

    @FXML void handleAddComment(ActionEvent event) {
        String text = commentField.getText().trim();
        if (text.isEmpty() || selectedArticle == null) return;
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        serviceCommentaire.add(new Commentaire(selectedArticle.getId(), text, user.getPrenom() + " " + user.getNom()));
        commentField.clear();
        loadComments();
    }

    @FXML void handleDashboard(ActionEvent event) { App.navigate("PatientDashboard"); }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}
