package com.oncoreminder.services;

import com.oncoreminder.models.LogConnexion;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.utils.MyDataBase;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUtilisateur {

    private Connection cnx;

    public ServiceUtilisateur() {
        updateConnection();
    }

    private boolean updateConnection() {
        this.cnx = MyDataBase.getInstance().getCnx();
        if (this.cnx == null) {
            System.err.println("ServiceUtilisateur: Impossible d'établir une connexion à la base de données.");
            return false;
        }
        return true;
    }

    // U-01 Inscription / CRUD Add
    public void add(Utilisateur user) {
        if (!updateConnection()) return;
        String req = "INSERT INTO utilisateur (nom, prenom, email, password, role) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            // Hash password with BCrypt
            pst.setString(4, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            pst.setString(5, user.getRole());
            pst.executeUpdate();
            System.out.println("Utilisateur ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    // U-04 CRUD List
    public List<Utilisateur> getAll() {
        List<Utilisateur> users = new ArrayList<>();
        if (!updateConnection()) return users;
        String req = "SELECT * FROM utilisateur";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                Utilisateur u = mapResultSetToUser(rs);
                users.add(u);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return users;
    }

    // U-04 CRUD Delete
    public void delete(int id) {
        if (!updateConnection()) return;
        String req = "DELETE FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Utilisateur supprimé !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // U-04 CRUD Update
    public void update(Utilisateur user) {
        if (!updateConnection()) return;
        String req = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, role = ? WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getRole());
            pst.setInt(5, user.getId());
            pst.executeUpdate();
            System.out.println("Utilisateur mis à jour !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // U-05 Dossier Médical Update
    public void updateMedicalRecord(Utilisateur user) {
        if (!updateConnection()) return;
        String req = "UPDATE utilisateur SET antecedents = ?, allergies = ?, groupe_sanguin = ?, poids = ?, taille = ? WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, user.getAntecedents());
            pst.setString(2, user.getAllergies());
            pst.setString(3, user.getGroupeSanguin());
            pst.setObject(4, user.getPoids());
            pst.setObject(5, user.getTaille());
            pst.setInt(6, user.getId());
            pst.executeUpdate();
            System.out.println("Dossier médical mis à jour !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // U-02 Authentification
    public Utilisateur login(String email, String password) {
        if (!updateConnection()) return null;
        
        String req = "SELECT * FROM utilisateur WHERE email = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("password");
                if (BCrypt.checkpw(password, hashed)) {
                    Utilisateur user = mapResultSetToUser(rs);
                    // U-06 Track Login
                    logConnection(user.getId());
                    return user;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    // U-06 Journal des connexions
    private void logConnection(int userId) {
        String req = "INSERT INTO log_connexions (user_id) VALUES (?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log connection: " + e.getMessage());
        }
    }

    public List<LogConnexion> getAllLogs() {
        List<LogConnexion> logs = new ArrayList<>();
        if (!updateConnection()) return logs;
        String req = "SELECT l.*, u.email FROM log_connexions l JOIN utilisateur u ON l.user_id = u.id ORDER BY l.date_connexion DESC";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                LogConnexion log = new LogConnexion(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getTimestamp("date_connexion").toLocalDateTime()
                );
                log.setUserEmail(rs.getString("email"));
                logs.add(log);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return logs;
    }

    private Utilisateur mapResultSetToUser(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role")
        );
        u.setAntecedents(rs.getString("antecedents"));
        u.setAllergies(rs.getString("allergies"));
        u.setGroupeSanguin(rs.getString("groupe_sanguin"));
        u.setPoids(rs.getObject("poids", Double.class));
        u.setTaille(rs.getObject("taille", Double.class));
        try { u.setMedecinId(rs.getObject("medecin_id", Integer.class)); } catch (SQLException ignored) {}
        return u;
    }

    public List<Utilisateur> getPatientsByMedecin(int medecinId) {
        List<Utilisateur> patients = new ArrayList<>();
        if (!updateConnection()) return patients;
        String req = "SELECT * FROM utilisateur WHERE role = 'PATIENT' AND medecin_id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, medecinId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) patients.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return patients;
    }

    public List<Utilisateur> getUnassignedPatients() {
        List<Utilisateur> patients = new ArrayList<>();
        if (!updateConnection()) return patients;
        String req = "SELECT * FROM utilisateur WHERE role = 'PATIENT' AND medecin_id IS NULL";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) patients.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return patients;
    }

    public void assignMedecin(int patientId, int medecinId) {
        if (!updateConnection()) return;
        String req = "UPDATE utilisateur SET medecin_id = ? WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, medecinId);
            pst.setInt(2, patientId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void unassignMedecin(int patientId) {
        if (!updateConnection()) return;
        String req = "UPDATE utilisateur SET medecin_id = NULL WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, patientId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public Utilisateur getById(int id) {
        if (!updateConnection()) return null;
        String req = "SELECT * FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public boolean emailExists(String email) {
        if (!updateConnection()) return false;
        String req = "SELECT id FROM utilisateur WHERE email = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}
