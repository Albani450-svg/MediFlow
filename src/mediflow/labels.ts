import type { Role } from './types';

export interface LangkahAlur {
  tahap: number;
  /** Nama singkat untuk spanduk dan header. */
  singkat: string;
  judul: string;
  deskripsi: string;
  /** Apa yang pasien lakukan atau tunggu pada langkah ini. */
  pasien: string;
  /** Apa yang petugas pemilik langkah kerjakan. */
  tugas: string;
  /** Label tombol untuk memajukan langkah. */
  tombol: string;
  pemilik: 'admin' | 'dokter' | 'farmasi' | 'done';
  pemilikNama: string;
}

export const FASE = [
  { id: 1, nama: 'Daftar', dari: 1, sampai: 3 },
  { id: 2, nama: 'Periksa', dari: 4, sampai: 5 },
  { id: 3, nama: 'Resep', dari: 6, sampai: 8 },
  { id: 4, nama: 'Ambil', dari: 9, sampai: 10 },
] as const;

export const LANGKAH: LangkahAlur[] = [
  {
    tahap: 1,
    singkat: 'Check-in kedatangan',
    judul: '1. Daftar dan check-in',
    deskripsi: 'Pasien memilih poli, menulis keluhan, lalu menekan check-in saat sampai.',
    pasien: 'Tekan Check-in kedatangan saat Anda sudah di rumah sakit.',
    tugas: 'Tunggu pasien check-in. Setelah itu, catat tanda vital.',
    tombol: 'Menunggu check-in pasien',
    pemilik: 'admin',
    pemilikNama: 'Admin pendaftaran',
  },
  {
    tahap: 2,
    singkat: 'Cek tanda vital',
    judul: '2. Cek tanda vital',
    deskripsi: 'Perawat mencatat tekanan darah, suhu, dan berat badan.',
    pasien: 'Tidak ada yang perlu diisi. Tunggu perawat selesai mencatat.',
    tugas: 'Isi tekanan darah, suhu, dan berat, simpan, lalu masukkan pasien ke ruang tunggu.',
    tombol: 'Tanda vital tersimpan, masuk ruang tunggu',
    pemilik: 'admin',
    pemilikNama: 'Perawat',
  },
  {
    tahap: 3,
    singkat: 'Menunggu dipanggil',
    judul: '3. Ruang tunggu poli',
    deskripsi: 'Pasien menunggu sampai dipanggil masuk ruang dokter.',
    pasien: 'Tunggu di poli. Notifikasi aplikasi memberitahu saat Anda dipanggil.',
    tugas: 'Panggil pasien masuk ruang dokter.',
    tombol: 'Panggil pasien masuk ruang dokter',
    pemilik: 'admin',
    pemilikNama: 'Admin poli',
  },
  {
    tahap: 4,
    singkat: 'Sedang diperiksa',
    judul: '4. Pemeriksaan dokter',
    deskripsi: 'Dokter menulis diagnosis dan tindakan.',
    pasien: 'Hasil pemeriksaan muncul di kartu di bawah. Anda tidak perlu menekan apa pun.',
    tugas: 'Pilih diagnosis, isi catatan, lalu selesaikan pemeriksaan.',
    tombol: 'Simpan diagnosis dan selesai periksa',
    pemilik: 'dokter',
    pemilikNama: 'Dokter',
  },
  {
    tahap: 5,
    singkat: 'Dokter menulis resep',
    judul: '5. Dokter mengirim resep',
    deskripsi: 'Dokter menambahkan obat dan mengirim resep ke farmasi.',
    pasien: 'Tunggu resep terkirim, lalu buka tab E-Resep.',
    tugas: 'Tambahkan obat, lalu kirim resep ke farmasi. Pasien hanya mendapat pemberitahuan, tanpa nama obat.',
    tombol: 'Kirim resep ke farmasi',
    pemilik: 'dokter',
    pemilikNama: 'Dokter',
  },
  {
    tahap: 6,
    singkat: 'Pilih tempat tebus',
    judul: '6. Pilih apotek',
    deskripsi: 'Pasien memilih apotek rumah sakit atau membawa resep ke apotek luar.',
    pasien: 'Pilih tebus di apotek rumah sakit atau bawa resep ke apotek luar.',
    tugas: 'Setelah pasien memilih apotek rumah sakit, teruskan resep ke antrean farmasi.',
    tombol: 'Teruskan ke antrean farmasi',
    pemilik: 'admin',
    pemilikNama: 'Admin kasir',
  },
  {
    tahap: 7,
    singkat: 'Obat disiapkan',
    judul: '7. Obat disiapkan',
    deskripsi: 'Apoteker menyiapkan obat sesuai resep dokter.',
    pasien: 'Obat sedang disiapkan. Notifikasi masuk saat obat siap diambil.',
    tugas: 'Siapkan obat sesuai daftar, lalu lanjutkan ke pengecekan ulang.',
    tombol: 'Obat selesai disiapkan',
    pemilik: 'farmasi',
    pemilikNama: 'Farmasi',
  },
  {
    tahap: 8,
    singkat: 'Obat dicek ulang',
    judul: '8. Pengecekan ulang obat',
    deskripsi: 'Farmasi mencocokkan dosis dan etiket sebelum obat dipanggil.',
    pasien: 'Obat sedang dicek ulang. Tetap tunggu notifikasi di aplikasi.',
    tugas: 'Cocokkan dosis dan etiket, lalu panggil pasien ke farmasi.',
    tombol: 'Cek ulang selesai, panggil pasien',
    pemilik: 'farmasi',
    pemilikNama: 'Farmasi',
  },
  {
    tahap: 9,
    singkat: 'Obat siap diambil',
    judul: '9. Ambil obat di farmasi',
    deskripsi: 'Pasien menunjukkan kode di aplikasi. Farmasi menyerahkan obat.',
    pasien: 'Buka tab E-Resep dan tunjukkan kode di farmasi. Keluarga yang mengambil perlu PIN.',
    tugas: 'Ketik kode dari aplikasi pasien, lalu serahkan obat.',
    tombol: 'Serahkan dengan kode pasien',
    pemilik: 'farmasi',
    pemilikNama: 'Farmasi',
  },
  {
    tahap: 10,
    singkat: 'Kunjungan selesai',
    judul: '10. Kunjungan selesai',
    deskripsi: 'Rekam medis tersimpan. Obat sudah diterima, atau resep dibawa ke apotek luar.',
    pasien: 'Kunjungan ini selesai. Daftar lagi di bawah jika Anda perlu poli lain.',
    tugas: 'Tidak ada tindakan. Kunjungan sudah ditutup.',
    tombol: 'Kunjungan selesai',
    pemilik: 'done',
    pemilikNama: 'Selesai',
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

export function langkah(tahap: number): LangkahAlur {
  return LANGKAH[Math.min(Math.max(tahap, 1), 10) - 1];
}

export function faseDari(tahap: number): (typeof FASE)[number] {
  return FASE.find((fase) => tahap >= fase.dari && tahap <= fase.sampai) ?? FASE[0];
}

export interface KonteksPanduan {
  tahap: number;
  checkIn: boolean;
  adaKeluhan: boolean;
  adaVital: boolean;
  adaResepItem: boolean;
  pilihan: 'belum' | 'rs' | 'luar' | null;
  bolehRs: boolean;
  batal: boolean;
  selesai: boolean;
}

export interface Panduan {
  faseId: number;
  status: string;
  tindakan: string;
  giliran: string;
  tombol: string;
  /** Peran ini yang seharusnya menekan tombol lanjut. */
  milikAnda: boolean;
  penghalang: string | null;
}

const BELUM: Record<Role, string> = {
  pasien: 'Belum ada kunjungan. Isi form daftar di bawah untuk mendapat nomor antrean.',
  dokter: 'Belum ada pasien di jadwal Anda hari ini.',
  admin: 'Belum ada kunjungan hari ini.',
  farmasi: 'Belum ada resep. Resep muncul di sini setelah pasien memilih apotek rumah sakit.',
};

export function panduan(role: Role, ctx: KonteksPanduan): Panduan {
  if (ctx.batal) {
    return {
      faseId: 1,
      status: 'Dibatalkan',
      tindakan: 'Kunjungan ini dibatalkan. Daftar lagi jika masih ingin berobat.',
      giliran: 'Selesai',
      tombol: 'Kunjungan dibatalkan',
      milikAnda: false,
      penghalang: null,
    };
  }
  if (ctx.tahap <= 0) {
    return {
      faseId: 0,
      status: 'Belum mulai',
      tindakan: BELUM[role],
      giliran: '—',
      tombol: '',
      milikAnda: false,
      penghalang: null,
    };
  }

  const item = langkah(ctx.tahap);
  const fase = faseDari(ctx.tahap);
  let penghalang: string | null = null;
  if (!ctx.selesai && ctx.tahap === 1 && !ctx.checkIn) {
    penghalang = 'Pasien belum menekan check-in kedatangan.';
  } else if (!ctx.selesai && ctx.tahap === 1 && !ctx.adaKeluhan) {
    penghalang = 'Keluhan belum diisi. Simpan keluhan sebelum lanjut.';
  } else if (!ctx.selesai && ctx.tahap === 2 && !ctx.adaVital) {
    penghalang = 'Tanda vital belum disimpan. Isi tekanan darah, suhu, dan berat, lalu simpan.';
  } else if (!ctx.selesai && ctx.tahap === 5 && !ctx.adaResepItem) {
    penghalang = 'Tambahkan minimal satu obat sebelum mengirim resep.';
  } else if (!ctx.selesai && ctx.tahap === 6 && (ctx.pilihan === 'belum' || ctx.pilihan === null)) {
    penghalang = ctx.bolehRs
      ? 'Pasien belum memilih apotek rumah sakit atau apotek luar.'
      : 'Stok rumah sakit kurang. Pasien perlu memilih apotek luar.';
  }

  let tindakan = role === 'pasien' ? item.pasien : item.tugas;
  let tombol = item.tombol;
  let milikAnda = item.pemilik === role;

  if (role === 'pasien') {
    if (ctx.selesai) {
      tindakan = 'Kunjungan selesai. Daftar poli lain di bawah jika Anda perlu berobat lagi.';
    } else if (ctx.tahap <= 3 && !ctx.checkIn) {
      tindakan = 'Tekan Check-in kedatangan saat Anda sudah di rumah sakit.';
    } else if (ctx.tahap === 6 && (ctx.pilihan === 'belum' || ctx.pilihan === null)) {
      tindakan = ctx.bolehRs
        ? 'Giliran Anda: pilih apotek rumah sakit atau apotek luar pada kartu Pilihan apotek.'
        : 'Stok rumah sakit tidak cukup. Pilih apotek luar pada kartu Pilihan apotek.';
    } else if (ctx.tahap === 6 && ctx.pilihan === 'luar') {
      tindakan = 'Anda memilih apotek luar. Buka tab E-Resep dan bawa kodenya ke apotek tersebut.';
    } else if (ctx.tahap === 6 && ctx.pilihan === 'rs') {
      tindakan = 'Apotek rumah sakit sudah dipilih. Tunggu petugas meneruskan resep ke farmasi.';
    }
  } else if (ctx.selesai) {
    tindakan = 'Kunjungan ini sudah selesai. Tidak ada langkah yang perlu dilanjutkan.';
    tombol = 'Kunjungan selesai';
  } else if (role === 'admin' && ctx.tahap === 1 && !ctx.checkIn) {
    tindakan = 'Tunggu pasien menekan Check-in kedatangan. Keluhan sudah tersimpan saat daftar.';
    tombol = 'Menunggu check-in pasien';
    milikAnda = false;
  } else if (role === 'dokter' && !milikAnda) {
    tindakan = `Bukan giliran Anda. ${item.pemilikNama} mengerjakan langkah ini: ${item.singkat}.`;
    tombol = 'Simpan catatan saja';
  } else if (role === 'farmasi' && ctx.tahap < 7) {
    tindakan = 'Resep belum masuk antrean racik. Tunggu pasien memilih apotek rumah sakit.';
    tombol = 'Menunggu pilihan pasien';
  } else if (role === 'farmasi' && ctx.tahap === 9) {
    tindakan = 'Pasien menunjukkan kode di aplikasi. Ketik kode itu pada kartu Serah terima, lalu serahkan obat.';
  } else if (role === 'farmasi' && !milikAnda) {
    tindakan = `Bukan giliran Anda. ${item.pemilikNama} mengerjakan: ${item.singkat}.`;
  } else if (role === 'admin' && ctx.tahap === 6 && ctx.pilihan === 'luar') {
    tindakan = 'Pasien membawa resep ke apotek luar. Kunjungan ini selesai. Stok rumah sakit tidak berkurang.';
    tombol = 'Kunjungan selesai';
    milikAnda = false;
  } else if (role === 'admin' && !milikAnda) {
    tindakan = `Giliran ${item.pemilikNama}. Tombol di bawah menjalankan simulasi tanpa perlu berganti akun.`;
  }

  return {
    faseId: fase.id,
    status: ctx.selesai ? 'Kunjungan selesai' : item.singkat,
    tindakan,
    giliran: item.pemilikNama,
    tombol,
    milikAnda: ctx.selesai ? false : milikAnda,
    penghalang: ctx.selesai ? null : penghalang,
  };
}
