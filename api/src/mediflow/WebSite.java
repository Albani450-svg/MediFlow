package mediflow;

import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Halaman HTML dan sesi per jendela. Token ada di URL dan sessionStorage. */
final class WebSite {
    private static final Object KUNCI = new Object();
    private static final LinkedHashMap<String, Sesi> SESI = new LinkedHashMap<>(32, 0.75f, true);
    private static final int BATAS_FORM = 2_000_000;

    private WebSite() {}

    static boolean layani(HttpExchange tukar, String metode, String jalur) throws IOException {
        if (berkas(tukar, metode, jalur)) {
            return true;
        }
        if ("POST".equals(metode) && ("/tindakan".equals(jalur) || "/masuk".equals(jalur))) {
            synchronized (VisitStore.GERBANG) {
                pos(tukar);
            }
            return true;
        }
        if ("/".equals(jalur) || "/app".equals(jalur) || "/masuk".equals(jalur) || "/daftar".equals(jalur)) {
            if (!"GET".equals(metode)) {
                tulisHtml(tukar, 405, singkat("Metode tidak didukung."));
                return true;
            }
            synchronized (VisitStore.GERBANG) {
                buka(tukar, jalur);
            }
            return true;
        }
        tulisHtml(tukar, 404, singkat("Halaman tidak ditemukan."));
        return true;
    }

    static void selaraskanSesi() {
        List<VisitStore> toko = new ArrayList<>();
        synchronized (KUNCI) {
            for (Sesi sesi : SESI.values()) {
                toko.add(sesi.toko);
            }
        }
        for (VisitStore item : toko) {
            item.selaraskanAkun();
        }
    }

    private static String halamanPublik(String jalur) {
        if ("/app".equals(jalur)) {
            return "/app";
        }
        if ("/daftar".equals(jalur)) {
            return "/daftar";
        }
        return "/";
    }

    private static void buka(HttpExchange tukar, String jalur) throws IOException {
        Map<String, String> pertanyaan = kueri(tukar);
        String token = pertanyaan.get("sesi");
        String halaman = halamanPublik(jalur);
        if (!tokenSah(token)) {
            token = tokenBaru();
            ambil(token);
            alih(tukar, 302, halaman + "?sesi=" + token);
            return;
        }
        Sesi sesi = ambil(token);
        synchronized (sesi) {
            boolean masuk = sesi.toko.masuk();
            if (masuk && !"/app".equals(jalur)) {
                alih(tukar, 302, "/app?sesi=" + token);
                return;
            }
            if (!masuk && "/app".equals(jalur)) {
                alih(tukar, 302, "/?sesi=" + token);
                return;
            }
            sesi.layar.token = token;
            sesi.layar.halaman = halaman;
            sesi.layar.tujuan = halaman;
            if (!masuk && "/".equals(halaman)) {
                sesi.layar.akunDariAlamat(pertanyaan.get("akun"));
            }
            try {
                tulisHtml(tukar, 200, sesi.layar.html());
            } catch (RuntimeException error) {
                System.err.println("[mediflow] Halaman gagal: " + error);
                tulisHtml(tukar, 500, singkat("Halaman gagal dimuat."));
            }
        }
    }

    private static void pos(HttpExchange tukar) throws IOException {
        Map<String, String> form;
        try {
            form = pasangan(new String(baca(tukar), StandardCharsets.UTF_8));
        } catch (IOException error) {
            tulisHtml(tukar, 400, singkat("Form terlalu besar."));
            return;
        }
        String token = form.get("sesi");
        if (!tokenSah(token)) {
            token = kueri(tukar).get("sesi");
        }
        if (!tokenSah(token)) {
            token = tokenBaru();
            Sesi sesi = ambil(token);
            synchronized (sesi) {
                sesi.layar.kabar("Sesi berakhir. Masuk lagi.", true);
            }
            alih(tukar, 303, "/?sesi=" + token);
            return;
        }
        Sesi sesi = ambil(token);
        synchronized (sesi) {
            sesi.layar.token = token;
            sesi.layar.tindakan(form);
            alih(tukar, 303, halamanPublik(sesi.layar.tujuan) + "?sesi=" + token);
        }
    }

    private static Sesi ambil(String token) {
        synchronized (KUNCI) {
            Sesi ada = SESI.get(token);
            if (ada != null) {
                return ada;
            }
        }
        VisitStore toko = VisitStore.dariServer();
        synchronized (KUNCI) {
            Sesi ada = SESI.get(token);
            if (ada != null) {
                return ada;
            }
            Sesi baru = new Sesi(token, toko);
            SESI.put(token, baru);
            while (SESI.size() > 40) {
                String lama = SESI.keySet().iterator().next();
                SESI.remove(lama);
            }
            return baru;
        }
    }

    private static String tokenBaru() {
        synchronized (KUNCI) {
            String token;
            do {
                token = PasswordHash.randomToken(16);
            } while (SESI.containsKey(token));
            return token;
        }
    }

    private static boolean tokenSah(String token) {
        return token != null && token.matches("[A-Za-z0-9_\\-]{16,64}");
    }

    /** Jalur ikon yang diminta peramban, dipetakan ke berkas di public/assets/icons. */
    private static String ikonAlias(String jalur) {
        return switch (jalur) {
            case "/favicon.ico" -> "assets/icons/48x48.png";
            case "/apple-touch-icon.png", "/apple-touch-icon-precomposed.png" -> "assets/icons/192x192.png";
            default -> null;
        };
    }

    private static boolean berkas(HttpExchange tukar, String metode, String jalur) throws IOException {
        if (!"GET".equals(metode) && !"HEAD".equals(metode)) {
            return false;
        }
        if (jalur.indexOf("..") >= 0 || jalur.indexOf('\\') >= 0 || !jalur.startsWith("/")) {
            return false;
        }
        Path publik = akar().resolve("public").normalize();
        String relatif = ikonAlias(jalur);
        if (relatif == null) {
            relatif = jalur.substring(1);
        }
        Path tujuan = publik.resolve(relatif).normalize();
        if (!tujuan.startsWith(publik) || !Files.isRegularFile(tujuan)) {
            return false;
        }
        byte[] isi = Files.readAllBytes(tujuan);
        tukar.getResponseHeaders().set("Content-Type", tipe(tujuan.getFileName().toString()));
        tukar.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
        tukar.sendResponseHeaders(200, "HEAD".equals(metode) ? -1 : isi.length);
        if (!"HEAD".equals(metode)) {
            tukar.getResponseBody().write(isi);
        }
        return true;
    }

    private static String tipe(String nama) {
        String huruf = nama.toLowerCase(Locale.ROOT);
        if (huruf.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (huruf.endsWith(".js")) {
            return "text/javascript; charset=UTF-8";
        }
        if (huruf.endsWith(".json")) {
            return "application/manifest+json; charset=UTF-8";
        }
        if (huruf.endsWith(".png")) {
            return "image/png";
        }
        if (huruf.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (huruf.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        return "application/octet-stream";
    }

    static Path akar() {
        Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        if (Files.isRegularFile(cwd.resolve("public").resolve("manifest.json"))) {
            return cwd;
        }
        Path induk = cwd.getParent();
        if (induk != null && Files.isRegularFile(induk.resolve("public").resolve("manifest.json"))) {
            return induk;
        }
        try {
            Path kode = Path.of(ApplicationServer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath().normalize();
            for (int i = 0; i < 4 && kode != null; i++) {
                if (Files.isRegularFile(kode.resolve("public").resolve("manifest.json"))) {
                    return kode;
                }
                kode = kode.getParent();
            }
        } catch (URISyntaxException | IllegalArgumentException error) {
            System.err.println("[mediflow] Akar proyek tidak dikenali: " + error.getMessage());
        }
        return cwd;
    }

    private static void alih(HttpExchange tukar, int status, String lokasi) throws IOException {
        tukar.getResponseHeaders().set("Location", lokasi);
        tukar.getResponseHeaders().set("Cache-Control", "no-store");
        tukar.sendResponseHeaders(status, -1);
    }

    private static void tulisHtml(HttpExchange tukar, int status, String html) throws IOException {
        byte[] isi = html.getBytes(StandardCharsets.UTF_8);
        tukar.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        tukar.getResponseHeaders().set("Cache-Control", "no-store");
        tukar.sendResponseHeaders(status, isi.length);
        tukar.getResponseBody().write(isi);
    }

    private static String singkat(String pesan) {
        return "<!DOCTYPE html><html lang=\"id\"><meta charset=\"utf-8\"><title>MediFlow</title><p>"
                + HtmlPages.e(pesan) + "</p></html>";
    }

    private static Map<String, String> kueri(HttpExchange tukar) {
        return pasangan(tukar.getRequestURI().getRawQuery());
    }

    private static Map<String, String> pasangan(String mentah) {
        Map<String, String> hasil = new LinkedHashMap<>();
        if (mentah == null || mentah.isEmpty()) {
            return hasil;
        }
        for (String bagian : mentah.split("&")) {
            if (bagian.isEmpty()) {
                continue;
            }
            int sama = bagian.indexOf('=');
            String kunci = url(sama < 0 ? bagian : bagian.substring(0, sama));
            String nilai = url(sama < 0 ? "" : bagian.substring(sama + 1));
            hasil.put(kunci, nilai);
        }
        return hasil;
    }

    private static String url(String teks) {
        try {
            return URLDecoder.decode(teks, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            return teks;
        }
    }

    private static byte[] baca(HttpExchange tukar) throws IOException {
        try (InputStream masuk = tukar.getRequestBody(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] potong = new byte[8192];
            int jumlah;
            int total = 0;
            while ((jumlah = masuk.read(potong)) >= 0) {
                total += jumlah;
                if (total > BATAS_FORM) {
                    throw new IOException("Body terlalu besar.");
                }
                buffer.write(potong, 0, jumlah);
            }
            return buffer.toByteArray();
        }
    }

    private static final class Sesi {
        final VisitStore toko;
        final HtmlPages layar;

        Sesi(String token, VisitStore toko) {
            this.toko = toko;
            this.layar = new HtmlPages(toko);
            this.layar.token = token;
        }
    }
}
