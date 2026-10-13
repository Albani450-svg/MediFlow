package mediflow;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Hash kata sandi demo dan penyamaran NIK. Produksi memakai hash lambat di server. */
final class PasswordHash {
    private static final SecureRandom ACAK = new SecureRandom();

    private PasswordHash() {}

    static String hashPassword(String plain) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    static String maskNik(String plain) {
        if (plain == null || plain.length() < 8) {
            return "••••";
        }
        return plain.substring(0, 4) + "••••••••" + plain.substring(plain.length() - 4);
    }

    static String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        ACAK.nextBytes(buf);
        StringBuilder hex = new StringBuilder(buf.length * 2);
        for (byte b : buf) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    static String randomPin() {
        int n = ACAK.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
