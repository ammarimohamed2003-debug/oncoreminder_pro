CREATE TABLE IF NOT EXISTS `utilisateur` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL,
  `prenom` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL DEFAULT 'PATIENT',
  `antecedents` text DEFAULT NULL,
  `allergies` text DEFAULT NULL,
  `groupe_sanguin` varchar(10) DEFAULT NULL,
  `poids` double DEFAULT NULL,
  `taille` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQ_EMAIL` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `log_connexions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `date_connexion` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_LOG_USER` FOREIGN KEY (`user_id`) REFERENCES `utilisateur` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insertion d'un admin par défaut (password: 123456)
-- Password hash for '123456' using BCrypt
INSERT INTO `utilisateur` (`nom`, `prenom`, `email`, `password`, `role`) 
VALUES ('Admin', 'System', 'admin@oncoreminder.com', '$2a$10$vI8A.NfRzWqB6M.K9Q0lO.M1A0S3Z.K7vB1M1A0S3Z.K7vB1M1A0', 'ADMIN')
ON DUPLICATE KEY UPDATE role='ADMIN';
