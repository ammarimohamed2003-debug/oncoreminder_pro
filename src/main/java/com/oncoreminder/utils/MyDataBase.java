package com.oncoreminder.utils;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDataBase {

    private static MyDataBase instance;
    private final String URL = "jdbc:mysql://127.0.0.1:3306/oncoreminder?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private final String USERNAME = "root";
    private final String PASSWORD = "123456"; // Please verify if this is your correct password
    private Connection cnx;

    private MyDataBase() {
        System.out.println("Tentative de connexion avec l'URL : " + URL);
        try {
            // Try with password '123456'
            cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion à oncoreminder réussie (avec mot de passe) !");
            initializeTables();
        } catch (SQLException e1) {
            try {
                // Try with empty password (common default)
                cnx = DriverManager.getConnection(URL, USERNAME, "");
                System.out.println("Connexion à oncoreminder réussie (sans mot de passe) !");
                initializeTables();
            } catch (SQLException e2) {
                System.err.println("CRITICAL: Erreur de connexion à la base de données !");
                System.err.println("Dernière erreur : " + e2.getMessage());
                System.err.println("Vérifiez que MySQL est lancé et que le mot de passe pour 'root' est soit '123456' soit vide.");
            }
        }
    }

    private void initializeTables() {
        System.out.println("Initialisation des tables en cours...");
        String[] queries = {
            "CREATE TABLE IF NOT EXISTS `utilisateur` (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT," +
            "  `nom` varchar(255) NOT NULL," +
            "  `prenom` varchar(255) NOT NULL," +
            "  `email` varchar(255) NOT NULL," +
            "  `password` varchar(255) NOT NULL," +
            "  `role` varchar(50) NOT NULL DEFAULT 'PATIENT'," +
            "  `antecedents` text DEFAULT NULL," +
            "  `allergies` text DEFAULT NULL," +
            "  `groupe_sanguin` varchar(10) DEFAULT NULL," +
            "  `poids` double DEFAULT NULL," +
            "  `taille` double DEFAULT NULL," +
            "  `traitements` text DEFAULT NULL," +
            "  `notes` text DEFAULT NULL," +
            "  `medecin_id` int(11) DEFAULT NULL," +
            "  PRIMARY KEY (`id`)," +
            "  UNIQUE KEY `UNIQ_EMAIL` (`email`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",

            "CREATE TABLE IF NOT EXISTS `log_connexions` (" +
            "  `id` int(11) NOT NULL AUTO_INCREMENT," +
            "  `user_id` int(11) NOT NULL," +
            "  `date_connexion` datetime DEFAULT CURRENT_TIMESTAMP," +
            "  PRIMARY KEY (`id`)," +
            "  CONSTRAINT `FK_LOG_USER` FOREIGN KEY (`user_id`) REFERENCES `utilisateur` (`id`) ON DELETE CASCADE" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
        };

        try (Statement stmt = cnx.createStatement()) {
            // Temporarily disable foreign key checks to avoid initialization order issues
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            for (String query : queries) {
                stmt.execute(query);
            }

            // Ensure missing columns are added if the table already existed
            String[] alterQueries = {
                "ALTER TABLE `utilisateur` ADD COLUMN `role` varchar(50) NOT NULL DEFAULT 'PATIENT'",
                "ALTER TABLE `utilisateur` ADD COLUMN `antecedents` text DEFAULT NULL",
                "ALTER TABLE `utilisateur` ADD COLUMN `allergies` text DEFAULT NULL",
                "ALTER TABLE `utilisateur` ADD COLUMN `groupe_sanguin` varchar(10) DEFAULT NULL",
                "ALTER TABLE `utilisateur` ADD COLUMN `poids` double DEFAULT NULL",
                "ALTER TABLE `utilisateur` ADD COLUMN `taille` double DEFAULT NULL",
                "ALTER TABLE `utilisateur` ADD COLUMN `traitements` text DEFAULT NULL",
                "ALTER TABLE `utilisateur` ADD COLUMN `notes` text DEFAULT NULL",
                "ALTER TABLE `utilisateur` ADD COLUMN `medecin_id` int(11) DEFAULT NULL"
            };

            String[] articleAlters = {
                "ALTER TABLE `article` ADD COLUMN `tags` VARCHAR(500) DEFAULT NULL",
                "ALTER TABLE `article` ADD COLUMN `views` INT DEFAULT 0",
                "ALTER TABLE `article` ADD COLUMN `icd_code` VARCHAR(20) DEFAULT NULL",
                "ALTER TABLE `article` ADD COLUMN `image_path` VARCHAR(500) DEFAULT NULL",
                "ALTER TABLE `commentaire` ADD COLUMN `likes` INT DEFAULT 0"
            };
            for (String alter : articleAlters) {
                try { stmt.execute(alter); } catch (SQLException ignored) {}
            }

            for (String alter : alterQueries) {
                try {
                    // Note: IF NOT EXISTS for columns requires MySQL 8.0.19+ or MariaDB 10.2.2+
                    // If your version is older, we'll catch the 'duplicate column' error
                    stmt.execute(alter);
                } catch (SQLException e) {
                    // Ignore error if column already exists
                }
            }
            
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            // Ensure admin exists with correct hash
            String checkAdmin = "SELECT id FROM utilisateur WHERE email = 'admin@oncoreminder.com'";
            var rs = stmt.executeQuery(checkAdmin);
            String adminPass = BCrypt.hashpw("123456", BCrypt.gensalt());
            if (!rs.next()) {
                String insertAdmin = "INSERT INTO utilisateur (nom, prenom, email, password, role) " +
                                   "VALUES ('Admin', 'System', 'admin@oncoreminder.com', '" + adminPass + "', 'ADMIN')";
                stmt.execute(insertAdmin);
                System.out.println("Admin par défaut créé !");
            } else {
                // Force update password to 123456 in case it was wrong
                String updateAdmin = "UPDATE utilisateur SET password = '" + adminPass + "' WHERE email = 'admin@oncoreminder.com'";
                stmt.execute(updateAdmin);
                System.out.println("Mot de passe Admin réinitialisé !");
            }
            
            System.out.println("Tables initialisées avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation des tables : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                try {
                    cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                } catch (SQLException e) {
                    cnx = DriverManager.getConnection(URL, USERNAME, "");
                }
                if (cnx != null) {
                    System.out.println("Reconnexion à oncoreminder réussie !");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de reconnexion : " + e.getMessage());
        }
        return cnx;
    }
}
