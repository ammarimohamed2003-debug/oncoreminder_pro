package models;

import java.time.LocalDateTime;

public class Reservation {

    private int id;
    private int eventId;
    private int userId;
    private LocalDateTime dateReservation;
    private String statut;

    public Reservation() {}

    public Reservation(int eventId, int userId) {
        this.eventId = eventId;
        this.userId = userId;
        this.dateReservation = LocalDateTime.now();
        this.statut = "CONFIRMEE";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", eventId=" + eventId + ", userId=" + userId + ", statut=" + statut + "}";
    }
}
