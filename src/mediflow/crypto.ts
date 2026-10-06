/** Demo klien saja. Di produksi NIK dienkripsi di server, dan kata sandi memakai hash lambat (argon2/bcrypt). */

const NIK_SECRET = 'MediFlowNIKKey2026';

export function encryptNik(plain: string): string {
  const mixed = Array.from(plain)
    .map((ch, i) =>
      String.fromCharCode(ch.charCodeAt(0) ^ NIK_SECRET.charCodeAt(i % NIK_SECRET.length))
    )
    .join('');
  return `enc.v1.${btoa(mixed)}`;
}

export function decryptNik(payload: string): string {
  if (!payload.startsWith('enc.v1.')) return '';
  const raw = atob(payload.slice('enc.v1.'.length));
  return Array.from(raw)
    .map((ch, i) =>
      String.fromCharCode(ch.charCodeAt(0) ^ NIK_SECRET.charCodeAt(i % NIK_SECRET.length))
    )
    .join('');
}

export function maskNik(plain: string): string {
  if (plain.length < 8) return '••••';
  return `${plain.slice(0, 4)}••••••••${plain.slice(-4)}`;
}

export async function hashPassword(plain: string): Promise<string> {
  const data = new TextEncoder().encode(plain);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

export function randomToken(bytes = 16): string {
  const buf = new Uint8Array(bytes);
  crypto.getRandomValues(buf);
  return [...buf].map((b) => b.toString(16).padStart(2, '0')).join('');
}

export function randomPin(): string {
  const n = crypto.getRandomValues(new Uint32Array(1))[0] % 1000000;
  return String(n).padStart(6, '0');
}
