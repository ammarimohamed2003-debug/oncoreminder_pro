package service;

import entity.Role;
import entity.Utilisateur;
import org.mindrot.jbcrypt.BCrypt;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Utilisateur entities and database operations.
 */
public class UtilisateurService {
    private Connection conn;

    public UtilisateurService() {
        conn = MyConnection.getInstance().getConn();
        ensureRolesExist();
    }

    private void ensureRolesExist() {
        String[] roles = {"DOCTEUR", "PATIENT"};
        for (String roleName : roles) {
            String checkSql = "SELECT COUNT(*) FROM role WHERE nom = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, roleName);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO role (nom) VALUES (?)";
                    try (PreparedStatement ips = conn.prepareStatement(insertSql)) {
                        ips.setString(1, roleName);
                        ips.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void addUtilisateur(Utilisateur u) {
        String sql = "INSERT INTO utilisateur (username, password, email, role_id, date_creation) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, BCrypt.hashpw(u.getPassword(), BCrypt.gensalt()));
            ps.setString(3, u.getEmail());
            ps.setInt(4, u.getRole_id());
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Utilisateur> getAllUtilisateurs() {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT u.*, r.nom as role_name FROM utilisateur u JOIN role r ON u.role_id = r.id";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Utilisateur u = new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getInt("role_id"),
                        rs.getTimestamp("date_creation")
                );
                u.setRoleName(rs.getString("role_name"));
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateUtilisateur(Utilisateur u) {
        // Check if password was changed (for simplicity we update it if it's not already hashed or if user provides a new one)
        // In a real app, you'd check if the password field in the UI is modified.
        String sql = "UPDATE utilisateur SET username=?, password=?, email=?, role_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            // Here we assume the password passed is the new one and needs hashing.
            // A more robust way would be needed if we don't always want to change password.
            ps.setString(2, BCrypt.hashpw(u.getPassword(), BCrypt.gensalt()));
            ps.setString(3, u.getEmail());
            ps.setInt(4, u.getRole_id());
            ps.setInt(5, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteUtilisateur(int id) {
        String sql = "DELETE FROM utilisateur WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Role> getAllRoles() {
        List<Role> list = new ArrayList<>();
        String sql = "SELECT * FROM role";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Role(rs.getInt("id"), rs.getString("nom")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
