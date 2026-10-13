package mediflow;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** CRUD satu tabel, plus aturan jadwal, pendaftaran, dan notifikasi. */
final class Resource {
    record Jawaban(int status, Map<String, Object> body) {}

    private record Klausul(String name, String param) {}

    private Resource() {}

    static Jawaban daftar(Connection db, Catalog.Table tabel, Map<String, String> saringan) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(Snapshot.kutip(tabel.name()));
        List<Object> nilai = new ArrayList<>();
        boolean awal = true;
        for (String kolom : tabel.filters()) {
            if (!saringan.containsKey(kolom)) {
                continue;
            }
            String isi = saringan.get(kolom);
            if (isi == null || isi.isEmpty()) {
                continue;
            }
            sql.append(awal ? " WHERE " : " AND ");
            awal = false;
            sql.append(Snapshot.kutip(kolom)).append(" = ?");
            if (tabel.booleans().contains(kolom)) {
                nilai.add(Values.nyala(isi) ? 1 : 0);
            } else {
                nilai.add(isi);
            }
        }
        sql.append(" ORDER BY ").append(Snapshot.kutip(tabel.pk())).append(" ASC");
        List<Map<String, Object>> baris = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(sql.toString())) {
            ikatDaftar(ps, nilai);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    baris.add(bacaBaris(tabel, rs));
                }
            }
        }
        return ok("Data " + tabel.label() + " berhasil diambil", baris, 200);
    }

    static Jawaban satu(Connection db, Catalog.Table tabel, long id) throws SQLException {
        Map<String, Object> baris = cari(db, tabel, id);
        if (baris == null) {
            return gagal("Data " + tabel.label() + " tidak ditemukan", 404, null);
        }
        return ok("Data " + tabel.label() + " berhasil diambil", baris, 200);
    }

    static Jawaban buat(Connection db, Catalog.Table tabel, Map<String, Object> mentah) throws SQLException {
        Map<String, Object> data = siapkan(tabel, mentah);
        List<String> kurang = kurang(tabel, data);
        if (!kurang.isEmpty()) {
            return gagal("Data " + tabel.label() + " belum lengkap", 400, Map.of("wajib", kurang));
        }
        String aturan = periksa(db, tabel, data, Map.of(), null);
        if (aturan != null) {
            return gagal(aturan, 422, null);
        }
        Map<String, String> galat = validasi(db, tabel, salinCek(tabel, data), null);
        if (!galat.isEmpty()) {
            return gagal("Data " + tabel.label() + " gagal ditambahkan", 422, new LinkedHashMap<>(galat));
        }
        try {
            long id = sisip(db, tabel, data);
            return ok("Data " + tabel.label() + " berhasil ditambahkan", Map.of(tabel.pk(), id), 201);
        } catch (SQLException error) {
            return galatBasis(tabel, error, "Data " + tabel.label() + " gagal ditambahkan");
        }
    }

    static Jawaban ubah(Connection db, Catalog.Table tabel, long id, Map<String, Object> mentah) throws SQLException {
        Map<String, Object> sebelum = cari(db, tabel, id);
        if (sebelum == null) {
            return gagal("Data " + tabel.label() + " tidak ditemukan", 404, null);
        }
        Map<String, Object> data = siapkan(tabel, mentah);
        if (data.isEmpty()) {
            return gagal("Data pembaruan tidak boleh kosong", 400, null);
        }
        String aturan = periksa(db, tabel, data, sebelum, id);
        if (aturan != null) {
            return gagal(aturan, 422, null);
        }
        Map<String, String> galat = validasi(db, tabel, salinCek(tabel, data), id);
        if (!galat.isEmpty()) {
            return gagal("Data " + tabel.label() + " gagal diperbarui", 422, new LinkedHashMap<>(galat));
        }
        try {
            perbarui(db, tabel, id, data);
            return ok("Data " + tabel.label() + " berhasil diperbarui");
        } catch (SQLException error) {
            return galatBasis(tabel, error, "Data " + tabel.label() + " gagal diperbarui");
        }
    }

    static Jawaban hapus(Connection db, Catalog.Table tabel, long id) throws SQLException {
        if (cari(db, tabel, id) == null) {
            return gagal("Data " + tabel.label() + " tidak ditemukan", 404, null);
        }
        String sql = "DELETE FROM " + Snapshot.kutip(tabel.name()) + " WHERE " + Snapshot.kutip(tabel.pk()) + " = ?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setLong(1, id);
            int jumlah = ps.executeUpdate();
            if (jumlah == 0) {
                return gagal("Data " + tabel.label() + " tidak bisa dihapus karena masih dipakai.", 409, null);
            }
            return ok("Data " + tabel.label() + " berhasil dihapus");
        } catch (SQLException error) {
            return galatBasis(tabel, error, "Data " + tabel.label() + " tidak bisa dihapus karena masih dipakai.");
        }
    }

    private static Map<String, Object> siapkan(Catalog.Table tabel, Map<String, Object> mentah) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (String bidang : tabel.writable()) {
            if (mentah.containsKey(bidang)) {
                data.put(bidang, mentah.get(bidang));
            }
        }
        for (String kolom : tabel.nullable()) {
            if (data.containsKey(kolom) && "".equals(data.get(kolom))) {
                data.put(kolom, null);
            }
        }
        for (String kolom : tabel.booleans()) {
            Object nilai = data.get(kolom);
            if (data.containsKey(kolom) && nilai != null && !"".equals(nilai)) {
                data.put(kolom, Values.nyala(nilai) ? 1 : 0);
            }
        }
        for (String kolom : tabel.dates()) {
            Object nilai = data.get(kolom);
            if (nilai instanceof String teks && !teks.isEmpty() && teks.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                data.put(kolom, teks.substring(0, 10));
            }
        }
        for (String kolom : tabel.datetimes()) {
            Object nilai = data.get(kolom);
            if (nilai instanceof String teks && !teks.isEmpty()) {
                String sql = Values.keSqlWaktu(teks);
                if (sql != null) {
                    data.put(kolom, sql);
                }
            }
        }
        for (String kolom : tabel.times()) {
            Object nilai = data.get(kolom);
            if (nilai instanceof String teks && !teks.isEmpty()) {
                String jam = Values.keJam(teks);
                if (jam != null) {
                    data.put(kolom, jam);
                }
            }
        }
        for (String kolom : tabel.integers()) {
            Object nilai = data.get(kolom);
            if (data.containsKey(kolom) && Values.numerik(nilai)) {
                data.put(kolom, Values.keLong(nilai));
            }
        }
        for (String kolom : tabel.decimals()) {
            Object nilai = data.get(kolom);
            if (data.containsKey(kolom) && Values.numerik(nilai)) {
                data.put(kolom, Values.keDouble(nilai));
            }
        }
        return data;
    }

    private static List<String> kurang(Catalog.Table tabel, Map<String, Object> data) {
        List<String> hilang = new ArrayList<>();
        for (String kolom : tabel.required()) {
            if (!data.containsKey(kolom) || data.get(kolom) == null) {
                hilang.add(kolom);
                continue;
            }
            if ("".equals(data.get(kolom)) && !tabel.allowEmpty().contains(kolom)) {
                hilang.add(kolom);
            }
        }
        return hilang;
    }

    private static Map<String, Object> salinCek(Catalog.Table tabel, Map<String, Object> data) {
        Map<String, Object> cek = new LinkedHashMap<>(data);
        for (String kolom : tabel.nullable()) {
            if (cek.containsKey(kolom) && cek.get(kolom) == null) {
                cek.put(kolom, "");
            }
        }
        return cek;
    }

    private static Map<String, String> validasi(Connection db, Catalog.Table tabel, Map<String, Object> data, Long id) throws SQLException {
        Map<String, String> galat = new LinkedHashMap<>();
        boolean pembaruan = id != null;
        for (Catalog.Field bidang : tabel.fields()) {
            List<Klausul> klausul = klausul(bidang.rules());
            boolean jikaAda = klausul.stream().anyMatch(item -> item.name.equals("if_exist"));
            boolean bolehKosong = klausul.stream().anyMatch(item -> item.name.equals("permit_empty"));
            if (pembaruan && !data.containsKey(bidang.name())) {
                continue;
            }
            if (!pembaruan && jikaAda && !data.containsKey(bidang.name())) {
                continue;
            }
            Object nilai = data.get(bidang.name());
            String teks = Values.phpString(nilai);
            boolean kosong = nilai == null || teks.trim().isEmpty();
            for (Klausul aturan : klausul) {
                if (aturan.name.equals("if_exist") || aturan.name.equals("permit_empty")) {
                    continue;
                }
                if (kosong && bolehKosong && !aturan.name.equals("required")) {
                    continue;
                }
                if (!lolos(db, aturan, teks, id)) {
                    galat.put(bidang.name(), pesanAturan(bidang, aturan));
                    break;
                }
            }
        }
        return galat;
    }

    private static boolean lolos(Connection db, Klausul aturan, String teks, Long id) throws SQLException {
        return switch (aturan.name) {
            case "required" -> !teks.trim().isEmpty();
            case "min_length" -> teks.codePointCount(0, teks.length()) >= Integer.parseInt(aturan.param);
            case "max_length" -> teks.codePointCount(0, teks.length()) <= Integer.parseInt(aturan.param);
            case "integer" -> teks.matches("[+-]?\\d+");
            case "numeric" -> teks.matches("[+-]?\\d*\\.?\\d+");
            case "is_natural_no_zero" -> teks.matches("\\d+") && !"0".equals(teks);
            case "greater_than" -> angka(teks) && Double.parseDouble(teks) > Double.parseDouble(aturan.param);
            case "greater_than_equal_to" -> angka(teks) && Double.parseDouble(teks) >= Double.parseDouble(aturan.param);
            case "less_than_equal_to" -> angka(teks) && Double.parseDouble(teks) <= Double.parseDouble(aturan.param);
            case "in_list" -> List.of(aturan.param.split(",")).contains(teks);
            case "regex_match" -> cocok(teks, aturan.param);
            case "valid_date" -> Values.tanggalSah(teks, aturan.param);
            case "is_unique" -> unik(db, aturan.param, teks, id);
            case "is_not_unique" -> sudahAda(db, aturan.param, teks);
            default -> true;
        };
    }

    private static boolean angka(String teks) {
        return teks.matches("[+-]?\\d*\\.?\\d+");
    }

    private static boolean cocok(String teks, String pola) {
        String inti = pola;
        if (inti.length() >= 2 && inti.startsWith("/") && inti.endsWith("/")) {
            inti = inti.substring(1, inti.length() - 1);
        }
        return Pattern.compile(inti).matcher(teks).find();
    }

    private static boolean unik(Connection db, String param, String teks, Long id) throws SQLException {
        String[] bagian = param.split(",", 3);
        String[] letak = bagian[0].split("\\.", 2);
        String abaikan = bagian.length > 1 ? bagian[1] : null;
        String nilaiAbaikan = bagian.length > 2 ? bagian[2] : null;
        if (id != null && nilaiAbaikan != null && nilaiAbaikan.matches("\\{\\w+}")) {
            nilaiAbaikan = Long.toString(id);
        } else if (nilaiAbaikan != null && nilaiAbaikan.matches("\\{\\w+}")) {
            abaikan = null;
        }
        StringBuilder sql = new StringBuilder("SELECT 1 FROM ")
                .append(Snapshot.kutip(letak[0]))
                .append(" WHERE ")
                .append(Snapshot.kutip(letak[1]))
                .append(" = ?");
        if (abaikan != null && nilaiAbaikan != null && !nilaiAbaikan.isEmpty()) {
            sql.append(" AND ").append(Snapshot.kutip(abaikan)).append(" <> ?");
        }
        sql.append(" LIMIT 1");
        try (PreparedStatement ps = db.prepareStatement(sql.toString())) {
            ps.setString(1, teks);
            if (abaikan != null && nilaiAbaikan != null && !nilaiAbaikan.isEmpty()) {
                ps.setString(2, nilaiAbaikan);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        }
    }

    private static boolean sudahAda(Connection db, String param, String teks) throws SQLException {
        String[] letak = param.split(",", 3)[0].split("\\.", 2);
        String sql = "SELECT 1 FROM " + Snapshot.kutip(letak[0]) + " WHERE " + Snapshot.kutip(letak[1]) + " = ? LIMIT 1";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, teks);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String pesanAturan(Catalog.Field bidang, Klausul aturan) {
        String khusus = bidang.messages().get(aturan.name);
        if (khusus != null) {
            return khusus;
        }
        String template = switch (aturan.name) {
            case "required" -> "Kolom {field} wajib diisi.";
            case "min_length" -> "Kolom {field} minimal {param} karakter.";
            case "max_length" -> "Kolom {field} tidak boleh lebih dari {param} karakter.";
            case "integer" -> "Kolom {field} harus berupa bilangan bulat.";
            case "numeric" -> "Kolom {field} harus berupa angka.";
            case "is_natural_no_zero" -> "Kolom {field} harus berupa angka lebih dari nol.";
            case "greater_than" -> "Kolom {field} harus lebih besar dari {param}.";
            case "greater_than_equal_to" -> "Kolom {field} harus lebih besar atau sama dengan {param}.";
            case "less_than_equal_to" -> "Kolom {field} harus lebih kecil atau sama dengan {param}.";
            case "in_list" -> "Kolom {field} harus salah satu dari: {param}.";
            case "regex_match" -> "Kolom {field} tidak sesuai format.";
            case "valid_date" -> "Kolom {field} harus berupa tanggal yang valid.";
            case "is_unique" -> "Kolom {field} sudah terdaftar.";
            case "is_not_unique" -> "Kolom {field} harus sudah ada di database.";
            default -> "Kolom {field} tidak valid.";
        };
        return template.replace("{field}", bidang.name()).replace("{param}", aturan.param == null ? "" : aturan.param);
    }

    private static List<Klausul> klausul(String spec) {
        List<Klausul> hasil = new ArrayList<>();
        int i = 0;
        while (i < spec.length()) {
            int dalam = 0;
            int j = i;
            for (; j < spec.length(); j++) {
                char huruf = spec.charAt(j);
                if (huruf == '[') {
                    dalam++;
                } else if (huruf == ']') {
                    dalam = Math.max(0, dalam - 1);
                } else if (huruf == '|' && dalam == 0) {
                    break;
                }
            }
            String bagian = spec.substring(i, j);
            int buka = bagian.indexOf('[');
            if (buka < 0) {
                hasil.add(new Klausul(bagian, ""));
            } else {
                hasil.add(new Klausul(bagian.substring(0, buka), bagian.substring(buka + 1, bagian.length() - 1)));
            }
            i = j + 1;
        }
        return hasil;
    }

    private static String periksa(Connection db, Catalog.Table tabel, Map<String, Object> data, Map<String, Object> sebelum, Long id) throws SQLException {
        return switch (tabel.check() == null ? "" : tabel.check()) {
            case "jadwal" -> periksaJadwal(db, data, sebelum, id);
            case "pendaftaran" -> periksaPendaftaran(db, data, sebelum, id);
            case "notifikasi" -> periksaNotifikasi(db, data, sebelum);
            default -> null;
        };
    }

    private static String periksaJadwal(Connection db, Map<String, Object> data, Map<String, Object> sebelum, Long id) throws SQLException {
        Object mulai = nilai(data, sebelum, "jam_mulai");
        Object selesai = nilai(data, sebelum, "jam_selesai");
        Object hari = nilai(data, sebelum, "hari");
        Object dokter = nilai(data, sebelum, "id_dokter");
        boolean aktif = data.containsKey("is_active")
                ? Values.nyala(data.get("is_active"))
                : sebelum.isEmpty() || Values.nyala(sebelum.containsKey("is_active") ? sebelum.get("is_active") : 1);
        if (mulai instanceof String jamMulai && selesai instanceof String jamSelesai && Values.menit(jamMulai) >= Values.menit(jamSelesai)) {
            return "Jam selesai harus setelah jam mulai.";
        }
        if (aktif && Values.ada(dokter) && Values.ada(hari) && mulai instanceof String jamMulai && selesai instanceof String jamSelesai) {
            String sql = "SELECT id_jadwal, jam_mulai, jam_selesai FROM jadwal_dokter WHERE id_dokter = ? AND hari = ? AND is_active = 1";
            try (PreparedStatement ps = db.prepareStatement(sql)) {
                ps.setLong(1, Values.keLong(dokter));
                ps.setString(2, String.valueOf(hari));
                try (ResultSet rs = ps.executeQuery()) {
                    int awal = Values.menit(jamMulai);
                    int akhir = Values.menit(jamSelesai);
                    while (rs.next()) {
                        if (id != null && rs.getLong("id_jadwal") == id) {
                            continue;
                        }
                        int lainAwal = Values.menit(rs.getString("jam_mulai"));
                        int lainAkhir = Values.menit(rs.getString("jam_selesai"));
                        if (awal < lainAkhir && lainAwal < akhir) {
                            return "Jam ini bertumpuk dengan jadwal lain dokter yang sama.";
                        }
                    }
                }
            }
        }
        if (id != null
                && data.containsKey("is_active")
                && !Values.nyala(data.get("is_active"))
                && Values.nyala(sebelum.getOrDefault("is_active", 0))
                && antreanHariIni(db, id) > 0) {
            return "Jadwal ini masih punya antrean hari ini.";
        }
        if (id != null && data.containsKey("kuota_maksimal")) {
            if (Values.keLong(data.get("kuota_maksimal")) < antreanHariIni(db, id)) {
                return "Kuota lebih kecil dari antrean yang sudah terdaftar hari ini.";
            }
        }
        return null;
    }

    private static String periksaPendaftaran(Connection db, Map<String, Object> data, Map<String, Object> sebelum, Long id) throws SQLException {
        Object pasien = nilai(data, sebelum, "id_pasien");
        Object jadwal = nilai(data, sebelum, "id_jadwal");
        Object tanggal = nilai(data, sebelum, "tanggal_kunjungan");
        Object nomor = nilai(data, sebelum, "nomor_antrean");
        Object status = nilai(data, sebelum, "status_antrean");
        if (status == null) {
            status = "Menunggu";
        }
        if (Values.ada(jadwal) && Values.ada(tanggal) && Values.ada(nomor)) {
            long sama = hitung(db, """
                    SELECT COUNT(*) FROM pendaftaran_poli
                    WHERE id_jadwal = ? AND tanggal_kunjungan = ? AND nomor_antrean = ?
                    AND (? IS NULL OR id_pendaftaran <> ?)
                    """, jadwal, tanggal, nomor, id, id);
            if (sama > 0) {
                return "Nomor antrean sudah dipakai pada jadwal ini.";
            }
        }
        if (!"Batal".equals(String.valueOf(status)) && Values.ada(pasien) && Values.ada(jadwal) && Values.ada(tanggal)) {
            long ganda = hitung(db, """
                    SELECT COUNT(*) FROM pendaftaran_poli
                    WHERE id_pasien = ? AND id_jadwal = ? AND tanggal_kunjungan = ? AND status_antrean <> 'Batal'
                    AND (? IS NULL OR id_pendaftaran <> ?)
                    """, pasien, jadwal, tanggal, id, id);
            if (ganda > 0) {
                return "Pasien sudah terdaftar pada jadwal ini.";
            }
        }
        return null;
    }

    private static String periksaNotifikasi(Connection db, Map<String, Object> data, Map<String, Object> sebelum) throws SQLException {
        Object mentah = nilai(data, sebelum, "pesan");
        String pesan = mentah == null ? "" : String.valueOf(mentah).trim();
        if (pesan.isEmpty()) {
            return null;
        }
        if (Pattern.compile("whatsapp", Pattern.CASE_INSENSITIVE).matcher(pesan).find()) {
            return "Pesan notifikasi tidak boleh menyebut WhatsApp.";
        }
        try (Statement perintah = db.createStatement();
             ResultSet rs = perintah.executeQuery("SELECT nama_obat FROM obat")) {
            while (rs.next()) {
                String nama = rs.getString(1);
                if (nama != null && !nama.trim().isEmpty() && berisi(pesan, nama.trim())) {
                    return "Pesan notifikasi tidak boleh memuat nama obat.";
                }
            }
        }
        try (Statement perintah = db.createStatement();
             ResultSet rs = perintah.executeQuery("SELECT diagnosis FROM pemeriksaan")) {
            while (rs.next()) {
                String diagnosis = rs.getString(1);
                if (diagnosis == null) {
                    continue;
                }
                diagnosis = diagnosis.trim();
                if (diagnosis.getBytes(StandardCharsets.UTF_8).length < 4) {
                    continue;
                }
                if (berisi(pesan, diagnosis)) {
                    return "Pesan notifikasi tidak boleh memuat diagnosis.";
                }
            }
        }
        return null;
    }

    private static boolean berisi(String pesan, String cari) {
        return pesan.toLowerCase(Locale.ROOT).contains(cari.toLowerCase(Locale.ROOT));
    }

    private static long antreanHariIni(Connection db, long idJadwal) throws SQLException {
        String hariIni = LocalDate.now(ZoneId.of("Asia/Jakarta")).toString();
        return hitung(db, """
                SELECT COUNT(*) FROM pendaftaran_poli
                WHERE id_jadwal = ? AND tanggal_kunjungan = ? AND status_antrean <> 'Batal'
                """, idJadwal, hariIni);
    }

    private static long hitung(Connection db, String sql, Object... nilai) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            for (int i = 0; i < nilai.length; i++) {
                ikatNilai(ps, i + 1, nilai[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static Object nilai(Map<String, Object> data, Map<String, Object> sebelum, String kunci) {
        if (data.containsKey(kunci) && data.get(kunci) != null) {
            return data.get(kunci);
        }
        if (sebelum.containsKey(kunci) && sebelum.get(kunci) != null) {
            return sebelum.get(kunci);
        }
        return null;
    }

    private static Map<String, Object> cari(Connection db, Catalog.Table tabel, long id) throws SQLException {
        String sql = "SELECT * FROM " + Snapshot.kutip(tabel.name()) + " WHERE " + Snapshot.kutip(tabel.pk()) + " = ?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return bacaBaris(tabel, rs);
            }
        }
    }

    private static Map<String, Object> bacaBaris(Catalog.Table tabel, ResultSet rs) throws SQLException {
        Map<String, Object> baris = new LinkedHashMap<>();
        for (Catalog.Col kolom : tabel.columns()) {
            baris.put(kolom.name(), Values.bacaCrud(rs.getString(kolom.name()), kolom));
        }
        return baris;
    }

    private static long sisip(Connection db, Catalog.Table tabel, Map<String, Object> data) throws SQLException {
        List<String> kolom = new ArrayList<>(data.keySet());
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(Snapshot.kutip(tabel.name())).append(" (");
        for (int i = 0; i < kolom.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(Snapshot.kutip(kolom.get(i)));
        }
        sql.append(") VALUES (");
        sql.append("?,".repeat(Math.max(kolom.size(), 1)));
        sql.setLength(sql.length() - 1);
        sql.append(')');
        try (PreparedStatement ps = db.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < kolom.size(); i++) {
                ikatKolom(ps, i + 1, data.get(kolom.get(i)), tabel.kolom(kolom.get(i)));
            }
            ps.executeUpdate();
            try (ResultSet kunci = ps.getGeneratedKeys()) {
                if (kunci.next()) {
                    return kunci.getLong(1);
                }
            }
        }
        throw new SQLException("Id baru tidak kembali dari database.");
    }

    private static void perbarui(Connection db, Catalog.Table tabel, long id, Map<String, Object> data) throws SQLException {
        List<String> kolom = new ArrayList<>(data.keySet());
        StringBuilder sql = new StringBuilder("UPDATE ").append(Snapshot.kutip(tabel.name())).append(" SET ");
        for (int i = 0; i < kolom.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append(Snapshot.kutip(kolom.get(i))).append(" = ?");
        }
        sql.append(" WHERE ").append(Snapshot.kutip(tabel.pk())).append(" = ?");
        try (PreparedStatement ps = db.prepareStatement(sql.toString())) {
            for (int i = 0; i < kolom.size(); i++) {
                ikatKolom(ps, i + 1, data.get(kolom.get(i)), tabel.kolom(kolom.get(i)));
            }
            ps.setLong(kolom.size() + 1, id);
            ps.executeUpdate();
        }
    }

    private static void ikatKolom(PreparedStatement ps, int indeks, Object nilai, Catalog.Col kolom) throws SQLException {
        if (nilai == null || kolom == null) {
            ps.setNull(indeks, Types.VARCHAR);
            return;
        }
        switch (kolom.type()) {
            case INT -> ps.setLong(indeks, Values.keLong(nilai));
            case BOOL -> ps.setInt(indeks, Values.nyala(nilai) ? 1 : 0);
            case DEC -> ps.setDouble(indeks, Values.keDouble(nilai));
            default -> ps.setString(indeks, String.valueOf(nilai));
        }
    }

    private static void ikatDaftar(PreparedStatement ps, List<Object> nilai) throws SQLException {
        for (int i = 0; i < nilai.size(); i++) {
            ikatNilai(ps, i + 1, nilai.get(i));
        }
    }

    private static void ikatNilai(PreparedStatement ps, int indeks, Object nilai) throws SQLException {
        if (nilai == null) {
            ps.setNull(indeks, Types.BIGINT);
        } else if (nilai instanceof Number angka) {
            ps.setLong(indeks, angka.longValue());
        } else {
            ps.setString(indeks, String.valueOf(nilai));
        }
    }

    private static Jawaban galatBasis(Catalog.Table tabel, SQLException error, String aksi) {
        String pesan = Values.pesan(error);
        int kode = 422;
        int vendor = error.getErrorCode();
        String rendah = pesan.toLowerCase(Locale.ROOT);
        if (vendor == 1062 || pesan.contains("1062") || pesan.contains("Duplicate")) {
            kode = 409;
            aksi = "Data " + tabel.label() + " sudah ada.";
        } else if (vendor == 1451 || vendor == 1452 || pesan.contains("1451") || pesan.contains("1452") || rendah.contains("foreign key")) {
            kode = 409;
            aksi = "Data " + tabel.label() + " masih terhubung ke catatan lain.";
        }
        Map<String, Object> rinci = new LinkedHashMap<>();
        rinci.put("database", pesan);
        return gagal(aksi, kode, rinci);
    }

    private static Jawaban ok(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", true);
        body.put("message", message);
        return new Jawaban(200, body);
    }

    private static Jawaban ok(String message, Object data, int kode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", true);
        body.put("message", message);
        body.put("data", data);
        return new Jawaban(kode, body);
    }

    private static Jawaban gagal(String message, int kode, Map<String, ?> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", false);
        body.put("message", message);
        if (errors != null && !errors.isEmpty()) {
            body.put("errors", errors);
        }
        return new Jawaban(kode, body);
    }
}
