import type { Ketersediaan, StatusResep } from './types';

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

export function labelPenjamin(nomorBpjs: string | null): string {
  return nomorBpjs ? 'BPJS Kesehatan' : 'Umum';
}

export function labelKetersediaan(status: Ketersediaan): string {
  if (status === 'tersedia') return 'Tersedia';
  if (status === 'sebagian') return 'Sebagian';
  return 'Kosong';
}

export function labelStatusResep(status: StatusResep): string {
  switch (status) {
    case 'Menunggu Pilihan':
      return 'Menunggu pilihan tebus';
    case 'Antrean Farmasi':
      return 'Antrean farmasi';
    case 'Sedang Diracik':
      return 'Sedang diracik';
    case 'Siap Diambil':
      return 'Siap diambil';
    case 'Selesai Diambil':
      return 'Selesai diambil';
    case 'Dibatalkan':
      return 'Dibatalkan';
    default:
      return status;
  }
}
