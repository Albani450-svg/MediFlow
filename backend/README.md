# API MediFlow

API ini aplikasi CodeIgniter 4 untuk database `medicflow_db`. Cara memasang PHP, MySQL, dan menjalankan server ada di [README proyek](../README.md).

`npm run dev` dan `npm run api` menjalankan `public/index.php` lewat server bawaan PHP di `127.0.0.1:8088`.

| Metode | Jalur | Fungsi |
| --- | --- | --- |
| GET | `/api/database` | Seluruh tabel dalam bentuk yang dipakai PWA |
| PUT, POST | `/api/database` | Ganti seluruh isi tabel |
| GET | `/api/pasien` | Daftar pasien |
| GET | `/api/pasien/{id}` | Satu pasien |
| POST | `/api/pasien` | Tambah pasien |
| PUT | `/api/pasien/{id}` | Ubah pasien |
| DELETE | `/api/pasien/{id}` | Hapus pasien |

Koneksi ada di `app/Config/Database.php`. Bawaan: `127.0.0.1`, user `root`, kata sandi kosong, database `medicflow_db`.

Folder `vendor/` dipasang dengan `composer install` di folder ini. Jangan mengubah isi `vendor/`.
