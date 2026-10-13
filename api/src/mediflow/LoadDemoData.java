package mediflow;

import java.sql.Connection;

/** Mengganti isi medicflow_db dengan data uji hari ini. */
public final class LoadDemoData {
    private LoadDemoData() {}

    public static void main(String[] args) throws Exception {
        DatabaseRecords basis = DemoData.buat();
        try (Connection db = Database.open()) {
            DatabaseSnapshot.simpan(db, basis.kePeta());
        }
        System.out.println("[mediflow] Data uji tersimpan. pengguna=" + basis.users.size()
                + " antrean=" + basis.pendaftaran_poli.size()
                + " obat=" + basis.obat.size());
    }
}
