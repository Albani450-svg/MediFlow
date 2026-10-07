importScripts(
  'https://storage.googleapis.com/workbox-cdn/releases/7.4.1/workbox-sw.js'
);

self.addEventListener('message', (event) => {
  const data = event.data || {};
  if (data.type !== 'mediflow-notify' || !self.registration?.showNotification) return;
  event.waitUntil(
    self.registration.showNotification(data.title || 'MediFlow', {
      body: data.body || '',
      icon: '/assets/icons/192x192.png',
      badge: '/assets/icons/48x48.png',
      data: { url: data.url || '/app' },
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = new URL(event.notification.data?.url || '/app', self.location.origin).href;
  event.waitUntil((async () => {
    const terbuka = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    for (const client of terbuka) {
      if (!('focus' in client)) continue;
      await client.focus();
      client.postMessage({ type: 'mediflow-open', url: target });
      return;
    }
    await self.clients.openWindow(target);
  })());
});

workbox.precaching.precacheAndRoute(self.__WB_MANIFEST || []);
