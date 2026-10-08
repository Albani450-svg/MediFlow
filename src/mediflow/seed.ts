import { formatJam } from './format';
import { estimasiJamMasuk, namaHari, tanggalIso, teksNotifikasi } from './rules';
import type {
  Database,
  DetailResep,
  Dokter,
  Hari,
  JadwalDokter,
  JenisKelamin,
  JenisNotifikasi,
  LogAktivitas,
  MutasiStok,
  Notifikasi,
  Obat,
  OtpVerifikasi,
  Pasien,
  Pemeriksaan,
  PendaftaranPoli,
  PengambilanObat,
  Resep,
  User,
} from './types';

/**
 * Data uji MediFlow.
 * Akun layar masuk tetap budi/pasien123, dokter01/dokter123, admin/admin123, farmasi01/apotek123.
 * Kunjungan pasien lain mengisi antrean hari ini. Poli Gigi, Anak, dan Mata punya jadwal
 * supaya pasien bisa mendaftar lagi di poli yang berbeda.
 * Resep siap diambil: kode MF-SITI-SIAP, PIN keluarga 482913.
 */

const AWAL = '2026-10-02T20:53:44.000Z';
const PASIEN_DIBUAT = '2026-10-07T03:46:17.000Z';

const HASH = {
  pasien: 'b3cb1bf1350e826eafc2250c570837e1ef1a0e8d6fece2c3478740670ce8fed1',
  dokter: 'b3959dee9b178b030c2b8373da55a04ab3adb318edeb178953ff8b77301a360a',
  admin: '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
  farmasi: '4561c9fdf633bb9b5a6d72ec03f9568d683f3fa2cb3a8e8cd1c10c967f902553',
};

const ATURAN = '3x1 Sehari (Sesudah Makan)';
const TINDAKAN = 'Rawat Jalan + Terapi Obat';

export function createDatabase(sekarang = new Date()): Database {
  const tanggal = tanggalIso(sekarang);
  const kemarin3 = geserHari(sekarang, -3);
  const kemarin10 = geserHari(sekarang, -10);
  const jadwal = buatJadwal();
  const obat = buatObat();
  const jadwalHariIni = jadwalPada(jadwal, 1, tanggal);
  const kadaluarsaSiti = new Date(sekarang.getTime() + 20 * 60 * 60 * 1000).toISOString();
  const kadaluarsaLama = saat(kemarin3, '18:00');

  const users: User[] = [
    akun(1, 'admin', HASH.admin, 'admin', 'Administrator', '081234567890', AWAL),
    akun(2, 'dokter01', HASH.dokter, 'dokter', 'Dr. Ahmad', '081234567891', AWAL),
    akun(3, 'farmasi01', HASH.farmasi, 'farmasi', 'Petugas Farmasi', '081234567892', AWAL),
    akun(4, 'budi', HASH.pasien, 'pasien', 'Budi Santoso Edit', '089876543210', PASIEN_DIBUAT),
    akun(5, 'dokter02', HASH.dokter, 'dokter', 'Dr. Sari Wijaya', '081234567893', AWAL),
    akun(6, 'dokter03', HASH.dokter, 'dokter', 'Dr. Bima Putra', '081234567894', AWAL),
    akun(7, 'dokter04', HASH.dokter, 'dokter', 'Dr. Lina Kusuma', '081234567895', AWAL),
  ];

  const pasien: Pasien[] = [
    pasienBaru(2, 4, '3471001234567890', '0001234567890', 'Budi Santoso Edit', '2000-01-15', 'L', 'Sleman, Yogyakarta', '089876543210', true),
  ];

  const tambahan: Profil[] = [
    profil(3, 8, 'siti', 'Siti Rahayu', '3471012345678901', '0001234567891', '1992-03-14', 'P', 'Bantul, Yogyakarta', '081300000001'),
    profil(4, 9, 'andi', 'Andi Wijaya', '3471012345678902', '0001234567892', '1988-11-02', 'L', 'Kulon Progo, Yogyakarta', '081300000002'),
    profil(5, 10, 'rina', 'Rina Kusuma', '3471012345678903', '0001234567893', '1998-07-21', 'P', 'Kota Yogyakarta', '081300000003'),
    profil(6, 11, 'joko', 'Joko Susilo', '3471012345678904', '0001234567894', '1975-05-09', 'L', 'Sleman, Yogyakarta', '081300000004'),
    profil(7, 12, 'maya', 'Maya Anggraini', '3471012345678905', '0001234567895', '2001-12-30', 'P', 'Bantul, Yogyakarta', '081300000005'),
    profil(8, 13, 'dewi', 'Dewi Lestari', '3471012345678906', '0001234567896', '1985-02-18', 'P', 'Gunungkidul, Yogyakarta', '081300000006'),
    profil(9, 14, 'agus', 'Agus Pratama', '3471012345678907', '0001234567897', '1990-09-25', 'L', 'Sleman, Yogyakarta', '081300000007'),
    profil(10, 15, 'hendra', 'Hendra Saputra', '3471012345678908', null, '2003-06-11', 'L', 'Kota Yogyakarta', '081300000008'),
  ];
  tambahan.forEach((row) => {
    users.push(akun(row.idUser, row.username, HASH.pasien, 'pasien', row.nama, row.telp, PASIEN_DIBUAT));
    pasien.push(pasienBaru(row.idPasien, row.idUser, row.nik, row.bpjs, row.nama, row.lahir, row.jk, row.alamat, row.telp, false));
  });

  const dokter: Dokter[] = [
    dokterBaru(1, 2, 1, 'Dr. Ahmad', 'Dokter Umum', 15),
    dokterBaru(2, 5, 2, 'Dr. Sari Wijaya', 'Dokter Gigi', 20),
    dokterBaru(3, 6, 3, 'Dr. Bima Putra', 'Dokter Anak', 20),
    dokterBaru(4, 7, 4, 'Dr. Lina Kusuma', 'Dokter Mata', 15),
  ];

  const pendaftaran: PendaftaranPoli[] = [];
  const pemeriksaan: Pemeriksaan[] = [];
  const resep: Resep[] = [];
  const detail: DetailResep[] = [];
  const notifikasi: Notifikasi[] = [];
  const otp: OtpVerifikasi[] = [];
  const pengambilan: PengambilanObat[] = [];

  const masuk = (row: PendaftaranPoli, periksa: Pemeriksaan) => {
    pendaftaran.push(row);
    pemeriksaan.push(periksa);
  };

  const estimasi = (nomor: number, baris = jadwalHariIni) =>
    estimasiJamMasuk(tanggal, baris.jam_mulai, nomor, 15);

  masuk(
    daftar(15, 2, jadwalHariIni.id_jadwal, tanggal, 1, saat(tanggal, '07:40'), estimasi(1), 'Masuk Ruangan', 'Tidak ada alergi obat', saat(tanggal, '06:30'), saat(tanggal, '08:05')),
    periksa(15, 15, 1, 'Demam naik turun disertai nyeri menelan dan batuk kering.', saat(tanggal, '08:05'), {
      tekanan_darah: '120/80',
      suhu_tubuh: 37.8,
      berat_badan: 64,
      tinggi_badan: 170,
    })
  );
  notifikasi.push(
    pesan(1, 2, 15, 'Konfirmasi Pendaftaran', '089876543210', { nomor: 1, poli: 'Poli Umum', tanggal, jam: formatJam(estimasi(1)) }, saat(tanggal, '06:30')),
    pesan(2, 2, 15, 'Pengingat Kunjungan', '089876543210', { nomor: 1, poli: 'Poli Umum' }, saat(tanggal, '07:00'))
  );

  masuk(
    daftar(14, 10, jadwalHariIni.id_jadwal, tanggal, 2, null, estimasi(2), 'Menunggu', null, saat(tanggal, '06:40'), saat(tanggal, '06:40')),
    periksa(14, 14, 1, 'Pusing sejak semalam, belum minum obat.', saat(tanggal, '06:40'))
  );

  masuk(
    daftar(13, 4, jadwalHariIni.id_jadwal, tanggal, 3, saat(tanggal, '07:50'), estimasi(3), 'Menunggu', 'Alergi debu, bukan alergi obat.', saat(tanggal, '06:45'), saat(tanggal, '07:50')),
    periksa(13, 13, 1, 'Batuk berdahak selama tiga hari.', saat(tanggal, '06:45'))
  );

  masuk(
    daftar(12, 5, jadwalHariIni.id_jadwal, tanggal, 4, saat(tanggal, '07:55'), estimasi(4), 'Dipanggil', 'Alergi penisilin.', saat(tanggal, '06:50'), saat(tanggal, '08:00')),
    periksa(12, 12, 1, 'Nyeri ulu hati setelah makan pedas.', saat(tanggal, '06:50'), {
      tekanan_darah: '110/70',
      suhu_tubuh: 36.6,
      berat_badan: 52,
      tinggi_badan: 158,
    })
  );

  masuk(
    daftar(11, 6, jadwalHariIni.id_jadwal, tanggal, 5, saat(tanggal, '08:00'), estimasi(5), 'Selesai Pemeriksaan', null, saat(tanggal, '06:55'), saat(tanggal, '08:30')),
    periksa(11, 11, 1, 'Perut kembung dan mual sejak dua hari.', saat(tanggal, '08:10'), {
      tekanan_darah: '130/85',
      suhu_tubuh: 36.8,
      berat_badan: 70,
      tinggi_badan: 168,
      diagnosis: 'K29.7 - Gastritis / Dispepsia Akut',
      tindakan: TINDAKAN,
      catatan_dokter: 'Hindari makanan pedas dan makan teratur.',
    })
  );
  resep.push(resepBaru(1, 11, 'MF-JOKO-DRAF', saat(tanggal, '08:35'), 'Belum Memilih', 'Menunggu Pilihan', null, null));
  detail.push(item(1, 1, 1, 10, ATURAN, null, saat(tanggal, '08:35')));

  masuk(
    daftar(10, 7, jadwalHariIni.id_jadwal, tanggal, 6, saat(tanggal, '08:10'), estimasi(6), 'Selesai Pemeriksaan', null, saat(tanggal, '07:00'), saat(tanggal, '09:00')),
    periksa(10, 10, 1, 'Diare sejak kemarin malam.', saat(tanggal, '08:20'), {
      tekanan_darah: '100/70',
      suhu_tubuh: 37.2,
      berat_badan: 48,
      tinggi_badan: 155,
      diagnosis: 'A09 - Gastroenteritis dan Diare Infeksi',
      tindakan: TINDAKAN,
      catatan_dokter: 'Banyak minum air.',
      waktu_selesai: saat(tanggal, '08:50'),
    })
  );
  resep.push(resepBaru(2, 10, 'MF-MAYA-PILIH', saat(tanggal, '08:50'), 'Apotek RS', 'Menunggu Pilihan', saat(tanggal, '09:20'), null));
  detail.push(item(2, 2, 1, 2, ATURAN, null, saat(tanggal, '08:50')));
  notifikasi.push(pesan(3, 7, 10, 'QR Resep', '081300000005', { nomor: 6 }, saat(tanggal, '08:50')));

  masuk(
    daftar(9, 8, jadwalHariIni.id_jadwal, tanggal, 7, saat(tanggal, '08:15'), estimasi(7), 'Selesai Pemeriksaan', 'Tidak ada alergi obat', saat(tanggal, '07:05'), saat(tanggal, '09:10')),
    periksa(9, 9, 1, 'Demam dan pilek sejak kemarin.', saat(tanggal, '08:30'), {
      tekanan_darah: '118/76',
      suhu_tubuh: 38.1,
      berat_badan: 55,
      tinggi_badan: 160,
      diagnosis: 'J06.9 - Infeksi Saluran Napas Atas (ISPA)',
      tindakan: TINDAKAN,
      catatan_dokter: 'Istirahat dan habiskan obat.',
      waktu_selesai: saat(tanggal, '09:00'),
    })
  );
  resep.push(resepBaru(3, 9, 'MF-DEWI-ANTRE', saat(tanggal, '09:00'), 'Apotek RS', 'Antrean Farmasi', saat(tanggal, '09:30'), null));
  detail.push(item(3, 3, 2, 2, ATURAN, null, saat(tanggal, '09:00')));

  masuk(
    daftar(8, 9, jadwalHariIni.id_jadwal, tanggal, 8, saat(tanggal, '08:20'), estimasi(8), 'Selesai Pemeriksaan', null, saat(tanggal, '07:10'), saat(tanggal, '09:20')),
    periksa(8, 8, 1, 'Batuk berdahak dan tenggorokan gatal.', saat(tanggal, '08:40'), {
      tekanan_darah: '125/80',
      suhu_tubuh: 36.9,
      berat_badan: 62,
      tinggi_badan: 165,
      diagnosis: 'J06.9 - Infeksi Saluran Napas Atas (ISPA)',
      tindakan: TINDAKAN,
      catatan_dokter: 'Racikan diminum setelah makan.',
      waktu_selesai: saat(tanggal, '09:05'),
    })
  );
  resep.push(resepBaru(4, 8, 'MF-AGUS-RACIK', saat(tanggal, '09:05'), 'Apotek RS', 'Sedang Diracik', saat(tanggal, '09:40'), null));
  detail.push(item(4, 4, 4, 1, ATURAN, 'Racik sesuai aturan farmasi', saat(tanggal, '09:05')));

  masuk(
    daftar(7, 3, jadwalHariIni.id_jadwal, tanggal, 9, saat(tanggal, '08:25'), estimasi(9), 'Selesai Pemeriksaan', null, saat(tanggal, '07:15'), saat(tanggal, '09:30')),
    periksa(7, 7, 1, 'Pusing dan tengkuk terasa berat.', saat(tanggal, '08:45'), {
      tekanan_darah: '150/95',
      suhu_tubuh: 36.5,
      berat_badan: 58,
      tinggi_badan: 157,
      diagnosis: 'I10 - Hipertensi Esensial Primer',
      tindakan: TINDAKAN,
      catatan_dokter: 'Kontrol tekanan darah minggu depan.',
      waktu_selesai: saat(tanggal, '09:15'),
    })
  );
  resep.push(resepBaru(5, 7, 'MF-SITI-SIAP', saat(tanggal, '09:15'), 'Apotek RS', 'Siap Diambil', saat(tanggal, '09:45'), kadaluarsaSiti));
  detail.push(item(5, 5, 1, 1, '1x1 Sehari (Malam Sebelum Tidur)', null, saat(tanggal, '09:15')));
  otp.push({
    id_otp: 1,
    id_user: 8,
    kode_otp: '482913',
    tujuan: '081300000001',
    expired_at: kadaluarsaSiti,
    verified_at: null,
    attempt: 0,
    created_at: saat(tanggal, '09:15'),
  });
  notifikasi.push(pesan(4, 3, 7, 'Obat Siap', '081300000001', { nomor: 9 }, saat(tanggal, '09:15')));

  const jadwalLama = jadwalPada(jadwal, 1, kemarin3);
  const jadwalLuar = jadwalPada(jadwal, 1, kemarin10);
  masuk(
    daftar(6, 2, jadwalLama.id_jadwal, kemarin3, 1, saat(kemarin3, '07:45'), estimasiJamMasuk(kemarin3, jadwalLama.jam_mulai, 1, 15), 'Selesai Pemeriksaan', null, saat(kemarin3, '06:20'), saat(kemarin3, '10:00')),
    periksa(6, 6, 1, 'Batuk dan demam tiga hari.', saat(kemarin3, '08:00'), {
      tekanan_darah: '120/78',
      suhu_tubuh: 37.4,
      berat_badan: 64,
      tinggi_badan: 170,
      diagnosis: 'J06.9 - Infeksi Saluran Napas Atas (ISPA)',
      tindakan: TINDAKAN,
      catatan_dokter: 'Kontrol bila demam berlanjut.',
      waktu_selesai: saat(kemarin3, '08:40'),
    })
  );
  resep.push(resepBaru(6, 6, 'MF-BUDI-LAMA', saat(kemarin3, '08:40'), 'Apotek RS', 'Selesai Diambil', saat(kemarin3, '09:10'), kadaluarsaLama));
  detail.push(
    item(6, 6, 2, 6, ATURAN, null, saat(kemarin3, '08:40')),
    item(7, 6, 5, 10, '1x1 Sehari (Malam Sebelum Tidur)', null, saat(kemarin3, '08:40'))
  );
  otp.push({
    id_otp: 2,
    id_user: 4,
    kode_otp: '193847',
    tujuan: '089876543210',
    expired_at: kadaluarsaLama,
    verified_at: saat(kemarin3, '10:00'),
    attempt: 1,
    created_at: saat(kemarin3, '09:10'),
  });
  pengambilan.push({
    id_pengambilan: 1,
    id_resep: 6,
    id_user_petugas: 3,
    diambil_oleh: 'Pasien',
    nama_pengambil: 'Budi Santoso Edit',
    waktu_pengambilan: saat(kemarin3, '10:00'),
    metode_verifikasi: 'QR',
    status: 'Berhasil',
    catatan: null,
    created_at: saat(kemarin3, '10:00'),
    updated_at: saat(kemarin3, '10:00'),
  });
  notifikasi.push(
    pesan(5, 2, 6, 'Obat Siap', '089876543210', { nomor: 1, selesai: true }, saat(kemarin3, '10:00'))
  );

  masuk(
    daftar(5, 2, jadwalLuar.id_jadwal, kemarin10, 1, saat(kemarin10, '08:00'), estimasiJamMasuk(kemarin10, jadwalLuar.jam_mulai, 1, 15), 'Selesai Pemeriksaan', 'Alergi debu.', saat(kemarin10, '07:00'), saat(kemarin10, '09:00')),
    periksa(5, 5, 1, 'Gatal di kulit setelah debu.', saat(kemarin10, '08:10'), {
      tekanan_darah: '118/76',
      suhu_tubuh: 36.6,
      berat_badan: 64,
      tinggi_badan: 170,
      diagnosis: 'K29.7 - Gastritis / Dispepsia Akut',
      tindakan: TINDAKAN,
      catatan_dokter: 'Tebus di apotek luar karena stok rumah sakit kurang.',
      waktu_selesai: saat(kemarin10, '08:40'),
    })
  );
  resep.push(resepBaru(7, 5, 'MF-BUDI-LUAR', saat(kemarin10, '08:40'), 'Apotek Luar', 'Menunggu Pilihan', null, null));
  detail.push(item(8, 7, 3, 10, '1x1 Sehari (Malam Sebelum Tidur)', null, saat(kemarin10, '08:40')));

  const mutasi: MutasiStok[] = [
    barisMutasi(1, 1, 3, 'Masuk', 100, 0, 100, 'Stok awal', AWAL),
    barisMutasi(2, 2, 3, 'Masuk', 80, 0, 80, 'Stok awal', AWAL),
    barisMutasi(3, 3, 3, 'Masuk', 4, 0, 4, 'Stok awal di bawah minimum', AWAL),
    barisMutasi(4, 4, 3, 'Masuk', 20, 0, 20, 'Stok awal', AWAL),
    barisMutasi(5, 5, 3, 'Masuk', 10, 0, 10, 'Stok awal', AWAL),
    barisMutasi(6, 2, 3, 'Keluar', 6, 80, 74, 'Penyerahan resep 6', saat(kemarin3, '10:00')),
    barisMutasi(7, 5, 3, 'Keluar', 10, 10, 0, 'Penyerahan resep 6', saat(kemarin3, '10:00')),
  ];

  const log: LogAktivitas[] = [
    jejak(1, 4, 'DAFTAR_POLI', 'pendaftaran_poli', 15, 'Antrean 1 Poli Umum', saat(tanggal, '06:30')),
    jejak(2, 1, 'SIMPAN_VITAL', 'pemeriksaan', 15, 'Tanda vital diperbarui', saat(tanggal, '07:50')),
    jejak(3, 2, 'SIMPAN_PEMERIKSAAN', 'pemeriksaan', 11, 'K29.7 - Gastritis / Dispepsia Akut', saat(tanggal, '08:30')),
    jejak(4, 12, 'PILIH_TEBUS', 'resep', 2, 'Apotek RS', saat(tanggal, '08:50')),
    jejak(5, 1, 'LANJUT_LANGKAH', 'pendaftaran_poli', 9, 'Ke langkah 7', saat(tanggal, '09:00')),
    jejak(6, 3, 'UBAH_STATUS_RESEP', 'resep', 5, 'Sedang Diracik -> Siap Diambil. Siap di farmasi', saat(tanggal, '09:15')),
    jejak(7, 3, 'SERAH_OBAT', 'pengambilan_obat', 6, 'pasien', saat(kemarin3, '10:00')),
    jejak(8, 3, 'NONAKTIF_OBAT', 'obat', 5, 'OBT005', AWAL),
  ];

  return {
    users,
    pasien,
    dokter,
    poliklinik: [
      { id_poli: 1, nama_poli: 'Poli Umum', deskripsi: 'Pelayanan kesehatan umum', is_active: true, created_at: AWAL, updated_at: AWAL },
      { id_poli: 2, nama_poli: 'Poli Gigi', deskripsi: 'Pelayanan kesehatan gigi dan mulut', is_active: true, created_at: AWAL, updated_at: AWAL },
      { id_poli: 3, nama_poli: 'Poli Anak', deskripsi: 'Pelayanan kesehatan anak', is_active: true, created_at: AWAL, updated_at: AWAL },
      { id_poli: 4, nama_poli: 'Poli Mata', deskripsi: 'Pelayanan kesehatan mata', is_active: true, created_at: AWAL, updated_at: AWAL },
    ],
    jadwal_dokter: jadwal,
    obat,
    pendaftaran_poli: pendaftaran,
    pemeriksaan,
    resep,
    detail_resep: detail,
    notifikasi,
    otp_verifikasi: otp,
    pengambilan_obat: pengambilan,
    mutasi_stok: mutasi,
    log_aktivitas: log,
  };
}

interface Profil {
  idPasien: number;
  idUser: number;
  username: string;
  nama: string;
  nik: string;
  bpjs: string | null;
  lahir: string;
  jk: JenisKelamin;
  alamat: string;
  telp: string;
}

function profil(
  idPasien: number,
  idUser: number,
  username: string,
  nama: string,
  nik: string,
  bpjs: string | null,
  lahir: string,
  jk: JenisKelamin,
  alamat: string,
  telp: string
): Profil {
  return { idPasien, idUser, username, nama, nik, bpjs, lahir, jk, alamat, telp };
}

function akun(
  id: number,
  username: string,
  hash: string,
  role: User['role'],
  nama: string,
  telp: string,
  dibuat: string
): User {
  return {
    id_user: id,
    username,
    password_hash: hash,
    role,
    nama_lengkap: nama,
    no_telepon: telp,
    is_active: true,
    created_at: dibuat,
    updated_at: dibuat,
  };
}

function pasienBaru(
  id: number,
  idUser: number,
  nik: string,
  bpjs: string | null,
  nama: string,
  lahir: string,
  jk: JenisKelamin,
  alamat: string,
  telp: string,
  satusehat: boolean
): Pasien {
  return {
    id_pasien: id,
    id_user: idUser,
    nik,
    nomor_bpjs: bpjs,
    nama_lengkap: nama,
    tanggal_lahir: lahir,
    jenis_kelamin: jk,
    alamat,
    no_telepon: telp,
    terdaftar_satusehat: satusehat,
    created_at: PASIEN_DIBUAT,
    updated_at: PASIEN_DIBUAT,
  };
}

function dokterBaru(id: number, idUser: number, idPoli: number, nama: string, spesialisasi: string, menit: number): Dokter {
  return {
    id_dokter: id,
    id_user: idUser,
    id_poli: idPoli,
    nama_dokter: nama,
    spesialisasi,
    rata_waktu_periksa_menit: menit,
    is_active: true,
    created_at: AWAL,
    updated_at: AWAL,
  };
}

function buatJadwal(): JadwalDokter[] {
  const baris: JadwalDokter[] = [];
  const pagi: Hari[] = ['Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu', 'Minggu'];
  pagi.forEach((hari, index) => baris.push(slot(index + 1, 1, hari, '08:00:00', '12:00:00', 30)));
  const gigi: Hari[] = ['Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat'];
  gigi.forEach((hari, index) => baris.push(slot(8 + index, 2, hari, '09:00:00', '13:00:00', 20)));
  (['Selasa', 'Kamis', 'Sabtu'] as Hari[]).forEach((hari, index) => baris.push(slot(13 + index, 3, hari, '08:00:00', '11:00:00', 15)));
  (['Senin', 'Rabu', 'Jumat'] as Hari[]).forEach((hari, index) => baris.push(slot(16 + index, 4, hari, '13:00:00', '16:00:00', 12)));
  return baris;
}

function slot(id: number, idDokter: number, hari: Hari, mulai: string, selesai: string, kuota: number): JadwalDokter {
  return {
    id_jadwal: id,
    id_dokter: idDokter,
    hari,
    jam_mulai: mulai,
    jam_selesai: selesai,
    kuota_maksimal: kuota,
    is_active: true,
    created_at: AWAL,
    updated_at: AWAL,
  };
}

function jadwalPada(baris: JadwalDokter[], idDokter: number, tanggal: string): JadwalDokter {
  const hari = namaHari(new Date(`${tanggal}T12:00:00+07:00`));
  const ketemu = baris.find((row) => row.id_dokter === idDokter && row.hari === hari && row.is_active);
  if (!ketemu) throw new Error(`Jadwal dokter ${idDokter} pada ${hari} tidak ada`);
  return ketemu;
}

function buatObat(): Obat[] {
  return [
    obat(1, 'OBT001', 'Paracetamol 500mg', 'Jadi', 'Tablet', true, 100, 20, 500),
    obat(2, 'OBT002', 'Amoxicillin 500mg', 'Jadi', 'Kapsul', true, 74, 20, 1000),
    obat(3, 'OBT003', 'Cetirizine 10mg', 'Jadi', 'Tablet', false, 4, 10, 1500),
    obat(4, 'OBT004', 'Racikan Batuk', 'Racikan', 'Paket', false, 20, 5, 15000),
    obat(5, 'OBT005', 'Vitamin C 500mg', 'Jadi', 'Tablet', false, 0, 5, 800, false),
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
  harga: number,
  aktif = true
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
    is_active: aktif,
    created_at: AWAL,
    updated_at: AWAL,
  };
}

function daftar(
  id: number,
  idPasien: number,
  idJadwal: number,
  tanggal: string,
  nomor: number,
  checkIn: string | null,
  estimasi: string,
  status: PendaftaranPoli['status_antrean'],
  catatan: string | null,
  dibuat: string,
  diubah: string
): PendaftaranPoli {
  return {
    id_pendaftaran: id,
    id_pasien: idPasien,
    id_jadwal: idJadwal,
    tanggal_kunjungan: tanggal,
    nomor_antrean: nomor,
    waktu_check_in: checkIn,
    estimasi_jam_masuk: estimasi,
    status_antrean: status,
    catatan_pasien: catatan,
    created_at: dibuat,
    updated_at: diubah,
  };
}

function periksa(
  id: number,
  idPendaftaran: number,
  idDokter: number,
  keluhan: string,
  waktu: string,
  isi: Partial<Pemeriksaan> = {}
): Pemeriksaan {
  return {
    id_pemeriksaan: id,
    id_pendaftaran: idPendaftaran,
    id_dokter: idDokter,
    keluhan,
    tekanan_darah: null,
    suhu_tubuh: null,
    berat_badan: null,
    tinggi_badan: null,
    diagnosis: '',
    tindakan: '',
    catatan_dokter: null,
    waktu_mulai: waktu,
    waktu_selesai: null,
    created_at: waktu,
    updated_at: isi.waktu_selesai ?? waktu,
    ...isi,
  };
}

function resepBaru(
  id: number,
  idPendaftaran: number,
  kode: string,
  terbit: string,
  pilihan: Resep['pilihan_penebusan'],
  status: Resep['status_resep'],
  estimasi: string | null,
  kadaluarsa: string | null
): Resep {
  return {
    id_resep: id,
    id_pendaftaran: idPendaftaran,
    kode_qr_unik: kode,
    waktu_diterbitkan: terbit,
    pilihan_penebusan: pilihan,
    estimasi_jam_selesai: estimasi,
    status_resep: status,
    kadaluarsa_qr: kadaluarsa,
    created_at: terbit,
    updated_at: terbit,
  };
}

function item(
  id: number,
  idResep: number,
  idObat: number,
  jumlah: number,
  aturan: string,
  racikan: string | null,
  waktu: string
): DetailResep {
  return {
    id_detail: id,
    id_resep: idResep,
    id_obat: idObat,
    jumlah,
    dosis_aturan_pakai: aturan,
    instruksi_racikan: racikan,
    catatan: null,
    created_at: waktu,
    updated_at: waktu,
  };
}

function pesan(
  id: number,
  idPasien: number,
  idPendaftaran: number,
  jenis: JenisNotifikasi,
  nomor: string,
  ctx: Parameters<typeof teksNotifikasi>[1],
  waktu: string
): Notifikasi {
  return {
    id_notifikasi: id,
    id_pasien: idPasien,
    id_pendaftaran: idPendaftaran,
    jenis_notifikasi: jenis,
    channel: 'PWA',
    nomor_tujuan: nomor,
    pesan: teksNotifikasi(jenis, ctx),
    status: 'Terkirim',
    waktu_dikirim: waktu,
    response_api: null,
    created_at: waktu,
  };
}

function barisMutasi(
  id: number,
  idObat: number,
  idUser: number,
  jenis: MutasiStok['jenis_mutasi'],
  jumlah: number,
  sebelum: number,
  sesudah: number,
  keterangan: string,
  waktu: string
): MutasiStok {
  return {
    id_mutasi: id,
    id_obat: idObat,
    id_user: idUser,
    jenis_mutasi: jenis,
    jumlah,
    stok_sebelum: sebelum,
    stok_sesudah: sesudah,
    keterangan,
    created_at: waktu,
  };
}

function jejak(
  id: number,
  idUser: number,
  aktivitas: string,
  tabel: string,
  idReferensi: number,
  keterangan: string,
  waktu: string
): LogAktivitas {
  return {
    id_log: id,
    id_user: idUser,
    aktivitas,
    tabel_referensi: tabel,
    id_referensi: idReferensi,
    keterangan,
    ip_address: '127.0.0.1',
    created_at: waktu,
  };
}

function saat(tanggal: string, jam: string): string {
  const [jamAngka, menit] = jam.split(':');
  return new Date(`${tanggal}T${jamAngka.padStart(2, '0')}:${menit.padStart(2, '0')}:00+07:00`).toISOString();
}

function geserHari(asal: Date, hari: number): string {
  return tanggalIso(new Date(asal.getFullYear(), asal.getMonth(), asal.getDate() + hari));
}
