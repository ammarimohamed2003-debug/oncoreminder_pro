package com.oncoreminder.utils;

import javafx.geometry.Insets;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

public class MarkdownRenderer {

    private static final String BASE = "-fx-font-size:13px; -fx-fill:#2D3748;";

    public static void render(String markdown, VBox container) {
        container.getChildren().clear();
        if (markdown == null || markdown.isBlank()) return;

        int orderedCounter = 0;

        for (String line : markdown.split("\n", -1)) {

            if (line.equals("---")) {
                Separator sep = new Separator();
                sep.setPadding(new Insets(4, 0, 4, 0));
                container.getChildren().add(sep);
                orderedCounter = 0;

            } else if (line.startsWith("# ")) {
                Text t = new Text(line.substring(2));
                t.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-fill:#4A2D8F;");
                container.getChildren().add(new TextFlow(t));
                orderedCounter = 0;

            } else if (line.startsWith("## ")) {
                Text t = new Text(line.substring(3));
                t.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-fill:#5B35A5;");
                container.getChildren().add(new TextFlow(t));
                orderedCounter = 0;

            } else if (line.startsWith("- ")) {
                HBox row = new HBox(8);
                Text bullet = new Text("•");
                bullet.setStyle("-fx-fill:#2BBCB0; -fx-font-size:14px; -fx-font-weight:bold;");
                TextFlow tf = parseInline(line.substring(2));
                HBox.setHgrow(tf, Priority.ALWAYS);
                row.getChildren().addAll(bullet, tf);
                container.getChildren().add(row);
                orderedCounter = 0;

            } else if (line.matches("^\\d+\\.\\s.*")) {
                orderedCounter++;
                int dot = line.indexOf(". ");
                HBox row = new HBox(8);
                Text num = new Text(orderedCounter + ".");
                num.setStyle("-fx-fill:#2BBCB0; -fx-font-size:13px; -fx-font-weight:bold;");
                TextFlow tf = parseInline(line.substring(dot + 2));
                HBox.setHgrow(tf, Priority.ALWAYS);
                row.getChildren().addAll(num, tf);
                container.getChildren().add(row);

            } else if (line.startsWith("> ")) {
                HBox quote = new HBox(0);
                VBox bar = new VBox();
                bar.setStyle("-fx-background-color:#2BBCB0; -fx-min-width:4; -fx-background-radius:2;");
                bar.setMinWidth(4);
                TextFlow tf = parseInline(line.substring(2));
                tf.setStyle("-fx-background-color:#E8F8F7; -fx-padding:6 12; -fx-background-radius:0 4 4 0;");
                HBox.setHgrow(tf, Priority.ALWAYS);
                quote.getChildren().addAll(bar, tf);
                container.getChildren().add(quote);
                orderedCounter = 0;

            } else if (line.startsWith("[center]") && line.endsWith("[/center]")) {
                String inner = line.substring(8, line.length() - 9);
                TextFlow tf = parseInline(inner);
                tf.setTextAlignment(TextAlignment.CENTER);
                tf.setMaxWidth(Double.MAX_VALUE);
                container.getChildren().add(tf);
                orderedCounter = 0;

            } else if (line.isEmpty()) {
                container.getChildren().add(new TextFlow(new Text(" ")));
                orderedCounter = 0;

            } else {
                container.getChildren().add(parseInline(line));
                orderedCounter = 0;
            }
        }
    }

    static TextFlow parseInline(String text) {
        List<Text> nodes = new ArrayList<>();
        int i = 0;
        StringBuilder plain = new StringBuilder();

        while (i < text.length()) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;

            // ── Gras **text** ──
            if (c == '*' && next == '*') {
                flush(plain, nodes);
                int end = text.indexOf("**", i + 2);
                if (end != -1) {
                    Text t = new Text(text.substring(i + 2, end));
                    t.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-fill:#2D3748;");
                    nodes.add(t); i = end + 2;
                } else { plain.append(c); i++; }

            // ── Barré ~~text~~ ──
            } else if (c == '~' && next == '~') {
                flush(plain, nodes);
                int end = text.indexOf("~~", i + 2);
                if (end != -1) {
                    Text t = new Text(text.substring(i + 2, end));
                    t.setStyle("-fx-font-size:13px; -fx-fill:#718096;");
                    t.setStrikethrough(true);
                    nodes.add(t); i = end + 2;
                } else { plain.append(c); i++; }

            // ── Souligné __text__ ──
            } else if (c == '_' && next == '_') {
                flush(plain, nodes);
                int end = text.indexOf("__", i + 2);
                if (end != -1) {
                    Text t = new Text(text.substring(i + 2, end));
                    t.setStyle("-fx-underline:true; -fx-font-size:13px; -fx-fill:#2D3748;");
                    nodes.add(t); i = end + 2;
                } else { plain.append(c); i++; }

            // ── Italique *text* ──
            } else if (c == '*') {
                flush(plain, nodes);
                int end = text.indexOf("*", i + 1);
                if (end != -1) {
                    Text t = new Text(text.substring(i + 1, end));
                    t.setStyle("-fx-font-style:italic; -fx-font-size:13px; -fx-fill:#2D3748;");
                    nodes.add(t); i = end + 1;
                } else { plain.append(c); i++; }

            // ── Code `text` ──
            } else if (c == '`') {
                flush(plain, nodes);
                int end = text.indexOf("`", i + 1);
                if (end != -1) {
                    Text t = new Text(text.substring(i + 1, end));
                    t.setStyle("-fx-font-family:'Courier New'; -fx-font-size:12px; -fx-fill:#C0392B; -fx-background-color:#FFF5F5;");
                    nodes.add(t); i = end + 1;
                } else { plain.append(c); i++; }

            // ── Tags [tag]text[/tag] ──
            } else if (c == '[' && next != '/') {
                int closeBracket = text.indexOf(']', i + 1);
                if (closeBracket != -1) {
                    String tag = text.substring(i + 1, closeBracket);
                    String closeTag = "[/" + tag + "]";
                    int closeIdx = text.indexOf(closeTag, closeBracket + 1);
                    if (closeIdx != -1) {
                        flush(plain, nodes);
                        String inner = text.substring(closeBracket + 1, closeIdx);
                        Text t = applyTag(tag, inner);
                        nodes.add(t);
                        i = closeIdx + closeTag.length();
                        continue;
                    }
                }
                plain.append(c); i++;

            } else {
                plain.append(c); i++;
            }
        }
        flush(plain, nodes);

        TextFlow flow = new TextFlow();
        flow.setMaxWidth(Double.MAX_VALUE);
        flow.getChildren().addAll(nodes);
        return flow;
    }

    private static Text applyTag(String tag, String content) {
        Text t = new Text(content);
        switch (tag) {
            // Tailles
            case "small":  t.setStyle("-fx-font-size:10px; -fx-fill:#2D3748;"); break;
            case "normal": t.setStyle("-fx-font-size:13px; -fx-fill:#2D3748;"); break;
            case "big":    t.setStyle("-fx-font-size:17px; -fx-fill:#2D3748;"); break;
            case "xl":     t.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-fill:#2D3748;"); break;
            // Exposant / Indice
            case "sup":
                t.setStyle("-fx-font-size:9px; -fx-fill:#2D3748;");
                t.setTranslateY(-5);
                break;
            case "sub":
                t.setStyle("-fx-font-size:9px; -fx-fill:#2D3748;");
                t.setTranslateY(4);
                break;
            // Surligné
            case "hl":     t.setStyle("-fx-font-size:13px; -fx-fill:#7B341E; -fx-background-color:#FEFCBF;"); break;
            // Couleurs
            case "red":    t.setStyle("-fx-font-size:13px; -fx-fill:#E53E3E;"); break;
            case "orange": t.setStyle("-fx-font-size:13px; -fx-fill:#DD6B20;"); break;
            case "green":  t.setStyle("-fx-font-size:13px; -fx-fill:#38A169;"); break;
            case "blue":   t.setStyle("-fx-font-size:13px; -fx-fill:#3182CE;"); break;
            case "purple": t.setStyle("-fx-font-size:13px; -fx-fill:#6B46C1;"); break;
            case "teal":   t.setStyle("-fx-font-size:13px; -fx-fill:#2BBCB0;"); break;
            case "gray":   t.setStyle("-fx-font-size:13px; -fx-fill:#718096;"); break;
            case "black":  t.setStyle("-fx-font-size:13px; -fx-fill:#1A202C;"); break;
            default:       t.setStyle(BASE); break;
        }
        return t;
    }

    private static void flush(StringBuilder sb, List<Text> nodes) {
        if (sb.length() == 0) return;
        Text t = new Text(sb.toString());
        t.setStyle(BASE);
        nodes.add(t);
        sb.setLength(0);
    }
}
