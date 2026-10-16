ALTER TABLE `users`
  ADD COLUMN `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `username`,
  ADD COLUMN `account_status` enum('PENDING','ACTIVE','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' AFTER `is_active`,
  ADD COLUMN `wa_terverifikasi_pada` datetime DEFAULT NULL AFTER `account_status`,
  ADD UNIQUE KEY `email` (`email`);

ALTER TABLE `pasien`
  ADD COLUMN `no_rekam_medis` varchar(24) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `id_user`,
  ADD UNIQUE KEY `no_rekam_medis` (`no_rekam_medis`);

ALTER TABLE `otp_verifikasi`
  MODIFY COLUMN `kode_otp` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL;

CREATE TABLE `auth_rate_limits` (
  `scope_key` char(64) COLLATE ascii_bin NOT NULL,
  `window_started_at` datetime NOT NULL,
  `attempt_count` int UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`scope_key`),
  KEY `idx_auth_rate_window` (`window_started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
