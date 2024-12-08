package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.avspeak.Client.AVSpeakClient
import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.image.Image
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
import java.util.prefs.Preferences

class MainApp : Application() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java)
        }
    }

    private val preferences = Preferences.userNodeForPackage(LoginWindow::class.java)

    private var xOffset = 0.0
    private var yOffset = 0.0
    private lateinit var connectButton: Button
    private lateinit var createServerButton: Button
    private lateinit var settingsButton: Button
    private lateinit var smallAvatarWrapper: StackPane
    private lateinit var serverComboBox: ComboBox<String>
    private var smallAvatarImageView = AppState.smallAvatarImageView
    private lateinit var helpButton: Button
    private var friendsButton = Button("Друзья")


    val registerButton = Button("")
    val label = Label("AVSpeak")
    private var headerLabel = Label("Добро пожаловать, пожалуйста выполните:")
    val loginButton = Button("Вход")
    val smallUserNameLabel = Label(" ${AppState.username ?: "Гость"} ")

    override fun start(primaryStage: Stage) {
        //primaryStage.initStyle(StageStyle.UNDECORATED)

        // Создаем основной контейнер
        val root = BorderPane()
        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()
        // Создаем панель заголовка
        val titleBar = HBox()
        titleBar.style = "-fx-padding: 10;"
        titleBar.alignment = Pos.CENTER_RIGHT
        val icon = Image(javaClass.getResource("/io/vdartsabvile/avspeak/letter_a.png")?.toExternalForm())
        val iconView = javafx.scene.image.ImageView(icon)
        iconView.fitHeight = 38.0 // Установите высоту иконки
        iconView.isPreserveRatio = true // Сохраняем пропорции
        HBox.setMargin(iconView, javafx.geometry.Insets(0.0, 4.0, 0.0, 4.0)) // 5 пикселей отступа слева



        val label2 = Label("AVSpeak")
        label2.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 38px; -fx-text-fill: #FFFFFF; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 0);" // Устанавливаем размер шрифта и жирный стиль
        label2.styleClass.add("AVSpeak")
        // Кнопка закрытия
        val closeButton = Button("✖")
        closeButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        closeButton.setOnAction {
            StartUp().stopNodeServer()
            DisconnectServer().notifyServerLogout()
            StartUp().stopNodeClient()
            primaryStage.close()
            StartUp().killAllNodeProcesses()
        }
        // Кнопка сворачивания
        val minimizeButton = Button("−")
        minimizeButton.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        minimizeButton.setOnAction { primaryStage.isIconified = true}

        // Добавление кнопок на панель заголовка
        // Устанавливаем выравнивание кнопок вправо
        HBox.setHgrow(closeButton, Priority.ALWAYS)
        HBox.setHgrow(minimizeButton, Priority.ALWAYS)
        titleBar.children.addAll(iconView, label2)

        val helpButtonTop = Button("?")
        helpButtonTop.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        helpButtonTop.setOnAction { HelpWindow().openHelpForm(primaryStage) }

        // Заполнение пространства слева
        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        titleBar.children.addAll(spacer, helpButtonTop, Label("  "),minimizeButton, closeButton)

        // Выравнивание
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


        // Создаем метку с текстом
        label.style = "-fx-font-family: 'Secession [by me]' ;-fx-font-size: 200px; -fx-font-weight: normal;" // Устанавливаем размер шрифта и жирный стиль
        label.styleClass.add("labell")

        val logo = Image(javaClass.getResource("/io/vdartsabvile/avspeak/icon.png")?.toExternalForm())
        val letters = Image(javaClass.getResource("/io/vdartsabvile/avspeak/letters_sc.png")?.toExternalForm())
        val logoView = javafx.scene.image.ImageView(letters)
        //logoView.fitWidth = 302.0 // Установите ширину иконки
        logoView.fitHeight = 170.0 // Установите высоту иконки
        logoView.isPreserveRatio = true // Сохраняем пропорции
        logoView.style = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,1), 20, 0, 0, 0);"
        HBox.setMargin(logoView, javafx.geometry.Insets(0.0, 5.0, 0.0, 5.0))


        // Создаем HBox для логотипа и текста
        val logoTextBox = HBox(10.0) // Отступ между элементами 10 пикселей
        logoTextBox.children.addAll(logoView)
        logoTextBox.alignment = Pos.CENTER // Выравнивание по центру


        // Создаем VBox для вертикального размещения надписи и кнопок
        val vbox = VBox(20.0) // Отступ между элементами 20 пикселей
        vbox.children.addAll(logoTextBox)
        vbox.alignment = Pos.CENTER // Выравнивание по центру


        // Создаем кнопки для входа и регистрации
        loginButton.styleClass.add("buttonn")
        loginButton.setOnAction {
            // Открытие окна для входа
            LoginWindow().openLoginForm(primaryStage) { success ->
                if (success) {
                    loginButton.style = "-fx-opacity: 100;"
                    AppState.isAuthenticated.set(true)
                    enableAppFeatures()
                    }
            }
        }

        val savedUsername = preferences.get("username", null)
        val savedPassword = preferences.get("password", null)
        updateRegisterButtonText(registerButton) // Устанавливаем текст кнопки в зависимости от состояния
        registerButton.styleClass.add("buttonn")
        registerButton.setOnAction {
            if (AppState.isAuthenticated.get()) {  // Используем .get() для чтения значения
                // Логика выхода
                AppState.isAuthenticated.set(false)  // Используем .set(false) для установки значения
                registerButton.text = "Регистрация"
                headerLabel.text = "Добро пожаловать, пожалуйста выполните:"
                loginButton.isVisible = true
                preferences.remove("username")
                preferences.remove("password")
                DisconnectServer().notifyServerLogout()
                disableAppFeatures()
            } else {
                RegistrationWindow().openRegistrationForm(primaryStage)
            }
        }


        if (AppState.isAuthenticated.get()) {
            headerLabel = Label("${AppState.username ?: "Гость"}, будешпили? Если нет:")
            loginButton.isVisible = false
        }
        headerLabel.styleClass.add("savedd")


        vbox.children.addAll(headerLabel, loginButton, registerButton)

        vbox.alignment = Pos.CENTER


        // Добавляем правую панель с аватаром, настройками и серверами
        val rightPanel = createRightPanel(primaryStage)


        // Добавляем VBox в основной контейнер
        root.center = vbox // Помещаем VBox по центру
        root.top = titleBar
        root.right = rightPanel // Добавляем правую панель

        // Создаем сцену с заданными размерами
        val scene = Scene(root, 1600.0, 900.0)

        scene.fill = Color.TRANSPARENT  // Устанавливаем прозрачный фон сцены

        // Устанавливаем тему с Dracula
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        scene.stylesheets.add(javaClass.getResource("/io/vdartsabvile/avspeak/styles.css")?.toExternalForm())



        // Устанавливаем сцену на Stage и показываем
        // Установка иконки
        val ico = Image(javaClass.getResourceAsStream("/io/vdartsabvile/avspeak/ico.png"))
        primaryStage.icons.add(ico)

        primaryStage.title = "AVSpeak"
        primaryStage.scene = scene
        primaryStage.show()
    }
    private fun updateRegisterButtonText(button: Button) {
        button.text = if (AppState.isAuthenticated.get()) {"Выход"} else {"Регистрация"}
    }


    // Метод, который накладывает ограничения на элементы интерфейса
    private fun applyAuthenticationRestrictions(vararg buttons: Button) {
        for (button in buttons) {
            button.isDisable = !AppState.isAuthenticated.get()
            if (!AppState.isAuthenticated.get()){
            button.style = "-fx-opacity: 0.5;"}
            else button.style = "-fx-opacity: 1.0;"
        }
    }

    private fun updateAvatarAppearance(smallAvatarWrapper: StackPane) {
        if (!AppState.isAuthenticated.get()) {
            smallAvatarWrapper.opacity = 0.5 // Делаем полупрозрачным
            smallAvatarWrapper.isMouseTransparent = true // Делает `StackPane` нечувствительным к действиям мыши
        } else {
            smallAvatarWrapper.opacity = 1.0 // Полная непрозрачность
            smallAvatarWrapper.isMouseTransparent = false // Восстанавливаем чувственность к действиям мыши
        }
    }
    private fun applyAuthenticationRestrictionsToComboBox(comboBox: ComboBox<String>) {
        if (!AppState.isAuthenticated.get()) {
            comboBox.isDisable = true // Отключаем ComboBox
            comboBox.opacity = 0.5 // Делаем его полупрозрачным
        } else {
            comboBox.isDisable = false // Включаем ComboBox, если пользователь авторизован
            comboBox.opacity = 1.0 // Восстанавливаем полную непрозрачность
        }
    }

    // Создаем правую панель
    private fun createRightPanel(primaryStage: Stage): VBox {
        val rightPanel = VBox(20.0)
        rightPanel.styleClass.add("rightPanel")

        //Инициализация кнопок из rightPanel
        connectButton = Button("Подключиться к серверу")
        createServerButton = Button("Создать сервер")
        settingsButton = Button("Настройки")
        helpButton = Button("Помощь")

        smallAvatarImageView?.fitWidth = 100.0
        smallAvatarImageView?.fitHeight = 100.0
        smallAvatarImageView?.isPreserveRatio = true
        smallAvatarImageView?.let {
            it.fitWidth = 100.0
            it.fitHeight = 100.0
            it.isPreserveRatio = true
            it.clip = Circle(it.fitWidth / 2, it.fitHeight / 2, it.fitWidth / 2)
        }


        val smallBackgroundCircle = Circle(60.0) // Фон немного больше
        smallBackgroundCircle.fill = Color.GRAY

        smallAvatarWrapper = StackPane(smallBackgroundCircle, smallAvatarImageView)
        smallAvatarWrapper.setOnMouseEntered {
            smallAvatarWrapper.style = "-fx-opacity: 0.8;"
        }

        smallAvatarWrapper.setOnMouseExited {
            smallAvatarWrapper.style = "-fx-opacity: 1.0;"
        }

        val savedServersLabel = Label("Сохраненные сервера")
        savedServersLabel.styleClass.add("savedd")
        savedServersLabel.style = "-fx-text-fill: white;"

        // Создаем выпадающий список (ComboBox) для сохраненных серверов
        val savedServers = getSavedServers()
        serverComboBox = ComboBox<String>()
        serverComboBox.promptText = "Выберите сервер"

        // Заполняем ComboBox сохраненными серверами
        if (savedServers.isNotEmpty()) {
            serverComboBox.items.addAll(savedServers)
        }


        connectButton.styleClass.add("Rbutton")
        connectButton.setOnAction {
            if (AppState.isAuthenticated.get()) {
                //ConnectionWindow().openConnectionForm(primaryStage)
                AVSpeakClient().start(primaryStage)
            }
        }



        createServerButton.styleClass.add("Rbutton")
        createServerButton.setOnAction {
            if (AppState.isAuthenticated.get()) {
                CreationWindow().openCreationForm(primaryStage)
                println("Создание нового сервера")
            }
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


        // Добавляем аватар и имя пользователя в уменьшенном виде на правую панель

        smallAvatarWrapper.alignment = Pos.CENTER

        smallAvatarWrapper.style = "-fx-cursor: hand;"

        smallAvatarWrapper.setOnMouseClicked {
            if (AppState.isAuthenticated.get()) {
                ProfileWindow().openProfileForm(primaryStage) // Открываем профиль, если авторизован
            }
        }

        // Применение визуальных ограничений
        updateAvatarAppearance(smallAvatarWrapper)

        smallUserNameLabel.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold"




        settingsButton.styleClass.add("Rbutton")
        settingsButton.setOnAction {
            if (AppState.isAuthenticated.get()) {
                SettingsWindow().openSettingsForm(primaryStage)
                println("Открытие настроек пользователя")
            }
        }

        helpButton.styleClass.add("Rbutton")
        helpButton.setOnAction {
                HelpWindow().openHelpForm(primaryStage)
                println("Открытие раздела помощи")
        }

        friendsButton.styleClass.add("Rbutton")
        friendsButton.setOnAction {
            FriendsWindow(AppState.username.toString()).openFriendsForm(primaryStage)
            println("Открытие окна друзей")
        }



        // Добавляем элементы на правую панель
        rightPanel.children.addAll(savedServersLabel, serverComboBox, path, connectButton, createServerButton, path2,Region())
        rightPanel.children.addAll(smallAvatarWrapper, smallUserNameLabel,path3, friendsButton, settingsButton)
        applyAuthenticationRestrictions(connectButton, createServerButton, settingsButton, friendsButton)
        updateAvatarAppearance(smallAvatarWrapper)
        applyAuthenticationRestrictionsToComboBox(serverComboBox)

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
    private fun showPopup(message: String) {
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
        layout.setStyle("-fx-background-color: rgba(127, 199, 255, 0.8); -fx-padding: 10;")

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

    private fun enableAppFeatures() {

        // Включаем кнопки, которые были отключены
        applyAuthenticationRestrictions(connectButton, createServerButton, settingsButton, friendsButton)

        // Включаем видимость аватара и делаем его доступным для взаимодействия
        updateAvatarAppearance(smallAvatarWrapper)

        // Включаем ComboBox для выбора сервера
        applyAuthenticationRestrictionsToComboBox(serverComboBox)

        // Обновляем состояние имени
        smallUserNameLabel.text = AppState.username ?: "Гость" // Принудительно обновляем метку

        headerLabel.text = "${AppState.username ?: "Гость"}, будешпили? Если нет:"

        registerButton.text = "Выход"
        loginButton.isVisible = false



        // Дополнительные действия при успехе авторизации
        showPopup("Функции приложения активированы. Добро пожаловать!")
    }
    private fun disableAppFeatures() {
        connectButton.style = "-fx-opacity: 0.5"
        connectButton.isDisable = true

        friendsButton.style = "-fx-opacity: 0.5"
        friendsButton.isDisable = true

        settingsButton.style = "-fx-opacity: 0.5"
        settingsButton.isDisable = true

        smallAvatarWrapper.style = "-fx-opacity: 0.5"
        smallAvatarWrapper.isDisable = true

        createServerButton.style = "-fx-opacity: 0.5"
        createServerButton.isDisable = true
        serverComboBox.isDisable = true // Отключаем ComboBox
        serverComboBox.opacity = 0.5 // Делаем его полупрозрачным

        smallAvatarWrapper.opacity = 0.5 // Делаем полупрозрачным
        smallAvatarWrapper.isMouseTransparent = true // Делает `StackPane` нечувствительным к действиям мыши

        smallUserNameLabel.text = "Гость"
        smallAvatarImageView?.image = Image(javaClass.getResource("/io/vdartsabvile/avspeak/null.png")?.toExternalForm())

    }

}