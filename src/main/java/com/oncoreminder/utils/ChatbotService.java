package com.oncoreminder.utils;

import java.util.List;

public class ChatbotService {

    private record Rule(String[] keywords, String response) {}

    // ── Doctor rules ──────────────────────────────────────────────────────────

    private static final List<Rule> DOCTOR_RULES = List.of(

        new Rule(new String[]{"bonjour", "salut", "hello", "bonsoir", "bj"},
            "Bonjour Docteur ! 👋 Je suis votre assistant OncoReminder.\n\n"
          + "Je peux vous guider sur toutes les fonctionnalités. Cliquez sur un sujet ci-dessous ou posez votre question directement."),

        new Rule(new String[]{"article", "articles", "publier", "rédiger", "écrire", "créer article"},
            "📰 **Mes Articles**\n\n"
          + "1. Cliquez « Mes Articles » dans la barre latérale\n"
          + "2. Bouton « + Nouvel Article » en haut à droite\n"
          + "3. Remplissez titre, contenu, catégorie et image\n"
          + "4. Publiez ou enregistrez en brouillon\n\n"
          + "Vous pouvez **modifier** et **supprimer** vos articles depuis la liste.\n"
          + "Les patients peuvent commenter et évaluer vos articles."),

        new Rule(new String[]{"calendrier", "agenda", "planning", "calendar"},
            "📆 **Calendrier**\n\n"
          + "Le calendrier affiche vos rendez-vous et événements du mois :\n\n"
          + "• Point **vert** = RDV accepté\n"
          + "• Point **orange** = RDV en attente\n"
          + "• Point **violet** = événement\n\n"
          + "**Navigation :** utilisez ‹ › pour changer de mois, « Aujourd'hui » pour revenir.\n"
          + "**Détail :** cliquez sur un jour pour voir les éléments de la journée."),

        new Rule(new String[]{"rendez-vous", "rdv", "consultation", "rendezvous", "accepter", "refuser"},
            "🗓 **Rendez-vous**\n\n"
          + "Dans la page « Rendez-vous » :\n"
          + "• Consultez toutes les demandes patients\n"
          + "• **Accepter** → RDV confirmé et visible dans le Calendrier\n"
          + "• **Refuser** → demande rejetée\n"
          + "• Filtrez par statut : En attente / Accepté / Refusé\n\n"
          + "💡 Les RDV acceptés apparaissent automatiquement dans votre Calendrier."),

        new Rule(new String[]{"patient", "patients", "dossier", "malade", "mes patients"},
            "👥 **Mes Patients**\n\n"
          + "La liste affiche vos patients (basés sur les RDV acceptés) :\n\n"
          + "• Cliquez sur un patient → dossier médical complet\n"
          + "• **Modifier** : groupe sanguin, poids, taille, antécédents, allergies, traitements, notes\n"
          + "• **Historique** : tous les rendez-vous passés\n"
          + "• **Supprimer** : avec confirmation\n\n"
          + "Les patients sont triés par date du dernier rendez-vous."),

        new Rule(new String[]{"événement", "evenement", "événements", "evenements", "event", "créer event"},
            "📅 **Événements**\n\n"
          + "Créez et gérez des événements médicaux :\n"
          + "1. Allez dans « Événements » via la barre latérale\n"
          + "2. Ajoutez titre, description, date, lieu et capacité\n"
          + "3. Consultez les **réservations** des participants\n\n"
          + "💡 Les événements apparaissent aussi dans votre Calendrier."),

        new Rule(new String[]{"ordonnance", "ordonnances", "prescription", "médicament"},
            "📋 **Ordonnances**\n\n"
          + "Pour créer une ordonnance :\n"
          + "1. Cliquez « Ordonnances » dans la barre latérale\n"
          + "2. Sélectionnez un patient parmi les RDV acceptés\n"
          + "3. Rédigez les médicaments et la posologie\n"
          + "4. Cliquez « Enregistrer »\n\n"
          + "Vous pouvez consulter et supprimer les ordonnances existantes."),

        new Rule(new String[]{"profil", "compte", "informations", "modifier profil", "spécialité", "hôpital"},
            "👤 **Mon Profil**\n\n"
          + "Dans l'Espace Médecin (tableau de bord) :\n"
          + "• Onglet **Mon Profil** → nom, spécialité, hôpital, adresse, téléphone\n"
          + "• Onglet **Sécurité** → changement de mot de passe\n\n"
          + "N'oubliez pas de cliquer « 💾 Enregistrer » après vos modifications."),

        new Rule(new String[]{"mot de passe", "password", "mdp", "sécurité", "connexion"},
            "🔐 **Mot de passe**\n\n"
          + "1. Allez dans l'Espace Médecin (Mon Espace)\n"
          + "2. Cliquez l'onglet « 🔐 Sécurité »\n"
          + "3. Entrez votre mot de passe actuel\n"
          + "4. Entrez et confirmez le nouveau\n"
          + "5. Cliquez « Changer le mot de passe »\n\n"
          + "Cochez « Afficher » pour voir les mots de passe saisis."),

        new Rule(new String[]{"déconnexion", "logout", "quitter", "sortir", "déconnecter"},
            "🔓 **Déconnexion**\n\n"
          + "Cliquez sur « 🔓 Déconnexion » en bas de la barre latérale gauche.\n\n"
          + "Votre session sera fermée et vous serez redirigé vers la page de connexion."),

        new Rule(new String[]{"aide", "help", "que faire", "comment", "navigation", "menu"},
            "🤖 **Que puis-je faire ?**\n\n"
          + "• 📰 **Articles** — créer, modifier, publier\n"
          + "• 📆 **Calendrier** — voir RDV et événements\n"
          + "• 🗓 **Rendez-vous** — accepter/refuser les demandes\n"
          + "• 👥 **Patients** — dossiers médicaux\n"
          + "• 📅 **Événements** — créer et gérer\n"
          + "• 📋 **Ordonnances** — rédiger et consulter\n"
          + "• 👤 **Profil** — modifier vos informations\n\n"
          + "Posez votre question ou cliquez un sujet !")
    );

    // ── Patient rules ─────────────────────────────────────────────────────────

    private static final List<Rule> PATIENT_RULES = List.of(

        new Rule(new String[]{"bonjour", "salut", "hello", "bonsoir", "bj"},
            "Bonjour ! 👋 Je suis votre assistant OncoReminder.\n\n"
          + "Je peux vous guider dans l'application. Cliquez sur un sujet ou posez votre question."),

        new Rule(new String[]{"rendez-vous", "rdv", "consultation", "prendre rendez", "médecin", "docteur"},
            "🗓 **Prendre un Rendez-vous**\n\n"
          + "1. Cliquez « Rendez-vous » dans la barre latérale\n"
          + "2. Choisissez un médecin dans la liste\n"
          + "3. Sélectionnez date, heure et lieu\n"
          + "4. Ajoutez des notes si nécessaire\n"
          + "5. Cliquez « Confirmer le rendez-vous »\n\n"
          + "**Suivi des statuts :**\n"
          + "• ⏳ En attente → le médecin répond\n"
          + "• ✅ Accepté → RDV confirmé\n"
          + "• ❌ Refusé → prenez un autre RDV"),

        new Rule(new String[]{"article", "articles", "lire", "information", "actualité", "santé"},
            "📰 **Articles Médicaux**\n\n"
          + "Dans la section « Articles » :\n"
          + "• Parcourez les articles des médecins spécialistes\n"
          + "• Filtrez par **catégorie** (type de cancer/organe)\n"
          + "• Recherchez par **mot-clé** dans la barre de recherche\n"
          + "• Cliquez un article pour le lire en détail\n"
          + "• Laissez un **commentaire** et **évaluez** l'article\n\n"
          + "Les articles sont paginés pour une navigation facile."),

        new Rule(new String[]{"événement", "evenement", "événements", "evenements", "inscription", "réservation", "réserver"},
            "📅 **Événements**\n\n"
          + "Pour participer à un événement :\n"
          + "1. Allez dans « Événements » dans la barre latérale\n"
          + "2. Consultez les événements disponibles\n"
          + "3. Vérifiez date, lieu et places restantes\n"
          + "4. Cliquez « Réserver » pour vous inscrire\n"
          + "5. Annulez depuis « Mes réservations » si besoin\n\n"
          + "⚠️ Les places sont limitées — réservez rapidement !"),

        new Rule(new String[]{"profil", "compte", "informations", "dossier médical", "données", "modifier"},
            "👤 **Mon Profil**\n\n"
          + "Dans votre tableau de bord :\n"
          + "• Onglet **Mon Profil** → nom, email, téléphone, date de naissance\n"
          + "• Onglet **Dossier Médical** → groupe sanguin, antécédents, allergies, traitements en cours\n"
          + "• Onglet **Sécurité** → changement de mot de passe\n\n"
          + "💡 Ces informations sont accessibles par votre médecin."),

        new Rule(new String[]{"mot de passe", "password", "mdp", "sécurité"},
            "🔐 **Changer le mot de passe**\n\n"
          + "1. Allez dans votre tableau de bord\n"
          + "2. Onglet « 🔐 Sécurité »\n"
          + "3. Entrez l'ancien puis le nouveau mot de passe\n"
          + "4. Confirmez et cliquez « Changer »"),

        new Rule(new String[]{"déconnexion", "logout", "quitter", "sortir"},
            "🔓 **Déconnexion**\n\n"
          + "Cliquez « 🔓 Déconnexion » en bas de la barre latérale pour fermer votre session."),

        new Rule(new String[]{"aide", "help", "comment", "navigation", "menu", "que faire"},
            "🤖 **Comment puis-je vous aider ?**\n\n"
          + "• 🗓 **Rendez-vous** — prendre et suivre\n"
          + "• 📰 **Articles** — lire les actualités médicales\n"
          + "• 📅 **Événements** — s'inscrire à des événements\n"
          + "• 👤 **Profil** — modifier vos informations\n"
          + "• 🔐 **Mot de passe** — changer votre mot de passe\n\n"
          + "Posez votre question ou cliquez un sujet !")
    );

    private static final String DEFAULT =
        "🤔 Je n'ai pas bien compris. Voici quelques sujets sur lesquels je peux vous aider :\n\n"
      + "Tapez un mot comme : **rendez-vous**, **articles**, **calendrier**, **patients**, **événements**, **profil**, **ordonnances**, **aide**";

    // ── Public API ────────────────────────────────────────────────────────────

    public static String getWelcome(String role) {
        return "MEDECIN".equals(role)
            ? "👨‍⚕️ Bonjour Docteur ! Je suis votre assistant OncoReminder.\n\nJe peux vous guider dans l'application. Cliquez sur un sujet ou posez votre question :"
            : "😊 Bonjour ! Je suis votre assistant OncoReminder.\n\nJe peux vous guider dans l'application. Cliquez sur un sujet ou posez votre question :";
    }

    public static List<String> getQuickReplies(String role) {
        return "MEDECIN".equals(role)
            ? List.of("📆 Calendrier", "🗓 Rendez-vous", "👥 Patients", "📰 Articles", "📋 Ordonnances")
            : List.of("🗓 Rendez-vous", "📰 Articles", "📅 Événements", "👤 Mon Profil", "🆘 Aide");
    }

    public static String getResponse(String input, String role) {
        if (input == null || input.isBlank()) return DEFAULT;
        String lower = input.toLowerCase().trim();
        List<Rule> rules = "MEDECIN".equals(role) ? DOCTOR_RULES : PATIENT_RULES;
        for (Rule rule : rules)
            for (String kw : rule.keywords())
                if (lower.contains(kw)) return rule.response();
        return DEFAULT;
    }
}
