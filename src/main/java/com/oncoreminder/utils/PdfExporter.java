package com.oncoreminder.utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.oncoreminder.models.Article;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfExporter {

    public static void export(Article article, String destPath) throws IOException, DocumentException {
        Document doc = new Document(PageSize.A4, 55, 55, 65, 65);
        PdfWriter.getInstance(doc, new FileOutputStream(destPath));
        doc.open();

        Font brandFont  = new Font(Font.HELVETICA, 11, Font.BOLD,   new Color(43, 188, 176));
        Font titleFont  = new Font(Font.HELVETICA, 22, Font.BOLD,   new Color(58, 29, 122));
        Font h2Font     = new Font(Font.HELVETICA, 14, Font.BOLD,   new Color(58, 29, 122));
        Font h3Font     = new Font(Font.HELVETICA, 13, Font.BOLD,   new Color(91, 53, 165));
        Font labelFont  = new Font(Font.HELVETICA, 10, Font.BOLD,   new Color(91, 53, 165));
        Font metaFont   = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(107, 114, 128));
        Font bodyFont   = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(45, 55, 72));
        Font footerFont = new Font(Font.HELVETICA,  9, Font.ITALIC, new Color(156, 163, 175));

        // Branding
        Paragraph brand = new Paragraph("OncoReminder PRO", brandFont);
        brand.setAlignment(Element.ALIGN_RIGHT);
        doc.add(brand);
        doc.add(Chunk.NEWLINE);

        // Titre
        Paragraph titre = new Paragraph(article.getTitre(), titleFont);
        titre.setSpacingAfter(8);
        doc.add(titre);

        addSeparator(doc, new Color(221, 214, 248), 1.5f);

        // Métadonnées
        if (article.getOrgane() != null && !article.getOrgane().isBlank()) {
            Paragraph p = new Paragraph("Organe : " + article.getOrgane(), labelFont);
            p.setSpacingBefore(6); p.setSpacingAfter(3);
            doc.add(p);
        }
        if (article.getMedecinNom() != null && !article.getMedecinNom().isBlank()) {
            Paragraph p = new Paragraph("Medecin : Dr. " + article.getMedecinNom(), labelFont);
            p.setSpacingAfter(3);
            doc.add(p);
        }
        if (article.getDatePublication() != null) {
            Paragraph p = new Paragraph("Publie le : " + article.getDatePublication(), metaFont);
            p.setSpacingAfter(3);
            doc.add(p);
        }
        Paragraph stats = new Paragraph(
            "Vues : " + article.getViews() + "     J'aime : " + article.getLikes(), metaFont);
        stats.setSpacingAfter(14);
        doc.add(stats);

        addSeparator(doc, new Color(237, 233, 248), 0.5f);

        // Contenu
        String raw = article.getContenu() != null ? article.getContenu() : "";
        renderContent(doc, raw, bodyFont, h2Font, h3Font);

        // Pied de page
        doc.add(Chunk.NEWLINE);
        addSeparator(doc, new Color(200, 200, 220), 0.5f);
        Paragraph footer = new Paragraph(
            "Exporte depuis OncoReminder PRO — " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(6);
        doc.add(footer);

        doc.close();
    }

    private static void addSeparator(Document doc, Color color, float width) throws DocumentException {
        LineSeparator sep = new LineSeparator(width, 100, color, Element.ALIGN_CENTER, -4);
        doc.add(new Chunk(sep));
        doc.add(Chunk.NEWLINE);
    }

    private static void renderContent(Document doc, String raw, Font bodyFont, Font h2Font, Font h3Font)
            throws DocumentException {
        for (String line : raw.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                doc.add(Chunk.NEWLINE);
                continue;
            }
            if (t.startsWith("# ")) {
                Paragraph p = new Paragraph(t.substring(2), h2Font);
                p.setSpacingBefore(10); p.setSpacingAfter(4);
                doc.add(p);
            } else if (t.startsWith("## ") || t.startsWith("### ")) {
                Paragraph p = new Paragraph(t.replaceFirst("^#{2,3} ", ""), h3Font);
                p.setSpacingBefore(7); p.setSpacingAfter(3);
                doc.add(p);
            } else if (t.startsWith("- ") || t.startsWith("* ")) {
                Paragraph p = new Paragraph("•  " + t.substring(2), bodyFont);
                p.setIndentationLeft(16); p.setSpacingAfter(2);
                doc.add(p);
            } else {
                String clean = t
                    .replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1")
                    .replaceAll("_([^_]+)_", "$1")
                    .replaceAll("`([^`]+)`", "$1")
                    .replaceAll("~~([^~]+)~~", "$1");
                Paragraph p = new Paragraph(clean, bodyFont);
                p.setSpacingAfter(4);
                doc.add(p);
            }
        }
    }
}
