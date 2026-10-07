<?php

namespace App\Controllers\Api;

use App\Controllers\BaseController;
use App\Models\PasienModel;
use Throwable;

class PasienController extends BaseController
{
    private const WAJIB = [
        'id_user',
        'nik',
        'nama_lengkap',
        'tanggal_lahir',
        'jenis_kelamin',
        'no_telepon',
    ];

    protected PasienModel $pasienModel;

    public function __construct()
    {
        $this->pasienModel = new PasienModel();
    }

    public function index()
    {
        return $this->response->setJSON([
            'status' => true,
            'message' => 'Data pasien berhasil diambil',
            'data' => $this->pasienModel->findAll(),
        ], true);
    }

    public function show($id)
    {
        $data = $this->pasienModel->find($id);

        if (! $data) {
            return $this->gagal('Data pasien tidak ditemukan', 404);
        }

        return $this->response->setJSON([
            'status' => true,
            'message' => 'Data pasien berhasil diambil',
            'data' => $data,
        ], true);
    }

    public function create()
    {
        $data = $this->request->getJSON(true);

        if (! is_array($data) || $data === []) {
            return $this->gagal('Data tidak boleh kosong', 400);
        }

        $data = $this->siapkan($data);
        $kurang = array_values(array_filter(
            self::WAJIB,
            static fn (string $kolom): bool => ! array_key_exists($kolom, $data) || $data[$kolom] === '' || $data[$kolom] === null
        ));

        if ($kurang !== []) {
            return $this->gagal('Data pasien belum lengkap', 400, [
                'wajib' => $kurang,
            ]);
        }

        try {
            $berhasil = $this->pasienModel->insert($data);
        } catch (Throwable $error) {
            return $this->gagal('Data pasien gagal ditambahkan', 422, [
                'database' => $error->getMessage(),
            ]);
        }

        if ($berhasil === false) {
            return $this->gagal('Data pasien gagal ditambahkan', 422, $this->pasienModel->errors());
        }

        return $this->response->setJSON([
            'status' => true,
            'message' => 'Data pasien berhasil ditambahkan',
            'data' => [
                'id_pasien' => (int) $this->pasienModel->getInsertID(),
            ],
        ], true)->setStatusCode(201);
    }

    public function update($id = null)
    {
        $pasien = $this->pasienModel->find($id);

        if (! $pasien) {
            return $this->gagal('Data pasien tidak ditemukan', 404);
        }

        $data = $this->request->getJSON(true);

        if (! is_array($data) || $data === []) {
            return $this->gagal('Data pembaruan tidak boleh kosong', 400);
        }

        $data = $this->siapkan($data);

        if ($data === []) {
            return $this->gagal('Data pembaruan tidak boleh kosong', 400);
        }

        try {
            $berhasil = $this->pasienModel->update($id, $data);
        } catch (Throwable $error) {
            return $this->gagal('Data pasien gagal diperbarui', 422, [
                'database' => $error->getMessage(),
            ]);
        }

        if ($berhasil === false) {
            return $this->gagal('Data pasien gagal diperbarui', 422, $this->pasienModel->errors());
        }

        return $this->response->setJSON([
            'status' => true,
            'message' => 'Data pasien berhasil diperbarui',
        ], true);
    }

    public function delete($id = null)
    {
        $pasien = $this->pasienModel->find($id);

        if (! $pasien) {
            return $this->gagal('Data pasien tidak ditemukan', 404);
        }

        try {
            $this->pasienModel->delete($id);
        } catch (Throwable) {
            return $this->gagal('Data pasien tidak bisa dihapus karena masih dipakai kunjungan.', 409);
        }

        return $this->response->setJSON([
            'status' => true,
            'message' => 'Data pasien berhasil dihapus',
        ], true);
    }

    /**
     * @param array<string, mixed> $data
     * @return array<string, mixed>
     */
    private function siapkan(array $data): array
    {
        unset($data['id_pasien'], $data['created_at'], $data['updated_at']);

        foreach (['nomor_bpjs', 'alamat'] as $kolom) {
            if (array_key_exists($kolom, $data) && $data[$kolom] === '') {
                $data[$kolom] = null;
            }
        }

        if (array_key_exists('terdaftar_satusehat', $data)) {
            $nilai = $data['terdaftar_satusehat'];
            $data['terdaftar_satusehat'] = $nilai === true || $nilai === 1 || $nilai === '1';
        }

        return $data;
    }

    /**
     * @param array<string, mixed> $errors
     */
    private function gagal(string $message, int $kode, array $errors = [])
    {
        $body = [
            'status' => false,
            'message' => $message,
        ];

        if ($errors !== []) {
            $body['errors'] = $errors;
        }

        return $this->response->setStatusCode($kode)->setJSON($body, true);
    }
}
