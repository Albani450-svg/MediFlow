<?php

namespace App\Models;

class UserModel extends MedicflowModel
{
    protected $table = 'users';
    protected $primaryKey = 'id_user';
    protected string $nama = 'pengguna';

    protected $allowedFields = [
        'username',
        'password_hash',
        'role',
        'nama_lengkap',
        'no_telepon',
        'is_active',
    ];

    protected array $kolomWajib = ['username', 'password_hash', 'role', 'nama_lengkap'];
    protected array $kolomNull = ['no_telepon'];
    protected array $kolomBoolean = ['is_active'];
    protected array $kolomBulat = ['id_user'];
    protected array $kolomSaringan = ['role', 'is_active', 'username'];

    protected $validationRules = [
        'username' => 'required|min_length[3]|max_length[50]|is_unique[users.username,id_user,{id_user}]',
        'password_hash' => 'required|max_length[255]',
        'role' => 'required|in_list[admin,pasien,dokter,farmasi]',
        'nama_lengkap' => 'required|max_length[100]',
        'no_telepon' => 'permit_empty|max_length[20]|regex_match[/^08[0-9]{8,12}$/]',
        'is_active' => 'if_exist|in_list[0,1]',
    ];

    protected $validationMessages = [
        'no_telepon' => [
            'regex_match' => 'Nomor telepon harus diawali 08.',
        ],
        'username' => [
            'is_unique' => 'Nama pengguna sudah terdaftar.',
        ],
    ];
}
