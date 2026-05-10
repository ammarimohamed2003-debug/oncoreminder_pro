package com.oncoreminder.services;

import com.oncoreminder.models.Commentaire;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire {

    private Connection cnx;

    public ServiceCommentaire() {
        updateConnection();
        ensureTable();
    }

    private void ensureTable() {
        try {
            cnx.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS commentaire_likes (" +
                "user_id INT NOT NULL, commentaire_id INT NOT NULL, " +
                "PRIMARY KEY (user_id, commentaire_id))");
        } catch (SQLException ignored) {}
    }

    private boolean updateConnection() {
        this.cnx = MyDataBase.getInstance().getCnx();
        if (this.cnx == null) {
            System.err.println("ServiceCommentaire: Impossible d'établir une connexion.");
            return false;
        }
        return true;
    }

    public void add(Commentaire commentaire) {
        if (!updateConnection()) return;
        String req = "INSERT INTO commentaire (article_id, contenu, auteur) VALUES (?, ?, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, commentaire.getArticleId());
            pst.setString(2, commentaire.getContenu());
            pst.setString(3, commentaire.getAuteur());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur ajout commentaire: " + e.getMessage());
        }
    }

    public List<Commentaire> getByArticle(int articleId) {
        List<Commentaire> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM commentaire WHERE article_id = ? ORDER BY date_commentaire ASC");
            pst.setInt(1, articleId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Commentaire c = new Commentaire(
                    rs.getInt("id"),
                    rs.getInt("article_id"),
                    rs.getString("contenu"),
                    rs.getString("auteur"),
                    rs.getTimestamp("date_commentaire").toLocalDateTime()
                );
                try { c.setLikes(rs.getInt("likes")); } catch (SQLException ignored) {}
                list.add(c);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    /** Toggle like commentaire — retourne true si maintenant aimé. */
    public boolean toggleLike(int commentaireId, int userId) {
        if (!updateConnection()) return false;
        try {
            if (hasLiked(commentaireId, userId)) {
                cnx.prepareStatement(
                    "DELETE FROM commentaire_likes WHERE user_id=" + userId + " AND commentaire_id=" + commentaireId
                ).executeUpdate();
                cnx.prepareStatement(
                    "UPDATE commentaire SET likes=GREATEST(likes-1,0) WHERE id=" + commentaireId
                ).executeUpdate();
                return false;
            } else {
                cnx.prepareStatement(
                    "INSERT INTO commentaire_likes(user_id,commentaire_id) VALUES(" + userId + "," + commentaireId + ")"
                ).executeUpdate();
                cnx.prepareStatement(
                    "UPDATE commentaire SET likes=likes+1 WHERE id=" + commentaireId
                ).executeUpdate();
                return true;
            }
        } catch (SQLException e) { System.out.println(e.getMessage()); return false; }
    }

    public boolean hasLiked(int commentaireId, int userId) {
        if (!updateConnection()) return false;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT 1 FROM commentaire_likes WHERE user_id=? AND commentaire_id=?");
            pst.setInt(1, userId); pst.setInt(2, commentaireId);
            return pst.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public int countByArticle(int articleId) {
        if (!updateConnection()) return 0;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT COUNT(*) FROM commentaire WHERE article_id = ?");
            pst.setInt(1, articleId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    /** @deprecated Utiliser toggleLike(commentaireId, userId) */
    public void addLike(int id) {
        if (!updateConnection()) return;
        try { cnx.prepareStatement("UPDATE commentaire SET likes=likes+1 WHERE id=" + id).executeUpdate(); }
        catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public void delete(int id) {
        if (!updateConnection()) return;
        try {
            PreparedStatement pst = cnx.prepareStatement("DELETE FROM commentaire WHERE id = ?");
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
