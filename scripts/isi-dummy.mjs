import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { createServer } from 'vite';

/**
 * Mengisi medicflow_db dengan data uji dari src/mediflow/seed.ts.
 * Seluruh isi tabel diganti. Skema harus sudah ada (db/schema.sql).
 */

function memoryStorage() {
  const data = new Map();
  return {
    getItem: (key) => (data.has(key) ? data.get(key) : null),
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: (key) => data.delete(key),
    clear: () => data.clear(),
    key: (index) => [...data.keys()][index] ?? null,
    get length() {
      return data.size;
    },
  };
}

globalThis.localStorage = memoryStorage();
globalThis.sessionStorage = memoryStorage();

const TABEL = [
  'users',
  'poliklinik',
  'obat',
  'pasien',
  'dokter',
  'jadwal_dokter',
  'pendaftaran_poli',
  'pemeriksaan',
  'resep',
  'detail_resep',
  'notifikasi',
  'otp_verifikasi',
  'pengambilan_obat',
  'mutasi_stok',
  'log_aktivitas',
];

function sqlLiteral(value) {
  if (value === null || value === undefined) return 'NULL';
  if (typeof value === 'boolean') return value ? '1' : '0';
  if (typeof value === 'number') return Number.isFinite(value) ? String(value) : 'NULL';
  let text = String(value);
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(text)) text = text.slice(0, 19).replace('T', ' ');
  return `'${text.replace(/\\/g, '\\\\').replace(/'/g, "''")}'`;
}

function insert(table, rows) {
  if (!rows.length) return '';
  const kolom = Object.keys(rows[0]);
  const values = rows
    .map((row) => `(${kolom.map((nama) => sqlLiteral(row[nama])).join(',')})`)
    .join(',\n');
  return `INSERT INTO \`${table}\` (${kolom.map((nama) => `\`${nama}\``).join(',')}) VALUES\n${values};\n`;
}

function mysqlBin() {
  const calon = [process.env.MYSQL_BIN, 'C:\\xampp\\mysql\\bin\\mysql.exe', 'D:\\xampp\\mysql\\bin\\mysql.exe'].filter(Boolean);
  const ketemu = calon.find((path) => existsSync(path));
  if (!ketemu) throw new Error('mysql.exe tidak ditemukan. Set MYSQL_BIN atau pasang XAMPP.');
  return ketemu;
}

function mysql(sql) {
  const host = process.env.MEDIFLOW_DB_HOST || '127.0.0.1';
  const user = process.env.MEDIFLOW_DB_USER || 'root';
  const password = process.env.MEDIFLOW_DB_PASSWORD ?? '';
  const database = process.env.MEDIFLOW_DB_NAME || 'medicflow_db';
  const args = ['-h', host, '-u', user, '--default-character-set=utf8mb4', database];
  if (password) args.splice(4, 0, `-p${password}`);
  return new Promise((resolve, reject) => {
    const child = spawn(mysqlBin(), args, { stdio: ['pipe', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => {
      stdout += chunk;
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk;
    });
    child.on('error', reject);
    child.on('close', (code) => {
      if (code !== 0) reject(new Error(stderr.trim() || stdout.trim() || `mysql berhenti dengan kode ${code}`));
      else resolve(stdout.trim());
    });
    child.stdin.end(sql);
  });
}

const server = await createServer({
  server: { middlewareMode: true },
  appType: 'custom',
  logLevel: 'error',
});

try {
  const modul = await server.ssrLoadModule('/src/mediflow/seed.ts');
  const db = modul.createDatabase(new Date());
  const hariIni = db.pendaftaran_poli.find((row) => row.id_pendaftaran === 15)?.tanggal_kunjungan ?? '';
  const sql = [
    'SET NAMES utf8mb4;',
    'START TRANSACTION;',
    'SET FOREIGN_KEY_CHECKS = 0;',
    ...[...TABEL].reverse().map((tabel) => `DELETE FROM \`${tabel}\`;`),
    ...TABEL.map((tabel) => insert(tabel, db[tabel])),
    'SET FOREIGN_KEY_CHECKS = 1;',
    'COMMIT;',
  ].join('\n');
  await mysql(sql);
  const laporan = await mysql(`
    SELECT 'users', COUNT(*) FROM users
    UNION ALL SELECT 'pasien', COUNT(*) FROM pasien
    UNION ALL SELECT 'dokter', COUNT(*) FROM dokter
    UNION ALL SELECT 'jadwal', COUNT(*) FROM jadwal_dokter
    UNION ALL SELECT 'obat', COUNT(*) FROM obat
    UNION ALL SELECT 'kunjungan', COUNT(*) FROM pendaftaran_poli
    UNION ALL SELECT 'resep', COUNT(*) FROM resep
    UNION ALL SELECT 'hari_ini', COUNT(*) FROM pendaftaran_poli WHERE tanggal_kunjungan = '${hariIni}';
    SELECT p.nomor_antrean, ps.nama_lengkap, p.status_antrean, IFNULL(r.status_resep, '-'), IFNULL(r.kode_qr_unik, '-')
    FROM pendaftaran_poli p
    JOIN pasien ps ON ps.id_pasien = p.id_pasien
    LEFT JOIN resep r ON r.id_pendaftaran = p.id_pendaftaran
    WHERE p.tanggal_kunjungan = '${hariIni}'
    ORDER BY p.nomor_antrean;
    SELECT r.kode_qr_unik, o.kode_otp, r.kadaluarsa_qr = o.expired_at
    FROM resep r
    JOIN pendaftaran_poli p ON p.id_pendaftaran = r.id_pendaftaran
    JOIN pasien ps ON ps.id_pasien = p.id_pasien
    JOIN otp_verifikasi o ON o.id_user = ps.id_user AND o.expired_at = r.kadaluarsa_qr
    WHERE r.kode_qr_unik = 'MF-SITI-SIAP';
  `);
  console.log(laporan);
  console.log('Data uji tersimpan di medicflow_db.');
} finally {
  await server.close();
}
