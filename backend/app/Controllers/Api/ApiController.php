<?php

namespace App\Controllers\Api;

use App\Controllers\BaseController;
use CodeIgniter\HTTP\ResponseInterface;
use Throwable;

abstract class ApiController extends BaseController
{
    protected function ok(string $message, mixed $data = null, int $kode = 200): ResponseInterface
    {
        $body = [
            'status' => true,
            'message' => $message,
        ];

        if (func_num_args() > 1) {
            $body['data'] = $data;
        }

        return $this->response->setStatusCode($kode)->setJSON($body, true);
    }

    /**
     * @param array<string, mixed> $errors
     */
    protected function gagal(string $message, int $kode, array $errors = []): ResponseInterface
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

    /**
     * @return array<string, mixed>|ResponseInterface
     */
    protected function badan(): array|ResponseInterface
    {
        try {
            $data = $this->request->getJSON(true);
        } catch (Throwable) {
            return $this->gagal('Body JSON tidak valid.', 400);
        }

        if (! is_array($data) || array_is_list($data)) {
            return $this->gagal($data === [] ? 'Data tidak boleh kosong' : 'Body JSON tidak valid.', 400);
        }

        return $data;
    }
}
