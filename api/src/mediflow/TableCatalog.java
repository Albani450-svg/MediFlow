package mediflow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bentuk tabel medicflow_db dan aturan validasi CRUD. */
final class TableCatalog {
    enum Type { INT, BOOL, DEC, STR, DT, DATE, TIME }

    record Col(String name, Type type, boolean nullable) {}

    record Field(String name, String rules, Map<String, String> messages) {}

    record Table(
            String name,
            String label,
            String pk,
            List<Col> columns,
            List<String> writable,
            List<String> required,
            Set<String> allowEmpty,
            Set<String> nullable,
            Set<String> booleans,
            Set<String> integers,
            Set<String> decimals,
            Set<String> dates,
            Set<String> datetimes,
            Set<String> times,
            List<String> filters,
            List<Field> fields,
            String check
    ) {
        Col kolom(String nama) {
            for (Col kolom : columns) {
                if (kolom.name().equals(nama)) {
                    return kolom;
                }
            }
            return null;
        }
    }

    static final List<Table> TABLES;
    static final Map<String, Table> BY_NAME;

    static {
        List<Table> daftar = List.of(
                akun(),
                admin(),
                farmasi(),
                poliklinik(),
                obat(),
                pasien(),
                dokter(),
                jadwal(),
                pendaftaran(),
                pemeriksaan(),
                resep(),
                detailResep(),
                notifikasi(),
                otp(),
                pengambilan(),
                mutasi(),
                log()
        );
        TABLES = List.copyOf(daftar);
        Map<String, Table> peta = new LinkedHashMap<>();
        for (Table tabel : daftar) {
            peta.put(tabel.name(), tabel);
        }
        BY_NAME = Map.copyOf(peta);
    }

    private TableCatalog() {}

    private static Table akun() {
        return bangun("accounts", "akun", "account_id", stempel(
                i("account_id"), s("username"), sz("email"), s("password_hash"), b("is_active")
        ), List.of("username", "email", "password_hash", "is_active"),
                List.of("username", "password_hash"),
                Set.of(),
                List.of("is_active", "username"),
                List.of(
                        field("username", "required|min_length[3]|max_length[50]|is_unique[accounts.username,account_id,{account_id}]", "is_unique", "Nama pengguna sudah terdaftar."),
                        field("email", "permit_empty|max_length[100]|is_unique[accounts.email,account_id,{account_id}]", "is_unique", "Email sudah terdaftar."),
                        field("password_hash", "required|max_length[255]"),
                        field("is_active", "if_exist|in_list[0,1]")
                ), null);
    }

    private static Table admin() {
        return bangun("admins", "admin", "id_admin", stempel(
                i("id_admin"), i("account_id"), s("nama"), sz("phone"), b("is_super")
        ), List.of("account_id", "nama", "phone", "is_super"),
                List.of("account_id", "nama"),
                Set.of(),
                List.of("account_id", "is_super"),
                List.of(
                        field("account_id", "required|is_natural_no_zero|is_not_unique[accounts.account_id]|is_unique[admins.account_id,id_admin,{id_admin}]",
                                "is_not_unique", "Akun tidak ditemukan.", "is_unique", "Akun ini sudah terhubung ke admin lain."),
                        field("nama", "required|max_length[150]"),
                        field("phone", "permit_empty|max_length[20]|regex_match[/^08[0-9]{8,12}$/]", "regex_match", "Nomor telepon harus diawali 08."),
                        field("is_super", "if_exist|in_list[0,1]")
                ), null);
    }

    private static Table farmasi() {
        return bangun("farmasi_staff", "petugas farmasi", "id_farmasi", stempel(
                i("id_farmasi"), i("account_id"), s("nama"), sz("phone")
        ), List.of("account_id", "nama", "phone"),
                List.of("account_id", "nama"),
                Set.of(),
                List.of("account_id"),
                List.of(
                        field("account_id", "required|is_natural_no_zero|is_not_unique[accounts.account_id]|is_unique[farmasi_staff.account_id,id_farmasi,{id_farmasi}]",
                                "is_not_unique", "Akun tidak ditemukan.", "is_unique", "Akun ini sudah terhubung ke petugas farmasi lain."),
                        field("nama", "required|max_length[150]"),
                        field("phone", "permit_empty|max_length[20]|regex_match[/^08[0-9]{8,12}$/]", "regex_match", "Nomor telepon harus diawali 08.")
                ), null);
    }

    private static Table poliklinik() {
        return bangun("poliklinik", "poliklinik", "id_poli", stempel(
                i("id_poli"), s("nama_poli"), sz("deskripsi"), b("is_active")
        ), List.of("nama_poli", "deskripsi", "is_active"),
                List.of("nama_poli"), Set.of(),
                List.of("is_active", "nama_poli"),
                List.of(
                        field("nama_poli", "required|max_length[100]|is_unique[poliklinik.nama_poli,id_poli,{id_poli}]", "is_unique", "Nama poliklinik sudah terdaftar."),
                        field("deskripsi", "permit_empty"),
                        field("is_active", "if_exist|in_list[0,1]")
                ), null);
    }

    private static Table obat() {
        return bangun("obat", "obat", "id_obat", stempel(
                i("id_obat"), s("kode_kemenkes"), s("nama_obat"), s("jenis_obat"), s("satuan"),
                b("cover_bpjs"), i("stok_rs"), i("stok_minimum"), d("harga"), b("is_active")
        ), List.of("kode_kemenkes", "nama_obat", "jenis_obat", "satuan", "cover_bpjs", "stok_rs", "stok_minimum", "harga", "is_active"),
                List.of("kode_kemenkes", "nama_obat", "satuan"), Set.of(),
                List.of("jenis_obat", "is_active", "kode_kemenkes"),
                List.of(
                        field("kode_kemenkes", "required|max_length[50]|is_unique[obat.kode_kemenkes,id_obat,{id_obat}]", "is_unique", "Kode Kemenkes sudah terdaftar."),
                        field("nama_obat", "required|max_length[150]"),
                        field("jenis_obat", "if_exist|in_list[Jadi,Racikan]"),
                        field("satuan", "required|max_length[30]"),
                        field("cover_bpjs", "if_exist|in_list[0,1]"),
                        field("stok_rs", "if_exist|integer|greater_than_equal_to[0]"),
                        field("stok_minimum", "if_exist|integer|greater_than_equal_to[0]"),
                        field("harga", "if_exist|numeric|greater_than_equal_to[0]"),
                        field("is_active", "if_exist|in_list[0,1]")
                ), null);
    }

    private static Table pasien() {
        return bangun("patients", "pasien", "id_pasien", stempel(
                i("id_pasien"), i("account_id"), sz("no_rekam_medis"), s("nik"), s("nama_lengkap"),
                date("tanggal_lahir"), s("jenis_kelamin"), sz("alamat"), s("no_telepon"), b("terdaftar_satusehat")
        ), List.of("account_id", "nik", "nama_lengkap", "tanggal_lahir", "jenis_kelamin", "alamat", "no_telepon", "terdaftar_satusehat"),
                List.of("account_id", "nik", "nama_lengkap", "tanggal_lahir", "jenis_kelamin", "no_telepon"), Set.of(),
                List.of("account_id", "nik", "jenis_kelamin"),
                List.of(
                        field("account_id", "required|is_natural_no_zero|is_not_unique[accounts.account_id]|is_unique[patients.account_id,id_pasien,{id_pasien}]",
                                "is_not_unique", "Akun tidak ditemukan.", "is_unique", "Akun ini sudah terhubung ke pasien lain."),
                        field("nik", "required|max_length[16]|is_unique[patients.nik,id_pasien,{id_pasien}]", "is_unique", "NIK sudah terdaftar."),
                        field("nama_lengkap", "required|max_length[150]"),
                        field("tanggal_lahir", "required|valid_date[Y-m-d]"),
                        field("jenis_kelamin", "required|in_list[L,P]", "in_list", "Jenis kelamin harus L atau P."),
                        field("alamat", "permit_empty"),
                        field("no_telepon", "required|max_length[20]|regex_match[/^08[0-9]{8,12}$/]", "regex_match", "Nomor telepon harus diawali 08."),
                        field("terdaftar_satusehat", "if_exist|in_list[0,1]")
                ), null);
    }

    private static Table dokter() {
        return bangun("dokter", "dokter", "id_dokter", stempel(
                i("id_dokter"), i("account_id"), i("id_poli"), s("nama_dokter"), sz("spesialisasi"), i("rata_waktu_periksa_menit"), b("is_active")
        ), List.of("account_id", "id_poli", "nama_dokter", "spesialisasi", "rata_waktu_periksa_menit", "is_active"),
                List.of("account_id", "id_poli", "nama_dokter"), Set.of(),
                List.of("account_id", "id_poli", "is_active"),
                List.of(
                        field("account_id", "required|is_natural_no_zero|is_not_unique[accounts.account_id]|is_unique[dokter.account_id,id_dokter,{id_dokter}]",
                                "is_not_unique", "Akun tidak ditemukan.", "is_unique", "Akun ini sudah terhubung ke dokter lain."),
                        field("id_poli", "required|is_natural_no_zero|is_not_unique[poliklinik.id_poli]", "is_not_unique", "Poliklinik tidak ditemukan."),
                        field("nama_dokter", "required|max_length[150]"),
                        field("spesialisasi", "permit_empty|max_length[100]"),
                        field("rata_waktu_periksa_menit", "if_exist|integer|greater_than[0]|less_than_equal_to[180]"),
                        field("is_active", "if_exist|in_list[0,1]")
                ), null);
    }

    private static Table jadwal() {
        return bangun("jadwal_dokter", "jadwal dokter", "id_jadwal", stempel(
                i("id_jadwal"), i("id_dokter"), s("hari"), time("jam_mulai"), time("jam_selesai"), i("kuota_maksimal"), b("is_active")
        ), List.of("id_dokter", "hari", "jam_mulai", "jam_selesai", "kuota_maksimal", "is_active"),
                List.of("id_dokter", "hari", "jam_mulai", "jam_selesai"), Set.of(),
                List.of("id_dokter", "hari", "is_active"),
                List.of(
                        field("id_dokter", "required|is_natural_no_zero|is_not_unique[dokter.id_dokter]", "is_not_unique", "Dokter tidak ditemukan."),
                        field("hari", "required|in_list[Minggu,Senin,Selasa,Rabu,Kamis,Jumat,Sabtu]"),
                        field("jam_mulai", "required|regex_match[/^\\d{2}:\\d{2}:\\d{2}$/]", "regex_match", "Jam mulai tidak valid."),
                        field("jam_selesai", "required|regex_match[/^\\d{2}:\\d{2}:\\d{2}$/]", "regex_match", "Jam selesai tidak valid."),
                        field("kuota_maksimal", "if_exist|integer|greater_than[0]|less_than_equal_to[200]"),
                        field("is_active", "if_exist|in_list[0,1]")
                ), "jadwal");
    }

    private static Table pendaftaran() {
        return bangun("pendaftaran_poli", "pendaftaran", "id_pendaftaran", stempel(
                i("id_pendaftaran"), i("id_pasien"), i("id_jadwal"), date("tanggal_kunjungan"), i("nomor_antrean"),
                tz("waktu_check_in"), tz("estimasi_jam_masuk"), s("status_antrean"), sz("catatan_pasien")
        ), List.of("id_pasien", "id_jadwal", "tanggal_kunjungan", "nomor_antrean", "waktu_check_in", "estimasi_jam_masuk", "status_antrean", "catatan_pasien"),
                List.of("id_pasien", "id_jadwal", "tanggal_kunjungan", "nomor_antrean"), Set.of(),
                List.of("id_pasien", "id_jadwal", "tanggal_kunjungan", "status_antrean"),
                List.of(
                        field("id_pasien", "required|is_natural_no_zero|is_not_unique[patients.id_pasien]", "is_not_unique", "Pasien tidak ditemukan."),
                        field("id_jadwal", "required|is_natural_no_zero|is_not_unique[jadwal_dokter.id_jadwal]", "is_not_unique", "Jadwal dokter tidak ditemukan."),
                        field("tanggal_kunjungan", "required|valid_date[Y-m-d]"),
                        field("nomor_antrean", "required|integer|greater_than[0]"),
                        field("waktu_check_in", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("estimasi_jam_masuk", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("status_antrean", "if_exist|in_list[Menunggu,Dipanggil,Masuk Ruangan,Selesai Pemeriksaan,Batal]"),
                        field("catatan_pasien", "permit_empty")
                ), "pendaftaran");
    }

    private static Table pemeriksaan() {
        return bangun("pemeriksaan", "pemeriksaan", "id_pemeriksaan", stempel(
                i("id_pemeriksaan"), i("id_pendaftaran"), i("id_dokter"), s("keluhan"), sz("tekanan_darah"),
                dz("suhu_tubuh"), dz("berat_badan"), dz("tinggi_badan"), s("diagnosis"), s("tindakan"),
                sz("catatan_dokter"), t("waktu_mulai"), tz("waktu_selesai")
        ), List.of("id_pendaftaran", "id_dokter", "keluhan", "tekanan_darah", "suhu_tubuh", "berat_badan", "tinggi_badan", "diagnosis", "tindakan", "catatan_dokter", "waktu_mulai", "waktu_selesai"),
                List.of("id_pendaftaran", "id_dokter", "keluhan", "diagnosis", "tindakan"),
                Set.of("diagnosis", "tindakan"),
                List.of("id_pendaftaran", "id_dokter"),
                List.of(
                        field("id_pendaftaran", "required|is_natural_no_zero|is_not_unique[pendaftaran_poli.id_pendaftaran]|is_unique[pemeriksaan.id_pendaftaran,id_pemeriksaan,{id_pemeriksaan}]",
                                "is_not_unique", "Pendaftaran tidak ditemukan.", "is_unique", "Pendaftaran ini sudah punya pemeriksaan."),
                        field("id_dokter", "required|is_natural_no_zero|is_not_unique[dokter.id_dokter]", "is_not_unique", "Dokter tidak ditemukan."),
                        field("keluhan", "required"),
                        field("tekanan_darah", "permit_empty|max_length[20]"),
                        field("suhu_tubuh", "permit_empty|numeric"),
                        field("berat_badan", "permit_empty|numeric"),
                        field("tinggi_badan", "permit_empty|numeric"),
                        field("diagnosis", "permit_empty"),
                        field("tindakan", "permit_empty"),
                        field("catatan_dokter", "permit_empty"),
                        field("waktu_mulai", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("waktu_selesai", "permit_empty|valid_date[Y-m-d H:i:s]")
                ), null);
    }

    private static Table resep() {
        return bangun("resep", "resep", "id_resep", stempel(
                i("id_resep"), i("id_pendaftaran"), s("kode_qr_unik"), t("waktu_diterbitkan"), s("pilihan_penebusan"),
                tz("estimasi_jam_selesai"), s("status_resep"), tz("kadaluarsa_qr")
        ), List.of("id_pendaftaran", "kode_qr_unik", "waktu_diterbitkan", "pilihan_penebusan", "estimasi_jam_selesai", "status_resep", "kadaluarsa_qr"),
                List.of("id_pendaftaran", "kode_qr_unik"), Set.of(),
                List.of("id_pendaftaran", "status_resep", "pilihan_penebusan", "kode_qr_unik"),
                List.of(
                        field("id_pendaftaran", "required|is_natural_no_zero|is_not_unique[pendaftaran_poli.id_pendaftaran]|is_unique[resep.id_pendaftaran,id_resep,{id_resep}]",
                                "is_not_unique", "Pendaftaran tidak ditemukan.", "is_unique", "Pendaftaran ini sudah punya resep."),
                        field("kode_qr_unik", "required|max_length[100]|is_unique[resep.kode_qr_unik,id_resep,{id_resep}]", "is_unique", "Kode QR sudah dipakai."),
                        field("waktu_diterbitkan", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("pilihan_penebusan", "if_exist|in_list[Belum Memilih,Apotek RS,Apotek Luar]"),
                        field("estimasi_jam_selesai", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("status_resep", "if_exist|in_list[Menunggu Pilihan,Antrean Farmasi,Sedang Diracik,Siap Diambil,Selesai Diambil,Dibatalkan]"),
                        field("kadaluarsa_qr", "permit_empty|valid_date[Y-m-d H:i:s]")
                ), null);
    }

    private static Table detailResep() {
        return bangun("detail_resep", "detail resep", "id_detail", stempel(
                i("id_detail"), i("id_resep"), i("id_obat"), i("jumlah"), s("dosis_aturan_pakai"), sz("instruksi_racikan"), sz("catatan")
        ), List.of("id_resep", "id_obat", "jumlah", "dosis_aturan_pakai", "instruksi_racikan", "catatan"),
                List.of("id_resep", "id_obat", "jumlah", "dosis_aturan_pakai"), Set.of(),
                List.of("id_resep", "id_obat"),
                List.of(
                        field("id_resep", "required|is_natural_no_zero|is_not_unique[resep.id_resep]", "is_not_unique", "Resep tidak ditemukan."),
                        field("id_obat", "required|is_natural_no_zero|is_not_unique[obat.id_obat]", "is_not_unique", "Obat tidak ditemukan."),
                        field("jumlah", "required|integer|greater_than[0]"),
                        field("dosis_aturan_pakai", "required|max_length[255]"),
                        field("instruksi_racikan", "permit_empty"),
                        field("catatan", "permit_empty")
                ), null);
    }

    private static Table notifikasi() {
        return bangun("notifikasi", "notifikasi", "id_notifikasi", dibuat(
                i("id_notifikasi"), i("id_pasien"), iz("id_pendaftaran"), s("jenis_notifikasi"), s("channel"),
                s("nomor_tujuan"), s("pesan"), s("status"), tz("waktu_dikirim"), sz("response_api")
        ), List.of("id_pasien", "id_pendaftaran", "jenis_notifikasi", "channel", "nomor_tujuan", "pesan", "status", "waktu_dikirim", "response_api"),
                List.of("id_pasien", "jenis_notifikasi", "nomor_tujuan", "pesan"), Set.of(),
                List.of("id_pasien", "id_pendaftaran", "jenis_notifikasi", "channel", "status"),
                List.of(
                        field("id_pasien", "required|is_natural_no_zero|is_not_unique[patients.id_pasien]", "is_not_unique", "Pasien tidak ditemukan."),
                        field("id_pendaftaran", "permit_empty|is_natural_no_zero|is_not_unique[pendaftaran_poli.id_pendaftaran]", "is_not_unique", "Pendaftaran tidak ditemukan."),
                        field("jenis_notifikasi", "required|in_list[Verifikasi WA,Konfirmasi Pendaftaran,Pengingat Kunjungan,QR Resep,Obat Siap]"),
                        field("channel", "if_exist|in_list[WhatsApp,PWA]"),
                        field("nomor_tujuan", "required|max_length[20]|regex_match[/^08[0-9]{8,12}$/]", "regex_match", "Nomor tujuan harus diawali 08."),
                        field("pesan", "required"),
                        field("status", "if_exist|in_list[Pending,Terkirim,Gagal]"),
                        field("waktu_dikirim", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("response_api", "permit_empty")
                ), "notifikasi");
    }

    private static Table otp() {
        return bangun("otp_verifikasi", "OTP", "id_otp", dibuat(
                i("id_otp"), i("account_id"), s("kode_otp"), s("tujuan"), t("expired_at"), tz("verified_at"), i("attempt")
        ), List.of("account_id", "kode_otp", "tujuan", "expired_at", "verified_at", "attempt"),
                List.of("account_id", "kode_otp", "tujuan", "expired_at"), Set.of(),
                List.of("account_id"),
                List.of(
                        field("account_id", "required|is_natural_no_zero|is_not_unique[accounts.account_id]", "is_not_unique", "Akun tidak ditemukan."),
                        field("kode_otp", "required|max_length[64]"),
                        field("tujuan", "required|max_length[20]"),
                        field("expired_at", "required|valid_date[Y-m-d H:i:s]"),
                        field("verified_at", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("attempt", "if_exist|integer|greater_than_equal_to[0]")
                ), null);
    }

    private static Table pengambilan() {
        return bangun("pengambilan_obat", "pengambilan obat", "id_pengambilan", stempel(
                i("id_pengambilan"), i("id_resep"), i("account_petugas"), s("diambil_oleh"), sz("nama_pengambil"),
                t("waktu_pengambilan"), s("metode_verifikasi"), s("status"), sz("catatan")
        ), List.of("id_resep", "account_petugas", "diambil_oleh", "nama_pengambil", "waktu_pengambilan", "metode_verifikasi", "status", "catatan"),
                List.of("id_resep", "account_petugas", "diambil_oleh", "metode_verifikasi"), Set.of(),
                List.of("id_resep", "account_petugas", "status", "diambil_oleh"),
                List.of(
                        field("id_resep", "required|is_natural_no_zero|is_not_unique[resep.id_resep]|is_unique[pengambilan_obat.id_resep,id_pengambilan,{id_pengambilan}]",
                                "is_not_unique", "Resep tidak ditemukan.", "is_unique", "Resep ini sudah dicatat pengambilannya."),
                        field("account_petugas", "required|is_natural_no_zero|is_not_unique[accounts.account_id]", "is_not_unique", "Petugas tidak ditemukan."),
                        field("diambil_oleh", "required|in_list[Pasien,Keluarga]"),
                        field("nama_pengambil", "permit_empty|max_length[150]"),
                        field("waktu_pengambilan", "permit_empty|valid_date[Y-m-d H:i:s]"),
                        field("metode_verifikasi", "required|in_list[QR,QR + PIN,QR + OTP]"),
                        field("status", "if_exist|in_list[Berhasil,Ditolak]"),
                        field("catatan", "permit_empty")
                ), null);
    }

    private static Table mutasi() {
        return bangun("mutasi_stok", "mutasi stok", "id_mutasi", dibuat(
                i("id_mutasi"), i("id_obat"), i("account_user"), s("jenis_mutasi"), i("jumlah"), i("stok_sebelum"), i("stok_sesudah"), sz("keterangan")
        ), List.of("id_obat", "account_user", "jenis_mutasi", "jumlah", "stok_sebelum", "stok_sesudah", "keterangan"),
                List.of("id_obat", "account_user", "jenis_mutasi", "jumlah", "stok_sebelum", "stok_sesudah"), Set.of(),
                List.of("id_obat", "account_user", "jenis_mutasi"),
                List.of(
                        field("id_obat", "required|is_natural_no_zero|is_not_unique[obat.id_obat]", "is_not_unique", "Obat tidak ditemukan."),
                        field("account_user", "required|is_natural_no_zero|is_not_unique[accounts.account_id]", "is_not_unique", "Pengguna tidak ditemukan."),
                        field("jenis_mutasi", "required|in_list[Masuk,Keluar,Penyesuaian]"),
                        field("jumlah", "required|integer|greater_than[0]"),
                        field("stok_sebelum", "required|integer"),
                        field("stok_sesudah", "required|integer"),
                        field("keterangan", "permit_empty")
                ), null);
    }

    private static Table log() {
        return bangun("log_aktivitas", "log aktivitas", "id_log", dibuat(
                i("id_log"), iz("account_id"), s("aktivitas"), sz("tabel_referensi"), iz("id_referensi"), sz("keterangan"), sz("ip_address")
        ), List.of("account_id", "aktivitas", "tabel_referensi", "id_referensi", "keterangan", "ip_address"),
                List.of("aktivitas"), Set.of(),
                List.of("account_id", "aktivitas", "tabel_referensi"),
                List.of(
                        field("account_id", "permit_empty|is_natural_no_zero|is_not_unique[accounts.account_id]", "is_not_unique", "Pengguna tidak ditemukan."),
                        field("aktivitas", "required|max_length[100]"),
                        field("tabel_referensi", "permit_empty|max_length[100]"),
                        field("id_referensi", "permit_empty|is_natural_no_zero"),
                        field("keterangan", "permit_empty"),
                        field("ip_address", "permit_empty|max_length[45]")
                ), null);
    }

    private static Table bangun(
            String name,
            String label,
            String pk,
            List<Col> columns,
            List<String> writable,
            List<String> required,
            Set<String> allowEmpty,
            List<String> filters,
            List<Field> fields,
            String check
    ) {
        Set<String> tulis = new LinkedHashSet<>(writable);
        Set<String> nullable = new LinkedHashSet<>();
        Set<String> booleans = new LinkedHashSet<>();
        Set<String> integers = new LinkedHashSet<>();
        Set<String> decimals = new LinkedHashSet<>();
        Set<String> dates = new LinkedHashSet<>();
        Set<String> datetimes = new LinkedHashSet<>();
        Set<String> times = new LinkedHashSet<>();
        for (Col kolom : columns) {
            if (!tulis.contains(kolom.name())) {
                continue;
            }
            if (kolom.nullable()) {
                nullable.add(kolom.name());
            }
            switch (kolom.type()) {
                case BOOL -> booleans.add(kolom.name());
                case INT -> integers.add(kolom.name());
                case DEC -> decimals.add(kolom.name());
                case DATE -> dates.add(kolom.name());
                case DT -> datetimes.add(kolom.name());
                case TIME -> times.add(kolom.name());
                default -> { }
            }
        }
        return new Table(
                name, label, pk, columns, List.copyOf(writable), List.copyOf(required), allowEmpty,
                Set.copyOf(nullable), Set.copyOf(booleans), Set.copyOf(integers), Set.copyOf(decimals),
                Set.copyOf(dates), Set.copyOf(datetimes), Set.copyOf(times), List.copyOf(filters), List.copyOf(fields), check
        );
    }

    private static List<Col> stempel(Col... kolom) {
        List<Col> semua = new ArrayList<>(List.of(kolom));
        semua.add(t("created_at"));
        semua.add(t("updated_at"));
        return List.copyOf(semua);
    }

    private static List<Col> dibuat(Col... kolom) {
        List<Col> semua = new ArrayList<>(List.of(kolom));
        semua.add(t("created_at"));
        return List.copyOf(semua);
    }

    private static Field field(String name, String rules) {
        return new Field(name, rules, Map.of());
    }

    private static Field field(String name, String rules, String aturan, String pesan) {
        return new Field(name, rules, Map.of(aturan, pesan));
    }

    private static Field field(String name, String rules, String aturan, String pesan, String aturan2, String pesan2) {
        return new Field(name, rules, Map.of(aturan, pesan, aturan2, pesan2));
    }

    private static Col i(String name) { return new Col(name, Type.INT, false); }
    private static Col iz(String name) { return new Col(name, Type.INT, true); }
    private static Col b(String name) { return new Col(name, Type.BOOL, false); }
    private static Col d(String name) { return new Col(name, Type.DEC, false); }
    private static Col dz(String name) { return new Col(name, Type.DEC, true); }
    private static Col s(String name) { return new Col(name, Type.STR, false); }
    private static Col sz(String name) { return new Col(name, Type.STR, true); }
    private static Col t(String name) { return new Col(name, Type.DT, false); }
    private static Col tz(String name) { return new Col(name, Type.DT, true); }
    private static Col date(String name) { return new Col(name, Type.DATE, false); }
    private static Col time(String name) { return new Col(name, Type.TIME, false); }
}