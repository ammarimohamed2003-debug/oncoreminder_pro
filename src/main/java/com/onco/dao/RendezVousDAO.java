package com.onco.dao;

import com.onco.model.RendezVous;
import com.onco.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RendezVousDAO {

    private final Connection cnx;

    public RendezVousDAO() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────
    public boolean ajouter(RendezVous rv) {
        String sql = "INSERT INTO rendez_vous (traitement_id, date_rdv, lieu, notes) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rv.getTraitementId());
            ps.setTimestamp(2, Timestamp.valueOf(rv.getDateRdv()));
            // lieu and notes are nullable
            if (rv.getLieu() != null) ps.setString(3, rv.getLieu());
            else ps.setNull(3, Types.VARCHAR);
            if (rv.getNotes() != null) ps.setString(4, rv.getNotes());
            else ps.setNull(4, Types.VARCHAR);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) rv.setId(keys.getInt(1));
                System.out.println("✅ Rendez-vous ajouté (id=" + rv.getId() + ")");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout rendez-vous : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — by id
    // ─────────────────────────────────────────────────────────────
    public RendezVous getById(int id) {
        String sql = "SELECT * FROM rendez_vous WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("❌ Erreur getById rendez-vous : " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — all
    // ─────────────────────────────────────────────────────────────
    public List<RendezVous> getAll() {
        List<RendezVous> list = new ArrayList<>();
        String sql = "SELECT * FROM rendez_vous ORDER BY date_rdv DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("❌ Erreur getAll rendez-vous : " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — by traitement_id (tous les RDV d'un traitement)
    // ─────────────────────────────────────────────────────────────
    public List<RendezVous> getByTraitement(int traitementId) {
        List<RendezVous> list = new ArrayList<>();
        String sql = "SELECT * FROM rendez_vous WHERE traitement_id = ? ORDER BY date_rdv DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, traitementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("❌ Erreur getByTraitement : " + e.getMessage());
        }
        return list;
    }

    public boolean traitementExists(int traitementId) {
        String sql = "SELECT 1 FROM traitement WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, traitementId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("❌ Erreur verification traitement : " + e.getMessage());
        }
        return false;
    }

    public Integer getFirstTraitementId() {
        String sql = "SELECT id FROM traitement ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur lecture premier traitement : " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────
    public boolean modifier(RendezVous rv) {
        String sql = "UPDATE rendez_vous SET traitement_id=?, date_rdv=?, lieu=?, notes=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, rv.getTraitementId());
            ps.setTimestamp(2, Timestamp.valueOf(rv.getDateRdv()));
            if (rv.getLieu() != null) ps.setString(3, rv.getLieu());
            else ps.setNull(3, Types.VARCHAR);
            if (rv.getNotes() != null) ps.setString(4, rv.getNotes());
            else ps.setNull(4, Types.VARCHAR);
            ps.setInt(5, rv.getId());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.println("✅ Rendez-vous modifié (id=" + rv.getId() + ")");
            return ok;
        } catch (SQLException e) {
            System.out.println("❌ Erreur modification rendez-vous : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────
    public boolean supprimer(int id) {
        String sql = "DELETE FROM rendez_vous WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.println("✅ Rendez-vous supprimé (id=" + id + ")");
            return ok;
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression rendez-vous : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Helper — map ResultSet row → RendezVous
    // ─────────────────────────────────────────────────────────────
    private RendezVous mapRow(ResultSet rs) throws SQLException {
        return new RendezVous(
                rs.getInt("id"),
                rs.getInt("traitement_id"),
                rs.getTimestamp("date_rdv").toLocalDateTime(),
                rs.getString("lieu"),    // nullable → returns null if SQL NULL
                rs.getString("notes")   // nullable → returns null if SQL NULL
        );
    }
}
