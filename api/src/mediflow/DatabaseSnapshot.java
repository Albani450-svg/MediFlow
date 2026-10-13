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

/** Baca dan tulis seluruh medicflow_db dalam bentuk yang dipakai PWA. */
final class DatabaseSnapshot {
    private DatabaseSnapshot() {}

    static Map<String, List<Map<String, Object>>> baca(Connection db) throws SQLException {
        Map<String, List<Map<String, Object>>> hasil = new LinkedHashMap<>();
        for (TableCatalog.Table tabel : TableCatalog.TABLES) {
            List<Map<String, Object>> baris = new ArrayList<>();
            try (Statement perintah = db.createStatement();
                 ResultSet rs = perintah.executeQuery("SELECT * FROM " + kutip(tabel.name()))) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    for (TableCatalog.Col kolom : tabel.columns()) {
                        item.put(kolom.name(), ColumnValues.baca(rs.getString(kolom.name()), kolom));
                    }
                    baris.add(item);
                }
            }
            hasil.put(tabel.name(), baris);
        }
        return hasil;
    }

    static void simpan(Connection db, Map<String, Object> data) throws SQLException {
        for (TableCatalog.Table tabel : TableCatalog.TABLES) {
            if (!(data.get(tabel.name()) instanceof List<?>)) {
                throw new IllegalStateException("Tabel " + tabel.name() + " tidak lengkap.");
            }
        }
        if (((List<?>) data.get("users")).isEmpty()) {
            throw new IllegalStateException("Users kosong, penyimpanan dibatalkan.");
        }

        boolean otomatis = db.getAutoCommit();
        db.setAutoCommit(false);
        try (Statement perintah = db.createStatement()) {
            perintah.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (int i = TableCatalog.TABLES.size() - 1; i >= 0; i--) {
                perintah.execute("DELETE FROM " + kutip(TableCatalog.TABLES.get(i).name()));
            }
            for (TableCatalog.Table tabel : TableCatalog.TABLES) {
                for (Object item : (List<?>) data.get(tabel.name())) {
                    if (!(item instanceof Map<?, ?> mentah)) {
                        throw new IllegalStateException("Baris " + tabel.name() + " tidak valid.");
                    }
                    Map<String, Object> baris = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entri : mentah.entrySet()) {
                        baris.put(String.valueOf(entri.getKey()), entri.getValue());
                    }
                    if ("pemeriksaan".equals(tabel.name()) && baris.get("waktu_mulai") == null) {
                        Object dibuat = baris.get("created_at");
                        baris.put("waktu_mulai", dibuat != null ? dibuat : ColumnValues.sekarangUtc());
                    }
                    sisip(db, tabel, baris);
                }
            }
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

    private static void sisip(Connection db, TableCatalog.Table tabel, Map<String, Object> baris) throws SQLException {
        List<TableCatalog.Col> kolom = tabel.columns();
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(kutip(tabel.name())).append(" (");
        for (int i = 0; i < kolom.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(kutip(kolom.get(i).name()));
        }
        sql.append(") VALUES (");
        sql.append("?,".repeat(kolom.size()));
        sql.setLength(sql.length() - 1);
        sql.append(')');
        try (PreparedStatement ps = db.prepareStatement(sql.toString())) {
            for (int i = 0; i < kolom.size(); i++) {
                ikat(ps, i + 1, baris.get(kolom.get(i).name()), kolom.get(i));
            }
            ps.executeUpdate();
        }
    }

    private static void ikat(PreparedStatement ps, int indeks, Object nilai, TableCatalog.Col kolom) throws SQLException {
        boolean kosong = nilai == null || (nilai instanceof String teks && teks.isEmpty());
        if (kosong) {
            if (kolom.type() == TableCatalog.Type.BOOL) {
                ps.setInt(indeks, 0);
                return;
            }
            if (kolom.type() == TableCatalog.Type.STR && !kolom.nullable()) {
                ps.setString(indeks, "");
                return;
            }
            ps.setNull(indeks, Types.VARCHAR);
            return;
        }
        switch (kolom.type()) {
            case INT -> ps.setLong(indeks, ColumnValues.keLong(nilai));
            case BOOL -> ps.setInt(indeks, ColumnValues.benarSnapshot(nilai) ? 1 : 0);
            case DEC -> ps.setDouble(indeks, ColumnValues.numerik(nilai) ? ColumnValues.keDouble(nilai) : 0d);
            case DT -> {
                String sql = ColumnValues.keSql(String.valueOf(nilai));
                if (sql == null) {
                    ps.setNull(indeks, Types.VARCHAR);
                } else {
                    ps.setString(indeks, sql);
                }
            }
            default -> ps.setString(indeks, String.valueOf(nilai));
        }
    }

    static String kutip(String nama) {
        if (!nama.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Nama kolom tidak valid.");
        }
        return "`" + nama + "`";
    }
}
