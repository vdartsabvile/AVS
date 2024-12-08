package io.vdartsabvile

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.avspeak.*
import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
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
import java.io.*
import java.net.*

class ConnectionWindow {
    private var xOffset = 0.0
    private var yOffset = 0.0
    private var smallAvatarImageView = AppState.smallAvatarImageView


    fun openConnectionForm(primaryStage: Stage) {
        // Создаем заголовок окна
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
        label2.styleClass.add("labell")
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

        val headerLabel = Label("Подключение к серверу")
        headerLabel.styleClass.add("savedd")
        headerLabel.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 70px;"
        // Создание формы для подключения
        val NameLabel = Label("Введите имя сервера:")
        val NameField = TextField()
        NameField.promptText = "Имя сервера"

        val ipLabel = Label("Введите IP сервера:")
        val ipField = TextField()
        ipField.promptText = "IP-адрес"

        val portLabel = Label("Введите порт сервера:")
        val portField = TextField()
        portField.promptText = "Порт"

        val connectButton = Button("Подключиться")
        connectButton.styleClass.add("buttonn")
        connectButton.setOnAction {
            val serverName = NameField.text
            val ip = ipField.text
            val port = portField.text.toIntOrNull()

            if (serverName.isBlank() || ip.isBlank() || port == null) {
                showErrorPopup("Пожалуйста, заполните все поля корректно.")
                return@setOnAction
            }

            println("Попытка подключения к серверу $serverName($ip:$port)")

            // Создаем и запускаем задачу подключения в отдельном потоке
            Thread {
                try {
                    val socket = Socket()
                    val socketAddress = InetSocketAddress(ip, port)
                    val timeoutMs = 5000 // 5 секунд таймаут

                    Platform.runLater { showErrorPopup("Попытка подключения к серверу") }

                    socket.connect(socketAddress, timeoutMs)

                    println("Соединение установлено успешно.")

                    val output = PrintWriter(socket.getOutputStream(), true)
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                    // Сохраняем сервер в список сохраненных серверов
                    saveServer(serverName, ip, port)

                    // Запускаем ServerWindow или выполняем другие действия
                    Platform.runLater {
                        ServerWindow(serverName, port, ip, socket, input, output).start(Stage())
                        primaryStage.close()
                    }
                } catch (e: UnknownHostException) {
                    Platform.runLater {
                        showErrorPopup("Неизвестный хост: ${e.message}. Проверьте правильность IP-адреса.")
                    }
                } catch (e: SocketTimeoutException) {
                    Platform.runLater {
                        showErrorPopup("Превышено время ожидания подключения. Сервер не отвечает.")
                    }
                } catch (e: ConnectException) {
                    Platform.runLater {
                        showErrorPopup("Не удалось установить соединение. Сервер не запущен или недоступен.")
                    }
                } catch (e: IOException) {
                    Platform.runLater {
                        showErrorPopup("Ошибка ввода/вывода при подключении: ${e.message}")
                    }
                } catch (e: Exception) {
                    Platform.runLater {
                        showErrorPopup("Непредвиденная ошибка: ${e.message}")
                    }
                    e.printStackTrace()
                }
            }.start()
        }





        // Создаем VBox для формы
        val formBox = VBox(20.0)
        formBox.style = "-fx-padding: 40; -fx-alignment: center;"
        formBox.children.addAll(headerLabel, NameLabel, NameField,ipLabel, ipField, portLabel, portField, connectButton)
        formBox.alignment = Pos.CENTER

        // Добавляем правую панель с аватаром, настройками и серверами
        val rightPanel = createRightPanel(primaryStage)

        // Основной макет
        val root = BorderPane()
        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()
        root.center = formBox
        root.top = titleBar
        root.right = rightPanel // Добавляем правую панель

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
            openConnectionForm(primaryStage)
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
        val serverEntry = "$serverName($ip:$port)"
        try {
            val fileWriter = FileWriter("saved_servers.txt", true)
            fileWriter.use { it.write("$serverEntry\n") }
            println("Сервер $serverEntry сохранен.")
        } catch (e: IOException) {
            println("Ошибка сохранения сервера: ${e.message}")
            showErrorPopup("Не удалось сохранить сервер.")
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

        // Делаем окно всегда поверх других
        popupStage.isAlwaysOnTop = true

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

        // Анимация появления окна (плавное появление)
        val fadeIn = FadeTransition(Duration.millis(500.0), layout)
        fadeIn.fromValue = 0.0
        fadeIn.toValue = 1.0
        fadeIn.play() // Запускаем анимацию

        // Показываем окно
        popupStage.show()

        // Таймер для автоматического закрытия через 5 секунд с анимацией исчезновения
        val timeline = Timeline()
        timeline.keyFrames.add(KeyFrame(Duration.seconds(5.0), {
            // Анимация исчезновения (плавное исчезновение)
            val fadeOut = FadeTransition(Duration.millis(500.0), layout)
            fadeOut.fromValue = 1.0
            fadeOut.toValue = 0.0
            fadeOut.play() // Запускаем анимацию

            fadeOut.setOnFinished {
                popupStage.close() // Закрываем окно после анимации
            }
        }))
        timeline.play()
    }
}
