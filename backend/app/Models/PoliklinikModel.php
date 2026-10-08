<?php

namespace App\Models;

class PoliklinikModel extends MedicflowModel
{
    protected $table = 'poliklinik';
    protected $primaryKey = 'id_poli';
    protected string $nama = 'poliklinik';

    protected $allowedFields = [
        'nama_poli',
        'deskripsi',
        'is_active',
    ];

    protected array $kolomWajib = ['nama_poli'];
    protected array $kolomNull = ['deskripsi'];
    protected array $kolomBoolean = ['is_active'];
    protected array $kolomBulat = ['id_poli'];
    protected array $kolomSaringan = ['is_active', 'nama_poli'];

    protected $validationRules = [
        'nama_poli' => 'required|max_length[100]|is_unique[poliklinik.nama_poli,id_poli,{id_poli}]',
        'deskripsi' => 'permit_empty',
        'is_active' => 'if_exist|in_list[0,1]',
    ];

    protected $validationMessages = [
        'nama_poli' => [
            'is_unique' => 'Nama poliklinik sudah terdaftar.',
        ],
    ];
}
