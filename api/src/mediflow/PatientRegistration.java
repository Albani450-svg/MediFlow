package mediflow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Registrasi pasien publik. Role akun selalu ditetapkan di server. */
final class PatientRegistration {
    record Result(int status, Map<String, Object> body) {}

    private static final String PRIVACY_POLICY_VERSION = "1.0";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private PatientRegistration() {}

    static Result daftar(Map<String, Object> request, String ip) {
        String nama = teks(request.get("nama_lengkap")).trim();
        String email = teks(request.get("email")).trim().toLowerCase(java.util.Locale.ROOT);
        String wa = teks(request.get("no_telepon")).replaceAll("\\s+", "");
        String password = teks(request.get("password"));
        String role = teks(request.get("role")).trim().toLowerCase(java.util.Locale.ROOT);
        if (role.isBlank()) {
            role = "pasien";
        }
        String secret = env("MEDIFLOW_OTP_SECRET");
        String deliveryUrl = env("MEDIFLOW_DELIVERY_URL");
        String deliveryToken = env("MEDIFLOW_DELIVERY_TOKEN");

        if (!("pasien".equals(role) || "dokter".equals(role) || "farmasi".equals(role))
                || nama.isBlank() || nama.length() > 100
                || !email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,50}@[A-Za-z0-9.-]{1,40}\\.[A-Za-z]{2,}$")
                || !wa.matches("^08[0-9]{8,12}$")
                || password.length() < 10 || password.length() > 200) {
            return gagal(422, "Nama, email, nomor WhatsApp, atau kata sandi tidak memenuhi syarat.");
        }
        if (secret.length() < 32 || deliveryToken.isBlank() || !endpointAman(deliveryUrl)) {
            return gagal(503, "Pengiriman OTP belum dikonfigurasi dengan aman. Hubungi administrator.");
        }

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        try (Connection db = Database.open()) {
            if (dibatasi(db, "register-ip", ip, 10, 3600)
                    || dibatasi(db, "register-wa", wa, 3, 3600)) {
                return gagal(429, "Batas pendaftaran atau pengiriman kode per jam tercapai.");
            }
            if (ada(db, "SELECT 1 FROM users WHERE email = ? OR username = ? OR no_telepon = ? LIMIT 1",
                    email, email, wa)) {
                return gagal(409, "Email atau nomor WhatsApp sudah digunakan.");
            }

            boolean autoCommit = db.getAutoCommit();
            db.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO users (username, email, password_hash, role, nama_lengkap, no_telepon, is_active, account_status) "
                                + "VALUES (?, ?, ?, ?, ?, ?, 0, 'PENDING')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, email);
                    ps.setString(2, email);
                    ps.setString(3, PasswordHash.hashPasswordLambat(password));
                    ps.setString(4, role);
                    ps.setString(5, nama);
                    ps.setString(6, wa);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("ID akun pasien tidak terbentuk.");
                        }
                        userId = keys.getLong(1);
                    }
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO otp_verifikasi (id_user, kode_otp, tujuan, expired_at, attempt, created_at) "
                                + "VALUES (?, ?, ?, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), 0, UTC_TIMESTAMP())")) {
                    ps.setLong(1, userId);
                    ps.setString(2, hashOtp(otp, secret));
                    ps.setString(3, wa);
                    ps.executeUpdate();
                }
                kirimOtp(deliveryUrl, deliveryToken, wa, otp);
                db.commit();
                return baik(202, "Kode verifikasi dikirim ke WhatsApp yang didaftarkan.");
            } catch (Exception error) {
                db.rollback();
                System.err.println("[mediflow] Registrasi pasien gagal: " + error.getClass().getSimpleName());
                return gagal(503, "Registrasi belum dapat diproses. Periksa layanan pesan atau coba kembali.");
            } finally {
                db.setAutoCommit(autoCommit);
            }
        } catch (SQLException error) {
            System.err.println("[mediflow] Database registrasi tidak tersedia: " + ColumnValues.pesan(error));
            return gagal(503, "Database registrasi tidak dapat dijangkau.");
        }
    }

    static Result verifikasi(Map<String, Object> request, String ip) {
        String email = teks(request.get("email")).trim().toLowerCase(java.util.Locale.ROOT);
        String kode = teks(request.get("kode_otp")).trim();
        String secret = env("MEDIFLOW_OTP_SECRET");
        if (!emailAman(email) || !kode.matches("^[0-9]{6}$")) {
            return gagal(422, "Email atau kode OTP tidak valid.");
        }
        if (secret.length() < 32) {
            return gagal(503, "Verifikasi OTP belum dikonfigurasi.");
        }
        try (Connection db = Database.open()) {
            if (dibatasi(db, "verify-ip", ip, 20, 3600)
                    || dibatasi(db, "verify-email", email, 10, 3600)) {
                return gagal(429, "Terlalu banyak percobaan verifikasi. Coba kembali nanti.");
            }
            db.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT id_user FROM users WHERE email = ? AND role IN ('pasien','dokter','farmasi') "
                                + "AND account_status = 'PENDING' AND is_active = 0 FOR UPDATE")) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            db.rollback();
                            return gagal(400, "Akun pendaftaran tidak ditemukan atau sudah aktif.");
                        }
                        userId = rs.getLong(1);
                    }
                }

                long otpId;
                String hash;
                int attempts;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT id_otp, kode_otp, attempt FROM otp_verifikasi "
                                + "WHERE id_user = ? AND verified_at IS NULL AND expired_at > UTC_TIMESTAMP() "
                                + "ORDER BY id_otp DESC LIMIT 1 FOR UPDATE")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            db.rollback();
                            return gagal(400, "Kode OTP kedaluwarsa. Daftar kembali untuk meminta kode baru.");
                        }
                        otpId = rs.getLong("id_otp");
                        hash = rs.getString("kode_otp");
                        attempts = rs.getInt("attempt");
                    }
                }
                if (attempts >= 5) {
                    db.rollback();
                    return gagal(429, "Batas percobaan kode OTP tercapai.");
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE otp_verifikasi SET attempt = attempt + 1 WHERE id_otp = ?")) {
                    ps.setLong(1, otpId);
                    ps.executeUpdate();
                }
                if (!MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),
                        hashOtp(kode, secret).getBytes(StandardCharsets.US_ASCII))) {
                    db.commit();
                    return gagal(400, "Kode OTP tidak cocok.");
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE otp_verifikasi SET verified_at = UTC_TIMESTAMP() WHERE id_otp = ?")) {
                    ps.setLong(1, otpId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE users SET wa_terverifikasi_pada = UTC_TIMESTAMP(), updated_at = UTC_TIMESTAMP() "
                                + "WHERE id_user = ? AND role IN ('pasien','dokter','farmasi') AND account_status = 'PENDING'")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
                db.commit();
                return baik(200, "Nomor WhatsApp terverifikasi. Lengkapi profil pasien.");
            } catch (SQLException error) {
                db.rollback();
                throw error;
            } finally {
                db.setAutoCommit(true);
            }
        } catch (SQLException error) {
            System.err.println("[mediflow] Verifikasi OTP gagal: " + ColumnValues.pesan(error));
            return gagal(503, "Verifikasi OTP tidak dapat diproses.");
        }
    }

    static Result lengkapiProfil(Map<String, Object> request) {
        String email = teks(request.get("email")).trim().toLowerCase(java.util.Locale.ROOT);
        String nik = teks(request.get("nik")).trim();
        String tanggal = teks(request.get("tanggal_lahir")).trim();
        String jk = teks(request.get("jenis_kelamin")).trim();
        String bpjs = teks(request.get("nomor_bpjs")).trim();
        String alamat = teks(request.get("alamat")).trim();
        String nomorSatusehat = teks(request.get("nomor_satusehat")).trim();
        boolean persetujuanPrivasi = Boolean.TRUE.equals(request.get("persetujuan_privasi"))
                || "true".equalsIgnoreCase(teks(request.get("persetujuan_privasi")));
        String licenseNumber = teks(request.get("license_number")).trim();
        String specialization = teks(request.get("specialization")).trim();
        long poliId = angka(request.get("id_poli"));
        if (!emailAman(email) || !bpjs.matches("^[0-9]{0,20}$") || nomorSatusehat.length() > 50
                || alamat.length() > 2000) {
            return gagal(422, "Email atau data profil tidak valid.");
        }

        try (Connection db = Database.open()) {
            db.setAutoCommit(false);
            try {
                long userId;
                String nama;
                String wa;
                String role;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT id_user, nama_lengkap, no_telepon, role FROM users WHERE email = ? "
                                + "AND role IN ('pasien','dokter','farmasi') "
                                + "AND account_status = 'PENDING' AND is_active = 0 "
                                + "AND wa_terverifikasi_pada IS NOT NULL FOR UPDATE")) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            db.rollback();
                            return gagal(403, "Verifikasi WhatsApp wajib diselesaikan sebelum melengkapi profil.");
                        }
                        userId = rs.getLong("id_user");
                        nama = rs.getString("nama_lengkap");
                        wa = rs.getString("no_telepon");
                        role = rs.getString("role");
                    }
                }
                if ("dokter".equals(role) || "farmasi".equals(role)) {
                    String jenisIzin = "dokter".equals(role) ? "SIP" : "SIPA";
                    if (!licenseNumber.matches("^[A-Za-z0-9./-]{4,50}$")
                            || ("dokter".equals(role) && (specialization.isBlank() || specialization.length() > 100
                            || poliId <= 0 || !ada(db,
                                    "SELECT 1 FROM poliklinik WHERE id_poli = ? AND is_active = 1 LIMIT 1",
                                    Long.toString(poliId))))
                            || ("farmasi".equals(role) && !specialization.isBlank())) {
                        db.rollback();
                        return gagal(422, "Nomor " + jenisIzin + " atau data staf tidak valid.");
                    }
                    long requestId;
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT INTO staff_registration_requests "
                                    + "(id_user, license_number, specialization, id_poli, status) "
                                    + "VALUES (?, ?, ?, ?, 'PENDING')",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ps.setLong(1, userId);
                        ps.setString(2, licenseNumber);
                        ps.setString(3, specialization.isBlank() ? null : specialization);
                        if ("dokter".equals(role)) {
                            ps.setLong(4, poliId);
                        } else {
                            ps.setNull(4, java.sql.Types.BIGINT);
                        }
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("ID permohonan staf tidak terbentuk.");
                            }
                            requestId = keys.getLong(1);
                        }
                    }
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT INTO log_aktivitas (id_user, aktivitas, tabel_referensi, id_referensi, keterangan) "
                                    + "VALUES (?, 'REGISTER_STAFF_REQUEST', 'staff_registration_requests', ?, ?)")) {
                        ps.setLong(1, userId);
                        ps.setLong(2, requestId);
                        ps.setString(3, "Permohonan akun " + role + " menunggu persetujuan admin");
                        ps.executeUpdate();
                    }
                    db.commit();
                    return baik(202, "Permohonan akun staf terkirim. Akun menunggu pemeriksaan dan persetujuan admin.");
                }
                if (!nik.matches("^[0-9]{16}$") || !tanggalValid(tanggal)
                        || !("L".equals(jk) || "P".equals(jk)) || alamat.isBlank()
                        || !persetujuanPrivasi) {
                    db.rollback();
                    return gagal(422, "NIK, tanggal lahir, jenis kelamin, alamat, dan persetujuan kebijakan privasi wajib diisi.");
                }
                if (ada(db, "SELECT 1 FROM pasien WHERE nik = ? LIMIT 1", nik)) {
                    db.rollback();
                    return gagal(409, "NIK sudah terdaftar.");
                }
                long pasienId;
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO pasien (id_user, no_rekam_medis, nik, nomor_bpjs, nama_lengkap, tanggal_lahir, "
                                + "jenis_kelamin, alamat, no_telepon, no_satusehat, privacy_consent_at, "
                                + "privacy_policy_version, terdaftar_satusehat) "
                                + "VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(), ?, 0)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setString(2, nik);
                    ps.setString(3, bpjs.isBlank() ? null : bpjs);
                    ps.setString(4, nama);
                    ps.setString(5, tanggal);
                    ps.setString(6, jk);
                    ps.setString(7, alamat.isBlank() ? null : alamat);
                    ps.setString(8, wa);
                    ps.setString(9, nomorSatusehat.isBlank() ? null : nomorSatusehat);
                    ps.setString(10, PRIVACY_POLICY_VERSION);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("ID profil pasien tidak terbentuk.");
                        }
                        pasienId = keys.getLong(1);
                    }
                }
                String nomorRekamMedis = "RM" + LocalDate.now(ZoneOffset.UTC).getYear()
                        + String.format("%08d", pasienId);
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE pasien SET no_rekam_medis = ? WHERE id_pasien = ?")) {
                    ps.setString(1, nomorRekamMedis);
                    ps.setLong(2, pasienId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE users SET account_status = 'ACTIVE', is_active = 1, updated_at = UTC_TIMESTAMP() "
                                + "WHERE id_user = ? AND role = 'pasien' AND wa_terverifikasi_pada IS NOT NULL")) {
                    ps.setLong(1, userId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Status akun pasien tidak dapat diaktifkan.");
                    }
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO log_aktivitas (id_user, aktivitas, tabel_referensi, id_referensi, keterangan) "
                                + "VALUES (?, 'REGISTER_PATIENT', 'pasien', ?, 'Pasien menyelesaikan registrasi mandiri')")) {
                    ps.setLong(1, userId);
                    ps.setLong(2, pasienId);
                    ps.executeUpdate();
                }
                db.commit();
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id_pasien", pasienId);
                data.put("no_rekam_medis", nomorRekamMedis);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("status", true);
                body.put("message", "Registrasi pasien selesai. Silakan masuk menggunakan email dan kata sandi.");
                body.put("data", data);
                return new Result(201, body);
            } catch (SQLException error) {
                db.rollback();
                throw error;
            } finally {
                db.setAutoCommit(true);
            }
        } catch (SQLException error) {
            System.err.println("[mediflow] Profil pasien gagal disimpan: " + ColumnValues.pesan(error));
            return gagal(503, "Profil pasien tidak dapat disimpan. Periksa database lalu coba kembali.");
        }
    }

    static List<Map<String, Object>> stafMenunggu() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection db = Database.open();
             PreparedStatement ps = db.prepareStatement(
                     "SELECT r.id_request, r.id_user, u.nama_lengkap, u.email, u.no_telepon, u.role, "
                          + "r.license_number, r.specialization, r.id_poli, p.nama_poli, r.created_at "
                             + "FROM staff_registration_requests r JOIN users u ON u.id_user = r.id_user "
                          + "LEFT JOIN poliklinik p ON p.id_poli = r.id_poli "
                             + "WHERE r.status = 'PENDING' AND u.account_status = 'PENDING' "
                             + "ORDER BY r.created_at, r.id_request");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id_request", rs.getLong("id_request"));
                row.put("id_user", rs.getLong("id_user"));
                row.put("nama_lengkap", rs.getString("nama_lengkap"));
                row.put("email", rs.getString("email"));
                row.put("no_telepon", rs.getString("no_telepon"));
                row.put("role", rs.getString("role"));
                row.put("license_number", rs.getString("license_number"));
                row.put("specialization", rs.getString("specialization"));
                row.put("id_poli", rs.getObject("id_poli"));
                row.put("nama_poli", rs.getString("nama_poli"));
                row.put("created_at", rs.getString("created_at"));
                result.add(row);
            }
        }
        return result;
    }

    static Result tinjauStaf(long requestId, long adminId, boolean setuju) {
        if (requestId <= 0 || adminId <= 0) {
            return gagal(422, "Permohonan atau akun admin tidak valid.");
        }
        try (Connection db = Database.open()) {
            db.setAutoCommit(false);
            try {
                long userId;
                String role;
                String nama;
                String specialization;
                long poliTersimpan;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT r.id_user, u.role, u.nama_lengkap, r.specialization, r.id_poli "
                                + "FROM staff_registration_requests r JOIN users u ON u.id_user = r.id_user "
                                + "WHERE r.id_request = ? AND r.status = 'PENDING' "
                                + "AND u.account_status = 'PENDING' FOR UPDATE")) {
                    ps.setLong(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            db.rollback();
                            return gagal(404, "Permohonan staf tidak ditemukan atau sudah ditinjau.");
                        }
                        userId = rs.getLong("id_user");
                        role = rs.getString("role");
                        nama = rs.getString("nama_lengkap");
                        specialization = rs.getString("specialization");
                        poliTersimpan = rs.getLong("id_poli");
                    }
                }
                if (setuju && "dokter".equals(role)) {
                    if (poliTersimpan <= 0 || !ada(db, "SELECT 1 FROM poliklinik WHERE id_poli = ? AND is_active = 1", Long.toString(poliTersimpan))) {
                        db.rollback();
                        return gagal(422, "Poli permohonan dokter tidak tersedia atau tidak aktif.");
                    }
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT INTO dokter (id_user, id_poli, nama_dokter, spesialisasi, is_active) "
                                    + "VALUES (?, ?, ?, ?, 1)")) {
                        ps.setLong(1, userId);
                        ps.setLong(2, poliTersimpan);
                        ps.setString(3, nama);
                        ps.setString(4, specialization);
                        ps.executeUpdate();
                    }
                }
                String statusPermohonan = setuju ? "APPROVED" : "REJECTED";
                String statusAkun = setuju ? "ACTIVE" : "SUSPENDED";
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE users SET account_status = ?, is_active = ?, updated_at = UTC_TIMESTAMP() "
                                + "WHERE id_user = ? AND account_status = 'PENDING'")) {
                    ps.setString(1, statusAkun);
                    ps.setInt(2, setuju ? 1 : 0);
                    ps.setLong(3, userId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Akun staf tidak berada dalam status menunggu.");
                    }
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE staff_registration_requests SET status = ?, reviewed_by = ?, reviewed_at = UTC_TIMESTAMP() "
                                + "WHERE id_request = ? AND status = 'PENDING'")) {
                    ps.setString(1, statusPermohonan);
                    ps.setLong(2, adminId);
                    ps.setLong(3, requestId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Permohonan staf tidak dapat diperbarui.");
                    }
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO log_aktivitas (id_user, aktivitas, tabel_referensi, id_referensi, keterangan) "
                                + "VALUES (?, ?, 'users', ?, ?)")) {
                    ps.setLong(1, adminId);
                    ps.setString(2, setuju ? "APPROVE_STAFF_REGISTRATION" : "REJECT_STAFF_REGISTRATION");
                    ps.setLong(3, userId);
                    ps.setString(4, (setuju ? "Admin menyetujui " : "Admin menolak ") + role + " " + nama);
                    ps.executeUpdate();
                }
                db.commit();
                return baik(200, setuju ? "Pendaftaran staf disetujui." : "Pendaftaran staf ditolak.");
            } catch (SQLException error) {
                db.rollback();
                throw error;
            } finally {
                db.setAutoCommit(true);
            }
        } catch (SQLException error) {
            System.err.println("[mediflow] Persetujuan staf gagal: " + ColumnValues.pesan(error));
            return gagal(503, "Permohonan staf tidak dapat diproses.");
        }
    }

    private static boolean dibatasi(Connection db, String aksi, String subjek, int batas, int detik)
            throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
                "DELETE FROM auth_rate_limits WHERE window_started_at < DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 DAY)")) {
            ps.executeUpdate();
        }
        String kunci = sha256(aksi + ":" + subjek.toLowerCase(java.util.Locale.ROOT));
        try (PreparedStatement ps = db.prepareStatement(
                "INSERT INTO auth_rate_limits (scope_key, window_started_at, attempt_count) "
                        + "VALUES (?, UTC_TIMESTAMP(), 1) ON DUPLICATE KEY UPDATE "
                        + "attempt_count = IF(window_started_at < DATE_SUB(UTC_TIMESTAMP(), INTERVAL ? SECOND), "
                        + "1, attempt_count + 1), window_started_at = IF("
                        + "window_started_at < DATE_SUB(UTC_TIMESTAMP(), INTERVAL ? SECOND), UTC_TIMESTAMP(), window_started_at)")) {
            ps.setString(1, kunci);
            ps.setInt(2, detik);
            ps.setInt(3, detik);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT attempt_count FROM auth_rate_limits WHERE scope_key = ?")) {
            ps.setString(1, kunci);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > batas;
            }
        }
    }

    private static boolean ada(Connection db, String sql, String... values) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setString(i + 1, values[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static long angka(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static void kirimOtp(String url, String token, String wa, String kode) throws Exception {
        Map<String, String> payload = Map.of(
                "to", "+62" + wa.substring(1),
                "message", "Kode verifikasi MediFlow Anda: " + kode + ". Kode berlaku 10 menit."
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(JsonConverter.MAPPER.writeValueAsBytes(payload)))
                .build();
        HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Provider REST menolak pesan (HTTP " + response.statusCode() + ").");
        }
    }

    private static String hashOtp(String kode, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = hmac.doFinal(kode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException("Hash OTP tidak tersedia.", error);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static boolean tanggalValid(String value) {
        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return !date.isAfter(LocalDate.now(ZoneOffset.UTC));
        } catch (java.time.DateTimeException error) {
            return false;
        }
    }

    private static boolean endpointAman(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static boolean emailAman(String email) {
        return email != null && email.length() <= 50
                && email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,50}@[A-Za-z0-9.-]{1,40}\\.[A-Za-z]{2,}$");
    }

    private static String teks(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String env(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    private static Result baik(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", true);
        body.put("message", message);
        return new Result(status, body);
    }

    private static Result gagal(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", false);
        body.put("message", message);
        return new Result(status, body);
    }
}