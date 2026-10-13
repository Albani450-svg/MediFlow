package mediflow;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Pemeriksaan alur yang sama dengan data uji di memori. */
public final class UjiAlur {
    private UjiAlur() {}

    public static void main(String[] args) {
        jalan();
        System.out.println("alur-ok");
    }

    static void jalan() {
        sama(Aturan.estimasiJamMasuk("2026-10-06", "08:00", 5, 15), "2026-10-06T02:00:00.000Z");
        sama(Aturan.estimasiFarmasiMenit(2, List.of("Jadi", "Racikan")), 40);
        sama(Aturan.statusKetersediaan(0, 10), "kosong");
        sama(Aturan.statusKetersediaan(4, 10), "sebagian");
        sama(Aturan.statusKetersediaan(10, 10), "tersedia");
        benar(!Aturan.bolehTebusRs(List.of("tersedia", "kosong")));
        benar(Aturan.bolehTebusRs(List.of("tersedia", "tersedia")));

        String pesanStok = Aturan.teksNotifikasi("QR Resep", new Aturan.KonteksPesan().nomor(1).poli("Poli Umum"));
        benar(!Pattern.compile("amoxicillin|faringitis", Pattern.CASE_INSENSITIVE).matcher(pesanStok).find());

        Tampilan.Panduan arahPasien = Tampilan.panduan("pasien", new Tampilan.Konteks(
                4, true, true, true, false, null, true, false, false));
        sama(arahPasien.faseId(), 2);
        sama(arahPasien.status(), "Sedang diperiksa");
        benar(!Pattern.compile("acc|whatsapp", Pattern.CASE_INSENSITIVE).matcher(arahPasien.tindakan()).find());
        Tampilan.Panduan arahDokter = Tampilan.panduan("dokter", new Tampilan.Konteks(
                5, true, true, true, false, null, true, false, false));
        benar(arahDokter.milikAnda());
        sama(arahDokter.penghalang(), "Tambahkan minimal satu obat sebelum mengirim resep.");

        Toko toko = Toko.memori();
        Basis awal = toko.snapshot();
        sama(awal.poliklinik.size(), 4);
        sama(awal.obat.size(), 5);
        sama(hitung(awal.obat, obat -> obat.is_active), 4);
        sama(awal.dokter.size(), 4);
        sama(hitung(awal.jadwal_dokter, row -> row.id_dokter == 1 && row.is_active), 7);
        sama(awal.pasien.get(0).nik, "3471001234567890");
        sama(awal.pasien.get(0).nama_lengkap, "Budi Santoso Edit");
        benar(awal.users.stream().anyMatch(user -> "farmasi".equals(user.role) && "farmasi01".equals(user.username)));
        sama(obatKode(awal, "OBT003").stok_rs, 4);
        benar(!obatKode(awal, "OBT005").is_active);

        Long demo = null;
        for (Toko.Kunjungan row : toko.kunjungan()) {
            if (row.idPasien == 2 && row.tahap == 4) {
                demo = row.pendaftaran.id_pendaftaran;
                break;
            }
        }
        if (demo == null) {
            throw new AssertionError("Kunjungan demo tidak ada");
        }
        Toko.Kunjungan siti = null;
        for (Toko.Kunjungan row : toko.kunjungan()) {
            if ("Siti Rahayu".equals(row.pasienNama)) {
                siti = row;
                break;
            }
        }
        sama(siti.tahap, 9);
        sama(siti.pinKeluarga, "482913");
        sama(siti.resep.kode_qr_unik, "MF-SITI-SIAP");
        benar(toko.kunjungan().stream().anyMatch(row -> row.tahap == 7 && "Dewi Lestari".equals(row.pasienNama)));

        benar(!toko.login("budi", "bukan-sandi", null).ok);
        pastikan(toko.login("budi", "pasien123", null));
        Toko.Kunjungan awalKunjungan = toko.kunjunganById(demo);
        sama(awalKunjungan.tahap, 4);
        sama(awalKunjungan.poliNama, "Poli Umum");
        benar(!toko.daftar(awalKunjungan.jadwal.id_jadwal, "Batuk", null).ok);

        toko.logout();
        pastikan(toko.login("farmasi01", "apotek123", null));
        pastikan(toko.sesuaikanStok(1, 0, "Stok fisik habis"));
        toko.logout();
        pastikan(toko.login("dokter01", "dokter123", null));
        pastikan(toko.simpanPemeriksaan(demo, "J02.9 - Faringitis Akut (Radang Tenggorokan)",
                "Faring hiperemis. Habiskan obat sesuai aturan.", "Rawat Jalan + Terapi Obat"));
        pastikan(toko.tambahObat(demo, 1, 10, "3x1 Sehari (Sesudah Makan)"));
        pastikan(toko.majuTahap(demo));
        pastikan(toko.majuTahap(demo));
        toko.logout();
        pastikan(toko.login("budi", "pasien123", null));
        benar(!toko.pilihTebus(demo, "apotek_rs").ok);
        pastikan(toko.pilihTebus(demo, "apotek_luar"));
        sama(toko.kunjunganById(demo).resep.pilihan_penebusan, "Apotek Luar");

        toko.resetDemo();
        toko.logout();
        pastikan(toko.login("dokter01", "dokter123", null));
        pastikan(toko.simpanPemeriksaan(demo, "J02.9 - Faringitis Akut (Radang Tenggorokan)",
                "Faring hiperemis. Habiskan obat sesuai aturan.", "Rawat Jalan + Terapi Obat"));
        pastikan(toko.tambahObat(demo, 1, 10, "3x1 Sehari (Sesudah Makan)"));
        pastikan(toko.majuTahap(demo));
        pastikan(toko.majuTahap(demo));
        benar(toko.kunjunganById(demo).pemeriksaan.waktu_selesai != null);

        toko.logout();
        pastikan(toko.login("budi", "pasien123", null));
        pastikan(toko.pilihTebus(demo, "apotek_rs"));
        sama(toko.kunjunganById(demo).tahap, 6);

        toko.logout();
        pastikan(toko.login("admin", "admin123", null));
        pastikan(toko.majuTahap(demo));
        sama(toko.kunjunganById(demo).resep.status_resep, "Antrean Farmasi");

        toko.logout();
        pastikan(toko.login("farmasi01", "apotek123", null));
        pastikan(toko.majuTahap(demo));
        pastikan(toko.majuTahap(demo));
        Toko.Kunjungan siap = toko.kunjunganById(demo);
        if (siap.pinKeluarga == null) {
            throw new AssertionError("PIN keluarga belum terbit");
        }
        sama(siap.tahap, 9);
        sama(siap.resep.status_resep, "Siap Diambil");
        String pinSalah = "482913".equals(siap.pinKeluarga) ? "000000" : ( "000000".equals(siap.pinKeluarga) ? "000001" : "000000");
        benar(!toko.serahkan(demo, siap.resep.kode_qr_unik, "keluarga", pinSalah).ok);
        pastikan(toko.serahkan(demo, siap.resep.kode_qr_unik, "pasien", ""));
        benar(!toko.serahkan(demo, siap.resep.kode_qr_unik, "pasien", "").ok);

        Toko.Kunjungan selesai = toko.kunjunganById(demo);
        sama(selesai.tahap, 10);
        sama(selesai.resep.status_resep, "Selesai Diambil");
        benar(selesai.qrDipakai);
        benar(toko.snapshot().pasien.get(0).terdaftar_satusehat);
        sama(cariObat(toko, 1).stok_rs, 90);
        benar(toko.snapshot().mutasi_stok.stream().anyMatch(log -> "Keluar".equals(log.jenis_mutasi)));
        benar(toko.snapshot().pengambilan_obat.stream().anyMatch(row -> "Berhasil".equals(row.status)));
        benar(!toko.snapshot().log_aktivitas.isEmpty());

        StringBuilder isi = new StringBuilder();
        for (Kabar item : toko.snapshot().notifikasi) {
            isi.append(item.pesan).append('\n');
        }
        benar(!Pattern.compile("amoxicillin|paracetamol|faringitis|vitamin c", Pattern.CASE_INSENSITIVE).matcher(isi).find());
        benar(!Pattern.compile("whatsapp", Pattern.CASE_INSENSITIVE).matcher(isi).find());
        benar(isi.toString().contains("Poli Umum"));
        benar(toko.snapshot().notifikasi.stream().allMatch(item -> "PWA".equals(item.channel)));

        toko.resetDemo();
        toko.logout();
        benar(!toko.login("budi", "pasien123", "12345").ok);
        pastikan(toko.login("budi", "pasien123", "081298765432"));
        String telepon = null;
        for (Pasien row : toko.snapshot().pasien) {
            if (row.id_user == 4) {
                telepon = row.no_telepon;
            }
        }
        sama(telepon, "081298765432");
        pastikan(toko.simpanCatatan(demo, "Alergi penisilin"));
        pastikan(toko.simpanKeluhan(demo, "Batuk kering"));
        Toko.Kunjungan catatan = toko.kunjunganById(demo);
        sama(catatan.pemeriksaan.keluhan, "Batuk kering");
        sama(catatan.pendaftaran.catatan_pasien, "Alergi penisilin");
        benar(!toko.simpanObat(null, "OBT-X", "Vitamin Tes", 3, 1000, "Tablet", "Jadi", false).ok);

        toko.logout();
        pastikan(toko.login("dokter01", "dokter123", null));
        long idHariIni = toko.kunjunganById(demo).jadwal.id_jadwal;
        benar(!toko.nonaktifkanJadwal(idHariIni).ok);
        long idJadwal = pastikan(toko.simpanJadwal(null, "Kamis", "13:00", "15:00", 12));
        Jadwal tersimpan = null;
        for (Jadwal row : toko.snapshot().jadwal_dokter) {
            if (row.id_jadwal == idJadwal) {
                tersimpan = row;
            }
        }
        sama(tersimpan.kuota_maksimal, 12);
        benar(!toko.simpanJadwal(null, "Senin", "09:00", "10:00", 10).ok);
        pastikan(toko.nonaktifkanJadwal(idJadwal));
        benar(toko.jadwalSaya().stream().noneMatch(row -> row.id_jadwal == idJadwal));

        toko.logout();
        pastikan(toko.login("admin", "admin123", null));
        benar(!toko.simpanJadwal(null, "Jumat", "08:00", "10:00", 5).ok);

        toko.logout();
        pastikan(toko.login("farmasi01", "apotek123", null));
        long idObat = pastikan(toko.simpanObat(null, "OBT-TEST", "Vitamin Tes", 5, 2000, "Tablet", "Jadi", true));
        final long idObatFinal = idObat;
        benar(toko.snapshot().mutasi_stok.stream().anyMatch(row -> row.id_obat == idObatFinal
                && "Masuk".equals(row.jenis_mutasi) && row.stok_sesudah == 5));
        benar(!toko.simpanObat(null, "OBT001", "Kode bentrok", 1, 1, "Tablet", "Jadi", false).ok);
        pastikan(toko.nonaktifkanObat(idObat));
        benar(toko.obatAktif().stream().noneMatch(row -> row.id_obat == idObatFinal));
        benar(toko.snapshot().notifikasi.stream().allMatch(item -> "PWA".equals(item.channel)));
        StringBuilder akhir = new StringBuilder();
        for (Kabar item : toko.snapshot().notifikasi) {
            akhir.append(item.pesan).append('\n');
        }
        benar(!Pattern.compile("whatsapp", Pattern.CASE_INSENSITIVE).matcher(akhir).find());
        toko.resetDemo();
        toko.logout();
    }

    private static <T> T pastikan(Toko.Hasil<T> hasil) {
        if (!hasil.ok) {
            throw new AssertionError(hasil.error);
        }
        return hasil.data;
    }

    private static void sama(Object a, Object b) {
        if (!Objects.equals(a, b)) {
            throw new AssertionError(a + " != " + b);
        }
    }

    private static void benar(boolean nilai) {
        if (!nilai) {
            throw new AssertionError("harapan tidak terpenuhi");
        }
    }

    private static Obat obatKode(Basis basis, String kode) {
        for (Obat obat : basis.obat) {
            if (kode.equals(obat.kode_kemenkes)) {
                return obat;
            }
        }
        throw new AssertionError(kode);
    }

    private static Obat cariObat(Toko toko, long id) {
        for (Obat obat : toko.snapshot().obat) {
            if (obat.id_obat == id) {
                return obat;
            }
        }
        throw new AssertionError(id);
    }

    private interface Cocok<T> {
        boolean uji(T nilai);
    }

    private static <T> int hitung(List<T> baris, Cocok<T> cocok) {
        int jumlah = 0;
        for (T item : baris) {
            if (cocok.uji(item)) {
                jumlah++;
            }
        }
        return jumlah;
    }
}
