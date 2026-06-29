package ru.agromarket.data

import kotlinx.serialization.json.Json

/**
 * Единый JSON-конфиг для сети: используется и Retrofit-конвертером (AppModule),
 * и разбором ошибок FastAPI `detail` в AgroRepository.safeCall.
 *
 * - ignoreUnknownKeys: бэкенд может прислать лишние поля — не падать на них.
 * - coerceInputValues: null для non-null поля с дефолтом сводится к дефолту, а не к падению.
 */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
