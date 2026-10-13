# MediFlow

MediFlow adalah progressive web app (PWA) untuk antrean poliklinik, rekam medis, dan e-resep rumah sakit. Pasien, admin pendaftaran, dokter, dan farmasi memakai satu aplikasi. Layar, aturan kunjungan, dan API ditulis dalam Java dan dilayani oleh satu proses di komputer ini.

## Peran

| Peran | Akun demo | Kata sandi | Tugas |
| --- | --- | --- | --- |
| Pasien | `budi` | `pasien123` | Daftar poli, check-in, pilih apotek, tunjukkan kode pengambilan |
| Admin | `admin` | `admin123` | Tanda vital, panggil pasien, teruskan resep ke farmasi |
| Dokter | `dokter01` | `dokter123` | Pemeriksaan, diagnosis, kirim resep |
| Farmasi | `farmasi01` | `apotek123` | Siapkan obat, cek ulang, serah terima, sesuaikan stok |

Setiap jendela peramban punya sesi sendiri. Token sesi disimpan di `sessionStorage` dengan kunci `mediflow-sesi` dan ikut di alamat sebagai `?sesi=`. Beberapa jendela bisa masuk sebagai peran yang berbeda pada saat yang sama.

## Alur kunjungan

Tidak ada kolom tahap di database. Sepuluh langkah dihitung dari status antrean, status resep, dan pilihan penebusan. Tab Alur mengelompokkannya menjadi empat bagian.

| Bagian | Langkah | Pemilik |
| --- | --- | --- |
| Daftar | 1. Daftar dan check-in | Admin, setelah pasien check-in |
| Daftar | 2. Cek tanda vital | Admin / perawat |
| Daftar | 3. Ruang tunggu poli | Admin poli |
| Periksa | 4. Pemeriksaan dokter | Dokter yang bertugas |
| Periksa | 5. Dokter mengirim resep | Dokter |
| Resep | 6. Pilih apotek | Pasien memilih; admin meneruskan antrean |
| Resep | 7. Obat disiapkan | Farmasi |
| Resep | 8. Pengecekan ulang obat | Farmasi |
| Ambil | 9. Ambil obat di farmasi | Farmasi, lewat kode dan PIN |
| Ambil | 10. Kunjungan selesai | Tidak ada tindakan |

Pasien tidak memajukan langkah. Pada demo, admin tetap bisa memajukan setiap tahap; spanduk menandai tombol itu sebagai simulasi. Langkah 9 memakai kode serah terima, bukan tombol maju. Kode berlaku 24 jam dan hanya sekali. Keluarga yang mengambil obat memakai PIN di aplikasi. Tidak ada pemindai kamera: petugas mengetik kode yang tampil di aplikasi pasien.

Memilih apotek luar pada langkah 6 menutup kunjungan tanpa mengurangi stok rumah sakit. Antrean farmasi baru menampilkan resep setelah dokter mengirimnya pada langkah 5.

## Menjalankan di komputer ini

Perlu JDK 17 atau lebih baru, dan MySQL atau MariaDB. Pengembangan di sini memakai XAMPP untuk MySQL. `java` harus ada di PATH, atau tunjuk folder JDK lewat `JAVA_HOME`. Node.js tidak dipakai.

```powershell
# 1. Buat database medicflow_db, lalu impor skema dan penyesuaian demo.
#    phpMyAdmin, atau:
#    mysql -u root < db/schema.sql
#    mysql -u root medicflow_db < db/sesuaikan-pwa.sql

# 2. Isi data uji: antrean hari ini, resep, stok, dan jadwal semua poli.
#    Perintah ini mengganti isi 15 tabel medicflow_db.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\jalankan.ps1 dummy

# 3. Jalankan aplikasi. Peramban terbuka ke halaman masuk.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\jalankan.ps1
```

- Halaman dan API: http://127.0.0.1:8088/
- Kesehatan API: http://127.0.0.1:8088/api
- Pemeriksaan alur tanpa menyalakan server: `scripts\jalankan.ps1 periksa`

`npm run dev`, `npm run check`, dan `npm run dummy` memanggil skrip yang sama. Server hanya menjawab dari komputer ini (`127.0.0.1`).

Koneksi MySQL ada di `api/src/mediflow/Db.java`. Bawaan: host `127.0.0.1`, user `root`, kata sandi kosong, database `medicflow_db`, port `3306`. Nilai itu bisa ditimpa dengan `MEDIFLOW_DB_HOST`, `MEDIFLOW_DB_USER`, `MEDIFLOW_DB_PASSWORD`, `MEDIFLOW_DB_NAME`, dan `MEDIFLOW_DB_PORT`.

`db/schema.sql` adalah skema beserta data master. `db/sesuaikan-pwa.sql` dijalankan sesudahnya supaya login demo cocok dengan aplikasi: kata sandi demo memakai SHA-256, dan pasien Budi Santoso Edit mendapat akun `budi` sendiri. `scripts\jalankan.ps1 dummy` mengisi data uji dari `api/src/mediflow/DataUji.java` dan mengganti isi tabel.

Jika MySQL tidak terjangkau, proses Java memakai data uji di memori dan tidak menuliskannya kembali. Jika proses Java berhenti, aplikasi tidak bisa dibuka: layar tidak lagi disimpan di peramban.

Layar masuk tetap empat akun di atas. Pasien lain (Siti, Andi, Rina, Joko, Maya, Dewi, Agus, Hendra) dan dokter Poli Gigi, Anak, serta Mata ada di data uji supaya antrean dan jadwal terlihat. Antrean hari ini di Poli Umum Dr. Ahmad:

| Antrean | Pasien | Posisi | Yang bisa dicoba |
| --- | --- | --- | --- |
| 1 | Budi Santoso Edit | Pemeriksaan dokter | `dokter01` menulis diagnosis dan mengirim resep |
| 2 | Hendra Saputra | Belum check-in | Admin tidak bisa lanjut sebelum pasien check-in |
| 3 | Andi Wijaya | Sudah check-in | `admin` mengisi tanda vital |
| 4 | Rina Kusuma | Ruang tunggu, alergi penisilin | `admin` memanggil masuk ruang |
| 5 | Joko Susilo | Diagnosis sudah ada, resep belum kirim | `dokter01` mengirim resep |
| 6 | Maya Anggraini | Sudah pilih Apotek RS | `admin` meneruskan ke farmasi |
| 7 | Dewi Lestari | Antrean farmasi | `farmasi01` menyiapkan obat |
| 8 | Agus Pratama | Sedang diracik | `farmasi01` cek ulang |
| 9 | Siti Rahayu | Siap diambil | Kode `MF-SITI-SIAP`, PIN keluarga `482913` |

Budi juga punya dua kunjungan lama: satu selesai di apotek rumah sakit, satu ditebus di apotek luar. Cetirizine di bawah stok minimum. Vitamin C disembunyikan dan tetap menempel pada resep lama. Dr. Ahmad praktik setiap hari pukul 08.00–12.00. Poli lain punya jadwal di hari kerja, jadi Budi bisa mendaftar kunjungan kedua dari tombol Kunjungan lain.

Tombol Kembalikan data demo di panel admin memuat ulang data uji untuk hari ini. Tombol itu juga menulis ke MySQL bila koneksi hidup.

## Perintah

| Perintah | Fungsi |
| --- | --- |
| `scripts\jalankan.ps1` | Menyusun sumber Java, lalu menjalankan aplikasi di port 8088 |
| `scripts\jalankan.ps1 periksa` | Pemeriksaan alur di memori: jadwal ganda, stok habis, kode yang dipakai ulang, dan catatan stok |
| `scripts\jalankan.ps1 dummy` | Mengisi `medicflow_db` dengan data uji hari ini. Isi tabel yang ada diganti |

Di Visual Studio Code, **Run PWA** membuka Edge ke http://127.0.0.1:8088 dan menyalakan server yang sama.

## Struktur

```
MediFlow/
  public/
    mediflow.css           gaya layar
    app.js                 token sesi, izin notifikasi, dan tutup toast
    sw.js                  cache berkas statis dan notifikasi
    manifest.json          identitas pemasangan PWA
    assets/icons/          ikon 24, 48, 192, dan 512
  scripts/
    jalankan.ps1           menyusun dan menjalankan aplikasi Java
  db/
    schema.sql             skema medicflow_db
    sesuaikan-pwa.sql      akun demo setelah impor dump
  api/
    lib/                   jar Jackson dan MySQL, tanpa Maven
    src/mediflow/          domain, API, dan layar HTML
```

Kode Java ada di `api/src/mediflow/`:

| Berkas | Isi |
| --- | --- |
| `ApiServer.java` | HTTP di 127.0.0.1:8088, API dan halaman |
| `Situs.java` | Sesi per jendela dan berkas statis |
| `Layar.java` | HTML masuk dan beranda |
| `Toko.java` | Masuk, kunjungan, resep, dan stok |
| `Aturan.java` | Tahap, bentrok jadwal, stok, dan teks notifikasi |
| `Tampilan.java` | Sepuluh langkah, empat bagian, tanggal, dan rupiah |
| `DataUji.java` | Data cadangan bila MySQL tidak menjawab |
| `Basis.java` | Bentuk data, sama dengan tabel MySQL |
| `Sandi.java` | Hash kata sandi demo dan penyamaran NIK |
| `UjiAlur.java` | Pemeriksaan alur |
| `IsiDummy.java` | Menulis data uji ke MySQL |

API tetap tersedia di proses yang sama:

| Metode | Jalur |
| --- | --- |
| GET | `/api` kesehatan `{ "ok", "service": "mediflow", "language": "java" }` |
| GET, PUT, POST | `/api/database` |
| GET, POST | `/api/users`, `/api/pasien`, `/api/poliklinik`, `/api/dokter`, `/api/jadwal_dokter`, `/api/obat`, `/api/pendaftaran_poli`, `/api/pemeriksaan`, `/api/resep`, `/api/detail_resep`, `/api/notifikasi`, `/api/otp_verifikasi`, `/api/pengambilan_obat`, `/api/mutasi_stok`, `/api/log_aktivitas` |
| GET, PUT, PATCH, DELETE | jalur yang sama ditambah `/{id}` |

`GET /` dengan header `Accept: application/json` juga mengembalikan objek kesehatan itu. Permintaan peramban biasa mendapat halaman HTML.

Balasan CRUD berbentuk `{ "status", "message", "data" }`. Daftar bisa disaring lewat query, misalnya `GET /api/pendaftaran_poli?id_pasien=2&tanggal_kunjungan=2026-10-08`. Semua jalur hanya menjawab dari komputer ini. Jadwal yang bertumpuk, kuota di bawah antrean hari ini, atau jadwal yang disembunyikan padahal masih ada antrean hari ini ditolak. Pasien tidak bisa mendaftar dua kali pada jadwal dan tanggal yang sama. Pesan notifikasi tidak boleh memuat kata WhatsApp, nama obat, atau diagnosis.

Perubahan dari layar menulis seluruh database lewat snapshot. Dua jendela yang menyimpan bersamaan saling menimpa: simpanan terakhir mengganti perubahan yang lain.

Tabel: `users`, `pasien`, `poliklinik`, `dokter`, `jadwal_dokter`, `obat`, `pendaftaran_poli`, `pemeriksaan`, `resep`, `detail_resep`, `notifikasi`, `otp_verifikasi`, `pengambilan_obat`, `mutasi_stok`, `log_aktivitas`.
