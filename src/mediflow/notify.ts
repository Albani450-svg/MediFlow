/** Notifikasi perangkat lewat service worker, hanya saat aplikasi tidak sedang dibuka. */
export async function notifyBrowser(title: string, body: string): Promise<void> {
  if (typeof window === 'undefined' || typeof Notification === 'undefined') return;
  if (Notification.permission !== 'granted') return;
  if (typeof document !== 'undefined' && document.visibilityState === 'visible') return;
  const sw = navigator.serviceWorker;
  if (!sw) return;
  const reg = await sw.ready;
  reg.active?.postMessage({
    type: 'mediflow-notify',
    title,
    body,
    url: '/app',
  });
}

export async function mintaIzinNotifikasi(): Promise<boolean> {
  if (typeof Notification === 'undefined') return false;
  if (Notification.permission === 'granted') return true;
  if (Notification.permission === 'denied') return false;
  const hasil = await Notification.requestPermission();
  return hasil === 'granted';
}
