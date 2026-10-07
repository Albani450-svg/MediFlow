<?php

use CodeIgniter\Router\RouteCollection;

/**
 * @var RouteCollection $routes
 */
$routes->get('/', 'Home::index');

$routes->get('api/database', 'Api\DatabaseController::index');
$routes->put('api/database', 'Api\DatabaseController::update');
$routes->post('api/database', 'Api\DatabaseController::update');

$routes->get('api/pasien', 'Api\PasienController::index');
$routes->get('api/pasien/(:num)', 'Api\PasienController::show/$1');
$routes->post('api/pasien', 'Api\PasienController::create');
$routes->put('api/pasien/(:num)', 'Api\PasienController::update/$1');
$routes->delete('api/pasien/(:num)', 'Api\PasienController::delete/$1');