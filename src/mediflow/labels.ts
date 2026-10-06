export interface LangkahAlur {
  tahap: number;
  judul: string;
  deskripsi: string;
  pemilik: 'admin' | 'dokter' | 'apoteker' | 'done';
  pemilikNama: string;
}

export const LANGKAH: LangkahAlur[] = [
  {
    tahap: 1,
    judul: '1. Pendaftaran & Form Keluhan',
    deskripsi: 'Pasien mengisi keluhan, poli tujuan, dan alergi obat',
    pemilik: 'admin',
    pemilikNama: 'Admin Pendaftaran',
  },
  {
    tahap: 2,
    judul: '2. Skrining & Tanda Vital',
    deskripsi: 'Perawat menginput tensi darah, suhu tubuh, dan berat badan',
    pemilik: 'admin',
    pemilikNama: 'Perawat / Admin',
  },
  {
    tahap: 3,
    judul: '3. Ruang Tunggu Poliklinik',
    deskripsi: 'Admin poli memanggil pasien masuk ke ruang dokter',
    pemilik: 'admin',
    pemilikNama: 'Admin Poli',
  },
  {
    tahap: 4,
    judul: '4. Pemeriksaan Dokter Poli',
    deskripsi: 'Dokter mengisi ICD-10, anamnesa, tindakan, dan jadwal kontrol',
    pemilik: 'dokter',
    pemilikNama: 'Dokter Poli',
  },
  {
    tahap: 5,
    judul: '5. Input & Kirim E-Resep',
    deskripsi: 'Dokter memvalidasi daftar obat dan mengirim ke farmasi',
    pemilik: 'dokter',
    pemilikNama: 'Dokter Poli',
  },
  {
    tahap: 6,
    judul: '6. Verifikasi Kasir / Klaim BPJS',
    deskripsi: 'Admin memverifikasi penjamin biaya atau pelunasan tagihan',
    pemilik: 'admin',
    pemilikNama: 'Admin Kasir',
  },
  {
    tahap: 7,
    judul: '7. Peracikan & Penyiapan Obat',
    deskripsi: 'Apoteker menyiapkan obat sesuai e-resep dokter',
    pemilik: 'apoteker',
    pemilikNama: 'Apoteker',
  },
  {
    tahap: 8,
    judul: '8. Pengecekan Ganda (QC Obat)',
    deskripsi: 'Validasi kesesuaian dosis, etiket, dan alergi pasien',
    pemilik: 'apoteker',
    pemilikNama: 'Apoteker',
  },
  {
    tahap: 9,
    judul: '9. Panggilan Loket Farmasi',
    deskripsi: 'Penyerahan obat di loket dan edukasi aturan minum',
    pemilik: 'apoteker',
    pemilikNama: 'Apoteker',
  },
  {
    tahap: 10,
    judul: '10. Kunjungan Selesai',
    deskripsi: 'Seluruh rekam medis dan obat telah diterima pasien',
    pemilik: 'done',
    pemilikNama: 'Sistem Selesai',
  },
];

export const ICD = [
  { kode: 'J02.9', nama: 'Faringitis Akut (Radang Tenggorokan)' },
  { kode: 'J06.9', nama: 'Infeksi Saluran Napas Atas (ISPA)' },
  { kode: 'K29.7', nama: 'Gastritis / Dispepsia Akut' },
  { kode: 'I10', nama: 'Hipertensi Esensial Primer' },
  { kode: 'A09', nama: 'Gastroenteritis dan Diare Infeksi' },
];

export const TINDAKAN = [
  'Rawat Jalan + Terapi Obat',
  'Nebulizer + Rawat Jalan',
  'Rujuk Cek Laboratorium',
  'Edukasi tanpa resep',
];

export const ATURAN_PAKAI = [
  '3x1 Sehari (Sesudah Makan)',
  '2x1 Sehari (Pagi & Malam)',
  '1x1 Sehari (Malam Sebelum Tidur)',
  '3x1 Sehari (Sebelum Makan)',
];

export const LOKET = ['Loket Farmasi A (BPJS)', 'Loket Farmasi B (Umum/Eksekutif)'];

export function langkah(tahap: number): LangkahAlur {
  return LANGKAH[Math.min(Math.max(tahap, 1), 10) - 1];
}
