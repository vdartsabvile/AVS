package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.ConnectionWindow
import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
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

class ProfileWindow {
    private var xOffset = 0.0
    private var yOffset = 0.0
    private var smallAvatarImageView = AppState.smallAvatarImageView
    private var avatarImageView = AppState.avatarImageView


    fun openProfileForm(primaryStage: Stage) {
        val root = BorderPane()
        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()

        // Панель заголовка
        val titleBar = HBox()
        titleBar.style = "-fx-padding: 10;"
        titleBar.alignment = Pos.CENTER_RIGHT

        // Иконка и название
        val icon = Image(javaClass.getResource("/io/vdartsabvile/avspeak/icon.png")?.toExternalForm())
        val iconView = ImageView(icon)
        iconView.fitWidth = 34.0
        iconView.fitHeight = 34.0
        iconView.isPreserveRatio = true
        HBox.setMargin(iconView, javafx.geometry.Insets(0.0, 0.0, 0.0, 8.0))

        val label2 = Label("AVSpeak")
        label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal;"
        label2.styleClass.add("labell")
        label2.setOnMouseEntered {
            label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal; -fx-opacity: 0.8;"
        }

        label2.setOnMouseExited {
            label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-font-weight: normal; -fx-opacity: 1.0;"
        }
        label2.setOnMouseClicked {
            MainApp().start(primaryStage)
        }

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
        titleBar.children.addAll(spacer, minimizeButton, closeButton)

        titleBar.alignment = Pos.CENTER_LEFT
        titleBar.setOnMousePressed { event ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }
        titleBar.setOnMouseDragged { event ->
            primaryStage.x = event.screenX - xOffset
            primaryStage.y = event.screenY - yOffset
        }

        val label = Label("Профиль")
        label.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 80px; -fx-font-weight: normal;"
        label.styleClass.add("labell")

        // Создаем ImageView для аватара с кругом

        avatarImageView?.fitWidth = 180.0
        avatarImageView?.fitHeight = 180.0
        avatarImageView?.isPreserveRatio = true
        avatarImageView?.clip = Circle(avatarImageView?.fitWidth!! / 2, avatarImageView?.fitHeight!! / 2, 75.0)

        avatarImageView?.style = "-fx-background-color: black;" // фон на случай прозрачности картинки

        // Создаем круг для фона (немного больше аватара)
        val backgroundCircle = Circle(90.0) // Радиус чуть больше радиуса аватара
        backgroundCircle.fill = Color.GRAY // Задаем цвет фона

        // Объединяем круг фона и аватар
        val avatarWrapper = StackPane(backgroundCircle, avatarImageView)
        avatarWrapper.alignment = Pos.CENTER

        val uploadAvatarButton = Button("Изменить аватар")
        uploadAvatarButton.setOnAction {
            AvatarCustomizationWindow().openAvatarCustomizationForm(primaryStage)

        }

        val userNameLabel = Label("Ваше имя: ${AppState.username ?: "Гость"} ")
        userNameLabel.style = "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold"

        val vbox = VBox(20.0)
        vbox.children.addAll(label, avatarWrapper, uploadAvatarButton, userNameLabel)
        vbox.alignment = Pos.CENTER

        val rightPanel = createRightPanel(primaryStage)



        root.center = vbox
        root.top = titleBar
        root.right = rightPanel

        val scene = Scene(root, 1600.0, 900.0)
        scene.fill = Color.TRANSPARENT  // Устанавливаем прозрачный фон сцены
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        scene.stylesheets.add(javaClass.getResource("/io/vdartsabvile/avspeak/styles.css")?.toExternalForm())

        primaryStage.title = "AVSpeak"
        primaryStage.scene = scene

        primaryStage.show()
    }

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
