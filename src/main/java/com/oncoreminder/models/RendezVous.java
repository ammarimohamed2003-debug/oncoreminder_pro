package com.oncoreminder.models;

import java.time.LocalDateTime;

public class RendezVous {

    public enum Statut { EN_ATTENTE, ACCEPTE, REFUSE }

    private int id;
    private int patientId;
    private Integer medecinId;
    private LocalDateTime dateRdv;
    private String lieu;
    private String notes;
    private Statut statut;
    private String patientNom;  // from JOIN, not persisted
    private String medecinNom;  // from JOIN, not persisted

    public RendezVous() { this.statut = Statut.EN_ATTENTE; }

    /** Patient creates a request without doctor */
    public RendezVous(int patientId, LocalDateTime dateRdv, String lieu, String notes) {
        this.patientId = patientId;
        this.dateRdv = dateRdv;
        this.lieu = lieu;
        this.notes = notes;
        this.statut = Statut.EN_ATTENTE;
    }

    /** Patient creates a request targeting a specific doctor */
    public RendezVous(int patientId, Integer medecinId, LocalDateTime dateRdv, String lieu, String notes) {
        this.patientId = patientId;
        this.medecinId = medecinId;
        this.dateRdv = dateRdv;
        this.lieu = lieu;
        this.notes = notes;
        this.statut = Statut.EN_ATTENTE;
    }

    /** Used when loading from DB */
    public RendezVous(int id, int patientId, Integer medecinId, LocalDateTime dateRdv,
                      String lieu, String notes, Statut statut) {
        this.id = id;
        this.patientId = patientId;
        this.medecinId = medecinId;
        this.dateRdv = dateRdv;
        this.lieu = lieu;
        this.notes = notes;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public Integer getMedecinId() { return medecinId; }
    public void setMedecinId(Integer medecinId) { this.medecinId = medecinId; }

    public LocalDateTime getDateRdv() { return dateRdv; }
    public void setDateRdv(LocalDateTime dateRdv) { this.dateRdv = dateRdv; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public String getPatientNom() { return patientNom == null ? "" : patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getMedecinNom() { return medecinNom == null ? "" : medecinNom; }
    public void setMedecinNom(String medecinNom) { this.medecinNom = medecinNom; }

    public String getStatutLabel() {
        if (statut == null) return "EN ATTENTE";
        return switch (statut) {
            case EN_ATTENTE -> "⏳ En attente";
            case ACCEPTE    -> "✅ Accepté";
            case REFUSE     -> "❌ Refusé";
        };
    }

    @Override
    public String toString() {
        return dateRdv != null
                ? dateRdv.toLocalDate() + " " + dateRdv.toLocalTime().toString().substring(0, 5)
                  + (lieu != null ? " — " + lieu : "")
                : "Rendez-vous #" + id;
    }
}