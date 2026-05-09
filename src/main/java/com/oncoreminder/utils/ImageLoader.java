package com.oncoreminder.utils;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

public class ImageLoader {

    /**
     * Charge un fichier image (PNG, JPEG, GIF, BMP) en tant que JavaFX Image.
     * Essaie d'abord le décodeur JavaFX, puis ImageIO si JavaFX échoue
     * (utile pour les PNG avec profil couleur inhabituel, CMYK, etc.)
     */
    public static Image load(File f) {
        if (f == null || !f.exists()) return null;

        // Tentative 1 : URL file:/// (trois slashes, Windows compatible)
        try {
            String url = "file:///" + f.getAbsolutePath().replace('\\', '/').replace(" ", "%20");
            Image img = new Image(url);
            if (!img.isError()) return img;
        } catch (Exception ignored) {}

        // Tentative 2 : FileInputStream
        try (FileInputStream fis = new FileInputStream(f)) {
            Image img = new Image(fis);
            if (!img.isError()) return img;
        } catch (Exception ignored) {}

        // Tentative 3 : ImageIO → re-encode PNG → JavaFX (gère tous les formats)
        try {
            BufferedImage bi = ImageIO.read(f);
            if (bi == null) {
                System.out.println("[ImageLoader] ImageIO ne reconnaît pas : " + f.getName());
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            Image img = new Image(new ByteArrayInputStream(baos.toByteArray()));
            if (!img.isError()) return img;
        } catch (Exception e) {
            System.out.println("[ImageLoader] Échec final : " + e.getMessage());
        }

        return null;
    }
}
