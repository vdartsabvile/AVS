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
import javafx.scene.shape.*
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import java.io.File
import java.io.IOException

class HelpWindow {

    private var xOffset = 0.0
    private var yOffset = 0.0
    private var smallAvatarImageView = AppState.smallAvatarImageView

    // Получение списка доступных устройств (микрофоны и наушники)

    // Функция для открытия формы настроек
    fun openHelpForm(primaryStage: Stage) {
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

        val helpButtonTop = Button("?")
        helpButtonTop.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50px; -fx-min-height: 40px;"
        helpButtonTop.setOnAction { HelpWindow().openHelpForm(primaryStage) }

        titleBar.children.addAll(iconView, label2)

        val helpContent = """
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Помощь | AVSpeak</title>
    <style>
        /* Основные стили страницы */
        body {
            font-family: Arial, sans-serif;
            background-color: #1C1C1E;
            margin: 0;
            padding: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100vh;
            color: #E0E0E0;
        }

        /* Основной контейнер */
        .container {
            background-color: #2E2E31;
            border-radius: 12px;
            padding: 30px;
            width: 60%;
            max-width: 800px;
            box-shadow: 0 12px 20px rgba(0, 0, 0, 0.2);
            animation: fadeIn 0.6s ease;
        }

        /* Заголовок и стили подзаголовков */
        h2 {
            color: #FFFFFF;
            font-size: 32px;
            font-weight: bold;
            margin-top: 0;
        }
        h3 {
            color: #4CAF50;
            font-size: 24px;
            margin-top: 20px;
            display: flex;
            align-items: center;
        }

        /* Описание и стиль текста */
        p {
            color: #E0E0E0;
            line-height: 1.6;
            font-size: 16px;
        }

        /* Блоки информации */
        .feature {
            margin-bottom: 25px;
            padding-left: 15px;
            border-left: 4px solid #4CAF50;
        }

        /* Стиль для ссылки на почту */
        .footer a, #email-link {
            color: #4CAF50;
            font-weight: bold;
            text-decoration: none;
            transition: color 0.3s ease;
        }
        .footer a:hover, #email-link:hover {
            color: #2DAF6B;
        }

        /* Стиль нижнего колонтитула */
        .footer {
            font-size: 14px;
            color: #AAAAAA;
            margin-top: 40px;
            text-align: center;
        }

        /* Анимация плавного появления контейнера */
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(-10px); }
            to { opacity: 1; transform: translateY(0); }
        }
    </style>
</head>
<body oncontextmenu="return false;">
    <div class="container">
        <h2>Добро пожаловать в AVSpeak!</h2>
        <p>AVSpeak — это мощное приложение для голосового общения. Вот краткое руководство по использованию:</p>
        
        <div class="feature">
            <h3>Подключение к серверу</h3>
            <p>Чтобы присоединиться к разговору, выберите сервер из списка сохранённых или введите новый адрес сервера во вкладке "Подключение к серверу".</p>
        </div>
        
        <div class="feature">
            <h3>Создание сервера</h3>
            <p>Создайте свой собственный сервер, нажав кнопку "Создать сервер".</p>
        </div>
        
        <div class="feature">
            <h3>Настройки профиля</h3>
            <p>Добавьте аватар и измените его под свой вкус в разделе профиль, нажав на иконку аватара на правой панели.</p>
        </div>
        
        <div class="feature">
            <h3>Управление звуком</h3>
            <p>Настройте микрофон и наушники для управления вашим аудио в разделе "Настройки".</p>
        </div>
        
        <p>Возникли вопросы? Обратитесь в службу поддержки: <a href="javascript:void(0);" id="email-link" onclick="copyEmailToClipboard()">avspeakoff@gmail.com</a>.</p>

        <p class="footer">С уважением,<br>Команда AVSpeak</p>
    </div>

    <script>
        // Функция копирования email в буфер обмена и отображения уведомления
        function copyEmailToClipboard() {
            var emailText = document.getElementById("email-link").textContent;
            var textarea = document.createElement("textarea");
            textarea.value = emailText;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand("copy");
            document.body.removeChild(textarea);

            alert("Email скопирован в буфер обмена: " + emailText);
        }
    </script>
</body>
<style>
    body {
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
        user-select: none;
    }
</style>
</html>
""".trimIndent()


        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        titleBar.children.addAll(spacer, helpButtonTop, Label("  "),minimizeButton, closeButton)
        titleBar.alignment = Pos.CENTER_LEFT

        val rightPanel = createRightPanel(primaryStage)

        val headerLabel = Label("Помощь")
        headerLabel.styleClass.add("savedd")
        headerLabel.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 70px;"
        // VBox для формы
        val formBox = VBox(20.0)
        formBox.style = "-fx-padding: 40; -fx-alignment: center;"
        formBox.children.add(headerLabel)
        formBox.alignment = Pos.CENTER


        val webView = WebView()
        webView.engine.loadContent(helpContent, "text/html")


        val root = BorderPane()
        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()
        root.top = titleBar
        root.right = rightPanel
        //root.center = formBox
        root.center = webView


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
            if(AppState.isAuthenticated.get()){
                ConnectionWindow().openConnectionForm(primaryStage)
                println("Открытие окна подключения к серверу")}
            else {
                connectButton.style = "-fx-opacity: 0.5"
            }
        }

        val createServerButton = Button("Создать сервер")
        createServerButton.styleClass.add("Rbutton")
        createServerButton.setOnAction {
            if(AppState.isAuthenticated.get()){
                CreationWindow().openCreationForm(primaryStage)
                println("Создание нового сервера")}
            else {
                createServerButton.style = "-fx-opacity: 0.5"
            }
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
        smallAvatarWrapper.styleClass.add("avatar-wrapper")

        smallAvatarWrapper.setOnMouseClicked {
            if (AppState.isAuthenticated.get()) {
                ProfileWindow().openProfileForm(primaryStage) // Открываем профиль, если авторизован
            }
            else {
                smallAvatarWrapper.style = "-fx-opacity: 0.5"
            }
        }

        val smallUserNameLabel = Label(" ${AppState.username ?: "Гость"} ")
        smallUserNameLabel.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold"

        val settingsButton = Button("Настройки")
        settingsButton.styleClass.add("Rbutton")
        settingsButton.setOnAction {
            if(AppState.isAuthenticated.get()){
            SettingsWindow().openSettingsForm(primaryStage)
            println("Открытие настроек пользователя")}
            else {
                settingsButton.style = "-fx-opacity: 0.5"
            }
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
}
