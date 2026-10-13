package mediflow;

import java.sql.Connection;

/** Mengganti isi medicflow_db dengan data uji hari ini. */
public final class IsiDummy {
    private IsiDummy() {}

    public static void main(String[] args) throws Exception {
        Basis basis = DataUji.buat();
        try (Connection db = Db.open()) {
            Snapshot.simpan(db, basis.kePeta());
        }
        System.out.println("[mediflow] Data uji tersimpan. pengguna=" + basis.users.size()
                + " antrean=" + basis.pendaftaran_poli.size()
                + " obat=" + basis.obat.size());
    }
}
