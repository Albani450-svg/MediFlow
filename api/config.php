<?php

declare(strict_types=1);

function koneksiMedicflow(): mysqli
{
    $host = getenv('MEDIFLOW_DB_HOST') ?: '127.0.0.1';
    $user = getenv('MEDIFLOW_DB_USER') ?: 'root';
    $pass = getenv('MEDIFLOW_DB_PASS') !== false ? (string) getenv('MEDIFLOW_DB_PASS') : '';
    $name = getenv('MEDIFLOW_DB_NAME') ?: 'medicflow_db';
    $db = new mysqli($host, $user, $pass, $name, 3306);
    $db->set_charset('utf8mb4');
    return $db;
}
