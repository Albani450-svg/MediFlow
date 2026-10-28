-- Migrasi 002. Hanya untuk skema lama sebelum dump 22 Oktober 2026. Jangan dijalankan pada db/schema.sql sekarang.
-- Jalankan sesudah 001. Butuh tabel users. Dump baru tidak punya staff_registration_requests.
-- Satu pengguna hanya punya satu pengajuan. Nomor izin tidak boleh dipakai pengajuan lain.
-- Aman dijalankan ulang pada MariaDB 10.4 (XAMPP proyek ini).
-- Aplikasi Java belum membaca tabel ini, jadi penyimpanan layar tidak menghapus barisnya.
--
-- Jangan dijalankan pada database mediflow yang sudah ada di DBeaver.
-- mysql -u root mediflow < db/migrations/002-registrasi-staf.sql

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `staff_registration_requests` (
  `id_request` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `id_user` bigint UNSIGNED NOT NULL,
  `license_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `specialization` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `reviewed_by` bigint UNSIGNED DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_request`),
  UNIQUE KEY `uq_staff_request_user` (`id_user`),
  UNIQUE KEY `uq_staff_request_license` (`license_number`),
  KEY `idx_staff_request_status` (`status`),
  KEY `idx_staff_request_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_staff_request_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_staff_request_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id_user`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabel yang dibuat versi lama belum punya updated_at dan kunci nomor izin.
ALTER TABLE `staff_registration_requests`
  ADD COLUMN IF NOT EXISTS `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

ALTER TABLE `staff_registration_requests`
  MODIFY COLUMN `license_number` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  MODIFY COLUMN `specialization` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `status` enum('PENDING','APPROVED','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  MODIFY COLUMN `reviewed_by` bigint UNSIGNED DEFAULT NULL,
  MODIFY COLUMN `reviewed_at` datetime DEFAULT NULL,
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`;

ALTER TABLE `staff_registration_requests`
  ADD UNIQUE KEY IF NOT EXISTS `uq_staff_request_user` (`id_user`),
  ADD UNIQUE KEY IF NOT EXISTS `uq_staff_request_license` (`license_number`),
  ADD KEY IF NOT EXISTS `idx_staff_request_status` (`status`);

-- Kunci asing peninjau sudah membuat indeks. Jangan menambah indeks kedua.
SET @sql := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `staff_registration_requests` ADD KEY `idx_staff_request_reviewer` (`reviewed_by`)',
    'DO 0')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'staff_registration_requests'
    AND COLUMN_NAME = 'reviewed_by'
);
PREPARE mf_mig FROM @sql;
EXECUTE mf_mig;
DEALLOCATE PREPARE mf_mig;

ALTER TABLE `staff_registration_requests`
  ADD FOREIGN KEY IF NOT EXISTS `fk_staff_request_user` (`id_user`) REFERENCES `users` (`id_user`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `staff_registration_requests`
  ADD FOREIGN KEY IF NOT EXISTS `fk_staff_request_reviewer` (`reviewed_by`) REFERENCES `users` (`id_user`) ON DELETE SET NULL ON UPDATE CASCADE;
