package com.oncoreminder.services;

import com.oncoreminder.models.Reservation;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReservation {

    private final ServiceEvent serviceEvent = new ServiceEvent();

    public boolean add(Reservation r) {
        if (!serviceEvent.hasPlacesDisponibles(r.getEventId())) {
            System.out.println("Plus de places disponibles !");
            return false;
        }
        if (hasReservationActive(r.getEventId(), r.getUserId())) {
            System.out.println("Reservation deja active pour cet utilisateur !");
            return false;
        }
        String req = "INSERT INTO reservation(event_id, utilisateur_id, date_reservation, statut) VALUES (?,?,?,?)";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, r.getEventId());
            ps.setInt(2, r.getUserId());
            ps.setTimestamp(3, Timestamp.valueOf(r.getDateReservation()));
            ps.setString(4, r.getStatut());
            ps.executeUpdate();
            serviceEvent.decrementerPlaces(r.getEventId());
            return true;
        } catch (SQLException ex) {
            System.err.println("ServiceReservation.add: " + ex.getMessage());
        }
        return false;
    }

    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String req = "SELECT * FROM reservation ORDER BY date_reservation DESC";
        try {
            Statement stm = MyDataBase.getInstance().getCnx().createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                Reservation r = new Reservation();
                r.setId(rs.getInt("id"));
                r.setEventId(rs.getInt("event_id"));
                r.setUserId(rs.getInt("utilisateur_id"));
                r.setDateReservation(rs.getTimestamp("date_reservation").toLocalDateTime());
                r.setStatut(rs.getString("statut"));
                list.add(r);
            }
        } catch (SQLException ex) {
            System.err.println("ServiceReservation.getAll: " + ex.getMessage());
        }
        return list;
    }

    public void update(Reservation r) {
        String req = "UPDATE reservation SET statut=? WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, r.getStatut());
            ps.setInt(2, r.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("ServiceReservation.update: " + ex.getMessage());
        }
    }

    public void delete(Reservation r) {
        if ("ANNULEE".equalsIgnoreCase(r.getStatut())) return;
        r.setStatut("ANNULEE");
        update(r);
        serviceEvent.incrementerPlaces(r.getEventId());
    }

    public boolean hasReservationActive(int eventId, int userId) {
        String req = "SELECT COUNT(*) FROM reservation WHERE event_id=? AND utilisateur_id=? AND statut='CONFIRMEE'";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setInt(1, eventId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException ex) {
            System.err.println("ServiceReservation.hasActive: " + ex.getMessage());
        }
        return false;
    }
}
