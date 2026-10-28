# MediFlow

MediFlow adalah progressive web app (PWA) untuk antrean poliklinik, rekam medis, dan e-resep rumah sakit. Pasien, admin pendaftaran, dokter, dan farmasi memakai satu aplikasi. Layar, aturan kunjungan, dan API ditulis dalam Java dan dilayani oleh satu proses di komputer ini.

## Peran

| Peran | Akun demo | Kata sandi | Tugas |
| --- | --- | --- | --- |
| Pasien | `dummy_pasien01` | `pasien123` | Siti Rahma Dummy. Kunjungan 26 Oktober 2026 selesai dan obat sudah diambil. Kartu pengambilan menampilkan QR beserta teks `DUMMY-QR-MEDICFLOW-100` |
| Pasien | `dummy_pasien02` | `pasien123` | Andi Pratama Dummy. Antrean 26 Oktober 2026 di database masih menunggu, tetapi belum ada baris pemeriksaan, jadi layar antreannya kosong |
| Admin | `admin` | `admin123` | Tanda vital, panggil pasien, teruskan resep ke farmasi |
| Dokter | `dokter01` | `dokter123` | Pemeriksaan, diagnosis, kirim resep |
| Farmasi | `farmasi01` | `apotek123` | Siapkan obat, cek ulang, serah terima, sesuaikan stok |

Pasien melihat kunjungannya sendiri tanpa saringan tanggal, asalkan ada baris pemeriksaan. Dokter dan admin hanya melihat kunjungan hari ini, jadi antrean 26 Oktober 2026 tidak muncul di daftar mereka pada hari lain. Farmasi melihat resep rumah sakit yang sudah sampai langkah 7, termasuk yang tanggalnya sudah lewat.

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

Pasien tidak memajukan langkah. Pada demo, admin tetap bisa memajukan setiap tahap; spanduk menandai tombol itu sebagai simulasi. Langkah 9 memakai kode serah terima, bukan tombol maju. Aplikasi pasien menggambar kode itu sebagai QR, dengan teks kode di bawah gambar. Kode berlaku 24 jam dan hanya sekali. Keluarga yang mengambil obat memakai PIN yang hanya tampil di aplikasi. Tidak ada pemindai kamera: petugas mengetik kode yang terlihat di bawah QR.

Memilih apotek luar pada langkah 6 menutup kunjungan tanpa mengurangi stok rumah sakit. Antrean farmasi menampilkan resep rumah sakit mulai langkah 7. Resep apotek luar tidak masuk antrean itu.

## Menjalankan di komputer ini

Perlu JDK 17 atau lebih baru, dan MySQL atau MariaDB. Pengembangan di sini memakai XAMPP untuk MySQL. `java` harus ada di PATH, atau tunjuk folder JDK lewat `JAVA_HOME`. Node.js tidak dipakai.

```powershell
# Database yang dipakai sudah ada: mediflow di localhost:3306
# (koneksi DBeaver bernama MediFlow). Jangan buat database baru,
# dan jangan impor db/schema.sql atau migrasi 001-003 ke server ini.

# Jalankan aplikasi. Peramban terbuka ke halaman masuk.
# MEDIFLOW_TANPA_BROWSER=1 menahan peramban itu.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-application.ps1
```

- Halaman dan API: http://127.0.0.1:8088/
- Kesehatan API: http://127.0.0.1:8088/api
- Pemeriksaan alur tanpa menyalakan server: `scripts\run-application.ps1 periksa`
- Menghentikan server: `scripts\run-application.ps1 henti`

`npm run dev`, `npm run start`, `npm run api`, `npm run check`, dan `npm run dummy` memanggil skrip yang sama. Server hanya menjawab dari komputer ini (`127.0.0.1`).

Koneksi MySQL ada di `api/src/mediflow/Database.java`. Bawaan: host `127.0.0.1`, user `root`, kata sandi kosong, database `mediflow`, port `3306`. Itu database yang sama dengan koneksi DBeaver MediFlow. Nilai itu bisa ditimpa dengan `MEDIFLOW_DB_HOST`, `MEDIFLOW_DB_USER`, `MEDIFLOW_DB_PASSWORD`, `MEDIFLOW_DB_NAME`, dan `MEDIFLOW_DB_PORT`. Jangan membuat database baru.

## Registrasi akun

Layar masuk berisi username atau email, kata sandi, dan tombol **Belum punya akun? Daftar**. Tidak ada pemilih peran di form masuk. Pendaftaran dimulai dengan pilihan Pasien, Dokter, atau Apoteker (disimpan sebagai role `farmasi`). Pendaftar mengisi nama, email (paling panjang 50 karakter, sekaligus menjadi username), nomor telepon yang diawali `08`, dan kata sandi minimal 10 karakter, lalu menekan Lanjut dan melengkapi profil. Kode WhatsApp tidak diminta karena pengiriman pesan belum terhubung. Nomor telepon disimpan sebagai kontak. Kata sandi baru disimpan dengan PBKDF2-HMAC-SHA256; akun demo lama tetap memakai SHA-256.

Pasien melengkapi NIK, tanggal lahir, jenis kelamin, dan alamat, lalu menyetujui kebijakan privasi. Nomor BPJS dan SATUSEHAT masih ditanyakan di formulir, tetapi `patients` tidak punya kolom untuk keduanya. Pasien aktif setelah data lengkap dan mendapat nomor rekam medis. Dokter menambah SIP, spesialisasi, dan poli; apoteker menambah SIPA. Akun staf tetap nonaktif sampai admin menyetujuinya. Nomor izin disimpan di log aktivitas karena tidak ada tabel permohonan. Permohonan yang ditolak tidak bisa dipakai untuk masuk.

Peran tidak disimpan di `accounts`. Admin, dokter, farmasi, dan pasien masing-masing punya baris di `admins`, `dokter`, `farmasi_staff`, atau `patients`. Jumlah pendaftaran tetap dibatasi per alamat IP dan nomor telepon di `auth_rate_limits`. `POST /api/auth/register/verify-otp` menjawab HTTP 410: verifikasi kode WhatsApp belum tersedia.

Data yang dipakai aplikasi sudah ada di database `mediflow` (koneksi DBeaver MediFlow). `db/schema.sql` hanya menyimpan salinan dump 22 Oktober 2026 pukul 09:35 (berkas `dump-medicflow_db-202610220935.sql`) dan lembar login dummy, sudah dipetakan ke skema `accounts` / `patients`. Jangan mengimpor berkas itu untuk membuat database baru. `db/sesuaikan-akun-demo.sql` pernah dipasang supaya `admin`, `dokter01`, `farmasi01`, `dummy_pasien01`, dan `dummy_pasien02` bisa masuk: kata sandi demo memakai SHA-256. Lembar Excel menandai kata sandi dua pasien dummy sebagai belum disetel; keduanya memakai `pasien123`. Akun yang mendaftar sendiri tidak diubah.

Dump sumber memakai tabel `users` dan `pasien`. Dua baris pasien lama tetap menempel pada akun staf karena demikian adanya di dump: Budi Santoso Edit pada akun `admin`, dan Pasien Uji MedicFlow pada akun `dokter01`. Keduanya tidak punya username pasien sendiri. Nomor BPJS Budi tidak disimpan karena tabel `patients` tidak punya kolom itu. `scripts\run-application.ps1 dummy` mengisi data uji lain dari `api/src/mediflow/DemoData.java` (akun `budi` dan antrean panjang) dan mengganti 17 tabel katalog. `auth_rate_limits` tidak ikut.

Jika MySQL tidak terjangkau, proses Java memakai data uji di memori dan tidak menuliskannya kembali. Jika proses Java berhenti, aplikasi tidak bisa dibuka: layar tidak lagi disimpan di peramban.

Perintah `dummy` mengganti 17 tabel katalog dengan data uji. Akun layar masuk pada data uji itu adalah `budi`, `dokter01`, `admin`, dan `farmasi01`. Pasien lain (Siti, Andi, Rina, Joko, Maya, Dewi, Agus, Hendra) dan dokter Poli Gigi, Anak, serta Mata ada di data uji supaya antrean dan jadwal terlihat. Antrean hari ini di Poli Umum Dr. Ahmad:

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
| 9 | Siti Rahayu | Siap diambil | QR dan teks `MF-SITI-SIAP`, PIN keluarga `482913` |

Budi juga punya dua kunjungan lama: satu selesai di apotek rumah sakit, satu ditebus di apotek luar. Cetirizine di bawah stok minimum. Vitamin C disembunyikan dan tetap menempel pada resep lama. Dr. Ahmad praktik setiap hari pukul 08.00–12.00. Poli lain punya jadwal di hari kerja, jadi Budi bisa mendaftar kunjungan kedua dari tombol Kunjungan lain.

Tombol Kembalikan data demo di panel admin memuat ulang data uji untuk hari ini. Tombol itu juga menulis ke MySQL bila koneksi hidup.

## Perintah

| Perintah | Fungsi |
| --- | --- |
| `scripts\run-application.ps1` | Menyusun sumber Java, lalu menjalankan aplikasi di port 8088 |
| `scripts\run-application.ps1 periksa` | Pemeriksaan alur di memori: jadwal ganda, stok habis, kode yang dipakai ulang, gambar QR, dan catatan stok |
| `scripts\run-application.ps1 dummy` | Mengganti 17 tabel katalog di `mediflow` dengan data uji hari ini. `auth_rate_limits` tidak ikut |
| `scripts\run-application.ps1 henti` | Menghentikan proses Java aplikasi di port 8088 |

Di Visual Studio Code, **Run application** membuka Edge ke http://127.0.0.1:8088 dan menyalakan server yang sama.

## Struktur

```
MediFlow/
  public/
    mediflow.css           gaya layar
    application.js         token sesi, registrasi, izin notifikasi, dan tutup toast
    service-worker.js      cache berkas statis dan notifikasi; halaman serta /api tidak di-cache
    manifest.json          identitas pemasangan aplikasi
    assets/icons/          ikon 24, 48, 192, dan 512
  scripts/
    run-application.ps1    menyusun dan menjalankan aplikasi Java
  db/
    schema.sql             salinan skema mediflow (dokumentasi; jangan impor)
    migrations/            skema lama `users`/`pasien`; jangan dijalankan pada mediflow
    sesuaikan-akun-demo.sql  sudah dipasang; jangan dijalankan ulang pada database yang hidup
  api/
    lib/                   jar Jackson dan MySQL, tanpa Maven
    src/mediflow/          domain, API, dan layar HTML
    src/mediflow/qr/       pembuat kode QR (Project Nayuki, lisensi MIT)
```

Kode Java ada di `api/src/mediflow/`:

| Berkas | Isi |
| --- | --- |
| `ApplicationServer.java` | HTTP di 127.0.0.1:8088, halaman dan jalur data |
| `PatientRegistration.java` | Registrasi akun tanpa kode WhatsApp, plus batas pendaftaran |
| `WebSite.java` | Sesi per jendela dan berkas statis |
| `HtmlPages.java` | HTML masuk dan beranda |
| `QrGambar.java` | Gambar QR pengambilan, berupa SVG dari kode serah terima |
| `VisitStore.java` | Masuk, kunjungan, resep, dan stok |
| `VisitRules.java` | Tahap, bentrok jadwal, stok, dan teks notifikasi |
| `ScreenText.java` | Sepuluh langkah, empat bagian, tanggal, dan rupiah |
| `DemoData.java` | Data cadangan bila MySQL tidak menjawab |
| `DatabaseRecords.java` | Bentuk data, sama dengan tabel MySQL |
| `PasswordHash.java` | Hash kata sandi (SHA-256 demo dan PBKDF2 pendaftaran) serta penyamaran NIK |
| `VisitFlowCheck.java` | Pemeriksaan alur |
| `LoadDemoData.java` | Menulis data uji ke MySQL |
| `Database.java` | Koneksi MySQL |
| `DatabaseSnapshot.java` | Baca dan tulis 17 tabel katalog. `auth_rate_limits` tidak ikut |
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
| POST | `/api/auth/register/verify-otp` — HTTP 410; kode WhatsApp belum terhubung |
| POST | `/api/auth/register/profile` — pasien: email, NIK, nomor BPJS, `tanggal_lahir`, `jenis_kelamin`, alamat, `nomor_satusehat`, dan `persetujuan_privasi`; staf: email, `license_number`, `specialization`, dan `id_poli` untuk dokter |

`GET /` dengan header `Accept: application/json` juga mengembalikan objek kesehatan itu. Permintaan peramban biasa mendapat halaman HTML.

Balasan CRUD berbentuk `{ "status", "message", "data" }`. Daftar bisa disaring lewat query, misalnya `GET /api/pendaftaran_poli?id_pasien=2&tanggal_kunjungan=2026-10-08`. Semua jalur hanya menjawab dari komputer ini. Jadwal yang bertumpuk, kuota di bawah antrean hari ini, atau jadwal yang disembunyikan padahal masih ada antrean hari ini ditolak. Pasien tidak bisa mendaftar dua kali pada jadwal dan tanggal yang sama. Pesan notifikasi tidak boleh memuat kata WhatsApp, nama obat, atau diagnosis.

Perubahan dari layar menulis 17 tabel katalog lewat snapshot. `auth_rate_limits` tidak ikut, jadi batas pendaftaran tetap ada setelah layar disimpan. Dua jendela yang menyimpan bersamaan saling menimpa: simpanan terakhir mengganti perubahan yang lain.

Tabel: `accounts`, `admins`, `farmasi_staff`, `patients`, `poliklinik`, `dokter`, `jadwal_dokter`, `obat`, `pendaftaran_poli`, `pemeriksaan`, `resep`, `detail_resep`, `notifikasi`, `otp_verifikasi`, `auth_rate_limits`, `pengambilan_obat`, `mutasi_stok`, `log_aktivitas`.