<?php

namespace App\Controllers\Api;

use App\Models\JadwalDokterModel;
use App\Models\PendaftaranPoliModel;
use DateTimeImmutable;
use DateTimeZone;

class JadwalDokterController extends ApiResourceController
{
    protected string $modelClass = JadwalDokterModel::class;

    protected function periksa(array $data, array $sebelum, int|string|null $id): ?string
    {
        $mulai = $data['jam_mulai'] ?? $sebelum['jam_mulai'] ?? null;
        $selesai = $data['jam_selesai'] ?? $sebelum['jam_selesai'] ?? null;
        $hari = $data['hari'] ?? $sebelum['hari'] ?? null;
        $dokter = $data['id_dokter'] ?? $sebelum['id_dokter'] ?? null;
        $aktif = array_key_exists('is_active', $data)
            ? $this->nyala($data['is_active'])
            : ($sebelum === [] || $this->nyala($sebelum['is_active'] ?? 1));

        if (is_string($mulai) && is_string($selesai) && $this->menit($mulai) >= $this->menit($selesai)) {
            return 'Jam selesai harus setelah jam mulai.';
        }

        if ($aktif && $dokter && $hari && is_string($mulai) && is_string($selesai)) {
            $lain = (new JadwalDokterModel())
                ->where('id_dokter', $dokter)
                ->where('hari', $hari)
                ->where('is_active', 1)
                ->findAll();

            foreach ($lain as $baris) {
                if ($id !== null && (int) $baris['id_jadwal'] === (int) $id) {
                    continue;
                }
                $mulaiLain = $this->menit((string) $baris['jam_mulai']);
                $selesaiLain = $this->menit((string) $baris['jam_selesai']);
                if ($this->menit($mulai) < $selesaiLain && $mulaiLain < $this->menit($selesai)) {
                    return 'Jam ini bertumpuk dengan jadwal lain dokter yang sama.';
                }
            }
        }

        if (
            $id !== null
            && array_key_exists('is_active', $data)
            && ! $this->nyala($data['is_active'])
            && $this->nyala($sebelum['is_active'] ?? 0)
            && $this->antreanHariIni($id) > 0
        ) {
            return 'Jadwal ini masih punya antrean hari ini.';
        }

        if ($id !== null && array_key_exists('kuota_maksimal', $data)) {
            $terpakai = $this->antreanHariIni($id);
            if ((int) $data['kuota_maksimal'] < $terpakai) {
                return 'Kuota lebih kecil dari antrean yang sudah terdaftar hari ini.';
            }
        }

        return null;
    }

    private function antreanHariIni(int|string $idJadwal): int
    {
        $hariIni = (new DateTimeImmutable('now', new DateTimeZone('Asia/Jakarta')))->format('Y-m-d');

        return (int) (new PendaftaranPoliModel())
            ->where('id_jadwal', $idJadwal)
            ->where('tanggal_kunjungan', $hariIni)
            ->where('status_antrean !=', 'Batal')
            ->countAllResults();
    }

    private function menit(string $jam): int
    {
        $bagian = explode(':', $jam);

        return ((int) $bagian[0]) * 60 + (int) ($bagian[1] ?? 0);
    }
}
