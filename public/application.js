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

  var registrasi = document.getElementById('registrasi-pasien');
  var bukaRegistrasi = document.getElementById('buka-registrasi');
  var tutupRegistrasi = document.getElementById('tutup-registrasi');
  var bukaLoginPasien = document.getElementById('buka-login-pasien');
  var loginPasien = document.getElementById('login-pasien');
  var pesanRegistrasi = document.getElementById('registrasi-pesan');
  var emailRegistrasi = '';

  function tampilkanPesanRegistrasi(teks, gagal) {
    if (!pesanRegistrasi) return;
    pesanRegistrasi.hidden = false;
    pesanRegistrasi.className = gagal ? 'notice error' : 'notice';
    pesanRegistrasi.textContent = teks;
  }

  function dataJson(email) {
    return {
      nama_lengkap: document.getElementById('reg-nama').value.trim(),
      email: email || document.getElementById('reg-email').value.trim(),
      no_telepon: document.getElementById('reg-wa').value.trim(),
      password: document.getElementById('reg-password').value
    };
  }

  function kirimRegistrasi(url, data, tombol, selesai) {
    tombol.disabled = true;
    fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(data)
    }).then(function (response) {
      return response.json().then(function (body) {
        if (!response.ok || !body.status) throw new Error(body.message || 'Registrasi gagal.');
        return body;
      });
    }).then(selesai).catch(function (error) {
      tampilkanPesanRegistrasi(error.message || 'Layanan registrasi tidak dapat dijangkau.', true);
    }).finally(function () {
      tombol.disabled = false;
    });
  }

  if (registrasi && bukaRegistrasi) {
    if (bukaLoginPasien && loginPasien) {
      bukaLoginPasien.addEventListener('click', function () {
        loginPasien.hidden = false;
        bukaLoginPasien.hidden = true;
        document.getElementById('akun-login').focus();
      });
    }
    bukaRegistrasi.addEventListener('click', function () {
      registrasi.hidden = !registrasi.hidden;
      bukaRegistrasi.textContent = registrasi.hidden ? 'Daftar sebagai pasien baru' : 'Tutup registrasi';
      if (!registrasi.hidden) document.getElementById('reg-nama').focus();
    });
    tutupRegistrasi.addEventListener('click', function () {
      registrasi.hidden = true;
      bukaRegistrasi.textContent = 'Daftar sebagai pasien baru';
    });

    document.getElementById('kirim-registrasi').addEventListener('click', function (event) {
      var data = dataJson('');
      if (!data.nama_lengkap || !data.email || !data.no_telepon || data.password.length < 10) {
        tampilkanPesanRegistrasi('Lengkapi nama, email, nomor WhatsApp, dan kata sandi minimal 10 karakter.', true);
        return;
      }
      emailRegistrasi = data.email;
      kirimRegistrasi('/api/auth/register', data, event.currentTarget, function (body) {
        document.getElementById('registrasi-langkah-1').hidden = true;
        document.getElementById('registrasi-langkah-2').hidden = false;
        tampilkanPesanRegistrasi(body.message, false);
        document.getElementById('reg-otp').focus();
      });
    });

    document.getElementById('verifikasi-registrasi').addEventListener('click', function (event) {
      var kode = document.getElementById('reg-otp').value.trim();
      if (!/^[0-9]{6}$/.test(kode)) {
        tampilkanPesanRegistrasi('Masukkan kode OTP enam digit.', true);
        return;
      }
      kirimRegistrasi('/api/auth/register/verify-otp', {
        email: emailRegistrasi,
        kode_otp: kode
      }, event.currentTarget, function (body) {
        document.getElementById('registrasi-langkah-2').hidden = true;
        document.getElementById('registrasi-langkah-3').hidden = false;
        tampilkanPesanRegistrasi(body.message, false);
      });
    });

    document.getElementById('lengkapi-registrasi').addEventListener('click', function (event) {
      var profil = {
        email: emailRegistrasi,
        nik: document.getElementById('reg-nik').value.trim(),
        tanggal_lahir: document.getElementById('reg-tanggal').value,
        jenis_kelamin: document.getElementById('reg-jk').value,
        nomor_bpjs: document.getElementById('reg-bpjs').value.trim(),
        alamat: document.getElementById('reg-alamat').value.trim()
      };
      if (!/^[0-9]{16}$/.test(profil.nik) || !profil.tanggal_lahir || !profil.jenis_kelamin) {
        tampilkanPesanRegistrasi('Isi NIK 16 digit, tanggal lahir, dan jenis kelamin.', true);
        return;
      }
      kirimRegistrasi('/api/auth/register/profile', profil, event.currentTarget, function (body) {
        document.getElementById('registrasi-langkah-3').hidden = true;
        document.getElementById('akun-login').value = emailRegistrasi;
        document.getElementById('sandi-login').value = document.getElementById('reg-password').value;
        if (bukaLoginPasien) bukaLoginPasien.click();
        var waLogin = document.querySelector('input[name="wa"]');
        if (waLogin) waLogin.value = document.getElementById('reg-wa').value.trim();
        tampilkanPesanRegistrasi(body.message + ' Nomor rekam medis: ' + body.data.no_rekam_medis, false);
      });
    });
  }

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
      navigator.serviceWorker.register('/service-worker.js').catch(function () {});
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