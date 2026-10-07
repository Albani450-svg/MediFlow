import assert from 'node:assert/strict';
import { panduan } from '../src/mediflow/labels';
import { bolehTebusRs, estimasiFarmasiMenit, estimasiJamMasuk, statusKetersediaan, teksNotifikasi } from '../src/mediflow/rules';
import { mediflow } from '../src/mediflow/store';

function pastikan<T>(hasil: { ok: true; data: T } | { ok: false; error: string }): T {
  if (!hasil.ok) throw new Error(hasil.error);
  return hasil.data;
}

const estimasi = estimasiJamMasuk('2026-10-06', '08:00', 5, 15);
assert.equal(new Date(estimasi).toISOString(), '2026-10-06T02:00:00.000Z');
assert.equal(estimasiFarmasiMenit(2, ['Jadi', 'Racikan']), 40);
assert.equal(statusKetersediaan(0, 10), 'kosong');
assert.equal(statusKetersediaan(4, 10), 'sebagian');
assert.equal(statusKetersediaan(10, 10), 'tersedia');
assert.equal(bolehTebusRs(['tersedia', 'kosong']), false);
assert.equal(bolehTebusRs(['tersedia', 'tersedia']), true);

const pesanStok = teksNotifikasi('QR Resep', { nomor: 1, poli: 'Poli Umum' });
assert.equal(/amoxicillin|faringitis/i.test(pesanStok), false);

const arahPasien = panduan('pasien', {
  tahap: 4,
  checkIn: true,
  adaKeluhan: true,
  adaVital: true,
  adaResepItem: false,
  pilihan: null,
  bolehRs: true,
  batal: false,
  selesai: false,
});
assert.equal(arahPasien.faseId, 2);
assert.equal(arahPasien.status, 'Sedang diperiksa');
assert.equal(/acc|whatsapp/i.test(arahPasien.tindakan), false);
const arahDokter = panduan('dokter', {
  tahap: 5,
  checkIn: true,
  adaKeluhan: true,
  adaVital: true,
  adaResepItem: false,
  pilihan: null,
  bolehRs: true,
  batal: false,
  selesai: false,
});
assert.equal(arahDokter.milikAnda, true);
assert.equal(arahDokter.penghalang, 'Tambahkan minimal satu obat sebelum mengirim resep.');

const dbAwal = mediflow.snapshot();
assert.equal(dbAwal.poliklinik.length, 4);
assert.equal(dbAwal.obat.length, 4);
assert.equal(dbAwal.dokter.length, 1);
assert.equal(dbAwal.pasien[0].nik, '3471001234567890');
assert.equal(dbAwal.pasien[0].nama_lengkap, 'Budi Santoso Edit');
assert.equal(dbAwal.users.some((user) => user.role === 'farmasi' && user.username === 'farmasi01'), true);

const salah = await mediflow.login('budi', 'bukan-sandi');
assert.equal(salah.ok, false);
pastikan(await mediflow.login('budi', 'pasien123'));

const awal = mediflow.kunjunganById(1);
if (!awal) throw new Error('Kunjungan demo tidak ada');
assert.equal(awal.tahap, 4);
assert.equal(awal.poliNama, 'Poli Umum');
const ganda = mediflow.daftar({
  idJadwal: awal.jadwal.id_jadwal,
  keluhan: 'Batuk',
});
assert.equal(ganda.ok, false);

mediflow.logout();
pastikan(await mediflow.login('farmasi01', 'apotek123'));
pastikan(mediflow.sesuaikanStok(1, 0, 'Stok fisik habis'));
mediflow.logout();
pastikan(await mediflow.login('dokter01', 'dokter123'));
pastikan(
  mediflow.simpanPemeriksaan(1, {
    diagnosis: 'J02.9 - Faringitis Akut (Radang Tenggorokan)',
    tindakan: 'Rawat Jalan + Terapi Obat',
    catatan: 'Faring hiperemis. Habiskan obat sesuai aturan.',
  })
);
pastikan(mediflow.tambahObat(1, 1, 10, '3x1 Sehari (Sesudah Makan)'));
pastikan(mediflow.majuTahap(1));
pastikan(mediflow.majuTahap(1));
mediflow.logout();
pastikan(await mediflow.login('budi', 'pasien123'));
const tolakRs = mediflow.pilihTebus(1, 'apotek_rs');
assert.equal(tolakRs.ok, false);
pastikan(mediflow.pilihTebus(1, 'apotek_luar'));
assert.equal(mediflow.kunjunganById(1)?.resep?.pilihan_penebusan, 'Apotek Luar');

mediflow.resetDemo();
mediflow.logout();
pastikan(await mediflow.login('dokter01', 'dokter123'));
pastikan(
  mediflow.simpanPemeriksaan(1, {
    diagnosis: 'J02.9 - Faringitis Akut (Radang Tenggorokan)',
    tindakan: 'Rawat Jalan + Terapi Obat',
    catatan: 'Faring hiperemis. Habiskan obat sesuai aturan.',
  })
);
pastikan(mediflow.tambahObat(1, 1, 10, '3x1 Sehari (Sesudah Makan)'));
pastikan(mediflow.majuTahap(1));
pastikan(mediflow.majuTahap(1));
assert.equal(mediflow.kunjunganById(1)?.pemeriksaan.waktu_selesai == null, false);

mediflow.logout();
pastikan(await mediflow.login('budi', 'pasien123'));
pastikan(mediflow.pilihTebus(1, 'apotek_rs'));
assert.equal(mediflow.kunjunganById(1)?.tahap, 6);

mediflow.logout();
pastikan(await mediflow.login('admin', 'admin123'));
pastikan(mediflow.majuTahap(1));
assert.equal(mediflow.kunjunganById(1)?.resep?.status_resep, 'Antrean Farmasi');

mediflow.logout();
pastikan(await mediflow.login('farmasi01', 'apotek123'));
pastikan(mediflow.majuTahap(1));
pastikan(mediflow.majuTahap(1));
const siap = mediflow.kunjunganById(1);
if (!siap?.pinKeluarga) throw new Error('PIN keluarga belum terbit');
assert.equal(siap.tahap, 9);
assert.equal(siap.resep?.status_resep, 'Siap Diambil');

const pinSalah = siap.pinKeluarga === '000000' ? '000001' : '000000';
const tolakPin = mediflow.serahkan(1, siap.resep?.kode_qr_unik ?? '', 'keluarga', pinSalah);
assert.equal(tolakPin.ok, false);
pastikan(mediflow.serahkan(1, siap.resep?.kode_qr_unik ?? '', 'pasien', ''));
const ulang = mediflow.serahkan(1, siap.resep?.kode_qr_unik ?? '', 'pasien', '');
assert.equal(ulang.ok, false);

const selesai = mediflow.kunjunganById(1);
assert.equal(selesai?.tahap, 10);
assert.equal(selesai?.resep?.status_resep, 'Selesai Diambil');
assert.equal(selesai?.qrDipakai, true);
assert.equal(mediflow.snapshot().pasien[0].terdaftar_satusehat, true);
assert.equal(mediflow.snapshot().obat.find((obat) => obat.id_obat === 1)?.stok_rs, 90);
assert.equal(mediflow.snapshot().mutasi_stok.some((log) => log.jenis_mutasi === 'Keluar'), true);
assert.equal(mediflow.snapshot().pengambilan_obat.some((row) => row.status === 'Berhasil'), true);
assert.equal(mediflow.snapshot().log_aktivitas.length > 0, true);

const isi = mediflow.snapshot().notifikasi.map((item) => item.pesan).join('\n');
assert.equal(/amoxicillin|paracetamol|faringitis|vitamin c/i.test(isi), false);
assert.equal(/whatsapp/i.test(isi), false);
assert.equal(isi.includes('Poli Umum'), true);
assert.equal(
  mediflow.snapshot().notifikasi.every((item) => item.channel === 'PWA'),
  true
);

console.log('alur-ok');
