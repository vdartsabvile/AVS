package io.vdartsabvile.avspeak

import java.net.HttpURLConnection

class DisconnectServer {
     fun notifyServerLogout() {
        try {
            val url = java.net.URL("http://localhost:3000/api/updateStatus")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val output = """{"username": "${AppState.username}", "isOnline": false}"""
            connection.outputStream.use { it.write(output.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                println("Уведомление сервера об отключении успешно отправлено.")
            } else {
                println("Ошибка уведомления сервера: $responseCode")
            }
        } catch (e: Exception) {
            println("Ошибка при уведомлении сервера: ${e.message}")
        }
    }

}