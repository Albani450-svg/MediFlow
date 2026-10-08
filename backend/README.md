# API MediFlow

API ini aplikasi CodeIgniter 4 untuk database `medicflow_db`. Cara memasang PHP, MySQL, dan menjalankan server ada di [README proyek](../README.md).

`npm run dev` dan `npm run api` menjalankan `public/index.php` lewat server bawaan PHP di `127.0.0.1:8088`.

PWA membaca dan menulis lewat snapshot. CRUD di bawah dipakai bila satu tabel yang diubah.

| Metode | Jalur | Fungsi |
| --- | --- | --- |
| GET | `/api/database` | Seluruh tabel dalam bentuk yang dipakai PWA |
| PUT, POST | `/api/database` | Ganti seluruh isi tabel |
| GET | `/api/{tabel}` | Daftar baris. Query mengikuti kolom relasi, misalnya `?id_pasien=2` |
| GET | `/api/{tabel}/{id}` | Satu baris |
| POST | `/api/{tabel}` | Tambah baris. Balasan `201` dan id baru |
| PUT, PATCH | `/api/{tabel}/{id}` | Ubah kolom yang dikirim |
| DELETE | `/api/{tabel}/{id}` | Hapus baris. Gagal `409` bila masih dipakai tabel lain |

`{tabel}` adalah `users`, `pasien`, `poliklinik`, `dokter`, `jadwal_dokter`, `obat`, `pendaftaran_poli`, `pemeriksaan`, `resep`, `detail_resep`, `notifikasi`, `otp_verifikasi`, `pengambilan_obat`, `mutasi_stok`, atau `log_aktivitas`.

Balasan CRUD: `{ "status": true, "message": "...", "data": ... }`. Galat memakai `"status": false`. Snapshot tetap memakai `{ "ok": true, "data": ... }`.

Setiap jalur `/api` hanya melayani `127.0.0.1`. Jadwal dokter menolak jam yang bertumpuk, kuota di bawah antrean hari ini, dan penyembunyian jadwal yang masih punya antrean. Pendaftaran menolak nomor antrean yang bentrok dan pasien yang sudah terdaftar pada jadwal yang sama. Notifikasi menolak pesan yang memuat WhatsApp, nama obat, atau diagnosis.

Koneksi ada di `app/Config/Database.php`. Bawaan: `127.0.0.1`, user `root`, kata sandi kosong, database `medicflow_db`, port `3306`. Timpa dengan variabel lingkungan `MEDIFLOW_DB_HOST`, `MEDIFLOW_DB_USER`, `MEDIFLOW_DB_PASSWORD`, `MEDIFLOW_DB_NAME`, dan `MEDIFLOW_DB_PORT`.

Folder `vendor/` dipasang dengan `composer install` di folder ini. Jangan mengubah isi `vendor/`.
