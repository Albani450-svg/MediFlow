import type { Database } from './types';

const URL_API = '/api/database';

export function databaseUtuh(data: unknown): data is Database {
  if (!data || typeof data !== 'object') return false;
  const db = data as Partial<Database>;
  return (
    Array.isArray(db.users) &&
    Array.isArray(db.pasien) &&
    Array.isArray(db.obat) &&
    Array.isArray(db.poliklinik) &&
    Array.isArray(db.pendaftaran_poli) &&
    Array.isArray(db.resep)
  );
}

export async function ambilDatabase(): Promise<Database | null> {
  for (let percobaan = 0; percobaan < 8; percobaan += 1) {
    try {
      const respon = await fetch(URL_API, {
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(2000),
      });
      if (respon.ok) {
        const body = (await respon.json()) as { ok?: boolean; data?: unknown };
        return body.ok && databaseUtuh(body.data) ? body.data : null;
      }
      if (respon.status < 500 && respon.status !== 404) return null;
    } catch {
      // API belum siap atau MySQL sedang mati.
    }
    await new Promise((selesai) => setTimeout(selesai, 250));
  }
  return null;
}

export async function simpanDatabase(db: Database): Promise<boolean> {
  if (!db.users.length) return false;
  const respon = await fetch(URL_API, {
    method: 'PUT',
    keepalive: true,
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(db),
  });
  if (!respon.ok) return false;
  const body = (await respon.json()) as { ok?: boolean };
  return Boolean(body.ok);
}
