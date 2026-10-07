/** Model data MediFlow, mengikuti db/schema.sql (medicflow_db). */

export type Role = 'admin' | 'pasien' | 'dokter' | 'farmasi';

export type JenisKelamin = 'L' | 'P';

export type Hari =
  | 'Minggu'
  | 'Senin'
  | 'Selasa'
  | 'Rabu'
  | 'Kamis'
  | 'Jumat'
  | 'Sabtu';

export type StatusAntrean =
  | 'Menunggu'
  | 'Dipanggil'
  | 'Masuk Ruangan'
  | 'Selesai Pemeriksaan'
  | 'Batal';

export type StatusResep =
  | 'Menunggu Pilihan'
  | 'Antrean Farmasi'
  | 'Sedang Diracik'
  | 'Siap Diambil'
  | 'Selesai Diambil'
  | 'Dibatalkan';

export type PilihanPenebusan = 'Belum Memilih' | 'Apotek RS' | 'Apotek Luar';

export type JenisObat = 'Jadi' | 'Racikan';

export type JenisNotifikasi =
  | 'Verifikasi WA'
  | 'Konfirmasi Pendaftaran'
  | 'Pengingat Kunjungan'
  | 'QR Resep'
  | 'Obat Siap';

export type ChannelNotifikasi = 'WhatsApp' | 'PWA';

export type StatusNotifikasi = 'Pending' | 'Terkirim' | 'Gagal';

export type JenisMutasi = 'Masuk' | 'Keluar' | 'Penyesuaian';

export type DiambilOleh = 'Pasien' | 'Keluarga';

export type MetodeVerifikasi = 'QR' | 'QR + PIN' | 'QR + OTP';

export type StatusPengambilan = 'Berhasil' | 'Ditolak';

/** Dihitung dari stok, tidak disimpan di tabel detail_resep. */
export type Ketersediaan = 'tersedia' | 'sebagian' | 'kosong';

export interface User {
  id_user: number;
  username: string;
  password_hash: string;
  role: Role;
  nama_lengkap: string;
  no_telepon: string | null;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface Pasien {
  id_pasien: number;
  id_user: number;
  nik: string;
  nomor_bpjs: string | null;
  nama_lengkap: string;
  tanggal_lahir: string;
  jenis_kelamin: JenisKelamin;
  alamat: string | null;
  no_telepon: string;
  terdaftar_satusehat: boolean;
  created_at: string;
  updated_at: string;
}

export interface Dokter {
  id_dokter: number;
  id_user: number;
  id_poli: number;
  nama_dokter: string;
  spesialisasi: string | null;
  rata_waktu_periksa_menit: number;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface Poliklinik {
  id_poli: number;
  nama_poli: string;
  deskripsi: string | null;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface JadwalDokter {
  id_jadwal: number;
  id_dokter: number;
  hari: Hari;
  jam_mulai: string;
  jam_selesai: string;
  kuota_maksimal: number;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface Obat {
  id_obat: number;
  kode_kemenkes: string;
  nama_obat: string;
  jenis_obat: JenisObat;
  satuan: string;
  cover_bpjs: boolean;
  stok_rs: number;
  stok_minimum: number;
  harga: number;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface PendaftaranPoli {
  id_pendaftaran: number;
  id_pasien: number;
  id_jadwal: number;
  tanggal_kunjungan: string;
  nomor_antrean: number;
  waktu_check_in: string | null;
  estimasi_jam_masuk: string | null;
  status_antrean: StatusAntrean;
  catatan_pasien: string | null;
  created_at: string;
  updated_at: string;
}

export interface Pemeriksaan {
  id_pemeriksaan: number;
  id_pendaftaran: number;
  id_dokter: number;
  keluhan: string;
  tekanan_darah: string | null;
  suhu_tubuh: number | null;
  berat_badan: number | null;
  tinggi_badan: number | null;
  diagnosis: string;
  tindakan: string;
  catatan_dokter: string | null;
  waktu_mulai: string | null;
  waktu_selesai: string | null;
  created_at: string;
  updated_at: string;
}

export interface Resep {
  id_resep: number;
  id_pendaftaran: number;
  kode_qr_unik: string;
  waktu_diterbitkan: string;
  pilihan_penebusan: PilihanPenebusan;
  estimasi_jam_selesai: string | null;
  status_resep: StatusResep;
  kadaluarsa_qr: string | null;
  created_at: string;
  updated_at: string;
}

export interface DetailResep {
  id_detail: number;
  id_resep: number;
  id_obat: number;
  jumlah: number;
  dosis_aturan_pakai: string;
  instruksi_racikan: string | null;
  catatan: string | null;
  created_at: string;
  updated_at: string;
}

export interface Notifikasi {
  id_notifikasi: number;
  id_pasien: number;
  id_pendaftaran: number | null;
  jenis_notifikasi: JenisNotifikasi;
  /** Aplikasi ini mengisi PWA. Skema juga mengizinkan WhatsApp. */
  channel: ChannelNotifikasi;
  nomor_tujuan: string;
  pesan: string;
  status: StatusNotifikasi;
  waktu_dikirim: string | null;
  response_api: string | null;
  created_at: string;
}

export interface OtpVerifikasi {
  id_otp: number;
  id_user: number;
  kode_otp: string;
  tujuan: string;
  expired_at: string;
  verified_at: string | null;
  attempt: number;
  created_at: string;
}

export interface PengambilanObat {
  id_pengambilan: number;
  id_resep: number;
  id_user_petugas: number;
  diambil_oleh: DiambilOleh;
  nama_pengambil: string | null;
  waktu_pengambilan: string;
  metode_verifikasi: MetodeVerifikasi;
  status: StatusPengambilan;
  catatan: string | null;
  created_at: string;
  updated_at: string;
}

export interface MutasiStok {
  id_mutasi: number;
  id_obat: number;
  id_user: number;
  jenis_mutasi: JenisMutasi;
  jumlah: number;
  stok_sebelum: number;
  stok_sesudah: number;
  keterangan: string | null;
  created_at: string;
}

export interface LogAktivitas {
  id_log: number;
  id_user: number | null;
  aktivitas: string;
  tabel_referensi: string | null;
  id_referensi: number | null;
  keterangan: string | null;
  ip_address: string | null;
  created_at: string;
}

export interface Database {
  users: User[];
  pasien: Pasien[];
  dokter: Dokter[];
  poliklinik: Poliklinik[];
  jadwal_dokter: JadwalDokter[];
  obat: Obat[];
  pendaftaran_poli: PendaftaranPoli[];
  pemeriksaan: Pemeriksaan[];
  resep: Resep[];
  detail_resep: DetailResep[];
  notifikasi: Notifikasi[];
  otp_verifikasi: OtpVerifikasi[];
  pengambilan_obat: PengambilanObat[];
  mutasi_stok: MutasiStok[];
  log_aktivitas: LogAktivitas[];
}

export interface Session {
  id_user: number;
  role: Role;
}
