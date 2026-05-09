package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Event;
import com.oncoreminder.models.Reservation;
import com.oncoreminder.services.ServiceEvent;
import com.oncoreminder.services.ServiceReservation;
import com.oncoreminder.utils.UserSession;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class ReservationController {

    @FXML private ComboBox<Event> cbEvent;

    @FXML private TableView<Reservation>           tableReservation;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, Integer> colEventId;
    @FXML private TableColumn<Reservation, Integer> colUserId;
    @FXML private TableColumn<Reservation, String>  colStatut;

    @FXML private Label lbMessage;

    private Runnable onRetourEvents;
    public void setOnRetourEvents(Runnable r) { this.onRetourEvents = r; }

    private final ServiceReservation serviceReservation = new ServiceReservation();
    private final ServiceEvent serviceEvent = new ServiceEvent();
    private Reservation reservationSelectionnee = null;

    @FXML
    void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colEventId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getEventId()).asObject());
        colUserId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getUserId()).asObject());
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        tableReservation.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) reservationSelectionnee = sel;
        });

        cbEvent.setItems(FXCollections.observableArrayList(serviceEvent.getAll()));
        chargerReservations();
    }

    @FXML
    void reserverPlace(ActionEvent e) {
        Event eventChoisi = cbEvent.getValue();
        if (eventChoisi == null) { lbMessage.setText("Sélectionnez un événement."); return; }
        var user = UserSession.getInstance().getCurrentUser();
        if (user == null) { lbMessage.setText("Aucun utilisateur connecté."); return; }
        try {
            Reservation r = new Reservation(eventChoisi.getId(), user.getId());
            serviceReservation.add(r);
            lbMessage.setText("✅ Réservation confirmée !");
            cbEvent.setItems(FXCollections.observableArrayList(serviceEvent.getAll()));
            chargerReservations();
        } catch (Exception ex) {
            lbMessage.setText("❌ Erreur : " + ex.getMessage());
        }
    }

    @FXML
    void annulerReservation(ActionEvent e) {
        if (reservationSelectionnee == null) { lbMessage.setText("Sélectionnez une réservation."); return; }
        serviceReservation.delete(reservationSelectionnee);
        lbMessage.setText("✅ Réservation annulée !");
        cbEvent.setItems(FXCollections.observableArrayList(serviceEvent.getAll()));
        chargerReservations();
    }

    @FXML void retourEvents(ActionEvent e) {
        if (onRetourEvents != null) { onRetourEvents.run(); return; }
        var user = UserSession.getInstance().getCurrentUser();
        App.navigate(user != null && "MEDECIN".equals(user.getRole()) ? "DoctorDashboard" : "PatientDashboard");
    }
    @FXML void handleDashboard(ActionEvent e) { retourEvents(e); }
    @FXML void handleArticles(ActionEvent e)  { App.navigate("ArticleList"); }
    @FXML void handleLogout(ActionEvent e)    { UserSession.getInstance().logout(); App.navigate("Login"); }

    private void chargerReservations() {
        List<Reservation> liste = serviceReservation.getAll();
        tableReservation.setItems(FXCollections.observableArrayList(liste));
        lbMessage.setText("Total : " + liste.size() + " réservation(s)");
    }
}