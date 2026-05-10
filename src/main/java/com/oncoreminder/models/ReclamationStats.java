package com.oncoreminder.models;

public class ReclamationStats {

    private int total;
    private int enCours;
    private int traitees;
    private int fermees;

    public ReclamationStats(int total, int enCours, int traitees, int fermees) {
        this.total    = total;
        this.enCours  = enCours;
        this.traitees = traitees;
        this.fermees  = fermees;
    }

    public int    getTotal()    { return total; }
    public int    getEnCours()  { return enCours; }
    public int    getTraitees() { return traitees; }
    public int    getFermees()  { return fermees; }

    /** Taux de traitement = (traitées + fermées) / total × 100 */
    public double getTauxTraitement() {
        if (total == 0) return 0.0;
        return ((double)(traitees + fermees) / total) * 100.0;
    }

    @Override
    public String toString() {
        return String.format(
            "Total: %d | EN_COURS: %d | TRAITEE: %d | FERMEE: %d | Taux: %.1f%%",
            total, enCours, traitees, fermees, getTauxTraitement()
        );
    }
}
