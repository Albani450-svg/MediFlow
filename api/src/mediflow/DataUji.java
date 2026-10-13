package mediflow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data uji. Akun layar masuk: budi, dokter01, admin, farmasi01. */
final class DataUji {
    private static final String AWAL = "2026-10-02T20:53:44.000Z";
    private static final String PASIEN_DIBUAT = "2026-10-07T03:46:17.000Z";
    private static final String HASH_PASIEN = "b3cb1bf1350e826eafc2250c570837e1ef1a0e8d6fece2c3478740670ce8fed1";
    private static final String HASH_DOKTER = "b3959dee9b178b030c2b8373da55a04ab3adb318edeb178953ff8b77301a360a";
    private static final String HASH_ADMIN = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
    private static final String HASH_FARMASI = "4561c9fdf633bb9b5a6d72ec03f9568d683f3fa2cb3a8e8cd1c10c967f902553";
    private static final String ATURAN = "3x1 Sehari (Sesudah Makan)";
    private static final String TINDAKAN = "Rawat Jalan + Terapi Obat";

    private DataUji() {}

    static Basis buat() {
        return buat(ZonedDateTime.now(Aturan.ZONA));
    }

    static Basis buat(ZonedDateTime sekarang) {
        String tanggal = Aturan.tanggalIso(sekarang);
        String kemarin3 = geser(sekarang, -3);
        String kemarin10 = geser(sekarang, -10);
        List<Jadwal> jadwal = buatJadwal();
        Jadwal hariIni = jadwalPada(jadwal, 1, tanggal);
        String kadaluarsaSiti = Aturan.iso(sekarang.toInstant().plusSeconds(20 * 60 * 60));
        String kadaluarsaLama = saat(kemarin3, "18:00");

        Basis db = new Basis();
        db.users.add(akun(1, "admin", HASH_ADMIN, "admin", "Administrator", "081234567890", AWAL));
        db.users.add(akun(2, "dokter01", HASH_DOKTER, "dokter", "Dr. Ahmad", "081234567891", AWAL));
        db.users.add(akun(3, "farmasi01", HASH_FARMASI, "farmasi", "Petugas Farmasi", "081234567892", AWAL));
        db.users.add(akun(4, "budi", HASH_PASIEN, "pasien", "Budi Santoso Edit", "089876543210", PASIEN_DIBUAT));
        db.users.add(akun(5, "dokter02", HASH_DOKTER, "dokter", "Dr. Sari Wijaya", "081234567893", AWAL));
        db.users.add(akun(6, "dokter03", HASH_DOKTER, "dokter", "Dr. Bima Putra", "081234567894", AWAL));
        db.users.add(akun(7, "dokter04", HASH_DOKTER, "dokter", "Dr. Lina Kusuma", "081234567895", AWAL));
        db.pasien.add(pasien(2, 4, "3471001234567890", "0001234567890", "Budi Santoso Edit", "2000-01-15", "L",
                "Sleman, Yogyakarta", "089876543210", true));

        Profil[] tambahan = {
                profil(3, 8, "siti", "Siti Rahayu", "3471012345678901", "0001234567891", "1992-03-14", "P", "Bantul, Yogyakarta", "081300000001"),
                profil(4, 9, "andi", "Andi Wijaya", "3471012345678902", "0001234567892", "1988-11-02", "L", "Kulon Progo, Yogyakarta", "081300000002"),
                profil(5, 10, "rina", "Rina Kusuma", "3471012345678903", "0001234567893", "1998-07-21", "P", "Kota Yogyakarta", "081300000003"),
                profil(6, 11, "joko", "Joko Susilo", "3471012345678904", "0001234567894", "1975-05-09", "L", "Sleman, Yogyakarta", "081300000004"),
                profil(7, 12, "maya", "Maya Anggraini", "3471012345678905", "0001234567895", "2001-12-30", "P", "Bantul, Yogyakarta", "081300000005"),
                profil(8, 13, "dewi", "Dewi Lestari", "3471012345678906", "0001234567896", "1985-02-18", "P", "Gunungkidul, Yogyakarta", "081300000006"),
                profil(9, 14, "agus", "Agus Pratama", "3471012345678907", "0001234567897", "1990-09-25", "L", "Sleman, Yogyakarta", "081300000007"),
                profil(10, 15, "hendra", "Hendra Saputra", "3471012345678908", null, "2003-06-11", "L", "Kota Yogyakarta", "081300000008")
        };
        for (Profil row : tambahan) {
            db.users.add(akun(row.idUser, row.username, HASH_PASIEN, "pasien", row.nama, row.telp, PASIEN_DIBUAT));
            db.pasien.add(pasien(row.idPasien, row.idUser, row.nik, row.bpjs, row.nama, row.lahir, row.jk, row.alamat, row.telp, false));
        }
        db.dokter.add(dokter(1, 2, 1, "Dr. Ahmad", "Dokter Umum", 15));
        db.dokter.add(dokter(2, 5, 2, "Dr. Sari Wijaya", "Dokter Gigi", 20));
        db.dokter.add(dokter(3, 6, 3, "Dr. Bima Putra", "Dokter Anak", 20));
        db.dokter.add(dokter(4, 7, 4, "Dr. Lina Kusuma", "Dokter Mata", 15));
        db.jadwal_dokter.addAll(jadwal);
        db.obat.addAll(buatObat());
        db.poliklinik.add(poli(1, "Poli Umum", "Pelayanan kesehatan umum"));
        db.poliklinik.add(poli(2, "Poli Gigi", "Pelayanan kesehatan gigi dan mulut"));
        db.poliklinik.add(poli(3, "Poli Anak", "Pelayanan kesehatan anak"));
        db.poliklinik.add(poli(4, "Poli Mata", "Pelayanan kesehatan mata"));

        String e1 = estimasi(tanggal, hariIni, 1);
        masuk(db, daftar(15, 2, hariIni.id_jadwal, tanggal, 1, saat(tanggal, "07:40"), e1, "Masuk Ruangan",
                "Tidak ada alergi obat", saat(tanggal, "06:30"), saat(tanggal, "08:05")),
                periksa(15, 15, 1, "Demam naik turun disertai nyeri menelan dan batuk kering.", saat(tanggal, "08:05"),
                        "120/80", 37.8, 64, 170, "", "", null, null));
        db.notifikasi.add(pesan(1, 2, 15, "Konfirmasi Pendaftaran", "089876543210",
                new Aturan.KonteksPesan().nomor(1).poli("Poli Umum").tanggal(tanggal).jam(Tampilan.formatJam(e1)),
                saat(tanggal, "06:30")));
        db.notifikasi.add(pesan(2, 2, 15, "Pengingat Kunjungan", "089876543210",
                new Aturan.KonteksPesan().nomor(1).poli("Poli Umum"), saat(tanggal, "07:00")));

        masuk(db, daftar(14, 10, hariIni.id_jadwal, tanggal, 2, null, estimasi(tanggal, hariIni, 2), "Menunggu", null,
                saat(tanggal, "06:40"), saat(tanggal, "06:40")),
                periksa(14, 14, 1, "Pusing sejak semalam, belum minum obat.", saat(tanggal, "06:40"),
                        null, null, null, null, "", "", null, null));
        masuk(db, daftar(13, 4, hariIni.id_jadwal, tanggal, 3, saat(tanggal, "07:50"), estimasi(tanggal, hariIni, 3), "Menunggu",
                "Alergi debu, bukan alergi obat.", saat(tanggal, "06:45"), saat(tanggal, "07:50")),
                periksa(13, 13, 1, "Batuk berdahak selama tiga hari.", saat(tanggal, "06:45"),
                        null, null, null, null, "", "", null, null));
        masuk(db, daftar(12, 5, hariIni.id_jadwal, tanggal, 4, saat(tanggal, "07:55"), estimasi(tanggal, hariIni, 4), "Dipanggil",
                "Alergi penisilin.", saat(tanggal, "06:50"), saat(tanggal, "08:00")),
                periksa(12, 12, 1, "Nyeri ulu hati setelah makan pedas.", saat(tanggal, "06:50"),
                        "110/70", 36.6, 52, 158, "", "", null, null));
        masuk(db, daftar(11, 6, hariIni.id_jadwal, tanggal, 5, saat(tanggal, "08:00"), estimasi(tanggal, hariIni, 5),
                "Selesai Pemeriksaan", null, saat(tanggal, "06:55"), saat(tanggal, "08:30")),
                periksa(11, 11, 1, "Perut kembung dan mual sejak dua hari.", saat(tanggal, "08:10"),
                        "130/85", 36.8, 70, 168, "K29.7 - Gastritis / Dispepsia Akut", TINDAKAN,
                        "Hindari makanan pedas dan makan teratur.", null));
        db.resep.add(resep(1, 11, "MF-JOKO-DRAF", saat(tanggal, "08:35"), "Belum Memilih", "Menunggu Pilihan", null, null));
        db.detail_resep.add(item(1, 1, 1, 10, ATURAN, null, saat(tanggal, "08:35")));

        masuk(db, daftar(10, 7, hariIni.id_jadwal, tanggal, 6, saat(tanggal, "08:10"), estimasi(tanggal, hariIni, 6),
                "Selesai Pemeriksaan", null, saat(tanggal, "07:00"), saat(tanggal, "09:00")),
                periksa(10, 10, 1, "Diare sejak kemarin malam.", saat(tanggal, "08:20"),
                        "100/70", 37.2, 48, 155, "A09 - Gastroenteritis dan Diare Infeksi", TINDAKAN,
                        "Banyak minum air.", saat(tanggal, "08:50")));
        db.resep.add(resep(2, 10, "MF-MAYA-PILIH", saat(tanggal, "08:50"), "Apotek RS", "Menunggu Pilihan", saat(tanggal, "09:20"), null));
        db.detail_resep.add(item(2, 2, 1, 2, ATURAN, null, saat(tanggal, "08:50")));
        db.notifikasi.add(pesan(3, 7, 10, "QR Resep", "081300000005", new Aturan.KonteksPesan().nomor(6), saat(tanggal, "08:50")));

        masuk(db, daftar(9, 8, hariIni.id_jadwal, tanggal, 7, saat(tanggal, "08:15"), estimasi(tanggal, hariIni, 7),
                "Selesai Pemeriksaan", "Tidak ada alergi obat", saat(tanggal, "07:05"), saat(tanggal, "09:10")),
                periksa(9, 9, 1, "Demam dan pilek sejak kemarin.", saat(tanggal, "08:30"),
                        "118/76", 38.1, 55, 160, "J06.9 - Infeksi Saluran Napas Atas (ISPA)", TINDAKAN,
                        "Istirahat dan habiskan obat.", saat(tanggal, "09:00")));
        db.resep.add(resep(3, 9, "MF-DEWI-ANTRE", saat(tanggal, "09:00"), "Apotek RS", "Antrean Farmasi", saat(tanggal, "09:30"), null));
        db.detail_resep.add(item(3, 3, 2, 2, ATURAN, null, saat(tanggal, "09:00")));

        masuk(db, daftar(8, 9, hariIni.id_jadwal, tanggal, 8, saat(tanggal, "08:20"), estimasi(tanggal, hariIni, 8),
                "Selesai Pemeriksaan", null, saat(tanggal, "07:10"), saat(tanggal, "09:20")),
                periksa(8, 8, 1, "Batuk berdahak dan tenggorokan gatal.", saat(tanggal, "08:40"),
                        "125/80", 36.9, 62, 165, "J06.9 - Infeksi Saluran Napas Atas (ISPA)", TINDAKAN,
                        "Racikan diminum setelah makan.", saat(tanggal, "09:05")));
        db.resep.add(resep(4, 8, "MF-AGUS-RACIK", saat(tanggal, "09:05"), "Apotek RS", "Sedang Diracik", saat(tanggal, "09:40"), null));
        db.detail_resep.add(item(4, 4, 4, 1, ATURAN, "Racik sesuai aturan farmasi", saat(tanggal, "09:05")));

        masuk(db, daftar(7, 3, hariIni.id_jadwal, tanggal, 9, saat(tanggal, "08:25"), estimasi(tanggal, hariIni, 9),
                "Selesai Pemeriksaan", null, saat(tanggal, "07:15"), saat(tanggal, "09:30")),
                periksa(7, 7, 1, "Pusing dan tengkuk terasa berat.", saat(tanggal, "08:45"),
                        "150/95", 36.5, 58, 157, "I10 - Hipertensi Esensial Primer", TINDAKAN,
                        "Kontrol tekanan darah minggu depan.", saat(tanggal, "09:15")));
        db.resep.add(resep(5, 7, "MF-SITI-SIAP", saat(tanggal, "09:15"), "Apotek RS", "Siap Diambil", saat(tanggal, "09:45"), kadaluarsaSiti));
        db.detail_resep.add(item(5, 5, 1, 1, "1x1 Sehari (Malam Sebelum Tidur)", null, saat(tanggal, "09:15")));
        db.otp_verifikasi.add(otp(1, 8, "482913", "081300000001", kadaluarsaSiti, null, 0, saat(tanggal, "09:15")));
        db.notifikasi.add(pesan(4, 3, 7, "Obat Siap", "081300000001", new Aturan.KonteksPesan().nomor(9), saat(tanggal, "09:15")));

        Jadwal lama = jadwalPada(jadwal, 1, kemarin3);
        Jadwal luar = jadwalPada(jadwal, 1, kemarin10);
        masuk(db, daftar(6, 2, lama.id_jadwal, kemarin3, 1, saat(kemarin3, "07:45"),
                Aturan.estimasiJamMasuk(kemarin3, lama.jam_mulai, 1, 15), "Selesai Pemeriksaan", null,
                saat(kemarin3, "06:20"), saat(kemarin3, "10:00")),
                periksa(6, 6, 1, "Batuk dan demam tiga hari.", saat(kemarin3, "08:00"),
                        "120/78", 37.4, 64, 170, "J06.9 - Infeksi Saluran Napas Atas (ISPA)", TINDAKAN,
                        "Kontrol bila demam berlanjut.", saat(kemarin3, "08:40")));
        db.resep.add(resep(6, 6, "MF-BUDI-LAMA", saat(kemarin3, "08:40"), "Apotek RS", "Selesai Diambil", saat(kemarin3, "09:10"), kadaluarsaLama));
        db.detail_resep.add(item(6, 6, 2, 6, ATURAN, null, saat(kemarin3, "08:40")));
        db.detail_resep.add(item(7, 6, 5, 10, "1x1 Sehari (Malam Sebelum Tidur)", null, saat(kemarin3, "08:40")));
        db.otp_verifikasi.add(otp(2, 4, "193847", "089876543210", kadaluarsaLama, saat(kemarin3, "10:00"), 1, saat(kemarin3, "09:10")));
        Ambil ambil = new Ambil();
        ambil.id_pengambilan = 1;
        ambil.id_resep = 6;
        ambil.id_user_petugas = 3;
        ambil.diambil_oleh = "Pasien";
        ambil.nama_pengambil = "Budi Santoso Edit";
        ambil.waktu_pengambilan = saat(kemarin3, "10:00");
        ambil.metode_verifikasi = "QR";
        ambil.status = "Berhasil";
        ambil.created_at = saat(kemarin3, "10:00");
        ambil.updated_at = saat(kemarin3, "10:00");
        db.pengambilan_obat.add(ambil);
        db.notifikasi.add(pesan(5, 2, 6, "Obat Siap", "089876543210",
                new Aturan.KonteksPesan().nomor(1).selesai(true), saat(kemarin3, "10:00")));

        masuk(db, daftar(5, 2, luar.id_jadwal, kemarin10, 1, saat(kemarin10, "08:00"),
                Aturan.estimasiJamMasuk(kemarin10, luar.jam_mulai, 1, 15), "Selesai Pemeriksaan", "Alergi debu.",
                saat(kemarin10, "07:00"), saat(kemarin10, "09:00")),
                periksa(5, 5, 1, "Gatal di kulit setelah debu.", saat(kemarin10, "08:10"),
                        "118/76", 36.6, 64, 170, "K29.7 - Gastritis / Dispepsia Akut", TINDAKAN,
                        "Tebus di apotek luar karena stok rumah sakit kurang.", saat(kemarin10, "08:40")));
        db.resep.add(resep(7, 5, "MF-BUDI-LUAR", saat(kemarin10, "08:40"), "Apotek Luar", "Menunggu Pilihan", null, null));
        db.detail_resep.add(item(8, 7, 3, 10, "1x1 Sehari (Malam Sebelum Tidur)", null, saat(kemarin10, "08:40")));

        db.mutasi_stok.add(mutasi(1, 1, 3, "Masuk", 100, 0, 100, "Stok awal", AWAL));
        db.mutasi_stok.add(mutasi(2, 2, 3, "Masuk", 80, 0, 80, "Stok awal", AWAL));
        db.mutasi_stok.add(mutasi(3, 3, 3, "Masuk", 4, 0, 4, "Stok awal di bawah minimum", AWAL));
        db.mutasi_stok.add(mutasi(4, 4, 3, "Masuk", 20, 0, 20, "Stok awal", AWAL));
        db.mutasi_stok.add(mutasi(5, 5, 3, "Masuk", 10, 0, 10, "Stok awal", AWAL));
        db.mutasi_stok.add(mutasi(6, 2, 3, "Keluar", 6, 80, 74, "Penyerahan resep 6", saat(kemarin3, "10:00")));
        db.mutasi_stok.add(mutasi(7, 5, 3, "Keluar", 10, 10, 0, "Penyerahan resep 6", saat(kemarin3, "10:00")));

        db.log_aktivitas.add(jejak(1, 4, "DAFTAR_POLI", "pendaftaran_poli", 15, "Antrean 1 Poli Umum", saat(tanggal, "06:30")));
        db.log_aktivitas.add(jejak(2, 1, "SIMPAN_VITAL", "pemeriksaan", 15, "Tanda vital diperbarui", saat(tanggal, "07:50")));
        db.log_aktivitas.add(jejak(3, 2, "SIMPAN_PEMERIKSAAN", "pemeriksaan", 11, "K29.7 - Gastritis / Dispepsia Akut", saat(tanggal, "08:30")));
        db.log_aktivitas.add(jejak(4, 12, "PILIH_TEBUS", "resep", 2, "Apotek RS", saat(tanggal, "08:50")));
        db.log_aktivitas.add(jejak(5, 1, "LANJUT_LANGKAH", "pendaftaran_poli", 9, "Ke langkah 7", saat(tanggal, "09:00")));
        db.log_aktivitas.add(jejak(6, 3, "UBAH_STATUS_RESEP", "resep", 5, "Sedang Diracik -> Siap Diambil. Siap di farmasi", saat(tanggal, "09:15")));
        db.log_aktivitas.add(jejak(7, 3, "SERAH_OBAT", "pengambilan_obat", 6, "pasien", saat(kemarin3, "10:00")));
        db.log_aktivitas.add(jejak(8, 3, "NONAKTIF_OBAT", "obat", 5, "OBT005", AWAL));
        return db;
    }

    private record Profil(int idPasien, int idUser, String username, String nama, String nik, String bpjs,
                          String lahir, String jk, String alamat, String telp) {}

    private static Profil profil(int idPasien, int idUser, String username, String nama, String nik, String bpjs,
                                 String lahir, String jk, String alamat, String telp) {
        return new Profil(idPasien, idUser, username, nama, nik, bpjs, lahir, jk, alamat, telp);
    }

    private static String geser(ZonedDateTime asal, int hari) {
        return asal.withZoneSameInstant(Aturan.ZONA).toLocalDate().plusDays(hari).toString();
    }

    private static String saat(String tanggal, String jam) {
        String[] bagian = jam.split(":");
        LocalTime waktu = LocalTime.of(Integer.parseInt(bagian[0]), Integer.parseInt(bagian[1]));
        return Aturan.iso(ZonedDateTime.of(LocalDate.parse(tanggal), waktu, Aturan.ZONA).toInstant());
    }

    private static String estimasi(String tanggal, Jadwal jadwal, int nomor) {
        return Aturan.estimasiJamMasuk(tanggal, jadwal.jam_mulai, nomor, 15);
    }

    private static void masuk(Basis db, Daftar daftar, Periksa periksa) {
        db.pendaftaran_poli.add(daftar);
        db.pemeriksaan.add(periksa);
    }

    private static Pengguna akun(long id, String username, String hash, String role, String nama, String telp, String dibuat) {
        Pengguna item = new Pengguna();
        item.id_user = id;
        item.username = username;
        item.password_hash = hash;
        item.role = role;
        item.nama_lengkap = nama;
        item.no_telepon = telp;
        item.is_active = true;
        item.created_at = dibuat;
        item.updated_at = dibuat;
        return item;
    }

    private static Pasien pasien(long id, long idUser, String nik, String bpjs, String nama, String lahir, String jk,
                                 String alamat, String telp, boolean satusehat) {
        Pasien item = new Pasien();
        item.id_pasien = id;
        item.id_user = idUser;
        item.nik = nik;
        item.nomor_bpjs = bpjs;
        item.nama_lengkap = nama;
        item.tanggal_lahir = lahir;
        item.jenis_kelamin = jk;
        item.alamat = alamat;
        item.no_telepon = telp;
        item.terdaftar_satusehat = satusehat;
        item.created_at = PASIEN_DIBUAT;
        item.updated_at = PASIEN_DIBUAT;
        return item;
    }

    private static Dokter dokter(long id, long idUser, long idPoli, String nama, String spesialisasi, int menit) {
        Dokter item = new Dokter();
        item.id_dokter = id;
        item.id_user = idUser;
        item.id_poli = idPoli;
        item.nama_dokter = nama;
        item.spesialisasi = spesialisasi;
        item.rata_waktu_periksa_menit = menit;
        item.is_active = true;
        item.created_at = AWAL;
        item.updated_at = AWAL;
        return item;
    }

    private static Poli poli(long id, String nama, String deskripsi) {
        Poli item = new Poli();
        item.id_poli = id;
        item.nama_poli = nama;
        item.deskripsi = deskripsi;
        item.is_active = true;
        item.created_at = AWAL;
        item.updated_at = AWAL;
        return item;
    }

    private static List<Jadwal> buatJadwal() {
        List<Jadwal> baris = new ArrayList<>();
        String[] pagi = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
        for (int i = 0; i < pagi.length; i++) {
            baris.add(slot(i + 1, 1, pagi[i], "08:00:00", "12:00:00", 30));
        }
        String[] gigi = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
        for (int i = 0; i < gigi.length; i++) {
            baris.add(slot(8 + i, 2, gigi[i], "09:00:00", "13:00:00", 20));
        }
        String[] anak = {"Selasa", "Kamis", "Sabtu"};
        for (int i = 0; i < anak.length; i++) {
            baris.add(slot(13 + i, 3, anak[i], "08:00:00", "11:00:00", 15));
        }
        String[] mata = {"Senin", "Rabu", "Jumat"};
        for (int i = 0; i < mata.length; i++) {
            baris.add(slot(16 + i, 4, mata[i], "13:00:00", "16:00:00", 12));
        }
        return baris;
    }

    private static Jadwal slot(long id, long idDokter, String hari, String mulai, String selesai, int kuota) {
        Jadwal item = new Jadwal();
        item.id_jadwal = id;
        item.id_dokter = idDokter;
        item.hari = hari;
        item.jam_mulai = mulai;
        item.jam_selesai = selesai;
        item.kuota_maksimal = kuota;
        item.is_active = true;
        item.created_at = AWAL;
        item.updated_at = AWAL;
        return item;
    }

    private static Jadwal jadwalPada(List<Jadwal> baris, long idDokter, String tanggal) {
        String hari = Aturan.namaHari(tanggal);
        for (Jadwal row : baris) {
            if (row.id_dokter == idDokter && hari.equals(row.hari) && row.is_active) {
                return row;
            }
        }
        throw new IllegalStateException("Jadwal dokter " + idDokter + " pada " + hari + " tidak ada");
    }

    private static List<Obat> buatObat() {
        return List.of(
                obat(1, "OBT001", "Paracetamol 500mg", "Jadi", "Tablet", true, 100, 20, 500, true),
                obat(2, "OBT002", "Amoxicillin 500mg", "Jadi", "Kapsul", true, 74, 20, 1000, true),
                obat(3, "OBT003", "Cetirizine 10mg", "Jadi", "Tablet", false, 4, 10, 1500, true),
                obat(4, "OBT004", "Racikan Batuk", "Racikan", "Paket", false, 20, 5, 15000, true),
                obat(5, "OBT005", "Vitamin C 500mg", "Jadi", "Tablet", false, 0, 5, 800, false)
        );
    }

    private static Obat obat(long id, String kode, String nama, String jenis, String satuan, boolean bpjs,
                             int stok, int minimum, double harga, boolean aktif) {
        Obat item = new Obat();
        item.id_obat = id;
        item.kode_kemenkes = kode;
        item.nama_obat = nama;
        item.jenis_obat = jenis;
        item.satuan = satuan;
        item.cover_bpjs = bpjs;
        item.stok_rs = stok;
        item.stok_minimum = minimum;
        item.harga = harga;
        item.is_active = aktif;
        item.created_at = AWAL;
        item.updated_at = AWAL;
        return item;
    }

    private static Daftar daftar(long id, long idPasien, long idJadwal, String tanggal, int nomor, String checkIn,
                                 String estimasi, String status, String catatan, String dibuat, String diubah) {
        Daftar item = new Daftar();
        item.id_pendaftaran = id;
        item.id_pasien = idPasien;
        item.id_jadwal = idJadwal;
        item.tanggal_kunjungan = tanggal;
        item.nomor_antrean = nomor;
        item.waktu_check_in = checkIn;
        item.estimasi_jam_masuk = estimasi;
        item.status_antrean = status;
        item.catatan_pasien = catatan;
        item.created_at = dibuat;
        item.updated_at = diubah;
        return item;
    }

    private static Periksa periksa(long id, long idPendaftaran, long idDokter, String keluhan, String waktu,
                                   String tensi, Double suhu, Integer berat, Integer tinggi, String diagnosis,
                                   String tindakan, String catatan, String selesai) {
        Periksa item = new Periksa();
        item.id_pemeriksaan = id;
        item.id_pendaftaran = idPendaftaran;
        item.id_dokter = idDokter;
        item.keluhan = keluhan;
        item.tekanan_darah = tensi;
        item.suhu_tubuh = suhu;
        item.berat_badan = berat == null ? null : berat.doubleValue();
        item.tinggi_badan = tinggi == null ? null : tinggi.doubleValue();
        item.diagnosis = diagnosis;
        item.tindakan = tindakan;
        item.catatan_dokter = catatan;
        item.waktu_mulai = waktu;
        item.waktu_selesai = selesai;
        item.created_at = waktu;
        item.updated_at = selesai != null ? selesai : waktu;
        return item;
    }

    private static Resep resep(long id, long idPendaftaran, String kode, String terbit, String pilihan, String status,
                               String estimasi, String kadaluarsa) {
        Resep item = new Resep();
        item.id_resep = id;
        item.id_pendaftaran = idPendaftaran;
        item.kode_qr_unik = kode;
        item.waktu_diterbitkan = terbit;
        item.pilihan_penebusan = pilihan;
        item.estimasi_jam_selesai = estimasi;
        item.status_resep = status;
        item.kadaluarsa_qr = kadaluarsa;
        item.created_at = terbit;
        item.updated_at = terbit;
        return item;
    }

    private static Detail item(long id, long idResep, long idObat, int jumlah, String aturan, String racikan, String waktu) {
        Detail item = new Detail();
        item.id_detail = id;
        item.id_resep = idResep;
        item.id_obat = idObat;
        item.jumlah = jumlah;
        item.dosis_aturan_pakai = aturan;
        item.instruksi_racikan = racikan;
        item.created_at = waktu;
        item.updated_at = waktu;
        return item;
    }

    private static Kabar pesan(long id, long idPasien, long idPendaftaran, String jenis, String nomor,
                               Aturan.KonteksPesan ctx, String waktu) {
        Kabar item = new Kabar();
        item.id_notifikasi = id;
        item.id_pasien = idPasien;
        item.id_pendaftaran = idPendaftaran;
        item.jenis_notifikasi = jenis;
        item.channel = "PWA";
        item.nomor_tujuan = nomor;
        item.pesan = Aturan.teksNotifikasi(jenis, ctx);
        item.status = "Terkirim";
        item.waktu_dikirim = waktu;
        item.created_at = waktu;
        return item;
    }

    private static Otp otp(long id, long idUser, String kode, String tujuan, String expired, String verified, int attempt, String dibuat) {
        Otp item = new Otp();
        item.id_otp = id;
        item.id_user = idUser;
        item.kode_otp = kode;
        item.tujuan = tujuan;
        item.expired_at = expired;
        item.verified_at = verified;
        item.attempt = attempt;
        item.created_at = dibuat;
        return item;
    }

    private static Mutasi mutasi(long id, long idObat, long idUser, String jenis, int jumlah, int sebelum, int sesudah,
                                 String keterangan, String waktu) {
        Mutasi item = new Mutasi();
        item.id_mutasi = id;
        item.id_obat = idObat;
        item.id_user = idUser;
        item.jenis_mutasi = jenis;
        item.jumlah = jumlah;
        item.stok_sebelum = sebelum;
        item.stok_sesudah = sesudah;
        item.keterangan = keterangan;
        item.created_at = waktu;
        return item;
    }

    private static Jejak jejak(long id, long idUser, String aktivitas, String tabel, long idReferensi, String keterangan, String waktu) {
        Jejak item = new Jejak();
        item.id_log = id;
        item.id_user = idUser;
        item.aktivitas = aktivitas;
        item.tabel_referensi = tabel;
        item.id_referensi = idReferensi;
        item.keterangan = keterangan;
        item.ip_address = "127.0.0.1";
        item.created_at = waktu;
        return item;
    }
}
