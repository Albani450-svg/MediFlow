package mediflow;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash kata sandi demo dan penyamaran NIK. Produksi memakai hash lambat di server. */
final class PasswordHash {
    private static final SecureRandom ACAK = new SecureRandom();
    private static final int ITERASI = 210_000;

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

    static String hashPasswordLambat(String plain) {
        byte[] salt = new byte[16];
        ACAK.nextBytes(salt);
        byte[] hash = derive(plain, salt, ITERASI);
        return "pbkdf2-sha256$" + ITERASI + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    static boolean verifikasiPassword(String plain, String tersimpan) {
        if (tersimpan == null) {
            return false;
        }
        if (!tersimpan.startsWith("pbkdf2-sha256$")) {
            return hashPassword(plain).equals(tersimpan);
        }
        String[] bagian = tersimpan.split("\\$", -1);
        if (bagian.length != 4) {
            return false;
        }
        try {
            int iterasi = Integer.parseInt(bagian[1]);
            if (iterasi < 100_000 || iterasi > 2_000_000) {
                return false;
            }
            byte[] salt = Base64.getUrlDecoder().decode(bagian[2]);
            byte[] expected = Base64.getUrlDecoder().decode(bagian[3]);
            return MessageDigest.isEqual(expected, derive(plain, salt, iterasi));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static byte[] derive(String plain, byte[] salt, int iterasi) {
        PBEKeySpec spec = new PBEKeySpec(plain.toCharArray(), salt, iterasi, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException error) {
            throw new IllegalStateException("Hash kata sandi tidak tersedia.", error);
        } finally {
            spec.clearPassword();
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