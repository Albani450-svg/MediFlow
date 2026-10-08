<?php

namespace App\Models;

class JadwalDokterModel extends MedicflowModel
{
    protected $table = 'jadwal_dokter';
    protected $primaryKey = 'id_jadwal';
    protected string $nama = 'jadwal dokter';

    protected $allowedFields = [
        'id_dokter',
        'hari',
        'jam_mulai',
        'jam_selesai',
        'kuota_maksimal',
        'is_active',
    ];

    protected array $kolomWajib = ['id_dokter', 'hari', 'jam_mulai', 'jam_selesai'];
    protected array $kolomBoolean = ['is_active'];
    protected array $kolomBulat = ['id_jadwal', 'id_dokter', 'kuota_maksimal'];
    protected array $kolomJam = ['jam_mulai', 'jam_selesai'];
    protected array $kolomSaringan = ['id_dokter', 'hari', 'is_active'];

    protected $validationRules = [
        'id_dokter' => 'required|is_natural_no_zero|is_not_unique[dokter.id_dokter]',
        'hari' => 'required|in_list[Minggu,Senin,Selasa,Rabu,Kamis,Jumat,Sabtu]',
        'jam_mulai' => 'required|regex_match[/^\d{2}:\d{2}:\d{2}$/]',
        'jam_selesai' => 'required|regex_match[/^\d{2}:\d{2}:\d{2}$/]',
        'kuota_maksimal' => 'if_exist|integer|greater_than[0]|less_than_equal_to[200]',
        'is_active' => 'if_exist|in_list[0,1]',
    ];

    protected $validationMessages = [
        'id_dokter' => [
            'is_not_unique' => 'Dokter tidak ditemukan.',
        ],
        'jam_mulai' => [
            'regex_match' => 'Jam mulai tidak valid.',
        ],
        'jam_selesai' => [
            'regex_match' => 'Jam selesai tidak valid.',
        ],
    ];
}
