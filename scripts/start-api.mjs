import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const API_PORT = 8088;

const backend = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'backend');
const publik = path.join(backend, 'public');
const rewrite = path.join(backend, 'vendor', 'codeigniter4', 'framework', 'system', 'rewrite.php');

function phpBin() {
  if (process.env.PHP_BIN && existsSync(process.env.PHP_BIN)) return process.env.PHP_BIN;
  const calon = ['C:\\xampp\\php\\php.exe', 'D:\\xampp\\php\\php.exe'];
  return calon.find((berkas) => existsSync(berkas)) ?? null;
}

function portTerbuka(port) {
  return new Promise((selesai) => {
    const soket = net.connect({ port, host: '127.0.0.1' }, () => {
      soket.end();
      selesai(true);
    });
    soket.on('error', () => selesai(false));
  });
}

export async function pastikanApi() {
  if (await portTerbuka(API_PORT)) return true;
  const php = phpBin();
  if (!php) {
    console.warn('[mediflow] PHP tidak ditemukan. PWA memakai data di peramban.');
    return false;
  }
  if (!existsSync(rewrite)) {
    console.warn('[mediflow] CodeIgniter belum terpasang. Jalankan composer install di folder backend.');
    return false;
  }
  const anak = spawn(php, ['-S', `127.0.0.1:${API_PORT}`, '-t', publik, rewrite], {
    cwd: publik,
    stdio: 'inherit',
    windowsHide: true,
  });
  anak.on('error', (error) => console.warn('[mediflow] API PHP gagal dijalankan:', error.message));
  for (let i = 0; i < 25; i += 1) {
    if (await portTerbuka(API_PORT)) return true;
    await new Promise((selesai) => setTimeout(selesai, 150));
  }
  console.warn(`[mediflow] API belum menjawab di 127.0.0.1:${API_PORT}`);
  return false;
}

const dipanggilLangsung = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
if (dipanggilLangsung) {
  pastikanApi().then((hidup) => {
    if (!hidup) process.exit(1);
    console.log(`[mediflow] API CodeIgniter medicflow_db di http://127.0.0.1:${API_PORT}/api/database`);
  });
}
