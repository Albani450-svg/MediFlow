-- MediFlow — skema PostgreSQL
-- Menggabungkan rancangan awal, field yang kurang, tabel yang belum ada,
-- dan kebutuhan alur (estimasi poli/farmasi, 10 tahap, QR sekali pakai).
-- PWA demo menyimpan bentuk yang sama di localStorage (src/mediflow).

CREATE TABLE users (
  id_user            SERIAL PRIMARY KEY,
  username           VARCHAR(60) NOT NULL UNIQUE,
  password_hash      VARCHAR(255) NOT NULL,
  role               VARCHAR(20) NOT NULL CHECK (role IN ('pasien', 'dokter', 'admin', 'apoteker')),
  nama_lengkap       VARCHAR(120) NOT NULL,
  email              VARCHAR(120) NOT NULL UNIQUE,
  no_wa              VARCHAR(20) NOT NULL,
  wa_terverifikasi_pada TIMESTAMPTZ,
  is_aktif           BOOLEAN NOT NULL DEFAULT TRUE,
  last_login         TIMESTAMPTZ,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pasien (
  id_pasien          SERIAL PRIMARY KEY,
  id_user            INT NOT NULL UNIQUE REFERENCES users(id_user),
  nik_terenkripsi    TEXT NOT NULL,
  no_rekam_medis     VARCHAR(30) NOT NULL UNIQUE,
  nama               VARCHAR(120) NOT NULL,
  no_telepon         VARCHAR(20) NOT NULL,
  tanggal_lahir      DATE NOT NULL,
  jenis_kelamin      CHAR(1) NOT NULL CHECK (jenis_kelamin IN ('L', 'P')),
  alamat             TEXT NOT NULL,
  no_bpjs            VARCHAR(20),
  terdaftar_satusehat BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE poli (
  id_poli            SERIAL PRIMARY KEY,
  kode               VARCHAR(4) NOT NULL UNIQUE,
  nama_poli          VARCHAR(80) NOT NULL
);

CREATE TABLE dokter (
  id_dokter          SERIAL PRIMARY KEY,
  id_user            INT NOT NULL UNIQUE REFERENCES users(id_user),
  id_poli            INT NOT NULL REFERENCES poli(id_poli),
  no_sip             VARCHAR(40) NOT NULL,
  spesialisasi       VARCHAR(80) NOT NULL,
  rata_waktu_periksa_menit INT NOT NULL CHECK (rata_waktu_periksa_menit > 0)
);

CREATE TABLE apoteker (
  id_apoteker        SERIAL PRIMARY KEY,
  id_user            INT NOT NULL UNIQUE REFERENCES users(id_user),
  no_sipa            VARCHAR(40) NOT NULL
);

CREATE TABLE jadwal_dokter (
  id_jadwal          SERIAL PRIMARY KEY,
  id_dokter          INT NOT NULL REFERENCES dokter(id_dokter),
  id_poli            INT NOT NULL REFERENCES poli(id_poli),
  hari               VARCHAR(10) NOT NULL CHECK (hari IN ('Minggu','Senin','Selasa','Rabu','Kamis','Jumat','Sabtu')),
  jam_mulai          TIME NOT NULL,
  jam_selesai        TIME NOT NULL,
  kuota              INT NOT NULL CHECK (kuota > 0)
);

CREATE TABLE obat (
  id_obat            SERIAL PRIMARY KEY,
  kode_obat          VARCHAR(40) NOT NULL UNIQUE,
  kode_kemenkes      VARCHAR(40) NOT NULL,
  nama_obat          VARCHAR(160) NOT NULL,
  jenis_obat         VARCHAR(20) NOT NULL CHECK (jenis_obat IN ('Jadi', 'Racikan')),
  satuan             VARCHAR(30) NOT NULL,
  bentuk_kekuatan    VARCHAR(80) NOT NULL,
  stok               INT NOT NULL DEFAULT 0 CHECK (stok >= 0),
  stok_minimum       INT NOT NULL DEFAULT 0,
  harga              NUMERIC(12,2) NOT NULL DEFAULT 0,
  cover_bpjs         BOOLEAN NOT NULL DEFAULT FALSE,
  is_aktif           BOOLEAN NOT NULL DEFAULT TRUE,
  tanggal_kedaluwarsa DATE,
  nomor_batch        VARCHAR(40)
);

CREATE TABLE pendaftaran_poli (
  id_pendaftaran     SERIAL PRIMARY KEY,
  id_pasien          INT NOT NULL REFERENCES pasien(id_pasien),
  id_jadwal          INT NOT NULL REFERENCES jadwal_dokter(id_jadwal),
  tanggal_kunjungan  DATE NOT NULL,
  nomor_antrean      INT NOT NULL CHECK (nomor_antrean > 0),
  kode_booking       VARCHAR(20) NOT NULL UNIQUE,
  jenis_penjamin     VARCHAR(10) NOT NULL CHECK (jenis_penjamin IN ('bpjs', 'umum')),
  status_antrean     VARCHAR(20) NOT NULL CHECK (status_antrean IN ('Terdaftar','Menunggu','Masuk Ruangan','Selesai','Batal')),
  tahap_alur         SMALLINT NOT NULL DEFAULT 1 CHECK (tahap_alur BETWEEN 1 AND 10),
  waktu_check_in     TIMESTAMPTZ,
  waktu_dibatalkan   TIMESTAMPTZ,
  estimasi_jam_masuk TIMESTAMPTZ NOT NULL,
  loket              VARCHAR(80) NOT NULL,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Satu pasien tidak boleh daftar ganda pada jadwal dan tanggal yang sama.
CREATE UNIQUE INDEX uq_pendaftaran_aktif
  ON pendaftaran_poli (id_pasien, id_jadwal, tanggal_kunjungan)
  WHERE status_antrean <> 'Batal';

-- Nomor antrean tidak boleh kembar pada jadwal dan tanggal yang sama.
CREATE UNIQUE INDEX uq_nomor_antrean
  ON pendaftaran_poli (id_jadwal, tanggal_kunjungan, nomor_antrean)
  WHERE status_antrean <> 'Batal';

CREATE TABLE pemeriksaan (
  id_pemeriksaan     SERIAL PRIMARY KEY,
  id_pendaftaran     INT NOT NULL UNIQUE REFERENCES pendaftaran_poli(id_pendaftaran),
  id_dokter          INT NOT NULL REFERENCES dokter(id_dokter),
  keluhan            TEXT NOT NULL,
  durasi_keluhan     VARCHAR(40) NOT NULL DEFAULT '',
  alergi             VARCHAR(160) NOT NULL DEFAULT '',
  diagnosis          TEXT NOT NULL DEFAULT '',
  tindakan           TEXT NOT NULL DEFAULT '',
  catatan            TEXT NOT NULL DEFAULT '',
  kode_icd10         VARCHAR(10) NOT NULL DEFAULT '',
  tensi              VARCHAR(20) NOT NULL DEFAULT '',
  suhu               VARCHAR(20) NOT NULL DEFAULT '',
  berat_badan        VARCHAR(20) NOT NULL DEFAULT '',
  jadwal_kontrol     VARCHAR(80) NOT NULL DEFAULT '',
  waktu_mulai        TIMESTAMPTZ,
  waktu_selesai      TIMESTAMPTZ
);

CREATE TABLE resep (
  id_resep           SERIAL PRIMARY KEY,
  nomor_resep        VARCHAR(30) NOT NULL UNIQUE,
  id_pemeriksaan     INT NOT NULL UNIQUE REFERENCES pemeriksaan(id_pemeriksaan),
  id_pendaftaran     INT NOT NULL UNIQUE REFERENCES pendaftaran_poli(id_pendaftaran),
  id_dokter          INT NOT NULL REFERENCES dokter(id_dokter),
  id_apoteker        INT REFERENCES apoteker(id_apoteker),
  kode_qr            VARCHAR(80) NOT NULL UNIQUE,
  kode_qr_kedaluwarsa TIMESTAMPTZ,
  qr_dipakai         BOOLEAN NOT NULL DEFAULT FALSE,
  pin_pengambil      VARCHAR(10),
  status_resep       VARCHAR(30) NOT NULL CHECK (status_resep IN (
    'Draft','Menunggu Pilihan','Verifikasi Kasir','Antrean Farmasi','Sedang Diracik',
    'Pengecekan','Siap Diambil','Selesai Diambil','Tebus Luar'
  )),
  pilihan_tebus      VARCHAR(20) NOT NULL CHECK (pilihan_tebus IN ('belum_memilih','apotek_rs','apotek_luar')),
  estimasi_selesai   TIMESTAMPTZ,
  waktu_diterbitkan  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  waktu_dikirim      TIMESTAMPTZ,
  waktu_siap         TIMESTAMPTZ,
  waktu_diambil      TIMESTAMPTZ,
  diverifikasi_oleh  INT REFERENCES users(id_user)
);

CREATE TABLE detail_resep (
  id_detail          SERIAL PRIMARY KEY,
  id_resep           INT NOT NULL REFERENCES resep(id_resep),
  id_obat            INT NOT NULL REFERENCES obat(id_obat),
  jumlah             INT NOT NULL CHECK (jumlah > 0),
  dosis              VARCHAR(80) NOT NULL,
  aturan_pakai       VARCHAR(120) NOT NULL,
  instruksi_racikan  TEXT,
  status_ketersediaan VARCHAR(20) NOT NULL CHECK (status_ketersediaan IN ('tersedia','sebagian','kosong'))
);

CREATE TABLE notifikasi (
  id_notifikasi      SERIAL PRIMARY KEY,
  id_user            INT NOT NULL REFERENCES users(id_user),
  jenis_kejadian     VARCHAR(40) NOT NULL,
  kanal              VARCHAR(20) NOT NULL CHECK (kanal IN ('whatsapp','push')),
  isi_pesan          TEXT NOT NULL,
  status_kirim       VARCHAR(20) NOT NULL CHECK (status_kirim IN ('antrian','terkirim','gagal')),
  jumlah_percobaan   INT NOT NULL DEFAULT 0,
  waktu_kirim        TIMESTAMPTZ,
  referensi_id       VARCHAR(40) NOT NULL
);

CREATE TABLE push_subscription (
  id_subscription    SERIAL PRIMARY KEY,
  id_user            INT NOT NULL REFERENCES users(id_user),
  endpoint           TEXT NOT NULL,
  p256dh             TEXT NOT NULL,
  auth               TEXT NOT NULL,
  dibuat_pada        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE log_stok (
  id_log             SERIAL PRIMARY KEY,
  id_obat            INT NOT NULL REFERENCES obat(id_obat),
  jenis              VARCHAR(20) NOT NULL CHECK (jenis IN ('masuk','keluar','penyesuaian')),
  jumlah             INT NOT NULL CHECK (jumlah >= 0),
  stok_sebelum       INT NOT NULL,
  stok_sesudah       INT NOT NULL,
  keterangan         TEXT NOT NULL,
  id_user            INT NOT NULL REFERENCES users(id_user),
  id_detail          INT REFERENCES detail_resep(id_detail),
  waktu              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE log_status_resep (
  id_log             SERIAL PRIMARY KEY,
  id_resep           INT NOT NULL REFERENCES resep(id_resep),
  status_lama        VARCHAR(30) NOT NULL,
  status_baru        VARCHAR(30) NOT NULL,
  id_user            INT NOT NULL REFERENCES users(id_user),
  waktu              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  catatan            TEXT NOT NULL
);

CREATE TABLE refresh_token (
  id_token           SERIAL PRIMARY KEY,
  id_user            INT NOT NULL REFERENCES users(id_user),
  token_hash         VARCHAR(255) NOT NULL,
  kedaluwarsa        TIMESTAMPTZ NOT NULL,
  dicabut_pada       TIMESTAMPTZ,
  dibuat_pada        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
  id_audit           SERIAL PRIMARY KEY,
  id_user            INT NOT NULL REFERENCES users(id_user),
  aksi               VARCHAR(40) NOT NULL,
  entitas            VARCHAR(40) NOT NULL,
  id_entitas         INT NOT NULL,
  waktu              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  keterangan         TEXT NOT NULL
);
