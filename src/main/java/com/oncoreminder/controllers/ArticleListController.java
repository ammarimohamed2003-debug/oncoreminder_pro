package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Article;
import com.oncoreminder.models.Commentaire;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.services.ServiceArticleEvaluation;
import com.oncoreminder.services.ServiceCommentaire;
import com.oncoreminder.utils.ImageLoader;
import com.oncoreminder.utils.MarkdownRenderer;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ArticleListController {

    // ── Grid view ─────────────────────────────────────────────
    @FXML private VBox      gridView;
    @FXML private FlowPane  articleFlowPane;
    @FXML private HBox      filterBar;
    @FXML private HBox      paginationBar;
    @FXML private TextField searchField;
    @FXML private Button    newArticleBtn;
    @FXML private Label     userNameLabel;

    // ── Statut filters (médecin) ──────────────────────────────
    @FXML private HBox   statutFilterBar;
    @FXML private Button btnStatutTous;
    @FXML private Button btnStatutPublie;
    @FXML private Button btnStatutBrouillon;
    @FXML private Button btnStatutArchive;

    // ── Detail view ───────────────────────────────────────────
    @FXML private VBox      detailView;
    @FXML private Label     articleTitreLabel;
    @FXML private Label     statutBadge;
    @FXML private Label     dateLabel;
    @FXML private Label     organeLabel;
    @FXML private Label     tagsLabel;
    @FXML private Label     viewsLabel;
    @FXML private Label     medecinLabel;
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
    @FXML private Button   emojiBtn;

    // Évaluation
    @FXML private VBox  evalSection;
    @FXML private Label evalTitre;
    @FXML private HBox  evalStarsBox;
    @FXML private Label evalMessage;
    @FXML private VBox  evalListBox;

    private final ServiceArticle           serviceArticle     = new ServiceArticle();
    private final ServiceArticleEvaluation serviceEval        = new ServiceArticleEvaluation();
    private final ServiceCommentaire       serviceCommentaire = new ServiceCommentaire();

    private static final int PAGE_SIZE = 5;

    private Article       selectedArticle;
    private boolean       showingAll = true;
    private String        currentStatutFilter  = null;
    private List<Article> currentArticles;
    private List<Article> displayedArticles;
    private int           currentPage          = 0;

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
        statutFilterBar.setVisible(isMedecin);
        statutFilterBar.setManaged(isMedecin);

        searchField.textProperty().addListener((obs, o, n) -> filterBySearch(n));

        loadArticles();
    }

    private void loadArticles() {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        List<Article> all;
        if ("MEDECIN".equals(user.getRole())) {
            all = showingAll ? serviceArticle.getAll() : serviceArticle.getByMedecin(user.getId());
            if (currentStatutFilter != null)
                all = all.stream().filter(a -> currentStatutFilter.equals(a.getStatut())).collect(Collectors.toList());
        } else {
            all = serviceArticle.getPublished();
        }
        currentArticles = all;
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
        paginationBar.setAlignment(Pos.CENTER);
        paginationBar.setSpacing(7);

        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (totalPages <= 1) return;

        // Sliding window of max 9 dots
        int maxDots = 9;
        int start = Math.max(0, Math.min(currentPage - maxDots / 2, totalPages - maxDots));
        int end   = Math.min(totalPages, start + maxDots);
        start = Math.max(0, end - maxDots);

        if (currentPage > 0) {
            Button prev = carouselArrow("‹");
            prev.setOnAction(e -> { currentPage--; renderGrid(displayedArticles); });
            paginationBar.getChildren().add(prev);
        }

        for (int i = start; i < end; i++) {
            final int page = i;
            boolean active = i == currentPage;
            Button dot = new Button();
            double size = active ? 13 : 9;
            dot.setPrefSize(size, size);
            dot.setMinSize(size, size);
            dot.setMaxSize(size, size);
            dot.setStyle(
                "-fx-background-radius: 50; -fx-border-width: 0; -fx-padding: 0;" +
                "-fx-background-color: " + (active ? "#5B35A5" : "#D4C4EE") + ";" +
                (active ? "" : "-fx-cursor: hand;")
            );
            if (!active) dot.setOnAction(e -> { currentPage = page; renderGrid(displayedArticles); });
            paginationBar.getChildren().add(dot);
        }

        if (currentPage < totalPages - 1) {
            Button next = carouselArrow("›");
            next.setOnAction(e -> { currentPage++; renderGrid(displayedArticles); });
            paginationBar.getChildren().add(next);
        }
    }

    private Button carouselArrow(String label) {
        Button btn = new Button(label);
        btn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #5B35A5;" +
            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-border-width: 0;" +
            "-fx-cursor: hand; -fx-padding: 0 2;"
        );
        return btn;
    }

    private static final int CARD_W = 340;

    private VBox buildCard(Article article) {
        VBox card = new VBox(8);
        card.setPrefWidth(CARD_W);
        card.setMaxWidth(CARD_W);
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
                imgView.setFitWidth(CARD_W);
                imgView.setFitHeight(158);
                imgView.setPreserveRatio(false);
                imgView.setSmooth(true);
                Rectangle clip = new Rectangle(CARD_W, 158);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                imgView.setClip(clip);
                card.getChildren().add(imgView);
            }
        }

        VBox inner = new VBox(8);
        inner.setPadding(new Insets(12, 16, 14, 16));
        card.getChildren().add(inner);

        // Statut — ligne dédiée, toujours visible
        HBox statutRow = new HBox();
        statutRow.setAlignment(Pos.CENTER_RIGHT);
        Label badge = new Label(statutLabel(article.getStatut()));
        badge.getStyleClass().add(getBadgeStyle(article.getStatut()));
        statutRow.getChildren().add(badge);
        inner.getChildren().add(statutRow);

        // Organe + médecin badges
        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        if (article.getOrgane() != null && !article.getOrgane().isEmpty()) {
            Label organeBadge = new Label("🏥 " + article.getOrgane());
            organeBadge.setStyle(
                "-fx-background-color: #EEF2FF; -fx-text-fill: #5B35A5;" +
                "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px;"
            );
            badgeRow.getChildren().add(organeBadge);
        }
        if (article.getMedecinNom() != null && !article.getMedecinNom().isBlank()) {
            Label drBadge = new Label("🩺 Dr. " + article.getMedecinNom());
            drBadge.setStyle(
                "-fx-background-color: rgba(43,188,176,0.12); -fx-text-fill: #2BBCB0;" +
                "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;"
            );
            badgeRow.getChildren().add(drBadge);
        }
        if (!badgeRow.getChildren().isEmpty()) inner.getChildren().add(badgeRow);

        // Titre — toute la largeur
        Label titre = new Label(article.getTitre());
        titre.setWrapText(true);
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2D1B69;");
        inner.getChildren().add(titre);

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

        // Footer: date, likes, views, comments
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        if (article.getDatePublication() != null) {
            Label date = new Label("📅 " + article.getDatePublication());
            date.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            footer.getChildren().add(date);
        }
        Region footerSpacer = new Region(); HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        footer.getChildren().add(footerSpacer);
        Label likes = new Label("❤ " + article.getLikes());
        likes.setStyle("-fx-text-fill: #FC8181; -fx-font-size: 11px;");
        footer.getChildren().add(likes);
        if (article.getViews() > 0) {
            Label views = new Label("👁 " + article.getViews());
            views.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            footer.getChildren().add(views);
        }
        int nbComments = serviceCommentaire.countByArticle(article.getId());
        Label comments = new Label("💬 " + nbComments);
        comments.setStyle("-fx-text-fill: #5B35A5; -fx-font-size: 11px;");
        footer.getChildren().add(comments);
        int evalCount = serviceEval.getCount(article.getId());
        if (evalCount > 0) {
            int pct = (int) Math.round(serviceEval.getAverage(article.getId()) / 5.0 * 100);
            Label evalLbl = new Label("⭐ " + pct + "% (" + evalCount + ")");
            evalLbl.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px; -fx-font-weight: bold;");
            footer.getChildren().add(evalLbl);
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

    private String statutLabel(String statut) {
        if (statut == null) return "";
        return switch (statut) {
            case "PUBLIE"    -> "✅ Publié";
            case "BROUILLON" -> "📝 Brouillon";
            case "ARCHIVE"   -> "📦 Archivé";
            default          -> statut;
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

        String nom = article.getMedecinNom();
        boolean hasNom = nom != null && !nom.isBlank();
        medecinLabel.setText(hasNom ? "🩺 Dr. " + nom : "");
        medecinLabel.setVisible(hasNom);
        medecinLabel.setManaged(hasNom);

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

        boolean isOwner = "MEDECIN".equals(user.getRole()) && article.getMedecinId() == user.getId();
        medecinActions.setVisible(isOwner);
        medecinActions.setManaged(isOwner);

        if (isOwner) loadEvalAggregate(article.getId());
        else         loadEvalUser(article, userId);
        if (isOwner) {
            publishBtn.setVisible(!"PUBLIE".equals(article.getStatut()));
            publishBtn.setManaged(!"PUBLIE".equals(article.getStatut()));
            archiveBtn.setVisible(!"ARCHIVE".equals(article.getStatut()));
            archiveBtn.setManaged(!"ARCHIVE".equals(article.getStatut()));
        }

        loadComments();
        showDetail(true);
    }

    // ─── Évaluation ──────────────────────────────────────────────────

    private void loadEvalAggregate(int articleId) {
        evalSection.setVisible(true);
        evalSection.setManaged(true);
        evalStarsBox.getChildren().clear();
        evalListBox.getChildren().clear();
        evalListBox.setVisible(false); evalListBox.setManaged(false);
        evalMessage.setVisible(false); evalMessage.setManaged(false);

        evalTitre.setText("📊  Notes reçues sur cet article");

        int count = serviceEval.getCount(articleId);
        if (count == 0) {
            evalMessage.setText("Aucune évaluation pour le moment.");
            evalMessage.setVisible(true); evalMessage.setManaged(true);
            return;
        }

        double avg   = serviceEval.getAverage(articleId);
        int    pct   = (int) Math.round(avg / 5.0 * 100);
        int    rounded = (int) Math.round(avg);
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= rounded ? "★" : "☆");
            star.setStyle("-fx-font-size: 24px; -fx-text-fill: " + (i <= rounded ? "#F6AD55;" : "#CBD5E0;"));
            evalStarsBox.getChildren().add(star);
        }
        evalMessage.setText(String.format("%.1f / 5  ·  %d%%  ·  %d évaluation(s)", avg, pct, count));
        evalMessage.setVisible(true); evalMessage.setManaged(true);

        // Liste détaillée des évaluateurs
        var evals = serviceEval.getEvaluations(articleId);
        if (!evals.isEmpty()) {
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #EDE9F8;");
            evalListBox.getChildren().add(sep);
            for (String[] e : evals) {
                int    n    = Integer.parseInt(e[1]);
                String stars = "★".repeat(n) + "☆".repeat(5 - n);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label nomLbl = new Label("👤 " + e[0]);
                nomLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2D1B69; -fx-font-weight: bold;");
                Label starLbl = new Label(stars);
                starLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #F6AD55;");
                Label noteLbl = new Label("(" + n + "/5)");
                noteLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
                row.getChildren().addAll(nomLbl, starLbl, noteLbl);
                evalListBox.getChildren().add(row);
            }
            evalListBox.setVisible(true); evalListBox.setManaged(true);
        }
    }

    private void loadEvalUser(Article article, int userId) {
        evalSection.setVisible(true);
        evalSection.setManaged(true);
        evalStarsBox.getChildren().clear();
        evalMessage.setVisible(false); evalMessage.setManaged(false);

        Integer existing = serviceEval.getUserNote(article.getId(), userId);
        if (existing != null) {
            evalTitre.setText("⭐  Votre évaluation");
            afficherEtoilesLecture(existing);
            evalMessage.setText(noteLabel(existing));
            evalMessage.setVisible(true); evalMessage.setManaged(true);
        } else {
            evalTitre.setText("⭐  Évaluez cet article");
            afficherEtoilesCliquables(article, userId);
        }
    }

    private void afficherEtoilesLecture(int note) {
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= note ? "★" : "☆");
            star.setStyle("-fx-font-size: 26px; -fx-text-fill: " + (i <= note ? "#F6AD55;" : "#CBD5E0;"));
            evalStarsBox.getChildren().add(star);
        }
    }

    private void afficherEtoilesCliquables(Article article, int userId) {
        Button[] stars = new Button[5];
        for (int i = 1; i <= 5; i++) {
            final int val = i;
            Button star = new Button("☆");
            star.setStyle("-fx-background-color: transparent; -fx-font-size: 28px; " +
                          "-fx-text-fill: #CBD5E0; -fx-cursor: hand; -fx-padding: 0 2;");
            star.setOnMouseEntered(ev -> survoleEtoiles(stars, val));
            star.setOnMouseExited(ev  -> resetEtoiles(stars));
            star.setOnAction(ev -> {
                serviceEval.saveNote(article.getId(), userId, val);
                evalStarsBox.getChildren().clear();
                afficherEtoilesLecture(val);
                evalTitre.setText("⭐  Votre évaluation");
                evalMessage.setText(noteLabel(val));
                evalMessage.setVisible(true); evalMessage.setManaged(true);
            });
            stars[i - 1] = star;
            evalStarsBox.getChildren().add(star);
        }
    }

    private void survoleEtoiles(Button[] stars, int jusqua) {
        for (int i = 0; i < 5; i++)
            stars[i].setStyle("-fx-background-color: transparent; -fx-font-size: 28px; " +
                "-fx-text-fill: " + (i < jusqua ? "#F6AD55;" : "#CBD5E0;") + " -fx-cursor: hand; -fx-padding: 0 2;");
    }

    private void resetEtoiles(Button[] stars) {
        for (Button s : stars)
            s.setStyle("-fx-background-color: transparent; -fx-font-size: 28px; " +
                       "-fx-text-fill: #CBD5E0; -fx-cursor: hand; -fx-padding: 0 2;");
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

        HBox likeRow = new HBox(8);
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

        // ── Threaded replies ──
        VBox repliesBox = new VBox(6);
        repliesBox.setPadding(new Insets(4, 0, 0, 24));
        List<Commentaire> replies = serviceCommentaire.getReplies(c.getId());
        repliesBox.setVisible(!replies.isEmpty());
        repliesBox.setManaged(!replies.isEmpty());

        TextArea replyField = new TextArea();
        replyField.setPromptText("Votre réponse...");
        replyField.setPrefHeight(52);
        replyField.setWrapText(true);
        replyField.getStyleClass().add("field-input");

        Button sendReplyBtn = new Button("Envoyer →");
        sendReplyBtn.getStyleClass().add("btn-primary");
        sendReplyBtn.setStyle("-fx-padding: 0 12; -fx-font-size: 12px;");

        HBox replyInputRow = new HBox(8, replyField, sendReplyBtn);
        replyInputRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(replyField, Priority.ALWAYS);

        VBox replyInputBox = new VBox(6, replyInputRow);
        replyInputBox.setPadding(new Insets(6, 0, 0, 24));
        replyInputBox.setVisible(false);
        replyInputBox.setManaged(false);

        Utilisateur currentUser = UserSession.getInstance().getCurrentUser();
        String replyAuteur = "MEDECIN".equals(currentUser.getRole())
            ? "Dr. " + currentUser.getNom()
            : currentUser.getPrenom() + " " + currentUser.getNom();

        // populate replies now that replyField/replyInputBox exist
        for (Commentaire r : replies) repliesBox.getChildren().add(createReplyCard(r, replyField, replyInputBox));

        sendReplyBtn.setOnAction(ev -> {
            String text = replyField.getText().trim();
            if (text.isEmpty()) return;
            Commentaire reply = new Commentaire(c.getArticleId(), text, replyAuteur);
            reply.setParentId(c.getId());
            serviceCommentaire.add(reply);
            replyField.clear();
            replyInputBox.setVisible(false);
            replyInputBox.setManaged(false);
            repliesBox.getChildren().clear();
            List<Commentaire> updated = serviceCommentaire.getReplies(c.getId());
            for (Commentaire r : updated) repliesBox.getChildren().add(createReplyCard(r, replyField, replyInputBox));
            repliesBox.setVisible(true);
            repliesBox.setManaged(true);
        });

        Button replyBtn = new Button("💬 Répondre");
        replyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5B35A5; " +
            "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0 4; -fx-border-width: 0;");
        replyBtn.setOnAction(ev -> {
            boolean show = !replyInputBox.isVisible();
            replyInputBox.setVisible(show);
            replyInputBox.setManaged(show);
            if (show) replyField.requestFocus();
        });

        likeRow.getChildren().addAll(cLikeBtn, likeCount, replyBtn);
        card.getChildren().addAll(header, contenu, likeRow, repliesBox, replyInputBox);
        return card;
    }

    private VBox createReplyCard(Commentaire r, TextArea parentReplyField, VBox parentReplyInputBox) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #EEF4FF; -fx-background-radius: 6; " +
            "-fx-border-color: #B8D4F0; -fx-border-radius: 6; -fx-border-width: 1;");
        card.setPadding(new Insets(7, 10, 7, 10));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label auteur = new Label("↳ " + r.getAuteur());
        auteur.setStyle("-fx-font-weight: bold; -fx-text-fill: #2BBCB0; -fx-font-size: 11px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label date = new Label(r.getFormattedDate());
        date.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 10px;");
        header.getChildren().addAll(auteur, sp, date);

        Label contenu = new Label(r.getContenu());
        contenu.setWrapText(true);
        contenu.setStyle("-fx-text-fill: #2D3748; -fx-font-size: 12px;");

        Button replyBtn = new Button("💬 Répondre");
        replyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2BBCB0; " +
            "-fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 0 2; -fx-border-width: 0;");
        replyBtn.setOnAction(ev -> {
            parentReplyInputBox.setVisible(true);
            parentReplyInputBox.setManaged(true);
            parentReplyField.setText("@" + r.getAuteur() + " ");
            parentReplyField.requestFocus();
            parentReplyField.positionCaret(parentReplyField.getText().length());
        });

        card.getChildren().addAll(header, contenu, replyBtn);
        return card;
    }

    // ─── Actions ──────────────────────────────────────────────

    @FXML void filterAll(ActionEvent event)  { currentPage = 0; showingAll = true;  loadArticles(); }
    @FXML void filterMine(ActionEvent event) { currentPage = 0; showingAll = false; loadArticles(); }

    @FXML void filterStatutAll(ActionEvent e)       { currentPage = 0; currentStatutFilter = null;       updateStatutButtons(); loadArticles(); }
    @FXML void filterStatutPublie(ActionEvent e)    { currentPage = 0; currentStatutFilter = "PUBLIE";   updateStatutButtons(); loadArticles(); }
    @FXML void filterStatutBrouillon(ActionEvent e) { currentPage = 0; currentStatutFilter = "BROUILLON"; updateStatutButtons(); loadArticles(); }
    @FXML void filterStatutArchive(ActionEvent e)   { currentPage = 0; currentStatutFilter = "ARCHIVE";  updateStatutButtons(); loadArticles(); }

    private void updateStatutButtons() {
        String active   = "-fx-background-color: #5B35A5; -fx-text-fill: white; -fx-padding: 3 12; -fx-background-radius: 7; -fx-font-size: 10px; -fx-cursor: hand;";
        String publie   = "PUBLIE".equals(currentStatutFilter)
            ? active : "-fx-background-color: #C6F6D5; -fx-text-fill: #276749; -fx-padding: 3 10; -fx-background-radius: 7; -fx-font-size: 10px; -fx-cursor: hand;";
        String brouillon= "BROUILLON".equals(currentStatutFilter)
            ? active : "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 3 10; -fx-background-radius: 7; -fx-font-size: 10px; -fx-cursor: hand;";
        String archive  = "ARCHIVE".equals(currentStatutFilter)
            ? active : "-fx-background-color: #E2E8F0; -fx-text-fill: #4A5568; -fx-padding: 3 10; -fx-background-radius: 7; -fx-font-size: 10px; -fx-cursor: hand;";
        String tous     = currentStatutFilter == null
            ? active : "-fx-background-color: #EDE8FF; -fx-text-fill: #5B35A5; -fx-padding: 3 12; -fx-background-radius: 7; -fx-font-size: 10px; -fx-cursor: hand;";
        btnStatutTous.setStyle(tous);
        btnStatutPublie.setStyle(publie);
        btnStatutBrouillon.setStyle(brouillon);
        btnStatutArchive.setStyle(archive);
    }

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

    @FXML void handleEmojiPicker(ActionEvent event) {
        showEmojiPopup(commentField, emojiBtn);
    }

    private void showEmojiPopup(TextArea target, Node anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        String[][] groups = {
            {"😀","😊","😂","🥰","😍","😢","😮","😡","🤔","😎","🙂","😉","😋","🤗","😴","🤩","😏","🥲","😬","🤭","🤫","🫠"},
            {"💊","🏥","🩺","🩻","💉","🧬","🫀","🫁","🧪","🩹","💪","🧠","🦷","👁","🫂","🩸","🌡","🔬","🦠","💆","🛌","🏃"},
            {"❤️","💙","💚","💛","🔥","⭐","✅","❌","⚠️","👍","👎","🙏","💯","🎉","✨","🌟","💥","🌈","🏅","🥇","🌺","🍀"},
            {"📝","🔍","💬","📌","💡","📊","🏆","📅","📋","🔐","🌐","💼","📱","💻","📤","📥","✉️","📞","📖","⏰","🗓","🔑"}
        };
        String[] tabLabels = {"😊 Humeur", "🩺 Médical", "❤️ Général", "📝 Pro"};

        // Grilles
        FlowPane[] grids = new FlowPane[groups.length];
        for (int g = 0; g < groups.length; g++) {
            FlowPane grid = new FlowPane(4, 4);
            grid.setPadding(new Insets(12));
            grid.setPrefWidth(350);
            for (String emoji : groups[g]) {
                Button btn = new Button(emoji);
                btn.setStyle("-fx-font-size: 20px; -fx-min-width: 40; -fx-pref-height: 40;"
                    + "-fx-border-width: 0; -fx-cursor: hand; -fx-background-radius: 8;"
                    + "-fx-background-color: transparent;");
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 20px; -fx-min-width: 40;"
                    + "-fx-pref-height: 40; -fx-border-width: 0; -fx-cursor: hand;"
                    + "-fx-background-radius: 8; -fx-background-color: #EDE9F8;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 20px; -fx-min-width: 40;"
                    + "-fx-pref-height: 40; -fx-border-width: 0; -fx-cursor: hand;"
                    + "-fx-background-radius: 8; -fx-background-color: transparent;"));
                final String em = emoji;
                btn.setOnAction(e -> {
                    int pos = target.getCaretPosition();
                    target.insertText(pos, em);
                    target.requestFocus();
                    target.positionCaret(pos + em.length());
                    popup.hide();
                });
                grid.getChildren().add(btn);
            }
            grids[g] = grid;
        }

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 14, 10, 16));
        header.setStyle("-fx-background-color: linear-gradient(to right, #5B35A5, #2BBCB0);"
            + "-fx-background-radius: 14 14 0 0;");
        Label title = new Label("Choisir un émoji");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: white;"
            + "-fx-background-radius: 20; -fx-border-width: 0; -fx-padding: 2 8;"
            + "-fx-cursor: hand; -fx-font-size: 11px;");
        closeBtn.setOnAction(e -> popup.hide());
        header.getChildren().addAll(title, sp, closeBtn);

        // Onglets
        HBox tabBar = new HBox(0);
        tabBar.setStyle("-fx-background-color: #F8F5FF;"
            + "-fx-border-color: transparent transparent #EDE9F8 transparent; -fx-border-width: 1;");
        StackPane gridContainer = new StackPane(grids[0]);
        gridContainer.setMinHeight(160);

        Button[] tabBtns = new Button[groups.length];
        for (int g = 0; g < groups.length; g++) {
            final int idx = g;
            Button tab = new Button(tabLabels[g]);
            tab.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tab, Priority.ALWAYS);
            String activeStyle = "-fx-background-color: white; -fx-border-color: transparent transparent #5B35A5 transparent;"
                + "-fx-border-width: 0 0 2.5 0; -fx-padding: 8 4; -fx-cursor: hand; -fx-background-radius: 0;"
                + "-fx-text-fill: #5B35A5; -fx-font-weight: bold; -fx-font-size: 11px;";
            String inactiveStyle = "-fx-background-color: transparent; -fx-border-width: 0;"
                + "-fx-padding: 8 4; -fx-cursor: hand; -fx-background-radius: 0;"
                + "-fx-text-fill: #9CA3AF; -fx-font-size: 11px;";
            tab.setStyle(g == 0 ? activeStyle : inactiveStyle);
            tab.setOnAction(e -> {
                gridContainer.getChildren().setAll(grids[idx]);
                for (int i = 0; i < tabBtns.length; i++)
                    tabBtns[i].setStyle(i == idx ? activeStyle : inactiveStyle);
            });
            tabBtns[g] = tab;
            tabBar.getChildren().add(tab);
        }

        // Assemblage
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
            + "-fx-border-color: #E2D9F8; -fx-border-width: 1; -fx-border-radius: 14;"
            + "-fx-effect: dropshadow(gaussian, rgba(90,53,165,0.25), 20, 0, 0, 6);");
        container.getChildren().addAll(header, tabBar, gridContainer);
        popup.getContent().add(container);

        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b != null)
            popup.show(anchor.getScene().getWindow(), b.getMaxX() - 350, b.getMinY() - 340);
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

    @FXML void handleRendezVous(ActionEvent event) { DoctorDashboardController.showEventsOnLoad = true; App.navigate("DoctorDashboard"); }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}
