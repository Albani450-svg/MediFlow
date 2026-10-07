import { hashPassword, randomPin, randomToken } from './crypto';
import { notifyBrowser } from './notify';
import { createDatabase } from './seed';
import { formatJam, formatTanggal } from './format';
import {
  bentrokNomorAntrean,
  bentrokPasien,
  bolehTebusRs,
  estimasiFarmasiMenit,
  estimasiJamMasuk,
  hitungTahap,
  judulNotifikasi,
  namaHari,
  qrMasihBerlaku,
  statusKetersediaan,
  tambahJam,
  tanggalIso,
  teksNotifikasi,
} from './rules';
import type {
  Database,
  DetailResep,
  JadwalDokter,
  JenisNotifikasi,
  Ketersediaan,
  LogAktivitas,
  MutasiStok,
  Notifikasi,
  Obat,
  Pemeriksaan,
  PendaftaranPoli,
  Resep,
  Role,
  Session,
  StatusResep,
  User,
} from './types';

const DB_KEY = 'mediflow-db-v2';
const SESSION_KEY = 'mediflow-session-v2';
const BACA_KEY = 'mediflow-baca-v2';

export type Result<T = null> = { ok: true; data: T } | { ok: false; error: string };

export interface DetailTampil extends DetailResep {
  obat: Obat;
  ketersediaan: Ketersediaan;
}

export interface NotifikasiTampil extends Notifikasi {
  dibaca: boolean;
}

export interface Kunjungan {
  pendaftaran: PendaftaranPoli;
  pasienNama: string;
  nomorBpjs: string | null;
  idUserPasien: number;
  idPasien: number;
  poliNama: string;
  dokterNama: string;
  spesialisasi: string;
  idDokter: number;
  idUserDokter: number;
  jadwal: JadwalDokter;
  pemeriksaan: Pemeriksaan;
  resep: Resep | null;
  detail: DetailTampil[];
  bolehRs: boolean;
  tahap: number;
  resepTerkirim: boolean;
  qrDipakai: boolean;
  pinKeluarga: string | null;
  selesai: boolean;
}

export interface SlotJadwal {
  jadwal: JadwalDokter;
  dokterNama: string;
  spesialisasi: string;
  poliNama: string;
  sisa: number;
}

function gagal(error: string): Result<never> {
  return { ok: false, error };
}

function nextId<T>(rows: T[], key: keyof T): number {
  return rows.reduce((max, row) => Math.max(max, Number(row[key]) || 0), 0) + 1;
}

function sekarangIso(): string {
  return new Date().toISOString();
}

function angka(nilai: string): number | null {
  const angka = Number(String(nilai).replace(',', '.'));
  if (!Number.isFinite(angka)) return null;
  return angka;
}

function loadDb(): Database {
  try {
    const raw = localStorage.getItem(DB_KEY);
    if (!raw) return createDatabase();
    const parsed = JSON.parse(raw) as Database;
    if (!Array.isArray(parsed.users) || !Array.isArray(parsed.poliklinik) || !Array.isArray(parsed.mutasi_stok)) {
      return createDatabase();
    }
    return parsed;
  } catch {
    return createDatabase();
  }
}

function bacaIds(): Set<number> {
  try {
    const raw = sessionStorage.getItem(BACA_KEY);
    const ids = raw ? (JSON.parse(raw) as number[]) : [];
    return new Set(Array.isArray(ids) ? ids : []);
  } catch {
    return new Set();
  }
}

function simpanBaca(ids: Set<number>): void {
  sessionStorage.setItem(BACA_KEY, JSON.stringify([...ids]));
}

class MediflowStore {
  private db: Database;
  private listeners = new Set<() => void>();

  constructor() {
    this.db = loadDb();
    this.persist();
  }

  subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private persist(): void {
    localStorage.setItem(DB_KEY, JSON.stringify(this.db));
  }

  private emit(): void {
    this.persist();
    this.listeners.forEach((listener) => listener());
  }

  session(): Session | null {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      if (!raw) return null;
      const session = JSON.parse(raw) as Session;
      const user = this.db.users.find((row) => row.id_user === session.id_user && row.is_active);
      if (!user || user.role !== session.role) return null;
      return session;
    } catch {
      return null;
    }
  }

  private actor(): User | null {
    const session = this.session();
    if (!session) return null;
    return this.db.users.find((user) => user.id_user === session.id_user && user.is_active) ?? null;
  }

  snapshot(): Database {
    return this.db;
  }

  async login(username: string, password: string): Promise<Result<Session>> {
    const user = this.db.users.find(
      (row) => row.username.toLowerCase() === username.trim().toLowerCase() && row.is_active
    );
    if (!user) return gagal('Akun tidak ditemukan atau nonaktif.');
    const hash = await hashPassword(password);
    if (hash !== user.password_hash) return gagal('Kata sandi tidak sesuai.');

    user.updated_at = sekarangIso();
    const session: Session = { id_user: user.id_user, role: user.role };
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.catat(user.id_user, 'LOGIN', 'users', user.id_user, `Masuk sebagai ${user.role}`);
    this.emit();
    return { ok: true, data: session };
  }

  logout(): void {
    const session = this.session();
    if (session) this.catat(session.id_user, 'LOGOUT', 'users', session.id_user, 'Keluar dari sesi');
    sessionStorage.removeItem(SESSION_KEY);
    this.emit();
  }

  resetDemo(): void {
    const actor = this.actor();
    this.db = createDatabase();
    sessionStorage.removeItem(BACA_KEY);
    sessionStorage.removeItem(SESSION_KEY);
    if (actor) {
      const user = this.db.users.find((row) => row.id_user === actor.id_user);
      if (user) {
        const session: Session = { id_user: user.id_user, role: user.role };
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
      }
    }
    this.persist();
    this.listeners.forEach((listener) => listener());
  }

  userAktif(): User | null {
    return this.actor();
  }

  pasienAktif() {
    const actor = this.actor();
    if (!actor) return null;
    return this.db.pasien.find((row) => row.id_user === actor.id_user) ?? null;
  }

  dokterAktif() {
    const actor = this.actor();
    if (!actor) return null;
    return this.db.dokter.find((row) => row.id_user === actor.id_user && row.is_active) ?? null;
  }

  poli() {
    return this.db.poliklinik.filter((row) => row.is_active);
  }

  obatAktif(): Obat[] {
    return this.db.obat.filter((row) => row.is_active);
  }

  kunjungan(): Kunjungan[] {
    return this.db.pendaftaran_poli
      .slice()
      .sort((a, b) => b.id_pendaftaran - a.id_pendaftaran)
      .map((row) => this.susun(row))
      .filter((row): row is Kunjungan => row !== null);
  }

  kunjunganById(id: number): Kunjungan | null {
    const row = this.db.pendaftaran_poli.find((item) => item.id_pendaftaran === id);
    return row ? this.susun(row) : null;
  }

  slotHari(tanggal = tanggalIso(), idPoli?: number): SlotJadwal[] {
    const hari = namaHari(new Date(`${tanggal}T12:00:00+07:00`));
    return this.db.jadwal_dokter
      .filter((row) => row.is_active && row.hari === hari)
      .map((jadwal) => {
        const dokter = this.db.dokter.find((row) => row.id_dokter === jadwal.id_dokter && row.is_active);
        if (!dokter) return null;
        if (idPoli && dokter.id_poli !== idPoli) return null;
        const poli = this.db.poliklinik.find((row) => row.id_poli === dokter.id_poli && row.is_active);
        if (!poli) return null;
        const terpakai = this.db.pendaftaran_poli.filter(
          (row) =>
            row.id_jadwal === jadwal.id_jadwal &&
            row.tanggal_kunjungan === tanggal &&
            row.status_antrean !== 'Batal'
        ).length;
        return {
          jadwal,
          dokterNama: dokter.nama_dokter,
          spesialisasi: dokter.spesialisasi ?? '',
          poliNama: poli.nama_poli,
          sisa: Math.max(0, jadwal.kuota_maksimal - terpakai),
        };
      })
      .filter((row): row is SlotJadwal => row !== null);
  }

  notifikasiUser(idUser?: number): NotifikasiTampil[] {
    const id = idUser ?? this.actor()?.id_user;
    if (!id) return [];
    const pasien = this.db.pasien.find((row) => row.id_user === id);
    if (!pasien) return [];
    const dibaca = bacaIds();
    return this.db.notifikasi
      .filter((row) => row.id_pasien === pasien.id_pasien)
      .slice()
      .sort((a, b) => b.id_notifikasi - a.id_notifikasi)
      .map((row) => ({ ...row, dibaca: dibaca.has(row.id_notifikasi) }));
  }

  jumlahBelumDibaca(idUser?: number): number {
    return this.notifikasiUser(idUser).filter((row) => !row.dibaca).length;
  }

  tandaiDibaca(idNotifikasi?: number): void {
    const pasien = this.pasienAktif();
    if (!pasien) return;
    const ids = bacaIds();
    let berubah = false;
    this.db.notifikasi.forEach((row) => {
      if (row.id_pasien !== pasien.id_pasien || ids.has(row.id_notifikasi)) return;
      if (idNotifikasi !== undefined && row.id_notifikasi !== idNotifikasi) return;
      ids.add(row.id_notifikasi);
      berubah = true;
    });
    if (berubah) simpanBaca(ids);
    if (berubah) this.listeners.forEach((listener) => listener());
  }

  aktivitasTerbaru(batas = 6): LogAktivitas[] {
    return this.db.log_aktivitas.slice().sort((a, b) => b.id_log - a.id_log).slice(0, batas);
  }

  mutasiTerbaru(batas = 6): MutasiStok[] {
    return this.db.mutasi_stok.slice().sort((a, b) => b.id_mutasi - a.id_mutasi).slice(0, batas);
  }

  catatAksesRekam(idPendaftaran: number): void {
    const actor = this.actor();
    if (!actor || actor.role === 'pasien') return;
    const sudah = this.db.log_aktivitas.some(
      (row) =>
        row.id_user === actor.id_user &&
        row.aktivitas === 'LIHAT_REKAM_MEDIS' &&
        row.id_referensi === idPendaftaran &&
        Date.now() - new Date(row.created_at).getTime() < 10 * 60 * 1000
    );
    if (sudah) return;
    this.catat(actor.id_user, 'LIHAT_REKAM_MEDIS', 'pemeriksaan', idPendaftaran, 'Membuka data medis kunjungan');
    this.emit();
  }

  daftar(input: { idJadwal: number; keluhan: string }): Result<number> {
    const actor = this.actor();
    const pasien = this.pasienAktif();
    if (!actor || !pasien || actor.role !== 'pasien') return gagal('Hanya pasien yang dapat mendaftar.');
    const keluhan = input.keluhan.trim();
    if (!keluhan) return gagal('Keluhan utama wajib diisi.');
    const jadwal = this.db.jadwal_dokter.find((row) => row.id_jadwal === input.idJadwal && row.is_active);
    if (!jadwal) return gagal('Jadwal tidak ditemukan.');
    const tanggal = tanggalIso();
    if (jadwal.hari !== namaHari(new Date(`${tanggal}T12:00:00+07:00`))) {
      return gagal('Jadwal itu bukan untuk hari ini.');
    }
    if (bentrokPasien(this.db.pendaftaran_poli, pasien.id_pasien, jadwal.id_jadwal, tanggal)) {
      return gagal('Anda sudah terdaftar pada jadwal dan tanggal yang sama.');
    }
    const terpakai = this.db.pendaftaran_poli.filter(
      (row) => row.id_jadwal === jadwal.id_jadwal && row.tanggal_kunjungan === tanggal && row.status_antrean !== 'Batal'
    );
    if (terpakai.length >= jadwal.kuota_maksimal) return gagal('Kuota jadwal ini sudah penuh.');
    const nomor = this.db.pendaftaran_poli
      .filter((row) => row.id_jadwal === jadwal.id_jadwal && row.tanggal_kunjungan === tanggal)
      .reduce((max, row) => Math.max(max, row.nomor_antrean), 0) + 1;
    if (bentrokNomorAntrean(this.db.pendaftaran_poli, jadwal.id_jadwal, tanggal, nomor)) {
      return gagal('Nomor antrean bentrok. Coba sekali lagi.');
    }
    const dokter = this.db.dokter.find((row) => row.id_dokter === jadwal.id_dokter);
    const poli = dokter ? this.db.poliklinik.find((row) => row.id_poli === dokter.id_poli) : undefined;
    if (!dokter || !poli) return gagal('Dokter atau poliklinik tidak ditemukan.');
    const sekarang = sekarangIso();
    const idPendaftaran = nextId(this.db.pendaftaran_poli, 'id_pendaftaran');
    const estimasi = estimasiJamMasuk(tanggal, jadwal.jam_mulai, nomor, dokter.rata_waktu_periksa_menit);
    this.db.pendaftaran_poli.push({
      id_pendaftaran: idPendaftaran,
      id_pasien: pasien.id_pasien,
      id_jadwal: jadwal.id_jadwal,
      tanggal_kunjungan: tanggal,
      nomor_antrean: nomor,
      waktu_check_in: null,
      estimasi_jam_masuk: estimasi,
      status_antrean: 'Menunggu',
      catatan_pasien: keluhan,
      created_at: sekarang,
      updated_at: sekarang,
    });
    this.db.pemeriksaan.push({
      id_pemeriksaan: nextId(this.db.pemeriksaan, 'id_pemeriksaan'),
      id_pendaftaran: idPendaftaran,
      id_dokter: dokter.id_dokter,
      keluhan,
      tekanan_darah: null,
      suhu_tubuh: null,
      berat_badan: null,
      tinggi_badan: null,
      diagnosis: '',
      tindakan: '',
      catatan_dokter: null,
      waktu_mulai: null,
      waktu_selesai: null,
      created_at: sekarang,
      updated_at: sekarang,
    });
    const ctx = {
      poli: poli.nama_poli,
      tanggal: formatTanggal(tanggal),
      jam: formatJam(estimasi),
      nomor,
    };
    this.kirimPesan(pasien.id_pasien, idPendaftaran, 'Konfirmasi Pendaftaran', ctx);
    this.kirimPesan(pasien.id_pasien, idPendaftaran, 'Pengingat Kunjungan', ctx);
    this.catat(actor.id_user, 'DAFTAR_POLI', 'pendaftaran_poli', idPendaftaran, `Antrean ${nomor} ${poli.nama_poli}`);
    this.emit();
    return { ok: true, data: idPendaftaran };
  }

  simpanKeluhan(idPendaftaran: number, keluhan: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'pasien' || kunjungan.idUserPasien !== actor.id_user) {
      return gagal('Keluhan hanya dapat diubah oleh pasien.');
    }
    if (kunjungan.tahap >= 5 || kunjungan.selesai) return gagal('Pemeriksaan sudah dikunci.');
    if (!keluhan.trim()) return gagal('Keluhan tidak boleh kosong.');
    const sekarang = sekarangIso();
    kunjungan.pemeriksaan.keluhan = keluhan.trim();
    kunjungan.pemeriksaan.updated_at = sekarang;
    kunjungan.pendaftaran.catatan_pasien = keluhan.trim();
    kunjungan.pendaftaran.updated_at = sekarang;
    this.emit();
    return { ok: true, data: null };
  }

  checkIn(idPendaftaran: number): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (kunjungan.idUserPasien !== actor.id_user) return gagal('Check-in hanya untuk pasien pemilik kunjungan.');
    if (kunjungan.pendaftaran.waktu_check_in) return gagal('Check-in sudah tercatat.');
    if (kunjungan.pendaftaran.status_antrean === 'Batal') return gagal('Pendaftaran dibatalkan.');
    const sekarang = sekarangIso();
    kunjungan.pendaftaran.waktu_check_in = sekarang;
    kunjungan.pendaftaran.updated_at = sekarang;
    this.kirimPesan(kunjungan.idPasien, idPendaftaran, 'Pengingat Kunjungan', {
      nomor: kunjungan.pendaftaran.nomor_antrean,
      jam: formatJam(kunjungan.pendaftaran.estimasi_jam_masuk),
      mode: 'checkin',
    });
    this.emit();
    return { ok: true, data: null };
  }

  batalkan(idPendaftaran: number): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    const pemilik = kunjungan.idUserPasien === actor.id_user;
    if (!pemilik && actor.role !== 'admin') return gagal('Anda tidak dapat membatalkan kunjungan ini.');
    if (kunjungan.tahap > 3) return gagal('Kunjungan yang sudah masuk ruang tidak dapat dibatalkan.');
    if (kunjungan.pendaftaran.status_antrean === 'Batal') return gagal('Pendaftaran sudah dibatalkan.');
    const sekarang = sekarangIso();
    kunjungan.pendaftaran.status_antrean = 'Batal';
    kunjungan.pendaftaran.updated_at = sekarang;
    const resep = kunjungan.resep;
    if (resep && resep.status_resep !== 'Selesai Diambil') {
      resep.status_resep = 'Dibatalkan';
      resep.updated_at = sekarang;
    }
    this.catat(actor.id_user, 'BATAL_DAFTAR', 'pendaftaran_poli', idPendaftaran, `Antrean ${kunjungan.pendaftaran.nomor_antrean}`);
    this.emit();
    return { ok: true, data: null };
  }

  simpanVital(idPendaftaran: number, tekanan: string, suhu: string, berat: string, tinggi: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'admin') return gagal('Tanda vital diisi oleh perawat atau admin.');
    const suhuAngka = angka(suhu);
    const beratAngka = angka(berat);
    const tinggiAngka = angka(tinggi);
    if (!tekanan.trim() || suhuAngka === null || beratAngka === null || tinggiAngka === null) {
      return gagal('Tekanan darah, suhu, berat, dan tinggi badan wajib diisi.');
    }
    const pemeriksaan = kunjungan.pemeriksaan;
    pemeriksaan.tekanan_darah = tekanan.trim();
    pemeriksaan.suhu_tubuh = suhuAngka;
    pemeriksaan.berat_badan = beratAngka;
    pemeriksaan.tinggi_badan = tinggiAngka;
    pemeriksaan.updated_at = sekarangIso();
    this.catat(actor.id_user, 'SIMPAN_VITAL', 'pemeriksaan', pemeriksaan.id_pemeriksaan, 'Tanda vital diperbarui');
    this.emit();
    return { ok: true, data: null };
  }

  simpanPemeriksaan(
    idPendaftaran: number,
    input: { diagnosis: string; catatan: string; tindakan: string }
  ): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'dokter' || actor.id_user !== kunjungan.idUserDokter) {
      return gagal('Pemeriksaan ini milik dokter jadwal tersebut.');
    }
    if (kunjungan.tahap < 4) return gagal('Pasien belum masuk ruang periksa.');
    if (!input.diagnosis.trim() || !input.catatan.trim() || !input.tindakan.trim()) {
      return gagal('Diagnosis, tindakan, dan catatan pemeriksaan wajib diisi.');
    }
    const pemeriksaan = kunjungan.pemeriksaan;
    const sekarang = sekarangIso();
    pemeriksaan.diagnosis = input.diagnosis.trim();
    pemeriksaan.catatan_dokter = input.catatan.trim();
    pemeriksaan.tindakan = input.tindakan.trim();
    if (!pemeriksaan.waktu_mulai) pemeriksaan.waktu_mulai = sekarang;
    pemeriksaan.updated_at = sekarang;
    this.catat(actor.id_user, 'SIMPAN_PEMERIKSAAN', 'pemeriksaan', pemeriksaan.id_pemeriksaan, input.diagnosis);
    this.emit();
    return { ok: true, data: null };
  }

  tambahObat(idPendaftaran: number, idObat: number, jumlah: number, aturan: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'dokter' || actor.id_user !== kunjungan.idUserDokter) {
      return gagal('Hanya dokter pemeriksa yang menambah resep.');
    }
    if (kunjungan.resepTerkirim || kunjungan.tahap > 5) return gagal('Resep sudah dikirim dan tidak dapat diubah.');
    const obat = this.db.obat.find((row) => row.id_obat === idObat && row.is_active);
    if (!obat) return gagal('Obat tidak ditemukan.');
    if (!aturan.trim()) return gagal('Aturan pakai wajib diisi.');
    const qty = Math.max(1, Math.floor(jumlah));
    const resep = this.pastikanResep(kunjungan);
    const sekarang = sekarangIso();
    const ada = this.db.detail_resep.find((row) => row.id_resep === resep.id_resep && row.id_obat === idObat);
    if (ada) {
      ada.jumlah += qty;
      ada.dosis_aturan_pakai = aturan.trim();
      ada.updated_at = sekarang;
    } else {
      this.db.detail_resep.push({
        id_detail: nextId(this.db.detail_resep, 'id_detail'),
        id_resep: resep.id_resep,
        id_obat: idObat,
        jumlah: qty,
        dosis_aturan_pakai: aturan.trim(),
        instruksi_racikan: obat.jenis_obat === 'Racikan' ? 'Racik sesuai aturan farmasi' : null,
        catatan: null,
        created_at: sekarang,
        updated_at: sekarang,
      });
    }
    this.emit();
    return { ok: true, data: null };
  }

  hapusObat(idDetail: number): Result<null> {
    const actor = this.actor();
    if (!actor || actor.role !== 'dokter') return gagal('Hanya dokter yang dapat menghapus item resep.');
    const detail = this.db.detail_resep.find((row) => row.id_detail === idDetail);
    if (!detail) return gagal('Item resep tidak ditemukan.');
    const resep = this.db.resep.find((row) => row.id_resep === detail.id_resep);
    if (!resep) return gagal('Resep tidak ditemukan.');
    const kunjungan = this.kunjunganById(resep.id_pendaftaran);
    if (!kunjungan || kunjungan.idUserDokter !== actor.id_user) return gagal('Resep ini bukan milik Anda.');
    if (kunjungan.resepTerkirim) return gagal('Resep sudah dikirim.');
    this.db.detail_resep = this.db.detail_resep.filter((row) => row.id_detail !== idDetail);
    this.emit();
    return { ok: true, data: null };
  }

  pilihTebus(idPendaftaran: number, pilihan: 'apotek_rs' | 'apotek_luar'): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan || !kunjungan.resep) return gagal('Resep belum tersedia.');
    if (kunjungan.idUserPasien !== actor.id_user) return gagal('Pilihan tebus diisi oleh pasien.');
    if (!kunjungan.resepTerkirim) return gagal('Dokter belum mengirim e-resep.');
    if (kunjungan.resep.pilihan_penebusan !== 'Belum Memilih') return gagal('Pilihan tebus sudah dikunci.');
    if (pilihan === 'apotek_rs' && !kunjungan.bolehRs) {
      return gagal('Stok rumah sakit tidak mencukupi untuk seluruh item. Pilih apotek luar atau hubungi farmasi.');
    }
    const resep = kunjungan.resep;
    const sekarang = sekarangIso();
    resep.pilihan_penebusan = pilihan === 'apotek_rs' ? 'Apotek RS' : 'Apotek Luar';
    resep.updated_at = sekarang;
    if (pilihan === 'apotek_rs') {
      const antrian = this.db.resep.filter(
        (row) => row.id_resep !== resep.id_resep && (row.status_resep === 'Antrean Farmasi' || row.status_resep === 'Sedang Diracik')
      ).length;
      const menit = estimasiFarmasiMenit(antrian, kunjungan.detail.map((row) => row.obat.jenis_obat));
      resep.estimasi_jam_selesai = tambahJam(sekarang, menit / 60);
    }
    this.catat(actor.id_user, 'PILIH_TEBUS', 'resep', resep.id_resep, resep.pilihan_penebusan);
    if (pilihan === 'apotek_luar') {
      const pasien = this.db.pasien.find((row) => row.id_pasien === kunjungan.idPasien);
      if (pasien) {
        pasien.terdaftar_satusehat = true;
        pasien.updated_at = sekarang;
      }
      this.kirimPesan(kunjungan.idPasien, idPendaftaran, 'Obat Siap', {
        nomor: kunjungan.pendaftaran.nomor_antrean,
        selesai: true,
      });
    }
    this.emit();
    return { ok: true, data: null };
  }

  majuTahap(idPendaftaran: number): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    const tahap = kunjungan.tahap;
    if (tahap >= 10 || tahap <= 0 || kunjungan.selesai) return gagal('Kunjungan sudah selesai.');
    if (!this.bolehMaju(actor.role, tahap, actor.id_user === kunjungan.idUserDokter)) {
      return gagal('Peran Anda tidak melanjutkan langkah ini.');
    }
    const cek = this.validasiMaju(kunjungan, tahap);
    if (cek) return gagal(cek);
    const error = this.terapkanTahap(kunjungan, tahap + 1, actor.id_user);
    if (error) return gagal(error);
    this.catat(actor.id_user, 'LANJUT_LANGKAH', 'pendaftaran_poli', idPendaftaran, `Ke langkah ${tahap + 1}`);
    this.emit();
    return { ok: true, data: null };
  }

  serahkan(idPendaftaran: number, kode: string, pengambil: 'pasien' | 'keluarga', pin: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan || !kunjungan.resep) return gagal('Resep tidak ditemukan.');
    if (actor.role !== 'farmasi' && actor.role !== 'admin') return gagal('Penyerahan dilakukan farmasi.');
    const resep = kunjungan.resep;
    if (resep.kode_qr_unik.trim().toLowerCase() !== kode.trim().toLowerCase()) return gagal('Kode QR tidak cocok.');
    if (resep.status_resep !== 'Siap Diambil' || kunjungan.tahap < 9) {
      return gagal('Obat belum berstatus siap diambil.');
    }
    if (!qrMasihBerlaku(resep.kadaluarsa_qr, kunjungan.qrDipakai)) {
      return gagal('QR kedaluwarsa atau sudah dipakai. Tolak pengambilan.');
    }
    if (pengambil === 'keluarga') {
      const otp = this.otpResep(kunjungan);
      if (!otp || pin.trim() !== otp.kode_otp) {
        if (otp) otp.attempt += 1;
        this.emit();
        return gagal('PIN keluarga tidak sesuai.');
      }
    }
    const hasil = this.selesaiAmbil(kunjungan, actor.id_user, pengambil);
    if (!hasil.ok) return hasil;
    this.emit();
    return { ok: true, data: null };
  }

  sesuaikanStok(idObat: number, stokBaru: number, keterangan: string): Result<null> {
    const actor = this.actor();
    if (!actor || (actor.role !== 'farmasi' && actor.role !== 'admin')) {
      return gagal('Penyesuaian stok dilakukan farmasi.');
    }
    const obat = this.db.obat.find((row) => row.id_obat === idObat);
    if (!obat) return gagal('Obat tidak ditemukan.');
    const tujuan = Math.max(0, Math.floor(stokBaru));
    const sebelum = obat.stok_rs;
    if (tujuan === sebelum) return gagal('Stok tidak berubah.');
    obat.stok_rs = tujuan;
    obat.updated_at = sekarangIso();
    this.catatMutasi(obat.id_obat, actor.id_user, 'Penyesuaian', Math.abs(tujuan - sebelum), sebelum, tujuan, keterangan.trim() || 'Penyesuaian stok fisik');
    this.catat(actor.id_user, 'SESUAIKAN_STOK', 'obat', obat.id_obat, `${sebelum} -> ${tujuan}`);
    this.emit();
    return { ok: true, data: null };
  }

  private bolehMaju(role: Role, tahap: number, dokterPemilik: boolean): boolean {
    if (role === 'admin') return true;
    if (role === 'dokter' && dokterPemilik && (tahap === 4 || tahap === 5)) return true;
    if (role === 'farmasi' && (tahap === 7 || tahap === 8)) return true;
    return false;
  }

  private validasiMaju(kunjungan: Kunjungan, tahap: number): string | null {
    if (tahap === 1 && !kunjungan.pendaftaran.waktu_check_in) return 'Pasien belum check-in.';
    if (tahap === 1 && !kunjungan.pemeriksaan.keluhan.trim()) return 'Keluhan pasien belum diisi.';
    if (tahap === 2 && !kunjungan.pemeriksaan.tekanan_darah?.trim()) return 'Tanda vital belum diisi.';
    if (tahap === 4 && !kunjungan.pemeriksaan.diagnosis.trim()) return 'Diagnosis belum disimpan.';
    if (tahap === 5 && kunjungan.detail.length === 0) return 'Tambahkan minimal satu obat sebelum mengirim resep.';
    if (tahap === 6 && (!kunjungan.resep || kunjungan.resep.pilihan_penebusan === 'Belum Memilih')) {
      return 'Pasien belum memilih tempat tebus.';
    }
    if (tahap === 6 && kunjungan.resep?.pilihan_penebusan === 'Apotek Luar') {
      return 'Resep dibawa ke apotek luar. Tidak masuk antrean farmasi.';
    }
    return null;
  }

  private terapkanTahap(kunjungan: Kunjungan, tahap: number, actorId: number): string | null {
    const pendaftaran = kunjungan.pendaftaran;
    const sekarang = sekarangIso();
    if (tahap === 4) {
      pendaftaran.status_antrean = 'Masuk Ruangan';
      pendaftaran.updated_at = sekarang;
      if (!kunjungan.pemeriksaan.waktu_mulai) kunjungan.pemeriksaan.waktu_mulai = sekarang;
      kunjungan.pemeriksaan.updated_at = sekarang;
      this.kirimPesan(kunjungan.idPasien, pendaftaran.id_pendaftaran, 'Pengingat Kunjungan', {
        nomor: pendaftaran.nomor_antrean,
        mode: 'panggil',
      });
    }
    if (tahap === 5) {
      pendaftaran.status_antrean = 'Selesai Pemeriksaan';
      pendaftaran.updated_at = sekarang;
    }
    if (tahap === 6) {
      const resep = this.pastikanResep(kunjungan);
      kunjungan.pemeriksaan.waktu_selesai = sekarang;
      kunjungan.pemeriksaan.updated_at = sekarang;
      this.ubahStatusResep(resep, 'Menunggu Pilihan', actorId, 'E-resep dikirim ke pasien');
      this.kirimPesan(kunjungan.idPasien, pendaftaran.id_pendaftaran, 'QR Resep', {
        nomor: pendaftaran.nomor_antrean,
      });
    }
    if (tahap === 7 && kunjungan.resep) {
      this.ubahStatusResep(kunjungan.resep, 'Antrean Farmasi', actorId, 'Masuk antrean farmasi');
    }
    if (tahap === 8 && kunjungan.resep) {
      this.ubahStatusResep(kunjungan.resep, 'Sedang Diracik', actorId, 'Obat disiapkan');
    }
    if (tahap === 9 && kunjungan.resep) {
      const resep = kunjungan.resep;
      resep.kadaluarsa_qr = tambahJam(sekarang, 24);
      resep.updated_at = sekarang;
      this.buatPin(kunjungan, resep.kadaluarsa_qr);
      this.ubahStatusResep(resep, 'Siap Diambil', actorId, 'Siap di farmasi');
      this.kirimPesan(kunjungan.idPasien, pendaftaran.id_pendaftaran, 'Obat Siap', {
        nomor: pendaftaran.nomor_antrean,
      });
    }
    if (tahap === 10 && kunjungan.resep) {
      const hasil = this.selesaiAmbil(kunjungan, actorId, 'pasien');
      if (!hasil.ok) return hasil.error;
    }
    return null;
  }

  private selesaiAmbil(kunjungan: Kunjungan, actorId: number, pengambil: 'pasien' | 'keluarga'): Result<null> {
    const resep = kunjungan.resep;
    if (!resep) return gagal('Resep tidak ditemukan.');
    if (resep.status_resep === 'Selesai Diambil') return { ok: true, data: null };
    const kurang = this.db.detail_resep
      .filter((row) => row.id_resep === resep.id_resep)
      .some((detail) => {
        const obat = this.db.obat.find((row) => row.id_obat === detail.id_obat);
        return !obat || obat.stok_rs < detail.jumlah;
      });
    if (kurang) return gagal('Stok rumah sakit tidak cukup untuk menyerahkan resep ini.');
    const sekarang = sekarangIso();
    const pasien = this.db.pasien.find((row) => row.id_pasien === kunjungan.idPasien);
    this.keluarkanStok(resep, actorId);
    resep.updated_at = sekarang;
    this.ubahStatusResep(resep, 'Selesai Diambil', actorId, `Diserahkan ke ${pengambil}`);
    const otp = this.otpResep(kunjungan);
    if (otp && !otp.verified_at) otp.verified_at = sekarang;
    this.db.pengambilan_obat.push({
      id_pengambilan: nextId(this.db.pengambilan_obat, 'id_pengambilan'),
      id_resep: resep.id_resep,
      id_user_petugas: actorId,
      diambil_oleh: pengambil === 'keluarga' ? 'Keluarga' : 'Pasien',
      nama_pengambil: pengambil === 'keluarga' ? 'Keluarga' : pasien?.nama_lengkap ?? null,
      waktu_pengambilan: sekarang,
      metode_verifikasi: pengambil === 'keluarga' ? 'QR + PIN' : 'QR',
      status: 'Berhasil',
      catatan: null,
      created_at: sekarang,
      updated_at: sekarang,
    });
    if (pasien) {
      pasien.terdaftar_satusehat = true;
      pasien.updated_at = sekarang;
    }
    this.kirimPesan(kunjungan.idPasien, kunjungan.pendaftaran.id_pendaftaran, 'Obat Siap', {
      nomor: kunjungan.pendaftaran.nomor_antrean,
      selesai: true,
    });
    this.catat(actorId, 'SERAH_OBAT', 'pengambilan_obat', resep.id_resep, pengambil);
    return { ok: true, data: null };
  }

  private pastikanResep(kunjungan: Kunjungan): Resep {
    const ada = this.db.resep.find((row) => row.id_pendaftaran === kunjungan.pendaftaran.id_pendaftaran);
    if (ada) return ada;
    const sekarang = sekarangIso();
    const resep: Resep = {
      id_resep: nextId(this.db.resep, 'id_resep'),
      id_pendaftaran: kunjungan.pendaftaran.id_pendaftaran,
      kode_qr_unik: randomToken(12),
      waktu_diterbitkan: sekarang,
      pilihan_penebusan: 'Belum Memilih',
      estimasi_jam_selesai: null,
      status_resep: 'Menunggu Pilihan',
      kadaluarsa_qr: null,
      created_at: sekarang,
      updated_at: sekarang,
    };
    this.db.resep.push(resep);
    return resep;
  }

  private ubahStatusResep(resep: Resep, baru: StatusResep, actorId: number, catatan: string): void {
    const lama = resep.status_resep;
    if (lama === baru) return;
    resep.status_resep = baru;
    resep.updated_at = sekarangIso();
    this.catat(actorId, 'UBAH_STATUS_RESEP', 'resep', resep.id_resep, `${lama} -> ${baru}. ${catatan}`);
  }

  private buatPin(kunjungan: Kunjungan, kedaluwarsa: string): void {
    const pasien = this.db.pasien.find((row) => row.id_pasien === kunjungan.idPasien);
    if (!pasien) return;
    const sudah = this.db.otp_verifikasi.some(
      (row) => row.id_user === pasien.id_user && row.expired_at === kedaluwarsa
    );
    if (sudah) return;
    this.db.otp_verifikasi.push({
      id_otp: nextId(this.db.otp_verifikasi, 'id_otp'),
      id_user: pasien.id_user,
      kode_otp: randomPin(),
      tujuan: pasien.no_telepon,
      expired_at: kedaluwarsa,
      verified_at: null,
      attempt: 0,
      created_at: sekarangIso(),
    });
  }

  private otpResep(kunjungan: Kunjungan) {
    const kedaluwarsa = kunjungan.resep?.kadaluarsa_qr;
    if (!kedaluwarsa) return undefined;
    return this.db.otp_verifikasi.find(
      (row) => row.id_user === kunjungan.idUserPasien && row.expired_at === kedaluwarsa
    );
  }

  private keluarkanStok(resep: Resep, actorId: number): void {
    this.db.detail_resep
      .filter((row) => row.id_resep === resep.id_resep)
      .forEach((detail) => {
        const obat = this.db.obat.find((row) => row.id_obat === detail.id_obat);
        if (!obat) return;
        const sebelum = obat.stok_rs;
        obat.stok_rs = sebelum - detail.jumlah;
        obat.updated_at = sekarangIso();
        this.catatMutasi(obat.id_obat, actorId, 'Keluar', detail.jumlah, sebelum, obat.stok_rs, `Penyerahan resep ${resep.id_resep}`);
      });
  }

  private catatMutasi(
    idObat: number,
    idUser: number,
    jenis: MutasiStok['jenis_mutasi'],
    jumlah: number,
    sebelum: number,
    sesudah: number,
    keterangan: string
  ): void {
    this.db.mutasi_stok.push({
      id_mutasi: nextId(this.db.mutasi_stok, 'id_mutasi'),
      id_obat: idObat,
      id_user: idUser,
      jenis_mutasi: jenis,
      jumlah,
      stok_sebelum: sebelum,
      stok_sesudah: sesudah,
      keterangan,
      created_at: sekarangIso(),
    });
  }

  private kirimPesan(
    idPasien: number,
    idPendaftaran: number | null,
    jenis: JenisNotifikasi,
    ctx: Parameters<typeof teksNotifikasi>[1]
  ): void {
    const pasien = this.db.pasien.find((row) => row.id_pasien === idPasien);
    if (!pasien) return;
    const pesan = teksNotifikasi(jenis, ctx);
    const sekarang = sekarangIso();
    this.db.notifikasi.push({
      id_notifikasi: nextId(this.db.notifikasi, 'id_notifikasi'),
      id_pasien: idPasien,
      id_pendaftaran: idPendaftaran,
      jenis_notifikasi: jenis,
      channel: 'PWA',
      nomor_tujuan: pasien.no_telepon,
      pesan,
      status: 'Terkirim',
      waktu_dikirim: sekarang,
      response_api: null,
      created_at: sekarang,
    });
    if (this.actor()?.id_user === pasien.id_user) void notifyBrowser(judulNotifikasi(jenis, pesan), pesan);
  }

  private susun(pendaftaran: PendaftaranPoli): Kunjungan | null {
    const pasien = this.db.pasien.find((row) => row.id_pasien === pendaftaran.id_pasien);
    const pemeriksaan = this.db.pemeriksaan.find((row) => row.id_pendaftaran === pendaftaran.id_pendaftaran);
    const jadwal = this.db.jadwal_dokter.find((row) => row.id_jadwal === pendaftaran.id_jadwal);
    if (!pasien || !pemeriksaan || !jadwal) return null;
    const dokter = this.db.dokter.find((row) => row.id_dokter === pemeriksaan.id_dokter);
    const poli = dokter ? this.db.poliklinik.find((row) => row.id_poli === dokter.id_poli) : undefined;
    if (!dokter || !poli) return null;
    const resep = this.db.resep.find((row) => row.id_pendaftaran === pendaftaran.id_pendaftaran) ?? null;
    const detail = (resep ? this.db.detail_resep.filter((row) => row.id_resep === resep.id_resep) : []).map((row) => {
      const obat = this.db.obat.find((item) => item.id_obat === row.id_obat)!;
      return { ...row, obat, ketersediaan: statusKetersediaan(obat.stok_rs, row.jumlah) };
    });
    const resepTerkirim = Boolean(resep && pemeriksaan.waktu_selesai);
    const qrDipakai = Boolean(
      resep && this.db.pengambilan_obat.some((row) => row.id_resep === resep.id_resep && row.status === 'Berhasil')
    );
    const tahap = hitungTahap({
      statusAntrean: pendaftaran.status_antrean,
      checkIn: Boolean(pendaftaran.waktu_check_in),
      adaVital: Boolean(pemeriksaan.tekanan_darah?.trim()),
      resep: resep
        ? { status: resep.status_resep, pilihan: resep.pilihan_penebusan, terkirim: resepTerkirim }
        : null,
    });
    const otp = resep?.kadaluarsa_qr
      ? this.db.otp_verifikasi.find((row) => row.id_user === pasien.id_user && row.expired_at === resep.kadaluarsa_qr)
      : undefined;
    return {
      pendaftaran,
      pasienNama: pasien.nama_lengkap,
      nomorBpjs: pasien.nomor_bpjs,
      idUserPasien: pasien.id_user,
      idPasien: pasien.id_pasien,
      poliNama: poli.nama_poli,
      dokterNama: dokter.nama_dokter,
      spesialisasi: dokter.spesialisasi ?? '',
      idDokter: dokter.id_dokter,
      idUserDokter: dokter.id_user,
      jadwal,
      pemeriksaan,
      resep,
      detail,
      bolehRs: bolehTebusRs(detail.map((row) => row.ketersediaan)),
      tahap,
      resepTerkirim,
      qrDipakai,
      pinKeluarga: otp?.kode_otp ?? null,
      selesai: tahap >= 10,
    };
  }

  private catat(
    idUser: number,
    aktivitas: string,
    tabel: string,
    idReferensi: number,
    keterangan: string
  ): void {
    this.db.log_aktivitas.push({
      id_log: nextId(this.db.log_aktivitas, 'id_log'),
      id_user: idUser,
      aktivitas,
      tabel_referensi: tabel,
      id_referensi: idReferensi,
      keterangan,
      ip_address: null,
      created_at: sekarangIso(),
    });
  }
}

export const mediflow = new MediflowStore();
