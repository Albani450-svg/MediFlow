import type { JenisPenjamin, Ketersediaan, StatusResep } from './types';

const zona = 'Asia/Jakarta';

export function formatJam(iso: string | null | undefined): string {
  if (!iso) return '-';
  return new Intl.DateTimeFormat('id-ID', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: zona,
  }).format(new Date(iso));
}

export function formatTanggal(isoOrDate: string | null | undefined): string {
  if (!isoOrDate) return '-';
  const date = isoOrDate.length <= 10 ? new Date(`${isoOrDate}T00:00:00+07:00`) : new Date(isoOrDate);
  return new Intl.DateTimeFormat('id-ID', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: zona,
  }).format(date);
}

export function formatRp(nilai: number): string {
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0,
  }).format(nilai);
}

export function labelPenjamin(jenis: JenisPenjamin): string {
  return jenis === 'bpjs' ? 'BPJS Kesehatan' : 'Umum';
}

export function labelKetersediaan(status: Ketersediaan): string {
  if (status === 'tersedia') return 'Tersedia';
  if (status === 'sebagian') return 'Sebagian';
  return 'Kosong';
}

export function labelStatusResep(status: StatusResep): string {
  switch (status) {
    case 'Draft':
      return 'Draft dokter';
    case 'Menunggu Pilihan':
      return 'Menunggu pilihan tebus';
    case 'Verifikasi Kasir':
      return 'Verifikasi kasir / BPJS';
    case 'Antrean Farmasi':
      return 'Antrean farmasi';
    case 'Sedang Diracik':
      return 'Sedang diracik';
    case 'Pengecekan':
      return 'Pengecekan ganda';
    case 'Siap Diambil':
      return 'Siap diambil';
    case 'Selesai Diambil':
      return 'Selesai diambil';
    case 'Tebus Luar':
      return 'Ditebus di apotek luar';
    default:
      return status;
  }
}
