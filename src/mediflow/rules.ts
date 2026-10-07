import type {
  Hari,
  JenisNotifikasi,
  JenisObat,
  Ketersediaan,
  PendaftaranPoli,
  PilihanPenebusan,
  StatusAntrean,
  StatusResep,
} from './types';

export interface KonteksPesan {
  poli?: string;
  tanggal?: string;
  jam?: string;
  nomor?: number;
  mode?: 'checkin' | 'panggil';
  selesai?: boolean;
}

const HARI: Hari[] = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];

export function tanggalIso(date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function namaHari(date = new Date()): Hari {
  return HARI[date.getDay()];
}

export function jamPendek(jam: string): string {
  return jam.slice(0, 5);
}

/** Jam masuk = jam mulai praktik + (nomor antrean - 1) × rata-rata menit periksa. */
export function estimasiJamMasuk(
  tanggal: string,
  jamMulai: string,
  nomorAntrean: number,
  rataMenit: number
): string {
  const [jam, menit] = jamMulai.split(':').map((part) => Number(part));
  const start = new Date(
    `${tanggal}T${String(jam).padStart(2, '0')}:${String(menit).padStart(2, '0')}:00+07:00`
  );
  start.setMinutes(start.getMinutes() + Math.max(0, nomorAntrean - 1) * rataMenit);
  return start.toISOString();
}

export function statusKetersediaan(stok: number, jumlah: number): Ketersediaan {
  if (stok <= 0) return 'kosong';
  if (stok < jumlah) return 'sebagian';
  return 'tersedia';
}

export function bolehTebusRs(statuses: Ketersediaan[]): boolean {
  return statuses.length > 0 && statuses.every((status) => status === 'tersedia');
}

/** 10 menit per resep di depan, 5 menit obat jadi, 15 menit obat racikan. */
export function estimasiFarmasiMenit(antrianDiDepan: number, jenis: JenisObat[]): number {
  const racik = jenis.reduce((sum, item) => sum + (item === 'Racikan' ? 15 : 5), 0);
  return Math.max(0, antrianDiDepan) * 10 + racik;
}

export function bentrokPasien(
  rows: Pick<PendaftaranPoli, 'id_pasien' | 'id_jadwal' | 'tanggal_kunjungan' | 'status_antrean'>[],
  idPasien: number,
  idJadwal: number,
  tanggal: string
): boolean {
  return rows.some(
    (row) =>
      row.id_pasien === idPasien &&
      row.id_jadwal === idJadwal &&
      row.tanggal_kunjungan === tanggal &&
      row.status_antrean !== 'Batal'
  );
}

/** Indeks unique_antrean_harian mencakup baris Batal. Nomor tidak dipakai ulang. */
export function bentrokNomorAntrean(
  rows: Pick<PendaftaranPoli, 'id_jadwal' | 'tanggal_kunjungan' | 'nomor_antrean'>[],
  idJadwal: number,
  tanggal: string,
  nomor: number
): boolean {
  return rows.some(
    (row) => row.id_jadwal === idJadwal && row.tanggal_kunjungan === tanggal && row.nomor_antrean === nomor
  );
}

export function qrMasihBerlaku(kedaluwarsa: string | null, dipakai: boolean, sekarang = Date.now()): boolean {
  if (dipakai || !kedaluwarsa) return false;
  return new Date(kedaluwarsa).getTime() > sekarang;
}

export interface KonteksTahap {
  statusAntrean: StatusAntrean;
  checkIn: boolean;
  adaVital: boolean;
  resep: { status: StatusResep; pilihan: PilihanPenebusan; terkirim: boolean } | null;
}

/** Posisi 1–10 diturunkan dari kolom skema, tidak disimpan sendiri. */
export function hitungTahap(ctx: KonteksTahap): number {
  if (ctx.statusAntrean === 'Batal') return 0;
  const resep = ctx.resep;
  if (resep?.pilihan === 'Apotek Luar') return 10;
  if (resep?.status === 'Selesai Diambil') return 10;
  if (resep?.status === 'Siap Diambil') return 9;
  if (resep?.status === 'Sedang Diracik') return 8;
  if (resep?.status === 'Antrean Farmasi') return 7;
  if (resep?.terkirim && resep.status === 'Menunggu Pilihan') return 6;
  if (ctx.statusAntrean === 'Selesai Pemeriksaan') return 5;
  if (ctx.statusAntrean === 'Masuk Ruangan') return 4;
  if (ctx.statusAntrean === 'Dipanggil' || ctx.adaVital) return 3;
  if (ctx.checkIn) return 2;
  return 1;
}

/** Notifikasi di dalam PWA. Sengaja tanpa nama obat dan tanpa diagnosis. */
export function judulNotifikasi(jenis: JenisNotifikasi, pesan = ''): string {
  if (jenis === 'Obat Siap' && pesan.startsWith('Kunjungan selesai')) return 'Kunjungan selesai';
  switch (jenis) {
    case 'Verifikasi WA':
      return 'Verifikasi nomor';
    case 'Konfirmasi Pendaftaran':
      return 'Pendaftaran berhasil';
    case 'Pengingat Kunjungan':
      return 'Pengingat kunjungan';
    case 'QR Resep':
      return 'E-resep terbit';
    case 'Obat Siap':
      return 'Obat siap diambil';
    default:
      return 'MediFlow';
  }
}

export function teksNotifikasi(jenis: JenisNotifikasi, ctx: KonteksPesan): string {
  const nomor = ctx.nomor ?? '-';
  switch (jenis) {
    case 'Verifikasi WA':
      return 'Notifikasi MediFlow aktif. Pembaruan kunjungan akan muncul di aplikasi ini.';
    case 'Konfirmasi Pendaftaran':
      return `Pendaftaran antrean ${nomor} berhasil di ${ctx.poli}. Estimasi masuk ${ctx.jam} pada ${ctx.tanggal}.`;
    case 'Pengingat Kunjungan':
      if (ctx.mode === 'checkin') {
        return `Check-in berhasil untuk antrean ${nomor}. Estimasi masuk ruang ${ctx.jam}.`;
      }
      if (ctx.mode === 'panggil') {
        return `Anda dipanggil masuk ruang periksa. Nomor antrean ${nomor}.`;
      }
      return `Pengingat kunjungan hari ini di ${ctx.poli ?? 'poliklinik'}. Nomor antrean ${nomor}. Silakan datang dan lakukan check-in.`;
    case 'QR Resep':
      return 'E-resep sudah terbit. Buka tab E-Resep untuk melihat ketersediaan dan memilih tempat tebus. Notifikasi ini tidak memuat nama obat.';
    case 'Obat Siap':
      if (ctx.selesai) {
        return 'Kunjungan selesai. Riwayat pengobatan ditandai terkirim ke rekam medis rumah sakit.';
      }
      return `Obat untuk antrean ${nomor} siap diambil di farmasi rumah sakit. Buka tab E-Resep dan tunjukkan kode. Berlaku 24 jam dan hanya sekali pakai.`;
    default:
      return 'Ada pembaruan pada kunjungan MediFlow Anda.';
  }
}

export function tambahJam(iso: string, jam: number): string {
  return new Date(new Date(iso).getTime() + jam * 60 * 60 * 1000).toISOString();
}
