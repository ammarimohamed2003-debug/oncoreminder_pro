package com.oncoreminder.services;

import com.oncoreminder.models.Event;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvent {

    public void add(Event e, int createurId) {
        String req = "INSERT INTO `event`(titre, description, date_event, lieu, capacite_max, places_restantes, createur_id, image_path) VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getDateEvent()));
            ps.setString(4, e.getLieu());
            ps.setInt(5, e.getCapacite());
            ps.setInt(6, e.getCapacite());
            ps.setInt(7, createurId);
            ps.setString(8, e.getImagePath());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceEvent.add: " + ex.getMessage());
        }
    }

    public List<Event> getAll() {
        List<Event> events = new ArrayList<>();
        String req = "SELECT * FROM `event` ORDER BY date_event ASC";
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
            System.err.println("ServiceEvent.getAll: " + ex.getMessage());
        }
        return events;
    }

    public void update(Event e) {
        String req = "UPDATE `event` SET titre=?, description=?, date_event=?, lieu=?, capacite_max=?, places_restantes=?, image_path=? WHERE id=?";
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
        } catch (SQLException ex) {
            System.err.println("ServiceEvent.update: " + ex.getMessage());
        }
    }

    public void delete(Event e) {
        String req = "DELETE FROM `event` WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceEvent.delete: " + ex.getMessage());
        }
    }

    public boolean hasPlacesDisponibles(int eventId) {
        String req = "SELECT places_restantes FROM `event` WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("places_restantes") > 0;
        } catch (SQLException ex) {
            System.err.println("ServiceEvent.hasPlaces: " + ex.getMessage());
        }
        return false;
    }

    public void decrementerPlaces(int eventId) {
        String req = "UPDATE `event` SET places_restantes = places_restantes - 1 WHERE id=? AND places_restantes > 0";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, eventId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceEvent.decrementer: " + ex.getMessage());
        }
    }

    public void incrementerPlaces(int eventId) {
        String req = "UPDATE `event` SET places_restantes = places_restantes + 1 WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, eventId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceEvent.incrementer: " + ex.getMessage());
        }
    }

    public int countReservationsActives() {
        String req = "SELECT COUNT(*) FROM reservation WHERE statut='CONFIRMEE'";
        try {
            Statement stm = MyDataBase.getInstance().getCnx().createStatement();
            ResultSet rs = stm.executeQuery(req);
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("ServiceEvent.countResa: " + ex.getMessage());
        }
        return 0;
    }
}