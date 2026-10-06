import { LitElement, html, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import QRCode from 'qrcode';
import { decryptNik, maskNik } from '../../mediflow/crypto';
import { formatJam, formatRp, formatTanggal, labelKetersediaan, labelPenjamin, labelStatusResep } from '../../mediflow/format';
import { ATURAN_PAKAI, ICD, LANGKAH, LOKET, TINDAKAN, langkah } from '../../mediflow/labels';
import { mediflow, type Kunjungan } from '../../mediflow/store';
import type { JenisPenjamin, Role } from '../../mediflow/types';
import { tanggalIso } from '../../mediflow/rules';
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
  @state() private jenisPenjamin: JenisPenjamin = 'bpjs';
  @state() private keluhanBaru = '';
  @state() private durasiBaru = '';
  @state() private alergiBaru = '';
  @state() private keluhan = '';
  @state() private durasi = '';
  @state() private alergi = '';
  @state() private tensi = '';
  @state() private suhu = '';
  @state() private berat = '';
  @state() private loket = LOKET[0];
  @state() private kodeIcd = ICD[0].kode;
  @state() private catatan = '';
  @state() private tindakan = TINDAKAN[0];
  @state() private kontrol = '';
  @state() private idObat = 1;
  @state() private jumlahObat = 10;
  @state() private aturan = ATURAN_PAKAI[0];
  @state() private kodeSerah = '';
  @state() private pengambil: 'pasien' | 'keluarga' = 'pasien';
  @state() private pinSerah = '';
  @state() private revealNik = false;
  @state() private qrUrl = '';
  @state() private stokDraft: Record<number, string> = {};

  private formKunci = '';
  private qrFor = '';
  private lepas = () => {};

  connectedCallback(): void {
    super.connectedCallback();
    if (!mediflow.session()) {
      go('/');
      return;
    }
    this.lepas = mediflow.subscribe(() => this.requestUpdate());
  }

  disconnectedCallback(): void {
    this.lepas();
    super.disconnectedCallback();
  }

  protected willUpdate(): void {
    const kunjungan = this.aktif();
    const kunci = kunjungan
      ? `${kunjungan.pendaftaran.id_pendaftaran}:${kunjungan.pendaftaran.tahap_alur}`
      : '';
    if (kunci === this.formKunci) return;
    this.formKunci = kunci;
    this.revealNik = false;
    this.kodeSerah = '';
    this.pinSerah = '';
    if (!kunjungan) return;
    const periksa = kunjungan.pemeriksaan;
    this.keluhan = periksa.keluhan;
    this.durasi = periksa.durasi_keluhan;
    this.alergi = periksa.alergi;
    this.tensi = periksa.tensi || '120/80';
    this.suhu = periksa.suhu || '36.8';
    this.berat = periksa.berat_badan || '60';
    this.loket = kunjungan.pendaftaran.loket || LOKET[0];
    this.kodeIcd = periksa.kode_icd10 || ICD[0].kode;
    this.catatan = periksa.catatan;
    this.tindakan = periksa.tindakan || TINDAKAN[0];
    this.kontrol = periksa.jadwal_kontrol;
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
    if (aktor.role === 'apoteker') {
      return semua.filter(
        (item) => item.resep !== null && item.pendaftaran.tahap_alur >= 5 && item.pendaftaran.status_antrean !== 'Batal'
      );
    }
    return semua.filter((item) => item.pendaftaran.tanggal_kunjungan === hari);
  }

  private aktif(): Kunjungan | null {
    const daftar = this.daftarPeran();
    const dipilih = daftar.find((item) => item.pendaftaran.id_pendaftaran === this.selectedId);
    if (dipilih) return dipilih;
    return (
      daftar.find(
        (item) => item.pendaftaran.status_antrean !== 'Selesai' && item.pendaftaran.status_antrean !== 'Batal'
      ) ??
      daftar[0] ??
      null
    );
  }

  private kodeQr(kunjungan: Kunjungan | null): string {
    if (!kunjungan?.resep) return '';
    const siap = kunjungan.pendaftaran.tahap_alur >= 9 || kunjungan.resep.status_resep === 'Tebus Luar';
    return siap ? kunjungan.resep.kode_qr : '';
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
      kodeIcd: this.kodeIcd,
      diagnosis: this.icdNama(this.kodeIcd),
      catatan: this.catatan,
      tindakan: this.tindakan,
      kontrol: this.kontrol,
    });
    if (!simpan.ok) {
      this.hasil(false, '', simpan.error);
      return;
    }
    if (kunjungan.pendaftaran.tahap_alur === 4 || kunjungan.pendaftaran.tahap_alur === 5) {
      const maju = mediflow.majuTahap(kunjungan.pendaftaran.id_pendaftaran);
      this.hasil(maju.ok, 'Tahap pemeriksaan diperbarui.', maju.ok ? '' : maju.error);
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
      jenisPenjamin: this.jenisPenjamin,
      keluhan: this.keluhanBaru,
      durasi: this.durasiBaru,
      alergi: this.alergiBaru,
    });
    if (!hasil.ok) {
      this.hasil(false, '', hasil.error);
      return;
    }
    this.selectedId = hasil.data;
    this.showDaftar = false;
    this.keluhanBaru = '';
    this.kabar('Booking berhasil. Konfirmasi dikirim sebagai pesan WhatsApp.');
  }

  render() {
    const aktor = mediflow.userAktif();
    if (!aktor) return nothing;
    const kunjungan = this.aktif();
    return html`
      <div class="app">
        ${this.renderHeader(aktor.role, aktor.nama_lengkap, kunjungan)}
        <main class="container">
          ${this.notice ? html`<div class="notice ${this.noticeError ? 'error' : ''}">${this.notice}</div>` : ''}
          ${this.renderBanner(aktor.role)}
          ${this.tab === 'main'
            ? this.renderUtama(aktor.role, kunjungan)
            : this.tab === 'alur'
              ? this.renderAlur(kunjungan)
              : this.renderResep(kunjungan, aktor.role)}
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
          : role === 'apoteker'
            ? `Apoteker • ${nama}`
            : 'Petugas • Perawat & Admin';
    const tahap = kunjungan?.pendaftaran.tahap_alur ?? 0;
    const judul = kunjungan ? langkah(tahap).judul : 'Belum ada kunjungan aktif';
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
          <button class="logout-btn" @click=${() => { mediflow.logout(); go('/'); }}>Ganti Akun</button>
        </div>
        <div class="queue-banner">
          <div class="queue-left">
            <span>${kunjungan ? `${kunjungan.poliNama} • ${labelPenjamin(kunjungan.pendaftaran.jenis_penjamin)}` : 'Antrean'}</span>
            <h2>${kunjungan?.pendaftaran.kode_booking ?? '—'}</h2>
            <p>${judul}</p>
            <small>
              ${kunjungan
                ? `Antrean ${kunjungan.pendaftaran.nomor_antrean} • Estimasi masuk ${formatJam(kunjungan.pendaftaran.estimasi_jam_masuk)}`
                : 'Pilih poli dan jadwal dokter untuk mendapat nomor booking'}
            </small>
          </div>
          <div class="queue-badge">
            <strong>${kunjungan ? `${tahap} / 10` : '0 / 10'}</strong>
            <small>Tahapan</small>
          </div>
        </div>
      </header>
    `;
  }

  private renderBanner(role: Role) {
    const teks =
      role === 'pasien'
        ? 'Isi keluhan, pantau progres, dan pilih tempat tebus'
        : role === 'dokter'
          ? 'Input diagnosa ICD-10, e-resep, dan ACC poli'
          : role === 'apoteker'
            ? 'Racik, cek stok, dan serahkan obat dengan QR'
            : 'Input tanda vital, loket, dan ACC tahapan';
    const badge = role === 'pasien' ? 'PASIEN' : role === 'dokter' ? 'DOKTER' : role === 'apoteker' ? 'APOTEKER' : 'ADMIN';
    return html`<div class="role-banner"><span>${teks}</span><span class="pill">${badge}</span></div>`;
  }

  private renderUtama(role: Role, kunjungan: Kunjungan | null) {
    if (role === 'pasien') return this.renderPasien();
    if (!kunjungan) return html`<div class="card"><p class="hint">Tidak ada kunjungan pada daftar peran ini.</p></div>`;
    return html`
      ${this.renderPemilih(kunjungan)}
      ${role === 'dokter'
        ? this.renderDokter(kunjungan)
        : role === 'apoteker'
          ? this.renderApoteker(kunjungan)
          : this.renderAdmin(kunjungan)}
    `;
  }

  private renderPemilih(kunjungan: Kunjungan) {
    const daftar = this.daftarPeran();
    if (daftar.length < 2) return nothing;
    return html`
      <div class="form-group">
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
                ${item.pendaftaran.kode_booking} • ${item.pasienNama} • tahap ${item.pendaftaran.tahap_alur}
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
      jalan.pendaftaran.status_antrean !== 'Selesai' &&
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
    const terkunci = kunjungan.pendaftaran.tahap_alur >= 5;
    return html`
      <div class="section-title">
        1. Form Data & Keluhan Pasien
        <span class="pill">${terkunci ? 'Terkunci' : 'Dapat diubah'}</span>
      </div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Poliklinik</small><strong>${kunjungan.poliNama}</strong></div>
          <div class="info-item"><small>Dokter</small><strong>${kunjungan.dokterNama}</strong></div>
          <div class="info-item"><small>Penjamin</small><strong>${labelPenjamin(kunjungan.pendaftaran.jenis_penjamin)}</strong></div>
          <div class="info-item"><small>Jadwal</small><strong>${kunjungan.jadwal.jam_mulai}–${kunjungan.jadwal.jam_selesai}</strong></div>
        </div>
        <div class="form-group">
          <label class="form-label">Keluhan utama</label>
          <textarea class="textarea-field" .value=${this.keluhan} ?disabled=${terkunci} @input=${(event: Event) => { this.keluhan = nilai(event); }}></textarea>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Lama keluhan</label>
            <input class="input-field" .value=${this.durasi} ?disabled=${terkunci} @input=${(event: Event) => { this.durasi = nilai(event); }} />
          </div>
          <div class="form-group">
            <label class="form-label">Riwayat alergi obat</label>
            <input class="input-field" .value=${this.alergi} ?disabled=${terkunci} @input=${(event: Event) => { this.alergi = nilai(event); }} />
          </div>
        </div>
        <button
          class="btn-primary"
          ?disabled=${terkunci}
          @click=${() => {
            const hasil = mediflow.simpanKeluhan(kunjungan.pendaftaran.id_pendaftaran, this.keluhan, this.durasi, this.alergi);
            this.hasil(hasil.ok, 'Keluhan tersimpan untuk dokter.', hasil.ok ? '' : hasil.error);
          }}
        >Simpan Data Keluhan Pasien</button>
      </div>
    `;
  }

  private renderFormDaftar(tampil: boolean, adaJalan: boolean) {
    const slot = mediflow.slotHari(tanggalIso(), this.idPoli);
    return html`
      <div class="section-title">
        ${adaJalan ? 'Kunjungan baru' : 'Daftar poliklinik'}
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
                  <label class="form-label">Penjamin biaya</label>
                  <select class="select-field" .value=${this.jenisPenjamin} @change=${(event: Event) => { this.jenisPenjamin = nilai(event) as JenisPenjamin; }}>
                    <option value="bpjs">BPJS Kesehatan</option>
                    <option value="umum">Umum</option>
                  </select>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Dokter dan jadwal hari ini</label>
                <select class="select-field" .value=${String(this.idJadwal || slot[0]?.jadwal.id_jadwal || '')} @change=${(event: Event) => { this.idJadwal = Number(nilai(event)); }}>
                  ${slot.map(
                    (item) => html`
                      <option value=${item.jadwal.id_jadwal}>
                        ${item.dokterNama}, ${item.spesialisasi} • ${item.jadwal.jam_mulai}–${item.jadwal.jam_selesai} • sisa ${item.sisa}
                      </option>
                    `
                  )}
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Keluhan utama</label>
                <textarea class="textarea-field" placeholder="Contoh: Demam naik turun, nyeri menelan..." .value=${this.keluhanBaru} @input=${(event: Event) => { this.keluhanBaru = nilai(event); }}></textarea>
              </div>
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">Lama keluhan</label>
                  <input class="input-field" placeholder="Mis: 3 Hari" .value=${this.durasiBaru} @input=${(event: Event) => { this.durasiBaru = nilai(event); }} />
                </div>
                <div class="form-group">
                  <label class="form-label">Riwayat alergi obat</label>
                  <input class="input-field" placeholder="Mis: Tidak ada / Penisilin" .value=${this.alergiBaru} @input=${(event: Event) => { this.alergiBaru = nilai(event); }} />
                </div>
              </div>
              <button class="btn-primary" id="btn-daftar" @click=${() => this.daftarBaru()}>Daftar dan dapatkan nomor booking</button>
              <p class="hint">Satu pasien tidak bisa daftar dua kali pada jadwal dan tanggal yang sama. Nomor antrean tidak boleh kembar.</p>
            </div>
          `
        : nothing}
    `;
  }

  private renderStatus(kunjungan: Kunjungan) {
    const tahap = kunjungan.pendaftaran.tahap_alur;
    const item = langkah(tahap);
    const selesai = tahap === 10;
    return html`
      <div class="section-title">
        2. Status tahapan antrean
        <span class=${selesai ? 'pill' : 'pill-warn'}>${selesai ? 'Kunjungan selesai' : `Menunggu ${item.pemilikNama}`}</span>
      </div>
      <div class="card">
        <div class="step-item current">
          <div class="step-dot">${tahap}</div>
          <div class="step-body">
            <h4>${item.judul}</h4>
            <p>${item.deskripsi}</p>
            <span class="owner-tag">Petugas pemroses: ${item.pemilikNama}</span>
          </div>
        </div>
        <p class="hint">Pasien tidak memajukan tahapan sendiri. Tahap berpindah saat dokter, admin, atau apoteker memberi ACC.</p>
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
            ?disabled=${kunjungan.pendaftaran.tahap_alur > 3}
            @click=${() => {
              const hasil = mediflow.batalkan(kunjungan.pendaftaran.id_pendaftaran);
              this.hasil(hasil.ok, 'Pendaftaran dibatalkan. Kuota dikembalikan.', hasil.ok ? '' : hasil.error);
            }}
          >Batalkan booking</button>
        </div>
      </div>
    `;
  }

  private renderRingkasan(kunjungan: Kunjungan) {
    const periksa = kunjungan.pemeriksaan;
    const icd = periksa.kode_icd10 ? `${periksa.kode_icd10} - ${periksa.diagnosis}` : '-';
    return html`
      <div class="section-title">3. Ringkasan hasil pemeriksaan<span>Rekam medis</span></div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Tensi dan suhu</small><strong>${periksa.tensi || '-'} | ${periksa.suhu || '-'}</strong></div>
          <div class="info-item"><small>Diagnosa ICD-10</small><strong>${icd}</strong></div>
          <div class="info-item"><small>Tindakan</small><strong>${periksa.tindakan || '-'}</strong></div>
          <div class="info-item"><small>Kontrol ulang</small><strong>${periksa.jadwal_kontrol || '-'}</strong></div>
        </div>
        <small class="form-label">Catatan medis dokter</small>
        <p class="hint">${periksa.catatan || 'Belum ada catatan.'}</p>
      </div>
    `;
  }

  private renderPilihan(kunjungan: Kunjungan) {
    const resep = kunjungan.resep;
    if (!resep?.waktu_dikirim) return nothing;
    return html`
      <div class="section-title">Pilihan penebusan<span>${labelStatusResep(resep.status_resep)}</span></div>
      <div class="card">
        ${resep.pilihan_tebus === 'belum_memilih'
          ? html`
              <p class="hint">Tombol apotek RS nonaktif jika ada item yang stoknya kurang dari jumlah resep.</p>
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
                Pilihan terkunci: ${resep.pilihan_tebus === 'apotek_rs' ? 'Apotek RS' : 'Apotek luar'}.
                ${resep.estimasi_selesai ? ` Estimasi selesai racik ${formatJam(resep.estimasi_selesai)}.` : ''}
              </p>
            `}
      </div>
    `;
  }

  private renderQr(kunjungan: Kunjungan) {
    const resep = kunjungan.resep;
    if (!resep) return nothing;
    const tampil = kunjungan.pendaftaran.tahap_alur >= 9 || resep.status_resep === 'Tebus Luar';
    if (!tampil) return nothing;
    return html`
      <div class="section-title">Kode pengambilan<span>${resep.qr_dipakai ? 'Sudah dipakai' : 'Sekali pakai'}</span></div>
      <div class="card qr-box">
        ${this.qrUrl ? html`<img src=${this.qrUrl} alt="Kode QR resep" />` : nothing}
        <p class="mono">${resep.kode_qr}</p>
        <p class="hint">
          ${resep.kode_qr_kedaluwarsa
            ? `Berlaku sampai ${formatTanggal(resep.kode_qr_kedaluwarsa)} ${formatJam(resep.kode_qr_kedaluwarsa)}.`
            : 'Tunjukkan kode ini di apotek.'}
          ${resep.pin_pengambil ? ` PIN keluarga: ${resep.pin_pengambil}.` : ''}
          PIN tidak dikirim lewat WhatsApp.
        </p>
      </div>
    `;
  }

  private renderProfil() {
    const pasien = mediflow.pasienAktif();
    const user = mediflow.userAktif();
    if (!pasien || !user) return nothing;
    const nik = decryptNik(pasien.nik_terenkripsi);
    return html`
      <div class="section-title">Profil pasien<span>${pasien.no_rekam_medis}</span></div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Nama</small><strong>${pasien.nama}</strong></div>
          <div class="info-item"><small>NIK</small><strong>${this.revealNik ? nik : maskNik(nik)}</strong></div>
          <div class="info-item"><small>Lahir</small><strong>${formatTanggal(pasien.tanggal_lahir)} • ${pasien.jenis_kelamin === 'L' ? 'Laki-laki' : 'Perempuan'}</strong></div>
          <div class="info-item"><small>BPJS</small><strong>${pasien.no_bpjs ?? '-'}</strong></div>
          <div class="info-item"><small>WhatsApp</small><strong>${user.no_wa}</strong></div>
          <div class="info-item"><small>WA terverifikasi</small><strong>${user.wa_terverifikasi_pada ? formatTanggal(user.wa_terverifikasi_pada) : 'Belum'}</strong></div>
        </div>
        <p class="hint">${pasien.alamat}</p>
        <button class="btn-outline" @click=${() => { this.revealNik = !this.revealNik; }}>
          ${this.revealNik ? 'Sembunyikan NIK' : 'Tampilkan NIK'}
        </button>
        <p class="hint">NIK disimpan terenkripsi di data lokal, bukan sebagai teks polos. SATUSEHAT: ${pasien.terdaftar_satusehat ? 'riwayat kunjungan ditandai terkirim' : 'belum dikirim'}.</p>
      </div>
    `;
  }

  private renderPesan() {
    const pesan = mediflow.notifikasiUser();
    return html`
      <div class="section-title">Pesan WhatsApp<span>${pesan.length}</span></div>
      <div class="card">
        ${pesan.length === 0 ? html`<p class="hint">Belum ada pesan.</p>` : nothing}
        ${pesan.map(
          (item) => html`
            <div class="med-item">
              <div>
                <h4>${item.jenis_kejadian.replaceAll('_', ' ')}</h4>
                <p>${item.isi_pesan}</p>
              </div>
              <span class="pill">${item.status_kirim}</span>
            </div>
          `
        )}
        <p class="hint">Gateway WhatsApp disimulasikan. Isi pesan tidak menyebut nama obat atau diagnosis. Percobaan kirim: tercatat di tiap baris.</p>
        <button
          class="btn-outline"
          @click=${async () => {
            const izin = await Notification.requestPermission();
            if (izin !== 'granted') {
              this.kabar('Izin notifikasi browser ditolak.', true);
              return;
            }
            const hasil = await mediflow.aktifkanPush();
            this.hasil(hasil.ok, 'Perangkat dicatat di push subscription.', hasil.ok ? '' : hasil.error);
          }}
        >${mediflow.punyaPush() ? 'Notifikasi browser aktif' : 'Aktifkan notifikasi browser'}</button>
      </div>
    `;
  }

  private renderDokter(kunjungan: Kunjungan) {
    const periksa = kunjungan.pemeriksaan;
    const tahap = kunjungan.pendaftaran.tahap_alur;
    const terkunci = tahap > 5;
    const labelTombol =
      tahap === 4
        ? 'Simpan diagnosa dan ACC pemeriksaan'
        : tahap === 5
          ? 'Validasi dan kirim e-resep ke farmasi'
          : 'Perbarui catatan medis';
    return html`
      <div class="section-title">Data keluhan dan tanda vital<span>${kunjungan.noRekamMedis}</span></div>
      <div class="card">
        <div class="allergy-alert">Riwayat alergi obat pasien: ${periksa.alergi || 'Tidak ada'}</div>
        <div class="info-grid">
          <div class="info-item"><small>Keluhan</small><strong>${periksa.keluhan || '-'}</strong></div>
          <div class="info-item"><small>Lama sakit</small><strong>${periksa.durasi_keluhan || '-'}</strong></div>
          <div class="info-item"><small>Tekanan darah</small><strong>${periksa.tensi || '-'}</strong></div>
          <div class="info-item"><small>Suhu dan berat</small><strong>${periksa.suhu || '-'} / ${periksa.berat_badan || '-'}</strong></div>
        </div>
      </div>
      <div class="section-title">Form pemeriksaan<span class="pill">Tahap ${tahap}</span></div>
      <div class="card">
        <div class="form-group">
          <label class="form-label">Kode dan nama diagnosa (ICD-10)</label>
          <select class="select-field" .value=${this.kodeIcd} @change=${(event: Event) => { this.kodeIcd = nilai(event); }}>
            ${ICD.map((item) => html`<option value=${item.kode}>${item.kode} - ${item.nama}</option>`)}
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Anamnesa dan catatan fisik</label>
          <textarea class="textarea-field" .value=${this.catatan} @input=${(event: Event) => { this.catatan = nilai(event); }}></textarea>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Tindakan</label>
            <select class="select-field" .value=${this.tindakan} @change=${(event: Event) => { this.tindakan = nilai(event); }}>
              ${TINDAKAN.map((item) => html`<option value=${item}>${item}</option>`)}
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Tanggal kontrol</label>
            <input class="input-field" .value=${this.kontrol} @input=${(event: Event) => { this.kontrol = nilai(event); }} />
          </div>
        </div>
        <hr class="line" />
        <div class="form-group">
          <label class="form-label">E-resep dari master obat</label>
          <div class="form-row">
            <select class="select-field" .value=${String(this.idObat)} @change=${(event: Event) => { this.idObat = Number(nilai(event)); }}>
              ${mediflow.obatAktif().map((obat) => html`<option value=${obat.id_obat}>${obat.nama_obat} • stok ${obat.stok}</option>`)}
            </select>
            <input class="input-field" type="number" min="1" .value=${String(this.jumlahObat)} @input=${(event: Event) => { this.jumlahObat = Number(nilai(event)); }} />
          </div>
          <div class="form-row" style="margin-top:8px;">
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
        ${this.renderDaftarObat(kunjungan, true)}
        <button class="btn-primary" id="btn-dokter" @click=${() => this.simpanDanMajuDokter(kunjungan)}>${labelTombol}</button>
        <p class="hint">
          ${tahap === 4
            ? 'Pasien sedang diperiksa di ruang poli Anda.'
            : tahap === 5
              ? 'Kirim meneruskan resep ke instalasi farmasi tanpa menuliskan nama obat di WhatsApp.'
              : `Tahap saat ini dikelola pada langkah ${tahap}.`}
        </p>
      </div>
    `;
  }

  private renderAdmin(kunjungan: Kunjungan) {
    const tahap = kunjungan.pendaftaran.tahap_alur;
    const item = langkah(tahap);
    const selesai = tahap >= 10;
    return html`
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
            <label class="form-label">Berat</label>
            <input class="input-field" .value=${this.berat} @input=${(event: Event) => { this.berat = nilai(event); }} />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Loket farmasi</label>
          <select class="select-field" .value=${this.loket} @change=${(event: Event) => { this.loket = nilai(event); }}>
            ${LOKET.map((loket) => html`<option value=${loket}>${loket}</option>`)}
          </select>
        </div>
        <button
          class="btn-outline"
          style="margin-top:0;"
          @click=${() => {
            const hasil = mediflow.simpanVital(kunjungan.pendaftaran.id_pendaftaran, this.tensi, this.suhu, this.berat, this.loket);
            this.hasil(hasil.ok, 'Tanda vital dan loket diperbarui.', hasil.ok ? '' : hasil.error);
          }}
        >Update data tanda vital dan loket</button>
      </div>
      <div class="section-title">Kontrol ACC alur<span class="pill">Tahap ${tahap}/10</span></div>
      <div class="card">
        <p class="hint">${item.judul} — ${item.deskripsi}</p>
        <button
          class="btn-primary"
          id="btn-admin"
          ?disabled=${selesai}
          @click=${() => {
            const hasil = mediflow.majuTahap(kunjungan.pendaftaran.id_pendaftaran);
            this.hasil(hasil.ok, 'Tahap dilanjutkan.', hasil.ok ? '' : hasil.error);
          }}
        >${selesai ? 'Seluruh 10 tahap selesai' : `Beri ACC (${item.pemilikNama}) → tahap ${Math.min(tahap + 1, 10)}`}</button>
        <button
          class="btn-outline"
          @click=${() => {
            mediflow.resetDemo();
            this.formKunci = '';
            this.selectedId = 0;
            this.kabar('Data demo dikembalikan ke kunjungan tahap 4.');
          }}
        >Reset simulasi ke data awal</button>
      </div>
      ${this.renderJejak()}
    `;
  }

  private renderApoteker(kunjungan: Kunjungan) {
    const resep = kunjungan.resep;
    const tahap = kunjungan.pendaftaran.tahap_alur;
    return html`
      <div class="section-title">
        Antrean farmasi
        <span>${resep ? labelStatusResep(resep.status_resep) : 'Belum ada resep'}</span>
      </div>
      <div class="card">
        <div class="info-grid">
          <div class="info-item"><small>Pasien</small><strong>${kunjungan.pasienNama}</strong></div>
          <div class="info-item"><small>Resep</small><strong>${resep?.nomor_resep ?? '-'}</strong></div>
          <div class="info-item"><small>Loket</small><strong>${kunjungan.pendaftaran.loket}</strong></div>
          <div class="info-item"><small>Estimasi racik</small><strong>${formatJam(resep?.estimasi_selesai)}</strong></div>
        </div>
        ${this.renderDaftarObat(kunjungan, false)}
        <button
          class="btn-primary"
          ?disabled=${tahap < 7 || tahap >= 9}
          @click=${() => {
            const hasil = mediflow.majuTahap(kunjungan.pendaftaran.id_pendaftaran);
            this.hasil(hasil.ok, 'Status farmasi diperbarui.', hasil.ok ? '' : hasil.error);
          }}
        >${tahap < 7 ? 'Menunggu kasir dan pilihan pasien' : tahap === 7 ? 'Selesaikan racik, lanjut QC' : tahap === 8 ? 'QC selesai, panggil pasien' : 'Obat sudah dipanggil'}</button>
      </div>
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
        <p class="hint">QR yang kedaluwarsa atau sudah dipindai ditolak. Pasien tanpa ponsel dapat dilayani admin lewat jalur loket manual.</p>
      </div>
      <div class="section-title">Stok obat<span>Log setiap perubahan</span></div>
      <div class="card">
        ${mediflow.obatAktif().map(
          (obat) => html`
            <div class="med-item">
              <div>
                <h4>${obat.nama_obat}</h4>
                <p>${obat.bentuk_kekuatan} • minimum ${obat.stok_minimum} • ${obat.jenis_obat}${obat.nomor_batch ? ` • batch ${obat.nomor_batch}` : ''}</p>
              </div>
              <div class="med-side">
                <input
                  class="input-field"
                  style="width:64px;"
                  type="number"
                  .value=${this.stokDraft[obat.id_obat] ?? String(obat.stok)}
                  @input=${(event: Event) => {
                    this.stokDraft = { ...this.stokDraft, [obat.id_obat]: nilai(event) };
                  }}
                />
                <button
                  class="btn-small"
                  @click=${() => {
                    const hasil = mediflow.sesuaikanStok(obat.id_obat, Number(this.stokDraft[obat.id_obat] ?? obat.stok), 'Hitung ulang stok fisik');
                    this.hasil(hasil.ok, `Stok ${obat.nama_obat} disesuaikan.`, hasil.ok ? '' : hasil.error);
                  }}
                >Simpan</button>
              </div>
            </div>
          `
        )}
      </div>
      ${this.renderJejak()}
    `;
  }

  private renderDaftarObat(kunjungan: Kunjungan, bolehHapus: boolean) {
    if (kunjungan.detail.length === 0) return html`<p class="hint">Belum ada item resep.</p>`;
    return html`
      ${kunjungan.detail.map((item) => {
        const bayar = kunjungan.pendaftaran.jenis_penjamin === 'umum' || !item.obat.cover_bpjs;
        return html`
          <div class="med-item">
            <div>
              <h4>${item.obat.nama_obat}</h4>
              <p>${item.aturan_pakai}${item.instruksi_racikan ? ` • ${item.instruksi_racikan}` : ''}</p>
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
    const tahap = kunjungan?.pendaftaran.tahap_alur ?? 0;
    return html`
      <div class="section-title">
        10 tahap alur antrean dan farmasi
        <span>${tahap * 10}% selesai</span>
      </div>
      <div class="card">
        ${LANGKAH.map((item) => {
          const kelas = item.tahap < tahap ? 'done' : item.tahap === tahap ? 'current' : '';
          return html`
            <div class="step-item ${kelas}">
              <div class="step-dot">${item.tahap < tahap ? '✓' : item.tahap}</div>
              <div class="step-body">
                <h4>${item.judul}</h4>
                <p>${item.deskripsi}</p>
                <span class="owner-tag">Otoritas ACC: ${item.pemilikNama}</span>
              </div>
            </div>
          `;
        })}
      </div>
    `;
  }

  private renderResep(kunjungan: Kunjungan | null, role: Role) {
    if (!kunjungan) return html`<div class="card"><p class="hint">Pilih kunjungan untuk melihat e-resep.</p></div>`;
    return html`
      <div class="section-title">
        Daftar obat e-resep
        <span class="pill">${kunjungan.pendaftaran.loket}</span>
      </div>
      <div class="card">
        <p class="hint">${kunjungan.resep ? `${kunjungan.resep.nomor_resep} • ${labelStatusResep(kunjungan.resep.status_resep)}` : 'Resep belum dibuat.'}</p>
        ${this.renderDaftarObat(kunjungan, role === 'dokter' && kunjungan.pendaftaran.tahap_alur <= 5)}
      </div>
      ${role === 'pasien' ? html`${this.renderPilihan(kunjungan)}${this.renderQr(kunjungan)}${this.renderPesan()}` : nothing}
    `;
  }

  private renderJejak() {
    const db = mediflow.snapshot();
    return html`
      <div class="section-title">Jejak stok dan akses<span>${db.audit_log.length} audit</span></div>
      <div class="card">
        ${mediflow.logStokTerbaru().map((log) => {
          const obat = db.obat.find((item) => item.id_obat === log.id_obat);
          return html`<div class="med-item"><div><h4>${obat?.nama_obat ?? 'Obat'} • ${log.jenis}</h4><p>${log.stok_sebelum} → ${log.stok_sesudah}. ${log.keterangan}</p></div></div>`;
        })}
        ${mediflow.auditTerbaru().map(
          (log) => html`<div class="med-item"><div><h4>${log.aksi}</h4><p>${log.entitas} #${log.id_entitas} • ${log.keterangan}</p></div></div>`
        )}
        ${db.log_stok.length === 0 && db.audit_log.length === 0 ? html`<p class="hint">Belum ada jejak.</p>` : nothing}
      </div>
    `;
  }

  private renderNav(role: Role) {
    const utama = role === 'pasien' ? 'Data Saya' : role === 'dokter' ? 'Form Medis' : role === 'apoteker' ? 'Farmasi' : 'Kontrol';
    const item = (id: 'main' | 'alur' | 'resep', ikon: string, label: string) => html`
      <button class="nav-item ${this.tab === id ? 'active' : ''}" @click=${() => { this.tab = id; }}>
        <span>${ikon}</span><span>${label}</span>
      </button>
    `;
    return html`
      <nav class="bottom-nav">
        ${item('main', '📝', utama)}
        ${item('alur', '📋', '10 Tahap')}
        ${item('resep', '💊', 'E-Resep')}
      </nav>
    `;
  }
}
