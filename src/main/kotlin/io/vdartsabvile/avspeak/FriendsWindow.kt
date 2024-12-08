package io.vdartsabvile.avspeak

import atlantafx.base.theme.CupertinoDark
import io.vdartsabvile.ConnectionWindow
import io.vdartsabvile.avspeak.Client.ClientFriendProfileWindow
import javafx.animation.*
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class FriendsWindow(private val currentUsername: String) {

    private var xOffset = 0.0
    private var yOffset = 0.0
    private var smallAvatarImageView = AppState.smallAvatarImageView
    val root = BorderPane()

    val friendsList = AppState.friendsList

    private val friendsListView = createFriendsListView()


    // Стили для ListView друзей и запросов на дружбу
    fun styleListView(listView: ListView<*>) {
        listView.style = """
        -fx-background-color: #444;
        -fx-control-inner-background: #444;
        -fx-cell-size: 50px;
        -fx-max-width: 1100px;
        -fx-border-radius: 5px;
        -fx-border-color: #06f;
        -fx-padding: 5;
    """.trimIndent()
    }

    fun openFriendsForm(primaryStage: Stage) {
        // Установка иконки
        val ico = Image(javaClass.getResourceAsStream("/io/vdartsabvile/avspeak/ico.png"))
        primaryStage.icons.add(ico)

        updateFriendsListView()
        checkFriendStatuses()
        startStatusUpdateTimer()

        root.style = """
            -fx-background-radius: 13;                    
        """.trimIndent()

        showInfoPopup("Если список друзей не отображается - перезагрузите его.", primaryStage)

        // Панель заголовка
        val titleBar = HBox()
        titleBar.style = "-fx-padding: 10;"
        titleBar.alignment = Pos.CENTER_RIGHT

        // Иконка и название
        val icon = Image(javaClass.getResource("/io/vdartsabvile/avspeak/letter_a.png")?.toExternalForm())
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
        titleBar.children.addAll(spacer, helpButton, Label("  "),minimizeButton, closeButton)

        titleBar.alignment = Pos.CENTER_LEFT
        titleBar.setOnMousePressed { event ->
            xOffset = event.sceneX
            yOffset = event.sceneY
        }
        titleBar.setOnMouseDragged { event ->
            primaryStage.x = event.screenX - xOffset
            primaryStage.y = event.screenY - yOffset
        }

        val label = Label("Друзья")
        label.style = "-fx-font-family: 'Secession [by me]'; -fx-font-size: 80px; -fx-font-weight: normal;"
        label.styleClass.add("labell")

        // Добавление нового друга
        val addFriendLabel = Label("Добавить друга")
            addFriendLabel.styleClass.add("savedd")

        val addFriendField = javafx.scene.control.TextField()
        addFriendField.promptText = "Введите имя друга"
        addFriendField.style = """
            -fx-max-width: 600px;
            """

        val addFriendButton = Button("Добавить в друзья")
        addFriendButton.setOnAction {
            val friendName = addFriendField.text.trim()
            if (friendName.isNotEmpty()) {
                val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw" // Ваш токен для Яндекс Диска

                val task = object : Task<Pair<String?, Image?>>() {
                    override fun call(): Pair<String?, Image?> {
                        val userManager = UserManager()
                        val friendManager = FriendManager()
                        val friendIp = friendManager.getUserIpFromYandexDisk(token, friendName)
                        val avatarImage = userManager.downloadAvatarFromYandexDisk(token, friendName)
                        return Pair(friendIp, avatarImage)
                    }
                }

                task.setOnSucceeded { event ->
                    val (friendIp, avatarImage) = task.value
                    if (friendIp != null && avatarImage != null) {
                        addFriend(friendName, friendIp, avatarImage)
                        addFriendField.clear()
                        uploadFriendsInfo() // Обновляем данные на Яндекс Диске
                        println("Друг успешно добавлен: $friendName с IP: $friendIp")
                        showInfoPopup("Друг $friendName успешно добавлен!", primaryStage)
                    } else {
                        showErrorPopup("Не удалось получить информацию о пользователе $friendName")
                    }
                }

                task.setOnFailed { event ->
                    showErrorPopup("Ошибка при добавлении друга: ${task.exception.message}")
                }

                Thread(task).start() // Запускаем задачу в фоновом потоке
            } else {
                showErrorPopup("Введите имя друга")
            }
        }


        styleListView(friendsListView)

        // Применяем стиль к ячейкам ListView
        friendsListView.setCellFactory { listView ->
            object : ListCell<Friend>() {
                override fun updateItem(p0: Friend?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        text = null
                        style = null
                    } else {
                        text = item.toString()
                        style = "-fx-text-fill: white; -fx-font-size: 14px;"
                    }
                }
            }
        }

        val reloadButton = Button()
        reloadButton.style = """
            -fx-background-radius: 50;
            -fx-background-color: #2d2d2d;
            -fx-padding: 10;
        """.trimIndent()

        // Добавляем SVG-иконку
        val reloadIcon = SVGPath()
        reloadIcon.content = "M12 2a10 10 0 0 0 0 20h1v-2h-1a8 8 0 1 1 5.66-13.66l-1.42 1.42H22V2l-1.42 1.42A9.95 9.95 0 0 0 12 2z"
        reloadIcon.fill = Color.WHITE
        reloadIcon.scaleX = 1.5
        reloadIcon.scaleY = 1.5

        reloadButton.graphic = reloadIcon

        // Анимация вращения
        val rotateTransition = RotateTransition(Duration.seconds(1.0), reloadIcon)
        rotateTransition.byAngle = 360.0
        rotateTransition.cycleCount = RotateTransition.INDEFINITE

        reloadButton.setOnMousePressed {
            if (rotateTransition.status == Animation.Status.RUNNING) {
                rotateTransition.stop()
            } else {
                rotateTransition.play()
            }
        }

        reloadButton.setOnMouseClicked {
            println("Перезагрузка...")
            updateFriendsListView()
            checkFriendStatuses()
            val reloadDelayTimeline = Timeline(
                KeyFrame(Duration.seconds(2.0), {
            if (rotateTransition.status == Animation.Status.RUNNING) {
                rotateTransition.stop()
            } }))
            reloadDelayTimeline.play()
        }

        val vbox = VBox(20.0)
        vbox.children.addAll(label, reloadButton, friendsListView, addFriendLabel, addFriendField, addFriendButton)
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

    private fun createFriendsListView(): ListView<Friend> {
        return ListView<Friend>()
    }

    private fun addFriend(friendName: String, friendIp: String, avatarImage: Image) {
        val newFriend = Friend(friendName, friendIp, avatarImage)
        friendsList.add(newFriend)
        updateFriendsListView()
    }



    fun updateFriendsListView() {
        Platform.runLater {
            friendsListView.items.clear()
            friendsListView.items.addAll(friendsList)

            friendsListView.setCellFactory { listView ->
                object : ListCell<Friend>() {
                    private val avatarView = ImageView()
                    private val statusIndicator = Circle(10.0)
                    private val friendNameLabel = Label()
                    private val friendIpLabel = Label()
                    private val hBox = HBox(15.0, avatarView, VBox(5.0, friendNameLabel, friendIpLabel), statusIndicator)

                    init {
                        hBox.alignment = Pos.CENTER_LEFT
                        hBox.prefHeight = 100.0
                    }

                    override fun updateItem(friend: Friend?, empty: Boolean) {
                        super.updateItem(friend, empty)
                        if (empty || friend == null) {
                            text = null
                            graphic = null
                        } else {
                            friendNameLabel.text = friend.friendUsername
                            friendNameLabel.style = "-fx-text-fill: white; -fx-font-size: 17px;"
                            friendIpLabel.text = friend.friendIp
                            friendIpLabel.style = "-fx-text-fill: gray; -fx-font-size: 16px;"

                            avatarView.image = friend.avatarImage
                            avatarView.fitHeight = 80.0
                            avatarView.fitWidth = 80.0
                            avatarView.isPreserveRatio = true

                            statusIndicator.fill = if (friend.isOnline) Color.LIGHTGREEN else Color.RED

                            // Добавьте обработчик событий для клика на ячейку
                            setOnMouseClicked { event ->
                                if (event.clickCount == 1) {
                                    showFriendPopup(friend, listView.scene.window as Stage)
                                }
                            }

                            graphic = hBox
                        }
                    }
                }
            }
        }
    }

    private fun uploadFriendsInfo() {
        val userInfo = mapOf("username" to currentUsername) // Используем имя текущего пользователя
        val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw" // Ваш токен для доступа к API Яндекс Диска
        // Загружаем информацию о друзьях на Яндекс Диск
        val task = object : Task<Void>() {
            override fun call(): Void? {
                // Выполните запросы и загрузки здесь
                FriendManager().uploadFriendsInfoToYandexDisk(userInfo, friendsList, token, currentUsername)
                return null
            }
        }
        task.setOnSucceeded {
            println("Данные успешно загружены!")
        }
        task.setOnFailed {
            println("Ошибка загрузки: ${task.exception?.message}")
        }
        Thread(task).start()
        updateFriendsListView()

    }
    fun loadFriendsInfo() {
        val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw" // Токен для доступа к Яндекс.Диск
        val task = object : Task<List<Friend>>() {
            override fun call(): List<Friend> {
                return FriendManager().downloadFriendsInfoFromYandexDisk(token, currentUsername)
            }
        }

        task.setOnSucceeded {
            val loadedFriends = task.value
            friendsList.clear()
            friendsList.addAll(loadedFriends)

            // Обновляем friendsListView в UI-потоке
            Platform.runLater {
                updateFriendsListView()
                println("Информация о друзьях успешно загружена!")
                println("Загруженные друзья: $loadedFriends")
            }
        }

        task.setOnFailed {
            println("Ошибка загрузки информации о друзьях в loadFriendsInfo(): ${task.exception?.message}")
        }

        Thread(task).start()
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
        popupStage.initStyle(StageStyle.TRANSPARENT)
        popupStage.title = "Ошибка"

        // Делаем окно всегда поверх других
        popupStage.isAlwaysOnTop = true

        val label = Label(message)
        val timerLabel = Label("Исчезнет через 5 секунд...")
        val layout = VBox(10.0, label, timerLabel)
        layout.alignment = Pos.CENTER
        layout.style = "-fx-background-color: rgba(255, 0, 0, 0.8); -fx-padding: 10; -fx-background-radius: 8px; -fx-border-radius: 8px;"

        val scene = Scene(layout)
        scene.fill = null // Убираем цвет фона сцены
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
    private fun showInfoPopup(message: String,  primaryStage: Stage) {
        // Создаем всплывающее окно
        val popupStage = Stage()
        popupStage.initStyle(StageStyle.TRANSPARENT) // Устанавливаем стиль для прозрачного окна
        popupStage.initOwner(primaryStage) // Устанавливаем владельца окна

        // Создаем текст сообщения
        val label = Label(message)
        val timerLabel = Label("Исчезнет через 5 секунд...")

        // Настраиваем компоновку
        val layout = VBox(10.0, label, timerLabel)
        layout.alignment = Pos.CENTER
        layout.style = """
                -fx-background-color: rgba(127, 199, 255, 0.8); 
                -fx-padding: 10; 
                -fx-background-radius: 8px; 
                -fx-border-radius: 8px;
            """.trimIndent()

        // Создаем сцену с прозрачным фоном
        val scene = Scene(layout)
        scene.fill = null // Убираем цвет фона сцены

        // Устанавливаем сцену в окно
        popupStage.scene = scene

        // Устанавливаем положение окна
        popupStage.x = 180.0
        popupStage.y = 120.0
        popupStage.isAlwaysOnTop = true // Делаем окно всегда поверх других

        popupStage.show()

        // Анимация появления
        val fadeIn = FadeTransition(Duration.millis(500.0), layout)
        fadeIn.fromValue = 0.0
        fadeIn.toValue = 1.0
        fadeIn.play()

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

    fun startStatusUpdateTimer() {
        val timer = Timeline(
            KeyFrame(
                Duration.seconds(10.0), // Опрос каждые 10 секунд
                { checkFriendStatuses()
                    updateFriendsListView() }
            )
        )
        timer.cycleCount = Animation.INDEFINITE
        timer.play()
    }


    fun checkFriendStatuses() {
        val updatedFriends = fetchFriendStatuses() // Получаем обновленные данные
        Platform.runLater {
            updatedFriends.forEach { updatedFriend ->
                val existingFriend = friendsList.find { it.friendUsername == updatedFriend.friendUsername }
                if (existingFriend != null && existingFriend.isOnline != updatedFriend.isOnline) {
                    existingFriend.isOnline = updatedFriend.isOnline
                    updateFriendListItem(existingFriend)
                }
            }
        }
    }
    private fun updateFriendListItem(friend: Friend) {
        val index = friendsList.indexOf(friend)
        if (index != -1) {
            friendsListView.refresh()
        }
    }

    fun fetchFriendStatuses(): List<Friend> {
        val url = URL("http://localhost:3000/api/getFriendStatuses") // Замените на ваш URL
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        return try {
            connection.inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                val json = JSONObject(response)
                val friendsJson = json.getJSONArray("friends")
                val friends = mutableListOf<Friend>()
                for (i in 0 until friendsJson.length()) {
                    val friendJson = friendsJson.getJSONObject(i)
                    val token = "y0_AgAAAABp--5nAAy69AAAAAEXVwYhAAAbFRr3hBtCjok0h8HRTVOvZIXCVw"
                    val friendUsername = friendJson.getString("username")
                    friends.add(
                        Friend(
                            friendUsername = friendJson.getString("username"),
                            friendIp = friendJson.optString("friendIp", "Unknown"),
                            //avatarImage = UserManager().downloadAvatarFromYandexDisk(token, friendUsername),
                            isOnline = friendJson.getBoolean("isOnline")
                        )
                    )
                }
                friends
            }
        } catch (e: Exception) {
            println("Ошибка получения статусов: ${e.message}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
    private fun showFriendPopup(friend: Friend, primaryStage: Stage) {
        val popupStage = Stage()
        popupStage.initStyle(StageStyle.TRANSPARENT)
        popupStage.initOwner(primaryStage) // Устанавливаем владельца для popupStage

        // Создание кнопки "Х"
        val closeButton = Button("x")
        closeButton.style = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 19px; -fx-font-weight: bold;"
        closeButton.setOnAction { popupStage.close() } // Закрытие окна при нажатии
        // Выравнивание кнопки "Х" в правом верхнем углу
        val closeButtonContainer = StackPane(closeButton)
        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT)

        val avatarView = ImageView(friend.avatarImage)
        avatarView.fitWidth = 100.0
        avatarView.fitHeight = 100.0
        avatarView.isPreserveRatio = true
        val backgroundCircle = Circle(65.0) // Радиус чуть больше радиуса аватара
        backgroundCircle.fill = Color.GRAY // Задаем цвет фона

        // Объединяем круг фона и аватар
        val avatarWrapper = StackPane(backgroundCircle, avatarView)
        avatarWrapper.styleClass.add("avatar-wrapper")
        avatarWrapper.setOnMouseClicked {
            popupStage.close() // Закрываем текущее всплывающее окно
            //FriendProfileWindow(friend).openFriendProfileWindow(primaryStage)
            ClientFriendProfileWindow(friend).openClientFriendProfileWindow(primaryStage)
        }

        avatarWrapper.alignment = Pos.CENTER

        val nameLabel = Label(friend.friendUsername)
        nameLabel.style = "-fx-font-size: 20px; -fx-font-weight: bold;"

        val ipLabel = Label("IP: ${friend.friendIp}")
        ipLabel.style = "-fx-font-size: 14px;"

        val statusLabel = Label("Статус: ${if (friend.isOnline) "Онлайн" else "Оффлайн"}")
        statusLabel.styleClass.add("specialText")
        statusLabel.style = "-fx-text-fill: ${if (friend.isOnline) "lightgreen" else "red"}; -fx-font-size: 16px;"

        val callButton = Button("Позвонить")
        val messageButton = Button("Написать")

        val buttonsBox = HBox(15.0, callButton, messageButton)
        buttonsBox.alignment = Pos.CENTER

        val vbox = VBox(15.0, closeButtonContainer, avatarWrapper, nameLabel, ipLabel, statusLabel, buttonsBox)
        vbox.alignment = Pos.CENTER
        vbox.style = "-fx-padding: 10; -fx-background-color: #2d2d2d; -fx-background-radius: 15; -fx-border-radius: 15;"
        vbox.prefWidth = 300.0
        vbox.prefHeight = 400.0


        // Контейнер с прозрачным фоном
        val backgroundPane = StackPane(vbox)
        backgroundPane.style = "-fx-background-color: rgba(0, 0, 0, 0);" // Прозрачный черный фон

        // Удаляем конфликтующий обработчик на сцене
        val scene = Scene(backgroundPane)
        scene.fill = Color.TRANSPARENT


        popupStage.scene = scene

        // Перетаскиваемость окна
        val dragDelta = DoubleArray(2)
        vbox.setOnMousePressed { event ->
            dragDelta[0] = popupStage.x - event.screenX
            dragDelta[1] = popupStage.y - event.screenY
        }
        vbox.setOnMouseDragged { event ->
            popupStage.x = event.screenX + dragDelta[0]
            popupStage.y = event.screenY + dragDelta[1]
        }

        popupStage.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                // Если окно теряет фокус, закрываем его
                popupStage.close()
            }
        }

        popupStage.show()

        // Анимация появления
        val fadeIn = FadeTransition(Duration.millis(300.0), vbox)
        fadeIn.fromValue = 0.0
        fadeIn.toValue = 1.0
        fadeIn.play()

        // Обработчики кнопок
        callButton.setOnAction {
            println("Звонок другу ${friend.friendUsername}")
        }

        messageButton.setOnAction {
            println("Сообщение другу ${friend.friendUsername}")
        }

        // Закрытие по клику вне окна
        scene.setOnMouseClicked { event ->
            if (event.target == scene.root) {
                val fadeOut = FadeTransition(Duration.millis(200.0), vbox)
                fadeOut.fromValue = 1.0
                fadeOut.toValue = 0.0
                fadeOut.setOnFinished { popupStage.close() }
                fadeOut.play()
            }
        }

        // Добавить стили
        scene.stylesheets.add(CupertinoDark().userAgentStylesheet)
        scene.stylesheets.add(javaClass.getResource("/io/vdartsabvile/avspeak/styles.css")?.toExternalForm())
    }


}
