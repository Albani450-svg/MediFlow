<?php

namespace App\Models;

class ResepModel extends MedicflowModel
{
    protected $table = 'resep';
    protected $primaryKey = 'id_resep';
    protected string $nama = 'resep';

    protected $allowedFields = [
        'id_pendaftaran',
        'kode_qr_unik',
        'waktu_diterbitkan',
        'pilihan_penebusan',
        'estimasi_jam_selesai',
        'status_resep',
        'kadaluarsa_qr',
    ];

    protected array $kolomWajib = ['id_pendaftaran', 'kode_qr_unik'];
    protected array $kolomNull = ['estimasi_jam_selesai', 'kadaluarsa_qr'];
    protected array $kolomBulat = ['id_resep', 'id_pendaftaran'];
    protected array $kolomWaktu = ['waktu_diterbitkan', 'estimasi_jam_selesai', 'kadaluarsa_qr'];
    protected array $kolomSaringan = ['id_pendaftaran', 'status_resep', 'pilihan_penebusan', 'kode_qr_unik'];

    protected $validationRules = [
        'id_pendaftaran' => 'required|is_natural_no_zero|is_not_unique[pendaftaran_poli.id_pendaftaran]|is_unique[resep.id_pendaftaran,id_resep,{id_resep}]',
        'kode_qr_unik' => 'required|max_length[100]|is_unique[resep.kode_qr_unik,id_resep,{id_resep}]',
        'waktu_diterbitkan' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'pilihan_penebusan' => 'if_exist|in_list[Belum Memilih,Apotek RS,Apotek Luar]',
        'estimasi_jam_selesai' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'status_resep' => 'if_exist|in_list[Menunggu Pilihan,Antrean Farmasi,Sedang Diracik,Siap Diambil,Selesai Diambil,Dibatalkan]',
        'kadaluarsa_qr' => 'permit_empty|valid_date[Y-m-d H:i:s]',
    ];

    protected $validationMessages = [
        'id_pendaftaran' => [
            'is_not_unique' => 'Pendaftaran tidak ditemukan.',
            'is_unique' => 'Pendaftaran ini sudah punya resep.',
        ],
        'kode_qr_unik' => [
            'is_unique' => 'Kode QR sudah dipakai.',
        ],
    ];
}
