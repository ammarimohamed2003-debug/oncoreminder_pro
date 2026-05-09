package com.oncoreminder.services;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * EmailService — Envoi d'emails via SMTP Gmail.
 * Implémentation pure Java (aucune dépendance externe).
 *
 * ⚠️ Configuration requise :
 *   1. Activez l'authentification à 2 facteurs sur votre compte Gmail.
 *   2. Créez un "Mot de passe d'application" (Compte Google → Sécurité → App Passwords).
 *   3. Remplacez GMAIL_USER et GMAIL_APP_PASSWORD ci-dessous.
 *
 * 💡 Mode DEV : Si non configuré, le code OTP apparaît dans la console IntelliJ.
 */
public class EmailService {

    // ── ⚙️  CONFIGURATION — Modifiez ces deux valeurs ─────────────────
    private static final String GMAIL_USER         = "votre.email@gmail.com";   // ← Votre Gmail
    private static final String GMAIL_APP_PASSWORD = "xxxx xxxx xxxx xxxx";     // ← App Password Google
    // ──────────────────────────────────────────────────────────────────

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    /**
     * Vérifie si l'email est configuré (non placeholder).
     */
    public static boolean isConfigured() {
        return !GMAIL_USER.startsWith("votre.email") &&
               !GMAIL_APP_PASSWORD.startsWith("xxxx");
    }

    /**
     * Envoie un email OTP HTML au destinataire via SMTP Gmail avec STARTTLS.
     * Utilise uniquement les APIs Java standard (javax.net.ssl).
     *
     * @param toEmail  Adresse email du destinataire
     * @param otpCode  Code OTP 6 chiffres
     * @throws Exception si l'envoi échoue
     */
    public static void sendOtpEmail(String toEmail, String otpCode) throws Exception {
        if (!isConfigured()) {
            throw new Exception("EmailService non configuré — mode DEV activé.");
        }

        // ── Étape 1 : Connexion TCP plain-text sur port 587 ───────────
        try (Socket socket = new Socket(SMTP_HOST, SMTP_PORT)) {
            socket.setSoTimeout(15_000); // 15 secondes timeout

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Lire le message de bienvenue du serveur : "220 smtp.gmail.com ..."
            readSmtpResponse(reader, "220");

            // ── EHLO ──────────────────────────────────────────────────
            writeLine(writer, "EHLO localhost");
            readSmtpResponse(reader, "250");

            // ── STARTTLS — demande de mise à niveau en SSL ────────────
            writeLine(writer, "STARTTLS");
            readSmtpResponse(reader, "220");

            // ── Mise à niveau SSL ──────────────────────────────────────
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, SMTP_HOST, SMTP_PORT, true);
            sslSocket.startHandshake();

            BufferedReader sslReader = new BufferedReader(
                new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter sslWriter = new PrintWriter(
                new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true);

            // ── EHLO à nouveau après STARTTLS ──────────────────────────
            writeLine(sslWriter, "EHLO localhost");
            readSmtpResponse(sslReader, "250");

            // ── AUTH LOGIN ────────────────────────────────────────────
            writeLine(sslWriter, "AUTH LOGIN");
            readSmtpResponse(sslReader, "334");

            writeLine(sslWriter, base64(GMAIL_USER));
            readSmtpResponse(sslReader, "334");

            writeLine(sslWriter, base64(GMAIL_APP_PASSWORD));
            readSmtpResponse(sslReader, "235"); // 235 = authentification réussie

            // ── MAIL FROM ──────────────────────────────────────────────
            writeLine(sslWriter, "MAIL FROM:<" + GMAIL_USER + ">");
            readSmtpResponse(sslReader, "250");

            // ── RCPT TO ────────────────────────────────────────────────
            writeLine(sslWriter, "RCPT TO:<" + toEmail + ">");
            readSmtpResponse(sslReader, "250");

            // ── DATA ───────────────────────────────────────────────────
            writeLine(sslWriter, "DATA");
            readSmtpResponse(sslReader, "354");

            // Construire l'email RFC 2822
            String subjectB64 = base64("Réinitialisation mot de passe - OncoReminder");
            String htmlBody   = buildHtmlBody(otpCode);

            sslWriter.println("From: OncoReminder Pro <" + GMAIL_USER + ">");
            sslWriter.println("To: " + toEmail);
            sslWriter.println("Subject: =?UTF-8?B?" + subjectB64 + "?=");
            sslWriter.println("MIME-Version: 1.0");
            sslWriter.println("Content-Type: text/html; charset=UTF-8");
            sslWriter.println("Content-Transfer-Encoding: quoted-printable");
            sslWriter.println(); // ligne vide obligatoire entre headers et body
            sslWriter.println(htmlBody);
            sslWriter.println("."); // fin du DATA
            sslWriter.flush();
            readSmtpResponse(sslReader, "250");

            // ── QUIT ───────────────────────────────────────────────────
            writeLine(sslWriter, "QUIT");
            readSmtpResponse(sslReader, "221");

            System.out.println("[EmailService] ✅ Email OTP envoyé à : " + toEmail);
        }
    }

    // ── Helpers SMTP ──────────────────────────────────────────────────

    private static void writeLine(PrintWriter w, String line) {
        w.println(line);
        w.flush();
    }

    /**
     * Lit la réponse du serveur SMTP et vérifie le code attendu.
     * Gère les réponses multi-lignes (ex: EHLO retourne plusieurs lignes 250-...).
     */
    private static String readSmtpResponse(BufferedReader reader, String expectedCode)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
            // Réponse multi-ligne : "250-..." (tiret) vs "250 ..." (espace = dernière ligne)
            if (line.length() >= 4 && line.charAt(3) != '-') break;
        }
        String response = sb.toString();
        if (expectedCode != null && !response.startsWith(expectedCode)) {
            throw new IOException("Réponse SMTP inattendue (attendu " + expectedCode + ") : " + response);
        }
        return response;
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    // ── Template HTML de l'email ──────────────────────────────────────

    private static String buildHtmlBody(String otpCode) {
        return "<!DOCTYPE html>" +
            "<html lang='fr'><head><meta charset='UTF-8'>" +
            "<style>" +
            "body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;margin:0;padding:0}" +
            ".c{max-width:520px;margin:40px auto;background:#fff;border-radius:16px;" +
            "box-shadow:0 4px 24px rgba(43,188,176,.15);overflow:hidden}" +
            ".h{background:linear-gradient(135deg,#0F0626 0%,#2D1B69 60%,#0D5C57 100%);" +
            "padding:32px 24px;text-align:center}" +
            ".h h1{color:#fff;margin:0;font-size:24px;letter-spacing:1px}" +
            ".h p{color:#2BBCB0;margin:6px 0 0;font-size:13px}" +
            ".b{padding:36px 32px;text-align:center}" +
            ".b p{color:#4a5568;font-size:15px;line-height:1.6;margin:0 0 24px}" +
            ".ob{display:inline-block;background:linear-gradient(135deg,#2BBCB0,#4A2D8F);" +
            "border-radius:12px;padding:18px 40px;margin:8px 0 24px}" +
            ".oc{font-size:38px;font-weight:bold;color:#fff;letter-spacing:8px;" +
            "font-family:'Courier New',monospace}" +
            ".exp{color:#e53e3e;font-size:13px;margin-bottom:20px}" +
            ".note{background:#f7faff;border-left:4px solid #2BBCB0;padding:12px 16px;" +
            "border-radius:0 8px 8px 0;font-size:12px;color:#718096;text-align:left}" +
            ".f{background:#f7f7fb;padding:16px;text-align:center;font-size:11px;color:#a0aec0}" +
            "</style></head><body>" +
            "<div class='c'>" +
            "<div class='h'><h1>&#9877; OncoReminder Pro</h1>" +
            "<p>La plateforme intelligente pour le suivi oncologique</p></div>" +
            "<div class='b'>" +
            "<p>Bonjour,<br>Votre code de v&#233;rification est :</p>" +
            "<div class='ob'><div class='oc'>" + otpCode + "</div></div>" +
            "<p class='exp'>&#9201; Ce code expire dans <strong>5 minutes</strong>.</p>" +
            "<div class='note'>&#128274; Si vous n'avez pas demand&#233; cette r&#233;initialisation, " +
            "ignorez cet email. Votre compte reste s&#233;curis&#233;.</div>" +
            "</div>" +
            "<div class='f'>OncoReminder Pro &copy; 2025 &mdash; Ne pas r&#233;pondre.</div>" +
            "</div></body></html>";
    }
}
