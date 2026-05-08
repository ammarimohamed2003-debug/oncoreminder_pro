package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Article;
import com.oncoreminder.models.Commentaire;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.services.ServiceCommentaire;
import com.oncoreminder.utils.ImageLoader;
import com.oncoreminder.utils.MarkdownRenderer;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.List;

public class ArticleListController {

    // ── Grid view ─────────────────────────────────────────────
    @FXML private VBox      gridView;
    @FXML private FlowPane  articleFlowPane;
    @FXML private HBox      filterBar;
    @FXML private HBox      paginationBar;
    @FXML private TextField searchField;
    @FXML private Button    newArticleBtn;
    @FXML private Label     userNameLabel;

    // ── Detail view ───────────────────────────────────────────
    @FXML private VBox      detailView;
    @FXML private Label     articleTitreLabel;
    @FXML private Label     statutBadge;
    @FXML private Label     dateLabel;
    @FXML private Label     organeLabel;
    @FXML private Label     tagsLabel;
    @FXML private Label     viewsLabel;
    @FXML private VBox      contenuContainer;
    @FXML private StackPane imageContainer;
    @FXML private ImageView detailImageView;
    @FXML private Button    likeBtn;
    @FXML private Label  likesLabel;
    @FXML private HBox   medecinActions;
    @FXML private Button publishBtn;
    @FXML private Button archiveBtn;
    @FXML private VBox   commentsContainer;
    @FXML private TextArea commentField;

    private final ServiceArticle     serviceArticle     = new ServiceArticle();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    private static final int PAGE_SIZE = 5;

    private Article       selectedArticle;
    private boolean       showingAll = true;
    private List<Article> currentArticles;
    private List<Article> displayedArticles;
    private int           currentPage = 0;

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

        searchField.textProperty().addListener((obs, o, n) -> filterBySearch(n));

        loadArticles();
    }

    private void loadArticles() {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        if ("MEDECIN".equals(user.getRole())) {
            currentArticles = showingAll ? serviceArticle.getAll() : serviceArticle.getByMedecin(user.getId());
        } else {
            currentArticles = serviceArticle.getPublished();
        }
        renderGrid(currentArticles);
    }

    private void filterBySearch(String keyword) {
        currentPage = 0;
        if (keyword == null || keyword.trim().isEmpty()) {
            renderGrid(currentArticles);
            return;
        }
        List<Article> filtered = serviceArticle.search(keyword.trim());
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        if (!"MEDECIN".equals(user.getRole())) {
            filtered.removeIf(a -> !"PUBLIE".equals(a.getStatut()));
        }
        renderGrid(filtered);
    }

    private void renderGrid(List<Article> articles) {
        this.displayedArticles = articles;
        articleFlowPane.getChildren().clear();

        int total = articles.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)           currentPage = 0;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        if (total == 0) {
            Label empty = new Label("Aucun article disponible.");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 13px; -fx-padding: 20;");
            articleFlowPane.getChildren().add(empty);
        } else {
            for (Article article : articles.subList(from, to)) {
                articleFlowPane.getChildren().add(buildCard(article));
            }
        }
        updatePaginationBar(total);
    }

    private void updatePaginationBar(int total) {
        paginationBar.getChildren().clear();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));

        // Prev
        Button prev = new Button("←");
        prev.setDisable(currentPage == 0);
        prev.setStyle(pageStyle(false));
        prev.setOnAction(e -> { currentPage--; renderGrid(displayedArticles); });
        paginationBar.getChildren().add(prev);

        // Page numbers (max 7 visible)
        int start = Math.max(0, currentPage - 3);
        int end   = Math.min(totalPages - 1, start + 6);
        start = Math.max(0, end - 6);

        if (start > 0) {
            paginationBar.getChildren().add(pageBtn(0));
            if (start > 1) paginationBar.getChildren().add(ellipsisLabel());
        }
        for (int i = start; i <= end; i++) paginationBar.getChildren().add(pageBtn(i));
        if (end < totalPages - 1) {
            if (end < totalPages - 2) paginationBar.getChildren().add(ellipsisLabel());
            paginationBar.getChildren().add(pageBtn(totalPages - 1));
        }

        // Next
        Button next = new Button("→");
        next.setDisable(currentPage >= totalPages - 1);
        next.setStyle(pageStyle(false));
        next.setOnAction(e -> { currentPage++; renderGrid(displayedArticles); });
        paginationBar.getChildren().add(next);

        // Count label (centré, après les boutons)
        int from = currentPage * PAGE_SIZE + 1;
        int to   = Math.min((currentPage + 1) * PAGE_SIZE, total);
        String range = total == 0 ? "0" : from + "–" + to;
        Label count = new Label("  " + range + " sur " + total + " article" + (total > 1 ? "s" : ""));
        count.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
        paginationBar.getChildren().add(count);
    }

    private Button pageBtn(int index) {
        Button btn = new Button(String.valueOf(index + 1));
        btn.setStyle(pageStyle(index == currentPage));
        btn.setOnAction(e -> { currentPage = index; renderGrid(displayedArticles); });
        return btn;
    }

    private Label ellipsisLabel() {
        Label l = new Label("…");
        l.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 0 4;");
        return l;
    }

    private String pageStyle(boolean active) {
        return active
            ? "-fx-background-color: #5B35A5; -fx-text-fill: white; -fx-background-radius: 8;" +
              "-fx-border-width: 0; -fx-font-size: 13px; -fx-min-width: 34; -fx-pref-height: 34; -fx-cursor: hand;"
            : "-fx-background-color: white; -fx-text-fill: #374151; -fx-background-radius: 8;" +
              "-fx-border-color: #E5E7EB; -fx-border-radius: 8; -fx-border-width: 1;" +
              "-fx-font-size: 13px; -fx-min-width: 34; -fx-pref-height: 34; -fx-cursor: hand;";
    }

    private VBox buildCard(Article article) {
        VBox card = new VBox(8);
        card.setPrefWidth(268);
        card.setMaxWidth(268);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-border-color: #E9E4F7; -fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.08), 8, 0, 0, 2);"
        );
        card.setPadding(new Insets(0));
        card.setCursor(javafx.scene.Cursor.HAND);

        // Cover image with rounded-top clip
        if (article.getImagePath() != null && !article.getImagePath().isEmpty()) {
            Image img = ImageLoader.load(new File(article.getImagePath()));
            if (img != null && !img.isError()) {
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(268);
                imgView.setFitHeight(158);
                imgView.setPreserveRatio(false);
                imgView.setSmooth(true);
                Rectangle clip = new Rectangle(268, 158);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                imgView.setClip(clip);
                card.getChildren().add(imgView);
            }
        }

        VBox inner = new VBox(8);
        inner.setPadding(new Insets(12, 16, 14, 16));
        card.getChildren().add(inner);

        // Organe badge
        if (article.getOrgane() != null && !article.getOrgane().isEmpty()) {
            Label organeBadge = new Label("🏥 " + article.getOrgane());
            organeBadge.setStyle(
                "-fx-background-color: #EEF2FF; -fx-text-fill: #5B35A5;" +
                "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px;"
            );
            inner.getChildren().add(organeBadge);
        }

        // Title + status badge
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.TOP_LEFT);
        Label titre = new Label(article.getTitre());
        titre.setWrapText(true);
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2D1B69;");
        HBox.setHgrow(titre, Priority.ALWAYS);
        Label badge = new Label(article.getStatut());
        badge.getStyleClass().add(getBadgeStyle(article.getStatut()));
        titleRow.getChildren().addAll(titre, badge);
        inner.getChildren().add(titleRow);

        // Excerpt
        String contenu = article.getContenu() != null ? article.getContenu() : "";
        String plain = contenu.replaceAll("\\[/?[a-z]+]", "").replaceAll("[#*`~>_]", "").trim();
        if (plain.length() > 130) plain = plain.substring(0, 130) + "…";
        if (!plain.isEmpty()) {
            Label excerpt = new Label(plain);
            excerpt.setWrapText(true);
            excerpt.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            inner.getChildren().add(excerpt);
        }

        // Tags pills
        if (article.getTags() != null && !article.getTags().isEmpty()) {
            HBox tagsRow = new HBox(5);
            tagsRow.setAlignment(Pos.CENTER_LEFT);
            tagsRow.setStyle("-fx-padding: 2 0 0 0;");
            for (String tag : article.getTags().split(",")) {
                Label pill = new Label(tag.trim());
                pill.setStyle(
                    "-fx-background-color: #F3F0FF; -fx-text-fill: #5B35A5;" +
                    "-fx-padding: 2 7; -fx-background-radius: 8; -fx-font-size: 10px;"
                );
                tagsRow.getChildren().add(pill);
            }
            inner.getChildren().add(tagsRow);
        }

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #F0EBF8;");
        inner.getChildren().add(sep);

        // Footer: date, likes, views
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        if (article.getDatePublication() != null) {
            Label date = new Label("📅 " + article.getDatePublication());
            date.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            footer.getChildren().add(date);
        }
        Label likes = new Label("❤ " + article.getLikes());
        likes.setStyle("-fx-text-fill: #FC8181; -fx-font-size: 11px;");
        footer.getChildren().add(likes);
        if (article.getViews() > 0) {
            Label views = new Label("👁 " + article.getViews());
            views.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            footer.getChildren().add(views);
        }
        inner.getChildren().add(footer);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-border-color: #5B35A5; -fx-border-radius: 12; -fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.18), 12, 0, 0, 4);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-border-color: #E9E4F7; -fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.08), 8, 0, 0, 2);"
        ));
        card.setOnMouseClicked(e -> selectArticle(article));

        return card;
    }

    private String getBadgeStyle(String statut) {
        if (statut == null) return "stat-badge-info";
        return switch (statut) {
            case "PUBLIE"    -> "stat-badge-success";
            case "BROUILLON" -> "stat-badge-warning";
            default          -> "stat-badge-info";
        };
    }

    private void selectArticle(Article article) {
        this.selectedArticle = article;
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        int userId = user.getId();

        if (serviceArticle.incrementViewIfNew(article.getId(), userId))
            article.setViews(article.getViews() + 1);

        articleTitreLabel.setText(article.getTitre());
        statutBadge.setText(article.getStatut());
        statutBadge.getStyleClass().setAll(getBadgeStyle(article.getStatut()));
        dateLabel.setText(article.getDatePublication() != null ? "📅 " + article.getDatePublication() : "");
        organeLabel.setText(article.getOrgane() != null ? "🏥 " + article.getOrgane() : "");
        viewsLabel.setText("👁 " + article.getViews() + " vues");

        String tags = article.getTags();
        tagsLabel.setText(tags != null && !tags.isEmpty() ? "🏷 " + tags : "");
        tagsLabel.setVisible(tags != null && !tags.isEmpty());
        tagsLabel.setManaged(tags != null && !tags.isEmpty());

        // Image hero
        String imgPath = article.getImagePath();
        Image detailImg = (imgPath != null && !imgPath.isEmpty())
            ? ImageLoader.load(new File(imgPath)) : null;
        if (detailImg != null && !detailImg.isError()) {
            detailImageView.setImage(detailImg);
            imageContainer.setVisible(true);
            imageContainer.setManaged(true);
        } else {
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
        }

        MarkdownRenderer.render(article.getContenu(), contenuContainer);
        likesLabel.setText(article.getLikes() + " likes");

        applyLikeBtnState(serviceArticle.hasLiked(article.getId(), userId));

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
        showDetail(true);
    }

    private void applyLikeBtnState(boolean liked) {
        if (liked) {
            likeBtn.setText("❤  Aimé ✓");
            likeBtn.getStyleClass().setAll("btn-primary");
            likeBtn.setStyle("-fx-background-color: #FC8181; -fx-padding: 0 20; -fx-font-size: 13px;");
        } else {
            likeBtn.setText("❤  J'aime");
            likeBtn.getStyleClass().setAll("btn-outline");
            likeBtn.setStyle("-fx-padding: 0 20; -fx-font-size: 13px;");
        }
    }

    private void showDetail(boolean show) {
        gridView.setVisible(!show);
        gridView.setManaged(!show);
        detailView.setVisible(show);
        detailView.setManaged(show);
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

    // ─── Actions ──────────────────────────────────────────────

    @FXML void filterAll(ActionEvent event)  { currentPage = 0; showingAll = true;  loadArticles(); }
    @FXML void filterMine(ActionEvent event) { currentPage = 0; showingAll = false; loadArticles(); }

    @FXML void handleNewArticle(ActionEvent event) {
        ArticleFormController.setArticleToEdit(null);
        App.navigate("ArticleForm");
    }

    @FXML void handleEdit(ActionEvent event) {
        if (selectedArticle == null) return;
        ArticleFormController.setArticleToEdit(selectedArticle);
        App.navigate("ArticleForm");
    }

    @FXML void handleLike(ActionEvent event) {
        if (selectedArticle == null) return;
        int userId = UserSession.getInstance().getCurrentUser().getId();
        boolean nowLiked = serviceArticle.toggleLike(selectedArticle.getId(), userId);
        selectedArticle.setLikes(nowLiked ? selectedArticle.getLikes() + 1 : selectedArticle.getLikes() - 1);
        likesLabel.setText(selectedArticle.getLikes() + " likes");
        applyLikeBtnState(nowLiked);
    }

    @FXML void handlePublish(ActionEvent event) {
        if (selectedArticle == null) return;
        serviceArticle.updateStatut(selectedArticle.getId(), "PUBLIE");
        selectedArticle.setStatut("PUBLIE");
        selectArticle(selectedArticle);
    }

    @FXML void handleArchive(ActionEvent event) {
        if (selectedArticle == null) return;
        serviceArticle.updateStatut(selectedArticle.getId(), "ARCHIVE");
        selectedArticle.setStatut("ARCHIVE");
        selectArticle(selectedArticle);
    }

    @FXML void handleDelete(ActionEvent event) {
        if (selectedArticle == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer \"" + selectedArticle.getTitre() + "\" ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                serviceArticle.delete(selectedArticle.getId());
                selectedArticle = null;
                loadArticles();
                showDetail(false);
            }
        });
    }

    @FXML void handleAddComment(ActionEvent event) {
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

    @FXML void handleBackToGrid(ActionEvent event) {
        showDetail(false);
        renderGrid(currentArticles);
    }

    @FXML void handleBack(ActionEvent event) {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        App.navigate("MEDECIN".equals(user.getRole()) ? "DoctorDashboard" : "PatientDashboard");
    }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}
