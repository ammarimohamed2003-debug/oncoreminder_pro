package com.oncoreminder.models;

import java.time.LocalDateTime;

public class Event {

    private int id;
    private String titre;
    private String description;
    private LocalDateTime dateEvent;
    private String lieu;
    private int capacite;
    private int placesRestantes;
    private String imagePath;

    public Event() {}

    public Event(String titre, String description, LocalDateTime dateEvent, String lieu, int capacite) {
        this.titre = titre;
        this.description = description;
        this.dateEvent = dateEvent;
        this.lieu = lieu;
        this.capacite = capacite;
        this.placesRestantes = capacite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateEvent() { return dateEvent; }
    public void setDateEvent(LocalDateTime dateEvent) { this.dateEvent = dateEvent; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public int getPlacesRestantes() { return placesRestantes; }
    public void setPlacesRestantes(int placesRestantes) { this.placesRestantes = placesRestantes; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return titre + " — " + lieu + " (" + placesRestantes + " places)";
    }
}