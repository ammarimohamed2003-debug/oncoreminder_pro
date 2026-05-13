package com.onco.model;

import java.time.LocalDateTime;

public class RendezVous {

    private int id;
    private int traitementId;    // FK → traitement
    private LocalDateTime dateRdv;
    private String lieu;         // nullable
    private String notes;        // nullable

    // ── Constructors ────────────────────────────────────────────
    public RendezVous() {}

    /** Used when loading from DB (id known) */
    public RendezVous(int id, int traitementId, LocalDateTime dateRdv, String lieu, String notes) {
        this.id = id;
        this.traitementId = traitementId;
        this.dateRdv = dateRdv;
        this.lieu = lieu;
        this.notes = notes;
    }

    /** Used when inserting a new record (id auto-generated) */
    public RendezVous(int traitementId, LocalDateTime dateRdv, String lieu, String notes) {
        this.traitementId = traitementId;
        this.dateRdv = dateRdv;
        this.lieu = lieu;
        this.notes = notes;
    }

    // ── Getters & Setters ────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTraitementId() { return traitementId; }
    public void setTraitementId(int traitementId) { this.traitementId = traitementId; }

    public LocalDateTime getDateRdv() { return dateRdv; }
    public void setDateRdv(LocalDateTime dateRdv) { this.dateRdv = dateRdv; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "RendezVous{id=" + id +
                ", traitementId=" + traitementId +
                ", dateRdv=" + dateRdv +
                ", lieu='" + lieu + '\'' +
                ", notes='" + notes + '\'' + '}';
    }
}