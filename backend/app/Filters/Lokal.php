<?php

namespace App\Filters;

use CodeIgniter\Filters\FilterInterface;
use CodeIgniter\HTTP\RequestInterface;
use CodeIgniter\HTTP\ResponseInterface;

/**
 * API pengembangan hanya menjawab permintaan dari komputer ini.
 * Proksi Vite tetap terlihat sebagai 127.0.0.1.
 */
class Lokal implements FilterInterface
{
    public function before(RequestInterface $request, $arguments = null)
    {
        $asal = (string) $request->getServer('REMOTE_ADDR');
        if (in_array($asal, ['127.0.0.1', '::1'], true)) {
            return null;
        }

        return service('response')->setStatusCode(403)->setJSON([
            'ok' => false,
            'status' => false,
            'message' => 'API hanya melayani komputer ini.',
            'error' => 'API hanya melayani komputer ini.',
        ], true);
    }

    public function after(RequestInterface $request, ResponseInterface $response, $arguments = null)
    {
        return null;
    }
}
