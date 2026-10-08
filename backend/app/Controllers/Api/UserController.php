<?php

namespace App\Controllers\Api;

use App\Models\UserModel;

class UserController extends ApiResourceController
{
    protected string $modelClass = UserModel::class;
}
