<?php

namespace App\Models;

class ObatModel extends MedicflowModel
{
    protected $table = 'obat';
    protected $primaryKey = 'id_obat';
    protected string $nama = 'obat';

    protected $allowedFields = [
        'kode_kemenkes',
        'nama_obat',
        'jenis_obat',
        'satuan',
        'cover_bpjs',
        'stok_rs',
        'stok_minimum',
        'harga',
        'is_active',
    ];

    protected array $kolomWajib = ['kode_kemenkes', 'nama_obat', 'satuan'];
    protected array $kolomBoolean = ['cover_bpjs', 'is_active'];
    protected array $kolomBulat = ['id_obat', 'stok_rs', 'stok_minimum'];
    protected array $kolomDesimal = ['harga'];
    protected array $kolomSaringan = ['jenis_obat', 'is_active', 'kode_kemenkes'];

    protected $validationRules = [
        'kode_kemenkes' => 'required|max_length[50]|is_unique[obat.kode_kemenkes,id_obat,{id_obat}]',
        'nama_obat' => 'required|max_length[150]',
        'jenis_obat' => 'if_exist|in_list[Jadi,Racikan]',
        'satuan' => 'required|max_length[30]',
        'cover_bpjs' => 'if_exist|in_list[0,1]',
        'stok_rs' => 'if_exist|integer|greater_than_equal_to[0]',
        'stok_minimum' => 'if_exist|integer|greater_than_equal_to[0]',
        'harga' => 'if_exist|numeric|greater_than_equal_to[0]',
        'is_active' => 'if_exist|in_list[0,1]',
    ];

    protected $validationMessages = [
        'kode_kemenkes' => [
            'is_unique' => 'Kode Kemenkes sudah terdaftar.',
        ],
    ];
}
