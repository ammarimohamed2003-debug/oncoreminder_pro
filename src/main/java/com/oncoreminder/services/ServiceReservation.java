package com.oncoreminder.services;

import com.oncoreminder.models.Reservation;
import com.oncoreminder.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReservation {

    private final ServiceEvent serviceEvent = new ServiceEvent();

    public void add(Reservation r) {
        if (!serviceEvent.hasPlacesDisponibles(r.getEventId())) {
            System.out.println("Plus de places disponibles !");
            return;
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
        } catch (SQLException ex) {
            System.err.println("ServiceReservation.add: " + ex.getMessage());
        }
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
        r.setStatut("ANNULEE");
        update(r);
        serviceEvent.incrementerPlaces(r.getEventId());
    }
}