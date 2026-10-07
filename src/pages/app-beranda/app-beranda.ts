import { LitElement, html, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import QRCode from 'qrcode';
import { maskNik } from '../../mediflow/crypto';
import { formatJam, formatRp, formatTanggal, labelKetersediaan, labelPenjamin, labelStatusResep } from '../../mediflow/format';
import { ATURAN_PAKAI, FASE, ICD, LANGKAH, TINDAKAN, langkah, panduan, type KonteksPanduan, type Panduan } from '../../mediflow/labels';
import { mintaIzinNotifikasi } from '../../mediflow/notify';
import { mediflow, type Kunjungan, type NotifikasiTampil } from '../../mediflow/store';
import type { Role } from '../../mediflow/types';
import { jamPendek, judulNotifikasi, tanggalIso } from '../../mediflow/rules';
import { go } from '../../router';
import { mediflowStyles } from '../../styles/mediflow-styles';

function nilai(event: Event): string {
  return (event.target as HTMLInputElement).value;
}

@customElement('app-beranda')
export class AppBeranda extends LitElement {
  static styles = mediflowStyles;

  @state() private tab: 'main' | 'alur' | 'resep' = 'main';
  @state() private selectedId = 0;
  @state() private notice = '';
  @state() private noticeError = false;
  @state() private showDaftar = false;
  @state() private idPoli = 1;
  @state() private idJadwal = 0;
  @state() private keluhanBaru = '';
  @state() private keluhan = '';
  @state() private tensi = '';
  @state() private suhu = '';
  @state() private berat = '';
  @state() private tinggi = '';
  @state() private kodeIcd = ICD[0].kode;
  @state() private catatan = '';
  @state() private tindakan = TINDAKAN[0];
  @state() private idObat = 1;
  @state() private jumlahObat = 10;
  @state() private aturan = ATURAN_PAKAI[0];
  @state() private kodeSerah = '';
  @state() private pengambil: 'pasien' | 'keluarga' = 'pasien';
  @state() private pinSerah = '';
  @state() private revealNik = false;
  @state() private qrUrl = '';
  @state() private stokDraft: Record<number, string> = {};
  @state() private panelTerbuka = false;
  @state() private toasts: { id: number; judul: string; isi: string }[] = [];

  private formKunci = '';
  private qrFor = '';
  private lepas = () => {};
  private dikenal = new Set<number>();
  private toastTimer = new Map<number, number>();
  private onSwMessage = (event: MessageEvent) => {
    const data = event.data as { type?: string } | null;
    if (data?.type !== 'mediflow-open') return;
    this.panelTerbuka = true;
  };

  connectedCallback(): void {
    super.connectedCallback();
    if (!mediflow.session()) {
      go('/');
      return;
    }
    mediflow.notifikasiUser().forEach((item) => this.dikenal.add(item.id_notifikasi));
    this.lepas = mediflow.subscribe(() => {
      this.tangkapNotifikasiBaru();
      this.requestUpdate();
    });
    navigator.serviceWorker?.addEventListener('message', this.onSwMessage);
  }

  disconnectedCallback(): void {
    this.lepas();
    navigator.serviceWorker?.removeEventListener('message', this.onSwMessage);
    this.toastTimer.forEach((timer) => window.clearTimeout(timer));
    this.toastTimer.clear();
    super.disconnectedCallback();
  }

  protected willUpdate(): void {
    const kunjungan = this.aktif();
    const kunci = kunjungan
      ? `${kunjungan.pendaftaran.id_pendaftaran}:${kunjungan.tahap}`
      : '';
    if (kunci === this.formKunci) return;
    this.formKunci = kunci;
    this.revealNik = false;
    this.kodeSerah = '';
    this.pinSerah = '';
    if (!kunjungan) return;
    const periksa = kunjungan.pemeriksaan;
    const cocok = ICD.find((item) => periksa.diagnosis.startsWith(item.kode));
    this.keluhan = periksa.keluhan;
    this.tensi = periksa.tekanan_darah || '120/80';
    this.suhu = periksa.suhu_tubuh == null ? '36.8' : String(periksa.suhu_tubuh);
    this.berat = periksa.berat_badan == null ? '60' : String(periksa.berat_badan);
    this.tinggi = periksa.tinggi_badan == null ? '165' : String(periksa.tinggi_badan);
    this.kodeIcd = cocok?.kode ?? ICD[0].kode;
    this.catatan = periksa.catatan_dokter ?? '';
    this.tindakan = periksa.tindakan || TINDAKAN[0];
    this.idObat = mediflow.obatAktif()[0]?.id_obat ?? 1;
  }

  protected updated(): void {
    const kunjungan = this.aktif();
    const aktor = mediflow.userAktif();
    if (kunjungan && aktor && aktor.role !== 'pasien') {
      mediflow.catatAksesRekam(kunjungan.pendaftaran.id_pendaftaran);
    }
    const kode = this.kodeQr(kunjungan);
    if (!kode) return;
    if (this.qrFor === kode) return;
    this.qrFor = kode;
    void QRCode.toDataURL(kode, { margin: 1, width: 220, errorCorrectionLevel: 'M' }).then((url) => {
      this.qrUrl = url;
    });
  }

  private daftarPeran(): Kunjungan[] {
    const semua = mediflow.kunjungan();
    const aktor = mediflow.userAktif();
    if (!aktor) return [];
    const hari = tanggalIso();
    if (aktor.role === 'pasien') return semua.filter((item) => item.idUserPasien === aktor.id_user);
    if (aktor.role === 'dokter') {
      return semua.filter(
        (item) =>
          item.idUserDokter === aktor.id_user &&
          item.pendaftaran.tanggal_kunjungan === hari &&
          item.pendaftaran.status_antrean !== 'Batal'
      );
    }
    if (aktor.role === 'farmasi') {
      return semua.filter(
        (item) =>
          item.tahap >= 7 &&
          item.resep?.pilihan_penebusan !== 'Apotek Luar' &&
          item.pendaftaran.status_antrean !== 'Batal'
      );
    }
    return semua.filter((item) => item.pendaftaran.tanggal_kunjungan === hari);
  }

  private aktif(): Kunjungan | null {
    const daftar = this.daftarPeran();
    const dipilih = daftar.find((item) => item.pendaftaran.id_pendaftaran === this.selectedId);
    if (dipilih) return dipilih;
    return (
      daftar.find((item) => !item.selesai && item.pendaftaran.status_antrean !== 'Batal') ??
      daftar[0] ??
      null
    );
  }

  private konteks(kunjungan: Kunjungan | null): KonteksPanduan {
    if (!kunjungan) {
      return {
        tahap: 0,
        checkIn: false,
        adaKeluhan: false,
        adaVital: false,
        adaResepItem: false,
        pilihan: null,
        bolehRs: true,
        batal: false,
        selesai: false,
      };
    }
    const daftar = kunjungan.pendaftaran;
    const resep = kunjungan.resep;
    const pilihan =
      resep?.pilihan_penebusan === 'Apotek RS'
        ? 'rs'
        : resep?.pilihan_penebusan === 'Apotek Luar'
          ? 'luar'
          : resep
            ? 'belum'
            : null;
    return {
      tahap: kunjungan.tahap,
      checkIn: Boolean(daftar.waktu_check_in),
      adaKeluhan: Boolean(kunjungan.pemeriksaan.keluhan.trim()),
      adaVital: Boolean(kunjungan.pemeriksaan.tekanan_darah?.trim()),
      adaResepItem: kunjungan.detail.length > 0,
      pilihan,
      bolehRs: kunjungan.bolehRs,
      batal: daftar.status_antrean === 'Batal',
      selesai: kunjungan.selesai,
    };
  }

  private infoAlur(role: Role, kunjungan: Kunjungan | null): Panduan {
    return panduan(role, this.konteks(kunjungan));
  }

  private kodeQr(kunjungan: Kunjungan | null): string {
    if (!kunjungan?.resep) return '';
    const siap = kunjungan.tahap >= 9 || kunjungan.resep.pilihan_penebusan === 'Apotek Luar';
    return siap ? kunjungan.resep.kode_qr_unik : '';
  }

  private kabar(pesan: string, gagal = false): void {
    this.notice = pesan;
    this.noticeError = gagal;
  }

  private hasil(ok: boolean, sukses: string, error = ''): void {
    this.kabar(ok ? sukses : error, !ok);
  }

  private icdNama(kode: string): string {
    return ICD.find((item) => item.kode === kode)?.nama ?? '';
  }

  private async simpanDanMajuDokter(kunjungan: Kunjungan): Promise<void> {
    const simpan = mediflow.simpanPemeriksaan(kunjungan.pendaftaran.id_pendaftaran, {
      diagnosis: `${this.kodeIcd} - ${this.icdNama(this.kodeIcd)}`,
      catatan: this.catatan,
      tindakan: this.tindakan,
    });
    if (!simpan.ok) {
      this.hasil(false, '', simpan.error);
      return;
    }
    if (kunjungan.tahap === 4 || kunjungan.tahap === 5) {
      const dari = kunjungan.tahap;
      const maju = mediflow.majuTahap(kunjungan.pendaftaran.id_pendaftaran);
      const sukses =
        dari === 4
          ? 'Pemeriksaan selesai. Berikutnya, kirim resep ke farmasi.'
          : 'Resep terkirim. Pasien sekarang memilih apotek.';
      this.hasil(maju.ok, sukses, maju.ok ? '' : maju.error);
      return;
    }
    this.kabar('Catatan medis diperbarui.');
  }

  private daftarBaru(): void {
    const slot = mediflow
      .slotHari(tanggalIso(), this.idPoli)
      .find((item) => item.jadwal.id_jadwal === this.idJadwal) ?? mediflow.slotHari(tanggalIso(), this.idPoli)[0];
    if (!slot) {
      this.kabar('Tidak ada jadwal untuk poli ini hari ini.', true);
      return;
    }
    const hasil = mediflow.daftar({
      idJadwal: slot.jadwal.id_jadwal,
      keluhan: this.keluhanBaru,
    });
    if (!hasil.ok) {
      this.hasil(false, '', hasil.error);
      return;
    }
    this.selectedId = hasil.data;
    this.showDaftar = false;
    this.keluhanBaru = '';
    this.kabar('Pendaftaran berhasil. Konfirmasi masuk ke notifikasi aplikasi.');
  }

  private tangkapNotifikasiBaru(): void {
    const sekarang = mediflow.notifikasiUser();
    const ids = new Set(sekarang.map((item) => item.id_notifikasi));
    const diganti = [...this.dikenal].some((id) => !ids.has(id));
    if (diganti) {
      this.dikenal = ids;
      return;
    }
    const baru = sekarang.filter((item) => !this.dikenal.has(item.id_notifikasi));
    if (baru.length === 0) return;
    const masuk = baru.map((item) => {
      this.dikenal.add(item.id_notifikasi);
      return { id: item.id_notifikasi, judul: judulNotifikasi(item.jenis_notifikasi, item.pesan), isi: item.pesan };
    });
    this.toasts = [...masuk, ...this.toasts].slice(0, 3);
    masuk.forEach((item) => {
      const timer = window.setTimeout(() => this.tutupToast(item.id), 6000);
      this.toastTimer.set(item.id, timer);
    });
  }

  private tutupToast(id: number): void {
    const timer = this.toastTimer.get(id);
    if (timer !== undefined) window.clearTimeout(timer);
    this.toastTimer.delete(id);
    this.toasts = this.toasts.filter((item) => item.id !== id);
  }

  private tutupPanel(): void {
    if (!this.panelTerbuka) return;
    this.panelTerbuka = false;
    mediflow.tandaiDibaca();
  }

  private bukaPanel(): void {
    if (this.panelTerbuka) {
      this.tutupPanel();
      return;
    }
    this.panelTerbuka = true;
  }

  private waktuNotifikasi(iso: string | null): string {
    if (!iso) return '';
    return `${formatTanggal(iso)} ${formatJam(iso)}`;
  }

  render() {
    const aktor = mediflow.userAktif();
    if (!aktor) return nothing;
    const kunjungan = this.aktif();
    return html`
      <div class="app">
        ${this.panelTerbuka
          ? html`<button class="notify-backdrop" aria-label="Tutup notifikasi" @click=${() => this.tutupPanel()}></button>`
          : nothing}
        ${this.renderToasts()}
        ${this.renderHeader(aktor.role, aktor.nama_lengkap, kunjungan)}
        <main class="container">
          ${this.notice ? html`<div class="notice ${this.noticeError ? 'error' : ''}">${this.notice}</div>` : ''}
          ${this.renderBanner(aktor.role, kunjungan)}
          <div class="layout">
            ${this.tab === 'main'
              ? this.renderUtama(aktor.role, kunjungan)
              : this.tab === 'alur'
                ? this.renderAlur(kunjungan)
                : this.renderResep(kunjungan, aktor.role)}
          </div>
        </main>
        ${this.renderNav(aktor.role)}
      </div>
    `;
  }

  private renderHeader(role: Role, nama: string, kunjungan: Kunjungan | null) {
    const sub =
      role === 'pasien'
        ? `Pasien • ${nama}`
        : role === 'dokter'
          ? `Dokter • ${nama}${mediflow.dokterAktif()?.spesialisasi ? ', ' + mediflow.dokterAktif()!.spesialisasi : ''}`
          : role === 'farmasi'
            ? `Farmasi • ${nama}`
            : 'Petugas • Perawat & Admin';
    const tahap = kunjungan?.tahap ?? 0;
    const info = this.infoAlur(role, kunjungan);
    const judul = kunjungan ? info.status : 'Belum ada kunjungan';
    const kosong =
      role === 'pasien'
        ? 'Pilih poli dan jadwal dokter untuk mendapat nomor antrean'
        : role === 'dokter'
          ? 'Belum ada pasien di jadwal Anda hari ini'
          : role === 'farmasi'
            ? 'Resep muncul setelah pasien memilih apotek rumah sakit'
            : 'Belum ada kunjungan hari ini';
    return html`
      <header class="app-header">
        <div class="header-top">
          <div class="brand">
            <div class="brand-icon">+</div>
            <div class="brand-text">
              <h1>MediFlow</h1>
              <p>${sub}</p>
            </div>
          </div>
          <div class="header-actions">
            <button
              class="icon-btn"
              aria-label="Notifikasi"
              aria-expanded=${this.panelTerbuka ? 'true' : 'false'}
              @click=${() => this.bukaPanel()}
            >
              <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                <path fill="currentColor" d="M12 3a5 5 0 0 0-5 5v2.1c0 .7-.3 1.4-.8 1.9L4.6 13.6A1.5 1.5 0 0 0 5.7 16h12.6a1.5 1.5 0 0 0 1.1-2.4l-1.6-1.6a2.7 2.7 0 0 1-.8-1.9V8a5 5 0 0 0-5-5zm0 18a2.5 2.5 0 0 0 2.4-2h-4.8A2.5 2.5 0 0 0 12 21z"/>
              </svg>
              ${mediflow.jumlahBelumDibaca() > 0
                ? html`<span class="bell-badge">${mediflow.jumlahBelumDibaca() > 9 ? '9+' : mediflow.jumlahBelumDibaca()}</span>`
                : nothing}
            </button>
            ${this.panelTerbuka ? this.renderPanel() : nothing}
            <button class="logout-btn" @click=${() => { mediflow.logout(); go('/'); }}>Ganti Akun</button>
          </div>
        </div>
        <div class="queue-banner">
          <div class="queue-left">
            <span>${kunjungan ? `${kunjungan.poliNama} • ${labelPenjamin(kunjungan.nomorBpjs)}` : 'Antrean'}</span>
            <h2>${kunjungan ? `Antrean ${kunjungan.pendaftaran.nomor_antrean}` : '—'}</h2>
            <p>${judul}</p>
            <small>
              ${kunjungan
                ? `Estimasi masuk ${formatJam(kunjungan.pendaftaran.estimasi_jam_masuk)} • ${kunjungan.dokterNama}`
                : kosong}
            </small>
          </div>
          <div class="queue-badge">
            <strong>${kunjungan ? `${tahap} / 10` : '—'}</strong>
            <small>${kunjungan ? FASE.find((fase) => fase.id === info.faseId)?.nama ?? 'Selesai' : 'Mulai'}</small>
          </div>
        </div>
      </header>
    `;
  }

  private renderBanner(role: Role, kunjungan: Kunjungan | null) {
    const info = this.infoAlur(role, kunjungan);
    return html`
      <div class="role-banner">
        <div class="fase-track" aria-label="Bagian kunjungan">
          ${FASE.map((fase) => {
            const selesai = info.status === 'Kunjungan selesai';
            const kelas = selesai || info.faseId > fase.id ? 'done' : info.faseId === fase.id ? 'now' : '';
            return html`<span class=${kelas}>${fase.nama}</span>`;
          })}
        </div>
        <p><strong>${info.status}.</strong> ${info.tindakan}</p>
        ${info.penghalang ? html`<p class="warn">${info.penghalang}</p>` : nothing}
      </div>
    `;
  }

  private renderUtama(role: Role, kunjungan: Kunjungan | null) {
    if (role === 'pasien') return this.renderPasien();
    if (!kunjungan) {
      return html`<section class="block wide"><div class="card"><p class="hint">${this.infoAlur(role, null).tindakan}</p></div></section>`;
    }
    return html`
      ${this.renderPemilih(kunjungan)}
      ${role === 'dokter'
        ? this.renderDokter(kunjungan)
        : role === 'farmasi'
          ? this.renderFarmasi(kunjungan)
          : this.renderAdmin(kunjungan)}
    `;
  }

  private renderPemilih(kunjungan: Kunjungan) {
    const daftar = this.daftarPeran();
    if (daftar.length < 2) return nothing;
    return html`
      <div class="form-group span-all">
        <label class="form-label">Kunjungan</label>
        <select
          class="select-field"
          .value=${String(kunjungan.pendaftaran.id_pendaftaran)}
          @change=${(event: Event) => {
            this.selectedId = Number(nilai(event));
            this.formKunci = '';
          }}
        >
          ${daftar.map(
            (item) => html`
              <option value=${item.pendaftaran.id_pendaftaran}>
                Antrean ${item.pendaftaran.nomor_antrean} • ${item.pasienNama} • ${langkah(item.tahap).singkat}
              </option>
            `
          )}
        </select>
      </div>
    `;
  }

  private renderPasien() {
    const jalan = this.aktif();
    const masih =
      jalan !== null &&
      !jalan.selesai &&
      jalan.pendaftaran.status_antrean !== 'Batal';
    return html`
      ${this.daftarPeran().length > 1 && jalan ? this.renderPemilih(jalan) : nothing}
      ${masih && jalan ? this.renderFormKeluhan(jalan) : nothing}
      ${this.renderFormDaftar(!masih || this.showDaftar, Boolean(masih))}
      ${jalan ? this.renderStatus(jalan) : nothing}
      ${jalan ? this.renderRingkasan(jalan) : nothing}
      ${jalan ? this.renderPilihan(jalan) : nothing}
      ${jalan ? this.renderQr(jalan) : nothing}
      ${this.renderProfil()}
      ${this.renderPesan()}
    `;
  }

  private renderFormKeluhan(kunjungan: Kunjungan) {
    const terkunci = kunjungan.tahap >= 5;
    return html`
      <section class="block wide">
      <div class="section-title">
        Keluhan untuk dokter
        <span class="pill">${terkunci ? 'Terkunci' : 'Masih bisa diubah'}</span>
      </div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Poliklinik</small><strong>${kunjungan.poliNama}</strong></div>
          <div class="info-item"><small>Dokter</small><strong>${kunjungan.dokterNama}</strong></div>
          <div class="info-item"><small>Penjamin</small><strong>${labelPenjamin(kunjungan.nomorBpjs)}</strong></div>
          <div class="info-item"><small>Jadwal</small><strong>${jamPendek(kunjungan.jadwal.jam_mulai)}–${jamPendek(kunjungan.jadwal.jam_selesai)}</strong></div>
        </div>
        <div class="form-group">
          <label class="form-label">Keluhan utama</label>
          <textarea class="textarea-field" .value=${this.keluhan} ?disabled=${terkunci} @input=${(event: Event) => { this.keluhan = nilai(event); }}></textarea>
        </div>
        <button
          class="btn-primary"
          ?disabled=${terkunci}
          @click=${() => {
            const hasil = mediflow.simpanKeluhan(kunjungan.pendaftaran.id_pendaftaran, this.keluhan);
            this.hasil(hasil.ok, 'Keluhan tersimpan untuk dokter.', hasil.ok ? '' : hasil.error);
          }}
        >Simpan keluhan</button>
      </div>
      </section>
    `;
  }

  private renderFormDaftar(tampil: boolean, adaJalan: boolean) {
    const slot = mediflow.slotHari(tanggalIso(), this.idPoli);
    return html`
      <section class="block wide">
      <div class="section-title">
        ${adaJalan ? 'Kunjungan lain' : 'Daftar poliklinik'}
        ${adaJalan
          ? html`<button class="btn-small" @click=${() => { this.showDaftar = !this.showDaftar; }}>${this.showDaftar ? 'Tutup' : 'Buka'}</button>`
          : html`<span>Kuota dicek otomatis</span>`}
      </div>
      ${tampil
        ? html`
            <div class="card">
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">Poliklinik tujuan</label>
                  <select class="select-field" .value=${String(this.idPoli)} @change=${(event: Event) => { this.idPoli = Number(nilai(event)); this.idJadwal = 0; }}>
                    ${mediflow.poli().map((poli) => html`<option value=${poli.id_poli}>${poli.nama_poli}</option>`)}
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">Penjamin</label>
                  <input class="input-field" readonly .value=${labelPenjamin(mediflow.pasienAktif()?.nomor_bpjs ?? null)} />
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Dokter dan jadwal hari ini</label>
                <select class="select-field" .value=${String(this.idJadwal || slot[0]?.jadwal.id_jadwal || '')} @change=${(event: Event) => { this.idJadwal = Number(nilai(event)); }}>
                  ${slot.map(
                    (item) => html`
                      <option value=${item.jadwal.id_jadwal}>
                        ${item.dokterNama}, ${item.spesialisasi} • ${jamPendek(item.jadwal.jam_mulai)}–${jamPendek(item.jadwal.jam_selesai)} • sisa ${item.sisa}
                      </option>
                    `
                  )}
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Keluhan utama</label>
                <textarea class="textarea-field" placeholder="Contoh: Demam naik turun, nyeri menelan..." .value=${this.keluhanBaru} @input=${(event: Event) => { this.keluhanBaru = nilai(event); }}></textarea>
              </div>
              <button class="btn-primary" id="btn-daftar" @click=${() => this.daftarBaru()}>Daftar dan dapatkan nomor antrean</button>
              <p class="hint">Penjamin mengikuti nomor BPJS di profil. Satu pasien tidak bisa mengambil jadwal yang sama dua kali. Poli tanpa jadwal hari ini tidak muncul.</p>
            </div>
          `
        : nothing}
      </section>
    `;
  }

  private renderStatus(kunjungan: Kunjungan) {
    const sudah = Boolean(kunjungan.pendaftaran.waktu_check_in);
    const bolehBatal = kunjungan.tahap > 0 && kunjungan.tahap <= 3 && kunjungan.pendaftaran.status_antrean !== 'Batal';
    return html`
      <section class="block">
      <div class="section-title">
        Kedatangan
        <span class=${sudah ? 'pill' : 'pill-warn'}>${sudah ? 'Sudah check-in' : 'Belum check-in'}</span>
      </div>
      <div class="card">
        <p class="hint" style="margin-top:0;">Check-in saat Anda sampai di rumah sakit. Pendaftaran bisa dibatalkan sebelum masuk ruang dokter.</p>
        <div class="btn-row">
          <button
            class="btn-outline"
            ?disabled=${Boolean(kunjungan.pendaftaran.waktu_check_in)}
            @click=${() => {
              const hasil = mediflow.checkIn(kunjungan.pendaftaran.id_pendaftaran);
              this.hasil(hasil.ok, 'Check-in tercatat.', hasil.ok ? '' : hasil.error);
            }}
          >${kunjungan.pendaftaran.waktu_check_in ? `Check-in ${formatJam(kunjungan.pendaftaran.waktu_check_in)}` : 'Check-in kedatangan'}</button>
          <button
            class="btn-outline danger"
            ?disabled=${!bolehBatal}
            @click=${() => {
              const hasil = mediflow.batalkan(kunjungan.pendaftaran.id_pendaftaran);
              this.hasil(hasil.ok, 'Pendaftaran dibatalkan. Kuota dikembalikan.', hasil.ok ? '' : hasil.error);
            }}
          >Batalkan pendaftaran</button>
        </div>
      </div>
      </section>
    `;
  }

  private renderRingkasan(kunjungan: Kunjungan) {
    const periksa = kunjungan.pemeriksaan;
    return html`
      <section class="block">
      <div class="section-title">Hasil pemeriksaan<span>Rekam medis</span></div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Tekanan darah</small><strong>${periksa.tekanan_darah || '-'}</strong></div>
          <div class="info-item"><small>Suhu</small><strong>${periksa.suhu_tubuh ?? '-'}</strong></div>
          <div class="info-item"><small>Berat dan tinggi</small><strong>${periksa.berat_badan ?? '-'} kg / ${periksa.tinggi_badan ?? '-'} cm</strong></div>
          <div class="info-item"><small>Diagnosis</small><strong>${periksa.diagnosis || '-'}</strong></div>
          <div class="info-item"><small>Tindakan</small><strong>${periksa.tindakan || '-'}</strong></div>
        </div>
        <small class="form-label">Catatan medis dokter</small>
        <p class="hint">${periksa.catatan_dokter || 'Belum ada catatan.'}</p>
      </div>
      </section>
    `;
  }

  private renderPilihan(kunjungan: Kunjungan) {
    const resep = kunjungan.resep;
    if (!kunjungan.resepTerkirim || !resep) return nothing;
    return html`
      <section class="block">
      <div class="section-title">Pilihan apotek<span>${resep.pilihan_penebusan === 'Apotek Luar' ? 'Dibawa ke apotek luar' : labelStatusResep(resep.status_resep)}</span></div>
      <div class="card">
        ${resep.pilihan_penebusan === 'Belum Memilih'
          ? html`
              <p class="hint">Pilih satu tempat. Apotek rumah sakit hanya aktif jika semua obat tersedia.</p>
              <div class="btn-row">
                <button
                  class="btn-primary"
                  ?disabled=${!kunjungan.bolehRs}
                  @click=${() => {
                    const hasil = mediflow.pilihTebus(kunjungan.pendaftaran.id_pendaftaran, 'apotek_rs');
                    this.hasil(hasil.ok, 'Anda memilih tebus di apotek rumah sakit.', hasil.ok ? '' : hasil.error);
                  }}
                >Tebus di Apotek RS</button>
                <button
                  class="btn-outline"
                  @click=${() => {
                    const hasil = mediflow.pilihTebus(kunjungan.pendaftaran.id_pendaftaran, 'apotek_luar');
                    this.hasil(hasil.ok, 'Resep dapat ditebus di apotek luar.', hasil.ok ? '' : hasil.error);
                  }}
                >Tebus di Apotek Luar</button>
              </div>
            `
          : html`
              <p class="hint">
                Pilihan terkunci: ${resep.pilihan_penebusan}.
                ${resep.estimasi_jam_selesai ? ` Estimasi selesai racik ${formatJam(resep.estimasi_jam_selesai)}.` : ''}
              </p>
            `}
      </div>
      </section>
    `;
  }

  private renderQr(kunjungan: Kunjungan) {
    const resep = kunjungan.resep;
    if (!resep) return nothing;
    const tampil = kunjungan.tahap >= 9 || resep.pilihan_penebusan === 'Apotek Luar';
    if (!tampil) return nothing;
    const luar = resep.pilihan_penebusan === 'Apotek Luar';
    return html`
      <section class="block">
      <div class="section-title">Kode pengambilan<span>${kunjungan.qrDipakai ? 'Sudah dipakai' : 'Sekali pakai'}</span></div>
      <div class="card qr-box">
        ${this.qrUrl ? html`<img src=${this.qrUrl} alt="Kode QR resep" />` : nothing}
        <p class="mono">${resep.kode_qr_unik}</p>
        <p class="hint">
          ${luar
            ? 'Bawa kode ini ke apotek luar. Stok rumah sakit tidak berkurang.'
            : resep.kadaluarsa_qr
              ? `Tunjukkan kode ini di farmasi rumah sakit. Berlaku sampai ${formatTanggal(resep.kadaluarsa_qr)} ${formatJam(resep.kadaluarsa_qr)}.`
              : 'Tunjukkan kode ini di farmasi rumah sakit.'}
          ${kunjungan.pinKeluarga ? ` PIN keluarga: ${kunjungan.pinKeluarga}.` : ''}
          PIN hanya tampil di aplikasi ini.
        </p>
      </div>
      </section>
    `;
  }

  private renderProfil() {
    const pasien = mediflow.pasienAktif();
    const user = mediflow.userAktif();
    if (!pasien || !user) return nothing;
    return html`
      <section class="block">
      <div class="section-title">Profil pasien<span>${labelPenjamin(pasien.nomor_bpjs)}</span></div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Nama</small><strong>${pasien.nama_lengkap}</strong></div>
          <div class="info-item"><small>NIK</small><strong>${this.revealNik ? pasien.nik : maskNik(pasien.nik)}</strong></div>
          <div class="info-item"><small>Lahir</small><strong>${formatTanggal(pasien.tanggal_lahir)} • ${pasien.jenis_kelamin === 'L' ? 'Laki-laki' : 'Perempuan'}</strong></div>
          <div class="info-item"><small>BPJS</small><strong>${pasien.nomor_bpjs ?? '-'}</strong></div>
          <div class="info-item"><small>Nomor HP</small><strong>${pasien.no_telepon}</strong></div>
          <div class="info-item"><small>Notifikasi</small><strong>Di aplikasi</strong></div>
        </div>
        <p class="hint">${pasien.alamat ?? ''}</p>
        <button class="btn-outline" @click=${() => { this.revealNik = !this.revealNik; }}>
          ${this.revealNik ? 'Sembunyikan NIK' : 'Tampilkan NIK'}
        </button>
        <p class="hint">NIK ditampilkan tertutup di layar. SATUSEHAT: ${pasien.terdaftar_satusehat ? 'riwayat kunjungan ditandai terkirim' : 'belum dikirim'}.</p>
      </div>
      </section>
    `;
  }

  private renderItemNotifikasi(pesan: NotifikasiTampil[]) {
    return pesan.map(
      (item) => html`
        <div class="med-item ${item.dibaca ? '' : 'unread'}">
          <div>
            <h4>${judulNotifikasi(item.jenis_notifikasi, item.pesan)}</h4>
            <p>${item.pesan}</p>
            <p>${this.waktuNotifikasi(item.waktu_dikirim ?? item.created_at)}</p>
          </div>
          <span class="pill">${item.dibaca ? 'Dibaca' : 'Baru'}</span>
        </div>
      `
    );
  }

  private renderToasts() {
    if (this.toasts.length === 0) return nothing;
    return html`
      <div class="toast-stack">
        ${this.toasts.map(
          (item) => html`
            <div class="toast" role="status">
              <div class="toast-top">
                <h4>${item.judul}</h4>
                <button aria-label="Tutup" @click=${() => this.tutupToast(item.id)}>×</button>
              </div>
              <p>${item.isi}</p>
            </div>
          `
        )}
      </div>
    `;
  }

  private renderPanel() {
    const pesan = mediflow.notifikasiUser();
    return html`
      <div class="notify-panel" role="dialog" aria-label="Notifikasi aplikasi">
        <h3>Notifikasi aplikasi</h3>
        ${pesan.length === 0
          ? html`<p class="hint">Belum ada notifikasi di akun ini. Pembaruan kunjungan masuk ke aplikasi pasien.</p>`
          : this.renderItemNotifikasi(pesan)}
      </div>
    `;
  }

  private renderPesan(lebar = false) {
    const pesan = mediflow.notifikasiUser();
    const belum = pesan.filter((item) => !item.dibaca).length;
    return html`
      <section class="block${lebar ? ' wide' : ''}">
      <div class="section-title">Notifikasi aplikasi<span>${belum > 0 ? `${belum} baru` : pesan.length}</span></div>
      <div class="card">
        ${pesan.length === 0 ? html`<p class="hint">Belum ada notifikasi.</p>` : this.renderItemNotifikasi(pesan)}
        <p class="hint">Semua pemberitahuan kunjungan masuk ke aplikasi ini. Isi tidak menyebut nama obat atau diagnosis.</p>
        ${belum > 0
          ? html`<button class="btn-outline" @click=${() => mediflow.tandaiDibaca()}>Tandai sudah dibaca</button>`
          : nothing}
        <button
          class="btn-outline"
          @click=${async () => {
            const izin = await mintaIzinNotifikasi();
            this.hasil(
              izin,
              'Perangkat siap menampilkan notifikasi saat aplikasi di latar.',
              'Izin notifikasi perangkat ditolak. Notifikasi tetap tampil di dalam aplikasi.'
            );
          }}
        >${typeof Notification !== 'undefined' && Notification.permission === 'granted' ? 'Notifikasi perangkat aktif' : 'Izinkan notifikasi perangkat'}</button>
      </div>
      </section>
    `;
  }

  private renderDokter(kunjungan: Kunjungan) {
    const periksa = kunjungan.pemeriksaan;
    const tahap = kunjungan.tahap;
    const terkunci = tahap > 5;
    const info = this.infoAlur('dokter', kunjungan);
    const labelTombol = info.milikAnda ? info.tombol : 'Simpan catatan saja';
    return html`
      <section class="block wide">
      <div class="section-title">Data keluhan dan tanda vital<span>${kunjungan.pasienNama}</span></div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Keluhan</small><strong>${periksa.keluhan || '-'}</strong></div>
          <div class="info-item"><small>Tekanan darah</small><strong>${periksa.tekanan_darah || '-'}</strong></div>
          <div class="info-item"><small>Suhu</small><strong>${periksa.suhu_tubuh ?? '-'}</strong></div>
          <div class="info-item"><small>Berat dan tinggi</small><strong>${periksa.berat_badan ?? '-'} kg / ${periksa.tinggi_badan ?? '-'} cm</strong></div>
        </div>
      </div>
      </section>
      <section class="block wide">
      <div class="section-title">Form pemeriksaan<span class="pill">${info.milikAnda ? 'Giliran Anda' : 'Lihat saja'}</span></div>
      <div class="card">
        <div class="form-group">
          <label class="form-label">Kode dan nama diagnosa (ICD-10)</label>
          <select class="select-field" .value=${this.kodeIcd} @change=${(event: Event) => { this.kodeIcd = nilai(event); }}>
            ${ICD.map((item) => html`<option value=${item.kode}>${item.kode} - ${item.nama}</option>`)}
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Anamnesa dan catatan fisik</label>
          <textarea class="textarea-field" placeholder="Catatan pemeriksaan" .value=${this.catatan} @input=${(event: Event) => { this.catatan = nilai(event); }}></textarea>
        </div>
        <div class="form-group">
          <label class="form-label">Tindakan</label>
          <select class="select-field" .value=${this.tindakan} @change=${(event: Event) => { this.tindakan = nilai(event); }}>
            ${TINDAKAN.map((item) => html`<option value=${item}>${item}</option>`)}
          </select>
        </div>
        <hr class="line" />
        <div class="form-group">
          <label class="form-label">E-resep dari master obat</label>
          <div class="form-row compact">
            <select class="select-field" .value=${String(this.idObat)} @change=${(event: Event) => { this.idObat = Number(nilai(event)); }}>
              ${mediflow.obatAktif().map((obat) => html`<option value=${obat.id_obat}>${obat.nama_obat} • stok ${obat.stok_rs}</option>`)}
            </select>
            <input class="input-field" type="number" min="1" .value=${String(this.jumlahObat)} @input=${(event: Event) => { this.jumlahObat = Number(nilai(event)); }} />
          </div>
          <div class="form-row compact-btn" style="margin-top:8px;">
            <select class="select-field" .value=${this.aturan} @change=${(event: Event) => { this.aturan = nilai(event); }}>
              ${ATURAN_PAKAI.map((item) => html`<option value=${item}>${item}</option>`)}
            </select>
            <button
              class="btn-outline"
              style="margin-top:0;"
              ?disabled=${terkunci}
              @click=${() => {
                const hasil = mediflow.tambahObat(kunjungan.pendaftaran.id_pendaftaran, this.idObat, this.jumlahObat, this.aturan);
                this.hasil(hasil.ok, 'Item resep ditambahkan.', hasil.ok ? '' : hasil.error);
              }}
            >+ Tambah obat</button>
          </div>
        </div>
        ${this.renderDaftarObat(kunjungan, !terkunci)}
        <button class="btn-primary" id="btn-dokter" @click=${() => this.simpanDanMajuDokter(kunjungan)}>${labelTombol}</button>
        <p class="hint">${info.penghalang ?? info.tindakan}</p>
      </div>
      </section>
    `;
  }

  private renderAdmin(kunjungan: Kunjungan) {
    const selesai = kunjungan.selesai || kunjungan.pendaftaran.status_antrean === 'Batal';
    const info = this.infoAlur('admin', kunjungan);
    return html`
      <section class="block">
      <div class="section-title">Skrining perawat<span>Tanda vital</span></div>
      <div class="card">
        <div class="form-row-3">
          <div class="form-group">
            <label class="form-label">Tensi</label>
            <input class="input-field" .value=${this.tensi} @input=${(event: Event) => { this.tensi = nilai(event); }} />
          </div>
          <div class="form-group">
            <label class="form-label">Suhu</label>
            <input class="input-field" .value=${this.suhu} @input=${(event: Event) => { this.suhu = nilai(event); }} />
          </div>
          <div class="form-group">
            <label class="form-label">Berat (kg)</label>
            <input class="input-field" .value=${this.berat} @input=${(event: Event) => { this.berat = nilai(event); }} />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Tinggi (cm)</label>
          <input class="input-field" .value=${this.tinggi} @input=${(event: Event) => { this.tinggi = nilai(event); }} />
        </div>
        <button
          class="btn-outline"
          style="margin-top:0;"
          @click=${() => {
            const hasil = mediflow.simpanVital(
              kunjungan.pendaftaran.id_pendaftaran,
              this.tensi,
              this.suhu,
              this.berat,
              this.tinggi
            );
            this.hasil(hasil.ok, 'Tanda vital diperbarui.', hasil.ok ? '' : hasil.error);
          }}
        >Simpan tanda vital</button>
      </div>
      </section>
      <section class="block">
      <div class="section-title">Lanjutkan kunjungan<span class="pill">${info.milikAnda ? 'Giliran Anda' : info.giliran}</span></div>
      <div class="card">
        <p class="hint" style="margin-top:0;">${info.tindakan}</p>
        ${info.penghalang ? html`<p class="hint warn">${info.penghalang}</p>` : nothing}
        <button
          class="btn-primary"
          id="btn-admin"
          ?disabled=${selesai}
          @click=${() => {
            const hasil = mediflow.majuTahap(kunjungan.pendaftaran.id_pendaftaran);
            this.hasil(hasil.ok, 'Kunjungan berpindah ke langkah berikutnya.', hasil.ok ? '' : hasil.error);
          }}
        >${selesai ? 'Kunjungan selesai' : info.tombol}</button>
        <button
          class="btn-outline"
          @click=${() => {
            mediflow.resetDemo();
            this.formKunci = '';
            this.selectedId = 0;
            this.kabar('Data demo dikembalikan ke pemeriksaan dokter.');
          }}
        >Kembalikan data demo</button>
      </div>
      </section>
      ${this.renderJejak()}
    `;
  }

  private renderFarmasi(kunjungan: Kunjungan) {
    const resep = kunjungan.resep;
    const tahap = kunjungan.tahap;
    const info = this.infoAlur('farmasi', kunjungan);
    const bolehLanjut = tahap === 7 || tahap === 8;
    return html`
      <section class="block">
      <div class="section-title">
        Antrean farmasi
        <span>${resep ? labelStatusResep(resep.status_resep) : 'Belum ada resep'}</span>
      </div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Pasien</small><strong>${kunjungan.pasienNama}</strong></div>
          <div class="info-item"><small>Resep</small><strong>${resep ? `Resep #${resep.id_resep}` : '-'}</strong></div>
          <div class="info-item"><small>Tempat</small><strong>Farmasi rumah sakit</strong></div>
          <div class="info-item"><small>Estimasi racik</small><strong>${formatJam(resep?.estimasi_jam_selesai)}</strong></div>
        </div>
        ${this.renderDaftarObat(kunjungan, false)}
        ${bolehLanjut
          ? html`<button
              class="btn-primary"
              @click=${() => {
                const hasil = mediflow.majuTahap(kunjungan.pendaftaran.id_pendaftaran);
                this.hasil(hasil.ok, 'Status farmasi diperbarui.', hasil.ok ? '' : hasil.error);
              }}
            >${info.tombol}</button>`
          : nothing}
        <p class="hint">${info.penghalang ?? info.tindakan}</p>
      </div>
      </section>
      <section class="block">
      <div class="section-title">Serah terima QR<span>1×24 jam</span></div>
      <div class="card">
        <div class="form-group">
          <label class="form-label">Kode QR</label>
          <input class="input-field mono" .value=${this.kodeSerah} @input=${(event: Event) => { this.kodeSerah = nilai(event); }} />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Pengambil</label>
            <select class="select-field" .value=${this.pengambil} @change=${(event: Event) => { this.pengambil = nilai(event) as 'pasien' | 'keluarga'; }}>
              <option value="pasien">Pasien</option>
              <option value="keluarga">Keluarga</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">PIN keluarga</label>
            <input class="input-field" .value=${this.pinSerah} ?disabled=${this.pengambil !== 'keluarga'} @input=${(event: Event) => { this.pinSerah = nilai(event); }} />
          </div>
        </div>
        <button
          class="btn-primary"
          id="btn-serah"
          @click=${() => {
            const hasil = mediflow.serahkan(kunjungan.pendaftaran.id_pendaftaran, this.kodeSerah, this.pengambil, this.pinSerah);
            this.hasil(hasil.ok, 'Obat diserahkan. Stok berkurang dan QR tidak bisa dipakai lagi.', hasil.ok ? '' : hasil.error);
          }}
        >Verifikasi dan serahkan obat</button>
        <p class="hint">Kode yang kedaluwarsa atau sudah dipakai ditolak. Jika keluarga yang mengambil, minta PIN yang tampil di aplikasi pasien.</p>
      </div>
      </section>
      <section class="block wide">
      <div class="section-title">Stok obat<span>Log setiap perubahan</span></div>
      <div class="card">
        ${mediflow.obatAktif().map(
          (obat) => html`
            <div class="med-item">
              <div>
                <h4>${obat.nama_obat}</h4>
                <p>${obat.kode_kemenkes} • ${obat.satuan} • minimum ${obat.stok_minimum} • ${obat.jenis_obat}</p>
              </div>
              <div class="med-side">
                <input
                  class="input-field"
                  style="width:64px;"
                  type="number"
                  .value=${this.stokDraft[obat.id_obat] ?? String(obat.stok_rs)}
                  @input=${(event: Event) => {
                    this.stokDraft = { ...this.stokDraft, [obat.id_obat]: nilai(event) };
                  }}
                />
                <button
                  class="btn-small"
                  @click=${() => {
                    const hasil = mediflow.sesuaikanStok(obat.id_obat, Number(this.stokDraft[obat.id_obat] ?? obat.stok_rs), 'Hitung ulang stok fisik');
                    this.hasil(hasil.ok, `Stok ${obat.nama_obat} disesuaikan.`, hasil.ok ? '' : hasil.error);
                  }}
                >Simpan</button>
              </div>
            </div>
          `
        )}
      </div>
      </section>
      ${this.renderJejak()}
    `;
  }

  private renderDaftarObat(kunjungan: Kunjungan, bolehHapus: boolean) {
    if (kunjungan.detail.length === 0) return html`<p class="hint">Belum ada item resep.</p>`;
    return html`
      ${kunjungan.detail.map((item) => {
        const bayar = !kunjungan.nomorBpjs || !item.obat.cover_bpjs;
        return html`
          <div class="med-item">
            <div>
              <h4>${item.obat.nama_obat}</h4>
              <p>${item.dosis_aturan_pakai}${item.instruksi_racikan ? ` • ${item.instruksi_racikan}` : ''}</p>
              <p>${bayar ? `Berbayar ${formatRp(item.obat.harga * item.jumlah)}` : 'Ditanggung BPJS'}</p>
            </div>
            <div class="med-side">
              <span class="pill ${item.ketersediaan === 'tersedia' ? '' : 'pill-danger'}">${item.jumlah} ${item.obat.satuan} • ${labelKetersediaan(item.ketersediaan)}</span>
              ${bolehHapus
                ? html`<button class="btn-small danger" @click=${() => mediflow.hapusObat(item.id_detail)}>×</button>`
                : nothing}
            </div>
          </div>
        `;
      })}
    `;
  }

  private renderAlur(kunjungan: Kunjungan | null) {
    const role = mediflow.userAktif()?.role ?? 'pasien';
    const tahap = kunjungan?.tahap ?? 0;
    const info = this.infoAlur(role, kunjungan);
    return html`
      <section class="block wide">
      <div class="section-title">
        Perjalanan kunjungan
        <span>${tahap ? `Langkah ${tahap} dari 10` : 'Belum mulai'}</span>
      </div>
      <div class="card">
        <p class="hint" style="margin-top:0;">
          Kunjungan berjalan dari daftar, periksa, resep, sampai obat diambil.
          Bagian berbingkai hijau adalah posisi sekarang. Pasien menunggu. Petugas bagian itu yang melanjutkan dari halaman tugasnya.
        </p>
        <div class="fase-board">
          ${FASE.map((fase) => {
            const selesai = info.status === 'Kunjungan selesai';
            const keadaan = selesai || info.faseId > fase.id ? 'done' : info.faseId === fase.id ? 'now' : '';
            const label = keadaan === 'done' ? 'Selesai' : keadaan === 'now' ? 'Sekarang' : 'Nanti';
            return html`
              <div class="fase-col ${keadaan}">
                <h3>${fase.nama}<span>${label}</span></h3>
                ${LANGKAH.filter((item) => item.tahap >= fase.dari && item.tahap <= fase.sampai).map((item) => {
                  const kelas = selesai || (tahap > 0 && item.tahap < tahap) ? 'done' : item.tahap === tahap ? 'current' : '';
                  return html`
                    <div class="step-item ${kelas}">
                      <div class="step-dot">${kelas === 'done' ? '✓' : item.tahap}</div>
                      <div class="step-body">
                        <h4>${item.judul}</h4>
                        <p>${item.deskripsi}</p>
                        <span class="owner-tag">${item.tahap === tahap ? 'Sedang dikerjakan · ' : ''}${item.pemilikNama}</span>
                      </div>
                    </div>
                  `;
                })}
              </div>
            `;
          })}
        </div>
      </div>
      </section>
    `;
  }

  private renderResep(kunjungan: Kunjungan | null, role: Role) {
    if (!kunjungan) {
      return html`<section class="block wide"><div class="card"><p class="hint">${this.infoAlur(role, null).tindakan}</p></div></section>`;
    }
    return html`
      <section class="block wide">
      <div class="section-title">
        Daftar obat e-resep
        <span class="pill">${kunjungan.resep?.pilihan_penebusan === 'Apotek Luar' ? 'Apotek luar' : 'Farmasi rumah sakit'}</span>
      </div>
      <div class="card">
        <p class="hint">${kunjungan.resep ? `Resep #${kunjungan.resep.id_resep} • ${kunjungan.resepTerkirim ? labelStatusResep(kunjungan.resep.status_resep) : 'Belum dikirim dokter'}` : 'Resep belum dibuat.'}</p>
        ${this.renderDaftarObat(kunjungan, role === 'dokter' && !kunjungan.resepTerkirim && kunjungan.tahap <= 5)}
      </div>
      </section>
      ${role === 'pasien' ? html`${this.renderPilihan(kunjungan)}${this.renderQr(kunjungan)}${this.renderPesan(true)}` : nothing}
    `;
  }

  private renderJejak() {
    const db = mediflow.snapshot();
    return html`
      <section class="block wide">
      <div class="section-title">Jejak stok dan akses<span>${db.log_aktivitas.length} catatan</span></div>
      <div class="card">
        ${mediflow.mutasiTerbaru().map((log) => {
          const obat = db.obat.find((item) => item.id_obat === log.id_obat);
          return html`<div class="med-item"><div><h4>${obat?.nama_obat ?? 'Obat'} • ${log.jenis_mutasi}</h4><p>${log.stok_sebelum} → ${log.stok_sesudah}. ${log.keterangan ?? ''}</p></div></div>`;
        })}
        ${mediflow.aktivitasTerbaru().map(
          (log) => html`<div class="med-item"><div><h4>${log.aktivitas}</h4><p>${log.tabel_referensi ?? '-'} #${log.id_referensi ?? '-'} • ${log.keterangan ?? ''}</p></div></div>`
        )}
        ${db.mutasi_stok.length === 0 && db.log_aktivitas.length === 0 ? html`<p class="hint">Belum ada jejak.</p>` : nothing}
      </div>
      </section>
    `;
  }

  private renderNav(role: Role) {
    const utama = role === 'pasien' ? 'Data Saya' : role === 'dokter' ? 'Form Medis' : role === 'farmasi' ? 'Farmasi' : 'Kontrol';
    const item = (id: 'main' | 'alur' | 'resep', ikon: string, label: string) => html`
      <button class="nav-item ${this.tab === id ? 'active' : ''}" @click=${() => { this.tab = id; this.tutupPanel(); }}>
        <span>${ikon}</span><span>${label}</span>
      </button>
    `;
    return html`
      <nav class="bottom-nav">
        ${item('main', '📝', utama)}
        ${item('alur', '📋', 'Alur')}
        ${item('resep', '💊', 'E-Resep')}
      </nav>
    `;
  }
}
