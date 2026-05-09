package com.oncoreminder.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private int id;
    private int articleId;
    private int expediteurId;
    private int destinataireId;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private boolean lu;
    private String expediteurNom; // transient — populated via JOIN

    public Message() {}

    public Message(int articleId, int expediteurId, int destinataireId, String contenu) {
        this.articleId = articleId;
        this.expediteurId = expediteurId;
        this.destinataireId = destinataireId;
        this.contenu = contenu;
    }

    public int getId()                       { return id; }
    public void setId(int id)                { this.id = id; }
    public int getArticleId()                { return articleId; }
    public void setArticleId(int v)          { this.articleId = v; }
    public int getExpediteurId()             { return expediteurId; }
    public void setExpediteurId(int v)       { this.expediteurId = v; }
    public int getDestinataire()             { return destinataireId; }
    public void setDestinataire(int v)       { this.destinataireId = v; }
    public String getContenu()               { return contenu; }
    public void setContenu(String v)         { this.contenu = v; }
    public LocalDateTime getDateEnvoi()      { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime v){ this.dateEnvoi = v; }
    public boolean isLu()                    { return lu; }
    public void setLu(boolean v)             { this.lu = v; }
    public String getExpediteurNom()         { return expediteurNom; }
    public void setExpediteurNom(String v)   { this.expediteurNom = v; }

    public String getFormattedDate() {
        if (dateEnvoi == null) return "";
        return dateEnvoi.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }
}
