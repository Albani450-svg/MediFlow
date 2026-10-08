<?php

namespace App\Controllers\Api;

use App\Models\DokterModel;

class DokterController extends ApiResourceController
{
    protected string $modelClass = DokterModel::class;
}
