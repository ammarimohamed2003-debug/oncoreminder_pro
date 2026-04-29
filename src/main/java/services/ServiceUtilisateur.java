package services;

import interfaces.IService;
import models.Utilisateur;
import utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceUtilisateur implements IService<Utilisateur> {

    private Connection cnx;

    public ServiceUtilisateur() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Utilisateur user) {
        String req = "INSERT INTO utilisateur (nom, prenom, email, password, role) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getPassword());
            pst.setString(5, user.getRole());
            pst.executeUpdate();
            System.out.println("Utilisateur ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout de l'utilisateur : " + e.getMessage());
        }
    }

    @Override
    public List<Utilisateur> getAll() {
        List<Utilisateur> users = new ArrayList<>();
        String req = "SELECT * FROM utilisateur";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                Utilisateur u = new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                );
                users.add(u);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return users;
    }

    @Override
    public void delete(Utilisateur user) {
        String req = "DELETE FROM utilisateur WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setInt(1, user.getId());
            pst.executeUpdate();
            System.out.println("Utilisateur supprimé !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Utilisateur user) {
        String req = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, password = ?, role = ? WHERE id = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getPassword());
            pst.setString(5, user.getRole());
            pst.setInt(6, user.getId());
            pst.executeUpdate();
            System.out.println("Utilisateur mis à jour !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Utilisateur login(String email, String password) {
        String req = "SELECT * FROM utilisateur WHERE email = ? AND password = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, email);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("role")
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
    
    public boolean emailExists(String email) {
        String req = "SELECT id FROM utilisateur WHERE email = ?";
        try {
            PreparedStatement pst = cnx.prepareStatement(req);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}
