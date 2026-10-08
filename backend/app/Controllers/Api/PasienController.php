<?php

namespace App\Controllers\Api;

use App\Models\PasienModel;

class PasienController extends ApiResourceController
{
    protected string $modelClass = PasienModel::class;
}
