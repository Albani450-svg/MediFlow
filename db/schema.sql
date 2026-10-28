-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 22, 2026 at 09:35 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.5.4
--
-- Isi data dari dump MySQL 8.0.30 (22 Oktober 2026, 09:35) dan lembar login dummy.
-- Skema tetap accounts, admins, dokter, farmasi_staff, dan patients.
-- users.id_user menjadi accounts.account_id. Tabel pasien menjadi patients.
-- Hash di bawah masih placeholder. Jalankan db/sesuaikan-akun-demo.sql sesudah impor.

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `mediflow` (sudah ada di DBeaver, localhost:3306). Jangan buat database baru.
--

-- --------------------------------------------------------

--
-- Table structure for table `accounts`
--

CREATE TABLE `accounts` (
  `account_id` bigint(20) UNSIGNED NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `accounts`
--

INSERT INTO `accounts` (`account_id`, `username`, `email`, `password_hash`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'admin', NULL, '$2y$10$abcdefghijklmnopqrstuv', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(2, 'dokter01', NULL, '$2y$10$abcdefghijklmnopqrstuv', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(3, 'farmasi01', NULL, '$2y$10$abcdefghijklmnopqrstuv', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(100, 'dummy_pasien01', NULL, '$2y$10$abcdefghijklmnopqrstuv', 1, '2026-10-22 09:30:29', '2026-10-22 09:30:29'),
(101, 'dummy_pasien02', NULL, '$2y$10$abcdefghijklmnopqrstuv', 1, '2026-10-22 09:30:29', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `admins`
--

CREATE TABLE `admins` (
  `id_admin` bigint(20) UNSIGNED NOT NULL,
  `account_id` bigint(20) UNSIGNED NOT NULL,
  `nama` varchar(150) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `is_super` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `admins`
--

INSERT INTO `admins` (`id_admin`, `account_id`, `nama`, `phone`, `is_super`, `created_at`, `updated_at`) VALUES
(1, 1, 'Administrator', '081234567890', 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `auth_rate_limits`
--

CREATE TABLE `auth_rate_limits` (
  `scope_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `window_started_at` datetime NOT NULL,
  `attempt_count` int(10) UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dump 09:35 tidak berisi baris auth_rate_limits.

-- --------------------------------------------------------

--
-- Table structure for table `detail_resep`
--

CREATE TABLE `detail_resep` (
  `id_detail` bigint(20) UNSIGNED NOT NULL,
  `id_resep` bigint(20) UNSIGNED NOT NULL,
  `id_obat` bigint(20) UNSIGNED NOT NULL,
  `jumlah` int(10) UNSIGNED NOT NULL,
  `dosis_aturan_pakai` varchar(255) NOT NULL,
  `instruksi_racikan` text DEFAULT NULL,
  `catatan` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `detail_resep`
--

INSERT INTO `detail_resep` (`id_detail`, `id_resep`, `id_obat`, `jumlah`, `dosis_aturan_pakai`, `instruksi_racikan`, `catatan`, `created_at`, `updated_at`) VALUES
(100, 100, 1, 10, 'DATA DUMMY: sesuai instruksi dokter', NULL, 'Simulasi saja; bukan petunjuk penggunaan obat nyata', '2026-10-22 09:30:29', '2026-10-22 09:30:29'),
(101, 100, 3, 5, 'DATA DUMMY: sesuai instruksi dokter', NULL, 'Simulasi saja; bukan petunjuk penggunaan obat nyata', '2026-10-22 09:30:29', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `dokter`
--

CREATE TABLE `dokter` (
  `id_dokter` bigint(20) UNSIGNED NOT NULL,
  `account_id` bigint(20) UNSIGNED NOT NULL,
  `id_poli` bigint(20) UNSIGNED NOT NULL,
  `nama_dokter` varchar(150) NOT NULL,
  `spesialisasi` varchar(100) DEFAULT NULL,
  `rata_waktu_periksa_menit` int(10) UNSIGNED NOT NULL DEFAULT 15,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `dokter`
--

INSERT INTO `dokter` (`id_dokter`, `account_id`, `id_poli`, `nama_dokter`, `spesialisasi`, `rata_waktu_periksa_menit`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 2, 1, 'Dr. Ahmad', 'Dokter Umum', 15, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `farmasi_staff`
--

CREATE TABLE `farmasi_staff` (
  `id_farmasi` bigint(20) UNSIGNED NOT NULL,
  `account_id` bigint(20) UNSIGNED NOT NULL,
  `nama` varchar(150) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `farmasi_staff`
--

INSERT INTO `farmasi_staff` (`id_farmasi`, `account_id`, `nama`, `phone`, `created_at`, `updated_at`) VALUES
(3, 3, 'Petugas Farmasi', '081234567892', '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `jadwal_dokter`
--

CREATE TABLE `jadwal_dokter` (
  `id_jadwal` bigint(20) UNSIGNED NOT NULL,
  `id_dokter` bigint(20) UNSIGNED NOT NULL,
  `hari` enum('Senin','Selasa','Rabu','Kamis','Jumat','Sabtu','Minggu') NOT NULL,
  `jam_mulai` time NOT NULL,
  `jam_selesai` time NOT NULL,
  `kuota_maksimal` int(10) UNSIGNED NOT NULL DEFAULT 20,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
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
  `id_log` bigint(20) UNSIGNED NOT NULL,
  `account_id` bigint(20) UNSIGNED DEFAULT NULL,
  `aktivitas` varchar(100) NOT NULL,
  `tabel_referensi` varchar(100) DEFAULT NULL,
  `id_referensi` bigint(20) UNSIGNED DEFAULT NULL,
  `keterangan` text DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `log_aktivitas`
--

INSERT INTO `log_aktivitas` (`id_log`, `account_id`, `aktivitas`, `tabel_referensi`, `id_referensi`, `keterangan`, `ip_address`, `created_at`) VALUES
(100, 1, 'SEED_DATA_DUMMY', 'pendaftaran_poli', 100, 'Data dummy MedicFlow dimasukkan untuk pengujian lokal', '127.0.0.1', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `mutasi_stok`
--

CREATE TABLE `mutasi_stok` (
  `id_mutasi` bigint(20) UNSIGNED NOT NULL,
  `id_obat` bigint(20) UNSIGNED NOT NULL,
  `account_user` bigint(20) UNSIGNED NOT NULL,
  `jenis_mutasi` enum('Masuk','Keluar','Penyesuaian') NOT NULL,
  `jumlah` int(10) UNSIGNED NOT NULL,
  `stok_sebelum` int(11) NOT NULL,
  `stok_sesudah` int(11) NOT NULL,
  `keterangan` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `mutasi_stok`
--

INSERT INTO `mutasi_stok` (`id_mutasi`, `id_obat`, `account_user`, `jenis_mutasi`, `jumlah`, `stok_sebelum`, `stok_sesudah`, `keterangan`, `created_at`) VALUES
(100, 1, 3, 'Penyesuaian', 100, 100, 100, 'DATA DUMMY - pencatatan simulasi; stok tabel obat tidak diubah oleh seed ini', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `notifikasi`
--

CREATE TABLE `notifikasi` (
  `id_notifikasi` bigint(20) UNSIGNED NOT NULL,
  `id_pasien` bigint(20) UNSIGNED NOT NULL,
  `id_pendaftaran` bigint(20) UNSIGNED DEFAULT NULL,
  `jenis_notifikasi` enum('Verifikasi WA','Konfirmasi Pendaftaran','Pengingat Kunjungan','QR Resep','Obat Siap') NOT NULL,
  `channel` enum('WhatsApp','PWA') NOT NULL DEFAULT 'WhatsApp',
  `nomor_tujuan` varchar(20) NOT NULL,
  `pesan` text NOT NULL,
  `status` enum('Pending','Terkirim','Gagal') NOT NULL DEFAULT 'Pending',
  `waktu_dikirim` datetime DEFAULT NULL,
  `response_api` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `notifikasi`
--

INSERT INTO `notifikasi` (`id_notifikasi`, `id_pasien`, `id_pendaftaran`, `jenis_notifikasi`, `channel`, `nomor_tujuan`, `pesan`, `status`, `waktu_dikirim`, `response_api`, `created_at`) VALUES
(100, 100, 100, 'Konfirmasi Pendaftaran', 'WhatsApp', '081200000100', 'DATA DUMMY: Pendaftaran MedicFlow berhasil dicatat. Silakan periksa jadwal melalui aplikasi.', 'Pending', NULL, 'Seed dummy; belum dikirim ke WhatsApp', '2026-10-22 09:30:29'),
(101, 101, 101, 'Pengingat Kunjungan', 'WhatsApp', '081200000101', 'DATA DUMMY: Ini contoh pengingat kunjungan MedicFlow untuk pengujian.', 'Pending', NULL, 'Seed dummy; belum dikirim ke WhatsApp', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `obat`
--

CREATE TABLE `obat` (
  `id_obat` bigint(20) UNSIGNED NOT NULL,
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
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `obat`
--

INSERT INTO `obat` (`id_obat`, `kode_kemenkes`, `nama_obat`, `jenis_obat`, `satuan`, `cover_bpjs`, `stok_rs`, `stok_minimum`, `harga`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'OBT001', 'Paracetamol 500mg', 'Jadi', 'Tablet', 1, 100, 20, 500.00, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(2, 'OBT002', 'Amoxicillin 500mg', 'Jadi', 'Kapsul', 1, 80, 20, 1000.00, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(3, 'OBT003', 'Cetirizine 10mg', 'Jadi', 'Tablet', 0, 50, 10, 1500.00, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44'),
(4, 'OBT004', 'Racikan Batuk', 'Racikan', 'Paket', 0, 20, 5, 15000.00, 1, '2026-10-02 20:53:44', '2026-10-02 20:53:44');

-- --------------------------------------------------------

--
-- Table structure for table `otp_verifikasi`
--

CREATE TABLE `otp_verifikasi` (
  `id_otp` bigint(20) UNSIGNED NOT NULL,
  `account_id` bigint(20) UNSIGNED NOT NULL,
  `kode_otp` varchar(64) NOT NULL,
  `tujuan` varchar(20) NOT NULL,
  `expired_at` datetime NOT NULL,
  `verified_at` datetime DEFAULT NULL,
  `attempt` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `otp_verifikasi`
--

INSERT INTO `otp_verifikasi` (`id_otp`, `account_id`, `kode_otp`, `tujuan`, `expired_at`, `verified_at`, `attempt`, `created_at`) VALUES
(100, 100, '123456', '081200000100', '2026-10-26 10:00:00', NULL, 0, '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `patients`
--

CREATE TABLE `patients` (
  `id_pasien` bigint(20) UNSIGNED NOT NULL,
  `account_id` bigint(20) UNSIGNED NOT NULL,
  `no_rekam_medis` varchar(24) DEFAULT NULL,
  `nik` varchar(16) NOT NULL,
  `nama_lengkap` varchar(150) NOT NULL,
  `tanggal_lahir` date NOT NULL,
  `jenis_kelamin` enum('L','P') NOT NULL,
  `alamat` text DEFAULT NULL,
  `no_telepon` varchar(20) NOT NULL,
  `terdaftar_satusehat` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `patients`
--

INSERT INTO `patients` (`id_pasien`, `account_id`, `no_rekam_medis`, `nik`, `nama_lengkap`, `tanggal_lahir`, `jenis_kelamin`, `alamat`, `no_telepon`, `terdaftar_satusehat`, `created_at`, `updated_at`) VALUES
(2, 1, NULL, '3471001234567890', 'Budi Santoso Edit', '2000-01-15', 'L', 'Sleman, Yogyakarta', '089876543210', 1, '2026-10-07 03:46:17', '2026-10-20 07:10:22'),
(5, 2, NULL, 'TEST000000000001', 'Pasien Uji MedicFlow', '2000-01-15', 'L', 'Alamat Uji', '081234567890', 0, '2026-10-20 06:53:15', '2026-10-20 06:53:15'),
(100, 100, NULL, 'TEST000000000100', 'Siti Rahma Dummy', '1998-04-12', 'P', 'Alamat Dummy 100, Yogyakarta', '081200000100', 0, '2026-10-22 09:30:29', '2026-10-22 09:30:29'),
(101, 101, NULL, 'TEST000000000101', 'Andi Pratama Dummy', '1995-09-23', 'L', 'Alamat Dummy 101, Yogyakarta', '081200000101', 0, '2026-10-22 09:30:29', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `pemeriksaan`
--

CREATE TABLE `pemeriksaan` (
  `id_pemeriksaan` bigint(20) UNSIGNED NOT NULL,
  `id_pendaftaran` bigint(20) UNSIGNED NOT NULL,
  `id_dokter` bigint(20) UNSIGNED NOT NULL,
  `keluhan` text NOT NULL,
  `tekanan_darah` varchar(20) DEFAULT NULL,
  `suhu_tubuh` decimal(4,1) DEFAULT NULL,
  `berat_badan` decimal(5,2) DEFAULT NULL,
  `tinggi_badan` decimal(5,2) DEFAULT NULL,
  `diagnosis` text DEFAULT NULL,
  `tindakan` text DEFAULT NULL,
  `catatan_dokter` text DEFAULT NULL,
  `waktu_mulai` datetime NOT NULL DEFAULT current_timestamp(),
  `waktu_selesai` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `pemeriksaan`
--

INSERT INTO `pemeriksaan` (`id_pemeriksaan`, `id_pendaftaran`, `id_dokter`, `keluhan`, `tekanan_darah`, `suhu_tubuh`, `berat_badan`, `tinggi_badan`, `diagnosis`, `tindakan`, `catatan_dokter`, `waktu_mulai`, `waktu_selesai`, `created_at`, `updated_at`) VALUES
(100, 100, 1, 'DATA DUMMY: keluhan sakit kepala ringan', '120/80', 36.7, 58.00, 160.00, 'DATA DUMMY: evaluasi keluhan umum', 'Pemeriksaan umum dan edukasi', 'Data simulasi, bukan rekam medis nyata', '2026-10-26 08:00:00', '2026-10-26 08:15:00', '2026-10-22 09:30:29', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `pendaftaran_poli`
--

CREATE TABLE `pendaftaran_poli` (
  `id_pendaftaran` bigint(20) UNSIGNED NOT NULL,
  `id_pasien` bigint(20) UNSIGNED NOT NULL,
  `id_jadwal` bigint(20) UNSIGNED NOT NULL,
  `tanggal_kunjungan` date NOT NULL,
  `nomor_antrean` int(10) UNSIGNED NOT NULL,
  `waktu_check_in` datetime DEFAULT NULL,
  `estimasi_jam_masuk` datetime DEFAULT NULL,
  `status_antrean` enum('Menunggu','Dipanggil','Masuk Ruangan','Selesai Pemeriksaan','Batal') NOT NULL DEFAULT 'Menunggu',
  `catatan_pasien` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `pendaftaran_poli`
--

INSERT INTO `pendaftaran_poli` (`id_pendaftaran`, `id_pasien`, `id_jadwal`, `tanggal_kunjungan`, `nomor_antrean`, `waktu_check_in`, `estimasi_jam_masuk`, `status_antrean`, `catatan_pasien`, `created_at`, `updated_at`) VALUES
(100, 100, 1, '2026-10-26', 1, '2026-10-26 07:45:00', '2026-10-26 08:00:00', 'Selesai Pemeriksaan', 'DATA DUMMY - kontrol umum', '2026-10-22 09:30:29', '2026-10-22 09:30:29'),
(101, 101, 1, '2026-10-26', 2, NULL, '2026-10-26 08:15:00', 'Menunggu', 'DATA DUMMY - pemeriksaan umum', '2026-10-22 09:30:29', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `pengambilan_obat`
--

CREATE TABLE `pengambilan_obat` (
  `id_pengambilan` bigint(20) UNSIGNED NOT NULL,
  `id_resep` bigint(20) UNSIGNED NOT NULL,
  `account_petugas` bigint(20) UNSIGNED NOT NULL,
  `diambil_oleh` enum('Pasien','Keluarga') NOT NULL,
  `nama_pengambil` varchar(150) DEFAULT NULL,
  `waktu_pengambilan` datetime NOT NULL DEFAULT current_timestamp(),
  `metode_verifikasi` enum('QR','QR + PIN','QR + OTP') NOT NULL,
  `status` enum('Berhasil','Ditolak') NOT NULL DEFAULT 'Berhasil',
  `catatan` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `pengambilan_obat`
--

INSERT INTO `pengambilan_obat` (`id_pengambilan`, `id_resep`, `account_petugas`, `diambil_oleh`, `nama_pengambil`, `waktu_pengambilan`, `metode_verifikasi`, `status`, `catatan`, `created_at`, `updated_at`) VALUES
(100, 100, 3, 'Pasien', 'Siti Rahma Dummy', '2026-10-26 09:05:00', 'QR', 'Berhasil', 'DATA DUMMY - simulasi pengambilan', '2026-10-22 09:30:29', '2026-10-22 09:30:29');

-- --------------------------------------------------------

--
-- Table structure for table `poliklinik`
--

CREATE TABLE `poliklinik` (
  `id_poli` bigint(20) UNSIGNED NOT NULL,
  `nama_poli` varchar(100) NOT NULL,
  `deskripsi` text DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
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
  `id_resep` bigint(20) UNSIGNED NOT NULL,
  `id_pendaftaran` bigint(20) UNSIGNED NOT NULL,
  `kode_qr_unik` varchar(100) NOT NULL,
  `waktu_diterbitkan` datetime NOT NULL DEFAULT current_timestamp(),
  `pilihan_penebusan` enum('Belum Memilih','Apotek RS','Apotek Luar') NOT NULL DEFAULT 'Belum Memilih',
  `estimasi_jam_selesai` datetime DEFAULT NULL,
  `status_resep` enum('Menunggu Pilihan','Antrean Farmasi','Sedang Diracik','Siap Diambil','Selesai Diambil','Dibatalkan') NOT NULL DEFAULT 'Menunggu Pilihan',
  `kadaluarsa_qr` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `resep`
--

INSERT INTO `resep` (`id_resep`, `id_pendaftaran`, `kode_qr_unik`, `waktu_diterbitkan`, `pilihan_penebusan`, `estimasi_jam_selesai`, `status_resep`, `kadaluarsa_qr`, `created_at`, `updated_at`) VALUES
(100, 100, 'DUMMY-QR-MEDICFLOW-100', '2026-10-26 08:16:00', 'Apotek RS', '2026-10-26 09:00:00', 'Selesai Diambil', '2026-10-27 08:16:00', '2026-10-22 09:30:29', '2026-10-22 09:30:29');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `accounts`
--
ALTER TABLE `accounts`
  ADD PRIMARY KEY (`account_id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- Indexes for table `admins`
--
ALTER TABLE `admins`
  ADD PRIMARY KEY (`id_admin`),
  ADD UNIQUE KEY `account_id` (`account_id`);

--
-- Indexes for table `auth_rate_limits`
--
ALTER TABLE `auth_rate_limits`
  ADD PRIMARY KEY (`scope_key`),
  ADD KEY `idx_auth_rate_window` (`window_started_at`);

--
-- Indexes for table `detail_resep`
--
ALTER TABLE `detail_resep`
  ADD PRIMARY KEY (`id_detail`),
  ADD KEY `fk_detail_resep` (`id_resep`),
  ADD KEY `fk_detail_obat` (`id_obat`);

--
-- Indexes for table `dokter`
--
ALTER TABLE `dokter`
  ADD PRIMARY KEY (`id_dokter`),
  ADD UNIQUE KEY `account_id` (`account_id`),
  ADD KEY `fk_dokter_poli` (`id_poli`);

--
-- Indexes for table `farmasi_staff`
--
ALTER TABLE `farmasi_staff`
  ADD PRIMARY KEY (`id_farmasi`),
  ADD UNIQUE KEY `account_id` (`account_id`);

--
-- Indexes for table `jadwal_dokter`
--
ALTER TABLE `jadwal_dokter`
  ADD PRIMARY KEY (`id_jadwal`),
  ADD KEY `fk_jadwal_dokter` (`id_dokter`);

--
-- Indexes for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  ADD PRIMARY KEY (`id_log`),
  ADD KEY `fk_log_account` (`account_id`);

--
-- Indexes for table `mutasi_stok`
--
ALTER TABLE `mutasi_stok`
  ADD PRIMARY KEY (`id_mutasi`),
  ADD KEY `fk_mutasi_obat` (`id_obat`),
  ADD KEY `fk_mutasi_account` (`account_user`);

--
-- Indexes for table `notifikasi`
--
ALTER TABLE `notifikasi`
  ADD PRIMARY KEY (`id_notifikasi`),
  ADD KEY `fk_notifikasi_pasien` (`id_pasien`),
  ADD KEY `fk_notifikasi_pendaftaran` (`id_pendaftaran`);

--
-- Indexes for table `obat`
--
ALTER TABLE `obat`
  ADD PRIMARY KEY (`id_obat`),
  ADD UNIQUE KEY `kode_kemenkes` (`kode_kemenkes`);

--
-- Indexes for table `otp_verifikasi`
--
ALTER TABLE `otp_verifikasi`
  ADD PRIMARY KEY (`id_otp`),
  ADD KEY `fk_otp_account` (`account_id`),
  ADD KEY `idx_otp_expired` (`expired_at`);

--
-- Indexes for table `patients`
--
ALTER TABLE `patients`
  ADD PRIMARY KEY (`id_pasien`),
  ADD UNIQUE KEY `account_id` (`account_id`),
  ADD UNIQUE KEY `nik` (`nik`),
  ADD UNIQUE KEY `no_rekam_medis` (`no_rekam_medis`);

--
-- Indexes for table `pemeriksaan`
--
ALTER TABLE `pemeriksaan`
  ADD PRIMARY KEY (`id_pemeriksaan`),
  ADD UNIQUE KEY `id_pendaftaran` (`id_pendaftaran`),
  ADD KEY `fk_pemeriksaan_dokter` (`id_dokter`);

--
-- Indexes for table `pendaftaran_poli`
--
ALTER TABLE `pendaftaran_poli`
  ADD PRIMARY KEY (`id_pendaftaran`),
  ADD UNIQUE KEY `unique_antrean_harian` (`id_jadwal`,`tanggal_kunjungan`,`nomor_antrean`),
  ADD KEY `fk_pendaftaran_pasien` (`id_pasien`);

--
-- Indexes for table `pengambilan_obat`
--
ALTER TABLE `pengambilan_obat`
  ADD PRIMARY KEY (`id_pengambilan`),
  ADD KEY `fk_pengambilan_resep` (`id_resep`),
  ADD KEY `fk_pengambilan_petugas_account` (`account_petugas`);

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
  ADD UNIQUE KEY `kode_qr_unik` (`kode_qr_unik`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `accounts`
--
ALTER TABLE `accounts`
  MODIFY `account_id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=102;

--
-- AUTO_INCREMENT for table `admins`
--
ALTER TABLE `admins`
  MODIFY `id_admin` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `detail_resep`
--
ALTER TABLE `detail_resep`
  MODIFY `id_detail` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=102;

--
-- AUTO_INCREMENT for table `dokter`
--
ALTER TABLE `dokter`
  MODIFY `id_dokter` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `farmasi_staff`
--
ALTER TABLE `farmasi_staff`
  MODIFY `id_farmasi` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `jadwal_dokter`
--
ALTER TABLE `jadwal_dokter`
  MODIFY `id_jadwal` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  MODIFY `id_log` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT for table `mutasi_stok`
--
ALTER TABLE `mutasi_stok`
  MODIFY `id_mutasi` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT for table `notifikasi`
--
ALTER TABLE `notifikasi`
  MODIFY `id_notifikasi` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=102;

--
-- AUTO_INCREMENT for table `obat`
--
ALTER TABLE `obat`
  MODIFY `id_obat` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `otp_verifikasi`
--
ALTER TABLE `otp_verifikasi`
  MODIFY `id_otp` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT for table `patients`
--
ALTER TABLE `patients`
  MODIFY `id_pasien` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=102;

--
-- AUTO_INCREMENT for table `pemeriksaan`
--
ALTER TABLE `pemeriksaan`
  MODIFY `id_pemeriksaan` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT for table `pendaftaran_poli`
--
ALTER TABLE `pendaftaran_poli`
  MODIFY `id_pendaftaran` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=102;

--
-- AUTO_INCREMENT for table `pengambilan_obat`
--
ALTER TABLE `pengambilan_obat`
  MODIFY `id_pengambilan` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT for table `poliklinik`
--
ALTER TABLE `poliklinik`
  MODIFY `id_poli` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `resep`
--
ALTER TABLE `resep`
  MODIFY `id_resep` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `admins`
--
ALTER TABLE `admins`
  ADD CONSTRAINT `fk_admin_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `detail_resep`
--
ALTER TABLE `detail_resep`
  ADD CONSTRAINT `fk_detail_obat` FOREIGN KEY (`id_obat`) REFERENCES `obat` (`id_obat`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_detail_resep` FOREIGN KEY (`id_resep`) REFERENCES `resep` (`id_resep`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `dokter`
--
ALTER TABLE `dokter`
  ADD CONSTRAINT `fk_dokter_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_dokter_poli` FOREIGN KEY (`id_poli`) REFERENCES `poliklinik` (`id_poli`) ON DELETE NO ACTION ON UPDATE CASCADE;

--
-- Constraints for table `farmasi_staff`
--
ALTER TABLE `farmasi_staff`
  ADD CONSTRAINT `fk_pharmacy_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `jadwal_dokter`
--
ALTER TABLE `jadwal_dokter`
  ADD CONSTRAINT `fk_jadwal_dokter` FOREIGN KEY (`id_dokter`) REFERENCES `dokter` (`id_dokter`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  ADD CONSTRAINT `fk_log_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `mutasi_stok`
--
ALTER TABLE `mutasi_stok`
  ADD CONSTRAINT `fk_mutasi_account` FOREIGN KEY (`account_user`) REFERENCES `accounts` (`account_id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_mutasi_obat` FOREIGN KEY (`id_obat`) REFERENCES `obat` (`id_obat`) ON UPDATE CASCADE;

--
-- Constraints for table `notifikasi`
--
ALTER TABLE `notifikasi`
  ADD CONSTRAINT `fk_notifikasi_pasien` FOREIGN KEY (`id_pasien`) REFERENCES `patients` (`id_pasien`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_notifikasi_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `otp_verifikasi`
--
ALTER TABLE `otp_verifikasi`
  ADD CONSTRAINT `fk_otp_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `patients`
--
ALTER TABLE `patients`
  ADD CONSTRAINT `fk_patient_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `pemeriksaan`
--
ALTER TABLE `pemeriksaan`
  ADD CONSTRAINT `fk_pemeriksaan_dokter` FOREIGN KEY (`id_dokter`) REFERENCES `dokter` (`id_dokter`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pemeriksaan_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON UPDATE CASCADE;

--
-- Constraints for table `pendaftaran_poli`
--
ALTER TABLE `pendaftaran_poli`
  ADD CONSTRAINT `fk_pendaftaran_jadwal` FOREIGN KEY (`id_jadwal`) REFERENCES `jadwal_dokter` (`id_jadwal`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pendaftaran_pasien` FOREIGN KEY (`id_pasien`) REFERENCES `patients` (`id_pasien`) ON UPDATE CASCADE;

--
-- Constraints for table `pengambilan_obat`
--
ALTER TABLE `pengambilan_obat`
  ADD CONSTRAINT `fk_pengambilan_petugas_account` FOREIGN KEY (`account_petugas`) REFERENCES `accounts` (`account_id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pengambilan_resep` FOREIGN KEY (`id_resep`) REFERENCES `resep` (`id_resep`) ON UPDATE CASCADE;

--
-- Constraints for table `resep`
--
ALTER TABLE `resep`
  ADD CONSTRAINT `fk_resep_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `pendaftaran_poli` (`id_pendaftaran`) ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
