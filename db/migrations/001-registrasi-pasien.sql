-- Migrasi 001. Hanya untuk skema lama (tabel users dan pasien) sebelum dump 22 Oktober 2026.
-- Jangan dijalankan pada db/schema.sql yang sekarang. Skema itu memakai accounts, patients, admins, dan farmasi_staff.
-- Menyamakan users, pasien, otp_verifikasi, dan auth_rate_limits dengan bentuk lama.
-- Akun lama menerima account_status ACTIVE. Email kosong dijadikan NULL sebelum indeks unik dipasang.
-- Aman dijalankan ulang pada MariaDB 10.4 (XAMPP proyek ini).
--
-- Jangan dijalankan pada database medicflow_db yang sudah ada di DBeaver.
-- mysql -u root mediflow < db/migrations/001-registrasi-pasien.sql

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SESSION sql_safe_updates = 0;

-- email tetap varchar(100), sama dengan schema.sql.
-- Aplikasi menolak email lebih dari 50 karakter karena nilai itu juga disimpan sebagai username.
ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `username`,
  ADD COLUMN IF NOT EXISTS `account_status` enum('PENDING','ACTIVE','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' AFTER `is_active`,
  ADD COLUMN IF NOT EXISTS `wa_terverifikasi_pada` datetime DEFAULT NULL AFTER `account_status`;

ALTER TABLE `users`
  MODIFY COLUMN `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `username`,
  MODIFY COLUMN `account_status` enum('PENDING','ACTIVE','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' AFTER `is_active`,
  MODIFY COLUMN `wa_terverifikasi_pada` datetime DEFAULT NULL AFTER `account_status`;

UPDATE `users`
SET `email` = NULL
WHERE `email` IS NOT NULL AND CHAR_LENGTH(TRIM(`email`)) = 0;

-- Indeks biasa bernama email menghalangi kunci unik. Indeks unik yang sudah benar dibiarkan.
SET @sql := (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `users` DROP INDEX `email`', 'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'email'
    AND NON_UNIQUE = 1
);
PREPARE mf_mig FROM @sql;
EXECUTE mf_mig;
DEALLOCATE PREPARE mf_mig;

ALTER TABLE `users`
  ADD UNIQUE KEY IF NOT EXISTS `email` (`email`);

ALTER TABLE `pasien`
  ADD COLUMN IF NOT EXISTS `no_rekam_medis` varchar(24) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `id_user`;

ALTER TABLE `pasien`
  MODIFY COLUMN `no_rekam_medis` varchar(24) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `id_user`;

UPDATE `pasien`
SET `no_rekam_medis` = NULL
WHERE `no_rekam_medis` IS NOT NULL AND CHAR_LENGTH(TRIM(`no_rekam_medis`)) = 0;

SET @sql := (
  SELECT IF(COUNT(*) > 0, 'ALTER TABLE `pasien` DROP INDEX `no_rekam_medis`', 'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pasien'
    AND INDEX_NAME = 'no_rekam_medis'
    AND NON_UNIQUE = 1
);
PREPARE mf_mig FROM @sql;
EXECUTE mf_mig;
DEALLOCATE PREPARE mf_mig;

ALTER TABLE `pasien`
  ADD UNIQUE KEY IF NOT EXISTS `no_rekam_medis` (`no_rekam_medis`);

-- Hash OTP pasien adalah HMAC-SHA256 sepanjang 64 karakter hex. PIN keluarga tetap muat.
ALTER TABLE `otp_verifikasi`
  MODIFY COLUMN `kode_otp` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL;

CREATE TABLE IF NOT EXISTS `auth_rate_limits` (
  `scope_key` char(64) COLLATE ascii_bin NOT NULL,
  `window_started_at` datetime NOT NULL,
  `attempt_count` int UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`scope_key`),
  KEY `idx_auth_rate_window` (`window_started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
