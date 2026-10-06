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
  namaHari,
  qrMasihBerlaku,
  statusKetersediaan,
  tanggalIso,
  teksNotifikasi,
  tambahJam,
} from './rules';
import type {
  AuditLog,
  Database,
  DetailResep,
  JadwalDokter,
  JenisNotifikasi,
  JenisPenjamin,
  Ketersediaan,
  LogStok,
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

const DB_KEY = 'mediflow-db-v1';
const SESSION_KEY = 'mediflow-session-v1';

export type Result<T = null> = { ok: true; data: T } | { ok: false; error: string };

export interface DetailTampil extends DetailResep {
  obat: Obat;
  ketersediaan: Ketersediaan;
}

export interface Kunjungan {
  pendaftaran: PendaftaranPoli;
  pasienNama: string;
  noRekamMedis: string;
  idUserPasien: number;
  poliNama: string;
  poliKode: string;
  dokterNama: string;
  spesialisasi: string;
  idDokter: number;
  idUserDokter: number;
  jadwal: JadwalDokter;
  pemeriksaan: Pemeriksaan;
  resep: Resep | null;
  detail: DetailTampil[];
  bolehRs: boolean;
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

function loadDb(): Database {
  try {
    const raw = localStorage.getItem(DB_KEY);
    if (!raw) return createDatabase();
    const parsed = JSON.parse(raw) as Database;
    if (!Array.isArray(parsed.users) || !Array.isArray(parsed.poli)) return createDatabase();
    return parsed;
  } catch {
    return createDatabase();
  }
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
      const token = this.db.refresh_token.find((row) => row.id_token === session.refresh_id);
      if (!token || token.dicabut_pada || new Date(token.kedaluwarsa).getTime() < Date.now()) return null;
      return session;
    } catch {
      return null;
    }
  }

  private actor(): User | null {
    const session = this.session();
    if (!session) return null;
    return this.db.users.find((user) => user.id_user === session.id_user && user.is_aktif) ?? null;
  }

  snapshot(): Database {
    return this.db;
  }

  async login(username: string, password: string): Promise<Result<Session>> {
    const user = this.db.users.find(
      (row) => row.username.toLowerCase() === username.trim().toLowerCase() && row.is_aktif
    );
    if (!user) return gagal('Akun tidak ditemukan atau nonaktif.');
    const hash = await hashPassword(password);
    if (hash !== user.password_hash) return gagal('Kata sandi tidak sesuai.');

    const sekarang = new Date().toISOString();
    user.last_login = sekarang;
    user.updated_at = sekarang;
    const secret = randomToken(24);
    const tokenHash = await hashPassword(secret);
    const refresh = {
      id_token: nextId(this.db.refresh_token, 'id_token'),
      id_user: user.id_user,
      token_hash: tokenHash,
      kedaluwarsa: tambahJam(sekarang, 24 * 7),
      dicabut_pada: null,
      dibuat_pada: sekarang,
    };
    this.db.refresh_token.push(refresh);
    this.audit(user.id_user, 'LOGIN', 'users', user.id_user, `Masuk sebagai ${user.role}`);
    const session: Session = {
      id_user: user.id_user,
      role: user.role,
      refresh_id: refresh.id_token,
      access_token: randomToken(16),
    };
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.emit();
    return { ok: true, data: session };
  }

  logout(): void {
    const session = this.session();
    if (session) {
      const token = this.db.refresh_token.find((row) => row.id_token === session.refresh_id);
      if (token) token.dicabut_pada = new Date().toISOString();
      this.audit(session.id_user, 'LOGOUT', 'users', session.id_user, 'Keluar dari sesi');
    }
    sessionStorage.removeItem(SESSION_KEY);
    this.emit();
  }

  resetDemo(): void {
    const actor = this.actor();
    this.db = createDatabase();
    this.persist();
    sessionStorage.removeItem(SESSION_KEY);
    if (actor) {
      const user = this.db.users.find((row) => row.id_user === actor.id_user);
      if (user) {
        const sekarang = new Date().toISOString();
        const secret = randomToken(24);
        const refresh = {
          id_token: 1,
          id_user: user.id_user,
          token_hash: secret,
          kedaluwarsa: tambahJam(sekarang, 24 * 7),
          dicabut_pada: null,
          dibuat_pada: sekarang,
        };
        this.db.refresh_token.push(refresh);
        const session: Session = {
          id_user: user.id_user,
          role: user.role,
          refresh_id: refresh.id_token,
          access_token: randomToken(16),
        };
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
        this.persist();
      }
    }
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
    return this.db.dokter.find((row) => row.id_user === actor.id_user) ?? null;
  }

  apotekerAktif() {
    const actor = this.actor();
    if (!actor) return null;
    return this.db.apoteker.find((row) => row.id_user === actor.id_user) ?? null;
  }

  poli() {
    return this.db.poli;
  }

  obatAktif(): Obat[] {
    return this.db.obat.filter((row) => row.is_aktif);
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
      .filter((row) => row.hari === hari && (idPoli ? row.id_poli === idPoli : true))
      .map((jadwal) => {
        const dokter = this.db.dokter.find((row) => row.id_dokter === jadwal.id_dokter)!;
        const user = this.db.users.find((row) => row.id_user === dokter.id_user)!;
        const poli = this.db.poli.find((row) => row.id_poli === jadwal.id_poli)!;
        const terpakai = this.db.pendaftaran_poli.filter(
          (row) =>
            row.id_jadwal === jadwal.id_jadwal &&
            row.tanggal_kunjungan === tanggal &&
            row.status_antrean !== 'Batal'
        ).length;
        return {
          jadwal,
          dokterNama: user.nama_lengkap,
          spesialisasi: dokter.spesialisasi,
          poliNama: poli.nama_poli,
          sisa: Math.max(0, jadwal.kuota - terpakai),
        };
      });
  }

  notifikasiUser(idUser?: number): Notifikasi[] {
    const id = idUser ?? this.actor()?.id_user;
    if (!id) return [];
    return this.db.notifikasi
      .filter((row) => row.id_user === id)
      .slice()
      .sort((a, b) => b.id_notifikasi - a.id_notifikasi);
  }

  auditTerbaru(batas = 6): AuditLog[] {
    return this.db.audit_log.slice().sort((a, b) => b.id_audit - a.id_audit).slice(0, batas);
  }

  logStokTerbaru(batas = 6): LogStok[] {
    return this.db.log_stok.slice().sort((a, b) => b.id_log - a.id_log).slice(0, batas);
  }

  catatAksesRekam(idPendaftaran: number): void {
    const actor = this.actor();
    if (!actor || actor.role === 'pasien') return;
    const sudah = this.db.audit_log.some(
      (row) =>
        row.id_user === actor.id_user &&
        row.aksi === 'LIHAT_REKAM_MEDIS' &&
        row.id_entitas === idPendaftaran &&
        Date.now() - new Date(row.waktu).getTime() < 10 * 60 * 1000
    );
    if (sudah) return;
    this.audit(actor.id_user, 'LIHAT_REKAM_MEDIS', 'pemeriksaan', idPendaftaran, 'Membuka data medis kunjungan');
    this.emit();
  }

  async aktifkanPush(): Promise<Result<null>> {
    const actor = this.actor();
    if (!actor) return gagal('Silakan masuk kembali.');
    const ada = this.db.push_subscription.some((row) => row.id_user === actor.id_user);
    if (!ada) {
      this.db.push_subscription.push({
        id_subscription: nextId(this.db.push_subscription, 'id_subscription'),
        id_user: actor.id_user,
        endpoint: `local://perangkat-${actor.id_user}-${randomToken(4)}`,
        p256dh: 'demo-p256dh',
        auth: 'demo-auth',
        dibuat_pada: new Date().toISOString(),
      });
      this.emit();
    }
    return { ok: true, data: null };
  }

  punyaPush(): boolean {
    const actor = this.actor();
    if (!actor) return false;
    return this.db.push_subscription.some((row) => row.id_user === actor.id_user);
  }

  daftar(input: {
    idJadwal: number;
    jenisPenjamin: JenisPenjamin;
    keluhan: string;
    durasi: string;
    alergi: string;
  }): Result<number> {
    const actor = this.actor();
    const pasien = this.pasienAktif();
    if (!actor || !pasien || actor.role !== 'pasien') return gagal('Hanya pasien yang dapat mendaftar.');
    const keluhan = input.keluhan.trim();
    if (!keluhan) return gagal('Keluhan utama wajib diisi.');
    const jadwal = this.db.jadwal_dokter.find((row) => row.id_jadwal === input.idJadwal);
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
    if (terpakai.length >= jadwal.kuota) return gagal('Kuota jadwal ini sudah penuh.');
    const nomor = terpakai.reduce((max, row) => Math.max(max, row.nomor_antrean), 0) + 1;
    if (bentrokNomorAntrean(this.db.pendaftaran_poli, jadwal.id_jadwal, tanggal, nomor)) {
      return gagal('Nomor antrean bentrok. Coba sekali lagi.');
    }
    const dokter = this.db.dokter.find((row) => row.id_dokter === jadwal.id_dokter)!;
    const poli = this.db.poli.find((row) => row.id_poli === jadwal.id_poli)!;
    const sekarang = new Date().toISOString();
    const idPendaftaran = nextId(this.db.pendaftaran_poli, 'id_pendaftaran');
    const idPemeriksaan = nextId(this.db.pemeriksaan, 'id_pemeriksaan');
    const kode = this.kodeBooking(poli.kode);
    const estimasi = estimasiJamMasuk(tanggal, jadwal.jam_mulai, nomor, dokter.rata_waktu_periksa_menit);
    this.db.pendaftaran_poli.push({
      id_pendaftaran: idPendaftaran,
      id_pasien: pasien.id_pasien,
      id_jadwal: jadwal.id_jadwal,
      tanggal_kunjungan: tanggal,
      nomor_antrean: nomor,
      kode_booking: kode,
      jenis_penjamin: input.jenisPenjamin,
      status_antrean: 'Terdaftar',
      tahap_alur: 1,
      waktu_check_in: null,
      waktu_dibatalkan: null,
      estimasi_jam_masuk: estimasi,
      loket: input.jenisPenjamin === 'bpjs' ? 'Loket Farmasi A (BPJS)' : 'Loket Farmasi B (Umum/Eksekutif)',
      created_at: sekarang,
    });
    this.db.pemeriksaan.push({
      id_pemeriksaan: idPemeriksaan,
      id_pendaftaran: idPendaftaran,
      id_dokter: dokter.id_dokter,
      keluhan,
      durasi_keluhan: input.durasi.trim() || '-',
      alergi: input.alergi.trim() || 'Tidak Ada',
      diagnosis: '',
      tindakan: '',
      catatan: '',
      kode_icd10: '',
      tensi: '',
      suhu: '',
      berat_badan: '',
      jadwal_kontrol: '',
      waktu_mulai: null,
      waktu_selesai: null,
    });
    const ctx = {
      kode,
      poli: poli.nama_poli,
      tanggal: formatTanggal(tanggal),
      jam: formatJam(estimasi),
      nomor,
    };
    this.kirimPesan(actor.id_user, 'konfirmasi_booking', ctx, `pendaftaran:${idPendaftaran}`);
    this.kirimPesan(actor.id_user, 'pengingat_hari_h', ctx, `pendaftaran:${idPendaftaran}`);
    this.audit(actor.id_user, 'DAFTAR_POLI', 'pendaftaran_poli', idPendaftaran, kode);
    this.emit();
    return { ok: true, data: idPendaftaran };
  }

  simpanKeluhan(idPendaftaran: number, keluhan: string, durasi: string, alergi: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'pasien' || kunjungan.idUserPasien !== actor.id_user) {
      return gagal('Keluhan hanya dapat diubah oleh pasien.');
    }
    if (kunjungan.pendaftaran.tahap_alur >= 5) return gagal('Pemeriksaan sudah dikunci.');
    if (!keluhan.trim()) return gagal('Keluhan tidak boleh kosong.');
    kunjungan.pemeriksaan.keluhan = keluhan.trim();
    kunjungan.pemeriksaan.durasi_keluhan = durasi.trim() || '-';
    kunjungan.pemeriksaan.alergi = alergi.trim() || 'Tidak Ada';
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
    kunjungan.pendaftaran.waktu_check_in = new Date().toISOString();
    this.kirimPesan(
      actor.id_user,
      'info_giliran',
      {
        kode: kunjungan.pendaftaran.kode_booking,
        jam: formatJam(kunjungan.pendaftaran.estimasi_jam_masuk),
        mode: 'checkin',
      },
      `pendaftaran:${idPendaftaran}`
    );
    this.emit();
    return { ok: true, data: null };
  }

  batalkan(idPendaftaran: number): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    const pemilik = kunjungan.idUserPasien === actor.id_user;
    if (!pemilik && actor.role !== 'admin') return gagal('Anda tidak dapat membatalkan kunjungan ini.');
    if (kunjungan.pendaftaran.tahap_alur > 3) return gagal('Kunjungan yang sudah masuk ruang tidak dapat dibatalkan.');
    kunjungan.pendaftaran.status_antrean = 'Batal';
    kunjungan.pendaftaran.waktu_dibatalkan = new Date().toISOString();
    this.audit(actor.id_user, 'BATAL_DAFTAR', 'pendaftaran_poli', idPendaftaran, kunjungan.pendaftaran.kode_booking);
    this.emit();
    return { ok: true, data: null };
  }

  simpanVital(idPendaftaran: number, tensi: string, suhu: string, berat: string, loket: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'admin') return gagal('Tanda vital diisi oleh perawat atau admin.');
    if (!tensi.trim() || !suhu.trim() || !berat.trim()) return gagal('Tensi, suhu, dan berat badan wajib diisi.');
    kunjungan.pemeriksaan.tensi = tensi.trim();
    kunjungan.pemeriksaan.suhu = suhu.trim();
    kunjungan.pemeriksaan.berat_badan = berat.trim();
    if (loket.trim()) kunjungan.pendaftaran.loket = loket.trim();
    this.audit(actor.id_user, 'SIMPAN_VITAL', 'pemeriksaan', kunjungan.pemeriksaan.id_pemeriksaan, 'Tanda vital diperbarui');
    this.emit();
    return { ok: true, data: null };
  }

  simpanPemeriksaan(
    idPendaftaran: number,
    input: { kodeIcd: string; diagnosis: string; catatan: string; tindakan: string; kontrol: string }
  ): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'dokter' || actor.id_user !== kunjungan.idUserDokter) {
      return gagal('Pemeriksaan ini milik dokter jadwal tersebut.');
    }
    if (kunjungan.pendaftaran.tahap_alur < 4) return gagal('Pasien belum masuk ruang periksa.');
    if (!input.kodeIcd.trim() || !input.catatan.trim()) return gagal('ICD-10 dan catatan pemeriksaan wajib diisi.');
    const pemeriksaan = kunjungan.pemeriksaan;
    pemeriksaan.kode_icd10 = input.kodeIcd.trim();
    pemeriksaan.diagnosis = input.diagnosis.trim();
    pemeriksaan.catatan = input.catatan.trim();
    pemeriksaan.tindakan = input.tindakan.trim();
    pemeriksaan.jadwal_kontrol = input.kontrol.trim();
    if (!pemeriksaan.waktu_mulai) pemeriksaan.waktu_mulai = new Date().toISOString();
    this.audit(actor.id_user, 'SIMPAN_PEMERIKSAAN', 'pemeriksaan', pemeriksaan.id_pemeriksaan, input.kodeIcd);
    this.emit();
    return { ok: true, data: null };
  }

  tambahObat(idPendaftaran: number, idObat: number, jumlah: number, aturan: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    if (actor.role !== 'dokter' || actor.id_user !== kunjungan.idUserDokter) return gagal('Hanya dokter pemeriksa yang menambah resep.');
    if (kunjungan.pendaftaran.tahap_alur > 5) return gagal('Resep sudah dikirim dan tidak dapat diubah.');
    const obat = this.db.obat.find((row) => row.id_obat === idObat && row.is_aktif);
    if (!obat) return gagal('Obat tidak ditemukan.');
    const qty = Math.max(1, Math.floor(jumlah));
    let resep = this.db.resep.find((row) => row.id_pendaftaran === idPendaftaran) ?? null;
    if (!resep) {
      const sekarang = new Date().toISOString();
      resep = {
        id_resep: nextId(this.db.resep, 'id_resep'),
        nomor_resep: this.nomorResep(),
        id_pemeriksaan: kunjungan.pemeriksaan.id_pemeriksaan,
        id_pendaftaran: idPendaftaran,
        id_dokter: kunjungan.idDokter,
        id_apoteker: null,
        kode_qr: randomToken(12),
        kode_qr_kedaluwarsa: null,
        qr_dipakai: false,
        pin_pengambil: null,
        status_resep: 'Draft',
        pilihan_tebus: 'belum_memilih',
        estimasi_selesai: null,
        waktu_diterbitkan: sekarang,
        waktu_dikirim: null,
        waktu_siap: null,
        waktu_diambil: null,
        diverifikasi_oleh: null,
      };
      this.db.resep.push(resep);
    }
    if (resep.waktu_dikirim) return gagal('Resep sudah dikirim.');
    const ada = this.db.detail_resep.find((row) => row.id_resep === resep!.id_resep && row.id_obat === idObat);
    if (ada) {
      ada.jumlah += qty;
      ada.status_ketersediaan = statusKetersediaan(obat.stok, ada.jumlah);
    } else {
      this.db.detail_resep.push({
        id_detail: nextId(this.db.detail_resep, 'id_detail'),
        id_resep: resep.id_resep,
        id_obat: idObat,
        jumlah: qty,
        dosis: obat.bentuk_kekuatan,
        aturan_pakai: aturan,
        instruksi_racikan: obat.jenis_obat === 'Racikan' ? 'Racik sesuai aturan apoteker' : null,
        status_ketersediaan: statusKetersediaan(obat.stok, qty),
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
    if (!resep || resep.waktu_dikirim) return gagal('Resep sudah dikirim.');
    const kunjungan = this.kunjunganById(resep.id_pendaftaran);
    if (!kunjungan || kunjungan.idUserDokter !== actor.id_user) return gagal('Resep ini bukan milik Anda.');
    this.db.detail_resep = this.db.detail_resep.filter((row) => row.id_detail !== idDetail);
    this.emit();
    return { ok: true, data: null };
  }

  pilihTebus(idPendaftaran: number, pilihan: 'apotek_rs' | 'apotek_luar'): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan || !kunjungan.resep) return gagal('Resep belum tersedia.');
    if (kunjungan.idUserPasien !== actor.id_user) return gagal('Pilihan tebus diisi oleh pasien.');
    if (!kunjungan.resep.waktu_dikirim) return gagal('Dokter belum mengirim e-resep.');
    if (kunjungan.resep.pilihan_tebus !== 'belum_memilih') return gagal('Pilihan tebus sudah dikunci.');
    if (pilihan === 'apotek_rs' && !kunjungan.bolehRs) {
      return gagal('Stok rumah sakit tidak mencukupi untuk seluruh item. Pilih apotek luar atau hubungi farmasi.');
    }
    const resep = kunjungan.resep;
    const lama = resep.status_resep;
    resep.pilihan_tebus = pilihan;
    if (pilihan === 'apotek_rs') {
      const antrian = this.db.resep.filter(
        (row) =>
          row.id_resep !== resep.id_resep &&
          (row.status_resep === 'Antrean Farmasi' || row.status_resep === 'Sedang Diracik')
      ).length;
      const menit = estimasiFarmasiMenit(
        antrian,
        kunjungan.detail.map((row) => row.obat.jenis_obat)
      );
      resep.estimasi_selesai = tambahJam(new Date().toISOString(), menit / 60);
    }
    this.logStatus(resep, lama, resep.status_resep, actor.id_user, `Pasien memilih ${pilihan}`);
    this.emit();
    return { ok: true, data: null };
  }

  majuTahap(idPendaftaran: number): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan) return gagal('Kunjungan tidak ditemukan.');
    const tahap = kunjungan.pendaftaran.tahap_alur;
    if (tahap >= 10 || kunjungan.pendaftaran.status_antrean === 'Batal') {
      return gagal('Kunjungan sudah selesai.');
    }
    if (!this.bolehMaju(actor.role, tahap, actor.id_user === kunjungan.idUserDokter)) {
      return gagal('Peran Anda tidak mengacc tahap ini.');
    }
    const cek = this.validasiMaju(kunjungan, tahap);
    if (cek) return gagal(cek);

    if (tahap === 6 && kunjungan.resep?.pilihan_tebus === 'apotek_luar') {
      this.selesaiLuar(kunjungan, actor.id_user);
      this.emit();
      return { ok: true, data: null };
    }

    const berikut = tahap + 1;
    kunjungan.pendaftaran.tahap_alur = berikut;
    this.terapkanTahap(kunjungan, berikut, actor.id_user);
    this.audit(actor.id_user, 'ACC_TAHAP', 'pendaftaran_poli', idPendaftaran, `Ke tahap ${berikut}`);
    this.emit();
    return { ok: true, data: null };
  }

  serahkan(idPendaftaran: number, kode: string, pengambil: 'pasien' | 'keluarga', pin: string): Result<null> {
    const actor = this.actor();
    const kunjungan = this.kunjunganById(idPendaftaran);
    if (!actor || !kunjungan || !kunjungan.resep) return gagal('Resep tidak ditemukan.');
    if (actor.role !== 'apoteker' && actor.role !== 'admin') return gagal('Penyerahan dilakukan apoteker.');
    const resep = kunjungan.resep;
    if (resep.kode_qr.trim().toLowerCase() !== kode.trim().toLowerCase()) return gagal('Kode QR tidak cocok.');
    if (resep.status_resep !== 'Siap Diambil' || kunjungan.pendaftaran.tahap_alur < 9) {
      return gagal('Obat belum berstatus siap diambil.');
    }
    if (!qrMasihBerlaku(resep.kode_qr_kedaluwarsa, resep.qr_dipakai)) {
      return gagal('QR kedaluwarsa atau sudah dipakai. Tolak pengambilan.');
    }
    if (pengambil === 'keluarga' && pin.trim() !== resep.pin_pengambil) {
      return gagal('PIN keluarga tidak sesuai.');
    }
    this.selesaiAmbil(kunjungan, actor.id_user, pengambil);
    this.emit();
    return { ok: true, data: null };
  }

  sesuaikanStok(idObat: number, stokBaru: number, keterangan: string): Result<null> {
    const actor = this.actor();
    if (!actor || (actor.role !== 'apoteker' && actor.role !== 'admin')) {
      return gagal('Penyesuaian stok dilakukan farmasi.');
    }
    const obat = this.db.obat.find((row) => row.id_obat === idObat);
    if (!obat) return gagal('Obat tidak ditemukan.');
    const tujuan = Math.max(0, Math.floor(stokBaru));
    const sebelum = obat.stok;
    if (tujuan === sebelum) return gagal('Stok tidak berubah.');
    obat.stok = tujuan;
    this.db.log_stok.push({
      id_log: nextId(this.db.log_stok, 'id_log'),
      id_obat: obat.id_obat,
      jenis: 'penyesuaian',
      jumlah: Math.abs(tujuan - sebelum),
      stok_sebelum: sebelum,
      stok_sesudah: tujuan,
      keterangan: keterangan.trim() || 'Penyesuaian stok fisik',
      id_user: actor.id_user,
      id_detail: null,
      waktu: new Date().toISOString(),
    });
    this.segarkanKetersediaan();
    this.audit(actor.id_user, 'SESUAIKAN_STOK', 'obat', obat.id_obat, `${sebelum} -> ${tujuan}`);
    this.emit();
    return { ok: true, data: null };
  }

  private bolehMaju(role: Role, tahap: number, dokterPemilik: boolean): boolean {
    if (role === 'admin') return true;
    if (role === 'dokter' && dokterPemilik && (tahap === 4 || tahap === 5)) return true;
    if (role === 'apoteker' && tahap >= 7 && tahap <= 9) return true;
    return false;
  }

  private validasiMaju(kunjungan: Kunjungan, tahap: number): string | null {
    if (tahap === 1 && !kunjungan.pemeriksaan.keluhan.trim()) return 'Keluhan pasien belum diisi.';
    if (tahap === 2 && !kunjungan.pemeriksaan.tensi.trim()) return 'Tanda vital belum diisi.';
    if (tahap === 4 && !kunjungan.pemeriksaan.kode_icd10.trim()) return 'Diagnosa ICD-10 belum disimpan.';
    if (tahap === 5 && kunjungan.detail.length === 0) return 'E-resep masih kosong.';
    if (tahap === 6 && (!kunjungan.resep || kunjungan.resep.pilihan_tebus === 'belum_memilih')) {
      return 'Pasien belum memilih tempat tebus.';
    }
    return null;
  }

  private terapkanTahap(kunjungan: Kunjungan, tahap: number, actorId: number): void {
    const pendaftaran = kunjungan.pendaftaran;
    const sekarang = new Date().toISOString();
    if (tahap === 3) {
      pendaftaran.status_antrean = 'Menunggu';
    }
    if (tahap === 4) {
      pendaftaran.status_antrean = 'Masuk Ruangan';
      if (!kunjungan.pemeriksaan.waktu_mulai) kunjungan.pemeriksaan.waktu_mulai = sekarang;
      this.kirimPesan(
        kunjungan.idUserPasien,
        'info_giliran',
        { kode: pendaftaran.kode_booking, mode: 'panggil' },
        `pendaftaran:${pendaftaran.id_pendaftaran}`
      );
    }
    if (tahap === 5) {
      kunjungan.pemeriksaan.waktu_selesai = sekarang;
    }
    if (tahap === 6) {
      this.pastikanResep(kunjungan);
      const resep = this.db.resep.find((row) => row.id_pendaftaran === pendaftaran.id_pendaftaran)!;
      this.ubahStatusResep(resep, 'Verifikasi Kasir', actorId, 'E-resep dikirim');
      resep.waktu_dikirim = sekarang;
      this.segarkanKetersediaan();
      this.kirimPesan(
        kunjungan.idUserPasien,
        'info_ketersediaan',
        { kode: pendaftaran.kode_booking },
        `resep:${resep.id_resep}`
      );
    }
    if (tahap === 7) {
      const resep = kunjungan.resep!;
      this.pasangApoteker(actorId, resep);
      this.ubahStatusResep(resep, 'Sedang Diracik', actorId, 'Masuk peracikan');
    }
    if (tahap === 8) {
      const resep = kunjungan.resep!;
      resep.diverifikasi_oleh = actorId;
      this.pasangApoteker(actorId, resep);
      this.ubahStatusResep(resep, 'Pengecekan', actorId, 'Quality control');
    }
    if (tahap === 9) {
      const resep = kunjungan.resep!;
      this.pasangApoteker(actorId, resep);
      resep.waktu_siap = sekarang;
      resep.kode_qr_kedaluwarsa = tambahJam(sekarang, 24);
      resep.pin_pengambil = randomPin();
      resep.qr_dipakai = false;
      this.snapshotKetersediaan(resep.id_resep);
      this.ubahStatusResep(resep, 'Siap Diambil', actorId, 'Siap di loket');
      this.kirimPesan(
        kunjungan.idUserPasien,
        'qr_resep',
        { kode: pendaftaran.kode_booking, loket: pendaftaran.loket },
        `resep:${resep.id_resep}`
      );
    }
    if (tahap === 10 && kunjungan.resep) {
      this.selesaiAmbil(kunjungan, actorId, 'pasien');
    }
  }

  private selesaiLuar(kunjungan: Kunjungan, actorId: number): void {
    const resep = kunjungan.resep!;
    kunjungan.pendaftaran.tahap_alur = 10;
    kunjungan.pendaftaran.status_antrean = 'Selesai';
    this.ubahStatusResep(resep, 'Tebus Luar', actorId, 'Resep dibawa ke apotek luar');
    const pasien = this.db.pasien.find((row) => row.id_pasien === kunjungan.pendaftaran.id_pasien);
    if (pasien) pasien.terdaftar_satusehat = true;
    this.kirimPesan(
      kunjungan.idUserPasien,
      'selesai',
      { kode: kunjungan.pendaftaran.kode_booking },
      `resep:${resep.id_resep}`
    );
    this.audit(actorId, 'TEBUS_LUAR', 'resep', resep.id_resep, 'Tanpa pengurangan stok RS');
  }

  private selesaiAmbil(kunjungan: Kunjungan, actorId: number, pengambil: 'pasien' | 'keluarga'): void {
    const resep = kunjungan.resep!;
    if (resep.status_resep === 'Selesai Diambil') return;
    kunjungan.pendaftaran.tahap_alur = 10;
    kunjungan.pendaftaran.status_antrean = 'Selesai';
    resep.qr_dipakai = true;
    resep.waktu_diambil = new Date().toISOString();
    this.pasangApoteker(actorId, resep);
    this.keluarkanStok(resep, actorId);
    this.ubahStatusResep(resep, 'Selesai Diambil', actorId, `Diserahkan ke ${pengambil}`);
    const pasien = this.db.pasien.find((row) => row.id_pasien === kunjungan.pendaftaran.id_pasien);
    if (pasien) pasien.terdaftar_satusehat = true;
    this.kirimPesan(
      kunjungan.idUserPasien,
      'selesai',
      { kode: kunjungan.pendaftaran.kode_booking },
      `resep:${resep.id_resep}`
    );
    this.audit(actorId, 'SERAH_OBAT', 'resep', resep.id_resep, pengambil);
  }

  private pastikanResep(kunjungan: Kunjungan): void {
    if (this.db.resep.some((row) => row.id_pendaftaran === kunjungan.pendaftaran.id_pendaftaran)) return;
    this.db.resep.push({
      id_resep: nextId(this.db.resep, 'id_resep'),
      nomor_resep: this.nomorResep(),
      id_pemeriksaan: kunjungan.pemeriksaan.id_pemeriksaan,
      id_pendaftaran: kunjungan.pendaftaran.id_pendaftaran,
      id_dokter: kunjungan.idDokter,
      id_apoteker: null,
      kode_qr: randomToken(12),
      kode_qr_kedaluwarsa: null,
      qr_dipakai: false,
      pin_pengambil: null,
      status_resep: 'Draft',
      pilihan_tebus: 'belum_memilih',
      estimasi_selesai: null,
      waktu_diterbitkan: new Date().toISOString(),
      waktu_dikirim: null,
      waktu_siap: null,
      waktu_diambil: null,
      diverifikasi_oleh: null,
    });
  }

  private ubahStatusResep(resep: Resep, baru: StatusResep, actorId: number, catatan: string): void {
    const lama = resep.status_resep;
    if (lama === baru) return;
    resep.status_resep = baru;
    this.logStatus(resep, lama, baru, actorId, catatan);
  }

  private logStatus(resep: Resep, lama: string, baru: StatusResep, actorId: number, catatan: string): void {
    if (lama === baru && !catatan) return;
    this.db.log_status_resep.push({
      id_log: nextId(this.db.log_status_resep, 'id_log'),
      id_resep: resep.id_resep,
      status_lama: lama,
      status_baru: baru,
      id_user: actorId,
      waktu: new Date().toISOString(),
      catatan,
    });
  }

  private pasangApoteker(actorId: number, resep: Resep): void {
    const apoteker = this.db.apoteker.find((row) => row.id_user === actorId);
    if (apoteker) resep.id_apoteker = apoteker.id_apoteker;
  }

  private keluarkanStok(resep: Resep, actorId: number): void {
    const details = this.db.detail_resep.filter((row) => row.id_resep === resep.id_resep);
    details.forEach((detail) => {
      const obat = this.db.obat.find((row) => row.id_obat === detail.id_obat);
      if (!obat) return;
      const keluar = Math.min(obat.stok, detail.jumlah);
      if (keluar <= 0) return;
      const sebelum = obat.stok;
      obat.stok = sebelum - keluar;
      this.db.log_stok.push({
        id_log: nextId(this.db.log_stok, 'id_log'),
        id_obat: obat.id_obat,
        jenis: 'keluar',
        jumlah: keluar,
        stok_sebelum: sebelum,
        stok_sesudah: obat.stok,
        keterangan: `Penyerahan ${resep.nomor_resep}`,
        id_user: actorId,
        id_detail: detail.id_detail,
        waktu: new Date().toISOString(),
      });
    });
  }

  private segarkanKetersediaan(): void {
    this.db.detail_resep.forEach((detail) => {
      const resep = this.db.resep.find((row) => row.id_resep === detail.id_resep);
      if (!resep || resep.status_resep === 'Siap Diambil' || resep.status_resep === 'Selesai Diambil') return;
      const obat = this.db.obat.find((row) => row.id_obat === detail.id_obat);
      if (!obat) return;
      detail.status_ketersediaan = statusKetersediaan(obat.stok, detail.jumlah);
    });
  }

  private snapshotKetersediaan(idResep: number): void {
    this.db.detail_resep
      .filter((row) => row.id_resep === idResep)
      .forEach((detail) => {
        const obat = this.db.obat.find((row) => row.id_obat === detail.id_obat);
        if (obat) detail.status_ketersediaan = statusKetersediaan(obat.stok, detail.jumlah);
      });
  }

  private kirimPesan(
    idUser: number,
    jenis: JenisNotifikasi,
    ctx: Parameters<typeof teksNotifikasi>[1],
    referensi: string
  ): void {
    const isi = teksNotifikasi(jenis, ctx);
    this.db.notifikasi.push({
      id_notifikasi: nextId(this.db.notifikasi, 'id_notifikasi'),
      id_user: idUser,
      jenis_kejadian: jenis,
      kanal: 'whatsapp',
      isi_pesan: isi,
      status_kirim: 'terkirim',
      jumlah_percobaan: 1,
      waktu_kirim: new Date().toISOString(),
      referensi_id: referensi,
    });
    const judul: Record<JenisNotifikasi, string> = {
      verifikasi_wa: 'Verifikasi WhatsApp',
      konfirmasi_booking: 'Booking berhasil',
      pengingat_hari_h: 'Pengingat kunjungan',
      info_giliran: 'Giliran antrean',
      info_ketersediaan: 'E-resep terbit',
      qr_resep: 'Obat siap diambil',
      obat_siap: 'Obat siap diambil',
      selesai: 'Kunjungan selesai',
    };
    void notifyBrowser(judul[jenis], isi);
  }

  private susun(pendaftaran: PendaftaranPoli): Kunjungan | null {
    const pasien = this.db.pasien.find((row) => row.id_pasien === pendaftaran.id_pasien);
    const pemeriksaan = this.db.pemeriksaan.find((row) => row.id_pendaftaran === pendaftaran.id_pendaftaran);
    const jadwal = this.db.jadwal_dokter.find((row) => row.id_jadwal === pendaftaran.id_jadwal);
    if (!pasien || !pemeriksaan || !jadwal) return null;
    const dokter = this.db.dokter.find((row) => row.id_dokter === pemeriksaan.id_dokter);
    const userDokter = dokter ? this.db.users.find((row) => row.id_user === dokter.id_user) : undefined;
    const poli = this.db.poli.find((row) => row.id_poli === jadwal.id_poli);
    if (!dokter || !userDokter || !poli) return null;
    const resep = this.db.resep.find((row) => row.id_pendaftaran === pendaftaran.id_pendaftaran) ?? null;
    const kunciSnapshot = resep?.status_resep === 'Siap Diambil' || resep?.status_resep === 'Selesai Diambil';
    const detail = (resep ? this.db.detail_resep.filter((row) => row.id_resep === resep.id_resep) : []).map((row) => {
      const obat = this.db.obat.find((item) => item.id_obat === row.id_obat)!;
      const ketersediaan = kunciSnapshot ? row.status_ketersediaan : statusKetersediaan(obat.stok, row.jumlah);
      return { ...row, obat, ketersediaan };
    });
    return {
      pendaftaran,
      pasienNama: pasien.nama,
      noRekamMedis: pasien.no_rekam_medis,
      idUserPasien: pasien.id_user,
      poliNama: poli.nama_poli,
      poliKode: poli.kode,
      dokterNama: userDokter.nama_lengkap,
      spesialisasi: dokter.spesialisasi,
      idDokter: dokter.id_dokter,
      idUserDokter: dokter.id_user,
      jadwal,
      pemeriksaan,
      resep,
      detail,
      bolehRs: bolehTebusRs(detail.map((row) => row.ketersediaan)),
    };
  }

  private kodeBooking(kodePoli: string): string {
    const nomor = this.db.pendaftaran_poli.reduce((max, row) => {
      const cocok = row.kode_booking.match(/-(\d+)$/);
      return Math.max(max, cocok ? Number(cocok[1]) : 0);
    }, 0);
    return `${kodePoli}-${String(nomor + 1).padStart(3, '0')}`;
  }

  private nomorResep(): string {
    const tanggal = tanggalIso().replace(/-/g, '');
    const urut = this.db.resep.filter((row) => row.nomor_resep.includes(tanggal)).length + 1;
    return `RSP-${tanggal}-${String(urut).padStart(4, '0')}`;
  }

  private audit(idUser: number, aksi: string, entitas: string, idEntitas: number, keterangan: string): void {
    this.db.audit_log.push({
      id_audit: nextId(this.db.audit_log, 'id_audit'),
      id_user: idUser,
      aksi,
      entitas,
      id_entitas: idEntitas,
      waktu: new Date().toISOString(),
      keterangan,
    });
  }
}

export const mediflow = new MediflowStore();
