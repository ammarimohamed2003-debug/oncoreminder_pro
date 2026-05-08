package com.oncoreminder.models;

public class Cancer {

    private int    id;
    private String nom;
    private String classification;
    private String stade;
    private String organe;
    private String description;
    private String symptomes;

    public Cancer() {}

    public Cancer(String nom, String classification, String stade,
                  String organe, String description, String symptomes) {
        this.nom            = nom;
        this.classification = classification;
        this.stade          = stade;
        this.organe         = organe;
        this.description    = description;
        this.symptomes      = symptomes;
    }

    public int    getId()             { return id; }
    public void   setId(int id)       { this.id = id; }

    public String getNom()            { return nom; }
    public void   setNom(String nom)  { this.nom = nom; }

    public String getClassification()                        { return classification; }
    public void   setClassification(String classification)   { this.classification = classification; }

    public String getStade()              { return stade; }
    public void   setStade(String stade)  { this.stade = stade; }

    public String getOrgane()               { return organe; }
    public void   setOrgane(String organe)  { this.organe = organe; }

    public String getDescription()                    { return description; }
    public void   setDescription(String description)  { this.description = description; }

    public String getSymptomes()                    { return symptomes; }
    public void   setSymptomes(String symptomes)    { this.symptomes = symptomes; }

    @Override
    public String toString() { return nom != null ? nom : ""; }
}
