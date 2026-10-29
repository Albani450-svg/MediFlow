package mediflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * medicflow_db menyimpan akun di users dan pasien, dengan id_user.
 * Aplikasi tetap memakai nama accounts, patients, dan account_id di memori.
 * Koneksi tidak membuat tabel baru. auth_rate_limits tidak ada di DBeaver.
 */
final class SkemaFisik {
    private static final String[] HAPUS = {
            "detail_resep", "pengambilan_obat", "resep", "pemeriksaan", "pendaftaran_poli",
            "notifikasi", "otp_verifikasi", "jadwal_dokter", "dokter", "mutasi_stok",
            "obat", "pasien", "log_aktivitas", "poliklinik", "users"
    };

    private static volatile String cacheKatalog;
    private static volatile boolean cacheNilai;

    private SkemaFisik() {}

    static boolean warisan(Connection db) throws SQLException {
        String katalog = db.getCatalog();
        if (katalog != null && katalog.equals(cacheKatalog)) {
            return cacheNilai;
        }
        boolean users = adaTabel(db, "users");
        boolean accounts = adaTabel(db, "accounts");
        boolean nilai = users && !accounts;
        if (katalog != null) {
            cacheKatalog = katalog;
            cacheNilai = nilai;
        }
        return nilai;
    }

    static String tabelFisik(boolean warisan, String tabel) {
        if (!warisan) {
            return tabel;
        }
        return switch (tabel) {
            case "accounts", "admins", "farmasi_staff" -> "users";
            case "patients" -> "pasien";
            default -> tabel;
        };
    }

    static String kolomFisik(boolean warisan, String tabel, String kolom) {
        if (!warisan) {
            return kolom;
        }
        return switch (tabel + "." + kolom) {
            case "accounts.email", "patients.no_rekam_medis", "admins.is_super" -> null;
            case "accounts.account_id", "admins.id_admin", "admins.account_id",
                    "farmasi_staff.id_farmasi", "farmasi_staff.account_id",
                    "patients.account_id", "dokter.account_id", "otp_verifikasi.account_id",
                    "log_aktivitas.account_id", "mutasi_stok.account_user" -> "id_user";
            case "pengambilan_obat.account_petugas" -> "id_user_petugas";
            case "admins.nama", "farmasi_staff.nama" -> "nama_lengkap";
            case "admins.phone", "farmasi_staff.phone" -> "no_telepon";
            default -> kolom;
        };
    }

    static String ekspresi(boolean warisan, String tabel, String kolom) {
        if (!warisan) {
            return DatabaseSnapshot.kutip(kolom);
        }
        return switch (tabel + "." + kolom) {
            case "admins.is_super" -> "IF(`username` = 'admin', 1, 0)";
            case "accounts.email" -> "CASE WHEN `username` LIKE '%@%' THEN `username` ELSE NULL END";
            case "patients.no_rekam_medis" -> "NULL";
            default -> {
                String fisik = kolomFisik(true, tabel, kolom);
                yield DatabaseSnapshot.kutip(fisik == null ? kolom : fisik);
            }
        };
    }

    static String saringPeran(boolean warisan, String tabel) {
        if (!warisan) {
            return "";
        }
        return switch (tabel) {
            case "admins" -> "`role` = 'admin'";
            case "farmasi_staff" -> "`role` = 'farmasi'";
            default -> "";
        };
    }

    static String pilih(boolean warisan, TableCatalog.Table tabel) {
        if (!warisan) {
            return "SELECT * FROM " + DatabaseSnapshot.kutip(tabel.name());
        }
        StringBuilder sql = new StringBuilder("SELECT ");
        List<TableCatalog.Col> kolom = tabel.columns();
        for (int i = 0; i < kolom.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            String nama = kolom.get(i).name();
            String ekspresi = ekspresi(true, tabel.name(), nama);
            sql.append(ekspresi);
            if (!ekspresi.equals(DatabaseSnapshot.kutip(nama))) {
                sql.append(" AS ").append(DatabaseSnapshot.kutip(nama));
            }
        }
        if ("patients".equals(tabel.name())) {
            sql.append(", `nomor_bpjs`");
        }
        sql.append(" FROM ").append(DatabaseSnapshot.kutip(tabelFisik(true, tabel.name())));
        return sql.toString();
    }

    static Map<String, List<Map<String, Object>>> baca(Connection db) throws SQLException {
        Map<String, List<Map<String, Object>>> hasil = new LinkedHashMap<>();
        for (TableCatalog.Table tabel : TableCatalog.TABLES) {
            String sql = pilih(true, tabel);
            String peran = saringPeran(true, tabel.name());
            if (!peran.isEmpty()) {
                sql += " WHERE " + peran;
            }
            List<Map<String, Object>> baris = new ArrayList<>();
            try (Statement perintah = db.createStatement();
                 ResultSet rs = perintah.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    for (TableCatalog.Col kolom : tabel.columns()) {
                        item.put(kolom.name(), ColumnValues.baca(rs.getString(kolom.name()), kolom));
                    }
                    if ("patients".equals(tabel.name())) {
                        item.put("nomor_bpjs", rs.getString("nomor_bpjs"));
                    }
                    baris.add(item);
                }
            }
            hasil.put(tabel.name(), baris);
        }
        return hasil;
    }

    static void simpan(Connection db, Map<String, Object> data) throws SQLException {
        Map<Long, Lama> lama = bacaLama(db);
        Map<Long, String> bpjs = bacaBpjs(db);
        List<Map<String, Object>> users = susunUsers(data, lama);
        if (users.isEmpty()) {
            throw new IllegalStateException("Akun kosong, penyimpanan dibatalkan.");
        }
        boolean otomatis = db.getAutoCommit();
        db.setAutoCommit(false);
        try (Statement perintah = db.createStatement()) {
            perintah.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String tabel : HAPUS) {
                perintah.execute("DELETE FROM " + DatabaseSnapshot.kutip(tabel));
            }
            for (Map<String, Object> baris : users) {
                sisipUsers(db, baris);
            }
            sisipKatalog(db, "poliklinik", daftar(data, "poliklinik"));
            sisipKatalog(db, "obat", daftar(data, "obat"));
            for (Map<String, Object> baris : daftar(data, "patients")) {
                sisipPasien(db, baris, bpjs);
            }
            sisipKatalog(db, "dokter", daftar(data, "dokter"));
            sisipKatalog(db, "jadwal_dokter", daftar(data, "jadwal_dokter"));
            sisipKatalog(db, "pendaftaran_poli", daftar(data, "pendaftaran_poli"));
            sisipKatalog(db, "pemeriksaan", daftar(data, "pemeriksaan"));
            sisipKatalog(db, "resep", daftar(data, "resep"));
            sisipKatalog(db, "detail_resep", daftar(data, "detail_resep"));
            sisipKatalog(db, "notifikasi", daftar(data, "notifikasi"));
            sisipKatalog(db, "otp_verifikasi", daftar(data, "otp_verifikasi"));
            sisipKatalog(db, "pengambilan_obat", daftar(data, "pengambilan_obat"));
            sisipKatalog(db, "mutasi_stok", daftar(data, "mutasi_stok"));
            sisipKatalog(db, "log_aktivitas", daftar(data, "log_aktivitas"));
            perintah.execute("SET FOREIGN_KEY_CHECKS = 1");
            db.commit();
        } catch (RuntimeException | SQLException error) {
            db.rollback();
            try (Statement perintah = db.createStatement()) {
                perintah.execute("SET FOREIGN_KEY_CHECKS = 1");
            } catch (SQLException diabaikan) {
                // Koneksi bisa sudah putus. Pemeriksaan kunci tetap dinyalakan di sesi baru.
            }
            throw error;
        } finally {
            db.setAutoCommit(otomatis);
        }
    }

    private static List<Map<String, Object>> susunUsers(Map<String, Object> data, Map<Long, Lama> lama) {
        Map<Long, Map<String, Object>> admin = indeks(daftar(data, "admins"), "account_id");
        Map<Long, Map<String, Object>> farmasi = indeks(daftar(data, "farmasi_staff"), "account_id");
        Map<Long, Map<String, Object>> dokter = indeks(daftar(data, "dokter"), "account_id");
        Map<Long, Map<String, Object>> pasien = indeks(daftar(data, "patients"), "account_id");
        List<Map<String, Object>> log = daftar(data, "log_aktivitas");
        List<Map<String, Object>> hasil = new ArrayList<>();
        for (Map<String, Object> akun : daftar(data, "accounts")) {
            long id = DatabaseRecords.lng(akun.get("account_id"));
            Lama salinan = lama.get(id);
            String username = teks(akun.get("username"));
            String role;
            String nama;
            String telepon;
            Map<String, Object> profilAdmin = admin.get(id);
            Map<String, Object> profilDokter = dokter.get(id);
            Map<String, Object> profilFarmasi = farmasi.get(id);
            Map<String, Object> profilPasien = pasien.get(id);
            if (profilAdmin != null) {
                role = "admin";
                nama = teks(profilAdmin.get("nama"));
                telepon = teks(profilAdmin.get("phone"));
            } else if (profilDokter != null) {
                role = "dokter";
                nama = teks(profilDokter.get("nama_dokter"));
                telepon = salinan == null ? "" : salinan.telepon;
            } else if (profilFarmasi != null) {
                role = "farmasi";
                nama = teks(profilFarmasi.get("nama"));
                telepon = teks(profilFarmasi.get("phone"));
            } else if (profilPasien != null) {
                role = "pasien";
                nama = teks(profilPasien.get("nama_lengkap"));
                telepon = teks(profilPasien.get("no_telepon"));
            } else {
                String[] pending = pending(log, id);
                role = pending == null ? (salinan == null ? "" : salinan.role) : pending[0];
                nama = pending == null ? (salinan == null ? "" : salinan.nama) : pending[1];
                telepon = pending == null ? (salinan == null ? "" : salinan.telepon) : pending[2];
            }
            if (role.isBlank() || !peranSah(role)) {
                throw new IllegalStateException("Peran akun " + username + " tidak dikenali.");
            }
            if (telepon.isBlank() && salinan != null) {
                telepon = salinan.telepon;
            }
            if (nama.isBlank()) {
                nama = salinan != null && !salinan.nama.isBlank() ? salinan.nama : username;
            }
            Map<String, Object> baris = new LinkedHashMap<>();
            baris.put("id_user", id);
            baris.put("username", username);
            baris.put("password_hash", akun.get("password_hash"));
            baris.put("role", role);
            baris.put("nama_lengkap", potong(nama, 100));
            baris.put("no_telepon", telepon.isBlank() ? null : potong(telepon, 20));
            baris.put("is_active", akun.get("is_active"));
            baris.put("created_at", akun.get("created_at"));
            baris.put("updated_at", akun.get("updated_at"));
            hasil.add(baris);
        }
        return hasil;
    }

    private static boolean peranSah(String role) {
        return "admin".equals(role) || "pasien".equals(role) || "dokter".equals(role) || "farmasi".equals(role);
    }

    private static String[] pending(List<Map<String, Object>> log, long accountId) {
        Map<String, Object> pilih = null;
        long idLog = -1;
        for (Map<String, Object> item : log) {
            if (DatabaseRecords.lng(item.get("account_id")) != accountId
                    || !"REGISTER_PENDING".equals(String.valueOf(item.get("aktivitas")))) {
                continue;
            }
            long ini = DatabaseRecords.lng(item.get("id_log"));
            if (ini >= idLog) {
                pilih = item;
                idLog = ini;
            }
        }
        if (pilih == null || pilih.get("keterangan") == null) {
            return null;
        }
        String[] bagian = String.valueOf(pilih.get("keterangan")).split("\n", -1);
        if (bagian.length < 3 || !peranSah(bagian[0])) {
            return null;
        }
        return new String[] {bagian[0], bagian[1], bagian[2]};
    }

    private static void sisipUsers(Connection db, Map<String, Object> baris) throws SQLException {
        String sql = "INSERT INTO `users` (id_user, username, password_hash, role, nama_lengkap, no_telepon, "
                + "is_active, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            DatabaseSnapshot.ikat(ps, 1, baris.get("id_user"), kolom("accounts", "account_id"));
            DatabaseSnapshot.ikat(ps, 2, baris.get("username"), kolom("accounts", "username"));
            DatabaseSnapshot.ikat(ps, 3, baris.get("password_hash"), kolom("accounts", "password_hash"));
            ikatTeks(ps, 4, baris.get("role"));
            ikatTeks(ps, 5, baris.get("nama_lengkap"));
            ikatTeks(ps, 6, baris.get("no_telepon"));
            DatabaseSnapshot.ikat(ps, 7, baris.get("is_active"), kolom("accounts", "is_active"));
            DatabaseSnapshot.ikat(ps, 8, baris.get("created_at"), kolom("accounts", "created_at"));
            DatabaseSnapshot.ikat(ps, 9, baris.get("updated_at"), kolom("accounts", "updated_at"));
            ps.executeUpdate();
        }
    }

    private static void sisipPasien(Connection db, Map<String, Object> baris, Map<Long, String> bpjs) throws SQLException {
        long idPasien = DatabaseRecords.lng(baris.get("id_pasien"));
        String nomor = teks(baris.get("nomor_bpjs"));
        if (nomor.isBlank()) {
            nomor = bpjs.getOrDefault(idPasien, "");
        }
        List<TableCatalog.Col> meta = new ArrayList<>();
        List<String> fisik = new ArrayList<>();
        List<Object> nilai = new ArrayList<>();
        for (TableCatalog.Col kolom : TableCatalog.BY_NAME.get("patients").columns()) {
            String nama = kolomFisik(true, "patients", kolom.name());
            if (nama == null) {
                continue;
            }
            fisik.add(nama);
            nilai.add(baris.get(kolom.name()));
            meta.add(kolom);
        }
        fisik.add("nomor_bpjs");
        nilai.add(nomor.isBlank() ? null : nomor);
        meta.add(null);
        sisip(db, "pasien", fisik, nilai, meta);
    }

    private static void sisipKatalog(Connection db, String tabel, List<Map<String, Object>> baris) throws SQLException {
        for (Map<String, Object> item : baris) {
            List<TableCatalog.Col> meta = new ArrayList<>();
            List<String> fisik = new ArrayList<>();
            List<Object> nilai = new ArrayList<>();
            for (TableCatalog.Col kolom : TableCatalog.BY_NAME.get(tabel).columns()) {
                String nama = kolomFisik(true, tabel, kolom.name());
                if (nama == null) {
                    continue;
                }
                fisik.add(nama);
                nilai.add(item.get(kolom.name()));
                meta.add(kolom);
            }
            sisip(db, tabelFisik(true, tabel), fisik, nilai, meta);
        }
    }

    private static void sisip(Connection db, String tabel, List<String> kolom, List<Object> nilai, List<TableCatalog.Col> meta)
            throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(DatabaseSnapshot.kutip(tabel)).append(" (");
        for (int i = 0; i < kolom.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(DatabaseSnapshot.kutip(kolom.get(i)));
        }
        sql.append(") VALUES (");
        sql.append("?,".repeat(kolom.size()));
        sql.setLength(sql.length() - 1);
        sql.append(')');
        try (PreparedStatement ps = db.prepareStatement(sql.toString())) {
            for (int i = 0; i < kolom.size(); i++) {
                if (meta.get(i) == null) {
                    ikatTeks(ps, i + 1, nilai.get(i));
                } else {
                    DatabaseSnapshot.ikat(ps, i + 1, nilai.get(i), meta.get(i));
                }
            }
            ps.executeUpdate();
        }
    }

    private static Map<Long, Lama> bacaLama(Connection db) throws SQLException {
        Map<Long, Lama> hasil = new LinkedHashMap<>();
        try (Statement perintah = db.createStatement();
             ResultSet rs = perintah.executeQuery("SELECT id_user, role, nama_lengkap, no_telepon FROM users")) {
            while (rs.next()) {
                hasil.put(rs.getLong("id_user"), new Lama(
                        rs.getString("role"),
                        rs.getString("nama_lengkap") == null ? "" : rs.getString("nama_lengkap"),
                        rs.getString("no_telepon") == null ? "" : rs.getString("no_telepon")
                ));
            }
        }
        return hasil;
    }

    private static Map<Long, String> bacaBpjs(Connection db) throws SQLException {
        Map<Long, String> hasil = new LinkedHashMap<>();
        try (Statement perintah = db.createStatement();
             ResultSet rs = perintah.executeQuery("SELECT id_pasien, nomor_bpjs FROM pasien")) {
            while (rs.next()) {
                String nomor = rs.getString("nomor_bpjs");
                if (nomor != null && !nomor.isBlank()) {
                    hasil.put(rs.getLong("id_pasien"), nomor);
                }
            }
        }
        return hasil;
    }

    static boolean adaTabel(Connection db, String nama) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, nama);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static TableCatalog.Col kolom(String tabel, String nama) {
        return TableCatalog.BY_NAME.get(tabel).kolom(nama);
    }

    private static Map<Long, Map<String, Object>> indeks(List<Map<String, Object>> baris, String kunci) {
        Map<Long, Map<String, Object>> hasil = new LinkedHashMap<>();
        for (Map<String, Object> item : baris) {
            hasil.put(DatabaseRecords.lng(item.get(kunci)), item);
        }
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> daftar(Map<String, Object> data, String nama) {
        Object nilai = data.get(nama);
        if (nilai instanceof List<?> daftar) {
            return (List<Map<String, Object>>) daftar;
        }
        return List.of();
    }

    private static String teks(Object nilai) {
        return nilai == null ? "" : String.valueOf(nilai).trim();
    }

    private static String potong(String nilai, int batas) {
        return nilai.length() <= batas ? nilai : nilai.substring(0, batas);
    }

    private static void ikatTeks(PreparedStatement ps, int indeks, Object nilai) throws SQLException {
        if (nilai == null || String.valueOf(nilai).isBlank()) {
            ps.setNull(indeks, Types.VARCHAR);
            return;
        }
        ps.setString(indeks, String.valueOf(nilai));
    }

    private record Lama(String role, String nama, String telepon) {}
}
