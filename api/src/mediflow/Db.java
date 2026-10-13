package mediflow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Koneksi medicflow_db. Variabel lingkungan sama dengan API PHP. */
final class Db {
    private Db() {}

    static Connection open() throws SQLException {
        String host = env("MEDIFLOW_DB_HOST", "127.0.0.1");
        String user = env("MEDIFLOW_DB_USER", "root");
        String password = env("MEDIFLOW_DB_PASSWORD", "");
        String name = env("MEDIFLOW_DB_NAME", "medicflow_db");
        String port = env("MEDIFLOW_DB_PORT", "3306");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"
                + "&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
                + "&allowPublicKeyRetrieval=true&useSSL=false&tinyInt1isBit=false";
        Connection koneksi = DriverManager.getConnection(url, user, password);
        try (Statement perintah = koneksi.createStatement()) {
            perintah.execute("SET time_zone = '+00:00'");
            perintah.execute("SET NAMES utf8mb4");
        }
        return koneksi;
    }

    private static String env(String nama, String bawaan) {
        String nilai = System.getenv(nama);
        if (nilai == null || nilai.isEmpty()) {
            return bawaan;
        }
        return nilai;
    }
}
