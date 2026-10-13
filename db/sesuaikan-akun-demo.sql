-- Penyesuaian agar dump medicflow_db bisa dipakai aplikasi.
-- Dump memakai hash bcrypt palsu, dan baris pasien Budi menunjuk akun admin.
-- Kata sandi di bawah adalah SHA-256, sama dengan yang diperiksa layar masuk.

UPDATE users
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE username = 'admin';

UPDATE users
SET password_hash = 'b3959dee9b178b030c2b8373da55a04ab3adb318edeb178953ff8b77301a360a'
WHERE username = 'dokter01';

UPDATE users
SET password_hash = '4561c9fdf633bb9b5a6d72ec03f9568d683f3fa2cb3a8e8cd1c10c967f902553'
WHERE username = 'farmasi01';

INSERT INTO users (username, password_hash, role, nama_lengkap, no_telepon, is_active, created_at, updated_at)
SELECT 'budi',
       'b3cb1bf1350e826eafc2250c570837e1ef1a0e8d6fece2c3478740670ce8fed1',
       'pasien',
       'Budi Santoso Edit',
       '089876543210',
       1,
       '2026-10-07 03:46:17',
       '2026-10-07 03:46:17'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'budi');

UPDATE pasien p
INNER JOIN users u ON u.username = 'budi'
SET p.id_user = u.id_user
WHERE p.nik = '3471001234567890';
