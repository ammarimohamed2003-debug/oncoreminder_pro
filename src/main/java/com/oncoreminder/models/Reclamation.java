package com.oncoreminder.models;

import java.time.LocalDateTime;

public class Reclamation {

    private int           id;
    private int           utilisateurId;
    private String        sujet;
    private String        message;
    private LocalDateTime dateReclamation;
    private String        statut;
    private String        utilisateurNomComplet;
    private Integer       note;

    public Reclamation() {}

    public Reclamation(int utilisateurId, String sujet, String message) {
        this.utilisateurId = utilisateurId;
        this.sujet         = sujet;
        this.message       = message;
        this.statut        = "EN_COURS";
    }

    public int           getId()                    { return id; }
    public void          setId(int id)              { this.id = id; }
    public int           getUtilisateurId()         { return utilisateurId; }
    public void          setUtilisateurId(int v)    { this.utilisateurId = v; }
    public String        getSujet()                 { return sujet; }
    public void          setSujet(String v)         { this.sujet = v; }
    public String        getMessage()               { return message; }
    public void          setMessage(String v)       { this.message = v; }
    public LocalDateTime getDateReclamation()       { return dateReclamation; }
    public void          setDateReclamation(LocalDateTime v) { this.dateReclamation = v; }
    public String        getStatut()                { return statut; }
    public void          setStatut(String v)        { this.statut = v; }
    public String        getUtilisateurNomComplet() { return utilisateurNomComplet; }
    public void          setUtilisateurNomComplet(String v) { this.utilisateurNomComplet = v; }
    public Integer       getNote()                 { return note; }
    public void          setNote(Integer v)        { this.note = v; }
}