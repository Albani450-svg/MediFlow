(function () {
  var kunci = 'mediflow-sesi';
  var params = new URLSearchParams(location.search);
  var urlSesi = params.get('sesi');
  var simpan = sessionStorage.getItem(kunci);
  if (simpan && urlSesi !== simpan) {
    params.set('sesi', simpan);
    location.replace(location.pathname + '?' + params.toString() + location.hash);
    return;
  }
  if (urlSesi) sessionStorage.setItem(kunci, urlSesi);

  var sesi = sessionStorage.getItem(kunci) || '';
  document.querySelectorAll('form').forEach(function (form) {
    var input = form.querySelector('input[name="sesi"]');
    if (!input) {
      input = document.createElement('input');
      input.type = 'hidden';
      input.name = 'sesi';
      form.prepend(input);
    }
    if (!input.value) input.value = sesi;
  });

  document.querySelectorAll('.toast').forEach(function (el) {
    window.setTimeout(function () { el.remove(); }, 6000);
  });
  document.querySelectorAll('.wa-toast').forEach(function (el) {
    window.setTimeout(function () { el.remove(); }, 4000);
  });

  var izin = document.getElementById('izin-notif');
  if (izin) {
    var perbarui = function () {
      if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
        izin.textContent = 'Notifikasi perangkat aktif';
      }
    };
    perbarui();
    izin.addEventListener('click', function () {
      var selesai = function (boleh) {
        perbarui();
        var notice = document.querySelector('main .notice');
        if (!notice) {
          notice = document.createElement('div');
          var main = document.querySelector('main.container');
          if (!main) return;
          main.prepend(notice);
        }
        notice.className = boleh ? 'notice' : 'notice error';
        notice.textContent = boleh
          ? 'Perangkat siap menampilkan notifikasi saat aplikasi di latar.'
          : 'Izin notifikasi perangkat ditolak. Notifikasi tetap tampil di dalam aplikasi.';
      };
      if (typeof Notification === 'undefined' || !Notification.requestPermission) {
        selesai(false);
        return;
      }
      Notification.requestPermission().then(function (hasil) {
        selesai(hasil === 'granted');
      }).catch(function () { selesai(false); });
    });
  }

  if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
      navigator.serviceWorker.register('/sw.js').catch(function () {});
    });
    navigator.serviceWorker.addEventListener('message', function (event) {
      if (!event.data || event.data.type !== 'mediflow-open') return;
      var form = document.getElementById('buka-panel');
      if (form) form.submit();
    });
    var kabar = document.getElementById('kabar-baru');
    if (kabar) {
      var daftar = [];
      try { daftar = JSON.parse(kabar.textContent); } catch (error) { daftar = []; }
      navigator.serviceWorker.ready.then(function (reg) {
        var tujuan = reg.active || navigator.serviceWorker.controller;
        if (!tujuan) return;
        daftar.forEach(function (item) {
          tujuan.postMessage({
            type: 'mediflow-notify',
            title: item.judul,
            body: item.isi,
            url: location.pathname + location.search
          });
        });
      }).catch(function () {});
    }
  }
})();
