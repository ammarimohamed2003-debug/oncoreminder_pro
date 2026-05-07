package com.oncoreminder.models;

import java.time.LocalDate;

public class Article {
    private int id;
    private String titre;
    private String contenu;
    private String statut;
    private LocalDate datePublication;
    private Integer cancerId;
    private String organe;
    private int likes;
    private int medecinId;
    private String tags;
    private int views;
    private String icdCode;

    public Article() {}

    public Article(int id, String titre, String contenu, String statut, LocalDate datePublication,
                   Integer cancerId, String organe, int likes, int medecinId) {
        this.id = id; this.titre = titre; this.contenu = contenu; this.statut = statut;
        this.datePublication = datePublication; this.cancerId = cancerId;
        this.organe = organe; this.likes = likes; this.medecinId = medecinId;
    }

    public Article(String titre, String contenu, String statut, String organe, int medecinId) {
        this.titre = titre; this.contenu = contenu; this.statut = statut;
        this.organe = organe; this.medecinId = medecinId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDate getDatePublication() { return datePublication; }
    public void setDatePublication(LocalDate d) { this.datePublication = d; }
    public Integer getCancerId() { return cancerId; }
    public void setCancerId(Integer cancerId) { this.cancerId = cancerId; }
    public String getOrgane() { return organe; }
    public void setOrgane(String organe) { this.organe = organe; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    public String getIcdCode() { return icdCode; }
    public void setIcdCode(String icdCode) { this.icdCode = icdCode; }

    @Override
    public String toString() { return titre + " [" + statut + "]"; }
}
