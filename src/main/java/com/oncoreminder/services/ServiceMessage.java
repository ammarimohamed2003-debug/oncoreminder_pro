package com.oncoreminder.services;

import com.oncoreminder.models.Message;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.*;

public class ServiceMessage {

    private Connection cnx;

    public ServiceMessage() {
        updateConnection();
        ensureTable();
    }

    private void ensureTable() {
        try {
            cnx.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS `message` (" +
                "`id` int(11) NOT NULL AUTO_INCREMENT," +
                "`article_id` int(11) NOT NULL," +
                "`expediteur_id` int(11) NOT NULL," +
                "`destinataire_id` int(11) NOT NULL," +
                "`contenu` text NOT NULL," +
                "`date_envoi` datetime DEFAULT CURRENT_TIMESTAMP," +
                "`lu` tinyint(1) DEFAULT 0," +
                "PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (SQLException ignored) {}
    }

    private boolean updateConnection() {
        this.cnx = MyDataBase.getInstance().getCnx();
        return this.cnx != null;
    }

    public void send(int articleId, int expediteurId, int destinataireId, String contenu) {
        if (!updateConnection()) return;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "INSERT INTO message (article_id, expediteur_id, destinataire_id, contenu) VALUES (?,?,?,?)");
            pst.setInt(1, articleId);
            pst.setInt(2, expediteurId);
            pst.setInt(3, destinataireId);
            pst.setString(4, contenu);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur envoi message: " + e.getMessage());
        }
    }

    /** All messages between two users for a given article, ordered chronologically. */
    public List<Message> getConversation(int articleId, int userId1, int userId2) {
        List<Message> list = new ArrayList<>();
        if (!updateConnection()) return list;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT m.*, CONCAT(u.prenom,' ',u.nom) AS exp_nom " +
                "FROM message m JOIN utilisateur u ON m.expediteur_id = u.id " +
                "WHERE m.article_id = ? AND " +
                "((m.expediteur_id=? AND m.destinataire_id=?) OR " +
                " (m.expediteur_id=? AND m.destinataire_id=?)) " +
                "ORDER BY m.date_envoi ASC");
            pst.setInt(1, articleId);
            pst.setInt(2, userId1); pst.setInt(3, userId2);
            pst.setInt(4, userId2); pst.setInt(5, userId1);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setId(rs.getInt("id"));
                msg.setArticleId(rs.getInt("article_id"));
                msg.setExpediteurId(rs.getInt("expediteur_id"));
                msg.setDestinataire(rs.getInt("destinataire_id"));
                msg.setContenu(rs.getString("contenu"));
                msg.setDateEnvoi(rs.getTimestamp("date_envoi").toLocalDateTime());
                msg.setLu(rs.getBoolean("lu"));
                msg.setExpediteurNom(rs.getString("exp_nom"));
                list.add(msg);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    /** Distinct patients (id → full name) who sent messages to a doctor about an article. */
    public Map<Integer, String> getPatientsByArticle(int articleId, int medecinId) {
        Map<Integer, String> map = new LinkedHashMap<>();
        if (!updateConnection()) return map;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT DISTINCT m.expediteur_id, CONCAT(u.prenom,' ',u.nom) AS nom " +
                "FROM message m JOIN utilisateur u ON m.expediteur_id = u.id " +
                "WHERE m.article_id=? AND m.destinataire_id=?");
            pst.setInt(1, articleId);
            pst.setInt(2, medecinId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) map.put(rs.getInt("expediteur_id"), rs.getString("nom"));
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return map;
    }

    public int countUnread(int articleId, int destinataireId, int expediteurId) {
        if (!updateConnection()) return 0;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "SELECT COUNT(*) FROM message WHERE article_id=? AND destinataire_id=? AND expediteur_id=? AND lu=0");
            pst.setInt(1, articleId); pst.setInt(2, destinataireId); pst.setInt(3, expediteurId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return 0;
    }

    public void markRead(int articleId, int destinataireId, int expediteurId) {
        if (!updateConnection()) return;
        try {
            PreparedStatement pst = cnx.prepareStatement(
                "UPDATE message SET lu=1 WHERE article_id=? AND destinataire_id=? AND expediteur_id=?");
            pst.setInt(1, articleId); pst.setInt(2, destinataireId); pst.setInt(3, expediteurId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
