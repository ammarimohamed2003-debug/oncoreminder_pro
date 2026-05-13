package com.oncoreminder.models;

import java.time.LocalDateTime;

public class Reponse {

    private int           id;
    private int           reclamationId;
    private int           administrateurId;
    private String        message;
    private LocalDateTime dateReponse;
    private String        adminNomComplet;

    public Reponse() {}

    public Reponse(int reclamationId, int administrateurId, String message) {
        this.reclamationId    = reclamationId;
        this.administrateurId = administrateurId;
        this.message          = message;
    }

    public int           getId()                { return id; }
    public void          setId(int id)          { this.id = id; }
    public int           getReclamationId()     { return reclamationId; }
    public void          setReclamationId(int v){ this.reclamationId = v; }
    public int           getAdministrateurId()  { return administrateurId; }
    public void          setAdministrateurId(int v) { this.administrateurId = v; }
    public String        getMessage()           { return message; }
    public void          setMessage(String v)   { this.message = v; }
    public LocalDateTime getDateReponse()       { return dateReponse; }
    public void          setDateReponse(LocalDateTime v) { this.dateReponse = v; }
    public String        getAdminNomComplet()   { return adminNomComplet; }
    public void          setAdminNomComplet(String v) { this.adminNomComplet = v; }
}
