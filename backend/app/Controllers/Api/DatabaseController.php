<?php

namespace App\Controllers\Api;

use App\Controllers\BaseController;
use App\Libraries\MedicflowSnapshot;
use Throwable;

class DatabaseController extends BaseController
{
    public function index()
    {
        try {
            $snap = new MedicflowSnapshot();

            return $this->response->setJSON([
                'ok' => true,
                'data' => $snap->baca(),
            ], true);
        } catch (Throwable $error) {
            return $this->response->setStatusCode(500)->setJSON([
                'ok' => false,
                'error' => $error->getMessage(),
            ], true);
        }
    }

    public function update()
    {
        $data = $this->request->getJSON(true);
        if (! is_array($data)) {
            return $this->response->setStatusCode(400)->setJSON([
                'ok' => false,
                'error' => 'Body JSON tidak valid.',
            ], true);
        }

        try {
            $snap = new MedicflowSnapshot();
            $snap->simpan($data);

            return $this->response->setJSON(['ok' => true], true);
        } catch (Throwable $error) {
            return $this->response->setStatusCode(500)->setJSON([
                'ok' => false,
                'error' => $error->getMessage(),
            ], true);
        }
    }
}
