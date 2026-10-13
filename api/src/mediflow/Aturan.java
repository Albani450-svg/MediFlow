package mediflow;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Tahap kunjungan, jadwal, stok, dan teks notifikasi. */
final class Aturan {
    static final ZoneId ZONA = ZoneId.of("Asia/Jakarta");
    static final String[] HARI = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private Aturan() {}

    static String iso(Instant saat) {
        return ISO.format(saat.truncatedTo(ChronoUnit.MILLIS));
    }

    static String sekarangIso() {
        return iso(Instant.now());
    }

    static String tanggalIso() {
        return LocalDate.now(ZONA).toString();
    }

    static String tanggalIso(ZonedDateTime saat) {
        return saat.withZoneSameInstant(ZONA).toLocalDate().toString();
    }

    static String namaHari(LocalDate tanggal) {
        return HARI[tanggal.getDayOfWeek().getValue() % 7];
    }

    static String namaHari(String tanggal) {
        return namaHari(LocalDate.parse(tanggal));
    }

    static String jamPendek(String jam) {
        if (jam == null) {
            return "";
        }
        return jam.length() <= 5 ? jam : jam.substring(0, 5);
    }

    static String estimasiJamMasuk(String tanggal, String jamMulai, int nomorAntrean, int rataMenit) {
        String jam = jamPendek(jamMulai);
        ZonedDateTime mulai = ZonedDateTime.parse(tanggal + "T" + jam + ":00+07:00");
        mulai = mulai.plusMinutes((long) Math.max(0, nomorAntrean - 1) * rataMenit);
        return iso(mulai.toInstant());
    }

    static String statusKetersediaan(int stok, int jumlah) {
        if (stok <= 0) {
            return "kosong";
        }
        if (stok < jumlah) {
            return "sebagian";
        }
        return "tersedia";
    }

    static boolean bolehTebusRs(List<String> statuses) {
        if (statuses.isEmpty()) {
            return false;
        }
        for (String status : statuses) {
            if (!"tersedia".equals(status)) {
                return false;
            }
        }
        return true;
    }

    static int estimasiFarmasiMenit(int antrianDiDepan, List<String> jenis) {
        int racik = 0;
        for (String item : jenis) {
            racik += "Racikan".equals(item) ? 15 : 5;
        }
        return Math.max(0, antrianDiDepan) * 10 + racik;
    }

    static boolean bentrokPasien(List<Daftar> rows, long idPasien, long idJadwal, String tanggal) {
        for (Daftar row : rows) {
            if (row.id_pasien == idPasien && row.id_jadwal == idJadwal && tanggal.equals(row.tanggal_kunjungan)
                    && !"Batal".equals(row.status_antrean)) {
                return true;
            }
        }
        return false;
    }

    static boolean bentrokNomorAntrean(List<Daftar> rows, long idJadwal, String tanggal, int nomor) {
        for (Daftar row : rows) {
            if (row.id_jadwal == idJadwal && tanggal.equals(row.tanggal_kunjungan) && row.nomor_antrean == nomor) {
                return true;
            }
        }
        return false;
    }

    static boolean qrMasihBerlaku(String kedaluwarsa, boolean dipakai) {
        return qrMasihBerlaku(kedaluwarsa, dipakai, Instant.now());
    }

    static boolean qrMasihBerlaku(String kedaluwarsa, boolean dipakai, Instant sekarang) {
        if (dipakai || kedaluwarsa == null || kedaluwarsa.isEmpty()) {
            return false;
        }
        return Instant.parse(kedaluwarsa).isAfter(sekarang);
    }

    static int hitungTahap(String statusAntrean, boolean checkIn, boolean adaVital, Resep resep, boolean terkirim) {
        if ("Batal".equals(statusAntrean)) {
            return 0;
        }
        if (resep != null && "Apotek Luar".equals(resep.pilihan_penebusan)) {
            return 10;
        }
        if (resep != null && "Selesai Diambil".equals(resep.status_resep)) {
            return 10;
        }
        if (resep != null && "Siap Diambil".equals(resep.status_resep)) {
            return 9;
        }
        if (resep != null && "Sedang Diracik".equals(resep.status_resep)) {
            return 8;
        }
        if (resep != null && "Antrean Farmasi".equals(resep.status_resep)) {
            return 7;
        }
        if (resep != null && terkirim && "Menunggu Pilihan".equals(resep.status_resep)) {
            return 6;
        }
        if ("Selesai Pemeriksaan".equals(statusAntrean)) {
            return 5;
        }
        if ("Masuk Ruangan".equals(statusAntrean)) {
            return 4;
        }
        if ("Dipanggil".equals(statusAntrean) || adaVital) {
            return 3;
        }
        if (checkIn) {
            return 2;
        }
        return 1;
    }

    static String judulNotifikasi(String jenis, String pesan) {
        if ("Obat Siap".equals(jenis) && pesan != null && pesan.startsWith("Kunjungan selesai")) {
            return "Kunjungan selesai";
        }
        return switch (jenis) {
            case "Verifikasi WA" -> "Verifikasi nomor";
            case "Konfirmasi Pendaftaran" -> "Pendaftaran berhasil";
            case "Pengingat Kunjungan" -> "Pengingat kunjungan";
            case "QR Resep" -> "E-resep terbit";
            case "Obat Siap" -> "Obat siap diambil";
            default -> "MediFlow";
        };
    }

    static String teksNotifikasi(String jenis, KonteksPesan ctx) {
        String nomor = ctx.nomor == null ? "-" : String.valueOf(ctx.nomor);
        return switch (jenis) {
            case "Verifikasi WA" -> "Notifikasi MediFlow aktif. Pembaruan kunjungan akan muncul di aplikasi ini.";
            case "Konfirmasi Pendaftaran" -> "Pendaftaran antrean " + nomor + " berhasil di " + ctx.poli
                    + ". Estimasi masuk " + ctx.jam + " pada " + ctx.tanggal + ".";
            case "Pengingat Kunjungan" -> {
                if ("checkin".equals(ctx.mode)) {
                    yield "Check-in berhasil untuk antrean " + nomor + ". Estimasi masuk ruang " + ctx.jam + ".";
                }
                if ("panggil".equals(ctx.mode)) {
                    yield "Anda dipanggil masuk ruang periksa. Nomor antrean " + nomor + ".";
                }
                String poli = ctx.poli == null ? "poliklinik" : ctx.poli;
                yield "Pengingat kunjungan hari ini di " + poli + ". Nomor antrean " + nomor
                        + ". Silakan datang dan lakukan check-in.";
            }
            case "QR Resep" -> "E-resep sudah terbit. Buka tab E-Resep untuk melihat ketersediaan dan memilih tempat tebus. "
                    + "Notifikasi ini tidak memuat nama obat.";
            case "Obat Siap" -> {
                if (ctx.selesai) {
                    yield "Kunjungan selesai. Riwayat pengobatan ditandai terkirim ke rekam medis rumah sakit.";
                }
                yield "Obat untuk antrean " + nomor
                        + " siap diambil di farmasi rumah sakit. Buka tab E-Resep dan tunjukkan kode. "
                        + "Berlaku 24 jam dan hanya sekali pakai.";
            }
            default -> "Ada pembaruan pada kunjungan MediFlow Anda.";
        };
    }

    static String tambahJam(String iso, double jam) {
        Instant awal = Instant.parse(iso);
        long ms = Math.round(jam * 3_600_000d);
        return iso(awal.plusMillis(ms));
    }

    static final class KonteksPesan {
        String poli;
        String tanggal;
        String jam;
        Integer nomor;
        String mode;
        boolean selesai;

        KonteksPesan nomor(int nilai) {
            this.nomor = nilai;
            return this;
        }

        KonteksPesan poli(String nilai) {
            this.poli = nilai;
            return this;
        }

        KonteksPesan tanggal(String nilai) {
            this.tanggal = nilai;
            return this;
        }

        KonteksPesan jam(String nilai) {
            this.jam = nilai;
            return this;
        }

        KonteksPesan mode(String nilai) {
            this.mode = nilai;
            return this;
        }

        KonteksPesan selesai(boolean nilai) {
            this.selesai = nilai;
            return this;
        }
    }
}
