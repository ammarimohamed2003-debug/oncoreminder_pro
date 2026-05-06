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
        ensureMedecinIdColumn();
    }

    private boolean updateConnection() {
        this.cnx = MyDataBase.getInstance().getCnx();
        if (this.cnx == null) {
            System.err.println("ServiceArticle: Impossible d'établir une connexion.");
            return false;
        }
        return true;
    }

    private void ensureMedecinIdColumn() {
        try {
            cnx.createStatement().execute("ALTER TABLE article ADD COLUMN medecin_id INT DEFAULT NULL");
        } catch (SQLException ignored) {
            // Column already exists
        }
    }

    public void add(Article article) {
        if (!updateConnection()) return;
        String req = "INSERT INTO article (titre, contenu, statut, date_publication, organe, likes, medecin_id) VALUES (?, ?, ?, ?, ?, 0, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, article.getTitre());
            pst.setString(2, article.getContenu());
            pst.setString(3, article.getStatut());
            pst.setObject(4, "PUBLIE".equals(article.getStatut()) ? Date.valueOf(LocalDate.now()) : null);
            pst.setString(5, article.getOrgane());
            pst.setInt(6, article.getMedecinId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ajout article: " + e.getMessage());
        }
    }

    public List<Article> getAll() {
        List<Article> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM article ORDER BY id DESC");
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<Article> getPublished() {
        List<Article> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM article WHERE statut = 'PUBLIE' ORDER BY date_publication DESC");
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<Article> getByMedecin(int medecinId) {
        List<Article> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM article WHERE medecin_id = ? ORDER BY id DESC");
            pst.setInt(1, medecinId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public void update(Article article) {
        if (!updateConnection()) return;
        String req = "UPDATE article SET titre = ?, contenu = ?, statut = ?, organe = ?, date_publication = ? WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, article.getTitre());
            pst.setString(2, article.getContenu());
            pst.setString(3, article.getStatut());
            pst.setString(4, article.getOrgane());
            if ("PUBLIE".equals(article.getStatut()) && article.getDatePublication() == null) {
                pst.setObject(5, Date.valueOf(LocalDate.now()));
            } else if (article.getDatePublication() != null) {
                pst.setObject(5, Date.valueOf(article.getDatePublication()));
            } else {
                pst.setNull(5, Types.DATE);
            }
            pst.setInt(6, article.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur update article: " + e.getMessage());
        }
    }

    public void updateStatut(int id, String statut) {
        if (!updateConnection()) return;
        String req = "PUBLIE".equals(statut)
            ? "UPDATE article SET statut = ?, date_publication = CURDATE() WHERE id = ?"
            : "UPDATE article SET statut = ? WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, statut);
            pst.setInt(2, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addLike(int id) {
        if (!updateConnection()) return;
        try {
            PreparedStatement pst = cnx.prepareStatement("UPDATE article SET likes = likes + 1 WHERE id = ?");
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void delete(int id) {
        if (!updateConnection()) return;
        try {
            PreparedStatement pst = cnx.prepareStatement("DELETE FROM article WHERE id = ?");
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
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
        return a;
    }
}
