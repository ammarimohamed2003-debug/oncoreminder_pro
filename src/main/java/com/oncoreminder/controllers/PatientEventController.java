package com.oncoreminder.controllers;

import com.oncoreminder.app.App;
import com.oncoreminder.models.Event;
import com.oncoreminder.services.ServiceEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PatientEventController {

    @FXML private FlowPane eventsFlowPane;
    @FXML private Label    lbCount;

    private Runnable onReserver;
    public void setOnReserver(Runnable r) { this.onReserver = r; }

    private final ServiceEvent serviceEvent = new ServiceEvent();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    void initialize() {
        chargerEvents();
    }

    private void chargerEvents() {
        List<Event> list = serviceEvent.getAll();
        eventsFlowPane.getChildren().clear();
        lbCount.setText(list.size() + " événement(s) disponible(s)");
        if (list.isEmpty()) {
            Label empty = new Label("🗓  Aucun événement disponible pour le moment");
            empty.setStyle("-fx-text-fill:#B0C4D8; -fx-font-size:14px; -fx-font-style:italic; -fx-padding:40;");
            eventsFlowPane.getChildren().add(empty);
            return;
        }
        for (Event ev : list) eventsFlowPane.getChildren().add(buildCard(ev));
    }

    private VBox buildCard(Event ev) {
        VBox card = new VBox(0);
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:14;" +
            "-fx-border-color:#E9E4F7; -fx-border-radius:14; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(90,53,165,0.10),10,0,0,3);"
        );

        // Image
        if (ev.getImagePath() != null && !ev.getImagePath().isBlank()) {
            File f = new File(ev.getImagePath());
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 260, 150, false, true, true));
                iv.setFitWidth(260); iv.setFitHeight(150); iv.setPreserveRatio(false);
                Rectangle clip = new Rectangle(260, 150);
                clip.setArcWidth(28); clip.setArcHeight(28);
                iv.setClip(clip);
                card.getChildren().add(iv);
            }
        }

        VBox body = new VBox(8);
        body.setPadding(new Insets(14, 16, 16, 16));

        Label titre = new Label(ev.getTitre());
        titre.setWrapText(true);
        titre.setStyle("-fx-font-weight:bold; -fx-font-size:14px; -fx-text-fill:#2D1B69;");

        Label date = new Label("📅  " + ev.getDateEvent().format(FMT));
        date.setStyle("-fx-text-fill:#5B35A5; -fx-font-size:11px;");

        Label lieu = new Label("📍  " + (ev.getLieu() != null ? ev.getLieu() : "—"));
        lieu.setStyle("-fx-text-fill:#718096; -fx-font-size:11px;");

        boolean complet = ev.getPlacesRestantes() <= 0;
        Label badge = new Label(complet ? "Complet" : ev.getPlacesRestantes() + " place(s) restante(s)");
        badge.setStyle(
            "-fx-background-color:" + (complet ? "#FED7D7" : "#C6F6D5") + ";" +
            "-fx-text-fill:" + (complet ? "#C53030" : "#276749") + ";" +
            "-fx-padding:3 10; -fx-background-radius:10; -fx-font-size:11px; -fx-font-weight:bold;"
        );

        Button btn = new Button(complet ? "Complet" : "🎟  Réserver");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setDisable(complet);
        btn.setStyle(
            "-fx-background-color:" + (complet ? "#E2E8F0" : "#0D9488") + ";" +
            "-fx-text-fill:" + (complet ? "#A0AEC0" : "#FFFFFF") + ";" +
            "-fx-font-weight:bold; -fx-font-size:12px; -fx-background-radius:10; -fx-border-radius:10;" +
            "-fx-padding:9 0 9 0; -fx-cursor:" + (complet ? "default" : "hand") + ";" +
            (complet ? "" : "-fx-effect:dropshadow(gaussian,rgba(13,148,136,0.35),8,0,0,3);")
        );
        if (!complet) btn.setOnAction(e -> { if (onReserver != null) onReserver.run(); else App.navigate("GestionReservation"); });

        body.getChildren().addAll(titre, date, lieu, badge, btn);
        card.getChildren().add(body);

        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color:white; -fx-background-radius:14;" +
            "-fx-border-color:#5B35A5; -fx-border-radius:14; -fx-border-width:2;" +
            "-fx-effect:dropshadow(gaussian,rgba(90,53,165,0.20),14,0,0,5);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color:white; -fx-background-radius:14;" +
            "-fx-border-color:#E9E4F7; -fx-border-radius:14; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(90,53,165,0.10),10,0,0,3);"
        ));

        return card;
    }

    @FXML void handleRefresh(ActionEvent e) { chargerEvents(); }
}
