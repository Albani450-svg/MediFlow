<?php

namespace App\Controllers\Api;

use App\Models\NotifikasiModel;
use App\Models\ObatModel;
use App\Models\PemeriksaanModel;

class NotifikasiController extends ApiResourceController
{
    protected string $modelClass = NotifikasiModel::class;

    protected function periksa(array $data, array $sebelum, int|string|null $id): ?string
    {
        $pesan = trim((string) ($data['pesan'] ?? $sebelum['pesan'] ?? ''));
        if ($pesan === '') {
            return null;
        }

        if (preg_match('/whatsapp/i', $pesan) === 1) {
            return 'Pesan notifikasi tidak boleh menyebut WhatsApp.';
        }

        foreach ((new ObatModel())->findAll() as $obat) {
            $nama = trim((string) ($obat['nama_obat'] ?? ''));
            if ($nama !== '' && stripos($pesan, $nama) !== false) {
                return 'Pesan notifikasi tidak boleh memuat nama obat.';
            }
        }

        foreach ((new PemeriksaanModel())->select('diagnosis')->findAll() as $baris) {
            $diagnosis = trim((string) ($baris['diagnosis'] ?? ''));
            if (strlen($diagnosis) < 4) {
                continue;
            }
            if (stripos($pesan, $diagnosis) !== false) {
                return 'Pesan notifikasi tidak boleh memuat diagnosis.';
            }
        }

        return null;
    }
}
