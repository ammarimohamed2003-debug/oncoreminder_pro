package com.oncoreminder.utils;

import com.oncoreminder.models.Utilisateur;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

public class ChatbotFab {

    public static StackPane wrapWithChatbot(Parent root) {
        StackPane wrapper = new StackPane(root);

        Utilisateur user = UserSession.getInstance().getCurrentUser();
        String role = user != null ? user.getRole() : "PATIENT";

        // Messages area
        VBox messagesBox = new VBox(10);
        messagesBox.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(messagesBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: #F8FAFC; -fx-background: #F8FAFC; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Build panel and FAB
        VBox chatPanel = buildChatPanel(scroll, messagesBox, role);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        StackPane.setAlignment(chatPanel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatPanel, new Insets(0, 20, 72, 0));

        Button fab = buildFab();
        StackPane.setAlignment(fab, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fab, new Insets(0, 20, 20, 0));

        fab.setOnAction(e -> {
            boolean showing = chatPanel.isVisible();
            chatPanel.setVisible(!showing);
            chatPanel.setManaged(!showing);
            fab.setText(showing ? "🤖" : "✕");
        });

        // Welcome message + quick replies
        addBotMessage(messagesBox, ChatbotService.getWelcome(role), scroll);
        addQuickReplies(messagesBox, ChatbotService.getQuickReplies(role), scroll, role);

        wrapper.getChildren().addAll(chatPanel, fab);
        return wrapper;
    }

    // ── FAB button ────────────────────────────────────────────────────────────

    private static Button buildFab() {
        Button fab = new Button("🤖");
        fab.setPrefSize(44, 44);
        fab.setStyle(fabStyle());
        fab.setOnMouseEntered(e -> fab.setOpacity(0.88));
        fab.setOnMouseExited(e  -> fab.setOpacity(1.0));
        return fab;
    }

    private static String fabStyle() {
        return "-fx-background-color: linear-gradient(to bottom right, #2BBCB0, #1A9E93);"
             + "-fx-text-fill: white; -fx-font-size: 18px;"
             + "-fx-background-radius: 22; -fx-border-width: 0; -fx-cursor: hand;"
             + "-fx-effect: dropshadow(gaussian, rgba(43,188,176,0.50), 12, 0, 0, 3);";
    }

    // ── Chat panel ────────────────────────────────────────────────────────────

    private static VBox buildChatPanel(ScrollPane scroll, VBox messagesBox, String role) {
        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color: linear-gradient(to right, #2BBCB0, #1E3A5F);"
                      + "-fx-background-radius: 16 16 0 0;");

        javafx.scene.control.Label avatar = label("🤖", "-fx-font-size: 20px;");
        VBox titleBox = new VBox(2,
            label("Assistant OncoReminder",
                  "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"),
            label("En ligne  •  Toujours disponible",
                  "-fx-text-fill: rgba(255,255,255,0.65); -fx-font-size: 10px;")
        );
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Online dot
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5,
                javafx.scene.paint.Color.web("#4ADE80"));
        header.getChildren().addAll(avatar, titleBox, sp, dot);

        // Input area
        TextField input = new TextField();
        input.setPromptText("Posez votre question...");
        input.setStyle("-fx-background-color: white; -fx-background-radius: 20;"
                     + "-fx-border-color: #E2E8F0; -fx-border-radius: 20; -fx-border-width: 1;"
                     + "-fx-padding: 8 14; -fx-font-size: 12px;");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button sendBtn = new Button("→");
        sendBtn.setStyle("-fx-background-color: #2BBCB0; -fx-text-fill: white;"
                       + "-fx-background-radius: 20; -fx-border-width: 0; -fx-font-size: 15px;"
                       + "-fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;");
        sendBtn.setOnMouseEntered(e -> sendBtn.setOpacity(0.88));
        sendBtn.setOnMouseExited(e  -> sendBtn.setOpacity(1.0));

        HBox inputRow = new HBox(8, input, sendBtn);
        inputRow.setAlignment(Pos.CENTER);
        inputRow.setPadding(new Insets(10, 12, 12, 12));
        inputRow.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16;"
                        + "-fx-border-color: #F1F5F9 transparent transparent transparent;"
                        + "-fx-border-width: 1;");

        Runnable onSend = () -> {
            String text = input.getText().trim();
            if (text.isEmpty()) return;
            addUserMessage(messagesBox, text, scroll);
            input.clear();
            String clean = text.replaceAll("[\\p{So}\\p{Sk}]", "").trim().toLowerCase();
            String response = ChatbotService.getResponse(clean, role);
            new Thread(() -> {
                try { Thread.sleep(350); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> addBotMessage(messagesBox, response, scroll));
            }).start();
        };

        sendBtn.setOnAction(e -> onSend.run());
        input.setOnAction(e -> onSend.run());

        VBox panel = new VBox(header, scroll, inputRow);
        panel.setPrefWidth(340);
        panel.setPrefHeight(470);
        panel.setMaxHeight(470);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                     + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 22, 0, 0, 6);");
        return panel;
    }

    // ── Message builders ──────────────────────────────────────────────────────

    private static void addUserMessage(VBox box, String text, ScrollPane scroll) {
        javafx.scene.control.Label msg = new javafx.scene.control.Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(220);
        msg.setStyle("-fx-background-color: #2BBCB0; -fx-text-fill: white;"
                   + "-fx-background-radius: 16 16 4 16; -fx-padding: 8 12; -fx-font-size: 12px;");
        HBox row = new HBox(msg);
        row.setAlignment(Pos.CENTER_RIGHT);
        box.getChildren().add(row);
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private static void addBotMessage(VBox box, String text, ScrollPane scroll) {
        TextFlow tf = parseMarkdown(text);
        tf.setMaxWidth(220);
        tf.setPadding(new Insets(9, 12, 9, 12));
        tf.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 4 16 16 16;");

        javafx.scene.control.Label avatar = label("🤖", "-fx-font-size: 15px;");
        HBox row = new HBox(6, avatar, tf);
        row.setAlignment(Pos.TOP_LEFT);
        box.getChildren().add(row);
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private static void addQuickReplies(VBox box, List<String> replies, ScrollPane scroll, String role) {
        FlowPane chips = new FlowPane(6, 6);
        chips.setPadding(new Insets(4, 0, 0, 22));
        for (String r : replies) {
            Button chip = new Button(r);
            chip.setStyle("-fx-background-color: white; -fx-text-fill: #2BBCB0;"
                        + "-fx-background-radius: 14; -fx-border-color: #2BBCB0DD; -fx-border-radius: 14;"
                        + "-fx-border-width: 1; -fx-padding: 4 12; -fx-font-size: 11px; -fx-cursor: hand;");
            chip.setOnMouseEntered(e ->
                chip.setStyle(chip.getStyle().replace("white; -fx-text-fill: #2BBCB0", "#E6FAF8; -fx-text-fill: #1A9E93")));
            chip.setOnMouseExited(e ->
                chip.setStyle(chip.getStyle().replace("#E6FAF8; -fx-text-fill: #1A9E93", "white; -fx-text-fill: #2BBCB0")));
            chip.setOnAction(e -> {
                box.getChildren().remove(chips);
                addUserMessage(box, r, scroll);
                String clean = r.replaceAll("[\\p{So}\\p{Sk}\\p{P}]", "").trim().toLowerCase();
                new Thread(() -> {
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    String response = ChatbotService.getResponse(clean, role);
                    Platform.runLater(() -> addBotMessage(box, response, scroll));
                }).start();
            });
            chips.getChildren().add(chip);
        }
        box.getChildren().add(chips);
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    // ── Markdown parser (bold **text**, newlines) ─────────────────────────────

    private static TextFlow parseMarkdown(String text) {
        TextFlow flow = new TextFlow();
        flow.setMaxWidth(220);
        String[] parts = text.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            boolean bold = (i % 2 == 1);
            String[] lines = parts[i].split("\n", -1);
            for (int j = 0; j < lines.length; j++) {
                if (j > 0) flow.getChildren().add(new Text("\n"));
                if (!lines[j].isEmpty()) {
                    Text t = new Text(lines[j]);
                    t.setFont(Font.font("System", bold ? FontWeight.BOLD : FontWeight.NORMAL, 12));
                    t.setFill(javafx.scene.paint.Color.web("#334155"));
                    flow.getChildren().add(t);
                }
            }
        }
        return flow;
    }

    private static javafx.scene.control.Label label(String text, String style) {
        javafx.scene.control.Label l = new javafx.scene.control.Label(text);
        l.setStyle(style);
        return l;
    }
}
