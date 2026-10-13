package mediflow;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Konversi nilai yang dipakai snapshot dan CRUD. */
final class ColumnValues {
    private static final Pattern AWALAN_ANGKA = Pattern.compile("^[+-]?\\d+");
    private static final DateTimeFormatter UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private ColumnValues() {}

    static String sekarangUtc() {
        return UTC.format(Instant.now());
    }

    /**
     * Datetime MySQL menjadi ISO tanpa menggeser zona waktu.
     * "2026-10-02 20:53:44" tetap "2026-10-02T20:53:44.000Z".
     */
    static String keIso(String nilai) {
        if (nilai == null || nilai.isEmpty()) {
            return null;
        }
        if (!nilai.contains(" ") && !nilai.contains("T")) {
            return nilai;
        }
        nilai = nilai.replace(' ', 'T');
        if (!nilai.endsWith("Z")) {
            nilai = nilai + (nilai.length() == 19 ? ".000Z" : "Z");
        }
        return nilai;
    }

    static String keSql(String nilai) {
        if (nilai == null || nilai.isEmpty()) {
            return null;
        }
        if (nilai.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return nilai;
        }
        if (nilai.matches("\\d{2}:\\d{2}(:\\d{2})?")) {
            return nilai.length() == 5 ? nilai + ":00" : nilai;
        }
        String ganti = nilai.replace('T', ' ');
        return ganti.length() <= 19 ? ganti : ganti.substring(0, 19);
    }

    static String keSqlWaktu(String nilai) {
        nilai = nilai.trim().replace('T', ' ');
        nilai = nilai.replaceFirst("(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$", "");
        if (!tanggalSah(nilai, "Y-m-d H:i:s")) {
            return null;
        }
        return nilai;
    }

    static String keJam(String nilai) {
        Matcher cocok = Pattern.compile("^(\\d{2}):(\\d{2})(?::(\\d{2}))?$").matcher(nilai.trim());
        if (!cocok.matches()) {
            return null;
        }
        int jam = Integer.parseInt(cocok.group(1));
        int menit = Integer.parseInt(cocok.group(2));
        int detik = cocok.group(3) == null ? 0 : Integer.parseInt(cocok.group(3));
        if (jam > 23 || menit > 59 || detik > 59) {
            return null;
        }
        return String.format("%02d:%02d:%02d", jam, menit, detik);
    }

    static boolean tanggalSah(String teks, String format) {
        if (teks == null || teks.isEmpty()) {
            return false;
        }
        String pola = switch (format) {
            case "Y-m-d" -> "uuuu-MM-dd";
            case "Y-m-d H:i:s" -> "uuuu-MM-dd HH:mm:ss";
            default -> null;
        };
        if (pola == null) {
            return false;
        }
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pola).withResolverStyle(ResolverStyle.STRICT);
            if ("uuuu-MM-dd".equals(pola)) {
                LocalDate.parse(teks, fmt);
            } else {
                LocalDateTime.parse(teks, fmt);
            }
            return true;
        } catch (DateTimeParseException error) {
            return false;
        }
    }

    static String rapikanWaktuDb(String nilai) {
        if (nilai == null) {
            return null;
        }
        if (nilai.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+")) {
            return nilai.substring(0, 19);
        }
        if (nilai.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d+")) {
            return nilai.substring(0, 8);
        }
        return nilai;
    }

    static Object baca(String nilai, TableCatalog.Col kolom) {
        nilai = rapikanWaktuDb(nilai);
        if (nilai == null || nilai.isEmpty()) {
            if (kolom.nullable()) {
                return null;
            }
            return kolom.type() == TableCatalog.Type.STR ? "" : null;
        }
        return switch (kolom.type()) {
            case INT -> keLong(nilai);
            case BOOL -> "1".equals(nilai) || "true".equals(nilai);
            case DEC -> Double.parseDouble(nilai);
            case DT -> keIso(nilai);
            default -> nilai;
        };
    }

    static Object bacaCrud(String nilai, TableCatalog.Col kolom) {
        nilai = rapikanWaktuDb(nilai);
        if (nilai == null) {
            return null;
        }
        return switch (kolom.type()) {
            case INT -> keLong(nilai);
            case BOOL -> "1".equals(nilai) || "true".equals(nilai);
            case DEC -> Double.parseDouble(nilai);
            default -> nilai;
        };
    }

    static boolean nyala(Object nilai) {
        if (nilai instanceof Boolean bendera) {
            return bendera;
        }
        if (nilai instanceof Number angka) {
            return (int) angka.doubleValue() == 1;
        }
        if (nilai == null) {
            return false;
        }
        String teks = String.valueOf(nilai).trim().toLowerCase(Locale.ROOT);
        return teks.equals("1") || teks.equals("true") || teks.equals("ya");
    }

    /** Hanya true, angka 1, atau teks "1". Dipakai saat menulis snapshot. */
    static boolean benarSnapshot(Object nilai) {
        if (nilai instanceof Boolean bendera) {
            return bendera;
        }
        if (nilai instanceof Byte || nilai instanceof Short || nilai instanceof Integer || nilai instanceof Long) {
            return ((Number) nilai).longValue() == 1L;
        }
        return "1".equals(nilai);
    }

    static boolean ada(Object nilai) {
        if (nilai == null) {
            return false;
        }
        if (nilai instanceof Boolean bendera) {
            return bendera;
        }
        if (nilai instanceof Number angka) {
            return angka.doubleValue() != 0d && !Double.isNaN(angka.doubleValue());
        }
        String teks = String.valueOf(nilai);
        return !teks.isEmpty() && !"0".equals(teks);
    }

    static boolean numerik(Object nilai) {
        if (nilai instanceof Boolean || nilai == null) {
            return false;
        }
        if (nilai instanceof Number angka) {
            if (angka instanceof Double desimal) {
                return !desimal.isNaN() && !desimal.isInfinite();
            }
            return true;
        }
        String teks = String.valueOf(nilai).trim();
        return teks.matches("[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?");
    }

    static long keLong(Object nilai) {
        if (nilai instanceof Boolean bendera) {
            return bendera ? 1L : 0L;
        }
        if (nilai instanceof Number angka) {
            return angka.longValue();
        }
        String teks = String.valueOf(nilai).trim();
        try {
            if (teks.indexOf('.') >= 0 || teks.indexOf('e') >= 0 || teks.indexOf('E') >= 0) {
                return (long) Double.parseDouble(teks);
            }
            return Long.parseLong(teks);
        } catch (NumberFormatException error) {
            Matcher cocok = AWALAN_ANGKA.matcher(teks);
            if (cocok.find()) {
                return Long.parseLong(cocok.group());
            }
            return 0L;
        }
    }

    static double keDouble(Object nilai) {
        if (nilai instanceof Boolean bendera) {
            return bendera ? 1d : 0d;
        }
        if (nilai instanceof Number angka) {
            return angka.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(nilai).trim());
        } catch (NumberFormatException error) {
            return 0d;
        }
    }

    static String phpString(Object nilai) {
        if (nilai == null) {
            return "";
        }
        if (nilai instanceof Boolean bendera) {
            return bendera ? "1" : "";
        }
        if (nilai instanceof Double || nilai instanceof Float) {
            double angka = ((Number) nilai).doubleValue();
            if (!Double.isNaN(angka) && !Double.isInfinite(angka) && angka == Math.rint(angka)
                    && Math.abs(angka) < 1e15) {
                return Long.toString((long) angka);
            }
            return Double.toString(angka);
        }
        if (nilai instanceof Number angka) {
            return Long.toString(angka.longValue());
        }
        return String.valueOf(nilai);
    }

    static int menit(String jam) {
        String[] bagian = jam.split(":", -1);
        int jamAngka = parseAman(bagian[0]);
        int menitAngka = bagian.length > 1 ? parseAman(bagian[1]) : 0;
        return jamAngka * 60 + menitAngka;
    }

    private static int parseAman(String teks) {
        try {
            return Integer.parseInt(teks);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    static String pesan(Throwable error) {
        String teks = error.getMessage();
        if (teks == null || teks.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return teks;
    }
}
