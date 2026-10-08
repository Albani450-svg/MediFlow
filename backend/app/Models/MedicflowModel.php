<?php

namespace App\Models;

use CodeIgniter\Model;

/**
 * Model tabel medicflow_db. Angka, boolean, dan waktu dinormalkan
 * supaya JSON API mendekati bentuk yang dipakai PWA.
 */
abstract class MedicflowModel extends Model
{
    protected $returnType = 'array';
    protected $useAutoIncrement = true;
    protected $protectFields = true;
    protected $useTimestamps = true;
    protected $dateFormat = 'datetime';
    protected $createdField = 'created_at';
    protected $updatedField = 'updated_at';
    protected $skipValidation = false;
    protected $cleanValidationRules = true;

    protected string $nama = '';

    /** @var list<string> */
    protected array $kolomWajib = [];

    /** @var list<string> */
    protected array $kolomNull = [];

    /** Teks yang boleh string kosong, misalnya diagnosis yang belum diisi. */
    /** @var list<string> */
    protected array $kolomBolehKosong = [];

    /** @var list<string> */
    protected array $kolomBoolean = [];

    /** @var list<string> */
    protected array $kolomBulat = [];

    /** @var list<string> */
    protected array $kolomDesimal = [];

    /** @var list<string> */
    protected array $kolomTanggal = [];

    /** @var list<string> */
    protected array $kolomWaktu = [];

    /** @var list<string> */
    protected array $kolomJam = [];

    /** Query GET yang boleh dipakai menyaring daftar. */
    /** @var list<string> */
    protected array $kolomSaringan = [];

    protected $afterFind = ['normalkanBaris'];

    public function nama(): string
    {
        return $this->nama;
    }

    public function kunci(): string
    {
        return $this->primaryKey;
    }

    /** @return list<string> */
    public function wajib(): array
    {
        return $this->kolomWajib;
    }

    /** @return list<string> */
    public function bolehNull(): array
    {
        return $this->kolomNull;
    }

    /** @return list<string> */
    public function bolehKosong(): array
    {
        return $this->kolomBolehKosong;
    }

    /** @return list<string> */
    public function boolean(): array
    {
        return $this->kolomBoolean;
    }

    /** @return list<string> */
    public function bulat(): array
    {
        return $this->kolomBulat;
    }

    /** @return list<string> */
    public function desimal(): array
    {
        return $this->kolomDesimal;
    }

    /** @return list<string> */
    public function tanggal(): array
    {
        return $this->kolomTanggal;
    }

    /** @return list<string> */
    public function waktu(): array
    {
        return $this->kolomWaktu;
    }

    /** @return list<string> */
    public function jam(): array
    {
        return $this->kolomJam;
    }

    /** @return list<string> */
    public function saringan(): array
    {
        return $this->kolomSaringan;
    }

    /**
     * @param array<string, mixed> $event
     * @return array<string, mixed>
     */
    protected function normalkanBaris(array $event): array
    {
        if (! isset($event['data']) || $event['data'] === null || ! is_array($event['data'])) {
            return $event;
        }

        $satu = (bool) ($event['singleton'] ?? false);
        $baris = $satu ? [$event['data']] : $event['data'];

        foreach ($baris as $indeks => $item) {
            if (is_array($item)) {
                $baris[$indeks] = $this->normalkanSatu($item);
            }
        }

        $event['data'] = $satu ? ($baris[0] ?? null) : $baris;

        return $event;
    }

    /**
     * @param array<string, mixed> $baris
     * @return array<string, mixed>
     */
    private function normalkanSatu(array $baris): array
    {
        foreach ($this->kolomBulat as $kolom) {
            if (array_key_exists($kolom, $baris) && $baris[$kolom] !== null && $baris[$kolom] !== '') {
                $baris[$kolom] = (int) $baris[$kolom];
            }
        }

        foreach ($this->kolomDesimal as $kolom) {
            if (array_key_exists($kolom, $baris) && $baris[$kolom] !== null && $baris[$kolom] !== '') {
                $baris[$kolom] = (float) $baris[$kolom];
            }
        }

        foreach ($this->kolomBoolean as $kolom) {
            if (array_key_exists($kolom, $baris) && $baris[$kolom] !== null) {
                $nilai = $baris[$kolom];
                $baris[$kolom] = $nilai === true || $nilai === 1 || $nilai === '1';
            }
        }

        return $baris;
    }
}
