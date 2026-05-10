package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Article;
import com.oncoreminder.models.Commentaire;
import com.oncoreminder.models.Message;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.services.ServiceArticle;
import com.oncoreminder.services.ServiceCommentaire;
import com.oncoreminder.services.ServiceMessage;
import com.oncoreminder.services.ServiceUtilisateur;
import com.oncoreminder.utils.ImageLoader;
import com.oncoreminder.utils.MarkdownRenderer;
import com.oncoreminder.utils.PdfExporter;
import com.oncoreminder.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.Popup;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.List;

public class PatientArticleListController {

    // Sidebar
    @FXML private Label patientNameLabel;

    // Grid view
    @FXML private VBox      gridView;
    @FXML private TextField searchField;
    @FXML private FlowPane  articleFlowPane;
    @FXML private HBox      paginationBar;

    // Detail view
    @FXML private VBox      detailView;
    @FXML private Label     articleTitreLabel;
    @FXML private Label     statutBadge;
    @FXML private Label     dateLabel;
    @FXML private Label     organeLabel;
    @FXML private Label     tagsLabel;
    @FXML private Label     viewsLabel;
    @FXML private Label     medecinLabel;
    @FXML private Label     medecinSidebarLabel;
    @FXML private VBox      contenuContainer;
    @FXML private StackPane imageContainer;
    @FXML private ImageView detailImageView;
    @FXML private Button    likeBtn;
    @FXML private Label  likesLabel;
    @FXML private VBox   commentsContainer;
    @FXML private TextArea commentField;
    @FXML private Button   emojiBtn;

    // Messagerie
    @FXML private VBox     msgSection;
    @FXML private VBox     msgContainer;
    @FXML private TextArea msgField;

    private final ServiceArticle     serviceArticle     = new ServiceArticle();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private final ServiceMessage     serviceMessage     = new ServiceMessage();

    private static final int PAGE_SIZE = 5;

    private Article       selectedArticle;
    private List<Article> currentArticles;
    private List<Article> displayedArticles;
    private int           currentPage    = 0;
    private Integer       currentMedecinId;

    private final ServiceUtilisateur serviceUtilisateur = new ServiceUtilisateur();

    @FXML
    public void initialize() {
        Utilisateur user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            patientNameLabel.setText(user.getPrenom() + " " + user.getNom());
            if (user.getMedecinId() != null) {
                Utilisateur dr = serviceUtilisateur.getById(user.getMedecinId());
                if (dr != null) {
                    medecinSidebarLabel.setText("🩺 Dr. " + dr.getPrenom() + " " + dr.getNom());
                    medecinSidebarLabel.setVisible(true);
                    medecinSidebarLabel.setManaged(true);
                }
            }
        }
        searchField.textProperty().addListener((obs, o, n) -> filterBySearch(n));
        loadArticles();
    }

    // ─── Chargement ──────────────────────────────────────────────────

    private void loadArticles() {
        currentArticles = serviceArticle.getPublished();
        renderGrid(currentArticles);
    }

    private void filterBySearch(String keyword) {
        currentPage = 0;
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
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 14px; -fx-padding: 20;");
            articleFlowPane.getChildren().add(empty);
        } else {
            for (Article a : articles.subList(from, to)) articleFlowPane.getChildren().add(buildCard(a));
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

    private VBox buildCard(Article article) {
        VBox card = new VBox(10);
        card.setPrefWidth(268);
        card.setMaxWidth(268);
        card.setPadding(new Insets(0));
        card.setCursor(Cursor.HAND);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E0D4F8;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(90,50,180,0.10), 10, 0, 0, 3);"
        );

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

        VBox inner = new VBox(10);
        inner.setPadding(new Insets(12, 16, 14, 16));
        card.getChildren().add(inner);

        // Organe + médecin badges
        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        if (article.getOrgane() != null && !article.getOrgane().isEmpty()) {
            Label organeBadge = new Label("🏥 " + article.getOrgane());
            organeBadge.setStyle(
                "-fx-background-color: #EEF2FF; -fx-text-fill: #5B35A5;" +
                "-fx-padding: 2 8; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: bold;"
            );
            badgeRow.getChildren().add(organeBadge);
        }
        if (article.getMedecinNom() != null && !article.getMedecinNom().isBlank()) {
            Label drBadge = new Label("🩺 Dr. " + article.getMedecinNom());
            drBadge.setStyle(
                "-fx-background-color: rgba(43,188,176,0.12); -fx-text-fill: #2BBCB0;" +
                "-fx-padding: 2 8; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: bold;"
            );
            badgeRow.getChildren().add(drBadge);
        }
        if (!badgeRow.getChildren().isEmpty()) inner.getChildren().add(badgeRow);

        // Titre
        Label titre = new Label(article.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #3A1D7A;");
        titre.setWrapText(true);
        titre.setMaxWidth(236);
        inner.getChildren().add(titre);

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
            inner.getChildren().add(excLabel);
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
            inner.getChildren().add(tagsRow);
        }

        // Séparateur visuel
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E9E3FF;");
        inner.getChildren().add(sep);

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
        inner.getChildren().add(footer);

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
        Utilisateur u = UserSession.getInstance().getCurrentUser();
        int userId = u.getId();

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

        // Messagerie avec le médecin
        currentMedecinId = u.getMedecinId();
        boolean hasMedecin = currentMedecinId != null;
        msgSection.setVisible(hasMedecin);
        msgSection.setManaged(hasMedecin);
        if (hasMedecin) loadMessages(article.getId(), userId, currentMedecinId);

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

    @FXML void handleExportPdf(ActionEvent event) {
        if (selectedArticle == null) return;
        Window window = likeBtn.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName(
            selectedArticle.getTitre().replaceAll("[^a-zA-Z0-9_\\- ]", "").trim() + ".pdf"
        );
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File dest = chooser.showSaveDialog(window);
        if (dest == null) return;
        try {
            PdfExporter.export(selectedArticle, dest.getAbsolutePath());
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "PDF exporté avec succès !");
            ok.setHeaderText(null);
            ok.setTitle("Export PDF");
            ok.showAndWait();
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'export : " + ex.getMessage());
            err.setHeaderText(null);
            err.showAndWait();
        }
    }

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

    // ─── Messagerie ──────────────────────────────────────────────────

    private void loadMessages(int articleId, int patientId, int medecinId) {
        msgContainer.getChildren().clear();
        List<Message> msgs = serviceMessage.getConversation(articleId, patientId, medecinId);
        if (msgs.isEmpty()) {
            Label empty = new Label("Aucun échange avec votre médecin pour cet article.");
            empty.setStyle("-fx-text-fill: #B0C4D8; -fx-font-size: 12px;");
            msgContainer.getChildren().add(empty);
        } else {
            for (Message m : msgs) msgContainer.getChildren().add(createMsgBubble(m, patientId));
            serviceMessage.markRead(articleId, patientId, medecinId);
        }
    }

    private HBox createMsgBubble(Message m, int myId) {
        boolean isMine = m.getExpediteurId() == myId;
        Label bubble = new Label(m.getContenu());
        bubble.setWrapText(true);
        bubble.setMaxWidth(340);
        bubble.setPadding(new Insets(8, 14, 8, 14));
        bubble.setStyle(
            "-fx-background-radius: 14; -fx-font-size: 13px;" +
            (isMine
                ? "-fx-background-color: #5B35A5; -fx-text-fill: white;"
                : "-fx-background-color: #EDE9F8; -fx-text-fill: #3A1D7A;")
        );
        Label timeLabel = new Label((isMine ? "Moi" : m.getExpediteurNom()) + "  " + m.getFormattedDate());
        timeLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px;");
        VBox bubbleBox = new VBox(3, bubble, timeLabel);
        bubbleBox.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        HBox row = new HBox(bubbleBox);
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));
        return row;
    }

    @FXML void handleSendMessage(ActionEvent event) {
        String text = msgField.getText().trim();
        if (text.isEmpty() || selectedArticle == null || currentMedecinId == null) return;
        Utilisateur u = UserSession.getInstance().getCurrentUser();
        serviceMessage.send(selectedArticle.getId(), u.getId(), currentMedecinId, text);
        msgField.clear();
        loadMessages(selectedArticle.getId(), u.getId(), currentMedecinId);
    }

    @FXML void handleDashboard(ActionEvent event)     { App.navigate("PatientDashboard"); }
    @FXML void handleRendezVous(ActionEvent event)    { PatientDashboardController.showEventsOnLoad = true; App.navigate("PatientDashboard"); }
    @FXML void handleReclamations(ActionEvent event)  { PatientDashboardController.showReclamationsOnLoad = true; App.navigate("PatientDashboard"); }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}
