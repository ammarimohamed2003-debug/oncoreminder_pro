package services;

import interfaces.IService;
import models.Reservation;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceReservation implements IService<Reservation> {

    private final ServiceEvent serviceEvent = new ServiceEvent();

    @Override
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
            System.out.println("Réservation confirmée !");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public List<Reservation> getAll() {
        List<Reservation> list = new ArrayList<>();
        String req = "SELECT * FROM reservation";
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
            System.out.println(ex.getMessage());
        }
        return list;
    }

    @Override
    public void update(Reservation r) {
        String req = "UPDATE reservation SET statut=? WHERE id=?";
        try {
            PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(req);
            ps.setString(1, r.getStatut());
            ps.setInt(2, r.getId());
            ps.executeUpdate();
            System.out.println("Réservation mise à jour !");
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void delete(Reservation r) {
        r.setStatut("ANNULEE");
        update(r);
        serviceEvent.incrementerPlaces(r.getEventId());
        System.out.println("Réservation annulée !");
    }
}
