package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.ConnectionWindow
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class AdminServerWindow(private val serverName: String, private val port: Int, private val ip: String
) : Application() {
    private var xOffset = 0.0
    private var yOffset = 0.0


    private var smallAvatarImageView = AppState.smallAvatarImageView



    private val participants = mutableListOf<String>() // Список участников
    private lateinit var participantsListView: ListView<String> // Объявляем participantsListView как свойство класса

    private lateinit var audioSocket: DatagramSocket
    private var isVoiceChatActive = false

    override fun start(primaryStage: Stage) {

        primaryStage.title = "Сервер: $serverName"

        // Информация о сервере
        val serverInfo = VBox().apply {
            padding = Insets(10.0)
            spacing = 10.0
            children.addAll(
                Label("Порт: $port"),
                Label("IP: $ip")
            )
        }

        // Список участников
        participantsListView = ListView<String>().apply {
            items.addAll(participants)

        }
        participantsListView.style = "-fx-text-fill: black;"  // Черный цвет текста

        // Оформление остального интерфейса
        val chatArea = TextArea().apply { isEditable = false }
        val chatInput = TextField()
        val sendButton = Button("Отправить").apply {
            setOnAction {
                val message = chatInput.text
                if (message.isNotBlank()) {
                    chatArea.appendText("Вы: $message\n")
                    chatInput.clear()
                }
            }
        }

        // Панель для ввода сообщений
        val chatInputBox = HBox(chatInput, sendButton).apply { spacing = 10.0 }


        val titleBar = HBox()
        titleBar.style = "-fx-padding: 10;"
        titleBar.alignment = Pos.CENTER_RIGHT
        val icon = Image(javaClass.getResource("/io/vdartsabvile/avspeak/icon.png")?.toExternalForm())
        val iconView = javafx.scene.image.ImageView(icon)
        iconView.fitWidth = 34.0
        iconView.fitHeight = 34.0
        iconView.isPreserveRatio = true
        HBox.setMargin(iconView, javafx.geometry.Insets(0.0, 0.0, 0.0, 8.0))

        val label2 = Label("AVSpeak")
        label2.style = "-fx-font-family: 'Secession [by me]' ;-fx-font-size: 38px; -fx-font-weight: normal;"

        // Кнопка закрытия
        val closeButton = Button("✖")
        closeButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        closeButton.setOnAction {
            StartUp().stopNodeServer()
            primaryStage.close() }
        // Кнопка сворачивания
        val minimizeButton = Button("−")
        minimizeButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        minimizeButton.setOnAction { primaryStage.isIconified = true }

        // Добавление кнопок на панель заголовка
        HBox.setHgrow(minimizeButton, Priority.ALWAYS)
        titleBar.children.addAll(iconView, label2)

        // Заполнение пространства слева
        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        titleBar.children.addAll(spacer, minimizeButton, closeButton)
        titleBar.alignment = Pos.CENTER_LEFT

        // Перетаскивание окна
        titleBar.setOnMousePressed { event: javafx.scene.input.MouseEvent ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }
        titleBar.setOnMouseDragged { event: javafx.scene.input.MouseEvent ->
            primaryStage.x = event.screenX - xOffset
            primaryStage.y = event.screenY - yOffset
        }
        // Добавляем правую панель с аватаром, настройками и серверами
        val rightPanel = createRightPanel(primaryStage)

        val headerLabel = Label("Сервер: $serverName")
        headerLabel.styleClass.add("savedd")
        headerLabel.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 70px;"

        // Создание формы для подключения

        // Создаем VBox для формы
        val formBox = VBox(20.0)
        formBox.style = "-fx-padding: 40; -fx-alignment: center;"
        formBox.children.addAll(headerLabel)
        formBox.alignment = Pos.CENTER

        // Основной макет
        val root = BorderPane()
        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()
        root.top = titleBar
        root.right = rightPanel // Добавляем правую панель
        root.center = formBox

        // Audio input control
        val startVoiceChatButton = Button("Начать голосовой чат").apply {
            setOnAction {
                if (!isVoiceChatActive) {
                    startVoiceChat()
                    text = "Остановить голосовой чат"
                } else {
                    stopVoiceChat()
                    text = "Начать голосовой чат"
                }
                isVoiceChatActive = !isVoiceChatActive
            }
        }
        // Основное содержимое окна
        formBox.children.addAll(serverInfo, startVoiceChatButton, chatArea, chatInputBox, participantsListView)
        // Устанавливаем сцену
        val scene = Scene(root, 1600.0, 900.0)
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        scene.stylesheets.add(javaClass.getResource("/io/vdartsabvile/avspeak/styles.css")?.toExternalForm())

        primaryStage.scene = scene
        primaryStage.show()

        // Инициализация аудио сокета
        audioSocket = DatagramSocket(port)

    }

    // Метод для добавления участника в список и отображения его в ListView
    fun addParticipant(name: String) {
        if (!participants.contains(name)) {
            participants.add(name)
            Platform.runLater {
                participantsListView.items.add(name)
            }
        }
    }



    private fun startVoiceChat() {
        Thread {
            try {
                val format = getAudioFormat()
                val microphone = AudioSystem.getTargetDataLine(format)
                microphone.open(format)
                microphone.start()

                val buffer = ByteArray(1024)

                while (isVoiceChatActive) {
                    val bytesRead = microphone.read(buffer, 0, buffer.size)
                    val packet = DatagramPacket(buffer, bytesRead, InetAddress.getByName(ip), port)
                    audioSocket.send(packet)
                }

                microphone.stop()
                microphone.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        startAudioReceiver()
    }

    private fun stopVoiceChat() {
        isVoiceChatActive = false
    }

    private fun startAudioReceiver() {
        Thread {
            try {
                val format = getAudioFormat()
                val speakers = AudioSystem.getSourceDataLine(format)
                speakers.open(format)
                speakers.start()

                val buffer = ByteArray(1024)

                while (isVoiceChatActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    audioSocket.receive(packet)
                    speakers.write(packet.data, 0, packet.length)
                }

                speakers.stop()
                speakers.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun getAudioFormat(): AudioFormat {
        val sampleRate = 44100.0f
        val sampleSizeInBits = 16
        val channels = 1
        val signed = true
        val bigEndian = false
        return AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)
    }

    // Создаем правую панель
    private fun createRightPanel(primaryStage: Stage): VBox {
        val rightPanel = VBox(20.0)
        rightPanel.styleClass.add("rightPanel")

        val savedServersLabel = Label("Сохраненные сервера")
        savedServersLabel.styleClass.add("savedd")
        savedServersLabel.style = "-fx-text-fill: white;"

        // Создаем выпадающий список (ComboBox) для сохраненных серверов
        val savedServers = getSavedServers()
        val serverComboBox = ComboBox<String>()
        serverComboBox.promptText = "Выберите сервер"

        // Заполняем ComboBox сохраненными серверами
        if (savedServers.isNotEmpty()) {
            serverComboBox.items.addAll(savedServers)
        }

        val connectButton = Button("Подключиться к серверу")
        connectButton.styleClass.add("Rbutton")
        connectButton.setOnAction {
            ConnectionWindow().openConnectionForm(primaryStage)
        }

        val createServerButton = Button("Создать сервер")
        createServerButton.styleClass.add("Rbutton")
        createServerButton.setOnAction {
            CreationWindow().openCreationForm(primaryStage)
            println("Создание нового сервера")
        }
        val Hrline = Line(0.0, 0.0, 300.0, 0.0) // X1, Y1, X2, Y2
        Hrline.stroke = Color.WHITE // Устанавливаем цвет линии
        Hrline.strokeWidth = 2.0

        // Аватар и имя пользователя
        // Используйте экземпляр переменной smallAvatarImageView
        smallAvatarImageView?.fitWidth = 100.0
        smallAvatarImageView?.fitHeight = 100.0
        smallAvatarImageView?.isPreserveRatio = true
        smallAvatarImageView?.clip = Circle(smallAvatarImageView?.fitWidth!! / 2, smallAvatarImageView?.fitHeight!! / 2, smallAvatarImageView?.fitWidth!! / 2)



        val smallBackgroundCircle = Circle(60.0) // Фон немного больше
        smallBackgroundCircle.fill = Color.GRAY


        val smallAvatarWrapper = StackPane(smallBackgroundCircle, smallAvatarImageView)
        smallAvatarWrapper.alignment = Pos.CENTER
        smallAvatarWrapper.setOnMouseEntered {
            smallAvatarWrapper.style = "-fx-opacity: 0.8;"
        }

        smallAvatarWrapper.setOnMouseExited {
            smallAvatarWrapper.style = "-fx-opacity: 1.0;"
        }
        smallAvatarWrapper.style = "-fx-cursor: hand;"

        smallAvatarWrapper.setOnMouseClicked {
            if (AppState.isAuthenticated.get()) {
                ProfileWindow().openProfileForm(primaryStage) // Открываем профиль, если авторизован
            }
        }

        val smallUserNameLabel = Label(" ${AppState.username ?: "Гость"} ")
        smallUserNameLabel.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold"


        val settingsButton = Button("Настройки")
        settingsButton.styleClass.add("Rbutton")
        settingsButton.setOnAction {
            SettingsWindow().openSettingsForm(primaryStage)
            println("Открытие настроек пользователя")
        }
        val helpButton = Button("Помощь")
        helpButton.styleClass.add("Rbutton")
        helpButton.setOnAction {
            HelpWindow().openHelpForm(primaryStage)
            println("Открытие раздела помощи")
        }



        // Добавляем элементы в правую панель
        rightPanel.children.addAll(savedServersLabel, serverComboBox,Hrline, connectButton, createServerButton, Region(), smallAvatarWrapper, smallUserNameLabel, settingsButton, helpButton)

        return rightPanel
    }
    // Добавил метод для сохранения сервера
    private fun saveServer(ip: String, port: Int) {
        val serverEntry = "$ip:$port\n"
        try {
            val fileWriter = FileWriter("saved_servers.txt", true)
            fileWriter.write(serverEntry)
            fileWriter.close()
            println("Сервер $serverEntry сохранен.")
        } catch (e: IOException) {
            showErrorPopup("Ошибка сохранения сервера: ${e.message}")
        }
    }

    // Получаем список сохраненных серверов
    private fun getSavedServers(): List<String> {
        val servers = mutableListOf<String>()
        try {
            File("saved_servers.txt").forEachLine { line ->
                servers.add(line.trim())
            }
        } catch (e: IOException) {
            showErrorPopup("Ошибка загрузки серверов: ${e.message}")
        }
        return servers
    }

    private fun showErrorPopup(message: String) {
        // Метод для показа всплывающего окна с ошибкой
        val popupStage = Stage()
        popupStage.initStyle(StageStyle.UNDECORATED)
        popupStage.title = "Ошибка"

        val label = Label(message)
        val timerLabel = Label("Исчезнет через 5 секунд...")
        val layout = VBox(10.0, label, timerLabel)
        layout.alignment = Pos.CENTER
        layout.setStyle("-fx-background-color: rgba(255, 0, 0, 0.8); -fx-padding: 10;")

        val scene = Scene(layout)
        scene.fill = Color.TRANSPARENT  // Устанавливаем прозрачный фон сцены
        popupStage.scene = scene
        popupStage.x = 180.0
        popupStage.y = 120.0
        popupStage.show()

        val timeline = Timeline()
        timeline.keyFrames.add(KeyFrame(Duration.seconds(5.0), { popupStage.close() }))
        timeline.play()

        var remainingTime = 5
        val timerTimeline = Timeline()
        timerTimeline.keyFrames.add(KeyFrame(Duration.seconds(1.0), {
            remainingTime--
            timerLabel.text = "Исчезнет через $remainingTime секунд${if (remainingTime == 1) "" else "ы..."}"
            if (remainingTime <= 0) {
                timerTimeline.stop()
            }
        }))
        timerTimeline.cycleCount = Timeline.INDEFINITE
        timerTimeline.play()
    }

}