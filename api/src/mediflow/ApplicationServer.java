package mediflow;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** Aplikasi dan API MediFlow di 127.0.0.1:8088. */
public final class ApplicationServer {
    private static final int BATAS_BODY = 20_000_000;

    public static void main(String[] args) throws IOException {
        int port = 8088;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        } catch (IOException error) {
            System.err.println("[mediflow] Port " + port + " sudah dipakai. Tutup server lama, lalu jalankan lagi.");
            System.exit(1);
            return;
        }
        server.createContext("/", ApplicationServer::tangani);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[mediflow] Aplikasi Java di http://127.0.0.1:" + port + "/");
        System.out.println("[mediflow] API Java di http://127.0.0.1:" + port + "/api/database");
        System.out.println("[mediflow] Database " + Database.nama() + " di 127.0.0.1:3306");
        System.out.flush();
    }

    private static void tangani(HttpExchange tukar) throws IOException {
        try {
            if (!lokal(tukar.getRemoteAddress().getAddress())) {
                kirim(tukar, 403, tolakLokal());
                return;
            }
            String jalur = tukar.getRequestURI().getPath();
            if (jalur == null || jalur.isEmpty()) {
                jalur = "/";
            }
            if (jalur.length() > 1 && jalur.endsWith("/")) {
                jalur = jalur.substring(0, jalur.length() - 1);
            }
            String metode = tukar.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("/api".equals(jalur)) {
                if ("GET".equals(metode)) {
                    kirim(tukar, 200, sehat());
                } else {
                    kirim(tukar, 404, tidakAda());
                }
                return;
            }
            if ("/".equals(jalur) && "GET".equals(metode) && mintaJson(tukar)) {
                kirim(tukar, 200, sehat());
                return;
            }
            if ("/api/database".equals(jalur)) {
                database(tukar, metode);
                return;
            }
            if (jalur.startsWith("/api/auth/register")) {
                registrasiPasien(tukar, metode, jalur);
                return;
            }
            if (jalur.startsWith("/api/")) {
                sumber(tukar, metode, jalur.substring("/api/".length()));
                return;
            }
            WebSite.layani(tukar, metode, jalur);
        } finally {
            tukar.close();
        }
    }

    private static void registrasiPasien(HttpExchange tukar, String metode, String jalur) throws IOException {
        tukar.getResponseHeaders().set("Cache-Control", "no-store");
        if (!"/api/auth/register".equals(jalur)
                && !"/api/auth/register/verify-otp".equals(jalur)
                && !"/api/auth/register/profile".equals(jalur)) {
            kirim(tukar, 404, tidakAda());
            return;
        }
        if (!"POST".equals(metode)) {
            tukar.getResponseHeaders().set("Allow", "POST");
            kirim(tukar, 405, Map.of("status", false, "message", "Gunakan metode POST."));
            return;
        }
        byte[] body;
        try {
            body = bacaBodyTerbatas(tukar, 16_384);
        } catch (IOException error) {
            kirim(tukar, 413, Map.of("status", false, "message", "Body registrasi terlalu besar."));
            return;
        }
        Map<String, Object> data;
        try {
            data = JsonConverter.objek(body);
        } catch (JsonConverter.Rusak error) {
            kirim(tukar, 400, Map.of("status", false, "message", "Body harus berupa objek JSON yang valid."));
            return;
        } catch (RuntimeException error) {
            kirim(tukar, 400, Map.of("status", false, "message", "Body harus berupa objek JSON yang valid."));
            return;
        }
        String ip = tukar.getRemoteAddress().getAddress().getHostAddress();
        PatientRegistration.Result result;
        if ("/api/auth/register".equals(jalur)) {
            result = PatientRegistration.daftar(data, ip);
        } else if ("/api/auth/register/verify-otp".equals(jalur)) {
            result = PatientRegistration.verifikasi(data, ip);
        } else {
            result = PatientRegistration.lengkapiProfil(data);
        }
        if (result.status() >= 200 && result.status() < 300) {
            WebSite.selaraskanSesi();
        }
        kirim(tukar, result.status(), result.body());
    }

    private static byte[] bacaBodyTerbatas(HttpExchange tukar, int batas) throws IOException {
        try (InputStream masuk = tukar.getRequestBody(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] potong = new byte[2048];
            int jumlah;
            int total = 0;
            while ((jumlah = masuk.read(potong)) >= 0) {
                total += jumlah;
                if (total > batas) {
                    throw new IOException("Body registrasi terlalu besar.");
                }
                buffer.write(potong, 0, jumlah);
            }
            return buffer.toByteArray();
        }
    }

    private static void database(HttpExchange tukar, String metode) throws IOException {
        if ("PUT".equals(metode) || "POST".equals(metode)) {
            kirim(tukar, 403, Map.of(
                    "ok", false,
                    "error", "Snapshot database bersifat hanya-baca. Gunakan endpoint khusus yang terotorisasi."
            ));
            return;
        }
        try (Connection db = Database.open()) {
            if ("GET".equals(metode)) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("ok", true);
                body.put("data", DatabaseSnapshot.baca(db));
                kirim(tukar, 200, body);
                return;
            }
            if ("PUT".equals(metode) || "POST".equals(metode)) {
                Map<String, Object> data = JsonConverter.objekSnapshot(bacaBody(tukar));
                DatabaseSnapshot.simpan(db, data);
                kirim(tukar, 200, Map.of("ok", true));
                return;
            }
            kirim(tukar, 404, tidakAda());
        } catch (JsonConverter.Rusak error) {
            kirim(tukar, 400, Map.of("ok", false, "error", error.getMessage()));
        } catch (Exception error) {
            System.err.println("[mediflow] " + ColumnValues.pesan(error));
            kirim(tukar, 500, Map.of("ok", false, "error", ColumnValues.pesan(error)));
        }
    }

    private static void sumber(HttpExchange tukar, String metode, String sisa) throws IOException {
        String[] bagian = sisa.split("/");
        if (bagian.length == 0 || bagian.length > 2 || bagian[0].isEmpty()) {
            kirim(tukar, 404, tidakAda());
            return;
        }
        TableCatalog.Table tabel = TableCatalog.BY_NAME.get(bagian[0]);
        if (tabel == null) {
            kirim(tukar, 404, tidakAda());
            return;
        }
        if (("accounts".equals(tabel.name()) || "patients".equals(tabel.name())
                || "admins".equals(tabel.name()) || "farmasi_staff".equals(tabel.name()))
                && ("POST".equals(metode) || "PUT".equals(metode)
                || "PATCH".equals(metode) || "DELETE".equals(metode))) {
            kirim(tukar, 403, Map.of(
                    "status", false,
                    "message", "Akun dan profil pasien tidak dapat diubah melalui CRUD umum. Gunakan alur registrasi atau administrasi staf."
            ));
            return;
        }
        Long id = null;
        if (bagian.length == 2) {
            if (!bagian[1].matches("\\d+") || bagian[1].length() > 18) {
                kirim(tukar, 404, tidakAda());
                return;
            }
            id = Long.parseLong(bagian[1]);
        }
        try (Connection db = Database.open()) {
            TableEndpoint.Jawaban jawaban = layani(db, tabel, metode, id, tukar);
            if (jawaban == null) {
                kirim(tukar, 404, tidakAda());
                return;
            }
            kirim(tukar, jawaban.status(), jawaban.body());
        } catch (JsonConverter.Rusak error) {
            kirim(tukar, 400, gagal(error.getMessage()));
        } catch (Exception error) {
            System.err.println("[mediflow] " + ColumnValues.pesan(error));
            kirim(tukar, 500, gagal(ColumnValues.pesan(error)));
        }
    }

    private static TableEndpoint.Jawaban layani(
            Connection db, TableCatalog.Table tabel, String metode, Long id, HttpExchange tukar) throws Exception {
        if (id == null && "GET".equals(metode)) {
            return TableEndpoint.daftar(db, tabel, kueri(tukar.getRequestURI().getRawQuery()));
        }
        if (id == null && "POST".equals(metode)) {
            return TableEndpoint.buat(db, tabel, JsonConverter.objek(bacaBody(tukar)));
        }
        if (id != null && "GET".equals(metode)) {
            return TableEndpoint.satu(db, tabel, id);
        }
        if (id != null && ("PUT".equals(metode) || "PATCH".equals(metode))) {
            return TableEndpoint.ubah(db, tabel, id, JsonConverter.objek(bacaBody(tukar)));
        }
        if (id != null && "DELETE".equals(metode)) {
            return TableEndpoint.hapus(db, tabel, id);
        }
        return null;
    }

    private static Map<String, String> kueri(String mentah) {
        Map<String, String> hasil = new LinkedHashMap<>();
        if (mentah == null || mentah.isEmpty()) {
            return hasil;
        }
        for (String pasangan : mentah.split("&")) {
            if (pasangan.isEmpty()) {
                continue;
            }
            int sama = pasangan.indexOf('=');
            String kunci = url(sama < 0 ? pasangan : pasangan.substring(0, sama));
            String nilai = url(sama < 0 ? "" : pasangan.substring(sama + 1));
            hasil.put(kunci, nilai);
        }
        return hasil;
    }

    private static String url(String teks) {
        return URLDecoder.decode(teks, StandardCharsets.UTF_8);
    }

    private static byte[] bacaBody(HttpExchange tukar) throws IOException {
        try (InputStream masuk = tukar.getRequestBody(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] potong = new byte[8192];
            int jumlah;
            int total = 0;
            while ((jumlah = masuk.read(potong)) >= 0) {
                total += jumlah;
                if (total > BATAS_BODY) {
                    throw new IOException("Body terlalu besar.");
                }
                buffer.write(potong, 0, jumlah);
            }
            return buffer.toByteArray();
        }
    }

    private static boolean lokal(InetAddress alamat) {
        if (alamat instanceof Inet4Address) {
            return "127.0.0.1".equals(alamat.getHostAddress());
        }
        return alamat.isLoopbackAddress();
    }

    private static Map<String, Object> tolakLokal() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("status", false);
        body.put("message", "API hanya melayani komputer ini.");
        body.put("error", "API hanya melayani komputer ini.");
        return body;
    }

    private static Map<String, Object> tidakAda() {
        return Map.of("status", false, "message", "Jalur API tidak ditemukan.");
    }

    private static Map<String, Object> gagal(String message) {
        return Map.of("status", false, "message", message);
    }

    private static Map<String, Object> sehat() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("service", "mediflow");
        body.put("language", "java");
        return body;
    }

    private static boolean mintaJson(HttpExchange tukar) {
        String accept = tukar.getRequestHeaders().getFirst("Accept");
        if (accept == null) {
            return false;
        }
        String huruf = accept.toLowerCase(Locale.ROOT);
        return huruf.contains("application/json") && !huruf.contains("text/html");
    }

    private static void kirim(HttpExchange tukar, int status, Object body) throws IOException {
        byte[] byteBody = JsonConverter.MAPPER.writeValueAsBytes(body);
        tukar.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        tukar.sendResponseHeaders(status, byteBody.length);
        tukar.getResponseBody().write(byteBody);
    }
}