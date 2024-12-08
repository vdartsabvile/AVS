package io.vdartsabvile.avspeak.Client

import io.vdartsabvile.avspeak.AppState
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class AVSpeakClient : Application() {
    private lateinit var audioReceiveSocket: DatagramSocket
    private lateinit var audioSendSocket: DatagramSocket

    private lateinit var endCallButton: Button
    private var audioThread: Thread? = null


    private lateinit var webSocket: WebSocketClient
    private lateinit var chatArea: TextArea
    private lateinit var messageField: TextField
    private lateinit var recipientField: TextField
    private lateinit var sendButton: Button
    private lateinit var callButton: Button
    private lateinit var statusButton: Button

    private lateinit var audioSocket: DatagramSocket
    private var isVoiceChatActive = false
    private var remoteIp: String? = null
    private var remotePort = 4000 // Укажите порт для передачи данных



    override fun start(stage: Stage) {
        // Тест JSON-парсера
        try {
            val jsonString = """{"name": "John", "age": 30}"""
            val jsonObject = JSONObject(jsonString)
            println("JSON test: ${jsonObject.getString("name")}, ${jsonObject.getInt("age")}")
        } catch (e: Exception) {
            println("Error testing JSON parser: ${e.message}")
        }
        val username = AppState.username ?: "Unknown User" // Здесь можно реализовать ввод имени пользователя

        // Создание элементов интерфейса
        chatArea = TextArea().apply { isEditable = false }
        messageField = TextField()
        recipientField = TextField().apply { promptText = "Recipient" }
        sendButton = Button("Send")
        callButton = Button("Call")
        statusButton = Button("Get Status")

        // Настройка действий кнопок
        sendButton.setOnAction { sendMessage() }
        callButton.setOnAction { initiateCall() }
        statusButton.setOnAction { getStatus() }

        endCallButton = Button("End Call").apply {
            isDisable = true
            setOnAction { endCall() }
        }

        // Обновляем компоновку интерфейса
        val layout = VBox(10.0, chatArea, recipientField, messageField, sendButton, callButton, endCallButton, statusButton)
        layout.padding = Insets(10.0)

        // Создание сцены и отображение окна
        val scene = Scene(layout, 400.0, 500.0)
        stage.title = "AVSpeak Client"
        stage.scene = scene
        stage.show()

        // Инициализация WebSocket
        initWebSocket(username)
    }

    private fun initWebSocket(username: String) {
        val serverUrl = "ws://localhost:3000?username=$username"
        webSocket = object : WebSocketClient(URI(serverUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Platform.runLater { appendToChatArea("Connected to server") }
            }

            override fun onMessage(message: String?) {
                message?.let {
                    val json = JSONObject(it)
                    when (json.getString("type")) {
                        "statusUpdate" -> handleStatusUpdate(json)
                        "chat" -> handleChatMessage(json)
                        "callRequest" -> handleCallRequest(json)
                        "callAccepted" -> handleCallAccepted(json)
                        "callRejected" -> handleCallRejected(json)
                        "callEnded" -> handleCallEnded(json)
                    }
                }
            }



            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Platform.runLater { appendToChatArea("Disconnected from server") }
            }

            override fun onError(ex: Exception?) {
                Platform.runLater { appendToChatArea("Error: ${ex?.message}") }
            }
        }
        webSocket.connect()
    }

    private fun sendMessage() {
        val recipient = recipientField.text
        val content = messageField.text
        if (recipient.isNotEmpty() && content.isNotEmpty()) {
            val message = JSONObject().apply {
                put("type", "chat")
                put("recipient", recipient)
                put("content", content)
            }
            webSocket.send(message.toString())
            messageField.clear()
        }
    }

    private fun initiateCall() {
        val recipient = recipientField.text
        if (recipient.isNotEmpty()) {
            val message = JSONObject().apply {
                put("type", "call")
                put("recipient", recipient)
            }
            webSocket.send(message.toString())
            appendToChatArea("Calling $recipient...")
        }
    }

    private fun getStatus() {
        val message = JSONObject().apply {
            put("type", "getStatus")
        }
        webSocket.send(message.toString())
    }

    private fun handleStatusUpdate(json: JSONObject) {
        val users = json.getJSONArray("users")
        val statusText = users.joinToString("\n") { user ->
            val userObj = user as JSONObject
            "${userObj.getString("username")}: ${if (userObj.getBoolean("isOnline")) "Online" else "Offline"}"
        }
        Platform.runLater { appendToChatArea("User Statuses:\n$statusText") }
    }

    private fun handleChatMessage(json: JSONObject) {
        val message = json.getJSONObject("message")
        val sender = message.getString("sender")
        val content = message.getString("content")
        Platform.runLater { appendToChatArea("$sender: $content") }
    }

    private fun handleCallRequest(json: JSONObject) {
        val from = json.getString("from")
        Platform.runLater {
            val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Incoming Call"
                headerText = "Call from $from"
                contentText = "Do you want to accept the call?"
            }
            alert.showAndWait().ifPresent { result ->
                if (result == ButtonType.OK) {
                    acceptCall(from)
                } else {
                    rejectCall(from)
                }
            }
        }
    }

    private fun acceptCall(from: String) {
        val localIp = InetAddress.getLocalHost().hostAddress
        val message = JSONObject().apply {
            put("type", "callAccept")
            put("recipient", from)
            put("ip", localIp)
            put("port", remotePort)
        }
        webSocket.send(message.toString())
        appendToChatArea("Call accepted from $from")
    }


    private fun rejectCall(from: String) {
        val message = JSONObject().apply {
            put("type", "callReject")
            put("recipient", from)
        }
        webSocket.send(message.toString())
        appendToChatArea("Call rejected from $from")
    }

    /*private fun handleCallAccepted(json: JSONObject) {
        val from = json.getString("from")
        val ip = json.optString("ip", InetAddress.getLocalHost().hostAddress)
        val port = json.optInt("port", remotePort)

        Platform.runLater {
            appendToChatArea("$from accepted your call")
            if (ip != null && port != -1) {
                startVoiceChat(ip, port)
            } else {
                appendToChatArea("Error: Missing IP or port information for voice chat")
            }
        }
    }*/
    private fun handleCallAccepted(json: JSONObject) {
        val from = json.getString("from")
        val ip = json.getString("ip") // Получаем IP собеседника
        val port = json.getInt("port") // Порт собеседника

        Platform.runLater {
            appendToChatArea("$from accepted your call")
            startVoiceChat(ip, port) // Используем IP и порт собеседника
        }
    }


    private fun handleCallRejected(json: JSONObject) {
        val from = json.getString("from")
        Platform.runLater { appendToChatArea("$from rejected your call") }
    }

    private fun handleCallEnded(json: JSONObject) {
        val from = json.getString("from")
        Platform.runLater {
            stopVoiceChat()
            appendToChatArea("Call ended by $from") }
    }

    private fun appendToChatArea(message: String) {
        chatArea.appendText("$message\n")
    }

    private fun startVoiceChat(remoteIp: String, remotePort: Int) {

        isVoiceChatActive = true
        this.remoteIp = remoteIp
        this.remotePort = remotePort

        // Инициализация сокетов
        audioSendSocket = DatagramSocket()
        audioReceiveSocket = DatagramSocket(remotePort)
        println("Сокеты успешно инициализированы: отправка на порту ${audioSendSocket.localPort}, прием на порту $remotePort")
        println("Отправка аудиопакетов на IP: $remoteIp, порт: $remotePort")

        audioThread = Thread {
            try {
                val format = getAudioFormat()
                val microphone = AudioSystem.getTargetDataLine(format)
                val speakers = AudioSystem.getSourceDataLine(format)

                microphone.open(format)
                speakers.open(format)

                microphone.start()
                speakers.start()

                println("Микрофон и динамики запущены")

                // Запуск потока приема
                startAudioReceiveThread(speakers)

                val buffer = ByteArray(4096)
                while (isVoiceChatActive) {
                    try {
                        // Отправка аудио
                        val bytesRead = microphone.read(buffer, 0, buffer.size)
                        if (bytesRead > 0) {
                            val sendPacket = DatagramPacket(buffer, bytesRead, InetAddress.getByName(remoteIp), remotePort)
                            audioSendSocket.send(sendPacket)
                            println("Отправлен аудиопакет: $bytesRead байт")
                        }
                    } catch (e: Exception) {
                        if (!isVoiceChatActive) break
                        e.printStackTrace()
                        println("Ошибка при отправке аудио: ${e.message}")
                    }
                }

                // Остановка устройств
                microphone.stop()
                microphone.close()
                speakers.stop()
                speakers.close()

                println("Микрофон и динамики остановлены")
            } catch (e: Exception) {
                e.printStackTrace()
                Platform.runLater { appendToChatArea("Ошибка при работе с аудио: ${e.message}") }
            }
        }

        audioThread?.start()
        println("Аудио поток запущен")
        Platform.runLater {
            appendToChatArea("Voice chat started")
            endCallButton.isDisable = false
        }
    }

    private fun startAudioReceiveThread(speakers: SourceDataLine) {
        Thread {
            val buffer = ByteArray(4096)
            while (isVoiceChatActive) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    audioReceiveSocket.receive(receivePacket)
                    println("Получен аудиопакет: ${receivePacket.length} байт")
                    speakers.write(receivePacket.data, 0, receivePacket.length)
                } catch (e: Exception) {
                    if (!isVoiceChatActive) break
                    e.printStackTrace()
                    println("Ошибка при приеме аудио: ${e.message}")
                }
            }
            println("Поток приема аудио остановлен")
        }.start()
    }

    private fun stopVoiceChat() {
        if (!isVoiceChatActive) return
        isVoiceChatActive = false
        audioThread?.join(1000)
        audioSendSocket.close()
        audioReceiveSocket.close()
        Platform.runLater {
            appendToChatArea("Voice chat stopped")
            endCallButton.isDisable = true
        }
    }



    private fun endCall() {
        if (!isVoiceChatActive) {
            appendToChatArea("No active call to end")
            return
        }
        stopVoiceChat()
        val recipient = recipientField.text
        if (recipient.isNotEmpty()) {
            val message = JSONObject().apply {
                put("type", "endCall")
                put("recipient", recipient)
            }
            webSocket.send(message.toString())
            appendToChatArea("Call ended with $recipient")
        } else {
            appendToChatArea("Recipient field is empty, but call stopped locally")
        }
    }


    private fun getAudioFormat(): AudioFormat {
        val sampleRate = 44100.0f
        val sampleSizeInBits = 16
        val channels = 1
        val signed = true
        val bigEndian = false
        return AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)
    }


}

fun main() {
    Application.launch(AVSpeakClient::class.java)
}