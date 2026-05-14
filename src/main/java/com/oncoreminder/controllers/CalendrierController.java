package com.oncoreminder.controllers;

import com.oncoreminder.models.Event;
import com.oncoreminder.models.RendezVous;
import com.oncoreminder.services.ServiceEvent;
import com.oncoreminder.services.ServiceRendezVous;
import com.oncoreminder.utils.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CalendrierController {

    @FXML private DoctorSidebarController doctorSidebarController;
    @FXML private GridPane calHeaderGrid;
    @FXML private GridPane calGrid;
    @FXML private Label monthYearLabel;

    @FXML private VBox detailEmpty;
    @FXML private VBox detailContent;
    @FXML private Label detailDayLabel;
    @FXML private Label detailMonthLabel;
    @FXML private Label detailRdvCount;
    @FXML private Label detailEventCount;
    @FXML private VBox detailItemsContainer;

    private final ServiceRendezVous serviceRdv = new ServiceRendezVous();
    private final ServiceEvent serviceEvent   = new ServiceEvent();

    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private final Map<LocalDate, List<RendezVous>> rdvsByDate   = new HashMap<>();
    private final Map<LocalDate, List<Event>>      eventsByDate = new HashMap<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] DAY_NAMES   = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
    private static final String[] MONTHS_FR   = {
        "Janvier","Février","Mars","Avril","Mai","Juin",
        "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };
    private static final String[] DAYS_LONG_FR = {
        "Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"
    };

    @FXML
    public void initialize() {
        if (doctorSidebarController != null) doctorSidebarController.setActivePage("calendrier");
        currentMonth = YearMonth.now();
        loadData();
        buildDayHeaders();
        renderCalendar();
    }

    @FXML void handlePrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        selectedDate = null;
        loadData();
        renderCalendar();
        showEmptyState();
    }

    @FXML void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        selectedDate = null;
        loadData();
        renderCalendar();
        showEmptyState();
    }

    @FXML void handleToday() {
        currentMonth = YearMonth.now();
        selectedDate = null;
        loadData();
        renderCalendar();
        showEmptyState();
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadData() {
        rdvsByDate.clear();
        eventsByDate.clear();
        var user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;

        for (RendezVous rv : serviceRdv.getAllForDoctor(user.getId())) {
            if (rv.getDateRdv() != null)
                rdvsByDate.computeIfAbsent(rv.getDateRdv().toLocalDate(), k -> new ArrayList<>()).add(rv);
        }
        for (Event ev : serviceEvent.getAll()) {
            if (ev.getDateEvent() != null)
                eventsByDate.computeIfAbsent(ev.getDateEvent().toLocalDate(), k -> new ArrayList<>()).add(ev);
        }
    }

    // ── Calendar rendering ────────────────────────────────────────────────────

    private void buildDayHeaders() {
        calHeaderGrid.getChildren().clear();
        calHeaderGrid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            calHeaderGrid.getColumnConstraints().add(cc);

            Label lbl = new Label(DAY_NAMES[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            boolean weekend = (i >= 5);
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; "
                    + "-fx-text-fill: " + (weekend ? "#94A3B8" : "#64748B") + "; -fx-padding: 4 0;");
            calHeaderGrid.add(lbl, i, 0);
        }
    }

    private void renderCalendar() {
        monthYearLabel.setText(MONTHS_FR[currentMonth.getMonthValue() - 1] + " " + currentMonth.getYear());

        calGrid.getChildren().clear();
        calGrid.getColumnConstraints().clear();
        calGrid.getRowConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            calGrid.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < 6; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(68);
            rc.setVgrow(Priority.ALWAYS);
            calGrid.getRowConstraints().add(rc);
        }

        LocalDate today    = LocalDate.now();
        int startCol       = currentMonth.atDay(1).getDayOfWeek().getValue() - 1;
        int daysInMonth    = currentMonth.lengthOfMonth();
        int col = startCol, row = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            VBox cell = buildDayCell(date,
                    rdvsByDate.getOrDefault(date, List.of()),
                    eventsByDate.getOrDefault(date, List.of()),
                    today);
            calGrid.add(cell, col, row);
            GridPane.setVgrow(cell, Priority.ALWAYS);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            if (++col == 7) { col = 0; row++; }
        }
    }

    private VBox buildDayCell(LocalDate date, List<RendezVous> rdvs, List<Event> events, LocalDate today) {
        boolean isToday    = date.equals(today);
        boolean isSelected = date.equals(selectedDate);
        boolean hasItems   = !rdvs.isEmpty() || !events.isEmpty();

        // Compute styles up-front so hover handlers can reference them
        final String normalStyle;
        final String hoverStyle;

        if (isSelected) {
            normalStyle = cellStyle("#5B35A5", "#5B35A5", 1);
            hoverStyle  = normalStyle;
        } else if (isToday) {
            normalStyle = cellStyle("#EEF2FF", "#4F46E5", 1.5);
            hoverStyle  = cellStyle("#E0E7FF", "#4F46E5", 1.5);
        } else if (hasItems) {
            normalStyle = cellStyle("#FAFBFF", "#E2E8F0", 1);
            hoverStyle  = cellStyle("#F0F4FF", "#C7D2FE", 1);
        } else {
            normalStyle = cellStyle("transparent", "#F1F5F9", 1);
            hoverStyle  = cellStyle("#F8FAFC",    "#E2E8F0", 1);
        }

        VBox cell = new VBox(4);
        cell.setPadding(new Insets(6));
        cell.setAlignment(Pos.TOP_RIGHT);
        cell.setMinHeight(68);
        cell.setStyle(normalStyle);

        // Day number
        String numColor = isSelected ? "white" : (isToday ? "#4F46E5" : "#334155");
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.setStyle("-fx-font-size: 13px; -fx-font-weight: " + (isToday ? "bold" : "normal")
                + "; -fx-text-fill: " + numColor + ";");
        cell.getChildren().add(dayNum);

        // Spacer pushes dots to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        cell.getChildren().add(spacer);

        // Colored dots
        if (hasItems) {
            HBox dots = new HBox(3);
            dots.setAlignment(Pos.CENTER_LEFT);
            addDots(dots, rdvs, events, isSelected);
            cell.getChildren().add(dots);
        }

        // Interactions
        cell.setOnMouseClicked(e -> handleDayClick(date));
        if (!isSelected) {
            cell.setOnMouseEntered(e -> cell.setStyle(hoverStyle));
            cell.setOnMouseExited(e  -> cell.setStyle(normalStyle));
        }

        return cell;
    }

    private String cellStyle(String bg, String border, double borderWidth) {
        return "-fx-background-color: " + bg + "; -fx-background-radius: 10;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 10;"
                + "-fx-border-width: " + borderWidth + "; -fx-cursor: hand;";
    }

    private void addDots(HBox dots, List<RendezVous> rdvs, List<Event> events, boolean onDark) {
        String dotTeal   = onDark ? "white" : "#2BBCB0";
        String dotAmber  = onDark ? "white" : "#F59E0B";
        String dotPurple = onDark ? "white" : "#5B35A5";

        long accepted  = rdvs.stream().filter(r -> r.getStatut() == RendezVous.Statut.ACCEPTE).count();
        long pending   = rdvs.stream().filter(r -> r.getStatut() == RendezVous.Statut.EN_ATTENTE).count();

        if (accepted > 0) {
            dots.getChildren().add(dot(dotTeal));
            if (accepted > 1) dots.getChildren().add(countLabel("+" + (accepted - 1), dotTeal));
        }
        if (pending > 0) dots.getChildren().add(dot(dotAmber));
        if (!events.isEmpty()) dots.getChildren().add(dot(dotPurple));
    }

    private Circle dot(String hex) { return new Circle(4, Color.web(hex)); }
    private Label countLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 9px; -fx-text-fill: " + color + ";");
        return l;
    }

    // ── Day click → detail panel ──────────────────────────────────────────────

    private void handleDayClick(LocalDate date) {
        selectedDate = date;
        renderCalendar();
        showDetailForDate(date);
    }

    private void showDetailForDate(LocalDate date) {
        List<RendezVous> rdvs   = rdvsByDate.getOrDefault(date, List.of());
        List<Event>      events = eventsByDate.getOrDefault(date, List.of());

        detailDayLabel.setText(DAYS_LONG_FR[date.getDayOfWeek().getValue() - 1] + " " + date.getDayOfMonth());
        detailMonthLabel.setText(MONTHS_FR[date.getMonthValue() - 1] + " " + date.getYear());
        detailRdvCount.setText(rdvs.size() + " rendez-vous");
        detailEventCount.setText(events.size() + " événement(s)");

        detailItemsContainer.getChildren().clear();

        if (rdvs.isEmpty() && events.isEmpty()) {
            Label empty = new Label("Aucun élément ce jour");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
            empty.setPadding(new Insets(20, 0, 0, 0));
            detailItemsContainer.getChildren().add(empty);
        } else {
            if (!rdvs.isEmpty()) {
                detailItemsContainer.getChildren().add(sectionLabel("Rendez-vous", "8 0 4 0"));
                rdvs.forEach(rv -> detailItemsContainer.getChildren().add(buildRdvCard(rv)));
            }
            if (!events.isEmpty()) {
                detailItemsContainer.getChildren().add(sectionLabel("Événements", "12 0 4 0"));
                events.forEach(ev -> detailItemsContainer.getChildren().add(buildEventCard(ev)));
            }
        }

        detailEmpty.setVisible(false);
        detailEmpty.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);
    }

    private Label sectionLabel(String text, String padding) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;"
                + "-fx-padding: " + padding + ";");
        return l;
    }

    private VBox buildRdvCard(RendezVous rv) {
        String statColor, bgColor;
        switch (rv.getStatut()) {
            case ACCEPTE -> { statColor = "#059669"; bgColor = "#F0FDF4"; }
            case REFUSE  -> { statColor = "#DC2626"; bgColor = "#FEF2F2"; }
            default      -> { statColor = "#D97706"; bgColor = "#FFFBEB"; }
        }

        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10;"
                + "-fx-border-color: " + statColor + "40; -fx-border-radius: 10; -fx-border-width: 1;");

        // Top row: time + badge
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label time = new Label("🕐 " + rv.getDateRdv().format(TIME_FMT));
        time.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        String badgeText = switch (rv.getStatut()) {
            case ACCEPTE -> "✓ Accepté";
            case REFUSE  -> "✗ Refusé";
            default      -> "⏳ En attente";
        };
        Label badge = new Label(badgeText);
        badge.setStyle("-fx-background-color: " + statColor + "20; -fx-text-fill: " + statColor + ";"
                + "-fx-background-radius: 12; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        top.getChildren().addAll(time, sp, badge);
        card.getChildren().add(top);

        if (!rv.getPatientNom().isBlank()) {
            Label patient = new Label("👤 " + rv.getPatientNom());
            patient.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
            card.getChildren().add(patient);
        }
        if (rv.getLieu() != null && !rv.getLieu().isBlank()) {
            Label lieu = new Label("📍 " + rv.getLieu());
            lieu.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
            card.getChildren().add(lieu);
        }
        return card;
    }

    private VBox buildEventCard(Event ev) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #F5F3FF; -fx-background-radius: 10;"
                + "-fx-border-color: #5B35A540; -fx-border-radius: 10; -fx-border-width: 1;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label time = new Label("🕐 " + ev.getDateEvent().format(TIME_FMT));
        time.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #5B35A5;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label badge = new Label("Événement");
        badge.setStyle("-fx-background-color: #5B35A520; -fx-text-fill: #5B35A5;"
                + "-fx-background-radius: 12; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        top.getChildren().addAll(time, sp, badge);
        card.getChildren().add(top);

        Label titre = new Label(ev.getTitre());
        titre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3730A3;");
        titre.setWrapText(true);
        card.getChildren().add(titre);

        if (ev.getLieu() != null && !ev.getLieu().isBlank()) {
            Label lieu = new Label("📍 " + ev.getLieu());
            lieu.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
            card.getChildren().add(lieu);
        }
        Label places = new Label("💺 " + ev.getPlacesRestantes() + "/" + ev.getCapacite() + " places");
        places.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        card.getChildren().add(places);

        return card;
    }

    private void showEmptyState() {
        detailEmpty.setVisible(true);
        detailEmpty.setManaged(true);
        detailContent.setVisible(false);
        detailContent.setManaged(false);
    }
}
