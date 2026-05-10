package com.oncoreminder.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oncoreminder.utils.GeminiService;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * Service IA pour la gestion des réclamations.
 *
 * Mode Gemini  : si gemini.properties contient une vraie clé, appels API réels.
 * Mode local   : analyse par mots-clés + templates — fonctionne sans clé API.
 */
public class ReclamationAIService {

    private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private final String     apiKey;
    private final HttpClient client;
    private final boolean    geminiEnabled;

    public ReclamationAIService() {
        String key = null;
        try {
            key = loadApiKey();
        } catch (Exception ignored) {
            // Mode local activé silencieusement
        }
        this.apiKey        = key;
        this.geminiEnabled = key != null;
        this.client        = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        System.out.println("[ReclamationAI] Mode : " + (geminiEnabled ? "Gemini API" : "IA locale (sans clé)"));
    }

    /** Toujours disponible : mode local si pas de clé API. */
    public boolean isAvailable() { return true; }

    // ── Suggestion de réponse ─────────────────────────────────────────

    public String suggestResponse(String sujet, String message) throws Exception {
        if (geminiEnabled) {
            return call(buildResponsePrompt(sujet, message), 0.5, 400);
        }
        return suggestResponseLocal(sujet, message);
    }

    // ── Analyse urgence ───────────────────────────────────────────────

    public String analyzeUrgency(String sujet, String message) throws Exception {
        if (geminiEnabled) {
            String prompt =
                "Analyse cette réclamation d'un patient dans un système médical oncologique.\n" +
                "Sujet : " + sujet + "\n" +
                "Message : " + message + "\n\n" +
                "Réponds UNIQUEMENT par un seul mot parmi : FAIBLE, MOYEN, ÉLEVÉ\n" +
                "Critères : ÉLEVÉ = urgence médicale ou sécurité, MOYEN = problème de service, FAIBLE = insatisfaction générale.";
            String result = call(prompt, 0.1, 10).trim().toUpperCase();
            return switch (result) {
                case "FAIBLE", "MOYEN", "ÉLEVÉ" -> result;
                default -> "MOYEN";
            };
        }
        return analyzeUrgencyLocal(sujet, message);
    }

    // ── Résumé court ──────────────────────────────────────────────────

    public String summarize(String sujet, String message) throws Exception {
        if (geminiEnabled) {
            String prompt =
                "Résume en une seule phrase courte (20 mots max, en français) la réclamation suivante :\n" +
                "Sujet : " + sujet + "\n" +
                "Message : " + message + "\n" +
                "Réponds UNIQUEMENT avec la phrase de résumé, sans introduction ni ponctuation finale.";
            return call(prompt, 0.3, 60).trim();
        }
        return summarizeLocal(sujet, message);
    }

    // ── IA locale (sans clé API) ──────────────────────────────────────

    private String analyzeUrgencyLocal(String sujet, String message) {
        String text = (sujet + " " + message).toLowerCase();
        if (containsAny(text, "urgent", "grave", "danger", "douleur", "erreur médicale",
                         "sécurité", "traitement incorrect", "mauvais médicament", "overdose",
                         "allergie", "réaction", "accident")) {
            return "ÉLEVÉ";
        }
        if (containsAny(text, "problème", "retard", "annul", "report", "délai", "attente",
                         "rendez-vous", "oubli", "manque", "incomplet", "insatisfait")) {
            return "MOYEN";
        }
        return "FAIBLE";
    }

    private String summarizeLocal(String sujet, String message) {
        // Nettoie et tronque à 20 mots
        String clean = message.replaceAll("\n+", " ").trim();
        String[] words = clean.split("\\s+");
        if (words.length <= 18) return clean;
        return String.join(" ", Arrays.copyOf(words, 18)) + "...";
    }

    private String suggestResponseLocal(String sujet, String message) {
        String tone = detectTone(sujet, message);
        return "Madame, Monsieur,\n\n" +
               "Nous avons bien reçu votre réclamation concernant « " + sujet + " » " +
               "et nous vous remercions de nous en avoir informé.\n\n" +
               tone +
               "\n\nNous restons à votre entière disposition pour tout renseignement complémentaire " +
               "et vous assurons que votre satisfaction est notre priorité.\n\n" +
               "Veuillez agréer, Madame, Monsieur, l'expression de nos salutations distinguées.\n\n" +
               "L'équipe administrative\nOncoReminder Pro";
    }

    private String detectTone(String sujet, String message) {
        String text = (sujet + " " + message).toLowerCase();
        if (containsAny(text, "urgent", "grave", "danger", "douleur", "sécurité")) {
            return "Nous comprenons le caractère urgent de votre situation et nous mobilisons " +
                   "immédiatement notre équipe pour traiter votre cas en priorité absolue. " +
                   "Un responsable prendra contact avec vous dans les plus brefs délais.";
        }
        if (containsAny(text, "rendez-vous", "annul", "report", "délai", "retard")) {
            return "Nous prenons note de la difficulté rencontrée concernant votre prise en charge " +
                   "et nous mettons tout en œuvre pour régulariser cette situation rapidement. " +
                   "Notre équipe examine actuellement les disponibilités afin de vous proposer " +
                   "une solution adaptée.";
        }
        return "Nous prenons votre réclamation très au sérieux et notre équipe examine " +
               "attentivement votre dossier. Nous nous engageons à vous apporter une réponse " +
               "complète et une solution satisfaisante dans les meilleurs délais.";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    // ── Appel API Gemini ──────────────────────────────────────────────

    private String call(String prompt, double temperature, int maxTokens) throws Exception {
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject genCfg = new JsonObject();
        genCfg.addProperty("temperature", temperature);
        genCfg.addProperty("maxOutputTokens", maxTokens);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", genCfg);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            String detail = extractError(resp.body());
            throw new RuntimeException("Erreur API Gemini (" + resp.statusCode() + ") : " + detail);
        }
        return extractText(resp.body());
    }

    // ── Prompt Gemini ─────────────────────────────────────────────────

    private String buildResponsePrompt(String sujet, String message) {
        return "Tu es un administrateur professionnel d'une plateforme médicale oncologique (OncoReminder Pro).\n" +
               "Un patient a soumis la réclamation suivante :\n\n" +
               "Sujet : " + sujet + "\n" +
               "Message : " + message + "\n\n" +
               "Rédige une réponse professionnelle, empathique et constructive en français.\n" +
               "RÈGLES STRICTES :\n" +
               "- Commence par 'Madame, Monsieur,' ou une formule de politesse appropriée\n" +
               "- Remercie le patient pour sa réclamation\n" +
               "- Adresse le problème avec clarté\n" +
               "- Propose une solution ou des étapes de résolution\n" +
               "- Termine par une formule de courtoisie professionnelle\n" +
               "- Longueur : 80 à 150 mots\n" +
               "- Ne mentionne PAS de nom propre d'admin\n" +
               "Réponds UNIQUEMENT avec le texte de la réponse, sans titre ni introduction.";
    }

    // ── Parsing ───────────────────────────────────────────────────────

    private String extractText(String json) {
        try {
            return JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonArray("candidates").get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString().trim();
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'extraire la réponse Gemini : " + e.getMessage());
        }
    }

    private String extractError(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject()
                .getAsJsonObject("error").get("message").getAsString();
        } catch (Exception e) {
            return json.length() > 200 ? json.substring(0, 200) : json;
        }
    }

    private String loadApiKey() {
        try (InputStream is = GeminiService.class.getResourceAsStream("/gemini.properties")) {
            if (is == null) throw new RuntimeException("gemini.properties introuvable");
            Properties props = new Properties();
            props.load(is);
            String key = props.getProperty("api.key", "").trim();
            if (key.isEmpty() || key.equals("VOTRE_CLE_API_ICI"))
                throw new RuntimeException("Clé API Gemini non configurée");
            return key;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
