package io.vdartsabvile.avspeak

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

fun uploadToYandexDisk(filePath: String, token: String, userInfo: Map<String, String>) {
    val client = OkHttpClient()
    val file = File(filePath)

    // Создаем JSON-объект с информацией о пользователе
    val userJson = JSONObject(userInfo)

    // Сохраняем JSON-объект в локальный файл
    val tempFile = createTempFile("user_info", ".json")
    tempFile.writeText(userJson.toString())

    // Запрос на получение URL для загрузки файла
    val request = Request.Builder()
        .url("https://cloud-api.yandex.net/v1/disk/resources/upload?path=${tempFile.name}&overwrite=true")
        .addHeader("Authorization", "OAuth $token")
        .build()

    client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
            // Получаем URL для загрузки
            val uploadUrl = JSONObject(response.body?.string()).getString("href")

            // Запрос для загрузки файла по полученному URL
            val fileRequest = Request.Builder()
                .url(uploadUrl)
                .put(tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .build()

            client.newCall(fileRequest).execute().use { fileResponse ->
                if (fileResponse.isSuccessful) {
                    println("Информация о пользователе успешно загружена на Яндекс.Диск")
                } else {
                    println("Ошибка загрузки: ${fileResponse.message}")
                }
            }
        } else {
            println("Ошибка при запросе URL для загрузки: ${response.message}")
        }
    }
}
