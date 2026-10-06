import { LitElement, html } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import type { Role } from '../../mediflow/types';
import { mediflow } from '../../mediflow/store';
import { go } from '../../router';
import { mediflowStyles } from '../../styles/mediflow-styles';

interface AkunDemo {
  role: Role;
  username: string;
  password: string;
  nama: string;
  judul: string;
  deskripsi: string;
  ikon: string;
}

const AKUN: AkunDemo[] = [
  {
    role: 'pasien',
    username: 'budi',
    password: 'pasien123',
    nama: 'Budi Santoso (RM-0049281)',
    judul: 'Pasien (User)',
    deskripsi: 'Daftar poli, pantau 10 tahap, pilih tebus obat, dan tunjukkan QR',
    ikon: '👤',
  },
  {
    role: 'dokter',
    username: 'hendra',
    password: 'dokter123',
    nama: 'dr. Hendra Wijaya, Sp.PD',
    judul: 'Dokter Pemeriksa',
    deskripsi: 'Isi anamnesa, kode ICD-10, tindakan, jadwal kontrol, dan e-resep',
    ikon: '🩺',
  },
  {
    role: 'admin',
    username: 'siti',
    password: 'admin123',
    nama: 'Siti Rahma, A.Md.Kep / Admin',
    judul: 'Admin & Perawat',
    deskripsi: 'Input tanda vital, verifikasi BPJS, dan ACC tahapan loket',
    ikon: '🛡️',
  },
  {
    role: 'apoteker',
    username: 'rina',
    password: 'apotek123',
    nama: 'apt. Rina Kusuma (SIPA)',
    judul: 'Apoteker',
    deskripsi: 'Racik obat, cek stok, pindai QR, dan serahkan ke pasien atau keluarga',
    ikon: '💊',
  },
];

@customElement('app-login')
export class AppLogin extends LitElement {
  static styles = mediflowStyles;

  @state() private peran: Role = 'pasien';
  @state() private password = AKUN[0].password;
  @state() private error = '';
  @state() private loading = false;

  connectedCallback(): void {
    super.connectedCallback();
    if (mediflow.session()) go('/app');
  }

  private akun(): AkunDemo {
    return AKUN.find((item) => item.role === this.peran) ?? AKUN[0];
  }

  private pilih(role: Role): void {
    const akun = AKUN.find((item) => item.role === role) ?? AKUN[0];
    this.peran = role;
    this.password = akun.password;
    this.error = '';
  }

  private async masuk(): Promise<void> {
    this.loading = true;
    this.error = '';
    const hasil = await mediflow.login(this.akun().username, this.password);
    this.loading = false;
    if (!hasil.ok) {
      this.error = hasil.error;
      return;
    }
    go('/app');
  }

  render() {
    const akun = this.akun();
    return html`
      <section class="login-screen">
        <div class="login-panel">
          <div class="login-brand">
            <div class="login-logo">+</div>
            <h1>MediFlow</h1>
            <p>Sistem Rekam Medis, Antrean Poli & E-Resep</p>
          </div>
          <div class="login-card">
            <h2>Pilih Peran Akses Login</h2>
            <p class="sub">Setiap peran memiliki form isian dan hak akses berbeda</p>
            ${AKUN.map(
              (item) => html`
                <button
                  class="role-option ${item.role === this.peran ? 'selected' : ''}"
                  @click=${() => this.pilih(item.role)}
                >
                  <span class="role-icon">${item.ikon}</span>
                  <span class="role-info">
                    <h3>${item.judul}</h3>
                    <p>${item.deskripsi}</p>
                  </span>
                </button>
              `
            )}
            <div class="form-group">
              <label class="form-label">Akun terpilih</label>
              <input class="input-field" readonly .value=${akun.nama} />
            </div>
            <div class="form-group">
              <label class="form-label">Kata sandi</label>
              <input
                class="input-field"
                type="password"
                .value=${this.password}
                @input=${(event: Event) => {
                  this.password = (event.target as HTMLInputElement).value;
                }}
              />
            </div>
            ${this.error ? html`<p class="notice error">${this.error}</p>` : ''}
            <button class="btn-primary" id="btn-login" ?disabled=${this.loading} @click=${() => this.masuk()}>
              ${this.loading ? 'Memeriksa...' : 'Masuk ke Sistem →'}
            </button>
            <p class="hint">Demo lokal memakai kata sandi yang sudah terisi. Data tersimpan di peramban ini.</p>
          </div>
        </div>
      </section>
    `;
  }
}
