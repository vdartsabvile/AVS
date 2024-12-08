package io.vdartsabvile.avspeak

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread

class VoiceChatClient(private val serverAddress: String, private val serverPort: Int) {
    private val socket = DatagramSocket()
    private val targetAddress = InetAddress.getByName(serverAddress)

    // Запись и отправка звука
    fun startRecordingAndSending() {
        val format = AudioFormat(16000f, 16, 1, true, false) // Формат аудио
        val line = AudioSystem.getTargetDataLine(format)
        line.open(format)
        line.start()

        thread {
            while (true) {
                val buffer = ByteArray(1024)
                val bytesRead = line.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    val packet = DatagramPacket(buffer, bytesRead, targetAddress, serverPort)
                    socket.send(packet)
                }
            }
        }
    }

    // Получение и воспроизведение звука
    fun startReceivingAndPlaying() {
        val format = AudioFormat(16000f, 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format)
        line.start()

        thread {
            while (true) {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                line.write(packet.data, 0, packet.length)
            }
        }
    }
}

fun main() {
    val client = VoiceChatClient("localhost", 12345)
    client.startRecordingAndSending()
    client.startReceivingAndPlaying()
}
