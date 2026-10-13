var CACHE = 'mediflow-v1';
var BERKAS = [
  '/mediflow.css',
  '/app.js',
  '/manifest.json',
  '/assets/icons/24x24.png',
  '/assets/icons/48x48.png',
  '/assets/icons/192x192.png',
  '/assets/icons/512x512.png'
];

self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(CACHE).then(function (cache) {
      return cache.addAll(BERKAS);
    }).then(function () {
      return self.skipWaiting();
    })
  );
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (kunci) {
      return Promise.all(kunci.filter(function (nama) {
        return nama !== CACHE;
      }).map(function (nama) {
        return caches.delete(nama);
      }));
    }).then(function () {
      return self.clients.claim();
    })
  );
});

self.addEventListener('fetch', function (event) {
  var url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== self.location.origin) return;
  if (url.pathname === '/' || url.pathname === '/app' || url.pathname === '/masuk' || url.pathname.indexOf('/api') === 0) {
    return;
  }
  event.respondWith(
    caches.match(event.request).then(function (temuan) {
      return temuan || fetch(event.request);
    })
  );
});

self.addEventListener('message', function (event) {
  var data = event.data || {};
  if (data.type !== 'mediflow-notify' || !self.registration || !self.registration.showNotification) return;
  event.waitUntil(
    self.registration.showNotification(data.title || 'MediFlow', {
      body: data.body || '',
      icon: '/assets/icons/192x192.png',
      badge: '/assets/icons/48x48.png',
      data: { url: data.url || '/app' }
    })
  );
});

self.addEventListener('notificationclick', function (event) {
  event.notification.close();
  var target = new URL((event.notification.data && event.notification.data.url) || '/app', self.location.origin).href;
  event.waitUntil((async function () {
    var terbuka = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    for (var i = 0; i < terbuka.length; i++) {
      var client = terbuka[i];
      if (!('focus' in client)) continue;
      await client.focus();
      client.postMessage({ type: 'mediflow-open', url: target });
      return;
    }
    await self.clients.openWindow(target);
  })());
});
