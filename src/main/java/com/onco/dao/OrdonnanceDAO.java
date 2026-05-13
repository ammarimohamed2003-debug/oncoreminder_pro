package com.onco.dao;

import com.onco.model.Ordonnance;
import com.onco.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceDAO {

    private final Connection cnx;

    public OrdonnanceDAO() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────
    public boolean ajouter(Ordonnance ord) {
        String sql = "INSERT INTO ordonnance (rendez_vous_id, medicaments, posologie, duree_jours, date_emission) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ord.getRendezVousId());
            ps.setString(2, ord.getMedicaments());
            ps.setString(3, ord.getPosologie());
            ps.setInt(4, ord.getDureeJours());
            ps.setDate(5, Date.valueOf(ord.getDateEmission()));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) ord.setId(keys.getInt(1));
                System.out.println("✅ Ordonnance ajoutée (id=" + ord.getId() + ")");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout ordonnance : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — by id
    // ─────────────────────────────────────────────────────────────
    public Ordonnance getById(int id) {
        String sql = "SELECT * FROM ordonnance WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("❌ Erreur getById ordonnance : " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — all
    // ─────────────────────────────────────────────────────────────
    public List<Ordonnance> getAll() {
        List<Ordonnance> list = new ArrayList<>();
        String sql = "SELECT * FROM ordonnance ORDER BY date_emission DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("❌ Erreur getAll ordonnances : " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — by rendez_vous_id (ordonnance liée à un RDV)
    // ─────────────────────────────────────────────────────────────
    public Ordonnance getByRendezVousId(int rendezVousId) {
        String sql = "SELECT * FROM ordonnance WHERE rendez_vous_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("❌ Erreur getByRendezVousId : " + e.getMessage());
        }
        return null;
    }

    public boolean rendezVousExists(int rendezVousId) {
        String sql = "SELECT 1 FROM rendez_vous WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("❌ Erreur verification rendez-vous : " + e.getMessage());
        }
        return false;
    }

    public Integer getFirstRendezVousId() {
        String sql = "SELECT id FROM rendez_vous ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur lecture premier rendez-vous : " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // READ — toutes les ordonnances d'un traitement
    //        (via jointure rendez_vous → ordonnance)
    // ─────────────────────────────────────────────────────────────
    public List<Ordonnance> getOrdonnancesParTraitement(int traitementId) {
        List<Ordonnance> list = new ArrayList<>();
        String sql = """
                SELECT o.*
                FROM ordonnance o
                JOIN rendez_vous r ON o.rendez_vous_id = r.id
                WHERE r.traitement_id = ?
                ORDER BY o.date_emission DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, traitementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println("❌ Erreur getOrdonnancesParTraitement : " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────
    public boolean modifier(Ordonnance ord) {
        String sql = "UPDATE ordonnance SET rendez_vous_id=?, medicaments=?, posologie=?, duree_jours=?, date_emission=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ord.getRendezVousId());
            ps.setString(2, ord.getMedicaments());
            ps.setString(3, ord.getPosologie());
            ps.setInt(4, ord.getDureeJours());
            ps.setDate(5, Date.valueOf(ord.getDateEmission()));
            ps.setInt(6, ord.getId());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.println("✅ Ordonnance modifiée (id=" + ord.getId() + ")");
            return ok;
        } catch (SQLException e) {
            System.out.println("❌ Erreur modification ordonnance : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────
    public boolean supprimer(int id) {
        String sql = "DELETE FROM ordonnance WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) System.out.println("✅ Ordonnance supprimée (id=" + id + ")");
            return ok;
        } catch (SQLException e) {
            System.out.println("❌ Erreur suppression ordonnance : " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Helper — map ResultSet row → Ordonnance
    // ─────────────────────────────────────────────────────────────
    private Ordonnance mapRow(ResultSet rs) throws SQLException {
        return new Ordonnance(
                rs.getInt("id"),
                rs.getInt("rendez_vous_id"),
                rs.getString("medicaments"),
                rs.getString("posologie"),
                rs.getInt("duree_jours"),
                rs.getDate("date_emission").toLocalDate()
        );
    }
}
