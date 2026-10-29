package mediflow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Registrasi akun. Kode WhatsApp tidak dipakai karena pengiriman belum terhubung. */
final class PatientRegistration {
    record Result(int status, Map<String, Object> body) {}

    private PatientRegistration() {}

    static Result daftar(Map<String, Object> request, String ip) {
        String nama = teks(request.get("nama_lengkap")).trim();
        String email = teks(request.get("email")).trim().toLowerCase(java.util.Locale.ROOT);
        String telepon = teks(request.get("no_telepon")).replaceAll("\\s+", "");
        String password = teks(request.get("password"));
        String role = teks(request.get("role")).trim().toLowerCase(java.util.Locale.ROOT);
        if (role.isBlank()) {
            role = "pasien";
        }

        if (!("pasien".equals(role) || "dokter".equals(role) || "farmasi".equals(role))
                || nama.isBlank() || nama.length() > 100
                || email.length() > 50
                || !email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,50}@[A-Za-z0-9.-]{1,40}\\.[A-Za-z]{2,}$")
                || !telepon.matches("^08[0-9]{8,12}$")
                || password.length() < 10 || password.length() > 200) {
            return gagal(422, "Nama, email, nomor telepon, atau kata sandi tidak memenuhi syarat.");
        }

        try (Connection db = Database.open()) {
            if (dibatasi(db, "register-ip", ip, 10, 3600)
                    || dibatasi(db, "register-phone", telepon, 3, 3600)) {
                return gagal(429, "Batas pendaftaran per jam tercapai.");
            }
            boolean warisan = SkemaFisik.warisan(db);
            if (warisan
                    ? ada(db, "SELECT 1 FROM users u WHERE u.username = ? OR u.no_telepon = ? "
                            + "OR EXISTS (SELECT 1 FROM pasien p WHERE p.no_telepon = ?) "
                            + "OR EXISTS (SELECT 1 FROM otp_verifikasi o JOIN users b ON b.id_user = o.id_user "
                            + "WHERE o.tujuan = ? AND b.is_active = 0) "
                            + "OR EXISTS (SELECT 1 FROM log_aktivitas l JOIN users c ON c.id_user = l.id_user "
                            + "WHERE l.aktivitas = 'REGISTER_PENDING' AND c.is_active = 0 "
                            + "AND l.id_log = (SELECT MAX(x.id_log) FROM log_aktivitas x "
                            + "WHERE x.id_user = c.id_user AND x.aktivitas = 'REGISTER_PENDING') "
                            + "AND SUBSTRING_INDEX(l.keterangan, '\\n', -1) = ?) LIMIT 1",
                    email, telepon, telepon, telepon, telepon)
                    : ada(db, "SELECT 1 FROM accounts a WHERE a.email = ? OR a.username = ? "
                            + "OR EXISTS (SELECT 1 FROM patients p WHERE p.no_telepon = ?) "
                            + "OR EXISTS (SELECT 1 FROM admins ad WHERE ad.phone = ?) "
                            + "OR EXISTS (SELECT 1 FROM farmasi_staff f WHERE f.phone = ?) "
                            + "OR EXISTS (SELECT 1 FROM otp_verifikasi o JOIN accounts b ON b.account_id = o.account_id "
                            + "WHERE o.tujuan = ? AND b.is_active = 0) "
                            + "OR EXISTS (SELECT 1 FROM log_aktivitas l JOIN accounts c ON c.account_id = l.account_id "
                            + "WHERE l.aktivitas = 'REGISTER_PENDING' AND c.is_active = 0 "
                            + "AND l.id_log = (SELECT MAX(x.id_log) FROM log_aktivitas x "
                            + "WHERE x.account_id = c.account_id AND x.aktivitas = 'REGISTER_PENDING') "
                            + "AND SUBSTRING_INDEX(l.keterangan, '\\n', -1) = ?) LIMIT 1",
                    email, email, telepon, telepon, telepon, telepon, telepon)) {
                return gagal(409, "Email atau nomor telepon sudah digunakan.");
            }

            boolean autoCommit = db.getAutoCommit();
            db.setAutoCommit(false);
            try {
                long userId;
                String sqlAkun = warisan
                        ? "INSERT INTO users (username, password_hash, role, nama_lengkap, no_telepon, is_active) VALUES (?, ?, ?, ?, ?, 0)"
                        : "INSERT INTO accounts (username, email, password_hash, is_active) VALUES (?, ?, ?, 0)";
                try (PreparedStatement ps = db.prepareStatement(sqlAkun, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, email);
                    if (warisan) {
                        ps.setString(2, PasswordHash.hashPasswordLambat(password));
                        ps.setString(3, role);
                        ps.setString(4, nama);
                        ps.setString(5, telepon);
                    } else {
                        ps.setString(2, email);
                        ps.setString(3, PasswordHash.hashPasswordLambat(password));
                    }
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("ID akun tidak terbentuk.");
                        }
                        userId = keys.getLong(1);
                    }
                }
                catat(db, userId, "REGISTER_PENDING", "accounts", userId, role + "\n" + nama + "\n" + telepon);
                db.commit();
                return baik(200, "Akun tersimpan. Lengkapi profil Anda.");
            } catch (Exception error) {
                db.rollback();
                System.err.println("[mediflow] Registrasi pasien gagal: " + error.getClass().getSimpleName());
                return gagal(503, "Registrasi belum dapat diproses. Coba kembali.");
            } finally {
                db.setAutoCommit(autoCommit);
            }
        } catch (SQLException error) {
            System.err.println("[mediflow] Database registrasi tidak tersedia: " + ColumnValues.pesan(error));
            return gagal(503, "Database registrasi tidak dapat dijangkau.");
        }
    }

    static Result verifikasi(Map<String, Object> request, String ip) {
        return gagal(410, "Verifikasi kode WhatsApp belum tersedia. Lanjutkan dengan melengkapi profil.");
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
            if (SkemaFisik.warisan(db)) {
                return lengkapiWarisan(db, email, nik, tanggal, jk, bpjs, alamat, persetujuanPrivasi,
                        licenseNumber, specialization, poliId);
            }
            db.setAutoCommit(false);
            try {
                long userId;
                String nama;
                String wa;
                String role;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT a.account_id FROM accounts a WHERE a.email = ? AND a.is_active = 0 "
                                + "AND EXISTS (SELECT 1 FROM log_aktivitas l WHERE l.account_id = a.account_id "
                                + "AND l.aktivitas = 'REGISTER_PENDING') "
                                + "AND NOT EXISTS (SELECT 1 FROM patients p WHERE p.account_id = a.account_id) "
                                + "AND NOT EXISTS (SELECT 1 FROM dokter d WHERE d.account_id = a.account_id) "
                                + "AND NOT EXISTS (SELECT 1 FROM farmasi_staff f WHERE f.account_id = a.account_id) "
                                + "FOR UPDATE")) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            db.rollback();
                            return gagal(403, "Akun pendaftaran tidak ditemukan. Daftar kembali.");
                        }
                        userId = rs.getLong("account_id");
                    }
                }
                String[] pending = bacaPending(db, userId);
                if (pending == null) {
                    db.rollback();
                    return gagal(403, "Data pendaftaran tidak ditemukan. Daftar kembali.");
                }
                role = pending[0];
                nama = pending[1];
                wa = pending[2];
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
                    if ("dokter".equals(role)) {
                        try (PreparedStatement ps = db.prepareStatement(
                                "INSERT INTO dokter (account_id, id_poli, nama_dokter, spesialisasi, is_active) "
                                        + "VALUES (?, ?, ?, ?, 0)")) {
                            ps.setLong(1, userId);
                            ps.setLong(2, poliId);
                            ps.setString(3, nama);
                            ps.setString(4, specialization);
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = db.prepareStatement(
                                "INSERT INTO farmasi_staff (account_id, nama, phone) VALUES (?, ?, ?)")) {
                            ps.setLong(1, userId);
                            ps.setString(2, nama);
                            ps.setString(3, wa);
                            ps.executeUpdate();
                        }
                    }
                    catat(db, userId, "REGISTER_STAFF", "dokter".equals(role) ? "dokter" : "farmasi_staff", userId,
                            licenseNumber + "\n" + specialization + "\n" + ("dokter".equals(role) ? poliId : 0) + "\n" + wa);
                    db.commit();
                    return baik(202, "Permohonan akun staf terkirim. Akun menunggu pemeriksaan dan persetujuan admin.");
                }
                if (!nik.matches("^[0-9]{16}$") || !tanggalValid(tanggal)
                        || !("L".equals(jk) || "P".equals(jk)) || alamat.isBlank()
                        || !persetujuanPrivasi) {
                    db.rollback();
                    return gagal(422, "NIK, tanggal lahir, jenis kelamin, alamat, dan persetujuan kebijakan privasi wajib diisi.");
                }
                if (ada(db, "SELECT 1 FROM patients WHERE nik = ? LIMIT 1", nik)) {
                    db.rollback();
                    return gagal(409, "NIK sudah terdaftar.");
                }
                long pasienId;
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO patients (account_id, no_rekam_medis, nik, nama_lengkap, tanggal_lahir, "
                                + "jenis_kelamin, alamat, no_telepon, terdaftar_satusehat) "
                                + "VALUES (?, NULL, ?, ?, ?, ?, ?, ?, 0)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId);
                    ps.setString(2, nik);
                    ps.setString(3, nama);
                    ps.setString(4, tanggal);
                    ps.setString(5, jk);
                    ps.setString(6, alamat);
                    ps.setString(7, wa);
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
                        "UPDATE patients SET no_rekam_medis = ? WHERE id_pasien = ?")) {
                    ps.setString(1, nomorRekamMedis);
                    ps.setLong(2, pasienId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE accounts SET is_active = 1, updated_at = UTC_TIMESTAMP() WHERE account_id = ? AND is_active = 0")) {
                    ps.setLong(1, userId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Status akun pasien tidak dapat diaktifkan.");
                    }
                }
                catat(db, userId, "REGISTER_PATIENT", "patients", pasienId, "Pasien menyelesaikan registrasi mandiri");
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
        try (Connection db = Database.open()) {
            boolean warisan = SkemaFisik.warisan(db);
        String dokterSql = warisan
                ? "SELECT l.id_log AS id_request, u.id_user, d.nama_dokter AS nama_lengkap, "
                + "u.username AS email, l.keterangan, d.spesialisasi AS specialization, d.id_poli, p.nama_poli, l.created_at "
                + "FROM dokter d JOIN users u ON u.id_user = d.id_user "
                + "JOIN log_aktivitas l ON l.id_user = d.id_user AND l.aktivitas = 'REGISTER_STAFF' "
                + "AND l.id_log = (SELECT MAX(x.id_log) FROM log_aktivitas x "
                + "WHERE x.id_user = d.id_user AND x.aktivitas = 'REGISTER_STAFF') "
                + "LEFT JOIN poliklinik p ON p.id_poli = d.id_poli "
                + "WHERE d.is_active = 0 AND u.is_active = 0 ORDER BY l.id_log"
                : "SELECT l.id_log AS id_request, a.account_id AS id_user, d.nama_dokter AS nama_lengkap, "
                + "a.email, l.keterangan, d.spesialisasi AS specialization, d.id_poli, p.nama_poli, l.created_at "
                + "FROM dokter d JOIN accounts a ON a.account_id = d.account_id "
                + "JOIN log_aktivitas l ON l.account_id = d.account_id AND l.aktivitas = 'REGISTER_STAFF' "
                + "AND l.id_log = (SELECT MAX(x.id_log) FROM log_aktivitas x "
                + "WHERE x.account_id = d.account_id AND x.aktivitas = 'REGISTER_STAFF') "
                + "LEFT JOIN poliklinik p ON p.id_poli = d.id_poli "
                + "WHERE d.is_active = 0 AND a.is_active = 0 ORDER BY l.id_log";
        String farmasiSql = warisan
                ? "SELECT l.id_log AS id_request, u.id_user, u.nama_lengkap, u.username AS email, "
                + "l.keterangan, u.no_telepon AS phone, l.created_at "
                + "FROM users u JOIN log_aktivitas l ON l.id_user = u.id_user AND l.aktivitas = 'REGISTER_STAFF' "
                + "AND l.id_log = (SELECT MAX(x.id_log) FROM log_aktivitas x "
                + "WHERE x.id_user = u.id_user AND x.aktivitas = 'REGISTER_STAFF') "
                + "WHERE u.role = 'farmasi' AND u.is_active = 0 ORDER BY l.id_log"
                : "SELECT l.id_log AS id_request, a.account_id AS id_user, f.nama AS nama_lengkap, "
                + "a.email, l.keterangan, f.phone, l.created_at "
                + "FROM farmasi_staff f JOIN accounts a ON a.account_id = f.account_id "
                + "JOIN log_aktivitas l ON l.account_id = f.account_id AND l.aktivitas = 'REGISTER_STAFF' "
                + "AND l.id_log = (SELECT MAX(x.id_log) FROM log_aktivitas x "
                + "WHERE x.account_id = f.account_id AND x.aktivitas = 'REGISTER_STAFF') "
                + "WHERE a.is_active = 0 ORDER BY l.id_log";
            try (PreparedStatement ps = db.prepareStatement(dokterSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] keterangan = barisKeterangan(rs.getString("keterangan"), 4);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id_request", rs.getLong("id_request"));
                    row.put("id_user", rs.getLong("id_user"));
                    row.put("nama_lengkap", rs.getString("nama_lengkap"));
                    row.put("email", rs.getString("email"));
                    row.put("no_telepon", keterangan[3]);
                    row.put("role", "dokter");
                    row.put("license_number", keterangan[0]);
                    row.put("specialization", rs.getString("specialization"));
                    row.put("id_poli", rs.getObject("id_poli"));
                    row.put("nama_poli", rs.getString("nama_poli"));
                    row.put("created_at", rs.getString("created_at"));
                    result.add(row);
                }
            }
            try (PreparedStatement ps = db.prepareStatement(farmasiSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String[] keterangan = barisKeterangan(rs.getString("keterangan"), 4);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id_request", rs.getLong("id_request"));
                    row.put("id_user", rs.getLong("id_user"));
                    row.put("nama_lengkap", rs.getString("nama_lengkap"));
                    row.put("email", rs.getString("email"));
                    row.put("no_telepon", rs.getString("phone") == null ? keterangan[3] : rs.getString("phone"));
                    row.put("role", "farmasi");
                    row.put("license_number", keterangan[0]);
                    row.put("specialization", "");
                    row.put("id_poli", null);
                    row.put("nama_poli", null);
                    row.put("created_at", rs.getString("created_at"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    static Result tinjauStaf(long requestId, long adminId, boolean setuju) {
        if (requestId <= 0 || adminId <= 0) {
            return gagal(422, "Permohonan atau akun admin tidak valid.");
        }
        try (Connection db = Database.open()) {
            if (SkemaFisik.warisan(db)) {
                return tinjauWarisan(db, requestId, adminId, setuju);
            }
            db.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT account_id FROM log_aktivitas WHERE id_log = ? AND aktivitas = 'REGISTER_STAFF' FOR UPDATE")) {
                    ps.setLong(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            db.rollback();
                            return gagal(404, "Permohonan staf tidak ditemukan atau sudah ditinjau.");
                        }
                        userId = rs.getLong("account_id");
                    }
                }
                if (!ada(db, "SELECT 1 FROM accounts WHERE account_id = ? AND is_active = 0", Long.toString(userId))) {
                    db.rollback();
                    return gagal(404, "Permohonan staf tidak ditemukan atau sudah ditinjau.");
                }
                String role = null;
                String nama = null;
                long poliTersimpan = 0;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT nama_dokter, id_poli FROM dokter WHERE account_id = ? AND is_active = 0 FOR UPDATE")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            role = "dokter";
                            nama = rs.getString("nama_dokter");
                            poliTersimpan = rs.getLong("id_poli");
                        }
                    }
                }
                if (role == null) {
                    try (PreparedStatement ps = db.prepareStatement(
                            "SELECT nama FROM farmasi_staff WHERE account_id = ? FOR UPDATE")) {
                        ps.setLong(1, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                role = "farmasi";
                                nama = rs.getString("nama");
                            }
                        }
                    }
                }
                if (role == null) {
                    db.rollback();
                    return gagal(404, "Permohonan staf tidak ditemukan atau sudah ditinjau.");
                }
                if (setuju && "dokter".equals(role)) {
                    if (poliTersimpan <= 0 || !ada(db, "SELECT 1 FROM poliklinik WHERE id_poli = ? AND is_active = 1", Long.toString(poliTersimpan))) {
                        db.rollback();
                        return gagal(422, "Poli permohonan dokter tidak tersedia atau tidak aktif.");
                    }
                    try (PreparedStatement ps = db.prepareStatement(
                            "UPDATE dokter SET is_active = 1, updated_at = UTC_TIMESTAMP() WHERE account_id = ? AND is_active = 0")) {
                        ps.setLong(1, userId);
                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("Profil dokter tidak dapat diaktifkan.");
                        }
                    }
                }
                if (setuju) {
                    try (PreparedStatement ps = db.prepareStatement(
                            "UPDATE accounts SET is_active = 1, updated_at = UTC_TIMESTAMP() WHERE account_id = ? AND is_active = 0")) {
                        ps.setLong(1, userId);
                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("Akun staf tidak berada dalam status menunggu.");
                        }
                    }
                } else if ("dokter".equals(role)) {
                    try (PreparedStatement ps = db.prepareStatement(
                            "DELETE FROM dokter WHERE account_id = ? AND is_active = 0")) {
                        ps.setLong(1, userId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = db.prepareStatement(
                            "DELETE FROM farmasi_staff WHERE account_id = ?")) {
                        ps.setLong(1, userId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE log_aktivitas SET aktivitas = 'REGISTER_STAFF_CLOSED' WHERE id_log = ? AND aktivitas = 'REGISTER_STAFF'")) {
                    ps.setLong(1, requestId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Permohonan staf tidak dapat diperbarui.");
                    }
                }
                catat(db, adminId, setuju ? "APPROVE_STAFF_REGISTRATION" : "REJECT_STAFF_REGISTRATION", "accounts", userId,
                        (setuju ? "Admin menyetujui " : "Admin menolak ") + role + " " + nama);
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

    private static Result lengkapiWarisan(
            Connection db, String email, String nik, String tanggal, String jk, String bpjs, String alamat,
            boolean persetujuanPrivasi, String licenseNumber, String specialization, long poliId
    ) {
        boolean otomatis = true;
        try {
            otomatis = db.getAutoCommit();
            db.setAutoCommit(false);
            long userId;
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT u.id_user FROM users u WHERE u.username = ? AND u.is_active = 0 "
                            + "AND EXISTS (SELECT 1 FROM log_aktivitas l WHERE l.id_user = u.id_user "
                            + "AND l.aktivitas = 'REGISTER_PENDING') "
                            + "AND NOT EXISTS (SELECT 1 FROM pasien p WHERE p.id_user = u.id_user) "
                            + "AND NOT EXISTS (SELECT 1 FROM dokter d WHERE d.id_user = u.id_user) "
                            + "AND NOT EXISTS (SELECT 1 FROM log_aktivitas s WHERE s.id_user = u.id_user "
                            + "AND s.aktivitas = 'REGISTER_STAFF') FOR UPDATE")) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        db.rollback();
                        return gagal(403, "Akun pendaftaran tidak ditemukan. Daftar kembali.");
                    }
                    userId = rs.getLong("id_user");
                }
            }
            String[] pending = bacaPending(db, userId);
            if (pending == null) {
                db.rollback();
                return gagal(403, "Data pendaftaran tidak ditemukan. Daftar kembali.");
            }
            String role = pending[0];
            String nama = pending[1];
            String wa = pending[2];
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
                if ("dokter".equals(role)) {
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT INTO dokter (id_user, id_poli, nama_dokter, spesialisasi, is_active) "
                                    + "VALUES (?, ?, ?, ?, 0)")) {
                        ps.setLong(1, userId);
                        ps.setLong(2, poliId);
                        ps.setString(3, nama);
                        ps.setString(4, specialization);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE users SET nama_lengkap = ?, no_telepon = ?, updated_at = UTC_TIMESTAMP() "
                                + "WHERE id_user = ?")) {
                    ps.setString(1, nama);
                    ps.setString(2, wa);
                    ps.setLong(3, userId);
                    ps.executeUpdate();
                }
                catat(db, userId, "REGISTER_STAFF", "dokter".equals(role) ? "dokter" : "users", userId,
                        licenseNumber + "\n" + specialization + "\n" + ("dokter".equals(role) ? poliId : 0) + "\n" + wa);
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
                    "INSERT INTO pasien (id_user, nik, nomor_bpjs, nama_lengkap, tanggal_lahir, "
                            + "jenis_kelamin, alamat, no_telepon, terdaftar_satusehat) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setString(2, nik);
                if (bpjs.isBlank()) {
                    ps.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(3, bpjs);
                }
                ps.setString(4, nama);
                ps.setString(5, tanggal);
                ps.setString(6, jk);
                ps.setString(7, alamat);
                ps.setString(8, wa);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("ID profil pasien tidak terbentuk.");
                    }
                    pasienId = keys.getLong(1);
                }
            }
            try (PreparedStatement ps = db.prepareStatement(
                    "UPDATE users SET is_active = 1, nama_lengkap = ?, no_telepon = ?, updated_at = UTC_TIMESTAMP() "
                            + "WHERE id_user = ? AND is_active = 0")) {
                ps.setString(1, nama);
                ps.setString(2, wa);
                ps.setLong(3, userId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Status akun pasien tidak dapat diaktifkan.");
                }
            }
            catat(db, userId, "REGISTER_PATIENT", "pasien", pasienId, "Pasien menyelesaikan registrasi mandiri");
            db.commit();
            String nomorRekamMedis = "RM" + LocalDate.now(ZoneOffset.UTC).getYear()
                    + String.format("%08d", pasienId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id_pasien", pasienId);
            data.put("no_rekam_medis", nomorRekamMedis);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", true);
            body.put("message", "Registrasi pasien selesai. Silakan masuk menggunakan email dan kata sandi.");
            body.put("data", data);
            return new Result(201, body);
        } catch (SQLException error) {
            try {
                db.rollback();
            } catch (SQLException diabaikan) {
                // Transaksi sudah batal.
            }
            System.err.println("[mediflow] Profil pasien gagal disimpan: " + ColumnValues.pesan(error));
            return gagal(503, "Profil pasien tidak dapat disimpan. Periksa database lalu coba kembali.");
        } finally {
            try {
                db.setAutoCommit(otomatis);
            } catch (SQLException diabaikan) {
                // Koneksi ditutup oleh pemanggil.
            }
        }
    }

    private static Result tinjauWarisan(Connection db, long requestId, long adminId, boolean setuju) {
        boolean otomatis = true;
        try {
            otomatis = db.getAutoCommit();
            db.setAutoCommit(false);
            long userId;
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT id_user FROM log_aktivitas WHERE id_log = ? AND aktivitas = 'REGISTER_STAFF' FOR UPDATE")) {
                ps.setLong(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        db.rollback();
                        return gagal(404, "Permohonan staf tidak ditemukan atau sudah ditinjau.");
                    }
                    userId = rs.getLong("id_user");
                }
            }
            if (!ada(db, "SELECT 1 FROM users WHERE id_user = ? AND is_active = 0", Long.toString(userId))) {
                db.rollback();
                return gagal(404, "Permohonan staf tidak ditemukan atau sudah ditinjau.");
            }
            String role = null;
            String nama = null;
            long poliTersimpan = 0;
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT nama_dokter, id_poli FROM dokter WHERE id_user = ? AND is_active = 0 FOR UPDATE")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        role = "dokter";
                        nama = rs.getString("nama_dokter");
                        poliTersimpan = rs.getLong("id_poli");
                    }
                }
            }
            if (role == null) {
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT nama_lengkap FROM users WHERE id_user = ? AND role = 'farmasi' AND is_active = 0 FOR UPDATE")) {
                    ps.setLong(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            role = "farmasi";
                            nama = rs.getString("nama_lengkap");
                        }
                    }
                }
            }
            if (role == null) {
                db.rollback();
                return gagal(404, "Permohonan staf tidak ditemukan atau sudah ditinjau.");
            }
            if (setuju && "dokter".equals(role)) {
                if (poliTersimpan <= 0 || !ada(db, "SELECT 1 FROM poliklinik WHERE id_poli = ? AND is_active = 1", Long.toString(poliTersimpan))) {
                    db.rollback();
                    return gagal(422, "Poli permohonan dokter tidak tersedia atau tidak aktif.");
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE dokter SET is_active = 1, updated_at = UTC_TIMESTAMP() WHERE id_user = ? AND is_active = 0")) {
                    ps.setLong(1, userId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Profil dokter tidak dapat diaktifkan.");
                    }
                }
            }
            if (setuju) {
                try (PreparedStatement ps = db.prepareStatement(
                        "UPDATE users SET is_active = 1, updated_at = UTC_TIMESTAMP() WHERE id_user = ? AND is_active = 0")) {
                    ps.setLong(1, userId);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Akun staf tidak berada dalam status menunggu.");
                    }
                }
            } else if ("dokter".equals(role)) {
                try (PreparedStatement ps = db.prepareStatement(
                        "DELETE FROM dokter WHERE id_user = ? AND is_active = 0")) {
                    ps.setLong(1, userId);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = db.prepareStatement(
                    "UPDATE log_aktivitas SET aktivitas = 'REGISTER_STAFF_CLOSED' WHERE id_log = ? AND aktivitas = 'REGISTER_STAFF'")) {
                ps.setLong(1, requestId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Permohonan staf tidak dapat diperbarui.");
                }
            }
            catat(db, adminId, setuju ? "APPROVE_STAFF_REGISTRATION" : "REJECT_STAFF_REGISTRATION", "users", userId,
                    (setuju ? "Admin menyetujui " : "Admin menolak ") + role + " " + nama);
            db.commit();
            return baik(200, setuju ? "Pendaftaran staf disetujui." : "Pendaftaran staf ditolak.");
        } catch (SQLException error) {
            try {
                db.rollback();
            } catch (SQLException diabaikan) {
                // Transaksi sudah batal.
            }
            System.err.println("[mediflow] Persetujuan staf gagal: " + ColumnValues.pesan(error));
            return gagal(503, "Permohonan staf tidak dapat diproses.");
        } finally {
            try {
                db.setAutoCommit(otomatis);
            } catch (SQLException diabaikan) {
                // Koneksi ditutup oleh pemanggil.
            }
        }
    }

    private static void catat(Connection db, long accountId, String aktivitas, String tabel, long referensi, String keterangan)
            throws SQLException {
        boolean warisan = SkemaFisik.warisan(db);
        String kolom = warisan ? "id_user" : "account_id";
        String acuan = warisan ? tabelWarisan(tabel) : tabel;
        try (PreparedStatement ps = db.prepareStatement(
                "INSERT INTO log_aktivitas (" + kolom + ", aktivitas, tabel_referensi, id_referensi, keterangan) VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, accountId);
            ps.setString(2, aktivitas);
            ps.setString(3, acuan);
            ps.setLong(4, referensi);
            ps.setString(5, keterangan);
            ps.executeUpdate();
        }
    }

    private static String tabelWarisan(String tabel) {
        return switch (tabel) {
            case "accounts" -> "users";
            case "patients" -> "pasien";
            case "farmasi_staff" -> "users";
            default -> tabel;
        };
    }

    private static String[] bacaPending(Connection db, long accountId) throws SQLException {
        String kolom = SkemaFisik.warisan(db) ? "id_user" : "account_id";
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT keterangan FROM log_aktivitas WHERE " + kolom + " = ? AND aktivitas = 'REGISTER_PENDING' "
                        + "ORDER BY id_log DESC LIMIT 1")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String[] bagian = barisKeterangan(rs.getString("keterangan"), 3);
                if (!("pasien".equals(bagian[0]) || "dokter".equals(bagian[0]) || "farmasi".equals(bagian[0]))
                        || bagian[1].isBlank() || bagian[2].isBlank()) {
                    return null;
                }
                return bagian;
            }
        }
    }

    private static String[] barisKeterangan(String teks, int jumlah) {
        String[] bagian = teks == null ? new String[0] : teks.split("\n", -1);
        String[] hasil = new String[jumlah];
        for (int i = 0; i < jumlah; i++) {
            hasil[i] = i < bagian.length && bagian[i] != null ? bagian[i] : "";
        }
        return hasil;
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

    private static boolean emailAman(String email) {
        return email != null && email.length() <= 50
                && email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,50}@[A-Za-z0-9.-]{1,40}\\.[A-Za-z]{2,}$");
    }

    private static String teks(Object value) {
        return value == null ? "" : String.valueOf(value);
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