package com.oncoreminder.services;

import com.oncoreminder.utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceArticleEvaluation {

    public ServiceArticleEvaluation() {
        ensureTable();
    }

    private void ensureTable() {
        try {
            MyDataBase.getInstance().getCnx().createStatement().execute(
                "CREATE TABLE IF NOT EXISTS article_evaluation (" +
                "article_id INT NOT NULL," +
                "user_id    INT NOT NULL," +
                "note       TINYINT NOT NULL," +
                "PRIMARY KEY (article_id, user_id)" +
                ")"
            );
        } catch (SQLException ignored) {}
    }

    /** Note donnée par cet utilisateur pour cet article (null si aucune). */
    public Integer getUserNote(int articleId, int userId) {
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(
                "SELECT note FROM article_evaluation WHERE article_id=? AND user_id=?");
            ps.setInt(1, articleId); ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("note");
        } catch (SQLException e) { System.err.println("ArticleEval.getUserNote: " + e.getMessage()); }
        return null;
    }

    /** Sauvegarde ou met à jour la note d'un utilisateur. */
    public void saveNote(int articleId, int userId, int note) {
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(
                "INSERT INTO article_evaluation (article_id, user_id, note) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE note=VALUES(note)");
            ps.setInt(1, articleId); ps.setInt(2, userId); ps.setInt(3, note);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("ArticleEval.saveNote: " + e.getMessage()); }
    }

    /** Moyenne des notes pour un article (0 si aucune évaluation). */
    public double getAverage(int articleId) {
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(
                "SELECT AVG(note) FROM article_evaluation WHERE article_id=?");
            ps.setInt(1, articleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("ArticleEval.getAverage: " + e.getMessage()); }
        return 0;
    }

    /** Nombre total d'évaluations pour un article. */
    public int getCount(int articleId) {
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(
                "SELECT COUNT(*) FROM article_evaluation WHERE article_id=?");
            ps.setInt(1, articleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("ArticleEval.getCount: " + e.getMessage()); }
        return 0;
    }

    /** Liste des évaluateurs : [0]=nom complet, [1]=note (String). */
    public List<String[]> getEvaluations(int articleId) {
        List<String[]> list = new ArrayList<>();
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(
                "SELECT CONCAT(u.prenom,' ',u.nom) AS nom, ae.note " +
                "FROM article_evaluation ae " +
                "JOIN utilisateur u ON ae.user_id = u.id " +
                "WHERE ae.article_id = ? ORDER BY ae.note DESC");
            ps.setInt(1, articleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new String[]{rs.getString("nom"), String.valueOf(rs.getInt("note"))});
        } catch (SQLException e) { System.err.println("ArticleEval.getEvaluations: " + e.getMessage()); }
        return list;
    }
}
