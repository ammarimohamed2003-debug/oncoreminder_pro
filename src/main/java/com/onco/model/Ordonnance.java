package com.onco.model;

import java.time.LocalDate;

public class Ordonnance {

    private int id;
    private int rendezVousId;   // FK → rendez_vous
    private String medicaments; // TEXT, not null
    private String posologie;   // varchar(300), not null
    private int dureeJours;     // int, not null
    private LocalDate dateEmission; // date, not null

    // ── Constructors ────────────────────────────────────────────
    public Ordonnance() {}

    /** Used when loading from DB (id known) */
    public Ordonnance(int id, int rendezVousId, String medicaments,
                      String posologie, int dureeJours, LocalDate dateEmission) {
        this.id = id;
        this.rendezVousId = rendezVousId;
        this.medicaments = medicaments;
        this.posologie = posologie;
        this.dureeJours = dureeJours;
        this.dateEmission = dateEmission;
    }

    /** Used when inserting a new record (id auto-generated) */
    public Ordonnance(int rendezVousId, String medicaments,
                      String posologie, int dureeJours, LocalDate dateEmission) {
        this.rendezVousId = rendezVousId;
        this.medicaments = medicaments;
        this.posologie = posologie;
        this.dureeJours = dureeJours;
        this.dateEmission = dateEmission;
    }

    // ── Getters & Setters ────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRendezVousId() { return rendezVousId; }
    public void setRendezVousId(int rendezVousId) { this.rendezVousId = rendezVousId; }

    public String getMedicaments() { return medicaments; }
    public void setMedicaments(String medicaments) { this.medicaments = medicaments; }

    public String getPosologie() { return posologie; }
    public void setPosologie(String posologie) { this.posologie = posologie; }

    public int getDureeJours() { return dureeJours; }
    public void setDureeJours(int dureeJours) { this.dureeJours = dureeJours; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    @Override
    public String toString() {
        return "Ordonnance{id=" + id +
                ", rendezVousId=" + rendezVousId +
                ", medicaments='" + medicaments + '\'' +
                ", posologie='" + posologie + '\'' +
                ", dureeJours=" + dureeJours +
                ", dateEmission=" + dateEmission + '}';
    }
}