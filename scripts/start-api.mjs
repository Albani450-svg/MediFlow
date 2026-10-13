import { spawn, spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, readdirSync } from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const API_PORT = 8088;

const akar = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const apiDir = path.join(akar, 'api');
const srcDir = path.join(apiDir, 'src');
const outDir = path.join(apiDir, 'out');
const libDir = path.join(apiDir, 'lib');

function berkasJava(nama) {
  const file = process.platform === 'win32' ? `${nama}.exe` : nama;
  if (process.env.JAVA_HOME) {
    const calon = path.join(process.env.JAVA_HOME, 'bin', file);
    if (existsSync(calon)) return calon;
  }
  return file;
}

function sumber(dir) {
  const hasil = [];
  for (const entri of readdirSync(dir, { withFileTypes: true })) {
    const penuh = path.join(dir, entri.name);
    if (entri.isDirectory()) hasil.push(...sumber(penuh));
    else if (entri.name.endsWith('.java')) hasil.push(penuh);
  }
  return hasil;
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

function susun() {
  if (!existsSync(libDir)) {
    console.warn('[mediflow] Pustaka Java belum ada di api/lib.');
    return false;
  }
  mkdirSync(outDir, { recursive: true });
  const cp = path.join(libDir, '*');
  const hasil = spawnSync(
    berkasJava('javac'),
    ['--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', outDir, ...sumber(srcDir)],
    { cwd: apiDir, stdio: 'inherit' }
  );
  if (hasil.error) {
    console.warn('[mediflow] Java tidak ditemukan. Pasang JDK 17 atau lebih baru, atau set JAVA_HOME.');
    return false;
  }
  return hasil.status === 0;
}

export async function pastikanApi() {
  if (await portTerbuka(API_PORT)) return true;
  if (!susun()) return false;
  const cp = [outDir, path.join(libDir, '*')].join(path.delimiter);
  const anak = spawn(berkasJava('java'), ['-cp', cp, 'mediflow.ApiServer'], {
    cwd: apiDir,
    stdio: 'inherit',
    windowsHide: true,
  });
  anak.on('error', (error) => console.warn('[mediflow] API Java gagal dijalankan:', error.message));
  for (let i = 0; i < 40; i += 1) {
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
  });
}
