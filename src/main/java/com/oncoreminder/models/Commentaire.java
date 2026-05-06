package com.oncoreminder.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Commentaire {
    private int id;
    private int articleId;
    private String contenu;
    private String auteur;
    private LocalDateTime dateCommentaire;

    public Commentaire() {}

    public Commentaire(int id, int articleId, String contenu, String auteur, LocalDateTime dateCommentaire) {
        this.id = id;
        this.articleId = articleId;
        this.contenu = contenu;
        this.auteur = auteur;
        this.dateCommentaire = dateCommentaire;
    }

    public Commentaire(int articleId, String contenu, String auteur) {
        this.articleId = articleId;
        this.contenu = contenu;
        this.auteur = auteur;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getArticleId() { return articleId; }
    public void setArticleId(int articleId) { this.articleId = articleId; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getAuteur() { return auteur; }
    public void setAuteur(String auteur) { this.auteur = auteur; }
    public LocalDateTime getDateCommentaire() { return dateCommentaire; }
    public void setDateCommentaire(LocalDateTime dateCommentaire) { this.dateCommentaire = dateCommentaire; }

    public String getFormattedDate() {
        if (dateCommentaire == null) return "";
        return dateCommentaire.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
