package com.oncoreminder.services;

import com.oncoreminder.models.RendezVous;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {

    private final Connection cnx;

    public ServiceRendezVous() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /** Patient creates a new appointment request targeting a specific doctor */
    public boolean ajouter(RendezVous rv) {
        String sql = "INSERT INTO rendez_vous (patient_id, medecin_id, date_rdv, lieu, notes, statut) VALUES (?, ?, ?, ?, ?, 'EN_ATTENTE')";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rv.getPatientId());
            if (rv.getMedecinId() != null) ps.setInt(2, rv.getMedecinId());
            else ps.setNull(2, Types.INTEGER);
            ps.setTimestamp(3, Timestamp.valueOf(rv.getDateRdv()));
            if (rv.getLieu() != null) ps.setString(4, rv.getLieu()); else ps.setNull(4, Types.VARCHAR);
            if (rv.getNotes() != null) ps.setString(5, rv.getNotes()); else ps.setNull(5, Types.VARCHAR);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) rv.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur ajout rendez-vous : " + e.getMessage());
        }
        return false;
    }

    /** Patient modifies an EN_ATTENTE rendez-vous */
    public boolean modifier(RendezVous rv) {
        String sql = "UPDATE rendez_vous SET medecin_id=?, date_rdv=?, lieu=?, notes=? WHERE id=? AND patient_id=? AND statut='EN_ATTENTE'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (rv.getMedecinId() != null) ps.setInt(1, rv.getMedecinId());
            else ps.setNull(1, Types.INTEGER);
            ps.setTimestamp(2, Timestamp.valueOf(rv.getDateRdv()));
            if (rv.getLieu() != null) ps.setString(3, rv.getLieu()); else ps.setNull(3, Types.VARCHAR);
            if (rv.getNotes() != null) ps.setString(4, rv.getNotes()); else ps.setNull(4, Types.VARCHAR);
            ps.setInt(5, rv.getId());
            ps.setInt(6, rv.getPatientId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modifier rendez-vous : " + e.getMessage());
        }
        return false;
    }

    /** Doctor: get all requests sent to them (filtered by medecin_id) */
    public List<RendezVous> getAllForDoctor(int medecinId) {
        List<RendezVous> list = new ArrayList<>();
        String sql = """
                SELECT rv.*, CONCAT(u.prenom, ' ', u.nom) AS patient_nom
                FROM rendez_vous rv
                LEFT JOIN utilisateur u ON rv.patient_id = u.id
                WHERE rv.medecin_id = ?
                ORDER BY FIELD(rv.statut,'EN_ATTENTE','ACCEPTE','REFUSE'), rv.date_rdv ASC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRowWithNom(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAllForDoctor : " + e.getMessage());
        }
        return list;
    }

    /** Patient: get their own appointments with doctor name */
    public List<RendezVous> getByPatient(int patientId) {
        List<RendezVous> list = new ArrayList<>();
        String sql = """
                SELECT rv.*, CONCAT(m.prenom, ' ', m.nom) AS medecin_nom
                FROM rendez_vous rv
                LEFT JOIN utilisateur m ON rv.medecin_id = m.id
                WHERE rv.patient_id = ?
                ORDER BY rv.date_rdv DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RendezVous rv = mapRow(rs);
                try { rv.setMedecinNom(rs.getString("medecin_nom")); } catch (SQLException ignored) {}
                list.add(rv);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getByPatient : " + e.getMessage());
        }
        return list;
    }

    /** Get a single rdv with patient and doctor names */
    public RendezVous getById(int id) {
        String sql = """
                SELECT rv.*,
                       CONCAT(p.prenom, ' ', p.nom) AS patient_nom,
                       CONCAT(m.prenom, ' ', m.nom) AS medecin_nom
                FROM rendez_vous rv
                LEFT JOIN utilisateur p ON rv.patient_id = p.id
                LEFT JOIN utilisateur m ON rv.medecin_id = m.id
                WHERE rv.id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                RendezVous rv = mapRowWithNom(rs);
                try { rv.setMedecinNom(rs.getString("medecin_nom")); } catch (SQLException ignored) {}
                return rv;
            }
        } catch (SQLException e) {
            System.err.println("Erreur getById rendez-vous : " + e.getMessage());
        }
        return null;
    }

    /** Doctor: accept a request (assigns them as the doctor) */
    public boolean accepter(int id, int medecinId) {
        String sql = "UPDATE rendez_vous SET statut='ACCEPTE', medecin_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur accepter rendez-vous : " + e.getMessage());
        }
        return false;
    }

    /** Doctor: refuse a request */
    public boolean refuser(int id) {
        String sql = "UPDATE rendez_vous SET statut='REFUSE' WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur refuser rendez-vous : " + e.getMessage());
        }
        return false;
    }

    /** Get accepted RDVs for a specific doctor (for ordonnance ComboBox) */
    public List<RendezVous> getAcceptesForDoctor(int medecinId) {
        List<RendezVous> list = new ArrayList<>();
        String sql = """
                SELECT rv.*, CONCAT(u.prenom, ' ', u.nom) AS patient_nom
                FROM rendez_vous rv
                LEFT JOIN utilisateur u ON rv.patient_id = u.id
                WHERE rv.statut = 'ACCEPTE' AND rv.medecin_id = ?
                ORDER BY rv.date_rdv DESC
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRowWithNom(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAcceptesForDoctor : " + e.getMessage());
        }
        return list;
    }

    /** @deprecated Use getAcceptesForDoctor(medecinId) instead */
    public List<RendezVous> getAcceptes() {
        List<RendezVous> list = new ArrayList<>();
        String sql = """
                SELECT rv.*, CONCAT(u.prenom, ' ', u.nom) AS patient_nom
                FROM rendez_vous rv
                LEFT JOIN utilisateur u ON rv.patient_id = u.id
                WHERE rv.statut = 'ACCEPTE'
                ORDER BY rv.date_rdv DESC
                """;
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRowWithNom(rs));
        } catch (SQLException e) {
            System.err.println("Erreur getAcceptes : " + e.getMessage());
        }
        return list;
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM rendez_vous WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur suppression rendez-vous : " + e.getMessage());
        }
        return false;
    }

    private RendezVous mapRow(ResultSet rs) throws SQLException {
        return new RendezVous(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getObject("medecin_id") != null ? rs.getInt("medecin_id") : null,
                rs.getTimestamp("date_rdv").toLocalDateTime(),
                rs.getString("lieu"),
                rs.getString("notes"),
                parseStatut(rs.getString("statut"))
        );
    }

    private RendezVous mapRowWithNom(ResultSet rs) throws SQLException {
        RendezVous rv = mapRow(rs);
        try { rv.setPatientNom(rs.getString("patient_nom")); } catch (SQLException ignored) {}
        return rv;
    }

    private RendezVous.Statut parseStatut(String s) {
        if (s == null) return RendezVous.Statut.EN_ATTENTE;
        try { return RendezVous.Statut.valueOf(s); }
        catch (IllegalArgumentException e) { return RendezVous.Statut.EN_ATTENTE; }
    }
}