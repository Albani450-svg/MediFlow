<?php

namespace App\Models;

class MutasiStokModel extends MedicflowModel
{
    protected $table = 'mutasi_stok';
    protected $primaryKey = 'id_mutasi';
    protected $updatedField = '';
    protected string $nama = 'mutasi stok';

    protected $allowedFields = [
        'id_obat',
        'id_user',
        'jenis_mutasi',
        'jumlah',
        'stok_sebelum',
        'stok_sesudah',
        'keterangan',
    ];

    protected array $kolomWajib = [
        'id_obat',
        'id_user',
        'jenis_mutasi',
        'jumlah',
        'stok_sebelum',
        'stok_sesudah',
    ];

    protected array $kolomNull = ['keterangan'];
    protected array $kolomBulat = ['id_mutasi', 'id_obat', 'id_user', 'jumlah', 'stok_sebelum', 'stok_sesudah'];
    protected array $kolomSaringan = ['id_obat', 'id_user', 'jenis_mutasi'];

    protected $validationRules = [
        'id_obat' => 'required|is_natural_no_zero|is_not_unique[obat.id_obat]',
        'id_user' => 'required|is_natural_no_zero|is_not_unique[users.id_user]',
        'jenis_mutasi' => 'required|in_list[Masuk,Keluar,Penyesuaian]',
        'jumlah' => 'required|integer|greater_than[0]',
        'stok_sebelum' => 'required|integer',
        'stok_sesudah' => 'required|integer',
        'keterangan' => 'permit_empty',
    ];

    protected $validationMessages = [
        'id_obat' => [
            'is_not_unique' => 'Obat tidak ditemukan.',
        ],
        'id_user' => [
            'is_not_unique' => 'Pengguna tidak ditemukan.',
        ],
    ];
}
