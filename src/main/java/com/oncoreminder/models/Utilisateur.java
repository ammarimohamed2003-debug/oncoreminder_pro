package com.oncoreminder.models;

public class Utilisateur {
    private int    id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role; // ADMIN, MEDECIN, PATIENT

    // ── Dossier médical ─────────────────────────────────────────────
    private String antecedents;
    private String allergies;
    private String groupeSanguin;
    private Double poids;
    private Double taille;
    /** Traitements en cours (champ texte libre) */
    private String traitements;
    /** Notes médicales du médecin */
    private String notes;

    // ── Constructeurs ────────────────────────────────────────────────
    public Utilisateur() {}

    public Utilisateur(int id, String nom, String prenom, String email, String password, String role) {
        this.id       = id;
        this.nom      = nom;
        this.prenom   = prenom;
        this.email    = email;
        this.password = password;
        this.role     = role;
    }

    public Utilisateur(String nom, String prenom, String email, String password, String role) {
        this.nom      = nom;
        this.prenom   = prenom;
        this.email    = email;
        this.password = password;
        this.role     = role;
    }

    // ── Getters / Setters ────────────────────────────────────────────
    public int    getId()         { return id; }
    public void   setId(int id)   { this.id = id; }

    public String getNom()        { return nom; }
    public void   setNom(String nom) { this.nom = nom; }

    public String getPrenom()     { return prenom; }
    public void   setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail()      { return email; }
    public void   setEmail(String email) { this.email = email; }

    public String getPassword()   { return password; }
    public void   setPassword(String password) { this.password = password; }

    public String getRole()       { return role; }
    public void   setRole(String role) { this.role = role; }

    public String getAntecedents() { return antecedents; }
    public void   setAntecedents(String antecedents) { this.antecedents = antecedents; }

    public String getAllergies()   { return allergies; }
    public void   setAllergies(String allergies) { this.allergies = allergies; }

    public String getGroupeSanguin() { return groupeSanguin; }
    public void   setGroupeSanguin(String groupeSanguin) { this.groupeSanguin = groupeSanguin; }

    public Double getPoids()      { return poids; }
    public void   setPoids(Double poids) { this.poids = poids; }

    public Double getTaille()     { return taille; }
    public void   setTaille(Double taille) { this.taille = taille; }

    public String getTraitements() { return traitements; }
    public void   setTraitements(String traitements) { this.traitements = traitements; }

    public String getNotes()      { return notes; }
    public void   setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return nom + " " + prenom + " (" + role + ")";
    }
}
