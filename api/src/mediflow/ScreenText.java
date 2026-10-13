package mediflow;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/** Teks langkah, format tanggal, dan panduan peran. */
final class ScreenText {
    record Kode(String kode, String nama) {}

    record Langkah(int tahap, String singkat, String judul, String deskripsi, String pasien, String tugas,
                   String tombol, String pemilik, String pemilikNama) {}

    record Fase(int id, String nama, int dari, int sampai) {}

    record Panduan(int faseId, String status, String tindakan, String giliran, String tombol, boolean milikAnda,
                   String penghalang) {}

    record Konteks(int tahap, boolean checkIn, boolean adaKeluhan, boolean adaVital, boolean adaResepItem,
                   String pilihan, boolean bolehRs, boolean batal, boolean selesai) {}

    static final List<Fase> FASE = List.of(
            new Fase(1, "Daftar", 1, 3),
            new Fase(2, "Periksa", 4, 5),
            new Fase(3, "Resep", 6, 8),
            new Fase(4, "Ambil", 9, 10)
    );

    static final List<Langkah> LANGKAH = List.of(
            langkah(1, "Check-in kedatangan", "1. Daftar dan check-in",
                    "Pasien memilih poli, menulis keluhan, lalu menekan check-in saat sampai.",
                    "Tekan Check-in kedatangan saat Anda sudah di rumah sakit.",
                    "Tunggu pasien check-in. Setelah itu, catat tanda vital.",
                    "Menunggu check-in pasien", "admin", "Admin pendaftaran"),
            langkah(2, "Cek tanda vital", "2. Cek tanda vital",
                    "Perawat mencatat tekanan darah, suhu, dan berat badan.",
                    "Tidak ada yang perlu diisi. Tunggu perawat selesai mencatat.",
                    "Isi tekanan darah, suhu, dan berat, simpan, lalu masukkan pasien ke ruang tunggu.",
                    "Tanda vital tersimpan, masuk ruang tunggu", "admin", "Perawat"),
            langkah(3, "Menunggu dipanggil", "3. Ruang tunggu poli",
                    "Pasien menunggu sampai dipanggil masuk ruang dokter.",
                    "Tunggu di poli. Notifikasi aplikasi memberitahu saat Anda dipanggil.",
                    "Panggil pasien masuk ruang dokter.",
                    "Panggil pasien masuk ruang dokter", "admin", "Admin poli"),
            langkah(4, "Sedang diperiksa", "4. Pemeriksaan dokter",
                    "Dokter menulis diagnosis dan tindakan.",
                    "Hasil pemeriksaan muncul di kartu di bawah. Anda tidak perlu menekan apa pun.",
                    "Pilih diagnosis, isi catatan, lalu selesaikan pemeriksaan.",
                    "Simpan diagnosis dan selesai periksa", "dokter", "Dokter"),
            langkah(5, "Dokter menulis resep", "5. Dokter mengirim resep",
                    "Dokter menambahkan obat dan mengirim resep ke farmasi.",
                    "Tunggu resep terkirim, lalu buka tab E-Resep.",
                    "Tambahkan obat, lalu kirim resep ke farmasi. Pasien hanya mendapat pemberitahuan, tanpa nama obat.",
                    "Kirim resep ke farmasi", "dokter", "Dokter"),
            langkah(6, "Pilih tempat tebus", "6. Pilih apotek",
                    "Pasien memilih apotek rumah sakit atau membawa resep ke apotek luar.",
                    "Pilih tebus di apotek rumah sakit atau bawa resep ke apotek luar.",
                    "Setelah pasien memilih apotek rumah sakit, teruskan resep ke antrean farmasi.",
                    "Teruskan ke antrean farmasi", "admin", "Admin kasir"),
            langkah(7, "Obat disiapkan", "7. Obat disiapkan",
                    "Apoteker menyiapkan obat sesuai resep dokter.",
                    "Obat sedang disiapkan. Notifikasi masuk saat obat siap diambil.",
                    "Siapkan obat sesuai daftar, lalu lanjutkan ke pengecekan ulang.",
                    "Obat selesai disiapkan", "farmasi", "Farmasi"),
            langkah(8, "Obat dicek ulang", "8. Pengecekan ulang obat",
                    "Farmasi mencocokkan dosis dan etiket sebelum obat dipanggil.",
                    "Obat sedang dicek ulang. Tetap tunggu notifikasi di aplikasi.",
                    "Cocokkan dosis dan etiket, lalu panggil pasien ke farmasi.",
                    "Cek ulang selesai, panggil pasien", "farmasi", "Farmasi"),
            langkah(9, "Obat siap diambil", "9. Ambil obat di farmasi",
                    "Pasien menunjukkan kode di aplikasi. Farmasi menyerahkan obat.",
                    "Buka tab E-Resep dan tunjukkan kode di farmasi. Keluarga yang mengambil perlu PIN.",
                    "Ketik kode dari aplikasi pasien, lalu serahkan obat.",
                    "Serahkan dengan kode pasien", "farmasi", "Farmasi"),
            langkah(10, "Kunjungan selesai", "10. Kunjungan selesai",
                    "Rekam medis tersimpan. Obat sudah diterima, atau resep dibawa ke apotek luar.",
                    "Kunjungan ini selesai. Daftar lagi di bawah jika Anda perlu poli lain.",
                    "Tidak ada tindakan. Kunjungan sudah ditutup.",
                    "Kunjungan selesai", "done", "Selesai")
    );

    static final List<Kode> ICD = List.of(
            new Kode("J02.9", "Faringitis Akut (Radang Tenggorokan)"),
            new Kode("J06.9", "Infeksi Saluran Napas Atas (ISPA)"),
            new Kode("K29.7", "Gastritis / Dispepsia Akut"),
            new Kode("I10", "Hipertensi Esensial Primer"),
            new Kode("A09", "Gastroenteritis dan Diare Infeksi")
    );

    static final List<String> TINDAKAN = List.of(
            "Rawat Jalan + Terapi Obat",
            "Nebulizer + Rawat Jalan",
            "Rujuk Cek Laboratorium",
            "Edukasi tanpa resep"
    );

    static final List<String> ATURAN_PAKAI = List.of(
            "3x1 Sehari (Sesudah Makan)",
            "2x1 Sehari (Pagi & Malam)",
            "1x1 Sehari (Malam Sebelum Tidur)",
            "3x1 Sehari (Sebelum Makan)"
    );

    private static final String[] BULAN = {
            "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    };

    private static final Map<String, String> BELUM = Map.of(
            "pasien", "Belum ada kunjungan. Isi form daftar di bawah untuk mendapat nomor antrean.",
            "dokter", "Belum ada pasien di jadwal Anda hari ini.",
            "admin", "Belum ada kunjungan hari ini.",
            "farmasi", "Belum ada resep. Resep muncul di sini setelah pasien memilih apotek rumah sakit."
    );

    private ScreenText() {}

    private static Langkah langkah(int tahap, String singkat, String judul, String deskripsi, String pasien,
                                   String tugas, String tombol, String pemilik, String pemilikNama) {
        return new Langkah(tahap, singkat, judul, deskripsi, pasien, tugas, tombol, pemilik, pemilikNama);
    }

    static Langkah langkah(int tahap) {
        int indeks = Math.min(Math.max(tahap, 1), 10) - 1;
        return LANGKAH.get(indeks);
    }

    static Fase faseDari(int tahap) {
        for (Fase fase : FASE) {
            if (tahap >= fase.dari && tahap <= fase.sampai) {
                return fase;
            }
        }
        return FASE.get(0);
    }

    static String icdNama(String kode) {
        for (Kode item : ICD) {
            if (item.kode.equals(kode)) {
                return item.nama;
            }
        }
        return "";
    }

    static Panduan panduan(String role, Konteks ctx) {
        if (ctx.batal) {
            return new Panduan(1, "Dibatalkan", "Kunjungan ini dibatalkan. Daftar lagi jika masih ingin berobat.",
                    "Selesai", "Kunjungan dibatalkan", false, null);
        }
        if (ctx.tahap <= 0) {
            return new Panduan(0, "Belum mulai", BELUM.getOrDefault(role, ""), "—", "", false, null);
        }
        Langkah item = langkah(ctx.tahap);
        Fase fase = faseDari(ctx.tahap);
        String penghalang = null;
        if (!ctx.selesai && ctx.tahap == 1 && !ctx.checkIn) {
            penghalang = "Pasien belum menekan check-in kedatangan.";
        } else if (!ctx.selesai && ctx.tahap == 1 && !ctx.adaKeluhan) {
            penghalang = "Keluhan belum diisi. Simpan keluhan sebelum lanjut.";
        } else if (!ctx.selesai && ctx.tahap == 2 && !ctx.adaVital) {
            penghalang = "Tanda vital belum disimpan. Isi tekanan darah, suhu, dan berat, lalu simpan.";
        } else if (!ctx.selesai && ctx.tahap == 5 && !ctx.adaResepItem) {
            penghalang = "Tambahkan minimal satu obat sebelum mengirim resep.";
        } else if (!ctx.selesai && ctx.tahap == 6 && (ctx.pilihan == null || "belum".equals(ctx.pilihan))) {
            penghalang = ctx.bolehRs
                    ? "Pasien belum memilih apotek rumah sakit atau apotek luar."
                    : "Stok rumah sakit kurang. Pasien perlu memilih apotek luar.";
        }
        String tindakan = "pasien".equals(role) ? item.pasien : item.tugas;
        String tombol = item.tombol;
        boolean milikAnda = item.pemilik.equals(role);
        if ("pasien".equals(role)) {
            if (ctx.selesai) {
                tindakan = "Kunjungan selesai. Daftar poli lain di bawah jika Anda perlu berobat lagi.";
            } else if (ctx.tahap <= 3 && !ctx.checkIn) {
                tindakan = "Tekan Check-in kedatangan saat Anda sudah di rumah sakit.";
            } else if (ctx.tahap == 6 && (ctx.pilihan == null || "belum".equals(ctx.pilihan))) {
                tindakan = ctx.bolehRs
                        ? "Giliran Anda: pilih apotek rumah sakit atau apotek luar pada kartu Pilihan apotek."
                        : "Stok rumah sakit tidak cukup. Pilih apotek luar pada kartu Pilihan apotek.";
            } else if (ctx.tahap == 6 && "luar".equals(ctx.pilihan)) {
                tindakan = "Anda memilih apotek luar. Buka tab E-Resep dan bawa kodenya ke apotek tersebut.";
            } else if (ctx.tahap == 6 && "rs".equals(ctx.pilihan)) {
                tindakan = "Apotek rumah sakit sudah dipilih. Tunggu petugas meneruskan resep ke farmasi.";
            }
        } else if (ctx.selesai) {
            tindakan = "Kunjungan ini sudah selesai. Tidak ada langkah yang perlu dilanjutkan.";
            tombol = "Kunjungan selesai";
        } else if ("admin".equals(role) && ctx.tahap == 1 && !ctx.checkIn) {
            tindakan = "Tunggu pasien menekan Check-in kedatangan. Keluhan sudah tersimpan saat daftar.";
            tombol = "Menunggu check-in pasien";
            milikAnda = false;
        } else if ("dokter".equals(role) && !milikAnda) {
            tindakan = "Bukan giliran Anda. " + item.pemilikNama + " mengerjakan langkah ini: " + item.singkat + ".";
            tombol = "Simpan catatan saja";
        } else if ("farmasi".equals(role) && ctx.tahap < 7) {
            tindakan = "Resep belum masuk antrean racik. Tunggu pasien memilih apotek rumah sakit.";
            tombol = "Menunggu pilihan pasien";
        } else if ("farmasi".equals(role) && ctx.tahap == 9) {
            tindakan = "Pasien menunjukkan kode di aplikasi. Ketik kode itu pada kartu Serah terima, lalu serahkan obat.";
        } else if ("farmasi".equals(role) && !milikAnda) {
            tindakan = "Bukan giliran Anda. " + item.pemilikNama + " mengerjakan: " + item.singkat + ".";
        } else if ("admin".equals(role) && ctx.tahap == 6 && "luar".equals(ctx.pilihan)) {
            tindakan = "Pasien membawa resep ke apotek luar. Kunjungan ini selesai. Stok rumah sakit tidak berkurang.";
            tombol = "Kunjungan selesai";
            milikAnda = false;
        } else if ("admin".equals(role) && !milikAnda) {
            tindakan = "Giliran " + item.pemilikNama + ". Tombol di bawah menjalankan simulasi tanpa perlu berganti akun.";
        }
        return new Panduan(fase.id, ctx.selesai ? "Kunjungan selesai" : item.singkat, tindakan, item.pemilikNama, tombol,
                ctx.selesai ? false : milikAnda, ctx.selesai ? null : penghalang);
    }

    static String formatJam(String iso) {
        if (iso == null || iso.isEmpty()) {
            return "-";
        }
        ZonedDateTime zona = Instant.parse(iso).atZone(VisitRules.ZONA);
        return String.format("%02d.%02d", zona.getHour(), zona.getMinute());
    }

    static String formatTanggal(String nilai) {
        if (nilai == null || nilai.isEmpty()) {
            return "-";
        }
        LocalDate tanggal = nilai.length() <= 10
                ? LocalDate.parse(nilai.substring(0, 10))
                : Instant.parse(nilai).atZone(VisitRules.ZONA).toLocalDate();
        return tanggal.getDayOfMonth() + " " + BULAN[tanggal.getMonthValue()] + " " + tanggal.getYear();
    }

    static String formatRp(double nilai) {
        long bulat = Math.round(nilai);
        String teks = String.format("%,d", bulat).replace(',', '.');
        return "Rp\u00A0" + teks;
    }

    static String labelPenjamin(String nomorBpjs) {
        return nomorBpjs != null && !nomorBpjs.isEmpty() ? "BPJS Kesehatan" : "Umum";
    }

    static String labelKetersediaan(String status) {
        if ("tersedia".equals(status)) {
            return "Tersedia";
        }
        if ("sebagian".equals(status)) {
            return "Sebagian";
        }
        return "Kosong";
    }

    static String labelStatusResep(String status) {
        return switch (status) {
            case "Menunggu Pilihan" -> "Menunggu pilihan tebus";
            case "Antrean Farmasi" -> "Antrean farmasi";
            case "Sedang Diracik" -> "Sedang diracik";
            case "Siap Diambil" -> "Siap diambil";
            case "Selesai Diambil" -> "Selesai diambil";
            case "Dibatalkan" -> "Dibatalkan";
            default -> status;
        };
    }

    static String angkaTampil(Double nilai) {
        if (nilai == null) {
            return "-";
        }
        if (nilai == Math.rint(nilai)) {
            return Long.toString(nilai.longValue());
        }
        return Double.toString(nilai);
    }
}
