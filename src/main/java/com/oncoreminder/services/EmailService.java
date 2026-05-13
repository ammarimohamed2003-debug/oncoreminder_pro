package com.oncoreminder.services;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EmailService {

    private static final String GMAIL_USER         = "votre.email@gmail.com";
    private static final String GMAIL_APP_PASSWORD = "xxxx xxxx xxxx xxxx";

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    public static boolean isConfigured() {
        return !GMAIL_USER.startsWith("votre.email") &&
               !GMAIL_APP_PASSWORD.startsWith("xxxx");
    }

    public static void sendOtpEmail(String toEmail, String otpCode) throws Exception {
        if (!isConfigured()) throw new Exception("EmailService non configuré — mode DEV activé.");
        sendMail(toEmail, "Réinitialisation mot de passe - OncoReminder", buildHtmlBody(otpCode));
        System.out.println("[EmailService] ✅ Email OTP envoyé à : " + toEmail);
    }

    // ── Helpers SMTP ──────────────────────────────────────────────────

    private static void writeLine(PrintWriter w, String line) {
        w.println(line);
        w.flush();
    }

    private static String readSmtpResponse(BufferedReader reader, String expectedCode)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
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

    private static void sendMail(String toEmail, String subject, String htmlBody) throws Exception {
        try (Socket socket = new Socket(SMTP_HOST, SMTP_PORT)) {
            socket.setSoTimeout(15_000);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            readSmtpResponse(reader, "220");
            writeLine(writer, "EHLO localhost");
            readSmtpResponse(reader, "250");
            writeLine(writer, "STARTTLS");
            readSmtpResponse(reader, "220");

            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) sslFactory.createSocket(socket, SMTP_HOST, SMTP_PORT, true);
            sslSocket.startHandshake();

            BufferedReader sr = new BufferedReader(
                new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter sw = new PrintWriter(
                new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true);

            writeLine(sw, "EHLO localhost");
            readSmtpResponse(sr, "250");
            writeLine(sw, "AUTH LOGIN");
            readSmtpResponse(sr, "334");
            writeLine(sw, base64(GMAIL_USER));
            readSmtpResponse(sr, "334");
            writeLine(sw, base64(GMAIL_APP_PASSWORD));
            readSmtpResponse(sr, "235");

            writeLine(sw, "MAIL FROM:<" + GMAIL_USER + ">");
            readSmtpResponse(sr, "250");
            writeLine(sw, "RCPT TO:<" + toEmail + ">");
            readSmtpResponse(sr, "250");
            writeLine(sw, "DATA");
            readSmtpResponse(sr, "354");

            sw.println("From: OncoReminder Pro <" + GMAIL_USER + ">");
            sw.println("To: " + toEmail);
            sw.println("Subject: =?UTF-8?B?" + base64(subject) + "?=");
            sw.println("MIME-Version: 1.0");
            sw.println("Content-Type: text/html; charset=UTF-8");
            sw.println("Content-Transfer-Encoding: quoted-printable");
            sw.println();
            sw.println(htmlBody);
            sw.println(".");
            sw.flush();
            readSmtpResponse(sr, "250");
            writeLine(sw, "QUIT");
            readSmtpResponse(sr, "221");
        }
    }

    // ── Template OTP ──────────────────────────────────────────────────

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
