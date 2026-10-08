<?php

namespace App\Models;

class DokterModel extends MedicflowModel
{
    protected $table = 'dokter';
    protected $primaryKey = 'id_dokter';
    protected string $nama = 'dokter';

    protected $allowedFields = [
        'id_user',
        'id_poli',
        'nama_dokter',
        'spesialisasi',
        'rata_waktu_periksa_menit',
        'is_active',
    ];

    protected array $kolomWajib = ['id_user', 'id_poli', 'nama_dokter'];
    protected array $kolomNull = ['spesialisasi'];
    protected array $kolomBoolean = ['is_active'];
    protected array $kolomBulat = ['id_dokter', 'id_user', 'id_poli', 'rata_waktu_periksa_menit'];
    protected array $kolomSaringan = ['id_user', 'id_poli', 'is_active'];

    protected $validationRules = [
        'id_user' => 'required|is_natural_no_zero|is_not_unique[users.id_user]|is_unique[dokter.id_user,id_dokter,{id_dokter}]',
        'id_poli' => 'required|is_natural_no_zero|is_not_unique[poliklinik.id_poli]',
        'nama_dokter' => 'required|max_length[150]',
        'spesialisasi' => 'permit_empty|max_length[100]',
        'rata_waktu_periksa_menit' => 'if_exist|integer|greater_than[0]|less_than_equal_to[180]',
        'is_active' => 'if_exist|in_list[0,1]',
    ];

    protected $validationMessages = [
        'id_user' => [
            'is_not_unique' => 'Akun pengguna tidak ditemukan.',
            'is_unique' => 'Akun ini sudah terhubung ke dokter lain.',
        ],
        'id_poli' => [
            'is_not_unique' => 'Poliklinik tidak ditemukan.',
        ],
    ];
}
