<?php

namespace App\Models;

class PasienModel extends MedicflowModel
{
    protected $table = 'pasien';
    protected $primaryKey = 'id_pasien';
    protected string $nama = 'pasien';

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

    protected array $kolomWajib = [
        'id_user',
        'nik',
        'nama_lengkap',
        'tanggal_lahir',
        'jenis_kelamin',
        'no_telepon',
    ];

    protected array $kolomNull = ['nomor_bpjs', 'alamat'];
    protected array $kolomBoolean = ['terdaftar_satusehat'];
    protected array $kolomBulat = ['id_pasien', 'id_user'];
    protected array $kolomTanggal = ['tanggal_lahir'];
    protected array $kolomSaringan = ['id_user', 'nik', 'jenis_kelamin'];

    protected $validationRules = [
        'id_user' => 'required|is_natural_no_zero|is_not_unique[users.id_user]|is_unique[pasien.id_user,id_pasien,{id_pasien}]',
        'nik' => 'required|max_length[16]|is_unique[pasien.nik,id_pasien,{id_pasien}]',
        'nomor_bpjs' => 'permit_empty|max_length[20]|is_unique[pasien.nomor_bpjs,id_pasien,{id_pasien}]',
        'nama_lengkap' => 'required|max_length[150]',
        'tanggal_lahir' => 'required|valid_date[Y-m-d]',
        'jenis_kelamin' => 'required|in_list[L,P]',
        'alamat' => 'permit_empty',
        'no_telepon' => 'required|max_length[20]|regex_match[/^08[0-9]{8,12}$/]',
        'terdaftar_satusehat' => 'if_exist|in_list[0,1]',
    ];

    protected $validationMessages = [
        'id_user' => [
            'is_not_unique' => 'Akun pengguna tidak ditemukan.',
            'is_unique' => 'Akun ini sudah terhubung ke pasien lain.',
        ],
        'nik' => [
            'is_unique' => 'NIK sudah terdaftar.',
        ],
        'nomor_bpjs' => [
            'is_unique' => 'Nomor BPJS sudah terdaftar.',
        ],
        'jenis_kelamin' => [
            'in_list' => 'Jenis kelamin harus L atau P.',
        ],
        'no_telepon' => [
            'regex_match' => 'Nomor telepon harus diawali 08.',
        ],
    ];
}
