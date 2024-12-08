package io.vdartsabvile.avspeak

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

// Класс для сервера
class VoiceChatServer(private val port: Int) {
    private val serverSocket = DatagramSocket(port)
    private val clients = mutableListOf<InetAddress>()

    fun start() {
        println("Server started on port $port")

        // Получаем данные от клиентов
        thread {
            while (true) {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                serverSocket.receive(packet)

                // Добавляем клиента в список (на всякий случай)
                if (!clients.contains(packet.address)) {
                    clients.add(packet.address)
                    println("New client connected: ${packet.address}")
                }

                // Отправляем полученные данные всем клиентам
                for (client in clients) {
                    val responsePacket = DatagramPacket(packet.data, packet.length, client, packet.port)
                    serverSocket.send(responsePacket)
                }
            }
        }
    }
}

fun main() {
    val server = VoiceChatServer(12345)
    server.start()
}
