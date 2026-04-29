# OncoReminder - Gestion Utilisateurs

Ce projet est une implémentation JavaFX complète pour la gestion des utilisateurs (CRUD).

## Fonctionnalités
- Ajout, Modification, Suppression et Affichage des utilisateurs.
- Hachage des mots de passe avec BCrypt.
- Gestion des rôles (DOCTEUR, PATIENT).
- Validation des entrées (Email, champs obligatoires).

## Technologies
- JavaFX
- MySQL
- JDBC
- jBCrypt

## Configuration
1. Créer une base de données MySQL nommée `oncoreminder`.
2. Configurer les accès dans `utils.MyConnection`.
3. Lancer avec `mvn javafx:run`.
