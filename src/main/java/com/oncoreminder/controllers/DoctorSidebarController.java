package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Utilisateur;
import com.oncoreminder.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class DoctorSidebarController {

    private static final String ACTIVE = "sidebar-nav-btn-doctor-active";
    private static final String NORMAL = "sidebar-nav-btn";

    @FXML private Label  doctorSidebarLabel;
    @FXML private Button btnMonEspace;
    @FXML private Button btnArticles;
    @FXML private Button btnEvenements;
    @FXML private Button btnRendezVous;
    @FXML private Button btnOrdonnances;

    private Runnable monEspaceCallback;
    private Runnable evenementsCallback;

    @FXML
    public void initialize() {
        Utilisateur doc = UserSession.getInstance().getCurrentUser();
        if (doc != null)
            doctorSidebarLabel.setText("Dr. " + doc.getNom() + " " + doc.getPrenom());
    }

    public void setMonEspaceCallback(Runnable r)  { monEspaceCallback  = r; }
    public void setEvenementsCallback(Runnable r) { evenementsCallback = r; }

    /** Called by DoctorDashboardController.showPane() to keep button highlights in sync. */
    public void setActivePane(int which) {
        btnMonEspace.getStyleClass().setAll(which == 0 ? ACTIVE : NORMAL);
        btnArticles.getStyleClass().setAll(NORMAL);
        btnEvenements.getStyleClass().setAll(which >= 1 ? ACTIVE : NORMAL);
        btnRendezVous.getStyleClass().setAll(NORMAL);
        btnOrdonnances.getStyleClass().setAll(NORMAL);
    }

    /** Called by article/rendez-vous/ordonnance pages to highlight the correct nav button. */
    public void setActivePage(String page) {
        btnMonEspace.getStyleClass().setAll(NORMAL);
        btnArticles.getStyleClass().setAll("articles".equals(page) ? ACTIVE : NORMAL);
        btnEvenements.getStyleClass().setAll("evenements".equals(page) ? ACTIVE : NORMAL);
        btnRendezVous.getStyleClass().setAll("rendezvous".equals(page) ? ACTIVE : NORMAL);
        btnOrdonnances.getStyleClass().setAll("ordonnance".equals(page) ? ACTIVE : NORMAL);
    }

    @FXML void handleMonEspace() {
        if (monEspaceCallback != null) monEspaceCallback.run();
        else App.navigate("DoctorDashboard");
    }

    @FXML void handleEvenements() {
        if (evenementsCallback != null) evenementsCallback.run();
        else { DoctorDashboardController.showEventsOnLoad = true; App.navigate("DoctorDashboard"); }
    }

    @FXML void handleArticles()    { App.navigate("ArticleList"); }
    @FXML void handleRendezVous()  { App.navigate("RendezVous"); }
    @FXML void handleOrdonnances() { App.navigate("Ordonnance"); }

    @FXML void handleLogout() {
        UserSession.getInstance().logout();
        App.navigate("Login");
    }
}