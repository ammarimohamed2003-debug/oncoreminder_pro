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
        String[] alters = {
            "ALTER TABLE article ADD COLUMN medecin_id INT DEFAULT NULL",
            "ALTER TABLE article ADD COLUMN tags VARCHAR(500) DEFAULT NULL",
            "ALTER TABLE article ADD COLUMN views INT DEFAULT 0",
            "ALTER TABLE article ADD COLUMN icd_code VARCHAR(20) DEFAULT NULL"
        };
        for (String sql : alters) {
            try { cnx.createStatement().execute(sql); } catch (SQLException ignored) {}
        }
    }

    public void add(Article article) {
        if (!updateConnection()) return;
        String req = "INSERT INTO article (titre, contenu, statut, date_publication, organe, likes, medecin_id, tags, icd_code) VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)";
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
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ajout article: " + e.getMessage());
        }
    }

    public List<Article> getAll() {
        return query("SELECT * FROM article ORDER BY id DESC");
    }

    public List<Article> getPublished() {
        return query("SELECT * FROM article WHERE statut = 'PUBLIE' ORDER BY date_publication DESC");
    }

    public List<Article> getByMedecin(int medecinId) {
        List<Article> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            PreparedStatement pst = cnx.prepareStatement("SELECT * FROM article WHERE medecin_id = ? ORDER BY id DESC");
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
                "SELECT * FROM article WHERE (titre LIKE ? OR contenu LIKE ? OR organe LIKE ? OR tags LIKE ?) " +
                "AND statut != 'ARCHIVE' ORDER BY id DESC");
            for (int i = 1; i <= 4; i++) pst.setString(i, like);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    public void update(Article article) {
        if (!updateConnection()) return;
        String req = "UPDATE article SET titre=?, contenu=?, statut=?, organe=?, date_publication=?, tags=?, icd_code=? WHERE id=?";
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
            pst.setInt(8, article.getId());
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

    public void addLike(int id) {
        if (!updateConnection()) return;
        try {
            cnx.prepareStatement("UPDATE article SET likes=likes+1 WHERE id=" + id).executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public void incrementViews(int id) {
        if (!updateConnection()) return;
        try {
            cnx.prepareStatement("UPDATE article SET views=views+1 WHERE id=" + id).executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public void delete(int id) {
        if (!updateConnection()) return;
        try {
            cnx.prepareStatement("DELETE FROM article WHERE id=" + id).executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    private List<Article> query(String sql) {
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
        try { a.setIcdCode(rs.getString("icd_code")); } catch (SQLException ignored) {}
        return a;
    }
}
