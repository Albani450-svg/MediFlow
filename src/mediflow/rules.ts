import type {
  Hari,
  Ketersediaan,
  JenisNotifikasi,
  JenisObat,
  PendaftaranPoli,
  PilihanTebus,
} from './types';

export interface KonteksPesan {
  kode?: string;
  poli?: string;
  tanggal?: string;
  jam?: string;
  nomor?: number;
  loket?: string;
  mode?: 'checkin' | 'panggil';
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

export function bentrokNomorAntrean(
  rows: Pick<PendaftaranPoli, 'id_jadwal' | 'tanggal_kunjungan' | 'nomor_antrean' | 'status_antrean'>[],
  idJadwal: number,
  tanggal: string,
  nomor: number
): boolean {
  return rows.some(
    (row) =>
      row.id_jadwal === idJadwal &&
      row.tanggal_kunjungan === tanggal &&
      row.nomor_antrean === nomor &&
      row.status_antrean !== 'Batal'
  );
}

export function qrMasihBerlaku(
  kedaluwarsa: string | null,
  dipakai: boolean,
  sekarang = Date.now()
): boolean {
  if (dipakai || !kedaluwarsa) return false;
  return new Date(kedaluwarsa).getTime() > sekarang;
}

/** Pesan WhatsApp sengaja tanpa nama obat dan tanpa diagnosis. */
export function teksNotifikasi(jenis: JenisNotifikasi, ctx: KonteksPesan): string {
  switch (jenis) {
    case 'verifikasi_wa':
      return 'Nomor WhatsApp Anda sudah terverifikasi untuk MediFlow.';
    case 'konfirmasi_booking':
      return `Booking ${ctx.kode} berhasil di ${ctx.poli}. Estimasi masuk ${ctx.jam} pada ${ctx.tanggal}. Nomor antrean ${ctx.nomor}.`;
    case 'pengingat_hari_h':
      return `Pengingat kunjungan hari ini. Kode booking ${ctx.kode}. Silakan datang dan lakukan check-in.`;
    case 'info_giliran':
      if (ctx.mode === 'checkin') {
        return `Check-in berhasil untuk kode ${ctx.kode}. Estimasi masuk ruang ${ctx.jam}.`;
      }
      return `Anda dipanggil ke ruang periksa. Kode booking ${ctx.kode}.`;
    case 'info_ketersediaan':
      return 'E-resep sudah terbit. Buka MediFlow untuk melihat ketersediaan dan memilih tempat tebus. Pesan ini tidak memuat nama obat.';
    case 'qr_resep':
      return `Obat siap diambil di ${ctx.loket}. Buka MediFlow dan tunjukkan kode QR. Berlaku 24 jam dan hanya sekali pakai.`;
    case 'obat_siap':
      return `Obat dengan kode ${ctx.kode} siap diambil di ${ctx.loket}.`;
    case 'selesai':
      return 'Pengambilan selesai. Riwayat pengobatan dikirim ke rekam medis rumah sakit.';
    default:
      return 'Ada pembaruan pada kunjungan MediFlow Anda.';
  }
}

export function tambahJam(iso: string, jam: number): string {
  return new Date(new Date(iso).getTime() + jam * 60 * 60 * 1000).toISOString();
}

export function pilihanLabel(pilihan: PilihanTebus): string {
  if (pilihan === 'apotek_rs') return 'Apotek RS';
  if (pilihan === 'apotek_luar') return 'Apotek Luar';
  return 'Belum Memilih';
}
