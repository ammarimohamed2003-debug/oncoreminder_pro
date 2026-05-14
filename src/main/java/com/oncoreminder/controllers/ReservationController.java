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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReservationController {

    // Sidebar (partagée via fx:include — présente uniquement en page standalone)
    @FXML private DoctorSidebarController doctorSidebarController;

    @FXML private ComboBox<Event> cbEvent;

    @FXML private TableView<Reservation> tableReservation;
    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, String> colEvent;
    @FXML private TableColumn<Reservation, String> colLieu;
    @FXML private TableColumn<Reservation, String> colDate;
    @FXML private TableColumn<Reservation, Integer> colUserId;
    @FXML private TableColumn<Reservation, String> colStatut;

    @FXML private Label lbMessage;

    private Runnable onRetourEvents;
    public void setOnRetourEvents(Runnable r) { this.onRetourEvents = r; }

    private final ServiceReservation serviceReservation = new ServiceReservation();
    private final ServiceEvent serviceEvent = new ServiceEvent();
    private Reservation reservationSelectionnee;
    private Map<Integer, Event> eventsParId = Map.of();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    void initialize() {
        if (doctorSidebarController != null) doctorSidebarController.setActivePage("evenements");
        configurerComboEvents();
        configurerTable();
        chargerEventsDisponibles();
        chargerReservations();
    }

    @FXML
    void reserverPlace(ActionEvent e) {
        Event eventChoisi = cbEvent.getValue();
        if (eventChoisi == null) {
            lbMessage.setText("Selectionnez un evenement disponible.");
            return;
        }
        var user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
            lbMessage.setText("Aucun utilisateur connecte.");
            return;
        }

        Reservation reservation = new Reservation(eventChoisi.getId(), user.getId());
        boolean ok = serviceReservation.add(reservation);
        lbMessage.setText(ok
                ? "Reservation confirmee pour : " + eventChoisi.getTitre()
                : "Reservation impossible : evenement complet ou deja reserve.");
        chargerEventsDisponibles();
        chargerReservations();
    }

    @FXML
    void annulerReservation(ActionEvent e) {
        Reservation sel = tableReservation.getSelectionModel().getSelectedItem();
        if (sel == null) sel = reservationSelectionnee;
        if (sel == null) {
            lbMessage.setText("Selectionnez une reservation a annuler.");
            return;
        }
        serviceReservation.delete(sel);
        reservationSelectionnee = null;
        tableReservation.getSelectionModel().clearSelection();
        lbMessage.setText("Reservation annulee.");
        chargerEventsDisponibles();
        chargerReservations();
    }

    @FXML void retourEvents(ActionEvent e) {
        if (onRetourEvents != null) {
            onRetourEvents.run();
            return;
        }
        var user = UserSession.getInstance().getCurrentUser();
        App.navigate(user != null && "MEDECIN".equals(user.getRole()) ? "DoctorDashboard" : "PatientDashboard");
    }

    @FXML void handleDashboard(ActionEvent e) { retourEvents(e); }

    private void configurerTable() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colEvent.setCellValueFactory(c -> new SimpleStringProperty(eventOf(c.getValue()).getTitre()));
        colLieu.setCellValueFactory(c -> new SimpleStringProperty(eventOf(c.getValue()).getLieu()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(formatDate(eventOf(c.getValue()).getDateEvent())));
        colUserId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getUserId()).asObject());
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(statut);
                badge.getStyleClass().setAll("ANNULEE".equalsIgnoreCase(statut) ? "event-badge-red" : "stat-badge-success");
                setGraphic(badge);
            }
        });

        tableReservation.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            reservationSelectionnee = sel;
        });
    }

    private void chargerReservations() {
        rafraichirEventsParId();
        List<Reservation> liste = serviceReservation.getAll();
        tableReservation.setItems(FXCollections.observableArrayList(liste));
        lbMessage.setText("Total : " + liste.size() + " reservation(s)");
    }

    private void chargerEventsDisponibles() {
        rafraichirEventsParId();
        List<Event> disponibles = eventsParId.values().stream()
                .filter(ev -> ev.getPlacesRestantes() > 0)
                .filter(ev -> ev.getDateEvent() == null || !ev.getDateEvent().isBefore(LocalDateTime.now()))
                .sorted((a, b) -> {
                    if (a.getDateEvent() == null) return 1;
                    if (b.getDateEvent() == null) return -1;
                    return a.getDateEvent().compareTo(b.getDateEvent());
                })
                .toList();
        cbEvent.setItems(FXCollections.observableArrayList(disponibles));
        cbEvent.setValue(null);
    }

    private void rafraichirEventsParId() {
        eventsParId = serviceEvent.getAll().stream()
                .collect(Collectors.toMap(Event::getId, Function.identity(), (a, b) -> a));
    }

    private Event eventOf(Reservation reservation) {
        Event event = eventsParId.get(reservation.getEventId());
        if (event != null) return event;

        Event fallback = new Event();
        fallback.setId(reservation.getEventId());
        fallback.setTitre("Evenement #" + reservation.getEventId());
        fallback.setLieu("-");
        fallback.setDateEvent(null);
        return fallback;
    }

    private void configurerComboEvents() {
        cbEvent.setConverter(new StringConverter<>() {
            @Override public String toString(Event ev) {
                if (ev == null) return "";
                return ev.getTitre()
                        + " - " + formatDate(ev.getDateEvent())
                        + " (" + ev.getPlacesRestantes() + " places)";
            }

            @Override public Event fromString(String string) {
                return null;
            }
        });
        cbEvent.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Event ev, boolean empty) {
                super.updateItem(ev, empty);
                setText(empty || ev == null ? null : cbEvent.getConverter().toString(ev));
            }
        });
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }
}
