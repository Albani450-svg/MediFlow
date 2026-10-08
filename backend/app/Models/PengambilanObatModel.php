<?php

namespace App\Models;

class PengambilanObatModel extends MedicflowModel
{
    protected $table = 'pengambilan_obat';
    protected $primaryKey = 'id_pengambilan';
    protected string $nama = 'pengambilan obat';

    protected $allowedFields = [
        'id_resep',
        'id_user_petugas',
        'diambil_oleh',
        'nama_pengambil',
        'waktu_pengambilan',
        'metode_verifikasi',
        'status',
        'catatan',
    ];

    protected array $kolomWajib = ['id_resep', 'id_user_petugas', 'diambil_oleh', 'metode_verifikasi'];
    protected array $kolomNull = ['nama_pengambil', 'catatan'];
    protected array $kolomBulat = ['id_pengambilan', 'id_resep', 'id_user_petugas'];
    protected array $kolomWaktu = ['waktu_pengambilan'];
    protected array $kolomSaringan = ['id_resep', 'id_user_petugas', 'status', 'diambil_oleh'];

    protected $validationRules = [
        'id_resep' => 'required|is_natural_no_zero|is_not_unique[resep.id_resep]|is_unique[pengambilan_obat.id_resep,id_pengambilan,{id_pengambilan}]',
        'id_user_petugas' => 'required|is_natural_no_zero|is_not_unique[users.id_user]',
        'diambil_oleh' => 'required|in_list[Pasien,Keluarga]',
        'nama_pengambil' => 'permit_empty|max_length[150]',
        'waktu_pengambilan' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'metode_verifikasi' => 'required|in_list[QR,QR + PIN,QR + OTP]',
        'status' => 'if_exist|in_list[Berhasil,Ditolak]',
        'catatan' => 'permit_empty',
    ];

    protected $validationMessages = [
        'id_resep' => [
            'is_not_unique' => 'Resep tidak ditemukan.',
            'is_unique' => 'Resep ini sudah dicatat pengambilannya.',
        ],
        'id_user_petugas' => [
            'is_not_unique' => 'Petugas tidak ditemukan.',
        ],
    ];
}
