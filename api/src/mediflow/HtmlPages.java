package mediflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Status layar dan HTML masuk serta beranda. Satu salinan untuk tiap sesi. */
final class HtmlPages {
    private static final String LONCENG = "<svg viewBox=\"0 0 24 24\" width=\"18\" height=\"18\" aria-hidden=\"true\">"
            + "<path fill=\"currentColor\" d=\"M12 3a5 5 0 0 0-5 5v2.1c0 .7-.3 1.4-.8 1.9L4.6 13.6A1.5 1.5 0 0 0 5.7 16h12.6a1.5 1.5 0 0 0 1.1-2.4l-1.6-1.6a2.7 2.7 0 0 1-.8-1.9V8a5 5 0 0 0-5-5zm0 18a2.5 2.5 0 0 0 2.4-2h-4.8A2.5 2.5 0 0 0 12 21z\"/>"
            + "</svg>";

    private record Akun(String peran, String username, String sandi, String nama, String judul, String deskripsi,
                         String ikon) {}

    private static final List<Akun> AKUN = List.of(
            new Akun("pasien", "budi", "pasien123", "Budi Santoso Edit • budi", "Pasien",
                    "Daftar poli, lihat giliran, pilih apotek, dan tunjukkan kode pengambilan", "👤"),
            new Akun("dokter", "dokter01", "dokter123", "Dr. Ahmad • dokter01", "Dokter",
                    "Periksa pasien, tulis diagnosis, dan kirim resep ke farmasi", "🩺"),
            new Akun("admin", "admin", "admin123", "Administrator • admin", "Admin & Perawat",
                    "Catat tanda vital, panggil pasien, dan selesaikan pembayaran", "🛡️"),
            new Akun("farmasi", "farmasi01", "apotek123", "Petugas Farmasi • farmasi01", "Farmasi",
                    "Siapkan obat, cek ulang, lalu serahkan setelah kode pasien cocok", "💊")
    );

    private final VisitStore toko;
    private final List<String[]> toasts = new ArrayList<>();
    private final Set<Long> dikenal = new HashSet<>();
    private final Map<Long, String> stokDraft = new HashMap<>();
    private long stokId;
    private String stokNilai;

    String token = "";
    String halaman = "/";
    String tujuan = "/";
    private String tab = "main";
    private long selectedId;
    private String notice = "";
    private boolean noticeError;
    private boolean showDaftar;
    private long idPoli = 1;
    private long idJadwal;
    private String keluhanBaru = "";
    private String catatanDaftar = "";
    private String keluhan = "";
    private String catatanAlergi = "";
    private String tensi = "";
    private String suhu = "";
    private String berat = "";
    private String tinggi = "";
    private String kodeIcd = ScreenText.ICD.get(0).kode();
    private String catatan = "";
    private String tindakanPilih = ScreenText.TINDAKAN.get(0);
    private long idObat = 1;
    private int jumlahObat = 10;
    private String aturan = ScreenText.ATURAN_PAKAI.get(0);
    private String kodeSerah = "";
    private String pengambil = "pasien";
    private String pinSerah = "";
    private boolean revealNik;
    private boolean panelTerbuka;
    private boolean loginPasienTerbuka;
    private String peran = "pasien";
    private String akunLogin = "";
    private String sandi = "";
    private long obatEdit;
    private String obatKode = "";
    private String obatNama = "";
    private String obatStok = "";
    private String obatHarga = "";
    private String obatSatuan = "Tablet";
    private String obatJenis = "Jadi";
    private boolean obatBpjs;
    private long jadwalEdit;
    private String jadwalHari = "Senin";
    private String jadwalMulai = "";
    private String jadwalSelesai = "";
    private String jadwalKuota = "";
    private String formKunci = "";
    private String waToast = "";
    private boolean dikenalSiap;
    private Long dikenalUntuk;

    HtmlPages(VisitStore toko) {
        this.toko = toko;
    }

    String html() {
        Pengguna aktor = toko.userAktif();
        if (aktor != null) {
            segarkan();
            if (!"pasien".equals(aktor.role)) {
                VisitStore.Kunjungan kunjungan = aktif();
                if (kunjungan != null) {
                    try {
                        toko.catatAksesRekam(kunjungan.pendaftaran.id_pendaftaran);
                    } catch (RuntimeException error) {
                        System.err.println("[mediflow] " + error.getMessage());
                    }
                }
            }
        }
        String isi = aktor != null ? aplikasi(aktor) : ("/daftar".equals(halaman) ? halamanDaftar() : halamanMasuk());
        String kabar = kabarJson(aktor);
        String hasil = dokumen(isi, kabar);
        notice = "";
        noticeError = false;
        toasts.clear();
        waToast = "";
        return hasil;
    }

    void kabar(String pesan, boolean gagal) {
        notice = pesan == null ? "" : pesan;
        noticeError = gagal;
    }

    void tindakan(Map<String, String> form) {
        tujuan = halaman == null ? "/" : halaman;
        tarik(form);
        String aksi = teks(form.get("aksi"));
        if (aksi.startsWith("peran-")) {
            pilihPeran(aksi.substring("peran-".length()));
        } else if (aksi.startsWith("hapus-")) {
            VisitStore.Hasil<Void> hasil = toko.hapusObat(angka(aksi.substring("hapus-".length()), 0));
            if (!hasil.ok) {
                kabar(hasil.error, true);
            }
        } else {
            switch (aksi) {
                case "masuk" -> masukAkun();
                case "keluar" -> keluar();
                case "tab" -> gantiTab(teks(form.get("tab")));
                case "pilih" -> {
                    selectedId = angka(form.get("id"), selectedId);
                    formKunci = "";
                }
                case "panel" -> {
                    if (panelTerbuka) {
                        tutupPanel();
                    } else {
                        panelTerbuka = true;
                    }
                }
                case "tutup-panel" -> tutupPanel();
                case "buka" -> panelTerbuka = true;
                case "baca" -> toko.tandaiDibaca();
                case "checkin" -> tindakanKunjungan((id, k) -> {
                    VisitStore.Hasil<Void> hasil = toko.checkIn(id);
                    hasil(hasil.ok, "Check-in tercatat.", hasil.error);
                });
                case "batal" -> tindakanKunjungan((id, k) -> {
                    VisitStore.Hasil<Void> hasil = toko.batalkan(id);
                    hasil(hasil.ok, "Pendaftaran dibatalkan. Kuota dikembalikan.", hasil.error);
                });
                case "keluhan" -> tindakanKunjungan((id, k) -> {
                    VisitStore.Hasil<Void> simpan = toko.simpanKeluhan(id, keluhan);
                    if (!simpan.ok) {
                        hasil(false, "", simpan.error);
                        return;
                    }
                    VisitStore.Hasil<Void> catatanHasil = toko.simpanCatatan(id, catatanAlergi);
                    hasil(catatanHasil.ok, "Keluhan dan catatan alergi tersimpan.", catatanHasil.error);
                });
                case "buka-daftar" -> showDaftar = !showDaftar;
                case "poli" -> idJadwal = 0;
                case "jadwal", "pengambil" -> {
                }
                case "daftar" -> daftarBaru();
                case "vital" -> tindakanKunjungan((id, k) -> {
                    VisitStore.Hasil<Void> hasil = toko.simpanVital(id, tensi, suhu, berat, tinggi);
                    hasil(hasil.ok, "Tanda vital diperbarui.", hasil.error);
                });
                case "periksa" -> simpanDanMajuDokter();
                case "tambah-obat" -> tindakanKunjungan((id, k) -> {
                    VisitStore.Hasil<Void> hasil = toko.tambahObat(id, idObat, jumlahObat, aturan);
                    hasil(hasil.ok, "Item resep ditambahkan.", hasil.error);
                });
                case "tebus" -> tindakanKunjungan((id, k) -> {
                    String pilihan = teks(form.get("pilihan"));
                    VisitStore.Hasil<Void> hasil = toko.pilihTebus(id, pilihan);
                    String sukses = "apotek_luar".equals(pilihan)
                            ? "Resep dapat ditebus di apotek luar."
                            : "Anda memilih tebus di apotek rumah sakit.";
                    hasil(hasil.ok, sukses, hasil.error);
                });
                case "maju" -> tindakanKunjungan((id, k) -> {
                    int dari = k.tahap;
                    VisitStore.Hasil<Void> hasil = toko.majuTahap(id);
                    if (hasil.ok && dari == 5) {
                        tampilkanWa(k.noTelepon);
                    }
                    String sukses = "farmasi".equals(toko.userAktif() == null ? "" : toko.userAktif().role)
                            ? "Status farmasi diperbarui."
                            : "Kunjungan berpindah ke langkah berikutnya.";
                    hasil(hasil.ok, sukses, hasil.error);
                });
                case "serah" -> tindakanKunjungan((id, k) -> {
                    VisitStore.Hasil<Void> hasil = toko.serahkan(id, kodeSerah, pengambil, pinSerah);
                    hasil(hasil.ok, "Obat diserahkan. Stok berkurang dan QR tidak bisa dipakai lagi.", hasil.error);
                });
                case "stok" -> sesuaikan();
                case "reset-demo" -> {
                    toko.resetDemo();
                    formKunci = "";
                    selectedId = 0;
                    kabar("Data demo dikembalikan ke pemeriksaan dokter.", false);
                }
                case "simpan-obat" -> simpanObatForm();
                case "batal-obat" -> resetObat();
                case "isi-obat" -> isiObat(angka(form.get("id_obat"), 0));
                case "sembunyi-obat" -> sembunyiObat(angka(form.get("id_obat"), 0));
                case "simpan-jadwal" -> simpanJadwalForm();
                case "batal-jadwal" -> resetJadwal();
                case "isi-jadwal" -> isiJadwal(angka(form.get("id_jadwal"), 0));
                case "sembunyi-jadwal" -> sembunyiJadwal(angka(form.get("id_jadwal"), 0));
                case "nik" -> revealNik = !revealNik;
                case "" -> {
                }
                default -> kabar("Aksi tidak dikenal.", true);
            }
        }
        tangkap();
    }

    private void tarik(Map<String, String> form) {
        if (form.containsKey("akun")) {
            akunLogin = form.get("akun").trim();
        }
        if (form.containsKey("sandi")) {
            sandi = form.get("sandi");
        }
        if (form.containsKey("keluhan")) {
            keluhan = form.get("keluhan");
        }
        if (form.containsKey("catatan_alergi")) {
            catatanAlergi = form.get("catatan_alergi");
        }
        if (form.containsKey("keluhan_baru")) {
            keluhanBaru = form.get("keluhan_baru");
        }
        if (form.containsKey("catatan_daftar")) {
            catatanDaftar = form.get("catatan_daftar");
        }
        if (form.containsKey("id_poli")) {
            idPoli = angka(form.get("id_poli"), idPoli);
        }
        if (form.containsKey("id_jadwal") && !"isi-jadwal".equals(form.get("aksi"))
                && !"sembunyi-jadwal".equals(form.get("aksi"))) {
            idJadwal = angka(form.get("id_jadwal"), idJadwal);
        }
        if (form.containsKey("icd")) {
            kodeIcd = form.get("icd");
        }
        if (form.containsKey("catatan")) {
            catatan = form.get("catatan");
        }
        if (form.containsKey("tindakan")) {
            tindakanPilih = form.get("tindakan");
        }
        if (form.containsKey("id_obat") && !"isi-obat".equals(form.get("aksi"))
                && !"sembunyi-obat".equals(form.get("aksi")) && !"stok".equals(form.get("aksi"))) {
            idObat = angka(form.get("id_obat"), idObat);
        }
        if (form.containsKey("jumlah")) {
            jumlahObat = (int) angka(form.get("jumlah"), jumlahObat);
        }
        if (form.containsKey("aturan")) {
            aturan = form.get("aturan");
        }
        if (form.containsKey("tensi")) {
            tensi = form.get("tensi");
        }
        if (form.containsKey("suhu")) {
            suhu = form.get("suhu");
        }
        if (form.containsKey("berat")) {
            berat = form.get("berat");
        }
        if (form.containsKey("tinggi")) {
            tinggi = form.get("tinggi");
        }
        if (form.containsKey("kode")) {
            kodeSerah = form.get("kode");
        }
        if (form.containsKey("pengambil") && ("pasien".equals(form.get("pengambil")) || "keluarga".equals(form.get("pengambil")))) {
            pengambil = form.get("pengambil");
        }
        if (form.containsKey("pin")) {
            pinSerah = form.get("pin");
        }
        if (form.containsKey("obat_kode")) {
            obatKode = form.get("obat_kode");
        }
        if (form.containsKey("obat_nama")) {
            obatNama = form.get("obat_nama");
        }
        if (form.containsKey("obat_stok")) {
            obatStok = form.get("obat_stok");
        }
        if (form.containsKey("obat_harga")) {
            obatHarga = form.get("obat_harga");
        }
        if (form.containsKey("obat_satuan")) {
            obatSatuan = form.get("obat_satuan");
        }
        if (form.containsKey("obat_jenis")) {
            obatJenis = form.get("obat_jenis");
        }
        if (form.containsKey("bpjs")) {
            obatBpjs = "1".equals(form.get("bpjs"));
        }
        if (form.containsKey("hari")) {
            jadwalHari = form.get("hari");
        }
        if (form.containsKey("mulai")) {
            jadwalMulai = form.get("mulai");
        }
        if (form.containsKey("selesai")) {
            jadwalSelesai = form.get("selesai");
        }
        if (form.containsKey("kuota")) {
            jadwalKuota = form.get("kuota");
        }
        if (form.containsKey("stok") && "stok".equals(form.get("aksi"))) {
            stokId = angka(form.get("id_obat"), 0);
            stokNilai = form.get("stok");
            stokDraft.put(stokId, stokNilai);
        }
    }

    private void pilihPeran(String nilai) {
        Akun akun = akun(nilai);
        peran = akun.peran;
        if (!"pasien".equals(peran)) {
            akunLogin = akun.username;
            sandi = akun.sandi;
        } else {
            akunLogin = "";
            sandi = "";
            loginPasienTerbuka = false;
        }
        tujuan = "/";
    }

    void akunDariAlamat(String email) {
        if (email == null) {
            return;
        }
        String bersih = email.trim();
        if (bersih.isEmpty() || bersih.length() > 50) {
            return;
        }
        peran = "pasien";
        akunLogin = bersih;
        sandi = "";
        loginPasienTerbuka = true;
    }

    private void masukAkun() {
        Akun akun = akun(peran);
        String username = "pasien".equals(peran) && !kosong(akunLogin) ? akunLogin : akun.username;
        VisitStore.Hasil<Void> hasil = toko.login(username, sandi, null);
        if (!hasil.ok) {
            kabar(hasil.error, true);
            loginPasienTerbuka = "pasien".equals(peran);
            tujuan = "/";
            return;
        }
        tujuan = "/app";
        notice = "";
        noticeError = false;
    }

    private void keluar() {
        toko.logout();
        tujuan = "/";
        tab = "main";
        selectedId = 0;
        showDaftar = false;
        panelTerbuka = false;
        toasts.clear();
        waToast = "";
        formKunci = "";
    }

    private void gantiTab(String nilai) {
        if ("main".equals(nilai) || "alur".equals(nilai) || "resep".equals(nilai) || "data".equals(nilai)) {
            tab = nilai;
        }
        tutupPanel();
    }

    private void tutupPanel() {
        if (!panelTerbuka) {
            return;
        }
        panelTerbuka = false;
        toko.tandaiDibaca();
    }

    private void daftarBaru() {
        List<VisitStore.Slot> slot = toko.slotHari(VisitRules.tanggalIso(), idPoli);
        VisitStore.Slot pilih = null;
        for (VisitStore.Slot item : slot) {
            if (item.jadwal.id_jadwal == idJadwal) {
                pilih = item;
            }
        }
        if (pilih == null && !slot.isEmpty()) {
            pilih = slot.get(0);
        }
        if (pilih == null) {
            kabar("Tidak ada jadwal untuk poli ini hari ini.", true);
            return;
        }
        VisitStore.Hasil<Long> hasil = toko.daftar(pilih.jadwal.id_jadwal, keluhanBaru, catatanDaftar);
        if (!hasil.ok) {
            hasil(false, "", hasil.error);
            return;
        }
        selectedId = hasil.data;
        showDaftar = false;
        keluhanBaru = "";
        catatanDaftar = "";
        kabar("Pendaftaran berhasil. Konfirmasi masuk ke notifikasi aplikasi.", false);
    }

    private void simpanDanMajuDokter() {
        VisitStore.Kunjungan kunjungan = aktif();
        if (kunjungan == null) {
            kabar("Kunjungan tidak ditemukan.", true);
            return;
        }
        long id = kunjungan.pendaftaran.id_pendaftaran;
        String diagnosis = kodeIcd + " - " + ScreenText.icdNama(kodeIcd);
        VisitStore.Hasil<Void> simpan = toko.simpanPemeriksaan(id, diagnosis, catatan, tindakanPilih);
        if (!simpan.ok) {
            hasil(false, "", simpan.error);
            return;
        }
        if (kunjungan.tahap == 4 || kunjungan.tahap == 5) {
            int dari = kunjungan.tahap;
            VisitStore.Hasil<Void> maju = toko.majuTahap(id);
            if (maju.ok && dari == 5) {
                tampilkanWa(kunjungan.noTelepon);
            }
            String sukses = dari == 4
                    ? "Pemeriksaan selesai. Berikutnya, kirim resep ke farmasi."
                    : "Resep terkirim. Pasien sekarang memilih apotek.";
            hasil(maju.ok, sukses, maju.error);
            return;
        }
        kabar("Catatan medis diperbarui.", false);
    }

    private void sesuaikan() {
        if (stokNilai == null) {
            kabar("Stok tidak berubah.", true);
            return;
        }
        String nama = "obat";
        for (Obat obat : toko.obatAktif()) {
            if (obat.id_obat == stokId) {
                nama = obat.nama_obat;
            }
        }
        VisitStore.Hasil<Void> hasil = toko.sesuaikanStok(stokId, (int) angka(stokNilai, 0), "Hitung ulang stok fisik");
        hasil(hasil.ok, "Stok " + nama + " disesuaikan.", hasil.error);
    }

    private void simpanObatForm() {
        VisitStore.Hasil<Long> hasil = toko.simpanObat(obatEdit == 0 ? null : obatEdit, obatKode, obatNama,
                angkaDouble(obatStok), angkaDouble(obatHarga), obatSatuan, obatJenis, obatBpjs);
        if (!hasil.ok) {
            hasil(false, "", hasil.error);
            return;
        }
        resetObat();
        kabar("Data obat tersimpan. Perubahan stok tercatat di jejak.", false);
    }

    private void isiObat(long id) {
        for (Obat obat : toko.obatAktif()) {
            if (obat.id_obat != id) {
                continue;
            }
            obatEdit = obat.id_obat;
            obatKode = teks(obat.kode_kemenkes);
            obatNama = teks(obat.nama_obat);
            obatStok = Integer.toString(obat.stok_rs);
            obatHarga = angkaForm(obat.harga);
            obatSatuan = teks(obat.satuan);
            obatJenis = teks(obat.jenis_obat);
            obatBpjs = obat.cover_bpjs;
            return;
        }
    }

    private void sembunyiObat(long id) {
        String nama = "Obat";
        for (Obat obat : toko.obatAktif()) {
            if (obat.id_obat == id) {
                nama = obat.nama_obat;
            }
        }
        VisitStore.Hasil<Void> hasil = toko.nonaktifkanObat(id);
        if (obatEdit == id) {
            resetObat();
        }
        hasil(hasil.ok, nama + " disembunyikan dari daftar obat.", hasil.error);
    }

    private void resetObat() {
        obatEdit = 0;
        obatKode = "";
        obatNama = "";
        obatStok = "";
        obatHarga = "";
        obatSatuan = "Tablet";
        obatJenis = "Jadi";
        obatBpjs = false;
    }

    private void simpanJadwalForm() {
        VisitStore.Hasil<Long> hasil = toko.simpanJadwal(jadwalEdit == 0 ? null : jadwalEdit, jadwalHari, jadwalMulai,
                jadwalSelesai, angkaDouble(jadwalKuota));
        if (!hasil.ok) {
            hasil(false, "", hasil.error);
            return;
        }
        resetJadwal();
        kabar("Jadwal praktik tersimpan.", false);
    }

    private void isiJadwal(long id) {
        for (Jadwal jadwal : toko.jadwalSaya()) {
            if (jadwal.id_jadwal != id) {
                continue;
            }
            jadwalEdit = jadwal.id_jadwal;
            jadwalHari = teks(jadwal.hari);
            jadwalMulai = VisitRules.jamPendek(jadwal.jam_mulai);
            jadwalSelesai = VisitRules.jamPendek(jadwal.jam_selesai);
            jadwalKuota = Integer.toString(jadwal.kuota_maksimal);
            return;
        }
    }

    private void sembunyiJadwal(long id) {
        String hari = "jadwal";
        for (Jadwal jadwal : toko.jadwalSaya()) {
            if (jadwal.id_jadwal == id) {
                hari = jadwal.hari;
            }
        }
        VisitStore.Hasil<Void> hasil = toko.nonaktifkanJadwal(id);
        if (jadwalEdit == id) {
            resetJadwal();
        }
        hasil(hasil.ok, "Jadwal " + hari + " disembunyikan.", hasil.error);
    }

    private void resetJadwal() {
        jadwalEdit = 0;
        jadwalHari = "Senin";
        jadwalMulai = "";
        jadwalSelesai = "";
        jadwalKuota = "";
    }

    private void tampilkanWa(String nomor) {
        waToast = "QR pengambilan resep dikirim ke nomor pasien " + teks(nomor) + ".";
    }

    private void hasil(boolean ok, String sukses, String error) {
        kabar(ok ? sukses : teks(error), !ok);
    }

    private interface AksiKunjungan {
        void jalankan(long id, VisitStore.Kunjungan kunjungan);
    }

    private void tindakanKunjungan(AksiKunjungan aksi) {
        VisitStore.Kunjungan kunjungan = aktif();
        if (kunjungan == null) {
            kabar("Kunjungan tidak ditemukan.", true);
            return;
        }
        aksi.jalankan(kunjungan.pendaftaran.id_pendaftaran, kunjungan);
    }

    private void segarkan() {
        VisitStore.Kunjungan kunjungan = aktif();
        String kunci = kunjungan == null ? "" : kunjungan.pendaftaran.id_pendaftaran + ":" + kunjungan.tahap;
        if (kunci.equals(formKunci)) {
            return;
        }
        formKunci = kunci;
        revealNik = false;
        kodeSerah = "";
        pinSerah = "";
        pengambil = "pasien";
        if (kunjungan == null) {
            return;
        }
        Periksa periksa = kunjungan.pemeriksaan;
        String diagnosis = teks(periksa.diagnosis);
        kodeIcd = ScreenText.ICD.get(0).kode();
        for (ScreenText.Kode item : ScreenText.ICD) {
            if (diagnosis.startsWith(item.kode())) {
                kodeIcd = item.kode();
                break;
            }
        }
        keluhan = teks(periksa.keluhan);
        String catatanPasien = kunjungan.pendaftaran.catatan_pasien == null ? "" : kunjungan.pendaftaran.catatan_pasien.trim();
        catatanAlergi = !catatanPasien.isEmpty() && !catatanPasien.equals(keluhan.trim()) ? catatanPasien : "";
        tensi = kosong(periksa.tekanan_darah) ? "120/80" : periksa.tekanan_darah;
        suhu = periksa.suhu_tubuh == null ? "36.8" : ScreenText.angkaTampil(periksa.suhu_tubuh);
        berat = periksa.berat_badan == null ? "60" : ScreenText.angkaTampil(periksa.berat_badan);
        tinggi = periksa.tinggi_badan == null ? "165" : ScreenText.angkaTampil(periksa.tinggi_badan);
        catatan = teks(periksa.catatan_dokter);
        tindakanPilih = kosong(periksa.tindakan) ? ScreenText.TINDAKAN.get(0) : periksa.tindakan;
        List<Obat> obat = toko.obatAktif();
        idObat = obat.isEmpty() ? 1 : obat.get(0).id_obat;
    }

    private void tangkap() {
        if (!aktorSama()) {
            return;
        }
        List<VisitStore.KabarTampil> sekarang = toko.notifikasiUser(null);
        Set<Long> ids = new HashSet<>();
        for (VisitStore.KabarTampil item : sekarang) {
            ids.add(item.baris.id_notifikasi);
        }
        for (Long id : dikenal) {
            if (!ids.contains(id)) {
                dikenal.clear();
                dikenal.addAll(ids);
                return;
            }
        }
        List<String[]> masuk = new ArrayList<>();
        for (VisitStore.KabarTampil item : sekarang) {
            if (!dikenal.add(item.baris.id_notifikasi)) {
                continue;
            }
            masuk.add(new String[] {
                    VisitRules.judulNotifikasi(item.baris.jenis_notifikasi, item.baris.pesan),
                    teks(item.baris.pesan)
            });
        }
        if (masuk.isEmpty()) {
            return;
        }
        List<String[]> gabung = new ArrayList<>(masuk);
        gabung.addAll(toasts);
        toasts.clear();
        for (int i = 0; i < Math.min(3, gabung.size()); i++) {
            toasts.add(gabung.get(i));
        }
    }

    private boolean aktorSama() {
        Pengguna user = toko.userAktif();
        Long id = user == null ? null : user.id_user;
        if (dikenalSiap && sama(id, dikenalUntuk)) {
            return true;
        }
        dikenal.clear();
        dikenalUntuk = id;
        dikenalSiap = true;
        for (VisitStore.KabarTampil item : toko.notifikasiUser(null)) {
            dikenal.add(item.baris.id_notifikasi);
        }
        return false;
    }

    private List<VisitStore.Kunjungan> daftarPeran() {
        List<VisitStore.Kunjungan> hasil = new ArrayList<>();
        Pengguna aktor = toko.userAktif();
        if (aktor == null) {
            return hasil;
        }
        String hari = VisitRules.tanggalIso();
        for (VisitStore.Kunjungan item : toko.kunjungan()) {
            if ("pasien".equals(aktor.role)) {
                if (item.idUserPasien == aktor.id_user) {
                    hasil.add(item);
                }
            } else if ("dokter".equals(aktor.role)) {
                if (item.idUserDokter == aktor.id_user && hari.equals(item.pendaftaran.tanggal_kunjungan)
                        && !"Batal".equals(item.pendaftaran.status_antrean)) {
                    hasil.add(item);
                }
            } else if ("farmasi".equals(aktor.role)) {
                String pilihan = item.resep == null ? null : item.resep.pilihan_penebusan;
                if (item.tahap >= 7 && !"Apotek Luar".equals(pilihan) && !"Batal".equals(item.pendaftaran.status_antrean)) {
                    hasil.add(item);
                }
            } else if (hari.equals(item.pendaftaran.tanggal_kunjungan)) {
                hasil.add(item);
            }
        }
        return hasil;
    }

    private VisitStore.Kunjungan aktif() {
        List<VisitStore.Kunjungan> daftar = daftarPeran();
        for (VisitStore.Kunjungan item : daftar) {
            if (item.pendaftaran.id_pendaftaran == selectedId) {
                return item;
            }
        }
        for (VisitStore.Kunjungan item : daftar) {
            if (!item.selesai && !"Batal".equals(item.pendaftaran.status_antrean)) {
                selectedId = item.pendaftaran.id_pendaftaran;
                return item;
            }
        }
        if (daftar.isEmpty()) {
            return null;
        }
        selectedId = daftar.get(0).pendaftaran.id_pendaftaran;
        return daftar.get(0);
    }

    private ScreenText.Konteks konteks(VisitStore.Kunjungan kunjungan) {
        if (kunjungan == null) {
            return new ScreenText.Konteks(0, false, false, false, false, null, true, false, false);
        }
        String pilihan = null;
        if (kunjungan.resep != null) {
            pilihan = switch (teks(kunjungan.resep.pilihan_penebusan)) {
                case "Apotek RS" -> "rs";
                case "Apotek Luar" -> "luar";
                default -> "belum";
            };
        }
        boolean checkIn = !kosong(kunjungan.pendaftaran.waktu_check_in);
        boolean adaKeluhan = kunjungan.pemeriksaan != null && !kosong(kunjungan.pemeriksaan.keluhan);
        boolean adaVital = kunjungan.pemeriksaan != null && !kosong(kunjungan.pemeriksaan.tekanan_darah);
        return new ScreenText.Konteks(kunjungan.tahap, checkIn, adaKeluhan, adaVital,
                kunjungan.detail != null && !kunjungan.detail.isEmpty(), pilihan, kunjungan.bolehRs,
                "Batal".equals(kunjungan.pendaftaran.status_antrean), kunjungan.selesai);
    }

    private ScreenText.Panduan panduan(String role, VisitStore.Kunjungan kunjungan) {
        return ScreenText.panduan(role, konteks(kunjungan));
    }

    private String halamanMasuk() {
        Akun akun = akun(peran);
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"login-screen\"><div class=\"login-panel\"><div class=\"login-brand\">");
        sb.append("<div class=\"login-logo\">+</div><h1>MediFlow</h1>");
        sb.append("<p>Satu kunjungan, dari daftar sampai obat diambil.</p><ol class=\"login-flow\">");
        sb.append("<li><b>1. Daftar</b>Pasien pilih poli dan datang ke rumah sakit.</li>");
        sb.append("<li><b>2. Periksa</b>Dokter memeriksa dan menulis resep.</li>");
        sb.append("<li><b>3. Resep</b>Pasien pilih apotek, lalu obat disiapkan.</li>");
        sb.append("<li><b>4. Ambil</b>Tunjukkan kode di aplikasi ini ke loket.</li>");
        sb.append("</ol></div><form class=\"login-card\" method=\"post\" action=\"/tindakan\">");
        sb.append(sembunyi("sesi", token));
        sb.append("<h2>Masuk sesuai tugas Anda</h2>");
        sb.append("<p class=\"sub\">Pilih peran. Setiap akun hanya melihat pekerjaan bagiannya.</p>");
        for (Akun item : AKUN) {
            sb.append("<button type=\"submit\" name=\"aksi\" value=\"peran-").append(e(item.peran))
                    .append("\" class=\"role-option").append(item.peran.equals(peran) ? " selected" : "").append("\">");
            sb.append("<span class=\"role-icon\">").append(item.ikon).append("</span><span class=\"role-info\"><h3>")
                    .append(e(item.judul)).append("</h3><p>").append(e(item.deskripsi)).append("</p></span></button>");
        }
        if ("pasien".equals(peran)) {
            sb.append("<button class=\"btn-outline\" id=\"buka-login-pasien\" type=\"button\"")
                    .append(loginPasienTerbuka ? " hidden" : "")
                    .append(">Sudah punya akun? Masuk</button>");
            sb.append("<div class=\"patient-login-fields\" id=\"login-pasien\"")
                    .append(loginPasienTerbuka ? "" : " hidden").append(">");
            sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"akun-login\">Email / akun pasien</label>");
            sb.append("<input class=\"input-field\" id=\"akun-login\" name=\"akun\" autocomplete=\"username\" value=\"")
                    .append(e(akunLogin)).append("\"></div>");
            sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"sandi-login\">Kata sandi</label>");
            sb.append("<input class=\"input-field\" id=\"sandi-login\" type=\"password\" name=\"sandi\" autocomplete=\"current-password\"></div>");
            sb.append("<button class=\"btn-primary\" id=\"btn-login\" type=\"submit\" name=\"aksi\" value=\"masuk\">Masuk ke Sistem →</button>");
            sb.append("</div>");
        } else {
            sb.append("<div class=\"form-group\"><label class=\"form-label\">Akun terpilih</label>");
            sb.append("<input class=\"input-field\" readonly value=\"").append(e(akun.nama)).append("\"></div>");
            sb.append("<div class=\"form-group\"><label class=\"form-label\">Kata sandi</label>");
            sb.append("<input class=\"input-field\" type=\"password\" name=\"sandi\" value=\"").append(e(sandi)).append("\"></div>");
        }
        if (!notice.isEmpty()) {
            sb.append("<p class=\"notice").append(noticeError ? " error" : "").append("\">").append(e(notice)).append("</p>");
        }
        if ("pasien".equals(peran)) {
            sb.append("<a class=\"btn-outline\" href=\"/daftar?sesi=").append(e(token)).append("\">Daftar sebagai pasien baru</a>");
        }
        sb.append("<p class=\"hint\">").append(e(sumberTeks())).append("</p></form>");
        sb.append("</div></section>");
        return sb.toString();
    }

    private String halamanDaftar() {
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"login-screen register-page\"><div class=\"login-panel\"><div class=\"login-brand\">");
        sb.append("<div class=\"login-logo\">+</div><h1>MediFlow</h1>");
        sb.append("<p>Akun baru hanya untuk pasien. Setelah profil lengkap, email itu dipakai untuk masuk dan daftar poli.</p>");
        sb.append("<ol class=\"login-flow\">");
        sb.append("<li><b>1. Akun</b>Nama, email, WhatsApp, dan kata sandi.</li>");
        sb.append("<li><b>2. WhatsApp</b>Masukkan kode enam digit yang dikirim ke nomor Anda.</li>");
        sb.append("<li><b>3. Profil</b>NIK dan tanggal lahir mengaktifkan nomor rekam medis.</li>");
        sb.append("</ol></div><section class=\"register-box\" id=\"registrasi-pasien\">");
        sb.append("<h2>Registrasi pasien</h2>");
        sb.append("<p class=\"sub\">Akun aktif setelah WhatsApp terverifikasi dan profil pasien lengkap.</p>");
        sb.append("<ol class=\"register-progress\" id=\"register-progress\">");
        sb.append("<li data-langkah=\"1\" class=\"aktif\">Akun</li>");
        sb.append("<li data-langkah=\"2\">WhatsApp</li>");
        sb.append("<li data-langkah=\"3\">Profil</li></ol>");
        if (!PatientRegistration.otpSiap()) {
            sb.append("<p class=\"notice error\">Pengiriman OTP belum dikonfigurasi. Isi MEDIFLOW_OTP_SECRET, MEDIFLOW_DELIVERY_URL, dan MEDIFLOW_DELIVERY_TOKEN sebelum mendaftar.</p>");
        }
        sb.append("<div class=\"register-step\" id=\"registrasi-langkah-1\">");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-nama\">Nama lengkap</label><input class=\"input-field\" id=\"reg-nama\" maxlength=\"100\" autocomplete=\"name\" required></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-email\">Email</label><input class=\"input-field\" id=\"reg-email\" type=\"email\" maxlength=\"50\" autocomplete=\"email\" required></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-wa\">Nomor WhatsApp</label><input class=\"input-field\" id=\"reg-wa\" type=\"tel\" inputmode=\"numeric\" placeholder=\"08xxxxxxxxxx\" autocomplete=\"tel\" required></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-password\">Kata sandi (min. 10 karakter)</label><input class=\"input-field\" id=\"reg-password\" type=\"password\" minlength=\"10\" maxlength=\"200\" autocomplete=\"new-password\" required></div>");
        sb.append("<button class=\"btn-primary\" id=\"kirim-registrasi\" type=\"button\">Kirim kode WhatsApp</button></div>");
        sb.append("<div class=\"register-step\" id=\"registrasi-langkah-2\" hidden>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-otp\">Kode OTP</label><input class=\"input-field\" id=\"reg-otp\" inputmode=\"numeric\" maxlength=\"6\" autocomplete=\"one-time-code\" required></div>");
        sb.append("<button class=\"btn-primary\" id=\"verifikasi-registrasi\" type=\"button\">Verifikasi WhatsApp</button></div>");
        sb.append("<div class=\"register-step\" id=\"registrasi-langkah-3\" hidden>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-nik\">NIK (16 digit)</label><input class=\"input-field\" id=\"reg-nik\" inputmode=\"numeric\" maxlength=\"16\" required></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-tanggal\">Tanggal lahir</label><input class=\"input-field\" id=\"reg-tanggal\" type=\"date\" required></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-jk\">Jenis kelamin</label><select class=\"input-field\" id=\"reg-jk\"><option value=\"\">Pilih</option><option value=\"L\">Laki-laki</option><option value=\"P\">Perempuan</option></select></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-bpjs\">Nomor BPJS (opsional)</label><input class=\"input-field\" id=\"reg-bpjs\" maxlength=\"20\"></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\" for=\"reg-alamat\">Alamat (opsional)</label><textarea class=\"input-field\" id=\"reg-alamat\" maxlength=\"2000\"></textarea></div>");
        sb.append("<button class=\"btn-primary\" id=\"lengkapi-registrasi\" type=\"button\">Selesaikan registrasi</button></div>");
        sb.append("<p class=\"notice\" id=\"registrasi-pesan\" role=\"status\" aria-live=\"polite\" hidden></p>");
        sb.append("<a class=\"btn-outline\" href=\"/?sesi=").append(e(token)).append("\">Kembali ke masuk</a>");
        sb.append("</section></div></section>");
        return sb.toString();
    }

    private String aplikasi(Pengguna aktor) {
        String role = aktor.role;
        String tabAktif = "pasien".equals(role) && "data".equals(tab) ? "main" : tab;
        VisitStore.Kunjungan kunjungan = aktif();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"app\">");
        if (!waToast.isEmpty()) {
            sb.append("<div class=\"wa-toast\" role=\"status\"><div class=\"wa-icon\">WA</div><div><h4>Pesan WhatsApp terkirim</h4><p>")
                    .append(e(waToast)).append("</p></div></div>");
        }
        if (panelTerbuka) {
            sb.append(formBuka("inline", "tutup-panel"));
            sb.append("<button class=\"notify-backdrop\" type=\"submit\" aria-label=\"Tutup notifikasi\"></button></form>");
        }
        sb.append(toastHtml());
        sb.append(header(role, aktor, kunjungan));
        sb.append("<main class=\"container\">");
        if (!notice.isEmpty()) {
            sb.append("<div class=\"notice").append(noticeError ? " error" : "").append("\">").append(e(notice)).append("</div>");
        }
        sb.append(banner(role, kunjungan));
        sb.append("<div class=\"layout\">");
        if ("alur".equals(tabAktif)) {
            sb.append(alur(role, kunjungan));
        } else if ("data".equals(tabAktif)) {
            sb.append("dokter".equals(role) ? dataJadwal() : dataObat());
        } else if ("resep".equals(tabAktif)) {
            sb.append(resep(role, kunjungan));
        } else if ("pasien".equals(role)) {
            sb.append(pasien(kunjungan));
        } else if (kunjungan == null) {
            sb.append("<section class=\"block wide\"><div class=\"card\"><p class=\"hint\">")
                    .append(e(panduan(role, null).tindakan())).append("</p></div></section>");
        } else {
            sb.append(pemilih(kunjungan));
            if ("dokter".equals(role)) {
                sb.append(dokter(kunjungan));
            } else if ("farmasi".equals(role)) {
                sb.append(farmasi(kunjungan));
            } else {
                sb.append(admin(kunjungan));
            }
        }
        sb.append("</div></main>");
        sb.append(nav(role));
        sb.append(formBuka("", "buka")).append("</form></div>");
        return sb.toString();
    }

    private String header(String role, Pengguna aktor, VisitStore.Kunjungan kunjungan) {
        ScreenText.Panduan info = panduan(role, kunjungan);
        String sub;
        if ("pasien".equals(role)) {
            sub = "Pasien • " + teks(aktor.nama_lengkap);
        } else if ("dokter".equals(role)) {
            Dokter dokter = toko.dokterAktif();
            sub = "Dokter • " + teks(aktor.nama_lengkap);
            if (dokter != null && !kosong(dokter.spesialisasi)) {
                sub += ", " + dokter.spesialisasi;
            }
        } else if ("farmasi".equals(role)) {
            sub = "Farmasi • " + teks(aktor.nama_lengkap);
        } else {
            sub = "Petugas • Perawat & Admin";
        }
        String judul = kunjungan == null ? "Belum ada kunjungan" : info.status();
        String kosongTeks = switch (role) {
            case "pasien" -> "Pilih poli dan jadwal dokter untuk mendapat nomor antrean";
            case "dokter" -> "Belum ada pasien di jadwal Anda hari ini";
            case "farmasi" -> "Resep muncul setelah pasien memilih apotek rumah sakit";
            default -> "Belum ada kunjungan hari ini";
        };
        String faseNama = "Selesai";
        boolean ketemu = false;
        for (ScreenText.Fase fase : ScreenText.FASE) {
            if (fase.id() == info.faseId()) {
                faseNama = fase.nama();
                ketemu = true;
            }
        }
        if (!ketemu && kunjungan == null) {
            faseNama = "Mulai";
        }
        int belum = toko.jumlahBelumDibaca();
        StringBuilder sb = new StringBuilder();
        sb.append("<header class=\"app-header\"><div class=\"header-top\"><div class=\"brand\"><div class=\"brand-icon\">+</div>");
        sb.append("<div class=\"brand-text\"><h1>MediFlow</h1><p>").append(e(sub)).append("</p></div></div>");
        sb.append("<div class=\"header-actions\">");
        sb.append(formBuka("inline", "panel"));
        sb.append("<button class=\"icon-btn\" type=\"submit\" aria-label=\"Notifikasi\" aria-expanded=\"")
                .append(panelTerbuka ? "true" : "false").append("\">").append(LONCENG);
        if (belum > 0) {
            sb.append("<span class=\"bell-badge\">").append(belum > 9 ? "9+" : Integer.toString(belum)).append("</span>");
        }
        sb.append("</button></form>");
        if (panelTerbuka) {
            sb.append(panel());
        }
        sb.append(formBuka("inline", "keluar"));
        sb.append("<button class=\"logout-btn\" type=\"submit\">Ganti Akun</button></form></div></div>");
        sb.append("<div class=\"queue-banner\"><div class=\"queue-left\"><span>");
        if (kunjungan == null) {
            sb.append("Antrean</span><h2>—</h2><p>").append(e(judul)).append("</p><small>").append(e(kosongTeks)).append("</small>");
        } else {
            sb.append(e(kunjungan.poliNama)).append(" • ").append(e(ScreenText.labelPenjamin(kunjungan.nomorBpjs)));
            sb.append("</span><h2>Antrean ").append(kunjungan.pendaftaran.nomor_antrean).append("</h2><p>").append(e(judul));
            sb.append("</p><small>Estimasi masuk ").append(e(jam(kunjungan.pendaftaran.estimasi_jam_masuk)));
            sb.append(" • ").append(e(kunjungan.dokterNama)).append("</small>");
        }
        sb.append("</div><div class=\"queue-badge\"><strong>");
        sb.append(kunjungan == null ? "—" : kunjungan.tahap + " / 10");
        sb.append("</strong><small>").append(e(faseNama)).append("</small></div></div></header>");
        return sb.toString();
    }

    private String banner(String role, VisitStore.Kunjungan kunjungan) {
        ScreenText.Panduan info = panduan(role, kunjungan);
        boolean selesai = "Kunjungan selesai".equals(info.status());
        StringBuilder sb = new StringBuilder("<div class=\"role-banner\"><div class=\"fase-track\" aria-label=\"Bagian kunjungan\">");
        for (ScreenText.Fase fase : ScreenText.FASE) {
            String kelas = selesai || info.faseId() > fase.id() ? "done" : info.faseId() == fase.id() ? "now" : "";
            sb.append("<span");
            if (!kelas.isEmpty()) {
                sb.append(" class=\"").append(kelas).append("\"");
            }
            sb.append(">").append(e(fase.nama())).append("</span>");
        }
        sb.append("</div><p><strong>").append(e(info.status())).append(".</strong> ").append(e(info.tindakan())).append("</p>");
        if (info.penghalang() != null) {
            sb.append("<p class=\"warn\">").append(e(info.penghalang())).append("</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String pemilih(VisitStore.Kunjungan kunjungan) {
        List<VisitStore.Kunjungan> daftar = daftarPeran();
        if (daftar.size() < 2) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<form class=\"form-group span-all\" method=\"post\" action=\"/tindakan\">");
        sb.append(sembunyi("sesi", token)).append(sembunyi("aksi", "pilih"));
        sb.append("<label class=\"form-label\">Kunjungan</label><select class=\"select-field\" name=\"id\" onchange=\"this.form.submit()\">");
        for (VisitStore.Kunjungan item : daftar) {
            String nilai = Long.toString(item.pendaftaran.id_pendaftaran);
            String label = "Antrean " + item.pendaftaran.nomor_antrean + " • " + item.pasienNama + " • "
                    + ScreenText.langkah(item.tahap).singkat();
            sb.append(opsi(nilai, Long.toString(kunjungan.pendaftaran.id_pendaftaran), label));
        }
        sb.append("</select></form>");
        return sb.toString();
    }

    private String pasien(VisitStore.Kunjungan jalan) {
        boolean masih = jalan != null && !jalan.selesai && !"Batal".equals(jalan.pendaftaran.status_antrean);
        StringBuilder sb = new StringBuilder();
        if (jalan != null) {
            sb.append(pemilih(jalan));
        }
        if (masih) {
            sb.append(formKeluhan(jalan));
        }
        sb.append(formDaftar(!masih || showDaftar, masih));
        if (jalan != null) {
            sb.append(status(jalan)).append(ringkasan(jalan)).append(pilihan(jalan)).append(qr(jalan));
        }
        sb.append(profil()).append(pesan(false));
        return sb.toString();
    }

    private String formKeluhan(VisitStore.Kunjungan kunjungan) {
        boolean terkunci = kunjungan.tahap >= 5;
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Keluhan untuk dokter<span class=\"pill\">");
        sb.append(terkunci ? "Terkunci" : "Masih bisa diubah").append("</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        sb.append("<div class=\"info-grid\">");
        sb.append(info("Poliklinik", kunjungan.poliNama));
        sb.append(info("Dokter", kunjungan.dokterNama));
        sb.append(info("Penjamin", ScreenText.labelPenjamin(kunjungan.nomorBpjs)));
        sb.append(info("Jadwal", VisitRules.jamPendek(kunjungan.jadwal.jam_mulai) + "–" + VisitRules.jamPendek(kunjungan.jadwal.jam_selesai)));
        sb.append("</div><div class=\"form-group\"><label class=\"form-label\">Keluhan utama</label>");
        sb.append("<textarea class=\"textarea-field\" name=\"keluhan\"").append(mati(terkunci)).append(">")
                .append(e(keluhan)).append("</textarea></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Catatan alergi obat</label>");
        sb.append("<input class=\"input-field\" name=\"catatan_alergi\" placeholder=\"Kosongkan jika tidak ada alergi\" value=\"")
                .append(e(catatanAlergi)).append("\"").append(mati(terkunci)).append("></div>");
        sb.append("<button class=\"btn-primary\" type=\"submit\" name=\"aksi\" value=\"keluhan\"").append(mati(terkunci))
                .append(">Simpan keluhan</button></form></section>");
        return sb.toString();
    }

    private String formDaftar(boolean tampil, boolean adaJalan) {
        List<VisitStore.Slot> slot = toko.slotHari(VisitRules.tanggalIso(), idPoli);
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">");
        sb.append(adaJalan ? "Kunjungan lain" : "Daftar poliklinik");
        if (adaJalan) {
            sb.append(formBuka("inline", "buka-daftar"));
            sb.append("<button class=\"btn-small\" type=\"submit\">").append(showDaftar ? "Tutup" : "Buka").append("</button></form>");
        } else {
            sb.append("<span>Kuota dicek otomatis</span>");
        }
        sb.append("</div>");
        if (tampil) {
            long terpilih = idJadwal;
            if ((terpilih == 0 || !adaSlot(slot, terpilih)) && !slot.isEmpty()) {
                terpilih = slot.get(0).jadwal.id_jadwal;
            }
            sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
            sb.append(sembunyi("aksi", "daftar"));
            sb.append("<div class=\"form-row\"><div class=\"form-group\"><label class=\"form-label\">Poliklinik tujuan</label>");
            sb.append("<select class=\"select-field\" name=\"id_poli\" onchange=\"this.form.querySelector('[name=aksi]').value='poli'; this.form.submit()\">");
            for (Poli poli : toko.poli()) {
                sb.append(opsi(Long.toString(poli.id_poli), Long.toString(idPoli), poli.nama_poli));
            }
            sb.append("</select></div><div class=\"form-group\"><label class=\"form-label\">Penjamin</label>");
            Pasien pasien = toko.pasienAktif();
            sb.append("<input class=\"input-field\" readonly value=\"")
                    .append(e(ScreenText.labelPenjamin(pasien == null ? null : pasien.nomor_bpjs))).append("\"></div></div>");
            sb.append("<div class=\"form-group\"><label class=\"form-label\">Dokter dan jadwal hari ini</label>");
            sb.append("<select class=\"select-field\" name=\"id_jadwal\" onchange=\"this.form.querySelector('[name=aksi]').value='jadwal'; this.form.submit()\">");
            for (VisitStore.Slot item : slot) {
                String label = item.dokterNama + ", " + item.spesialisasi + " • " + VisitRules.jamPendek(item.jadwal.jam_mulai)
                        + "–" + VisitRules.jamPendek(item.jadwal.jam_selesai) + " • sisa " + item.sisa;
                sb.append(opsi(Long.toString(item.jadwal.id_jadwal), Long.toString(terpilih), label));
            }
            sb.append("</select></div><div class=\"form-group\"><label class=\"form-label\">Keluhan utama</label>");
            sb.append("<textarea class=\"textarea-field\" name=\"keluhan_baru\" placeholder=\"Contoh: Demam naik turun, nyeri menelan...\">")
                    .append(e(keluhanBaru)).append("</textarea></div>");
            sb.append("<div class=\"form-group\"><label class=\"form-label\">Catatan alergi obat</label>");
            sb.append("<input class=\"input-field\" name=\"catatan_daftar\" placeholder=\"Contoh: Alergi penisilin. Kosongkan jika tidak ada.\" value=\"")
                    .append(e(catatanDaftar)).append("\"></div>");
            sb.append("<button class=\"btn-primary\" id=\"btn-daftar\" type=\"submit\" name=\"aksi\" value=\"daftar\">Daftar dan dapatkan nomor antrean</button>");
            sb.append("<p class=\"hint\">Penjamin mengikuti nomor BPJS di profil. Satu pasien tidak bisa mengambil jadwal yang sama dua kali. Poli tanpa jadwal hari ini tidak muncul.</p>");
            sb.append("</form>");
        }
        sb.append("</section>");
        return sb.toString();
    }

    private String status(VisitStore.Kunjungan kunjungan) {
        boolean sudah = !kosong(kunjungan.pendaftaran.waktu_check_in);
        boolean bolehBatal = kunjungan.tahap > 0 && kunjungan.tahap <= 3 && !"Batal".equals(kunjungan.pendaftaran.status_antrean);
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block\"><div class=\"section-title\">Kedatangan<span class=\"")
                .append(sudah ? "pill" : "pill-warn").append("\">")
                .append(sudah ? "Sudah check-in" : "Belum check-in").append("</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        sb.append("<p class=\"hint\" style=\"margin-top:0;\">Check-in saat Anda sampai di rumah sakit. Pendaftaran bisa dibatalkan sebelum masuk ruang dokter.</p>");
        sb.append("<div class=\"btn-row\"><button class=\"btn-outline\" type=\"submit\" name=\"aksi\" value=\"checkin\"").append(mati(sudah)).append(">");
        sb.append(sudah ? "Check-in " + jam(kunjungan.pendaftaran.waktu_check_in) : "Check-in kedatangan").append("</button>");
        sb.append("<button class=\"btn-outline danger\" type=\"submit\" name=\"aksi\" value=\"batal\"").append(mati(!bolehBatal))
                .append(">Batalkan pendaftaran</button></div></form></section>");
        return sb.toString();
    }

    private String ringkasan(VisitStore.Kunjungan kunjungan) {
        Periksa periksa = kunjungan.pemeriksaan;
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block\"><div class=\"section-title\">Hasil pemeriksaan<span>Rekam medis</span></div><div class=\"card\"><div class=\"info-grid\">");
        sb.append(info("Tekanan darah", kosong(periksa.tekanan_darah) ? "-" : periksa.tekanan_darah));
        sb.append(info("Suhu", ScreenText.angkaTampil(periksa.suhu_tubuh)));
        sb.append(info("Berat dan tinggi", ScreenText.angkaTampil(periksa.berat_badan) + " kg / " + ScreenText.angkaTampil(periksa.tinggi_badan) + " cm"));
        sb.append(info("Diagnosis", kosong(periksa.diagnosis) ? "-" : periksa.diagnosis));
        sb.append(info("Tindakan", kosong(periksa.tindakan) ? "-" : periksa.tindakan));
        sb.append("</div><small class=\"form-label\">Catatan medis dokter</small><p class=\"hint\">");
        sb.append(e(kosong(periksa.catatan_dokter) ? "Belum ada catatan." : periksa.catatan_dokter));
        sb.append("</p></div></section>");
        return sb.toString();
    }

    private String pilihan(VisitStore.Kunjungan kunjungan) {
        Resep resep = kunjungan.resep;
        if (!kunjungan.resepTerkirim || resep == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String kanan = "Apotek Luar".equals(resep.pilihan_penebusan) ? "Dibawa ke apotek luar" : ScreenText.labelStatusResep(resep.status_resep);
        sb.append("<section class=\"block\"><div class=\"section-title\">Pilihan apotek<span>").append(e(kanan)).append("</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        if ("Belum Memilih".equals(resep.pilihan_penebusan)) {
            sb.append("<p class=\"hint\">Pilih satu tempat. Apotek rumah sakit hanya aktif jika semua obat tersedia.</p><div class=\"btn-row\">");
            sb.append(sembunyi("aksi", "tebus"));
            sb.append("<button class=\"btn-primary\" type=\"submit\" name=\"pilihan\" value=\"apotek_rs\"").append(mati(!kunjungan.bolehRs)).append(">Tebus di Apotek RS</button>");
            sb.append("<button class=\"btn-outline\" type=\"submit\" name=\"pilihan\" value=\"apotek_luar\">Tebus di Apotek Luar</button></div>");
        } else {
            String teks = "Pilihan terkunci: " + resep.pilihan_penebusan + ".";
            if (!kosong(resep.estimasi_jam_selesai)) {
                teks += " Estimasi selesai racik " + jam(resep.estimasi_jam_selesai) + ".";
            }
            sb.append("<p class=\"hint\">").append(e(teks)).append("</p>");
        }
        sb.append("</form></section>");
        return sb.toString();
    }

    private String qr(VisitStore.Kunjungan kunjungan) {
        Resep resep = kunjungan.resep;
        if (resep == null) {
            return "";
        }
        boolean tampil = kunjungan.tahap >= 9 || "Apotek Luar".equals(resep.pilihan_penebusan);
        if (!tampil) {
            return "";
        }
        boolean luar = "Apotek Luar".equals(resep.pilihan_penebusan);
        String petunjuk;
        if (luar) {
            petunjuk = "Bawa kode ini ke apotek luar. Stok rumah sakit tidak berkurang.";
        } else if (!kosong(resep.kadaluarsa_qr)) {
            petunjuk = "Tunjukkan kode ini di farmasi rumah sakit. Berlaku sampai " + tgl(resep.kadaluarsa_qr) + " " + jam(resep.kadaluarsa_qr) + ".";
        } else {
            petunjuk = "Tunjukkan kode ini di farmasi rumah sakit.";
        }
        if (!kosong(kunjungan.pinKeluarga)) {
            petunjuk += " PIN keluarga: " + kunjungan.pinKeluarga + ".";
        }
        petunjuk += " PIN hanya tampil di aplikasi ini.";
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block\"><div class=\"section-title\">Kode pengambilan<span>")
                .append(kunjungan.qrDipakai ? "Sudah dipakai" : "Sekali pakai").append("</span></div>");
        sb.append("<div class=\"card qr-box\"><p class=\"mono qr-kode\">").append(e(resep.kode_qr_unik)).append("</p>");
        sb.append("<p class=\"hint\">").append(e(petunjuk)).append("</p></div></section>");
        return sb.toString();
    }

    private String profil() {
        Pasien pasien = toko.pasienAktif();
        Pengguna user = toko.userAktif();
        if (pasien == null || user == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block\"><div class=\"section-title\">Profil pasien<span>")
                .append(e(ScreenText.labelPenjamin(pasien.nomor_bpjs))).append("</span></div><div class=\"card\"><div class=\"info-grid\">");
        sb.append(info("Nama", pasien.nama_lengkap));
        sb.append(info("NIK", revealNik ? pasien.nik : PasswordHash.maskNik(pasien.nik)));
        sb.append(info("Lahir", tgl(pasien.tanggal_lahir) + " • " + ("L".equals(pasien.jenis_kelamin) ? "Laki-laki" : "Perempuan")));
        sb.append(info("BPJS", kosong(pasien.nomor_bpjs) ? "-" : pasien.nomor_bpjs));
        sb.append(info("Nomor WhatsApp", pasien.no_telepon));
        sb.append(info("Notifikasi", "Di aplikasi"));
        sb.append("</div><p class=\"hint\">").append(e(teks(pasien.alamat))).append("</p>");
        sb.append(formBuka("", "nik"));
        sb.append("<button class=\"btn-outline\" type=\"submit\">").append(revealNik ? "Sembunyikan NIK" : "Tampilkan NIK").append("</button></form>");
        sb.append("<p class=\"hint\">Nomor ini yang disebut saat petugas mengirim pemberitahuan QR. Isi pesan tetap masuk di aplikasi, tanpa nama obat. Ubah nomor dari halaman masuk.</p>");
        sb.append("<p class=\"hint\">NIK ditampilkan tertutup di layar. SATUSEHAT: ")
                .append(pasien.terdaftar_satusehat ? "riwayat kunjungan ditandai terkirim" : "belum dikirim").append(".</p>");
        sb.append("</div></section>");
        return sb.toString();
    }

    private String dokter(VisitStore.Kunjungan kunjungan) {
        Periksa periksa = kunjungan.pemeriksaan;
        boolean terkunci = kunjungan.tahap > 5;
        ScreenText.Panduan info = panduan("dokter", kunjungan);
        String labelTombol = info.milikAnda() ? info.tombol() : "Simpan catatan saja";
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Data keluhan dan tanda vital<span>")
                .append(e(kunjungan.pasienNama)).append("</span></div><div class=\"card\">");
        sb.append("<div class=\"allergy-alert\">Catatan pendaftaran: ").append(e(teksAlergi(kunjungan))).append("</div><div class=\"info-grid\">");
        sb.append(info("Keluhan", kosong(periksa.keluhan) ? "-" : periksa.keluhan));
        sb.append(info("Tekanan darah", kosong(periksa.tekanan_darah) ? "-" : periksa.tekanan_darah));
        sb.append(info("Suhu", ScreenText.angkaTampil(periksa.suhu_tubuh)));
        sb.append(info("Berat dan tinggi", ScreenText.angkaTampil(periksa.berat_badan) + " kg / " + ScreenText.angkaTampil(periksa.tinggi_badan) + " cm"));
        sb.append("</div></div></section>");
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Form pemeriksaan<span class=\"pill\">")
                .append(info.milikAnda() ? "Giliran Anda" : "Lihat saja").append("</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Kode dan nama diagnosa (ICD-10)</label>");
        sb.append("<select class=\"select-field\" name=\"icd\">");
        for (ScreenText.Kode item : ScreenText.ICD) {
            sb.append(opsi(item.kode(), kodeIcd, item.kode() + " - " + item.nama()));
        }
        sb.append("</select></div><div class=\"form-group\"><label class=\"form-label\">Anamnesa dan catatan fisik</label>");
        sb.append("<textarea class=\"textarea-field\" name=\"catatan\" placeholder=\"Catatan pemeriksaan\">").append(e(catatan)).append("</textarea></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Tindakan</label><select class=\"select-field\" name=\"tindakan\">");
        for (String item : ScreenText.TINDAKAN) {
            sb.append(opsi(item, tindakanPilih, item));
        }
        sb.append("</select></div><hr class=\"line\"><div class=\"form-group\"><label class=\"form-label\">E-resep dari master obat</label>");
        sb.append("<div class=\"form-row compact\"><select class=\"select-field\" name=\"id_obat\">");
        for (Obat obat : toko.obatAktif()) {
            sb.append(opsi(Long.toString(obat.id_obat), Long.toString(idObat), obat.nama_obat + " • stok " + obat.stok_rs));
        }
        sb.append("</select><input class=\"input-field\" type=\"number\" min=\"1\" name=\"jumlah\" value=\"").append(jumlahObat).append("\"></div>");
        sb.append("<div class=\"form-row compact-btn\" style=\"margin-top:8px;\"><select class=\"select-field\" name=\"aturan\">");
        for (String item : ScreenText.ATURAN_PAKAI) {
            sb.append(opsi(item, aturan, item));
        }
        sb.append("</select><button class=\"btn-outline\" style=\"margin-top:0;\" type=\"submit\" name=\"aksi\" value=\"tambah-obat\"")
                .append(mati(terkunci)).append(">+ Tambah obat</button></div></div>");
        sb.append(daftarObat(kunjungan, !terkunci));
        sb.append("<button class=\"btn-primary\" id=\"btn-dokter\" type=\"submit\" name=\"aksi\" value=\"periksa\">")
                .append(e(labelTombol)).append("</button><p class=\"hint\">")
                .append(e(info.penghalang() != null ? info.penghalang() : info.tindakan())).append("</p></form></section>");
        return sb.toString();
    }

    private String admin(VisitStore.Kunjungan kunjungan) {
        boolean selesai = kunjungan.selesai || "Batal".equals(kunjungan.pendaftaran.status_antrean);
        ScreenText.Panduan info = panduan("admin", kunjungan);
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block\"><div class=\"section-title\">Skrining perawat<span>Tanda vital</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        sb.append("<div class=\"form-row-3\"><div class=\"form-group\"><label class=\"form-label\">Tensi</label>");
        sb.append("<input class=\"input-field\" name=\"tensi\" value=\"").append(e(tensi)).append("\"></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Suhu</label><input class=\"input-field\" name=\"suhu\" value=\"")
                .append(e(suhu)).append("\"></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Berat (kg)</label><input class=\"input-field\" name=\"berat\" value=\"")
                .append(e(berat)).append("\"></div></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Tinggi (cm)</label><input class=\"input-field\" name=\"tinggi\" value=\"")
                .append(e(tinggi)).append("\"></div>");
        sb.append("<button class=\"btn-outline\" style=\"margin-top:0;\" type=\"submit\" name=\"aksi\" value=\"vital\">Simpan tanda vital</button></form></section>");
        sb.append("<section class=\"block\"><div class=\"section-title\">Lanjutkan kunjungan<span class=\"pill\">")
                .append(e(info.milikAnda() ? "Giliran Anda" : info.giliran())).append("</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        sb.append("<p class=\"hint\" style=\"margin-top:0;\">").append(e(info.tindakan())).append("</p>");
        if (info.penghalang() != null) {
            sb.append("<p class=\"hint warn\">").append(e(info.penghalang())).append("</p>");
        }
        sb.append("<button class=\"btn-primary\" id=\"btn-admin\" type=\"submit\" name=\"aksi\" value=\"maju\"").append(mati(selesai)).append(">");
        sb.append(e(selesai ? "Kunjungan selesai" : info.tombol())).append("</button>");
        sb.append("<button class=\"btn-outline\" type=\"submit\" name=\"aksi\" value=\"reset-demo\">Kembalikan data demo</button></form></section>");
        sb.append(jejak());
        return sb.toString();
    }

    private String farmasi(VisitStore.Kunjungan kunjungan) {
        Resep resep = kunjungan.resep;
        ScreenText.Panduan info = panduan("farmasi", kunjungan);
        boolean bolehLanjut = kunjungan.tahap == 7 || kunjungan.tahap == 8;
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block\"><div class=\"section-title\">Antrean farmasi<span>")
                .append(e(resep == null ? "Belum ada resep" : ScreenText.labelStatusResep(resep.status_resep))).append("</span></div>");
        sb.append("<div class=\"card\"><div class=\"info-grid\">");
        sb.append(info("Pasien", kunjungan.pasienNama));
        sb.append(info("Resep", resep == null ? "-" : "Resep #" + resep.id_resep));
        sb.append(info("Tempat", "Farmasi rumah sakit"));
        sb.append(info("Estimasi racik", jam(resep == null ? null : resep.estimasi_jam_selesai)));
        sb.append("</div>").append(daftarObat(kunjungan, false));
        if (bolehLanjut) {
            sb.append(formBuka("", "maju"));
            sb.append("<button class=\"btn-primary\" type=\"submit\">").append(e(info.tombol())).append("</button></form>");
        }
        sb.append("<p class=\"hint\">").append(e(info.penghalang() != null ? info.penghalang() : info.tindakan())).append("</p></div></section>");
        sb.append("<section class=\"block\"><div class=\"section-title\">Serah terima QR<span>1×24 jam</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token)).append(sembunyi("aksi", "serah"));
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Kode QR</label>");
        sb.append("<input class=\"input-field mono\" name=\"kode\" value=\"").append(e(kodeSerah)).append("\"></div>");
        sb.append("<div class=\"form-row\"><div class=\"form-group\"><label class=\"form-label\">Pengambil</label>");
        sb.append("<select class=\"select-field\" name=\"pengambil\" onchange=\"this.form.querySelector('[name=aksi]').value='pengambil'; this.form.submit()\">");
        sb.append(opsi("pasien", pengambil, "Pasien")).append(opsi("keluarga", pengambil, "Keluarga")).append("</select></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">PIN keluarga</label>");
        sb.append("<input class=\"input-field\" name=\"pin\" value=\"").append(e(pinSerah)).append("\"")
                .append(mati(!"keluarga".equals(pengambil))).append("></div></div>");
        sb.append("<button class=\"btn-primary\" id=\"btn-serah\" type=\"submit\" name=\"aksi\" value=\"serah\">Verifikasi dan serahkan obat</button>");
        sb.append("<p class=\"hint\">Kode yang kedaluwarsa atau sudah dipakai ditolak. Jika keluarga yang mengambil, minta PIN yang tampil di aplikasi pasien.</p></form></section>");
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Stok obat<span>Log setiap perubahan</span></div><div class=\"card\">");
        for (Obat obat : toko.obatAktif()) {
            String nilai = stokDraft.getOrDefault(obat.id_obat, Integer.toString(obat.stok_rs));
            sb.append("<form class=\"med-item\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
            sb.append(sembunyi("aksi", "stok")).append(sembunyi("id_obat", Long.toString(obat.id_obat)));
            sb.append("<div><h4>").append(e(obat.nama_obat)).append("</h4><p>").append(e(obat.kode_kemenkes)).append(" • ")
                    .append(e(obat.satuan)).append(" • minimum ").append(obat.stok_minimum).append(" • ").append(e(obat.jenis_obat)).append("</p></div>");
            sb.append("<div class=\"med-side\"><input class=\"input-field\" style=\"width:64px;\" type=\"number\" name=\"stok\" value=\"")
                    .append(e(nilai)).append("\"><button class=\"btn-small\" type=\"submit\">Simpan</button></div></form>");
        }
        sb.append("</div></section>").append(jejak());
        return sb.toString();
    }

    private String daftarObat(VisitStore.Kunjungan kunjungan, boolean bolehHapus) {
        if (kunjungan.detail == null || kunjungan.detail.isEmpty()) {
            return "<p class=\"hint\">Belum ada item resep.</p>";
        }
        StringBuilder sb = new StringBuilder();
        for (VisitStore.DetailTampil item : kunjungan.detail) {
            boolean bayar = kosong(kunjungan.nomorBpjs) || !item.obat.cover_bpjs;
            String aturanTeks = teks(item.baris.dosis_aturan_pakai);
            if (!kosong(item.baris.instruksi_racikan)) {
                aturanTeks += " • " + item.baris.instruksi_racikan;
            }
            String harga = bayar
                    ? "Berbayar " + ScreenText.formatRp(item.obat.harga * item.baris.jumlah)
                    : "Ditanggung BPJS";
            sb.append("<div class=\"med-item\"><div><h4>").append(e(item.obat.nama_obat)).append("</h4><p>")
                    .append(e(aturanTeks)).append("</p><p>").append(e(harga)).append("</p></div><div class=\"med-side\">");
            sb.append("<span class=\"pill").append("tersedia".equals(item.ketersediaan) ? "" : " pill-danger").append("\">");
            sb.append(item.baris.jumlah).append(' ').append(e(item.obat.satuan)).append(" • ")
                    .append(e(ScreenText.labelKetersediaan(item.ketersediaan))).append("</span>");
            if (bolehHapus) {
                sb.append("<button class=\"btn-small danger\" type=\"submit\" name=\"aksi\" value=\"hapus-")
                        .append(item.baris.id_detail).append("\">×</button>");
            }
            sb.append("</div></div>");
        }
        return sb.toString();
    }

    private String alur(String role, VisitStore.Kunjungan kunjungan) {
        int tahap = kunjungan == null ? 0 : kunjungan.tahap;
        ScreenText.Panduan info = panduan(role, kunjungan);
        boolean selesai = "Kunjungan selesai".equals(info.status());
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Perjalanan kunjungan<span>");
        sb.append(tahap > 0 ? "Langkah " + tahap + " dari 10" : "Belum mulai").append("</span></div><div class=\"card\">");
        sb.append("<p class=\"hint\" style=\"margin-top:0;\">Kunjungan berjalan dari daftar, periksa, resep, sampai obat diambil. ");
        sb.append("Bagian berbingkai hijau adalah posisi sekarang. Pasien menunggu. Petugas bagian itu yang melanjutkan dari halaman tugasnya.</p>");
        sb.append("<div class=\"fase-board\">");
        for (ScreenText.Fase fase : ScreenText.FASE) {
            String keadaan = selesai || info.faseId() > fase.id() ? "done" : info.faseId() == fase.id() ? "now" : "";
            String label = "done".equals(keadaan) ? "Selesai" : "now".equals(keadaan) ? "Sekarang" : "Nanti";
            sb.append("<div class=\"fase-col");
            if (!keadaan.isEmpty()) {
                sb.append(' ').append(keadaan);
            }
            sb.append("\"><h3>").append(e(fase.nama())).append("<span>").append(label).append("</span></h3>");
            for (ScreenText.Langkah item : ScreenText.LANGKAH) {
                if (item.tahap() < fase.dari() || item.tahap() > fase.sampai()) {
                    continue;
                }
                String kelas = selesai || (tahap > 0 && item.tahap() < tahap) ? "done" : item.tahap() == tahap ? "current" : "";
                sb.append("<div class=\"step-item");
                if (!kelas.isEmpty()) {
                    sb.append(' ').append(kelas);
                }
                sb.append("\"><div class=\"step-dot\">").append("done".equals(kelas) ? "✓" : Integer.toString(item.tahap())).append("</div>");
                sb.append("<div class=\"step-body\"><h4>").append(e(item.judul())).append("</h4><p>").append(e(item.deskripsi()));
                sb.append("</p><span class=\"owner-tag\">");
                if (item.tahap() == tahap) {
                    sb.append("Sedang dikerjakan · ");
                }
                sb.append(e(item.pemilikNama())).append("</span></div></div>");
            }
            sb.append("</div>");
        }
        sb.append("</div></div></section>");
        return sb.toString();
    }

    private String resep(String role, VisitStore.Kunjungan kunjungan) {
        if (kunjungan == null) {
            return "<section class=\"block wide\"><div class=\"card\"><p class=\"hint\">" + e(panduan(role, null).tindakan()) + "</p></div></section>";
        }
        boolean bolehHapus = "dokter".equals(role) && !kunjungan.resepTerkirim && kunjungan.tahap <= 5;
        String tempat = kunjungan.resep != null && "Apotek Luar".equals(kunjungan.resep.pilihan_penebusan)
                ? "Apotek luar" : "Farmasi rumah sakit";
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Daftar obat e-resep<span class=\"pill\">")
                .append(e(tempat)).append("</span></div>");
        if (bolehHapus) {
            sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        } else {
            sb.append("<div class=\"card\">");
        }
        if (kunjungan.resep == null) {
            sb.append("<p class=\"hint\">Resep belum dibuat.</p>");
        } else {
            sb.append("<p class=\"hint\">Resep #").append(kunjungan.resep.id_resep).append(" • ")
                    .append(e(kunjungan.resepTerkirim ? ScreenText.labelStatusResep(kunjungan.resep.status_resep) : "Belum dikirim dokter"))
                    .append("</p>");
        }
        sb.append(daftarObat(kunjungan, bolehHapus));
        sb.append(bolehHapus ? "</form>" : "</div>").append("</section>");
        if ("pasien".equals(role)) {
            sb.append(pilihan(kunjungan)).append(qr(kunjungan)).append(pesan(true));
        }
        return sb.toString();
    }

    private String jejak() {
        DatabaseRecords basis = toko.snapshot();
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Jejak stok dan akses<span>")
                .append(basis.log_aktivitas.size()).append(" catatan</span></div><div class=\"card\">");
        for (Mutasi log : toko.mutasiTerbaru(6)) {
            String nama = "Obat";
            for (Obat obat : basis.obat) {
                if (obat.id_obat == log.id_obat) {
                    nama = obat.nama_obat;
                }
            }
            sb.append("<div class=\"med-item\"><div><h4>").append(e(nama)).append(" • ").append(e(log.jenis_mutasi));
            sb.append("</h4><p>").append(log.stok_sebelum).append(" → ").append(log.stok_sesudah).append(". ")
                    .append(e(teks(log.keterangan))).append("</p></div></div>");
        }
        for (Jejak log : toko.aktivitasTerbaru(6)) {
            sb.append("<div class=\"med-item\"><div><h4>").append(e(log.aktivitas)).append("</h4><p>")
                    .append(e(kosong(log.tabel_referensi) ? "-" : log.tabel_referensi)).append(" #")
                    .append(log.id_referensi == null ? "-" : log.id_referensi).append(" • ")
                    .append(e(teks(log.keterangan))).append("</p></div></div>");
        }
        if (basis.mutasi_stok.isEmpty() && basis.log_aktivitas.isEmpty()) {
            sb.append("<p class=\"hint\">Belum ada jejak.</p>");
        }
        sb.append("</div></section>");
        return sb.toString();
    }

    private String dataObat() {
        List<String> satuan = new ArrayList<>(List.of("Tablet", "Kapsul", "Paket", "Botol"));
        if (!satuan.contains(obatSatuan) && !obatSatuan.isEmpty()) {
            satuan.add(0, obatSatuan);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Manajemen data obat<span>Tabel obat</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        sb.append("<div class=\"form-row\"><div class=\"form-group\"><label class=\"form-label\">Kode Kemenkes</label>");
        sb.append("<input class=\"input-field\" name=\"obat_kode\" placeholder=\"OBT-001\" value=\"").append(e(obatKode)).append("\"></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Nama obat</label>");
        sb.append("<input class=\"input-field\" name=\"obat_nama\" placeholder=\"Paracetamol 500mg\" value=\"").append(e(obatNama)).append("\"></div></div>");
        sb.append("<div class=\"form-row\"><div class=\"form-group\"><label class=\"form-label\">Stok RS</label>");
        sb.append("<input class=\"input-field\" type=\"number\" min=\"0\" name=\"obat_stok\" value=\"").append(e(obatStok)).append("\"></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Harga (Rp)</label>");
        sb.append("<input class=\"input-field\" type=\"number\" min=\"0\" name=\"obat_harga\" value=\"").append(e(obatHarga)).append("\"></div></div>");
        sb.append("<div class=\"form-row-3\"><div class=\"form-group\"><label class=\"form-label\">Satuan</label><select class=\"select-field\" name=\"obat_satuan\">");
        for (String item : satuan) {
            sb.append(opsi(item, obatSatuan, item));
        }
        sb.append("</select></div><div class=\"form-group\"><label class=\"form-label\">Jenis</label><select class=\"select-field\" name=\"obat_jenis\">");
        sb.append(opsi("Jadi", obatJenis, "Jadi")).append(opsi("Racikan", obatJenis, "Racikan")).append("</select></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">BPJS</label><label class=\"check-line\">");
        sb.append("<input type=\"hidden\" name=\"bpjs\" value=\"0\"><input type=\"checkbox\" name=\"bpjs\" value=\"1\"");
        if (obatBpjs) {
            sb.append(" checked");
        }
        sb.append("> Ditanggung</label></div></div>");
        sb.append("<button class=\"btn-primary\" type=\"submit\" name=\"aksi\" value=\"simpan-obat\">")
                .append(obatEdit == 0 ? "Tambah obat baru" : "Simpan perubahan").append("</button>");
        if (obatEdit != 0) {
            sb.append("<button class=\"btn-outline\" type=\"submit\" name=\"aksi\" value=\"batal-obat\">Batal edit</button>");
        }
        sb.append("<p class=\"hint\">Perubahan stok masuk ke jejak mutasi. Menyembunyikan obat tidak menghapus resep lama.</p></form></section>");
        sb.append("<section class=\"block wide\"><div class=\"card\"><div class=\"table-wrap\"><table class=\"crud-table\"><thead><tr>");
        sb.append("<th>Kode</th><th>Nama obat</th><th>Stok</th><th>Harga</th><th>Aksi</th></tr></thead><tbody>");
        List<Obat> daftar = toko.obatAktif();
        for (Obat obat : daftar) {
            sb.append("<tr><td>").append(e(obat.kode_kemenkes)).append("</td><td><strong>").append(e(obat.nama_obat));
            sb.append("</strong><br><span class=\"hint\">").append(e(obat.satuan)).append(" • ").append(e(obat.jenis_obat));
            if (obat.cover_bpjs) {
                sb.append(" • BPJS");
            }
            sb.append("</span></td><td>").append(obat.stok_rs).append("</td><td>").append(e(ScreenText.formatRp(obat.harga)));
            sb.append("</td><td class=\"aksi\">");
            sb.append(formBuka("inline", "isi-obat")).append(sembunyi("id_obat", Long.toString(obat.id_obat)));
            sb.append("<button class=\"btn-small\" type=\"submit\">Ubah</button></form>");
            sb.append("<form class=\"inline\" method=\"post\" action=\"/tindakan\" onsubmit=\"return confirm('Sembunyikan ")
                    .append(js(obat.nama_obat)).append(" dari daftar? Riwayat stok tetap tersimpan.')\">");
            sb.append(sembunyi("sesi", token)).append(sembunyi("aksi", "sembunyi-obat")).append(sembunyi("id_obat", Long.toString(obat.id_obat)));
            sb.append("<button class=\"btn-small danger\" type=\"submit\">Sembunyi</button></form></td></tr>");
        }
        sb.append("</tbody></table></div>");
        if (daftar.isEmpty()) {
            sb.append("<p class=\"hint\">Belum ada obat aktif.</p>");
        }
        sb.append("</div></section>");
        return sb.toString();
    }

    private String dataJadwal() {
        List<String> hari = new ArrayList<>(List.of("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"));
        if (!hari.contains(jadwalHari) && !jadwalHari.isEmpty()) {
            hari.add(0, jadwalHari);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block wide\"><div class=\"section-title\">Jadwal praktik saya<span>Tabel jadwal dokter</span></div>");
        sb.append("<form class=\"card\" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token));
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Hari praktik</label><select class=\"select-field\" name=\"hari\">");
        for (String item : hari) {
            sb.append(opsi(item, jadwalHari, item));
        }
        sb.append("</select></div><div class=\"form-row-3\"><div class=\"form-group\"><label class=\"form-label\">Jam mulai</label>");
        sb.append("<input class=\"input-field\" type=\"time\" name=\"mulai\" value=\"").append(e(jadwalMulai)).append("\"></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Jam selesai</label>");
        sb.append("<input class=\"input-field\" type=\"time\" name=\"selesai\" value=\"").append(e(jadwalSelesai)).append("\"></div>");
        sb.append("<div class=\"form-group\"><label class=\"form-label\">Kuota</label>");
        sb.append("<input class=\"input-field\" type=\"number\" min=\"1\" name=\"kuota\" placeholder=\"20\" value=\"")
                .append(e(jadwalKuota)).append("\"></div></div>");
        sb.append("<button class=\"btn-primary\" type=\"submit\" name=\"aksi\" value=\"simpan-jadwal\">")
                .append(jadwalEdit == 0 ? "Tambah jadwal baru" : "Simpan perubahan").append("</button>");
        if (jadwalEdit != 0) {
            sb.append("<button class=\"btn-outline\" type=\"submit\" name=\"aksi\" value=\"batal-jadwal\">Batal edit</button>");
        }
        sb.append("<p class=\"hint\">Jam yang bertumpuk ditolak. Jadwal yang masih punya antrean hari ini tidak dapat disembunyikan.</p></form></section>");
        sb.append("<section class=\"block wide\"><div class=\"card\"><div class=\"table-wrap\"><table class=\"crud-table\"><thead><tr>");
        sb.append("<th>Hari</th><th>Jam</th><th>Kuota</th><th>Aksi</th></tr></thead><tbody>");
        List<Jadwal> daftar = toko.jadwalSaya();
        for (Jadwal jadwal : daftar) {
            sb.append("<tr><td><strong>").append(e(jadwal.hari)).append("</strong></td><td>")
                    .append(e(VisitRules.jamPendek(jadwal.jam_mulai))).append("–").append(e(VisitRules.jamPendek(jadwal.jam_selesai)));
            sb.append("</td><td>").append(jadwal.kuota_maksimal).append("</td><td class=\"aksi\">");
            sb.append(formBuka("inline", "isi-jadwal")).append(sembunyi("id_jadwal", Long.toString(jadwal.id_jadwal)));
            sb.append("<button class=\"btn-small\" type=\"submit\">Ubah</button></form>");
            sb.append("<form class=\"inline\" method=\"post\" action=\"/tindakan\" onsubmit=\"return confirm('Sembunyikan jadwal ")
                    .append(js(jadwal.hari)).append("? Pasien tidak bisa mendaftar pada jam ini lagi.')\">");
            sb.append(sembunyi("sesi", token)).append(sembunyi("aksi", "sembunyi-jadwal"))
                    .append(sembunyi("id_jadwal", Long.toString(jadwal.id_jadwal)));
            sb.append("<button class=\"btn-small danger\" type=\"submit\">Sembunyi</button></form></td></tr>");
        }
        sb.append("</tbody></table></div>");
        if (daftar.isEmpty()) {
            sb.append("<p class=\"hint\">Belum ada jadwal aktif.</p>");
        }
        sb.append("</div></section>");
        return sb.toString();
    }

    private String pesan(boolean lebar) {
        List<VisitStore.KabarTampil> daftar = toko.notifikasiUser(null);
        int belum = 0;
        for (VisitStore.KabarTampil item : daftar) {
            if (!item.dibaca) {
                belum++;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"block").append(lebar ? " wide" : "").append("\"><div class=\"section-title\">Notifikasi aplikasi<span>");
        sb.append(belum > 0 ? belum + " baru" : Integer.toString(daftar.size())).append("</span></div><div class=\"card\">");
        if (daftar.isEmpty()) {
            sb.append("<p class=\"hint\">Belum ada notifikasi.</p>");
        } else {
            sb.append(daftarKabar(daftar));
        }
        sb.append("<p class=\"hint\">Semua pemberitahuan kunjungan masuk ke aplikasi ini. Isi tidak menyebut nama obat atau diagnosis.</p>");
        if (belum > 0) {
            sb.append(formBuka("", "baca"));
            sb.append("<button class=\"btn-outline\" type=\"submit\">Tandai sudah dibaca</button></form>");
        }
        sb.append("<button class=\"btn-outline\" type=\"button\" id=\"izin-notif\">Izinkan notifikasi perangkat</button></div></section>");
        return sb.toString();
    }

    private String panel() {
        List<VisitStore.KabarTampil> daftar = toko.notifikasiUser(null);
        StringBuilder sb = new StringBuilder("<div class=\"notify-panel\" role=\"dialog\" aria-label=\"Notifikasi aplikasi\"><h3>Notifikasi aplikasi</h3>");
        if (daftar.isEmpty()) {
            sb.append("<p class=\"hint\">Belum ada notifikasi di akun ini. Pembaruan kunjungan masuk ke aplikasi pasien.</p>");
        } else {
            sb.append(daftarKabar(daftar));
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String daftarKabar(List<VisitStore.KabarTampil> daftar) {
        StringBuilder sb = new StringBuilder();
        for (VisitStore.KabarTampil item : daftar) {
            sb.append("<div class=\"med-item").append(item.dibaca ? "" : " unread").append("\"><div><h4>")
                    .append(e(VisitRules.judulNotifikasi(item.baris.jenis_notifikasi, item.baris.pesan))).append("</h4><p>")
                    .append(e(item.baris.pesan)).append("</p><p>")
                    .append(e(waktu(kosong(item.baris.waktu_dikirim) ? item.baris.created_at : item.baris.waktu_dikirim)))
                    .append("</p></div><span class=\"pill\">").append(item.dibaca ? "Dibaca" : "Baru").append("</span></div>");
        }
        return sb.toString();
    }

    private String toastHtml() {
        if (toasts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"toast-stack\">");
        for (String[] item : toasts) {
            sb.append("<div class=\"toast\" role=\"status\"><div class=\"toast-top\"><h4>").append(e(item[0]));
            sb.append("</h4><button type=\"button\" aria-label=\"Tutup\" onclick=\"this.closest('.toast').remove()\">×</button></div><p>")
                    .append(e(item[1])).append("</p></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String nav(String role) {
        String utama = switch (role) {
            case "pasien" -> "Data Saya";
            case "dokter" -> "Form Medis";
            case "farmasi" -> "Farmasi";
            default -> "Kontrol";
        };
        String aktifTab = "pasien".equals(role) && "data".equals(tab) ? "main" : tab;
        StringBuilder sb = new StringBuilder("<nav class=\"bottom-nav\">");
        sb.append(navItem("main", "📝", utama, aktifTab));
        if (!"pasien".equals(role)) {
            sb.append(navItem("data", "🛠️", "Data", aktifTab));
        }
        sb.append(navItem("alur", "📋", "Alur", aktifTab));
        sb.append(navItem("resep", "💊", "E-Resep", aktifTab));
        sb.append("</nav>");
        return sb.toString();
    }

    private String navItem(String id, String ikon, String label, String aktifTab) {
        return formBuka("inline", "tab") + sembunyi("tab", id)
                + "<button class=\"nav-item" + (id.equals(aktifTab) ? " active" : "")
                + "\" type=\"submit\"><span>" + ikon + "</span><span>" + e(label) + "</span></button></form>";
    }

    private String dokumen(String isi, String kabar) {
        return "<!DOCTYPE html>\n<html lang=\"id\">\n<head>\n"
                + "<meta charset=\"utf-8\">\n<title>"
                + ("/daftar".equals(halaman) ? "Daftar pasien — MediFlow" : "MediFlow") + "</title>\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, viewport-fit=cover\">\n"
                + "<meta name=\"description\" content=\"MediFlow: antrean poliklinik, rekam medis, dan e-resep rumah sakit.\">\n"
                + "<meta name=\"theme-color\" content=\"#059669\">\n"
                + "<meta name=\"mobile-web-app-capable\" content=\"yes\">\n"
                + "<link rel=\"icon\" href=\"/assets/icons/24x24.png\" type=\"image/png\">\n"
                + "<link rel=\"manifest\" href=\"/manifest.json\">\n"
                + "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n"
                + "<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap\" rel=\"stylesheet\">\n"
                + "<link rel=\"stylesheet\" href=\"/mediflow.css\">\n</head>\n<body>\n"
                + isi + "\n" + kabar + "\n<script src=\"/application.js\"></script>\n</body>\n</html>\n";
    }

    private String kabarJson(Pengguna aktor) {
        if (aktor == null || !"pasien".equals(aktor.role) || toasts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<script type=\"application/json\" id=\"kabar-baru\">[");
        for (int i = 0; i < toasts.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"judul\":\"").append(json(toasts.get(i)[0])).append("\",\"isi\":\"")
                    .append(json(toasts.get(i)[1])).append("\"}");
        }
        return sb.append("]</script>").toString();
    }

    private String sumberTeks() {
        if ("mysql".equals(toko.sumber())) {
            return "Akun, poli, jadwal, dan stok obat mengikuti database medicflow_db.";
        }
        return "Database belum terjangkau. Data demo dipakai selama server ini berjalan.";
    }

    private String teksAlergi(VisitStore.Kunjungan kunjungan) {
        String catatanPasien = kunjungan.pendaftaran.catatan_pasien == null ? "" : kunjungan.pendaftaran.catatan_pasien.trim();
        String keluhanTeks = kunjungan.pemeriksaan.keluhan == null ? "" : kunjungan.pemeriksaan.keluhan.trim();
        if (catatanPasien.isEmpty() || catatanPasien.equals(keluhanTeks)) {
            return "Tidak ada alergi obat";
        }
        return catatanPasien;
    }

    private String waktu(String iso) {
        if (kosong(iso)) {
            return "";
        }
        return tgl(iso) + " " + jam(iso);
    }

    private Akun akun(String nilai) {
        for (Akun item : AKUN) {
            if (item.peran.equals(nilai)) {
                return item;
            }
        }
        return AKUN.get(0);
    }

    private static boolean adaSlot(List<VisitStore.Slot> slot, long id) {
        for (VisitStore.Slot item : slot) {
            if (item.jadwal.id_jadwal == id) {
                return true;
            }
        }
        return false;
    }

    private String formBuka(String kelas, String aksi) {
        StringBuilder sb = new StringBuilder("<form");
        if ("buka".equals(aksi)) {
            sb.append(" id=\"buka-panel\"");
        }
        if (!kelas.isEmpty()) {
            sb.append(" class=\"").append(kelas).append("\"");
        }
        if ("buka".equals(aksi)) {
            sb.append(" hidden");
        }
        sb.append(" method=\"post\" action=\"/tindakan\">").append(sembunyi("sesi", token)).append(sembunyi("aksi", aksi));
        return sb.toString();
    }

    private static String info(String kecil, String kuat) {
        return "<div class=\"info-item\"><small>" + e(kecil) + "</small><strong>" + e(kuat) + "</strong></div>";
    }

    private static String opsi(String nilai, String sekarang, String label) {
        return "<option value=\"" + e(nilai) + "\"" + (nilai.equals(sekarang) ? " selected" : "") + ">" + e(label) + "</option>";
    }

    private static String sembunyi(String nama, String nilai) {
        return "<input type=\"hidden\" name=\"" + e(nama) + "\" value=\"" + e(nilai) + "\">";
    }

    private static String mati(boolean ya) {
        return ya ? " disabled" : "";
    }

    static String e(String nilai) {
        if (nilai == null) {
            return "";
        }
        return nilai.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String js(String nilai) {
        return teks(nilai).replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", " ").replace("<", "");
    }

    private static String json(String nilai) {
        StringBuilder sb = new StringBuilder();
        String teks = teks(nilai);
        for (int i = 0; i < teks.length(); i++) {
            char c = teks.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '<' -> sb.append("\\u003c");
                case '>' -> sb.append("\\u003e");
                case '&' -> sb.append("\\u0026");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String teks(String nilai) {
        return nilai == null ? "" : nilai;
    }

    private static boolean kosong(String nilai) {
        return nilai == null || nilai.isBlank();
    }

    private static boolean sama(Long a, Long b) {
        return a == null ? b == null : a.equals(b);
    }

    private static long angka(String nilai, long cadangan) {
        if (nilai == null || nilai.isBlank()) {
            return cadangan;
        }
        try {
            return Long.parseLong(nilai.trim());
        } catch (NumberFormatException error) {
            return cadangan;
        }
    }

    private static double angkaDouble(String nilai) {
        if (nilai == null || nilai.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(nilai.trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static String angkaForm(double nilai) {
        if (!Double.isInfinite(nilai) && nilai == Math.rint(nilai)) {
            return Long.toString(Math.round(nilai));
        }
        return Double.toString(nilai);
    }

    private static String jam(String iso) {
        try {
            return ScreenText.formatJam(iso);
        } catch (RuntimeException error) {
            return "-";
        }
    }

    private static String tgl(String nilai) {
        try {
            return ScreenText.formatTanggal(nilai);
        } catch (RuntimeException error) {
            return "-";
        }
    }
}