<?php

namespace App\Controllers\Api;

use App\Models\PoliklinikModel;

class PoliklinikController extends ApiResourceController
{
    protected string $modelClass = PoliklinikModel::class;
}
