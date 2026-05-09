package services;

import interfaces.IService;
import models.Event;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvent implements IService<Event> {

    public ServiceEvent() {
        ensureImagePathColumn();
    }

    @Override
    public void add(Event e) {
        String req = "INSERT INTO event(titre, description, date_event, lieu, capacite_max, places_restantes, createur_id, image_path) VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getDateEvent()));
            ps.setString(4, e.getLieu());
            ps.setInt(5, e.getCapacite());
            ps.setInt(6, e.getCapacite());
            ps.setInt(7, 1);
            ps.setString(8, e.getImagePath());
            ps.executeUpdate();
            System.out.println("Event ajouté !");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public List<Event> getAll() {
        List<Event> events = new ArrayList<>();
        String req = "SELECT * FROM event";
        try {
            Statement stm = MyDataBase.getInstance().getCnx().createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                Event e = new Event();
                e.setId(rs.getInt("id"));
                e.setTitre(rs.getString("titre"));
                e.setDescription(rs.getString("description"));
                e.setDateEvent(rs.getTimestamp("date_event").toLocalDateTime());
                e.setLieu(rs.getString("lieu"));
                e.setCapacite(rs.getInt("capacite_max"));
                e.setPlacesRestantes(rs.getInt("places_restantes"));
                e.setImagePath(rs.getString("image_path"));
                events.add(e);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return events;
    }

    @Override
    public void update(Event e) {
        String req = "UPDATE event SET titre=?, description=?, date_event=?, lieu=?, capacite_max=?, places_restantes=?, image_path=? WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getDateEvent()));
            ps.setString(4, e.getLieu());
            ps.setInt(5, e.getCapacite());
            ps.setInt(6, e.getPlacesRestantes());
            ps.setString(7, e.getImagePath());
            ps.setInt(8, e.getId());
            ps.executeUpdate();
            System.out.println("Event modifié !");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void delete(Event e) {
        String req = "DELETE FROM event WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, e.getId());
            ps.executeUpdate();
            System.out.println("Event supprimé !");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public boolean hasPlacesDisponibles(int eventId) {
        String req = "SELECT places_restantes FROM event WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("places_restantes") > 0;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return false;
    }

    public void decrementerPlaces(int eventId) {
        String req = "UPDATE event SET places_restantes = places_restantes - 1 WHERE id=? AND places_restantes > 0";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, eventId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void incrementerPlaces(int eventId) {
        String req = "UPDATE event SET places_restantes = places_restantes + 1 WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, eventId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public int countReservationsActives() {
        String req = "SELECT COUNT(*) FROM reservation WHERE statut='CONFIRMEE'";
        try {
            Statement statement = MyDataBase.getInstance().getCnx().createStatement();
            ResultSet rs = statement.executeQuery(req);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return 0;
    }

    private void ensureImagePathColumn() {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();
            if (cnx == null) {
                return;
            }
            DatabaseMetaData metaData = cnx.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "event", "image_path");
            if (!columns.next()) {
                Statement statement = cnx.createStatement();
                statement.executeUpdate("ALTER TABLE event ADD COLUMN image_path VARCHAR(500) NULL");
            }
        } catch (SQLException ex) {
            System.out.println("Verification image_path : " + ex.getMessage());
        }
    }
}
