<?php

namespace App\Models;

class PendaftaranPoliModel extends MedicflowModel
{
    protected $table = 'pendaftaran_poli';
    protected $primaryKey = 'id_pendaftaran';
    protected string $nama = 'pendaftaran';

    protected $allowedFields = [
        'id_pasien',
        'id_jadwal',
        'tanggal_kunjungan',
        'nomor_antrean',
        'waktu_check_in',
        'estimasi_jam_masuk',
        'status_antrean',
        'catatan_pasien',
    ];

    protected array $kolomWajib = ['id_pasien', 'id_jadwal', 'tanggal_kunjungan', 'nomor_antrean'];
    protected array $kolomNull = ['waktu_check_in', 'estimasi_jam_masuk', 'catatan_pasien'];
    protected array $kolomBulat = ['id_pendaftaran', 'id_pasien', 'id_jadwal', 'nomor_antrean'];
    protected array $kolomTanggal = ['tanggal_kunjungan'];
    protected array $kolomWaktu = ['waktu_check_in', 'estimasi_jam_masuk'];
    protected array $kolomSaringan = ['id_pasien', 'id_jadwal', 'tanggal_kunjungan', 'status_antrean'];

    protected $validationRules = [
        'id_pasien' => 'required|is_natural_no_zero|is_not_unique[pasien.id_pasien]',
        'id_jadwal' => 'required|is_natural_no_zero|is_not_unique[jadwal_dokter.id_jadwal]',
        'tanggal_kunjungan' => 'required|valid_date[Y-m-d]',
        'nomor_antrean' => 'required|integer|greater_than[0]',
        'waktu_check_in' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'estimasi_jam_masuk' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'status_antrean' => 'if_exist|in_list[Menunggu,Dipanggil,Masuk Ruangan,Selesai Pemeriksaan,Batal]',
        'catatan_pasien' => 'permit_empty',
    ];

    protected $validationMessages = [
        'id_pasien' => [
            'is_not_unique' => 'Pasien tidak ditemukan.',
        ],
        'id_jadwal' => [
            'is_not_unique' => 'Jadwal dokter tidak ditemukan.',
        ],
    ];
}
