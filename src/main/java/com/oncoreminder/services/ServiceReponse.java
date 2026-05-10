package com.oncoreminder.services;

import com.oncoreminder.models.Reponse;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceReponse {

    // ── CRUD de base ──────────────────────────────────────────────────

    public void add(Reponse r) {
        String req = "INSERT INTO reponse(reclamation_id, administrateur_id, message) VALUES (?,?,?)";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, r.getReclamationId());
            ps.setInt(2, r.getAdministrateurId());
            ps.setString(3, r.getMessage());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceReponse.add: " + ex.getMessage());
        }
    }

    public List<Reponse> getByReclamation(int reclamationId) {
        List<Reponse> list = new ArrayList<>();
        String req = "SELECT rep.*, CONCAT(u.prenom, ' ', u.nom) AS admin_nom " +
                     "FROM reponse rep " +
                     "JOIN utilisateur u ON rep.administrateur_id = u.id " +
                     "WHERE rep.reclamation_id=? " +
                     "ORDER BY rep.date_reponse ASC";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("ServiceReponse.getByReclamation: " + ex.getMessage());
        }
        return list;
    }

    // ── Métier : consultation ─────────────────────────────────────────

    /**
     * Retourne la dernière réponse envoyée pour une réclamation.
     */
    public Optional<Reponse> getLatest(int reclamationId) {
        String req = "SELECT rep.*, CONCAT(u.prenom, ' ', u.nom) AS admin_nom " +
                     "FROM reponse rep " +
                     "JOIN utilisateur u ON rep.administrateur_id = u.id " +
                     "WHERE rep.reclamation_id=? " +
                     "ORDER BY rep.date_reponse DESC LIMIT 1";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("ServiceReponse.getLatest: " + ex.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Nombre de réponses pour une réclamation donnée.
     */
    public int countByReclamation(int reclamationId) {
        String req = "SELECT COUNT(*) FROM reponse WHERE reclamation_id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("ServiceReponse.countByReclamation: " + ex.getMessage());
        }
        return 0;
    }

    /**
     * Nombre total de réponses envoyées par un administrateur.
     */
    public int countByAdmin(int adminId) {
        String req = "SELECT COUNT(*) FROM reponse WHERE administrateur_id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, adminId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("ServiceReponse.countByAdmin: " + ex.getMessage());
        }
        return 0;
    }

    // ── Métier : règle "réclamation répondue" ─────────────────────────

    /**
     * Indique si la réclamation a déjà reçu au moins une réponse.
     */
    public boolean hasReponse(int reclamationId) {
        return countByReclamation(reclamationId) > 0;
    }

    // ── Métier : suppression ──────────────────────────────────────────

    /**
     * Supprime toutes les réponses d'une réclamation (avant suppression de la réclamation).
     */
    public void deleteByReclamation(int reclamationId) {
        String req = "DELETE FROM reponse WHERE reclamation_id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, reclamationId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceReponse.deleteByReclamation: " + ex.getMessage());
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────

    private Reponse mapRow(ResultSet rs) throws SQLException {
        Reponse r = new Reponse();
        r.setId(rs.getInt("id"));
        r.setReclamationId(rs.getInt("reclamation_id"));
        r.setAdministrateurId(rs.getInt("administrateur_id"));
        r.setMessage(rs.getString("message"));
        Timestamp ts = rs.getTimestamp("date_reponse");
        if (ts != null) r.setDateReponse(ts.toLocalDateTime());
        r.setAdminNomComplet(rs.getString("admin_nom"));
        return r;
    }
}
