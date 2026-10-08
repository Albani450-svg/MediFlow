# MediFlow

MediFlow adalah progressive web app (PWA) untuk antrean poliklinik, rekam medis, dan e-resep rumah sakit. Pasien, admin pendaftaran, dokter, dan farmasi memakai satu aplikasi. Data mengikuti skema MySQL `medicflow_db`.

Aplikasi dipasang dari peramban, tetap bisa dibuka saat API mati, dan menyimpan salinan data di komputer pemakai.

## Peran

| Peran | Akun demo | Kata sandi | Tugas |
| --- | --- | --- | --- |
| Pasien | `budi` | `pasien123` | Daftar poli, check-in, pilih apotek, tunjukkan kode pengambilan |
| Admin | `admin` | `admin123` | Tanda vital, panggil pasien, teruskan resep ke farmasi |
| Dokter | `dokter01` | `dokter123` | Pemeriksaan, diagnosis, kirim resep |
| Farmasi | `farmasi01` | `apotek123` | Siapkan obat, cek ulang, serah terima, sesuaikan stok |

Sesi masuk disimpan di `sessionStorage` (`mediflow-session-v2`). Beberapa jendela peramban bisa masuk sebagai peran yang berbeda pada saat yang sama.

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
| Ambil | 9. Ambil obat di farmasi | Farmasi, lewat kode QR/PIN |
| Ambil | 10. Kunjungan selesai | Tidak ada tindakan |

Pasien tidak memajukan langkah. Pada demo, admin tetap bisa memajukan setiap tahap; spanduk menandai tombol itu sebagai simulasi. Langkah 9 memakai kode serah terima, bukan tombol maju. Kode berlaku 24 jam dan hanya sekali. Keluarga yang mengambil obat memakai PIN di aplikasi. Tidak ada pemindai kamera.

Memilih apotek luar pada langkah 6 menutup kunjungan tanpa mengurangi stok rumah sakit. Antrean farmasi baru menampilkan resep setelah dokter mengirimnya pada langkah 5.

## Menjalankan di komputer ini

Perlu Node.js, PHP 8.1 atau lebih baru (ekstensi `intl`, dipakai CodeIgniter), Composer, dan MySQL atau MariaDB. Pengembangan di sini memakai XAMPP. PHP dicari di `C:\xampp\php\php.exe`, lalu `D:\xampp\php\php.exe`. Lokasi lain diatur lewat variabel lingkungan `PHP_BIN`.

```powershell
# 1. Buat database medicflow_db, lalu impor skema dan penyesuaian demo.
#    phpMyAdmin, atau:
#    mysql -u root < db/schema.sql
#    mysql -u root medicflow_db < db/sesuaikan-pwa.sql

# 2. Pasang dependensi. Ulangi composer install jika folder backend/vendor belum ada.
npm install
composer install --working-dir=backend

# 3. Buka aplikasi. Vite juga menyalakan API bila port 8088 masih kosong.
npm run dev
```

- Halaman: http://127.0.0.1:5173/
- API: http://127.0.0.1:8088/
- Hanya API: `npm run api`
- Komputer lain di jaringan yang sama: `npm run start-remote`, lalu buka alamat LAN yang dicetak Vite

Koneksi MySQL ada di `backend/app/Config/Database.php`. Bawaan: host `127.0.0.1`, user `root`, kata sandi kosong, database `medicflow_db`, port `3306`.

`db/schema.sql` adalah skema beserta data master. `db/sesuaikan-pwa.sql` dijalankan sesudahnya supaya login demo cocok dengan aplikasi: kata sandi demo memakai SHA-256, dan pasien Budi Santoso Edit mendapat akun `budi` sendiri.

Jika API atau MySQL mati, aplikasi memakai salinan di `localStorage` dengan kunci `mediflow-db-v2`. Master data cadangan: Poli Umum, Gigi, Anak, dan Mata; Dr. Ahmad praktik Senin dan Rabu pukul 08.00–12.00 di Poli Umum; empat obat (Paracetamol, Amoxicillin, Cetirizine, Racikan Batuk). Salinan peramban juga menyertakan satu kunjungan demo agar alur terlihat tanpa MySQL.

## Perintah

| Perintah | Fungsi |
| --- | --- |
| `npm run dev` | Vite dengan peramban terbuka, plus API CodeIgniter |
| `npm run api` | Hanya API di port 8088 |
| `npm run start-remote` | Vite yang menerima koneksi dari jaringan lokal |
| `npm run build` | Pemeriksaan TypeScript, lalu hasil produksi di `dist/` |
| `npm run check` | Pemeriksaan alur: jadwal ganda, stok habis, kode QR yang dipakai ulang, dan catatan stok |

## Struktur

```
MediFlow/
  index.html                 halaman pembuka
  public/
    manifest.json            identitas pemasangan PWA
    sw.js                    notifikasi dan cache Workbox
    assets/icons/            ikon 24, 48, 192, dan 512
  src/
    app-index.ts             kulit aplikasi
    router.ts                rute / (masuk) dan /app (beranda)
    pages/app-login/         layar masuk
    pages/app-beranda/       beranda empat peran
    styles/                  global.css dan gaya komponen
    mediflow/                tipe, aturan, simpanan, dan teks antarmuka
  scripts/
    start-api.mjs            menjalankan PHP bila port 8088 kosong
    run-check.mjs            menjalankan scripts/check.ts lewat Vite
  db/
    schema.sql               skema medicflow_db
    sesuaikan-pwa.sql        akun demo setelah impor dump
  backend/                   API CodeIgniter 4
```

Kode domain ada di `src/mediflow/`:

| Berkas | Isi |
| --- | --- |
| `types.ts` | Bentuk data, sama dengan tabel MySQL |
| `store.ts` | Status aplikasi, masuk, kunjungan, resep, dan stok |
| `rules.ts` | Tahap kunjungan, bentrok jadwal, stok, dan teks notifikasi |
| `labels.ts` | Sepuluh langkah, empat bagian, dan kalimat spanduk |
| `remote.ts` | Ambil dan simpan seluruh database lewat API |
| `seed.ts` | Data cadangan bila server tidak menjawab |
| `crypto.ts` | Hash kata sandi demo dan penyamaran NIK |
| `notify.ts` | Izin dan notifikasi perangkat |
| `format.ts` | Tanggal, jam, dan rupiah |

API kustom CodeIgniter:

| Berkas | Isi |
| --- | --- |
| `backend/app/Controllers/Api/DatabaseController.php` | `GET` dan `PUT /api/database` |
| `backend/app/Controllers/Api/PasienController.php` | CRUD `/api/pasien` |
| `backend/app/Libraries/MedicflowSnapshot.php` | Baca dan tulis seluruh tabel |
| `backend/app/Models/PasienModel.php` | Baris pasien |
| `backend/app/Config/Routes.php` | Rute di atas |
| `backend/app/Config/Database.php` | Koneksi MySQL |

Vite meneruskan permintaan `/api` ke `127.0.0.1:8088`. Setiap penyimpanan mengirim seluruh database. Dua jendela yang menyimpan bersamaan saling menimpa: simpanan terakhir mengganti perubahan yang lain.

Tabel: `users`, `pasien`, `poliklinik`, `dokter`, `jadwal_dokter`, `obat`, `pendaftaran_poli`, `pemeriksaan`, `resep`, `detail_resep`, `notifikasi`, `otp_verifikasi`, `pengambilan_obat`, `mutasi_stok`, `log_aktivitas`.

Perubahan stok masuk ke `mutasi_stok`. Pengambilan obat masuk ke `pengambilan_obat`. PIN keluarga masuk ke `otp_verifikasi`.

## Notifikasi

Setiap kabar kunjungan tampil di dalam PWA. Baris disimpan dengan kanal `PWA`, meskipun skema juga mengizinkan WhatsApp. Lonceng dan kartu pasien membaca notifikasi pengguna yang sedang masuk. Toast muncul selama pengguna ada di aplikasi. Notifikasi perangkat hanya muncul bila izin diberikan dan halaman sedang tersembunyi. Teks pesan tidak memuat nama obat atau diagnosis.

Ketukan notifikasi memfokuskan jendela yang sudah terbuka dan mengirim pesan `mediflow-open`.

## Tampilan

Di layar di bawah 960px, kolom aplikasi tetap selebar ponsel (`min(480px, 100%)`). Mulai 960px, lebar menjadi `min(1440px, calc(100% - 32px))`. Layar masuk di desktop terbagi dua: merek di kiri, peran dan formulir di kanan. Kartu pasien, admin, dan farmasi berdampingan. Papan sepuluh langkah terbagi 1–5 dan 6–10. Pemeriksaan dokter, stok, dan jejak audit tetap selebar penuh.

## Batasan demo

Ini aplikasi pengembangan, bukan rancangan produksi.

- Kata sandi demo di-hash SHA-256 di peramban. Produksi memakai hash lambat di server, misalnya bcrypt atau Argon2.
- NIK pada demo disamarkan di peramban. Produksi mengenkripsi NIK di server.
- `PUT /api/database` mengganti seluruh isi tabel. Tidak ada penggabungan perubahan dari dua pengguna.
- API pengembangan hanya dijangkau dari komputer ini, kecuali `npm run start-remote` membuka halaman Vite ke jaringan lokal.
- Belum ada pemindai QR. Petugas mengetik kode yang terlihat di aplikasi pasien.
