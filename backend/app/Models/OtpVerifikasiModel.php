<?php

namespace App\Models;

class OtpVerifikasiModel extends MedicflowModel
{
    protected $table = 'otp_verifikasi';
    protected $primaryKey = 'id_otp';
    protected $updatedField = '';
    protected string $nama = 'OTP';

    protected $allowedFields = [
        'id_user',
        'kode_otp',
        'tujuan',
        'expired_at',
        'verified_at',
        'attempt',
    ];

    protected array $kolomWajib = ['id_user', 'kode_otp', 'tujuan', 'expired_at'];
    protected array $kolomNull = ['verified_at'];
    protected array $kolomBulat = ['id_otp', 'id_user', 'attempt'];
    protected array $kolomWaktu = ['expired_at', 'verified_at'];
    protected array $kolomSaringan = ['id_user'];

    protected $validationRules = [
        'id_user' => 'required|is_natural_no_zero|is_not_unique[users.id_user]',
        'kode_otp' => 'required|max_length[10]',
        'tujuan' => 'required|max_length[20]',
        'expired_at' => 'required|valid_date[Y-m-d H:i:s]',
        'verified_at' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'attempt' => 'if_exist|integer|greater_than_equal_to[0]',
    ];

    protected $validationMessages = [
        'id_user' => [
            'is_not_unique' => 'Akun pengguna tidak ditemukan.',
        ],
    ];
}
