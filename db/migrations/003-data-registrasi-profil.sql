ALTER TABLE `pasien`
  ADD COLUMN `no_satusehat` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `no_telepon`,
  ADD COLUMN `privacy_consent_at` datetime DEFAULT NULL AFTER `no_satusehat`,
  ADD COLUMN `privacy_policy_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `privacy_consent_at`;

ALTER TABLE `staff_registration_requests`
  ADD COLUMN `id_poli` bigint UNSIGNED DEFAULT NULL AFTER `specialization`,
  ADD KEY `idx_staff_request_poli` (`id_poli`),
  ADD CONSTRAINT `fk_staff_request_poli` FOREIGN KEY (`id_poli`)
    REFERENCES `poliklinik` (`id_poli`) ON DELETE RESTRICT ON UPDATE CASCADE;
