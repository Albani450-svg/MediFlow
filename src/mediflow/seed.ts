import { encryptNik, randomToken } from './crypto';
import { formatJam } from './format';
import { estimasiJamMasuk, namaHari, statusKetersediaan, tanggalIso, teksNotifikasi } from './rules';
import type { Database, Hari, JadwalDokter, Obat } from './types';

const DIBUAT = '2026-09-01T01:00:00.000Z';

const HASH = {
  pasien: 'b3cb1bf1350e826eafc2250c570837e1ef1a0e8d6fece2c3478740670ce8fed1',
  dokter: 'b3959dee9b178b030c2b8373da55a04ab3adb318edeb178953ff8b77301a360a',
  admin: '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
  apotek: '4561c9fdf633bb9b5a6d72ec03f9568d683f3fa2cb3a8e8cd1c10c967f902553',
};

function user(
  id: number,
  username: string,
  passwordHash: string,
  role: Database['users'][number]['role'],
  nama: string,
  email: string,
  noWa: string,
  verified: string | null
): Database['users'][number] {
  return {
    id_user: id,
    username,
    password_hash: passwordHash,
    role,
    nama_lengkap: nama,
    email,
    no_wa: noWa,
    wa_terverifikasi_pada: verified,
    is_aktif: true,
    last_login: null,
    created_at: DIBUAT,
    updated_at: DIBUAT,
  };
}

export function createDatabase(sekarang = new Date()): Database {
  const hari = namaHari(sekarang);
  const tanggal = tanggalIso(sekarang);
  const jadwal = buatJadwal();
  const jadwalBudi = jadwal.find((row) => row.id_dokter === 1 && row.hari === hari)!;
  const obat = buatObat();
  const estimasi = estimasiJamMasuk(tanggal, jadwalBudi.jam_mulai, 5, 15);
  const checkIn = new Date(`${tanggal}T07:40:00+07:00`).toISOString();
  const mulai = new Date(`${tanggal}T08:50:00+07:00`).toISOString();

  const detailAwal = [
    { id_detail: 1, id_obat: 1, jumlah: 10, dosis: '500 mg', aturan: '3x1 Sehari (Sesudah Makan)' },
    { id_detail: 2, id_obat: 2, jumlah: 10, dosis: '500 mg', aturan: '3x1 Sehari (Sesudah Makan)' },
    { id_detail: 3, id_obat: 3, jumlah: 7, dosis: '500 mg', aturan: '1x1 Sehari (Malam Sebelum Tidur)' },
  ];

  return {
    users: [
      user(1, 'budi', HASH.pasien, 'pasien', 'Budi Santoso', 'budi.santoso@example.com', '081234567890', '2026-09-02T02:00:00.000Z'),
      user(2, 'hendra', HASH.dokter, 'dokter', 'dr. Hendra Wijaya', 'hendra.wijaya@rs.example', '081200000002', null),
      user(3, 'siti', HASH.admin, 'admin', 'Siti Rahma', 'siti.rahma@rs.example', '081200000003', null),
      user(4, 'rina', HASH.apotek, 'apoteker', 'apt. Rina Kusuma', 'rina.kusuma@rs.example', '081200000004', null),
      user(5, 'lina', HASH.dokter, 'dokter', 'dr. Lina Kartika', 'lina.kartika@rs.example', '081200000005', null),
    ],
    pasien: [
      {
        id_pasien: 1,
        id_user: 1,
        nik_terenkripsi: encryptNik('3201011204900001'),
        no_rekam_medis: 'RM-0049281',
        nama: 'Budi Santoso',
        no_telepon: '081234567890',
        tanggal_lahir: '1990-04-12',
        jenis_kelamin: 'L',
        alamat: 'Jl. Melati No. 18, Bandung',
        no_bpjs: '0001234567890',
        terdaftar_satusehat: false,
      },
    ],
    dokter: [
      {
        id_dokter: 1,
        id_user: 2,
        id_poli: 1,
        no_sip: 'SIP-3374-PD-2024',
        spesialisasi: 'Sp.PD',
        rata_waktu_periksa_menit: 15,
      },
      {
        id_dokter: 2,
        id_user: 5,
        id_poli: 3,
        no_sip: 'SIP-2291-A-2023',
        spesialisasi: 'Sp.A',
        rata_waktu_periksa_menit: 20,
      },
    ],
    apoteker: [
      {
        id_apoteker: 1,
        id_user: 4,
        no_sipa: 'SIPA-2020-0142',
      },
    ],
    poli: [
      { id_poli: 1, kode: 'A', nama_poli: 'Poli Penyakit Dalam' },
      { id_poli: 2, kode: 'B', nama_poli: 'Poli Jantung' },
      { id_poli: 3, kode: 'C', nama_poli: 'Poli Anak' },
      { id_poli: 4, kode: 'D', nama_poli: 'Poli Umum' },
    ],
    jadwal_dokter: jadwal,
    obat,
    pendaftaran_poli: [
      {
        id_pendaftaran: 1,
        id_pasien: 1,
        id_jadwal: jadwalBudi.id_jadwal,
        tanggal_kunjungan: tanggal,
        nomor_antrean: 5,
        kode_booking: 'A-042',
        jenis_penjamin: 'bpjs',
        status_antrean: 'Masuk Ruangan',
        tahap_alur: 4,
        waktu_check_in: checkIn,
        waktu_dibatalkan: null,
        estimasi_jam_masuk: estimasi,
        loket: 'Loket Farmasi A (BPJS)',
        created_at: new Date(`${tanggal}T06:30:00+07:00`).toISOString(),
      },
    ],
    pemeriksaan: [
      {
        id_pemeriksaan: 1,
        id_pendaftaran: 1,
        id_dokter: 1,
        keluhan: 'Demam naik turun disertai nyeri menelan dan batuk kering.',
        durasi_keluhan: '3 Hari',
        alergi: 'Tidak Ada Alergi Obat',
        diagnosis: 'Faringitis Akut (Radang Tenggorokan)',
        tindakan: 'Rawat Jalan + Terapi Obat',
        catatan:
          'Faring tampak hiperemis (+), tonsil T1-T1. Dianjurkan banyak minum air hangat dan habiskan antibiotik.',
        kode_icd10: 'J02.9',
        tensi: '120/80',
        suhu: '37.8',
        berat_badan: '64',
        jadwal_kontrol: '8 Okt 2026 (Bila keluhan berlanjut)',
        waktu_mulai: mulai,
        waktu_selesai: null,
      },
    ],
    resep: [
      {
        id_resep: 1,
        nomor_resep: `RSP-${tanggal.replace(/-/g, '')}-0001`,
        id_pemeriksaan: 1,
        id_pendaftaran: 1,
        id_dokter: 1,
        id_apoteker: null,
        kode_qr: randomToken(12),
        kode_qr_kedaluwarsa: null,
        qr_dipakai: false,
        pin_pengambil: null,
        status_resep: 'Draft',
        pilihan_tebus: 'belum_memilih',
        estimasi_selesai: null,
        waktu_diterbitkan: mulai,
        waktu_dikirim: null,
        waktu_siap: null,
        waktu_diambil: null,
        diverifikasi_oleh: null,
      },
    ],
    detail_resep: detailAwal.map((row) => {
      const item = obat.find((obatRow) => obatRow.id_obat === row.id_obat)!;
      return {
        id_detail: row.id_detail,
        id_resep: 1,
        id_obat: row.id_obat,
        jumlah: row.jumlah,
        dosis: row.dosis,
        aturan_pakai: row.aturan,
        instruksi_racikan: null,
        status_ketersediaan: statusKetersediaan(item.stok, row.jumlah),
      };
    }),
    notifikasi: [
      {
        id_notifikasi: 1,
        id_user: 1,
        jenis_kejadian: 'verifikasi_wa',
        kanal: 'whatsapp',
        isi_pesan: 'Nomor WhatsApp Anda sudah terverifikasi untuk MediFlow.',
        status_kirim: 'terkirim',
        jumlah_percobaan: 1,
        waktu_kirim: '2026-09-02T02:00:00.000Z',
        referensi_id: 'users:1',
      },
      {
        id_notifikasi: 2,
        id_user: 1,
        jenis_kejadian: 'konfirmasi_booking',
        kanal: 'whatsapp',
        isi_pesan: teksNotifikasi('konfirmasi_booking', {
          kode: 'A-042',
          poli: 'Poli Penyakit Dalam',
          tanggal,
          jam: formatJam(estimasi),
          nomor: 5,
        }),
        status_kirim: 'terkirim',
        jumlah_percobaan: 1,
        waktu_kirim: new Date(`${tanggal}T06:30:00+07:00`).toISOString(),
        referensi_id: 'pendaftaran:1',
      },
      {
        id_notifikasi: 3,
        id_user: 1,
        jenis_kejadian: 'pengingat_hari_h',
        kanal: 'whatsapp',
        isi_pesan: 'Pengingat kunjungan hari ini. Kode booking A-042. Silakan datang dan lakukan check-in.',
        status_kirim: 'terkirim',
        jumlah_percobaan: 1,
        waktu_kirim: new Date(`${tanggal}T00:30:00+07:00`).toISOString(),
        referensi_id: 'pendaftaran:1',
      },
    ],
    push_subscription: [],
    log_stok: [],
    log_status_resep: [],
    refresh_token: [],
    audit_log: [],
  };
}

function buatJadwal(): JadwalDokter[] {
  const hariKerja: Hari[] = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
  const rows: JadwalDokter[] = [];
  hariKerja.forEach((hari, index) => {
    rows.push({
      id_jadwal: index + 1,
      id_dokter: 1,
      id_poli: 1,
      hari,
      jam_mulai: '08:00',
      jam_selesai: '12:00',
      kuota: 20,
    });
    rows.push({
      id_jadwal: index + 8,
      id_dokter: 2,
      id_poli: 3,
      hari,
      jam_mulai: '13:00',
      jam_selesai: '16:00',
      kuota: 12,
    });
  });
  return rows;
}

function buatObat(): Obat[] {
  return [
    {
      id_obat: 1,
      kode_obat: 'OBT-AMOX-500',
      kode_kemenkes: '93001014',
      nama_obat: 'Amoxicillin 500 mg',
      jenis_obat: 'Jadi',
      satuan: 'tablet',
      bentuk_kekuatan: 'Kapsul 500 mg',
      stok: 80,
      stok_minimum: 20,
      harga: 1500,
      cover_bpjs: true,
      is_aktif: true,
      tanggal_kedaluwarsa: '2027-06-30',
      nomor_batch: 'AMX26061',
    },
    {
      id_obat: 2,
      kode_obat: 'OBT-PCT-500',
      kode_kemenkes: '93004555',
      nama_obat: 'Paracetamol 500 mg',
      jenis_obat: 'Jadi',
      satuan: 'tablet',
      bentuk_kekuatan: 'Tablet 500 mg',
      stok: 120,
      stok_minimum: 30,
      harga: 500,
      cover_bpjs: true,
      is_aktif: true,
      tanggal_kedaluwarsa: '2027-12-31',
      nomor_batch: 'PCT26121',
    },
    {
      id_obat: 3,
      kode_obat: 'OBT-VITC-500',
      kode_kemenkes: '93007890',
      nama_obat: 'Vitamin C 500 mg',
      jenis_obat: 'Jadi',
      satuan: 'tablet',
      bentuk_kekuatan: 'Tablet salut 500 mg',
      stok: 40,
      stok_minimum: 15,
      harga: 8000,
      cover_bpjs: false,
      is_aktif: true,
      tanggal_kedaluwarsa: null,
      nomor_batch: null,
    },
    {
      id_obat: 4,
      kode_obat: 'OBT-CTZ-10',
      kode_kemenkes: '93001102',
      nama_obat: 'Cetirizine 10 mg',
      jenis_obat: 'Jadi',
      satuan: 'tablet',
      bentuk_kekuatan: 'Tablet 10 mg',
      stok: 0,
      stok_minimum: 10,
      harga: 2000,
      cover_bpjs: true,
      is_aktif: true,
      tanggal_kedaluwarsa: '2026-11-30',
      nomor_batch: 'CTZ26011',
    },
    {
      id_obat: 5,
      kode_obat: 'OBT-PUYER-DEMAM',
      kode_kemenkes: '93009901',
      nama_obat: 'Puyer penurun demam',
      jenis_obat: 'Racikan',
      satuan: 'puyer',
      bentuk_kekuatan: 'Racikan',
      stok: 25,
      stok_minimum: 8,
      harga: 12000,
      cover_bpjs: true,
      is_aktif: true,
      tanggal_kedaluwarsa: null,
      nomor_batch: null,
    },
  ];
}
