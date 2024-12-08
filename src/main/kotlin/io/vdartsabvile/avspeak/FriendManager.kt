package io.vdartsabvile.avspeak

import javafx.scene.image.Image
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class Friend(val friendUsername: String, val friendIp: String, val avatarImage: Image? = null,
                  var isOnline: Boolean = false // Новый атрибут для статуса онлайн
)

class FriendManager {

    private val userManager = UserManager()

    // Метод для загрузки данных пользователя с Яндекс Диска
    fun downloadFriendsInfoFromYandexDisk(token: String, username: String): List<Friend> {
        val client = OkHttpClient()
        val downloadPath = "AVSpeak/Friends/friends_info_$username.json"



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
                    //println("URL для скачивания файла: $downloadUrl")


                client.newCall(fileRequest).execute().use { fileResponse ->
                    if (fileResponse.isSuccessful) {
                        val jsonString = fileResponse.body?.string()
                        val friendsInfo = JSONObject(jsonString)
                        //println("Ответ JSON: $jsonString")


                        // Получаем массив друзей под ключом "friends"
                        val friendsArray = friendsInfo.getJSONArray("friends")
                        val friendsList = mutableListOf<Friend>()

                        // Итерация по массиву друзей
                        for (i in 0 until friendsArray.length()) {
                            val friendInfo = friendsArray.getJSONObject(i)
                            val friendUsername = friendInfo.getString("friendUsername")
                            val friendIp = friendInfo.getString("friendIp")

                            // Загрузка аватара, если указан путь
                            val avatarImage = try {
                                userManager.downloadFriendAvatarFromYandexDisk(token, friendUsername)
                            } catch (e: Exception) {
                                println("Ошибка при загрузке аватара для $friendUsername: ${e.message}")
                                null
                            }


                            friendsList.add(Friend(friendUsername, friendIp, avatarImage))
                        }
                        //println("Загружено друзей: ${friendsList.size}")
                        return friendsList
                    }
                    else {
                        println("Ошибка при скачивании файла с друзьями: ${fileResponse.code}")
                    }
                }
            }
            else {
                println("Ошибка первого запроса: ${response.code}, сообщение: ${response.body?.string()}")
            }
        }
        println("Вернулся пустой лист")
        return emptyList()
    }


    // Метод для загрузки информации о друзьях
    fun uploadFriendsInfoToYandexDisk(userInfo: Map<String, String>, friendsList: List<Friend>, token: String, username: String) {
        val client = OkHttpClient()

        // Создаем JSON-объект с именем пользователя
        val userJson = JSONObject(userInfo)

        // Добавляем информацию о друзьях
        val friendsJsonArray = JSONArray()
        for (friend in friendsList) {
            val friendJson = JSONObject()
            friendJson.put("friendUsername", friend.friendUsername)
            friendJson.put("friendIp", friend.friendIp)
            friendsJsonArray.put(friendJson)
        }

        // Включаем список друзей в итоговый JSON
        userJson.put("friends", friendsJsonArray)

        // Сохраняем JSON-объект в локальный файл
        val tempFile = createTempFile("friends_info", ".json").apply {
            writeText(userJson.toString())
        }

        // Указываем путь для загрузки на Диск
        val uploadPath = "AVSpeak/Friends/friends_info_$username.json"
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
                        println("Информация о друзьях пользователя успешно загружена на Яндекс.Диск под именем $uploadPath")
                    } else {
                        println("Ошибка загрузки: ${fileResponse.message}")
                    }
                }

            } else {
                println("Ошибка при запросе URL для загрузки: ${response.message}")
            }
        }

        // Удаляем временный файл после загрузки
        tempFile.delete()
    }
    fun getUserIpFromYandexDisk(token: String, username: String): String? {
        val userInfo = downloadUserInfoFromYandexDisk(token, username)
        return userInfo?.optString("ipAddress", null)
    }
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


}
