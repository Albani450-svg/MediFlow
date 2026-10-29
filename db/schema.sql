-- Struktur database medicflow_db, sama dengan diagram DBeaver (MariaDB, 127.0.0.1:3306).
-- Lima belas tabel: detail_resep, dokter, jadwal_dokter, log_aktivitas, mutasi_stok,
-- notifikasi, obat, otp_verifikasi, pasien, pemeriksaan, pendaftaran_poli,
-- pengambilan_obat, poliklinik, resep, users.
-- Tidak ada accounts, patients, admins, farmasi_staff, atau auth_rate_limits.
-- Jangan impor berkas ini. Isi data tetap di server. Aplikasi hanya membaca dan menulis tabel di atas.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `detail_resep` (
  `id_detail` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_resep` bigint(20) unsigned NOT NULL,
  `id_obat` bigint(20) unsigned NOT NULL,
  `jumlah` int(10) unsigned NOT NULL,
  `dosis_aturan_pakai` varchar(255) NOT NULL,
  `instruksi_racikan` text DEFAULT NULL,
  `catatan` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_detail`),
  KEY `idx_detail_resep` (`id_resep`),
  KEY `idx_detail_obat` (`id_obat`),
  CONSTRAINT `fk_detail_obat` FOREIGN KEY (`id_obat`) REFERENCES `obat` (`id_obat`) ON UPDATE CASCADE,
  CONSTRAINT `fk_detail_resep` FOREIGN KEY (`id_resep`) REFERENCES `resep` (`id_resep`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dokter` (
  `id_dokter` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_user` bigint(20) unsigned NOT NULL,
  `id_poli` bigint(20) unsigned NOT NULL,
  `nama_dokter` varchar(150) NOT NULL,
  `spesialisasi` varchar(100) DEFAULT NULL,
  `rata_waktu_periksa_menit` int(10) unsigned NOT NULL DEFAULT 15,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_dokter`),
  UNIQUE KEY `id_user` (`id_user`),
  KEY `idx_dokter_poli` (`id_poli`),
  CONSTRAINT `fk_dokter_poli` FOREIGN KEY (`id_poli`) REFERENCES `poliklinik` (`id_poli`) ON UPDATE CASCADE,
  CONSTRAINT `fk_dokter_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jadwal_dokter` (
  `id_jadwal` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_dokter` bigint(20) unsigned NOT NULL,
  `hari` enum('Senin','Selasa','Rabu','Kamis','Jumat','Sabtu','Minggu') NOT NULL,
  `jam_mulai` time NOT NULL,
  `jam_selesai` time NOT NULL,
  `kuota_maksimal` int(10) unsigned NOT NULL DEFAULT 20,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_jadwal`),
  KEY `idx_jadwal_dokter` (`id_dokter`),
  KEY `idx_jadwal_hari` (`hari`),
  CONSTRAINT `fk_jadwal_dokter` FOREIGN KEY (`id_dokter`) REFERENCES `dokter` (`id_dokter`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log_aktivitas` (
  `id_log` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_user` bigint(20) unsigned DEFAULT NULL,
  `aktivitas` varchar(100) NOT NULL,
  `tabel_referensi` varchar(100) DEFAULT NULL,
  `id_referensi` bigint(20) unsigned DEFAULT NULL,
  `keterangan` text DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_log`),
  KEY `idx_log_user` (`id_user`),
  KEY `idx_log_aktivitas` (`aktivitas`),
  KEY `idx_log_tanggal` (`created_at`),
  CONSTRAINT `fk_log_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mutasi_stok` (
  `id_mutasi` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_obat` bigint(20) unsigned NOT NULL,
  `id_user` bigint(20) unsigned NOT NULL,
  `jenis_mutasi` enum('Masuk','Keluar','Penyesuaian') NOT NULL,
  `jumlah` int(10) unsigned NOT NULL,
  `stok_sebelum` int(11) NOT NULL,
  `stok_sesudah` int(11) NOT NULL,
  `keterangan` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_mutasi`),
  KEY `idx_mutasi_obat` (`id_obat`),
  KEY `idx_mutasi_user` (`id_user`),
  KEY `idx_mutasi_tanggal` (`created_at`),
  CONSTRAINT `fk_mutasi_obat` FOREIGN KEY (`id_obat`) REFERENCES `obat` (`id_obat`) ON UPDATE CASCADE,
  CONSTRAINT `fk_mutasi_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notifikasi` (
  `id_notifikasi` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_pasien` bigint(20) unsigned NOT NULL,
  `id_pendaftaran` bigint(20) unsigned DEFAULT NULL,
  `jenis_notifikasi` enum('Verifikasi WA','Konfirmasi Pendaftaran','Pengingat Kunjungan','QR Resep','Obat Siap') NOT NULL,
  `channel` enum('WhatsApp','PWA') NOT NULL DEFAULT 'WhatsApp',
  `nomor_tujuan` varchar(20) NOT NULL,
  `pesan` text NOT NULL,
  `status` enum('Pending','Terkirim','Gagal') NOT NULL DEFAULT 'Pending',
  `waktu_dikirim` datetime DEFAULT NULL,
  `response_api` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_notifikasi`),
  KEY `fk_notifikasi_pendaftaran` (`id_pendaftaran`),
  KEY `idx_notifikasi_pasien` (`id_pasien`),
  KEY `idx_notifikasi_status` (`status`),
  KEY `idx_notifikasi_tanggal` (`created_at`),
  CONSTRAINT `fk_notifikasi_pasien` FOREIGN KEY (`id_pasien`) REFERENCES `pasien` (`id_pasien`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_notifikasi_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `obat` (
  `id_obat` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `kode_kemenkes` varchar(50) NOT NULL,
  `nama_obat` varchar(150) NOT NULL,
  `jenis_obat` enum('Jadi','Racikan') NOT NULL DEFAULT 'Jadi',
  `satuan` varchar(30) NOT NULL,
  `cover_bpjs` tinyint(1) NOT NULL DEFAULT 0,
  `stok_rs` int(11) NOT NULL DEFAULT 0,
  `stok_minimum` int(11) NOT NULL DEFAULT 10,
  `harga` decimal(12,2) NOT NULL DEFAULT 0.00,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_obat`),
  UNIQUE KEY `kode_kemenkes` (`kode_kemenkes`),
  KEY `idx_obat_nama` (`nama_obat`),
  KEY `idx_obat_stok` (`stok_rs`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `otp_verifikasi` (
  `id_otp` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_user` bigint(20) unsigned NOT NULL,
  `kode_otp` varchar(10) NOT NULL,
  `tujuan` varchar(20) NOT NULL,
  `expired_at` datetime NOT NULL,
  `verified_at` datetime DEFAULT NULL,
  `attempt` int(10) unsigned NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_otp`),
  KEY `idx_otp_user` (`id_user`),
  KEY `idx_otp_expired` (`expired_at`),
  CONSTRAINT `fk_otp_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pasien` (
  `id_pasien` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_user` bigint(20) unsigned NOT NULL,
  `nik` varchar(16) NOT NULL,
  `nomor_bpjs` varchar(20) DEFAULT NULL,
  `nama_lengkap` varchar(150) NOT NULL,
  `tanggal_lahir` date NOT NULL,
  `jenis_kelamin` enum('L','P') NOT NULL,
  `alamat` text DEFAULT NULL,
  `no_telepon` varchar(20) NOT NULL,
  `terdaftar_satusehat` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_pasien`),
  UNIQUE KEY `id_user` (`id_user`),
  UNIQUE KEY `nik` (`nik`),
  UNIQUE KEY `nomor_bpjs` (`nomor_bpjs`),
  CONSTRAINT `fk_pasien_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pemeriksaan` (
  `id_pemeriksaan` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_pendaftaran` bigint(20) unsigned NOT NULL,
  `id_dokter` bigint(20) unsigned NOT NULL,
  `keluhan` text NOT NULL,
  `tekanan_darah` varchar(20) DEFAULT NULL,
  `suhu_tubuh` decimal(4,1) DEFAULT NULL,
  `berat_badan` decimal(5,2) DEFAULT NULL,
  `tinggi_badan` decimal(5,2) DEFAULT NULL,
  `diagnosis` text NOT NULL,
  `tindakan` text NOT NULL,
  `catatan_dokter` text DEFAULT NULL,
  `waktu_mulai` datetime NOT NULL DEFAULT current_timestamp(),
  `waktu_selesai` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_pemeriksaan`),
  UNIQUE KEY `id_pendaftaran` (`id_pendaftaran`),
  KEY `idx_pemeriksaan_dokter` (`id_dokter`),
  CONSTRAINT `fk_pemeriksaan_dokter` FOREIGN KEY (`id_dokter`) REFERENCES `dokter` (`id_dokter`) ON UPDATE CASCADE,
  CONSTRAINT `fk_pemeriksaan_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pendaftaran_poli` (
  `id_pendaftaran` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_pasien` bigint(20) unsigned NOT NULL,
  `id_jadwal` bigint(20) unsigned NOT NULL,
  `tanggal_kunjungan` date NOT NULL,
  `nomor_antrean` int(10) unsigned NOT NULL,
  `waktu_check_in` datetime DEFAULT NULL,
  `estimasi_jam_masuk` datetime DEFAULT NULL,
  `status_antrean` enum('Menunggu','Dipanggil','Masuk Ruangan','Selesai Pemeriksaan','Batal') NOT NULL DEFAULT 'Menunggu',
  `catatan_pasien` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_pendaftaran`),
  UNIQUE KEY `unique_antrean_harian` (`id_jadwal`,`tanggal_kunjungan`,`nomor_antrean`),
  KEY `idx_pendaftaran_pasien` (`id_pasien`),
  KEY `idx_pendaftaran_jadwal` (`id_jadwal`),
  KEY `idx_pendaftaran_tanggal` (`tanggal_kunjungan`),
  KEY `idx_pendaftaran_status` (`status_antrean`),
  CONSTRAINT `fk_pendaftaran_jadwal` FOREIGN KEY (`id_jadwal`) REFERENCES `jadwal_dokter` (`id_jadwal`) ON UPDATE CASCADE,
  CONSTRAINT `fk_pendaftaran_pasien` FOREIGN KEY (`id_pasien`) REFERENCES `pasien` (`id_pasien`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pengambilan_obat` (
  `id_pengambilan` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_resep` bigint(20) unsigned NOT NULL,
  `id_user_petugas` bigint(20) unsigned NOT NULL,
  `diambil_oleh` enum('Pasien','Keluarga') NOT NULL,
  `nama_pengambil` varchar(150) DEFAULT NULL,
  `waktu_pengambilan` datetime NOT NULL DEFAULT current_timestamp(),
  `metode_verifikasi` enum('QR','QR + PIN','QR + OTP') NOT NULL,
  `status` enum('Berhasil','Ditolak') NOT NULL DEFAULT 'Berhasil',
  `catatan` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_pengambilan`),
  UNIQUE KEY `id_resep` (`id_resep`),
  KEY `idx_pengambilan_petugas` (`id_user_petugas`),
  KEY `idx_pengambilan_waktu` (`waktu_pengambilan`),
  CONSTRAINT `fk_pengambilan_petugas` FOREIGN KEY (`id_user_petugas`) REFERENCES `users` (`id_user`) ON UPDATE CASCADE,
  CONSTRAINT `fk_pengambilan_resep` FOREIGN KEY (`id_resep`) REFERENCES `resep` (`id_resep`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `poliklinik` (
  `id_poli` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `nama_poli` varchar(100) NOT NULL,
  `deskripsi` text DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_poli`),
  UNIQUE KEY `nama_poli` (`nama_poli`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resep` (
  `id_resep` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_pendaftaran` bigint(20) unsigned NOT NULL,
  `kode_qr_unik` varchar(100) NOT NULL,
  `waktu_diterbitkan` datetime NOT NULL DEFAULT current_timestamp(),
  `pilihan_penebusan` enum('Belum Memilih','Apotek RS','Apotek Luar') NOT NULL DEFAULT 'Belum Memilih',
  `estimasi_jam_selesai` datetime DEFAULT NULL,
  `status_resep` enum('Menunggu Pilihan','Antrean Farmasi','Sedang Diracik','Siap Diambil','Selesai Diambil','Dibatalkan') NOT NULL DEFAULT 'Menunggu Pilihan',
  `kadaluarsa_qr` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_resep`),
  UNIQUE KEY `id_pendaftaran` (`id_pendaftaran`),
  UNIQUE KEY `kode_qr_unik` (`kode_qr_unik`),
  KEY `idx_resep_status` (`status_resep`),
  KEY `idx_resep_pilihan` (`pilihan_penebusan`),
  KEY `idx_resep_expired` (`kadaluarsa_qr`),
  CONSTRAINT `fk_resep_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id_user` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('admin','pasien','dokter','farmasi') NOT NULL,
  `nama_lengkap` varchar(100) NOT NULL,
  `no_telepon` varchar(20) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_user`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

