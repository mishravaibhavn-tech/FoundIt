package com.foundit;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hashes passwords with SHA-256 and a per-user random salt.
 * Stored format:  salt:hash   (both Base64)
 */
public class PasswordUtil {

    public static String hash(String plain) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + digest(plain, salt);
    }

    public static boolean verify(String plain, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        String[] parts = stored.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return digest(plain, salt).equals(parts[1]);
    }

    private static String digest(String plain, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return Base64.getEncoder().encodeToString(
                       md.digest(plain.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}