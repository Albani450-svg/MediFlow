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
    nama: 'Budi Santoso Edit • budi',
    judul: 'Pasien',
    deskripsi: 'Daftar poli, lihat giliran, pilih apotek, dan tunjukkan kode pengambilan',
    ikon: '👤',
  },
  {
    role: 'dokter',
    username: 'dokter01',
    password: 'dokter123',
    nama: 'Dr. Ahmad • dokter01',
    judul: 'Dokter',
    deskripsi: 'Periksa pasien, tulis diagnosis, dan kirim resep ke farmasi',
    ikon: '🩺',
  },
  {
    role: 'admin',
    username: 'admin',
    password: 'admin123',
    nama: 'Administrator • admin',
    judul: 'Admin & Perawat',
    deskripsi: 'Catat tanda vital, panggil pasien, dan selesaikan pembayaran',
    ikon: '🛡️',
  },
  {
    role: 'farmasi',
    username: 'farmasi01',
    password: 'apotek123',
    nama: 'Petugas Farmasi • farmasi01',
    judul: 'Farmasi',
    deskripsi: 'Siapkan obat, cek ulang, lalu serahkan setelah kode pasien cocok',
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
  @state() private sumber: 'mysql' | 'peramban' | 'memuat' = 'memuat';

  connectedCallback(): void {
    super.connectedCallback();
    void mediflow.siap.then(() => {
      if (!this.isConnected) return;
      this.sumber = mediflow.sumber();
      if (mediflow.session()) go('/app');
    });
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
            <p>Satu kunjungan, dari daftar sampai obat diambil.</p>
            <ol class="login-flow">
              <li><b>1. Daftar</b>Pasien pilih poli dan datang ke rumah sakit.</li>
              <li><b>2. Periksa</b>Dokter memeriksa dan menulis resep.</li>
              <li><b>3. Resep</b>Pasien pilih apotek, lalu obat disiapkan.</li>
              <li><b>4. Ambil</b>Tunjukkan kode di aplikasi ini ke loket.</li>
            </ol>
          </div>
          <div class="login-card">
            <h2>Masuk sesuai tugas Anda</h2>
            <p class="sub">Pilih peran. Setiap akun hanya melihat pekerjaan bagiannya.</p>
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
            <p class="hint">
              ${this.sumber === 'mysql'
                ? 'Akun, poli, jadwal, dan stok obat mengikuti database medicflow_db.'
                : this.sumber === 'memuat'
                  ? 'Menghubungi database medicflow_db...'
                  : 'Database belum terjangkau. Data demo tersimpan di peramban ini.'}
            </p>
          </div>
        </div>
      </section>
    `;
  }
}
