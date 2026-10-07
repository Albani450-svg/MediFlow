-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Oct 07, 2026 at 03:52 AM
-- Server version: 8.0.30
-- PHP Version: 8.1.10

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `medicflow_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `detail_resep`
--

CREATE TABLE `detail_resep` (
  `id_detail` bigint UNSIGNED NOT NULL,
  `id_resep` bigint UNSIGNED NOT NULL,
  `id_obat` bigint UNSIGNED NOT NULL,
  `jumlah` int UNSIGNED NOT NULL,
  `dosis_aturan_pakai` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `instruksi_racikan` text COLLATE utf8mb4_unicode_ci,
  `catatan` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `dokter`
--

CREATE TABLE `dokter` (
  `id_dokter` bigint UNSIGNED NOT NULL,
  `id_user` bigint UNSIGNED NOT NULL,
  `id_poli` bigint UNSIGNED NOT NULL,
  `nama_dokter` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `spesialisasi` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rata_waktu_periksa_menit` int UNSIGNED NOT NULL DEFAULT '15',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `dokter`
--

INSERT INTO `dokter` (`id_dokter`, `id_user`, `id_poli`, `nama_dokter`, `spesialisasi`, `rata_waktu_periksa_menit`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 2, 1, 'Dr. Ahmad', 'Dokter Umum', 15, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `jadwal_dokter`
--

CREATE TABLE `jadwal_dokter` (
  `id_jadwal` bigint UNSIGNED NOT NULL,
  `id_dokter` bigint UNSIGNED NOT NULL,
  `hari` enum('Senin','Selasa','Rabu','Kamis','Jumat','Sabtu','Minggu') COLLATE utf8mb4_unicode_ci NOT NULL,
  `jam_mulai` time NOT NULL,
  `jam_selesai` time NOT NULL,
  `kuota_maksimal` int UNSIGNED NOT NULL DEFAULT '20',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `jadwal_dokter`
--

INSERT INTO `jadwal_dokter` (`id_jadwal`, `id_dokter`, `hari`, `jam_mulai`, `jam_selesai`, `kuota_maksimal`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 1, 'Senin', '08:00:00', '12:00:00', 30, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(2, 1, 'Rabu', '08:00:00', '12:00:00', 30, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `log_aktivitas`
--

CREATE TABLE `log_aktivitas` (
  `id_log` bigint UNSIGNED NOT NULL,
  `id_user` bigint UNSIGNED DEFAULT NULL,
  `aktivitas` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tabel_referensi` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_referensi` bigint UNSIGNED DEFAULT NULL,
  `keterangan` text COLLATE utf8mb4_unicode_ci,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `mutasi_stok`
--

CREATE TABLE `mutasi_stok` (
  `id_mutasi` bigint UNSIGNED NOT NULL,
  `id_obat` bigint UNSIGNED NOT NULL,
  `id_user` bigint UNSIGNED NOT NULL,
  `jenis_mutasi` enum('Masuk','Keluar','Penyesuaian') COLLATE utf8mb4_unicode_ci NOT NULL,
  `jumlah` int UNSIGNED NOT NULL,
  `stok_sebelum` int NOT NULL,
  `stok_sesudah` int NOT NULL,
  `keterangan` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `notifikasi`
--

CREATE TABLE `notifikasi` (
  `id_notifikasi` bigint UNSIGNED NOT NULL,
  `id_pasien` bigint UNSIGNED NOT NULL,
  `id_pendaftaran` bigint UNSIGNED DEFAULT NULL,
  `jenis_notifikasi` enum('Verifikasi WA','Konfirmasi Pendaftaran','Pengingat Kunjungan','QR Resep','Obat Siap') COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` enum('WhatsApp','PWA') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WhatsApp',
  `nomor_tujuan` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `pesan` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('Pending','Terkirim','Gagal') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Pending',
  `waktu_dikirim` datetime DEFAULT NULL,
  `response_api` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `obat`
--

CREATE TABLE `obat` (
  `id_obat` bigint UNSIGNED NOT NULL,
  `kode_kemenkes` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_obat` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `jenis_obat` enum('Jadi','Racikan') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Jadi',
  `satuan` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cover_bpjs` tinyint(1) NOT NULL DEFAULT '0',
  `stok_rs` int NOT NULL DEFAULT '0',
  `stok_minimum` int NOT NULL DEFAULT '10',
  `harga` decimal(12,2) NOT NULL DEFAULT '0.00',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `obat`
--

INSERT INTO `obat` (`id_obat`, `kode_kemenkes`, `nama_obat`, `jenis_obat`, `satuan`, `cover_bpjs`, `stok_rs`, `stok_minimum`, `harga`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'OBT001', 'Paracetamol 500mg', 'Jadi', 'Tablet', 1, 100, 20, '500.00', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(2, 'OBT002', 'Amoxicillin 500mg', 'Jadi', 'Kapsul', 1, 80, 20, '1000.00', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(3, 'OBT003', 'Cetirizine 10mg', 'Jadi', 'Tablet', 0, 50, 10, '1500.00', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(4, 'OBT004', 'Racikan Batuk', 'Racikan', 'Paket', 0, 20, 5, '15000.00', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `otp_verifikasi`
--

CREATE TABLE `otp_verifikasi` (
  `id_otp` bigint UNSIGNED NOT NULL,
  `id_user` bigint UNSIGNED NOT NULL,
  `kode_otp` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tujuan` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expired_at` datetime NOT NULL,
  `verified_at` datetime DEFAULT NULL,
  `attempt` int UNSIGNED NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pasien`
--

CREATE TABLE `pasien` (
  `id_pasien` bigint UNSIGNED NOT NULL,
  `id_user` bigint UNSIGNED NOT NULL,
  `nik` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nomor_bpjs` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nama_lengkap` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tanggal_lahir` date NOT NULL,
  `jenis_kelamin` enum('L','P') COLLATE utf8mb4_unicode_ci NOT NULL,
  `alamat` text COLLATE utf8mb4_unicode_ci,
  `no_telepon` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `terdaftar_satusehat` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `pasien`
--

INSERT INTO `pasien` (`id_pasien`, `id_user`, `nik`, `nomor_bpjs`, `nama_lengkap`, `tanggal_lahir`, `jenis_kelamin`, `alamat`, `no_telepon`, `terdaftar_satusehat`, `created_at`, `updated_at`) VALUES
(2, 1, '3471001234567890', '0001234567890', 'Budi Santoso Edit', '2000-01-15', 'L', 'Sleman, Yogyakarta', '089876543210', 1, '2026-10-07 03:46:17', '2026-10-07 03:46:17');

-- --------------------------------------------------------

--
-- Table structure for table `pemeriksaan`
--

CREATE TABLE `pemeriksaan` (
  `id_pemeriksaan` bigint UNSIGNED NOT NULL,
  `id_pendaftaran` bigint UNSIGNED NOT NULL,
  `id_dokter` bigint UNSIGNED NOT NULL,
  `keluhan` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `tekanan_darah` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `suhu_tubuh` decimal(4,1) DEFAULT NULL,
  `berat_badan` decimal(5,2) DEFAULT NULL,
  `tinggi_badan` decimal(5,2) DEFAULT NULL,
  `diagnosis` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `tindakan` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `catatan_dokter` text COLLATE utf8mb4_unicode_ci,
  `waktu_mulai` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `waktu_selesai` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pendaftaran_poli`
--

CREATE TABLE `pendaftaran_poli` (
  `id_pendaftaran` bigint UNSIGNED NOT NULL,
  `id_pasien` bigint UNSIGNED NOT NULL,
  `id_jadwal` bigint UNSIGNED NOT NULL,
  `tanggal_kunjungan` date NOT NULL,
  `nomor_antrean` int UNSIGNED NOT NULL,
  `waktu_check_in` datetime DEFAULT NULL,
  `estimasi_jam_masuk` datetime DEFAULT NULL,
  `status_antrean` enum('Menunggu','Dipanggil','Masuk Ruangan','Selesai Pemeriksaan','Batal') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Menunggu',
  `catatan_pasien` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pengambilan_obat`
--

CREATE TABLE `pengambilan_obat` (
  `id_pengambilan` bigint UNSIGNED NOT NULL,
  `id_resep` bigint UNSIGNED NOT NULL,
  `id_user_petugas` bigint UNSIGNED NOT NULL,
  `diambil_oleh` enum('Pasien','Keluarga') COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_pengambil` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `waktu_pengambilan` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `metode_verifikasi` enum('QR','QR + PIN','QR + OTP') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('Berhasil','Ditolak') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Berhasil',
  `catatan` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `poliklinik`
--

CREATE TABLE `poliklinik` (
  `id_poli` bigint UNSIGNED NOT NULL,
  `nama_poli` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `deskripsi` text COLLATE utf8mb4_unicode_ci,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `poliklinik`
--

INSERT INTO `poliklinik` (`id_poli`, `nama_poli`, `deskripsi`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'Poli Umum', 'Pelayanan kesehatan umum', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(2, 'Poli Gigi', 'Pelayanan kesehatan gigi dan mulut', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(3, 'Poli Anak', 'Pelayanan kesehatan anak', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(4, 'Poli Mata', 'Pelayanan kesehatan mata', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `resep`
--

CREATE TABLE `resep` (
  `id_resep` bigint UNSIGNED NOT NULL,
  `id_pendaftaran` bigint UNSIGNED NOT NULL,
  `kode_qr_unik` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `waktu_diterbitkan` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pilihan_penebusan` enum('Belum Memilih','Apotek RS','Apotek Luar') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Belum Memilih',
  `estimasi_jam_selesai` datetime DEFAULT NULL,
  `status_resep` enum('Menunggu Pilihan','Antrean Farmasi','Sedang Diracik','Siap Diambil','Selesai Diambil','Dibatalkan') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Menunggu Pilihan',
  `kadaluarsa_qr` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id_user` bigint UNSIGNED NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('admin','pasien','dokter','farmasi') COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_lengkap` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `no_telepon` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id_user`, `username`, `password_hash`, `role`, `nama_lengkap`, `no_telepon`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'admin', '$2y$10$abcdefghijklmnopqrstuv', 'admin', 'Administrator', '081234567890', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(2, 'dokter01', '$2y$10$abcdefghijklmnopqrstuv', 'dokter', 'Dr. Ahmad', '081234567891', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(3, 'farmasi01', '$2y$10$abcdefghijklmnopqrstuv', 'farmasi', 'Petugas Farmasi', '081234567892', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `detail_resep`
--
ALTER TABLE `detail_resep`
  ADD PRIMARY KEY (`id_detail`),
  ADD KEY `idx_detail_resep` (`id_resep`),
  ADD KEY `idx_detail_obat` (`id_obat`);

--
-- Indexes for table `dokter`
--
ALTER TABLE `dokter`
  ADD PRIMARY KEY (`id_dokter`),
  ADD UNIQUE KEY `id_user` (`id_user`),
  ADD KEY `idx_dokter_poli` (`id_poli`);

--
-- Indexes for table `jadwal_dokter`
--
ALTER TABLE `jadwal_dokter`
  ADD PRIMARY KEY (`id_jadwal`),
  ADD KEY `idx_jadwal_dokter` (`id_dokter`),
  ADD KEY `idx_jadwal_hari` (`hari`);

--
-- Indexes for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  ADD PRIMARY KEY (`id_log`),
  ADD KEY `idx_log_user` (`id_user`),
  ADD KEY `idx_log_aktivitas` (`aktivitas`),
  ADD KEY `idx_log_tanggal` (`created_at`);

--
-- Indexes for table `mutasi_stok`
--
ALTER TABLE `mutasi_stok`
  ADD PRIMARY KEY (`id_mutasi`),
  ADD KEY `idx_mutasi_obat` (`id_obat`),
  ADD KEY `idx_mutasi_user` (`id_user`),
  ADD KEY `idx_mutasi_tanggal` (`created_at`);

--
-- Indexes for table `notifikasi`
--
ALTER TABLE `notifikasi`
  ADD PRIMARY KEY (`id_notifikasi`),
  ADD KEY `fk_notifikasi_pendaftaran` (`id_pendaftaran`),
  ADD KEY `idx_notifikasi_pasien` (`id_pasien`),
  ADD KEY `idx_notifikasi_status` (`status`),
  ADD KEY `idx_notifikasi_tanggal` (`created_at`);

--
-- Indexes for table `obat`
--
ALTER TABLE `obat`
  ADD PRIMARY KEY (`id_obat`),
  ADD UNIQUE KEY `kode_kemenkes` (`kode_kemenkes`),
  ADD KEY `idx_obat_nama` (`nama_obat`),
  ADD KEY `idx_obat_stok` (`stok_rs`);

--
-- Indexes for table `otp_verifikasi`
--
ALTER TABLE `otp_verifikasi`
  ADD PRIMARY KEY (`id_otp`),
  ADD KEY `idx_otp_user` (`id_user`),
  ADD KEY `idx_otp_expired` (`expired_at`);

--
-- Indexes for table `pasien`
--
ALTER TABLE `pasien`
  ADD PRIMARY KEY (`id_pasien`),
  ADD UNIQUE KEY `id_user` (`id_user`),
  ADD UNIQUE KEY `nik` (`nik`),
  ADD UNIQUE KEY `nomor_bpjs` (`nomor_bpjs`);

--
-- Indexes for table `pemeriksaan`
--
ALTER TABLE `pemeriksaan`
  ADD PRIMARY KEY (`id_pemeriksaan`),
  ADD UNIQUE KEY `id_pendaftaran` (`id_pendaftaran`),
  ADD KEY `idx_pemeriksaan_dokter` (`id_dokter`);

--
-- Indexes for table `pendaftaran_poli`
--
ALTER TABLE `pendaftaran_poli`
  ADD PRIMARY KEY (`id_pendaftaran`),
  ADD UNIQUE KEY `unique_antrean_harian` (`id_jadwal`,`tanggal_kunjungan`,`nomor_antrean`),
  ADD KEY `idx_pendaftaran_pasien` (`id_pasien`),
  ADD KEY `idx_pendaftaran_jadwal` (`id_jadwal`),
  ADD KEY `idx_pendaftaran_tanggal` (`tanggal_kunjungan`),
  ADD KEY `idx_pendaftaran_status` (`status_antrean`);

--
-- Indexes for table `pengambilan_obat`
--
ALTER TABLE `pengambilan_obat`
  ADD PRIMARY KEY (`id_pengambilan`),
  ADD UNIQUE KEY `id_resep` (`id_resep`),
  ADD KEY `idx_pengambilan_petugas` (`id_user_petugas`),
  ADD KEY `idx_pengambilan_waktu` (`waktu_pengambilan`);

--
-- Indexes for table `poliklinik`
--
ALTER TABLE `poliklinik`
  ADD PRIMARY KEY (`id_poli`),
  ADD UNIQUE KEY `nama_poli` (`nama_poli`);

--
-- Indexes for table `resep`
--
ALTER TABLE `resep`
  ADD PRIMARY KEY (`id_resep`),
  ADD UNIQUE KEY `id_pendaftaran` (`id_pendaftaran`),
  ADD UNIQUE KEY `kode_qr_unik` (`kode_qr_unik`),
  ADD KEY `idx_resep_status` (`status_resep`),
  ADD KEY `idx_resep_pilihan` (`pilihan_penebusan`),
  ADD KEY `idx_resep_expired` (`kadaluarsa_qr`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id_user`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `detail_resep`
--
ALTER TABLE `detail_resep`
  MODIFY `id_detail` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `dokter`
--
ALTER TABLE `dokter`
  MODIFY `id_dokter` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `jadwal_dokter`
--
ALTER TABLE `jadwal_dokter`
  MODIFY `id_jadwal` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  MODIFY `id_log` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `mutasi_stok`
--
ALTER TABLE `mutasi_stok`
  MODIFY `id_mutasi` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `notifikasi`
--
ALTER TABLE `notifikasi`
  MODIFY `id_notifikasi` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `obat`
--
ALTER TABLE `obat`
  MODIFY `id_obat` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `otp_verifikasi`
--
ALTER TABLE `otp_verifikasi`
  MODIFY `id_otp` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pasien`
--
ALTER TABLE `pasien`
  MODIFY `id_pasien` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `pemeriksaan`
--
ALTER TABLE `pemeriksaan`
  MODIFY `id_pemeriksaan` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pendaftaran_poli`
--
ALTER TABLE `pendaftaran_poli`
  MODIFY `id_pendaftaran` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pengambilan_obat`
--
ALTER TABLE `pengambilan_obat`
  MODIFY `id_pengambilan` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `poliklinik`
--
ALTER TABLE `poliklinik`
  MODIFY `id_poli` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `resep`
--
ALTER TABLE `resep`
  MODIFY `id_resep` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id_user` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `detail_resep`
--
ALTER TABLE `detail_resep`
  ADD CONSTRAINT `fk_detail_obat` FOREIGN KEY (`id_obat`) REFERENCES `obat` (`id_obat`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_detail_resep` FOREIGN KEY (`id_resep`) REFERENCES `resep` (`id_resep`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `dokter`
--
ALTER TABLE `dokter`
  ADD CONSTRAINT `fk_dokter_poli` FOREIGN KEY (`id_poli`) REFERENCES `poliklinik` (`id_poli`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_dokter_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `jadwal_dokter`
--
ALTER TABLE `jadwal_dokter`
  ADD CONSTRAINT `fk_jadwal_dokter` FOREIGN KEY (`id_dokter`) REFERENCES `dokter` (`id_dokter`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  ADD CONSTRAINT `fk_log_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `mutasi_stok`
--
ALTER TABLE `mutasi_stok`
  ADD CONSTRAINT `fk_mutasi_obat` FOREIGN KEY (`id_obat`) REFERENCES `obat` (`id_obat`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_mutasi_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `notifikasi`
--
ALTER TABLE `notifikasi`
  ADD CONSTRAINT `fk_notifikasi_pasien` FOREIGN KEY (`id_pasien`) REFERENCES `pasien` (`id_pasien`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_notifikasi_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `otp_verifikasi`
--
ALTER TABLE `otp_verifikasi`
  ADD CONSTRAINT `fk_otp_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `pasien`
--
ALTER TABLE `pasien`
  ADD CONSTRAINT `fk_pasien_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `pemeriksaan`
--
ALTER TABLE `pemeriksaan`
  ADD CONSTRAINT `fk_pemeriksaan_dokter` FOREIGN KEY (`id_dokter`) REFERENCES `dokter` (`id_dokter`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pemeriksaan_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `pendaftaran_poli`
--
ALTER TABLE `pendaftaran_poli`
  ADD CONSTRAINT `fk_pendaftaran_jadwal` FOREIGN KEY (`id_jadwal`) REFERENCES `jadwal_dokter` (`id_jadwal`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pendaftaran_pasien` FOREIGN KEY (`id_pasien`) REFERENCES `pasien` (`id_pasien`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `pengambilan_obat`
--
ALTER TABLE `pengambilan_obat`
  ADD CONSTRAINT `fk_pengambilan_petugas` FOREIGN KEY (`id_user_petugas`) REFERENCES `users` (`id_user`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pengambilan_resep` FOREIGN KEY (`id_resep`) REFERENCES `resep` (`id_resep`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `resep`
--
ALTER TABLE `resep`
  ADD CONSTRAINT `fk_resep_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON DELETE RESTRICT ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
