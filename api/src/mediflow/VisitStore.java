package mediflow;

import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Status kunjungan, masuk, resep, dan stok. Satu salinan untuk tiap jendela. */
final class VisitStore {
    /** Mengunci simpan snapshot dan registrasi supaya akun baru tidak tertimpa. */
    static final Object GERBANG = new Object();

    private static final Pattern WA = Pattern.compile("^08\\d{8,12}$");
    private static final Set<String> HARI = Set.of(VisitRules.HARI);

    final class Hasil<T> {
        final boolean ok;
        final T data;
        final String error;

        private Hasil(boolean ok, T data, String error) {
            this.ok = ok;
            this.data = data;
            this.error = error;
        }
    }

    static final class DetailTampil {
        final Detail baris;
        final Obat obat;
        final String ketersediaan;

        DetailTampil(Detail baris, Obat obat, String ketersediaan) {
            this.baris = baris;
            this.obat = obat;
            this.ketersediaan = ketersediaan;
        }
    }

    static final class KabarTampil {
        final Kabar baris;
        final boolean dibaca;

        KabarTampil(Kabar baris, boolean dibaca) {
            this.baris = baris;
            this.dibaca = dibaca;
        }
    }

    static final class Kunjungan {
        Daftar pendaftaran;
        String pasienNama;
        String nomorBpjs;
        long idUserPasien;
        long idPasien;
        String poliNama;
        String dokterNama;
        String spesialisasi;
        long idDokter;
        long idUserDokter;
        String noTelepon;
        Jadwal jadwal;
        Periksa pemeriksaan;
        Resep resep;
        List<DetailTampil> detail;
        boolean bolehRs;
        int tahap;
        boolean resepTerkirim;
        boolean qrDipakai;
        String pinKeluarga;
        boolean selesai;
    }

    static final class Slot {
        Jadwal jadwal;
        String dokterNama;
        String spesialisasi;
        String poliNama;
        int sisa;
    }

    private DatabaseRecords basis;
    private boolean bolehDorong;
    private String asal = "peramban";
    private Long idUser;
    private String role;
    private final Set<Long> dibaca = new HashSet<>();

    private VisitStore(boolean server) {
        if (!server) {
            basis = DemoData.buat();
            return;
        }
        try (Connection db = Database.open()) {
            basis = DatabaseRecords.dari(DatabaseSnapshot.baca(db));
            asal = "mysql";
            bolehDorong = true;
        } catch (Exception error) {
            basis = DemoData.buat();
            System.err.println("[mediflow] " + Database.nama() + " tidak terbaca: " + ColumnValues.pesan(error));
        }
    }

    static VisitStore memori() {
        return new VisitStore(false);
    }

    static VisitStore dariServer() {
        return new VisitStore(true);
    }

    String sumber() {
        return asal;
    }

    DatabaseRecords snapshot() {
        return basis;
    }

    boolean masuk() {
        return aktor() != null;
    }

    private <T> Hasil<T> gagal(String error) {
        return new Hasil<>(false, null, error);
    }

    private <T> Hasil<T> baik(T data) {
        return new Hasil<>(true, data, "");
    }

    private static long nextId(List<? extends Baris> rows, java.util.function.ToLongFunction<Baris> kunci) {
        long max = 0;
        for (Baris row : rows) {
            max = Math.max(max, kunci.applyAsLong(row));
        }
        return max + 1;
    }

    private void persist() {
        if (!bolehDorong) {
            return;
        }
        try (Connection db = Database.open()) {
            lengkapkanAkun(db);
            DatabaseSnapshot.simpan(db, basis.kePeta());
        } catch (Exception error) {
            System.err.println("[mediflow] Perubahan belum tersimpan ke " + Database.nama() + ": " + ColumnValues.pesan(error));
        }
    }

    /** Menyalin users, pasien, OTP, dan jejak dari MySQL ke sesi yang sedang berjalan. */
    void selaraskanAkun() {
        if (!"mysql".equals(asal)) {
            return;
        }
        try (Connection db = Database.open()) {
            DatabaseRecords baru = DatabaseRecords.dari(DatabaseSnapshot.baca(db));
            ganti(basis.users, baru.users);
            ganti(basis.pasien, baru.pasien);
            ganti(basis.dokter, baru.dokter);
            ganti(basis.otp_verifikasi, baru.otp_verifikasi);
            ganti(basis.log_aktivitas, baru.log_aktivitas);
        } catch (Exception error) {
            System.err.println("[mediflow] Akun baru belum masuk ke sesi: " + ColumnValues.pesan(error));
        }
    }

    private void lengkapkanAkun(Connection db) throws Exception {
        DatabaseRecords baru = DatabaseRecords.dari(DatabaseSnapshot.baca(db));
        tambahYangBelum(basis.users, baru.users, item -> item.id_user);
        tambahYangBelum(basis.pasien, baru.pasien, item -> item.id_pasien);
        tambahYangBelum(basis.dokter, baru.dokter, item -> item.id_dokter);
        tambahYangBelum(basis.otp_verifikasi, baru.otp_verifikasi, item -> item.id_otp);
        tambahYangBelum(basis.log_aktivitas, baru.log_aktivitas, item -> item.id_log);
        for (Pengguna dbUser : baru.users) {
            for (Pengguna lokal : basis.users) {
                if (lokal.id_user == dbUser.id_user && dbUser.is_active && !lokal.is_active) {
                    lokal.is_active = true;
                    lokal.account_status = dbUser.account_status;
                    lokal.role = dbUser.role;
                    lokal.nama_lengkap = dbUser.nama_lengkap;
                    lokal.no_telepon = dbUser.no_telepon;
                    lokal.email = dbUser.email;
                    lokal.password_hash = dbUser.password_hash;
                    lokal.id_profil = dbUser.id_profil;
                    lokal.is_super = dbUser.is_super;
                }
            }
        }
        for (Dokter dbDokter : baru.dokter) {
            for (Dokter lokal : basis.dokter) {
                if (lokal.id_dokter == dbDokter.id_dokter && dbDokter.is_active && !lokal.is_active) {
                    lokal.is_active = true;
                    lokal.id_user = dbDokter.id_user;
                    lokal.id_poli = dbDokter.id_poli;
                    lokal.nama_dokter = dbDokter.nama_dokter;
                    lokal.spesialisasi = dbDokter.spesialisasi;
                }
            }
        }
    }

    private static <T> void ganti(List<T> tujuan, List<T> sumber) {
        tujuan.clear();
        tujuan.addAll(sumber);
    }

    private static <T> void tambahYangBelum(List<T> tujuan, List<T> sumber, java.util.function.ToLongFunction<T> id) {
        Set<Long> ada = new HashSet<>();
        for (T item : tujuan) {
            ada.add(id.applyAsLong(item));
        }
        for (T item : sumber) {
            if (ada.add(id.applyAsLong(item))) {
                tujuan.add(item);
            }
        }
    }

    private void emit() {
        persist();
    }

    private Pengguna aktor() {
        if (idUser == null) {
            return null;
        }
        for (Pengguna user : basis.users) {
            if (user.id_user == idUser && user.is_active && user.role.equals(role)) {
                return user;
            }
        }
        return null;
    }

    private Pengguna cariAkunAktif(String username) {
        for (Pengguna row : basis.users) {
            if (akunSama(row, username) && row.is_active && "ACTIVE".equals(row.account_status)) {
                return row;
            }
        }
        return null;
    }

    private static boolean peranAkun(String role) {
        return "pasien".equals(role) || "dokter".equals(role) || "admin".equals(role) || "farmasi".equals(role);
    }

    private static boolean akunSama(Pengguna row, String akun) {
        String cari = akun == null ? "" : akun.trim();
        if (cari.isEmpty()) {
            return false;
        }
        if (row.username != null && row.username.equalsIgnoreCase(cari)) {
            return true;
        }
        return row.email != null && !row.email.isBlank() && row.email.equalsIgnoreCase(cari);
    }

    String nomorAkun(String username) {
        Pengguna user = null;
        for (Pengguna row : basis.users) {
            if (akunSama(row, username)) {
                user = row;
                break;
            }
        }
        if (user == null) {
            return "";
        }
        for (Pasien row : basis.pasien) {
            if (row.id_user == user.id_user) {
                return row.no_telepon == null ? "" : row.no_telepon;
            }
        }
        return user.no_telepon == null ? "" : user.no_telepon;
    }

    Hasil<Void> login(String username, String password, String nomorWa) {
        Pengguna user = cariAkunAktif(username);
        if (user == null && "mysql".equals(asal)) {
            try (Connection db = Database.open()) {
                basis = DatabaseRecords.dari(DatabaseSnapshot.baca(db));
                user = cariAkunAktif(username);
            } catch (Exception error) {
                System.err.println("[mediflow] Akun baru gagal dimuat: " + ColumnValues.pesan(error));
            }
        }
        if (user == null) {
            return gagal("Akun tidak ditemukan atau nonaktif.");
        }
        if (!PasswordHash.verifikasiAkun(user.username, password, user.password_hash)) {
            return gagal("Kata sandi tidak sesuai.");
        }
        if (!peranAkun(user.role)) {
            return gagal("Peran akun belum ditetapkan. Selesaikan pendaftaran atau hubungi admin.");
        }
        if ("pasien".equals(user.role) && nomorWa != null) {
            String nomor = nomorWhatsApp(nomorWa);
            if (nomor == null) {
                return gagal("Nomor WhatsApp wajib diisi dan dimulai dengan 08.");
            }
            Pasien pasien = null;
            for (Pasien row : basis.pasien) {
                if (row.id_user == user.id_user) {
                    pasien = row;
                    break;
                }
            }
            if (pasien == null) {
                return gagal("Data pasien tidak ditemukan.");
            }
            String sekarang = VisitRules.sekarangIso();
            pasien.no_telepon = nomor;
            pasien.updated_at = sekarang;
            user.no_telepon = nomor;
        }
        user.updated_at = VisitRules.sekarangIso();
        idUser = user.id_user;
        role = user.role;
        catat(user.id_user, "LOGIN", "accounts", user.id_user, "Masuk sebagai " + user.role);
        emit();
        return baik(null);
    }

    void logout() {
        Pengguna user = aktor();
        if (user != null) {
            catat(user.id_user, "LOGOUT", "accounts", user.id_user, "Keluar dari sesi");
        }
        idUser = null;
        role = null;
        emit();
    }

    void resetDemo() {
        Pengguna actor = aktor();
        basis = DemoData.buat();
        dibaca.clear();
        idUser = null;
        role = null;
        if (actor != null) {
            for (Pengguna user : basis.users) {
                if (user.id_user == actor.id_user) {
                    idUser = user.id_user;
                    role = user.role;
                    break;
                }
            }
        }
        emit();
    }

    Pengguna userAktif() {
        return aktor();
    }

    Pasien pasienAktif() {
        Pengguna actor = aktor();
        if (actor == null) {
            return null;
        }
        for (Pasien row : basis.pasien) {
            if (row.id_user == actor.id_user) {
                return row;
            }
        }
        return null;
    }

    Dokter dokterAktif() {
        Pengguna actor = aktor();
        if (actor == null) {
            return null;
        }
        for (Dokter row : basis.dokter) {
            if (row.id_user == actor.id_user && row.is_active) {
                return row;
            }
        }
        return null;
    }

    List<Poli> poli() {
        List<Poli> hasil = new ArrayList<>();
        for (Poli row : basis.poliklinik) {
            if (row.is_active) {
                hasil.add(row);
            }
        }
        return hasil;
    }

    List<Obat> obatAktif() {
        List<Obat> hasil = new ArrayList<>();
        for (Obat row : basis.obat) {
            if (row.is_active) {
                hasil.add(row);
            }
        }
        return hasil;
    }

    List<Kunjungan> kunjungan() {
        List<Daftar> urut = new ArrayList<>(basis.pendaftaran_poli);
        urut.sort(Comparator.comparingLong((Daftar row) -> row.id_pendaftaran).reversed());
        List<Kunjungan> hasil = new ArrayList<>();
        for (Daftar row : urut) {
            Kunjungan item = susun(row);
            if (item != null) {
                hasil.add(item);
            }
        }
        return hasil;
    }

    Kunjungan kunjunganById(long id) {
        for (Daftar item : basis.pendaftaran_poli) {
            if (item.id_pendaftaran == id) {
                return susun(item);
            }
        }
        return null;
    }

    List<Slot> slotHari(String tanggal, Long idPoli) {
        String hari = VisitRules.namaHari(tanggal);
        List<Slot> hasil = new ArrayList<>();
        for (Jadwal jadwal : basis.jadwal_dokter) {
            if (!jadwal.is_active || !hari.equals(jadwal.hari)) {
                continue;
            }
            Dokter dokter = null;
            for (Dokter row : basis.dokter) {
                if (row.id_dokter == jadwal.id_dokter && row.is_active) {
                    dokter = row;
                    break;
                }
            }
            if (dokter == null || (idPoli != null && dokter.id_poli != idPoli)) {
                continue;
            }
            Poli poli = null;
            for (Poli row : basis.poliklinik) {
                if (row.id_poli == dokter.id_poli && row.is_active) {
                    poli = row;
                    break;
                }
            }
            if (poli == null) {
                continue;
            }
            int terpakai = 0;
            for (Daftar row : basis.pendaftaran_poli) {
                if (row.id_jadwal == jadwal.id_jadwal && tanggal.equals(row.tanggal_kunjungan) && !"Batal".equals(row.status_antrean)) {
                    terpakai++;
                }
            }
            Slot slot = new Slot();
            slot.jadwal = jadwal;
            slot.dokterNama = dokter.nama_dokter;
            slot.spesialisasi = dokter.spesialisasi == null ? "" : dokter.spesialisasi;
            slot.poliNama = poli.nama_poli;
            slot.sisa = Math.max(0, jadwal.kuota_maksimal - terpakai);
            hasil.add(slot);
        }
        return hasil;
    }

    List<KabarTampil> notifikasiUser(Long idUserArg) {
        Long id = idUserArg != null ? idUserArg : (aktor() == null ? null : aktor().id_user);
        if (id == null) {
            return List.of();
        }
        Pasien pasien = null;
        for (Pasien row : basis.pasien) {
            if (row.id_user == id) {
                pasien = row;
                break;
            }
        }
        if (pasien == null) {
            return List.of();
        }
        List<Kabar> urut = new ArrayList<>();
        for (Kabar row : basis.notifikasi) {
            if (row.id_pasien == pasien.id_pasien) {
                urut.add(row);
            }
        }
        urut.sort(Comparator.comparingLong((Kabar row) -> row.id_notifikasi).reversed());
        List<KabarTampil> hasil = new ArrayList<>();
        for (Kabar row : urut) {
            hasil.add(new KabarTampil(row, dibaca.contains(row.id_notifikasi)));
        }
        return hasil;
    }

    int jumlahBelumDibaca() {
        int jumlah = 0;
        for (KabarTampil row : notifikasiUser(null)) {
            if (!row.dibaca) {
                jumlah++;
            }
        }
        return jumlah;
    }

    void tandaiDibaca() {
        Pasien pasien = pasienAktif();
        if (pasien == null) {
            return;
        }
        boolean berubah = false;
        for (Kabar row : basis.notifikasi) {
            if (row.id_pasien == pasien.id_pasien && dibaca.add(row.id_notifikasi)) {
                berubah = true;
            }
        }
        if (berubah) {
            // Dibaca hanya di jendela ini, tidak ditulis ke MySQL.
        }
    }

    List<Jejak> aktivitasTerbaru(int batas) {
        List<Jejak> urut = new ArrayList<>(basis.log_aktivitas);
        urut.sort(Comparator.comparingLong((Jejak row) -> row.id_log).reversed());
        return urut.subList(0, Math.min(batas, urut.size()));
    }

    List<Mutasi> mutasiTerbaru(int batas) {
        List<Mutasi> urut = new ArrayList<>(basis.mutasi_stok);
        urut.sort(Comparator.comparingLong((Mutasi row) -> row.id_mutasi).reversed());
        return urut.subList(0, Math.min(batas, urut.size()));
    }

    void catatAksesRekam(long idPendaftaran) {
        Pengguna actor = aktor();
        if (actor == null || "pasien".equals(actor.role)) {
            return;
        }
        long sekarang = System.currentTimeMillis();
        for (Jejak row : basis.log_aktivitas) {
            if (row.id_user != null && row.id_user == actor.id_user && "LIHAT_REKAM_MEDIS".equals(row.aktivitas)
                    && row.id_referensi != null && row.id_referensi == idPendaftaran
                    && sekarang - Instant.parse(row.created_at).toEpochMilli() < 10 * 60 * 1000) {
                return;
            }
        }
        catat(actor.id_user, "LIHAT_REKAM_MEDIS", "pemeriksaan", idPendaftaran, "Membuka data medis kunjungan");
        emit();
    }

    Hasil<Long> daftar(long idJadwal, String keluhanMentah, String catatan) {
        Pengguna actor = aktor();
        Pasien pasien = pasienAktif();
        if (actor == null || pasien == null || !"pasien".equals(actor.role)) {
            return gagal("Hanya pasien yang dapat mendaftar.");
        }
        String keluhan = keluhanMentah == null ? "" : keluhanMentah.trim();
        if (keluhan.isEmpty()) {
            return gagal("Keluhan utama wajib diisi.");
        }
        Jadwal jadwal = null;
        for (Jadwal row : basis.jadwal_dokter) {
            if (row.id_jadwal == idJadwal && row.is_active) {
                jadwal = row;
                break;
            }
        }
        if (jadwal == null) {
            return gagal("Jadwal tidak ditemukan.");
        }
        String tanggal = VisitRules.tanggalIso();
        if (!jadwal.hari.equals(VisitRules.namaHari(tanggal))) {
            return gagal("Jadwal itu bukan untuk hari ini.");
        }
        if (VisitRules.bentrokPasien(basis.pendaftaran_poli, pasien.id_pasien, jadwal.id_jadwal, tanggal)) {
            return gagal("Anda sudah terdaftar pada jadwal dan tanggal yang sama.");
        }
        int terpakai = 0;
        int nomor = 1;
        for (Daftar row : basis.pendaftaran_poli) {
            if (row.id_jadwal == jadwal.id_jadwal && tanggal.equals(row.tanggal_kunjungan)) {
                nomor = Math.max(nomor, row.nomor_antrean + 1);
                if (!"Batal".equals(row.status_antrean)) {
                    terpakai++;
                }
            }
        }
        if (terpakai >= jadwal.kuota_maksimal) {
            return gagal("Kuota jadwal ini sudah penuh.");
        }
        if (VisitRules.bentrokNomorAntrean(basis.pendaftaran_poli, jadwal.id_jadwal, tanggal, nomor)) {
            return gagal("Nomor antrean bentrok. Coba sekali lagi.");
        }
        Dokter dokter = null;
        for (Dokter row : basis.dokter) {
            if (row.id_dokter == jadwal.id_dokter) {
                dokter = row;
                break;
            }
        }
        Poli poli = null;
        if (dokter != null) {
            for (Poli row : basis.poliklinik) {
                if (row.id_poli == dokter.id_poli) {
                    poli = row;
                    break;
                }
            }
        }
        if (dokter == null || poli == null) {
            return gagal("Dokter atau poliklinik tidak ditemukan.");
        }
        String sekarang = VisitRules.sekarangIso();
        long idPendaftaran = nextId(basis.pendaftaran_poli, row -> ((Daftar) row).id_pendaftaran);
        String estimasi = VisitRules.estimasiJamMasuk(tanggal, jadwal.jam_mulai, nomor, dokter.rata_waktu_periksa_menit);
        Daftar daftar = new Daftar();
        daftar.id_pendaftaran = idPendaftaran;
        daftar.id_pasien = pasien.id_pasien;
        daftar.id_jadwal = jadwal.id_jadwal;
        daftar.tanggal_kunjungan = tanggal;
        daftar.nomor_antrean = nomor;
        daftar.estimasi_jam_masuk = estimasi;
        daftar.status_antrean = "Menunggu";
        daftar.catatan_pasien = catatan == null || catatan.trim().isEmpty() ? null : catatan.trim();
        daftar.created_at = sekarang;
        daftar.updated_at = sekarang;
        basis.pendaftaran_poli.add(daftar);
        Periksa periksa = new Periksa();
        periksa.id_pemeriksaan = nextId(basis.pemeriksaan, row -> ((Periksa) row).id_pemeriksaan);
        periksa.id_pendaftaran = idPendaftaran;
        periksa.id_dokter = dokter.id_dokter;
        periksa.keluhan = keluhan;
        periksa.diagnosis = "";
        periksa.tindakan = "";
        periksa.waktu_mulai = sekarang;
        periksa.created_at = sekarang;
        periksa.updated_at = sekarang;
        basis.pemeriksaan.add(periksa);
        VisitRules.KonteksPesan ctx = new VisitRules.KonteksPesan().poli(poli.nama_poli).tanggal(ScreenText.formatTanggal(tanggal))
                .jam(ScreenText.formatJam(estimasi)).nomor(nomor);
        kirimPesan(pasien.id_pasien, idPendaftaran, "Konfirmasi Pendaftaran", ctx);
        kirimPesan(pasien.id_pasien, idPendaftaran, "Pengingat Kunjungan", ctx);
        catat(actor.id_user, "DAFTAR_POLI", "pendaftaran_poli", idPendaftaran, "Antrean " + nomor + " " + poli.nama_poli);
        emit();
        return baik(idPendaftaran);
    }

    Hasil<Void> simpanKeluhan(long idPendaftaran, String keluhan) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        if (!"pasien".equals(actor.role) || kunjungan.idUserPasien != actor.id_user) {
            return gagal("Keluhan hanya dapat diubah oleh pasien.");
        }
        if (kunjungan.tahap >= 5 || kunjungan.selesai) {
            return gagal("Pemeriksaan sudah dikunci.");
        }
        if (keluhan == null || keluhan.trim().isEmpty()) {
            return gagal("Keluhan tidak boleh kosong.");
        }
        String sekarang = VisitRules.sekarangIso();
        kunjungan.pemeriksaan.keluhan = keluhan.trim();
        kunjungan.pemeriksaan.updated_at = sekarang;
        kunjungan.pendaftaran.updated_at = sekarang;
        emit();
        return baik(null);
    }

    Hasil<Void> simpanCatatan(long idPendaftaran, String catatan) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        if (!"pasien".equals(actor.role) || kunjungan.idUserPasien != actor.id_user) {
            return gagal("Catatan alergi hanya dapat diubah oleh pasien.");
        }
        if (kunjungan.tahap >= 5 || kunjungan.selesai) {
            return gagal("Pemeriksaan sudah dikunci.");
        }
        kunjungan.pendaftaran.catatan_pasien = catatan == null || catatan.trim().isEmpty() ? null : catatan.trim();
        kunjungan.pendaftaran.updated_at = VisitRules.sekarangIso();
        emit();
        return baik(null);
    }

    Hasil<Void> checkIn(long idPendaftaran) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        if (kunjungan.idUserPasien != actor.id_user) {
            return gagal("Check-in hanya untuk pasien pemilik kunjungan.");
        }
        if (kunjungan.pendaftaran.waktu_check_in != null && !kunjungan.pendaftaran.waktu_check_in.isEmpty()) {
            return gagal("Check-in sudah tercatat.");
        }
        if ("Batal".equals(kunjungan.pendaftaran.status_antrean)) {
            return gagal("Pendaftaran dibatalkan.");
        }
        String sekarang = VisitRules.sekarangIso();
        kunjungan.pendaftaran.waktu_check_in = sekarang;
        kunjungan.pendaftaran.updated_at = sekarang;
        kirimPesan(kunjungan.idPasien, idPendaftaran, "Pengingat Kunjungan", new VisitRules.KonteksPesan()
                .nomor(kunjungan.pendaftaran.nomor_antrean)
                .jam(ScreenText.formatJam(kunjungan.pendaftaran.estimasi_jam_masuk))
                .mode("checkin"));
        emit();
        return baik(null);
    }

    Hasil<Void> batalkan(long idPendaftaran) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        boolean pemilik = kunjungan.idUserPasien == actor.id_user;
        if (!pemilik && !"admin".equals(actor.role)) {
            return gagal("Anda tidak dapat membatalkan kunjungan ini.");
        }
        if (kunjungan.tahap > 3) {
            return gagal("Kunjungan yang sudah masuk ruang tidak dapat dibatalkan.");
        }
        if ("Batal".equals(kunjungan.pendaftaran.status_antrean)) {
            return gagal("Pendaftaran sudah dibatalkan.");
        }
        String sekarang = VisitRules.sekarangIso();
        kunjungan.pendaftaran.status_antrean = "Batal";
        kunjungan.pendaftaran.updated_at = sekarang;
        if (kunjungan.resep != null && !"Selesai Diambil".equals(kunjungan.resep.status_resep)) {
            kunjungan.resep.status_resep = "Dibatalkan";
            kunjungan.resep.updated_at = sekarang;
        }
        catat(actor.id_user, "BATAL_DAFTAR", "pendaftaran_poli", idPendaftaran, "Antrean " + kunjungan.pendaftaran.nomor_antrean);
        emit();
        return baik(null);
    }

    Hasil<Void> simpanVital(long idPendaftaran, String tekanan, String suhu, String berat, String tinggi) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        if (!"admin".equals(actor.role)) {
            return gagal("Tanda vital diisi oleh perawat atau admin.");
        }
        Double suhuAngka = angka(suhu);
        Double beratAngka = angka(berat);
        Double tinggiAngka = angka(tinggi);
        if (tekanan == null || tekanan.trim().isEmpty() || suhuAngka == null || beratAngka == null || tinggiAngka == null) {
            return gagal("Tekanan darah, suhu, berat, dan tinggi badan wajib diisi.");
        }
        Periksa pemeriksaan = kunjungan.pemeriksaan;
        pemeriksaan.tekanan_darah = tekanan.trim();
        pemeriksaan.suhu_tubuh = suhuAngka;
        pemeriksaan.berat_badan = beratAngka;
        pemeriksaan.tinggi_badan = tinggiAngka;
        pemeriksaan.updated_at = VisitRules.sekarangIso();
        catat(actor.id_user, "SIMPAN_VITAL", "pemeriksaan", pemeriksaan.id_pemeriksaan, "Tanda vital diperbarui");
        emit();
        return baik(null);
    }

    Hasil<Void> simpanPemeriksaan(long idPendaftaran, String diagnosis, String catatan, String tindakan) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        if (!"dokter".equals(actor.role) || actor.id_user != kunjungan.idUserDokter) {
            return gagal("Pemeriksaan ini milik dokter jadwal tersebut.");
        }
        if (kunjungan.tahap < 4) {
            return gagal("Pasien belum masuk ruang periksa.");
        }
        if (kosong(diagnosis) || kosong(catatan) || kosong(tindakan)) {
            return gagal("Diagnosis, tindakan, dan catatan pemeriksaan wajib diisi.");
        }
        String sekarang = VisitRules.sekarangIso();
        Periksa pemeriksaan = kunjungan.pemeriksaan;
        pemeriksaan.diagnosis = diagnosis.trim();
        pemeriksaan.catatan_dokter = catatan.trim();
        pemeriksaan.tindakan = tindakan.trim();
        if (pemeriksaan.waktu_mulai == null || pemeriksaan.waktu_mulai.isEmpty()) {
            pemeriksaan.waktu_mulai = sekarang;
        }
        pemeriksaan.updated_at = sekarang;
        catat(actor.id_user, "SIMPAN_PEMERIKSAAN", "pemeriksaan", pemeriksaan.id_pemeriksaan, diagnosis.trim());
        emit();
        return baik(null);
    }

    Hasil<Void> tambahObat(long idPendaftaran, long idObat, int jumlah, String aturan) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        if (!"dokter".equals(actor.role) || actor.id_user != kunjungan.idUserDokter) {
            return gagal("Hanya dokter pemeriksa yang menambah resep.");
        }
        if (kunjungan.resepTerkirim || kunjungan.tahap > 5) {
            return gagal("Resep sudah dikirim dan tidak dapat diubah.");
        }
        Obat obat = null;
        for (Obat row : basis.obat) {
            if (row.id_obat == idObat && row.is_active) {
                obat = row;
                break;
            }
        }
        if (obat == null) {
            return gagal("Obat tidak ditemukan.");
        }
        if (aturan == null || aturan.trim().isEmpty()) {
            return gagal("VisitRules pakai wajib diisi.");
        }
        int qty = Math.max(1, jumlah);
        Resep resep = pastikanResep(kunjungan);
        String sekarang = VisitRules.sekarangIso();
        Detail ada = null;
        for (Detail row : basis.detail_resep) {
            if (row.id_resep == resep.id_resep && row.id_obat == idObat) {
                ada = row;
                break;
            }
        }
        if (ada != null) {
            ada.jumlah += qty;
            ada.dosis_aturan_pakai = aturan.trim();
            ada.updated_at = sekarang;
        } else {
            Detail baru = new Detail();
            baru.id_detail = nextId(basis.detail_resep, row -> ((Detail) row).id_detail);
            baru.id_resep = resep.id_resep;
            baru.id_obat = idObat;
            baru.jumlah = qty;
            baru.dosis_aturan_pakai = aturan.trim();
            baru.instruksi_racikan = "Racikan".equals(obat.jenis_obat) ? "Racik sesuai aturan farmasi" : null;
            baru.created_at = sekarang;
            baru.updated_at = sekarang;
            basis.detail_resep.add(baru);
        }
        emit();
        return baik(null);
    }

    Hasil<Void> hapusObat(long idDetail) {
        Pengguna actor = aktor();
        if (actor == null || !"dokter".equals(actor.role)) {
            return gagal("Hanya dokter yang dapat menghapus item resep.");
        }
        Detail detail = null;
        for (Detail row : basis.detail_resep) {
            if (row.id_detail == idDetail) {
                detail = row;
                break;
            }
        }
        if (detail == null) {
            return gagal("Item resep tidak ditemukan.");
        }
        Resep resep = null;
        for (Resep row : basis.resep) {
            if (row.id_resep == detail.id_resep) {
                resep = row;
                break;
            }
        }
        if (resep == null) {
            return gagal("Resep tidak ditemukan.");
        }
        Kunjungan kunjungan = kunjunganById(resep.id_pendaftaran);
        if (kunjungan == null || kunjungan.idUserDokter != actor.id_user) {
            return gagal("Resep ini bukan milik Anda.");
        }
        if (kunjungan.resepTerkirim) {
            return gagal("Resep sudah dikirim.");
        }
        basis.detail_resep.remove(detail);
        emit();
        return baik(null);
    }

    Hasil<Void> pilihTebus(long idPendaftaran, String pilihan) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null || kunjungan.resep == null) {
            return gagal("Resep belum tersedia.");
        }
        if (kunjungan.idUserPasien != actor.id_user) {
            return gagal("Pilihan tebus diisi oleh pasien.");
        }
        if (!kunjungan.resepTerkirim) {
            return gagal("Dokter belum mengirim e-resep.");
        }
        if (!"Belum Memilih".equals(kunjungan.resep.pilihan_penebusan)) {
            return gagal("Pilihan tebus sudah dikunci.");
        }
        if ("apotek_rs".equals(pilihan) && !kunjungan.bolehRs) {
            return gagal("Stok rumah sakit tidak mencukupi untuk seluruh item. Pilih apotek luar atau hubungi farmasi.");
        }
        Resep resep = kunjungan.resep;
        String sekarang = VisitRules.sekarangIso();
        resep.pilihan_penebusan = "apotek_rs".equals(pilihan) ? "Apotek RS" : "Apotek Luar";
        resep.updated_at = sekarang;
        if ("apotek_rs".equals(pilihan)) {
            int antrian = 0;
            for (Resep row : basis.resep) {
                if (row.id_resep != resep.id_resep
                        && ("Antrean Farmasi".equals(row.status_resep) || "Sedang Diracik".equals(row.status_resep))) {
                    antrian++;
                }
            }
            List<String> jenis = new ArrayList<>();
            for (DetailTampil row : kunjungan.detail) {
                jenis.add(row.obat.jenis_obat);
            }
            int menit = VisitRules.estimasiFarmasiMenit(antrian, jenis);
            resep.estimasi_jam_selesai = VisitRules.tambahJam(sekarang, menit / 60d);
        }
        catat(actor.id_user, "PILIH_TEBUS", "resep", resep.id_resep, resep.pilihan_penebusan);
        if ("apotek_luar".equals(pilihan)) {
            for (Pasien pasien : basis.pasien) {
                if (pasien.id_pasien == kunjungan.idPasien) {
                    pasien.terdaftar_satusehat = true;
                    pasien.updated_at = sekarang;
                    break;
                }
            }
            kirimPesan(kunjungan.idPasien, idPendaftaran, "Obat Siap",
                    new VisitRules.KonteksPesan().nomor(kunjungan.pendaftaran.nomor_antrean).selesai(true));
        }
        emit();
        return baik(null);
    }

    Hasil<Void> majuTahap(long idPendaftaran) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null) {
            return gagal("Kunjungan tidak ditemukan.");
        }
        int tahap = kunjungan.tahap;
        if (tahap >= 10 || tahap <= 0 || kunjungan.selesai) {
            return gagal("Kunjungan sudah selesai.");
        }
        if (!bolehMaju(actor.role, tahap, actor.id_user == kunjungan.idUserDokter)) {
            return gagal("Peran Anda tidak melanjutkan langkah ini.");
        }
        String cek = validasiMaju(kunjungan, tahap);
        if (cek != null) {
            return gagal(cek);
        }
        String error = terapkanTahap(kunjungan, tahap + 1, actor.id_user);
        if (error != null) {
            return gagal(error);
        }
        catat(actor.id_user, "LANJUT_LANGKAH", "pendaftaran_poli", idPendaftaran, "Ke langkah " + (tahap + 1));
        emit();
        return baik(null);
    }

    Hasil<Void> serahkan(long idPendaftaran, String kode, String pengambil, String pin) {
        Pengguna actor = aktor();
        Kunjungan kunjungan = kunjunganById(idPendaftaran);
        if (actor == null || kunjungan == null || kunjungan.resep == null) {
            return gagal("Resep tidak ditemukan.");
        }
        if (!"farmasi".equals(actor.role) && !"admin".equals(actor.role)) {
            return gagal("Penyerahan dilakukan farmasi.");
        }
        Resep resep = kunjungan.resep;
        if (!resep.kode_qr_unik.trim().equalsIgnoreCase(kode == null ? "" : kode.trim())) {
            return gagal("Kode QR tidak cocok.");
        }
        if (!"Siap Diambil".equals(resep.status_resep) || kunjungan.tahap < 9) {
            return gagal("Obat belum berstatus siap diambil.");
        }
        if (!VisitRules.qrMasihBerlaku(resep.kadaluarsa_qr, kunjungan.qrDipakai)) {
            return gagal("QR kedaluwarsa atau sudah dipakai. Tolak pengambilan.");
        }
        if ("keluarga".equals(pengambil)) {
            Otp otp = otpResep(kunjungan);
            if (otp == null || pin == null || !pin.trim().equals(otp.kode_otp)) {
                if (otp != null) {
                    otp.attempt += 1;
                }
                emit();
                return gagal("PIN keluarga tidak sesuai.");
            }
        }
        Hasil<Void> hasil = selesaiAmbil(kunjungan, actor.id_user, pengambil);
        if (!hasil.ok) {
            return hasil;
        }
        emit();
        return baik(null);
    }

    Hasil<Void> sesuaikanStok(long idObat, int stokBaru, String keterangan) {
        Pengguna actor = aktor();
        if (actor == null || (!"farmasi".equals(actor.role) && !"admin".equals(actor.role))) {
            return gagal("Penyesuaian stok dilakukan farmasi.");
        }
        Obat obat = cariObat(idObat);
        if (obat == null) {
            return gagal("Obat tidak ditemukan.");
        }
        int tujuan = Math.max(0, stokBaru);
        int sebelum = obat.stok_rs;
        if (tujuan == sebelum) {
            return gagal("Stok tidak berubah.");
        }
        obat.stok_rs = tujuan;
        obat.updated_at = VisitRules.sekarangIso();
        catatMutasi(obat.id_obat, actor.id_user, "Penyesuaian", Math.abs(tujuan - sebelum), sebelum, tujuan,
                keterangan == null || keterangan.trim().isEmpty() ? "Penyesuaian stok fisik" : keterangan.trim());
        catat(actor.id_user, "SESUAIKAN_STOK", "obat", obat.id_obat, sebelum + " -> " + tujuan);
        emit();
        return baik(null);
    }

    Hasil<Long> simpanObat(Long idObat, String kodeMentah, String namaMentah, double stokMentah, double harga,
                           String satuanMentah, String jenis, boolean coverBpjs) {
        Pengguna actor = aktor();
        if (actor == null || (!"admin".equals(actor.role) && !"farmasi".equals(actor.role))) {
            return gagal("Data obat diubah oleh admin atau farmasi.");
        }
        String kode = kodeMentah == null ? "" : kodeMentah.trim();
        String nama = namaMentah == null ? "" : namaMentah.trim();
        String satuan = satuanMentah == null ? "" : satuanMentah.trim();
        if (kode.isEmpty() || nama.isEmpty() || satuan.isEmpty()) {
            return gagal("Kode, nama, dan satuan obat wajib diisi.");
        }
        if (!"Jadi".equals(jenis) && !"Racikan".equals(jenis)) {
            return gagal("Jenis obat tidak dikenal.");
        }
        if (Double.isNaN(stokMentah) || Double.isInfinite(stokMentah) || stokMentah < 0) {
            return gagal("Stok tidak boleh minus.");
        }
        int stok = (int) Math.floor(stokMentah);
        if (Double.isNaN(harga) || Double.isInfinite(harga) || harga < 0) {
            return gagal("Harga tidak boleh minus.");
        }
        for (Obat row : basis.obat) {
            if (row.kode_kemenkes.equalsIgnoreCase(kode) && (idObat == null || row.id_obat != idObat)) {
                return gagal("Kode Kemenkes sudah dipakai.");
            }
        }
        String sekarang = VisitRules.sekarangIso();
        if (idObat == null) {
            long id = nextId(basis.obat, row -> ((Obat) row).id_obat);
            Obat obat = new Obat();
            obat.id_obat = id;
            obat.kode_kemenkes = kode;
            obat.nama_obat = nama;
            obat.jenis_obat = jenis;
            obat.satuan = satuan;
            obat.cover_bpjs = coverBpjs;
            obat.stok_rs = stok;
            obat.stok_minimum = 10;
            obat.harga = harga;
            obat.is_active = true;
            obat.created_at = sekarang;
            obat.updated_at = sekarang;
            basis.obat.add(obat);
            if (stok > 0) {
                catatMutasi(id, actor.id_user, "Masuk", stok, 0, stok, "Stok awal obat baru");
            }
            catat(actor.id_user, "TAMBAH_OBAT", "obat", id, kode);
            emit();
            return baik(id);
        }
        Obat obat = cariObat(idObat);
        if (obat == null) {
            return gagal("Obat tidak ditemukan.");
        }
        int sebelum = obat.stok_rs;
        obat.kode_kemenkes = kode;
        obat.nama_obat = nama;
        obat.jenis_obat = jenis;
        obat.satuan = satuan;
        obat.cover_bpjs = coverBpjs;
        obat.harga = harga;
        obat.stok_rs = stok;
        obat.is_active = true;
        obat.updated_at = sekarang;
        if (stok != sebelum) {
            catatMutasi(obat.id_obat, actor.id_user, "Penyesuaian", Math.abs(stok - sebelum), sebelum, stok, "Perubahan stok dari data obat");
        }
        catat(actor.id_user, "UBAH_OBAT", "obat", obat.id_obat, kode);
        emit();
        return baik(obat.id_obat);
    }

    Hasil<Void> nonaktifkanObat(long idObat) {
        Pengguna actor = aktor();
        if (actor == null || (!"admin".equals(actor.role) && !"farmasi".equals(actor.role))) {
            return gagal("Data obat diubah oleh admin atau farmasi.");
        }
        Obat obat = null;
        for (Obat row : basis.obat) {
            if (row.id_obat == idObat && row.is_active) {
                obat = row;
                break;
            }
        }
        if (obat == null) {
            return gagal("Obat tidak ditemukan.");
        }
        obat.is_active = false;
        obat.updated_at = VisitRules.sekarangIso();
        catat(actor.id_user, "NONAKTIF_OBAT", "obat", obat.id_obat, obat.kode_kemenkes);
        emit();
        return baik(null);
    }

    List<Jadwal> jadwalSaya() {
        Dokter dokter = dokterAktif();
        if (dokter == null) {
            return List.of();
        }
        List<Jadwal> hasil = new ArrayList<>();
        for (Jadwal row : basis.jadwal_dokter) {
            if (row.id_dokter == dokter.id_dokter && row.is_active) {
                hasil.add(row);
            }
        }
        return hasil;
    }

    Hasil<Long> simpanJadwal(Long idJadwal, String hari, String mulaiMentah, String selesaiMentah, double kuotaMentah) {
        Pengguna actor = aktor();
        Dokter dokter = dokterAktif();
        if (actor == null || !"dokter".equals(actor.role) || dokter == null) {
            return gagal("Jadwal diubah oleh dokter pemiliknya.");
        }
        if (!HARI.contains(hari)) {
            return gagal("Hari praktik tidak dikenal.");
        }
        String mulai = waktuSql(mulaiMentah);
        String selesai = waktuSql(selesaiMentah);
        if (mulai == null || selesai == null) {
            return gagal("Jam mulai dan jam selesai wajib diisi.");
        }
        if (menitHari(mulai) >= menitHari(selesai)) {
            return gagal("Jam selesai harus setelah jam mulai.");
        }
        if (Double.isNaN(kuotaMentah) || Double.isInfinite(kuotaMentah)) {
            return gagal("Kuota diisi antara 1 dan 200.");
        }
        int kuota = (int) Math.floor(kuotaMentah);
        if (kuota < 1 || kuota > 200) {
            return gagal("Kuota diisi antara 1 dan 200.");
        }
        for (Jadwal row : basis.jadwal_dokter) {
            if (row.id_dokter != dokter.id_dokter || !row.is_active || (idJadwal != null && row.id_jadwal == idJadwal)
                    || !row.hari.equals(hari)) {
                continue;
            }
            if (menitHari(mulai) < menitHari(row.jam_selesai) && menitHari(row.jam_mulai) < menitHari(selesai)) {
                return gagal("Jam ini bertumpuk dengan jadwal Anda yang lain.");
            }
        }
        String sekarang = VisitRules.sekarangIso();
        if (idJadwal == null) {
            long id = nextId(basis.jadwal_dokter, row -> ((Jadwal) row).id_jadwal);
            Jadwal baru = new Jadwal();
            baru.id_jadwal = id;
            baru.id_dokter = dokter.id_dokter;
            baru.hari = hari;
            baru.jam_mulai = mulai;
            baru.jam_selesai = selesai;
            baru.kuota_maksimal = kuota;
            baru.is_active = true;
            baru.created_at = sekarang;
            baru.updated_at = sekarang;
            basis.jadwal_dokter.add(baru);
            catat(actor.id_user, "TAMBAH_JADWAL", "jadwal_dokter", id, hari + " " + mulai);
            emit();
            return baik(id);
        }
        Jadwal jadwal = null;
        for (Jadwal row : basis.jadwal_dokter) {
            if (row.id_jadwal == idJadwal && row.id_dokter == dokter.id_dokter) {
                jadwal = row;
                break;
            }
        }
        if (jadwal == null) {
            return gagal("Jadwal tidak ditemukan.");
        }
        int terpakai = 0;
        String hariIni = VisitRules.tanggalIso();
        for (Daftar row : basis.pendaftaran_poli) {
            if (row.id_jadwal == jadwal.id_jadwal && hariIni.equals(row.tanggal_kunjungan) && !"Batal".equals(row.status_antrean)) {
                terpakai++;
            }
        }
        if (kuota < terpakai) {
            return gagal("Kuota lebih kecil dari antrean yang sudah terdaftar hari ini.");
        }
        jadwal.hari = hari;
        jadwal.jam_mulai = mulai;
        jadwal.jam_selesai = selesai;
        jadwal.kuota_maksimal = kuota;
        jadwal.is_active = true;
        jadwal.updated_at = sekarang;
        catat(actor.id_user, "UBAH_JADWAL", "jadwal_dokter", jadwal.id_jadwal, hari + " " + mulai);
        emit();
        return baik(jadwal.id_jadwal);
    }

    Hasil<Void> nonaktifkanJadwal(long idJadwal) {
        Pengguna actor = aktor();
        Dokter dokter = dokterAktif();
        if (actor == null || !"dokter".equals(actor.role) || dokter == null) {
            return gagal("Jadwal diubah oleh dokter pemiliknya.");
        }
        Jadwal jadwal = null;
        for (Jadwal row : basis.jadwal_dokter) {
            if (row.id_jadwal == idJadwal && row.id_dokter == dokter.id_dokter && row.is_active) {
                jadwal = row;
                break;
            }
        }
        if (jadwal == null) {
            return gagal("Jadwal tidak ditemukan.");
        }
        String hariIni = VisitRules.tanggalIso();
        for (Daftar row : basis.pendaftaran_poli) {
            if (row.id_jadwal == jadwal.id_jadwal && hariIni.equals(row.tanggal_kunjungan) && !"Batal".equals(row.status_antrean)) {
                return gagal("Jadwal ini masih punya antrean hari ini.");
            }
        }
        jadwal.is_active = false;
        jadwal.updated_at = VisitRules.sekarangIso();
        catat(actor.id_user, "NONAKTIF_JADWAL", "jadwal_dokter", jadwal.id_jadwal, jadwal.hari);
        emit();
        return baik(null);
    }

    private boolean bolehMaju(String peran, int tahap, boolean dokterPemilik) {
        if ("admin".equals(peran)) {
            return true;
        }
        if ("dokter".equals(peran) && dokterPemilik && (tahap == 4 || tahap == 5)) {
            return true;
        }
        return "farmasi".equals(peran) && (tahap == 7 || tahap == 8);
    }

    private String validasiMaju(Kunjungan kunjungan, int tahap) {
        if (tahap == 1 && (kunjungan.pendaftaran.waktu_check_in == null || kunjungan.pendaftaran.waktu_check_in.isEmpty())) {
            return "Pasien belum check-in.";
        }
        if (tahap == 1 && (kunjungan.pemeriksaan.keluhan == null || kunjungan.pemeriksaan.keluhan.trim().isEmpty())) {
            return "Keluhan pasien belum diisi.";
        }
        if (tahap == 2 && (kunjungan.pemeriksaan.tekanan_darah == null || kunjungan.pemeriksaan.tekanan_darah.trim().isEmpty())) {
            return "Tanda vital belum diisi.";
        }
        if (tahap == 4 && (kunjungan.pemeriksaan.diagnosis == null || kunjungan.pemeriksaan.diagnosis.trim().isEmpty())) {
            return "Diagnosis belum disimpan.";
        }
        if (tahap == 5 && kunjungan.detail.isEmpty()) {
            return "Tambahkan minimal satu obat sebelum mengirim resep.";
        }
        if (tahap == 6 && (kunjungan.resep == null || "Belum Memilih".equals(kunjungan.resep.pilihan_penebusan))) {
            return "Pasien belum memilih tempat tebus.";
        }
        if (tahap == 6 && kunjungan.resep != null && "Apotek Luar".equals(kunjungan.resep.pilihan_penebusan)) {
            return "Resep dibawa ke apotek luar. Tidak masuk antrean farmasi.";
        }
        return null;
    }

    private String terapkanTahap(Kunjungan kunjungan, int tahap, long actorId) {
        Daftar pendaftaran = kunjungan.pendaftaran;
        String sekarang = VisitRules.sekarangIso();
        if (tahap == 4) {
            pendaftaran.status_antrean = "Masuk Ruangan";
            pendaftaran.updated_at = sekarang;
            if (kunjungan.pemeriksaan.waktu_mulai == null || kunjungan.pemeriksaan.waktu_mulai.isEmpty()) {
                kunjungan.pemeriksaan.waktu_mulai = sekarang;
            }
            kunjungan.pemeriksaan.updated_at = sekarang;
            kirimPesan(kunjungan.idPasien, pendaftaran.id_pendaftaran, "Pengingat Kunjungan",
                    new VisitRules.KonteksPesan().nomor(pendaftaran.nomor_antrean).mode("panggil"));
        }
        if (tahap == 5) {
            pendaftaran.status_antrean = "Selesai Pemeriksaan";
            pendaftaran.updated_at = sekarang;
        }
        if (tahap == 6) {
            Resep resep = pastikanResep(kunjungan);
            kunjungan.pemeriksaan.waktu_selesai = sekarang;
            kunjungan.pemeriksaan.updated_at = sekarang;
            ubahStatusResep(resep, "Menunggu Pilihan", actorId, "E-resep dikirim ke pasien");
            kirimPesan(kunjungan.idPasien, pendaftaran.id_pendaftaran, "QR Resep",
                    new VisitRules.KonteksPesan().nomor(pendaftaran.nomor_antrean));
        }
        if (tahap == 7 && kunjungan.resep != null) {
            ubahStatusResep(kunjungan.resep, "Antrean Farmasi", actorId, "Masuk antrean farmasi");
        }
        if (tahap == 8 && kunjungan.resep != null) {
            ubahStatusResep(kunjungan.resep, "Sedang Diracik", actorId, "Obat disiapkan");
        }
        if (tahap == 9 && kunjungan.resep != null) {
            Resep resep = kunjungan.resep;
            resep.kadaluarsa_qr = VisitRules.tambahJam(sekarang, 24);
            resep.updated_at = sekarang;
            buatPin(kunjungan, resep.kadaluarsa_qr);
            ubahStatusResep(resep, "Siap Diambil", actorId, "Siap di farmasi");
            kirimPesan(kunjungan.idPasien, pendaftaran.id_pendaftaran, "Obat Siap",
                    new VisitRules.KonteksPesan().nomor(pendaftaran.nomor_antrean));
        }
        if (tahap == 10 && kunjungan.resep != null) {
            Hasil<Void> hasil = selesaiAmbil(kunjungan, actorId, "pasien");
            if (!hasil.ok) {
                return hasil.error;
            }
        }
        return null;
    }

    private Hasil<Void> selesaiAmbil(Kunjungan kunjungan, long actorId, String pengambil) {
        Resep resep = kunjungan.resep;
        if (resep == null) {
            return gagal("Resep tidak ditemukan.");
        }
        if ("Selesai Diambil".equals(resep.status_resep)) {
            return baik(null);
        }
        for (Detail detail : basis.detail_resep) {
            if (detail.id_resep != resep.id_resep) {
                continue;
            }
            Obat obat = cariObat(detail.id_obat);
            if (obat == null || obat.stok_rs < detail.jumlah) {
                return gagal("Stok rumah sakit tidak cukup untuk menyerahkan resep ini.");
            }
        }
        String sekarang = VisitRules.sekarangIso();
        Pasien pasien = null;
        for (Pasien row : basis.pasien) {
            if (row.id_pasien == kunjungan.idPasien) {
                pasien = row;
                break;
            }
        }
        keluarkanStok(resep, actorId);
        resep.updated_at = sekarang;
        ubahStatusResep(resep, "Selesai Diambil", actorId, "Diserahkan ke " + pengambil);
        Otp otp = otpResep(kunjungan);
        if (otp != null && (otp.verified_at == null || otp.verified_at.isEmpty())) {
            otp.verified_at = sekarang;
        }
        Ambil ambil = new Ambil();
        ambil.id_pengambilan = nextId(basis.pengambilan_obat, row -> ((Ambil) row).id_pengambilan);
        ambil.id_resep = resep.id_resep;
        ambil.id_user_petugas = actorId;
        ambil.diambil_oleh = "keluarga".equals(pengambil) ? "Keluarga" : "Pasien";
        ambil.nama_pengambil = "keluarga".equals(pengambil) ? "Keluarga" : pasien == null ? null : pasien.nama_lengkap;
        ambil.waktu_pengambilan = sekarang;
        ambil.metode_verifikasi = "keluarga".equals(pengambil) ? "QR + PIN" : "QR";
        ambil.status = "Berhasil";
        ambil.created_at = sekarang;
        ambil.updated_at = sekarang;
        basis.pengambilan_obat.add(ambil);
        if (pasien != null) {
            pasien.terdaftar_satusehat = true;
            pasien.updated_at = sekarang;
        }
        kirimPesan(kunjungan.idPasien, kunjungan.pendaftaran.id_pendaftaran, "Obat Siap",
                new VisitRules.KonteksPesan().nomor(kunjungan.pendaftaran.nomor_antrean).selesai(true));
        catat(actorId, "SERAH_OBAT", "pengambilan_obat", resep.id_resep, pengambil);
        return baik(null);
    }

    private Resep pastikanResep(Kunjungan kunjungan) {
        for (Resep row : basis.resep) {
            if (row.id_pendaftaran == kunjungan.pendaftaran.id_pendaftaran) {
                return row;
            }
        }
        String sekarang = VisitRules.sekarangIso();
        Resep resep = new Resep();
        resep.id_resep = nextId(basis.resep, row -> ((Resep) row).id_resep);
        resep.id_pendaftaran = kunjungan.pendaftaran.id_pendaftaran;
        resep.kode_qr_unik = PasswordHash.randomToken(12);
        resep.waktu_diterbitkan = sekarang;
        resep.pilihan_penebusan = "Belum Memilih";
        resep.status_resep = "Menunggu Pilihan";
        resep.created_at = sekarang;
        resep.updated_at = sekarang;
        basis.resep.add(resep);
        kunjungan.resep = resep;
        return resep;
    }

    private void ubahStatusResep(Resep resep, String baru, long actorId, String catatan) {
        String lama = resep.status_resep;
        if (lama.equals(baru)) {
            return;
        }
        resep.status_resep = baru;
        resep.updated_at = VisitRules.sekarangIso();
        catat(actorId, "UBAH_STATUS_RESEP", "resep", resep.id_resep, lama + " -> " + baru + ". " + catatan);
    }

    private void buatPin(Kunjungan kunjungan, String kedaluwarsa) {
        Pasien pasien = null;
        for (Pasien row : basis.pasien) {
            if (row.id_pasien == kunjungan.idPasien) {
                pasien = row;
                break;
            }
        }
        if (pasien == null) {
            return;
        }
        for (Otp row : basis.otp_verifikasi) {
            if (row.id_user == pasien.id_user && kedaluwarsa.equals(row.expired_at)) {
                return;
            }
        }
        Otp otp = new Otp();
        otp.id_otp = nextId(basis.otp_verifikasi, row -> ((Otp) row).id_otp);
        otp.id_user = pasien.id_user;
        otp.kode_otp = PasswordHash.randomPin();
        otp.tujuan = pasien.no_telepon;
        otp.expired_at = kedaluwarsa;
        otp.attempt = 0;
        otp.created_at = VisitRules.sekarangIso();
        basis.otp_verifikasi.add(otp);
    }

    private Otp otpResep(Kunjungan kunjungan) {
        String kedaluwarsa = kunjungan.resep == null ? null : kunjungan.resep.kadaluarsa_qr;
        if (kedaluwarsa == null) {
            return null;
        }
        for (Otp row : basis.otp_verifikasi) {
            if (row.id_user == kunjungan.idUserPasien && kedaluwarsa.equals(row.expired_at)) {
                return row;
            }
        }
        return null;
    }

    private void keluarkanStok(Resep resep, long actorId) {
        for (Detail detail : basis.detail_resep) {
            if (detail.id_resep != resep.id_resep) {
                continue;
            }
            Obat obat = cariObat(detail.id_obat);
            if (obat == null) {
                continue;
            }
            int sebelum = obat.stok_rs;
            obat.stok_rs = sebelum - detail.jumlah;
            obat.updated_at = VisitRules.sekarangIso();
            catatMutasi(obat.id_obat, actorId, "Keluar", detail.jumlah, sebelum, obat.stok_rs, "Penyerahan resep " + resep.id_resep);
        }
    }

    private void catatMutasi(long idObat, long idUserMutasi, String jenis, int jumlah, int sebelum, int sesudah, String keterangan) {
        Mutasi item = new Mutasi();
        item.id_mutasi = nextId(basis.mutasi_stok, row -> ((Mutasi) row).id_mutasi);
        item.id_obat = idObat;
        item.id_user = idUserMutasi;
        item.jenis_mutasi = jenis;
        item.jumlah = jumlah;
        item.stok_sebelum = sebelum;
        item.stok_sesudah = sesudah;
        item.keterangan = keterangan;
        item.created_at = VisitRules.sekarangIso();
        basis.mutasi_stok.add(item);
    }

    private void kirimPesan(long idPasien, Long idPendaftaran, String jenis, VisitRules.KonteksPesan ctx) {
        Pasien pasien = null;
        for (Pasien row : basis.pasien) {
            if (row.id_pasien == idPasien) {
                pasien = row;
                break;
            }
        }
        if (pasien == null) {
            return;
        }
        String pesan = VisitRules.teksNotifikasi(jenis, ctx);
        String sekarang = VisitRules.sekarangIso();
        Kabar item = new Kabar();
        item.id_notifikasi = nextId(basis.notifikasi, row -> ((Kabar) row).id_notifikasi);
        item.id_pasien = idPasien;
        item.id_pendaftaran = idPendaftaran;
        item.jenis_notifikasi = jenis;
        item.channel = "PWA";
        item.nomor_tujuan = pasien.no_telepon;
        item.pesan = pesan;
        item.status = "Terkirim";
        item.waktu_dikirim = sekarang;
        item.created_at = sekarang;
        basis.notifikasi.add(item);
    }

    private Kunjungan susun(Daftar pendaftaran) {
        Pasien pasien = null;
        for (Pasien row : basis.pasien) {
            if (row.id_pasien == pendaftaran.id_pasien) {
                pasien = row;
                break;
            }
        }
        Periksa pemeriksaan = null;
        for (Periksa row : basis.pemeriksaan) {
            if (row.id_pendaftaran == pendaftaran.id_pendaftaran) {
                pemeriksaan = row;
                break;
            }
        }
        Jadwal jadwal = null;
        for (Jadwal row : basis.jadwal_dokter) {
            if (row.id_jadwal == pendaftaran.id_jadwal) {
                jadwal = row;
                break;
            }
        }
        if (pasien == null || pemeriksaan == null || jadwal == null) {
            return null;
        }
        Dokter dokter = null;
        for (Dokter row : basis.dokter) {
            if (row.id_dokter == pemeriksaan.id_dokter) {
                dokter = row;
                break;
            }
        }
        Poli poli = null;
        if (dokter != null) {
            for (Poli row : basis.poliklinik) {
                if (row.id_poli == dokter.id_poli) {
                    poli = row;
                    break;
                }
            }
        }
        if (dokter == null || poli == null) {
            return null;
        }
        Resep resep = null;
        for (Resep row : basis.resep) {
            if (row.id_pendaftaran == pendaftaran.id_pendaftaran) {
                resep = row;
                break;
            }
        }
        List<DetailTampil> detail = new ArrayList<>();
        if (resep != null) {
            for (Detail row : basis.detail_resep) {
                if (row.id_resep != resep.id_resep) {
                    continue;
                }
                Obat obat = cariObat(row.id_obat);
                if (obat == null) {
                    continue;
                }
                detail.add(new DetailTampil(row, obat, VisitRules.statusKetersediaan(obat.stok_rs, row.jumlah)));
            }
        }
        boolean resepTerkirim = resep != null && pemeriksaan.waktu_selesai != null && !pemeriksaan.waktu_selesai.isEmpty();
        boolean qrDipakai = false;
        if (resep != null) {
            for (Ambil row : basis.pengambilan_obat) {
                if (row.id_resep == resep.id_resep && "Berhasil".equals(row.status)) {
                    qrDipakai = true;
                    break;
                }
            }
        }
        boolean adaVital = pemeriksaan.tekanan_darah != null && !pemeriksaan.tekanan_darah.trim().isEmpty();
        boolean checkIn = pendaftaran.waktu_check_in != null && !pendaftaran.waktu_check_in.isEmpty();
        int tahap = VisitRules.hitungTahap(pendaftaran.status_antrean, checkIn, adaVital, resep, resepTerkirim);
        String pin = null;
        if (resep != null && resep.kadaluarsa_qr != null) {
            for (Otp row : basis.otp_verifikasi) {
                if (row.id_user == pasien.id_user && resep.kadaluarsa_qr.equals(row.expired_at)) {
                    pin = row.kode_otp;
                    break;
                }
            }
        }
        List<String> status = new ArrayList<>();
        for (DetailTampil row : detail) {
            status.add(row.ketersediaan);
        }
        Kunjungan hasil = new Kunjungan();
        hasil.pendaftaran = pendaftaran;
        hasil.pasienNama = pasien.nama_lengkap;
        hasil.nomorBpjs = pasien.nomor_bpjs;
        hasil.idUserPasien = pasien.id_user;
        hasil.idPasien = pasien.id_pasien;
        hasil.poliNama = poli.nama_poli;
        hasil.dokterNama = dokter.nama_dokter;
        hasil.spesialisasi = dokter.spesialisasi == null ? "" : dokter.spesialisasi;
        hasil.idDokter = dokter.id_dokter;
        hasil.idUserDokter = dokter.id_user;
        hasil.noTelepon = pasien.no_telepon;
        hasil.jadwal = jadwal;
        hasil.pemeriksaan = pemeriksaan;
        hasil.resep = resep;
        hasil.detail = detail;
        hasil.bolehRs = VisitRules.bolehTebusRs(status);
        hasil.tahap = tahap;
        hasil.resepTerkirim = resepTerkirim;
        hasil.qrDipakai = qrDipakai;
        hasil.pinKeluarga = pin;
        hasil.selesai = tahap >= 10;
        return hasil;
    }

    private void catat(long idUserCatat, String aktivitas, String tabel, long idReferensi, String keterangan) {
        Jejak item = new Jejak();
        item.id_log = nextId(basis.log_aktivitas, row -> ((Jejak) row).id_log);
        item.id_user = idUserCatat;
        item.aktivitas = aktivitas;
        item.tabel_referensi = tabel;
        item.id_referensi = idReferensi;
        item.keterangan = keterangan;
        item.created_at = VisitRules.sekarangIso();
        basis.log_aktivitas.add(item);
    }

    private Obat cariObat(long id) {
        for (Obat row : basis.obat) {
            if (row.id_obat == id) {
                return row;
            }
        }
        return null;
    }

    private static boolean kosong(String nilai) {
        return nilai == null || nilai.trim().isEmpty();
    }

    private static Double angka(String nilai) {
        if (nilai == null) {
            return null;
        }
        try {
            double angka = Double.parseDouble(nilai.trim().replace(',', '.'));
            return Double.isFinite(angka) ? angka : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static String nomorWhatsApp(String nilai) {
        String lokal = nilai.replaceAll("[^\\d]", "");
        if (lokal.startsWith("62")) {
            lokal = "0" + lokal.substring(2);
        }
        return WA.matcher(lokal).matches() ? lokal : null;
    }

    private static String waktuSql(String nilai) {
        if (nilai == null) {
            return null;
        }
        java.util.regex.Matcher cocok = Pattern.compile("^(\\d{2}):(\\d{2})(?::(\\d{2}))?$").matcher(nilai.trim());
        if (!cocok.matches()) {
            return null;
        }
        int jam = Integer.parseInt(cocok.group(1));
        int menit = Integer.parseInt(cocok.group(2));
        int detik = cocok.group(3) == null ? 0 : Integer.parseInt(cocok.group(3));
        if (jam > 23 || menit > 59 || detik > 59) {
            return null;
        }
        return String.format(Locale.ROOT, "%s:%s:%02d", cocok.group(1), cocok.group(2), detik);
    }

    private static int menitHari(String jam) {
        String[] bagian = jam.split(":");
        return Integer.parseInt(bagian[0]) * 60 + Integer.parseInt(bagian[1]);
    }
}