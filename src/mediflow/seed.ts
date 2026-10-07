import { formatJam } from './format';
import { estimasiJamMasuk, namaHari, tanggalIso, teksNotifikasi } from './rules';
import type { Database, JadwalDokter, Obat } from './types';

/**
 * Master data mengikuti dump medicflow_db.
 * Dua penyesuaian agar aplikasi bisa dipakai:
 * - kata sandi di-hash dengan cara yang diverifikasi layar masuk (dump memakai placeholder bcrypt);
 * - pasien Budi mendapat akun sendiri. Di dump, baris pasien menunjuk id_user admin.
 * Satu kunjungan demo ditambahkan supaya alur terlihat saat aplikasi dibuka.
 */

const AWAL = '2026-10-02T20:53:44.000Z';
const PASIEN_DIBUAT = '2026-10-07T03:46:17.000Z';

const HASH = {
  pasien: 'b3cb1bf1350e826eafc2250c570837e1ef1a0e8d6fece2c3478740670ce8fed1',
  dokter: 'b3959dee9b178b030c2b8373da55a04ab3adb318edeb178953ff8b77301a360a',
  admin: '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
  farmasi: '4561c9fdf633bb9b5a6d72ec03f9568d683f3fa2cb3a8e8cd1c10c967f902553',
};

export function createDatabase(sekarang = new Date()): Database {
  const hari = namaHari(sekarang);
  const tanggal = tanggalIso(sekarang);
  const jadwal = buatJadwal();
  const obat = buatObat();
  const jadwalHariIni = jadwal.find((row) => row.hari === hari) ?? jadwal[1];
  const estimasi = estimasiJamMasuk(tanggal, jadwalHariIni.jam_mulai, 1, 15);
  const checkIn = new Date(`${tanggal}T07:40:00+07:00`).toISOString();
  const mulai = new Date(`${tanggal}T08:05:00+07:00`).toISOString();
  const daftarPada = new Date(`${tanggal}T06:30:00+07:00`).toISOString();

  return {
    users: [
      {
        id_user: 1,
        username: 'admin',
        password_hash: HASH.admin,
        role: 'admin',
        nama_lengkap: 'Administrator',
        no_telepon: '081234567890',
        is_active: true,
        created_at: AWAL,
        updated_at: AWAL,
      },
      {
        id_user: 2,
        username: 'dokter01',
        password_hash: HASH.dokter,
        role: 'dokter',
        nama_lengkap: 'Dr. Ahmad',
        no_telepon: '081234567891',
        is_active: true,
        created_at: AWAL,
        updated_at: AWAL,
      },
      {
        id_user: 3,
        username: 'farmasi01',
        password_hash: HASH.farmasi,
        role: 'farmasi',
        nama_lengkap: 'Petugas Farmasi',
        no_telepon: '081234567892',
        is_active: true,
        created_at: AWAL,
        updated_at: AWAL,
      },
      {
        id_user: 4,
        username: 'budi',
        password_hash: HASH.pasien,
        role: 'pasien',
        nama_lengkap: 'Budi Santoso Edit',
        no_telepon: '089876543210',
        is_active: true,
        created_at: PASIEN_DIBUAT,
        updated_at: PASIEN_DIBUAT,
      },
    ],
    pasien: [
      {
        id_pasien: 2,
        id_user: 4,
        nik: '3471001234567890',
        nomor_bpjs: '0001234567890',
        nama_lengkap: 'Budi Santoso Edit',
        tanggal_lahir: '2000-01-15',
        jenis_kelamin: 'L',
        alamat: 'Sleman, Yogyakarta',
        no_telepon: '089876543210',
        terdaftar_satusehat: true,
        created_at: PASIEN_DIBUAT,
        updated_at: PASIEN_DIBUAT,
      },
    ],
    dokter: [
      {
        id_dokter: 1,
        id_user: 2,
        id_poli: 1,
        nama_dokter: 'Dr. Ahmad',
        spesialisasi: 'Dokter Umum',
        rata_waktu_periksa_menit: 15,
        is_active: true,
        created_at: AWAL,
        updated_at: AWAL,
      },
    ],
    poliklinik: [
      { id_poli: 1, nama_poli: 'Poli Umum', deskripsi: 'Pelayanan kesehatan umum', is_active: true, created_at: AWAL, updated_at: AWAL },
      { id_poli: 2, nama_poli: 'Poli Gigi', deskripsi: 'Pelayanan kesehatan gigi dan mulut', is_active: true, created_at: AWAL, updated_at: AWAL },
      { id_poli: 3, nama_poli: 'Poli Anak', deskripsi: 'Pelayanan kesehatan anak', is_active: true, created_at: AWAL, updated_at: AWAL },
      { id_poli: 4, nama_poli: 'Poli Mata', deskripsi: 'Pelayanan kesehatan mata', is_active: true, created_at: AWAL, updated_at: AWAL },
    ],
    jadwal_dokter: jadwal,
    obat,
    pendaftaran_poli: [
      {
        id_pendaftaran: 1,
        id_pasien: 2,
        id_jadwal: jadwalHariIni.id_jadwal,
        tanggal_kunjungan: tanggal,
        nomor_antrean: 1,
        waktu_check_in: checkIn,
        estimasi_jam_masuk: estimasi,
        status_antrean: 'Masuk Ruangan',
        catatan_pasien: 'Demam naik turun disertai nyeri menelan dan batuk kering.',
        created_at: daftarPada,
        updated_at: mulai,
      },
    ],
    pemeriksaan: [
      {
        id_pemeriksaan: 1,
        id_pendaftaran: 1,
        id_dokter: 1,
        keluhan: 'Demam naik turun disertai nyeri menelan dan batuk kering.',
        tekanan_darah: '120/80',
        suhu_tubuh: 37.8,
        berat_badan: 64,
        tinggi_badan: 170,
        diagnosis: '',
        tindakan: '',
        catatan_dokter: null,
        waktu_mulai: mulai,
        waktu_selesai: null,
        created_at: mulai,
        updated_at: mulai,
      },
    ],
    resep: [],
    detail_resep: [],
    notifikasi: [
      {
        id_notifikasi: 1,
        id_pasien: 2,
        id_pendaftaran: 1,
        jenis_notifikasi: 'Konfirmasi Pendaftaran',
        channel: 'PWA',
        nomor_tujuan: '089876543210',
        pesan: teksNotifikasi('Konfirmasi Pendaftaran', {
          nomor: 1,
          poli: 'Poli Umum',
          tanggal,
          jam: formatJam(estimasi),
        }),
        status: 'Terkirim',
        waktu_dikirim: daftarPada,
        response_api: null,
        created_at: daftarPada,
      },
      {
        id_notifikasi: 2,
        id_pasien: 2,
        id_pendaftaran: 1,
        jenis_notifikasi: 'Pengingat Kunjungan',
        channel: 'PWA',
        nomor_tujuan: '089876543210',
        pesan: teksNotifikasi('Pengingat Kunjungan', { nomor: 1, poli: 'Poli Umum' }),
        status: 'Terkirim',
        waktu_dikirim: new Date(`${tanggal}T00:30:00+07:00`).toISOString(),
        response_api: null,
        created_at: new Date(`${tanggal}T00:30:00+07:00`).toISOString(),
      },
    ],
    otp_verifikasi: [],
    pengambilan_obat: [],
    mutasi_stok: [],
    log_aktivitas: [],
  };
}

function buatJadwal(): JadwalDokter[] {
  const baris = [
    { id_jadwal: 1, hari: 'Senin' as const },
    { id_jadwal: 2, hari: 'Rabu' as const },
  ];
  return baris.map((row) => ({
    id_jadwal: row.id_jadwal,
    id_dokter: 1,
    hari: row.hari,
    jam_mulai: '08:00:00',
    jam_selesai: '12:00:00',
    kuota_maksimal: 30,
    is_active: true,
    created_at: AWAL,
    updated_at: AWAL,
  }));
}

function buatObat(): Obat[] {
  return [
    obat(1, 'OBT001', 'Paracetamol 500mg', 'Jadi', 'Tablet', true, 100, 20, 500),
    obat(2, 'OBT002', 'Amoxicillin 500mg', 'Jadi', 'Kapsul', true, 80, 20, 1000),
    obat(3, 'OBT003', 'Cetirizine 10mg', 'Jadi', 'Tablet', false, 50, 10, 1500),
    obat(4, 'OBT004', 'Racikan Batuk', 'Racikan', 'Paket', false, 20, 5, 15000),
  ];
}

function obat(
  id: number,
  kode: string,
  nama: string,
  jenis: Obat['jenis_obat'],
  satuan: string,
  bpjs: boolean,
  stok: number,
  minimum: number,
  harga: number
): Obat {
  return {
    id_obat: id,
    kode_kemenkes: kode,
    nama_obat: nama,
    jenis_obat: jenis,
    satuan,
    cover_bpjs: bpjs,
    stok_rs: stok,
    stok_minimum: minimum,
    harga,
    is_active: true,
    created_at: AWAL,
    updated_at: AWAL,
  };
}
