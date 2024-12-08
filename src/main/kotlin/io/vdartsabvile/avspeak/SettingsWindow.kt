package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.ConnectionWindow
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.io.File
import java.io.IOException
import javax.sound.sampled.*
import kotlin.concurrent.thread

class SettingsWindow {

    private var xOffset = 0.0
    private var yOffset = 0.0
    private var isMicrophoneTesting = false
    private var testThread: Thread? = null
    private var smallAvatarImageView = AppState.smallAvatarImageView
    val testMicrophoneButton = Button("Проверить микрофон")


    // Получение списка доступных устройств (микрофоны и наушники)
    private fun getAudioDevices(): List<Mixer.Info> {
        return AudioSystem.getMixerInfo().toList()
    }

    // Функция для открытия формы настроек
    fun openSettingsForm(primaryStage: Stage) {
        // Установка иконки
        val ico = Image(javaClass.getResourceAsStream("/io/vdartsabvile/avspeak/ico.png"))
        primaryStage.icons.add(ico)

        val titleBar = HBox()
        titleBar.style = "-fx-padding: 10;"
        titleBar.alignment = Pos.CENTER_RIGHT
        val icon = Image(javaClass.getResource("/io/vdartsabvile/avspeak/icon.png")?.toExternalForm())
        val iconView = ImageView(icon)
        iconView.fitWidth = 34.0
        iconView.fitHeight = 34.0
        iconView.isPreserveRatio = true
        HBox.setMargin(iconView, javafx.geometry.Insets(0.0, 0.0, 0.0, 8.0))

        // Перетаскивание окна
        titleBar.setOnMousePressed { event: javafx.scene.input.MouseEvent ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }
        titleBar.setOnMouseDragged { event: javafx.scene.input.MouseEvent ->
            primaryStage.x = event.screenX - xOffset
            primaryStage.y = event.screenY - yOffset
        }

        val label2 = Label("AVSpeak")
        label2.styleClass.add("labell")
        label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal;"
        label2.setOnMouseEntered {
            label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal; -fx-opacity: 0.8;"
        }

        label2.setOnMouseExited {
            label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal; -fx-opacity: 1.0;"
        }
        label2.setOnMouseClicked {
            MainApp().start(primaryStage)
            isMicrophoneTesting = false
        }

        // Кнопка закрытия
        val closeButton = Button("✖")
        closeButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        closeButton.setOnAction {
            StartUp().stopNodeServer()
            DisconnectServer().notifyServerLogout()
            StartUp().stopNodeClient()
            primaryStage.close()
            StartUp().killAllNodeProcesses() }



        val minimizeButton = Button("−")
        minimizeButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        minimizeButton.setOnAction { primaryStage.isIconified = true }

        val helpButton = Button("?")
        helpButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        helpButton.setOnAction { HelpWindow().openHelpForm(primaryStage) }

        titleBar.children.addAll(iconView, label2)

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        titleBar.children.addAll(spacer, helpButton, Label("  "),minimizeButton, closeButton)
        titleBar.alignment = Pos.CENTER_LEFT

        val rightPanel = createRightPanel(primaryStage)

        val headerLabel = Label("Настройки")
        headerLabel.styleClass.add("savedd")
        headerLabel.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 70px;"
        // VBox для формы
        val formBox = VBox(20.0)
        formBox.style = "-fx-padding: 40; -fx-alignment: center;"
        formBox.children.add(headerLabel)
        formBox.alignment = Pos.CENTER


        // Получаем список устройств аудио
        val audioDevicesMic = getFilteredMic()
        val audioDevicesDyn = getFilteredDynamics()

        AppState.loadSettings(audioDevicesMic, audioDevicesDyn)

        // ComboBox для выбора микрофона
        val microphoneLabel = Label("Выберите микрофон:")
        val microphoneComboBox = ComboBox<Mixer.Info>()
        microphoneComboBox.items.addAll(audioDevicesMic)

        // Установка сохраненного устройства
        val savedMicrophone = AppState.selectedMicrophone.get()
        if (savedMicrophone != null) {
            microphoneComboBox.selectionModel.select(savedMicrophone)
        } else {
            microphoneComboBox.selectionModel.selectFirst()
        }

        // ComboBox для выбора наушников (или колонок)
        val speakerLabel = Label("Выберите устройство воспроизведения:")
        val speakerComboBox = ComboBox<Mixer.Info>()
        speakerComboBox.items.addAll(audioDevicesDyn)

        // Установка сохраненного устройства
        val savedSpeaker = AppState.selectedSpeaker.get()
        if (savedSpeaker != null) {
            speakerComboBox.selectionModel.select(savedSpeaker)
        } else {
            speakerComboBox.selectionModel.selectFirst()
        }


        // Шкала для отображения уровня звука
        val micLevelProgressBar = ProgressBar(0.0)
        micLevelProgressBar.prefWidth = 220.0

        // Кнопка для проверки микрофона
        testMicrophoneButton.setOnAction {
            val selectedMicrophone = microphoneComboBox.value
            val selectedSpeaker = speakerComboBox.value
            if (!isMicrophoneTesting) {
                startMicrophoneTest(selectedMicrophone, selectedSpeaker, testMicrophoneButton, micLevelProgressBar)
            } else {
                stopMicrophoneTest(testMicrophoneButton)
            }
        }

        // Кнопка для сохранения настроек
        val saveButton = Button("Сохранить настройки")
        saveButton.setOnAction {
            val selectedMicrophone = microphoneComboBox.value
            val selectedSpeaker = speakerComboBox.value

            AppState.selectedMicrophone.set(selectedMicrophone)
            AppState.selectedSpeaker.set(selectedSpeaker)

            AppState.saveSettings()  // Сохраняем настройки в AppState
            println("Настройки сохранены!")
            println("Выбранный микрофон: $selectedMicrophone")
            println("Выбранные наушники/колонки: $selectedSpeaker")

        }


        formBox.children.addAll(microphoneLabel, microphoneComboBox, speakerLabel, speakerComboBox, testMicrophoneButton, micLevelProgressBar, saveButton)

        val root = BorderPane()
        root.top = titleBar
        root.right = rightPanel
        root.center = formBox
        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()


        val scene = Scene(root, 1600.0, 900.0)
        scene.fill = Color.TRANSPARENT  // Устанавливаем прозрачный фон сцены
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        scene.stylesheets.add(javaClass.getResource("/io/vdartsabvile/avspeak/styles.css")?.toExternalForm())

        primaryStage.scene = scene
        primaryStage.show()
    }

    // Получаем отфильтрованные аудиоустройства
    private fun getFilteredAudioDevices(): List<Mixer.Info> {
        val allMixers = AudioSystem.getMixerInfo().toList()

        // Фильтруем только микрофоны и динамики
        return allMixers.filter { mixerInfo ->
            val mixer = AudioSystem.getMixer(mixerInfo)

            // Ищем устройства, которые поддерживают либо TargetDataLine (микрофон), либо SourceDataLine (динамики)
            val supportsMic = mixer.targetLineInfo.any { it.lineClass == TargetDataLine::class.java }
            val supportsSpeaker = mixer.sourceLineInfo.any { it.lineClass == SourceDataLine::class.java }

            // Исключаем системные и виртуальные устройства по именам
            val isSystemDevice = mixerInfo.name.contains("Port", ignoreCase = true) ||
                    mixerInfo.name.contains("Mix", ignoreCase = true) ||
                    mixerInfo.name.contains("Monitor", ignoreCase = true) ||
                    mixerInfo.name.contains("Loopback", ignoreCase = true)

            // Возвращаем только реальные микрофоны и динамики
            !isSystemDevice && (supportsMic || supportsSpeaker)
        }
    }
    private fun getFilteredDynamics(): List<Mixer.Info> {
        val allMixers = AudioSystem.getMixerInfo().toList()

        // Фильтруем только микрофоны и динамики
        return allMixers.filter { mixerInfo ->
            val mixer = AudioSystem.getMixer(mixerInfo)

            // Ищем устройства, которые поддерживают SourceDataLine (динамики)
            val supportsSpeaker = mixer.sourceLineInfo.any { it.lineClass == SourceDataLine::class.java }

            // Исключаем системные и виртуальные устройства по именам
            val isSystemDevice = mixerInfo.name.contains("Port", ignoreCase = true) ||
                    mixerInfo.name.contains("Mix", ignoreCase = true) ||
                    mixerInfo.name.contains("Monitor", ignoreCase = true) ||
                    mixerInfo.name.contains("Loopback", ignoreCase = true)

            // Возвращаем только реальные микрофоны и динамики
            !isSystemDevice && supportsSpeaker
        }
    }
    // Получаем отфильтрованные аудиоустройства (только микрофоны)
    private fun getFilteredMic(): List<Mixer.Info> {
        val allMixers = AudioSystem.getMixerInfo().toList()

        // Фильтруем только микрофоны
        return allMixers.filter { mixerInfo ->
            val mixer = AudioSystem.getMixer(mixerInfo)

            // Проверяем только на поддержку TargetDataLine (характерно для микрофонов)
            val supportsMic = mixer.targetLineInfo.any { it.lineClass == TargetDataLine::class.java }

            // Исключаем системные и виртуальные устройства по именам
            val isSystemDevice = mixerInfo.name.contains("Port", ignoreCase = true) ||
                    mixerInfo.name.contains("Mix", ignoreCase = true) ||
                    mixerInfo.name.contains("Monitor", ignoreCase = true) ||
                    mixerInfo.name.contains("Loopback", ignoreCase = true)

            // Возвращаем только реальные микрофоны
            supportsMic && !isSystemDevice
        }
    }




    // Функция для начала теста микрофона (воспроизведение и вывод уровня)
    private fun startMicrophoneTest(
        selectedMicrophone: Mixer.Info,
        selectedSpeaker: Mixer.Info,
        button: Button,
        progressBar: ProgressBar
    ) {
        isMicrophoneTesting = true
        button.text = "Остановить проверку"

        testThread = thread(start = true) {
            try {
                val microphoneMixer = AudioSystem.getMixer(selectedMicrophone)
                val speakerMixer = AudioSystem.getMixer(selectedSpeaker)

                val format = AudioFormat(44100.0f, 16, 1, true, true)
                val micInfo = DataLine.Info(TargetDataLine::class.java, format)
                val speakerInfo = DataLine.Info(SourceDataLine::class.java, format)

                val microphoneLine = microphoneMixer.getLine(micInfo) as TargetDataLine
                val speakerLine = speakerMixer.getLine(speakerInfo) as SourceDataLine

                microphoneLine.open(format)
                speakerLine.open(format)

                microphoneLine.start()
                speakerLine.start()

                val buffer = ByteArray(1024)
                while (isMicrophoneTesting) {
                    val bytesRead = microphoneLine.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        // Воспроизведение звука через динамики
                        speakerLine.write(buffer, 0, bytesRead)

                        // Обновляем уровень микрофона на шкале
                        val level = calculateRMSLevel(buffer, bytesRead)
                        Platform.runLater {
                            progressBar.progress = level / 100 // нормируем уровень к 0-1 для ProgressBar
                        }
                    }
                }

                microphoneLine.stop()
                microphoneLine.close()
                speakerLine.stop()
                speakerLine.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // Функция для остановки теста микрофона
    private fun stopMicrophoneTest(button: Button) {
        isMicrophoneTesting = false
        button.text = "Проверить микрофон"
        testThread?.interrupt()
    }

    // Функция для расчета уровня сигнала RMS
    private fun calculateRMSLevel(buffer: ByteArray, bytesRead: Int): Double {
        var sum = 0.0
        for (i in 0 until bytesRead) {
            sum += buffer[i] * buffer[i]
        }
        return Math.sqrt(sum / bytesRead)
    }

    // Создаем правую панель
    private fun createRightPanel(primaryStage: Stage): VBox {
        val rightPanel = VBox(20.0)

        rightPanel.styleClass.add("rightPanel")

        val savedServersLabel = Label("Сохраненные сервера")
        savedServersLabel.styleClass.add("savedd")
        savedServersLabel.style = "-fx-text-fill: white;"

        val savedServers = getSavedServers()
        val serverComboBox = ComboBox<String>()
        serverComboBox.promptText = "Выберите сервер"

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

        // Используйте экземпляр переменной smallAvatarImageView
        smallAvatarImageView?.fitWidth = 100.0
        smallAvatarImageView?.fitHeight = 100.0
        smallAvatarImageView?.isPreserveRatio = true
        smallAvatarImageView?.clip = Circle(smallAvatarImageView?.fitWidth!! / 2, smallAvatarImageView?.fitHeight!! / 2, smallAvatarImageView?.fitWidth!! / 2)



        val smallBackgroundCircle = Circle(60.0) // Фон немного больше
        smallBackgroundCircle.fill = Color.GRAY

        val smallAvatarWrapper = StackPane(smallBackgroundCircle, smallAvatarImageView)
        smallAvatarWrapper.alignment = Pos.CENTER
        smallAvatarWrapper.style = "-fx-cursor: hand;"

        smallAvatarWrapper.setOnMouseClicked {
            ProfileWindow().openProfileForm(primaryStage) // Открываем профиль, если авторизован
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
        val friendsButton = Button("Друзья")
        friendsButton.styleClass.add("Rbutton")
        friendsButton.setOnAction {
            FriendsWindow(AppState.username.toString()).openFriendsForm(primaryStage)
            println("Открытие окна друзей")
        }

        val path = Path()
        path.elements.add(MoveTo(0.0, 0.0))
        path.elements.add(LineTo(300.0, 0.0))
        path.styleClass.add("line") // Применение стиля для линии
        val path2 = Path()
        path2.elements.add(MoveTo(0.0, 0.0))
        path2.elements.add(LineTo(300.0, 0.0))
        path2.styleClass.add("line") // Применение стиля для линии
        val path3 = Path()
        path3.elements.add(MoveTo(0.0, 0.0))
        path3.elements.add(LineTo(300.0, 0.0))
        path3.styleClass.add("line") // Применение стиля для линии

        rightPanel.children.addAll(savedServersLabel, serverComboBox, path, connectButton, createServerButton, path2, Region())
        rightPanel.children.addAll(smallAvatarWrapper, smallUserNameLabel, path3, friendsButton, settingsButton)

        return rightPanel
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
