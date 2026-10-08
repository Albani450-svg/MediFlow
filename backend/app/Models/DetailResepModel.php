<?php

namespace App\Models;

class DetailResepModel extends MedicflowModel
{
    protected $table = 'detail_resep';
    protected $primaryKey = 'id_detail';
    protected string $nama = 'detail resep';

    protected $allowedFields = [
        'id_resep',
        'id_obat',
        'jumlah',
        'dosis_aturan_pakai',
        'instruksi_racikan',
        'catatan',
    ];

    protected array $kolomWajib = ['id_resep', 'id_obat', 'jumlah', 'dosis_aturan_pakai'];
    protected array $kolomNull = ['instruksi_racikan', 'catatan'];
    protected array $kolomBulat = ['id_detail', 'id_resep', 'id_obat', 'jumlah'];
    protected array $kolomSaringan = ['id_resep', 'id_obat'];

    protected $validationRules = [
        'id_resep' => 'required|is_natural_no_zero|is_not_unique[resep.id_resep]',
        'id_obat' => 'required|is_natural_no_zero|is_not_unique[obat.id_obat]',
        'jumlah' => 'required|integer|greater_than[0]',
        'dosis_aturan_pakai' => 'required|max_length[255]',
        'instruksi_racikan' => 'permit_empty',
        'catatan' => 'permit_empty',
    ];

    protected $validationMessages = [
        'id_resep' => [
            'is_not_unique' => 'Resep tidak ditemukan.',
        ],
        'id_obat' => [
            'is_not_unique' => 'Obat tidak ditemukan.',
        ],
    ];
}
