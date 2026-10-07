<?php

namespace App\Models;

use CodeIgniter\Model;

class PasienModel extends Model
{
    protected $table = 'pasien';
    protected $primaryKey = 'id_pasien';
    protected $returnType = 'array';
    protected $useAutoIncrement = true;
    protected $protectFields = true;

    protected $allowedFields = [
        'id_user',
        'nik',
        'nomor_bpjs',
        'nama_lengkap',
        'tanggal_lahir',
        'jenis_kelamin',
        'alamat',
        'no_telepon',
        'terdaftar_satusehat',
    ];

    protected array $casts = [
        'id_pasien' => 'int',
        'id_user' => 'int',
        'terdaftar_satusehat' => 'int-bool',
    ];

    protected $useTimestamps = true;
    protected $dateFormat = 'datetime';
    protected $createdField = 'created_at';
    protected $updatedField = 'updated_at';

    protected $validationRules = [
        'id_user' => 'required|is_natural_no_zero|is_not_unique[users.id_user]',
        'nik' => 'required|max_length[16]|is_unique[pasien.nik,id_pasien,{id}]',
        'nomor_bpjs' => 'permit_empty|max_length[20]|is_unique[pasien.nomor_bpjs,id_pasien,{id}]',
        'nama_lengkap' => 'required|max_length[150]',
        'tanggal_lahir' => 'required|valid_date[Y-m-d]',
        'jenis_kelamin' => 'required|in_list[L,P]',
        'alamat' => 'permit_empty',
        'no_telepon' => 'required|max_length[20]',
    ];

    protected $validationMessages = [
        'id_user' => [
            'required' => 'Akun pengguna wajib diisi.',
            'is_not_unique' => 'Akun pengguna tidak ditemukan.',
        ],
        'nik' => [
            'required' => 'NIK wajib diisi.',
            'is_unique' => 'NIK sudah terdaftar.',
        ],
        'jenis_kelamin' => [
            'in_list' => 'Jenis kelamin harus L atau P.',
        ],
    ];

    protected $skipValidation = false;
    protected $cleanValidationRules = true;
}
