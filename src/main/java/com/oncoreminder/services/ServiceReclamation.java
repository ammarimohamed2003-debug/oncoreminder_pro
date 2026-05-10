package com.oncoreminder.services;

import com.oncoreminder.models.Reclamation;
import com.oncoreminder.models.ReclamationStats;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceReclamation {

    public ServiceReclamation() {
        ensureNoteColumn();
    }

    private void ensureNoteColumn() {
        try {
            MyDataBase.getInstance().getCnx()
                .createStatement()
                .executeUpdate("ALTER TABLE reclamation ADD COLUMN note TINYINT NULL DEFAULT NULL");
        } catch (SQLException ex) {
            // Colonne déjà existante (code 1060) — ignoré
        }
    }

    // ── CRUD de base ──────────────────────────────────────────────────

    public void add(Reclamation r) {
        String req = "INSERT INTO reclamation(utilisateur_id, sujet, message, statut) VALUES (?,?,?,?)";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, r.getUtilisateurId());
            ps.setString(2, r.getSujet());
            ps.setString(3, r.getMessage());
            ps.setString(4, "EN_COURS");
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.add: " + ex.getMessage());
        }
    }

    public List<Reclamation> getAll() {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT r.*, CONCAT(u.prenom, ' ', u.nom) AS utilisateur_nom " +
                     "FROM reclamation r " +
                     "JOIN utilisateur u ON r.utilisateur_id = u.id " +
                     "ORDER BY r.date_reclamation DESC";
        try {
            Statement stm = MyDataBase.getInstance().getCnx().createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) list.add(mapRow(rs, true));
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.getAll: " + ex.getMessage());
        }
        return list;
    }

    public List<Reclamation> getByUtilisateur(int userId) {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT * FROM reclamation WHERE utilisateur_id=? ORDER BY date_reclamation DESC";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs, false));
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.getByUtilisateur: " + ex.getMessage());
        }
        return list;
    }

    public void updateNote(int id, int note) {
        String req = "UPDATE reclamation SET note=? WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, note);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.updateNote: " + ex.getMessage());
        }
    }

    public void updateStatut(int id, String statut) {
        String req = "UPDATE reclamation SET statut=? WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.updateStatut: " + ex.getMessage());
        }
    }

    public void delete(int id) {
        String req = "DELETE FROM reclamation WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.delete: " + ex.getMessage());
        }
    }

    // ── Métier : statistiques ─────────────────────────────────────────

    /**
     * Retourne les statistiques globales des réclamations.
     */
    public ReclamationStats getStats() {
        String req = "SELECT " +
                     "  COUNT(*) AS total, " +
                     "  SUM(statut='EN_COURS') AS en_cours, " +
                     "  SUM(statut='TRAITEE')  AS traitees, " +
                     "  SUM(statut='FERMEE')   AS fermees " +
                     "FROM reclamation";
        try {
            Statement stm = MyDataBase.getInstance().getCnx().createStatement();
            ResultSet rs  = stm.executeQuery(req);
            if (rs.next()) {
                return new ReclamationStats(
                    rs.getInt("total"),
                    rs.getInt("en_cours"),
                    rs.getInt("traitees"),
                    rs.getInt("fermees")
                );
            }
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.getStats: " + ex.getMessage());
        }
        return new ReclamationStats(0, 0, 0, 0);
    }

    /**
     * Retourne le nombre de réclamations par statut.
     */
    public int countByStatut(String statut) {
        String req = "SELECT COUNT(*) FROM reclamation WHERE statut=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, statut);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.countByStatut: " + ex.getMessage());
        }
        return 0;
    }

    // ── Métier : historique et filtres ────────────────────────────────

    /**
     * Réclamations soumises dans les N derniers jours (pour tableau de bord admin).
     */
    public List<Reclamation> getRecentes(int nbJours) {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT r.*, CONCAT(u.prenom, ' ', u.nom) AS utilisateur_nom " +
                     "FROM reclamation r " +
                     "JOIN utilisateur u ON r.utilisateur_id = u.id " +
                     "WHERE r.date_reclamation >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                     "ORDER BY r.date_reclamation DESC";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, nbJours);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs, true));
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.getRecentes: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Filtre par statut avec informations patient (usage admin).
     */
    public List<Reclamation> getByStatut(String statut) {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT r.*, CONCAT(u.prenom, ' ', u.nom) AS utilisateur_nom " +
                     "FROM reclamation r " +
                     "JOIN utilisateur u ON r.utilisateur_id = u.id " +
                     "WHERE r.statut=? " +
                     "ORDER BY r.date_reclamation DESC";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, statut);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs, true));
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.getByStatut: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Réclamations d'un patient filtrées par statut.
     */
    public List<Reclamation> getByUtilisateurAndStatut(int userId, String statut) {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT * FROM reclamation WHERE utilisateur_id=? AND statut=? " +
                     "ORDER BY date_reclamation DESC";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, userId);
            ps.setString(2, statut);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs, false));
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.getByUtilisateurAndStatut: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Réclamations entre deux dates (usage admin pour rapports).
     */
    public List<Reclamation> getByPeriode(LocalDate from, LocalDate to) {
        List<Reclamation> list = new ArrayList<>();
        String req = "SELECT r.*, CONCAT(u.prenom, ' ', u.nom) AS utilisateur_nom " +
                     "FROM reclamation r " +
                     "JOIN utilisateur u ON r.utilisateur_id = u.id " +
                     "WHERE DATE(r.date_reclamation) BETWEEN ? AND ? " +
                     "ORDER BY r.date_reclamation DESC";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs, true));
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.getByPeriode: " + ex.getMessage());
        }
        return list;
    }

    // ── Métier : règles métier ────────────────────────────────────────

    /**
     * Vérifie si une réclamation peut encore recevoir des réponses.
     * Règle : les réclamations FERMEE ne sont plus répondables.
     */
    public boolean isRepondable(int reclamationId) {
        String req = "SELECT statut FROM reclamation WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return !"FERMEE".equals(rs.getString("statut"));
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.isRepondable: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Nombre de réclamations actives (EN_COURS) d'un patient.
     */
    public int countActifsByUtilisateur(int userId) {
        String req = "SELECT COUNT(*) FROM reclamation WHERE utilisateur_id=? AND statut='EN_COURS'";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("ServiceReclamation.countActifsByUtilisateur: " + ex.getMessage());
        }
        return 0;
    }

    // ── Mapping ───────────────────────────────────────────────────────

    private Reclamation mapRow(ResultSet rs, boolean withUserName) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(rs.getInt("id"));
        r.setUtilisateurId(rs.getInt("utilisateur_id"));
        r.setSujet(rs.getString("sujet"));
        r.setMessage(rs.getString("message"));
        r.setStatut(rs.getString("statut"));
        Timestamp ts = rs.getTimestamp("date_reclamation");
        if (ts != null) r.setDateReclamation(ts.toLocalDateTime());
        if (withUserName) r.setUtilisateurNomComplet(rs.getString("utilisateur_nom"));
        try { int n = rs.getInt("note"); if (!rs.wasNull()) r.setNote(n); } catch (SQLException ignored) {}
        return r;
    }
}
