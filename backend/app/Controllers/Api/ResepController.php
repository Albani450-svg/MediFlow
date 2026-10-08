<?php

namespace App\Controllers\Api;

use App\Models\ResepModel;

class ResepController extends ApiResourceController
{
    protected string $modelClass = ResepModel::class;
}
