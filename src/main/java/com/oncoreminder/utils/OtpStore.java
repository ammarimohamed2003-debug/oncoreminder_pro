package com.oncoreminder.utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * OtpStore — stockage temporaire des OTP en mémoire.
 * Chaque entrée contient le code OTP + sa date d'expiration (5 minutes).
 * Singleton thread-safe via double-checked locking.
 */
public class OtpStore {

    private static volatile OtpStore instance;

    /** Durée de validité d'un OTP en minutes */
    private static final int OTP_VALIDITY_MINUTES = 5;

    /** Map : email → OtpEntry */
    private final Map<String, OtpEntry> store = new HashMap<>();

    private OtpStore() {}

    public static OtpStore getInstance() {
        if (instance == null) {
            synchronized (OtpStore.class) {
                if (instance == null) {
                    instance = new OtpStore();
                }
            }
        }
        return instance;
    }

    // ── Interne : une entrée OTP ──────────────────────────────────────
    private static class OtpEntry {
        final String code;
        final LocalDateTime expiresAt;

        OtpEntry(String code) {
            this.code      = code;
            this.expiresAt = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    // ── API publique ──────────────────────────────────────────────────

    /**
     * Génère un OTP aléatoire de 6 chiffres, le stocke et le retourne.
     */
    public String generateAndStore(String email) {
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        store.put(email.toLowerCase(), new OtpEntry(otp));
        System.out.println("[OtpStore] OTP généré pour " + email + " : " + otp);
        return otp;
    }

    /**
     * Vérifie l'OTP fourni pour cet email.
     * @return OtpResult.VALID, OtpResult.EXPIRED ou OtpResult.INVALID
     */
    public OtpResult verify(String email, String inputCode) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return OtpResult.INVALID;
        if (entry.isExpired()) {
            store.remove(email.toLowerCase());
            return OtpResult.EXPIRED;
        }
        if (entry.code.equals(inputCode.trim())) {
            store.remove(email.toLowerCase()); // usage unique
            return OtpResult.VALID;
        }
        return OtpResult.INVALID;
    }

    /** Supprime l'OTP stocké pour cet email (annulation). */
    public void clear(String email) {
        store.remove(email.toLowerCase());
    }

    /** Résultat de la vérification OTP */
    public enum OtpResult {
        VALID, EXPIRED, INVALID
    }
}
