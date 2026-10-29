-- Kata sandi demo pada database medicflow_db (tabel users).
-- Dump menyimpan placeholder $2y$10$abcdefghijklmnopqrstuv, bukan hash yang bisa dipakai masuk.
-- Nilai di bawah adalah SHA-256. Akun lain, termasuk pasien yang mendaftar sendiri, tidak diubah.
-- Hanya baris yang masih berisi placeholder yang diganti.
-- Lembar login dummy menandai dummy_pasien01 dan dummy_pasien02 sebagai belum disetel.
-- Keduanya memakai kata sandi pasien yang sama dengan akun demo pasien: pasien123.

UPDATE users
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE username = 'admin' AND password_hash = '$2y$10$abcdefghijklmnopqrstuv';

UPDATE users
SET password_hash = 'b3959dee9b178b030c2b8373da55a04ab3adb318edeb178953ff8b77301a360a'
WHERE username = 'dokter01' AND password_hash = '$2y$10$abcdefghijklmnopqrstuv';

UPDATE users
SET password_hash = '4561c9fdf633bb9b5a6d72ec03f9568d683f3fa2cb3a8e8cd1c10c967f902553'
WHERE username = 'farmasi01' AND password_hash = '$2y$10$abcdefghijklmnopqrstuv';

UPDATE users
SET password_hash = 'b3cb1bf1350e826eafc2250c570837e1ef1a0e8d6fece2c3478740670ce8fed1'
WHERE username IN ('dummy_pasien01', 'dummy_pasien02')
  AND password_hash = '$2y$10$abcdefghijklmnopqrstuv';
