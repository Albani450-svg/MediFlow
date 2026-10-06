/** Model data MediFlow.
 *  Menggabungkan rancangan awal, field yang masih kurang, tabel yang belum ada,
 *  dan alur di dokumen gambaran serta prototipe 10 tahap.
 */

export type Role = 'pasien' | 'dokter' | 'admin' | 'apoteker';

export type JenisKelamin = 'L' | 'P';

export type Hari =
  | 'Minggu'
  | 'Senin'
  | 'Selasa'
  | 'Rabu'
  | 'Kamis'
  | 'Jumat'
  | 'Sabtu';

export type JenisPenjamin = 'bpjs' | 'umum';

export type StatusAntrean =
  | 'Terdaftar'
  | 'Menunggu'
  | 'Masuk Ruangan'
  | 'Selesai'
  | 'Batal';

export type StatusResep =
  | 'Draft'
  | 'Menunggu Pilihan'
  | 'Verifikasi Kasir'
  | 'Antrean Farmasi'
  | 'Sedang Diracik'
  | 'Pengecekan'
  | 'Siap Diambil'
  | 'Selesai Diambil'
  | 'Tebus Luar';

export type PilihanTebus = 'belum_memilih' | 'apotek_rs' | 'apotek_luar';

export type Ketersediaan = 'tersedia' | 'sebagian' | 'kosong';

export type JenisObat = 'Jadi' | 'Racikan';

export type JenisNotifikasi =
  | 'verifikasi_wa'
  | 'konfirmasi_booking'
  | 'pengingat_hari_h'
  | 'info_giliran'
  | 'info_ketersediaan'
  | 'qr_resep'
  | 'obat_siap'
  | 'selesai';

export type StatusKirim = 'antrian' | 'terkirim' | 'gagal';

export type JenisStok = 'masuk' | 'keluar' | 'penyesuaian';

export interface User {
  id_user: number;
  username: string;
  password_hash: string;
  role: Role;
  nama_lengkap: string;
  email: string;
  no_wa: string;
  wa_terverifikasi_pada: string | null;
  is_aktif: boolean;
  last_login: string | null;
  created_at: string;
  updated_at: string;
}

export interface Pasien {
  id_pasien: number;
  id_user: number;
  /** NIK tidak disimpan polos. Lihat encryptNik. */
  nik_terenkripsi: string;
  no_rekam_medis: string;
  nama: string;
  no_telepon: string;
  tanggal_lahir: string;
  jenis_kelamin: JenisKelamin;
  alamat: string;
  no_bpjs: string | null;
  terdaftar_satusehat: boolean;
}

export interface Dokter {
  id_dokter: number;
  id_user: number;
  id_poli: number;
  no_sip: string;
  spesialisasi: string;
  /** Menit rata-rata per pasien. Dipakai estimasi jam masuk poli. */
  rata_waktu_periksa_menit: number;
}

export interface Apoteker {
  id_apoteker: number;
  id_user: number;
  no_sipa: string;
}

export interface Poli {
  id_poli: number;
  kode: string;
  nama_poli: string;
}

export interface JadwalDokter {
  id_jadwal: number;
  id_dokter: number;
  id_poli: number;
  hari: Hari;
  jam_mulai: string;
  jam_selesai: string;
  kuota: number;
}

export interface Obat {
  id_obat: number;
  kode_obat: string;
  kode_kemenkes: string;
  nama_obat: string;
  jenis_obat: JenisObat;
  satuan: string;
  bentuk_kekuatan: string;
  stok: number;
  stok_minimum: number;
  harga: number;
  cover_bpjs: boolean;
  is_aktif: boolean;
  /** Opsional. Tidak semua obat punya batch di master. */
  tanggal_kedaluwarsa: string | null;
  nomor_batch: string | null;
}

export interface PendaftaranPoli {
  id_pendaftaran: number;
  id_pasien: number;
  id_jadwal: number;
  tanggal_kunjungan: string;
  nomor_antrean: number;
  kode_booking: string;
  jenis_penjamin: JenisPenjamin;
  status_antrean: StatusAntrean;
  /** Posisi pada 10 tahap alur antrean dan farmasi (prototipe UI). */
  tahap_alur: number;
  waktu_check_in: string | null;
  waktu_dibatalkan: string | null;
  estimasi_jam_masuk: string;
  loket: string;
  created_at: string;
}

export interface Pemeriksaan {
  id_pemeriksaan: number;
  id_pendaftaran: number;
  id_dokter: number;
  keluhan: string;
  durasi_keluhan: string;
  alergi: string;
  diagnosis: string;
  tindakan: string;
  catatan: string;
  kode_icd10: string;
  tensi: string;
  suhu: string;
  berat_badan: string;
  jadwal_kontrol: string;
  waktu_mulai: string | null;
  waktu_selesai: string | null;
}

export interface Resep {
  id_resep: number;
  nomor_resep: string;
  id_pemeriksaan: number;
  id_pendaftaran: number;
  id_dokter: number;
  id_apoteker: number | null;
  kode_qr: string;
  kode_qr_kedaluwarsa: string | null;
  qr_dipakai: boolean;
  pin_pengambil: string | null;
  status_resep: StatusResep;
  pilihan_tebus: PilihanTebus;
  estimasi_selesai: string | null;
  waktu_diterbitkan: string;
  waktu_dikirim: string | null;
  waktu_siap: string | null;
  waktu_diambil: string | null;
  diverifikasi_oleh: number | null;
}

export interface DetailResep {
  id_detail: number;
  id_resep: number;
  id_obat: number;
  jumlah: number;
  dosis: string;
  aturan_pakai: string;
  instruksi_racikan: string | null;
  status_ketersediaan: Ketersediaan;
}

export interface Notifikasi {
  id_notifikasi: number;
  id_user: number;
  jenis_kejadian: JenisNotifikasi;
  kanal: 'whatsapp' | 'push';
  isi_pesan: string;
  status_kirim: StatusKirim;
  jumlah_percobaan: number;
  waktu_kirim: string | null;
  referensi_id: string;
}

export interface PushSubscription {
  id_subscription: number;
  id_user: number;
  endpoint: string;
  p256dh: string;
  auth: string;
  dibuat_pada: string;
}

export interface LogStok {
  id_log: number;
  id_obat: number;
  jenis: JenisStok;
  jumlah: number;
  stok_sebelum: number;
  stok_sesudah: number;
  keterangan: string;
  id_user: number;
  id_detail: number | null;
  waktu: string;
}

export interface LogStatusResep {
  id_log: number;
  id_resep: number;
  status_lama: string;
  status_baru: StatusResep;
  id_user: number;
  waktu: string;
  catatan: string;
}

export interface RefreshToken {
  id_token: number;
  id_user: number;
  token_hash: string;
  kedaluwarsa: string;
  dicabut_pada: string | null;
  dibuat_pada: string;
}

export interface AuditLog {
  id_audit: number;
  id_user: number;
  aksi: string;
  entitas: string;
  id_entitas: number;
  waktu: string;
  keterangan: string;
}

export interface Database {
  users: User[];
  pasien: Pasien[];
  dokter: Dokter[];
  apoteker: Apoteker[];
  poli: Poli[];
  jadwal_dokter: JadwalDokter[];
  obat: Obat[];
  pendaftaran_poli: PendaftaranPoli[];
  pemeriksaan: Pemeriksaan[];
  resep: Resep[];
  detail_resep: DetailResep[];
  notifikasi: Notifikasi[];
  push_subscription: PushSubscription[];
  log_stok: LogStok[];
  log_status_resep: LogStatusResep[];
  refresh_token: RefreshToken[];
  audit_log: AuditLog[];
}

export interface Session {
  id_user: number;
  role: Role;
  refresh_id: number;
  access_token: string;
}
