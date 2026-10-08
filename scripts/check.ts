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
assert.equal(dbAwal.obat.length, 5);
assert.equal(dbAwal.obat.filter((row) => row.is_active).length, 4);
assert.equal(dbAwal.dokter.length, 4);
assert.equal(dbAwal.jadwal_dokter.filter((row) => row.id_dokter === 1 && row.is_active).length, 7);
assert.equal(dbAwal.pasien[0].nik, '3471001234567890');
assert.equal(dbAwal.pasien[0].nama_lengkap, 'Budi Santoso Edit');
assert.equal(dbAwal.users.some((user) => user.role === 'farmasi' && user.username === 'farmasi01'), true);
assert.equal(dbAwal.obat.find((row) => row.kode_kemenkes === 'OBT003')?.stok_rs, 4);
assert.equal(dbAwal.obat.find((row) => row.kode_kemenkes === 'OBT005')?.is_active, false);

const demo = mediflow.kunjungan().find((row) => row.idPasien === 2 && row.tahap === 4)?.pendaftaran.id_pendaftaran;
if (!demo) throw new Error('Kunjungan demo tidak ada');
const siti = mediflow.kunjungan().find((row) => row.pasienNama === 'Siti Rahayu');
assert.equal(siti?.tahap, 9);
assert.equal(siti?.pinKeluarga, '482913');
assert.equal(siti?.resep?.kode_qr_unik, 'MF-SITI-SIAP');
assert.equal(mediflow.kunjungan().some((row) => row.tahap === 7 && row.pasienNama === 'Dewi Lestari'), true);

const salah = await mediflow.login('budi', 'bukan-sandi');
assert.equal(salah.ok, false);
pastikan(await mediflow.login('budi', 'pasien123'));

const awal = mediflow.kunjunganById(demo);
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
  mediflow.simpanPemeriksaan(demo, {
    diagnosis: 'J02.9 - Faringitis Akut (Radang Tenggorokan)',
    tindakan: 'Rawat Jalan + Terapi Obat',
    catatan: 'Faring hiperemis. Habiskan obat sesuai aturan.',
  })
);
pastikan(mediflow.tambahObat(demo, 1, 10, '3x1 Sehari (Sesudah Makan)'));
pastikan(mediflow.majuTahap(demo));
pastikan(mediflow.majuTahap(demo));
mediflow.logout();
pastikan(await mediflow.login('budi', 'pasien123'));
const tolakRs = mediflow.pilihTebus(demo, 'apotek_rs');
assert.equal(tolakRs.ok, false);
pastikan(mediflow.pilihTebus(demo, 'apotek_luar'));
assert.equal(mediflow.kunjunganById(demo)?.resep?.pilihan_penebusan, 'Apotek Luar');

mediflow.resetDemo();
mediflow.logout();
pastikan(await mediflow.login('dokter01', 'dokter123'));
pastikan(
  mediflow.simpanPemeriksaan(demo, {
    diagnosis: 'J02.9 - Faringitis Akut (Radang Tenggorokan)',
    tindakan: 'Rawat Jalan + Terapi Obat',
    catatan: 'Faring hiperemis. Habiskan obat sesuai aturan.',
  })
);
pastikan(mediflow.tambahObat(demo, 1, 10, '3x1 Sehari (Sesudah Makan)'));
pastikan(mediflow.majuTahap(demo));
pastikan(mediflow.majuTahap(demo));
assert.equal(mediflow.kunjunganById(demo)?.pemeriksaan.waktu_selesai == null, false);

mediflow.logout();
pastikan(await mediflow.login('budi', 'pasien123'));
pastikan(mediflow.pilihTebus(demo, 'apotek_rs'));
assert.equal(mediflow.kunjunganById(demo)?.tahap, 6);

mediflow.logout();
pastikan(await mediflow.login('admin', 'admin123'));
pastikan(mediflow.majuTahap(demo));
assert.equal(mediflow.kunjunganById(demo)?.resep?.status_resep, 'Antrean Farmasi');

mediflow.logout();
pastikan(await mediflow.login('farmasi01', 'apotek123'));
pastikan(mediflow.majuTahap(demo));
pastikan(mediflow.majuTahap(demo));
const siap = mediflow.kunjunganById(demo);
if (!siap?.pinKeluarga) throw new Error('PIN keluarga belum terbit');
assert.equal(siap.tahap, 9);
assert.equal(siap.resep?.status_resep, 'Siap Diambil');

const pinSalah = siap.pinKeluarga === '000000' ? '000001' : '000000';
const tolakPin = mediflow.serahkan(demo, siap.resep?.kode_qr_unik ?? '', 'keluarga', pinSalah);
assert.equal(tolakPin.ok, false);
pastikan(mediflow.serahkan(demo, siap.resep?.kode_qr_unik ?? '', 'pasien', ''));
const ulang = mediflow.serahkan(demo, siap.resep?.kode_qr_unik ?? '', 'pasien', '');
assert.equal(ulang.ok, false);

const selesai = mediflow.kunjunganById(demo);
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

mediflow.resetDemo();
mediflow.logout();
const nomorSalah = await mediflow.login('budi', 'pasien123', '12345');
assert.equal(nomorSalah.ok, false);
pastikan(await mediflow.login('budi', 'pasien123', '081298765432'));
assert.equal(mediflow.snapshot().pasien.find((row) => row.id_user === 4)?.no_telepon, '081298765432');
pastikan(mediflow.simpanCatatan(demo, 'Alergi penisilin'));
pastikan(mediflow.simpanKeluhan(demo, 'Batuk kering'));
const catatan = mediflow.kunjunganById(demo);
assert.equal(catatan?.pemeriksaan.keluhan, 'Batuk kering');
assert.equal(catatan?.pendaftaran.catatan_pasien, 'Alergi penisilin');
const obatPasien = mediflow.simpanObat({
  kode: 'OBT-X',
  nama: 'Vitamin Tes',
  stok: 3,
  harga: 1000,
  satuan: 'Tablet',
  jenis: 'Jadi',
  coverBpjs: false,
});
assert.equal(obatPasien.ok, false);

mediflow.logout();
pastikan(await mediflow.login('dokter01', 'dokter123'));
const idHariIni = mediflow.kunjunganById(demo)?.jadwal.id_jadwal ?? 0;
assert.equal(mediflow.nonaktifkanJadwal(idHariIni).ok, false);
const idJadwal = pastikan(mediflow.simpanJadwal({ hari: 'Kamis', mulai: '13:00', selesai: '15:00', kuota: 12 }));
assert.equal(mediflow.snapshot().jadwal_dokter.find((row) => row.id_jadwal === idJadwal)?.kuota_maksimal, 12);
assert.equal(mediflow.simpanJadwal({ hari: 'Senin', mulai: '09:00', selesai: '10:00', kuota: 10 }).ok, false);
pastikan(mediflow.nonaktifkanJadwal(idJadwal));
assert.equal(mediflow.jadwalSaya().some((row) => row.id_jadwal === idJadwal), false);

mediflow.logout();
pastikan(await mediflow.login('admin', 'admin123'));
assert.equal(mediflow.simpanJadwal({ hari: 'Jumat', mulai: '08:00', selesai: '10:00', kuota: 5 }).ok, false);

mediflow.logout();
pastikan(await mediflow.login('farmasi01', 'apotek123'));
const idObat = pastikan(
  mediflow.simpanObat({
    kode: 'OBT-TEST',
    nama: 'Vitamin Tes',
    stok: 5,
    harga: 2000,
    satuan: 'Tablet',
    jenis: 'Jadi',
    coverBpjs: true,
  })
);
assert.equal(
  mediflow.snapshot().mutasi_stok.some((row) => row.id_obat === idObat && row.jenis_mutasi === 'Masuk' && row.stok_sesudah === 5),
  true
);
assert.equal(mediflow.simpanObat({
  kode: 'OBT001',
  nama: 'Kode bentrok',
  stok: 1,
  harga: 1,
  satuan: 'Tablet',
  jenis: 'Jadi',
  coverBpjs: false,
}).ok, false);
pastikan(mediflow.nonaktifkanObat(idObat));
assert.equal(mediflow.obatAktif().some((row) => row.id_obat === idObat), false);
assert.equal(mediflow.snapshot().notifikasi.every((item) => item.channel === 'PWA'), true);
assert.equal(/whatsapp/i.test(mediflow.snapshot().notifikasi.map((item) => item.pesan).join('\n')), false);

mediflow.resetDemo();
mediflow.logout();

console.log('alur-ok');
