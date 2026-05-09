============================================================
 OncoReminder Pro - Module Event + Reservation
============================================================

ETAPES POUR FAIRE MARCHER LE PROJET :

1. WAMP / PHPMYADMIN
   - Demarre WAMP
   - Ouvre phpMyAdmin : http://localhost/phpmyadmin
   - Va dans l'onglet Importer
   - Choisis le fichier "database.sql"
   - Clique Executer
   - La base "oncoreminder" sera creee automatiquement

2. INTELLIJ - OUVRIR LE PROJET
   - File > Open > selectionne le dossier "OncoReminderPro"
   - IntelliJ va detecter le pom.xml automatiquement
   - Clique "Load Maven Project" si demande
   - Attends que Maven telecharge les dependances

3. CHANGER LE MOT DE PASSE MYSQL SI NECESSAIRE
   - Ouvre : src/main/java/utils/MyDataBase.java
   - Par defaut WAMP utilise souvent :
     USERNAME = "root"
     PASSWORD = ""
   - Si ton MySQL a un mot de passe, change PASSWORD

4. LANCER L'APPLICATION
   - Dans l'onglet Maven, clique Reload All Maven Projects
   - Dans IntelliJ, fais Build > Rebuild Project
   - Avec Maven, lance javafx:run
   - Pour tester une reservation, utilise ID utilisateur = 1 ou 2

5. OPTIONS EVENT
   - OpenStreetMap a besoin d'une connexion Internet.
   - Pour la generation HuggingFace, ajoute une variable d'environnement :
     HF_API_TOKEN=ton_token_huggingface
   - Optionnel : HF_MODEL=HuggingFaceH4/zephyr-7b-beta
   - Sans token, le bouton genere une description locale simple.

============================================================
 STRUCTURE DES FICHIERS
============================================================

src/main/java/
  utils/             MyDataBase.java
  interfaces/        IService.java
  models/            Event.java
                     Reservation.java
  services/          ServiceEvent.java
                     ServiceReservation.java
  controllers/       Launcher.java
                     MainFx.java
                     EventController.java
                     ReservationController.java

src/main/resources/
  GestionEvent.fxml
  GestionReservation.fxml
  theme.css

database.sql

============================================================
