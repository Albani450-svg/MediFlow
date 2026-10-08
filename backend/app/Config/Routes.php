<?php

use CodeIgniter\Router\RouteCollection;

/**
 * @var RouteCollection $routes
 */
$routes->get('/', 'Home::index');

$routes->set404Override(static function (?string $pesan = null) {
    $path = trim((string) service('uri')->getPath(), '/');

    if (str_starts_with($path, 'api')) {
        service('response')->setContentType('application/json');

        return json_encode([
            'status' => false,
            'message' => 'Jalur API tidak ditemukan.',
        ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }

    return view('errors/html/error_404', [
        'message' => $pesan ?? 'Halaman tidak ditemukan.',
    ]);
});

$routes->group('api', ['namespace' => 'App\Controllers\Api', 'filter' => 'lokal'], static function (RouteCollection $routes): void {
    $routes->get('database', 'DatabaseController::index');
    $routes->match(['put', 'post'], 'database', 'DatabaseController::update');

    $peta = [
        'users' => 'UserController',
        'pasien' => 'PasienController',
        'poliklinik' => 'PoliklinikController',
        'dokter' => 'DokterController',
        'jadwal_dokter' => 'JadwalDokterController',
        'obat' => 'ObatController',
        'pendaftaran_poli' => 'PendaftaranPoliController',
        'pemeriksaan' => 'PemeriksaanController',
        'resep' => 'ResepController',
        'detail_resep' => 'DetailResepController',
        'notifikasi' => 'NotifikasiController',
        'otp_verifikasi' => 'OtpVerifikasiController',
        'pengambilan_obat' => 'PengambilanObatController',
        'mutasi_stok' => 'MutasiStokController',
        'log_aktivitas' => 'LogAktivitasController',
    ];

    foreach ($peta as $jalur => $kontrol) {
        $routes->get($jalur, $kontrol . '::index');
        $routes->get($jalur . '/(:num)', $kontrol . '::show/$1');
        $routes->post($jalur, $kontrol . '::create');
        $routes->match(['put', 'patch'], $jalur . '/(:num)', $kontrol . '::update/$1');
        $routes->delete($jalur . '/(:num)', $kontrol . '::delete/$1');
    }
});
