package controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import models.Event;
import models.Reservation;
import services.ServiceEvent;
import services.ServiceReservation;

import java.io.IOException;
import java.util.List;

public class ReservationController {

    @FXML private ComboBox<Event> cbEvent;
    @FXML private TextField tfUserId;

    @FXML private TableView<Reservation> tableReservation;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, Integer> colEventId;
    @FXML private TableColumn<Reservation, Integer> colUserId;
    @FXML private TableColumn<Reservation, String>  colStatut;

    @FXML private Label lbMessage;

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
        if (eventChoisi == null) { lbMessage.setText("Selectionnez un evenement."); return; }
        if (tfUserId.getText().isEmpty()) { lbMessage.setText("Entrez l'ID utilisateur."); return; }
        try {
            Reservation r = new Reservation(eventChoisi.getId(), Integer.parseInt(tfUserId.getText()));
            serviceReservation.add(r);
            lbMessage.setText("Reservation confirmee !");
            cbEvent.setItems(FXCollections.observableArrayList(serviceEvent.getAll()));
            chargerReservations();
        } catch (Exception ex) {
            lbMessage.setText("Erreur : " + ex.getMessage());
        }
    }

    @FXML
    void annulerReservation(ActionEvent e) {
        if (reservationSelectionnee == null) { lbMessage.setText("Selectionnez une reservation."); return; }
        serviceReservation.delete(reservationSelectionnee);
        lbMessage.setText("Reservation annulee !");
        cbEvent.setItems(FXCollections.observableArrayList(serviceEvent.getAll()));
        chargerReservations();
    }

    @FXML
    void retourEvents(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionEvent.fxml"));
            Parent root = loader.load();
            tfUserId.getScene().setRoot(root);
        } catch (IOException ex) {
            lbMessage.setText("Erreur navigation : " + ex.getMessage());
        }
    }

    private void chargerReservations() {
        List<Reservation> liste = serviceReservation.getAll();
        tableReservation.setItems(FXCollections.observableArrayList(liste));
        lbMessage.setText("Total : " + liste.size() + " reservation(s)");
    }
}
