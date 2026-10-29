package mediflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Salinan tabel mediflow yang diubah aplikasi. */
final class DatabaseRecords {
    final List<Pengguna> users = new ArrayList<>();
    final List<Pasien> pasien = new ArrayList<>();
    final List<Dokter> dokter = new ArrayList<>();
    final List<Poli> poliklinik = new ArrayList<>();
    final List<Jadwal> jadwal_dokter = new ArrayList<>();
    final List<Obat> obat = new ArrayList<>();
    final List<Daftar> pendaftaran_poli = new ArrayList<>();
    final List<Periksa> pemeriksaan = new ArrayList<>();
    final List<Resep> resep = new ArrayList<>();
    final List<Detail> detail_resep = new ArrayList<>();
    final List<Kabar> notifikasi = new ArrayList<>();
    final List<Otp> otp_verifikasi = new ArrayList<>();
    final List<Ambil> pengambilan_obat = new ArrayList<>();
    final List<Mutasi> mutasi_stok = new ArrayList<>();
    final List<Jejak> log_aktivitas = new ArrayList<>();

    static DatabaseRecords dari(Map<String, List<Map<String, Object>>> data) {
        DatabaseRecords basis = new DatabaseRecords();
        for (Map<String, Object> baris : daftar(data, "dokter")) {
            basis.dokter.add(Dokter.dari(samakanAkun(baris, "account_id")));
        }
        for (Map<String, Object> baris : daftar(data, "poliklinik")) {
            basis.poliklinik.add(Poli.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "jadwal_dokter")) {
            basis.jadwal_dokter.add(Jadwal.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "obat")) {
            basis.obat.add(Obat.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "pendaftaran_poli")) {
            basis.pendaftaran_poli.add(Daftar.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "pemeriksaan")) {
            basis.pemeriksaan.add(Periksa.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "resep")) {
            basis.resep.add(Resep.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "detail_resep")) {
            basis.detail_resep.add(Detail.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "notifikasi")) {
            basis.notifikasi.add(Kabar.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "otp_verifikasi")) {
            basis.otp_verifikasi.add(Otp.dari(samakanAkun(baris, "account_id")));
        }
        for (Map<String, Object> baris : daftar(data, "pengambilan_obat")) {
            basis.pengambilan_obat.add(Ambil.dari(samakanAkun(baris, "account_petugas", "id_user_petugas")));
        }
        for (Map<String, Object> baris : daftar(data, "mutasi_stok")) {
            basis.mutasi_stok.add(Mutasi.dari(samakanAkun(baris, "account_user")));
        }
        for (Map<String, Object> baris : daftar(data, "log_aktivitas")) {
            basis.log_aktivitas.add(Jejak.dari(samakanAkun(baris, "account_id")));
        }
        susunIdentitas(basis, daftar(data, "accounts"), daftar(data, "admins"), daftar(data, "farmasi_staff"), daftar(data, "patients"));
        if (basis.users.isEmpty()) {
            for (Map<String, Object> baris : daftar(data, "users")) {
                basis.users.add(Pengguna.dari(baris));
            }
            for (Map<String, Object> baris : daftar(data, "pasien")) {
                basis.pasien.add(Pasien.dari(baris));
            }
        }
        return basis;
    }

    Map<String, Object> kePeta() {
        Map<String, Object> hasil = new LinkedHashMap<>();
        hasil.put("accounts", petaAkun());
        hasil.put("admins", petaAdmin());
        hasil.put("farmasi_staff", petaFarmasi());
        hasil.put("poliklinik", peta(poliklinik));
        hasil.put("obat", peta(obat));
        hasil.put("patients", petaPasien());
        hasil.put("dokter", petaDenganAkun(dokter, "id_user", "account_id"));
        hasil.put("jadwal_dokter", peta(jadwal_dokter));
        hasil.put("pendaftaran_poli", peta(pendaftaran_poli));
        hasil.put("pemeriksaan", peta(pemeriksaan));
        hasil.put("resep", peta(resep));
        hasil.put("detail_resep", peta(detail_resep));
        hasil.put("notifikasi", peta(notifikasi));
        hasil.put("otp_verifikasi", petaDenganAkun(otp_verifikasi, "id_user", "account_id"));
        hasil.put("pengambilan_obat", petaDenganAkun(pengambilan_obat, "id_user_petugas", "account_petugas"));
        hasil.put("mutasi_stok", petaDenganAkun(mutasi_stok, "id_user", "account_user"));
        hasil.put("log_aktivitas", petaDenganAkun(log_aktivitas, "id_user", "account_id"));
        return hasil;
    }

    private List<Map<String, Object>> petaAkun() {
        List<Map<String, Object>> hasil = new ArrayList<>();
        for (Pengguna user : users) {
            Map<String, Object> baris = baris();
            baris.put("account_id", user.id_user);
            baris.put("username", user.username);
            baris.put("email", kosong(user.email) ? null : user.email);
            baris.put("password_hash", user.password_hash);
            baris.put("is_active", user.is_active && !"PENDING".equals(user.account_status) && !"SUSPENDED".equals(user.account_status));
            baris.put("created_at", user.created_at);
            baris.put("updated_at", user.updated_at);
            hasil.add(baris);
        }
        return hasil;
    }

    private List<Map<String, Object>> petaAdmin() {
        List<Map<String, Object>> hasil = new ArrayList<>();
        for (Pengguna user : users) {
            if (!"admin".equals(user.role)) {
                continue;
            }
            Map<String, Object> baris = baris();
            baris.put("id_admin", user.id_profil > 0 ? user.id_profil : user.id_user);
            baris.put("account_id", user.id_user);
            baris.put("nama", user.nama_lengkap);
            baris.put("phone", kosong(user.no_telepon) ? null : user.no_telepon);
            baris.put("is_super", user.is_super || (user.id_profil == 0 && "admin".equalsIgnoreCase(user.username)));
            baris.put("created_at", user.created_at);
            baris.put("updated_at", user.updated_at);
            hasil.add(baris);
        }
        return hasil;
    }

    private List<Map<String, Object>> petaFarmasi() {
        List<Map<String, Object>> hasil = new ArrayList<>();
        for (Pengguna user : users) {
            if (!"farmasi".equals(user.role)) {
                continue;
            }
            Map<String, Object> baris = baris();
            baris.put("id_farmasi", user.id_profil > 0 ? user.id_profil : user.id_user);
            baris.put("account_id", user.id_user);
            baris.put("nama", user.nama_lengkap);
            baris.put("phone", kosong(user.no_telepon) ? null : user.no_telepon);
            baris.put("created_at", user.created_at);
            baris.put("updated_at", user.updated_at);
            hasil.add(baris);
        }
        return hasil;
    }

    private List<Map<String, Object>> petaPasien() {
        List<Map<String, Object>> hasil = new ArrayList<>();
        for (Pasien item : pasien) {
            Map<String, Object> baris = item.peta();
            baris.put("account_id", item.id_user);
            baris.remove("id_user");
            baris.remove("no_satusehat");
            baris.remove("privacy_consent_at");
            baris.remove("privacy_policy_version");
            hasil.add(baris);
        }
        return hasil;
    }

    private static List<Map<String, Object>> petaDenganAkun(List<? extends Baris> baris, String lama, String baru) {
        List<Map<String, Object>> hasil = new ArrayList<>();
        for (Baris item : baris) {
            Map<String, Object> peta = item.peta();
            if (peta.containsKey(lama)) {
                peta.put(baru, peta.remove(lama));
            }
            hasil.add(peta);
        }
        return hasil;
    }

    private static void susunIdentitas(
            DatabaseRecords basis,
            List<Map<String, Object>> accounts,
            List<Map<String, Object>> admins,
            List<Map<String, Object>> farmasi,
            List<Map<String, Object>> patients
    ) {
        if (accounts.isEmpty() && admins.isEmpty() && patients.isEmpty()) {
            return;
        }
        Map<Long, Map<String, Object>> adminBy = indeks(admins, "account_id");
        Map<Long, Map<String, Object>> farmasiBy = indeks(farmasi, "account_id");
        Map<Long, Dokter> dokterBy = new HashMap<>();
        for (Dokter item : basis.dokter) {
            dokterBy.put(item.id_user, item);
        }
        for (Map<String, Object> baris : patients) {
            basis.pasien.add(Pasien.dari(samakanAkun(baris, "account_id")));
        }
        Map<Long, Pasien> pasienBy = new HashMap<>();
        for (Pasien item : basis.pasien) {
            pasienBy.put(item.id_user, item);
        }
        for (Map<String, Object> akun : accounts) {
            long id = lng(akun.get("account_id"));
            boolean aktif = bool(akun.get("is_active"));
            Pengguna user = new Pengguna();
            user.id_user = id;
            user.username = str(akun.get("username"));
            user.email = str(akun.get("email"));
            user.password_hash = str(akun.get("password_hash"));
            user.is_active = aktif;
            user.account_status = aktif ? "ACTIVE" : "PENDING";
            user.created_at = str(akun.get("created_at"));
            user.updated_at = str(akun.get("updated_at"));
            Map<String, Object> admin = adminBy.get(id);
            Dokter dokter = dokterBy.get(id);
            Map<String, Object> petugas = farmasiBy.get(id);
            Pasien pasien = pasienBy.get(id);
            if (admin != null) {
                user.role = "admin";
                user.nama_lengkap = str(admin.get("nama"));
                user.no_telepon = str(admin.get("phone"));
                user.id_profil = lng(admin.get("id_admin"));
                user.is_super = bool(admin.get("is_super"));
            } else if (dokter != null) {
                user.role = "dokter";
                user.nama_lengkap = dokter.nama_dokter;
                if (!aktif || !dokter.is_active) {
                    user.is_active = false;
                    user.account_status = "PENDING";
                }
            } else if (petugas != null) {
                user.role = "farmasi";
                user.nama_lengkap = str(petugas.get("nama"));
                user.no_telepon = str(petugas.get("phone"));
                user.id_profil = lng(petugas.get("id_farmasi"));
            } else if (pasien != null) {
                user.role = "pasien";
                user.nama_lengkap = pasien.nama_lengkap;
                user.no_telepon = pasien.no_telepon;
            } else {
                String[] pending = pendingTerbaru(basis.log_aktivitas, id);
                user.role = pending == null ? "" : pending[0];
                user.nama_lengkap = pending == null ? user.username : pending[1];
                user.no_telepon = pending == null ? null : pending[2];
                if (!aktif) {
                    user.account_status = "PENDING";
                }
            }
            basis.users.add(user);
        }
    }

    private static Map<Long, Map<String, Object>> indeks(List<Map<String, Object>> baris, String kunci) {
        Map<Long, Map<String, Object>> hasil = new HashMap<>();
        for (Map<String, Object> item : baris) {
            hasil.put(lng(item.get(kunci)), item);
        }
        return hasil;
    }

    private static String[] pendingTerbaru(List<Jejak> log, long accountId) {
        Jejak pilih = null;
        for (Jejak item : log) {
            if (item.id_user != null && item.id_user == accountId && "REGISTER_PENDING".equals(item.aktivitas)
                    && (pilih == null || item.id_log > pilih.id_log)) {
                pilih = item;
            }
        }
        if (pilih == null || pilih.keterangan == null) {
            return null;
        }
        String[] bagian = pilih.keterangan.split("\n", -1);
        if (bagian.length < 3) {
            return null;
        }
        if (!("pasien".equals(bagian[0]) || "dokter".equals(bagian[0]) || "farmasi".equals(bagian[0]))) {
            return null;
        }
        return new String[] {bagian[0], bagian[1], bagian[2]};
    }

    private static Map<String, Object> samakanAkun(Map<String, Object> baris, String sumber) {
        return samakanAkun(baris, sumber, "id_user");
    }

    private static Map<String, Object> samakanAkun(Map<String, Object> baris, String sumber, String tujuan) {
        if (baris.containsKey(tujuan) || !baris.containsKey(sumber)) {
            return baris;
        }
        Map<String, Object> salinan = new LinkedHashMap<>(baris);
        salinan.put(tujuan, salinan.get(sumber));
        return salinan;
    }

    private static boolean kosong(String nilai) {
        return nilai == null || nilai.isBlank();
    }

    private static List<Map<String, Object>> peta(List<? extends Baris> baris) {
        List<Map<String, Object>> hasil = new ArrayList<>();
        for (Baris item : baris) {
            hasil.add(item.peta());
        }
        return hasil;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> daftar(Map<String, List<Map<String, Object>>> data, String nama) {
        Object nilai = data.get(nama);
        if (nilai instanceof List<?> daftar) {
            return (List<Map<String, Object>>) daftar;
        }
        return List.of();
    }

    static long lng(Object nilai) {
        if (nilai == null) {
            return 0L;
        }
        if (nilai instanceof Number angka) {
            return angka.longValue();
        }
        return Long.parseLong(String.valueOf(nilai).trim());
    }

    static Long lngOrNull(Object nilai) {
        if (nilai == null || String.valueOf(nilai).isBlank()) {
            return null;
        }
        return lng(nilai);
    }

    static double dbl(Object nilai) {
        if (nilai == null) {
            return 0d;
        }
        if (nilai instanceof Number angka) {
            return angka.doubleValue();
        }
        return Double.parseDouble(String.valueOf(nilai).trim());
    }

    static Double dblOrNull(Object nilai) {
        if (nilai == null || String.valueOf(nilai).isBlank()) {
            return null;
        }
        return dbl(nilai);
    }

    static boolean bool(Object nilai) {
        if (nilai instanceof Boolean bendera) {
            return bendera;
        }
        if (nilai instanceof Number angka) {
            return angka.intValue() == 1;
        }
        String teks = String.valueOf(nilai);
        return "1".equals(teks) || "true".equalsIgnoreCase(teks);
    }

    static String str(Object nilai) {
        return nilai == null ? null : String.valueOf(nilai);
    }

    static Map<String, Object> baris() {
        return new LinkedHashMap<>();
    }
}

interface Baris {
    Map<String, Object> peta();
}

final class Pengguna implements Baris {
    long id_user;
    String username;
    String email;
    String password_hash;
    String role;
    String nama_lengkap;
    String no_telepon;
    boolean is_active;
    String account_status = "ACTIVE";
    String wa_terverifikasi_pada;
    long id_profil;
    boolean is_super;
    String created_at;
    String updated_at;

    static Pengguna dari(Map<String, Object> baris) {
        Pengguna item = new Pengguna();
        item.id_user = DatabaseRecords.lng(baris.get("id_user"));
        item.username = DatabaseRecords.str(baris.get("username"));
        item.email = DatabaseRecords.str(baris.get("email"));
        item.password_hash = DatabaseRecords.str(baris.get("password_hash"));
        item.role = DatabaseRecords.str(baris.get("role"));
        item.nama_lengkap = DatabaseRecords.str(baris.get("nama_lengkap"));
        item.no_telepon = DatabaseRecords.str(baris.get("no_telepon"));
        item.is_active = DatabaseRecords.bool(baris.get("is_active"));
        String statusAkun = DatabaseRecords.str(baris.get("account_status"));
        item.account_status = statusAkun.isBlank() ? "ACTIVE" : statusAkun;
        item.wa_terverifikasi_pada = DatabaseRecords.str(baris.get("wa_terverifikasi_pada"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_user", id_user);
        baris.put("username", username);
        baris.put("email", email);
        baris.put("password_hash", password_hash);
        baris.put("role", role);
        baris.put("nama_lengkap", nama_lengkap);
        baris.put("no_telepon", no_telepon);
        baris.put("is_active", is_active);
        baris.put("account_status", account_status);
        baris.put("wa_terverifikasi_pada", wa_terverifikasi_pada);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Pasien implements Baris {
    long id_pasien;
    long id_user;
    String no_rekam_medis;
    String nik;
    String nomor_bpjs;
    String nama_lengkap;
    String tanggal_lahir;
    String jenis_kelamin;
    String alamat;
    String no_telepon;
    String no_satusehat;
    String privacy_consent_at;
    String privacy_policy_version;
    boolean terdaftar_satusehat;
    String created_at;
    String updated_at;

    static Pasien dari(Map<String, Object> baris) {
        Pasien item = new Pasien();
        item.id_pasien = DatabaseRecords.lng(baris.get("id_pasien"));
        item.id_user = DatabaseRecords.lng(baris.get("id_user"));
        item.no_rekam_medis = DatabaseRecords.str(baris.get("no_rekam_medis"));
        item.nik = DatabaseRecords.str(baris.get("nik"));
        item.nomor_bpjs = DatabaseRecords.str(baris.get("nomor_bpjs"));
        item.nama_lengkap = DatabaseRecords.str(baris.get("nama_lengkap"));
        item.tanggal_lahir = DatabaseRecords.str(baris.get("tanggal_lahir"));
        item.jenis_kelamin = DatabaseRecords.str(baris.get("jenis_kelamin"));
        item.alamat = DatabaseRecords.str(baris.get("alamat"));
        item.no_telepon = DatabaseRecords.str(baris.get("no_telepon"));
        item.no_satusehat = DatabaseRecords.str(baris.get("no_satusehat"));
        item.privacy_consent_at = DatabaseRecords.str(baris.get("privacy_consent_at"));
        item.privacy_policy_version = DatabaseRecords.str(baris.get("privacy_policy_version"));
        item.terdaftar_satusehat = DatabaseRecords.bool(baris.get("terdaftar_satusehat"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_pasien", id_pasien);
        baris.put("id_user", id_user);
        baris.put("no_rekam_medis", no_rekam_medis);
        baris.put("nik", nik);
        baris.put("nomor_bpjs", nomor_bpjs);
        baris.put("nama_lengkap", nama_lengkap);
        baris.put("tanggal_lahir", tanggal_lahir);
        baris.put("jenis_kelamin", jenis_kelamin);
        baris.put("alamat", alamat);
        baris.put("no_telepon", no_telepon);
        baris.put("no_satusehat", no_satusehat);
        baris.put("privacy_consent_at", privacy_consent_at);
        baris.put("privacy_policy_version", privacy_policy_version);
        baris.put("terdaftar_satusehat", terdaftar_satusehat);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Dokter implements Baris {
    long id_dokter;
    long id_user;
    long id_poli;
    String nama_dokter;
    String spesialisasi;
    int rata_waktu_periksa_menit;
    boolean is_active;
    String created_at;
    String updated_at;

    static Dokter dari(Map<String, Object> baris) {
        Dokter item = new Dokter();
        item.id_dokter = DatabaseRecords.lng(baris.get("id_dokter"));
        item.id_user = DatabaseRecords.lng(baris.get("id_user"));
        item.id_poli = DatabaseRecords.lng(baris.get("id_poli"));
        item.nama_dokter = DatabaseRecords.str(baris.get("nama_dokter"));
        item.spesialisasi = DatabaseRecords.str(baris.get("spesialisasi"));
        item.rata_waktu_periksa_menit = (int) DatabaseRecords.lng(baris.get("rata_waktu_periksa_menit"));
        item.is_active = DatabaseRecords.bool(baris.get("is_active"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_dokter", id_dokter);
        baris.put("id_user", id_user);
        baris.put("id_poli", id_poli);
        baris.put("nama_dokter", nama_dokter);
        baris.put("spesialisasi", spesialisasi);
        baris.put("rata_waktu_periksa_menit", rata_waktu_periksa_menit);
        baris.put("is_active", is_active);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Poli implements Baris {
    long id_poli;
    String nama_poli;
    String deskripsi;
    boolean is_active;
    String created_at;
    String updated_at;

    static Poli dari(Map<String, Object> baris) {
        Poli item = new Poli();
        item.id_poli = DatabaseRecords.lng(baris.get("id_poli"));
        item.nama_poli = DatabaseRecords.str(baris.get("nama_poli"));
        item.deskripsi = DatabaseRecords.str(baris.get("deskripsi"));
        item.is_active = DatabaseRecords.bool(baris.get("is_active"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_poli", id_poli);
        baris.put("nama_poli", nama_poli);
        baris.put("deskripsi", deskripsi);
        baris.put("is_active", is_active);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Jadwal implements Baris {
    long id_jadwal;
    long id_dokter;
    String hari;
    String jam_mulai;
    String jam_selesai;
    int kuota_maksimal;
    boolean is_active;
    String created_at;
    String updated_at;

    static Jadwal dari(Map<String, Object> baris) {
        Jadwal item = new Jadwal();
        item.id_jadwal = DatabaseRecords.lng(baris.get("id_jadwal"));
        item.id_dokter = DatabaseRecords.lng(baris.get("id_dokter"));
        item.hari = DatabaseRecords.str(baris.get("hari"));
        item.jam_mulai = DatabaseRecords.str(baris.get("jam_mulai"));
        item.jam_selesai = DatabaseRecords.str(baris.get("jam_selesai"));
        item.kuota_maksimal = (int) DatabaseRecords.lng(baris.get("kuota_maksimal"));
        item.is_active = DatabaseRecords.bool(baris.get("is_active"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_jadwal", id_jadwal);
        baris.put("id_dokter", id_dokter);
        baris.put("hari", hari);
        baris.put("jam_mulai", jam_mulai);
        baris.put("jam_selesai", jam_selesai);
        baris.put("kuota_maksimal", kuota_maksimal);
        baris.put("is_active", is_active);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Obat implements Baris {
    long id_obat;
    String kode_kemenkes;
    String nama_obat;
    String jenis_obat;
    String satuan;
    boolean cover_bpjs;
    int stok_rs;
    int stok_minimum;
    double harga;
    boolean is_active;
    String created_at;
    String updated_at;

    static Obat dari(Map<String, Object> baris) {
        Obat item = new Obat();
        item.id_obat = DatabaseRecords.lng(baris.get("id_obat"));
        item.kode_kemenkes = DatabaseRecords.str(baris.get("kode_kemenkes"));
        item.nama_obat = DatabaseRecords.str(baris.get("nama_obat"));
        item.jenis_obat = DatabaseRecords.str(baris.get("jenis_obat"));
        item.satuan = DatabaseRecords.str(baris.get("satuan"));
        item.cover_bpjs = DatabaseRecords.bool(baris.get("cover_bpjs"));
        item.stok_rs = (int) DatabaseRecords.lng(baris.get("stok_rs"));
        item.stok_minimum = (int) DatabaseRecords.lng(baris.get("stok_minimum"));
        item.harga = DatabaseRecords.dbl(baris.get("harga"));
        item.is_active = DatabaseRecords.bool(baris.get("is_active"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_obat", id_obat);
        baris.put("kode_kemenkes", kode_kemenkes);
        baris.put("nama_obat", nama_obat);
        baris.put("jenis_obat", jenis_obat);
        baris.put("satuan", satuan);
        baris.put("cover_bpjs", cover_bpjs);
        baris.put("stok_rs", stok_rs);
        baris.put("stok_minimum", stok_minimum);
        baris.put("harga", harga);
        baris.put("is_active", is_active);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Daftar implements Baris {
    long id_pendaftaran;
    long id_pasien;
    long id_jadwal;
    String tanggal_kunjungan;
    int nomor_antrean;
    String waktu_check_in;
    String estimasi_jam_masuk;
    String status_antrean;
    String catatan_pasien;
    String created_at;
    String updated_at;

    static Daftar dari(Map<String, Object> baris) {
        Daftar item = new Daftar();
        item.id_pendaftaran = DatabaseRecords.lng(baris.get("id_pendaftaran"));
        item.id_pasien = DatabaseRecords.lng(baris.get("id_pasien"));
        item.id_jadwal = DatabaseRecords.lng(baris.get("id_jadwal"));
        item.tanggal_kunjungan = DatabaseRecords.str(baris.get("tanggal_kunjungan"));
        item.nomor_antrean = (int) DatabaseRecords.lng(baris.get("nomor_antrean"));
        item.waktu_check_in = DatabaseRecords.str(baris.get("waktu_check_in"));
        item.estimasi_jam_masuk = DatabaseRecords.str(baris.get("estimasi_jam_masuk"));
        item.status_antrean = DatabaseRecords.str(baris.get("status_antrean"));
        item.catatan_pasien = DatabaseRecords.str(baris.get("catatan_pasien"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_pendaftaran", id_pendaftaran);
        baris.put("id_pasien", id_pasien);
        baris.put("id_jadwal", id_jadwal);
        baris.put("tanggal_kunjungan", tanggal_kunjungan);
        baris.put("nomor_antrean", nomor_antrean);
        baris.put("waktu_check_in", waktu_check_in);
        baris.put("estimasi_jam_masuk", estimasi_jam_masuk);
        baris.put("status_antrean", status_antrean);
        baris.put("catatan_pasien", catatan_pasien);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Periksa implements Baris {
    long id_pemeriksaan;
    long id_pendaftaran;
    long id_dokter;
    String keluhan;
    String tekanan_darah;
    Double suhu_tubuh;
    Double berat_badan;
    Double tinggi_badan;
    String diagnosis;
    String tindakan;
    String catatan_dokter;
    String waktu_mulai;
    String waktu_selesai;
    String created_at;
    String updated_at;

    static Periksa dari(Map<String, Object> baris) {
        Periksa item = new Periksa();
        item.id_pemeriksaan = DatabaseRecords.lng(baris.get("id_pemeriksaan"));
        item.id_pendaftaran = DatabaseRecords.lng(baris.get("id_pendaftaran"));
        item.id_dokter = DatabaseRecords.lng(baris.get("id_dokter"));
        item.keluhan = DatabaseRecords.str(baris.get("keluhan"));
        item.tekanan_darah = DatabaseRecords.str(baris.get("tekanan_darah"));
        item.suhu_tubuh = DatabaseRecords.dblOrNull(baris.get("suhu_tubuh"));
        item.berat_badan = DatabaseRecords.dblOrNull(baris.get("berat_badan"));
        item.tinggi_badan = DatabaseRecords.dblOrNull(baris.get("tinggi_badan"));
        item.diagnosis = teks(baris.get("diagnosis"));
        item.tindakan = teks(baris.get("tindakan"));
        item.catatan_dokter = DatabaseRecords.str(baris.get("catatan_dokter"));
        item.waktu_mulai = DatabaseRecords.str(baris.get("waktu_mulai"));
        item.waktu_selesai = DatabaseRecords.str(baris.get("waktu_selesai"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    private static String teks(Object nilai) {
        return nilai == null ? "" : String.valueOf(nilai);
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_pemeriksaan", id_pemeriksaan);
        baris.put("id_pendaftaran", id_pendaftaran);
        baris.put("id_dokter", id_dokter);
        baris.put("keluhan", keluhan == null ? "" : keluhan);
        baris.put("tekanan_darah", tekanan_darah);
        baris.put("suhu_tubuh", suhu_tubuh);
        baris.put("berat_badan", berat_badan);
        baris.put("tinggi_badan", tinggi_badan);
        baris.put("diagnosis", diagnosis == null ? "" : diagnosis);
        baris.put("tindakan", tindakan == null ? "" : tindakan);
        baris.put("catatan_dokter", catatan_dokter);
        baris.put("waktu_mulai", waktu_mulai);
        baris.put("waktu_selesai", waktu_selesai);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Resep implements Baris {
    long id_resep;
    long id_pendaftaran;
    String kode_qr_unik;
    String waktu_diterbitkan;
    String pilihan_penebusan;
    String estimasi_jam_selesai;
    String status_resep;
    String kadaluarsa_qr;
    String created_at;
    String updated_at;

    static Resep dari(Map<String, Object> baris) {
        Resep item = new Resep();
        item.id_resep = DatabaseRecords.lng(baris.get("id_resep"));
        item.id_pendaftaran = DatabaseRecords.lng(baris.get("id_pendaftaran"));
        item.kode_qr_unik = DatabaseRecords.str(baris.get("kode_qr_unik"));
        item.waktu_diterbitkan = DatabaseRecords.str(baris.get("waktu_diterbitkan"));
        item.pilihan_penebusan = DatabaseRecords.str(baris.get("pilihan_penebusan"));
        item.estimasi_jam_selesai = DatabaseRecords.str(baris.get("estimasi_jam_selesai"));
        item.status_resep = DatabaseRecords.str(baris.get("status_resep"));
        item.kadaluarsa_qr = DatabaseRecords.str(baris.get("kadaluarsa_qr"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_resep", id_resep);
        baris.put("id_pendaftaran", id_pendaftaran);
        baris.put("kode_qr_unik", kode_qr_unik);
        baris.put("waktu_diterbitkan", waktu_diterbitkan);
        baris.put("pilihan_penebusan", pilihan_penebusan);
        baris.put("estimasi_jam_selesai", estimasi_jam_selesai);
        baris.put("status_resep", status_resep);
        baris.put("kadaluarsa_qr", kadaluarsa_qr);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Detail implements Baris {
    long id_detail;
    long id_resep;
    long id_obat;
    int jumlah;
    String dosis_aturan_pakai;
    String instruksi_racikan;
    String catatan;
    String created_at;
    String updated_at;

    static Detail dari(Map<String, Object> baris) {
        Detail item = new Detail();
        item.id_detail = DatabaseRecords.lng(baris.get("id_detail"));
        item.id_resep = DatabaseRecords.lng(baris.get("id_resep"));
        item.id_obat = DatabaseRecords.lng(baris.get("id_obat"));
        item.jumlah = (int) DatabaseRecords.lng(baris.get("jumlah"));
        item.dosis_aturan_pakai = DatabaseRecords.str(baris.get("dosis_aturan_pakai"));
        item.instruksi_racikan = DatabaseRecords.str(baris.get("instruksi_racikan"));
        item.catatan = DatabaseRecords.str(baris.get("catatan"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_detail", id_detail);
        baris.put("id_resep", id_resep);
        baris.put("id_obat", id_obat);
        baris.put("jumlah", jumlah);
        baris.put("dosis_aturan_pakai", dosis_aturan_pakai);
        baris.put("instruksi_racikan", instruksi_racikan);
        baris.put("catatan", catatan);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Kabar implements Baris {
    long id_notifikasi;
    long id_pasien;
    Long id_pendaftaran;
    String jenis_notifikasi;
    String channel;
    String nomor_tujuan;
    String pesan;
    String status;
    String waktu_dikirim;
    String response_api;
    String created_at;

    static Kabar dari(Map<String, Object> baris) {
        Kabar item = new Kabar();
        item.id_notifikasi = DatabaseRecords.lng(baris.get("id_notifikasi"));
        item.id_pasien = DatabaseRecords.lng(baris.get("id_pasien"));
        item.id_pendaftaran = DatabaseRecords.lngOrNull(baris.get("id_pendaftaran"));
        item.jenis_notifikasi = DatabaseRecords.str(baris.get("jenis_notifikasi"));
        item.channel = DatabaseRecords.str(baris.get("channel"));
        item.nomor_tujuan = DatabaseRecords.str(baris.get("nomor_tujuan"));
        item.pesan = DatabaseRecords.str(baris.get("pesan"));
        item.status = DatabaseRecords.str(baris.get("status"));
        item.waktu_dikirim = DatabaseRecords.str(baris.get("waktu_dikirim"));
        item.response_api = DatabaseRecords.str(baris.get("response_api"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_notifikasi", id_notifikasi);
        baris.put("id_pasien", id_pasien);
        baris.put("id_pendaftaran", id_pendaftaran);
        baris.put("jenis_notifikasi", jenis_notifikasi);
        baris.put("channel", channel);
        baris.put("nomor_tujuan", nomor_tujuan);
        baris.put("pesan", pesan);
        baris.put("status", status);
        baris.put("waktu_dikirim", waktu_dikirim);
        baris.put("response_api", response_api);
        baris.put("created_at", created_at);
        return baris;
    }
}

final class Otp implements Baris {
    long id_otp;
    long id_user;
    String kode_otp;
    String tujuan;
    String expired_at;
    String verified_at;
    int attempt;
    String created_at;

    static Otp dari(Map<String, Object> baris) {
        Otp item = new Otp();
        item.id_otp = DatabaseRecords.lng(baris.get("id_otp"));
        item.id_user = DatabaseRecords.lng(baris.get("id_user"));
        item.kode_otp = DatabaseRecords.str(baris.get("kode_otp"));
        item.tujuan = DatabaseRecords.str(baris.get("tujuan"));
        item.expired_at = DatabaseRecords.str(baris.get("expired_at"));
        item.verified_at = DatabaseRecords.str(baris.get("verified_at"));
        item.attempt = (int) DatabaseRecords.lng(baris.get("attempt"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_otp", id_otp);
        baris.put("id_user", id_user);
        baris.put("kode_otp", kode_otp);
        baris.put("tujuan", tujuan);
        baris.put("expired_at", expired_at);
        baris.put("verified_at", verified_at);
        baris.put("attempt", attempt);
        baris.put("created_at", created_at);
        return baris;
    }
}

final class Ambil implements Baris {
    long id_pengambilan;
    long id_resep;
    long id_user_petugas;
    String diambil_oleh;
    String nama_pengambil;
    String waktu_pengambilan;
    String metode_verifikasi;
    String status;
    String catatan;
    String created_at;
    String updated_at;

    static Ambil dari(Map<String, Object> baris) {
        Ambil item = new Ambil();
        item.id_pengambilan = DatabaseRecords.lng(baris.get("id_pengambilan"));
        item.id_resep = DatabaseRecords.lng(baris.get("id_resep"));
        item.id_user_petugas = DatabaseRecords.lng(baris.get("id_user_petugas"));
        item.diambil_oleh = DatabaseRecords.str(baris.get("diambil_oleh"));
        item.nama_pengambil = DatabaseRecords.str(baris.get("nama_pengambil"));
        item.waktu_pengambilan = DatabaseRecords.str(baris.get("waktu_pengambilan"));
        item.metode_verifikasi = DatabaseRecords.str(baris.get("metode_verifikasi"));
        item.status = DatabaseRecords.str(baris.get("status"));
        item.catatan = DatabaseRecords.str(baris.get("catatan"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        item.updated_at = DatabaseRecords.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_pengambilan", id_pengambilan);
        baris.put("id_resep", id_resep);
        baris.put("id_user_petugas", id_user_petugas);
        baris.put("diambil_oleh", diambil_oleh);
        baris.put("nama_pengambil", nama_pengambil);
        baris.put("waktu_pengambilan", waktu_pengambilan);
        baris.put("metode_verifikasi", metode_verifikasi);
        baris.put("status", status);
        baris.put("catatan", catatan);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Mutasi implements Baris {
    long id_mutasi;
    long id_obat;
    long id_user;
    String jenis_mutasi;
    int jumlah;
    int stok_sebelum;
    int stok_sesudah;
    String keterangan;
    String created_at;

    static Mutasi dari(Map<String, Object> baris) {
        Mutasi item = new Mutasi();
        item.id_mutasi = DatabaseRecords.lng(baris.get("id_mutasi"));
        item.id_obat = DatabaseRecords.lng(baris.get("id_obat"));
        item.id_user = DatabaseRecords.lng(baris.get("id_user"));
        item.jenis_mutasi = DatabaseRecords.str(baris.get("jenis_mutasi"));
        item.jumlah = (int) DatabaseRecords.lng(baris.get("jumlah"));
        item.stok_sebelum = (int) DatabaseRecords.lng(baris.get("stok_sebelum"));
        item.stok_sesudah = (int) DatabaseRecords.lng(baris.get("stok_sesudah"));
        item.keterangan = DatabaseRecords.str(baris.get("keterangan"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_mutasi", id_mutasi);
        baris.put("id_obat", id_obat);
        baris.put("id_user", id_user);
        baris.put("jenis_mutasi", jenis_mutasi);
        baris.put("jumlah", jumlah);
        baris.put("stok_sebelum", stok_sebelum);
        baris.put("stok_sesudah", stok_sesudah);
        baris.put("keterangan", keterangan);
        baris.put("created_at", created_at);
        return baris;
    }
}

final class Jejak implements Baris {
    long id_log;
    Long id_user;
    String aktivitas;
    String tabel_referensi;
    Long id_referensi;
    String keterangan;
    String ip_address;
    String created_at;

    static Jejak dari(Map<String, Object> baris) {
        Jejak item = new Jejak();
        item.id_log = DatabaseRecords.lng(baris.get("id_log"));
        item.id_user = DatabaseRecords.lngOrNull(baris.get("id_user"));
        item.aktivitas = DatabaseRecords.str(baris.get("aktivitas"));
        item.tabel_referensi = DatabaseRecords.str(baris.get("tabel_referensi"));
        item.id_referensi = DatabaseRecords.lngOrNull(baris.get("id_referensi"));
        item.keterangan = DatabaseRecords.str(baris.get("keterangan"));
        item.ip_address = DatabaseRecords.str(baris.get("ip_address"));
        item.created_at = DatabaseRecords.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = DatabaseRecords.baris();
        baris.put("id_log", id_log);
        baris.put("id_user", id_user);
        baris.put("aktivitas", aktivitas);
        baris.put("tabel_referensi", tabel_referensi);
        baris.put("id_referensi", id_referensi);
        baris.put("keterangan", keterangan);
        baris.put("ip_address", ip_address);
        baris.put("created_at", created_at);
        return baris;
    }
}