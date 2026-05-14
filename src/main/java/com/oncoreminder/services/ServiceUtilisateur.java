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
        String req = "INSERT INTO utilisateur (nom, prenom, email, password, role, specialite) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            pst.setString(5, user.getRole());
            pst.setString(6, user.getSpecialite());
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

    // U-04 CRUD Update (sans changement de mot de passe)
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
            System.out.println("[ServiceUtilisateur] Utilisateur #" + user.getId() + " mis à jour.");
        } catch (SQLException e) {
            System.err.println("[ServiceUtilisateur] Erreur update : " + e.getMessage());
        }
    }

    /**
     * U-04b CRUD Update avec nouveau mot de passe (déjà haché BCrypt).
     * Utilisé par l'admin quand il saisit un nouveau mot de passe.
     *
     * @param user Utilisateur avec le mot de passe déjà haché dans user.getPassword()
     */
    public void updateWithPassword(Utilisateur user) {
        if (!updateConnection()) return;
        String req = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, role = ?, password = ? WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getRole());
            pst.setString(5, user.getPassword()); // déjà haché BCrypt
            pst.setInt(6, user.getId());
            pst.executeUpdate();
            System.out.println("[ServiceUtilisateur] Utilisateur #" + user.getId() + " mis à jour avec nouveau mot de passe.");
        } catch (SQLException e) {
            System.err.println("[ServiceUtilisateur] Erreur updateWithPassword : " + e.getMessage());
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
        try { u.setSpecialite(rs.getString("specialite")); }  catch (SQLException ignored) {}
        try { u.setTraitements(rs.getString("traitements")); } catch (SQLException ignored) {}
        try { u.setNotes(rs.getString("notes")); }           catch (SQLException ignored) {}
        return u;
    }

    public List<Utilisateur> getAllMedecins() {
        List<Utilisateur> medecins = new ArrayList<>();
        if (!updateConnection()) return medecins;
        String req = "SELECT * FROM utilisateur WHERE role = 'MEDECIN' ORDER BY specialite, nom, prenom";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) medecins.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return medecins;
    }

    public List<Utilisateur> getMedecinsBySpecialite(String specialite) {
        List<Utilisateur> medecins = new ArrayList<>();
        if (!updateConnection()) return medecins;
        String req = "SELECT * FROM utilisateur WHERE role = 'MEDECIN' AND specialite = ? ORDER BY nom, prenom";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, specialite);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) medecins.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return medecins;
    }

    public List<String> getAllSpecialites() {
        List<String> specialites = new ArrayList<>();
        if (!updateConnection()) return specialites;
        String req = "SELECT DISTINCT specialite FROM utilisateur WHERE role = 'MEDECIN' AND specialite IS NOT NULL ORDER BY specialite";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) specialites.add(rs.getString("specialite"));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return specialites;
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

    /**
     * Met à jour le mot de passe d'un utilisateur identifié par son email.
     * Le mot de passe doit déjà être haché avec BCrypt avant l'appel.
     *
     * @param email          Email de l'utilisateur
     * @param hashedPassword Nouveau mot de passe déjà haché BCrypt
     * @return true si la mise à jour a réussi
     */
    public boolean updatePassword(String email, String hashedPassword) {
        if (!updateConnection()) return false;
        String req = "UPDATE utilisateur SET password = ? WHERE email = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, hashedPassword);
            pst.setString(2, email.toLowerCase().trim());
            int rows = pst.executeUpdate();
            System.out.println("[ServiceUtilisateur] Mot de passe mis à jour pour : " + email);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ServiceUtilisateur] Erreur updatePassword : " + e.getMessage());
        }
        return false;
    }

    /**
     * Vérifie l'ancien mot de passe d'un utilisateur (BCrypt).
     * Utilisé avant de permettre le changement de mot de passe depuis le profil.
     *
     * @param email    Email de l'utilisateur
     * @param plainPwd Mot de passe en clair saisi par l'utilisateur
     * @return true si le mot de passe correspond
     */
    public boolean verifyPassword(String email, String plainPwd) {
        if (!updateConnection()) return false;
        String req = "SELECT password FROM utilisateur WHERE email = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, email.toLowerCase().trim());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String hashed = rs.getString("password");
                return BCrypt.checkpw(plainPwd, hashed);
            }
        } catch (SQLException e) {
            System.err.println("[ServiceUtilisateur] Erreur verifyPassword : " + e.getMessage());
        }
        return false;
    }

    /**
     * Mise à jour complète du dossier patient par un médecin.
     * Met à jour : nom, prenom, email, groupe_sanguin, poids, taille,
     *              antecedents, allergies, traitements, notes.
     *
     * Sécurité : seul un utilisateur avec le rôle MEDECIN peut appeler cette méthode.
     *
     * @param patient  L'objet patient avec les nouvelles données
     * @return true si la mise à jour a réussi
     */
    public boolean updateDossierComplet(Utilisateur patient) {
        if (!updateConnection()) return false;
        String req = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, " +
                     "groupe_sanguin = ?, poids = ?, taille = ?, " +
                     "antecedents = ?, allergies = ?, traitements = ?, notes = ? " +
                     "WHERE id = ? AND role = 'PATIENT'";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, patient.getNom());
            pst.setString(2, patient.getPrenom());
            pst.setString(3, patient.getEmail());
            pst.setString(4, patient.getGroupeSanguin());
            pst.setObject(5, patient.getPoids());
            pst.setObject(6, patient.getTaille());
            pst.setString(7, patient.getAntecedents());
            pst.setString(8, patient.getAllergies());
            pst.setString(9, patient.getTraitements());
            pst.setString(10, patient.getNotes());
            pst.setInt(11, patient.getId());
            int rows = pst.executeUpdate();
            System.out.println("[ServiceUtilisateur] Dossier patient #" + patient.getId() + " mis à jour.");
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ServiceUtilisateur] Erreur updateDossierComplet : " + e.getMessage());
        }
        return false;
    }

    /**
     * Supprime un patient (seuls les PATIENT peuvent être supprimés via cette méthode).
     * Sécurité : la clause WHERE vérifie que l'id correspond bien à un PATIENT.
     *
     * @param patientId ID du patient à supprimer
     * @return true si la suppression a réussi
     */
    public boolean deletePatient(int patientId) {
        if (!updateConnection()) return false;
        String req = "DELETE FROM utilisateur WHERE id = ? AND role = 'PATIENT'";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, patientId);
            int rows = pst.executeUpdate();
            System.out.println("[ServiceUtilisateur] Patient #" + patientId + " supprimé.");
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ServiceUtilisateur] Erreur deletePatient : " + e.getMessage());
        }
        return false;
    }
}
