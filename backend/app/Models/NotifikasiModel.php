<?php

namespace App\Models;

class NotifikasiModel extends MedicflowModel
{
    protected $table = 'notifikasi';
    protected $primaryKey = 'id_notifikasi';
    protected $updatedField = '';
    protected string $nama = 'notifikasi';

    protected $allowedFields = [
        'id_pasien',
        'id_pendaftaran',
        'jenis_notifikasi',
        'channel',
        'nomor_tujuan',
        'pesan',
        'status',
        'waktu_dikirim',
        'response_api',
    ];

    protected array $kolomWajib = ['id_pasien', 'jenis_notifikasi', 'nomor_tujuan', 'pesan'];
    protected array $kolomNull = ['id_pendaftaran', 'waktu_dikirim', 'response_api'];
    protected array $kolomBulat = ['id_notifikasi', 'id_pasien', 'id_pendaftaran'];
    protected array $kolomWaktu = ['waktu_dikirim'];
    protected array $kolomSaringan = ['id_pasien', 'id_pendaftaran', 'jenis_notifikasi', 'channel', 'status'];

    protected $validationRules = [
        'id_pasien' => 'required|is_natural_no_zero|is_not_unique[pasien.id_pasien]',
        'id_pendaftaran' => 'permit_empty|is_natural_no_zero|is_not_unique[pendaftaran_poli.id_pendaftaran]',
        'jenis_notifikasi' => 'required|in_list[Verifikasi WA,Konfirmasi Pendaftaran,Pengingat Kunjungan,QR Resep,Obat Siap]',
        'channel' => 'if_exist|in_list[WhatsApp,PWA]',
        'nomor_tujuan' => 'required|max_length[20]|regex_match[/^08[0-9]{8,12}$/]',
        'pesan' => 'required',
        'status' => 'if_exist|in_list[Pending,Terkirim,Gagal]',
        'waktu_dikirim' => 'permit_empty|valid_date[Y-m-d H:i:s]',
        'response_api' => 'permit_empty',
    ];

    protected $validationMessages = [
        'id_pasien' => [
            'is_not_unique' => 'Pasien tidak ditemukan.',
        ],
        'id_pendaftaran' => [
            'is_not_unique' => 'Pendaftaran tidak ditemukan.',
        ],
        'nomor_tujuan' => [
            'regex_match' => 'Nomor tujuan harus diawali 08.',
        ],
    ];
}
