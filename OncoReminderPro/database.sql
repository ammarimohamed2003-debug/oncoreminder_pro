-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Hote : 127.0.0.1:3306
-- Genere le : mar. 05 mai 2026 a 16:07
-- Version du serveur : 8.4.7
-- Version de PHP : 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

CREATE DATABASE IF NOT EXISTS `oncoreminder`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `oncoreminder`;

DROP TABLE IF EXISTS `article`;
CREATE TABLE IF NOT EXISTS `article` (
  `id` int NOT NULL AUTO_INCREMENT,
  `titre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contenu` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `auteur_id` int NOT NULL,
  `cancer_id` int DEFAULT NULL,
  `date_publication` datetime DEFAULT CURRENT_TIMESTAMP,
  `statut` enum('PUBLIE','BROUILLON','ARCHIVE') COLLATE utf8mb4_unicode_ci DEFAULT 'PUBLIE',
  PRIMARY KEY (`id`),
  KEY `auteur_id` (`auteur_id`),
  KEY `cancer_id` (`cancer_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `cancer`;
CREATE TABLE IF NOT EXISTS `cancer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `classification` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `stade` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `organe` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `cancer` (`id`, `nom`, `description`, `classification`, `stade`, `organe`) VALUES
(1, 'Cancer du sein', 'Tumeur maligne du tissu mammaire, cancer le plus frequent chez la femme.', 'Carcinome', 'I', 'Sein');

DROP TABLE IF EXISTS `commentaire`;
CREATE TABLE IF NOT EXISTS `commentaire` (
  `id` int NOT NULL AUTO_INCREMENT,
  `article_id` int NOT NULL,
  `utilisateur_id` int NOT NULL,
  `contenu` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_commentaire` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `article_id` (`article_id`),
  KEY `utilisateur_id` (`utilisateur_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `dossier_medical`;
CREATE TABLE IF NOT EXISTS `dossier_medical` (
  `id` int NOT NULL AUTO_INCREMENT,
  `utilisateur_id` int NOT NULL,
  `antecedents` text COLLATE utf8mb4_unicode_ci,
  `allergies` text COLLATE utf8mb4_unicode_ci,
  `groupe_sanguin` varchar(5) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `poids` decimal(5,2) DEFAULT NULL,
  `taille` decimal(5,2) DEFAULT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `utilisateur_id` (`utilisateur_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `event`;
CREATE TABLE IF NOT EXISTS `event` (
  `id` int NOT NULL AUTO_INCREMENT,
  `titre` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `date_event` datetime NOT NULL,
  `lieu` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `capacite_max` int DEFAULT '100',
  `places_restantes` int DEFAULT '100',
  `createur_id` int NOT NULL,
  `cancer_id` int DEFAULT NULL,
  `image_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_creation` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `createur_id` (`createur_id`),
  KEY `cancer_id` (`cancer_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `ordonnance`;
CREATE TABLE IF NOT EXISTS `ordonnance` (
  `id` int NOT NULL AUTO_INCREMENT,
  `rendez_vous_id` int NOT NULL,
  `medicament` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `posologie` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duree` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `instructions` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `rendez_vous_id` (`rendez_vous_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `reclamation`;
CREATE TABLE IF NOT EXISTS `reclamation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `utilisateur_id` int NOT NULL,
  `sujet` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_reclamation` datetime DEFAULT CURRENT_TIMESTAMP,
  `statut` enum('EN_COURS','TRAITEE','FERMEE') COLLATE utf8mb4_unicode_ci DEFAULT 'EN_COURS',
  PRIMARY KEY (`id`),
  KEY `utilisateur_id` (`utilisateur_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `rendez_vous`;
CREATE TABLE IF NOT EXISTS `rendez_vous` (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_id` int NOT NULL,
  `medecin_id` int NOT NULL,
  `cancer_id` int DEFAULT NULL,
  `date_rdv` datetime NOT NULL,
  `motif` text COLLATE utf8mb4_unicode_ci,
  `statut` enum('PLANIFIE','REALISE','ANNULE') COLLATE utf8mb4_unicode_ci DEFAULT 'PLANIFIE',
  `notes` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `patient_id` (`patient_id`),
  KEY `medecin_id` (`medecin_id`),
  KEY `cancer_id` (`cancer_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `reponse`;
CREATE TABLE IF NOT EXISTS `reponse` (
  `id` int NOT NULL AUTO_INCREMENT,
  `reclamation_id` int NOT NULL,
  `administrateur_id` int NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `date_reponse` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `reclamation_id` (`reclamation_id`),
  KEY `administrateur_id` (`administrateur_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `reservation`;
CREATE TABLE IF NOT EXISTS `reservation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `event_id` int NOT NULL,
  `utilisateur_id` int NOT NULL,
  `date_reservation` datetime DEFAULT CURRENT_TIMESTAMP,
  `statut` enum('CONFIRMEE','ANNULEE') COLLATE utf8mb4_unicode_ci DEFAULT 'CONFIRMEE',
  PRIMARY KEY (`id`),
  KEY `event_id` (`event_id`),
  KEY `utilisateur_id` (`utilisateur_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `utilisateur`;
CREATE TABLE IF NOT EXISTS `utilisateur` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `prenom` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ADMIN','MEDECIN','PATIENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `telephone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `adresse` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_inscription` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `utilisateur` (`id`, `nom`, `prenom`, `email`, `password`, `role`, `telephone`, `adresse`) VALUES
(1, 'Admin', 'OncoReminder', 'admin@oncoreminder.tn', 'admin', 'ADMIN', '00000000', 'Tunis'),
(2, 'Patient', 'Test', 'patient@oncoreminder.tn', 'patient', 'PATIENT', '11111111', 'Tunis');

INSERT INTO `event` (`id`, `titre`, `description`, `date_event`, `lieu`, `capacite_max`, `places_restantes`, `createur_id`, `cancer_id`, `image_path`) VALUES
(1, 'Conference Oncologie 2026', 'Conference annuelle sur le cancer', '2026-06-15 09:00:00', 'Salle A - Hopital Tunis', 50, 50, 1, 1, NULL),
(2, 'Atelier Nutrition Cancer', 'Atelier pratique nutrition pour les patients', '2026-07-10 14:00:00', 'Clinique El Manar', 30, 30, 1, 1, NULL),
(3, 'Journee Sensibilisation', 'Journee de sensibilisation publique', '2026-08-20 08:00:00', 'Centre de Sante Ariana', 100, 100, 1, 1, NULL);

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
