package com.oncoreminder.services;

import com.oncoreminder.models.Ordonnance;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceOrdonnance {

    private final Connection cnx;

    public ServiceOrdonnance() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

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
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur ajout ordonnance : " + e.getMessage());
        }
        return false;
    }

    public Ordonnance getById(int id) {
        String sql = "SELECT * FROM ordonnance WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Erreur getById ordonnance : " + e.getMessage());
        }
        return null;
    }

    public List<Ordonnance> getAll() {
        List<Ordonnance> list = new ArrayList<>();
        String sql = "SELECT * FROM ordonnance ORDER BY date_emission DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAll ordonnances : " + e.getMessage());
        }
        return list;
    }

    public Ordonnance getByRendezVousId(int rendezVousId) {
        String sql = "SELECT * FROM ordonnance WHERE rendez_vous_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Erreur getByRendezVousId : " + e.getMessage());
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
            System.err.println("Erreur verification rendez-vous : " + e.getMessage());
        }
        return false;
    }

    public Integer getFirstRendezVousId() {
        String sql = "SELECT id FROM rendez_vous ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("Erreur lecture premier rendez-vous : " + e.getMessage());
        }
        return null;
    }

    /** Doctor: get all ordonnances for their accepted rdv */
    public List<Ordonnance> getAllForDoctor(int medecinId) {
        List<Ordonnance> list = new ArrayList<>();
        String sql = """
                SELECT o.* FROM ordonnance o
                JOIN rendez_vous r ON o.rendez_vous_id = r.id
                WHERE r.medecin_id = ?
                ORDER BY o.date_emission DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAllForDoctor ordonnances : " + e.getMessage());
        }
        return list;
    }

    /** Patient: get all their ordonnances */
    public List<Ordonnance> getByPatient(int patientId) {
        List<Ordonnance> list = new ArrayList<>();
        String sql = """
                SELECT o.* FROM ordonnance o
                JOIN rendez_vous r ON o.rendez_vous_id = r.id
                WHERE r.patient_id = ?
                ORDER BY o.date_emission DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getByPatient ordonnances : " + e.getMessage());
        }
        return list;
    }

    public boolean modifier(Ordonnance ord) {
        String sql = "UPDATE ordonnance SET rendez_vous_id=?, medicaments=?, posologie=?, duree_jours=?, date_emission=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ord.getRendezVousId());
            ps.setString(2, ord.getMedicaments());
            ps.setString(3, ord.getPosologie());
            ps.setInt(4, ord.getDureeJours());
            ps.setDate(5, Date.valueOf(ord.getDateEmission()));
            ps.setInt(6, ord.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification ordonnance : " + e.getMessage());
        }
        return false;
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM ordonnance WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur suppression ordonnance : " + e.getMessage());
        }
        return false;
    }

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