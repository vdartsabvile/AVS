package io.vdartsabvile.avspeak

import javafx.scene.image.Image
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.InputStream
import java.sql.Connection

class UserManager {
    private val sqliteConnector = SQLiteConnector()
    private val connection: Connection = sqliteConnector.connect()

    init {
        // Создание таблицы пользователей, если она не существует
        createUserTable()
    }

    private fun createUserTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            );
        """
        connection.createStatement().execute(sql)
    }

    fun registerUser(username: String, password: String, ipAddress: String, token: String): Boolean {
        if (userExists(username)) {
            return false // Пользователь уже существует
        }

        val sql = "INSERT INTO users (username, password) VALUES (?, ?)"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, username)
        preparedStatement.setString(2, password) // Лучше хешировать пароль
        preparedStatement.executeUpdate()

        // После успешной регистрации загружаем данные пользователя в облако
        val userInfo = mapOf(
            "username" to username,
            "password" to password, // Лучше хешировать пароль
            "ipAddress" to (ipAddress ?: "Неизвестный IP")
        )
        uploadUserInfoToYandexDisk(userInfo, token, username) // Передаем username для уникальности

        return true
    }

    // Измененный метод для входа, использующий данные с Яндекс Диска
    fun loginUser(username: String, password: String, token: String): Pair<Boolean, Image?> {
        // Загружаем данные пользователя с Яндекс Диска
        val userInfo = downloadUserInfoFromYandexDisk(token, username)

        // Проверяем, что файл загружен и содержит нужные данные
        if (userInfo != null) {
            val storedUsername = userInfo.optString("username")
            val storedPassword = userInfo.optString("password")

            // Сравниваем введённые данные с данными из загруженного файла
            if (username == storedUsername && password == storedPassword) {
                println("Вход выполнен успешно")

                // Загружаем аватар пользователя после успешного логина
                val avatarImage = downloadAvatarFromYandexDisk(token, username)
                return Pair(true, avatarImage)
            }
        }

        println("Неверные имя пользователя или пароль")
        return Pair(false, null)
    }


    // Метод для загрузки данных пользователя с Яндекс Диска
    // Метод для загрузки данных пользователя с Яндекс Диска
    private fun downloadUserInfoFromYandexDisk(token: String, username: String): JSONObject? {
        val client = OkHttpClient()
        val downloadPath = "AVSpeak/Users/user_info_$username.json" // Используем имя пользователя в пути

        // Запрос на получение ссылки для скачивания файла
        val request = Request.Builder()
            .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=$downloadPath")
            .addHeader("Authorization", "OAuth $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val downloadUrl = JSONObject(response.body?.string()).getString("href")

                // Запрос для скачивания файла
                val fileRequest = Request.Builder()
                    .url(downloadUrl)
                    .build()

                client.newCall(fileRequest).execute().use { fileResponse ->
                    if (fileResponse.isSuccessful) {
                        val jsonResponse = fileResponse.body?.string()
                        return JSONObject(jsonResponse)
                    } else {
                        println("Ошибка при загрузке файла с Яндекс.Диска: ${fileResponse.message}")
                    }
                }
            } else {
                println("Ошибка при запросе URL для загрузки информации с Яндекс.Диска: ${response.message}")
            }
        }
        return null
    }

    fun downloadAvatarFromYandexDisk(token: String, username: String): Image? {
        val client = OkHttpClient()
        val downloadPath = "AVSpeak/Avatars/avatar_$username.png" // Путь к аватару

        // Запрос на получение ссылки для скачивания файла
        val request = Request.Builder()
            .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=$downloadPath")
            .addHeader("Authorization", "OAuth $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val downloadUrl = JSONObject(response.body?.string()).getString("href")

                // Запрос для скачивания изображения
                val fileRequest = Request.Builder()
                    .url(downloadUrl)
                    .build()

                client.newCall(fileRequest).execute().use { fileResponse ->
                    if (fileResponse.isSuccessful) {
                        println("Аватар успешно загружен с яндекс диска!")
                        // Получаем InputStream и создаем Image
                        val inputStream: InputStream? = fileResponse.body?.byteStream()
                        return if (inputStream != null) Image(inputStream) else null
                    } else {
                        println("Ошибка при загрузке аватара: ${fileResponse.message}")
                    }
                }
            } else {
                println("Ошибка при запросе URL для загрузки аватара: ${response.message}")
            }
        }
        return null
    }
    fun downloadFriendAvatarFromYandexDisk(token: String, friendUsername: String): Image? {
        val client = OkHttpClient()
        val downloadPath = "AVSpeak/Avatars/avatar_$friendUsername.png" // Путь к аватару

        // Запрос на получение ссылки для скачивания файла
        val request = Request.Builder()
            .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=$downloadPath")
            .addHeader("Authorization", "OAuth $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val downloadUrl = JSONObject(response.body?.string()).getString("href")

                // Запрос для скачивания изображения
                val fileRequest = Request.Builder()
                    .url(downloadUrl)
                    .build()

                client.newCall(fileRequest).execute().use { fileResponse ->
                    if (fileResponse.isSuccessful) {
                        //println("Аватар успешно загружен с яндекс диска!")
                        // Получаем InputStream и создаем Image
                        val inputStream: InputStream? = fileResponse.body?.byteStream()
                        return if (inputStream != null) Image(inputStream) else null
                    } else {
                        println("Ошибка при загрузке аватара: ${fileResponse.message}")
                    }
                }
            } else {
                println("Ошибка при запросе URL для загрузки аватара: ${response.message}")
            }
        }
        return null
    }

    private fun userExists(username: String): Boolean {
        val sql = "SELECT * FROM users WHERE username = ?"
        val preparedStatement = connection.prepareStatement(sql)
        preparedStatement.setString(1, username)
        val resultSet = preparedStatement.executeQuery()
        return resultSet.next() // Возвращает true, если пользователь найден
    }

    fun uploadUserInfoToYandexDisk(userInfo: Map<String, String>, token: String, username: String) {
        val client = OkHttpClient()

        // Создаем JSON-объект с информацией о пользователе
        val userJson = JSONObject(userInfo)

        // Сохраняем JSON-объект в локальный файл с фиксированным именем
        val tempFile = createTempFile("user_info", ".json").apply {
            writeText(userJson.toString())
        }

        // Указываем, что файл должен загружаться как "AVSpeak/user_info_<username>.json" на Диске
        val uploadPath = "AVSpeak/Users/user_info_$username.json" // Уникальный путь для каждого пользователя
        val request = Request.Builder()
            .url("https://cloud-api.yandex.net/v1/disk/resources/upload?path=$uploadPath&overwrite=true")
            .addHeader("Authorization", "OAuth $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                // Получаем URL для загрузки
                val uploadUrl = JSONObject(response.body?.string()).getString("href")

                // Загружаем файл по указанному URL
                val fileRequest = Request.Builder()
                    .url(uploadUrl)
                    .put(tempFile.asRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(fileRequest).execute().use { fileResponse ->
                    if (fileResponse.isSuccessful) {
                        println("Информация о пользователе успешно загружена на Яндекс.Диск под именем $uploadPath")
                    } else {
                        println("Ошибка загрузки инфы на яндекс: ${fileResponse.message}")
                    }
                }
            } else {
                println("Ошибка при запросе URL для загрузки информации на Яндекс.Диск: ${response.message}")
            }
        }

        // Удаляем временный файл после загрузки, если это необходимо
        tempFile.delete()
    }



}
