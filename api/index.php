<?php

declare(strict_types=1);

// Pengembangan memakai CodeIgniter di backend/. Server `npm run dev` tidak lagi membuka file ini.

ini_set('display_errors', '0');
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

require __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

$asal = $_SERVER['REMOTE_ADDR'] ?? '';
if (!in_array($asal, ['127.0.0.1', '::1'], true)) {
    http_response_code(403);
    echo json_encode(['ok' => false, 'error' => 'API hanya melayani komputer ini.'], JSON_UNESCAPED_UNICODE);
    exit;
}

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

/**
 * Tipe kolom mengikuti db/schema.sql.
 * dt = datetime, date = tanggal, time = jam, dec = decimal, bool = tinyint(1).
 *
 * @return array<string, array<string, string>>
 */
function skema(): array
{
    return [
        'users' => [
            'id_user' => 'int', 'username' => 'str', 'password_hash' => 'str', 'role' => 'str',
            'nama_lengkap' => 'str', 'no_telepon' => 'str?', 'is_active' => 'bool',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'poliklinik' => [
            'id_poli' => 'int', 'nama_poli' => 'str', 'deskripsi' => 'str?', 'is_active' => 'bool',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'obat' => [
            'id_obat' => 'int', 'kode_kemenkes' => 'str', 'nama_obat' => 'str', 'jenis_obat' => 'str',
            'satuan' => 'str', 'cover_bpjs' => 'bool', 'stok_rs' => 'int', 'stok_minimum' => 'int',
            'harga' => 'dec', 'is_active' => 'bool', 'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'pasien' => [
            'id_pasien' => 'int', 'id_user' => 'int', 'nik' => 'str', 'nomor_bpjs' => 'str?',
            'nama_lengkap' => 'str', 'tanggal_lahir' => 'date', 'jenis_kelamin' => 'str',
            'alamat' => 'str?', 'no_telepon' => 'str', 'terdaftar_satusehat' => 'bool',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'dokter' => [
            'id_dokter' => 'int', 'id_user' => 'int', 'id_poli' => 'int', 'nama_dokter' => 'str',
            'spesialisasi' => 'str?', 'rata_waktu_periksa_menit' => 'int', 'is_active' => 'bool',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'jadwal_dokter' => [
            'id_jadwal' => 'int', 'id_dokter' => 'int', 'hari' => 'str', 'jam_mulai' => 'time',
            'jam_selesai' => 'time', 'kuota_maksimal' => 'int', 'is_active' => 'bool',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'pendaftaran_poli' => [
            'id_pendaftaran' => 'int', 'id_pasien' => 'int', 'id_jadwal' => 'int',
            'tanggal_kunjungan' => 'date', 'nomor_antrean' => 'int', 'waktu_check_in' => 'dt?',
            'estimasi_jam_masuk' => 'dt?', 'status_antrean' => 'str', 'catatan_pasien' => 'str?',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'pemeriksaan' => [
            'id_pemeriksaan' => 'int', 'id_pendaftaran' => 'int', 'id_dokter' => 'int',
            'keluhan' => 'str', 'tekanan_darah' => 'str?', 'suhu_tubuh' => 'dec?',
            'berat_badan' => 'dec?', 'tinggi_badan' => 'dec?', 'diagnosis' => 'str',
            'tindakan' => 'str', 'catatan_dokter' => 'str?', 'waktu_mulai' => 'dt',
            'waktu_selesai' => 'dt?', 'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'resep' => [
            'id_resep' => 'int', 'id_pendaftaran' => 'int', 'kode_qr_unik' => 'str',
            'waktu_diterbitkan' => 'dt', 'pilihan_penebusan' => 'str', 'estimasi_jam_selesai' => 'dt?',
            'status_resep' => 'str', 'kadaluarsa_qr' => 'dt?', 'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'detail_resep' => [
            'id_detail' => 'int', 'id_resep' => 'int', 'id_obat' => 'int', 'jumlah' => 'int',
            'dosis_aturan_pakai' => 'str', 'instruksi_racikan' => 'str?', 'catatan' => 'str?',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'notifikasi' => [
            'id_notifikasi' => 'int', 'id_pasien' => 'int', 'id_pendaftaran' => 'int?',
            'jenis_notifikasi' => 'str', 'channel' => 'str', 'nomor_tujuan' => 'str', 'pesan' => 'str',
            'status' => 'str', 'waktu_dikirim' => 'dt?', 'response_api' => 'str?', 'created_at' => 'dt',
        ],
        'otp_verifikasi' => [
            'id_otp' => 'int', 'id_user' => 'int', 'kode_otp' => 'str', 'tujuan' => 'str',
            'expired_at' => 'dt', 'verified_at' => 'dt?', 'attempt' => 'int', 'created_at' => 'dt',
        ],
        'pengambilan_obat' => [
            'id_pengambilan' => 'int', 'id_resep' => 'int', 'id_user_petugas' => 'int',
            'diambil_oleh' => 'str', 'nama_pengambil' => 'str?', 'waktu_pengambilan' => 'dt',
            'metode_verifikasi' => 'str', 'status' => 'str', 'catatan' => 'str?',
            'created_at' => 'dt', 'updated_at' => 'dt',
        ],
        'mutasi_stok' => [
            'id_mutasi' => 'int', 'id_obat' => 'int', 'id_user' => 'int', 'jenis_mutasi' => 'str',
            'jumlah' => 'int', 'stok_sebelum' => 'int', 'stok_sesudah' => 'int',
            'keterangan' => 'str?', 'created_at' => 'dt',
        ],
        'log_aktivitas' => [
            'id_log' => 'int', 'id_user' => 'int?', 'aktivitas' => 'str', 'tabel_referensi' => 'str?',
            'id_referensi' => 'int?', 'keterangan' => 'str?', 'ip_address' => 'str?', 'created_at' => 'dt',
        ],
    ];
}

function urutanTabel(): array
{
    return array_keys(skema());
}

function keIso(?string $nilai): ?string
{
    if ($nilai === null || $nilai === '') {
        return null;
    }
    if (!str_contains($nilai, ' ') && !str_contains($nilai, 'T')) {
        return $nilai;
    }
    $nilai = str_replace(' ', 'T', $nilai);
    if (!str_ends_with($nilai, 'Z')) {
        $nilai .= strlen($nilai) === 19 ? '.000Z' : 'Z';
    }
    return $nilai;
}

function keSql(?string $nilai): ?string
{
    if ($nilai === null || $nilai === '') {
        return null;
    }
    if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $nilai)) {
        return $nilai;
    }
    if (preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $nilai)) {
        return strlen($nilai) === 5 ? $nilai . ':00' : $nilai;
    }
    $nilai = substr(str_replace('T', ' ', $nilai), 0, 19);
    return $nilai;
}

function bacaNilai(?string $nilai, string $tipe): mixed
{
    $bolehKosong = str_ends_with($tipe, '?');
    $dasar = rtrim($tipe, '?');
    if ($nilai === null || $nilai === '') {
        return $bolehKosong ? null : ($dasar === 'str' ? '' : null);
    }
    return match ($dasar) {
        'int' => (int) $nilai,
        'bool' => $nilai === '1' || $nilai === 'true',
        'dec' => (float) $nilai,
        'dt' => keIso($nilai),
        'date', 'time' => $nilai,
        default => $nilai,
    };
}

function sqlNilai(mysqli $db, mixed $nilai, string $tipe): string
{
    $bolehKosong = str_ends_with($tipe, '?');
    $dasar = rtrim($tipe, '?');
    if ($nilai === null || $nilai === '') {
        if ($dasar === 'bool') {
            return '0';
        }
        if ($dasar === 'str' && !$bolehKosong) {
            return "''";
        }
        return 'NULL';
    }
    return match ($dasar) {
        'int' => (string) (int) $nilai,
        'bool' => ($nilai === true || $nilai === 1 || $nilai === '1') ? '1' : '0',
        'dec' => is_numeric($nilai) ? (string) (float) $nilai : '0',
        'dt' => "'" . $db->real_escape_string((string) keSql((string) $nilai)) . "'",
        default => "'" . $db->real_escape_string((string) $nilai) . "'",
    };
}

function bacaSemua(mysqli $db): array
{
    $hasil = [];
    foreach (skema() as $tabel => $kolom) {
        $query = $db->query('SELECT * FROM `' . $tabel . '`');
        $baris = [];
        while ($row = $query->fetch_assoc()) {
            $item = [];
            foreach ($kolom as $nama => $tipe) {
                $item[$nama] = bacaNilai($row[$nama] ?? null, $tipe);
            }
            $baris[] = $item;
        }
        $hasil[$tabel] = $baris;
    }
    return $hasil;
}

function simpanSemua(mysqli $db, array $data): void
{
    foreach (urutanTabel() as $tabel) {
        if (!isset($data[$tabel]) || !is_array($data[$tabel])) {
            throw new RuntimeException('Tabel ' . $tabel . ' tidak lengkap.');
        }
    }
    if ($data['users'] === []) {
        throw new RuntimeException('Users kosong, penyimpanan dibatalkan.');
    }

    $db->begin_transaction();
    try {
        $db->query('SET FOREIGN_KEY_CHECKS = 0');
        foreach (array_reverse(urutanTabel()) as $tabel) {
            $db->query('DELETE FROM `' . $tabel . '`');
        }
        foreach (skema() as $tabel => $kolom) {
            foreach ($data[$tabel] as $row) {
                if (!is_array($row)) {
                    throw new RuntimeException('Baris ' . $tabel . ' tidak valid.');
                }
                if ($tabel === 'pemeriksaan' && ($row['waktu_mulai'] ?? null) === null) {
                    $row['waktu_mulai'] = $row['created_at'] ?? gmdate('Y-m-d H:i:s');
                }
                $nama = array_keys($kolom);
                $nilai = [];
                foreach ($kolom as $kolomNama => $tipe) {
                    $nilai[] = sqlNilai($db, $row[$kolomNama] ?? null, $tipe);
                }
                $db->query(
                    'INSERT INTO `' . $tabel . '` (`' . implode('`,`', $nama) . '`) VALUES (' . implode(',', $nilai) . ')'
                );
            }
        }
        $db->query('SET FOREIGN_KEY_CHECKS = 1');
        $db->commit();
    } catch (Throwable $error) {
        $db->rollback();
        $db->query('SET FOREIGN_KEY_CHECKS = 1');
        throw $error;
    }
}

try {
    $db = koneksiMedicflow();
    $metode = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    if ($metode === 'GET') {
        echo json_encode(['ok' => true, 'data' => bacaSemua($db)], JSON_UNESCAPED_UNICODE);
        exit;
    }
    if ($metode === 'PUT' || $metode === 'POST') {
        $mentah = file_get_contents('php://input') ?: '';
        $data = json_decode($mentah, true);
        if (!is_array($data)) {
            throw new RuntimeException('Body JSON tidak valid.');
        }
        simpanSemua($db, $data);
        echo json_encode(['ok' => true], JSON_UNESCAPED_UNICODE);
        exit;
    }
    http_response_code(405);
    echo json_encode(['ok' => false, 'error' => 'Metode tidak didukung.'], JSON_UNESCAPED_UNICODE);
} catch (Throwable $error) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => $error->getMessage()], JSON_UNESCAPED_UNICODE);
}
