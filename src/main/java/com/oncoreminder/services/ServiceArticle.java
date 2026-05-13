package com.oncoreminder.services;

import com.oncoreminder.models.Article;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceArticle {

    private Connection cnx;

    public ServiceArticle() {
        updateConnection();
        ensureColumns();
    }

    private boolean updateConnection() {
        this.cnx = MyDataBase.getInstance().getCnx();
        return this.cnx != null;
    }

    private void ensureColumns() {
        String[] stmts = {
            "ALTER TABLE article ADD COLUMN medecin_id INT DEFAULT NULL",
            "ALTER TABLE article ADD COLUMN tags VARCHAR(500) DEFAULT NULL",
            "ALTER TABLE article ADD COLUMN views INT DEFAULT 0",
            "ALTER TABLE article ADD COLUMN icd_code VARCHAR(20) DEFAULT NULL",
            "CREATE TABLE IF NOT EXISTS article_likes (" +
                "user_id INT NOT NULL, article_id INT NOT NULL, " +
                "PRIMARY KEY (user_id, article_id))",
            "CREATE TABLE IF NOT EXISTS article_views (" +
                "user_id INT NOT NULL, article_id INT NOT NULL, " +
                "PRIMARY KEY (user_id, article_id))",
            "ALTER TABLE article ADD COLUMN image_path VARCHAR(500) DEFAULT NULL"
        };
        for (String sql : stmts) {
            try { cnx.createStatement().execute(sql); } catch (SQLException ignored) {}
        }
    }

    public void add(Article article) {
        if (!updateConnection()) return;
        String req = "INSERT INTO article (titre, contenu, statut, date_publication, organe, likes, medecin_id, tags, icd_code, image_path) VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, article.getTitre());
            pst.setString(2, article.getContenu());
            pst.setString(3, article.getStatut());
            pst.setObject(4, "PUBLIE".equals(article.getStatut()) ? Date.valueOf(LocalDate.now()) : null);
            pst.setString(5, article.getOrgane());
            pst.setInt(6, article.getMedecinId());
            pst.setString(7, article.getTags());
            pst.setString(8, article.getIcdCode());
            pst.setString(9, article.getImagePath());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ajout article: " + e.getMessage());
        }
    }

    private static final String JOIN_SQL =
        "SELECT a.*, CONCAT(u.prenom, ' ', u.nom) AS medecin_nom " +
        "FROM article a LEFT JOIN utilisateur u ON a.medecin_id = u.id ";

    public List<Article> getAll() {
        return queryJoin(JOIN_SQL + "ORDER BY a.id DESC");
    }

    public List<Article> getPublished() {
        return queryJoin(JOIN_SQL + "WHERE a.statut = 'PUBLIE' ORDER BY a.date_publication DESC");
    }

    public List<Article> getByMedecin(int medecinId) {
        List<Article> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                JOIN_SQL + "WHERE a.medecin_id = ? ORDER BY a.id DESC");
            pst.setInt(1, medecinId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    public List<Article> search(String keyword) {
        List<Article> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            String like = "%" + keyword + "%";
            PreparedStatement pst = cnx.prepareStatement(
                JOIN_SQL + "WHERE a.statut != 'ARCHIVE' AND (" +
                "  a.titre   LIKE ? OR " +
                "  a.contenu LIKE ? OR " +
                "  a.organe  LIKE ? OR " +
                "  a.tags    LIKE ?" +
                ") ORDER BY a.id DESC");
            for (int i = 1; i <= 4; i++) pst.setString(i, like);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    public void update(Article article) {
        if (!updateConnection()) return;
        String req = "UPDATE article SET titre=?, contenu=?, statut=?, organe=?, date_publication=?, tags=?, icd_code=?, image_path=? WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, article.getTitre());
            pst.setString(2, article.getContenu());
            pst.setString(3, article.getStatut());
            pst.setString(4, article.getOrgane());
            if ("PUBLIE".equals(article.getStatut()) && article.getDatePublication() == null)
                pst.setObject(5, Date.valueOf(LocalDate.now()));
            else if (article.getDatePublication() != null)
                pst.setObject(5, Date.valueOf(article.getDatePublication()));
            else pst.setNull(5, Types.DATE);
            pst.setString(6, article.getTags());
            pst.setString(7, article.getIcdCode());
            pst.setString(8, article.getImagePath());
            pst.setInt(9, article.getId());
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur update: " + e.getMessage()); }
    }

    public void updateStatut(int id, String statut) {
        if (!updateConnection()) return;
        String req = "PUBLIE".equals(statut)
            ? "UPDATE article SET statut=?, date_publication=CURDATE() WHERE id=?"
            : "UPDATE article SET statut=? WHERE id=?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, statut); pst.setInt(2, id);
            pst.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    /** Toggle like — retourne true si l'article est maintenant aimé, false si le like est retiré. */
    public boolean toggleLike(int articleId, int userId) {
        if (!updateConnection()) return false;
        try {
            if (hasLiked(articleId, userId)) {
                cnx.prepareStatement(
                    "DELETE FROM article_likes WHERE user_id=" + userId + " AND article_id=" + articleId
                ).executeUpdate();
                cnx.prepareStatement(
                    "UPDATE article SET likes=GREATEST(likes-1,0) WHERE id=" + articleId
                ).executeUpdate();
                return false;
            } else {
                cnx.prepareStatement(
                    "INSERT INTO article_likes(user_id,article_id) VALUES(" + userId + "," + articleId + ")"
                ).executeUpdate();
                cnx.prepareStatement(
                    "UPDATE article SET likes=likes+1 WHERE id=" + articleId
                ).executeUpdate();
                return true;
            }
        } catch (SQLException e) { System.out.println(e.getMessage()); return false; }
    }

    public boolean hasLiked(int articleId, int userId) {
        if (!updateConnection()) return false;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT 1 FROM article_likes WHERE user_id=? AND article_id=?");
            pst.setInt(1, userId); pst.setInt(2, articleId);
            return pst.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    /** Incrémente les vues seulement si l'utilisateur n'a jamais vu cet article. */
    public boolean incrementViewIfNew(int articleId, int userId) {
        if (!updateConnection()) return false;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT 1 FROM article_views WHERE user_id=? AND article_id=?");
            pst.setInt(1, userId); pst.setInt(2, articleId);
            if (pst.executeQuery().next()) return false;
            cnx.prepareStatement(
                "INSERT INTO article_views(user_id,article_id) VALUES(" + userId + "," + articleId + ")"
            ).executeUpdate();
            cnx.prepareStatement(
                "UPDATE article SET views=views+1 WHERE id=" + articleId
            ).executeUpdate();
            return true;
        } catch (SQLException e) { System.out.println(e.getMessage()); return false; }
    }

    /** @deprecated Utiliser toggleLike(articleId, userId) */
    public void addLike(int id) {
        if (!updateConnection()) return;
        try { cnx.prepareStatement("UPDATE article SET likes=likes+1 WHERE id=" + id).executeUpdate(); }
        catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    /** @deprecated Utiliser incrementViewIfNew(articleId, userId) */
    public void incrementViews(int id) {
        if (!updateConnection()) return;
        try { cnx.prepareStatement("UPDATE article SET views=views+1 WHERE id=" + id).executeUpdate(); }
        catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public void delete(int id) {
        if (!updateConnection()) return;
        try {
            cnx.prepareStatement("DELETE FROM article WHERE id=" + id).executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    private List<Article> queryJoin(String sql) {
        List<Article> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    private Article mapRow(ResultSet rs) throws SQLException {
        Article a = new Article();
        a.setId(rs.getInt("id"));
        a.setTitre(rs.getString("titre"));
        a.setContenu(rs.getString("contenu"));
        a.setStatut(rs.getString("statut"));
        Date d = rs.getDate("date_publication");
        if (d != null) a.setDatePublication(d.toLocalDate());
        a.setCancerId(rs.getObject("cancer_id", Integer.class));
        a.setOrgane(rs.getString("organe"));
        a.setLikes(rs.getInt("likes"));
        try { a.setMedecinId(rs.getInt("medecin_id")); } catch (SQLException ignored) {}
        try { a.setTags(rs.getString("tags")); }     catch (SQLException ignored) {}
        try { a.setViews(rs.getInt("views")); }      catch (SQLException ignored) {}
        try { a.setIcdCode(rs.getString("icd_code")); }   catch (SQLException ignored) {}
        try { a.setImagePath(rs.getString("image_path")); } catch (SQLException ignored) {}
        try { a.setMedecinNom(rs.getString("medecin_nom")); } catch (SQLException ignored) {}
        return a;
    }
}
