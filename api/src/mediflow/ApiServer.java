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

/** API MediFlow di 127.0.0.1:8088. Jalur dan bentuk JSON sama dengan API PHP sebelumnya. */
public final class ApiServer {
    private static final int BATAS_BODY = 20_000_000;

    public static void main(String[] args) throws IOException {
        int port = 8088;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", ApiServer::tangani);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[mediflow] API Java medicflow_db di http://127.0.0.1:" + port + "/api/database");
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
            if ("/".equals(jalur)) {
                kirim(tukar, 200, Map.of("ok", true, "service", "mediflow", "language", "java"));
                return;
            }
            if ("/api/database".equals(jalur)) {
                database(tukar, metode);
                return;
            }
            if (jalur.startsWith("/api/")) {
                sumber(tukar, metode, jalur.substring("/api/".length()));
                return;
            }
            kirim(tukar, 404, tidakAda());
        } finally {
            tukar.close();
        }
    }

    private static void database(HttpExchange tukar, String metode) throws IOException {
        try (Connection db = Db.open()) {
            if ("GET".equals(metode)) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("ok", true);
                body.put("data", Snapshot.baca(db));
                kirim(tukar, 200, body);
                return;
            }
            if ("PUT".equals(metode) || "POST".equals(metode)) {
                Map<String, Object> data = Json.objekSnapshot(bacaBody(tukar));
                Snapshot.simpan(db, data);
                kirim(tukar, 200, Map.of("ok", true));
                return;
            }
            kirim(tukar, 404, tidakAda());
        } catch (Json.Rusak error) {
            kirim(tukar, 400, Map.of("ok", false, "error", error.getMessage()));
        } catch (Exception error) {
            System.err.println("[mediflow] " + Values.pesan(error));
            kirim(tukar, 500, Map.of("ok", false, "error", Values.pesan(error)));
        }
    }

    private static void sumber(HttpExchange tukar, String metode, String sisa) throws IOException {
        String[] bagian = sisa.split("/");
        if (bagian.length == 0 || bagian.length > 2 || bagian[0].isEmpty()) {
            kirim(tukar, 404, tidakAda());
            return;
        }
        Catalog.Table tabel = Catalog.BY_NAME.get(bagian[0]);
        if (tabel == null) {
            kirim(tukar, 404, tidakAda());
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
        try (Connection db = Db.open()) {
            Resource.Jawaban jawaban = layani(db, tabel, metode, id, tukar);
            if (jawaban == null) {
                kirim(tukar, 404, tidakAda());
                return;
            }
            kirim(tukar, jawaban.status(), jawaban.body());
        } catch (Json.Rusak error) {
            kirim(tukar, 400, gagal(error.getMessage()));
        } catch (Exception error) {
            System.err.println("[mediflow] " + Values.pesan(error));
            kirim(tukar, 500, gagal(Values.pesan(error)));
        }
    }

    private static Resource.Jawaban layani(Connection db, Catalog.Table tabel, String metode, Long id, HttpExchange tukar) throws Exception {
        if (id == null && "GET".equals(metode)) {
            return Resource.daftar(db, tabel, kueri(tukar.getRequestURI().getRawQuery()));
        }
        if (id == null && "POST".equals(metode)) {
            return Resource.buat(db, tabel, Json.objek(bacaBody(tukar)));
        }
        if (id != null && "GET".equals(metode)) {
            return Resource.satu(db, tabel, id);
        }
        if (id != null && ("PUT".equals(metode) || "PATCH".equals(metode))) {
            return Resource.ubah(db, tabel, id, Json.objek(bacaBody(tukar)));
        }
        if (id != null && "DELETE".equals(metode)) {
            return Resource.hapus(db, tabel, id);
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

    private static void kirim(HttpExchange tukar, int status, Object body) throws IOException {
        byte[] byteBody = Json.MAPPER.writeValueAsBytes(body);
        tukar.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        tukar.sendResponseHeaders(status, byteBody.length);
        tukar.getResponseBody().write(byteBody);
    }
}
