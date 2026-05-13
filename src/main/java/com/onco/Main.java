package com.onco;

import com.onco.dao.OrdonnanceDAO;
import com.onco.dao.RendezVousDAO;
import com.onco.model.Ordonnance;
import com.onco.model.RendezVous;
import com.onco.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        RendezVousDAO rvDAO  = new RendezVousDAO();
        OrdonnanceDAO ordDAO = new OrdonnanceDAO();

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("   TEST MODULE RENDEZ-VOUS + ORDONNANCE        ");
        System.out.println("═══════════════════════════════════════════════");

        // ── 0. Récupérer un traitement_id existant dans la DB ────
        int traitementId = getFirstTraitementId();
        if (traitementId == -1) {
            System.out.println("❌ Aucun traitement trouvé dans la DB.");
            System.out.println("   → Insère d'abord un traitement dans la table 'traitement'.");
            System.out.println("   → SQL rapide : INSERT INTO traitement (patient_id) VALUES (1);");
            System.out.println("     (adapte les colonnes selon ta table)");
            return;
        }
        System.out.println("✅ traitement_id utilisé pour le test : " + traitementId);

        // ── 1. Ajouter un rendez-vous ────────────────────────────
        System.out.println("\n── 1. Ajout rendez-vous ──");
        RendezVous rv = new RendezVous(
                traitementId,
                LocalDateTime.of(2026, 6, 15, 10, 30),
                "Clinique El Manar, Tunis",
                "Patient à jeun"
        );
        rvDAO.ajouter(rv);
        System.out.println("Créé : " + rv);

        if (rv.getId() == 0) {
            System.out.println("❌ Arrêt : le rendez-vous n'a pas été inséré.");
            return;
        }

        // ── 2. Lire tous les rendez-vous ─────────────────────────
        System.out.println("\n── 2. Tous les rendez-vous ──");
        rvDAO.getAll().forEach(System.out::println);

        // ── 3. Rendez-vous par traitement ────────────────────────
        System.out.println("\n── 3. RDV du traitement_id=" + traitementId + " ──");
        rvDAO.getByTraitement(traitementId).forEach(System.out::println);

        // ── 4. Modifier ──────────────────────────────────────────
        System.out.println("\n── 4. Modification rendez-vous ──");
        rv.setLieu("Hôpital Charles Nicole");
        rv.setNotes("Report suite annulation");
        rv.setDateRdv(LocalDateTime.of(2026, 6, 20, 14, 0));
        rvDAO.modifier(rv);
        System.out.println("Modifié : " + rvDAO.getById(rv.getId()));

        // ── 5. Ajouter une ordonnance ────────────────────────────
        System.out.println("\n── 5. Ajout ordonnance ──");
        Ordonnance ord = new Ordonnance(
                rv.getId(),
                "Tamoxifène 20mg, Oméprazole 20mg",
                "1 comprimé matin et soir pendant le repas",
                30,
                LocalDate.now()
        );
        ordDAO.ajouter(ord);
        System.out.println("Créée : " + ord);

        // ── 6. Toutes les ordonnances ────────────────────────────
        System.out.println("\n── 6. Toutes les ordonnances ──");
        ordDAO.getAll().forEach(System.out::println);

        // ── 7. Ordonnance par rendez_vous_id ─────────────────────
        System.out.println("\n── 7. Ordonnance du RDV id=" + rv.getId() + " ──");
        System.out.println(ordDAO.getByRendezVousId(rv.getId()));

        // ── 8. Ordonnances d'un traitement (jointure) ────────────
        System.out.println("\n── 8. Ordonnances du traitement_id=" + traitementId + " ──");
        ordDAO.getOrdonnancesParTraitement(traitementId).forEach(System.out::println);

        // ── 9. Modifier ordonnance ───────────────────────────────
        System.out.println("\n── 9. Modification ordonnance ──");
        ord.setDureeJours(60);
        ord.setPosologie("1 comprimé le matin uniquement");
        ordDAO.modifier(ord);
        System.out.println("Modifiée : " + ordDAO.getById(ord.getId()));

        // ── 10. Supprimer ordonnance ─────────────────────────────
        System.out.println("\n── 10. Suppression ordonnance ──");
        ordDAO.supprimer(ord.getId());

        // ── 11. Supprimer rendez-vous ────────────────────────────
        System.out.println("\n── 11. Suppression rendez-vous ──");
        rvDAO.supprimer(rv.getId());

        System.out.println("\n═══════════════════════════════════════════════");
        System.out.println("           TESTS TERMINÉS ✅");
        System.out.println("═══════════════════════════════════════════════");
    }

    /**
     * Fetches the first available traitement id from the DB.
     * Returns -1 if the table is empty or doesn't exist.
     */
    private static int getFirstTraitementId() {
        try {
            Connection cnx = MyDataBase.getInstance().getCnx();
            // First, check what columns traitement table has
            ResultSet cols = cnx.getMetaData().getColumns(null, null, "traitement", null);
            StringBuilder colNames = new StringBuilder("Colonnes de 'traitement': ");
            while (cols.next()) colNames.append(cols.getString("COLUMN_NAME")).append(", ");
            System.out.println(colNames);

            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT id FROM traitement LIMIT 1");
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.out.println("⚠️  Erreur lecture traitement : " + e.getMessage());
        }
        return -1;
    }
}