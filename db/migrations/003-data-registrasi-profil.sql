-- Migrasi 003. Data profil registrasi untuk medicflow_db yang sudah ada.
-- Jalankan sesudah 001 dan 002. Butuh tabel pasien, poliklinik, dan staff_registration_requests.
-- Aman dijalankan ulang pada MariaDB 10.4 (XAMPP proyek ini).
-- no_satusehat, privacy_consent_at, dan privacy_policy_version belum ditulis aplikasi.
-- Simpanan layar menyisipkan ulang baris pasien tanpa kolom itu, jadi nilainya kembali kosong
-- sampai katalog tabel memakainya.
--
-- mysql -u root medicflow_db < db/migrations/003-data-registrasi-profil.sql

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `pasien`
  ADD COLUMN IF NOT EXISTS `no_satusehat` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `no_telepon`,
  ADD COLUMN IF NOT EXISTS `privacy_consent_at` datetime DEFAULT NULL AFTER `no_satusehat`,
  ADD COLUMN IF NOT EXISTS `privacy_policy_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `privacy_consent_at`;

ALTER TABLE `pasien`
  MODIFY COLUMN `no_satusehat` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `no_telepon`,
  MODIFY COLUMN `privacy_consent_at` datetime DEFAULT NULL AFTER `no_satusehat`,
  MODIFY COLUMN `privacy_policy_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `privacy_consent_at`;

ALTER TABLE `staff_registration_requests`
  ADD COLUMN IF NOT EXISTS `id_poli` bigint UNSIGNED DEFAULT NULL AFTER `specialization`;

ALTER TABLE `staff_registration_requests`
  MODIFY COLUMN `id_poli` bigint UNSIGNED DEFAULT NULL AFTER `specialization`;

ALTER TABLE `staff_registration_requests`
  ADD KEY IF NOT EXISTS `idx_staff_request_poli` (`id_poli`);

ALTER TABLE `staff_registration_requests`
  ADD FOREIGN KEY IF NOT EXISTS `fk_staff_request_poli` (`id_poli`) REFERENCES `poliklinik` (`id_poli`) ON DELETE RESTRICT ON UPDATE CASCADE;
