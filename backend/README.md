# API CodeIgniter (tidak dijalankan)

API yang dipakai aplikasi sekarang adalah layanan Java di [`api/`](../api). `npm run dev` dan `npm run api` menyusun lalu menjalankan `mediflow.ApiServer` di `127.0.0.1:8088`.

Folder ini adalah API CodeIgniter 4 yang digantikan. Jalur, bentuk JSON, dan database `medicflow_db` tetap sama. Cara menjalankan aplikasi ada di [README proyek](../README.md).

Koneksi bawaan Java: host `127.0.0.1`, user `root`, kata sandi kosong, database `medicflow_db`, port `3306`. Timpa dengan `MEDIFLOW_DB_HOST`, `MEDIFLOW_DB_USER`, `MEDIFLOW_DB_PASSWORD`, `MEDIFLOW_DB_NAME`, dan `MEDIFLOW_DB_PORT`.
