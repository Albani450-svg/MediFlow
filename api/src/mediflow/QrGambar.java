package mediflow;

import mediflow.qr.QrCode;

/** Gambar QR untuk kode pengambilan. Teks kode tetap ditampilkan di bawah gambar. */
final class QrGambar {
    private static final int TENANG = 4;

    private QrGambar() {}

    static String svg(String teks) {
        if (teks == null || teks.isEmpty()) {
            return "";
        }
        QrCode kode;
        try {
            kode = QrCode.encodeText(teks, QrCode.Ecc.MEDIUM);
        } catch (IllegalArgumentException error) {
            return "";
        }
        int sisi = kode.size + TENANG * 2;
        StringBuilder jalur = new StringBuilder();
        for (int y = 0; y < kode.size; y++) {
            for (int x = 0; x < kode.size; x++) {
                if (kode.getModule(x, y)) {
                    jalur.append('M').append(x + TENANG).append(',').append(y + TENANG).append("h1v1h-1z");
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + sisi + " " + sisi
                + "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"Kode QR pengambilan\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
                + "<path fill=\"#0f172a\" d=\"" + jalur + "\"/></svg>";
    }
}
