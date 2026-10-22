# MediFlow

MediFlow adalah progressive web app (PWA) untuk antrean poliklinik, rekam medis, dan e-resep rumah sakit. Pasien, admin pendaftaran, dokter, dan farmasi memakai satu aplikasi. Layar, aturan kunjungan, dan API ditulis dalam Java dan dilayani oleh satu proses di komputer ini.

## Peran

| Peran | Akun demo | Kata sandi | Tugas |
| --- | --- | --- | --- |
| Pasien | `dummy_pasien01` | `pasien123` | Siti Rahma Dummy. Kunjungan 26 Oktober 2026 selesai, obat sudah diambil |
| Pasien | `dummy_pasien02` | `pasien123` | Andi Pratama Dummy. Antrean 26 Oktober 2026 masih menunggu |
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
# 1. Buat database kosong, impor dump, lalu pasang kata sandi demo.
#    phpMyAdmin, atau:
#    mysql -u root -e "DROP DATABASE IF EXISTS medicflow_db; CREATE DATABASE medicflow_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
#    mysql -u root medicflow_db < db/schema.sql
#    mysql -u root medicflow_db < db/sesuaikan-akun-demo.sql
#    Migrasi 001-003 hanya untuk skema lama (users/pasien). Jangan dijalankan pada dump ini.

# 2. Isi data uji: antrean hari ini, resep, stok, dan jadwal semua poli.
#    Perintah ini mengganti isi 17 tabel medicflow_db.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-application.ps1 dummy

# 3. Jalankan aplikasi. Peramban terbuka ke halaman masuk.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-application.ps1
```

- Halaman dan API: http://127.0.0.1:8088/
- Kesehatan API: http://127.0.0.1:8088/api
- Pemeriksaan alur tanpa menyalakan server: `scripts\run-application.ps1 periksa`

`npm run dev`, `npm run check`, dan `npm run dummy` memanggil skrip yang sama. Server hanya menjawab dari komputer ini (`127.0.0.1`).

Koneksi MySQL ada di `api/src/mediflow/Database.java`. Bawaan: host `127.0.0.1`, user `root`, kata sandi kosong, database `medicflow_db`, port `3306`. Nilai itu bisa ditimpa dengan `MEDIFLOW_DB_HOST`, `MEDIFLOW_DB_USER`, `MEDIFLOW_DB_PASSWORD`, `MEDIFLOW_DB_NAME`, dan `MEDIFLOW_DB_PORT`.

## Registrasi akun

Di layar masuk, pilih peran Pasien untuk melihat tombol daftar atau masuk yang sebelumnya disembunyikan. Pendaftaran menerima peran Pasien, Dokter, atau Apoteker (disimpan sebagai role `farmasi`). Semua pendaftar memverifikasi nomor WhatsApp dengan OTP dan menetapkan kata sandi minimal 10 karakter. Kata sandi disimpan dengan PBKDF2-HMAC-SHA256; login demo lama tetap didukung.

Pasien melengkapi NIK, tanggal lahir, jenis kelamin, dan alamat, lalu menyetujui kebijakan privasi. Nomor BPJS dan SATUSEHAT masih ditanyakan di formulir, tetapi dump saat ini tidak punya kolom untuk keduanya. Nomor telepon diverifikasi lewat OTP. Pasien aktif setelah data lengkap dan mendapat nomor rekam medis. Dokter mengisi nama, SIP, spesialisasi, dan poli; apoteker mengisi nama dan SIPA. Akun staf tetap nonaktif sampai admin menyetujuinya. Nomor izin disimpan di log aktivitas karena tidak ada tabel permohonan. Permohonan yang ditolak tidak bisa dipakai untuk masuk.

Peran tidak disimpan di `accounts`. Admin, dokter, farmasi, dan pasien masing-masing punya baris di `admins`, `dokter`, `farmasi_staff`, atau `patients`. Sebelum menjalankan aplikasi, atur variabel lingkungan berikut:

- `MEDIFLOW_OTP_SECRET`: rahasia acak minimal 32 karakter untuk menyimpan hash OTP.
- `MEDIFLOW_DELIVERY_URL`: URL HTTPS endpoint provider WhatsApp yang menerima JSON `{"to":"+62...","message":"..."}`.
- `MEDIFLOW_DELIVERY_TOKEN`: token Bearer untuk provider tersebut.

Registrasi mengembalikan error jika database atau konfigurasi/provider pengiriman tidak tersedia; aplikasi tidak menganggap OTP terkirim. OTP berlaku 10 menit, percobaan verifikasi dibatasi, dan jumlah pendaftaran dibatasi per alamat IP serta nomor WhatsApp. Provider perlu disiapkan agar sesuai kontrak REST di atas.

`db/schema.sql` memuat data dump 22 Oktober 2026 pukul 09:35 (berkas `dump-medicflow_db-202610220935.sql`) dan lembar login dummy, sudah dipetakan ke skema `accounts` / `patients`. `db/sesuaikan-akun-demo.sql` dijalankan sesudahnya supaya `admin`, `dokter01`, `farmasi01`, `dummy_pasien01`, dan `dummy_pasien02` bisa masuk: kata sandi demo memakai SHA-256. Lembar Excel menandai kata sandi dua pasien dummy sebagai belum disetel; skrip itu mengisi keduanya dengan `pasien123`. Akun yang mendaftar sendiri tidak diubah.

Dump sumber memakai tabel `users` dan `pasien`. Dua baris pasien lama tetap menempel pada akun staf karena demikian adanya di dump: Budi Santoso Edit pada akun `admin`, dan Pasien Uji MedicFlow pada akun `dokter01`. Keduanya tidak punya username pasien sendiri. Nomor BPJS Budi tidak disimpan karena tabel `patients` tidak punya kolom itu. `scripts\run-application.ps1 dummy` mengisi data uji lain dari `api/src/mediflow/DemoData.java` (akun `budi` dan antrean panjang) dan mengganti isi tabel.

Jika MySQL tidak terjangkau, proses Java memakai data uji di memori dan tidak menuliskannya kembali. Jika proses Java berhenti, aplikasi tidak bisa dibuka: layar tidak lagi disimpan di peramban.

Perintah `dummy` mengganti database dengan data uji di memori. Akun layar masuk pada data uji itu tetap `budi`, `dokter01`, `admin`, dan `farmasi01`. Pasien lain (Siti, Andi, Rina, Joko, Maya, Dewi, Agus, Hendra) dan dokter Poli Gigi, Anak, serta Mata ada di data uji supaya antrean dan jadwal terlihat. Antrean hari ini di Poli Umum Dr. Ahmad:

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
| `scripts\run-application.ps1` | Menyusun sumber Java, lalu menjalankan aplikasi di port 8088 |
| `scripts\run-application.ps1 periksa` | Pemeriksaan alur di memori: jadwal ganda, stok habis, kode yang dipakai ulang, dan catatan stok |
| `scripts\run-application.ps1 dummy` | Mengisi `medicflow_db` dengan data uji hari ini. Isi tabel yang ada diganti |

Di Visual Studio Code, **Run application** membuka Edge ke http://127.0.0.1:8088 dan menyalakan server yang sama.

## Struktur

```
MediFlow/
  public/
    mediflow.css           gaya layar
    application.js         token sesi, izin notifikasi, dan tutup toast
    service-worker.js      cache berkas statis dan notifikasi
    manifest.json          identitas pemasangan aplikasi
    assets/icons/          ikon 24, 48, 192, dan 512
  scripts/
    run-application.ps1    menyusun dan menjalankan aplikasi Java
  db/
    schema.sql             skema medicflow_db
    migrations/            perubahan skema untuk database yang sudah ada
    sesuaikan-akun-demo.sql  akun demo setelah impor dump
  api/
    lib/                   jar Jackson dan MySQL, tanpa Maven
    src/mediflow/          domain, API, dan layar HTML
```

Kode Java ada di `api/src/mediflow/`:

| Berkas | Isi |
| --- | --- |
| `ApplicationServer.java` | HTTP di 127.0.0.1:8088, halaman dan jalur data |
| `PatientRegistration.java` | Registrasi pasien, OTP, rate limit, dan pengiriman OTP via REST |
| `WebSite.java` | Sesi per jendela dan berkas statis |
| `HtmlPages.java` | HTML masuk dan beranda |
| `VisitStore.java` | Masuk, kunjungan, resep, dan stok |
| `VisitRules.java` | Tahap, bentrok jadwal, stok, dan teks notifikasi |
| `ScreenText.java` | Sepuluh langkah, empat bagian, tanggal, dan rupiah |
| `DemoData.java` | Data cadangan bila MySQL tidak menjawab |
| `DatabaseRecords.java` | Bentuk data, sama dengan tabel MySQL |
| `PasswordHash.java` | Hash kata sandi demo dan penyamaran NIK |
| `VisitFlowCheck.java` | Pemeriksaan alur |
| `LoadDemoData.java` | Menulis data uji ke MySQL |
| `Database.java` | Koneksi MySQL |
| `DatabaseSnapshot.java` | Baca dan tulis seluruh database |
| `TableCatalog.java` | Bentuk tabel dan validasi |
| `TableEndpoint.java` | Buat, baca, ubah, dan hapus satu tabel |
| `ColumnValues.java` | Konversi nilai kolom |
| `JsonConverter.java` | Baca dan tulis teks JSON |

API tetap tersedia di proses yang sama:

| Metode | Jalur |
| --- | --- |
| GET | `/api` kesehatan `{ "ok", "service": "mediflow", "language": "java" }` |
| GET | `/api/database` (snapshot hanya-baca) |
| GET | `/api/accounts`, `/api/patients`, `/api/admins`, `/api/farmasi_staff`, dan `/{id}` (baris ini tidak dapat diubah melalui CRUD umum) |
| GET, POST | `/api/poliklinik`, `/api/dokter`, `/api/jadwal_dokter`, `/api/obat`, `/api/pendaftaran_poli`, `/api/pemeriksaan`, `/api/resep`, `/api/detail_resep`, `/api/notifikasi`, `/api/otp_verifikasi`, `/api/pengambilan_obat`, `/api/mutasi_stok`, `/api/log_aktivitas` |
| GET, PUT, PATCH, DELETE | jalur tersebut ditambah `/{id}` |
| POST | `/api/auth/register` — nama, email, `no_telepon`, password, dan role `pasien`, `dokter`, atau `farmasi` |
| POST | `/api/auth/register/verify-otp` — email dan `kode_otp` |
| POST | `/api/auth/register/profile` — pasien: email, NIK, nomor BPJS, `tanggal_lahir`, `jenis_kelamin`, alamat, `nomor_satusehat`, dan `persetujuan_privasi`; staf: email, `license_number`, `specialization`, dan `id_poli` untuk dokter |

`GET /` dengan header `Accept: application/json` juga mengembalikan objek kesehatan itu. Permintaan peramban biasa mendapat halaman HTML.

Balasan CRUD berbentuk `{ "status", "message", "data" }`. Daftar bisa disaring lewat query, misalnya `GET /api/pendaftaran_poli?id_pasien=2&tanggal_kunjungan=2026-10-08`. Semua jalur hanya menjawab dari komputer ini. Jadwal yang bertumpuk, kuota di bawah antrean hari ini, atau jadwal yang disembunyikan padahal masih ada antrean hari ini ditolak. Pasien tidak bisa mendaftar dua kali pada jadwal dan tanggal yang sama. Pesan notifikasi tidak boleh memuat kata WhatsApp, nama obat, atau diagnosis.

Perubahan dari layar menulis seluruh database lewat snapshot. Dua jendela yang menyimpan bersamaan saling menimpa: simpanan terakhir mengganti perubahan yang lain.

Tabel: `accounts`, `admins`, `farmasi_staff`, `patients`, `poliklinik`, `dokter`, `jadwal_dokter`, `obat`, `pendaftaran_poli`, `pemeriksaan`, `resep`, `detail_resep`, `notifikasi`, `otp_verifikasi`, `auth_rate_limits`, `pengambilan_obat`, `mutasi_stok`, `log_aktivitas`.