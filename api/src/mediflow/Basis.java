package mediflow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Salinan tabel medicflow_db yang diubah aplikasi. */
final class Basis {
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

    static Basis dari(Map<String, List<Map<String, Object>>> data) {
        Basis basis = new Basis();
        for (Map<String, Object> baris : daftar(data, "users")) {
            basis.users.add(Pengguna.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "pasien")) {
            basis.pasien.add(Pasien.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "dokter")) {
            basis.dokter.add(Dokter.dari(baris));
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
            basis.otp_verifikasi.add(Otp.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "pengambilan_obat")) {
            basis.pengambilan_obat.add(Ambil.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "mutasi_stok")) {
            basis.mutasi_stok.add(Mutasi.dari(baris));
        }
        for (Map<String, Object> baris : daftar(data, "log_aktivitas")) {
            basis.log_aktivitas.add(Jejak.dari(baris));
        }
        return basis;
    }

    Map<String, Object> kePeta() {
        Map<String, Object> hasil = new LinkedHashMap<>();
        hasil.put("users", peta(users));
        hasil.put("poliklinik", peta(poliklinik));
        hasil.put("obat", peta(obat));
        hasil.put("pasien", peta(pasien));
        hasil.put("dokter", peta(dokter));
        hasil.put("jadwal_dokter", peta(jadwal_dokter));
        hasil.put("pendaftaran_poli", peta(pendaftaran_poli));
        hasil.put("pemeriksaan", peta(pemeriksaan));
        hasil.put("resep", peta(resep));
        hasil.put("detail_resep", peta(detail_resep));
        hasil.put("notifikasi", peta(notifikasi));
        hasil.put("otp_verifikasi", peta(otp_verifikasi));
        hasil.put("pengambilan_obat", peta(pengambilan_obat));
        hasil.put("mutasi_stok", peta(mutasi_stok));
        hasil.put("log_aktivitas", peta(log_aktivitas));
        return hasil;
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
    String password_hash;
    String role;
    String nama_lengkap;
    String no_telepon;
    boolean is_active;
    String created_at;
    String updated_at;

    static Pengguna dari(Map<String, Object> baris) {
        Pengguna item = new Pengguna();
        item.id_user = Basis.lng(baris.get("id_user"));
        item.username = Basis.str(baris.get("username"));
        item.password_hash = Basis.str(baris.get("password_hash"));
        item.role = Basis.str(baris.get("role"));
        item.nama_lengkap = Basis.str(baris.get("nama_lengkap"));
        item.no_telepon = Basis.str(baris.get("no_telepon"));
        item.is_active = Basis.bool(baris.get("is_active"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
        baris.put("id_user", id_user);
        baris.put("username", username);
        baris.put("password_hash", password_hash);
        baris.put("role", role);
        baris.put("nama_lengkap", nama_lengkap);
        baris.put("no_telepon", no_telepon);
        baris.put("is_active", is_active);
        baris.put("created_at", created_at);
        baris.put("updated_at", updated_at);
        return baris;
    }
}

final class Pasien implements Baris {
    long id_pasien;
    long id_user;
    String nik;
    String nomor_bpjs;
    String nama_lengkap;
    String tanggal_lahir;
    String jenis_kelamin;
    String alamat;
    String no_telepon;
    boolean terdaftar_satusehat;
    String created_at;
    String updated_at;

    static Pasien dari(Map<String, Object> baris) {
        Pasien item = new Pasien();
        item.id_pasien = Basis.lng(baris.get("id_pasien"));
        item.id_user = Basis.lng(baris.get("id_user"));
        item.nik = Basis.str(baris.get("nik"));
        item.nomor_bpjs = Basis.str(baris.get("nomor_bpjs"));
        item.nama_lengkap = Basis.str(baris.get("nama_lengkap"));
        item.tanggal_lahir = Basis.str(baris.get("tanggal_lahir"));
        item.jenis_kelamin = Basis.str(baris.get("jenis_kelamin"));
        item.alamat = Basis.str(baris.get("alamat"));
        item.no_telepon = Basis.str(baris.get("no_telepon"));
        item.terdaftar_satusehat = Basis.bool(baris.get("terdaftar_satusehat"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
        baris.put("id_pasien", id_pasien);
        baris.put("id_user", id_user);
        baris.put("nik", nik);
        baris.put("nomor_bpjs", nomor_bpjs);
        baris.put("nama_lengkap", nama_lengkap);
        baris.put("tanggal_lahir", tanggal_lahir);
        baris.put("jenis_kelamin", jenis_kelamin);
        baris.put("alamat", alamat);
        baris.put("no_telepon", no_telepon);
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
        item.id_dokter = Basis.lng(baris.get("id_dokter"));
        item.id_user = Basis.lng(baris.get("id_user"));
        item.id_poli = Basis.lng(baris.get("id_poli"));
        item.nama_dokter = Basis.str(baris.get("nama_dokter"));
        item.spesialisasi = Basis.str(baris.get("spesialisasi"));
        item.rata_waktu_periksa_menit = (int) Basis.lng(baris.get("rata_waktu_periksa_menit"));
        item.is_active = Basis.bool(baris.get("is_active"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_poli = Basis.lng(baris.get("id_poli"));
        item.nama_poli = Basis.str(baris.get("nama_poli"));
        item.deskripsi = Basis.str(baris.get("deskripsi"));
        item.is_active = Basis.bool(baris.get("is_active"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_jadwal = Basis.lng(baris.get("id_jadwal"));
        item.id_dokter = Basis.lng(baris.get("id_dokter"));
        item.hari = Basis.str(baris.get("hari"));
        item.jam_mulai = Basis.str(baris.get("jam_mulai"));
        item.jam_selesai = Basis.str(baris.get("jam_selesai"));
        item.kuota_maksimal = (int) Basis.lng(baris.get("kuota_maksimal"));
        item.is_active = Basis.bool(baris.get("is_active"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_obat = Basis.lng(baris.get("id_obat"));
        item.kode_kemenkes = Basis.str(baris.get("kode_kemenkes"));
        item.nama_obat = Basis.str(baris.get("nama_obat"));
        item.jenis_obat = Basis.str(baris.get("jenis_obat"));
        item.satuan = Basis.str(baris.get("satuan"));
        item.cover_bpjs = Basis.bool(baris.get("cover_bpjs"));
        item.stok_rs = (int) Basis.lng(baris.get("stok_rs"));
        item.stok_minimum = (int) Basis.lng(baris.get("stok_minimum"));
        item.harga = Basis.dbl(baris.get("harga"));
        item.is_active = Basis.bool(baris.get("is_active"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_pendaftaran = Basis.lng(baris.get("id_pendaftaran"));
        item.id_pasien = Basis.lng(baris.get("id_pasien"));
        item.id_jadwal = Basis.lng(baris.get("id_jadwal"));
        item.tanggal_kunjungan = Basis.str(baris.get("tanggal_kunjungan"));
        item.nomor_antrean = (int) Basis.lng(baris.get("nomor_antrean"));
        item.waktu_check_in = Basis.str(baris.get("waktu_check_in"));
        item.estimasi_jam_masuk = Basis.str(baris.get("estimasi_jam_masuk"));
        item.status_antrean = Basis.str(baris.get("status_antrean"));
        item.catatan_pasien = Basis.str(baris.get("catatan_pasien"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_pemeriksaan = Basis.lng(baris.get("id_pemeriksaan"));
        item.id_pendaftaran = Basis.lng(baris.get("id_pendaftaran"));
        item.id_dokter = Basis.lng(baris.get("id_dokter"));
        item.keluhan = Basis.str(baris.get("keluhan"));
        item.tekanan_darah = Basis.str(baris.get("tekanan_darah"));
        item.suhu_tubuh = Basis.dblOrNull(baris.get("suhu_tubuh"));
        item.berat_badan = Basis.dblOrNull(baris.get("berat_badan"));
        item.tinggi_badan = Basis.dblOrNull(baris.get("tinggi_badan"));
        item.diagnosis = teks(baris.get("diagnosis"));
        item.tindakan = teks(baris.get("tindakan"));
        item.catatan_dokter = Basis.str(baris.get("catatan_dokter"));
        item.waktu_mulai = Basis.str(baris.get("waktu_mulai"));
        item.waktu_selesai = Basis.str(baris.get("waktu_selesai"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    private static String teks(Object nilai) {
        return nilai == null ? "" : String.valueOf(nilai);
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_resep = Basis.lng(baris.get("id_resep"));
        item.id_pendaftaran = Basis.lng(baris.get("id_pendaftaran"));
        item.kode_qr_unik = Basis.str(baris.get("kode_qr_unik"));
        item.waktu_diterbitkan = Basis.str(baris.get("waktu_diterbitkan"));
        item.pilihan_penebusan = Basis.str(baris.get("pilihan_penebusan"));
        item.estimasi_jam_selesai = Basis.str(baris.get("estimasi_jam_selesai"));
        item.status_resep = Basis.str(baris.get("status_resep"));
        item.kadaluarsa_qr = Basis.str(baris.get("kadaluarsa_qr"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_detail = Basis.lng(baris.get("id_detail"));
        item.id_resep = Basis.lng(baris.get("id_resep"));
        item.id_obat = Basis.lng(baris.get("id_obat"));
        item.jumlah = (int) Basis.lng(baris.get("jumlah"));
        item.dosis_aturan_pakai = Basis.str(baris.get("dosis_aturan_pakai"));
        item.instruksi_racikan = Basis.str(baris.get("instruksi_racikan"));
        item.catatan = Basis.str(baris.get("catatan"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_notifikasi = Basis.lng(baris.get("id_notifikasi"));
        item.id_pasien = Basis.lng(baris.get("id_pasien"));
        item.id_pendaftaran = Basis.lngOrNull(baris.get("id_pendaftaran"));
        item.jenis_notifikasi = Basis.str(baris.get("jenis_notifikasi"));
        item.channel = Basis.str(baris.get("channel"));
        item.nomor_tujuan = Basis.str(baris.get("nomor_tujuan"));
        item.pesan = Basis.str(baris.get("pesan"));
        item.status = Basis.str(baris.get("status"));
        item.waktu_dikirim = Basis.str(baris.get("waktu_dikirim"));
        item.response_api = Basis.str(baris.get("response_api"));
        item.created_at = Basis.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_otp = Basis.lng(baris.get("id_otp"));
        item.id_user = Basis.lng(baris.get("id_user"));
        item.kode_otp = Basis.str(baris.get("kode_otp"));
        item.tujuan = Basis.str(baris.get("tujuan"));
        item.expired_at = Basis.str(baris.get("expired_at"));
        item.verified_at = Basis.str(baris.get("verified_at"));
        item.attempt = (int) Basis.lng(baris.get("attempt"));
        item.created_at = Basis.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_pengambilan = Basis.lng(baris.get("id_pengambilan"));
        item.id_resep = Basis.lng(baris.get("id_resep"));
        item.id_user_petugas = Basis.lng(baris.get("id_user_petugas"));
        item.diambil_oleh = Basis.str(baris.get("diambil_oleh"));
        item.nama_pengambil = Basis.str(baris.get("nama_pengambil"));
        item.waktu_pengambilan = Basis.str(baris.get("waktu_pengambilan"));
        item.metode_verifikasi = Basis.str(baris.get("metode_verifikasi"));
        item.status = Basis.str(baris.get("status"));
        item.catatan = Basis.str(baris.get("catatan"));
        item.created_at = Basis.str(baris.get("created_at"));
        item.updated_at = Basis.str(baris.get("updated_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_mutasi = Basis.lng(baris.get("id_mutasi"));
        item.id_obat = Basis.lng(baris.get("id_obat"));
        item.id_user = Basis.lng(baris.get("id_user"));
        item.jenis_mutasi = Basis.str(baris.get("jenis_mutasi"));
        item.jumlah = (int) Basis.lng(baris.get("jumlah"));
        item.stok_sebelum = (int) Basis.lng(baris.get("stok_sebelum"));
        item.stok_sesudah = (int) Basis.lng(baris.get("stok_sesudah"));
        item.keterangan = Basis.str(baris.get("keterangan"));
        item.created_at = Basis.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
        item.id_log = Basis.lng(baris.get("id_log"));
        item.id_user = Basis.lngOrNull(baris.get("id_user"));
        item.aktivitas = Basis.str(baris.get("aktivitas"));
        item.tabel_referensi = Basis.str(baris.get("tabel_referensi"));
        item.id_referensi = Basis.lngOrNull(baris.get("id_referensi"));
        item.keterangan = Basis.str(baris.get("keterangan"));
        item.ip_address = Basis.str(baris.get("ip_address"));
        item.created_at = Basis.str(baris.get("created_at"));
        return item;
    }

    @Override
    public Map<String, Object> peta() {
        Map<String, Object> baris = Basis.baris();
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
