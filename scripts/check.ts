import assert from 'node:assert/strict';
import { decryptNik } from '../src/mediflow/crypto';
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

const pesanStok = teksNotifikasi('info_ketersediaan', { kode: 'A-042' });
assert.equal(/amoxicillin|faringitis/i.test(pesanStok), false);

const nik = decryptNik(mediflow.snapshot().pasien[0].nik_terenkripsi);
assert.equal(nik, '3201011204900001');
assert.equal(mediflow.snapshot().pasien[0].nik_terenkripsi.includes(nik), false);

const salah = await mediflow.login('budi', 'bukan-sandi');
assert.equal(salah.ok, false);
pastikan(await mediflow.login('budi', 'pasien123'));

const awal = mediflow.kunjunganById(1);
if (!awal) throw new Error('Kunjungan demo tidak ada');
const ganda = mediflow.daftar({
  idJadwal: awal.jadwal.id_jadwal,
  jenisPenjamin: 'bpjs',
  keluhan: 'Batuk',
  durasi: '1 hari',
  alergi: 'Tidak ada',
});
assert.equal(ganda.ok, false);

const lain = mediflow.slotHari().find((slot) => slot.jadwal.id_dokter !== awal.idDokter);
if (!lain) throw new Error('Jadwal dokter lain tidak ada');
const idLain = pastikan(
  mediflow.daftar({
    idJadwal: lain.jadwal.id_jadwal,
    jenisPenjamin: 'umum',
    keluhan: 'Demam',
    durasi: '2 hari',
    alergi: 'Tidak ada',
  })
);
assert.notEqual(idLain, 1);

mediflow.logout();
pastikan(await mediflow.login('rina', 'apotek123'));
pastikan(mediflow.sesuaikanStok(1, 0, 'Stok fisik habis'));
mediflow.logout();
pastikan(await mediflow.login('hendra', 'dokter123'));
pastikan(mediflow.majuTahap(1));
pastikan(mediflow.majuTahap(1));
mediflow.logout();
pastikan(await mediflow.login('budi', 'pasien123'));
const tolakRs = mediflow.pilihTebus(1, 'apotek_rs');
assert.equal(tolakRs.ok, false);
pastikan(mediflow.pilihTebus(1, 'apotek_luar'));

mediflow.resetDemo();
mediflow.logout();
pastikan(await mediflow.login('hendra', 'dokter123'));
pastikan(mediflow.majuTahap(1));
pastikan(mediflow.majuTahap(1));
assert.equal(mediflow.kunjunganById(1)?.resep?.waktu_dikirim == null, false);

mediflow.logout();
pastikan(await mediflow.login('budi', 'pasien123'));
pastikan(mediflow.pilihTebus(1, 'apotek_rs'));

mediflow.logout();
pastikan(await mediflow.login('siti', 'admin123'));
pastikan(mediflow.majuTahap(1));

mediflow.logout();
pastikan(await mediflow.login('rina', 'apotek123'));
pastikan(mediflow.majuTahap(1));
pastikan(mediflow.majuTahap(1));
const siap = mediflow.kunjunganById(1);
if (!siap?.resep?.pin_pengambil) throw new Error('PIN keluarga belum terbit');
assert.equal(siap.pendaftaran.tahap_alur, 9);
assert.equal(siap.resep.status_resep, 'Siap Diambil');
assert.equal(siap.resep.id_apoteker, 1);

const pinSalah = siap.resep.pin_pengambil === '000000' ? '000001' : '000000';
const tolakPin = mediflow.serahkan(1, siap.resep.kode_qr, 'keluarga', pinSalah);
assert.equal(tolakPin.ok, false);
pastikan(mediflow.serahkan(1, siap.resep.kode_qr, 'pasien', ''));
const ulang = mediflow.serahkan(1, siap.resep.kode_qr, 'pasien', '');
assert.equal(ulang.ok, false);

const selesai = mediflow.kunjunganById(1);
assert.equal(selesai?.pendaftaran.tahap_alur, 10);
assert.equal(selesai?.resep?.status_resep, 'Selesai Diambil');
assert.equal(selesai?.resep?.qr_dipakai, true);
assert.equal(mediflow.snapshot().pasien[0].terdaftar_satusehat, true);
assert.equal(mediflow.snapshot().obat.find((obat) => obat.id_obat === 1)?.stok, 70);
assert.equal(mediflow.snapshot().log_stok.some((log) => log.jenis === 'keluar'), true);
assert.equal(mediflow.snapshot().log_status_resep.length > 0, true);
assert.equal(mediflow.snapshot().refresh_token.length > 0, true);

const isi = mediflow.snapshot().notifikasi.map((item) => item.isi_pesan).join('\n');
assert.equal(/amoxicillin|paracetamol|faringitis|vitamin c/i.test(isi), false);
assert.equal(isi.includes('A-042'), true);

console.log('alur-ok');
