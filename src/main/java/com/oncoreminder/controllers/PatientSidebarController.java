package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class PatientSidebarController {

    private static final String ACTIVE = "sidebar-nav-btn-active";
    private static final String NORMAL = "sidebar-nav-btn";

    @FXML private Label  patientNameLabel;
    @FXML private Button btnMaSante;
    @FXML private Button btnArticles;
    @FXML private Button btnEvenements;
    @FXML private Button btnMesRendezVous;
    @FXML private Button btnReclamations;

    private Runnable maSanteCallback;
    private Runnable evenementsCallback;
    private Runnable reclamationsCallback;

    @FXML
    public void initialize() {
        Utilisateur patient = UserSession.getInstance().getCurrentUser();
        if (patient != null) {
            patientNameLabel.setText(patient.getPrenom() + " " + patient.getNom());
        }
    }

    public void refreshName(String fullName) { patientNameLabel.setText(fullName); }

    public void setMaSanteCallback(Runnable r)     { maSanteCallback     = r; }
    public void setEvenementsCallback(Runnable r)  { evenementsCallback  = r; }
    public void setReclamationsCallback(Runnable r){ reclamationsCallback = r; }

    /** Called by PatientDashboardController.showPane() to keep button highlights in sync. */
    public void setActivePane(int which) {
        btnMaSante.getStyleClass().setAll(which == 0 ? ACTIVE : NORMAL);
        btnArticles.getStyleClass().setAll(NORMAL);
        btnEvenements.getStyleClass().setAll(which == 1 || which == 2 ? ACTIVE : NORMAL);
        btnMesRendezVous.getStyleClass().setAll(NORMAL);
        if (btnReclamations != null)
            btnReclamations.getStyleClass().setAll(which == 3 ? ACTIVE : NORMAL);
    }

    /** Called by article/rendez-vous pages to highlight the correct nav button. */
    public void setActivePage(String page) {
        btnMaSante.getStyleClass().setAll(NORMAL);
        btnArticles.getStyleClass().setAll("articles".equals(page) ? ACTIVE : NORMAL);
        btnEvenements.getStyleClass().setAll(NORMAL);
        btnMesRendezVous.getStyleClass().setAll("rendezvous".equals(page) ? ACTIVE : NORMAL);
        if (btnReclamations != null)
            btnReclamations.getStyleClass().setAll(NORMAL);
    }

    @FXML void handleMaSante() {
        if (maSanteCallback != null) maSanteCallback.run();
        else App.navigate("PatientDashboard");
    }

    @FXML void handleEvenements() {
        if (evenementsCallback != null) evenementsCallback.run();
        else { PatientDashboardController.showEventsOnLoad = true; App.navigate("PatientDashboard"); }
    }

    @FXML void handleReclamations() {
        if (reclamationsCallback != null) reclamationsCallback.run();
        else { PatientDashboardController.showReclamationsOnLoad = true; App.navigate("PatientDashboard"); }
    }

    @FXML void handleArticles()      { App.navigate("PatientArticleList"); }
    @FXML void handleMesRendezVous() { App.navigate("PatientRendezVous"); }

    @FXML void handleLogout() {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}