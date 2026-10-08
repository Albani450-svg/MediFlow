<?php

namespace App\Controllers\Api;

use App\Models\MedicflowModel;
use CodeIgniter\HTTP\ResponseInterface;
use Throwable;

/**
 * CRUD JSON untuk satu tabel. Controller turunan mengisi model
 * dan, bila perlu, aturan bisnis di periksa().
 */
abstract class ApiResourceController extends ApiController
{
    /** @var class-string<MedicflowModel> */
    protected string $modelClass;

    public function index()
    {
        $model = $this->modelBaru();

        foreach ($model->saringan() as $kolom) {
            $nilai = $this->request->getGet($kolom);
            if ($nilai === null || $nilai === '' || ! is_scalar($nilai)) {
                continue;
            }
            if (in_array($kolom, $model->boolean(), true)) {
                $model->where($kolom, $this->nyala($nilai) ? 1 : 0);
                continue;
            }
            $model->where($kolom, $nilai);
        }

        return $this->ok(
            'Data ' . $model->nama() . ' berhasil diambil',
            $model->orderBy($model->kunci(), 'ASC')->findAll(),
        );
    }

    public function show($id = null)
    {
        $model = $this->modelBaru();
        $data = $model->find($id);

        if (! $data) {
            return $this->gagal('Data ' . $model->nama() . ' tidak ditemukan', 404);
        }

        return $this->ok('Data ' . $model->nama() . ' berhasil diambil', $data);
    }

    public function create()
    {
        $model = $this->modelBaru();
        $data = $this->badan();
        if ($data instanceof ResponseInterface) {
            return $data;
        }

        $data = $this->siapkan($model, $data);
        $kurang = $this->kurang($model, $data);
        if ($kurang !== []) {
            return $this->gagal('Data ' . $model->nama() . ' belum lengkap', 400, [
                'wajib' => $kurang,
            ]);
        }

        $pesan = $this->periksa($data, [], null);
        if ($pesan !== null) {
            return $this->gagal($pesan, 422);
        }

        $galat = $this->lolos($model, $data, null);
        if ($galat !== null) {
            return $this->gagal('Data ' . $model->nama() . ' gagal ditambahkan', 422, $galat);
        }

        try {
            $hasil = $model->skipValidation(true)->insert($data);
        } catch (Throwable $error) {
            return $this->galatBasis($model, $error, 'Data ' . $model->nama() . ' gagal ditambahkan');
        }

        if ($hasil === false) {
            return $this->gagal('Data ' . $model->nama() . ' gagal ditambahkan', 422, $model->errors());
        }

        return $this->ok('Data ' . $model->nama() . ' berhasil ditambahkan', [
            $model->kunci() => (int) $hasil,
        ], 201);
    }

    public function update($id = null)
    {
        $ada = $this->modelBaru()->find($id);
        if (! $ada) {
            $model = $this->modelBaru();

            return $this->gagal('Data ' . $model->nama() . ' tidak ditemukan', 404);
        }

        $data = $this->badan();
        if ($data instanceof ResponseInterface) {
            return $data;
        }

        $model = $this->modelBaru();
        $data = $this->siapkan($model, $data);
        if ($data === []) {
            return $this->gagal('Data pembaruan tidak boleh kosong', 400);
        }

        $pesan = $this->periksa($data, $ada, $id);
        if ($pesan !== null) {
            return $this->gagal($pesan, 422);
        }

        $galat = $this->lolos($model, $data, $id);
        if ($galat !== null) {
            return $this->gagal('Data ' . $model->nama() . ' gagal diperbarui', 422, $galat);
        }

        try {
            $berhasil = $model->skipValidation(true)->update($id, $data);
        } catch (Throwable $error) {
            return $this->galatBasis($model, $error, 'Data ' . $model->nama() . ' gagal diperbarui');
        }

        if ($berhasil === false) {
            return $this->gagal('Data ' . $model->nama() . ' gagal diperbarui', 422, $model->errors());
        }

        return $this->ok('Data ' . $model->nama() . ' berhasil diperbarui');
    }

    public function delete($id = null)
    {
        $model = $this->modelBaru();
        if (! $model->find($id)) {
            return $this->gagal('Data ' . $model->nama() . ' tidak ditemukan', 404);
        }

        try {
            $berhasil = $this->modelBaru()->delete($id);
        } catch (Throwable $error) {
            return $this->galatBasis($model, $error, 'Data ' . $model->nama() . ' tidak bisa dihapus karena masih dipakai.');
        }

        if ($berhasil === false) {
            return $this->gagal('Data ' . $model->nama() . ' tidak bisa dihapus karena masih dipakai.', 409);
        }

        return $this->ok('Data ' . $model->nama() . ' berhasil dihapus');
    }

    /**
     * Aturan di luar validasi kolom. Kembalikan kalimat galat, atau null bila lolos.
     *
     * @param array<string, mixed> $data
     * @param array<string, mixed> $sebelum
     */
    protected function periksa(array $data, array $sebelum, int|string|null $id): ?string
    {
        return null;
    }

    protected function nyala(mixed $nilai): bool
    {
        if (is_bool($nilai)) {
            return $nilai;
        }
        if (is_int($nilai) || is_float($nilai)) {
            return (int) $nilai === 1;
        }

        return in_array(strtolower(trim((string) $nilai)), ['1', 'true', 'ya'], true);
    }

    protected function modelBaru(): MedicflowModel
    {
        $kelas = $this->modelClass;

        return new $kelas();
    }

    /**
     * @param array<string, mixed> $data
     * @return array<string, mixed>
     */
    private function siapkan(MedicflowModel $model, array $data): array
    {
        unset($data[$model->kunci()], $data['created_at'], $data['updated_at']);

        foreach ($model->bolehNull() as $kolom) {
            if (array_key_exists($kolom, $data) && $data[$kolom] === '') {
                $data[$kolom] = null;
            }
        }

        foreach ($model->boolean() as $kolom) {
            if (array_key_exists($kolom, $data) && $data[$kolom] !== null && $data[$kolom] !== '') {
                $data[$kolom] = $this->nyala($data[$kolom]) ? 1 : 0;
            }
        }

        foreach ($model->tanggal() as $kolom) {
            if (! empty($data[$kolom]) && is_string($data[$kolom]) && preg_match('/^\d{4}-\d{2}-\d{2}/', $data[$kolom]) === 1) {
                $data[$kolom] = substr($data[$kolom], 0, 10);
            }
        }

        foreach ($model->waktu() as $kolom) {
            if (! empty($data[$kolom]) && is_string($data[$kolom])) {
                $sql = $this->keSqlWaktu($data[$kolom]);
                if ($sql !== null) {
                    $data[$kolom] = $sql;
                }
            }
        }

        foreach ($model->jam() as $kolom) {
            if (! empty($data[$kolom]) && is_string($data[$kolom])) {
                $jam = $this->keJam($data[$kolom]);
                if ($jam !== null) {
                    $data[$kolom] = $jam;
                }
            }
        }

        foreach ($model->bulat() as $kolom) {
            if ($kolom === $model->kunci()) {
                continue;
            }
            if (array_key_exists($kolom, $data) && is_numeric($data[$kolom])) {
                $data[$kolom] = (int) $data[$kolom];
            }
        }

        foreach ($model->desimal() as $kolom) {
            if (array_key_exists($kolom, $data) && is_numeric($data[$kolom])) {
                $data[$kolom] = (float) $data[$kolom];
            }
        }

        return $data;
    }

    /**
     * @param array<string, mixed> $data
     * @return list<string>
     */
    private function kurang(MedicflowModel $model, array $data): array
    {
        $boleh = $model->bolehKosong();

        return array_values(array_filter(
            $model->wajib(),
            static function (string $kolom) use ($data, $boleh): bool {
                if (! array_key_exists($kolom, $data) || $data[$kolom] === null) {
                    return true;
                }

                return $data[$kolom] === '' && ! in_array($kolom, $boleh, true);
            },
        ));
    }

    /**
     * @param array<string, mixed> $data
     * @return array<string, string>|null
     */
    private function lolos(MedicflowModel $model, array $data, int|string|null $id): ?array
    {
        $cek = $data;
        foreach ($model->bolehNull() as $kolom) {
            if (array_key_exists($kolom, $cek) && $cek[$kolom] === null) {
                $cek[$kolom] = '';
            }
        }

        if ($id !== null) {
            $aturan = $model->getValidationRules();
            $token = '{' . $model->kunci() . '}';
            $nilai = (string) (int) $id;
            foreach ($aturan as $kolom => $rule) {
                if (is_string($rule) && str_contains($rule, $token)) {
                    $aturan[$kolom] = str_replace($token, $nilai, $rule);
                }
            }
            $model->setValidationRules($aturan);
        }

        if ($model->validate($cek)) {
            return null;
        }

        $galat = $model->errors();

        return $galat === [] ? ['data' => 'Data tidak valid.'] : $galat;
    }

    private function galatBasis(MedicflowModel $model, Throwable $error, string $aksi): ResponseInterface
    {
        $pesan = $error->getMessage();
        $kode = 422;

        if (str_contains($pesan, '1062') || str_contains($pesan, 'Duplicate')) {
            $kode = 409;
            $aksi = 'Data ' . $model->nama() . ' sudah ada.';
        } elseif (
            str_contains($pesan, '1451')
            || str_contains($pesan, '1452')
            || str_contains(strtolower($pesan), 'foreign key')
        ) {
            $kode = 409;
            $aksi = 'Data ' . $model->nama() . ' masih terhubung ke catatan lain.';
        }

        return $this->gagal($aksi, $kode, [
            'database' => $pesan,
        ]);
    }

    private function keSqlWaktu(string $nilai): ?string
    {
        $nilai = trim(str_replace('T', ' ', $nilai));
        $nilai = preg_replace('/(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/', '', $nilai) ?? $nilai;
        $waktu = \DateTimeImmutable::createFromFormat('Y-m-d H:i:s', $nilai);

        if ($waktu === false || $waktu->format('Y-m-d H:i:s') !== $nilai) {
            return null;
        }

        return $nilai;
    }

    private function keJam(string $nilai): ?string
    {
        if (preg_match('/^(\d{2}):(\d{2})(?::(\d{2}))?$/', trim($nilai), $cocok) !== 1) {
            return null;
        }

        $jam = (int) $cocok[1];
        $menit = (int) $cocok[2];
        $detik = (int) ($cocok[3] ?? '0');
        if ($jam > 23 || $menit > 59 || $detik > 59) {
            return null;
        }

        return sprintf('%02d:%02d:%02d', $jam, $menit, $detik);
    }
}
