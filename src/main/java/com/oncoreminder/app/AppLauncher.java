package com.oncoreminder.app;

import com.oncoreminder.utils.MyDataBase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLauncher {

    public static void main(String[] args) {
        configureSystemProperties();
        configureUncaughtExceptionHandler();
        checkDatabaseConnection();
        App.main(args);
    }

    // ── Propriétés système JavaFX ─────────────────────────────────────

    private static void configureSystemProperties() {
        // Rendu hardware accéléré + anti-aliasing texte
        System.setProperty("prism.lcdtext",      "false");
        System.setProperty("prism.text",         "t2k");
        System.setProperty("javafx.animation.fullspeed", "false");

        log("OncoReminder Pro — démarrage");
        log("JVM   : " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");
        log("OS    : " + System.getProperty("os.name")
                + " " + System.getProperty("os.version"));
    }

    // ── Gestionnaire d'exceptions non capturées ───────────────────────

    private static void configureUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("[ERREUR NON CAPTURÉE] Thread : " + thread.getName());
            System.err.println("[ERREUR NON CAPTURÉE] " + throwable.getClass().getSimpleName()
                    + " : " + throwable.getMessage());
            throwable.printStackTrace(System.err);
        });
    }

    // ── Vérification connexion base de données ────────────────────────

    private static void checkDatabaseConnection() {
        try {

            var cnx = MyDataBase.getInstance().getCnx();
            if (cnx == null || cnx.isClosed()) {
                System.err.println("[BD] Connexion indisponible — vérifiez MySQL.");
            } else {
                log("BD    : connexion établie (" + cnx.getMetaData().getURL() + ")");
            }
        } catch (Exception e) {
            System.err.println("[BD] Impossible de vérifier la connexion : " + e.getMessage());
        }
    }

    // ── Logger minimal ────────────────────────────────────────────────

    private static void log(String msg) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + time + "] " + msg);
    }
}
