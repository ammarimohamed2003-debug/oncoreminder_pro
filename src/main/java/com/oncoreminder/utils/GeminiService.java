package com.oncoreminder.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

public class GeminiService {

    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private final String apiKey;
    private final HttpClient client;

    public GeminiService() {
        this.apiKey = loadApiKey();
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    private String loadApiKey() {
        try (InputStream is = GeminiService.class.getResourceAsStream("/gemini.properties")) {
            if (is == null) throw new RuntimeException("gemini.properties introuvable");
            Properties props = new Properties();
            props.load(is);
            String key = props.getProperty("api.key", "").trim();
            if (key.isEmpty() || key.equals("VOTRE_CLE_API_ICI"))
                throw new RuntimeException("Clé API Gemini non configurée dans gemini.properties");
            return key;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de lire gemini.properties : " + e.getMessage());
        }
    }

    public String generateArticle(String organe, String cancerType) throws Exception {
        String prompt = buildPrompt(organe, cancerType);

        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(part);

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.7);
        genConfig.addProperty("maxOutputTokens", 1200);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", genConfig);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String detail = extractApiError(response.body());
            throw new RuntimeException("Erreur API Gemini (" + response.statusCode() + ") : " + detail);
        }

        return extractText(response.body());
    }

    private String extractApiError(String json) {
        try {
            return JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject("error")
                .get("message").getAsString();
        } catch (Exception e) {
            return json.length() > 200 ? json.substring(0, 200) : json;
        }
    }

    private String extractText(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root
            .getAsJsonArray("candidates")
            .get(0).getAsJsonObject()
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).getAsJsonObject()
            .get("text").getAsString();
    }

    private String buildPrompt(String organe, String cancerType) {
        String sujet = (cancerType != null && !cancerType.isEmpty())
            ? "le " + cancerType + " (organe : " + organe + ")"
            : "le cancer lié à : " + organe;

        return "Tu es un médecin oncologue expert. Génère un article médical en français structuré en Markdown " +
               "destiné aux patients, sur le thème de " + sujet + ".\n\n" +
               "INSTRUCTIONS STRICTES :\n" +
               "- Première ligne : un titre accrocheur commençant par '# '\n" +
               "- Sections obligatoires avec '## ' : Introduction, Qu'est-ce que c'est ?, " +
               "Symptômes principaux, Diagnostic, Options de traitement, Conseils aux patients\n" +
               "- Utilise **gras** pour les termes médicaux importants\n" +
               "- Utilise des listes '- ' pour les énumérations\n" +
               "- Ton professionnel mais accessible aux patients non-spécialistes\n" +
               "- Longueur : environ 500 mots\n" +
               "- Toute dernière ligne OBLIGATOIRE au format exact : " +
               "'TAGS: ' suivi de 5 à 7 tags séparés par des virgules " +
               "(ex: oncologie, " + organe.toLowerCase() + ", chimiothérapie)";
    }

    // ── Parsing du résultat ───────────────────────────────────

    public static String extractTitle(String generated) {
        for (String line : generated.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) return trimmed.substring(2).trim();
        }
        return "";
    }

    public static String extractTags(String generated) {
        String[] lines = generated.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("TAGS:")) return trimmed.substring(5).trim();
        }
        return "";
    }

    public static String extractContent(String generated) {
        String[] lines = generated.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("TAGS:")) continue;
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }
}
