<?php

namespace App\Controllers\Api;

use App\Models\PendaftaranPoliModel;

class PendaftaranPoliController extends ApiResourceController
{
    protected string $modelClass = PendaftaranPoliModel::class;

    protected function periksa(array $data, array $sebelum, int|string|null $id): ?string
    {
        $pasien = $data['id_pasien'] ?? $sebelum['id_pasien'] ?? null;
        $jadwal = $data['id_jadwal'] ?? $sebelum['id_jadwal'] ?? null;
        $tanggal = $data['tanggal_kunjungan'] ?? $sebelum['tanggal_kunjungan'] ?? null;
        $nomor = $data['nomor_antrean'] ?? $sebelum['nomor_antrean'] ?? null;
        $status = $data['status_antrean'] ?? $sebelum['status_antrean'] ?? 'Menunggu';

        if ($jadwal && $tanggal && $nomor) {
            $nomorSama = (new PendaftaranPoliModel())
                ->where('id_jadwal', $jadwal)
                ->where('tanggal_kunjungan', $tanggal)
                ->where('nomor_antrean', $nomor);
            if ($id !== null) {
                $nomorSama->where('id_pendaftaran !=', $id);
            }
            if ($nomorSama->countAllResults() > 0) {
                return 'Nomor antrean sudah dipakai pada jadwal ini.';
            }
        }

        if ($status !== 'Batal' && $pasien && $jadwal && $tanggal) {
            $ganda = (new PendaftaranPoliModel())
                ->where('id_pasien', $pasien)
                ->where('id_jadwal', $jadwal)
                ->where('tanggal_kunjungan', $tanggal)
                ->where('status_antrean !=', 'Batal');
            if ($id !== null) {
                $ganda->where('id_pendaftaran !=', $id);
            }
            if ($ganda->countAllResults() > 0) {
                return 'Pasien sudah terdaftar pada jadwal ini.';
            }
        }

        return null;
    }
}
