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
                list.add(c);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
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
