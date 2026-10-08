<?php

namespace App\Models;

class LogAktivitasModel extends MedicflowModel
{
    protected $table = 'log_aktivitas';
    protected $primaryKey = 'id_log';
    protected $updatedField = '';
    protected string $nama = 'log aktivitas';

    protected $allowedFields = [
        'id_user',
        'aktivitas',
        'tabel_referensi',
        'id_referensi',
        'keterangan',
        'ip_address',
    ];

    protected array $kolomWajib = ['aktivitas'];
    protected array $kolomNull = ['id_user', 'tabel_referensi', 'id_referensi', 'keterangan', 'ip_address'];
    protected array $kolomBulat = ['id_log', 'id_user', 'id_referensi'];
    protected array $kolomSaringan = ['id_user', 'aktivitas', 'tabel_referensi'];

    protected $validationRules = [
        'id_user' => 'permit_empty|is_natural_no_zero|is_not_unique[users.id_user]',
        'aktivitas' => 'required|max_length[100]',
        'tabel_referensi' => 'permit_empty|max_length[100]',
        'id_referensi' => 'permit_empty|is_natural_no_zero',
        'keterangan' => 'permit_empty',
        'ip_address' => 'permit_empty|max_length[45]',
    ];

    protected $validationMessages = [
        'id_user' => [
            'is_not_unique' => 'Pengguna tidak ditemukan.',
        ],
    ];
}
