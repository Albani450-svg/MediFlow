<?php

namespace App\Models;

class PemeriksaanModel extends MedicflowModel
{
    protected $table = 'pemeriksaan';
    protected $primaryKey = 'id_pemeriksaan';
    protected string $nama = 'pemeriksaan';

    protected $allowedFields = [
        'id_pendaftaran',
        'id_dokter',
        'keluhan',
        'tekanan_darah',
        'suhu_tubuh',
        'berat_badan',
        'tinggi_badan',
        'diagnosis',
        'tindakan',
        'catatan_dokter',
        'waktu_mulai',
        'waktu_selesai',
    ];

    protected array $kolomWajib = [
        'id_pendaftaran',
        'id_dokter',
        'keluhan',
        'diagnosis',
        'tindakan',
    ];

    protected array $kolomBolehKosong = ['diagnosis', 'tindakan'];
    protected array $kolomNull = [
        'tekanan_darah',
        'suhu_tubuh',
        'berat_badan',
        'tinggi_badan',
        'catatan_dokter',
        'waktu_selesai',
    ];
    protected array $kolomBulat = ['id_pemeriksaan', 'id_pendaftaran', 'id_dokter'];
    protected array $kolomDesimal = ['suhu_tubuh', 'berat_badan', 'tinggi_badan'];
    protected array $kolomWaktu = ['waktu_mulai', 'waktu_selesai'];
    protected array $kolomSaringan = ['id_pendaftaran', 'id_dokter'];

    protected $validationRules = [
        'id_pendaftaran' => 'required|is_natural_no_zero|is_not_unique[pendaftaran_poli.id_pendaftaran]|is_unique[pemeriksaan.id_pendaftaran,id_pemeriksaan,{id_pemeriksaan}]',
        'id_dokter' => 'required|is_natural_no_zero|is_not_unique[dokter.id_dokter]',
        'keluhan' => 'required',
        'tekanan_darah' => 'permit_empty|max_length[20]',
        'suhu_tubuh' => 'permit_empty|numeric',
        'berat_badan' => 'permit_empty|numeric',
        'tinggi_badan' => 'permit_empty|numeric',
        'diagnosis' => 'permit_empty',
        'tindakan' => 'permit_empty',
        'catatan_dokter' => 'permit_empty',
        'waktu_mulai' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'waktu_selesai' => 'permit_empty|valid_date[Y-m-d H:i:s]',
    ];

    protected $validationMessages = [
        'id_pendaftaran' => [
            'is_not_unique' => 'Pendaftaran tidak ditemukan.',
            'is_unique' => 'Pendaftaran ini sudah punya pemeriksaan.',
        ],
        'id_dokter' => [
            'is_not_unique' => 'Dokter tidak ditemukan.',
        ],
    ];
}
