CREATE TABLE IF NOT EXISTS `utilisateur` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nom` varchar(255) NOT NULL,
  `prenom` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL DEFAULT 'ROLE_USER',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQ_EMAIL` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insertion d'un admin par défaut (password: admin123)
INSERT INTO `utilisateur` (`nom`, `prenom`, `email`, `password`, `role`) 
VALUES ('Admin', 'System', 'admin@oncoreminder.com', 'admin123', 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE role='ROLE_ADMIN';
