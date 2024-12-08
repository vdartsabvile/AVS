package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.ConnectionWindow
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
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
import java.net.InetAddress

class CreationWindow {

    private var xOffset = 0.0
    private var yOffset = 0.0
    private var smallAvatarImageView = AppState.smallAvatarImageView



    fun openCreationForm(primaryStage: Stage){
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
        label2.setOnMouseEntered {
            label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal; -fx-opacity: 0.8;"
        }

        label2.setOnMouseExited {
            label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal; -fx-opacity: 1.0;"
        }
        label2.setOnMouseClicked {
            MainApp().start(primaryStage)
        }

        // Кнопка закрытия
        val closeButton = Button("✖")
        closeButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        closeButton.setOnAction {
            StartUp().stopNodeServer()
            DisconnectServer().notifyServerLogout()
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

        // Основной макет
        val root = BorderPane()
        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()
        root.top = titleBar
        root.right = rightPanel // Добавляем правую панель


        // В коде, где вы инициализируете `root`, добавьте это:
        val centerPanel = VBox(20.0) // Инициализация centerPanel
        centerPanel.style = "-fx-padding: 40; -fx-alignment: center;"

        val headerLabel = Label("Создание сервера")
        headerLabel.styleClass.add("savedd")
        headerLabel.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 70px;"

        val serverNameLabel = Label("Введите имя сервера:")
        serverNameLabel.style = "-fx-text-fill: white;"

        val serverNameField = TextField()
        serverNameField.promptText = "Имя сервера"

        val portLabel = Label("Введите порт сервера:")
        portLabel.style = "-fx-text-fill: white;"

        val portField = TextField()
        portField.promptText = "Порт"

        val createServerButton = Button("Создать сервер")
        createServerButton.styleClass.add("buttonn")
        createServerButton.setOnAction {
            val serverName = serverNameField.text
            val ip = InetAddress.getLocalHost().hostAddress // Получаем IP адрес
            val port = portField.text.toIntOrNull()
            if (serverName.isNotBlank() && port != null) {
                saveServer(serverName, ip, port) // Сохраняем сервер
                AdminServerWindow(serverName, port, ip).start(primaryStage)
            } else {
                showErrorPopup("Пожалуйста, введите корректное имя и порт.")
            }
        }

        centerPanel.children.addAll(headerLabel, serverNameLabel, serverNameField,
            portLabel, portField, createServerButton)

        // Присвоение centerPanel центральной части `BorderPane`
        root.center = centerPanel

        // Устанавливаем сцену
        val scene = Scene(root, 1600.0, 900.0)
        scene.fill = Color.TRANSPARENT  // Устанавливаем прозрачный фон сцены
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        scene.stylesheets.add(javaClass.getResource("/io/vdartsabvile/avspeak/styles.css")?.toExternalForm())

        primaryStage.scene = scene
        primaryStage.show()

    }

    // Создаем правую панель
    private fun createRightPanel(primaryStage: Stage): VBox {
        val rightPanel = VBox(20.0)
        rightPanel.styleClass.add("rightPanel")

        val savedServersLabel = Label("Сохраненные сервера")
        savedServersLabel.styleClass.add("savedd")
        savedServersLabel.style = "-fx-text-fill: white;"

        // Получаем сохраненные сервера
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
    private fun saveServer(serverName: String, ip: String, port: Int) {
        val serverEntry = "$serverName($ip:$port)\n"
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