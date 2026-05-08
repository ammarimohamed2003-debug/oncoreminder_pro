package com.oncoreminder.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class ArticleGeneratorService {

    private static final String WP_API = "https://fr.wikipedia.org/w/api.php";

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public record GeneratedArticle(String titre, String contenu, String tags) {}

    // ── Point d'entrée ────────────────────────────────────────

    public GeneratedArticle generate(String organe, String cancerType) throws Exception {
        String query = (cancerType != null && !cancerType.isEmpty())
            ? cancerType : "cancer " + organe;

        String pageTitle = searchPage(query);
        if (pageTitle == null) pageTitle = searchPage("cancer " + organe);

        Map<String, String> sections = new LinkedHashMap<>();
        String intro = "";
        String subject = (pageTitle != null) ? pageTitle : organe;

        if (pageTitle != null) {
            String extract = fetchExtract(pageTitle);
            sections = parseSections(extract);
            intro = sections.getOrDefault("__intro__", "");
        }

        String titre   = buildTitle(subject);
        String contenu = buildContent(organe, subject, intro, sections);
        String tags    = buildTags(organe, cancerType);
        return new GeneratedArticle(titre, contenu, tags);
    }

    // ── Wikipedia API ─────────────────────────────────────────

    private String searchPage(String query) throws Exception {
        String url = WP_API + "?action=query&list=search&srsearch="
            + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&format=json&utf8=1&srlimit=1";
        JsonObject root = JsonParser.parseString(get(url)).getAsJsonObject();
        var results = root.getAsJsonObject("query").getAsJsonArray("search");
        if (results.size() == 0) return null;
        return results.get(0).getAsJsonObject().get("title").getAsString();
    }

    private String fetchExtract(String title) throws Exception {
        String url = WP_API + "?action=query&prop=extracts&explaintext=true&titles="
            + URLEncoder.encode(title, StandardCharsets.UTF_8)
            + "&format=json&utf8=1";
        JsonObject root = JsonParser.parseString(get(url)).getAsJsonObject();
        JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
        String pid = pages.keySet().iterator().next();
        var ext = pages.getAsJsonObject(pid).get("extract");
        return ext != null ? ext.getAsString() : "";
    }

    private String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "OncoReminderPro/1.0 (educational)")
            .GET()
            .timeout(Duration.ofSeconds(15))
            .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Wikipedia non disponible (" + resp.statusCode() + ")");
        return resp.body();
    }

    // ── Parsing de l'extrait Wikipedia ───────────────────────

    private Map<String, String> parseSections(String extract) {
        Map<String, String> sections = new LinkedHashMap<>();
        String current = "__intro__";
        StringBuilder buf = new StringBuilder();
        for (String line : extract.split("\n")) {
            if (line.matches("^== .+ ==$")) {
                sections.put(current, buf.toString().trim());
                current = line.replaceAll("^== | ==$", "").trim();
                buf = new StringBuilder();
            } else if (line.matches("^=== .+ ===$")) {
                buf.append("\n**").append(line.replaceAll("^=+ | =+$", "").trim()).append("**\n");
            } else {
                buf.append(line).append("\n");
            }
        }
        sections.put(current, buf.toString().trim());
        return sections;
    }

    // ── Construction de l'article ─────────────────────────────

    private String buildTitle(String subject) {
        return "Cancer du " + subject + " : Comprendre, Détecter et Traiter";
    }

    private String buildContent(String organe, String subject,
                                String intro, Map<String, String> sec) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(buildTitle(subject)).append("\n\n");

        // Introduction
        sb.append("## Introduction\n");
        sb.append(notEmpty(firstLines(intro, 4),
            "Le cancer du **" + organe + "** est une pathologie oncologique nécessitant "
            + "une prise en charge médicale spécialisée et multidisciplinaire."))
          .append("\n\n");

        // Qu'est-ce que c'est ?
        sb.append("## Qu'est-ce que c'est ?\n");
        String def = find(sec, "définition", "généralités", "description", "épidémiologie", "biologie");
        sb.append(notEmpty(firstLines(def, 5),
            "Le cancer du **" + organe + "** se développe lorsque des cellules anormales "
            + "prolifèrent de manière incontrôlée dans les tissus de cet organe."))
          .append("\n\n");

        // Symptômes
        sb.append("## Symptômes principaux\n");
        String symp = find(sec, "symptôme", "signe", "manifestation", "clinique", "présentation");
        sb.append(notEmpty(asList(firstLines(symp, 6)),
            "- Douleurs persistantes au niveau de l'organe concerné\n"
            + "- **Fatigue** inhabituelle et perte de poids inexpliquée\n"
            + "- Modifications physiques inhabituelles\n"
            + "- Symptômes spécifiques à l'organe affecté"))
          .append("\n\n");

        // Diagnostic
        sb.append("## Diagnostic\n");
        String diag = find(sec, "diagnostic", "dépistage", "examen", "bilan", "détection");
        sb.append(notEmpty(firstLines(diag, 5),
            "Le diagnostic repose sur plusieurs examens complémentaires :\n\n"
            + "- **Imagerie médicale** : scanner, IRM, échographie\n"
            + "- **Biopsie** : prélèvement tissulaire pour analyse anatomopathologique\n"
            + "- **Analyses sanguines** : dosage des marqueurs tumoraux"))
          .append("\n\n");

        // Traitements
        sb.append("## Options de traitement\n");
        String treat = find(sec, "traitement", "prise en charge", "thérapie", "thérapeutique", "chirurgie");
        sb.append(notEmpty(firstLines(treat, 6),
            "Les options thérapeutiques disponibles comprennent :\n\n"
            + "- **Chirurgie** : ablation de la tumeur lorsque c'est possible\n"
            + "- **Chimiothérapie** : traitement médicamenteux systémique\n"
            + "- **Radiothérapie** : rayonnements ionisants ciblés\n"
            + "- **Immunothérapie** : stimulation des défenses immunitaires du patient"))
          .append("\n\n");

        // Conseils
        sb.append("## Conseils aux patients\n");
        sb.append("- Suivez scrupuleusement votre **plan de traitement** prescrit\n")
          .append("- Maintenez une alimentation équilibrée adaptée à votre traitement\n")
          .append("- Signalez immédiatement tout nouveau symptôme à votre équipe médicale\n")
          .append("- Entourez-vous de proches et de groupes de soutien\n")
          .append("- Consultez un **onco-psychologue** si vous ressentez le besoin d'un soutien psychologique\n");

        return sb.toString().trim();
    }

    // ── Helpers ───────────────────────────────────────────────

    private String find(Map<String, String> sec, String... keywords) {
        for (String kw : keywords)
            for (var e : sec.entrySet())
                if (e.getKey().toLowerCase().contains(kw) && !e.getValue().isBlank())
                    return e.getValue();
        return "";
    }

    private String firstLines(String text, int maxSentences) {
        if (text == null || text.isBlank()) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        String[] parts = clean.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String p : parts) {
            if (n >= maxSentences) break;
            if (!p.isBlank()) { sb.append(p.trim()).append(" "); n++; }
        }
        return sb.toString().trim();
    }

    private String asList(String text) {
        if (text.isBlank()) return "";
        if (text.contains("\n- ")) return text;
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length <= 1) return text;
        StringBuilder sb = new StringBuilder();
        for (String s : sentences)
            if (!s.isBlank()) sb.append("- ").append(s.trim()).append("\n");
        return sb.toString().trim();
    }

    private String notEmpty(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String buildTags(String organe, String cancerType) {
        List<String> tags = new ArrayList<>(List.of("oncologie", organe.toLowerCase(),
            "chimiothérapie", "dépistage", "traitement"));
        if (cancerType != null && !cancerType.isBlank())
            tags.add(1, cancerType.toLowerCase());
        return String.join(", ", tags);
    }
}
